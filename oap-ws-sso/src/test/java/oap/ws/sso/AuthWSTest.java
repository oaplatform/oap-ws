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

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.httpUrl;

public class AuthWSTest extends IntegratedTest {

    @Test
    public void login() {
        userProvider().addUser( new TestUser( "admin@admin.com", "pass", Roles.ADMIN ) );
        assertLogin( "admin@admin.com", "pass" );
        assertGet( httpUrl( "/auth/current" ) )
            .respondedJson( HTTP_OK, "OK", "{\"email\":\"admin@admin.com\", \"role\":\"ADMIN\"}" );
    }

    @Test
    public void logout() {
        userProvider().addUser( new TestUser( "admin@admin.com", "pass", Roles.ADMIN ) );
        userProvider().addUser( new TestUser( "user@admin.com", "pass", Roles.USER ) );
        assertLogin( "admin@admin.com", "pass" );
        assertLogout();
        assertGet( httpUrl( "/auth/current" ) )
            .hasCode( HTTP_NOT_FOUND );
        assertLogin( "user@admin.com", "pass" );
        assertGet( httpUrl( "/auth/current" ) )
            .respondedJson( HTTP_OK, "OK", "{\"email\":\"user@admin.com\", \"role\":\"USER\"}" );
    }
}
