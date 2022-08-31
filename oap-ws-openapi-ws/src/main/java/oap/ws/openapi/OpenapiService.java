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

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import oap.ws.WebServices;

import java.security.Permission;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class OpenapiService {

    private final WebServices webServices;
    public ApiInfo info;

    public OpenapiService( WebServices webServices ) {
        this.webServices = webServices;
    }

    public OpenapiService( WebServices webServices, ApiInfo info ) {
        this( webServices );
        this.info = info;
    }

    public OpenAPI generateOpenApi() {
        OpenapiGeneratorSettings settings = OpenapiGeneratorSettings
            .builder()
            .processOnlyAnnotatedMethods( false )
            .outputType( OpenapiGeneratorSettings.Type.JSON )
            .build();
        OpenapiGenerator openapiGenerator = new OpenapiGenerator( info.title, info.description, settings );
        log.info( "OpenAPI generating '{}'...", info.title );
        for( Map.Entry<String, Object> ws : webServices.services.entrySet() ) {
            log.info( "Processing web-service {}...", ws.getKey() );
            Class clazz = ws.getValue().getClass();
            String context = ws.getKey();
            log.info( "Processing web-service implementation class '{}'", clazz.getCanonicalName() );
            openapiGenerator.processWebservice( clazz, context );
        }

        return openapiGenerator.build();
    }


    public Set<Permission> preparePermissions() {
        final OpenAPI openAPI = generateOpenApi();
        final Paths paths = openAPI.getPaths();
//        final List<String> collect = paths.values().stream().flatMap( path -> coalesce( path.getGet() ).stream() ).collect( Collectors.toList() );
//        System.out.println( collect );
//
//        final List<Stream<String>> streams = paths.values().stream().flatMap( pathItem -> pathItem.getGet().getSecurity().stream() )
//            .flatMap( securityRequirement -> securityRequirement.values().stream().map( Collection::stream ) ).collect( Collectors.toList() );
        return null;
    }

}