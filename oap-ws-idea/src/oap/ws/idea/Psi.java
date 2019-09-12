package oap.ws.idea;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;

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


}
