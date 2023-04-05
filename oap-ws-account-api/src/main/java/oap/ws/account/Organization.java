/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.json.ext.Ext;

import java.io.Serial;
import java.io.Serializable;

@ToString
@EqualsAndHashCode
public class Organization implements Serializable {
    public static final String SCHEMA = "/oap/ws/account/organization.schema.conf";
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
