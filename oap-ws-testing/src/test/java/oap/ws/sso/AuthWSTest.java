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


import oap.ws.sso.interceptor.ThrottleLoginInterceptor;
import org.testng.annotations.Test;

import static oap.http.Http.ContentType.TEXT_PLAIN;
import static oap.http.Http.StatusCode.BAD_REQUEST;
import static oap.http.Http.StatusCode.FORBIDDEN;
import static oap.http.Http.StatusCode.OK;
import static oap.http.Http.StatusCode.UNAUTHORIZED;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.assertPost;
import static oap.http.testng.HttpAsserts.getTestHttpPort;
import static oap.http.testng.HttpAsserts.httpUrl;
import static oap.util.Pair.__;
import static oap.ws.sso.testng.SecureWSFixture.assertLogin;
import static oap.ws.sso.testng.SecureWSFixture.assertLogout;
import static oap.ws.sso.testng.SecureWSFixture.assertMfaRequiredLogin;

public class AuthWSTest extends IntegratedTest {
    @Test
    public void loginWhoami() {
        userProvider().addUser( new TestUser( "admin@admin.com", "pass", __( "r1", "ADMIN" ) ) );
        assertLogin( "admin@admin.com", "pass" );
        assertGet( httpUrl( "/auth/whoami" ) )
            .respondedJson( "{\"email\":\"admin@admin.com\"}" );
    }

    @Test
    public void loginMfaRequired() {
        kernelFixture.service( "oap-ws-sso-api", ThrottleLoginInterceptor.class ).delay = -1;
        userProvider().addUser( new TestUser( "admin@admin.com", "pass", __( "r1", "ADMIN" ), true ) );
        assertMfaRequiredLogin( "admin@admin.com", "pass", getTestHttpPort().orElse( 80 ) );
        assertGet( httpUrl( "/auth/whoami" ) )
            .hasCode( UNAUTHORIZED );
    }

    @Test
    public void loginMfa() {
        kernelFixture.service( "oap-ws-sso-api", ThrottleLoginInterceptor.class ).delay = -1;
        userProvider().addUser( new TestUser( "admin@admin.com", "pass", __( "r1", "ADMIN" ), true ) );
        assertMfaRequiredLogin( "admin@admin.com", "pass", getTestHttpPort().orElse( 80 ) );
        assertLogin( "admin@admin.com", "pass", "proper_code", getTestHttpPort().orElse( 80 ) );
        assertGet( httpUrl( "/auth/whoami" ) )
            .respondedJson( "{\"email\":\"admin@admin.com\"}" );
    }

    @Test
    public void loginMfaWrongCode() {
        kernelFixture.service( "oap-ws-sso-api", ThrottleLoginInterceptor.class ).delay = -1;
        userProvider().addUser( new TestUser( "admin@admin.com", "pass", __( "r1", "ADMIN" ), true ) );
        assertPost( httpUrl( getTestHttpPort().orElse( 80 ), "/auth/login" ),
            "{  \"email\": \"admin@admin.com\",  \"password\": \"pass\", \"mfaCode\": \"wrong_code\"}" )
            .hasCode( BAD_REQUEST )
            .hasReason( "MFA code is incorrect or required" );
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
    public void relogin() {
        userProvider().addUser( new TestUser( "admin@admin.com", "pass", __( "r1", "ADMIN" ) ) );
        userProvider().addUser( new TestUser( "user@user.com", "pass", __( "r1", "USER" ) ) );
        assertLogin( "admin@admin.com", "pass" );
        assertGet( httpUrl( "/secure/r1" ) )
            .responded( OK, "OK", TEXT_PLAIN, "admin@admin.com" );
        assertLogin( "user@user.com", "pass" );
        assertGet( httpUrl( "/secure/r1" ) )
            .hasCode( FORBIDDEN );
    }
}
