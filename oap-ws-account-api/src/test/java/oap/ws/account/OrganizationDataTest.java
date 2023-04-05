/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account;

import org.testng.annotations.Test;

import static oap.testng.Asserts.assertString;

public class OrganizationDataTest {

    @Test
    public void addOrUpdateAccount() {
        var data = new OrganizationData( new Organization( "org1" ) );
        var account1 = new Account( "account1" );
        data.addOrUpdateAccount( account1 );
        assertString( account1.id ).isEqualTo( "CCNT1" );
        var account2 = new Account( "account1" );
        data.addOrUpdateAccount( account2 );
        assertString( account2.id ).isEqualTo( "CCNT0" );
        var account3 = new Account( "3", "account3" );
        data.addOrUpdateAccount( account3 );
        assertString( account3.id ).isEqualTo( "3" );
    }

}
