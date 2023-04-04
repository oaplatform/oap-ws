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

package oap.ws.sso.ws;

import lombok.extern.slf4j.Slf4j;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import oap.ws.sso.AbstractWS;
import oap.ws.sso.Accounts;
import oap.ws.sso.SecurityRoles;
import oap.ws.sso.WsSecurity;
import oap.ws.sso.model.UserData;
import oap.ws.sso.ws.OrganizationWS;
import oap.ws.validate.ValidationErrors;
import oap.ws.validate.WsValidate;

import java.net.HttpURLConnection;
import java.util.Optional;

import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static oap.ws.WsParam.From.PATH;
import static oap.ws.WsParam.From.SESSION;
import static oap.ws.sso.ws.OrganizationWS.ORGANIZATION_ID;
import static oap.ws.sso.Permissions.MANAGE_SELF;
import static oap.ws.sso.Permissions.USER_READ;

@Slf4j
public class UserWS extends AbstractWS {

    protected Accounts accounts;
    protected OrganizationWS organizationWS;

    public UserWS( SecurityRoles roles, Accounts accounts, OrganizationWS organizationWS ) {
        super( roles );
        this.accounts = accounts;
        this.organizationWS = organizationWS;
    }

    @WsMethod( method = GET, path = "/{organizationId}/{email}", description = "Returns user with given email" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { USER_READ, MANAGE_SELF } )
    @WsValidate( { "validateOrganizationAccess", "validateSameOrganization" } )
    public Optional<UserData.View> get( @WsParam( from = PATH ) String organizationId,
                                        @WsParam( from = PATH ) String email,
                                        @WsParam( from = SESSION ) UserData loggedUser ) {
        return accounts.getUser( email )
            .map( u -> email.equals( loggedUser.user.email ) || isSystem( loggedUser )
                ? u.secureView
                : u.view );
    }

    protected ValidationErrors validateSameOrganization( String organizationId, String email ) {
        return accounts.getUser( email )
            .filter( user -> user.belongsToOrganization( organizationId ) )
            .map( user -> ValidationErrors.empty() )
            .orElseGet( () -> ValidationErrors.error( HttpURLConnection.HTTP_NOT_FOUND, "not found " + email ) );
    }

    @WsMethod( method = GET, path = "/current", description = "Returns a current logged user" )
    @WsValidate( { "validateUserLoggedIn" } )
    public Optional<UserData.SecureView> current( @WsParam( from = SESSION ) Optional<UserData> loggedUser ) {
        return loggedUser.map( u -> u.secureView );
    }

}
