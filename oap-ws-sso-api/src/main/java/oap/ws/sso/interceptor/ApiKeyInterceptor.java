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

package oap.ws.sso.interceptor;

import lombok.extern.slf4j.Slf4j;
import oap.http.HttpResponse;
import oap.http.Request;
import oap.reflect.Reflection;
import oap.ws.Session;
import oap.ws.interceptor.Interceptor;
import oap.ws.sso.Authenticator;
import oap.ws.sso.User;

import java.util.Optional;

import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static oap.ws.sso.SSO.SESSION_USER_KEY;

@Slf4j
public class ApiKeyInterceptor implements Interceptor {
    public static final String SESSION_API_KEY_AUTHENTICATED = "apiKeyAuthenticated";
    private final Authenticator authenticator;

    public ApiKeyInterceptor( Authenticator authenticator ) {
        this.authenticator = authenticator;
    }

    @Override
    public Optional<HttpResponse> before( Request request, Session session, Reflection.Method method ) {
        return request.parameter( "accessKey" )
            .flatMap( accessKey -> request.parameter( "apiKey" )
                .flatMap( apiKey -> {
                        if( session.containsKey( SESSION_USER_KEY ) ) return Optional.of( HttpResponse
                            .status( HTTP_CONFLICT, "invoking service with apiKey while logged in" )
                            .response() );

                        var authentication = authenticator.authenticateWithApiKey( accessKey, apiKey );
                        if( authentication.isPresent() ) {
                            User user = authentication.get().user;
                            session.set( SESSION_USER_KEY, user );
                            session.set( SESSION_API_KEY_AUTHENTICATED, true );
                            log.trace( "set user {} into session {}", user, session );
                            return Optional.empty();
                        } else return Optional.of( HttpResponse.status( HTTP_UNAUTHORIZED ).response() );
                    }
                ) );
    }

    @Override
    public HttpResponse after( HttpResponse response, Session session ) {
        session.<Boolean>get( SESSION_API_KEY_AUTHENTICATED ).ifPresent( apiKeyAuthentication -> {
            if( apiKeyAuthentication ) {
                log.trace( "removing temporary authentication of {}", session.get( SESSION_USER_KEY ) );
                session.remove( SESSION_USER_KEY );
                session.remove( SESSION_API_KEY_AUTHENTICATED );
            }
        } );
        return response;
    }
}
