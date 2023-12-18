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

package oap.ws.sso;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.application.testng.KernelFixture;
import oap.testng.Fixtures;
import oap.testng.SystemTimerFixture;
import oap.util.Pair;
import oap.util.Result;
import oap.ws.account.UserData;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.AfterMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static oap.io.Resources.urlOrThrow;
import static oap.ws.account.testing.SecureWSFixture.assertLogout;
import static oap.ws.sso.AuthenticationFailure.TFA_REQUIRED;
import static oap.ws.sso.AuthenticationFailure.UNAUTHENTICATED;
import static oap.ws.sso.AuthenticationFailure.WRONG_TFA_CODE;
import static oap.ws.sso.UserProvider.toAccessKey;

public class IntegratedTest extends Fixtures {
    protected final KernelFixture kernelFixture;

    public IntegratedTest() {
        kernelFixture = fixture( new KernelFixture( urlOrThrow( getClass(), "/application.test.conf" ) ) );
        fixture( SystemTimerFixture.FIXTURE );
    }

    protected TestUserProvider userProvider() {
        return kernelFixture.service( "oap-ws-sso-test", TestUserProvider.class );
    }

    protected JWTExtractor tokenExtractor() {
        return kernelFixture.service( "oap-ws-sso-api", JWTExtractor.class );
    }


    @AfterMethod
    public void afterMethod() {
        assertLogout();
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

        private Result<? extends User, AuthenticationFailure> getAuthenticationResult( Optional<String> tfaCode, Optional<UserData> authenticated ) {
            if( authenticated.isPresent() ) {
                UserData userData = authenticated.get();
                if( !userData.user.tfaEnabled ) {
                    return Result.success( userData );
                } else {
                    if( tfaCode.isEmpty() ) {
                        return Result.failure( TFA_REQUIRED );
                    }
                    boolean tfaCheck = tfaCode.map( "proper_code"::equals )
                        .orElse( false );
                    return tfaCheck ? Result.success( userData ) : Result.failure( WRONG_TFA_CODE );
                }
            }
            return Result.failure( UNAUTHENTICATED );
        }

        @Override
        public Result<? extends User, AuthenticationFailure> getAuthenticated( String email, Optional<String> tfaCode ) {
            final Optional<TestUser> user = users.stream().filter( u -> u.getEmail().equalsIgnoreCase( email ) ).findAny();
            UserData userData = null;
            if( user.isPresent() ) {
                userData = new UserData( new oap.ws.account.User( user.get().email, "", "", null, true, user.get().tfaEnabled ), user.get().roles );
            }
            return getAuthenticationResult( tfaCode, Optional.ofNullable( userData ) );
        }

        @Override
        public Result<TestUser, AuthenticationFailure> getAuthenticated( String email, String password, Optional<String> mfaCode ) {
            log.trace( "authenticating {} with {}", email, password );
            log.trace( "users {}", users );
            return users.stream()
                .filter( u -> u.getEmail().equalsIgnoreCase( email ) && u.password.equals( password ) )
                .map( user -> {
                    if( user.tfaEnabled ) {
                        if( mfaCode.isEmpty() ) return Result.<TestUser, AuthenticationFailure>failure( TFA_REQUIRED );
                        var mfaCheck = mfaCode.map( "proper_code"::equals ).orElse( false );
                        return mfaCheck ? Result.<TestUser, AuthenticationFailure>success( user )
                            : Result.<TestUser, AuthenticationFailure>failure( WRONG_TFA_CODE );
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
    public static class TestUser implements User {
        public final String email;
        public final String password;
        public Map<String, String> roles = new HashMap<>();
        public final boolean tfaEnabled;
        public final String apiKey = RandomStringUtils.random( 10, true, true );
        public String defaultOrganization = "";
        public Map<String, String> defaultAccounts = new HashMap<>();
        @JsonIgnore
        public final View view = new View();

        public TestUser( String email, String password, Pair<String, String> role ) {
            this( email, password, role, false );
        }

        public TestUser( String email, String password, Map<String, String> roles ) {
            this.email = email;
            this.password = password;
            this.roles = roles;
            this.tfaEnabled = false;
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
        public Optional<String> getDefaultOrganization() {
            return Optional.ofNullable( defaultOrganization );
        }

        @Override
        public Map<String, String> getDefaultAccounts() {
            return defaultAccounts;
        }

        @Override
        public Optional<String> getDefaultAccount( String organizationId ) {
            return Optional.ofNullable( defaultAccounts.get( organizationId ) );
        }

        @Override
        public View getView() {
            return view;
        }

        public String getAccessKey() {
            return toAccessKey( email );
        }

        public class View implements User.View {
            @Override
            public String getEmail() {
                return TestUser.this.getEmail();
            }
        }

    }
}
