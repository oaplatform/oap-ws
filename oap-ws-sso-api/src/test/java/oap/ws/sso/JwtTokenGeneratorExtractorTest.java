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

import oap.util.Pair;
import oap.ws.sso.jwt.BasicJwtExtractor;
import oap.ws.sso.jwt.JwtExtractor;
import oap.ws.sso.jwt.JwtTokenGenerator;
import org.testng.annotations.Test;

import java.util.Set;

import static oap.testng.Asserts.assertString;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


public class JwtTokenGeneratorExtractorTest extends AbstractUserTest {

    private JwtTokenGenerator jwtTokenGenerator = new JwtTokenGenerator( "secret", "issuer", 100000 );
    private JwtExtractor jwtExtractor = new BasicJwtExtractor( "secret", "issuer", new SecurityRoles( new TestSecurityRolesProvider() ) );

    @Test
    public void generateAndExtractToken() {
        final String token = jwtTokenGenerator.generateToken( new TestUser( "email@email.com", "password", Pair.of( "org1", "ADMIN" ) ) );
        assertString( token ).isNotEmpty();
        assertTrue( jwtExtractor.verifyToken( token ) );
        assertEquals( jwtExtractor.getUserEmail( token ), "email@email.com" );
        assertEquals( jwtExtractor.getPermissions( token, "org1" ), Set.of( "accounts:list", "accounts:create" ) );
    }
}
