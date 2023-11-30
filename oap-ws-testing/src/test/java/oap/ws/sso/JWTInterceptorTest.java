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

import org.testng.annotations.Test;

import static oap.http.Http.ContentType.TEXT_PLAIN;
import static oap.http.Http.StatusCode.FORBIDDEN;
import static oap.http.Http.StatusCode.OK;
import static oap.http.Http.StatusCode.UNAUTHORIZED;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.httpUrl;
import static oap.util.Pair.__;
import static oap.ws.account.testing.SecureWSFixture.assertLogin;
import static oap.ws.account.testing.SecureWSFixture.assertSwitchOrganization;

public class JWTInterceptorTest extends IntegratedTest {
    @Test
    public void allowed() {
        userProvider().addUser( "admin@admin.com", "pass", __( "r1", "ADMIN" ) );
        assertLogin( "admin@admin.com", "pass" );
        assertGet( httpUrl( "/secure/r1" ) )
            .responded( OK, "OK", TEXT_PLAIN, "admin@admin.com" );
        assertGet( httpUrl( "/secure/r1" ) )
            .responded( OK, "OK", TEXT_PLAIN, "admin@admin.com" );
        assertGet( httpUrl( "/secure/r1" ) )
            .responded( OK, "OK", TEXT_PLAIN, "admin@admin.com" );
    }

    @Test
    public void wrongRealm() {
        userProvider().addUser( "admin@admin.com", "pass", __( "r1", "ADMIN" ) );
        assertLogin( "admin@admin.com", "pass" );
        assertGet( httpUrl( "/secure/r2" ) )
            .hasCode( FORBIDDEN );

    }

    @Test
    public void wrongRealmWithOrganizationLoggedIn() throws InterruptedException {
        userProvider().addUser( "admin@admin.com", "pass", __( "r1", "ADMIN" ) );
        assertLogin( "admin@admin.com", "pass" );
        assertSwitchOrganization( "r1" );
        assertGet( httpUrl( "/secure/r2" ) )
            .hasCode( FORBIDDEN );

    }

    @Test
    public void notLoggedIn() {
        assertGet( httpUrl( "/secure/r1" ) )
            .hasCode( UNAUTHORIZED );
    }

    @Test
    public void denied() {
        userProvider().addUser( "user@user.com", "pass", __( "r1", "USER" ) );
        assertLogin( "user@user.com", "pass" );
        assertGet( httpUrl( "/secure/r1" ) )
            .hasCode( FORBIDDEN );
    }

}
