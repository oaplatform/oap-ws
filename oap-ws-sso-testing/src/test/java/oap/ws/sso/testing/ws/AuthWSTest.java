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


import oap.ws.sso.interceptor.ThrottleLoginInterceptor;
import oap.ws.sso.testing.AccountFixture;
import oap.ws.sso.testing.IntegratedTest;
import org.testng.annotations.Test;

import static oap.http.Http.ContentType.TEXT_PLAIN;
import static oap.http.Http.StatusCode.BAD_REQUEST;
import static oap.http.Http.StatusCode.FORBIDDEN;
import static oap.http.Http.StatusCode.OK;
import static oap.http.Http.StatusCode.UNAUTHORIZED;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.assertPost;
import static oap.http.testng.HttpAsserts.httpUrl;
import static oap.util.Pair.__;
import static oap.ws.sso.testng.SecureWSFixture.assertLogin;
import static oap.ws.sso.testng.SecureWSFixture.assertLogout;
import static oap.ws.sso.testng.SecureWSFixture.assertTfaRequiredLogin;

public class AuthWSTest extends IntegratedTest {
    @Test
    public void loginWhoami() {
        userProvider().addUser( new AccountFixture.TestUser( "admin@admin.com", "pass", __( "r1", "ADMIN" ) ) );
        assertLogin( "admin@admin.com", "pass", accountFixture.defaultHttpPort() );
        assertGet( httpUrl( accountFixture.defaultHttpPort(), "/auth/whoami" ) )
            .respondedJson( "{\"email\":\"admin@admin.com\"}" );
    }

    @Test
    public void loginTfaRequired() {
        accountFixture.service( "oap-ws-sso-api", ThrottleLoginInterceptor.class ).delay = -1;
        userProvider().addUser( new AccountFixture.TestUser( "admin@admin.com", "pass", __( "r1", "ADMIN" ), true ) );
        assertTfaRequiredLogin( "admin@admin.com", "pass", accountFixture.defaultHttpPort() );
        assertGet( httpUrl( accountFixture.defaultHttpPort(), "/auth/whoami" ) )
            .hasCode( UNAUTHORIZED );
    }

    @Test
    public void loginTfa() {
        accountFixture.service( "oap-ws-sso-api", ThrottleLoginInterceptor.class ).delay = -1;
        userProvider().addUser( new AccountFixture.TestUser( "admin@admin.com", "pass", __( "r1", "ADMIN" ), true ) );
        assertTfaRequiredLogin( "admin@admin.com", "pass", accountFixture.defaultHttpPort() );
        assertLogin( "admin@admin.com", "pass", "proper_code", accountFixture.defaultHttpPort() );
        assertGet( httpUrl( accountFixture.defaultHttpPort(), "/auth/whoami" ) )
            .respondedJson( "{\"email\":\"admin@admin.com\"}" );
    }

    @Test
    public void loginTfaWrongCode() {
        accountFixture.service( "oap-ws-sso-api", ThrottleLoginInterceptor.class ).delay = -1;
        userProvider().addUser( new AccountFixture.TestUser( "admin@admin.com", "pass", __( "r1", "ADMIN" ), true ) );
        assertPost( httpUrl( accountFixture.defaultHttpPort(), "/auth/login" ),
            "{  \"email\": \"admin@admin.com\",  \"password\": \"pass\", \"tfaCode\": \"wrong_code\"}" )
            .hasCode( BAD_REQUEST )
            .hasReason( "TFA code is incorrect or required" );
    }

    @Test
    public void logout() {
        accountFixture.service( "oap-ws-sso-api", ThrottleLoginInterceptor.class ).delay = -1;
        userProvider().addUser( new AccountFixture.TestUser( "admin@admin.com", "pass", __( "r1", "ADMIN" ) ) );
        userProvider().addUser( new AccountFixture.TestUser( "user@admin.com", "pass", __( "r1", "USER" ) ) );
        assertLogin( "admin@admin.com", "pass", accountFixture.defaultHttpPort() );
        assertLogout( accountFixture.defaultHttpPort() );
        assertGet( httpUrl( accountFixture.defaultHttpPort(), "/auth/whoami" ) )
            .hasCode( UNAUTHORIZED );
        assertLogin( "user@admin.com", "pass", accountFixture.defaultHttpPort() );
        assertGet( httpUrl( accountFixture.defaultHttpPort(), "/auth/whoami" ) )
            .respondedJson( "{\"email\":\"user@admin.com\"}" );
    }

    @Test
    public void relogin() {
        userProvider().addUser( new AccountFixture.TestUser( "admin@admin.com", "pass", __( "r1", "ADMIN" ) ) );
        userProvider().addUser( new AccountFixture.TestUser( "user@user.com", "pass", __( "r1", "USER" ) ) );
        assertLogin( "admin@admin.com", "pass", accountFixture.defaultHttpPort() );
        assertGet( httpUrl( accountFixture.defaultHttpPort(), "/secure/r1" ) )
            .responded( OK, "OK", TEXT_PLAIN, "admin@admin.com" );
        assertLogin( "user@user.com", "pass", accountFixture.defaultHttpPort() );
        assertGet( httpUrl( accountFixture.defaultHttpPort(), "/secure/r1" ) )
            .hasCode( FORBIDDEN );
    }
}
