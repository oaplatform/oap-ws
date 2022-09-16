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

package oap.ws.sso.interceptor;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Collections;
import java.util.List;

public class Auth0TokenProvider extends AbstractAuthTokenProvider {

    public Auth0TokenProvider( String secret, String issuer ) {
        super( secret, issuer );
    }

    @Override
    public List<String> getPermissions( String token ) {
        final DecodedJWT decodedJWT = decodeJWT( token );
        final Claim permissions = decodedJWT.getClaims().get( "permissions" );
        return permissions != null ? permissions.asList( String.class ) : Collections.emptyList();
    }

    @Override
    public String getUserId( String token ) {
        final DecodedJWT decodedJWT = decodeJWT( token );
        return decodedJWT.getClaims().get( "user_id" ).asString();
    }

    @Override
    public List<String> getAccounts( String token ) {
        final DecodedJWT decodedJWT = decodeJWT( token );
        return ( List ) decodedJWT.getClaims().get( "app_metadata" ).asMap().get( "accounts" );
    }

}
