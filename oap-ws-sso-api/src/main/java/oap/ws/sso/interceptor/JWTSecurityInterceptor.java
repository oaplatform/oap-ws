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
import oap.ws.InvocationContext;
import oap.ws.Response;
import oap.ws.interceptor.Interceptor;
import oap.ws.sso.JWTExtractor;
import oap.ws.sso.SSO;
import oap.ws.sso.User;
import oap.ws.sso.UserProvider;
import oap.ws.sso.WsSecurity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static oap.http.Http.StatusCode.FORBIDDEN;
import static oap.http.Http.StatusCode.UNAUTHORIZED;
import static oap.ws.sso.AbstractJWTExtractor.extractBearerToken;
import static oap.ws.sso.SSO.SESSION_USER_KEY;
import static oap.ws.sso.WsSecurity.SYSTEM;

@Slf4j
public class JWTSecurityInterceptor implements Interceptor {

    private final JWTExtractor jwtExtractor;
    private final UserProvider userProvider;

    public JWTSecurityInterceptor( JWTExtractor jwtExtractor, UserProvider userProvider ) {
        this.jwtExtractor = jwtExtractor;
        this.userProvider = userProvider;
    }

    @Override
    public Optional<Response> before( InvocationContext context ) {
        if( context.session.containsKey( SESSION_USER_KEY ) ) {
            log.debug( "Proceed with user in session:" + context.session.get( SESSION_USER_KEY ) );
            return Optional.empty();
        }
        var jwtToken = SSO.getAuthentication( context.exchange );
        if( jwtToken != null ) {
            final String token = extractBearerToken( jwtToken );
            if( token == null || !jwtExtractor.verifyToken( token ) ) {
                log.trace( "Not authenticated." );
                return Optional.of( new Response( FORBIDDEN, "Invalid token" ) );
            }

            final String email = jwtExtractor.getUserEmail( token );
            User user = userProvider.getUser( email ).orElse( null );
            if( user != null ) {
                context.session.set( SESSION_USER_KEY, user );
                log.trace( "set user {} into session {}", user, context.session );
            } else
                log.trace( "User not found with email: " + email );
        }

        Optional<WsSecurity> wss = context.method.findAnnotation( WsSecurity.class );
        if( wss.isEmpty() ) {
            return Optional.empty();
        }

        log.trace( "Secure method {}", context.method );
        if( jwtToken == null )
            return Optional.of( new Response( UNAUTHORIZED ) );

        Optional<String> realm =
            SYSTEM.equals( wss.get().realm() ) ? Optional.of( SYSTEM ) : context.getParameter( wss.get().realm() );
        if( realm.isEmpty() ) {
            return Optional.of( new Response( FORBIDDEN, "realm is not passed" ) );
        }
        final List<String> permissions = jwtExtractor.getPermissions( extractBearerToken( jwtToken ), realm.get() );
        if( permissions != null ) {
            if( Arrays.stream( wss.get().permissions() ).anyMatch( permissions::contains ) )
                return Optional.empty();
        }
        log.info( format( "Permissions required: %s, but found: %s", Arrays.toString( wss.get().permissions() ), permissions ) );
        return Optional.of( new Response( FORBIDDEN, "user doesn't have permissions" ) );
    }
}

