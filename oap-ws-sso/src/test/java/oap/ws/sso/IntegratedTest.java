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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.application.testng.KernelFixture;
import oap.testng.Fixtures;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static oap.io.Resources.urlOrThrow;
import static oap.ws.sso.UserProvider.toAccessKey;

public class IntegratedTest extends Fixtures {
    protected final KernelFixture kernelFixture;

    public IntegratedTest() {
        kernelFixture = fixture( new KernelFixture( urlOrThrow( getClass(), "/application.test.conf" ) ) );
    }

    protected TestUserProvider userProvider() {
        return kernelFixture.service( "oap-ws-sso-test", TestUserProvider.class );
    }

    @Slf4j
    public static class TestUserProvider implements UserProvider {
        public final List<TestUser> users = new ArrayList<>();

        public TestUser addUser( String email, String password, String role ) {
            return addUser( new TestUser( email, password, role ) );
        }

        public TestUser addUser( TestUser user ) {
            users.add( user );
            return user;
        }

        @Override
        public Optional<TestUser> getUser( String email ) {
            return users.stream().filter( u -> u.getEmail().equalsIgnoreCase( email ) ).findAny();
        }

        @Override
        public UserAuth getAuthenticated( String email, String password, Optional<String> tfaCode ) {
            log.trace( "authenticating {} with {}", email, password );
            log.trace( "users {}", users );
            return users.stream()
                .filter( u -> u.getEmail().equalsIgnoreCase( email ) && u.password.equals( password ) )
                .map( user -> {
                    if( user.tfaEnabled ) {
                        var tfaCheck = tfaCode.map( "proper_code"::equals ).orElse( false );
                        return new UserAuth( user, tfaCheck, true );
                    }
                    return new UserAuth( user, true, false );
                } )
                .findAny().orElse( new UserAuth( null, false, false ) );
        }

        @Override
        public Optional<TestUser> getAuthenticatedByApiKey( String accessKey, String apiKey ) {
            return users.stream()
                .filter( u -> u.getAccessKey().equals( accessKey ) && u.apiKey.equals( apiKey ) )
                .findAny();
        }
    }

    @ToString
    @EqualsAndHashCode
    public static class TestUser implements User {
        public final String email;
        public final String password;
        public final String role;
        public final boolean tfaEnabled;
        public final String apiKey = RandomStringUtils.random( 10, true, true );
        @JsonIgnore
        public final View view = new View();

        public TestUser( String email, String password, String role ) {
            this( email, password, role, false );
        }

        public TestUser( String email, String password, String role, boolean tfaEnabled ) {
            this.email = email;
            this.password = password;
            this.role = role;
            this.tfaEnabled = tfaEnabled;
        }

        @Override
        public String getEmail() {
            return email;
        }

        @Override
        public String getRole() {
            return role;
        }

        @Override
        public View getView() {
            return view;
        }

        public String getAccessKey() {
            return toAccessKey( email );
        }

        public class View implements User.View {
            @Override
            public String getEmail() {
                return TestUser.this.getEmail();
            }

            @Override
            public String getRole() {
                return TestUser.this.getRole();
            }
        }

    }
}
