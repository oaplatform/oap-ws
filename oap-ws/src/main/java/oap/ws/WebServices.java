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
import oap.application.Configuration.ConfigurationWithURL;
import oap.application.Kernel;
import oap.application.KernelHelper;
import oap.http.HttpResponse;
import oap.http.Protocol;
import oap.http.cors.CorsPolicy;
import oap.http.server.Handler;
import oap.http.server.HttpServer;
import oap.json.Binder;
import oap.util.Lists;
import oap.ws.interceptor.Interceptor;
import org.apache.http.entity.ContentType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class WebServices {
    static {
        HttpResponse.registerProducer( ContentType.APPLICATION_JSON.getMimeType(), Binder.json::marshal );
    }

    public final Map<String, Object> services = new HashMap<>();
    private final List<ConfigurationWithURL<WsConfig>> wsConfigs;
    private final HttpServer server;
    private final SessionManager sessionManager;
    private final CorsPolicy globalCorsPolicy;
    private final Kernel kernel;

    public WebServices( Kernel kernel, HttpServer server, SessionManager sessionManager, CorsPolicy globalCorsPolicy ) {
        this( kernel, server, sessionManager, globalCorsPolicy, WsConfig.CONFIGURATION.fromClassPath() );
    }

    public WebServices( Kernel kernel, HttpServer server, SessionManager sessionManager, CorsPolicy globalCorsPolicy, WsConfig... wsConfigs ) {
        this( kernel, server, sessionManager, globalCorsPolicy, Lists.map( wsConfigs, wsc -> new ConfigurationWithURL<>( wsc, Optional.empty() ) ) );
    }

    public WebServices( Kernel kernel, HttpServer server, SessionManager sessionManager, CorsPolicy globalCorsPolicy, List<ConfigurationWithURL<WsConfig>> wsConfigs ) {
        this.kernel = kernel;
        this.wsConfigs = wsConfigs;
        this.server = server;
        this.sessionManager = sessionManager;
        this.globalCorsPolicy = globalCorsPolicy;
    }

    public void start() {
        log.info( "binding web services..." );


        for( var configWithURL : wsConfigs ) {
            log.trace( "config = {}", configWithURL );

            var config = configWithURL.configuration;

            if( !KernelHelper.profileEnabled( config.profiles, kernel.profiles ) ) {
                log.debug( "skipping " + config.name + " web configuration initialization with "
                    + "service profiles " + config.profiles );
                continue;
            }

            config.services.forEach( ( serviceName, serviceConfig ) -> {
                log.trace( "service = {}", serviceConfig );
                var interceptors = Lists.map( serviceConfig.interceptors, ( String name ) -> kernel.<Interceptor>service( name )
                    .orElseThrow( () -> new RuntimeException( "interceptor " + name + " not found" ) ) );
                if( !KernelHelper.profileEnabled( serviceConfig.profiles, kernel.profiles ) ) {
                    log.debug( "skipping " + serviceName + " web service initialization with "
                        + "service profiles " + serviceConfig.profiles );
                    return;
                }
                var corsPolicy = serviceConfig.corsPolicy != null ? serviceConfig.corsPolicy : globalCorsPolicy;
                bind( serviceName, corsPolicy, kernel.service( serviceConfig.service ).orElseThrow(),
                    serviceConfig.sessionAware, sessionManager, interceptors, serviceConfig.protocol );
            } );

            config.handlers.forEach( ( handlerName, handlerConfig ) -> {
                log.trace( "handler = {}", handlerConfig );
                var corsPolicy = handlerConfig.corsPolicy != null ? handlerConfig.corsPolicy : globalCorsPolicy;
                Protocol protocol = handlerConfig.protocol;
                bind( handlerName, corsPolicy, kernel.<Handler>service( handlerConfig.service )
                    .orElseThrow( () -> new ApplicationException( "service " + handlerConfig.service + " not found." ) ), protocol );
            } );
        }
    }


    public void stop() {
        for( var configWithURL : wsConfigs ) {
            var config = configWithURL.configuration;

            config.handlers.keySet().forEach( server::unbind );
            config.services.keySet().forEach( server::unbind );
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
