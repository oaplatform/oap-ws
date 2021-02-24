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

package oap.ws.sso.testng;

import oap.application.testng.KernelFixture;
import oap.testng.Fixtures;

import java.nio.file.Path;

public class SecureWSTest extends Fixtures {
    protected final KernelFixture kernelFixture;

    public SecureWSTest( Path conf ) {
        this( new KernelFixture( conf ) );
    }

    public SecureWSTest( KernelFixture kernelFixture ) {
        this.kernelFixture = fixture( kernelFixture );
    }

    public SecureWSTest( String conf ) {
        this( new KernelFixture( conf ) );
    }

    public SecureWSTest( String conf, String confd ) {
        this( new KernelFixture( conf, confd ) );
    }

    protected static void assertLogin( String login, String password ) {
        SecureWSFixture.assertLogin( login, password );
    }

    protected static void assertLogout() {
        SecureWSFixture.assertLogout();
    }
}
