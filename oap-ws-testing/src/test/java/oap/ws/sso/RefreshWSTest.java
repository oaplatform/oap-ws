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


import oap.json.Binder;
import oap.ws.sso.interceptor.ThrottleLoginInterceptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

import static oap.http.Http.StatusCode.OK;
import static oap.http.Http.StatusCode.UNAUTHORIZED;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.assertPost;
import static oap.http.testng.HttpAsserts.httpUrl;
import static oap.util.Pair.__;
import static org.testng.Assert.assertNotEquals;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

public class RefreshWSTest extends IntegratedTest {


    @BeforeMethod
    public void beforeMethod() {
        kernelFixture.service( "oap-ws-sso-api", ThrottleLoginInterceptor.class ).delay = -1;
    }

    @Test
    public void refreshResponseTest() throws InterruptedException {
        userProvider().addUser( new TestUser( "admin@admin.com", "pass", __( "r1", "ADMIN" ) ) );
        final String[] accessToken = new String[1];
        final String[] refreshToken = new String[1];
        assertPost( httpUrl( "/auth/login" ), "{ \"email\":\"admin@admin.com\",\"password\": \"pass\"}" )
            .hasCode( OK ).satisfies( resp -> {
                Map<String, String> response = Binder.json.unmarshal( Map.class, resp.contentString() );
                assertTrue( response.containsKey( "accessToken" ) );
                assertTrue( response.containsKey( "refreshToken" ) );
                accessToken[0] = response.get( "accessToken" );
                refreshToken[0] = response.get( "refreshToken" );
            } );
        Thread.sleep( 2000L );
        assertGet( httpUrl( "/refresh" ), Map.of( "organizationId", "r1" ), Map.of() ).satisfies( resp -> {
            Map<String, String> response = Binder.json.unmarshal( Map.class, resp.contentString() );
            assertTrue( response.containsKey( "accessToken" ) );
            assertNotEquals( response.get( "accessToken" ), accessToken[0] );
            assertTrue( response.containsKey( "refreshToken" ) );
            System.out.println( "RefreshToken in response:" + response.get( "refreshToken" ) );
            System.out.println( "RefreshToken previous:" + refreshToken[0] );
            assertNotEquals( response.get( "refreshToken" ), refreshToken[0] );
        } );
    }

    @Test
    public void refreshResponseWithEWrongOrgIdTest() {
        userProvider().addUser( new TestUser( "admin@admin.com", "pass", __( "r1", "ADMIN" ) ) );
        assertPost( httpUrl( "/auth/login" ), "{ \"email\":\"admin@admin.com\",\"password\": \"pass\"}" )
            .hasCode( OK ).satisfies( resp -> {
                Map<String, String> response = Binder.json.unmarshal( Map.class, resp.contentString() );
                assertTrue( response.containsKey( "accessToken" ) );
                assertTrue( response.containsKey( "refreshToken" ) );
            } );
        assertGet( httpUrl( "/refresh" ), Map.of( "organizationId", "r2" ), Map.of() ).hasCode( UNAUTHORIZED );
    }

    @Test
    public void refreshResponseWithoutOrgIdTest() {
        final TestUser testUser = userProvider().addUser( new TestUser( "admin@admin.com", "pass", Map.of( "r1", "ADMIN", "r2", "USER" ) ) );
        testUser.defaultOrganization = "r2";
        final String[] refreshToken = new String[1];
        assertPost( httpUrl( "/auth/login" ), "{ \"email\":\"admin@admin.com\",\"password\": \"pass\"}" )
            .hasCode( OK ).satisfies( resp -> {
                Map<String, String> response = Binder.json.unmarshal( Map.class, resp.contentString() );
                assertTrue( response.containsKey( "accessToken" ) );
                assertTrue( response.containsKey( "refreshToken" ) );
                refreshToken[0] = response.get( "refreshToken" );
            } );
        assertGet( httpUrl( "/refresh" ) ).hasCode( OK ).satisfies( resp -> {
            Map<String, String> response = Binder.json.unmarshal( Map.class, resp.contentString() );
            assertTrue( response.containsKey( "accessToken" ) );
            assertTrue( response.containsKey( "refreshToken" ) );
            final String organizationId = tokenExtractor().getOrganizationId( response.get( "accessToken" ) );
            assertEquals( "r2", organizationId );
        } );
    }


    @Test
    public void refreshWithExpiredRefreshToken() {
        final String expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyIjoidGVzdEB0ZXN0LmlvIiwiaXNzIjoiPGNoYW5nZSBtZT4iLCJleHAiOjE2OTY5MjYzODJ9.VZdySTBEThoTOwB73JMNpBgCjaXGvlmes8_13Bs3dXg";
        Map<String, Object> headers = Map.of( "Cookie", "refreshToken=" + expiredToken );
        assertGet( httpUrl( "/refresh" ), Map.of(), headers ).hasCode( 401 );
    }

}
