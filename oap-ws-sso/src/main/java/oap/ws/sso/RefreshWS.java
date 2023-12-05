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
import oap.ws.Response;
import oap.ws.SessionManager;
import oap.ws.WsMethod;
import oap.ws.WsParam;

import java.util.Optional;

import static oap.http.Http.StatusCode.UNAUTHORIZED;
import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static oap.ws.WsParam.From.COOKIE;
import static oap.ws.WsParam.From.QUERY;
import static oap.ws.sso.SSO.authenticatedResponse;
import static oap.ws.sso.SSO.notAuthenticatedResponse;

@Slf4j
@SuppressWarnings( "unused" )
public class RefreshWS {

    private final Authenticator authenticator;
    private final SessionManager sessionManager;

    public RefreshWS( Authenticator authenticator, SessionManager sessionManager ) {
        this.authenticator = authenticator;
        this.sessionManager = sessionManager;
    }

    @WsMethod( method = GET, path = "/" )
    public Response refreshToken( @WsParam( from = COOKIE ) String refreshToken,
                                  @WsParam( from = QUERY ) Optional<String> organizationId ) {
        var result = authenticator.refreshToken( refreshToken, organizationId );
        if( result.isSuccess() ) return authenticatedResponse( result.getSuccessValue(),
            sessionManager.cookieDomain, sessionManager.cookieExpiration, sessionManager.cookieSecure );
        else
            return notAuthenticatedResponse( UNAUTHORIZED, "Token is invalid", sessionManager.cookieDomain );
    }
}
