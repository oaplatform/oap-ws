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
import oap.util.AssocList;
import oap.util.Lists;
import oap.ws.WebServices;
import oap.ws.WsMethod;
import oap.ws.WsMethodDescriptor;
import oap.ws.WsParam;
import oap.ws.sso.WsSecurity;
import org.joda.time.DateTime;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static ch.qos.logback.core.joran.util.beans.BeanUtil.getPropertyName;
import static ch.qos.logback.core.joran.util.beans.BeanUtil.isGetter;
import static java.util.Comparator.comparing;
import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static oap.util.Strings.join;
import static oap.ws.WsParam.From.QUERY;

@Slf4j
public class ApiWS {
    private final WebServices webServices;

    public ApiWS( WebServices webServices ) {
        this.webServices = webServices;
    }

    private static boolean filterField( Reflection.Field field ) {
        return !field.isStatic()
            && !field.underlying.isSynthetic()
            && field.findAnnotation( JsonIgnore.class ).isEmpty();
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
        String result = "";
        for( Map.Entry<String, Object> ws : webServices.services.entrySet() ) {
            var context = ws.getKey();
            var r = Reflect.reflect( ws.getValue().getClass() );
            log.trace( "service {} -> {}", context, r.name() );
            result += "#".repeat( 80 ) + "\n";
            result += "Service " + r.name() + "\n";
            result += "Bound to " + context + "\n";
            result += "Methods:\n";

            List<Reflection.Method> methods = r.methods;
            methods.sort( comparing( Reflection.Method::name ) );
            for( Reflection.Method m : methods ) {
                if( !filterMethod( m ) ) continue;
                var d = new WsMethodDescriptor( m );
                log.trace( "method {}", m.name() );
                result += "\tMethod " + m.name() + "\n";
                result += "\t" + Arrays.toString( d.methods ) + " /" + context + d.path + "\n";
                result += "\tProduces " + d.produces + "\n";
                result += "\tRealm param " + formatRealm( m ) + "\n";
                result += "\tPermissions " + formatPermissions( m ) + "\n";
                result += "\tReturns " + formatType( 3, r, m.returnType(), new ArrayList<>() ) + "\n";
                List<Reflection.Parameter> params = Lists.filter( m.parameters, ApiWS::filterParameter );
                if( params.isEmpty() ) result += "\tNo parameters\n";
                else {
                    result += "\tParameters\n";
                    for( Reflection.Parameter p : params ) {
                        log.trace( "parameter {}", p.name() );
                        result += "\t\t" + formatParameter( p, new ArrayList<>() ) + "\n";
                    }
                }
                result += "\n";
            }
            result += "\n";
        }
        return result;
    }

    private String formatRealm( Reflection.Method method ) {
        return method.findAnnotation( WsSecurity.class )
            .map( WsSecurity::realm )
            .orElse( "<unsecure>" );
    }

    private String formatPermissions( Reflection.Method method ) {
        return method.findAnnotation( WsSecurity.class )
            .map( ws -> Arrays.toString( ws.permissions() ) )
            .orElse( "<unsecure>" );
    }

    private String formatParameter( Reflection.Parameter p, List<String> previouslyReferencedClasses ) {
        String from = p.findAnnotation( WsParam.class ).map( WsParam::from ).orElse( QUERY ).name().toLowerCase();
        return p.name() + ": " + from + " " + formatType( 3, null, p.type(), previouslyReferencedClasses );
    }

    private String formatType( int shift, Reflection clazz, Reflection r, List<String> previouslyReferencedClasses ) {
        if( r.isOptional() )
            return "optional " + formatType( shift, clazz, r.typeParameters.get( 0 ), previouslyReferencedClasses );
        if( r.assignableTo( AssocList.class ) ) return AssocList.class.getSimpleName();
        if( r.assignableTo( Map.class ) ) return Map.class.getSimpleName();
        if( r.assignableTo( Collection.class ) ) {
            log.trace( "DEBUG: Collections recursion - {}/{}/{}", shift, clazz, r );
            return formatType( shift, clazz, r.getCollectionComponentType(), previouslyReferencedClasses ) + "[]";
        }
        if( r.isArray() )
            return formatType( shift, clazz, Reflect.reflect( r.underlying.componentType() ), previouslyReferencedClasses ) + "[]";
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

        return formatComplexType( shift, r, previouslyReferencedClasses );
    }

    private String formatComplexType( int shift, Reflection r, List<String> previouslyReferencedClasses ) {
        if( previouslyReferencedClasses.contains( r.name() ) ) {
            return "<Recursive Reference>";
        }
        List<String> referencedClasses = new ArrayList<>( previouslyReferencedClasses );
        referencedClasses.add( r.name() );

        var result = r.underlying.getSimpleName() + "\n";
        log.trace( "complex type {}", r.name() );
        List<Reflection.Field> fields = new ArrayList<>( r.fields.values() );
        fields.sort( comparing( Reflection.Field::name ) );
        result += "\t".repeat( shift ) + "{\n";
        for( Reflection.Field f : fields ) {
            if( !filterField( f ) ) continue;
            log.trace( "type field {}", f.name() );
            result += "\t".repeat( shift + 1 ) + f.name() + ": " + formatField( shift, r, f, referencedClasses ) + "\n";
        }
        List<Reflection.Method> methods = r.methods;
        methods.sort( comparing( Reflection.Method::name ) );
        for( Reflection.Method m : methods ) {
            if( !filterGetters( m, fields ) ) continue;
            log.trace( "type getter {}", m.name() );
            result += "\t".repeat( shift + 1 ) + getPropertyName( m.underlying ) + ": " + formatType( shift + 1, r, m.returnType(), referencedClasses ) + "\n";
        }
        result += "\t".repeat( shift ) + "}";
        return result;
    }

    private boolean filterGetters( Reflection.Method m, List<Reflection.Field> fields ) {
        return !m.underlying.getDeclaringClass().equals( Object.class )
            && isGetter( m.underlying )
            && m.findAnnotation( JsonIgnore.class ).isEmpty()
            && !Lists.map( fields, Reflection.Field::name ).contains( getPropertyName( m.underlying ) );
    }

    private String formatField( int shift, Reflection r, Reflection.Field f, List<String> previouslyReferencedClasses ) {
        Class<?> ext = f.type().assignableTo( Ext.class )
            ? ExtDeserializer.extensionOf( r.underlying, f.name() ) : null;
        Reflection target = ext != null ? Reflect.reflect( ext ) : f.type();
        return formatType( shift + 1, r, target, previouslyReferencedClasses );
    }

}
