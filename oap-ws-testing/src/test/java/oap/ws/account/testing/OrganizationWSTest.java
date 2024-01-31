/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account.testing;

import oap.http.Http;
import oap.storage.mongo.MongoFixture;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import oap.ws.account.Account;
import oap.ws.account.Organization;
import oap.ws.account.OrganizationData;
import oap.ws.account.User;
import oap.ws.account.UserData;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static oap.http.Http.StatusCode.BAD_REQUEST;
import static oap.http.Http.StatusCode.FORBIDDEN;
import static oap.http.Http.StatusCode.NOT_FOUND;
import static oap.http.Http.StatusCode.OK;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.assertPost;
import static oap.mail.test.MessageAssertion.assertMessage;
import static oap.mail.test.MessagesAssertion.assertMessages;
import static oap.testng.Asserts.assertString;
import static oap.testng.Asserts.contentOfTestResource;
import static oap.ws.account.Roles.ADMIN;
import static oap.ws.account.Roles.ORGANIZATION_ADMIN;
import static oap.ws.account.Roles.USER;
import static oap.ws.account.testing.AccountFixture.DEFAULT_ORGANIZATION_ADMIN_EMAIL;
import static oap.ws.account.testing.AccountFixture.DEFAULT_ORGANIZATION_ID;
import static oap.ws.account.testing.AccountFixture.DEFAULT_PASSWORD;
import static oap.ws.account.testing.AccountFixture.ORG_ADMIN_USER;
import static oap.ws.account.testing.AccountFixture.REGULAR_USER;
import static oap.ws.validate.testng.ValidationAssertion.assertValidation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.time.DateTimeZone.UTC;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

public class OrganizationWSTest extends Fixtures {
    public static final String TODAY = DateTimeFormat.forPattern( "yyyy-MM-dd" ).print( DateTime.now( UTC ) );

    protected final AccountFixture accountFixture;

    public OrganizationWSTest() {
        fixture( new TestDirectoryFixture() );
        fixture( new MongoFixture() );
        accountFixture = fixture( new AccountFixture() );
    }

    @AfterMethod
    public void afterMethod() {
        accountFixture.assertLogout();
    }

    @Test
    public void store() {
        OrganizationData data = accountFixture.accounts().storeOrganization( new Organization( "test", "test" ) );
        accountFixture.assertSystemAdminLogin();
        assertPost( accountFixture.httpUrl( "/organizations/" + data.organization.id ), "{\"id\":\"" + data.organization.id + "\", \"name\":\"newname\"}", Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertThat( accountFixture.organizationStorage().get( data.organization.id ) ).isPresent().get()
            .satisfies( d -> assertString( d.organization.name ).isEqualTo( "newname" ) );
    }

    @Test
    public void storeOrgAdmin() {
        OrganizationData data = accountFixture.accounts().storeOrganization( new Organization( "test", "test" ) );
        UserData user = accountFixture.userStorage().store( new UserData( ORG_ADMIN_USER, Map.of( data.organization.id, ORGANIZATION_ADMIN ) ) );
        accountFixture.assertLogin( user.user.email, DEFAULT_PASSWORD );
        assertPost( accountFixture.httpUrl( "/organizations/" + data.organization.id ), "{\"id\":\"" + data.organization.id + "\", \"name\":\"newname\"}", Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertThat( accountFixture.organizationStorage().get( data.organization.id ) ).isPresent().get()
            .satisfies( d -> assertString( d.organization.name ).isEqualTo( "newname" ) );
    }

    @Test
    public void getOrgAdmin() {
        OrganizationData data = accountFixture.organizationStorage().store( new OrganizationData( new Organization( "test", "test" ) ) );
        UserData user = accountFixture.userStorage().store( new UserData( ORG_ADMIN_USER, Map.of( data.organization.id, ORGANIZATION_ADMIN ) ) );
        accountFixture.assertLogin( user.user.email, DEFAULT_PASSWORD );
        assertGet( accountFixture.httpUrl( "/organizations/" + data.organization.id ) )
            .respondedJson( OK, "OK", "{\"description\":\"test\", \"id\":\"TST\", \"name\":\"test\"}" );
    }

    @Test
    public void listOrgAdmin() {
        OrganizationData data = accountFixture.accounts().storeOrganization( new Organization( "test", "test" ) );
        UserData user = accountFixture.userStorage().store( new UserData( ORG_ADMIN_USER, Map.of( data.organization.id, ORGANIZATION_ADMIN ) ) );
        accountFixture.assertLogin( user.user.email, DEFAULT_PASSWORD );
        assertGet( accountFixture.httpUrl( "/organizations" ) )
            .respondedJson( OK, "OK", "[{\"description\":\"test\", \"id\":\"TST\", \"name\":\"test\"}]" );
    }

    @Test
    public void list() {
        accountFixture.accounts().storeOrganization( new Organization( "test", "test" ) );
        accountFixture.assertAdminLogin();
        assertGet( accountFixture.httpUrl( "/organizations" ) )
            .respondedJson( OK, "OK", "[{\"description\":\"test\", \"id\":\"TST\", \"name\":\"test\"}, "
                + "{\"description\": \"Default organization\", \"id\": \"" + DEFAULT_ORGANIZATION_ID + "\", \"name\": \"Default\"}]" );
        accountFixture.assertLogout();
        accountFixture.assertOrgAdminLogin();
        assertGet( accountFixture.httpUrl( "/organizations" ) )
            .respondedJson( OK, "OK", "[{\"description\": \"Default organization\", \"id\": \"" + DEFAULT_ORGANIZATION_ID + "\", \"name\": \"Default\"}]" );
    }

    @Test
    public void storeAccountOrgAdmin() {
        OrganizationData data = accountFixture.accounts().storeOrganization( new Organization( "test", "test" ) );
        UserData user = accountFixture.userStorage().store( new UserData( ORG_ADMIN_USER, Map.of( data.organization.id, ORGANIZATION_ADMIN ) ) );
        accountFixture.assertLogin( user.user.email, DEFAULT_PASSWORD );
        assertPost( accountFixture.httpUrl( "/organizations/" + data.organization.id + "/accounts" ), "{\"name\":\"acc1\"}", Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertThat( data.accounts ).containsOnly( new Account( "CC1", "acc1" ) );
    }

    @Test
    public void listAccountsOrgAdmin() {
        OrganizationData data = accountFixture.accounts().storeOrganization( new Organization( "test", "test" ) );
        accountFixture.accounts().storeAccount( data.organization.id, new Account( "acc2", "acc2" ) );
        accountFixture.accounts().storeAccount( data.organization.id, new Account( "acc1", "acc1" ) );
        UserData user = accountFixture.userStorage().store( new UserData( ORG_ADMIN_USER, Map.of( data.organization.id, ORGANIZATION_ADMIN ) ) );
        accountFixture.assertLogin( user.user.email, DEFAULT_PASSWORD );
        assertGet( accountFixture.httpUrl( "/organizations/" + data.organization.id + "/accounts" ) )
            .respondedJson( OK, "OK", "[{\"id\":\"acc2\", \"name\":\"acc2\"}, {\"id\":\"acc1\", \"name\":\"acc1\"}]" );
    }

    @Test
    public void listAccountsUser() {
        OrganizationData data = accountFixture.accounts().storeOrganization( new Organization( "test", "test" ) );
        accountFixture.accounts().storeAccount( data.organization.id, new Account( "acc2", "acc2" ) );
        accountFixture.accounts().storeAccount( data.organization.id, new Account( "acc1", "acc1" ) );
        accountFixture.organizationStorage().store( data );
        UserData user = new UserData( REGULAR_USER, Map.of( data.organization.id, USER ) );
        user.addAccount( data.organization.id, "acc1" );
        accountFixture.userStorage().store( user );
        accountFixture.assertLogin( user.user.email, DEFAULT_PASSWORD );
        assertGet( accountFixture.httpUrl( "/organizations/" + data.organization.id + "/accounts" ) )
            .respondedJson( OK, "OK", "[{\"id\":\"acc1\", \"name\":\"acc1\"}]" );
    }

    @Test
    public void getAccountUser() {
        Account account2 = new Account( "acc2", "acc2" );
        Account account1 = new Account( "acc1", "acc1" );
        OrganizationData data = new OrganizationData( new Organization( "test", "test" ) )
            .addOrUpdateAccount( account2 )
            .addOrUpdateAccount( account1 );
        accountFixture.organizationStorage().store( data );
        UserData user = new UserData( REGULAR_USER, Map.of( data.organization.id, USER ) );
        user.addAccount( data.organization.id, "acc1" );
        accountFixture.userStorage().store( user );
        accountFixture.assertLogin( user.user.email, DEFAULT_PASSWORD );
        assertGet( accountFixture.httpUrl( "/organizations/" + data.organization.id + "/accounts/" + account1.id ) )
            .respondedJson( OK, "OK", "{\"id\":\"acc1\", \"name\":\"acc1\"}" );
        assertGet( accountFixture.httpUrl( "/organizations/" + data.organization.id + "/accounts/" + account2.id ) )
            .hasCode( Http.StatusCode.FORBIDDEN );
        assertGet( accountFixture.httpUrl( "/organizations/" + data.organization.id + "/accounts/blabla" ) )
            .hasCode( Http.StatusCode.FORBIDDEN );
    }

    @Test
    public void account404() {
        OrganizationData data = accountFixture.accounts().storeOrganization( new Organization( "test", "test" ) );
        UserData user = accountFixture.userStorage().store( new UserData( ORG_ADMIN_USER, Map.of( data.organization.id, ORGANIZATION_ADMIN ) ) );
        accountFixture.assertLogin( user.user.email, DEFAULT_PASSWORD );
        assertPost( accountFixture.httpUrl( "/organizations/" + data.organization.id + "/accounts" ), "{\"id\":\"acc1\", \"name\":\"acc1\"}", Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertThat( data.accounts ).containsOnly( new Account( "acc1", "acc1" ) );
        assertGet( accountFixture.httpUrl( "/organizations/" + data.organization.id + "/accounts/acc1" ) )
            .respondedJson( OK, "OK", "{\"id\":\"acc1\", \"name\":\"acc1\"}" );
        assertPost( accountFixture.httpUrl( "/organizations/" + data.organization.id + "/accounts" ), "{\"id\":\"acc1\", \"name\":\"acc1\"}", Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertGet( accountFixture.httpUrl( "/organizations/" + data.organization.id + "/accounts/acc1" ) )
            .respondedJson( OK, "OK", "{\"id\":\"acc1\", \"name\":\"acc1\"}" );
    }

    @Test
    public void users() {
        accountFixture.assertOrgAdminLogin();
        assertGet( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users" ) )
            .respondedJson( OK, "OK",
                contentOfTestResource( getClass(), "users.json", Map.of( "LAST_LOGIN", TODAY ) ) );
    }

    @Test
    public void storeUserAdminByAdminCreateNew() {
        accountFixture.assertAdminLogin();
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users?role=ADMIN" ),
            contentOfTestResource( getClass(), "store-user-admin.json", Map.of() ), Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertThat( accountFixture.accounts().getUser( "newadmin@admins.com" ) )
            .isPresent()
            .get()
            .satisfies( u -> {
                assertThat( u.canAccessOrganization( DEFAULT_ORGANIZATION_ID ) ).isTrue();
                assertString( u.getRole( DEFAULT_ORGANIZATION_ID ).orElse( null ) ).isEqualTo( ADMIN );
            } );
    }

    @Test
    public void addUser() {
        String userEmail = "vk@xenoss.io";
        accountFixture.assertAdminLogin();
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users?role=USER" ),
            contentOfTestResource( getClass(), "user.json", Map.of( "EMAIL", userEmail ) ), Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertMessages( accountFixture.mailQueue().messages() )
            .sentTo( userEmail, message -> assertMessage( message )
                .hasSubject( "You've been invited" ) );
        accountFixture.assertLogout();
        assertPost( accountFixture.httpUrl( "/auth/login" ), "{\"email\": \"" + userEmail + "\", \"password\": \"pass\"}" )
            .hasCode( Http.StatusCode.UNAUTHORIZED );
        UserData user = accountFixture.userStorage().get( userEmail ).orElseThrow();
        String confirmUrl = accountFixture.accountMailman().confirmUrl( user );
        assertGet( confirmUrl )
            .hasCode( Http.StatusCode.FOUND )
            .containsHeader( "Location", "http://xenoss.io?apiKey=" + user.user.apiKey + "&accessKey=" + user.getAccessKey()
                + "&email=vk%40xenoss.io" + "&passwd=true" );
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/passwd?accessKey=" + user.getAccessKey() + "&apiKey=" + user.user.apiKey ), "{\"email\": \"vk@xenoss.io\", \"password\": \"pass\"}" )
            .hasCode( OK );
        accountFixture.assertLogin( userEmail, "pass" );
        accountFixture.assertLogout();
    }

    @Test

    public void registerUser() {
        String userEmail = "vk@xenoss.io";
        assertPost( accountFixture.httpUrl( "/organizations/register?organizationName=xenoss.io" ),
            contentOfTestResource( getClass(), "register-user.json", Map.of() ), Http.ContentType.APPLICATION_JSON )
            .respondedJson( getClass(), "registered-user.json" );
        UserData user = accountFixture.userStorage().get( userEmail ).orElseThrow();
        assertMessages( accountFixture.mailQueue().messages() )
            .sentTo( userEmail, message -> assertMessage( message )
                .hasSubject( "Registration successful" ) );
        assertThat( accountFixture.accounts().getOrganization( "XNSS" ) ).isNotEmpty();
        assertPost( accountFixture.httpUrl( "/auth/login" ), "{\"email\": \"" + user.user.email + "\", \"password\": \"pass\"}" )
            .hasCode( Http.StatusCode.UNAUTHORIZED );
        String confirmUrl = accountFixture.accountMailman().confirmUrl( user );
        assertGet( confirmUrl )
            .hasCode( Http.StatusCode.FOUND )
            .containsHeader( "Location", "http://xenoss.io?apiKey=" + user.user.apiKey + "&accessKey=" + user.getAccessKey()
                + "&email=vk%40xenoss.io" + "&passwd=false" );
        accountFixture.assertLogin( user.user.email, "pass" );
        accountFixture.assertLogout();
    }

    @Test
    public void storeUserAdminByAdminNotNewNotExist() {
        accountFixture.assertAdminLogin();
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users" ),
            contentOfTestResource( getClass(), "store-user-admin-not-new.json", Map.of() ), Http.ContentType.APPLICATION_JSON )
            .hasCode( Http.StatusCode.NOT_FOUND )
            .satisfies( response -> assertValidation( response )
                .hasErrors( "user newadmin@admins.com does not exists" ) );
    }

    @Test
    public void storeUserDuplicate() {
        accountFixture.assertAdminLogin();
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users?role=ADMIN" ),
            contentOfTestResource( getClass(), "store-user-admin.json", Map.of() ), Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users?role=ADMIN" ),
            contentOfTestResource( getClass(), "store-user-admin.json", Map.of() ), Http.ContentType.APPLICATION_JSON )
            .hasCode( Http.StatusCode.CONFLICT )
            .satisfies( response -> assertValidation( response )
                .hasErrors( "user with email newadmin@admins.com already exists" ) );
    }

    @Test
    public void storeUserAdminByNotAdmin() {
        accountFixture.assertOrgAdminLogin();
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users?role=ADMIN" ),
            contentOfTestResource( getClass(), "store-user-admin.json", Map.of() ), Http.ContentType.APPLICATION_JSON )
            .hasCode( Http.StatusCode.FORBIDDEN );
    }

    @Test
    public void storeUserWrongOrg() {
        accountFixture.assertOrgAdminLogin();
        assertPost( accountFixture.httpUrl( "/organizations/fake-org/users" ),
            contentOfTestResource( getClass(), "store-user-admin.json", Map.of() ), Http.ContentType.APPLICATION_JSON )
            .hasCode( Http.StatusCode.FORBIDDEN );
    }


    @Test
    public void passwd() {
        var email = "newuser@gmail.com";
        accountFixture.userStorage().store( new UserData( new User( "newuser@gmail.com", "John", "Smith", "pass123", true ), Map.of( DEFAULT_ORGANIZATION_ID, USER ) ) );
        accountFixture.assertLogin( email, "pass123" );
        assertPost( accountFixture.httpUrl( "/organizations/hackit/users/passwd" ), "{\"email\": \"" + email + "\", \"password\": \"newpass\"}" )
            .hasCode( Http.StatusCode.FORBIDDEN );
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/passwd" ), "{\"email\": \"" + DEFAULT_ORGANIZATION_ADMIN_EMAIL + "\", \"password\": \"newpass\"}" )
            .hasCode( Http.StatusCode.FORBIDDEN )
            .satisfies( response -> assertValidation( response ).hasErrors( "cannot manage " + DEFAULT_ORGANIZATION_ADMIN_EMAIL ) );
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/passwd" ), "{}" )
            .hasCode( Http.StatusCode.BAD_REQUEST )
            .satisfies( response -> assertValidation( response )
                .hasErrors(
                    "/password: required property is missing",
                    "/email: required property is missing"
                ) );
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/passwd" ), "{\"email\": \"" + email + "\", \"password\": \"newpass\"}" )
            .hasCode( OK );
        accountFixture.assertLogout();
        accountFixture.assertLogin( email, "newpass" );
        accountFixture.assertLogout();
        accountFixture.assertOrgAdminLogin();
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/passwd" ), "{\"email\": \"" + email + "\", \"password\": \"forcedpass\"}" )
            .hasCode( OK );
        accountFixture.assertLogout();
        accountFixture.assertLogin( email, "forcedpass" );
        accountFixture.assertLogout();
        accountFixture.assertAdminLogin();
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/passwd" ), "{\"email\": \"" + email + "\", \"password\": \"adminforcedpass\"}" )
            .hasCode( OK );
        accountFixture.assertLogout();
        accountFixture.assertLogin( email, "adminforcedpass" );
    }

    @Test
    public void ban() {
        final String email = "user@admin.com";
        var user = accountFixture.userStorage().store( new UserData( new User( email, "Joe", "Epstein", "pass123", true ), Map.of( DEFAULT_ORGANIZATION_ID, USER ) ) );
        accountFixture.assertLogin( email, "pass123" );
        accountFixture.assertLogout();
        accountFixture.assertOrgAdminLogin();
        assertGet( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/ban/" + user.user.email ) )
            .respondedJson( OK, "OK",
                contentOfTestResource( getClass(), "banned-user.json", Map.of(
                    "LAST_LOGIN", TODAY,
                    "BANNED", true
                ) ) );
        accountFixture.assertLogout();
        assertPost( accountFixture.httpUrl( "/auth/login" ), "{\"email\": \"" + user.user.email + "\", \"password\": \"pass\"}" )
            .hasCode( Http.StatusCode.UNAUTHORIZED );
        accountFixture.assertOrgAdminLogin();
        assertGet( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/unban/" + user.user.email ) )
            .respondedJson( OK, "OK",
                contentOfTestResource( getClass(), "banned-user.json", Map.of(
                    "LAST_LOGIN", TODAY,
                    "BANNED", false,
                    "API_KEY", user.user.apiKey
                ) ) );
        accountFixture.assertLogout();
        accountFixture.assertLogin( email, "pass123" );
        accountFixture.assertLogout();
    }

    @Test
    public void deleteUserByAdmin() {
        accountFixture.assertAdminLogin();
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users?role=ADMIN" ),
            contentOfTestResource( getClass(), "store-user-admin.json", Map.of() ), Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertThat( accountFixture.accounts().getUser( "newadmin@admins.com" ) ).isPresent();
        assertGet( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/delete/newadmin@admins.com" ) )
            .hasCode( OK );
        assertThat( accountFixture.accounts().getUser( "newadmin@admins.com" ) ).isNotPresent();
    }

    @Test
    public void addAccountToUserByAdmin() {
        accountFixture.userStorage().store( new UserData( REGULAR_USER, Map.of( DEFAULT_ORGANIZATION_ID, USER ) ) );
        final String userEmail = REGULAR_USER.email;
        accountFixture.assertSystemAdminLogin();
        final String account1 = "account1";
        assertPost( accountFixture.httpUrl( "/organizations/" + "testId" + "/users/" + userEmail + "/accounts/add?accountId=" + account1 ), Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertThat( accountFixture.accounts().getUser( userEmail ) )
            .isPresent()
            .get()
            .satisfies( u -> {
                assertThat( u.accounts.containsKey( "testId" ) ).isTrue();
                assertThat( u.accounts.get( "testId" ) ).contains( account1 );
            } );
    }

    @Test
    public void addAccountToUserByAdminWithLimitedRole() {
        accountFixture.userStorage().store( new UserData( REGULAR_USER, Map.of( DEFAULT_ORGANIZATION_ID, USER, "SYSTEM", USER ) ) );
        final String userEmail = REGULAR_USER.email;
        accountFixture.assertLogin( userEmail, DEFAULT_PASSWORD );
        final String account1 = "account1";
        assertPost( accountFixture.httpUrl( "/organizations/" + "testId" + "/users/" + userEmail + "/accounts/add?accountId=" + account1 ), Http.ContentType.APPLICATION_JSON )
            .hasCode( Http.StatusCode.FORBIDDEN );
    }

    @Test
    public void addAccountToUserByOrgAdmin() {
        accountFixture.userStorage().store( new UserData( REGULAR_USER, Map.of( DEFAULT_ORGANIZATION_ID, USER ) ) );
        final String userEmail = REGULAR_USER.email;
        accountFixture.assertOrgAdminLogin();
        final String account1 = "account1";
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/" + userEmail + "/accounts/add?accountId=" + account1 ), Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertThat( accountFixture.accounts().getUser( userEmail ) )
            .isPresent()
            .get()
            .satisfies( u -> {
                assertThat( u.accounts.containsKey( DEFAULT_ORGANIZATION_ID ) ).isTrue();
                assertThat( u.accounts.get( DEFAULT_ORGANIZATION_ID ) ).contains( account1 );
            } );
    }

    @Test
    public void accessToAllAccountsToUser() {
        accountFixture.userStorage().store( new UserData( REGULAR_USER, Map.of( DEFAULT_ORGANIZATION_ID, USER ) ) );
        final String userEmail = REGULAR_USER.email;
        accountFixture.assertOrgAdminLogin();
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/" + userEmail + "/accounts/add?accountId=" + "account1" ), Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/" + userEmail + "/accounts/add?accountId=" + "*" ), Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/" + userEmail + "/accounts/add?accountId=" + "account2" ), Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertThat( accountFixture.accounts().getUser( userEmail ) )
            .isPresent()
            .get()
            .satisfies( u -> {
                assertThat( u.accounts.containsKey( DEFAULT_ORGANIZATION_ID ) ).isTrue();
                assertThat( u.accounts.get( DEFAULT_ORGANIZATION_ID ) ).containsOnly( "account2" );
            } );
    }

    @Test
    public void addSeveralAccountsToUser() {
        accountFixture.userStorage().store( new UserData( REGULAR_USER, Map.of( DEFAULT_ORGANIZATION_ID, USER ) ) );
        final String userEmail = REGULAR_USER.email;
        accountFixture.assertOrgAdminLogin();
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/" + userEmail + "/accounts/add?accountId=" + "account1" ), Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/" + userEmail + "/accounts/add?accountId=" + "account2" ), Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertThat( accountFixture.accounts().getUser( userEmail ) )
            .isPresent()
            .get()
            .satisfies( u -> {
                assertThat( u.accounts.containsKey( DEFAULT_ORGANIZATION_ID ) ).isTrue();
                assertThat( u.accounts.get( DEFAULT_ORGANIZATION_ID ) ).containsOnly( "account1", "account2" );
            } );
    }

    @Test
    public void addSeveralDuplicatedAccountsToUser() {
        accountFixture.userStorage().store( new UserData( REGULAR_USER, Map.of( DEFAULT_ORGANIZATION_ID, USER ) ) );
        final String userEmail = REGULAR_USER.email;
        accountFixture.assertOrgAdminLogin();
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/" + userEmail + "/accounts/add?accountId=" + "account1" ), Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/" + userEmail + "/accounts/add?accountId=" + "account2" ), Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/" + userEmail + "/accounts/add?accountId=" + "account2" ), Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertThat( accountFixture.accounts().getUser( userEmail ) )
            .isPresent()
            .get()
            .satisfies( u -> {
                assertThat( u.accounts.containsKey( DEFAULT_ORGANIZATION_ID ) ).isTrue();
                assertThat( u.accounts.get( DEFAULT_ORGANIZATION_ID ) ).containsOnly( "account1", "account2" );
            } );
    }

    @Test
    public void refreshApiKey() {
        final UserData userData = accountFixture.userStorage().store( new UserData( REGULAR_USER, Map.of( DEFAULT_ORGANIZATION_ID, USER ) ) );
        final String apikeyCurrent = userData.user.apiKey;
        final String userEmail = REGULAR_USER.email;
        accountFixture.assertOrgAdminLogin();
        assertGet( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/apikey/" + userEmail ) )
            .hasCode( OK );
        final String newApikey = accountFixture.accounts().getUser( userEmail ).get().user.apiKey;
        assertThat( newApikey ).isNotEmpty();
        assertThat( apikeyCurrent ).isNotEqualTo( newApikey );
    }

    @Test
    public void refreshApiKeyByOneUserToAnother() {
        final UserData userData = accountFixture.userStorage().store( new UserData( new User( "newuser@gmail.com", "John", "Smith", "pass123", true ), Map.of( DEFAULT_ORGANIZATION_ID, USER ) ) );
        accountFixture.userStorage().store( new UserData( new User( "jga@test.com" ), Map.of( DEFAULT_ORGANIZATION_ID, USER ) ) );
        accountFixture.assertLogin( userData.user.email, "pass123" );
        assertGet( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/apikey/" + "jga@test.com" ) )
            .hasCode( FORBIDDEN );
    }

    @Test
    public void generateTfaAuthorizationLink() {
        final String email = "john@test.com";
        accountFixture.userStorage().store( new UserData( new User( email, "John", "Smith", "pass123", true ), Map.of( DEFAULT_ORGANIZATION_ID, USER ) ) );
        accountFixture.assertLogin( email, "pass123" );
        assertGet( accountFixture.httpUrl( "/organizations/users/tfa/" + email ) )
            .isOk().satisfies( response -> {
                byte[] decodedBytes = Base64.getDecoder().decode( response.content() );
                String decodedString = new String( decodedBytes );
                assertThat( decodedString.contains( "test.com" ) );
                assertThat( decodedString.contains( email ) );
                assertThat( decodedString.contains( "secretKey" ) );
            } );
    }

    @Test
    public void changeDefaultAccountUser() {
        OrganizationData org1 = accountFixture.accounts().storeOrganization( new Organization( "First", "test" ) );
        OrganizationData org2 = accountFixture.accounts().storeOrganization( new Organization( "Second", "test" ) );
        final String orgId = org1.organization.id;
        accountFixture.accounts().storeAccount( orgId, new Account( "acc1", "acc1" ) );
        accountFixture.accounts().storeAccount( orgId, new Account( "acc2", "acc2" ) );

        accountFixture.accounts().storeAccount( org2.organization.id, new Account( "acc3", "acc3" ) );
        accountFixture.accounts().storeAccount( org2.organization.id, new Account( "acc4", "acc4" ) );

        final String mail = "user@usr.com";
        UserData user = new UserData( new User( mail, "John", "Smith", "pass123", true ), Map.of( orgId, USER ) );
        user.addAccount( orgId, "acc1" );
        accountFixture.userStorage().store( user );
        assertEquals( "acc1", accountFixture.userStorage().getUser( mail ).get().getDefaultAccount( orgId ).get() );
        user.addAccount( orgId, "acc2" );
        assertEquals( "acc1", accountFixture.userStorage().getUser( mail ).get().getDefaultAccount( orgId ).get() );
        accountFixture.assertLogin( "user@usr.com", "pass123" );
        assertGet( accountFixture.httpUrl( "/organizations/" + orgId + "/users/" + mail + "/default-account/acc2" ) ).hasCode( OK );
        assertEquals( "acc2", accountFixture.userStorage().getUser( mail ).get().getDefaultAccount( orgId ).get() );
    }

    @Test
    public void setTheSameDefaultAccountToUser() {
        OrganizationData org1 = accountFixture.accounts().storeOrganization( new Organization( "First", "test" ) );
        final String orgId = org1.organization.id;
        accountFixture.accounts().storeAccount( orgId, new Account( "acc1", "acc1" ) );
        accountFixture.accounts().storeAccount( orgId, new Account( "acc2", "acc2" ) );

        final String mail = "user@usr.com";
        UserData user = new UserData( new User( mail, "John", "Smith", "pass123", true ), Map.of( orgId, USER ) );
        user.addAccount( orgId, "acc1" );
        accountFixture.userStorage().store( user );
        assertEquals( "acc1", accountFixture.userStorage().getUser( mail ).get().getDefaultAccount( orgId ).get() );
        user.addAccount( orgId, "acc2" );
        assertEquals( "acc1", accountFixture.userStorage().getUser( mail ).get().getDefaultAccount( orgId ).get() );
        accountFixture.assertLogin( "user@usr.com", "pass123" );
        assertGet( accountFixture.httpUrl( "/organizations/" + orgId + "/users/" + mail + "/default-account/acc2" ) ).hasCode( OK );
        assertGet( accountFixture.httpUrl( "/organizations/" + orgId + "/users/" + mail + "/default-account/acc2" ) ).hasCode( BAD_REQUEST );
        assertEquals( "acc2", accountFixture.userStorage().getUser( mail ).get().getDefaultAccount( orgId ).get() );
    }

    @Test
    public void setAccountToNonExistingUser() {
        OrganizationData org1 = accountFixture.accounts().storeOrganization( new Organization( "First", "test" ) );
        OrganizationData org2 = accountFixture.accounts().storeOrganization( new Organization( "Second", "test" ) );
        final String orgId = org1.organization.id;
        accountFixture.accounts().storeAccount( orgId, new Account( "acc1", "acc1" ) );
        accountFixture.accounts().storeAccount( orgId, new Account( "acc2", "acc2" ) );

        accountFixture.accounts().storeAccount( org2.organization.id, new Account( "acc3", "acc3" ) );
        accountFixture.accounts().storeAccount( org2.organization.id, new Account( "acc4", "acc4" ) );

        accountFixture.assertSystemAdminLogin();
        assertGet( accountFixture.httpUrl( "/organizations/" + orgId + "/users/non-exist@gmail.com/default-account/acc2" ) ).hasCode( NOT_FOUND );
    }

    @Test
    public void setNonExistingDefaultAccountToUser() {
        OrganizationData org1 = accountFixture.accounts().storeOrganization( new Organization( "First", "test" ) );
        final String orgId = org1.organization.id;
        accountFixture.accounts().storeAccount( orgId, new Account( "acc1", "acc1" ) );
        accountFixture.accounts().storeAccount( orgId, new Account( "acc2", "acc2" ) );

        final String mail = "user@usr.com";
        UserData user = new UserData( new User( mail, "John", "Smith", "pass123", true ), Map.of( orgId, ORGANIZATION_ADMIN ) );
        user.addAccount( orgId, "acc1" );
        accountFixture.userStorage().store( user );
        assertEquals( "acc1", accountFixture.userStorage().getUser( mail ).get().getDefaultAccount( orgId ).get() );
        user.addAccount( orgId, "acc2" );
        assertEquals( "acc1", accountFixture.userStorage().getUser( mail ).get().getDefaultAccount( orgId ).get() );
        accountFixture.assertLogin( "user@usr.com", "pass123" );
        assertGet( accountFixture.httpUrl( "/organizations/" + orgId + "/users/" + mail + "/default-account/acc2" ) ).hasCode( OK );
        assertGet( accountFixture.httpUrl( "/organizations/" + orgId + "/users/" + mail + "/default-account/acc3" ) ).hasCode( NOT_FOUND );
        assertEquals( "acc2", accountFixture.userStorage().getUser( mail ).get().getDefaultAccount( orgId ).get() );
    }

    @Test
    public void addOrganizationToUserBySystemAdmin() {
        OrganizationData org1 = accountFixture.accounts().storeOrganization( new Organization( "First", "test" ) );
        OrganizationData org2 = accountFixture.accounts().storeOrganization( new Organization( "Second", "test" ) );
        final String orgId = org1.organization.id;

        Map<String, String> roles = new HashMap<>();
        roles.put( orgId, USER );

        final String mail = "user@usr.com";
        UserData user = new UserData( new User( mail, "John", "Smith", "pass123", true ), roles );
        accountFixture.userStorage().store( user );

        accountFixture.assertSystemAdminLogin();
        assertGet( accountFixture.httpUrl( "/organizations/" + orgId + "/add?newOrganizationId=" + org2.organization.id + "&email=" + mail + "&role=ADMIN" ) ).hasCode( OK );
        assertTrue( accountFixture.userStorage().getUser( mail ).get().getRoles().containsKey( org2.organization.id ) );
    }

    @Test
    public void addOrganizationToUserByAdminInSeveralOrganizations() {
        OrganizationData org1 = accountFixture.accounts().storeOrganization( new Organization( "First", "test" ) );
        OrganizationData org2 = accountFixture.accounts().storeOrganization( new Organization( "Second", "test" ) );

        Map<String, String> adminRoles = new HashMap<>();
        adminRoles.put( org1.organization.id, ADMIN );
        adminRoles.put( org2.organization.id, ADMIN );

        final String adminMail = "orgadmin@usr.com";
        UserData admin = new UserData( new User( adminMail, "John", "Smith", "pass123", true ), adminRoles );

        final String userMail = "user@usr.com";
        Map<String, String> roles = new HashMap<>();
        roles.put( org1.organization.id, USER );
        UserData user = new UserData( new User( userMail, "John", "Smith", "pass", true ), roles );

        accountFixture.userStorage().store( admin );
        accountFixture.userStorage().store( user );

        accountFixture.assertLogin( adminMail, "pass123" );

        assertGet( accountFixture.httpUrl( "/organizations/" + org2.organization.id + "/add?newOrganizationId=" + org2.organization.id + "&email=" + userMail + "&role=ADMIN" ) ).hasCode( OK );
        assertTrue( accountFixture.userStorage().getUser( userMail ).get().getRoles().containsKey( org2.organization.id ) );
    }

    @Test
    public void addOrganizationToUserByUserWithDifferentRolesInOrganizations() {
        OrganizationData org1 = accountFixture.accounts().storeOrganization( new Organization( "First", "test" ) );
        OrganizationData org2 = accountFixture.accounts().storeOrganization( new Organization( "Second", "test" ) );

        Map<String, String> adminRoles = new HashMap<>();
        adminRoles.put( org1.organization.id, ADMIN );
        adminRoles.put( org2.organization.id, USER );

        final String adminMail = "orgadmin@usr.com";
        UserData admin = new UserData( new User( adminMail, "John", "Smith", "pass123", true ), adminRoles );

        final String userMail = "user@usr.com";
        Map<String, String> roles = new HashMap<>();
        roles.put( org1.organization.id, USER );
        UserData user = new UserData( new User( userMail, "John", "Smith", "pass", true ), roles );

        accountFixture.userStorage().store( admin );
        accountFixture.userStorage().store( user );

        accountFixture.assertLogin( adminMail, "pass123" );

        assertGet( accountFixture.httpUrl( "/organizations/" + org2.organization.id + "/add?newOrganizationId=" + org2.organization.id + "&email=" + userMail + "&role=ADMIN" ) ).hasCode( FORBIDDEN );
    }
}
