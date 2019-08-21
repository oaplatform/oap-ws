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

package oap.ws.resource;

import lombok.SneakyThrows;
import oap.http.HttpResponse;
import oap.ws.resource.file.FileResourceService;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.assertj.core.api.Assertions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Base64.getEncoder;
import static oap.ws.validate.testng.ValidationErrorsAssertion.validating;
import static org.assertj.core.api.Assertions.assertThat;

public class ResourceWSTest {
    public static final String ROOT_PATH = "/data/cdn";
    private File rootFolder = new File( ROOT_PATH );
    private ResourceService resourceService = new FileResourceService( ROOT_PATH );
    private ResourceWS ws = new ResourceWS( resourceService );

    @BeforeMethod
    public void setup() {
        if( rootFolder.exists() ) {
            rootFolder.delete();
        }
        rootFolder.mkdirs();
    }

    @AfterMethod
    public void cleanup() {
        rootFolder.delete();
    }

    @Test
    public void create() throws IOException {
        final var request = new ResourceRequest();
        final String contentType = "text/plain";
        final String base64 = new String( getEncoder().encode( "test".getBytes() ) );
        request.base64Data = "data:" + contentType + ";base64," + base64;
        Assertions.assertThat( validating( ws )
            .isNotFailed()
            .instance
            .receive( request ) )
            .satisfies( response -> {
                assertThat( response.code ).isEqualTo( HttpStatus.SC_CREATED );
                assertThat( getContentType( response ).getName() ).isEqualTo( "Content-Type" );
                assertThat( getContentType( response ).getValue() ).startsWith( contentType );
                assertCreateContent( contentType, response );
            } );
    }

    @Test
    public void read() throws IOException {
        final String request = "test.plain";
        final Path path = Paths.get( ROOT_PATH ).resolve( Paths.get( request ) );
        final String content = "test";
        Files.write( path, content.getBytes() );
        Assertions.assertThat( validating( ws )
            .isNotFailed()
            .instance
            .send( request ) )
            .satisfies( response -> {
                assertThat( response.code ).isEqualTo( HttpStatus.SC_OK );
                assertThat( getContentType( response ).getName() ).isEqualTo( "Content-Type" );
                assertThat( getContentType( response ).getValue() ).startsWith( "text/plain" );
                assertThat( getContent( response ) ).isEqualTo( content );
            } );
    }

    private Header getContentType( HttpResponse response ) {
        return response.contentEntity.getContentType();
    }

    private void assertCreateContent( String contentType, HttpResponse response ) {
        assertThat( getContent( response ) )
            .hasSizeGreaterThanOrEqualTo( 29 )
            .endsWith( "." + contentType.split( "/" )[1] );
    }

    @SneakyThrows
    private String getContent( HttpResponse response ) {
        return IOUtils.toString( response.contentEntity.getContent(), UTF_8 );
    }
}
