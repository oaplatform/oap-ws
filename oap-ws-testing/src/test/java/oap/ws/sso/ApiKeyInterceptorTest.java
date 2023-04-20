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

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static oap.http.Http.ContentType.TEXT_PLAIN;
import static oap.http.Http.StatusCode.CONFLICT;
import static oap.http.Http.StatusCode.OK;
import static oap.http.Http.StatusCode.UNAUTHORIZED;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.httpUrl;
import static oap.util.Pair.__;
import static oap.ws.sso.testng.SecureWSFixture.assertLogin;

public class ApiKeyInterceptorTest extends IntegratedTest {

    private TestUser user;

    @BeforeMethod
    public void beforeMethod() {
        user = userProvider().addUser( "admin@admin.com", "pass", __( "r1", "ADMIN" ) );
    }

    @Test
    public void allowed() {
        assertGet( httpUrl( "/secure/r1" ),
            __( "accessKey", user.getAccessKey() ),
            __( "apiKey", user.apiKey )
        ).responded( OK, "OK", TEXT_PLAIN, "admin@admin.com" );

        assertGet( httpUrl( "/secure/r1" ),
            __( "accessKey", "bla-bla" ),
            __( "apiKey", user.apiKey )
        ).hasCode( UNAUTHORIZED );

        assertGet( httpUrl( "/secure/r1" ),
            __( "accessKey", user.getAccessKey() ),
            __( "apiKey", "bla" )
        ).hasCode( UNAUTHORIZED );

        assertGet( httpUrl( "/secure/r1" ) )
            .hasCode( UNAUTHORIZED );

        assertGet( httpUrl( "/secure/r1" ),
            __( "accessKey", user.getAccessKey() ),
            __( "apiKey", user.apiKey )
        ).responded( OK, "OK", TEXT_PLAIN, "admin@admin.com" );
    }

    @Test
    public void conflict() {
        assertLogin( "admin@admin.com", "pass" );
        assertGet( httpUrl( "/secure/r1" ) )
            .hasCode( OK );
        assertGet( httpUrl( "/secure/r1" ),
            __( "accessKey", user.getAccessKey() ),
            __( "apiKey", user.apiKey )
        ).hasCode( CONFLICT );
    }
}
