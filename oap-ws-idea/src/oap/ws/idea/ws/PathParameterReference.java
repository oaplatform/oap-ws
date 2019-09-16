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

package oap.ws.idea.ws;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiTreeUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PathParameterReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {
    private String variable;

    public PathParameterReference( @Nonnull PsiElement element, String variable, int offset ) {
        super( element, new TextRange( offset, offset + variable.length() ) );
        this.variable = variable;
    }


    @Nonnull
    @Override
    public ResolveResult[] multiResolve( boolean incompleteCode ) {
        var paramater = resolve();
        return paramater == null ? ResolveResult.EMPTY_ARRAY
            : new ResolveResult[] { new PsiElementResolveResult( paramater ) };
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        PsiMethod method = PsiTreeUtil.getParentOfType( myElement, PsiMethod.class );
        if( method == null || !method.isValid() ) return null;
        var parameters = method.getParameterList().getParameters();
        for( PsiParameter parameter : parameters )
            if( Objects.equals( parameter.getName(), variable ) ) return parameter;
        return null;
    }

    @Nonnull
    @Override
    public Object[] getVariants() {
        PsiMethod method = PsiTreeUtil.getParentOfType( myElement, PsiMethod.class );
        if( method == null || !method.isValid() ) return LookupElement.EMPTY_ARRAY;
        var parameters = method.getParameterList().getParameters();
        LookupElement[] lookups = new LookupElement[parameters.length];
        for( int i = 0; i < parameters.length; i++ ) {
            PsiParameter parameter = parameters[i];
            lookups[i] = LookupElementBuilder.create( parameter )
                .withIcon( AllIcons.Nodes.Parameter )
                .withTypeText( parameter.getType().getPresentableText() );
        }
        return lookups;
    }

    public static PsiReference[] refs( PsiLiteralExpression psiElement ) {
        String text = psiElement.getText();
        List<PathParameterReference> references = new ArrayList<>();
        boolean insideReference = false;
        String current = "";
        for( int i = 0; i < text.length(); i++ ) {
            char c = text.charAt( i );
            if( c == '{' ) {
                if( insideReference ) {
                    insideReference = false;
                    current = "";
                } else insideReference = true;
            } else if( c == '}' ) {
                insideReference = false;
                if( current.length() > 0 )
                    references.add( new PathParameterReference( psiElement, current, i - current.length() ) );
                current = "";
            } else if( insideReference ) current += c;
        }
        return references.toArray( new PsiReference[0] );
    }

}
