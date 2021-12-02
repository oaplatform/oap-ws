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

import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.CookieImpl;
import lombok.extern.slf4j.Slf4j;
import oap.http.ContentTypes;
import oap.http.HttpStatusCodes;
import oap.http.server.nio.HttpHandler;
import oap.http.server.nio.HttpServerExchange;
import oap.json.Binder;
import oap.json.JsonException;
import oap.reflect.ReflectException;
import oap.reflect.Reflection;
import oap.util.Result;
import oap.util.Throwables;
import oap.ws.interceptor.Interceptor;
import oap.ws.interceptor.Interceptors;
import oap.ws.validate.ValidationErrors;
import oap.ws.validate.Validators;
import org.joda.time.DateTime;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

@Slf4j
public class WebService implements HttpHandler {
    private final boolean sessionAware;
    private final SessionManager sessionManager;
    private final List<Interceptor> interceptors;
    private final Object instance;
    private final WsMethodMatcher methodMatcher;

    public WebService( Object instance, boolean sessionAware,
                       SessionManager sessionManager, List<Interceptor> interceptors ) {
        this.instance = instance;
        this.methodMatcher = new WsMethodMatcher( instance.getClass() );
        this.sessionAware = sessionAware;
        this.sessionManager = sessionManager;
        this.interceptors = interceptors;
    }

    private void wsError( HttpServerExchange exchange, Throwable e ) {
        if( e instanceof ReflectException && e.getCause() != null )
            wsError( exchange, e.getCause() );
        else if( e instanceof InvocationTargetException )
            wsError( exchange, ( ( InvocationTargetException ) e ).getTargetException() );
        else if( e instanceof WsClientException ) {
            var clientException = ( WsClientException ) e;
            log.debug( this + ": " + clientException, clientException );
            exchange.setStatusCodeReasonPhrase( clientException.code, e.getMessage() );
            if( !clientException.errors.isEmpty() )
                exchange.responseJson( new ValidationErrors.ErrorResponse( clientException.errors ) );
        } else {
            log.error( this + ": " + e.toString(), e );
            exchange.responseJson( HttpStatusCodes.INTERNAL_SERVER_ERROR, e.getMessage(), new JsonStackTraceResponse( e ) );
        }
    }

    @Override
    public void handleRequest( HttpServerExchange exchange ) {
        try {
            var requestLine = exchange.getRelativePath();
            var method = methodMatcher.findMethod( requestLine, exchange.getRequestMethod() ).orElse( null );
            log.trace( "invoking {} for {}", method, requestLine );
            if( method != null ) {
                Session session = null;
                if( sessionAware ) {
                    String cookie = exchange.getRequestCookieValue( SessionManager.COOKIE_ID );
                    log.trace( "session cookie {}", cookie );
                    session = sessionManager.getOrInit( cookie );
                    log.trace( "session for {} is {}", this, session );
                }

                handleInternal( exchange, method, session );
            } else {
                log.trace( "[{}] not found", requestLine );
                exchange.responseNotFound();
            }

        } catch( Throwable e ) {
            wsError( exchange, e );
        }
    }

    private void handleInternal( HttpServerExchange exchange, Reflection.Method method, Session session ) {
        log.trace( "{}: session: [{}]", this, session );

        var wsMethod = method.findAnnotation( WsMethod.class );

        if( !Interceptors.before( interceptors, exchange, session, method ) ) {
            var parameters = method.parameters;
            var originalValues = getOriginalValues( session, parameters, exchange, wsMethod );

            var rb = ValidationErrors.empty()
                .validateParameters( originalValues, method, instance, true )
                .ifEmpty( exchange, () -> Validators.forMethod( method, instance, true )
                    .validate( originalValues.values().toArray( new Object[0] ), originalValues )
                    .ifEmpty( exchange, () -> {
                        var values = getValues( originalValues );

                        return ValidationErrors.empty()
                            .validateParameters( values, method, instance, false )
                            .ifEmpty( exchange, () -> {
                                var paramValues = values.values().toArray( new Object[0] );

                                return Validators
                                    .forMethod( method, instance, false )
                                    .validate( paramValues, values )
                                    .ifEmpty( exchange, () -> {
                                        if( session != null && !containsCookie( exchange.responseCookies(), SessionManager.COOKIE_ID ) ) {
                                            var cookie = new CookieImpl( SessionManager.COOKIE_ID, session.id )
                                                .setPath( sessionManager.cookiePath )
                                                .setExpires( DateTime.now().plus( sessionManager.cookieExpiration ).toDate() )
                                                .setDomain( sessionManager.cookieDomain )
                                                .setSecure( sessionManager.cookieSecure )
                                                .setHttpOnly( true );

                                            exchange.setResponseCookie( cookie );
                                        }

                                        return produceResultResponse( exchange, method, session, wsMethod, method.invoke( instance, paramValues ) );
                                    } );
                            } );
                    } ) );

            Interceptors.after( interceptors, exchange, session );
        }
    }

    private boolean containsCookie( Iterable<Cookie> cookies, String cookieId ) {
        for( var p : cookies ) {
            if( cookieId.equals( p.getName() ) ) return true;
        }
        return false;
    }

    private boolean produceResultResponse( HttpServerExchange exchange, Reflection.Method method, Session session, Optional<WsMethod> wsMethod, Object result ) {
        boolean isRaw = wsMethod.map( WsMethod::raw ).orElse( false );
        var produces = wsMethod.map( WsMethod::produces )
            .orElse( ContentTypes.APPLICATION_JSON );


        if( method.isVoid() ) {
            exchange.responseNoContent();
        } else if( result instanceof Optional<?> ) {
            var optResult = ( Optional<?> ) result;
            if( optResult.isEmpty() ) exchange.responseNotFound();
            else exchange.responseOk( optResult.get(), isRaw, produces );
        } else if( result instanceof Result<?, ?> ) {
            var resultResult = ( Result<?, ?> ) result;
            if( resultResult.isSuccess() ) {
                exchange.responseOk( resultResult.successValue, isRaw, produces );
            } else {
                exchange.responseJson( HttpStatusCodes.INTERNAL_SERVER_ERROR, "", resultResult.failureValue );
            }
        } else if( result instanceof java.util.stream.Stream<?> ) {
            var stream = ( java.util.stream.Stream<?> ) result;
            exchange.responseStream( stream, isRaw, produces );
        } else {
            exchange.responseOk( result, isRaw, produces );
        }

        return true;
    }

    private LinkedHashMap<Reflection.Parameter, Object> getValues( LinkedHashMap<Reflection.Parameter, Object> values ) {
        try {
            var res = new LinkedHashMap<Reflection.Parameter, Object>();

            values.forEach( ( key, value ) -> {
                Object map = map( key.type(), value );
                res.put( key, map );
            } );

            return res;
        } catch( JsonException e ) {
            throw new WsClientException( e );
        }
    }

    @SuppressWarnings( { "unchecked", "checkstyle:ParameterAssignment" } )
    private Object map( Reflection reflection, Object value ) {
        if( reflection.isOptional() )
            if( ( ( Optional<?> ) value ).isEmpty() ) return Optional.empty();
            else return Optional.ofNullable(
                map( reflection.typeParameters.get( 0 ), ( ( Optional<?> ) value ).orElseThrow() )
            );
        else {
            if( value instanceof Optional ) return map( reflection, ( ( Optional<?> ) value ).orElseThrow() );
            if( reflection.isEnum() ) return Enum.valueOf( ( Class<Enum> ) reflection.underlying, ( String ) value );

            // what is this for? I sincerelly hope there is a test for it.
            if( !( value instanceof String ) && Collection.class.isAssignableFrom( reflection.underlying ) )
                value = Binder.json.marshal( value );
            if( reflection.underlying.isInstance( value ) ) return value;

            return Binder.json.unmarshal( reflection, ( String ) value );
        }
    }

    @Override
    public String toString() {
        return instance.getClass().getName();
    }

    public LinkedHashMap<Reflection.Parameter, Object> getOriginalValues( Session session,
                                                                          List<Reflection.Parameter> parameters,
                                                                          HttpServerExchange exchange,
                                                                          Optional<WsMethod> wsMethod ) {
        var ret = new LinkedHashMap<Reflection.Parameter, Object>();

        for( var parameter : parameters ) {
            Object value = getValue( session, exchange, wsMethod, parameter )
                .orElseGet( () -> WsParams.fromQuery( exchange, parameter ) );
            ret.put( parameter, value );
        }
        return ret;
    }

    public Optional<Object> getValue( Session session,
                                      HttpServerExchange exchange,
                                      Optional<WsMethod> wsMethod,
                                      Reflection.Parameter parameter ) {

        if( parameter.type().assignableFrom( HttpServerExchange.class ) ) return Optional.of( exchange );
        if( parameter.type().assignableFrom( Session.class ) ) return Optional.ofNullable( session );

        WsParam wsParam = parameter.findAnnotation( WsParam.class ).orElse( null );
        if( wsParam == null ) return Optional.empty();

        return Optional.ofNullable( switch( wsParam.from() ) {
            case SESSION -> WsParams.fromSession( session, parameter );
            case HEADER -> WsParams.fromHeader( exchange, parameter, wsParam );
            case COOKIE -> WsParams.fromCookie( exchange, parameter, wsParam );
            case PATH -> WsParams.fromPath( exchange, wsMethod, parameter );
            case BODY -> WsParams.fromBody( exchange, parameter );
            case QUERY -> WsParams.fromQuery( exchange, parameter, wsParam );
        } );
    }


    private static class JsonStackTraceResponse implements Serializable {
        @Serial
        private static final long serialVersionUID = 8431608226448804296L;

        public final String message;

        private JsonStackTraceResponse( Throwable t ) {
            message = Throwables.getRootCause( t ).getMessage();
        }
    }
}
