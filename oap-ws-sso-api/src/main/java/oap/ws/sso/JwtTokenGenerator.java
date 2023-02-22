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

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;

import java.util.Date;

public class JwtTokenGenerator {

    private final String accessSecret;
    private final String refreshSecret;
    private final String issuer;
    private final long accessSecretExpiration;
    private final long refreshSecretExpiration;

    public JwtTokenGenerator( String accessSecret, String refreshSecret, String issuer, long accessSecretExpiration, long refreshSecretExpiration ) {
        this.accessSecret = accessSecret;
        this.refreshSecret = refreshSecret;
        this.issuer = issuer;
        this.accessSecretExpiration = accessSecretExpiration;
        this.refreshSecretExpiration = refreshSecretExpiration;
    }

    public String generateAccessToken( User user ) throws JWTCreationException {
        Algorithm algorithm = Algorithm.HMAC256( accessSecret );
        return JWT.create()
            .withClaim( "user", user.getEmail() )
            .withClaim( "roles", user.getRoles() )
            .withIssuer( issuer )
            .withExpiresAt( new Date( System.currentTimeMillis() + accessSecretExpiration ) )
            .sign( algorithm );
    }

    public String generateRefreshToken( User user ) throws JWTCreationException {
        Algorithm algorithm = Algorithm.HMAC256( refreshSecret );
        return JWT.create()
            .withClaim( "user", user.getEmail() )
            .withIssuer( issuer )
            .withExpiresAt( new Date( System.currentTimeMillis() + refreshSecretExpiration ) )
            .sign( algorithm );
    }

}
