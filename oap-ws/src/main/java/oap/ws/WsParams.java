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
import oap.http.Request;
import oap.reflect.Reflection;
import oap.util.Sets;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.lang.Character.isUpperCase;
import static java.lang.Character.toUpperCase;

@Slf4j
public class WsParams {

    @Nonnull
    public static String uncamelHeaderName( @Nonnull String camel ) {
        StringBuilder result = new StringBuilder();
        for( int i = 0; i < camel.length(); i++ ) {
            char c = camel.charAt( i );
            if( i == 0 ) result.append( toUpperCase( c ) );
            else if( isUpperCase( c ) ) result.append( "-" ).append( c );
            else result.append( c );
        }
        return result.toString();
    }

    public static Object fromSesstion( Session session, Reflection.Parameter parameter ) {
        return session == null ? null
            : parameter.type().isOptional()
                ? session.get( parameter.name() )
                : session.get( parameter.name() ).orElse( null );
    }

    public static Object fromHeader( Request request, Reflection.Parameter parameter, WsParam wsParam ) {
        log.trace( "headers: {}", request.getHeaders() );

        var names = Sets.of( wsParam.name() );
        names.add( uncamelHeaderName( parameter.name() ) );
        names.add( parameter.name() );
        log.trace( "names: {}", names );
        Optional<String> header;
        for( String name : names ) {
            header = request.header( name );
            if( header.isPresent() ) return unwrapOptionalOfRequired( parameter, header );
        }
        return unwrapOptionalOfRequired( parameter, Optional.empty() );
    }

    public static Object unwrapOptionalOfRequired( Reflection.Parameter parameter, Optional<?> opt ) {
        if( parameter.type().isOptional() ) return opt;

        return opt.orElseThrow( () -> new WsClientException( parameter.name() + " is required" ) );
    }

    public static Object fromCookie( Request request, Reflection.Parameter parameter, WsParam wsParam ) {
        var names = Sets.of( wsParam.name() );
        names.add( parameter.name() );
        Optional<String> cookie;
        for( String name : names ) {
            cookie = request.cookie( name );
            if( cookie.isPresent() )
                return unwrapOptionalOfRequired( parameter, cookie );
        }
        return unwrapOptionalOfRequired( parameter, Optional.empty() );
    }

    public static Optional<String> fromPath( Request request, Optional<WsMethod> wsMethod, Reflection.Parameter parameter ) {
        return wsMethod.map( wsm -> WsMethodMatcher.pathParam( wsm.path(), request.getRequestLine(),
            parameter.name() ) )
            .orElseThrow( () -> new WsException( "path parameter " + parameter.name() + " without " + WsMethod.class.getName() + " annotation" ) );
    }

    public static Object fromBody( Request request, Reflection.Parameter parameter ) {
        if( parameter.type().assignableFrom( byte[].class ) )
            return parameter.type().isOptional()
                ? request.readBody()
                : request.readBody().orElseThrow( () -> new WsClientException( "no body for " + parameter.name() ) );
        else if( parameter.type().assignableFrom( InputStream.class ) )
            return request.body.orElseThrow( () -> new WsClientException( "no body for " + parameter.name() ) );
        else
            return unwrapOptionalOfRequired( parameter, request.readBody().map( String::new ) );
    }

    public static Object fromQuery( Request request, Reflection.Parameter parameter, WsParam wsParam ) {
        Set<String> names = wsParam == null ? Sets.of() : Sets.of( wsParam.name() );
        names.add( parameter.name() );
        if( parameter.type().assignableTo( List.class ) )
            return names.stream()
                .map( request::parameters )
                .filter( values -> !values.isEmpty() )
                .findAny()
                .orElse( List.of() );
        else return unwrapOptionalOfRequired( parameter, names.stream()
            .map( request::parameter )
            .filter( Optional::isPresent )
            .map( Optional::orElseThrow )
            .findAny() );
    }

    public static Object fromQuery( Request request, Reflection.Parameter parameter ) {
        return fromQuery( request, parameter, null );
    }
}
