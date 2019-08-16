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

package oap.ws.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import oap.http.HttpResponse;
import oap.http.Request;
import oap.reflect.Reflect;
import oap.reflect.Reflection;
import oap.util.BiStream;
import oap.util.Stream;
import oap.util.Strings;
import oap.ws.WebServices;
import oap.ws.WsMethod;
import oap.ws.WsMethodDescriptor;
import oap.ws.WsParam;
import oap.ws.sso.WsSecurity;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static java.util.stream.Collectors.joining;
import static oap.http.Request.HttpMethod.GET;
import static oap.util.Pair.__;
import static oap.util.Strings.fill;
import static oap.ws.WsParam.From.QUERY;

public class ApiWS {
    private WebServices webServices;

    public ApiWS( WebServices webServices ) {
        this.webServices = webServices;
    }

    @WsMethod( produces = "text/plain", path = "/", method = GET )
    public String api() {
        return BiStream.of( webServices.services )
            .map( ( context, s ) -> __( context, Reflect.reflect( s.getClass() ) ) )
            .mapToObj( ( context, r ) -> "######################################################################\n"
                + "Service  " + r.name() + "\n"
                + "Bound to  " + context + "\n"
                + "Methods:\n" + Stream.of( r.methods )
                .filter( ApiWS::filterMethod )
                .map( WsMethodDescriptor::new )
                .sorted( Comparator.comparing( m -> m.method.name() ) )
                .map( m -> "\tMethod " + m.method.name() + "\n"
                    + "\t" + Arrays.toString( m.methods ) + " /" + context + m.path + "\n"
                    + "\tProduces " + m.produces + "\n"
                    + "\tPermissions " + formatPermissions( m.method ) + "\n"
                    + "\tReturns " + formatType( 3, m.method.returnType() ) + "\n"
                    + "\tParameters\n" + Stream.of( m.method.parameters )
                    .filter( ApiWS::filterParameter )
                    .map( p -> "\t\t" + formatParameter( p ) )
                    .collect( joining( "\n" ) )
                )
                .collect( joining( "\n\n" ) )
            )
            .collect( joining( "\n\n" ) );
    }

    private String formatPermissions( Reflection.Method method ) {
        return method.findAnnotation( WsSecurity.class )
            .map( ws -> Arrays.toString( ws.permissions() ) )
            .orElse( "<unsecure>" );
    }

    private String formatParameter( Reflection.Parameter p ) {
        String from = p.findAnnotation( WsParam.class ).map( WsParam::from ).orElse( QUERY ).name().toLowerCase();
        return p.name() + ": " + from + " " + formatType( 3, p.type() );
    }

    private String formatType( int shift, Reflection r ) {
        if( r.assignableTo( HttpResponse.class ) ) return "<http response>";
        if( r.isOptional() ) return "optional " + formatType( shift, r.typeParameters.get( 0 ) );
        if( r.assignableTo( Collection.class ) ) return formatType( shift, r.getCollectionComponentType() ) + "[]";
        if( r.isArray() ) return formatType( shift, Reflect.reflect( r.underlying.componentType() ) ) + "[]";
        if( r.isPrimitive() ) return r.underlying.getSimpleName();
        if( r.assignableTo( Integer.class ) ) return int.class.getSimpleName();
        if( r.assignableTo( Long.class ) ) return long.class.getSimpleName();
        if( r.assignableTo( Double.class ) ) return double.class.getSimpleName();
        if( r.assignableTo( Float.class ) ) return float.class.getSimpleName();
        if( r.assignableTo( Byte.class ) ) return byte.class.getSimpleName();
        if( r.assignableTo( Character.class ) ) return char.class.getSimpleName();
        if( r.assignableTo( String.class ) ) return String.class.getSimpleName();
        if( r.isEnum() ) return Strings.join( ",", List.of( r.underlying.getEnumConstants() ), "[", "]", "\"" );
        return formatComplexType( shift, r );
    }

    private String formatComplexType( int shift, Reflection r ) {
        return r.underlying.getSimpleName() + "\n" + Stream.of( r.fields.values() )
            .filter( ApiWS::filterField )
            .sorted( Comparator.comparing( Reflection.Field::name ) )
            .map( f -> fill( "\t", shift + 1 ) + f.name() + ": " + formatType( shift + 1, f.type() ) )
            .collect( joining( ",\n", fill( "\t", shift ) + "{\n", "\n" + fill( "\t", shift ) + "}\n" ) );
    }

    private static boolean filterField( Reflection.Field field ) {
        return field.findAnnotation( JsonIgnore.class ).isEmpty();
    }

    private static boolean filterMethod( Reflection.Method m ) {
        return !m.underlying.getDeclaringClass().equals( Object.class )
            && !m.underlying.isSynthetic()
            && !Modifier.isStatic( m.underlying.getModifiers() )
            && m.isPublic();
    }

    private static boolean filterParameter( Reflection.Parameter parameter ) {
        return parameter.findAnnotation( WsParam.class )
            .map( wsp -> wsp.from() != WsParam.From.SESSION )
            .orElse( true )
            && !parameter.type().assignableTo( Request.class );
    }

}
