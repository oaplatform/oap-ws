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
import oap.ws.sso.SecurityRoles;
import oap.ws.sso.User;
import oap.ws.sso.UserProvider;
import oap.ws.sso.WsSecurity;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;
import static oap.http.Http.StatusCode.FORBIDDEN;
import static oap.http.Http.StatusCode.UNAUTHORIZED;
import static oap.ws.sso.SSO.ISSUER;
import static oap.ws.sso.SSO.SESSION_USER_KEY;
import static oap.ws.sso.WsSecurity.SYSTEM;

@Slf4j
public class JWTSecurityInterceptor implements Interceptor {

    private final JWTExtractor jwtExtractor;
    private final UserProvider userProvider;
    private final SecurityRoles roles;

    public JWTSecurityInterceptor( JWTExtractor jwtExtractor, UserProvider userProvider, SecurityRoles roles ) {
        this.jwtExtractor = Objects.requireNonNull( jwtExtractor );
        this.userProvider = Objects.requireNonNull( userProvider );
        this.roles = roles;
    }

    @Override
    public Optional<Response> before( InvocationContext context ) {
        String organization = null;
        List<String> permissions;
        String jwtToken = SSO.getAuthentication( context.exchange );

        if( jwtToken != null && ( !context.session.containsKey( SESSION_USER_KEY ) || issuerFromContext( context ).equals( this.getClass().getSimpleName() ) ) ) {
            log.debug( "Proceed with user in session:" + context.session.get( SESSION_USER_KEY ) );

            final String token = JWTExtractor.extractBearerToken( jwtToken );
            if( token == null || !jwtExtractor.verifyToken( token ) ) {
                log.trace( "Not authenticated." );
                return Optional.of( new Response( FORBIDDEN, "Invalid token" ) );
            }

            final String email = jwtExtractor.getUserEmail( token );
            organization = jwtExtractor.getOrganizationId( token );

            User user = userProvider.getUser( email ).orElse( null );
            if( user == null )
                return Optional.of( new Response( FORBIDDEN, "User not found with email: " + email ) );

            context.session.set( SESSION_USER_KEY, user );
            context.session.set( ISSUER, this.getClass().getSimpleName() );
            log.trace( "set user {} into session {}", user, context.session );
        }
        Optional<WsSecurity> wss = context.method.findAnnotation( WsSecurity.class );
        if( wss.isEmpty() )
            return Optional.empty();

        log.trace( "Secure method {}", context.method );


        if( jwtToken == null && !issuerFromContext( context ).equals( ApiKeyInterceptor.class.getSimpleName() ) ) {
            log.trace( "Not authenticated! jwsToken {} issuerFromContext {}", jwtToken, issuerFromContext( context ) );
            return Optional.of( new Response( UNAUTHORIZED ) );
        }

        Optional<String> realm =
            SYSTEM.equals( wss.get().realm() ) ? Optional.of( SYSTEM ) : context.getParameter( wss.get().realm() );
        if( realm.isEmpty() ) {
            log.trace( "realm is not passed" );
            return Optional.of( new Response( FORBIDDEN, "realm is not passed" ) );
        }

        if( organization != null && !realm.get().equals( organization ) && !realm.get().equals( SYSTEM ) ) {
            log.trace( "realm is different from organization logged in" );
            return Optional.of( new Response( FORBIDDEN, "realm is different from organization logged in" ) );
        }
        if( issuerFromContext( context ).equals( this.getClass().getSimpleName() ) ) {
            permissions = jwtExtractor.getPermissions( JWTExtractor.extractBearerToken( jwtToken ), Objects.requireNonNullElseGet( organization, realm::get ) );
            if( permissions != null ) {
                if( Arrays.stream( wss.get().permissions() ).anyMatch( permissions::contains ) ) {
                    return Optional.empty();
                }
            }
            log.info( format( "Permissions required: %s, but found: %s", Arrays.toString( wss.get().permissions() ), permissions ) );
            return Optional.of( new Response( FORBIDDEN, "user doesn't have permissions" ) );
        } else {
            Optional<User> u = context.session.get( SESSION_USER_KEY );
            if( u.isEmpty() ) return Optional.of( new Response( UNAUTHORIZED ) );
            Optional<String> role = u.flatMap( user -> user.getRole( realm.get() ) );
            if( role.isEmpty() ) {
                log.trace( "user doesn't have access to realm {}", realm.get() );
                return Optional.of( new Response( FORBIDDEN, "user doesn't have access to realm " + realm.get() ) );
            }

            if( roles.granted( role.get(), wss.get().permissions() ) ) return Optional.empty();

            log.trace( "user {} has no access to method {} under realm {}",
                u.get().getEmail(), context.method.name(), realm.get() );
            return Optional.of( new Response( FORBIDDEN, "user " + u.get().getEmail() + " has no access to method "
                + context.method.name() + " under realm " + realm.get() ) );
        }
    }

    private String issuerFromContext( InvocationContext context ) {
        if( context.session.containsKey( ISSUER ) && context.session.get( ISSUER ).isPresent() ) {
            return context.session.get( ISSUER ).get().toString();
        }
        return "";
    }
}

