package oap.ws.idea;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Psi {

    public static String stripQuotes( String value ) {
        return value == null ? null : value.startsWith( "\"" )
            ? value.substring( 1, value.endsWith( "\"" ) ? value.length() - 1 : value.length() )
            : value;
    }


    public static <T extends PsiNamedElement> Map<String, T> toMap( T[] elements ) {
        Map<String, T> map = new HashMap<>();
        for( T e : elements ) map.put( e.getName(), e );
        return map;
    }

    @Nonnull
    public static <T extends PsiElement> List<T> toList( T[] elements ) {
        return new ArrayList<>( Arrays.asList( elements ) );
    }

    @Nonnull
    public static List<PsiParameter> getSignatureMismatch( PsiMethod match, PsiMethod with ) {
        Map<String, PsiParameter> withParameters = toMap( with.getParameterList().getParameters() );
        List<PsiParameter> matchParameters = toList( match.getParameterList().getParameters() );
        matchParameters.removeIf( p -> withParameters.containsKey( p.getName() )
            && p.getType().isAssignableFrom( withParameters.get( p.getName() ).getType() ) );
        return matchParameters;
    }

    @Nullable
    @SuppressWarnings( "unchecked" )
    public static <T extends PsiReference> T findReference( @Nonnull PsiElement psiElement, Class<T> clazz ) {
        for( PsiReference psiReference : psiElement.getReferences() )
            if( clazz.isInstance( psiReference ) ) return ( T ) psiReference;
        return null;
    }

    @Nonnull
    @SuppressWarnings( "unchecked" )
    public static <T extends PsiReference> List<T> findReferences( PsiElement psiElement, Class<T> clazz ) {
        var references = psiElement.getReferences();
        if( references.length == 0 ) return List.of();
        List<T> result = new ArrayList<>();
        for( PsiReference psiReference : references )
            if( clazz.isInstance( psiReference ) ) result.add( ( T ) psiReference );
        return result;
    }

    public static TextRange inElementToGlobal( PsiElement psiElement, TextRange range ) {
        return new TextRange( psiElement.getTextOffset() + range.getStartOffset(), psiElement.getTextOffset() + range.getEndOffset() );
    }
}
