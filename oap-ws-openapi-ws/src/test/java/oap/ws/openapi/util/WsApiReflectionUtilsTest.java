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

package oap.ws.openapi.util;

import oap.http.server.nio.HttpServerExchange;
import oap.reflect.Reflect;
import oap.reflect.Reflection;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import oap.ws.openapi.WsOpenapi;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static oap.ws.WsParam.From.BODY;
import static oap.ws.WsParam.From.PATH;
import static oap.ws.WsParam.From.QUERY;
import static oap.ws.WsParam.From.SESSION;
import static org.assertj.core.api.Assertions.assertThat;

public class WsApiReflectionUtilsTest {

    Reflection wsAnnotatedReflect;

    @BeforeTest
    public void setUp() {
        wsAnnotatedReflect = Reflect.reflect( WsAnnotatedType.class );
    }

    @Test
    public void filterMethodShouldNotFilterProperMethod() {
        var result = WsApiReflectionUtils.filterMethod( wsAnnotatedReflect.method( "test" ).get() );

        assertThat( result ).isTrue();
    }

    @Test
    public void filterMethodShouldFilterPrivateMethod() {
        Reflection.Method tst = wsAnnotatedReflect.method( "tst" ).get();
        var result = WsApiReflectionUtils.filterMethod( tst );

        assertThat( result ).isFalse();
        assertThat( tst.isAnnotatedWith( WsMethod.class ) ).isTrue();

        WsMethod wsMethod = tst.findAnnotation( WsMethod.class ).get();

        assertThat( wsMethod.method() ).isEqualTo( new HttpServerExchange.HttpMethod[] { GET } );
        assertThat( wsMethod.description() ).isEqualTo( "described" );
        assertThat( wsMethod.path() ).isEqualTo( "/tst" );
    }

    @Test
    public void filterMethodShouldFilterStaticMethod() {
        var result = WsApiReflectionUtils.filterMethod( wsAnnotatedReflect.method( "statictst" ).get() );

        assertThat( result ).isFalse();
    }

    @Test
    public void filterParameterShouldReturnTrue() {
        var paramsMethod = wsAnnotatedReflect.method( "paramsMethod" ).get();
        var result = WsApiReflectionUtils.filterParameter( paramsMethod.getParameter( "body" ) );

        assertThat( result ).isTrue();
    }

    @Test
    public void filterParameterShouldReturnFalseForSession() {
        var paramsMethod = wsAnnotatedReflect.method( "paramsMethod" ).get();
        var result = WsApiReflectionUtils.filterParameter( paramsMethod.getParameter( "ses" ) );

        assertThat( result ).isFalse();
    }

    @Test
    public void filterParameterShouldReturnFalseForExchange() {
        var paramsMethod = wsAnnotatedReflect.method( "paramsMethod" ).get();
        var result = WsApiReflectionUtils.filterParameter( paramsMethod.getParameter( "exchange" ) );

        assertThat( result ).isFalse();
    }

    @Test
    public void filterTypeShouldReturnTrue() {
        var result = WsApiReflectionUtils.filterType( wsAnnotatedReflect );

        assertThat( result ).isTrue();
    }

    @Test
    public void filterTypeShouldReturnFalseForNotAnnotated() {
        var result = WsApiReflectionUtils.filterType( Reflect.reflect( NotAnnotated.class ) );

        assertThat( result ).isFalse();
    }

    @Test
    public void filterTypeShouldReturnFalseForDisabled() {
        var result = WsApiReflectionUtils.filterType( Reflect.reflect( AnnotatedDisabled.class ) );

        assertThat( result ).isFalse();
    }

    @Test
    public void fromShouldReturnBody() {
        var paramsMethod = wsAnnotatedReflect.method( "paramsMethod" ).get();
        var result = WsApiReflectionUtils.from( paramsMethod.getParameter( "body" ) );

        assertThat( result ).isEqualTo( BODY.name().toLowerCase() );
    }

    @Test
    public void fromShouldReturnSession() {
        var paramsMethod = wsAnnotatedReflect.method( "paramsMethod" ).get();
        var result = WsApiReflectionUtils.from( paramsMethod.getParameter( "ses" ) );

        assertThat( result ).isEqualTo( SESSION.name().toLowerCase() );
    }

    @Test
    public void fromShouldReturnPath() {
        var paramsMethod = wsAnnotatedReflect.method( "paramsMethod" ).get();
        var result = WsApiReflectionUtils.from( paramsMethod.getParameter( "pathId" ) );

        assertThat( result ).isEqualTo( PATH.name().toLowerCase() );
    }

    @Test
    public void fromShouldReturnQuery() {
        var paramsMethod = wsAnnotatedReflect.method( "paramsMethod" ).get();
        var result = WsApiReflectionUtils.from( paramsMethod.getParameter( "query" ) );

        assertThat( result ).isEqualTo( QUERY.name().toLowerCase() );
    }

    @Test
    public void tagShouldReturnValueFromTag() {
        var result = WsApiReflectionUtils.tag( wsAnnotatedReflect, "context" );

        assertThat( result ).isEqualTo( "TagValue" );
    }

    @Test
    public void tagShouldReturnValueFromContextIfNotAnnotated() {
        var result = WsApiReflectionUtils.tag( Reflect.reflect( NotAnnotated.class ), "context" );

        assertThat( result ).isEqualTo( "context" );
    }

    @Test
    public void tagShouldReturnValueFromContextIfTagValueIsEmpty() {
        var result = WsApiReflectionUtils.tag( Reflect.reflect( AnnotatedDisabled.class ), "context" );

        assertThat( result ).isEqualTo( "context" );
    }

    @WsOpenapi( tag = "TagValue" )
    static class WsAnnotatedType {
        @WsMethod( method = GET, id = "returnTwo", path = "/test" )
        public int test() {
            return 2;
        }

        @WsMethod( method = GET, path = "/tst", description = "described" )
        private int tst() {
            return 2;
        }

        @WsMethod( method = GET, path = "/statictst" )
        public static int statictst() {
            return 2;
        }

        @WsMethod( method = GET, id = "returnTwo", path = "/{pathId}" )
        public String paramsMethod( @WsParam( from = BODY ) String body,
                                    @WsParam( from = SESSION ) String ses,
                                    @WsParam( from = PATH ) String pathId,
                                    String query,
                                    HttpServerExchange exchange ) {
            return "";
        }
    }

    static class NotAnnotated {
    }

    @WsOpenapi( enabled = false )
    static class AnnotatedDisabled {

    }
}
