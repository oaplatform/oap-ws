package oap.ws.idea;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.patterns.PatternCondition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import static com.intellij.patterns.PlatformPatterns.psiElement;

public class ValidatorReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders( @NotNull PsiReferenceRegistrar registrar ) {
        registrar.registerReferenceProvider(
            psiElement( PsiLiteralExpression.class )
                .withLanguage( JavaLanguage.INSTANCE )
                .with( new PatternCondition<>( "Validator Annotation" ) {
                    @Override
                    public boolean accepts( @NotNull PsiLiteralExpression psiLiteralExpression, ProcessingContext processingContext ) {
                        return Types.isValidatorReference( psiLiteralExpression );
                    }
                } ),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference[] getReferencesByElement( @NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext ) {
                    return new PsiReference[] { new ValidatorReference( psiElement ) };
                }
            } );
    }
}
