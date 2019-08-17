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

package oap.ws.resource.file;

import oap.ws.resource.DefaultResource;
import oap.ws.resource.Resource;
import oap.ws.resource.ResourceService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileResourceService implements ResourceService {
    @Override
    public void create( String path, String content ) throws IOException {
        Files.write( Paths.get( path ), content.getBytes() );
    }

    @Override
    public Resource read( String path ) throws IOException {
        final byte[] content = Files.readAllBytes( Paths.get( path ) );

        return new DefaultResource( path, content );
    }
}
