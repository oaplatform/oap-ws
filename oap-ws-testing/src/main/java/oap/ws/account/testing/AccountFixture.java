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

import oap.application.testng.AbstractKernelFixture;
import oap.http.testng.HttpAsserts;
import oap.json.Binder;
import oap.mail.MailQueue;
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

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static oap.io.Resources.urlOrThrow;
import static oap.ws.account.Roles.ADMIN;
import static oap.ws.account.Roles.ORGANIZATION_ADMIN;

public class AccountFixture extends AbstractKernelFixture<AccountFixture> {
    public static final String DEFAULT_PASSWORD = "Xenoss123";
    public static final String DEFAULT_ACCOUNT_ID = "DFLTACCT";
    public static final String DEFAULT_ORGANIZATION_ID = "DFLT";
    public static final String DEFAULT_ORGANIZATION_ADMIN_EMAIL = "orgadmin@admin.com";
    public static final String SYSTEM_ORGANIZATION_ADMIN_EMAIL = "systemadmin@admin.com";
    public static final String DEFAULT_ADMIN_EMAIL = "xenoss@xenoss.io";
    public static final User ORG_ADMIN_USER = new User( "org@admin.com", "Joe", "Haserton", DEFAULT_PASSWORD, true );
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
        return service( "oap-ws-account", OrganizationStorage.class );
    }

    public Accounts accounts() {
        return service( "oap-ws-account", AccountsService.class );
    }

    public UserStorage userStorage() {
        return service( "oap-ws-account", UserStorage.class );
    }

    @Override
    protected void before() {
        super.before();

        if( withoutMigration )
            reset();
    }

    public AccountMailman accountMailman() {
        return service( "oap-ws-account", AccountMailman.class );
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

    public AccountFixture withDefaultSystemAdmin( String email, String password, String firstName, String lastName, Map<String, String> roles, boolean ro ) {
        define( "DEFAULT_SYSTEM_ADMIN_EMAIL", email );
        define( "DEFAULT_SYSTEM_ADMIN_PASSWORD", password );
        define( "DEFAULT_SYSTEM_ADMIN_FIRST_NAME", firstName );
        define( "DEFAULT_SYSTEM_ADMIN_LAST_NAME", lastName );
        define( "DEFAULT_SYSTEM_ADMIN_ROLES", "json(" + Binder.json.marshal( roles ) + ")" );
        define( "DEFAULT_SYSTEM_READ_ONLY", ro );

        return this;
    }

    public AccountFixture withDefaultOrganization( String id, String name, String description, boolean ro ) {
        define( "DEFAULT_ORGANIZATION_ID", id );
        define( "DEFAULT_ORGANIZATION_NAME", name );
        define( "DEFAULT_ORGANIZATION_DESCRIPTION", description );
        define( "DEFAULT_ORGANIZATION_READ_ONLY", ro );

        return this;
    }

    @Override
    public void after() {
        assertLogout();
        super.after();
    }

}
