/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.sso;


import oap.id.Identifier;
import oap.json.Binder;
import oap.ws.sso.model.UserData;
import oap.ws.sso.model.UserInfo;
import org.testng.annotations.Test;

import java.util.Map;

import static oap.json.testng.JsonAsserts.assertJson;
import static oap.testng.Asserts.contentOfTestResource;
import static org.assertj.core.api.Assertions.assertThat;


public class UserTest {

    @Test
    public void marshal() {
        UserData user = new UserData( new UserInfo( "email", "John", "Smith" ), null );
        user.addAccount( "org1", "acc1" );
        user.addAccount( "org1", "acc2" );
        user.addAccount( "org1", "acc2" );
        String json = Binder.json.marshal( user );
        System.out.println( json );
        assertJson( json ).isStructurallyEqualTo( contentOfTestResource( getClass(), "user.json", Map.of(
            "API_KEY", user.user.apiKey
        ) ) );
        assertThat( Binder.json.unmarshal( UserData.class, json ) ).isEqualTo( user );
    }

    @Test
    public void marshalSecureView() {
        UserData user = new UserData( new UserInfo( "email", "John", "Smith" ), null );
        user.addAccount( "org1", "acc1" );
        user.addAccount( "org1", "acc1" );
        user.addAccount( "org1", "acc2" );
        String json = Binder.json.marshal( user.secureView );
        System.out.println( json );
        assertJson( json ).isStructurallyEqualTo( contentOfTestResource( getClass(), "secure-view.json", Map.of(
            "API_KEY", user.user.apiKey
        ) ) );
    }

    @Test
    public void id() {
        assertThat( Identifier.forAnnotationFixed().get( new UserInfo( "mail" ) ) )
            .isEqualTo( "mail" );
    }

}
