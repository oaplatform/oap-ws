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
import oap.application.Kernel;
import oap.application.KernelHelper;
import oap.application.module.ServiceExt;
import oap.http.HttpResponse;
import oap.http.Protocol;
import oap.http.cors.CorsPolicy;
import oap.http.server.Handler;
import oap.http.server.HttpServer;
import oap.json.Binder;
import oap.util.Lists;
import oap.ws.interceptor.Interceptor;
import org.apache.http.entity.ContentType;

import java.util.LinkedHashMap;
import java.util.List;

@Slf4j
public class WebServices {
    static {
        HttpResponse.registerProducer( ContentType.APPLICATION_JSON.getMimeType(), Binder.json::marshal );
    }

    public final LinkedHashMap<String, Object> services = new LinkedHashMap<>();
    private final HttpServer server;
    private final SessionManager sessionManager;
    private final CorsPolicy globalCorsPolicy;
    private final Kernel kernel;
    private List<ServiceExt<WsConfig>> wsConfigServices;
    private List<ServiceExt<WsConfig>> wsConfigHandlers;

    public WebServices( Kernel kernel, HttpServer server, SessionManager sessionManager, CorsPolicy globalCorsPolicy ) {
        this.kernel = kernel;
        this.server = server;
        this.sessionManager = sessionManager;
        this.globalCorsPolicy = globalCorsPolicy;
    }

    public void start() {
        log.info( "binding web services..." );

        wsConfigServices = kernel.servicesByExt( "ws-service", WsConfig.class );
        wsConfigHandlers = kernel.servicesByExt( "ws-handler", WsConfig.class );

        log.info( "ws-service: {}", Lists.map( wsConfigServices, ws -> ws.name ) );
        log.info( "ws-handler: {}", Lists.map( wsConfigServices, ws -> ws.name ) );

        for( var config : wsConfigServices ) {
            log.trace( "service: module = {}, config = {}", config.module.name, config.ext );

            log.trace( "service = {}", config.ext );
            var interceptors = Lists.map( config.ext.interceptors, ( String name ) -> kernel.<Interceptor>service( name )
                .orElseThrow( () -> new RuntimeException( "interceptor " + name + " not found" ) ) );
            if( !KernelHelper.profileEnabled( config.ext.profiles, kernel.profiles ) ) {
                log.debug( "skipping " + config.module.name + "." + config.name + " web service initialization with "
                    + "service profiles " + config.ext.profiles );
                continue;
            }

            var corsPolicy = config.ext.corsPolicy != null ? config.ext.corsPolicy : globalCorsPolicy;
            for( var path : config.ext.path ) {
                bind( path, corsPolicy, config.getInstance(),
                    config.ext.sessionAware, sessionManager, interceptors, config.ext.protocol );
            }
        }

        for( var config : wsConfigHandlers ) {
            log.trace( "handler = {}", config );

            var corsPolicy = config.ext.corsPolicy != null ? config.ext.corsPolicy : globalCorsPolicy;
            Protocol protocol = config.ext.protocol;
            for( var path : config.ext.path ) {
                bind( path, corsPolicy, ( Handler ) config.getInstance(), protocol );
            }
        }
    }


    public void stop() {
        if( wsConfigServices != null ) {

            for( var config : wsConfigServices )
                for( var path : config.ext.path )
                    server.unbind( path );
            wsConfigServices = null;
        }

        if( wsConfigHandlers != null ) {
            for( var config : wsConfigHandlers )
                for( var path : config.ext.path )
                    server.unbind( path );

            wsConfigHandlers = null;
        }

    }

    public void bind( String context, CorsPolicy corsPolicy, Object service, boolean sessionAware,
                      SessionManager sessionManager, List<Interceptor> interceptors, Protocol protocol ) {
        services.put( context, service );
        bind( context, corsPolicy, new WebService( service, sessionAware, sessionManager, interceptors ), protocol );
    }

    public void bind( String context, CorsPolicy corsPolicy, Handler handler, Protocol protocol ) {
        server.bind( context, corsPolicy, handler, protocol );
    }
}
