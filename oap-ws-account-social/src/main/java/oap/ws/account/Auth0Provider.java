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

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class Auth0Provider implements OauthProviderService {

    private final String issuer;
    private final String claimPrefix;
    private final String secret;

    public Auth0Provider( String issuer, String claimPrefix, String secret ) {
        this.issuer = issuer;
        this.claimPrefix = claimPrefix;
        this.secret = secret;
    }

    @Override
    public Optional<TokenInfo> getTokenInfo( String accessToken ) {
        try {
            Algorithm algorithm = Algorithm.HMAC256( secret );
            JWTVerifier verifier = JWT.require( algorithm )
                .withIssuer( issuer )
                .build();
            final DecodedJWT jwt = verifier.verify( accessToken );

            String email = jwt.getClaims().get( claimPrefix + "email" ).asString();
            String firstName = jwt.getClaims().get( claimPrefix + "given_name" ).asString();
            String lastName = jwt.getClaims().get( claimPrefix + "family_name" ).asString();
            return Optional.of( new TokenInfo( email, firstName, lastName ) );
        } catch( JWTVerificationException e ) {
            log.error( "Failed to extract user from Auth0 token", e );
            throw e;
        }
    }
}
