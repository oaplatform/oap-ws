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
package oap.ws.validate;

import oap.application.testng.KernelFixture;
import oap.http.HttpResponse;
import oap.testng.Fixtures;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import org.testng.annotations.Test;

import java.util.List;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_OK;
import static oap.http.ContentTypes.TEXT_PLAIN;
import static oap.http.Request.HttpMethod.GET;
import static oap.http.Request.HttpMethod.POST;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.assertPost;
import static oap.http.testng.HttpAsserts.httpUrl;
import static oap.ws.WsParam.From.BODY;
import static oap.ws.validate.ValidationErrors.empty;
import static oap.ws.validate.ValidationErrors.error;
import static oap.ws.validate.ValidationErrors.errors;

public class MethodValidatorPeerMethodTest extends Fixtures {
    {
        fixture( new KernelFixture( "/application.test.conf" ) );
    }

    @Test
    public void validationDefault() {
        assertPost( httpUrl( "/mvpm/run/validation/default" ), "test", TEXT_PLAIN )
            .responded( HTTP_OK, "OK", TEXT_PLAIN, "test" );
    }

    @Test
    public void validationOk() {
        assertPost( httpUrl( "/mvpm/run/validation/ok" ), "test", TEXT_PLAIN )
            .responded( HTTP_OK, "OK", TEXT_PLAIN, "test" );
    }

    @Test
    public void validationFail() {
        assertPost( httpUrl( "/mvpm/run/validation/fail" ), "test", TEXT_PLAIN )
            .respondedJson( HTTP_BAD_REQUEST, "validation failed", "{\"errors\":[\"error1\",\"error2\"]}" );
    }

    @Test
    public void validationFailCode() {
        assertPost( httpUrl( "/mvpm/run/validation/fail-code" ), "test", TEXT_PLAIN )
            .respondedJson( HTTP_FORBIDDEN, "validation failed", "{\"errors\":[\"denied\"]}" );
    }

    @Test
    public void validationMethods() {
        assertGet( httpUrl( "/mvpm/run/validation/methods?a=a&b=5&c=c" ) )
            .respondedJson( HTTP_BAD_REQUEST, "validation failed", "{\"errors\":[\"a\",\"a5\",\"5a\"]}" );
    }

    public static class TestWS {

        @WsMethod( path = "/run/validation/default", method = POST )
        public HttpResponse validationDefault( @WsParam( from = BODY ) String request ) {
            return HttpResponse.ok( request, true, TEXT_PLAIN ).response();
        }

        @WsMethod( path = "/run/validation/ok", method = POST, produces = "text/plain" )
        @WsValidate( "validateOk" )
        public String validationOk( @WsParam( from = BODY ) String request ) {
            return request;
        }

        @WsMethod( path = "/run/validation/fail", method = POST )
        @WsValidate( "validateFail" )
        public Object validationFail( @WsParam( from = BODY ) String request ) {
            return null;
        }

        @WsMethod( path = "/run/validation/fail-code", method = POST )
        @WsValidate( "validateFailCode" )
        public Object validationFailCode( @WsParam( from = BODY ) String request ) {
            return null;
        }

        @WsMethod( path = "/run/validation/methods", method = GET )
        @WsValidate( { "validateA", "validateAB", "validateBA" } )
        public String validationMethods( String a, int b, String c ) {
            return a + b + c;
        }

        protected ValidationErrors validateA( String a ) {
            return error( a );
        }

        protected ValidationErrors validateAB( String a, int b ) {
            return error( a + b );
        }

        protected ValidationErrors validateBA( int b, String a ) {
            return error( b + a );
        }

        protected ValidationErrors validateOk( String request ) {
            return empty();
        }

        protected ValidationErrors validateFail( String request ) {
            return errors( List.of( "error1", "error2" ) );
        }

        protected ValidationErrors validateFailCode( String request ) {
            return error( HTTP_FORBIDDEN, "denied" );
        }
    }
}
