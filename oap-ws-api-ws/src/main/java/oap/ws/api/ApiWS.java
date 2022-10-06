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
import lombok.extern.slf4j.Slf4j;
import oap.dictionary.Dictionary;
import oap.http.server.nio.HttpServerExchange;
import oap.json.ext.Ext;
import oap.json.ext.ExtDeserializer;
import oap.reflect.Reflect;
import oap.reflect.Reflection;
import oap.util.Lists;
import oap.ws.Response;
import oap.ws.WebServices;
import oap.ws.WsMethod;
import oap.ws.WsMethodDescriptor;
import oap.ws.WsParam;
import oap.ws.sso.WsSecurity;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;

import static ch.qos.logback.core.joran.util.beans.BeanUtil.getPropertyName;
import static ch.qos.logback.core.joran.util.beans.BeanUtil.isGetter;
import static java.util.Comparator.comparing;
import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static oap.util.Strings.join;
import static oap.ws.WsParam.From.QUERY;

@SuppressWarnings( "StringConcatenationInLoop" )
@Slf4j
public class ApiWS {
    private final WebServices webServices;

    public ApiWS( WebServices webServices ) {
        this.webServices = webServices;
    }

    private static boolean ignorable( Reflection.Field field ) {
        return field.isStatic()
            || field.underlying.isSynthetic()
            || field.findAnnotation( JsonIgnore.class ).isPresent();
    }

    private boolean ignorable( Reflection.Method m, List<Reflection.Field> fields ) {
        return !m.underlying.getDeclaringClass().equals( Object.class )
            && isGetter( m.underlying )
            && !m.isAnnotatedWith( JsonIgnore.class )
            && !Lists.map( fields, Reflection.Field::name ).contains( getPropertyName( m.underlying ) );
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
            && !parameter.type().assignableTo( HttpServerExchange.class );
    }

    @WsMethod( produces = "text/plain", path = "/", method = GET, description = "Generates description of WS method with parameters and result" )
    public String api() {
        String result = "# SERVICES " + "#".repeat( 69 ) + "\n";
        Types types = new Types();
        for( Map.Entry<String, Object> ws : webServices.services.entrySet() ) {
            var context = ws.getKey();
            var r = Reflect.reflect( ws.getValue().getClass() );
            log.trace( "service {} -> {}", context, r.name() );
            result += "## " + r.name() + " " + "#".repeat( Math.max( 0, 76 - r.name().length() ) ) + "\n";
            result += "Bound to " + context + "\n";
            result += "Methods:\n";

            List<Reflection.Method> methods = r.methods;
            methods.sort( comparing( Reflection.Method::name ) );
            for( Reflection.Method m : methods ) {
                if( !filterMethod( m ) ) continue;
                var d = new WsMethodDescriptor( m );
                log.trace( "method {}", m.name() );
                result += "\tMethod " + m.name()
                    + ( m.isAnnotatedWith( Deprecated.class ) ? " (Deprecated)" : "" ) + "\n";
                result += "\t" + Arrays.toString( d.methods ) + " /" + context + d.path + "\n";
                result += "\tProduces " + d.produces + "\n";
                result += "\tRealm param " + formatRealm( m ) + "\n";
                result += "\tPermissions " + formatPermissions( m ) + "\n";
                result += "\tReturns " + formatType( m.returnType(), types ) + "\n";
                List<Reflection.Parameter> params = Lists.filter( m.parameters, ApiWS::filterParameter );
                if( params.isEmpty() ) result += "\tNo parameters\n";
                else {
                    result += "\tParameters\n";
                    for( Reflection.Parameter p : params ) {
                        log.trace( "parameter {}", p.name() );
                        result += "\t\t" + formatParameter( p, types ) + "\n";
                    }
                }
                result += "\n";
            }
            result += "\n";
        }
        result += "# TYPES " + "#".repeat( 72 ) + "\n";
        for( Reflection type : types ) {
            result += "## " + type.name() + " " + "#".repeat( Math.max( 0, 76 - type.name().length() ) ) + "\n";
            result += formatComplexType( type, types ) + "\n";
            result += "\n";
        }
        return result;
    }

    private String formatRealm( Reflection.Method method ) {
        return method.findAnnotation( WsSecurity.class )
            .map( WsSecurity::realm )
            .orElse( "<no realm>" );
    }

    private String formatPermissions( Reflection.Method method ) {
        return method.findAnnotation( WsSecurity.class )
            .map( ws -> Arrays.toString( ws.permissions() ) )
            .orElse( "<no permissions>" );
    }

    private String formatParameter( Reflection.Parameter p, Types types ) {
        String from = p.findAnnotation( WsParam.class ).map( WsParam::from ).orElse( QUERY ).name().toLowerCase();
        return p.name() + ": " + from + " " + formatType( p.type(), types );
    }

    private String formatType( Reflection r, Types types ) {
        if( r.isOptional() )
            return "optional " + formatType( r.typeParameters.get( 0 ), types );
        if( r.assignableTo( Map.class ) ) return "map String -> " + formatType( r.getMapComponentsType()._2, types );
        if( r.assignableTo( Collection.class ) )
            return formatType( r.getCollectionComponentType(), types ) + "[]";
        if( r.assignableTo( Stream.class ) )
            return formatType( r.typeParameters.get( 0 ), types ) + "[]";
        if( r.assignableTo( Iterator.class ) )
            return formatType( r.typeParameters.get( 0 ), types ) + "[]";
        if( r.isArray() )
            return formatType( Reflect.reflect( r.underlying.componentType() ), types ) + "[]";
        if( r.isPrimitive() ) return r.underlying.getSimpleName();
        if( r.underlying.getPackageName().startsWith( DateTime.class.getPackageName() ) )
            return r.underlying.getSimpleName();
        if( r.underlying.equals( Date.class ) ) return r.underlying.getSimpleName();
        if( r.assignableTo( Integer.class ) ) return int.class.getSimpleName();
        if( r.assignableTo( Long.class ) ) return long.class.getSimpleName();
        if( r.assignableTo( Double.class ) ) return double.class.getSimpleName();
        if( r.assignableTo( Float.class ) ) return float.class.getSimpleName();
        if( r.assignableTo( Byte.class ) ) return byte.class.getSimpleName();
        if( r.assignableTo( Character.class ) ) return char.class.getSimpleName();
        if( r.assignableTo( String.class ) ) return String.class.getSimpleName();
        if( r.assignableTo( Boolean.class ) ) return Boolean.class.getSimpleName();
        if( r.assignableTo( Dictionary.class ) ) return Dictionary.class.getSimpleName();
        if( r.isEnum() ) return join( ",", List.of( r.underlying.getEnumConstants() ), "[", "]", "\"" );
        if( r.assignableTo( Response.class ) ) return "<http response>";
        types.push( r );
        return r.name();
    }

    private String formatComplexType( Reflection r, Types types ) {
        var result = r.underlying.getSimpleName() + "\n";
        log.trace( "complex type {}", r.name() );
        List<Reflection.Field> fields = new ArrayList<>( r.fields.values() );
        fields.sort( comparing( Reflection.Field::name ) );
        result += "{\n";
        for( Reflection.Field f : fields ) {
            if( ignorable( f ) ) continue;
            log.trace( "type field {}", f.name() );
            result += "\t" + f.name() + ": " + formatField( r, f, types )
                + ( f.isAnnotatedWith( Deprecated.class ) ? " (Deprecated)" : "" ) + "\n";
        }
        List<Reflection.Method> methods = r.methods;
        methods.sort( comparing( Reflection.Method::name ) );
        for( Reflection.Method m : methods ) {
            if( !ignorable( m, fields ) ) continue;
            log.trace( "type getter {}", m.name() );
            result += "\t" + getPropertyName( m.underlying ) + ": " + formatType( m.returnType(), types ) + "\n";
        }
        result += "\t".repeat( 0 ) + "}";
        return result;
    }

    private String formatField( Reflection r, Reflection.Field f, Types types ) {
        Class<?> ext = f.type().assignableTo( Ext.class )
            ? ExtDeserializer.extensionOf( r.underlying, f.name() )
            : null;
        Reflection target = ext != null ? Reflect.reflect( ext ) : f.type();
        return formatType( target, types );
    }

    private static class Types implements Iterable<Reflection> {
        private final Set<Reflection> processed = new HashSet<>();
        private final Queue<Reflection> types = new LinkedList<>();

        public void push( Reflection type ) {
            if( !processed.contains( type ) && !types.contains( type ) ) types.add( type );
        }

        @NotNull
        public Iterator<Reflection> iterator() {
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return !types.isEmpty();
                }

                @Override
                public Reflection next() {
                    Reflection next = types.poll();
                    processed.add( next );
                    return next;
                }
            };
        }
    }

}
