
/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account;

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
public class UserData implements oap.ws.sso.User, Serializable {
    @Serial
    private static final long serialVersionUID = -3371939128187130008L;

    @JsonIgnore
    public final View view = new View();
    @JsonIgnore
    public final SecureView secureView = new SecureView();

    public Map<String, String> roles = new HashMap<>();
    public Map<String, List<String>> accounts = new HashMap<>();
    public User user;

    @JsonFormat( shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd" )
    public DateTime lastLogin;
    public boolean banned = false;

    private static final String ALL_ACCOUNTS = "*";

    public UserData( User user, Map<String, String> roles ) {
        this.user = user;
        this.roles = roles;
    }

    public UserData( User user ) {
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
    public Optional<String> getDefaultOrganization() {
        return Optional.ofNullable( user.defaultOrganization );
    }

    @JsonIgnore
    @Override
    public Map<String, String> getDefaultAccounts() {
        return user.defaultAccounts;
    }

    @JsonIgnore
    public Optional<String> getDefaultAccount( String organizationId ) {
        return Optional.ofNullable( user.defaultAccounts.get( organizationId ) );
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
        user.defaultAccounts.computeIfAbsent( organizationId, k -> accountId );

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

    public boolean canAccessOrganization( String organizationId ) {
        return roles.containsKey( organizationId ) || roles.containsKey( SYSTEM );
    }

    public boolean belongsToOrganization( String organizationId ) {
        return roles.containsKey( organizationId );
    }

    public UserData encryptPassword( String password ) {
        this.user.encryptPassword( password );
        return this;
    }

    public UserData confirm( boolean status ) {
        this.user.confirmed = status;
        return this;
    }

    public UserData addOrganization( String organizationId, String role ) {
        this.roles.put( organizationId, role );
        if( getDefaultOrganization().isPresent() ) {
            this.user.defaultOrganization = organizationId;
        }
        return this;
    }

    public class View implements oap.ws.sso.User.View {
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

        public boolean isTfaEnabled() {
            return user.tfaEnabled;
        }

        public Map<String, String> getDefaultAccounts() {
            return user.defaultAccounts;
        }

        public String getDefaultOrganization() {
            return user.defaultOrganization;
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

        public String getSecretKey() {
            return user.getSecretKey();
        }
    }
}
