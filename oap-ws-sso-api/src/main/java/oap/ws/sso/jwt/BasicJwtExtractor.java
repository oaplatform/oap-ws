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

package oap.ws.sso.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import oap.ws.sso.SecurityRoles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static oap.ws.sso.WsSecurity.SYSTEM;

public class BasicJwtExtractor extends AbstractJwtExtractor {

    private final String secret;
    private final String issuer;
    private final SecurityRoles roles;

    public BasicJwtExtractor( String secret, String issuer, SecurityRoles roles ) {
        this.secret = secret;
        this.issuer = issuer;
        this.roles = roles;
    }

    @Override
    protected DecodedJWT decodeJWT( String token ) {
        if( token == null )
            return null;
        Algorithm algorithm = Algorithm.HMAC256( secret );
        JWTVerifier verifier = JWT.require( algorithm )
            .withIssuer( issuer )
            .build();
        return verifier.verify( token );
    }

    @Override
    public List<String> getPermissions( String token, String organizationId ) {
        final String role;
        final DecodedJWT decodedJWT = decodeJWT( token );
        if( decodedJWT != null ) {
            final Claim tokenRoles = decodedJWT.getClaims().get( "roles" );
            if( tokenRoles != null ) {
                final Map<String, Object> rolesByOrganization = tokenRoles.asMap();
                if( rolesByOrganization.get( SYSTEM ) != null ) {
                    role = ( String ) rolesByOrganization.get( SYSTEM );
                } else role = ( String ) rolesByOrganization.get( organizationId );
                if( role != null ) {
                    return new ArrayList<>( this.roles.permissionsOf( role ) );
                }
            }
        }
        return Collections.emptyList();
    }

    @Override
    public String getUserEmail( String token ) {
        final DecodedJWT decodedJWT = decodeJWT( token );
        final Claim user = decodedJWT.getClaims().get( "user" );
        if( user != null ) {
            return user.asString();
        }
        return null;
    }

    @Override
    public String getOrganizationId( String token ) {
        final DecodedJWT decodedJWT = decodeJWT( token );
        final Claim orgId = decodedJWT.getClaims().get( "org_id" );
        if( orgId != null ) {
            return orgId.asString();
        }
        return null;
    }

}
