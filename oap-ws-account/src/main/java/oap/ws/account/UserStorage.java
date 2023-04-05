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

import java.util.Optional;

import static oap.storage.Storage.Lock.SERIALIZED;
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
    public Result<? extends oap.ws.sso.User, AuthenticationFailure> getAuthenticated( String email, String password, Optional<String> tfaCode ) {
        var authenticated = get( email )
            .filter( u -> {
                boolean result = u.authenticate( password );
                log.debug( "authenticating {}, banned = {}, confirmed = {} => {}", email, u.banned, u.user.confirmed, result );
                return result;
            } );
        if( authenticated.isPresent() ) {
            update( email, user -> {
                user.lastLogin = DateTime.now( UTC );
                return user;
            } );
            return Result.success( authenticated.get() );
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
