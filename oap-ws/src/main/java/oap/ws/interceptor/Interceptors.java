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

package oap.ws.interceptor;

import lombok.extern.slf4j.Slf4j;
import oap.http.server.nio.HttpServerExchange;
import oap.reflect.Reflection;
import oap.ws.Response;
import oap.ws.Session;

import java.util.List;

@Slf4j
public class Interceptors {
    public static boolean before( List<Interceptor> interceptors, HttpServerExchange exchange, Session session, Reflection.Method method ) {
        for( var interceptor : interceptors ) {
            log.trace( "running before call {}", interceptor.getClass().getSimpleName() );
            var response = interceptor.before( exchange, session, method );
            if( response ) return true;
        }
        return false;
    }

    public static void after( List<Interceptor> interceptors, Response response, Session session ) {
        for( var i = interceptors.size() - 1; i >= 0; i-- ) {
            var interceptor = interceptors.get( i );
            log.trace( "running after call {}", interceptor.getClass().getSimpleName() );
            interceptor.after( response, session );
        }
    }
}
