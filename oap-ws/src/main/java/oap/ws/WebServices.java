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
import oap.application.ApplicationException;
import oap.application.Kernel;
import oap.application.KernelHelper;
import oap.application.ModuleExt;
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
    private List<ModuleExt<WsConfig>> wsConfigs;

    public WebServices( Kernel kernel, HttpServer server, SessionManager sessionManager, CorsPolicy globalCorsPolicy ) {
        this.kernel = kernel;
        this.server = server;
        this.sessionManager = sessionManager;
        this.globalCorsPolicy = globalCorsPolicy;
    }

    public void start() {
        log.info( "binding web services..." );

        wsConfigs = kernel.modulesByExt( "ws", WsConfig.class );

        for( var config : wsConfigs ) {
            log.trace( "module = {}, config = {}", config.module.name, config.ext );

            config.ext.services.forEach( ( serviceName, serviceConfig ) -> {
                log.trace( "service = {}", serviceConfig );
                var interceptors = Lists.map( serviceConfig.interceptors, ( String name ) -> kernel.<Interceptor>service( name )
                    .orElseThrow( () -> new RuntimeException( "interceptor " + name + " not found" ) ) );
                if( !KernelHelper.profileEnabled( serviceConfig.profiles, kernel.profiles ) ) {
                    log.debug( "skipping " + config.module.name + "." + serviceName + " web service initialization with "
                        + "service profiles " + serviceConfig.profiles );
                    return;
                }

                if( !config.containsService( serviceName ) ) {
                    throw new ApplicationException( "service " + config.module.name + "." + serviceName + " not found." );
                }

                if( config.isServiceInitialized( serviceName ) ) {
                    var corsPolicy = serviceConfig.corsPolicy != null ? serviceConfig.corsPolicy : globalCorsPolicy;
                    for( var path : serviceConfig.path ) {
                        bind( path, corsPolicy, config.getInstance( serviceName ),
                            serviceConfig.sessionAware, sessionManager, interceptors, serviceConfig.protocol );
                    }
                } else {
                    log.debug( "skipping " + config.module.name + "." + serviceName + " web service initialization. [service disabled]" );
                }
            } );

            config.ext.handlers.forEach( ( handlerName, handlerConfig ) -> {
                log.trace( "handler = {}", handlerConfig );

                if( !config.containsService( handlerName ) ) {
                    throw new ApplicationException( "handler " + config.module.name + "." + handlerName + " not found." );
                }

                if( config.isServiceInitialized( handlerName ) ) {
                    var corsPolicy = handlerConfig.corsPolicy != null ? handlerConfig.corsPolicy : globalCorsPolicy;
                    Protocol protocol = handlerConfig.protocol;
                    for( var path : handlerConfig.path ) {
                        bind( path, corsPolicy, ( Handler ) config.getInstance( handlerName ), protocol );
                    }
                } else {
                    log.debug( "skipping " + config.module.name + "." + handlerName + " web handler initialization. [service disabled]" );
                }
            } );
        }
    }


    public void stop() {
        if( wsConfigs == null ) return;

        for( var config : wsConfigs ) {
            for( var handler : config.ext.handlers.values() )
                for( var path : handler.path )
                    server.unbind( path );

            for( var service : config.ext.services.values() )
                for( var path : service.path )
                    server.unbind( path );
        }
        wsConfigs = null;
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
