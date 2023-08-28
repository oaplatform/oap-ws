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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

public class CognitoAWSProvider implements OauthProviderService {

    private static final int PAYLOAD = 1;

    private final CognitoIdentityProviderClient identityProviderClient;

    public CognitoAWSProvider( String region ) {
        this.identityProviderClient = CognitoIdentityProviderClient.builder()
            .region( Region.of( region ) )
            .credentialsProvider( ProfileCredentialsProvider.create() )
            .build();
    }

    @Override
    public Optional<TokenInfo> getTokenInfo( String accessToken ) {
        GetUserRequest getUserRequest = GetUserRequest.builder().accessToken( accessToken ).build();

        final GetUserResponse user = identityProviderClient.getUser( getUserRequest );
        for( AttributeType attribute : user.userAttributes() ) {
            if( "email".equals( attribute.name() ) ) {
                identityProviderClient.close();
                return Optional.of( new TokenInfo( attribute.value(), null, null ) );
            }
        }
        identityProviderClient.close();
        return Optional.empty();
    }

    public Optional<TokenInfo> getTokenInfoFromIdToken( String idToken ) {
        final String payload = idToken.split( "\\." )[PAYLOAD];
        final byte[] payloadBytes = Base64.getUrlDecoder().decode( payload );
        final String payloadString = new String( payloadBytes, StandardCharsets.UTF_8 );
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = ( JsonObject ) jsonParser.parse( payloadString );
        if( jsonObject != null ) {
            String email = getIfPresent( jsonObject, "email" ).orElse( null );
            String firstName = getIfPresent( jsonObject, "given_name" ).orElse( null );
            String lastName = getIfPresent( jsonObject, "family_name" ).orElse( null );
            return Optional.of( new TokenInfo( email, firstName, lastName ) );
        }
        return Optional.empty();
    }

    private Optional<String> getIfPresent( JsonObject object, String field ) {
        final JsonElement obj = object.get( field );
        if( obj != null ) {
            return Optional.ofNullable( obj.getAsString() );
        }
        return Optional.empty();
    }
}
