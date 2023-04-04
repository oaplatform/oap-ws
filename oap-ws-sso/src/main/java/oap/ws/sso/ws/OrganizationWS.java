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

package oap.ws.sso.ws;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.http.Http;
import oap.util.Stream;
import oap.ws.Response;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import oap.ws.sso.AbstractWS;
import oap.ws.sso.AccountMailman;
import oap.ws.sso.Accounts;
import oap.ws.sso.SecurityRoles;
import oap.ws.sso.WsSecurity;
import oap.ws.sso.model.Account;
import oap.ws.sso.model.Organization;
import oap.ws.sso.model.OrganizationData;
import oap.ws.sso.model.User;
import oap.ws.sso.model.UserData;
import oap.ws.sso.model.UserInfo;
import oap.ws.validate.ValidationErrors;
import oap.ws.validate.WsValidate;
import oap.ws.validate.WsValidateJson;
import org.apache.http.client.utils.URIBuilder;

import javax.annotation.Nonnull;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static oap.http.Http.StatusCode.FORBIDDEN;
import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static oap.http.server.nio.HttpServerExchange.HttpMethod.POST;
import static oap.ws.WsParam.From.BODY;
import static oap.ws.WsParam.From.PATH;
import static oap.ws.WsParam.From.QUERY;
import static oap.ws.WsParam.From.SESSION;
import static oap.ws.sso.Permissions.*;
import static oap.ws.sso.Roles.ADMIN;
import static oap.ws.sso.Roles.ORGANIZATION_ADMIN;
import static oap.ws.sso.WsSecurity.SYSTEM;
import static oap.ws.validate.ValidationErrors.empty;
import static oap.ws.validate.ValidationErrors.error;

@Slf4j
@SuppressWarnings( "unused" )
public class OrganizationWS extends AbstractWS {

    public static final String ORGANIZATION_ID = "organizationId";
    private final Accounts accounts;
    private final AccountMailman mailman;
    private final String confirmUrlFinish;
    private final boolean selfRegistrationEnabled;

    public OrganizationWS( Accounts accounts, AccountMailman mailman, SecurityRoles roles, String confirmUrlFinish, boolean selfRegistrationEnabled ) {
        super( roles );
        this.accounts = accounts;
        this.mailman = mailman;
        this.confirmUrlFinish = confirmUrlFinish;
        this.selfRegistrationEnabled = selfRegistrationEnabled;
    }

    @WsMethod( method = POST, path = "/{organizationId}" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { ORGANIZATION_UPDATE } )
    @WsValidate( "validateOrganizationAccess" )
    public Organization store( @WsParam( from = PATH ) String organizationId,
                               @WsValidateJson( schema = Organization.SCHEMA ) @WsParam( from = BODY ) Organization organization,
                               @WsParam( from = SESSION ) UserData loggedUser ) {
        return accounts.storeOrganization( organization ).organization;
    }

    @WsMethod( method = POST, path = "/" )
    @WsSecurity( permissions = { ORGANIZATION_STORE } )
    public Organization store( @WsValidateJson( schema = Organization.SCHEMA ) @WsParam( from = BODY ) Organization organization,
                               @WsParam( from = SESSION ) UserData loggedUser ) {
        return accounts.storeOrganization( organization ).organization;
    }

    @WsMethod( method = GET, path = "/{organizationId}" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { ORGANIZATION_READ } )
    @WsValidate( "validateOrganizationAccess" )
    public Optional<OrganizationData.View> get( @WsParam( from = PATH ) String organizationId, @WsParam( from = SESSION ) UserData loggedUser ) {
        return accounts.getOrganization( organizationId ).map( data -> data.view );
    }

    @WsMethod( method = GET, path = "/" )
    @WsValidate( { "validateUserLoggedIn" } )
    public List<OrganizationData.View> list( @WsParam( from = SESSION ) Optional<UserData> loggedUser ) {
        return Stream.of( accounts.getOrganizations() )
            .filter( o -> canAccessOrganization( loggedUser.get(), o.organization.id ) )
            .map( o -> o.view )
            .toList();
    }

    @WsMethod( method = POST, path = "/{organizationId}/accounts" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { ACCOUNT_STORE } )
    @WsValidate( { "validateOrganizationAccess" } )
    public Optional<OrganizationData.View> storeAccount( @WsParam( from = PATH ) String organizationId,
                                                         @WsParam( from = BODY ) @WsValidateJson( schema = Account.SCHEMA ) Account account,
                                                         @WsParam( from = SESSION ) UserData loggedUser ) {
        return accounts.storeAccount( organizationId, account ).map( o -> o.view );
    }

    @WsMethod( method = GET, path = "/{organizationId}/accounts" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { ACCOUNT_LIST } )
    @WsValidate( "validateOrganizationAccess" )
    public Optional<List<Account>> accounts( @WsParam( from = PATH ) String organizationId,
                                             @WsParam( from = SESSION ) UserData loggedUser ) {
        return accounts.getOrganization( organizationId )
            .map( o -> Stream.of( o.accounts )
                .filter( a -> canAccessAccount( loggedUser, organizationId, a.id ) )
                .toList() );
    }

    @WsMethod( method = GET, path = "/{organizationId}/accounts/{accountId}" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { ACCOUNT_READ } )
    @WsValidate( { "validateOrganizationAccess", "validateAccountAccess" } )
    public Optional<Account> account( @WsParam( from = PATH ) String organizationId,
                                      @WsParam( from = PATH ) String accountId,
                                      @WsParam( from = SESSION ) UserData loggedUser ) {
        return accounts.getOrganization( organizationId ).flatMap( o -> o.accounts.get( accountId ) );
    }

    @WsMethod( method = POST, path = "/{organizationId}/users/{email}/accounts/add" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { ACCOUNT_ADD } )
    public Optional<UserData.View> addAccountsToUser( @WsParam( from = PATH ) String organizationId,
                                                      @WsParam( from = PATH ) String email,
                                                      @WsParam( from = QUERY ) String accountId,
                                                      @WsParam( from = SESSION ) UserData loggedUser ) {
        return accounts.addAccountToUser( email, organizationId, accountId ).map( u -> u.view );
    }

    @WsMethod( method = GET, path = "/{organizationId}/users" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { ORGANIZATION_LIST_USERS } )
    @WsValidate( { "validateOrganizationAccess" } )
    public List<UserData.View> users( @WsParam( from = PATH ) String organizationId,
                                      @WsParam( from = SESSION ) UserData loggedUser ) {
        return Stream.of( accounts.getUsers( organizationId ) )
            .map( u -> u.view )
            .toList();
    }

    @WsMethod( method = POST, path = "/{organizationId}/users" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { ORGANIZATION_STORE_USER } )
    @WsValidate( { "validateOrganizationAccess", "validateUsersOrganization", "validateAdminRole", "validateUserRoleNotEmpty", "validateUserRegistered" } )
    public UserData.View storeUser( @WsParam( from = PATH ) String organizationId,
                                    @WsValidateJson( schema = UserInfo.SCHEMA ) @WsParam( from = BODY ) UserInfo user,
                                    @WsParam( from = QUERY ) Optional<String> role,
                                    @WsParam( from = SESSION ) UserData loggedUser ) {
        if( user.create ) {
            UserData userCreated = accounts.createUser( user, role.map( r -> new HashMap<>( Map.of( organizationId, r ) ) ).orElse( null ) );
            mailman.sendInvitedEmail( userCreated );
            return userCreated.view;
        }
        return accounts.updateUser( user.email, u -> u.update( user.firstName, user.lastName, user.ext ) )
            .orElseThrow().view;
    }

    @WsMethod( method = POST, path = "/register" )
    @WsValidate( "validateUserRegistered" )
    public UserData.View register( @WsValidateJson( schema = UserInfo.SCHEMA_REGISTRATION ) @WsParam( from = BODY ) UserInfo user,
                                   @WsParam( from = QUERY ) String organizationName ) {
        OrganizationData organizationData = accounts.storeOrganization( new Organization( organizationName ) );
        final String orgId = organizationData.organization.id;
        UserData userCreated = accounts.createUser( user, new HashMap<>( Map.of( orgId, ORGANIZATION_ADMIN ) ) );
        mailman.sendRegisteredEmail( userCreated );
        return userCreated.view;
    }

    @WsMethod( method = POST, path = "/{organizationId}/users/passwd" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { ORGANIZATION_USER_PASSWD, USER_PASSWD } )
    @WsValidate( { "validateOrganizationAccess", "validatePasswdOrganization", "validateUserAccess" } )
    public Optional<UserData.View> passwd( @WsParam( from = PATH ) String organizationId,
                                           @WsParam( from = BODY ) @WsValidateJson( schema = Passwd.SCHEMA ) Passwd passwd,
                                           @WsParam( from = SESSION ) UserData loggedUser ) {
        return accounts.passwd( passwd.email, passwd.password ).map( u -> u.view );
    }

    @WsMethod( method = GET, path = "/{organizationId}/users/apikey/{email}",
        description = "Generate new apikey for user" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { ORGANIZATION_APIKEY, USER_APIKEY } )
    @WsValidate( { "validateOrganizationAccess", "validateCreateApikey" } )
    public Optional<String> refreshApikey( @WsParam( from = PATH ) String organizationId,
                                           @WsParam( from = PATH ) String email,
                                           @WsParam( from = SESSION ) User loggedUser ) {

        return accounts.refreshApikey( email ).map( u -> u.user.apiKey );
    }

    @WsMethod( method = GET, path = "/{organizationId}/users/ban/{email}" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { BAN_USER } )
    @WsValidate( { "validateAdminBanAccess" } )
    public Optional<UserData.View> ban( @WsParam( from = PATH ) String organizationId,
                                        @WsParam( from = PATH ) String email,
                                        @WsParam( from = SESSION ) UserData loggedUser ) {


        return accounts.ban( email, true ).map( u -> u.view );
    }

    @WsMethod( method = GET, path = "/{organizationId}/users/delete/{email}" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { ACCOUNT_DELETE } )
    public Optional<UserData.View> delete( @WsParam( from = PATH ) String organizationId,
                                           @WsParam( from = PATH ) String email,
                                           @WsParam( from = SESSION ) UserData loggedUser ) {
        return accounts.delete( email ).map( u -> u.view );
    }

    @WsMethod( method = GET, path = "/{organizationId}/users/unban/{email}" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { UNBAN_USER } )
    public Optional<UserData.View> unban( @WsParam( from = PATH ) String organizationId,
                                          @WsParam( from = PATH ) String email,
                                          @WsParam( from = SESSION ) UserData loggedUser ) {
        return accounts.ban( email, false ).map( u -> u.view );
    }

    @WsMethod( method = GET, path = "/users/confirm/{email}" )
    @WsValidate( { "validateUserLoggedIn" } )
    @SneakyThrows
    public Response confirm( @WsParam( from = PATH ) String email,
                             @WsParam( from = SESSION ) Optional<UserData> loggedUser ) {
        UserInfo user = loggedUser.get().user;
        URI redirect = new URIBuilder( confirmUrlFinish )
            .addParameter( "apiKey", user.apiKey )
            .addParameter( "accessKey", user.getAccessKey() )
            .addParameter( "email", user.getEmail() )
            .addParameter( "passwd", String.valueOf( !user.hasPassword() ) )
            .build();

        UserData userConfirmed = accounts.confirm( email ).orElse( null );
        return userConfirmed != null ? Response.redirect( redirect ) : Response.notFound();
    }

    @WsMethod( method = POST, path = "/{organizationId}/assign" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { ASSIGN_ROLE } )
    public Optional<UserData.View> assignRole( @WsParam( from = PATH ) String organizationId,
                                               @WsParam( from = QUERY ) String email,
                                               @WsParam( from = QUERY ) String role,
                                               @WsParam( from = SESSION ) UserData loggedUser ) {
        return accounts.assignRole( email, organizationId, role ).map( u -> u.view );
    }

    protected ValidationErrors validateUserAccess( String organizationId, @Nonnull Passwd passwd, @Nonnull UserData loggedUser ) {
        return Objects.equals( passwd.email, loggedUser.user.email )
            || isSystem( loggedUser )
            || isOrganizationAdmin( loggedUser, organizationId )
            ? empty()
            : error( FORBIDDEN, "cannot manage " + passwd.email );
    }

    protected ValidationErrors validateUsersOrganization( String organizationId, UserData loggedUser ) {
        return validateEmailOrganizationAccess( organizationId, loggedUser.user.email );
    }

    protected ValidationErrors validatePasswdOrganization( String organizationId, @Nonnull Passwd passwd ) {
        return validateEmailOrganizationAccess( organizationId, passwd.email );
    }

    protected ValidationErrors validateCreateApikey( UserData loggedUser, String organizationId, @Nonnull String email ) {
        final Optional<String> role = loggedUser.getRole( organizationId );
        if( role.isPresent() && ORGANIZATION_ADMIN.equals( role.get() )
            || loggedUser.user.email.equals( email )
            || isSystemAdmin( loggedUser ) ) {
            return empty();
        }
        return error( FORBIDDEN, "User " + loggedUser.user.email + " is not allowed to change apikey of another user " + email );
    }

    private ValidationErrors validateEmailOrganizationAccess( String organizationId, String email ) {
        return accounts.getUser( email )
            .filter( u -> !u.belongsToOrganization( organizationId ) && u.getRole( SYSTEM ).isEmpty() )
            .map( u -> error( FORBIDDEN, "User belongs to other organization  " + email + "::" + organizationId ) )
            .orElse( empty() );
    }

    protected ValidationErrors validateUserRegistered( @Nonnull UserInfo user ) {
        if( !selfRegistrationEnabled ) return error( Http.StatusCode.NOT_FOUND, "not available" );
        var existing = accounts.getUser( user.email );
        if( existing.isPresent() && user.create )
            return error( Http.StatusCode.CONFLICT, "user with email " + user.email + " already exists" );
        else if( existing.isEmpty() && !user.create )
            return error( Http.StatusCode.NOT_FOUND, "user " + user.email + " does not exists" );
        else return empty();
    }

    protected ValidationErrors validateAdminRole( @Nonnull String organizationId, Optional<String> role, @Nonnull UserData loggedUser ) {
        if( role.isPresent() && ADMIN.equals( role.get() ) && !isSystemAdmin( loggedUser ) ) {
            return error( FORBIDDEN, "Only ADMIN can create another ADMIN" );
        } else return empty();
    }

    private boolean isSystemAdmin( UserData loggedUser ) {
        return ADMIN.equals( loggedUser.roles.get( SYSTEM ) );
    }

    protected ValidationErrors validateUserRoleNotEmpty( @Nonnull UserData loggedUser ) {
        return loggedUser.roles.isEmpty()
            ? error( FORBIDDEN, "User role is required" )
            : empty();
    }

    protected ValidationErrors validateAdminBanAccess( String email, UserData loggedUser, String organizationId ) {
        if( accounts.getUser( email ).isPresent() && ADMIN.equals( accounts.getUser( email ).get().getRole( organizationId ).orElse( null ) )
            && !ADMIN.equals( loggedUser.getRole( organizationId ).orElse( null ) )
            && !isSystemAdmin( loggedUser ) ) {
            return error( "ADMIN can be banned only by other ADMIN" );
        } else {
            return empty();
        }
    }

    public static class Passwd {
        public static final String SCHEMA = "/account/passwd.schema.conf";
        String email;
        String password;
    }
}
