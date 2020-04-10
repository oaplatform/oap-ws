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

package oap.ws.sso;

import lombok.extern.slf4j.Slf4j;
import oap.http.HttpResponse;
import oap.http.Request;
import oap.reflect.Reflection;
import oap.ws.Session;
import oap.ws.interceptor.Interceptor;

import javax.annotation.Nonnull;
import java.util.Optional;

import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
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
    @Nonnull
    public Optional<HttpResponse> before( @Nonnull Request request, Session session, @Nonnull Reflection.Method method ) {

        if( !session.containsKey( SESSION_USER_KEY ) ) {
            log.trace( "no user in session {}", session );
            var authId = SSO.getAuthentication( request );
            log.trace( "auth id {}", authId );
            Optional<Authentication> authentication = authId.flatMap( authenticator::authenticate );
            if( authentication.isEmpty() ) log.trace( "not authenticated" );
            else {
                User user = authentication.get().user;
                session.set( SESSION_USER_KEY, user );
                log.trace( "set user {} into session {}", user, session );
            }
        }

        log.trace( "session state {}", session );

        Optional<WsSecurity> annotation = method.findAnnotation( WsSecurity.class );
        if( annotation.isPresent() ) {
            log.trace( "secure method {}", method );

            return session.containsKey( SESSION_USER_KEY )
                ? session.<User>get( SESSION_USER_KEY )
                .filter( u -> !roles.granted( u.getRole(), annotation.get().permissions() ) )
                .map( u -> {
                    log.trace( "denied access to method {} for {} with role {} {}: required {}", method.name(), u.getEmail(), u.getRole(), roles.permissionsOf( u.getRole() ), annotation.get().permissions() );
                    return HttpResponse.status( HTTP_FORBIDDEN, format( "User [%s] has no access to method [%s]", u.getEmail(), method.name() ) ).response();
                } )
                : Optional.of( HttpResponse.status( HTTP_UNAUTHORIZED ).response() );
        }
        return Optional.empty();
    }
}

