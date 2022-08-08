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
import oap.ws.sso.Authentication;
import oap.ws.sso.Authenticator;
import oap.ws.sso.SSO;
import oap.ws.sso.SecurityRoles;
import oap.ws.sso.User;
import oap.ws.sso.WsSecurity;

import java.util.Optional;

import static oap.http.Http.StatusCode.FORBIDDEN;
import static oap.http.Http.StatusCode.UNAUTHORIZED;
import static oap.ws.sso.SSO.SESSION_USER_KEY;

@Slf4j
public class SecurityInterceptor implements Interceptor {
    private final Authenticator authenticator;
    private final SecurityRoles roles;
    private final String SYSTEM = "SYSTEM";

    public SecurityInterceptor( Authenticator authenticator, SecurityRoles roles ) {
        this.authenticator = authenticator;
        this.roles = roles;
    }

    @Override
    public Optional<Response> before( InvocationContext context ) {
        if( !context.session.containsKey( SESSION_USER_KEY ) ) {
            log.trace( "no user in session {}", context.session );
            var authId = SSO.getAuthentication( context.exchange );
            log.trace( "auth id {}", authId );
            if( authId == null ) log.trace( "not authenticated" );
            else {
                Authentication authentication = authenticator.authenticate( authId ).orElse( null );
                if( authentication != null ) {
                    User user = authentication.user;
                    context.session.set( SESSION_USER_KEY, user );
                    log.trace( "set user {} into session {}", user, context.session );
                } else log.trace( "not authenticated" );
            }
        }

        log.trace( "session state {}", context.session );

        Optional<WsSecurity> wss = context.method.findAnnotation( WsSecurity.class );
        if( wss.isEmpty() ) return Optional.empty();

        log.trace( "secure method {}", context.method );
        Optional<User> u = context.session.get( SESSION_USER_KEY );
        if( u.isEmpty() ) return Optional.of( new Response( UNAUTHORIZED ) );

        Optional<String> realm =
            wss.get().realm().equals( SYSTEM ) ? Optional.of( SYSTEM ) : context.getParameter( wss.get().realm() );
        if( realm.isEmpty() ) return Optional.of( new Response( FORBIDDEN, "realm is not passed" ) );

        Optional<String> role = u.flatMap( user -> user.getRole( realm.get() ) );
        if( role.isEmpty() && !realm.get().equals( SYSTEM ) )
            return Optional.of( new Response( FORBIDDEN, "user doesn't have access to realm " + realm.get() ) );

        if( realm.get().equals( SYSTEM ) || roles.granted( role.get(), wss.get().permissions() ) )
            return Optional.empty();

        return Optional.of( new Response( FORBIDDEN, "user " + u.get().getEmail() + " has no access to method "
            + context.method.name() + " under realm " + realm.get() ) );
    }
}

