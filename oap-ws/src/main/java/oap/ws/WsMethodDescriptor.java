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

import oap.http.server.nio.HttpServerExchange;
import oap.reflect.Reflection;
import oap.util.Strings;

import java.util.Optional;

import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

public class WsMethodDescriptor {
    public final Reflection.Method method;
    public final String path;
    public final HttpServerExchange.HttpMethod[] methods;
    public final String produces;
    public final String id;
    public final String description;

    public WsMethodDescriptor( Reflection.Method method ) {
        this.method = method;
        Optional<WsMethod> annotation = method.findAnnotation( WsMethod.class );
        if( annotation.isPresent() ) {
            WsMethod wsm = annotation.get();
            this.path = Strings.isUndefined( wsm.path() ) ? method.name() : wsm.path();
            this.methods = wsm.method();
            this.produces = wsm.produces();
            this.id = Strings.isUndefined( wsm.id() ) ? method.name() : wsm.id();
            this.description = Strings.isUndefined( wsm.description() ) ? "" : wsm.description();
        } else {
            this.path = "/" + method.name();
            this.methods = new HttpServerExchange.HttpMethod[] { GET };
            this.produces = APPLICATION_JSON.getMimeType();
            this.id = method.name();
            this.description = "";
        }
    }
}
