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

import com.fasterxml.jackson.core.type.TypeReference;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.core.util.RefUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import junit.framework.TestCase;
import oap.reflect.Reflect;
import oap.reflect.Reflection;
import oap.ws.openapi.ApiInfo;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class SchemaUtilsTest extends TestCase {

    Reflection innerTypeReflect;

    @BeforeTest
    public void setUp() {
        innerTypeReflect = Reflect.reflect( InnerType.class );
    }

    @Test
    public void testPrepareTypeForPrimitive() {
        Type type = SchemaUtils.prepareType( innerTypeReflect.field( "a" ).get().type() );
        assertThat( type ).isEqualTo( new TypeReference<Integer>() {}.getType() );
    }

    @Test
    public void testPrepareTypeForPrimitiveArray() {
        Type type = SchemaUtils.prepareType( innerTypeReflect.field( "b" ).get().type() );
        assertThat( type ).isEqualTo( new TypeReference<List<Integer>>() {}.getType() );
    }

    @Test
    public void testPrepareTypeForString() {
        Type type = SchemaUtils.prepareType( innerTypeReflect.field( "str" ).get().type() );
        assertThat( type ).isEqualTo( new TypeReference<String>() {}.getType() );
    }

    @Test
    public void testPrepareTypeForStringArray() {
        Type type = SchemaUtils.prepareType( innerTypeReflect.field( "objarray" ).get().type() );
        assertThat( type ).isEqualTo( new TypeReference<List<String>>() {}.getType() );
    }

    @Test
    public void testPrepareTypeForObjectList() {
        Type type = SchemaUtils.prepareType( innerTypeReflect.field( "listApi" ).get().type() );
        assertThat( type ).isEqualTo( new TypeReference<List<ApiInfo>>() {}.getType() );
    }

    @Test
    public void testCreateSchemaRefForNull() {
        assertThat( SchemaUtils.createSchemaRef( null, Map.of() ) ).isNull();
    }

    @Test
    public void testCreateSchemaRefForSchemaWithEmptyName() {
        assertThat( SchemaUtils.createSchemaRef( new StringSchema(), Map.of() ) )
            .isEqualTo( new StringSchema() );
    }

    @Test
    public void testCreateSchemaRefForSchemaForEmptyMap() {
        var resolvedSchema = ModelConverters.getInstance().readAllAsResolvedSchema( InnerType.class );
        assertThat( SchemaUtils.createSchemaRef( resolvedSchema.schema, Map.of() ) )
            .isEqualTo( resolvedSchema.schema );
    }

    @Test
    public void testCreateSchemaRefForSchema() {
        var resolvedSchema = ModelConverters.getInstance().readAllAsResolvedSchema( InnerType.class );
        var expected = new Schema();
        expected.name( resolvedSchema.schema.getName() ).$ref( RefUtils.constructRef( resolvedSchema.schema.getName() ) );
        assertThat( SchemaUtils.createSchemaRef( resolvedSchema.schema, resolvedSchema.referencedSchemas ) )
            .isEqualTo( expected );
    }

    @Test
    public void testPrepareSchemaShouldFillApiSchemaMap() {
        var openAPI = new OpenAPI();

        assertThat( openAPI.getComponents() ).isNull();

        var resolvedSchema = SchemaUtils.prepareSchema( InnerType.class, openAPI );

        assertThat( resolvedSchema.referencedSchemas ).isEqualTo( openAPI.getComponents().getSchemas() );
        assertThat( resolvedSchema.schema ).isNotNull();
    }

    @Test
    public void testPrepareSchemaShouldNotFillApiSchemaMap() {
        var openAPI = new OpenAPI();

        assertThat( openAPI.getComponents() ).isNull();

        var resolvedSchema = SchemaUtils.prepareSchema( String.class, openAPI );

        assertThat( openAPI.getComponents() ).isNull();
        assertThat( resolvedSchema.referencedSchemas ).isEmpty();
        assertThat( resolvedSchema.schema ).isNotNull();
    }

    @Test
    public void testResolvedSchemaForInnerType() {

        ResolvedSchema resolvedSchema = SchemaUtils.resolveSchema( InnerType.class );

        assertThat( resolvedSchema ).isNotNull();
        assertThat( resolvedSchema.schema.getProperties() ).hasSize( 9 );
        assertThat( resolvedSchema.schema.getName() ).isEqualTo( "InnerType" );
    }

    @Test
    public void testResolvedSchemaForStringType() {

        ResolvedSchema resolvedSchema = SchemaUtils.resolveSchema( String.class );

        assertThat( resolvedSchema ).isNotNull();
        assertThat( resolvedSchema.schema.getClass() ).isEqualTo( StringSchema.class );
    }

    @Test
    public void testResolvedSchemaForOptionalType() {

        ResolvedSchema resolvedSchema = SchemaUtils.resolveSchema( new TypeReference<Optional<String>>() {}.getType() );

        assertThat( resolvedSchema ).isNotNull();
        assertThat( resolvedSchema.schema.getClass() ).isEqualTo( StringSchema.class );
    }

    @Test
    public void testResolvedSchemaForListType() {

        ResolvedSchema resolvedSchema = SchemaUtils.resolveSchema( new TypeReference<List<String>>() {}.getType() );

        assertThat( resolvedSchema ).isNotNull();
        assertThat( resolvedSchema.schema.getClass() ).isEqualTo( ArraySchema.class );
        assertThat( ( ( ArraySchema ) resolvedSchema.schema ).getItems().getClass() ).isEqualTo( StringSchema.class );
    }

    @Test
    public void testResolvedSchemaForComplexListType() {

        ResolvedSchema resolvedSchema = SchemaUtils.resolveSchema( new TypeReference<Optional<List<List<String>>>>() {}.getType() );

        assertThat( resolvedSchema ).isNotNull();
        var schema = resolvedSchema.schema;
        assertThat( schema.getClass() ).isEqualTo( ArraySchema.class );
        var items = ( ( ArraySchema ) schema ).getItems();
        assertThat( items.getClass() ).isEqualTo( ArraySchema.class );
        assertThat( ( ( ArraySchema ) items ).getItems().getClass() ).isEqualTo( StringSchema.class );
    }

    @Test
    public void testResolvedSchemaForComplexMapType() {

        ResolvedSchema resolvedSchema = SchemaUtils.resolveSchema( new TypeReference<Optional<Map<String, List<String>>>>() {}.getType() );

        assertThat( resolvedSchema ).isNotNull();
        var schema = resolvedSchema.schema;
        assertThat( schema.getClass() ).isEqualTo( MapSchema.class );
        var properties = schema.getAdditionalProperties();
        assertThat( properties.getClass() ).isEqualTo( ArraySchema.class );
        assertThat( ( ( ArraySchema ) properties ).getItems().getClass() ).isEqualTo( StringSchema.class );
    }

    public static class InnerType {
        public int a;
        public int[] b;
        public String str;
        public String[] objarray;
        public List<ApiInfo> listApi;
        public ApiInfo api;
        public Optional<List<ApiInfo>> optional;
        public Map<String, ApiInfo> map;
        public Map<ApiInfo, List<ApiInfo>> mapList;

    }
}
