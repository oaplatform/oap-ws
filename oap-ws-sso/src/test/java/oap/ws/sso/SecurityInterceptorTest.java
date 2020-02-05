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

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.nio.charset.StandardCharsets.UTF_8;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.httpUrl;
import static oap.ws.sso.Roles.ADMIN;
import static oap.ws.sso.Roles.USER;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;

public class SecurityInterceptorTest extends IntegratedTest {
    @Test
    public void allowed() {
        userProvider().addUser( "admin@admin.com", "pass", ADMIN );
        assertLogin( "{\n" +
            "  \"email\": \"admin@admin.com\",\n" +
            "  \"password\": \"pass\"\n" +
            "}" );
        assertGet( httpUrl( "/secure" ) )
            .responded( HTTP_OK, "OK", TEXT_PLAIN.withCharset( UTF_8 ), "admin@admin.com" );
        assertGet( httpUrl( "/secure" ) )
            .responded( HTTP_OK, "OK", TEXT_PLAIN.withCharset( UTF_8 ), "admin@admin.com" );
        assertGet( httpUrl( "/secure" ) )
            .responded( HTTP_OK, "OK", TEXT_PLAIN.withCharset( UTF_8 ), "admin@admin.com" );
    }

    @Test
    public void notLoggedIn() {
        assertGet( httpUrl( "/secure" ) )
            .hasCode( HTTP_UNAUTHORIZED );
    }

    @Test
    public void denied() {
        userProvider().addUser( "user@user.com", "pass", USER );
        assertLogin( "{\n" +
            "  \"email\": \"user@user.com\",\n" +
            "  \"password\": \"pass\"\n" +
            "}" );
        assertGet( httpUrl( "/secure" ) )
            .hasCode( HTTP_FORBIDDEN );
    }

}
