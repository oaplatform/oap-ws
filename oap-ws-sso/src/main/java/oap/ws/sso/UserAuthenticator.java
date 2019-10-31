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
        return Optional.ofNullable( authentications.getIfPresent( authId ) );
    }

    @Override
    public Optional<Authentication> authenticate( String email, String password ) {
        return userProvider.getAuthenticated( email, password )
            .map( user -> {
                log.debug( "Generating new token for user [{}]...", user.getEmail() );
                Authentication authentication = new Authentication( cuid.next(), user );
                authentications.put( authentication.id, authentication );
                return authentication;
            } );
    }

//    public Optional<Authentication> authenticate( String email ) {
//        return userProvider.getUser( email ).map( this::getToken );
//    }

//    private synchronized Authentication getToken( User user ) {
//
//        //todo why is this?
//        for( Authentication authentication : authentications.asMap().values() )
//            if( Objects.equals( authentication.user.getEmail(), user.getEmail() ) ) return authentication;
//
//        log.debug( "Generating new token for user [{}]...", user.getEmail() );
//        Authentication authentication = new Authentication( cuid.next(), user );
//
//        authentications.put( authentication.id, authentication );
//
//        return authentication;
//    }

    public synchronized Optional<Authentication> getToken( String tokenId ) {
        return Optional.ofNullable( authentications.getIfPresent( tokenId ) );
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

}
