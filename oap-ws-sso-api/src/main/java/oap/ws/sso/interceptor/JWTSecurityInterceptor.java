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
import oap.ws.sso.WsSecurity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static oap.http.Http.StatusCode.FORBIDDEN;
import static oap.ws.sso.SSO.AUTHENTICATION_KEY;
import static oap.ws.sso.WsSecurity.SYSTEM;
import static oap.ws.sso.interceptor.AbstractAuthTokenProvider.extractBearerToken;

@Slf4j
public class JWTSecurityInterceptor implements Interceptor {

    private AbstractAuthTokenProvider tokenProvider;

    public JWTSecurityInterceptor( AbstractAuthTokenProvider tokenProvider ) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public Optional<Response> before( InvocationContext context ) {
        final String authorization = context.exchange.getRequestHeader( AUTHENTICATION_KEY );
        if( authorization == null || authorization.isEmpty() ) {
            log.trace( "Not authenticated. No token in header {}", context.exchange );
        }
        if( !tokenProvider.verifyToken( extractBearerToken( authorization ) ) ) {
            log.trace( "Not authenticated." );
            return Optional.of( new Response( FORBIDDEN, "Invalid token" ) );
        }

        Optional<WsSecurity> wss = context.method.findAnnotation( WsSecurity.class );
        if( wss.isEmpty() )
            return Optional.empty();

        log.trace( "secure method {}", context.method );

        Optional<String> realm =
            SYSTEM.equals( wss.get().realm() ) ? Optional.of( SYSTEM ) : context.getParameter( wss.get().realm() );
        if( realm.isEmpty() )
            return Optional.of( new Response( FORBIDDEN, "realm is not passed" ) );

        final List<String> permissions = tokenProvider.getPermissions( authorization );
        if( Arrays.stream( wss.get().permissions() ).anyMatch( permissions::contains ) )
            return Optional.empty();
        return Optional.of( new Response( FORBIDDEN, "user doesn't have permissions" ) );
    }
}

