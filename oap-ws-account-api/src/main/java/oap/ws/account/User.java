
/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.id.Id;
import oap.json.ext.Ext;
import oap.util.Hash;
import oap.util.Strings;
import oap.ws.sso.UserProvider;
import org.apache.commons.lang3.RandomStringUtils;

import javax.annotation.Nonnull;
import java.io.Serial;
import java.io.Serializable;

@ToString( exclude = { "password", "create" } )
@EqualsAndHashCode
public class User implements Serializable {
    @Serial
    private static final long serialVersionUID = -3371939128187130008L;

    public static final String SCHEMA = "/io/xenoss/account/user.schema.conf";
    public static final String SCHEMA_REGISTRATION = "/io/xenoss/account/user-registration.schema.conf";

    @Id
    public String email;
    public String firstName;
    public String lastName;
    public Ext ext;
    public String password;
    public boolean confirmed = false;
    public String apiKey = RandomStringUtils.random( 30, true, true );
    @JsonProperty( access = JsonProperty.Access.WRITE_ONLY )
    public boolean create;

    @JsonCreator
    public User( String email ) {
        this.email = email;
    }

    public User( String email, String firstName, String lastName, String password, boolean confirmed ) {
        this( email );
        this.firstName = firstName;
        this.lastName = lastName;
        this.confirm( confirmed );
        this.encryptPassword( password );
    }

    public User( String email, String firstName, String lastName ) {
        this( email, firstName, lastName, null, false );
    }

    public static String encrypt( String password ) {
        return Hash.md5( password );
    }

    public String getEmail() {
        return email;
    }

    public User update( String firstName, String lastName, Ext ext ) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.ext = ext;
        return this;
    }

    public User encryptPassword( String password ) {
        this.password = password != null ? encrypt( password ) : null;
        return this;
    }

    public boolean passwordMatches( @Nonnull String password ) {
        return this.password != null && this.password.equals( encrypt( password ) );
    }

    public User confirm( boolean status ) {
        this.confirmed = status;
        return this;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getAccessKey() {
        return UserProvider.toAccessKey( email );
    }

    public String refreshApiKey() {
        this.apiKey = org.apache.commons.lang.RandomStringUtils.random( 30, true, true );
        return this.apiKey;
    }

    public boolean hasPassword() {
        return password != null;
    }

    @SuppressWarnings( "unchecked" )
    public <E extends Ext> E ext() {
        return ( E ) ext;
    }


    public static String organizationFromEmail( String email ) {
        return Strings.substringAfter( "@", email );
    }

    public String organizationName() {
        return organizationFromEmail( email );
    }

}
