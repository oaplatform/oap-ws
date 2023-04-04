
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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.json.ext.Ext;
import oap.util.Hash;
import oap.ws.sso.UserProvider;
import org.joda.time.DateTime;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static oap.ws.sso.WsSecurity.SYSTEM;

@ToString( exclude = { "view", "secureView" } )
@EqualsAndHashCode( exclude = { "view", "secureView" } )
public class UserData implements User, Serializable {
    @Serial
    private static final long serialVersionUID = -3371939128187130008L;

    @JsonIgnore
    public final View view = new View();
    @JsonIgnore
    public final SecureView secureView = new SecureView();

    public Map<String, String> roles = new HashMap<>();
    public Map<String, List<String>> accounts = new HashMap<>();
    public UserInfo user;

    @JsonFormat( shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd" )
    public DateTime lastLogin;
    public boolean banned = false;

    private static final String ALL_ACCOUNTS = "*";

    public UserData( UserInfo user, Map<String, String> roles ) {
        this.user = user;
        this.roles = roles;
    }

    public UserData( UserInfo user ) {
        this.user = user;
    }

    public static String encrypt( String password ) {
        return Hash.md5( password );
    }

    @Override
    @JsonIgnore
    public String getEmail() {
        return user.email;
    }

    @Override
    public Optional<String> getRole( String realm ) {
        return Optional.ofNullable( roles.get( SYSTEM ) ).or( () -> Optional.ofNullable( roles.get( realm ) ) );
    }

    @Override
    public Map<String, String> getRoles() {
        return roles;
    }

    @JsonIgnore
    @Override
    public View getView() {
        return view;
    }

    public boolean canAccessAccount( String organizationId, String accountId ) {
        List<String> userAccounts = this.accounts.get( organizationId );
        return userAccounts != null && ( userAccounts.contains( ALL_ACCOUNTS ) || userAccounts.contains( accountId ) );
    }

    public UserData addAccount( String organizationId, String accountId ) {
        if( ALL_ACCOUNTS.equals( accountId ) ) {
            accounts.put( organizationId, new ArrayList<>( List.of( ALL_ACCOUNTS ) ) );
            return this;
        }
        List<String> accounts = this.accounts.get( organizationId );
        if( accounts == null || accounts.contains( ALL_ACCOUNTS ) ) {
            this.accounts.put( organizationId, new ArrayList<>( List.of( accountId ) ) );
            return this;
        }
        if( !accounts.contains( accountId ) ) accounts.add( accountId );
        return this;
    }

    public UserData update( String firstName, String lastName, Ext ext ) {
        user.firstName = firstName;
        user.lastName = lastName;
        user.ext = ext;
        return this;
    }

    public UserData ban( Boolean banStatus ) {
        this.banned = banStatus;
        return this;
    }

    @JsonIgnore
    public String getAccessKey() {
        return UserProvider.toAccessKey( user.email );
    }

    public UserData refreshApikey() {
        user.refreshApiKey();
        return this;
    }

    public boolean authenticate( String password ) {
        return !banned && user.confirmed && user.passwordMatches( password );
    }

    public boolean authenticate( String accessKey, String apiKey ) {
        return !banned && getAccessKey().equals( accessKey ) && this.user.apiKey.equals( apiKey );
    }

    public UserData assignRole( String organizationId, String role ) {
        this.roles.put( organizationId, role );
        return this;
    }

    public boolean belongsToOrganization( String organizationId ) {
        return roles.containsKey( organizationId ) || roles.containsKey( SYSTEM );
    }

    public UserData encryptPassword( String password ) {
        this.user.encryptPassword( password );
        return this;
    }

    public UserData confirm( boolean status ) {
        this.user.confirm( status );
        return this;
    }

    public class View implements User.View {
        public String getEmail() {
            return user.email;
        }

        public String getFirstName() {
            return user.firstName;
        }

        public String getLastName() {
            return user.lastName;
        }

        public Map<String, List<String>> getAccounts() {
            return accounts;
        }

        public Map<String, String> getRoles() {
            return roles;
        }

        public boolean isBanned() {
            return banned;
        }

        public boolean isConfirmed() {
            return user.confirmed;
        }

        @JsonFormat( shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd" )
        public DateTime getLastLogin() {
            return lastLogin;
        }

        public Ext getExt() {
            return user.ext;
        }
    }

    public class SecureView extends View {
        public String getApiKey() {
            return user.apiKey;
        }

        public String getAccessKey() {
            return user.getAccessKey();
        }
    }
}
