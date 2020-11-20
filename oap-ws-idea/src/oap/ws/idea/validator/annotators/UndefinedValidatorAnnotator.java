package oap.ws.idea.validator.annotators;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import oap.ws.idea.Psi;
import oap.ws.idea.validator.Types;
import oap.ws.idea.validator.ValidatorReference;
import org.jetbrains.annotations.NotNull;

import static com.intellij.lang.annotation.HighlightSeverity.ERROR;

public class UndefinedValidatorAnnotator implements Annotator {

    @Override
    public void annotate( @NotNull PsiElement psiElement, @NotNull AnnotationHolder holder ) {
        if( Types.isValidatorReference( psiElement ) ) {
            ValidatorReference reference = Psi.findReference( psiElement, ValidatorReference.class );
            if( reference != null && reference.resolve() == null )
                holder.newAnnotation( ERROR, "Undefined validator" )
                    .range( psiElement )
                    .create();
        }

    }
}
