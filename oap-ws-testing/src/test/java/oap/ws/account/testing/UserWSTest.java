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
import oap.ws.account.Organization;
import oap.ws.account.OrganizationData;
import oap.ws.account.User;
import oap.ws.account.UserData;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Random;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.io.content.ContentReader.ofString;
import static oap.testng.Asserts.contentOfTestResource;
import static oap.ws.account.Roles.USER;
import static oap.ws.account.testing.AccountFixture.DEFAULT_ADMIN_EMAIL;
import static oap.ws.account.testing.AccountFixture.DEFAULT_ORGANIZATION_ADMIN_EMAIL;
import static oap.ws.account.testing.AccountFixture.DEFAULT_ORGANIZATION_ID;
import static oap.ws.account.testing.OrganizationWSTest.TODAY;

public class UserWSTest extends Fixtures {
    protected final AccountFixture accountFixture;
    private byte[] randomBytes = new byte[] {
        0x12, 0x21, 0x12, 0x32, 0x42,
        0x59, 0x13, 0x22, 0x12, 0x38,
        0x70, 0x62, 0x14, 0x23, 0x12,
        0x05, 0x12, 0x72, 0x15, 0x24,
    };

    public UserWSTest() {
        fixture( new TestDirectoryFixture() );
        fixture( new MongoFixture() );
        accountFixture = fixture( new AccountFixture() );
        User.random = new Random() {
            @Override
            public void nextBytes( byte[] bytes ) {
                System.arraycopy( randomBytes, 0, bytes, 0, 20 );
            }
        };
    }

    @AfterMethod
    public void afterMethod() {
        accountFixture.assertLogout();
    }

    @Test
    public void current() {
        assertGet( accountFixture.httpUrl( "/user/current" ) )
            .hasCode( Http.StatusCode.UNAUTHORIZED );
        accountFixture.assertAdminLogin();
        UserData admin = accountFixture.userStorage().get( DEFAULT_ADMIN_EMAIL ).orElseThrow();
        assertGet( accountFixture.httpUrl( "/user/current" ) )
            .respondedJson( contentOfTestResource( getClass(), "current.json", Map.of(
                "LAST_LOGIN", TODAY,
                "ACCESS_KEY", admin.user.getAccessKey(),
                "API_KEY", admin.user.apiKey,
                "SECRET_KEY", admin.user.secretKey
            ) ) );
    }

    @Test
    public void get() {
        accountFixture.assertAdminLogin();
        UserData organizationAdmin = accountFixture.userStorage().get( DEFAULT_ORGANIZATION_ADMIN_EMAIL ).orElseThrow();
        assertGet( accountFixture.httpUrl( "/user/" + DEFAULT_ORGANIZATION_ID + "/" + organizationAdmin.user.email ) )
            .respondedJson( contentOfTestResource( getClass(), "org-admin-never-logged-in.json", Map.of(
                "ACCESS_KEY", organizationAdmin.getAccessKey(),
                "API_KEY", organizationAdmin.user.apiKey,
                "SECRET_KEY", organizationAdmin.user.secretKey
            ) ) );
        accountFixture.assertLogout();
        accountFixture.assertOrgAdminLogin();
        assertGet( accountFixture.httpUrl( "/user/" + DEFAULT_ORGANIZATION_ID + "/" + organizationAdmin.user.email ) )
            .respondedJson( contentOfTestResource( getClass(), "org-admin.json", Map.of(
                "LAST_LOGIN", TODAY,
                "ACCESS_KEY", organizationAdmin.getAccessKey(),
                "API_KEY", organizationAdmin.user.apiKey,
                "SECRET_KEY", organizationAdmin.user.secretKey
            ) ) );
        accountFixture.assertLogout();
    }

    @Test
    public void noSecureData() {
        UserData user = accountFixture.accounts().createUser( new User( "user@user.com", "Johnny", "Walker",
            "pass", true ), Map.of( DEFAULT_ORGANIZATION_ID, USER ) );
        accountFixture.assertOrgAdminLogin();
        assertGet( accountFixture.httpUrl( "/user/" + DEFAULT_ORGANIZATION_ID + "/" + user.user.email ) )
            .respondedJson( contentOfTestResource( getClass(), "user.json", ofString() ) );
        accountFixture.assertLogout();
    }

    @Test
    public void accessOtherOrgUser() {
        final OrganizationData organizationData = accountFixture.accounts().storeOrganization( new Organization( "THRRG", "otherOrg" ) );
        UserData user = accountFixture.accounts().createUser( new User( "other@other.com", "Other", "User",
            "pass", false ), Map.of( organizationData.organization.id, USER ) );

        accountFixture.assertOrgAdminLogin();
        assertGet( accountFixture.httpUrl( "/user/" + DEFAULT_ORGANIZATION_ID + "/" + user.user.email ) )
            .respondedJson( HTTP_NOT_FOUND, "validation failed", "{\"errors\":[\"not found other@other.com\"]}" );
        accountFixture.assertLogout();
    }
}
