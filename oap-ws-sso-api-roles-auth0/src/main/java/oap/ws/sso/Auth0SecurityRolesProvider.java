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

import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.client.mgmt.RolesEntity;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.mgmt.Permission;
import com.auth0.json.mgmt.PermissionsPage;
import com.auth0.json.mgmt.Role;
import com.auth0.json.mgmt.RolesPage;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class Auth0SecurityRolesProvider extends AbstractSecurityRolesProvider {

    public Auth0SecurityRolesProvider( Auth0Client auth0Client ) throws Auth0Exception {
        super( load( auth0Client ) );
    }

    private static Map<String, Set<String>> load( Auth0Client auth0Client ) throws Auth0Exception {
        log.info( "Loading roles and permissions from Auth0" );
        auth0Client.checkToken();
        HashMap<String, Set<String>> roles = new HashMap<>();
        final ManagementAPI api = auth0Client.getApi();
        final RolesEntity rolesEntity = api.roles();
        final RolesPage rolesPage = rolesEntity.list( null ).execute();
        if( rolesPage != null && !rolesPage.getItems().isEmpty() ) {
            for( Role role : rolesPage.getItems() ) {
                PermissionsPage permissionsPage = rolesEntity.listPermissions( role.getId(), null ).execute();
                if( permissionsPage != null ) {
                    roles.put( role.getName(), permissionsPage.getItems().stream().map( Permission::getName ).collect( Collectors.toSet() ) );
                }
            }
        }
        return Map.copyOf( roles );
    }
}
