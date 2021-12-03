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

import com.google.common.base.Preconditions;
import oap.http.ContentTypes;
import oap.http.Cookie;
import oap.http.Headers;
import oap.http.HttpStatusCodes;
import oap.http.server.nio.HttpServerExchange;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

public class Response {
    public final HashMap<String, String> headers = new HashMap<>();
    public final ArrayList<Cookie> cookies = new ArrayList<>();
    public int code;
    public String contentType;
    public Object body;
    public boolean raw;
    public String reasonPhrase;

    public Response( int code ) {
        this( code, null );
    }

    public Response( int code, String reasonPhrase ) {
        this( code, reasonPhrase, null );
    }

    public Response( int code, String reasonPhrase, String contentType ) {
        this( code, reasonPhrase, contentType, null );
    }

    public Response( int code, String reasonPhrase, String contentType, Object body ) {
        this( code, reasonPhrase, contentType, body, false );
    }

    public Response( int code, String reasonPhrase, String contentType, Object body, boolean raw ) {
        this.code = code;
        this.reasonPhrase = reasonPhrase;
        this.contentType = contentType;
        this.body = body;
        this.raw = raw;
    }

    public static Response noContent() {
        return new Response( HttpStatusCodes.NO_CONTENT );
    }

    public static Response jsonOk() {
        return new Response( HttpStatusCodes.OK ).withContentType( ContentTypes.APPLICATION_JSON );
    }

    public Response withStatusCode( int code ) {
        this.code = code;

        return this;
    }

    public Response withReasonPhrase( String reasonPhrase ) {
        this.reasonPhrase = reasonPhrase;

        return this;
    }

    public Response withContentType( String contentType ) {
        this.contentType = contentType;

        return this;
    }

    public Response withBody( Object body ) {
        return withBody( body, false );
    }

    public Response withBody( Object body, boolean raw ) {
        this.body = body;
        this.raw = raw;

        return this;
    }

    public Response withHeader( String name, String value ) {
        headers.put( name, value );

        return this;
    }

    public Response withCookie( Cookie cookie ) {
        cookies.add( cookie );

        return this;
    }

    public void send( HttpServerExchange exchange ) {
        exchange.setStatusCode( code );
        if( reasonPhrase != null ) exchange.setReasonPhrase( reasonPhrase );
        headers.forEach( exchange::setResponseHeader );
        cookies.forEach( cookie -> exchange.setResponseCookie( cookie ) );
        if( contentType != null ) exchange.setResponseHeader( Headers.CONTENT_TYPE, contentType );
        if( body != null ) {
            if( body instanceof byte[] ) exchange.send( ( byte[] ) body );
            else if( body instanceof ByteBuffer ) exchange.send( ( ByteBuffer ) body );
            else if( body instanceof String ) {
                if( raw ) exchange.send( ( String ) body );
                else {
                    exchange.send( HttpServerExchange.contentToString( true, body, contentType ) );
                }
            } else {
                Preconditions.checkArgument( !raw );
                exchange.send( HttpServerExchange.contentToString( false, body, contentType ) );
            }
        }
    }
}
