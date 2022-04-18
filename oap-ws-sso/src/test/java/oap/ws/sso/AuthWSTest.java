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


import oap.http.ContentTypes;
import oap.http.HttpStatusCodes;
import oap.ws.sso.interceptor.ThrottleLoginInterceptor;
import oap.ws.sso.utils.TOTPUtils;
import org.assertj.core.api.Condition;
import org.testng.annotations.Test;

import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.getTestHttpPort;
import static oap.http.testng.HttpAsserts.httpUrl;
import static oap.ws.sso.Roles.ADMIN;
import static oap.ws.sso.Roles.USER;
import static oap.ws.sso.testng.SecureWSFixture.assertLogin;
import static oap.ws.sso.testng.SecureWSFixture.assertLogout;
import static oap.ws.sso.testng.SecureWSFixture.assertTfaRequiredLogin;
import static org.assertj.core.api.Assertions.assertThat;

public class AuthWSTest extends IntegratedTest {

    @Test
    public void login() {
        userProvider().addUser( new TestUser( "admin@admin.com", "pass", ADMIN ) );
        assertLogin( "admin@admin.com", "pass" );
        assertGet( httpUrl( "/auth/whoami" ) )
            .respondedJson( "{\"email\":\"admin@admin.com\", \"role\":\"ADMIN\"}" );
    }

    @Test
    public void loginTfaRequired() {
        kernelFixture.service( "oap-ws-sso-api", ThrottleLoginInterceptor.class ).delay = -1;
        String secretKey = TOTPUtils.generateSecretKey();
        userProvider().addUser( new TestUser( "admin@admin.com", "pass", ADMIN, true, secretKey ) );
        assertTfaRequiredLogin( "admin@admin.com", "pass", getTestHttpPort().orElse( 80 ) );
        assertGet( httpUrl( "/auth/whoami" ) )
            .hasCode( HttpStatusCodes.UNAUTHORIZED );
    }

    @Test
    public void loginTfa() {
        kernelFixture.service( "oap-ws-sso-api", ThrottleLoginInterceptor.class ).delay = -1;
        String secretKey = TOTPUtils.generateSecretKey();
        userProvider().addUser( new TestUser( "admin@admin.com", "pass", ADMIN, true, secretKey ) );
        assertTfaRequiredLogin( "admin@admin.com", "pass", getTestHttpPort().orElse( 80 ) );
        assertLogin( "admin@admin.com", "pass",
            TOTPUtils.getTOTPCode( secretKey ), getTestHttpPort().orElse( 80 ) );
        assertGet( httpUrl( "/auth/whoami" ) )
            .respondedJson( "{\"email\":\"admin@admin.com\", \"role\":\"ADMIN\"}" );
    }

    @Test
    public void loginTfaUpdatesLoginDate() {
        kernelFixture.service( "oap-ws-sso-api", ThrottleLoginInterceptor.class ).delay = -1;
        String secretKey = TOTPUtils.generateSecretKey();
        userProvider().addUser( new TestUser( "admin@admin.com", "pass", ADMIN, true, secretKey ) );
        assertTfaRequiredLogin( "admin@admin.com", "pass", getTestHttpPort().orElse( 80 ) );
        assertThat( userProvider().getUser( "admin@admin.com" ).get() )
            .is( new Condition<>( testUser -> testUser.loginDate == null, "Should not have loginDate" ) );
        assertLogin( "admin@admin.com", "pass",
            TOTPUtils.getTOTPCode( secretKey ), getTestHttpPort().orElse( 80 ) );
        assertThat( userProvider().getUser( "admin@admin.com" ).get() )
            .is( new Condition<>( testUser -> testUser.loginDate != null, "Should have loginDate" ) );
        assertGet( httpUrl( "/auth/whoami" ) )
            .respondedJson( "{\"email\":\"admin@admin.com\", \"role\":\"ADMIN\"}" );
    }

    @Test
    public void logout() {
        kernelFixture.service( "oap-ws-sso-api", ThrottleLoginInterceptor.class ).delay = -1;

        userProvider().addUser( new TestUser( "admin@admin.com", "pass", ADMIN ) );
        userProvider().addUser( new TestUser( "user@admin.com", "pass", USER ) );
        assertLogin( "admin@admin.com", "pass" );
        assertLogout();
        assertGet( httpUrl( "/auth/whoami" ) )
            .hasCode( HttpStatusCodes.UNAUTHORIZED );
        assertLogin( "user@admin.com", "pass" );
        assertGet( httpUrl( "/auth/whoami" ) )
            .respondedJson( "{\"email\":\"user@admin.com\", \"role\":\"USER\"}" );
    }

    @Test
    public void relogin() {
        userProvider().addUser( new TestUser( "admin@admin.com", "pass", ADMIN ) );
        userProvider().addUser( new TestUser( "user@user.com", "pass", USER ) );
        assertLogin( "admin@admin.com", "pass" );
        assertGet( httpUrl( "/secure" ) )
            .responded( HttpStatusCodes.OK, "OK", ContentTypes.TEXT_PLAIN, "admin@admin.com" );
        assertLogin( "user@user.com", "pass" );
        assertGet( httpUrl( "/secure" ) )
            .hasCode( HttpStatusCodes.FORBIDDEN );
    }
}
