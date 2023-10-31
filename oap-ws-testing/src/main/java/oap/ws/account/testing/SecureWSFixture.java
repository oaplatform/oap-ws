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

package oap.ws.account.testing;

import oap.http.Http;
import org.joda.time.DateTime;

import static oap.http.testng.HttpAsserts.CookieHttpAssertion.assertCookie;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.assertPost;
import static oap.http.testng.HttpAsserts.getTestHttpPort;
import static oap.http.testng.HttpAsserts.httpUrl;
import static oap.io.content.ContentReader.ofString;
import static oap.testng.Asserts.contentOfTestResource;
import static oap.ws.sso.SSO.AUTHENTICATION_KEY;
import static oap.ws.sso.SSO.REFRESH_TOKEN_KEY;
import static org.joda.time.DateTimeZone.UTC;

/**
 * Created by igor.petrenko on 2021-02-24.
 */
public class SecureWSFixture {
    public static void assertLogin( String login, String password ) {
        assertLogin( login, password, getTestHttpPort().orElse( 80 ) );
    }

    public static void assertLogin( String login, String password, int port ) {
        assertPost( httpUrl( port, "/auth/login" ), "{  \"email\": \"" + login + "\",  \"password\": \"" + password + "\"}" )
            .hasCode( Http.StatusCode.OK )
            .containsCookie( AUTHENTICATION_KEY, cookie -> assertCookie( cookie )
                .hasPath( "/" )
                .isHttpOnly() )
            .containsCookie( REFRESH_TOKEN_KEY, cookie -> assertCookie( cookie )
                .hasPath( "/" )
                .isHttpOnly() );
    }

    public static void assertLogin( String login, String password, String tfaCode, int port ) {
        assertPost( httpUrl( port, "/auth/login" ),
            String.format( "{  \"email\": \"%s\",  \"password\": \"%s\", \"tfaCode\": \"%s\"}", login, password, tfaCode ) )
            .hasCode( Http.StatusCode.OK )
            .containsCookie( AUTHENTICATION_KEY, cookie -> assertCookie( cookie )
                .hasPath( "/" )
                .isHttpOnly() );
    }

    public static void assertSwitchOrganization( String orgId, int port ) {
        assertGet( httpUrl( port, "auth/switch/" + orgId ) ).
            hasCode( Http.StatusCode.OK )
            .containsCookie( AUTHENTICATION_KEY, cookie -> assertCookie( cookie )
                .hasPath( "/" )
                .isHttpOnly() )
            .containsCookie( REFRESH_TOKEN_KEY, cookie -> assertCookie( cookie )
                .hasPath( "/" )
                .isHttpOnly() );
    }

    public static void assertSwitchOrganization( String orgId ) {
        assertGet( httpUrl( "auth/switch/" + orgId ) ).
            hasCode( Http.StatusCode.OK )
            .containsCookie( AUTHENTICATION_KEY, cookie -> assertCookie( cookie )
                .hasPath( "/" )
                .isHttpOnly() )
            .containsCookie( REFRESH_TOKEN_KEY, cookie -> assertCookie( cookie )
                .hasPath( "/" )
                .isHttpOnly() );
    }

    public static void assertTfaRequiredLogin( String login, String password, int port ) {
        assertPost( httpUrl( port, "/auth/login" ), "{  \"email\": \"" + login + "\",  \"password\": \"" + password + "\"}" )
            .hasCode( Http.StatusCode.BAD_REQUEST )
            .hasReason( "TFA code is required" );
    }

    public static void assertWrongTfaLogin( String login, String password, String tfaCode, int port ) {
        assertPost( httpUrl( port, "/auth/login" ),
            String.format( "{  \"email\": \"%s\",  \"password\": \"%s\", \"tfaCode\": \"%s\"}", login, password, tfaCode ) )
            .hasCode( Http.StatusCode.BAD_REQUEST )
            .hasReason( "TFA code is incorrect" );
    }

    public static void assertLogout() {
        assertLogout( getTestHttpPort().orElse( 80 ) );
    }

    public static void assertLogout( int port ) {
        assertGet( httpUrl( port, "/auth/logout" ) )
            .hasCode( Http.StatusCode.NO_CONTENT )
            .containsCookie( AUTHENTICATION_KEY, cookie -> assertCookie( cookie )
                .hasValue( "<logged out>" )
                .expiresAt( new DateTime( 1970, 1, 1, 1, 1, UTC ) ) );
    }

    public static void assertLoginWithFBToken() {
        assertPost( httpUrl( getTestHttpPort().orElse( 80 ), "/auth/oauth/login" ), contentOfTestResource( SecureWSFixture.class, "token-credentials.json", ofString() ), Http.ContentType.APPLICATION_JSON )
            .containsCookie( AUTHENTICATION_KEY, cookie -> assertCookie( cookie )
                .hasPath( "/" )
                .isHttpOnly() )
            .containsCookie( REFRESH_TOKEN_KEY, cookie -> assertCookie( cookie )
                .hasPath( "/" )
                .isHttpOnly() );
    }

    public static void assertLoginWithFBTokenWithTfa() {
        assertPost( httpUrl( getTestHttpPort().orElse( 80 ), "/auth/oauth/login" ), contentOfTestResource( SecureWSFixture.class, "token-tfa-credentials.json", ofString() ), Http.ContentType.APPLICATION_JSON )
            .containsCookie( AUTHENTICATION_KEY, cookie -> assertCookie( cookie )
                .hasPath( "/" )
                .isHttpOnly() )
            .containsCookie( REFRESH_TOKEN_KEY, cookie -> assertCookie( cookie )
                .hasPath( "/" )
                .isHttpOnly() );
    }

    public static void assertLoginWithFBTokenWithTfaRequired() {
        assertPost( httpUrl( getTestHttpPort().orElse( 80 ), "/auth/oauth/login" ), contentOfTestResource( SecureWSFixture.class, "token-credentials.json", ofString() ), Http.ContentType.APPLICATION_JSON )
            .hasCode( Http.StatusCode.BAD_REQUEST )
            .hasReason( "TFA code is required" );
    }

    public static void assertLoginWithFBTokenWithWrongTfa() {
        assertPost( httpUrl( getTestHttpPort().orElse( 80 ), "/auth/oauth/login" ), contentOfTestResource( SecureWSFixture.class, "token-wrong-tfa-credentials.json", ofString() ), Http.ContentType.APPLICATION_JSON )
            .hasCode( Http.StatusCode.BAD_REQUEST )
            .hasReason( "TFA code is incorrect" );
    }
}
