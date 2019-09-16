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

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiTreeUtil;
import oap.ws.idea.Psi;
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
                .withTypeText( containingClass != null ? containingClass.getName() : null, true );
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

}
