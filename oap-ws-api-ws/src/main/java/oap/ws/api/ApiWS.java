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
import oap.http.HttpResponse;
import oap.http.Request;
import oap.json.ext.Ext;
import oap.json.ext.ExtDeserializer;
import oap.reflect.Reflect;
import oap.reflect.Reflection;
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
import java.util.List;
import java.util.Map;

import static ch.qos.logback.core.joran.util.beans.BeanUtil.getPropertyName;
import static ch.qos.logback.core.joran.util.beans.BeanUtil.isGetter;
import static java.util.Comparator.comparing;
import static oap.http.Request.HttpMethod.GET;
import static oap.util.Strings.fill;
import static oap.util.Strings.join;
import static oap.ws.WsParam.From.QUERY;

@Slf4j
public class ApiWS {
    private WebServices webServices;

    public ApiWS( WebServices webServices ) {
        this.webServices = webServices;
    }

    @WsMethod( produces = "text/plain", path = "/", method = GET )
    public String api() {
        String result = "";
        for( Map.Entry<String, Object> ws : webServices.services.entrySet() ) {
            var context = ws.getKey();
            var r = Reflect.reflect( ws.getValue().getClass() );
            log.trace( "service {} -> {}", context, r.name() );
            result += fill( "#", 80 ) + "\n";
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
                result += "\tPermissions " + formatPermissions( m ) + "\n";
                result += "\tReturns " + formatType( 3, r, m.returnType() ) + "\n";
                List<Reflection.Parameter> params = Lists.filter( m.parameters, ApiWS::filterParameter );
                if( params.isEmpty() ) result += "\tNo parameters\n";
                else {
                    result += "\tParameters\n";
                    for( Reflection.Parameter p : params ) {
                        log.trace( "parameter {}", p.name() );
                        result += "\t\t" + formatParameter( p ) + "\n";
                    }
                }
                result += "\n";
            }
            result += "\n";
        }
        return result;
    }

    private String formatPermissions( Reflection.Method method ) {
        return method.findAnnotation( WsSecurity.class )
            .map( ws -> Arrays.toString( ws.permissions() ) )
            .orElse( "<unsecure>" );
    }

    private String formatParameter( Reflection.Parameter p ) {
        String from = p.findAnnotation( WsParam.class ).map( WsParam::from ).orElse( QUERY ).name().toLowerCase();
        return p.name() + ": " + from + " " + formatType( 3, null, p.type() );
    }

    private String formatType( int shift, Reflection clazz, Reflection r ) {
        if( r.assignableTo( HttpResponse.class ) ) return "<http response>";
        if( r.isOptional() ) return "optional " + formatType( shift, clazz, r.typeParameters.get( 0 ) );
        if( r.assignableTo( Collection.class ) )
            return formatType( shift, clazz, r.getCollectionComponentType() ) + "[]";
        if( r.isArray() ) return formatType( shift, clazz, Reflect.reflect( r.underlying.componentType() ) ) + "[]";
        if( r.isPrimitive() ) return r.underlying.getSimpleName();
        if( r.underlying.getPackageName().startsWith( DateTime.class.getPackageName() ) )
            return r.underlying.getSimpleName();
        if( r.assignableTo( Integer.class ) ) return int.class.getSimpleName();
        if( r.assignableTo( Long.class ) ) return long.class.getSimpleName();
        if( r.assignableTo( Double.class ) ) return double.class.getSimpleName();
        if( r.assignableTo( Float.class ) ) return float.class.getSimpleName();
        if( r.assignableTo( Byte.class ) ) return byte.class.getSimpleName();
        if( r.assignableTo( Character.class ) ) return char.class.getSimpleName();
        if( r.assignableTo( String.class ) ) return String.class.getSimpleName();
        if( r.assignableTo( Boolean.class ) ) return Boolean.class.getSimpleName();
        if( r.isEnum() ) return join( ",", List.of( r.underlying.getEnumConstants() ), "[", "]", "\"" );

        return formatComplexType( shift, r );
    }

    private String formatComplexType( int shift, Reflection r ) {
        var result = r.underlying.getSimpleName() + "\n";
        log.trace( "complex type {}", r.name() );
        List<Reflection.Field> fields = new ArrayList<>( r.fields.values() );
        fields.sort( comparing( Reflection.Field::name ) );
        result += fill( "\t", shift ) + "{\n";
        for( Reflection.Field f : fields ) {
            if( !filterField( f ) ) continue;
            log.trace( "type field {}", f.name() );
            result += fill( "\t", shift + 1 ) + f.name() + ": " + formatField( shift, r, f ) + "\n";
        }
        List<Reflection.Method> methods = r.methods;
        methods.sort( comparing( Reflection.Method::name ) );
        for( Reflection.Method m : methods ) {
            if( !filterGetters( m, fields ) ) continue;
            log.trace( "type getter {}", m.name() );
            result += fill( "\t", shift + 1 ) + getPropertyName( m.underlying ) + ": " + formatType( shift + 1, r, m.returnType() ) + "\n";
        }
        result += fill( "\t", shift ) + "}";
        return result;
    }

    private boolean filterGetters( Reflection.Method m, List<Reflection.Field> fields ) {
        return !m.underlying.getDeclaringClass().equals( Object.class )
            && isGetter( m.underlying )
            && m.findAnnotation( JsonIgnore.class ).isEmpty()
            && !Lists.map( fields, Reflection.Field::name ).contains( getPropertyName( m.underlying ) );
    }

    private String formatField( int shift, Reflection r, Reflection.Field f ) {
        Class<?> ext = f.type().assignableTo( Ext.class )
            ? ExtDeserializer.extensionOf( r.underlying, f.name() ) : null;
        Reflection target = ext != null ? Reflect.reflect( ext ) : f.type();
        return formatType( shift + 1, r, target );
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
            && !parameter.type().assignableTo( Request.class );
    }

}
