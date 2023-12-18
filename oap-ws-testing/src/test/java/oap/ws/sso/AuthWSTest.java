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


import oap.http.Http;
import oap.json.Binder;
import oap.ws.sso.interceptor.ThrottleLoginInterceptor;
import org.testng.annotations.Test;

import java.util.Map;

import static oap.http.Http.StatusCode.FORBIDDEN;
import static oap.http.Http.StatusCode.OK;
import static oap.http.Http.StatusCode.UNAUTHORIZED;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.assertPost;
import static oap.http.testng.HttpAsserts.getTestHttpPort;
import static oap.http.testng.HttpAsserts.httpUrl;
import static oap.util.Pair.__;
import static oap.ws.account.testing.SecureWSFixture.assertLogin;
import static oap.ws.account.testing.SecureWSFixture.assertLoginWithFBToken;
import static oap.ws.account.testing.SecureWSFixture.assertLoginWithFBTokenWithTfa;
import static oap.ws.account.testing.SecureWSFixture.assertLoginWithFBTokenWithTfaRequired;
import static oap.ws.account.testing.SecureWSFixture.assertLoginWithFBTokenWithWrongTfa;
import static oap.ws.account.testing.SecureWSFixture.assertLogout;
import static oap.ws.account.testing.SecureWSFixture.assertSwitchOrganization;
import static oap.ws.account.testing.SecureWSFixture.assertTfaRequiredLogin;
import static oap.ws.account.testing.SecureWSFixture.assertWrongTfaLogin;
import static org.testng.AssertJUnit.assertTrue;

public class AuthWSTest extends IntegratedTest {
    @Test
    public void loginWhoami() {
        userProvider().addUser( new TestUser( "admin@admin.com", "pass", __( "r1", "ADMIN" ) ) );
        assertLogin( "admin@admin.com", "pass" );
        assertGet( httpUrl( "/auth/whoami" ) )
            .respondedJson( "{\"email\":\"admin@admin.com\"}" );
    }

    @Test
    public void loginResponseTest() {
        userProvider().addUser( new TestUser( "admin@admin.com", "pass", __( "r1", "ADMIN" ) ) );
        assertPost( httpUrl( "/auth/login" ), "{ \"email\":\"admin@admin.com\",\"password\": \"pass\"}" )
            .hasCode( Http.StatusCode.OK ).satisfies( resp -> {
                Map<String, String> response = Binder.json.unmarshal( Map.class, resp.contentString() );
                assertTrue( response.containsKey( "accessToken" ) );
                assertTrue( response.containsKey( "refreshToken" ) );
            } );
    }

    @Test
    public void loginMfaRequired() {
        kernelFixture.service( "oap-ws-sso-api", ThrottleLoginInterceptor.class ).delay = -1;
        userProvider().addUser( new TestUser( "admin@admin.com", "pass", __( "r1", "ADMIN" ), true ) );
        assertTfaRequiredLogin( "admin@admin.com", "pass", getTestHttpPort().orElse( 80 ) );
        assertGet( httpUrl( "/auth/whoami" ) )
            .hasCode( UNAUTHORIZED );
    }

    @Test
    public void loginMfa() {
        kernelFixture.service( "oap-ws-sso-api", ThrottleLoginInterceptor.class ).delay = -1;
        userProvider().addUser( new TestUser( "admin@admin.com", "pass", __( "r1", "ADMIN" ), true ) );
        assertTfaRequiredLogin( "admin@admin.com", "pass", getTestHttpPort().orElse( 80 ) );
        assertLogin( "admin@admin.com", "pass", "proper_code", getTestHttpPort().orElse( 80 ) );
        assertGet( httpUrl( "/auth/whoami" ) )
            .respondedJson( "{\"email\":\"admin@admin.com\"}" );
    }

    @Test
    public void loginMfaWrongCode() {
        kernelFixture.service( "oap-ws-sso-api", ThrottleLoginInterceptor.class ).delay = -1;
        userProvider().addUser( new TestUser( "admin@admin.com", "pass", __( "r1", "ADMIN" ), true ) );
        assertTfaRequiredLogin( "admin@admin.com", "pass", getTestHttpPort().orElse( 80 ) );
        assertWrongTfaLogin( "admin@admin.com", "pass", "wrong_code", getTestHttpPort().orElse( 80 ) );
        assertGet( httpUrl( "/auth/whoami" ) )
            .hasCode( UNAUTHORIZED );
    }

    @Test
    public void logout() {
        kernelFixture.service( "oap-ws-sso-api", ThrottleLoginInterceptor.class ).delay = -1;

        userProvider().addUser( new TestUser( "admin@admin.com", "pass", __( "r1", "ADMIN" ) ) );
        userProvider().addUser( new TestUser( "user@admin.com", "pass", __( "r1", "USER" ) ) );
        assertLogin( "admin@admin.com", "pass" );
        assertLogout();
        assertGet( httpUrl( "/auth/whoami" ) )
            .hasCode( UNAUTHORIZED );
        assertLogin( "user@admin.com", "pass" );
        assertGet( httpUrl( "/auth/whoami" ) )
            .respondedJson( "{\"email\":\"user@admin.com\"}" );
    }

    @Test
    public void loginAndTryToReachOrganization() {
        kernelFixture.service( "oap-ws-sso-api", ThrottleLoginInterceptor.class ).delay = -1;
        userProvider().addUser( new TestUser( "admin@admin.com", "pass", __( "r1", "ADMIN" ) ) );
        userProvider().addUser( new TestUser( "user@user.com", "pass", __( "r1", "USER" ) ) );
        assertLogin( "admin@admin.com", "pass" );
        assertGet( httpUrl( "/secure/r1" ) ).hasCode( OK );
        assertLogin( "admin@admin.com", "pass" );
        assertSwitchOrganization( "r1" );
        assertGet( httpUrl( "/secure/r1" ) )
            .hasCode( OK );
    }

    @Test
    public void loginThenUseSpecificOrganization() {
        kernelFixture.service( "oap-ws-sso-api", ThrottleLoginInterceptor.class ).delay = -1;
        userProvider().addUser( new TestUser( "admin@admin.com", "pass", Map.of( "r1", "ADMIN", "r2", "USER" ) ) );
        assertLogin( "admin@admin.com", "pass" );
        assertSwitchOrganization( "r2" );
    }

    @Test
    public void loginThenUseWrongOrganization() throws InterruptedException {
        userProvider().addUser( new TestUser( "admin@admin.com", "pass", Map.of( "r1", "ADMIN", "r2", "USER" ) ) );
        assertLogin( "admin@admin.com", "pass" );
        assertGet( httpUrl( "auth/switch/r3" ) ).hasCode( FORBIDDEN ).hasReason( "User doesn't belong to organization" );
        Thread.sleep( 5000L );
    }

    @Test
    public void loginWithExternalToken() {
        userProvider().addUser( new TestUser( "newuser@user.com", null, __( "r1", "USER" ) ) );
        assertLoginWithFBToken();
        assertGet( httpUrl( "/secure/r1" ) )
            .hasCode( FORBIDDEN );
        assertGet( httpUrl( "/auth/whoami" ) )
            .respondedJson( "{\"email\":\"newuser@user.com\"}" );
    }

    @Test
    public void loginWithExternalTokenWithTfa() {
        userProvider().addUser( new TestUser( "newuser@user.com", null, __( "r1", "USER" ), true ) );
        assertLoginWithFBTokenWithTfa();
        assertGet( httpUrl( "/secure/r1" ) )
            .hasCode( FORBIDDEN );
        assertGet( httpUrl( "/auth/whoami" ) )
            .respondedJson( "{\"email\":\"newuser@user.com\"}" );
    }

    @Test
    public void loginWithExternalTokenWithTfaRequired() {
        kernelFixture.service( "oap-ws-sso-api", ThrottleLoginInterceptor.class ).delay = -1;

        userProvider().addUser( new TestUser( "newuser@user.com", null, __( "r1", "USER" ), true ) );
        assertLoginWithFBTokenWithTfaRequired();
    }

    @Test
    public void loginWithExternalTokenWithWrongTfa() throws InterruptedException {
        kernelFixture.service( "oap-ws-sso-api", ThrottleLoginInterceptor.class ).delay = -1;

        userProvider().addUser( new TestUser( "newuser@user.com", null, __( "r1", "USER" ), true ) );
        assertLoginWithFBTokenWithWrongTfa();
    }
}
