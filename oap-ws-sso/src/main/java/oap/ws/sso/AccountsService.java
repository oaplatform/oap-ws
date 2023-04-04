/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.sso;

import lombok.extern.slf4j.Slf4j;
import oap.ws.sso.model.Account;
import oap.ws.sso.model.Organization;
import oap.ws.sso.model.OrganizationData;
import oap.ws.sso.model.User;
import oap.ws.sso.model.UserData;
import oap.ws.sso.model.UserInfo;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class AccountsService implements Accounts {
    protected OrganizationStorage organizationStorage;
    protected UserStorage userStorage;

    public AccountsService( OrganizationStorage organizationStorage, UserStorage userStorage ) {
        this.organizationStorage = organizationStorage;
        this.userStorage = userStorage;
    }

    @Override
    public OrganizationData storeOrganization( Organization organization ) {
        return organizationStorage.update( organization.id,
            o -> o.update( organization ),
            () -> new OrganizationData( organization ) );
    }

    @Override
    public Optional<OrganizationData> getOrganization( String organizationId ) {
        return organizationStorage.get( organizationId );
    }

    @Override
    public List<OrganizationData> getOrganizations() {
        return organizationStorage.list();
    }

    @Override
    public Optional<OrganizationData> storeAccount( String organizationId, Account account ) {
        return organizationStorage.update( organizationId, o -> {
            o.addOrUpdateAccount( account );
            return o;
        } );
    }

    @Override
    public List<UserData> getUsers( String organizationId ) {
        return userStorage.select()
            .filter( u -> u.belongsToOrganization( organizationId ) )
            .toList();
    }

    @Override
    public Optional<UserData> getUser( String email ) {
        return userStorage.get( email );
    }

    @Override
    public Optional<UserData> updateUser( String email, Consumer<UserInfo> update ) {
        return userStorage.update( email, u -> {
            update.accept( u.user );
            return u;
        } );
    }

    @Override
    public Optional<UserData> passwd( String email, String password ) {
        return userStorage.update( email, user -> user.encryptPassword( password ) );
    }

    @Override
    public Optional<UserData> ban( String email, boolean banStatus ) {
        log.debug( ( banStatus ? "ban" : "unban" ) + " user " + email );
        return userStorage.update( email, user -> user.ban( banStatus ) );
    }

    @Override
    public Optional<UserData> confirm( String email ) {
        log.debug( "confirming: {}", email );
        return userStorage.update( email, user -> user.confirm( true ) );
    }

    @Override
    public UserData createUser( UserInfo user, Map<String, String> roles ) {
        if( userStorage.get( user.email ).isPresent() )
            throw new IllegalArgumentException( "user: " + user.email + " is already registered" );
        user.encryptPassword( user.password );
        return userStorage.store( new UserData( user, roles ) );
    }

    @Override
    public Optional<UserData> delete( String email ) {
        return userStorage.delete( email );
    }

    @Override
    public Optional<UserData> assignRole( String email, String organizationId, String role ) {
        log.debug( "assign role: {} to user: {} in organization: {}", role, email, organizationId );
        userStorage.update( email, u -> u.assignRole( organizationId, role ) );
        return Optional.empty();
    }

    @Override
    public Optional<UserData> addAccountToUser( String email, String organizationId, String accountId ) {
        log.debug( "add account: {} to user: {} in organization: {}", accountId, email, organizationId );
        return userStorage.update( email, u -> u.addAccount( organizationId, accountId ) );
    }

    @Override
    public Optional<UserData> refreshApikey( String email ) {
        log.debug( "refresh apikey to user: {}", email );
        return userStorage.update( email, UserData::refreshApikey );
    }
}
