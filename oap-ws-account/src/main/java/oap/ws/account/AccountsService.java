/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account;

import lombok.extern.slf4j.Slf4j;

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
        log.debug( "storeAccount organizationId {} account {}", organizationId, account );

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
    public Optional<UserData> updateUser( String email, Consumer<User> update ) {
        log.debug( "updateUser email {}", email );

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
    public UserData createUser( User user, Map<String, String> roles ) {
        log.debug( "createUser user {} roles {}", user, roles );

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

    @SuppressWarnings( "checkstyle:UnnecessaryParentheses" )
    @Override
    public void permanentlyDeleteOrganization( String organizationId ) {
        log.debug( "permanentlyDeleteOrganization {}", organizationId );

        userStorage
            .select()
            .filter( ud -> ud.accounts.containsKey( organizationId ) || ud.roles.containsKey( organizationId ) )
            .forEach( ud -> {
                if( ( ud.accounts.containsKey( organizationId ) && ud.accounts.size() == 1 )
                    || ( ud.roles.containsKey( organizationId ) && ud.roles.size() == 1 ) ) {
                    log.trace( "permanentlyDeleteOrganization#delete user {}", ud.getEmail() );
                    userStorage.permanentlyDelete( ud.getEmail() );
                } else {
                    log.trace( "permanentlyDeleteOrganization#update user {}", ud.getEmail() );
                    userStorage.update( ud.getEmail(), d -> {
                        d.accounts.remove( organizationId );
                        d.roles.remove( organizationId );
                        return d;
                    } );
                }
            } );

        organizationStorage.permanentlyDelete( organizationId );
    }

    @Override
    public void permanentlyDeleteUser( String email ) {
        log.debug( "permanentlyDeleteUser {}", email );

        userStorage.permanentlyDelete( email );
    }

    @Override
    public void permanentlyDeleteAll() {
        var users = userStorage.select()
            .filter( u -> !userStorage.defaultSystemAdminEmail.equals( u.user.email ) )
            .map( u -> u.user.email )
            .toList();

        for( var user : users ) {
            userStorage.permanentlyDelete( user );
        }

        var organizations = organizationStorage.select()
            .filter( o -> !organizationStorage.defaultOrganizationId.equals( o.organization.id ) )
            .map( o -> o.organization.id )
            .toList();

        for( var organization : organizations ) {
            organizationStorage.permanentlyDelete( organization );
        }
    }

    @Override
    public Optional<UserData> addOrganizationToUser( String email, String organizationId, String role ) {
        return userStorage.update( email, u -> u.addOrganization( organizationId, role ) );
    }
}
