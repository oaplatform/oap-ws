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

package oap.ws.openapi.swagger;

import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.media.Schema;
import lombok.extern.slf4j.Slf4j;
import oap.util.Strings;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
public class DeprecationAnnotationResolver extends ModelResolver implements ModelConverter {
    public DeprecationAnnotationResolver( ModelResolver modelResolver ) {
        super( modelResolver.objectMapper() );
    }

    @Override
    protected void applyBeanValidatorAnnotations( Schema property, Annotation[] annotations, Schema parent ) {
        super.applyBeanValidatorAnnotations( property, annotations, parent );
        if ( annotations == null || annotations.length == 0 ) return;
        Optional<Annotation> deprecated = Arrays.stream( annotations ).filter( anno -> anno.annotationType().equals( Deprecated.class ) ).findAny();
        deprecated.ifPresent( annotation -> {
            Deprecated anno = ( Deprecated ) annotation;
            property.setDeprecated( true );
            String since = !Strings.isEmpty( anno.since() ) ? " since: " + anno.since() : "";
            if( property.getName() != null )
                log.debug( "Field '{}' marked as deprecated{}", property.getName(), since );
        } );
    }
}
