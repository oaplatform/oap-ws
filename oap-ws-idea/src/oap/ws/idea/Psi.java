package oap.ws.idea;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
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

    public static PsiClass psiClassOf( PsiElement element ) {
        var current = element;
        while( ( current = current.getParent() ) != null )
            if( current instanceof PsiClass ) return ( PsiClass ) current;
        return null;
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
}
