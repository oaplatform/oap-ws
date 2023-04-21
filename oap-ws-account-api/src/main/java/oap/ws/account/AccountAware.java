/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account;

import java.util.Objects;

public interface AccountAware extends OrganizationAware {
    String accountId();

    default boolean belongsToAccount( String organizationId, String accountId ) {
        return Objects.equals( organizationId(), organizationId )
            && ( Objects.equals( this.accountId(), accountId ) || Objects.equals( this.accountId(), "*" ) );
    }
}
