
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
import oap.util.Result;
import oap.ws.sso.jwt.JwtExtractor;
import oap.ws.sso.jwt.JwtTokenGenerator;
import oap.ws.sso.model.User;

import java.util.Optional;

import static oap.ws.sso.AuthenticationFailure.MFA_REQUIRED;
import static oap.ws.sso.AuthenticationFailure.UNAUTHENTICATED;

@Slf4j
public class JwtUserAuthenticator implements Authenticator {

    private JwtTokenGenerator jwtTokenGenerator;
    private JwtExtractor jwtExtractor;
    private UserProvider userProvider;

    public JwtUserAuthenticator( UserProvider userProvider, JwtTokenGenerator jwtTokenGenerator, JwtExtractor jwtExtractor ) {
        this.userProvider = userProvider;
        this.jwtTokenGenerator = jwtTokenGenerator;
        this.jwtExtractor = jwtExtractor;
    }

    @Override
    public Optional<Authentication> authenticate( String authId ) {
        if( jwtExtractor.verifyToken( authId ) ) {
            return userProvider.getUser( jwtExtractor.getUserEmail( authId ) ).map( user -> new Authentication( authId, user ) );
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Result<Authentication, AuthenticationFailure> authenticate( String email, String password, Optional<String> tfaCode ) {
        var authResult = userProvider.getAuthenticated( email, password, tfaCode );
        if( !authResult.isSuccess() ) {
            return Result.failure( authResult.getFailureValue() );
        }
        User user = authResult.getSuccessValue();
        try {
            var jwt = jwtTokenGenerator.generateToken( user );
            log.trace( "generating new authentication for user {} -> {}", user.getEmail(), jwt );
            Authentication authentication = new Authentication( jwt, user );
            return Result.success( authentication );
        } catch( Exception exception ) {
            log.error( "JWT creation failed {}", exception.getMessage() );
        }
        return null;
    }

    @Override
    public Optional<Authentication> authenticateTrusted( String email ) {
        return userProvider.getUser( email )
            .map( user -> {
                try {
                    var jwt = jwtTokenGenerator.generateToken( user );
                    log.trace( "generating new authentication for user {} -> {}", user.getEmail(), jwt );
                    return new Authentication( jwt, user );
                } catch( Exception exception ) {
                    log.error( "JWT creation failed {}", exception.getMessage() );
                }
                return null;
            } );
    }

    @Override
    public Optional<Authentication> authenticateWithApiKey( String accessKey, String apiKey ) {
        return userProvider.getAuthenticatedByApiKey( accessKey, apiKey )
            .map( user -> {
                try {
                    var jwt = jwtTokenGenerator.generateToken( user );
                    log.trace( "generating temporary authentication for user {} -> {}", user.getEmail(), jwt );
                    return new Authentication( jwt, user );
                } catch( Exception exception ) {
                    log.error( "JWT creation failed {}", exception.getMessage() );
                    return null;
                }
            } );
    }

    @Override
    public void invalidate( String email ) {
        // No op
    }
}
