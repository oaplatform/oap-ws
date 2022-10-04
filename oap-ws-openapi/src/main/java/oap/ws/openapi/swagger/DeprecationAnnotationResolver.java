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

import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.media.Schema;
import lombok.extern.slf4j.Slf4j;
import oap.json.ext.Ext;
import oap.json.ext.ExtDeserializer;
import oap.util.Pair;
import oap.util.Strings;

import javax.xml.bind.annotation.XmlAccessorType;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class DeprecationAnnotationResolver extends ModelResolver implements ModelConverter {
    private ModelConverterContext context;
    private Map<Pair<String, String>, Schema> extensionsSchemas = new HashMap<>();

    public DeprecationAnnotationResolver( ModelResolver modelResolver ) {
        super( modelResolver.objectMapper() );
    }

    @Override
    public Schema resolve( AnnotatedType annotatedType,
                           ModelConverterContext context,
                           Iterator<ModelConverter> next ) {
        this.context = context;
        return super.resolve( annotatedType, context, next );
    }

    @Override
    protected void applyBeanValidatorAnnotations( Schema property,
                                                  Annotation[] annotations,
                                                  Schema parent ) {
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

    @Override
    protected boolean ignore( Annotated member,
                              XmlAccessorType xmlAccessorTypeAnnotation,
                              String propName,
                              Set<String> propertiesToIgnore,
                              BeanPropertyDefinition propDef ) {
        if ( propDef.getPrimaryMember() != null && Ext.class.isAssignableFrom( member.getRawType() ) ) {
            Class<?> ext = ExtDeserializer.extensionOf( propDef.getPrimaryMember().getDeclaringClass(), propDef.getName() );
            AnnotatedType annotatedType = new AnnotatedType( ext );
            Schema extensionSchema = super.resolve( annotatedType, context, context.getConverters() );
            String className = propDef.getPrimaryMember().getDeclaringClass().getSimpleName();
            String fieldName = propDef.getName();
            extensionsSchemas.put(  Pair.__( className, fieldName ), extensionSchema );
            log.debug( "Field '{}' in class '{}' has dynamic extension with class {}", fieldName, className, ext.getCanonicalName() );
        }
        return super.ignore( member, xmlAccessorTypeAnnotation, propName, propertiesToIgnore, propDef );
    }

    public Schema getSchema( String className, String fieldName ) {
        return extensionsSchemas.get( Pair.__( className, fieldName ) );
    }
}
