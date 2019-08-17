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
import oap.http.Handler;
import oap.http.HttpResponse;
import oap.http.HttpServer;
import oap.http.Protocol;
import oap.http.cors.CorsPolicy;
import oap.json.Binder;
import oap.util.Lists;
import oap.ws.interceptor.Interceptor;
import org.apache.http.entity.ContentType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class WebServices {
    static {
        HttpResponse.registerProducer( ContentType.APPLICATION_JSON.getMimeType(), Binder.json::marshal );
    }

    final HashMap<String, Integer> exceptionToHttpCode = new HashMap<>();
    private final List<WsConfig> wsConfigs;
    private final HttpServer server;
    private final SessionManager sessionManager;
    private final CorsPolicy globalCorsPolicy;
    private final Kernel kernel;
    public final Map<String, Object> services = new HashMap<>();

    public WebServices( Kernel kernel, HttpServer server, SessionManager sessionManager, CorsPolicy globalCorsPolicy ) {
        this( kernel, server, sessionManager, globalCorsPolicy, WsConfig.CONFIGURATION.fromClassPath() );
    }

    public WebServices( Kernel kernel, HttpServer server, SessionManager sessionManager, CorsPolicy globalCorsPolicy, WsConfig... wsConfigs ) {
        this( kernel, server, sessionManager, globalCorsPolicy, List.of( wsConfigs ) );
    }

    public WebServices( Kernel kernel, HttpServer server, SessionManager sessionManager, CorsPolicy globalCorsPolicy, List<WsConfig> wsConfigs ) {
        this.kernel = kernel;
        this.wsConfigs = wsConfigs;
        this.server = server;
        this.sessionManager = sessionManager;
        this.globalCorsPolicy = globalCorsPolicy;
    }

    public void start() {
        log.info( "binding web services..." );


        for( var config : wsConfigs ) {
            log.trace( "config = {}", config );

            if( !KernelHelper.profileEnabled( config.profiles, kernel.profiles ) ) {
                log.debug( "skipping " + config.name + " web configuration initialization with "
                    + "service profiles " + config.profiles );
                continue;
            }

            var interceptors = Lists.map( config.interceptors, kernel::<Interceptor>serviceOrThrow );

            for( var entry : config.services.entrySet() ) {
                var serviceConfig = entry.getValue();

                log.trace( "service = {}", entry );

                if( !KernelHelper.profileEnabled( serviceConfig.profiles, kernel.profiles ) ) {
                    log.debug( "skipping " + entry.getKey() + " web service initialization with "
                        + "service profiles " + serviceConfig.profiles );
                    continue;
                }

                var corsPolicy = serviceConfig.corsPolicy != null ? serviceConfig.corsPolicy : globalCorsPolicy;
                bind( entry.getKey(), corsPolicy, kernel.serviceOrThrow( serviceConfig.service ),
                    serviceConfig.sessionAware, sessionManager, interceptors, serviceConfig.protocol );
            }

            for( var entry : config.handlers.entrySet() ) {
                var handlerConfig = entry.getValue();
                log.trace( "handler = {}", entry );

                var corsPolicy = handlerConfig.corsPolicy != null ? handlerConfig.corsPolicy : globalCorsPolicy;

                Protocol protocol = handlerConfig.protocol;
                bind( entry.getKey(), corsPolicy, kernel.serviceOrThrow( handlerConfig.service ), protocol );
            }
        }
    }


    public void stop() {
        for( var config : wsConfigs ) {
            config.handlers.keySet().forEach( server::unbind );
            config.services.keySet().forEach( server::unbind );
        }
    }

    public void bind( String context, CorsPolicy corsPolicy, Object service, boolean sessionAware,
                      SessionManager sessionManager, List<Interceptor> interceptors, Protocol protocol ) {
        services.put( context, service );
        bind( context, corsPolicy, new WebService( service, sessionAware, sessionManager, interceptors, exceptionToHttpCode ), protocol );
    }

    public void bind( String context, CorsPolicy corsPolicy, Handler handler, Protocol protocol ) {
        server.bind( context, corsPolicy, handler, protocol );
    }
}
