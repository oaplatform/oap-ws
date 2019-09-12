package oap.ws.idea;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiNameValuePair;
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
        PsiElement current = psiElement;
        while( ( current = current.getParent() ) != null )
            if( current instanceof PsiJavaFile ) return false;
            else if( isValidatorAnnotation( current ) ) return true;
        return false;
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
