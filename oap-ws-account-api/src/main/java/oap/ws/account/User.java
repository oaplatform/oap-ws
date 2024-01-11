
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
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.lang3.RandomStringUtils;

import javax.annotation.Nonnull;
import java.io.Serial;
import java.io.Serializable;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.random.RandomGenerator;

@ToString( exclude = { "password", "create" } )
@EqualsAndHashCode
public class User implements Serializable {
    @Serial
    private static final long serialVersionUID = -3371939128187130008L;

    public static final String SCHEMA = "/oap/ws/account/user.schema.conf";
    public static final String SCHEMA_REGISTRATION = "/oap/ws/account/user-registration.schema.conf";

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
    public boolean tfaEnabled;
    public String defaultOrganization;
    public Map<String, String> defaultAccounts = new HashMap<>();
    public static RandomGenerator random = new SecureRandom();
    public String secretKey = generateSecretKey();

    @JsonCreator
    public User( String email ) {
        this.email = email;
    }

    public User( String email, String firstName, String lastName, String password, boolean confirmed ) {
        this( email );
        this.firstName = firstName;
        this.lastName = lastName;
        this.confirmed = confirmed;
        this.encryptPassword( password );
    }

    public User( String email, String firstName, String lastName, String password, boolean confirmed, boolean tfaEnabled ) {
        this( email );
        this.firstName = firstName;
        this.lastName = lastName;
        this.confirmed = confirmed;
        this.encryptPassword( password );
        this.tfaEnabled = tfaEnabled;
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

    public User update( String firstName, String lastName, boolean tfaEnabled, Ext ext ) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.tfaEnabled = tfaEnabled;
        this.ext = ext;
        return this;
    }

    public void encryptPassword( String password ) {
        this.password = password != null ? encrypt( password ) : null;
    }

    public boolean passwordMatches( @Nonnull String password ) {
        return this.password != null && this.password.equals( encrypt( password ) );
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getAccessKey() {
        return UserProvider.toAccessKey( email );
    }

    public void refreshApiKey() {
        this.apiKey = org.apache.commons.lang3.RandomStringUtils.random( 30, true, true );
    }

    public String getSecretKey() {
        return this.secretKey;
    }

    public boolean hasPassword() {
        return password != null;
    }

    @SuppressWarnings( "unchecked" )
    public <E extends Ext> E ext() {
        return ( E ) ext;
    }


    public static String organizationFromEmail( String email ) {
        return Strings.substringAfter( email, "@" );
    }

    public String organizationName() {
        return organizationFromEmail( email );
    }

    private static String generateSecretKey() {
        byte[] bytes = new byte[20];
        random.nextBytes( bytes );
        Base32 base32 = new Base32();
        return base32.encodeToString( bytes );
    }
}
