package oap.ws.idea.validator.annotators;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiElement;
import oap.ws.idea.validator.Types;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DuplicateValidatorAnnotator implements Annotator {

    @Override
    public void annotate( @NotNull PsiElement psiElement, @NotNull AnnotationHolder holder ) {
        if( Types.isValidatorAnnotation( psiElement ) ) {
            PsiAnnotation psiAnnotation = ( PsiAnnotation ) psiElement;
            List<PsiAnnotationMemberValue> validators = Types.validatorReferences( psiAnnotation );
            Set<String> unique = new HashSet<>();
            for( PsiAnnotationMemberValue validator : validators ) {
                if( unique.contains( validator.getText() ) )
                    holder.createErrorAnnotation( validator, "Duplicate validator" );
                else unique.add( validator.getText() );
            }
        }
    }

}
