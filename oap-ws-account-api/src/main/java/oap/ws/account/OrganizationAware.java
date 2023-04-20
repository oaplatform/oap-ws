/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account;

import java.util.Objects;

public interface OrganizationAware {
    String organizationId();

    default boolean belongsToOrganization( String organizationId ) {
        return Objects.equals( organizationId(), organizationId );
    }

}
