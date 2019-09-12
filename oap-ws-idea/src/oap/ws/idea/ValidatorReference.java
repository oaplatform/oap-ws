package oap.ws.idea;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.ResolveResult;
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
        PsiClass psiClass = Psi.psiClassOf( myElement );
        if( psiClass == null ) return List.of();
        List<PsiMethod> methods = new ArrayList<>();
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
        List<PsiMethod> validators = collectValidators();
        LookupElement[] lookups = new LookupElement[validators.size()];
        for( int i = 0; i < validators.size(); i++ ) lookups[i] = LookupElementBuilder.create( validators.get( i ) );
        return lookups;
    }

    private List<PsiMethod> collectValidators() {
        PsiClass psiClass = Psi.psiClassOf( myElement );
        if( psiClass == null ) return List.of();
        List<PsiMethod> methods = new ArrayList<>();
        for( PsiMethod method : psiClass.getAllMethods() )
            if( Types.isValidator( method ) ) methods.add( method );
        return methods;
    }

    public static ValidatorReference find( PsiReference[] psiReferences ) {
        for( PsiReference psiReference : psiReferences )
            if( psiReference instanceof ValidatorReference ) return ( ValidatorReference ) psiReference;
        return null;
    }

}
