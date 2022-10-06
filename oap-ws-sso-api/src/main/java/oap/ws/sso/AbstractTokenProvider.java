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

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;

import java.security.interfaces.RSAPublicKey;

@Slf4j
public abstract class AbstractTokenProvider implements AuthTokenProvider {

    private final String domain;

    public AbstractTokenProvider( String domain ) {
        this.domain = domain;
    }

    @Override
    public boolean verifyToken( String token ) {
        try {
            final DecodedJWT decodedJWT = decodeJWT( token );
            return decodedJWT != null;
        } catch( JWTVerificationException e ) {
            log.trace( "Token not valid:", e );
            return false;
        }
    }

    protected DecodedJWT decodeJWT( String token ) {
        JwkProvider provider = new UrlJwkProvider( domain );
        try {
            DecodedJWT jwt = JWT.decode( token );
            Jwk jwk = provider.get( jwt.getKeyId() );
            Algorithm algorithm = Algorithm.RSA256( ( RSAPublicKey ) jwk.getPublicKey(), null );
            JWTVerifier verifier = JWT.require( algorithm )
                .withIssuer( domain )
                .build();
            return verifier.verify( token );
        } catch( Exception e ) {
            return null;
        }
    }

    public static String extractBearerToken( String authorization ) {
        if( authorization != null && authorization.startsWith( "Bearer " ) ) {
            return authorization.substring( "Bearer token ".length() );
        }
        return authorization;
    }
}
