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

import oap.ws.sso.model.Organization;
import oap.ws.sso.model.OrganizationData;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AccountsServiceTest {
    @Test
    public void storeSameOrganization() {
        AccountsService accountsService = new AccountsService( new OrganizationStorage(), new UserStorage() );
        OrganizationData saved = accountsService.storeOrganization( new Organization( "TEST" ) );
        OrganizationData notSaved = accountsService.storeOrganization( new Organization( "TEST" ) );
        assertThat( accountsService.getOrganizations() )
            .hasSize( 2 );
        assertThat( accountsService.getOrganizations().stream().map( o -> o.organization.id ) )
            .containsOnly( "TST0", "TST" );
    }
}
