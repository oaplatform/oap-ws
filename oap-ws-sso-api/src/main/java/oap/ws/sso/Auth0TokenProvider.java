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

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Auth0TokenProvider extends AbstractTokenProvider {


    public Auth0TokenProvider( String domain ) {
        super( domain );
    }

    @Override
    public List<String> getPermissions( String token ) {
        final DecodedJWT decodedJWT = decodeJWT( token );
        final Claim permissions = decodedJWT.getClaims().get( "permissions" );
        if( permissions != null ) {
            return permissions.asList( String.class ).stream().map( this::adaptPermission ).collect( Collectors.toList() );
        }
        return Collections.emptyList();
    }

    @Override
    public String getUserId( String token ) {
        final DecodedJWT decodedJWT = decodeJWT( token );
        return decodedJWT.getClaims().get( "sub" ).asString();
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public List<String> getAccounts( String token ) {
        final DecodedJWT decodedJWT = decodeJWT( token );
        return ( List<String> ) decodedJWT.getClaims().get( "app_metadata" ).asMap().get( "accounts" );
    }

    /**
     * Converts xnss:store:account into account:store
     */
    private String adaptPermission( String auth0Permission ) {
        final String[] words = auth0Permission.split( ":" );
        if( words.length == 3 ) {
            return words[2] + ":" + words[1];
        }
        if( words.length == 2 ) {
            return words[1] + ":" + words[0];
        }
        return auth0Permission;
    }

}
