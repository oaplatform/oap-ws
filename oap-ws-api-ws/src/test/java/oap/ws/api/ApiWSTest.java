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

package oap.ws.api;

import lombok.extern.slf4j.Slf4j;
import oap.application.testng.KernelFixture;
import oap.http.ContentTypes;
import oap.testng.Fixtures;
import org.testng.annotations.Test;

import java.util.Map;

import static java.net.HttpURLConnection.HTTP_OK;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.httpUrl;
import static oap.io.Resources.urlOrThrow;
import static oap.testng.Asserts.contentOfTestResource;

@Slf4j
public class ApiWSTest extends Fixtures {
    {
        fixture( new KernelFixture( urlOrThrow( getClass(), "/application.test.conf" ) ) );
    }

    @Test
    public void api() {
        assertGet( httpUrl( "/system/api" ) )
            .responded( HTTP_OK, "OK", ContentTypes.TEXT_PLAIN,
                contentOfTestResource( getClass(), "api.txt", Map.of() ) );
    }
}

