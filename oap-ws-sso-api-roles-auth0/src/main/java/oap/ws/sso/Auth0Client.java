/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.sso;

import com.auth0.client.auth.AuthAPI;
import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.auth.TokenHolder;
import com.auth0.net.AuthRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Slf4j
public class Auth0Client {

    private final AuthAPI authClient;
    private final String audience;
    private final String domain;
    private String managementApiAccessToken;
    private Date managementApiAccessTokenExpiresAt;

    public final String serverId;

    public Auth0Client( String domain, String audience, String clientId, String clientSecret, String serverId ) {
        this.authClient = new AuthAPI( domain, clientId, clientSecret );
        this.domain = domain;
        this.audience = audience;
        this.serverId = serverId;
    }

    public ManagementAPI getApi() {
        return new ManagementAPI( domain, managementApiAccessToken );
    }

    public void checkToken() {
        if( managementApiAccessToken == null || managementApiAccessTokenExpiresAt.before( new Date() ) ) {
            AuthRequest authRequest = authClient.requestToken( audience );
            TokenHolder holder = null;
            try {
                holder = authRequest.execute();
                managementApiAccessToken = holder.getAccessToken();
                managementApiAccessTokenExpiresAt = holder.getExpiresAt();
            } catch( Auth0Exception e ) {
                log.error( "Failed to get Management api token", e );
                managementApiAccessToken = null;
            }
        }
    }
}
