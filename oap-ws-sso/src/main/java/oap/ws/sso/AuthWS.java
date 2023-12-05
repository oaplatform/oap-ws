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
import oap.ws.Response;
import oap.ws.Session;
import oap.ws.SessionManager;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import oap.ws.account.OauthService;
import oap.ws.account.TokenInfo;
import oap.ws.validate.ValidationErrors;
import oap.ws.validate.WsValidate;

import java.util.Optional;

import static oap.http.Http.StatusCode.BAD_REQUEST;
import static oap.http.Http.StatusCode.FORBIDDEN;
import static oap.http.Http.StatusCode.UNAUTHORIZED;
import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static oap.http.server.nio.HttpServerExchange.HttpMethod.POST;
import static oap.ws.WsParam.From.BODY;
import static oap.ws.WsParam.From.COOKIE;
import static oap.ws.WsParam.From.PATH;
import static oap.ws.WsParam.From.SESSION;
import static oap.ws.sso.AuthenticationFailure.TFA_REQUIRED;
import static oap.ws.sso.AuthenticationFailure.TOKEN_NOT_VALID;
import static oap.ws.sso.AuthenticationFailure.WRONG_ORGANIZATION;
import static oap.ws.sso.AuthenticationFailure.WRONG_TFA_CODE;
import static oap.ws.sso.SSO.authenticatedResponse;
import static oap.ws.sso.SSO.logoutResponse;
import static oap.ws.sso.SSO.notAuthenticatedResponse;
import static oap.ws.validate.ValidationErrors.empty;
import static oap.ws.validate.ValidationErrors.error;

@Slf4j
@SuppressWarnings( "unused" )
public class AuthWS extends AbstractSecureWS {

    private final Authenticator authenticator;
    private final SessionManager sessionManager;

    private final OauthService oauthService;

    public AuthWS( SecurityRoles roles, Authenticator authenticator, SessionManager sessionManager, OauthService oauthService ) {
        super( roles );
        this.authenticator = authenticator;
        this.sessionManager = sessionManager;
        this.oauthService = oauthService;
    }

    public AuthWS( SecurityRoles roles, Authenticator authenticator, SessionManager sessionManager ) {
        this( roles, authenticator, sessionManager, null );
    }

    @WsMethod( method = POST, path = "/login" )
    public Response login( @WsParam( from = BODY ) Credentials credentials,
                           @WsParam( from = SESSION ) Optional<User> loggedUser,
                           Session session ) {
        return login( credentials.email, credentials.password, Optional.ofNullable( credentials.tfaCode ), loggedUser, session );
    }

    @WsMethod( method = GET, path = "/login" )
    public Response login( String email,
                           String password,
                           @WsParam( from = BODY ) Optional<String> tfaCode,
                           @WsParam( from = SESSION ) Optional<User> loggedUser,
                           Session session ) {
        loggedUser.ifPresent( user -> logout( loggedUser, session ) );
        var result = authenticator.authenticate( email, password, tfaCode );
        if( result.isSuccess() ) return authenticatedResponse( result.getSuccessValue(),
            sessionManager.cookieDomain, sessionManager.cookieExpiration, sessionManager.cookieSecure );
        else if( TFA_REQUIRED == result.getFailureValue() )
            return notAuthenticatedResponse( BAD_REQUEST, "TFA code is required", sessionManager.cookieDomain );
        else if( WRONG_TFA_CODE == result.getFailureValue() ) {
            return notAuthenticatedResponse( BAD_REQUEST, "TFA code is incorrect", sessionManager.cookieDomain );
        } else
            return notAuthenticatedResponse( UNAUTHORIZED, "Username or password is invalid", sessionManager.cookieDomain );
    }

    @WsMethod( method = POST, path = "/oauth/login" )
    public Response login( @WsParam( from = BODY ) TokenCredentials credentials,
                           @WsParam( from = SESSION ) Optional<User> loggedUser,
                           Session session ) {
        loggedUser.ifPresent( user -> logout( loggedUser, session ) );
        final Optional<TokenInfo> tokenInfo = oauthService.getOauthProvider( credentials.source ).getTokenInfo( credentials.accessToken );
        if( tokenInfo.isPresent() ) {
            var result = authenticator.authenticate( tokenInfo.get().email, credentials.tfaCode );
            if( result.isSuccess() ) return authenticatedResponse( result.getSuccessValue(),
                sessionManager.cookieDomain, sessionManager.cookieExpiration, sessionManager.cookieSecure );
            else if( TFA_REQUIRED == result.getFailureValue() )
                return notAuthenticatedResponse( BAD_REQUEST, "TFA code is required", sessionManager.cookieDomain );
            else if( WRONG_TFA_CODE == result.getFailureValue() ) {
                return notAuthenticatedResponse( BAD_REQUEST, "TFA code is incorrect", sessionManager.cookieDomain );
            } else
                return notAuthenticatedResponse( UNAUTHORIZED, "User not found", sessionManager.cookieDomain );
        }
        return notAuthenticatedResponse( UNAUTHORIZED, "Token is empty", sessionManager.cookieDomain );
    }

    @SuppressWarnings( "ParameterName" )
    @WsMethod( method = GET, path = "/switch/{organizationId}" )
    public Response switchOrganization( @WsParam( from = PATH ) String organizationId,
                                        @WsParam( from = SESSION ) Optional<User> loggedUser,
                                        @WsParam( from = COOKIE ) String Authorization,
                                        Session session ) {
        loggedUser.ifPresent( user -> logout( loggedUser, session ) );
        var result = authenticator.authenticateWithActiveOrgId( Authorization, organizationId );
        if( result.isSuccess() ) return authenticatedResponse( result.getSuccessValue(),
            sessionManager.cookieDomain, sessionManager.cookieExpiration, sessionManager.cookieSecure );
        else if( WRONG_ORGANIZATION == result.getFailureValue() )
            return notAuthenticatedResponse( FORBIDDEN, "User doesn't belong to organization", sessionManager.cookieDomain );
        else if( TOKEN_NOT_VALID == result.getFailureValue() ) {
            return notAuthenticatedResponse( UNAUTHORIZED, "Token is invalid", sessionManager.cookieDomain );
        } else
            return notAuthenticatedResponse( UNAUTHORIZED, "User not found", sessionManager.cookieDomain );
    }

    @WsMethod( method = GET, path = "/logout" )
    public Response logout( @WsParam( from = SESSION ) Optional<User> loggedUser,
                            Session session ) {
        loggedUser.ifPresent( user -> {
            log.debug( "Invalidating token for user [{}]", user.getEmail() );
            authenticator.invalidate( user.getEmail() );
        } );
        session.invalidate();
        return logoutResponse( sessionManager.cookieDomain );
    }

    protected ValidationErrors validateUserAccess( Optional<String> email, User loggedUser ) {
        return email
            .filter( e -> !loggedUser.getEmail().equalsIgnoreCase( e ) )
            .map( e -> error( Http.StatusCode.FORBIDDEN, "User [%s] doesn't have enough permissions", loggedUser.getEmail() ) )
            .orElse( empty() );
    }

    @WsMethod( method = GET, path = "/whoami" )
    @WsValidate( "validateUserLoggedIn" )
    public Optional<User.View> whoami( @WsParam( from = SESSION ) Optional<User> loggedUser ) {
        return loggedUser.map( User::getView );
    }
}
