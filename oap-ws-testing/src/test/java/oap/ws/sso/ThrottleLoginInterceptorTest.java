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

import oap.http.testng.HttpAsserts;
import oap.util.Dates;
import org.joda.time.DateTimeUtils;
import org.testng.annotations.Test;

import static oap.http.Http.StatusCode.FORBIDDEN;
import static oap.http.Http.StatusCode.UNAUTHORIZED;
import static oap.http.testng.HttpAsserts.assertPost;
import static oap.http.testng.HttpAsserts.httpUrl;
import static oap.http.testng.HttpAsserts.reset;
import static oap.util.Pair.__;

public class ThrottleLoginInterceptorTest extends IntegratedTest {
    @Test
    public void deniedAccept() {
        Dates.setTimeFixed( DateTimeUtils.currentTimeMillis() );
        reset();
        userProvider().addUser( "test1@user.com", "pass1", __( "realm", "ADMIN" ) );

        login( "test1@user.com", "pass" ).hasCode( UNAUTHORIZED );
        login( "test1@user.com", "pass1" ).hasCode( FORBIDDEN ).hasReason( "Please wait 5s before next attempt" );
        Dates.incFixed( Dates.s( 7 ) );
        login( "test1@user.com", "pass1" ).isOk();
    }

    public HttpAsserts.HttpAssertion login( String login, String password ) {
        return assertPost( httpUrl( "/auth/login" ), "{  \"email\": \"" + login + "\",  \"password\": \"" + password + "\"}" );
    }
}
