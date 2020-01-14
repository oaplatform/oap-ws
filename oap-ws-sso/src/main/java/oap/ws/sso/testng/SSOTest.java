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

package oap.ws.sso.testng;

import oap.application.testng.KernelFixture;
import oap.http.Cookie;
import oap.testng.Fixtures;
import org.joda.time.DateTime;

import java.nio.file.Path;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.httpUrl;
import static oap.ws.sso.SSO.AUTHENTICATION_KEY;

public class SSOTest extends Fixtures {
    protected KernelFixture kernelFixture;

    public SSOTest( Path conf ) {
        fixture( kernelFixture = new KernelFixture( conf ) );
    }

    public SSOTest( Path conf, String confd ) {
        fixture( kernelFixture = new KernelFixture( conf, confd ) );
    }

    protected static void assertLogin( String login, String password ) {
        assertGet( httpUrl( "/auth/login?email=" + login + "&password=" + password ) )
            .hasCode( HTTP_OK )
            .is( response -> {
                System.out.println( response.headers );
            } );
    }

    protected static void assertLogout() {
        assertGet( httpUrl( "/auth/logout" ) )
            .hasCode( HTTP_NO_CONTENT )
            .containsCookie( new Cookie( AUTHENTICATION_KEY, "<logged out>" )
                .withExpires( new DateTime( 1970, 1, 1, 1, 1 ) ) );
    }
}
