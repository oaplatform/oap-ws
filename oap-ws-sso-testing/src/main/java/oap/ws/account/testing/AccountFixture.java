/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package oap.ws.account.testing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.application.testng.AbstractKernelFixture;
import oap.http.testng.HttpAsserts;
import oap.mail.MailQueue;
import oap.util.Pair;
import oap.util.Result;
import oap.ws.account.Account;
import oap.ws.account.AccountMailman;
import oap.ws.account.Accounts;
import oap.ws.account.AccountsService;
import oap.ws.account.Organization;
import oap.ws.account.OrganizationData;
import oap.ws.account.OrganizationStorage;
import oap.ws.account.User;
import oap.ws.account.UserData;
import oap.ws.account.UserStorage;
import oap.ws.sso.AuthenticationFailure;
import oap.ws.sso.UserProvider;
import oap.ws.sso.testng.SecureWSFixture;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.AfterMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static oap.io.Resources.urlOrThrow;
import static oap.ws.account.Roles.ADMIN;
import static oap.ws.account.Roles.ORGANIZATION_ADMIN;
import static oap.ws.sso.AuthenticationFailure.MFA_REQUIRED;
import static oap.ws.sso.AuthenticationFailure.UNAUTHENTICATED;
import static oap.ws.sso.UserProvider.toAccessKey;

public class AccountFixture extends AbstractKernelFixture<AccountFixture> {
    public static final String DEFAULT_PASSWORD = "Xenoss123";
    public static final String DEFAULT_ACCOUNT_ID = "DFLTACCT";
    public static final String DEFAULT_ORGANIZATION_ID = "DFLT";
    public static final String DEFAULT_ORGANIZATION_ADMIN_EMAIL = "orgadmin@admin.com";
    public static final String SYSTEM_ORGANIZATION_ADMIN_EMAIL = "systemadmin@admin.com";
    public static final String DEFAULT_ADMIN_EMAIL = "xenoss@xenoss.io";
    public static final User ORG_ADMIN_USER = new User( "org@admin.com", "Joe", "Haserton", DEFAULT_PASSWORD, true );
//    public static final User ORG_ADMIN_USER_TFA = new User( "org@admin.com", "Joe", "Haserton", DEFAULT_PASSWORD, true, true );
    public static final User REGULAR_USER = new User( "user@admin.com", "Joe", "Epstein", DEFAULT_PASSWORD, true );

    private static final String PREFIX = "ACCOUNT_FIXTURE_";

    private boolean withoutMigration = false;

    public AccountFixture() {
        super( PREFIX, urlOrThrow( AccountFixture.class, "/application-account.fixture.conf" ) );

        define( "SESSION_MANAGER_EXPIRATION_TIME", "24h" );
    }

    public AccountFixture withoutMigration() {
        withoutMigration = true;
        define( "MONGO_MIGRATIONS", false );
        return this;
    }

    public AccountFixture withSessionManagerExpirationTime( long value, TimeUnit timeUnit ) {
        define( "SESSION_MANAGER_EXPIRATION_TIME", timeUnit.toMillis( value ) );
        return this;
    }

    public void assertAdminLogin() {
        assertLogin( DEFAULT_ADMIN_EMAIL, DEFAULT_PASSWORD );
    }

    public void assertSystemAdminLogin() {
        assertLogin( SYSTEM_ORGANIZATION_ADMIN_EMAIL, DEFAULT_PASSWORD );
    }

    public void assertOrgAdminLogin() {
        assertLogin( DEFAULT_ORGANIZATION_ADMIN_EMAIL, DEFAULT_PASSWORD );
    }

    public void assertLogin( String login, String password ) {
        SecureWSFixture.assertLogin( login, password, defaultHttpPort() );
    }

    public void assertLogout() {
        SecureWSFixture.assertLogout( defaultHttpPort() );
    }

    public OrganizationStorage organizationStorage() {
        return service( "oap-ws-sso", OrganizationStorage.class );
    }

    public Accounts accounts() {
        return service( "oap-ws-sso", AccountsService.class );
    }

    public UserStorage userStorage() {
        return service( "oap-ws-sso", UserStorage.class );
    }

    @Override
    protected void before() {
        super.before();

        if( withoutMigration )
            reset();
    }

    public AccountMailman accountMailman() {
        return service( "oap-ws-sso", AccountMailman.class );
    }

    public MailQueue mailQueue() {
        return service( "oap-mail", MailQueue.class );
    }

    public void reset() {
        organizationStorage().deleteAllPermanently();
        userStorage().deleteAllPermanently();

        var defaultOrganization = new OrganizationData( new Organization( DEFAULT_ORGANIZATION_ID, "Default", "Default organization" ) );
        organizationStorage().store( defaultOrganization );
        accounts().storeAccount( DEFAULT_ORGANIZATION_ID, new Account( DEFAULT_ACCOUNT_ID, "Account Default" ) );

        var orgAdmin = new UserData( new User( DEFAULT_ORGANIZATION_ADMIN_EMAIL, "Johnny", "Walker", DEFAULT_PASSWORD, true ),
            Map.of( DEFAULT_ORGANIZATION_ID, ORGANIZATION_ADMIN ) );
        orgAdmin.user.apiKey = "pz7r93Hh8ssbcV1Qhxsopej18ng2Q";
        userStorage().store( orgAdmin );

        var globalAdmin = new UserData( new User( DEFAULT_ADMIN_EMAIL, "System", "Admin", DEFAULT_PASSWORD, true ), Map.of( DEFAULT_ORGANIZATION_ID, ADMIN ) );
        globalAdmin.user.apiKey = "bDzf7zu8ngd4YLH29GsqkTGDtL23jy";
        userStorage().store( globalAdmin );
    }

    public String httpUrl( String url ) {
        return HttpAsserts.httpUrl( defaultHttpPort(), url );
    }

    public TestUserProvider userProvider() {
        return service( "oap-ws-sso", TestUserProvider.class );
    }

    @AfterMethod
    public void afterMethod() {
        assertLogout();
        super.after();
    }

    @Slf4j
    public static class TestUserProvider implements UserProvider {
        public final List<TestUser> users = new ArrayList<>();

        public TestUser addUser( String email, String password, Pair<String, String> role ) {
            return addUser( new TestUser( email, password, role ) );
        }

        public TestUser addUser( TestUser user ) {
            users.add( user );
            return user;
        }

        @Override
        public Optional<TestUser> getUser( String email ) {
            return users.stream().filter( u -> u.getEmail().equalsIgnoreCase( email ) ).findAny();
        }

        @Override
        public Result<TestUser, AuthenticationFailure> getAuthenticated( String email, String password, Optional<String> tfaCode ) {
            log.trace( "authenticating {} with {}", email, password );
            log.trace( "users {}", users );
            return users.stream()
                .filter( u -> u.getEmail().equalsIgnoreCase( email ) && u.password.equals( password ) )
                .map( user -> {
                    if( user.tfaEnabled ) {
                        var tfaCheck = tfaCode.map( "proper_code"::equals ).orElse( false );
                        return tfaCheck ? Result.<TestUser, AuthenticationFailure>success( user )
                            : Result.<TestUser, AuthenticationFailure>failure( MFA_REQUIRED );
                    }
                    return Result.<TestUser, AuthenticationFailure>success( user );
                } )
                .findAny().orElse( Result.failure( UNAUTHENTICATED ) );
        }

        @Override
        public Optional<TestUser> getAuthenticatedByApiKey( String accessKey, String apiKey ) {
            return users.stream()
                .filter( u -> u.getAccessKey().equals( accessKey ) && u.apiKey.equals( apiKey ) )
                .findAny();
        }
    }

    @ToString
    @EqualsAndHashCode
    public static class TestUser implements oap.ws.sso.User {
        public final String email;
        public final String password;
        public final Map<String, String> roles = new HashMap<>();
        public final boolean tfaEnabled;
        public final String apiKey = RandomStringUtils.random( 10, true, true );
        @JsonIgnore
        public final View view = new View();

        public TestUser( String email, String password, Pair<String, String> role ) {
            this( email, password, role, false );
        }

        public TestUser( String email, String password, Pair<String, String> role, boolean tfaEnabled ) {
            this.email = email;
            this.password = password;
            this.roles.put( role._1, role._2 );
            this.tfaEnabled = tfaEnabled;
        }

        @Override
        public String getEmail() {
            return email;
        }

        @Override
        public Optional<String> getRole( String realm ) {
            return Optional.ofNullable( roles.get( realm ) );
        }

        @Override
        public Map<String, String> getRoles() {
            return roles;
        }

        @Override
        public View getView() {
            return view;
        }

        public String getAccessKey() {
            return toAccessKey( email );
        }

        public class View implements oap.ws.sso.User.View {
            @Override
            public String getEmail() {
                return TestUser.this.getEmail();
            }
        }

    }
}
