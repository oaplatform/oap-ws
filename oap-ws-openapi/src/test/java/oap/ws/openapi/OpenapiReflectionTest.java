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

package oap.ws.openapi;

import oap.http.server.nio.HttpServerExchange;
import oap.reflect.Reflect;
import oap.reflect.Reflection.Method;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import org.testng.annotations.Test;

import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static oap.ws.WsParam.From.BODY;
import static oap.ws.WsParam.From.PATH;
import static oap.ws.WsParam.From.QUERY;
import static oap.ws.WsParam.From.SESSION;
import static org.assertj.core.api.Assertions.assertThat;

public class OpenapiReflectionTest {

    @Test
    public void webMethod() {
        assertThat( OpenapiReflection.webMethod( Reflect.reflect( Ws.class )
            .method( "test" )
            .orElseThrow() ) )
            .isTrue();
    }

    @Test
    public void webMethodPrivate() {
        Method method = Reflect.reflect( Ws.class ).method( "tst" ).orElseThrow();
        assertThat( OpenapiReflection.webMethod( method ) ).isFalse();
        assertThat( method.isAnnotatedWith( WsMethod.class ) ).isTrue();

        WsMethod wsMethod = method.findAnnotation( WsMethod.class ).orElseThrow();
        assertThat( wsMethod.method() ).containsOnly( GET );
        assertThat( wsMethod.description() ).isEqualTo( "described" );
        assertThat( wsMethod.path() ).isEqualTo( "/tst" );
    }

    @Test
    public void webMethodStatic() {
        assertThat( OpenapiReflection.webMethod( Reflect.reflect( Ws.class )
            .method( "statictst" )
            .orElseThrow() ) )
            .isFalse();
    }

    @Test
    public void webParameterBody() {

        assertThat( OpenapiReflection.webParameter( Reflect.reflect( Ws.class )
            .method( "paramsMethod" )
            .orElseThrow()
            .getParameter( "body" ) ) ).isTrue();
    }

    @Test
    public void webParameterSession() {
        assertThat( OpenapiReflection.webParameter( Reflect.reflect( Ws.class )
            .method( "paramsMethod" )
            .orElseThrow()
            .getParameter( "ses" ) ) )
            .isFalse();
    }

    @Test
    public void webParameterExchange() {
        assertThat( OpenapiReflection.webParameter( Reflect.reflect( Ws.class )
            .method( "paramsMethod" )
            .orElseThrow()
            .getParameter( "exchange" ) ) )
            .isFalse();
    }

    @Test
    public void fromBody() {
        assertThat( OpenapiReflection.from( Reflect.reflect( Ws.class )
            .method( "paramsMethod" )
            .orElseThrow()
            .getParameter( "body" ) ) )
            .isEqualTo( BODY.name().toLowerCase() );
    }

    @Test
    public void fromSession() {
        assertThat( OpenapiReflection.from( Reflect.reflect( Ws.class )
            .method( "paramsMethod" )
            .orElseThrow()
            .getParameter( "ses" ) ) )
            .isEqualTo( SESSION.name().toLowerCase() );
    }

    @Test
    public void fromPath() {
        assertThat( OpenapiReflection.from( Reflect.reflect( Ws.class )
            .method( "paramsMethod" )
            .orElseThrow()
            .getParameter( "pathId" ) ) )
            .isEqualTo( PATH.name().toLowerCase() );
    }

    @Test
    public void fromQuery() {
        assertThat( OpenapiReflection.from( Reflect.reflect( Ws.class )
            .method( "paramsMethod" )
            .orElseThrow()
            .getParameter( "query" ) ) )
            .isEqualTo( QUERY.name().toLowerCase() );
    }

    @Test
    public void tag() {
        assertThat( OpenapiReflection.tag( Reflect.reflect( Ws.class ) ) )
            .isEqualTo( Ws.class.getName() );
    }

    @SuppressWarnings( "unused" )
    static class Ws {
        @WsMethod( method = GET, path = "/test" )
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

        @WsMethod( method = GET, path = "/{pathId}" )
        public String paramsMethod( @WsParam( from = BODY ) String body,
                                    @WsParam( from = SESSION ) String ses,
                                    @WsParam( from = PATH ) String pathId,
                                    String query,
                                    HttpServerExchange exchange ) {
            return "";
        }
    }
}
