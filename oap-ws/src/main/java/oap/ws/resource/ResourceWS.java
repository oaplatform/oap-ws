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

import oap.http.HttpResponse;
import oap.util.Cuid;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import oap.ws.validate.WsValidateJson;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;

import java.io.IOException;

import static java.net.HttpURLConnection.HTTP_OK;
import static oap.http.HttpResponse.status;
import static oap.http.Request.HttpMethod.GET;
import static oap.http.Request.HttpMethod.POST;
import static oap.ws.WsParam.From.BODY;
import static oap.ws.WsParam.From.QUERY;
import static oap.ws.resource.Schema.RESOURCE_REQUEST;

public class ResourceWS {
    private final ResourceService resourceService;

    public ResourceWS( ResourceService resourceService ) {
        this.resourceService = resourceService;
    }

    @WsMethod( method = POST, path = "/" )
    public HttpResponse receive( @WsParam( from = BODY ) @WsValidateJson( schema = RESOURCE_REQUEST )
                                     ResourceRequest request ) throws IOException {
        String[] data = request.base64Data.split( ";" );
        final String fileExtension = data[0].split( ":" )[1].split( "/" )[1];
        String newFileName = Cuid.UNIQUE.next() + "." + fileExtension;
        final String body = data[1].split( "," )[1];
        resourceService.create( newFileName, body );

        return status( HttpStatus.SC_CREATED )
            .withContent( newFileName, ContentType.TEXT_PLAIN ).response();
    }

    @WsMethod( method = GET, path = "/" )
    public HttpResponse send( @WsParam( from = QUERY ) String path ) throws IOException {
        Resource resource = resourceService.read( path );
        ContentType contentType = getContentType( path );
        final HttpResponse.Builder builder = status( HTTP_OK );
        builder.contentEntity = new ByteArrayEntity( resource.getContent(), contentType );

        return builder.withHeader( "Content-Type", contentType.getMimeType() ).response();
    }

    private ContentType getContentType( String path ) {
        ContentType contentType = ContentType.TEXT_PLAIN;
        if( path.endsWith( "jpg" ) ) {
            contentType = ContentType.IMAGE_JPEG;
        } else if( path.endsWith( "png" ) ) {
            contentType = ContentType.IMAGE_PNG;
        } else if( path.endsWith( "bmp" ) ) {
            contentType = ContentType.IMAGE_BMP;
        } else if( path.endsWith( "gif" ) ) {
            contentType = ContentType.IMAGE_GIF;
        } else if( path.endsWith( "tiff" ) ) {
            contentType = ContentType.IMAGE_TIFF;
        } else if( path.endsWith( "svg" ) ) {
            contentType = ContentType.IMAGE_SVG;
        } else if( path.endsWith( "webp" ) ) {
            contentType = ContentType.IMAGE_WEBP;
        }


        return contentType;
    }
}
