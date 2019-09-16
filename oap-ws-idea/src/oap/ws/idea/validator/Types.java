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

package oap.ws.idea.validator;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.util.PsiTreeUtil;
import oap.ws.idea.Psi;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Types {
    public static final String ANNOTATION_TYPE = "oap.ws.validate.WsValidate";
    public static final String VALIDATOR_RETURN_TYPE = "oap.ws.validate.ValidationErrors";

    public static boolean isValidator( @NotNull PsiMethod psiMethod, @NotNull PsiMethod targetMethod ) {
        return isValidator( psiMethod ) && Psi.getSignatureMismatch( psiMethod, targetMethod ).isEmpty();
    }

    public static boolean isValidator( @NotNull PsiMethod psiMethod ) {
        PsiModifierList modifierList = psiMethod.getModifierList();
        return psiMethod.isValid()
            && psiMethod.getReturnType() != null
            && Objects.equals( psiMethod.getReturnType().getCanonicalText(), VALIDATOR_RETURN_TYPE )
            && !modifierList.hasExplicitModifier( "public" )
            && !modifierList.hasExplicitModifier( "static" );
    }

    public static boolean isValidatorReference( @NotNull PsiElement psiElement ) {
        if( !( psiElement instanceof PsiLiteralExpression ) ) return false;
        PsiAnnotation annotation = PsiTreeUtil.getParentOfType( psiElement, PsiAnnotation.class );
        return annotation != null && annotation.hasQualifiedName( ANNOTATION_TYPE );
    }

    public static boolean isValidatorAnnotation( @NotNull PsiElement psiElement ) {
        return psiElement instanceof PsiAnnotation
            && ( ( PsiAnnotation ) psiElement ).hasQualifiedName( ANNOTATION_TYPE );
    }

    public static List<PsiAnnotationMemberValue> validatorReferences( @NotNull PsiAnnotation annotation ) {
        List<PsiAnnotationMemberValue> names = new ArrayList<>();
        for( PsiNameValuePair attribute : annotation.getParameterList().getAttributes() ) {
            PsiAnnotationMemberValue[] values;
            if( attribute.getValue() instanceof PsiArrayInitializerMemberValue )
                values = ( ( PsiArrayInitializerMemberValue ) attribute.getValue() ).getInitializers();
            else values = new PsiAnnotationMemberValue[] { attribute.getValue() };
            Collections.addAll( names, values );
        }
        return names;
    }

}
