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
import oap.http.ContentTypes;
import oap.http.HttpStatusCodes;
import oap.http.server.nio.HttpServerExchange;
import oap.testng.Fixtures;
import org.testng.annotations.Test;

import java.util.List;

import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.assertPost;
import static oap.http.testng.HttpAsserts.httpUrl;
import static oap.io.Resources.urlOrThrow;
import static oap.ws.validate.ValidationErrors.empty;
import static oap.ws.validate.ValidationErrors.error;
import static oap.ws.validate.ValidationErrors.errors;

public class MethodValidatorPeerMethodTest extends Fixtures {
    {
        fixture( new KernelFixture( urlOrThrow( getClass(), "/application.test.conf" ) ) );
    }

    @Test
    public void validationDefault() {
        assertPost( httpUrl( "/mvpm/run/validation/default" ), "test", ContentTypes.TEXT_PLAIN )
            .responded( HttpStatusCodes.OK, "OK", ContentTypes.TEXT_PLAIN, "test" );
    }

    @Test
    public void validationOk() {
        assertPost( httpUrl( "/mvpm/run/validation/ok" ), "test", ContentTypes.TEXT_PLAIN )
            .responded( HttpStatusCodes.OK, "OK", ContentTypes.TEXT_PLAIN, "test" );
    }

    @Test
    public void validationFail() {
        assertPost( httpUrl( "/mvpm/run/validation/fail" ), "test", ContentTypes.TEXT_PLAIN )
            .respondedJson( HttpStatusCodes.BAD_REQUEST, "validation failed", "{\"errors\":[\"error1\",\"error2\"]}" );
    }

    @Test
    public void validationFailCode() {
        assertPost( httpUrl( "/mvpm/run/validation/fail-code" ), "test", ContentTypes.TEXT_PLAIN )
            .respondedJson( HttpStatusCodes.FORBIDDEN, "validation failed", "{\"errors\":[\"denied\"]}" );
    }

    @Test
    public void validationMethods() {
        assertGet( httpUrl( "/mvpm/run/validation/methods?a=a&b=5&c=c" ) )
            .respondedJson( HttpStatusCodes.BAD_REQUEST, "validation failed", "{\"errors\":[\"a\",\"a5\",\"5a\"]}" );
    }

    public static class TestWS {

        public void validationDefault( String request, HttpServerExchange exchange ) {
            exchange.responseOk( request, true, ContentTypes.TEXT_PLAIN );
        }

        @WsValidate( "validateOk" )
        public String validationOk( String request ) {
            return request;
        }

        @WsValidate( "validateFail" )
        public Object validationFail( String request ) {
            return null;
        }

        @WsValidate( "validateFailCode" )
        public Object validationFailCode( String request ) {
            return null;
        }

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
            return error( HttpStatusCodes.FORBIDDEN, "denied" );
        }
    }
}
