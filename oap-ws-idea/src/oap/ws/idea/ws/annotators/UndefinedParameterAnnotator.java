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

package oap.ws.idea.ws.annotators;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import oap.ws.idea.Psi;
import oap.ws.idea.ws.PathParameterReference;
import oap.ws.idea.ws.Types;

import javax.annotation.Nonnull;

import static com.intellij.lang.annotation.HighlightSeverity.ERROR;

public class UndefinedParameterAnnotator implements Annotator {

    @Override
    public void annotate( @Nonnull PsiElement psiElement, @Nonnull AnnotationHolder holder ) {
        if( Types.isPathVariableReference( psiElement ) ) {
            var references = Psi.findReferences( psiElement, PathParameterReference.class );
            for( PathParameterReference reference : references )
                if( reference.resolve() == null )
                    holder.newAnnotation( ERROR, "No such parameter defined in the method signature" )
                        .range( Psi.inElementToGlobal( psiElement, reference.getRangeInElement() ) )
                        .create();
        }
    }
}
