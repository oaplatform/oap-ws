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

package oap.ws.interceptor;

import oap.http.HttpResponse;
import oap.http.Request;
import oap.reflect.Reflection;
import oap.ws.Session;
import oap.ws.sso.User;

import java.util.Optional;

import static oap.ws.sso.SSO.SESSION_USER_KEY;

public class UnsecureInterceptor implements Interceptor {
    public static User ANONYMOUS = new User() {
        @Override
        public String getEmail() {
            return "ANONYMOUS";
        }

        @Override
        public String getRole() {
            return "ANONYMOUS";
        }

        @Override
        public View getView() {
            return view;
        }

        private final View view = new View() {
            @Override
            public String getEmail() {
                return ANONYMOUS.getEmail();
            }

            @Override
            public String getRole() {
                return ANONYMOUS.getRole();
            }
        };
    };

    @Override
    public Optional<HttpResponse> before( Request request, Session session, Reflection.Method method ) {
        session.set( SESSION_USER_KEY, ANONYMOUS );
        return Optional.empty();
    }

    @Override
    public HttpResponse after( HttpResponse response, Session session ) {
        session.remove( SESSION_USER_KEY );
        return response;
    }
}
