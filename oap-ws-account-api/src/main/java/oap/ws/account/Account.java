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
public class Account implements Serializable {
    public static final String SCHEMA = "/oap/ws/account/account.schema.conf";
    @Serial
    private static final long serialVersionUID = -1598345391160039855L;

    public String id;
    public String name;
    public String description;
    public Ext ext;

    public Account() {
    }

    public Account( String name ) {
        this.name = name;
    }

    public Account( String id, String name ) {
        this.id = id;
        this.name = name;
    }

    @SuppressWarnings( "unchecked" )
    public <E extends Ext> E ext() {
        return ( E ) ext;
    }
}
