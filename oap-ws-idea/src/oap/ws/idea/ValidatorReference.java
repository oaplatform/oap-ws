package oap.ws.idea;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ValidatorReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {

    public ValidatorReference( @NotNull PsiElement element ) {
        super( element );
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve( boolean incompleteCode ) {
        List<PsiMethod> methods = collectMatchingMethods();
        ResolveResult[] results = new ResolveResult[methods.size()];
        for( int i = 0; i < methods.size(); i++ ) results[i] = new PsiElementResolveResult( methods.get( i ) );
        return results;
    }

    private List<PsiMethod> collectMatchingMethods() {
        String name = Psi.stripQuotes( myElement.getText() );
        PsiClass psiClass = PsiTreeUtil.getParentOfType( myElement, PsiClass.class );
        List<PsiMethod> methods = new ArrayList<>();
        if( psiClass != null )
            for( PsiMethod method : psiClass.getAllMethods() )
                if( method.isValid() && Objects.equals( name, method.getName() ) ) methods.add( method );
        return methods;
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        ResolveResult[] resolveResults = multiResolve( false );
        return resolveResults.length == 1 ? resolveResults[0].getElement() : null;
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        List<PsiMethod> methods = collectValidators();
        LookupElement[] lookups = new LookupElement[methods.size()];
        for( int i = 0; i < methods.size(); i++ ) {
            PsiMethod method = methods.get( i );
            PsiClass containingClass = method.getContainingClass();
            lookups[i] = LookupElementBuilder.create( method )
                .withBoldness( method.getContainingFile() == myElement.getContainingFile() )
                .withIcon( AllIcons.Nodes.Method )
                .withTypeText( containingClass != null ? containingClass.getQualifiedName() : null, true );
        }
        return lookups;
    }

    private List<PsiMethod> collectValidators() {
        PsiClass psiClass = PsiTreeUtil.getParentOfType( myElement, PsiClass.class );
        PsiMethod annotatedMethod = PsiTreeUtil.getParentOfType( myElement, PsiMethod.class );
        List<PsiMethod> methods = new ArrayList<>();
        if( psiClass != null && annotatedMethod != null )
            for( PsiMethod method : psiClass.getAllMethods() )
                if( Types.isValidator( method, annotatedMethod ) ) methods.add( method );
        return methods;
    }

    public static ValidatorReference find( PsiReference[] psiReferences ) {
        for( PsiReference psiReference : psiReferences )
            if( psiReference instanceof ValidatorReference ) return ( ValidatorReference ) psiReference;
        return null;
    }

}
