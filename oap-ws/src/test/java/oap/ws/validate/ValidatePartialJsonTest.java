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

package oap.ws.validate;

import oap.application.testng.KernelFixture;
import oap.testng.Fixtures;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import org.testng.annotations.Test;

import java.util.ArrayList;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_OK;
import static oap.http.Request.HttpMethod.POST;
import static oap.http.testng.HttpAsserts.assertPost;
import static oap.http.testng.HttpAsserts.httpUrl;
import static oap.io.Resources.urlOrThrow;
import static oap.ws.WsParam.From.BODY;
import static oap.ws.WsParam.From.PATH;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

public class ValidatePartialJsonTest extends Fixtures {
    //todo refactor this static madness
    private static TestBean bean;

    {
        fixture( new KernelFixture( urlOrThrow( getClass(), "/application.test.conf" ) ) );
    }

    @Test
    public void validation1() {
        bean = new TestBean( "id1" );
        assertPost( httpUrl( "/vpj/run/validation/1/id1" ), "{\"id\":1}", APPLICATION_JSON )
            .respondedJson( HTTP_OK, "OK", "{\"a\":[{\"id\":1}],\"id\":\"id1\"}" );
        assertPost( httpUrl( "/vpj/run/validation/1/id1" ), "{\"b\":[{\"element\":\"test\"}],\"id\":1}", APPLICATION_JSON )
            .respondedJson( HTTP_OK, "OK", "{\"a\":[{\"id\":1,\"b\":[{\"element\":\"test\"}]}],\"id\":\"id1\"}" );
        assertPost( httpUrl( "/vpj/run/validation/1/id1" ), "{}", APPLICATION_JSON )
            .responded( HTTP_BAD_REQUEST, "validation failed", APPLICATION_JSON, "{\"errors\":[\"/a/1/id: required property is missing\"]}" );
    }

    @Test
    public void validation2() {
        bean = new TestBean( "id1" );
        assertPost( httpUrl( "/vpj/run/validation/2/id1" ), "{\"id\":1}", APPLICATION_JSON )
            .respondedJson( HTTP_OK, "OK", "{\"a\":[{\"id\":1}],\"id\":\"id1\"}" );
        assertPost( httpUrl( "/vpj/run/validation/2/id1" ), "{}", APPLICATION_JSON )
            .respondedJson( HTTP_OK, "OK", "{\"a\":[{}],\"id\":\"id1\"}" );
        assertPost( httpUrl( "/vpj/run/validation/2/id1" ), "{\"c\":1}", APPLICATION_JSON )
            .responded( HTTP_BAD_REQUEST, "validation failed", APPLICATION_JSON, "{\"errors\":[\"/a/1: additional properties are not permitted [c]\"]}" );
    }

    @Test
    public void validation3() {
        bean = new TestBean( "id1" );
        final TestBean.TestItem itemA = new TestBean.TestItem();
        itemA.id = 1;

        final TestBean.TestItem itemB = new TestBean.TestItem();
        itemB.id = 2;

        bean.a.add( itemA );
        bean.a.add( itemB );
        assertPost( httpUrl( "/vpj/run/validation/3/id1/2" ), "{\"element\":\"some text\"}", APPLICATION_JSON )
            .respondedJson( HTTP_OK, "OK", "{\"a\":[{\"id\":1},{\"id\":2,\"b\":[{\"element\":\"some text\"}]}],\"id\":\"id1\"}" );
    }

    @SuppressWarnings( "unused" )
    public static class TestWS {
        @WsMethod( path = "/run/validation/1/{id}", method = POST )
        public TestBean validation1(
            @WsParam( from = PATH ) String id,
            @WsPartialValidateJson(
                methodName = "findBean",
                idParameterName = "id",
                path = "a",
                schema = "/oap/ws/validate/ValidatePartialJsonTest/partial-schema.conf" )
            @WsParam( from = BODY ) TestBean.TestItem body
        ) {
            bean.a.clear();
            bean.a.add( body );
            return bean;
        }

        @WsMethod( path = "/run/validation/2/{id}", method = POST )
        public TestBean validation2(
            @WsParam( from = PATH ) String id,
            @WsPartialValidateJson(
                methodName = "findBean",
                idParameterName = "id",
                path = "a",
                schema = "/oap/ws/validate/ValidatePartialJsonTest/partial-schema.conf",
                ignoreRequired = true )
            @WsParam( from = BODY ) TestBean.TestItem body
        ) {
            bean.a.clear();
            bean.a.add( body );
            return bean;
        }

        @WsMethod( path = "/run/validation/3/{id}/{bId}", method = POST )
        public TestBean validation3(
            @WsParam( from = PATH ) String id,
            @WsParam( from = PATH ) Integer bId,
            @WsPartialValidateJson(
                methodName = "findBean",
                idParameterName = "id",
                path = "a.${bId}.b",
                schema = "/oap/ws/validate/ValidatePartialJsonTest/partial-schema.conf",
                ignoreRequired = true )
            @WsParam( from = BODY ) TestBean.TestItem.SubTestItem body
        ) {
            for( TestBean.TestItem t : bean.a )
                if( t.id.equals( bId ) ) t.b.add( body );

            return bean;
        }

        @SuppressWarnings( "unused" )
        public TestBean findBean( String id ) {
            return bean.id.equals( id ) ? bean : null;
        }
    }

    @SuppressWarnings( "unused" )
    public static class TestBean {
        public ArrayList<TestItem> a = new ArrayList<>();
        public String id;
        public ArrayList<TestItem.SubTestItem> elements = new ArrayList<>();

        public TestBean() {
        }

        public TestBean( String id ) {
            this.id = id;
        }

        public static class TestItem {
            public Integer id;
            public ArrayList<SubTestItem> b = new ArrayList<>();

            public static class SubTestItem {
                public String element;
            }
        }
    }
}
