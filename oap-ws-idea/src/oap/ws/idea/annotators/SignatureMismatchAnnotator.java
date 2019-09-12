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

package oap.ws.idea.annotators;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.util.PsiTreeUtil;
import oap.ws.idea.Psi;
import oap.ws.idea.Types;
import oap.ws.idea.ValidatorReference;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class SignatureMismatchAnnotator implements Annotator {

    @Override
    public void annotate( @NotNull PsiElement psiElement, @NotNull AnnotationHolder holder ) {
        if( Types.isValidatorReference( psiElement ) ) {
            ValidatorReference reference = ValidatorReference.find( psiElement.getReferences() );
            if( reference == null ) return;
            PsiMethod validator = ( PsiMethod ) reference.resolve();
            PsiMethod method = PsiTreeUtil.getParentOfType( psiElement, PsiMethod.class );
            if( validator == null || !Types.isValidator( validator ) || method == null || !method.isValid() ) return;
            List<PsiParameter> mismatch = Psi.getSignatureMismatch( validator, method );

            if( !mismatch.isEmpty() )
                holder.createErrorAnnotation( psiElement, "Method can't supply parameter"
                    + ( mismatch.size() == 1 ? " " : "s " )
                    + mismatch
                    .stream()
                    .map( p -> p.getType().getPresentableText() + " " + p.getName() )
                    .collect( Collectors.joining( ", " ) ) );
        }
    }

}
