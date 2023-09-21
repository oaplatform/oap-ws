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

package oap.ws.account;

import lombok.extern.slf4j.Slf4j;
import oap.ws.WsMethod;
import oap.ws.account.ws.AbstractWS;
import oap.ws.sso.SecurityRoles;
import oap.ws.sso.WsSecurity;
import oap.ws.validate.WsValidate;

import static oap.http.server.nio.HttpServerExchange.HttpMethod.DELETE;
import static oap.ws.account.Permissions.ORGANIZATION_UPDATE;

@Slf4j
public class AdminWS extends AbstractWS {
    private final Accounts accounts;

    public AdminWS( Accounts accounts, SecurityRoles roles ) {
        super( roles );
        this.accounts = accounts;
    }

    @WsMethod( method = DELETE, path = "/organizations/{organizationId}" )
    @WsSecurity( realm = OrganizationWS.ORGANIZATION_ID, permissions = { ORGANIZATION_UPDATE } )
    @WsValidate( "validateOrganizationAccess" )
    public void deleteOrganization( String organizationId ) {
        log.info( "organization {}", organizationId );

        accounts.permanentlyDeleteOrganization( organizationId );
    }
}
