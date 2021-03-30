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

package oap.ws;

import lombok.extern.slf4j.Slf4j;
import oap.application.testng.KernelFixture;
import oap.testng.Fixtures;
import org.testng.annotations.Test;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.httpUrl;
import static oap.io.Resources.urlOrThrow;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

@Slf4j
public class ValidationTest extends Fixtures {

    {
        fixture( new KernelFixture( urlOrThrow( getClass(), "/application.test.conf" ) ) );
    }

    @Test
    public void brokenValidator() {
        assertGet( httpUrl( "/vaildation/service/methodWithBrokenValidator?requiredParameter=10" ) )
            .respondedJson( HTTP_INTERNAL_ERROR, "CausedByException", "{\"message\":\"CausedByException\"}" );
    }

    @Test
    public void wrongValidatorName() {
        String errorMessage = "No such method wrongValidatorName with the following parameters: [int requiredParameter]";
        assertGet( httpUrl( "/vaildation/service/methodWithWrongValidatorName?requiredParameter=10" ) )
            .respondedJson( HTTP_INTERNAL_ERROR, errorMessage, "{\"message\":\"" + errorMessage + "\"}" );
    }

    @Test
    public void validatorWithWrongParameters() {
        String errorMessage = "missedParam required by validator wrongArgsValidator is not supplied by web method";
        assertGet( httpUrl( "/vaildation/service/methodWithWrongValidatorArgs?requiredParameter=10" ) )
            .responded( HTTP_INTERNAL_ERROR, errorMessage, APPLICATION_JSON, "{\"message\":\"" + errorMessage + "\"}" );
    }

    @Test
    public void exception() {
        assertGet( httpUrl( "/vaildation/service/exceptionRuntimeException" ) )
            .hasCode( HTTP_INTERNAL_ERROR );
    }
}

