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

import oap.util.Arrays;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Set;

import static oap.ws.sso.Permissions.MANAGE_SELF;
import static oap.ws.sso.Permissions.SUPERUSER;
import static oap.ws.sso.Roles.ADMIN;
import static oap.ws.sso.Roles.USER;
import static oap.ws.sso.SecurityRolesTest.Permissions.MEGATEST;
import static oap.ws.sso.SecurityRolesTest.Roles.MEGADMIN;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings( "checkstyle:InterfaceIsType" )
public class SecurityRolesTest {
    @Test
    public void merge() {
        SecurityRoles roles = new SecurityRoles();
        assertThat( roles.registeredRoles() ).containsOnly( ADMIN, MEGADMIN, USER );
        assertThat( roles.permissionsOf( ADMIN ) ).containsOnly( MEGATEST, SUPERUSER, MANAGE_SELF );
        assertThat( roles.permissionsOf( USER ) ).containsOnly( MANAGE_SELF );
        assertThat( roles.permissionsOf( MEGADMIN ) ).containsOnly( MEGATEST, MANAGE_SELF );
    }

    @Test
    public void granted() {
        SecurityRoles securityRoles = new SecurityRoles( new SecurityRoles.Config( Map.of( "USER", Set.of( "A", "B" ) ) ) );
        assertThat( securityRoles.granted( "VISITOR", Arrays.of( "A" ) ) ).isFalse();
        assertThat( securityRoles.granted( "USER", Arrays.of( "A" ) ) ).isTrue();
        assertThat( securityRoles.granted( "USER", Arrays.of( "C" ) ) ).isFalse();
        assertThat( securityRoles.granted( "USER", Arrays.of( "A", "C" ) ) ).isFalse();
        assertThat( securityRoles.granted( "USER", Arrays.of( "A", "B" ) ) ).isTrue();
    }

    public interface Roles extends oap.ws.sso.Roles {
        String MEGADMIN = "MEGADMIN";
    }

    public interface Permissions extends oap.ws.sso.Permissions {
        String MEGATEST = "MEGATEST";
    }
}
