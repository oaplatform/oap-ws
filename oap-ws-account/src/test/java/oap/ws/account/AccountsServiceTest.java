/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account;

import org.testng.annotations.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class AccountsServiceTest {
    @Test
    public void storeSameOrganization() {
        OrganizationStorage organizationStorage = new OrganizationStorage( "DFLT", "Default", "descr", true );
        UserStorage userStorage = new UserStorage( "xenoss@xenoss.io", "pwd", "fn", "ln", Map.of(), true );
        AccountsService accountsService = new AccountsService( organizationStorage, userStorage );
        OrganizationData saved = accountsService.storeOrganization( new Organization( "TEST" ) );
        OrganizationData notSaved = accountsService.storeOrganization( new Organization( "TEST" ) );
        assertThat( accountsService.getOrganizations() )
            .hasSize( 2 );
        assertThat( accountsService.getOrganizations().stream().map( o -> o.organization.id ) )
            .containsOnly( "TST0", "TST" );
    }
}
