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

package oap.ws.sso.model;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.json.ext.Ext;

import java.io.Serial;
import java.io.Serializable;

@ToString
@EqualsAndHashCode
public class Organization implements Serializable {
    public static final String SCHEMA = "/schemas/organization.schema.conf";
    @Serial
    private static final long serialVersionUID = 2685007887187110374L;

    public String id;
    public String name;
    public String description;
    public Ext ext;

    public Organization() {
    }

    public Organization( String id, String name, String description ) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public Organization( String name, String description ) {
        this( null, name, description );
    }

    public Organization( String name ) {
        this( name, null );
    }

    @SuppressWarnings( "unchecked" )
    public <E extends Ext> E ext() {
        return ( E ) ext;
    }
}
