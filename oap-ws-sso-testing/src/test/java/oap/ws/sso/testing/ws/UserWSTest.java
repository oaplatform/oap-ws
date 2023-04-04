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

package oap.ws.sso.testing.ws;

import oap.ws.sso.testing.AccountFixture;
import oap.http.Http;
import oap.storage.mongo.memory.MongoFixture;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import oap.ws.sso.model.Organization;
import oap.ws.sso.model.OrganizationData;
import oap.ws.sso.model.UserData;
import oap.ws.sso.model.UserInfo;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.Map;

import static oap.ws.sso.testing.AccountFixture.DEFAULT_ADMIN_EMAIL;
import static oap.ws.sso.testing.AccountFixture.DEFAULT_ORGANIZATION_ADMIN_EMAIL;
import static oap.ws.sso.testing.AccountFixture.DEFAULT_ORGANIZATION_ID;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.io.content.ContentReader.ofString;
import static oap.testng.Asserts.contentOfTestResource;
import static oap.ws.sso.Roles.USER;
import static oap.ws.sso.testing.ws.OrganizationWSTest.TODAY;

public class UserWSTest extends Fixtures {
    protected final AccountFixture accountFixture;

    public UserWSTest() {
        fixture( new TestDirectoryFixture() );
        fixture( new MongoFixture() );
        accountFixture = fixture( new AccountFixture() );
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
                "API_KEY", admin.user.apiKey
            ) ) );
    }

    @Test
    public void get() {
        accountFixture.assertAdminLogin();
        UserData organizationAdmin = accountFixture.userStorage().get( DEFAULT_ORGANIZATION_ADMIN_EMAIL ).orElseThrow();
        assertGet( accountFixture.httpUrl( "/user/" + DEFAULT_ORGANIZATION_ID + "/" + organizationAdmin.user.email ) )
            .respondedJson( contentOfTestResource( getClass(), "org-admin-never-logged-in.json", Map.of(
                "ACCESS_KEY", organizationAdmin.getAccessKey(),
                "API_KEY", organizationAdmin.user.apiKey
            ) ) );
        accountFixture.assertLogout();
        accountFixture.assertOrgAdminLogin();
        assertGet( accountFixture.httpUrl( "/user/" + DEFAULT_ORGANIZATION_ID + "/" + organizationAdmin.user.email ) )
            .respondedJson( contentOfTestResource( getClass(), "org-admin.json", Map.of(
                "LAST_LOGIN", TODAY,
                "ACCESS_KEY", organizationAdmin.getAccessKey(),
                "API_KEY", organizationAdmin.user.apiKey
            ) ) );
        accountFixture.assertLogout();
    }

    @Test
    public void noSecureData() {
        UserData organizationAdmin = accountFixture.userStorage().get( DEFAULT_ORGANIZATION_ADMIN_EMAIL ).orElseThrow();
        UserData user = accountFixture.accounts().createUser( new UserInfo( "user@user.com", "Johnny", "Walker",
            "pass", true ), Map.of( DEFAULT_ORGANIZATION_ID, USER ) );
        accountFixture.assertOrgAdminLogin();
        assertGet( accountFixture.httpUrl( "/user/" + DEFAULT_ORGANIZATION_ID + "/" + user.user.email ) )
            .respondedJson( contentOfTestResource( getClass(), "user.json", ofString() ) );
        accountFixture.assertLogout();
    }

    @Test
    public void accessOtherOrgUser() {
        final OrganizationData organizationData = accountFixture.accounts().storeOrganization( new Organization( "THRRG", "otherOrg" ) );
        UserData user = accountFixture.accounts().createUser( new UserInfo( "other@other.com", "Other", "User",
            "pass", false ), Map.of( organizationData.organization.id, USER ) );

        accountFixture.assertOrgAdminLogin();
        assertGet( accountFixture.httpUrl( "/user/" + DEFAULT_ORGANIZATION_ID + "/" + user.user.email ) )
            .respondedJson( HTTP_NOT_FOUND, "validation failed", "{\"errors\":[\"not found other@other.com\"]}" );
        accountFixture.assertLogout();
    }
}
