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
package oap.ws;

import lombok.extern.slf4j.Slf4j;
import oap.http.Client;
import oap.http.Handler;
import oap.http.HttpResponse;
import oap.http.Request;
import oap.http.Response;
import oap.metrics.Metrics;
import oap.testng.Fixtures;
import oap.util.Maps;
import oap.util.Pair;
import oap.ws.testng.WsFixture;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPOutputStream;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.nio.charset.StandardCharsets.UTF_8;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.assertPost;
import static oap.http.testng.HttpAsserts.httpUrl;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;

@Slf4j
public class WebServicesTest extends Fixtures {
    {
        fixture( new WsFixture( getClass(), ( ws, kernel ) -> {
            kernel.register( "math", new MathWS() );
            kernel.register( "handler", new TestHandler() );
        }, "ws.json", "ws.conf" ) );
    }

    @Test
    public void path() {
        assertGet( httpUrl( "/x/v/math" ) )
            .responded( HTTP_OK, "OK", APPLICATION_JSON, "2" );
    }

    @Test
    public void sort() {
        assertGet( httpUrl( "/x/v/math/test/sort/default" ) )
            .responded( HTTP_OK, "OK", APPLICATION_JSON, "\"__default__\"" );
        assertGet( httpUrl( "/x/v/math/test/sort/45" ) )
            .responded( HTTP_OK, "OK", APPLICATION_JSON, "\"45\"" );
    }

    @Test
    public void equal() {
        assertGet( httpUrl( "/x/v/math/test/sort=3/test" ) )
            .responded( HTTP_OK, "OK", APPLICATION_JSON, "\"3\"" );
    }


    @Test
    public void invocations() {
        assertGet( httpUrl( "/x/v/math/x?i=1&s=2" ) )
            .respondedJson( HTTP_INTERNAL_ERROR, "failed", "{\"message\":\"failed\"}" );
        assertGet( httpUrl( "/x/v/math/x?i=1&s=2" ) )
            .respondedJson( HTTP_INTERNAL_ERROR, "failed", "{\"message\":\"failed\"}" );
        assertGet( httpUrl( "/x/v/math/sumab?a=1&b=2" ) )
            .responded( HTTP_OK, "OK", APPLICATION_JSON, "3" );
        assertGet( httpUrl( "/x/v/math/x?i=1&s=2" ) )
            .respondedJson( HTTP_INTERNAL_ERROR, "failed", "{\"message\":\"failed\"}" );
        assertGet( httpUrl( "/x/v/math/sumabopt?a=1" ) )
            .responded( HTTP_OK, "OK", APPLICATION_JSON, "1" );
        assertGet( httpUrl( "/x/v/math/bean?i=1&s=sss" ) )
            .respondedJson( HTTP_OK, "OK", "{\"i\":1,\"s\":\"sss\"}" );
        assertPost( httpUrl( "/x/v/math/bytes" ), "1234", APPLICATION_OCTET_STREAM )
            .responded( HTTP_OK, "OK", APPLICATION_JSON, "\"1234\"" );
        assertPost( httpUrl( "/x/v/math/string" ), "1234", APPLICATION_OCTET_STREAM )
            .responded( HTTP_OK, "OK", APPLICATION_JSON, "\"1234\"" );
        assertGet( httpUrl( "/x/v/math/code?code=204" ) )
            .hasCode( HTTP_NO_CONTENT );
        assertEquals(
            Metrics.snapshot( Metrics.name( "rest_timer" )
                .tag( "service", MathWS.class.getName() )
                .tag( "method", "bean" ) ).count,
            1 );
        assertGet( httpUrl( "/x/h/" ) ).hasCode( HTTP_NO_CONTENT );
        assertGet( httpUrl( "/hocon/x/v/math/x?i=1&s=2" ) )
            .respondedJson( HTTP_INTERNAL_ERROR, "failed", "{\"message\":\"failed\"}" );

    }

    @Test
    public void enumValue() {
        assertGet( httpUrl( "/x/v/math/en?a=CLASS" ) )
            .responded( HTTP_OK, "OK", APPLICATION_JSON, "\"CLASS\"" );
    }

    @Test
    public void optional() {
        assertGet( httpUrl( "/x/v/math/sumabopt?a=1&b=2" ) )
            .responded( HTTP_OK, "OK", APPLICATION_JSON, "3" );
    }

    @Test
    public void parameterList() {
        assertGet( httpUrl( "/x/v/math/sum?a=1&b=2&b=3" ) )
            .responded( HTTP_OK, "OK", APPLICATION_JSON, "6" );
    }

    @Test
    public void string() {
        assertGet( httpUrl( "/x/v/math/id?a=aaa" ) )
            .responded( HTTP_OK, "OK", APPLICATION_JSON, "\"aaa\"" );
    }

    @Test
    public void request() {
        assertGet( httpUrl( "/x/v/math/req" ) )
            .responded( HTTP_OK, "OK", APPLICATION_JSON, "\"" + httpUrl( "/x/v/math\"" ) );
    }

    @Test
    public void bean() {
        assertPost( httpUrl( "/x/v/math/json" ), "{\"i\":1,\"s\":\"sss\"}", APPLICATION_JSON )
            .responded( HTTP_OK, "OK", APPLICATION_JSON, "{\"i\":1,\"s\":\"sss\"}" );
    }

    @Test
    public void list() {
        assertPost( httpUrl( "/x/v/math/list" ), "[\"1str\", \"2str\"]", APPLICATION_JSON )
            .responded( HTTP_OK, "OK", APPLICATION_JSON, "[\"1str\",\"2str\"]" );
    }

    @Test
    public void defaultHeaders() {
        assertGet( httpUrl( "/x/h/" ) )
            .containsHeader( "Access-Control-Allow-Origin", "*" );
        assertPost( httpUrl( "/x/v/math/json" ), "{\"i\":1,\"s\":\"sss\"}",
            APPLICATION_OCTET_STREAM ).containsHeader( "Access-Control-Allow-Origin", "*" );
    }

    @Test
    public void shouldVerifyGZIPRequestProcessing() throws Exception {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final GZIPOutputStream gzip = new GZIPOutputStream( byteArrayOutputStream );
        gzip.write( "{\"i\":1,\"s\":\"sss\"}".getBytes( UTF_8 ) );
        gzip.close();

        final Client.Response response = Client
            .custom()
            .build()
            .post( httpUrl( "/x/v/math/json" ),
                new ByteArrayInputStream( byteArrayOutputStream.toByteArray() ),
                APPLICATION_JSON, Maps.of( Pair.__( "Content-Encoding", "gzip" ) ) );

        assertThat( response.code ).isEqualTo( HTTP_OK );
        assertThat( response.contentString() ).isEqualTo( "{\"i\":1,\"s\":\"sss\"}" );
    }

    static class TestHandler implements Handler {
        @Override
        public void handle( Request request, Response response ) {
            response.respond( HttpResponse.NO_CONTENT );
        }
    }

}

