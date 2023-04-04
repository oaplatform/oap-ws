/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.sso;

import oap.http.Http;
import oap.ws.sso.model.UserData;
import oap.ws.validate.ValidationErrors;

import static oap.ws.sso.Roles.ORGANIZATION_ADMIN;
import static oap.ws.sso.WsSecurity.SYSTEM;
import static oap.ws.validate.ValidationErrors.empty;
import static oap.ws.validate.ValidationErrors.error;

public abstract class AbstractWS extends AbstractSecureWS {
    protected boolean securityDisabled = false;

    public AbstractWS( SecurityRoles roles ) {
        super( roles );
    }

    protected ValidationErrors validateOrganizationAccess( UserData loggedUser, String organizationId ) {
        return canAccessOrganization( loggedUser, organizationId )
            ? empty()
            : error( Http.StatusCode.FORBIDDEN, "%s cannot access organization %s", loggedUser.user.email, organizationId );
    }

    protected ValidationErrors validateAccountAccess( UserData loggedUser, String organizationId, String accountId ) {
        return canAccessAccount( loggedUser, organizationId, accountId )
            ? empty()
            : error( Http.StatusCode.FORBIDDEN, "%s cannot access account %s of organization %s", loggedUser.user.email, accountId, organizationId );
    }

    protected ValidationErrors validateSecurityDisabled() {
        return securityDisabled
            ? empty()
            : error( Http.StatusCode.FORBIDDEN, "this method is only allowed with disabled security" );
    }

    protected boolean canAccessOrganization( UserData loggedUser, String organizationId ) {
        return isSystem( loggedUser )
            || loggedUser.belongsToOrganization( organizationId );
    }

    protected boolean isSystem( UserData loggedUser ) {
        return loggedUser.roles.containsKey( SYSTEM );
    }

    protected boolean canAccessAccount( UserData loggedUser, String organizationId, String accountId ) {
        return isSystem( loggedUser )
            || isOrganizationAdmin( loggedUser, organizationId )
            || loggedUser.canAccessAccount( organizationId, accountId );
    }

    protected boolean isOrganizationAdmin( UserData loggedUser, String organizationId ) {
        return loggedUser.roles.get( organizationId ).equals( ORGANIZATION_ADMIN );
    }

}
