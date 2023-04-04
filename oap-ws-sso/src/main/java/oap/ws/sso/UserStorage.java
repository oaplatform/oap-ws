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

import lombok.extern.slf4j.Slf4j;
import oap.id.Identifier;
import oap.storage.MemoryStorage;
import oap.util.Result;
import oap.ws.sso.model.User;
import oap.ws.sso.model.UserData;
import org.joda.time.DateTime;

import java.util.Optional;

import static oap.storage.Storage.Lock.SERIALIZED;
import static oap.ws.sso.AuthenticationFailure.MFA_REQUIRED;
import static oap.ws.sso.AuthenticationFailure.UNAUTHENTICATED;
import static org.joda.time.DateTimeZone.UTC;

@Slf4j
public class UserStorage extends MemoryStorage<String, UserData> implements UserProvider {

    public UserStorage() {
        super( Identifier.<UserData>forId( u -> u.user.email, ( o, id ) -> o.user.email = id )
            .suggestion( u -> u.user.email )
            .build(), SERIALIZED );
    }

    @Override
    public Optional<? extends User> getUser( String email ) {
        return get( email );
    }

    @Override
    public Result<? extends User, AuthenticationFailure> getAuthenticated( String email, String password, Optional<String> tfaCode ) {
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
