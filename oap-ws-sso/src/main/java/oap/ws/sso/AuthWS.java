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
import oap.http.Http;
import oap.http.HttpStatusCodes;
import oap.ws.Response;
import oap.ws.Session;
import oap.ws.SessionManager;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import oap.ws.validate.ValidationErrors;

import java.util.Optional;

import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static oap.http.server.nio.HttpServerExchange.HttpMethod.POST;
import static oap.ws.WsParam.From.BODY;
import static oap.ws.WsParam.From.SESSION;
import static oap.ws.sso.AuthenticationFailure.MFA_REQUIRED;
import static oap.ws.sso.Permissions.MANAGE_SELF;
import static oap.ws.sso.SSO.authenticatedResponse;
import static oap.ws.sso.SSO.logoutResponse;
import static oap.ws.validate.ValidationErrors.empty;
import static oap.ws.validate.ValidationErrors.error;

@Slf4j
@SuppressWarnings( "unused" )
public class AuthWS {

    private final Authenticator authenticator;
    private final SessionManager sessionManager;

    public AuthWS( Authenticator authenticator, SessionManager sessionManager ) {
        this.authenticator = authenticator;
        this.sessionManager = sessionManager;
    }

    @WsMethod( method = POST, path = "/login" )
    public Response login( @WsParam( from = BODY ) Credentials credentials, @WsParam( from = SESSION ) Optional<User> loggedUser, Session session ) {
        return login( credentials.email, credentials.password, Optional.ofNullable( credentials.tfaCode ), loggedUser, session );
    }

    @WsMethod( method = GET, path = "/login" )
    public Response login( String email, String password, Optional<String> tfaCode, @WsParam( from = SESSION ) Optional<User> loggedUser, Session session ) {
        loggedUser.ifPresent( user -> logout( user, session ) );
        var result = authenticator.authenticate( email, password, tfaCode );
        if( result.isSuccess() ) {
            return authenticatedResponse( result.getSuccessValue(),
                sessionManager.cookieDomain, sessionManager.cookieExpiration, sessionManager.cookieSecure );
        } else if( MFA_REQUIRED.equals( result.getFailureValue() ) ) {
            return new Response( HttpStatusCodes.BAD_REQUEST, "TFA code is incorrect or required" );
        } else {
            return new Response( HttpStatusCodes.UNAUTHORIZED, "Username or password is invalid" );
        }
    }

    @WsMethod( method = GET, path = "/logout" )
    @WsSecurity( permissions = MANAGE_SELF )
    public Response logout( @WsParam( from = SESSION ) User loggedUser, Session session ) {
        log.debug( "Invalidating token for user [{}]", loggedUser.getEmail() );
        authenticator.invalidateByEmail( loggedUser.getEmail() );
        session.invalidate();

        return logoutResponse( sessionManager.cookieDomain );
    }

    protected ValidationErrors validateUserAccess( Optional<String> email, User loggedUser ) {
        return email
            .filter( e -> !loggedUser.getEmail().equalsIgnoreCase( e ) )
            .map( e -> error( Http.StatusCode.FORBIDDEN, "User [%s] doesn't have enough permissions", loggedUser.getEmail() ) )
            .orElse( empty() );
    }

    /**
     * @see #whoami(Session)
     */
    @Deprecated
    @WsMethod( method = GET, path = "/current" )
    public Optional<User.View> current( Session session ) {
        return session.<User>get( oap.ws.sso.SSO.SESSION_USER_KEY ).map( User::getView );
    }

    @WsMethod( method = GET, path = "/whoami" )
    public Response whoami( Session session ) {
        User user = session.<User>get( SSO.SESSION_USER_KEY ).orElse( null );
        if( user == null ) {
            return new Response( Http.StatusCode.UNAUTHORIZED );
        } else {
            return Response
                .jsonOk()
                .withBody( user.getView(), false );
        }
    }
}
