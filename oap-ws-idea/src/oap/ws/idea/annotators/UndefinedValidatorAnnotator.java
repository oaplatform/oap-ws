package oap.ws.idea.annotators;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import oap.ws.idea.Types;
import oap.ws.idea.ValidatorReference;
import org.jetbrains.annotations.NotNull;

public class UndefinedValidatorAnnotator implements Annotator {

    @Override
    public void annotate( @NotNull PsiElement psiElement, @NotNull AnnotationHolder holder ) {
        if( Types.isValidatorReference( psiElement ) ) {
            ValidatorReference reference = ValidatorReference.find( psiElement.getReferences() );
            if( reference == null ) return;
            if( reference.resolve() == null )
                holder.createErrorAnnotation( psiElement, "Undefined validator reference" );
        }

    }
}
