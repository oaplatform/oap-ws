/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account;

import de.taimos.totp.TOTP;
import lombok.extern.slf4j.Slf4j;
import oap.id.Identifier;
import oap.storage.MemoryStorage;
import oap.util.Result;
import oap.ws.sso.AuthenticationFailure;
import oap.ws.sso.User;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.Optional;

import static oap.storage.Storage.Lock.SERIALIZED;
import static oap.ws.sso.AuthenticationFailure.MFA_REQUIRED;
import static oap.ws.sso.AuthenticationFailure.UNAUTHENTICATED;
import static org.joda.time.DateTimeZone.UTC;

@Slf4j
public class UserStorage extends MemoryStorage<String, UserData> implements oap.ws.sso.UserProvider {

    public UserStorage() {
        super( Identifier.<UserData>forId( u -> u.user.email, ( o, id ) -> o.user.email = id )
            .suggestion( u -> u.user.email )
            .build(), SERIALIZED );
    }

    @Override
    public Optional<? extends oap.ws.sso.User> getUser( String email ) {
        return get( email );
    }

    @Override
    public Result<? extends User, AuthenticationFailure> getAuthenticated( String email, String password, Optional<String> tfaCode ) {
        Optional<UserData> authenticated = get( email )
            .filter( u -> u.authenticate( password ) );

        if( authenticated.isPresent() ) {
            UserData userData = authenticated.get();
            if( !userData.user.tfaEnabled ) {
                update( email, user -> {
                    user.lastLogin = DateTime.now( UTC );
                    return user;
                } );
                return Result.success( userData );
            } else {
                boolean tfaCheck = tfaCode.map( code -> getTOTPCode( userData.user.getSecretKey() ).equals( code ) )
                    .orElse( false );
                return tfaCheck ? Result.success( userData ) : Result.failure( MFA_REQUIRED );
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

    private static String getTOTPCode( String secretKey ) {
        Base32 base32 = new Base32();
        byte[] bytes = base32.decode( secretKey );
        String hexKey = Hex.encodeHexString( bytes );
        return TOTP.getOTP( hexKey );
    }
}
