/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account;


import oap.id.Identifier;
import oap.json.Binder;
import org.testng.annotations.Test;

import java.util.Map;

import static oap.json.testng.JsonAsserts.assertJson;
import static oap.testng.Asserts.contentOfTestResource;
import static org.assertj.core.api.Assertions.assertThat;


public class UserTest {

    @Test
    public void marshal() {
        UserData user = new UserData( new User( "email", "John", "Smith" ), Map.of( "r1", "ADMIN", "r2", "USER" ) );
        user.addAccount( "org1", "acc1" );
        user.addAccount( "org1", "acc2" );
        user.addAccount( "org1", "acc2" );
        user.user.defaultOrganization = "r1";
        String json = Binder.json.marshal( user );
        assertJson( json ).isStructurallyEqualTo( contentOfTestResource( getClass(), "user.json", Map.of(
            "API_KEY", user.user.apiKey, "SECRET_KEY", user.user.secretKey
        ) ) );
        assertThat( Binder.json.unmarshal( UserData.class, json ) ).isEqualTo( user );
    }

    @Test
    public void marshalSecureView() {
        UserData user = new UserData( new User( "email", "John", "Smith" ), Map.of( "r1", "ADMIN", "r2", "USER" ) );
        user.addAccount( "org1", "acc1" );
        user.addAccount( "org1", "acc2" );
        user.addAccount( "org1", "acc2" );
        user.user.defaultOrganization = "r1";
        String json = Binder.json.marshal( user.secureView );
        assertJson( json ).isStructurallyEqualTo( contentOfTestResource( getClass(), "secure-view.json", Map.of(
            "API_KEY", user.user.apiKey, "SECRET_KEY", user.user.secretKey
        ) ) );
    }

    @Test
    public void id() {
        assertThat( Identifier.forAnnotationFixed().get( new User( "mail" ) ) )
            .isEqualTo( "mail" );
    }

}
