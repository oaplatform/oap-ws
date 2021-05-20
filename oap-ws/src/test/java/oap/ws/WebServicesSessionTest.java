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

import oap.application.testng.KernelFixture;
import oap.http.HttpResponse;
import oap.testng.Fixtures;
import oap.util.Maps;
import org.apache.http.entity.ContentType;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static oap.http.Request.HttpMethod.GET;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.httpUrl;
import static oap.io.Resources.urlOrThrow;
import static oap.util.Pair.__;
import static oap.ws.WsParam.From.SESSION;

public class WebServicesSessionTest extends Fixtures {

    {
        fixture( new KernelFixture( urlOrThrow( getClass(), "/application.test.conf" ) ) );
    }

    @Test
    public void sessionViaResponse() {
        assertGet( httpUrl( "/session/put" ), Maps.of( __( "value", "vvv" ) ), Maps.empty() )
            .hasCode( 204 );
        assertGet( httpUrl( "/session/get" ) )
            .isOk()
            .hasBody( "vvv" );
    }

    @Test
    public void sessionDirectly() {
        assertGet( httpUrl( "/session/putDirectly" ), Maps.of( __( "value", "vvv" ) ), Maps.empty() )
            .hasCode( 204 );
        assertGet( httpUrl( "/session/get" ) )
            .isOk()
            .hasBody( "vvv" );
    }

    @Test
    public void respondHtmlContentType() {
        assertGet( httpUrl( "/session/putDirectly" ), Maps.of( __( "value", "vvv" ) ), Maps.empty() )
            .hasCode( 204 );
        assertGet( httpUrl( "/session/html" ) )
            .isOk()
            .hasBody( "vvv" )
            .hasContentType( ContentType.TEXT_HTML.withCharset( StandardCharsets.UTF_8 ) );
    }

    @SuppressWarnings( "unused" )
    private static class TestWS {

        public static final String IN_SESSION = "inSession";

        public HttpResponse put( String value ) {
            return HttpResponse.status( HTTP_NO_CONTENT )
                .withSessionValue( IN_SESSION, value )
                .response();
        }

        public void putDirectly( String value, Session session ) {
            session.set( IN_SESSION, value );
        }

        @WsMethod( path = "/get", method = GET, produces = "text/plain" )
        public String get( @WsParam( from = SESSION ) String inSession ) {
            return inSession;
        }

        @WsMethod( path = "/html", method = GET, produces = "text/html" )
        public String getHtml( @WsParam( from = SESSION ) String inSession ) {
            return inSession;
        }
    }
}
