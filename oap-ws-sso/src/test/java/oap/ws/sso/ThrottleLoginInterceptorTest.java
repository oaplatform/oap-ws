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

import oap.http.HttpStatusCodes;
import oap.http.testng.HttpAsserts;
import org.testng.annotations.Test;

import static oap.http.testng.HttpAsserts.assertPost;
import static oap.http.testng.HttpAsserts.httpUrl;
import static oap.ws.sso.Roles.ADMIN;

public class ThrottleLoginInterceptorTest extends IntegratedTest {
    @Test
    public void deniedAccept() throws Exception {
        userProvider().addUser( "test1@user.com", "pass1", ADMIN );

        login( "test1@user.com", "pass" ).hasCode( HttpStatusCodes.UNAUTHORIZED );
        login( "test1@user.com", "pass1" ).hasCode( HttpStatusCodes.FORBIDDEN ).hasReason( "Please wait 5 seconds before next attempt" );
        Thread.sleep( 1000 * 7 );
        login( "test1@user.com", "pass1" ).isOk();
    }


    public HttpAsserts.HttpAssertion login( String login, String password ) {
        return assertPost( httpUrl( "/auth/login" ), "{  \"email\": \"" + login + "\",  \"password\": \"" + password + "\"}" );
    }
}
