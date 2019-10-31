/*
  The MIT License (MIT)
  <p>
  Copyright (c) Open Application Platform Authors
  <p>
  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:
  <p>
  The above copyright notice and this permission notice shall be included in all
  copies or substantial portions of the Software.
  <p>
  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  SOFTWARE.
 */

package oap.ws.file;

import oap.application.testng.KernelFixture;
import oap.io.Files;
import oap.testng.Env;
import oap.testng.Fixtures;
import oap.testng.TestDirectory;
import org.testng.annotations.Test;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.nio.charset.StandardCharsets.UTF_8;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.assertPost;
import static oap.http.testng.HttpAsserts.httpUrl;
import static oap.testng.Asserts.contentOfTestResource;
import static oap.testng.Asserts.pathOfTestResource;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;
import static org.assertj.core.api.Assertions.assertThat;

public class FileWSTest extends Fixtures {
    {
        fixture( TestDirectory.FIXTURE );
        fixture( new KernelFixture( pathOfTestResource( getClass(), "application.test.conf" ) ) );
    }

    @Test
    public void upload() {
        assertPost( httpUrl( "/file" ), contentOfTestResource( getClass(), "data-complex.json" ), APPLICATION_JSON )
            .responded( HTTP_OK, "OK", TEXT_PLAIN.withCharset( UTF_8 ), "file.txt" );
        assertThat( Env.tmpPath( "default/file.txt" ) ).hasContent( "test" );

        assertPost( httpUrl( "/file?bucket=b1" ), contentOfTestResource( getClass(), "data-single.json" ), APPLICATION_JSON )
            .responded( HTTP_OK, "OK", TEXT_PLAIN.withCharset( UTF_8 ), "file.txt" );
        assertThat( Env.tmpPath( "b1/file.txt" ) ).hasContent( "test" );
    }

    @Test
    public void download() {
        Files.writeString( Env.tmpPath( "default/test.txt" ), "test" );
        assertGet( httpUrl( "/file?path=test.txt" ) )
            .responded( HTTP_OK, "OK", TEXT_PLAIN, "test" );

        Files.writeString( Env.tmpPath( "b1/test.txt" ), "b1test" );
        assertGet( httpUrl( "/file?path=test.txt&bucket=b1" ) )
            .responded( HTTP_OK, "OK", TEXT_PLAIN, "b1test" );
    }

}
