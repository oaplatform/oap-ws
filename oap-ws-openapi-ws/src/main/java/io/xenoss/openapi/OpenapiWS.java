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

package io.xenoss.openapi;

import com.fasterxml.jackson.databind.type.TypeFactory;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.core.util.RefUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import oap.http.server.nio.HttpServerExchange;
import oap.reflect.Reflect;
import oap.reflect.Reflection;
import oap.util.Lists;
import oap.ws.WebServices;
import oap.ws.WsMethod;
import oap.ws.WsMethodDescriptor;
import oap.ws.WsParam;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.apache.http.entity.ContentType;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Comparator.comparing;
import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static oap.ws.WsParam.From.QUERY;


@Slf4j
public class OpenapiWS {
    private static final Map<String, Class<?>> namePrimitiveMap = new HashMap<>();

    private final WebServices webServices;
    private final ModelConverters converters;
    public ApiInfo info;

    public OpenapiWS( WebServices webServices ) {
        this.webServices = webServices;
        this.converters = new ModelConverters();
    }

    public OpenapiWS( WebServices webServices, ApiInfo info ) {
        this( webServices );
        this.info = info;
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

    @WsMethod( path = "/", method = GET )
    public OpenAPI openapi() {
        OpenAPI api = new OpenAPI();
        api.info( createInfo() );
        for( Map.Entry<String, Object> ws : webServices.services.entrySet() ) {
            var r = Reflect.reflect( ws.getValue().getClass() );
            if( r.assignableTo( this.getClass() ) )
                continue;

            var context = ws.getKey();
            var tag = createTag( context );
            api.addTagsItem( tag );

            List<Reflection.Method> methods = r.methods;
            methods.sort( comparing( Reflection.Method::name ) );
            for( Reflection.Method method : methods ) {
                if( !filterMethod( method ) ) continue;
                var wsDescriptor = new WsMethodDescriptor( method );
                var paths = getPaths( api );
                var pathString = path( context, wsDescriptor.path );
                var pathItem = getPathItem( pathString, paths );

                var operation = prepareOperation( method, wsDescriptor, api, tag );

                for( HttpServerExchange.HttpMethod httpMethod : wsDescriptor.methods ) {
                    pathItem.operation( convertMethod( httpMethod ), operation );
                }

            }
        }
        return api;
    }

    private Operation prepareOperation( Reflection.Method method, WsMethodDescriptor wsDescriptor, OpenAPI api, Tag tag ) {
        var params = Lists.filter( method.parameters, OpenapiWS::filterParameter );
        var returnType = prepareType( method.returnType() );

        Operation operation = new Operation();
        operation.addTagsItem( tag.getName() );
        operation.setOperationId( method.name() );
        operation.setParameters( prepareParameters( params ) );
        operation.setRequestBody( prepareRequestBody( params, api ) );
        operation.setResponses( prepareResponse( returnType, wsDescriptor.produces, api ) );
        return operation;
    }

    private ApiResponses prepareResponse( Type type, String produces, OpenAPI api ) {
        var responses = new ApiResponses();
        ApiResponse response = new ApiResponse();
        responses.addApiResponse( "200", response );

        var resolvedSchema = prepareSchema( type, api );
        response.description( "" );
        response.content( createContent( produces,
            createSchemaRef( resolvedSchema.schema, api.getComponents().getSchemas() ) ) );

        return responses;
    }

    private RequestBody prepareRequestBody( List<Reflection.Parameter> parameters, OpenAPI api ) {
        return parameters.stream()
            .filter( item -> from( item ).equals( WsParam.From.BODY.name().toLowerCase() ) )
            .map( item -> createBody( item, api ) )
            .findFirst().orElse( null );
    }

    private List<Parameter> prepareParameters( List<Reflection.Parameter> parameters ) {
        return parameters.stream()
            .filter( item -> !from( item ).equals( WsParam.From.BODY.name().toLowerCase() ) )
            .map( this::createParameter )
            .toList();
    }

    private RequestBody createBody( Reflection.Parameter parameter, OpenAPI api ) {
        var resolvedSchema = prepareSchema( prepareType( parameter.type() ), api );
        var schemas = api.getComponents().getSchemas();

        var result = new RequestBody();
        result.setContent( createContent( ContentType.APPLICATION_JSON.getMimeType(),
            createSchemaRef( resolvedSchema.schema, schemas ) ) );
        if( schemas.containsKey( resolvedSchema.schema.getName() ) ) {
            api.getComponents().addRequestBodies( resolvedSchema.schema.getName(), result );
        }

        return result;
    }

    private Parameter createParameter( Reflection.Parameter parameter ) {
        var result = new Parameter();
        result.setName( parameter.name() );
        var from = from( parameter );
        result.setIn( from );
        if( WsParam.From.PATH.name().toLowerCase().equals( from ) ) {
            result.setRequired( true );
        }
        var resolvedSchema = this.converters.readAllAsResolvedSchema( prepareType( parameter.type() ) );
        if( resolvedSchema != null ) {
            result.setSchema( resolvedSchema.schema );
        }
        return result;
    }

    private Schema createSchemaRef( Schema schema, Map<String, Schema> map ) {
        if( schema != null && map.containsKey( schema.getName() ) ) {
            var result = new Schema<>();
            result.$ref( RefUtils.constructRef( schema.getName() ) );
            return result;
        }
        return schema;
    }

    private ResolvedSchema prepareSchema( Type type, OpenAPI api ) {
        var resolvedSchema = resolvedSchema( type, api );

        resolvedSchema.referencedSchemas.forEach( api::schema );

        return resolvedSchema;
    }

    private ResolvedSchema resolvedSchema( Type type, OpenAPI api ) {
        var resolvedSchema = this.converters.readAllAsResolvedSchema( type );
        if( resolvedSchema != null && resolvedSchema.schema == null ) {
            if( type instanceof ParameterizedType paramType ) {
                var rawClass = TypeFactory.rawClass( type );
                if( Collection.class.isAssignableFrom( rawClass ) ) {
                    resolvedSchema.schema = prepareArraySchema( paramType, api );
                } else if( Map.class.isAssignableFrom( rawClass ) ) {
                    resolvedSchema.schema = new MapSchema();
                }
            }
        }
        return resolvedSchema;
    }

    private ArraySchema prepareArraySchema( ParameterizedType type, OpenAPI api ) {
        var schema = new ArraySchema();
        if( type.getActualTypeArguments().length > 0 ) {
            var resolvedSchema = this.converters.readAllAsResolvedSchema( type.getActualTypeArguments()[0] );
            var itemSchema = resolvedSchema.schema;
            resolvedSchema.referencedSchemas.forEach( api::schema );
            schema.setItems( createSchemaRef( itemSchema, api.getComponents().getSchemas() ) );
        }
        return schema;
    }

    private PathItem.HttpMethod convertMethod( HttpServerExchange.HttpMethod method ) {
        return PathItem.HttpMethod.valueOf( method.toString() );
    }

    private String path( String context, String path ) {
        return "/" + context + path;
    }

    private Content createContent( String mimeType, Schema schema ) {
        var content = new Content();
        var mediaType = new MediaType();
        mediaType.schema( schema );
        content.addMediaType( mimeType, mediaType );
        return content;
    }

    private Info createInfo() {
        Info info = new Info();
        info.setTitle( this.info.title );
        info.setVersion( this.info.version );
        info.setDescription( this.info.description );
        return info;
    }

    private Tag createTag( String name ) {
        var tag = new Tag();
        tag.setName( name );
        return tag;
    }

    private PathItem getPathItem( String pathString, Paths paths ) {
        var pathItem = paths.get( pathString );
        if( pathItem == null ) {
            pathItem = new PathItem();
            paths.put( pathString, pathItem );
        }
        return pathItem;
    }

    private Paths getPaths( OpenAPI api ) {
        var paths = api.getPaths();
        if( paths == null ) {
            paths = new Paths();
            api.setPaths( paths );
        }
        return paths;
    }

    private String from( Reflection.Parameter p ) {
        return p.findAnnotation( WsParam.class ).map( WsParam::from ).orElse( QUERY ).name().toLowerCase();
    }

    private Type prepareType( Reflection type ) {
        if( type.isPrimitive() ) {
            return namePrimitiveMap.get( type.name() );
        } else if( type.isArray() ) {
            var componentType = type.underlying.componentType();
            if( componentType.isPrimitive() ) {
                componentType = namePrimitiveMap.get( componentType.getName() );
            }
            return TypeUtils.parameterize( List.class, componentType );
        }
        return type.getType();
    }

    static {
        namePrimitiveMap.put( "boolean", Boolean.class );
        namePrimitiveMap.put( "byte", Byte.class );
        namePrimitiveMap.put( "char", Character.class );
        namePrimitiveMap.put( "short", Short.class );
        namePrimitiveMap.put( "int", Integer.class );
        namePrimitiveMap.put( "long", Long.class );
        namePrimitiveMap.put( "double", Double.class );
        namePrimitiveMap.put( "float", Float.class );
        namePrimitiveMap.put( "void", Void.class );
    }
}
