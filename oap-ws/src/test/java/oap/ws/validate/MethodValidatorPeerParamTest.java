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
import oap.testng.Fixtures;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;

import static oap.http.testng.HttpAsserts.assertPost;
import static oap.http.testng.HttpAsserts.httpUrl;
import static oap.io.Resources.urlOrThrow;
import static oap.ws.validate.ValidationErrors.empty;
import static oap.ws.validate.ValidationErrors.error;

public class MethodValidatorPeerParamTest extends Fixtures {
    {
        fixture( new KernelFixture( urlOrThrow( getClass(), "/application.test.conf" ) ) );
    }

    @Test
    public void validationDefault() {
        assertPost( httpUrl( "/mvpp/run/validation/default?i=1" ), "test", ContentTypes.TEXT_PLAIN )
            .responded( HttpStatusCodes.OK, "OK", ContentTypes.APPLICATION_JSON, "\"1test\"" );
    }

    @Test
    public void validationOk() {
        assertPost( httpUrl( "/mvpp/run/validation/ok?i=1" ), "test", ContentTypes.TEXT_PLAIN )
            .responded( HttpStatusCodes.OK, "OK", ContentTypes.APPLICATION_JSON, "\"1test\"" );
    }

    @Test
    public void validationOkList() {
        assertPost( httpUrl( "/mvpp/run/validation/ok?i=1&listString=_11&listString=_12" ), "test", ContentTypes.TEXT_PLAIN )
            .responded( HttpStatusCodes.OK, "OK", ContentTypes.APPLICATION_JSON, "\"1_11/_12test\"" );
    }

    @Test
    public void validationOkOptional() {
        assertPost( httpUrl( "/mvpp/run/validation/ok?i=1&optString=2" ), "test", ContentTypes.TEXT_PLAIN )
            .responded( HttpStatusCodes.OK, "OK", ContentTypes.APPLICATION_JSON, "\"12test\"" );
    }

    @Test
    public void validationFail() {
        assertPost( httpUrl( "/mvpp/run/validation/fail?i=1" ), "test", ContentTypes.TEXT_PLAIN )
            .respondedJson( HttpStatusCodes.BAD_REQUEST, "validation failed", "{\"errors\": [\"error:1\", \"error:test\"]}" );
    }

    @Test
    public void validationRequiredFailed() {
        assertPost( httpUrl( "/mvpp/run/validation/ok" ), "test", ContentTypes.TEXT_PLAIN )
            .respondedJson( HttpStatusCodes.BAD_REQUEST, "i is required", "{\"errors\": [\"i is required\"]}" );
    }

    @Test
    public void validationTypeFailed() {
        assertPost( httpUrl( "/mvpp/run/validation/ok?i=test" ), "test", ContentTypes.TEXT_PLAIN )
            .hasCode( HttpStatusCodes.BAD_REQUEST );
    }

    public static class TestWS {

        public String validationDefault(
            int i,
            String string
        ) {
            return i + string;
        }

        public String validationOk(
            @WsValidate( "validateOkInt" ) int i,
            @WsValidate( "validateOkOptString" ) Optional<String> optString,
            @WsValidate( "validateOkListString" ) List<String> listString,
            @WsValidate( "validateOkString" ) String string
        ) {
            return i + optString.orElse( "" ) + String.join( "/", listString ) + string;
        }

        public String validationFail(
            @WsValidate( "validateFailInt" ) int i,
            @WsValidate( "validateFailString" ) String string
        ) {
            return i + string;
        }

        protected ValidationErrors validateOkInt( int i ) {
            return empty();
        }

        protected ValidationErrors validateOkOptString( Optional<String> optString ) {
            return empty();
        }

        protected ValidationErrors validateOkListString( List<String> listString ) {
            return empty();
        }

        protected ValidationErrors validateOkString( String string ) {
            return empty();
        }

        protected ValidationErrors validateFailInt( int i ) {
            return error( "error:" + i );
        }

        protected ValidationErrors validateFailString( String string ) {
            return error( "error:" + string );
        }
    }
}
