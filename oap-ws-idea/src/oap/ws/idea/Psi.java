package oap.ws.idea;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiParameter;
import org.jetbrains.annotations.NotNull;

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

    @NotNull
    public static <T extends PsiElement> List<T> toList( T[] elements ) {
        return new ArrayList<>( Arrays.asList( elements ) );
    }

    @NotNull
    public static List<PsiParameter> getSignatureMismatch( PsiMethod match, PsiMethod with ) {
        Map<String, PsiParameter> withParameters = toMap( with.getParameterList().getParameters() );
        List<PsiParameter> matchParameters = toList( match.getParameterList().getParameters() );
        matchParameters.removeIf( p -> withParameters.containsKey( p.getName() )
            && p.getType().isAssignableFrom( withParameters.get( p.getName() ).getType() ) );
        return matchParameters;
    }
}
