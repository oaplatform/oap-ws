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
import oap.http.HttpStatusCodes;
import oap.http.server.nio.HttpServerExchange;
import oap.reflect.Reflection;
import oap.ws.Session;
import oap.ws.interceptor.Interceptor;
import oap.ws.sso.Authentication;
import oap.ws.sso.Authenticator;
import oap.ws.sso.SSO;
import oap.ws.sso.SecurityRoles;
import oap.ws.sso.User;
import oap.ws.sso.WsSecurity;

import javax.annotation.Nonnull;
import java.util.Optional;

import static oap.ws.sso.SSO.SESSION_USER_KEY;

@Slf4j
public class SecurityInterceptor implements Interceptor {
    private final Authenticator authenticator;
    private final SecurityRoles roles;

    public SecurityInterceptor( Authenticator authenticator, SecurityRoles roles ) {
        this.authenticator = authenticator;
        this.roles = roles;
    }

    @Override
    public boolean before( @Nonnull HttpServerExchange exchange, Session session, @Nonnull Reflection.Method method ) {

        if( !session.containsKey( SESSION_USER_KEY ) ) {
            log.trace( "no user in session {}", session );
            var authId = SSO.getAuthentication( exchange );
            log.trace( "auth id {}", authId );
            if( authId == null ) {
                log.trace( "not authenticated" );
            } else {
                Authentication authentication = authenticator.authenticate( authId ).orElse( null );
                if( authentication != null ) {
                    User user = authentication.user;
                    session.set( SESSION_USER_KEY, user );
                    log.trace( "set user {} into session {}", user, session );
                } else {
                    log.trace( "not authenticated" );
                }
            }
        }

        log.trace( "session state {}", session );

        Optional<WsSecurity> annotation = method.findAnnotation( WsSecurity.class );
        if( annotation.isPresent() ) {
            log.trace( "secure method {}", method );

            if( !session.containsKey( SESSION_USER_KEY ) ) {
                exchange.setStatusCode( HttpStatusCodes.UNAUTHORIZED );
                return true;
            } else {
                var user = session.<User>get( SESSION_USER_KEY )
                    .filter( u -> !roles.granted( u.getRole(), annotation.get().permissions() ) )
                    .orElse( null );
                if( user != null ) {
                    exchange.setStatusCodeReasonPhrase( HttpStatusCodes.FORBIDDEN, String.format( "User [%s] has no access to method [%s]", user.getEmail(), method.name() ) );
                    return true;
                }
            }
        }
        return false;
    }
}

