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


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import oap.util.Cuid;

import java.util.Map;
import java.util.Optional;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Slf4j
public class UserAuthenticator implements Authenticator {

    private final Cache<String, Authentication> authentications;
    private final UserProvider userProvider;
    private Cuid cuid = Cuid.UNIQUE;

    public UserAuthenticator( UserProvider userProvider, long expirationTime ) {
        this.authentications = CacheBuilder.newBuilder()
            .expireAfterAccess( expirationTime, MILLISECONDS )
            .build();
        this.userProvider = userProvider;
    }

    @Override
    public Optional<Authentication> authenticate( String authId ) {
        log.trace( "available authentications: {}", authentications.asMap().keySet() );
        var auth = Optional.ofNullable( authentications.getIfPresent( authId ) );
        log.trace( "authentication for {} -> {}", authId, auth );
        return auth;
    }

    @Override
    public Optional<Authentication> authenticate( String email, String password ) {
        return userProvider.getAuthenticated( email, password )
            .map( user -> {
                var id = cuid.next();
                log.trace( "generating new authentication for user {} -> {}", user.getEmail(), id );
                Authentication authentication = new Authentication( id, user );
                authentications.put( authentication.id, authentication );
                return authentication;
            } );
    }

    @Override
    public Optional<Authentication> authenticateTrusted( String email ) {
        return userProvider.getUser( email )
            .map( user -> {
                var id = cuid.next();
                log.trace( "generating new authentication for user {} -> {}", user.getEmail(), id );
                Authentication authentication = new Authentication( id, user );
                authentications.put( authentication.id, authentication );
                return authentication;
            } );
    }

    @Override
    public void invalidateByEmail( String email ) {
        for( Map.Entry<String, Authentication> entry : authentications.asMap().entrySet() ) {
            if( entry.getValue().user.getEmail().equalsIgnoreCase( email ) ) {
                log.debug( "Deleting token [{}]...", entry.getKey() );
                authentications.invalidate( entry.getKey() );

                return;
            }
        }
    }

    @Override
    public Optional<Authentication> authenticateWithApiKey( String accessKey, String apiKey ) {
        return userProvider.getAuthenticatedByApiKey( accessKey, apiKey )
            .map( user -> {
                var id = cuid.next();
                log.trace( "generating temporaty authentication for user {} -> {}", user.getEmail(), id );
                return new Authentication( id, user );
            } );
    }

}
