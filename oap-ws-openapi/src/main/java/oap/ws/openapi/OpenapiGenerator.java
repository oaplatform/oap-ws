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

package oap.ws.openapi;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import lombok.Setter;
import oap.http.server.nio.HttpServerExchange;
import oap.reflect.Reflect;
import oap.reflect.Reflection;
import oap.util.Lists;
import oap.ws.WsMethod;
import oap.ws.WsMethodDescriptor;
import oap.ws.WsParam;
import oap.ws.WsSecurityDescriptor;
import oap.ws.openapi.util.WsApiReflectionUtils;
import org.apache.http.entity.ContentType;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Comparator.comparing;
import static oap.ws.openapi.util.SchemaUtils.createSchemaRef;
import static oap.ws.openapi.util.SchemaUtils.prepareSchema;
import static oap.ws.openapi.util.SchemaUtils.prepareType;
import static oap.ws.openapi.util.WsApiReflectionUtils.description;
import static oap.ws.openapi.util.WsApiReflectionUtils.filterMethod;
import static oap.ws.openapi.util.WsApiReflectionUtils.from;
import static oap.ws.openapi.util.WsApiReflectionUtils.tag;

public class OpenapiGenerator {
    public static final String OPEN_API_VERSION = "3.0.3";
    public static final String SECURITY_SCHEMA_NAME = "JWT";
    private final ArrayListMultimap<String, String> versions = ArrayListMultimap.create();
    private final ModelConverters converters = new ModelConverters();
    private final OpenAPI api = new OpenAPI();
    @Setter
    private String title;
    @Setter
    private String description;
    private final OpenapiGeneratorSettings settings;

    public OpenapiGenerator( String title, String description, OpenapiGeneratorSettings settings ) {
        this.title = title;
        this.description = description;
        this.settings = settings;
        api.openapi( OPEN_API_VERSION );
    }

    public OpenapiGenerator( String title, String description ) {
        this(
            title,
            description,
            OpenapiGeneratorSettings.builder()
                .processOnlyAnnotatedMethods( false )
                .outputType( OpenapiGeneratorSettings.Type.JSON )
                .build() );
    }

    public OpenAPI build() {
        api.info( createInfo( title, description ) );
        addSecuritySchema();
        return api;
    }

    // see https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.0.0.md#securitySchemeObject
    // and https://www.baeldung.com/openapi-jwt-authentication
    private void addSecuritySchema( ) {
        SecurityScheme securityScheme = new SecurityScheme()
            .name( "Bearer Authentication" )
            .type( SecurityScheme.Type.HTTP ) // "apiKey", "http", "oauth2", "openIdConnect"
            .description( "In order to use the method you have to be authorised" )
            .scheme( "bearer" ) //see https://www.rfc-editor.org/rfc/rfc7235#section-5.1
            .bearerFormat( SECURITY_SCHEMA_NAME );
        api.schemaRequirement( SECURITY_SCHEMA_NAME, securityScheme );
    }

    private Set<String> processedClasses = new HashSet<>();
    private Set<String> uniqueTags = new HashSet<>();
    private Set<String> uniqueVersions = new HashSet<>();

    public enum Result {
        PROCESSED_OK( "processed." ),
        SKIPPED_DUE_TO_ALREADY_PROCESSED( "has already been processed." ),
        SKIPPED_DUE_TO_CLASS_IS_NOT_WEB_SERVICE( "skipped due to class does not contain @WSMethod annotated methods" );

        private String description;

        Result( String description ) {
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    public Result processWebservice( Class clazz, String context ) {
        if ( !processedClasses.add( clazz.getCanonicalName() ) ) {
            return Result.SKIPPED_DUE_TO_ALREADY_PROCESSED;
        }
        var r = Reflect.reflect( clazz );
        var tag = createTag( tag( r ) );
        if ( uniqueTags.add( tag.getName() ) ) {
            api.addTagsItem( tag );
        }
        if ( uniqueVersions.add( r.getType().getTypeName() ) ) {
            versions.put( r.getClass().getPackage().getImplementationVersion(), r.getType().getTypeName() );
        }
        List<Reflection.Method> methods = r.methods;
        methods.sort( comparing( Reflection.Method::name ) );
        boolean webServiceValid = false;
        for( Reflection.Method method : methods ) {
            if( !filterMethod( method ) ) {
                continue;
            }
            if( settings.isProcessOnlyAnnotatedMethods() && method.findAnnotation( WsMethod.class ).isEmpty() ) {
                continue;
            }
            webServiceValid = true;
            var wsMethodDescriptor = new WsMethodDescriptor( method );
            var wsSecurityDescriptor = WsSecurityDescriptor.ofMethod( method );
            var paths = getPaths();
            var pathString = path( context, wsMethodDescriptor.path );
            var pathItem = getPathItem( pathString, paths );

            var operation = prepareOperation( method, wsMethodDescriptor, wsSecurityDescriptor, tag );

            for( HttpServerExchange.HttpMethod httpMethod : wsMethodDescriptor.methods ) {
                pathItem.operation( convertMethod( httpMethod ), operation );
            }
        }
        if ( !webServiceValid ) {
            return Result.SKIPPED_DUE_TO_CLASS_IS_NOT_WEB_SERVICE;
        }
        return Result.PROCESSED_OK;
    }

    private Operation prepareOperation( Reflection.Method method,
                                        WsMethodDescriptor wsMethodDescriptor,
                                        WsSecurityDescriptor wsSecurityDescriptor,
                                        Tag tag ) {
        var params = Lists.filter( method.parameters, WsApiReflectionUtils::filterParameter );
        var returnType = prepareType( method.returnType() );

        Operation operation = new Operation();
        operation.addTagsItem( tag.getName() );
        operation.setOperationId( wsMethodDescriptor.id );
        operation.setParameters( prepareParameters( params ) );
        operation.description( wsMethodDescriptor.description );
        operation.setRequestBody( prepareRequestBody( params ) );
        operation.setResponses( prepareResponse( returnType, wsMethodDescriptor.produces ) );
        Optional<Deprecated> deprecated = method.findAnnotation( Deprecated.class );
        deprecated.ifPresent( x -> operation.deprecated( true ) );
        if ( wsSecurityDescriptor != WsSecurityDescriptor.NO_SECURITY_SET ) {
            operation.addSecurityItem( new SecurityRequirement().addList( SECURITY_SCHEMA_NAME ) );
            String descriptionWithAuth = operation.getDescription();
            if ( descriptionWithAuth.length() > 0 ) {
                descriptionWithAuth += "\n    Note: \n- security permissions: "
                    + "\n  - " + Joiner.on( "\n  - " ).join( wsSecurityDescriptor.permissions )
                    + "\n- realm: " + wsSecurityDescriptor.realm;
                operation.description( descriptionWithAuth );
            }
            SecurityRequirement securityRequirement = new SecurityRequirement();
            securityRequirement.addList( wsSecurityDescriptor.realm, Arrays.asList( wsSecurityDescriptor.permissions ) );
            operation.addSecurityItem( securityRequirement );
        }
        return operation;
    }

    private ApiResponses prepareResponse( Type type, String produces ) {
        var responses = new ApiResponses();
        ApiResponse response = new ApiResponse();
        responses.addApiResponse( "200", response );

        var resolvedSchema = prepareSchema( type, api );
        response.description( "" );
        if ( type.equals( Void.class ) ) {
            return responses;
        }
        Map<String, Schema> schemas = api.getComponents() == null ? Collections.emptyMap() : api.getComponents().getSchemas();
        response.content( createContent( produces, createSchemaRef( resolvedSchema.schema, schemas ) ) );
        return responses;
    }

    private RequestBody prepareRequestBody( List<Reflection.Parameter> parameters ) {
        return parameters.stream()
            .filter( item -> from( item ).equals( WsParam.From.BODY.name().toLowerCase() ) )
            .map( item -> createBody( item ) )
            .findFirst().orElse( null );
    }

    private List<Parameter> prepareParameters( List<Reflection.Parameter> parameters ) {
        return parameters.stream()
            .filter( item -> !from( item ).equals( WsParam.From.BODY.name().toLowerCase() ) )
            .map( this::createParameter )
            .toList();
    }

    private RequestBody createBody( Reflection.Parameter parameter ) {
        var resolvedSchema = prepareSchema( prepareType( parameter.type() ), api );
        var schemas = api.getComponents() == null ? Collections.<String, Schema>emptyMap() : api.getComponents().getSchemas();

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
        } else if( WsParam.From.QUERY.name().toLowerCase().equals( from ) && !parameter.type().isOptional() ) {
            result.setRequired( true );
        }
        String description = description( parameter );
        if( description.trim().length() > 0 ) result.description( description );
        var resolvedSchema = this.converters.readAllAsResolvedSchema( prepareType( parameter.type() ) );
        if( resolvedSchema != null ) {
            result.setSchema( resolvedSchema.schema );
        }
        return result;
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

    private Paths getPaths( ) {
        var paths = api.getPaths();
        if( paths == null ) {
            paths = new Paths();
            api.setPaths( paths );
        }
        return paths;
    }

    public Info createInfo( String title, String description ) {
        Info info = new Info();
        info.setTitle( title );
        info.setDescription( description );
        List<String> webServiceVersions = new ArrayList<>();
        versions.asMap().forEach( ( key, value ) -> {
            webServiceVersions.add( key + " (" + Joiner.on( ", " ).skipNulls().join( value ) + ")" );
        } );
        info.setVersion( Joiner.on( ", " ).join( webServiceVersions ) );
        return info;
    }
}
