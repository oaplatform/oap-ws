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

package oap.ws.idea.validator.annotators;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import oap.ws.idea.Psi;
import oap.ws.idea.validator.Types;
import oap.ws.idea.validator.ValidatorReference;
import org.jetbrains.annotations.NotNull;

public class IllegalValidatorAnnotator implements Annotator {

    @Override
    public void annotate( @NotNull PsiElement psiElement, @NotNull AnnotationHolder holder ) {
        if( Types.isValidatorReference( psiElement ) ) {
            ValidatorReference reference = Psi.findReference( psiElement, ValidatorReference.class );
            if( reference == null ) return;
            PsiMethod validator = ( PsiMethod ) reference.resolve();
            if( validator != null && !Types.isValidator( validator ) )
                holder.createErrorAnnotation( psiElement, "Illegal validator (should be nonstatic, nonpublic, returning ValidationErrors)" );
        }

    }
}
