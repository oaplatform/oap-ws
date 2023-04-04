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

package oap.ws.sso.model;

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
public class UserInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = -3371939128187130008L;

    public static final String SCHEMA = "/schemas/user.schema.conf";
    public static final String SCHEMA_REGISTRATION = "/schemas/user-registration.schema.conf";

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

    @JsonCreator
    public UserInfo( String email ) {
        this.email = email;
    }

    public UserInfo( String email, String firstName, String lastName, String password, boolean confirmed ) {
        this( email );
        this.firstName = firstName;
        this.lastName = lastName;
        this.confirm( confirmed );
        this.encryptPassword( password );
    }

    public UserInfo( String email, String firstName, String lastName, String password, boolean confirmed, boolean tfaEnabled ) {
        this( email );
        this.firstName = firstName;
        this.lastName = lastName;
        this.confirm( confirmed );
        this.encryptPassword( password );
        this.tfaEnabled = tfaEnabled;
    }


    public UserInfo( String email, String firstName, String lastName ) {
        this( email, firstName, lastName, null, false );
    }

    public static String encrypt( String password ) {
        return Hash.md5( password );
    }

    public String getEmail() {
        return email;
    }

    public UserInfo update( String firstName, String lastName, Ext ext ) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.ext = ext;
        return this;
    }

    public UserInfo encryptPassword( String password ) {
        this.password = password != null ? encrypt( password ) : null;
        return this;
    }

    public boolean passwordMatches( @Nonnull String password ) {
        return this.password != null && this.password.equals( encrypt( password ) );
    }

    public UserInfo confirm( boolean status ) {
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
