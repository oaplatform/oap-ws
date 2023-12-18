/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account;

import lombok.extern.slf4j.Slf4j;
import oap.id.Identifier;
import oap.storage.MemoryStorage;
import oap.util.Result;
import oap.ws.sso.AuthenticationFailure;
import oap.ws.sso.User;
import org.joda.time.DateTime;

import java.util.Map;
import java.util.Optional;

import static oap.storage.Storage.Lock.SERIALIZED;
import static oap.ws.account.utils.TfaUtils.getTOTPCode;
import static oap.ws.sso.AuthenticationFailure.TFA_REQUIRED;
import static oap.ws.sso.AuthenticationFailure.UNAUTHENTICATED;
import static oap.ws.sso.AuthenticationFailure.WRONG_TFA_CODE;
import static org.joda.time.DateTimeZone.UTC;

@Slf4j
public class UserStorage extends MemoryStorage<String, UserData> implements oap.ws.sso.UserProvider {
    public final String defaultSystemAdminEmail;
    public final String defaultSystemAdminPassword;
    public final String defaultSystemAdminFirstName;
    public final String defaultSystemAdminLastName;
    public final Map<String, String> defaultSystemAdminRoles;
    public final boolean defaultSystemAdminReadOnly;

    /**
     * @param defaultSystemAdminEmail     default user email
     * @param defaultSystemAdminPassword  default user password
     * @param defaultSystemAdminFirstName default user first name
     * @param defaultSystemAdminLastName  default user last name
     * @param defaultSystemAdminRoles     default user roles map ( hocon/json format )
     * @param defaultSystemAdminReadOnly  if true, the storage modifies the default user to the default values on startup
     */
    public UserStorage( String defaultSystemAdminEmail,
                        String defaultSystemAdminPassword,
                        String defaultSystemAdminFirstName,
                        String defaultSystemAdminLastName,
                        Map<String, String> defaultSystemAdminRoles,
                        boolean defaultSystemAdminReadOnly ) {
        super( Identifier.<UserData>forId( u -> u.user.email, ( o, id ) -> o.user.email = id )
            .suggestion( u -> u.user.email )
            .build(), SERIALIZED );

        this.defaultSystemAdminEmail = defaultSystemAdminEmail;
        this.defaultSystemAdminPassword = defaultSystemAdminPassword;
        this.defaultSystemAdminFirstName = defaultSystemAdminFirstName;
        this.defaultSystemAdminLastName = defaultSystemAdminLastName;
        this.defaultSystemAdminRoles = defaultSystemAdminRoles;
        this.defaultSystemAdminReadOnly = defaultSystemAdminReadOnly;
    }

    public void start() {
        log.info( "default email {} firstName {} lastName {} roles {} ro {}",
            defaultSystemAdminEmail, defaultSystemAdminFirstName, defaultSystemAdminLastName, defaultSystemAdminRoles, defaultSystemAdminReadOnly );

        update( defaultSystemAdminEmail, u -> {
            if( defaultSystemAdminReadOnly ) {
                u.user.email = defaultSystemAdminEmail;
                u.user.encryptPassword( defaultSystemAdminPassword );
                u.user.firstName = defaultSystemAdminFirstName;
                u.user.lastName = defaultSystemAdminLastName;
                u.user.confirmed = true;
                u.roles.clear();
                u.roles.putAll( defaultSystemAdminRoles );
                u.user.defaultOrganization = defaultSystemAdminRoles.keySet().stream().findAny().get();
            }

            return u;
        }, () -> {
            var user = new oap.ws.account.User( defaultSystemAdminEmail, defaultSystemAdminFirstName, defaultSystemAdminLastName, defaultSystemAdminPassword, true );
            user.defaultOrganization = defaultSystemAdminRoles.keySet().stream().findAny().get();
            return new UserData( user, defaultSystemAdminRoles );
        } );
    }

    @Override
    public Optional<? extends oap.ws.sso.User> getUser( String email ) {
        return get( email );
    }

    @Override
    public Result<? extends User, AuthenticationFailure> getAuthenticated( String email, String password, Optional<String> tfaCode ) {
        Optional<UserData> authenticated = get( email )
            .filter( u -> u.authenticate( password ) );

        return getAuthenticationResult( email, tfaCode, authenticated );
    }

    @Override
    public Result<? extends User, AuthenticationFailure>
    getAuthenticated( String email, Optional<String> tfaCode ) {
        Optional<UserData> authenticated = get( email );
        return getAuthenticationResult( email, tfaCode, authenticated );
    }

    private Result<? extends User, AuthenticationFailure> getAuthenticationResult( String email, Optional<String> tfaCode, Optional<UserData> authenticated ) {
        if( authenticated.isPresent() ) {
            UserData userData = authenticated.get();
            if( !userData.user.tfaEnabled ) {
                update( email, user -> {
                    user.lastLogin = DateTime.now( UTC );
                    return user;
                } );
                return Result.success( userData );
            } else {
                if( tfaCode.isEmpty() ) {
                    return Result.failure( TFA_REQUIRED );
                }
                boolean tfaCheck = tfaCode.map( code -> getTOTPCode( userData.user.getSecretKey() ).equals( code ) )
                    .orElse( false );
                return tfaCheck ? Result.success( userData ) : Result.failure( WRONG_TFA_CODE );
            }
        }
        return Result.failure( UNAUTHENTICATED );
    }


    @Override
    public Optional<? extends User> getAuthenticatedByApiKey( String accessKey, String apiKey ) {
        return select().filter( u -> u.authenticate( accessKey, apiKey ) ).findAny();
    }

    public void deleteAllPermanently() {
        for( var user : this ) memory.removePermanently( user.user.email );
    }
}
