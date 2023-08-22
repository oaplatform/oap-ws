/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account;

import lombok.extern.slf4j.Slf4j;
import oap.id.Identifier;
import oap.json.Binder;
import oap.reflect.TypeRef;
import oap.storage.MemoryStorage;
import oap.system.Env;
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

    public static final String DEFAULT_USER_EMAIL = "xenoss@xenoss.io";
    public static final String DEFAULT_USER_PASSWORD = "Xenoss123";
    public static final String DEFAULT_USER_FIRST_NAME = "System";
    public static final String DEFAULT_USER_LAST_NAME = "Admin";
    public static final String DEFAULT_USER_ROLES = "{\"DFLT\": \"ADMIN\", \"SYSTEM\": \"ADMIN\"}";
    public static final String DEFAULT_USER_READONLY = "true";
    private final String defaultUserEmail;
    private final String defaultUserPassword;
    private final String defaultUserFirstName;
    private final String defaultUserLastName;
    private final Map<String, String> defaultUserRoles;
    private final boolean defaultUserReadOnly;

    public UserStorage() {
        this(
            Env.get( "DEFAULT_USER_EMAIL", DEFAULT_USER_EMAIL ),
            Env.get( "DEFAULT_USER_PASSWORD", DEFAULT_USER_PASSWORD ),
            Env.get( "DEFAULT_USER_FIRST_NAME", DEFAULT_USER_FIRST_NAME ),
            Env.get( "DEFAULT_USER_LAST_NAME", DEFAULT_USER_LAST_NAME ),
            Binder.hocon.unmarshal( new TypeRef<Map<String, String>>() {}, Env.get( "DEFAULT_USER_ROLES", DEFAULT_USER_ROLES ) ),
            Boolean.parseBoolean( Env.get( "DEFAULT_USER_READONLY", DEFAULT_USER_READONLY ) )
        );
    }

    /**
     * @param defaultUserEmail            default user email
     * @param defaultUserPassword         default user password
     * @param defaultUserFirstName        default user first name
     * @param defaultUserLastName         default user last name
     * @param defaultUserRoles            default user roles map ( hocon/json format )
     * @param defaultUserReadOnly if true, the storage modifies the default user to the default values on startup
     */
    public UserStorage( String defaultUserEmail,
                        String defaultUserPassword,
                        String defaultUserFirstName,
                        String defaultUserLastName,
                        Map<String, String> defaultUserRoles,
                        boolean defaultUserReadOnly ) {
        super( Identifier.<UserData>forId( u -> u.user.email, ( o, id ) -> o.user.email = id )
            .suggestion( u -> u.user.email )
            .build(), SERIALIZED );

        this.defaultUserEmail = defaultUserEmail;
        this.defaultUserPassword = defaultUserPassword;
        this.defaultUserFirstName = defaultUserFirstName;
        this.defaultUserLastName = defaultUserLastName;
        this.defaultUserRoles = defaultUserRoles;
        this.defaultUserReadOnly = defaultUserReadOnly;
    }

    public void start() {
        update( defaultUserEmail, u -> {
            if( defaultUserReadOnly ) {
                u.user.email = defaultUserEmail;
                u.user.encryptPassword( defaultUserEmail );
                u.user.firstName = defaultUserFirstName;
                u.user.lastName = defaultUserLastName;
                u.user.confirm( true );
                u.roles.clear();
                u.roles.putAll( defaultUserRoles );
            }

            return u;
        }, () -> {
            var user = new oap.ws.account.User( defaultUserEmail, defaultUserFirstName, defaultUserLastName, defaultUserPassword, true );
            return new UserData( user, defaultUserRoles );
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
    public Result<? extends User, AuthenticationFailure> getAuthenticated( String email, Optional<String> tfaCode ) {
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
