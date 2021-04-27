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

package oap.ws.sso;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.application.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class SecurityRoles {
    public static final Configuration<Config> CONFIGURAION = new Configuration<>( Config.class, "oap-ws-roles" );
    private final SetMultimap<String, String> roles = MultimapBuilder.SetMultimapBuilder.hashKeys().linkedHashSetValues().build();

    public SecurityRoles( Config config ) {
        this( List.of( config ) );
    }

    public SecurityRoles( List<Config> configs ) {
        log.info( "configs = {}", configs );

        for( var configWithUrl : configs ) {
            configWithUrl.roles.forEach( roles::putAll );
        }
    }

    public SecurityRoles() {
        this( CONFIGURAION.fromClassPath() );
    }

    public Set<String> permissionsOf( String role ) {
        return roles.get( role );
    }

    public boolean granted( String role, String... permissions ) {
        Set<String> granted = permissionsOf( role );
        for( String permission : permissions ) if( granted.contains( permission ) ) return true;
        return false;
    }

    public Set<String> registeredRoles() {
        return roles.keySet();
    }

    @ToString
    public static class Config {
        public final Map<String, Set<String>> roles;

        @JsonCreator
        public Config( Map<String, Set<String>> roles ) {
            this.roles = roles;
        }
    }
}
