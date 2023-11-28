/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public interface Accounts {
    OrganizationData storeOrganization( Organization organization );

    Optional<OrganizationData> getOrganization( String organizationId );

    List<OrganizationData> getOrganizations();

    Optional<OrganizationData> storeAccount( String organizationId, Account account );

    List<UserData> getUsers( String organizationId );

    Optional<UserData> getUser( String email );

    Optional<UserData> updateUser( String email, Consumer<User> update );

    Optional<UserData> passwd( String email, String password );

    Optional<UserData> ban( String email, boolean banStatus );

    Optional<UserData> confirm( String email );

    UserData createUser( User user, Map<String, String> roles );

    Optional<UserData> delete( String email );

    Optional<UserData> assignRole( String email, String organizationId, String role );

    Optional<UserData> addAccountToUser( String email, String organizationId, String accountId );

    Optional<UserData> refreshApikey( String email );

    void permanentlyDeleteOrganization( String organizationId );

    void permanentlyDeleteUser( String email );

    void permanentlyDeleteAll();

    Optional<UserData> addOrganizationToUser( String email, String organizationId, String role );
}
