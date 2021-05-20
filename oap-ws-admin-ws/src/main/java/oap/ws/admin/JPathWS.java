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

package oap.ws.admin;

import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;
import oap.application.Kernel;
import oap.http.HttpResponse;
import oap.jpath.JPath;
import oap.ws.WsMethod;
import oap.ws.WsParam;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static oap.http.Request.HttpMethod.GET;
import static oap.ws.WsParam.From.QUERY;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;

@Slf4j
public class JPathWS {
    private final Kernel kernel;

    public JPathWS( Kernel kernel ) {
        this.kernel = kernel;
    }

    @SuppressWarnings( "unchecked" )
    @WsMethod( method = GET, path = "/" )
    public HttpResponse get( @WsParam( from = QUERY ) String query ) {
        log.debug( "query = {}", query );
        try {
            AtomicReference<Object> result = new AtomicReference<>();
            JPath.evaluate( query, ( Map<String, Object> ) ( Object ) kernel.services.moduleMap, pointer -> result.set( pointer.get() ) );
            return HttpResponse.ok( result.get() ).response();
        } catch( Exception e ) {
            log.error( e.getMessage(), e );
            return HttpResponse.status( HTTP_BAD_REQUEST )
                .withReason( e.getMessage() )
                .withContent( Throwables.getStackTraceAsString( e ), TEXT_PLAIN )
                .response();
        }
    }
}
