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

package oap.ws.openapi;

import oap.reflect.Reflection;
import oap.ws.sso.WsSecurity;

import java.util.Optional;

public class WsSecurityDescriptor {
    public static final WsSecurityDescriptor NO_SECURITY_SET = new WsSecurityDescriptor( null, null );

    public final Reflection.Method method;
    public final String realm;
    public final String[] permissions;

    public static WsSecurityDescriptor ofMethod( Reflection.Method method ) {
        Optional<WsSecurity> annotation = method.findAnnotation( WsSecurity.class );
        if( annotation.isEmpty() ) return NO_SECURITY_SET;
        return new WsSecurityDescriptor( method, annotation.get() );
    }

    private WsSecurityDescriptor( Reflection.Method method, WsSecurity annotation ) {
        this.method = method;
        if ( annotation == null ) {
            realm = null;
            permissions = null;
            return;
        }
        this.realm = annotation.realm();
        this.permissions = annotation.permissions();
    }
}
