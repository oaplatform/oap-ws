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

        Optional<User> sessionUserKey = context.session.get( SESSION_USER_KEY );
        if( jwtToken != null && ( sessionUserKey.isEmpty() || issuerFromContext( context ).equals( this.getClass().getSimpleName() ) ) ) {
            log.debug( "Proceed with user {} in session: {}", sessionUserKey, context.session.id );

            final String token = JWTExtractor.extractBearerToken( jwtToken );
            if( token == null || !jwtExtractor.verifyToken( token ) ) {
                log.warn( "Not authenticated. Token {}", token );
                return Optional.of( new Response( FORBIDDEN, "Invalid token: " + token ) );
            }

            final String email = jwtExtractor.getUserEmail( token );
            organization = jwtExtractor.getOrganizationId( token );

            User user = userProvider.getUser( email ).orElse( null );
            if( user == null ) {
                log.warn( "User not found with email: {}", email );
                return Optional.of( new Response( FORBIDDEN, "User not found with email: " + email ) );
            }

            context.session.set( SESSION_USER_KEY, user );
            context.session.set( ISSUER, this.getClass().getSimpleName() );
            log.trace( "set user '{}' into session '{}'", user, context.session.id );
        }
        Optional<WsSecurity> wss = context.method.findAnnotation( WsSecurity.class );
        if( wss.isEmpty() ) {
            return Optional.empty();
        }

        log.trace( "Secure method {}", context.method );

        if( jwtToken == null && !issuerFromContext( context ).equals( ApiKeyInterceptor.class.getSimpleName() ) ) {
            log.warn( "Not authenticated! jwsToken {} issuerFromContext {}", jwtToken, issuerFromContext( context ) );
            return Optional.of( new Response( UNAUTHORIZED, jwtToken == null ? "jwtToken is null" : "Not desired interceptor: " + issuerFromContext( context ) ) );
        }

        String realmFromWss = wss.get().realm();
        Optional<String> realm = SYSTEM.equals( realmFromWss )
            ? Optional.of( SYSTEM )
            : context.getParameter( realmFromWss );
        if( realm.isEmpty() ) {
            log.warn( "realm is not passed" );
            return Optional.of( new Response( FORBIDDEN, "realm is not passed" ) );
        }

        String realmString = realm.get();
        if( organization != null && !realmString.equals( organization ) && !realmString.equals( SYSTEM ) ) {
            log.warn( "realm organization '{}' does not coincide organization '{}' logged in", realmString, organization );
            return Optional.of( new Response( FORBIDDEN, "realm organization '" + realmString + "' does not coincide organization '" + organization + "' logged in" ) );
        }
        String[] wssPermissions = wss.get().permissions();
        if( issuerFromContext( context ).equals( this.getClass().getSimpleName() ) ) {
            permissions = jwtExtractor.getPermissions( JWTExtractor.extractBearerToken( jwtToken ), Objects.requireNonNullElseGet( organization, realm::get ) );
            if( permissions != null && Arrays.stream( wssPermissions ).anyMatch( permissions::contains ) ) {
                    log.trace( "permissions: {} wss {}", permissions, wssPermissions );
                    return Optional.empty();
            }
            String requiredPermissions = Arrays.toString( wssPermissions );
            log.warn( format( "Permissions required: %s, but found: %s", requiredPermissions, permissions ) );
            return Optional.of( new Response( FORBIDDEN, "user doesn't have required permissions: '" + requiredPermissions + "', user permissions: '" + permissions + "'" ) );
        } else {
            if( sessionUserKey.isEmpty() ) {
                return Optional.of( new Response( UNAUTHORIZED, "no user in session" ) );
            }
            Optional<String> role = sessionUserKey.flatMap( user -> user.getRole( realmString ) );
            if( role.isEmpty() ) {
                log.warn( "user doesn't have access to realm {}", realmString );
                return Optional.of( new Response( FORBIDDEN, "user doesn't have access to realm '" + realmString + "'" ) );
            }

            if( roles.granted( role.get(), wssPermissions ) ) {
                log.trace( "roles.granted ({}, {}) -> false", role.get(), wssPermissions );
                return Optional.empty();
            }

            log.warn( "user {} has no access to method {} under realm {}", sessionUserKey.get().getEmail(), context.method.name(), realmString );
            return Optional.of( new Response( FORBIDDEN, "user " + sessionUserKey.get().getEmail() + " has no access to method "
                + context.method.name() + " under realm " + realmString ) );
        }
    }

    private String issuerFromContext( InvocationContext context ) {
        Optional<Object> issuer = context.session.get( ISSUER );
        if( issuer.isPresent() ) {
            return issuer.get().toString();
        }
        return "";
    }
}

