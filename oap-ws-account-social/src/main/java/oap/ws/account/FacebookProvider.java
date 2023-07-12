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

package oap.ws.account;


import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.Version;
import com.restfb.exception.FacebookException;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
@Slf4j
public class FacebookProvider implements OauthProviderService {
    private static final String FACEBOOK_FIELDS = "name,first_name,last_name,email";

    public Optional<TokenInfo> getTokenInfo( String accessToken ) {

        FacebookClient facebookClient = new DefaultFacebookClient( accessToken, Version.LATEST );
        try {
            com.restfb.types.User fbUser = facebookClient.fetchObject( "me", com.restfb.types.User.class,
                Parameter.with( "fields", FACEBOOK_FIELDS ) );
            return Optional.of( new TokenInfo( fbUser.getEmail(), fbUser.getFirstName(), fbUser.getLastName() ) );
        } catch( FacebookException e ) {
            log.error( "Failed to extract user from facebook token", e );
            throw e;
        }
    }
}
