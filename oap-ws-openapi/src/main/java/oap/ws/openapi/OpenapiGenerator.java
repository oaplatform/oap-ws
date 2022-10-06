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
import io.swagger.v3.core.util.Yaml;
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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.http.server.nio.HttpServerExchange;
import oap.io.content.ContentWriter;
import oap.reflect.Reflect;
import oap.reflect.Reflection;
import oap.util.Lists;
import oap.util.Strings;
import oap.ws.WsMethod;
import oap.ws.WsMethodDescriptor;
import oap.ws.WsParam;
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
import static oap.ws.openapi.OpenapiReflection.description;
import static oap.ws.openapi.OpenapiReflection.from;
import static oap.ws.openapi.OpenapiReflection.tag;
import static oap.ws.openapi.OpenapiReflection.webMethod;

import static oap.ws.openapi.OpenapiSchema.prepareType;

/**
 * Common procedure:
 *
 * OpenapiGenerator gen = new OpenapiGenerator(...);
 * gen.beforeProcesingServices();
 * gen.processWebservice(...);
 * gen.afterProcesingServices();
 * gem.build();
 */
@Slf4j
public class OpenapiGenerator {
    public static final String OPEN_API_VERSION = "3.0.3";
    public static final String SECURITY_SCHEMA_NAME = "JWT";
    private final ArrayListMultimap<String, String> versions = ArrayListMultimap.create();
    private final ModelConverters converters = new ModelConverters();
    private final OpenAPI api = new OpenAPI();
    private final OpenapiSchema openapiSchema = new OpenapiSchema();
    @Setter
    private String title;
    @Setter
    private String description;
    @Getter
    private final Settings settings;
    @Setter
    private OpenapiGenerationCallbackProcessor callbackProcessor = new OpenapiGenerationCallbackProcessor() {};

    public OpenapiGenerator( String title, String description, Settings settings ) {
        this.title = title;
        this.description = description;
        this.settings = settings;
        api.openapi( OPEN_API_VERSION );
    }

    public OpenapiGenerator( String title, String description ) {
        this( title, description, new Settings( Settings.OutputType.JSON, false ) );
    }

    public OpenAPI build() {
        addSecuritySchema();
        return api;
    }

    // see https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.0.0.md#securitySchemeObject
    // and https://www.baeldung.com/openapi-jwt-authentication
    private void addSecuritySchema() {
        SecurityScheme securityScheme = new SecurityScheme()
            .name( "Bearer Authentication" )
            .type( SecurityScheme.Type.HTTP ) // "apiKey", "http", "oauth2", "openIdConnect"
            .description( "In order to use the method you have to be authorised" )
            .scheme( "bearer" ) //see https://www.rfc-editor.org/rfc/rfc7235#section-5.1
            .bearerFormat( SECURITY_SCHEMA_NAME );
        api.schemaRequirement( SECURITY_SCHEMA_NAME, securityScheme );
    }

    private final Set<String> processedClasses = new HashSet<>();
    private final Set<String> uniqueTags = new HashSet<>();
    private final Set<String> uniqueVersions = new HashSet<>();

    public enum Result {
        PROCESSED_OK( "processed." ),
        SKIPPED_DUE_TO_ALREADY_PROCESSED( "has already been processed." ),
        SKIPPED_DUE_TO_CLASS_IS_NOT_WEB_SERVICE( "skipped due to class does not contain @WsMethod annotated methods" );

        private final String description;

        Result( String description ) {
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    public Result processWebservice( Class<?> clazz, String context ) {
        log.info( "Processing web-service {} implementation class '{}' ...", context, clazz.getCanonicalName() );
        if( !processedClasses.add( clazz.getCanonicalName() ) ) return Result.SKIPPED_DUE_TO_ALREADY_PROCESSED;
        var r = Reflect.reflect( clazz );
        var tag = createTag( tag( r ) );
        if( uniqueTags.add( tag.getName() ) ) api.addTagsItem( tag );
        if( uniqueVersions.add( r.getType().getTypeName() ) )
            versions.put(
                clazz.getPackage().getImplementationVersion() != null
                    ? clazz.getPackage().getImplementationVersion()
                    : Strings.UNDEFINED, r.getType().getTypeName() );
        List<oap.reflect.Reflection.Method> methods = r.methods.stream().sorted( comparing( oap.reflect.Reflection.Method::name ) ).toList();
        boolean webServiceValid = false;
        for( oap.reflect.Reflection.Method method : methods ) {
            if( !webMethod( method ) ) {
                callbackProcessor.doIfMethodIsNotWebservice( method );
                continue;
            }
            if( settings.processOnlyAnnotatedMethods && method.findAnnotation( WsMethod.class ).isEmpty() ) {
                callbackProcessor.doIfMethodHasNoAnnotation( method );
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
                callbackProcessor.doForHttpMethod( httpMethod, operation, method );
                pathItem.operation( convertMethod( httpMethod ), operation );
            }
        }
        if( !webServiceValid ) {
            callbackProcessor.doIfWebserviceIsNotValid( methods );
            return Result.SKIPPED_DUE_TO_CLASS_IS_NOT_WEB_SERVICE;
        }
        return Result.PROCESSED_OK;
    }

    private Operation prepareOperation( oap.reflect.Reflection.Method method,
                                        WsMethodDescriptor wsMethodDescriptor,
                                        WsSecurityDescriptor wsSecurityDescriptor,
                                        Tag tag ) {
        var params = Lists.filter( method.parameters, OpenapiReflection::webParameter );
        var returnType = prepareType( method.returnType() );

        Operation operation = new Operation();
        operation.addTagsItem( tag.getName() );
        operation.setOperationId( wsMethodDescriptor.id );
        operation.setParameters( prepareParameters( params ) );
        operation.description( wsMethodDescriptor.description );
        operation.setRequestBody( prepareRequestBody( params ) );
        operation.setResponses( prepareResponse( method, returnType, wsMethodDescriptor.produces ) );
        Optional<Deprecated> deprecated = method.findAnnotation( Deprecated.class );
        deprecated.ifPresent( x -> operation.deprecated( true ) );
        if( wsSecurityDescriptor != WsSecurityDescriptor.NO_SECURITY_SET ) {
            operation.addSecurityItem( new SecurityRequirement().addList( SECURITY_SCHEMA_NAME ) );
            String descriptionWithAuth = operation.getDescription();
            if( descriptionWithAuth.length() > 0 ) {
                descriptionWithAuth += "\n    Note: \n- security permissions: "
                    + "\n  - " + String.join( "\n  - ", wsSecurityDescriptor.permissions )
                    + "\n- realm: " + wsSecurityDescriptor.realm;
                operation.description( descriptionWithAuth );
            }
            SecurityRequirement securityRequirement = new SecurityRequirement();
            securityRequirement.addList( wsSecurityDescriptor.realm, Arrays.asList( wsSecurityDescriptor.permissions ) );
            operation.addSecurityItem( securityRequirement );
        }
        return operation;
    }

    private ApiResponses prepareResponse( Reflection.Method method, Type returnType, String produces ) {
        var responses = new ApiResponses();
        ApiResponse response = new ApiResponse();
        responses.addApiResponse( "200", response );
        var resolvedSchema = openapiSchema.prepareSchema( returnType, api );
        response.description( "" );
        if( returnType.equals( Void.class ) ) return responses;
        Map<String, Schema> schemas = api.getComponents() == null
            ? Collections.emptyMap()
            : api.getComponents().getSchemas();
        response.content( createContent( produces, openapiSchema.createSchemaRef( resolvedSchema.schema, schemas ) ) );
        return responses;
    }

    private RequestBody prepareRequestBody( List<oap.reflect.Reflection.Parameter> parameters ) {
        return parameters.stream()
            .filter( item -> from( item ).equals( WsParam.From.BODY.name().toLowerCase() ) )
            .map( this::createBody )
            .findFirst().orElse( null );
    }

    private List<Parameter> prepareParameters( List<oap.reflect.Reflection.Parameter> parameters ) {
        return parameters.stream()
            .filter( item -> !from( item ).equals( WsParam.From.BODY.name().toLowerCase() ) )
            .map( this::createParameter )
            .toList();
    }

    private RequestBody createBody( oap.reflect.Reflection.Parameter parameter ) {
        var resolvedSchema = openapiSchema.prepareSchema( prepareType( parameter.type() ), api );
        var schemas = api.getComponents() == null
            ? Collections.<String, Schema>emptyMap()
            : api.getComponents().getSchemas();
        var result = new RequestBody();
        result.setContent( createContent( ContentType.APPLICATION_JSON.getMimeType(),
            openapiSchema.createSchemaRef( resolvedSchema.schema, schemas ) ) );
        if( schemas.containsKey( resolvedSchema.schema.getName() ) )
            api.getComponents().addRequestBodies( resolvedSchema.schema.getName(), result );

        return result;
    }

    private Parameter createParameter( oap.reflect.Reflection.Parameter parameter ) {
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

    private Paths getPaths() {
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
        versions.asMap().forEach( ( key, value ) -> webServiceVersions.add( key + " (" + Joiner.on( ", " ).skipNulls().join( value ) + ")" ) );
        info.setVersion( String.join( ", ", webServiceVersions ) );
        return info;
    }

    @EqualsAndHashCode
    @ToString
    public static class Settings {
        /**
         * This trigger HSON or YAML output file.
         */
        public final OutputType outputType;

        /**
         * if trus, only methods annotated with @WsMethod will be processed.
         */
        public final boolean processOnlyAnnotatedMethods;

        public Settings( OutputType outputType, boolean processOnlyAnnotatedMethods ) {
            this.outputType = outputType;
            this.processOnlyAnnotatedMethods = processOnlyAnnotatedMethods;
        }

        public enum OutputType {
            YAML( ".yaml", new ContentWriter<>() {
                @Override
                @SneakyThrows
                public byte[] write( OpenAPI object ) {
                    return Yaml.mapper().writeValueAsBytes( object );
                }
            } ),
            JSON( ".json", ContentWriter.ofJson() );

            public final String fileExtension;
            public final ContentWriter<OpenAPI> writer;

            OutputType( String fileExtension, ContentWriter<OpenAPI> writer ) {
                this.fileExtension = fileExtension;
                this.writer = writer;
            }
        }
    }

    public void beforeProcesingServices() {
        log.info( "OpenAPI generating '{}'...", title );
    }

    public void afterProcesingServices() {
        try {
            api.getComponents().getSchemas().forEach( ( className, parentSchema ) -> {
                if( "object".equals( parentSchema.getType() ) && parentSchema.getProperties() != null ) {
                    parentSchema.getProperties().forEach( ( name, childSchema ) -> {
                        var fieldName = ( String ) name;
                        var schema = ( Schema ) childSchema;
                        if( !Strings.isEmpty( schema.get$ref() ) ) {
                            openapiSchema.processExtensionsInSchemas( schema, className, fieldName );
                        }
                    } );
                }
            } );
        } catch( Exception ex ) {
            log.error( "Cannot process extensions in schemas", ex );
            callbackProcessor.doIfException( ex );
        }
        api.info( createInfo( title, description ) );
    }
}
