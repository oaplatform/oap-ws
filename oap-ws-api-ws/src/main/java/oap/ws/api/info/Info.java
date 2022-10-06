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

package oap.ws.api.info;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.http.server.nio.HttpServerExchange;
import oap.reflect.Reflect;
import oap.reflect.Reflection;
import oap.util.BiStream;
import oap.util.Stream;
import oap.util.Strings;
import oap.ws.WebServices;
import oap.ws.WsMethod;

import java.lang.reflect.Modifier;
import java.util.Optional;

import static java.util.Comparator.comparing;
import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

public class Info {
    private final WebServices webServices;

    public Info( WebServices webServices ) {
        this.webServices = webServices;
    }

    public Stream<WebServiceInfo> services() {
        return BiStream.of( webServices.services )
            .mapToObj( ( context, ws ) -> new WebServiceInfo( Reflect.reflect( ws.getClass() ), context ) );
    }

    private static boolean isWebMethod( Reflection.Method m ) {
        return !m.underlying.getDeclaringClass().equals( Object.class )
            && !m.underlying.isSynthetic()
            && !Modifier.isStatic( m.underlying.getModifiers() )
            && m.isPublic();
    }


    @EqualsAndHashCode
    @ToString
    public static class WebServiceInfo {
        private final Reflection reflection;
        public final String context;
        public final String name;

        public WebServiceInfo( Reflection clazz, String context ) {
            this.reflection = clazz;
            this.context = context;
            this.name = clazz.name();
        }

        public Stream<WebMethodInfo> methods() {
            return Stream.of( reflection.methods )
                .filter( Info::isWebMethod )
                .sorted( comparing( Reflection.Method::name ) )
                .map( WebMethodInfo::new );
        }
    }

    @EqualsAndHashCode
    @ToString
    public static class WebMethodInfo {
        private final Reflection.Method method;
        public final String path;
        public final HttpServerExchange.HttpMethod[] methods;
        public final String produces;
        public final String id;
        public final String description;

        public WebMethodInfo( Reflection.Method method ) {
            this.method = method;
            Optional<WsMethod> annotation = method.findAnnotation( WsMethod.class );
            if( annotation.isPresent() ) {
                WsMethod wsm = annotation.get();
                this.path = Strings.isUndefined( wsm.path() ) ? method.name() : wsm.path();
                this.methods = wsm.method();
                this.produces = wsm.produces();
                this.id = method.name();
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
}
