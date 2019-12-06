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

import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.nio.charset.StandardCharsets.UTF_8;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.httpUrl;
import static oap.util.Pair.__;
import static oap.ws.sso.Roles.ADMIN;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;

public class ApiKeyInterceptorTest extends IntegratedTest {
    @Test
    public void allowed() {
        var user = userProvider().addUser( "admin@admin.com", "pass", ADMIN );

        assertGet( httpUrl( "/secure" ),
            __( "accessKey", user.getAccessKey() ),
            __( "apiKey", user.apiKey )
        ).responded( HTTP_OK, "OK", TEXT_PLAIN.withCharset( UTF_8 ), "admin@admin.com" );

        assertGet( httpUrl( "/secure" ),
            __( "accessKey", "bla-bla" ),
            __( "apiKey", user.apiKey )
        ).hasCode( HTTP_UNAUTHORIZED );

        assertGet( httpUrl( "/secure" ),
            __( "accessKey", user.getAccessKey() ),
            __( "apiKey", "bla" )
        ).hasCode( HTTP_UNAUTHORIZED );

        assertGet( httpUrl( "/secure" ) )
            .hasCode( HTTP_UNAUTHORIZED );

        assertGet( httpUrl( "/secure" ),
            __( "accessKey", user.getAccessKey() ),
            __( "apiKey", user.apiKey )
        ).responded( HTTP_OK, "OK", TEXT_PLAIN.withCharset( UTF_8 ), "admin@admin.com" );

        assertLogin( user.getEmail(), user.getPassword() );
        assertGet( httpUrl( "/secure" ) )
            .hasCode( HTTP_OK );
        assertGet( httpUrl( "/secure" ),
            __( "accessKey", user.getAccessKey() ),
            __( "apiKey", user.apiKey )
        ).hasCode( HTTP_CONFLICT );
    }
}
