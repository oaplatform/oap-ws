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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.id.Identifier;
import oap.json.ext.Ext;
import oap.util.AssocList;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.Serial;
import java.io.Serializable;

import static oap.id.Identifier.Option.COMPACT;


@ToString( exclude = "view" )
@EqualsAndHashCode( exclude = "view" )
@NotThreadSafe
public class OrganizationData implements Serializable {
    @Serial
    private static final long serialVersionUID = 649896869101430210L;

    public Organization organization;
    public Accounts accounts = new Accounts();
    @JsonIgnore
    public View view = new View();

    @JsonCreator
    public OrganizationData( Organization organization ) {
        this.organization = organization;
    }

    public OrganizationData addOrUpdateAccount( Account account ) {
        if( account.id == null ) {
            account.id = Identifier.generate( account.name, 5, id -> accounts.containsKey( id ), 10, COMPACT );
        }
        this.accounts.add( account );
        return this;
    }

    public OrganizationData removeAccount( String accountId ) {
        this.accounts.removeKey( accountId );
        return this;
    }

    public OrganizationData update( Organization organization ) {
        this.organization = organization;
        return this;
    }

    public static class Accounts extends AssocList<String, Account> {
        @Override
        protected String keyOf( Account account ) {
            return account.id;
        }
    }

    public class View implements Serializable {
        @Serial
        private static final long serialVersionUID = 9049298204022935855L;

        public String getId() {
            return OrganizationData.this.organization.id;
        }

        public String getName() {
            return OrganizationData.this.organization.name;
        }

        public String getDescription() {
            return OrganizationData.this.organization.description;
        }

        public Ext getExt() {
            return OrganizationData.this.organization.ext;
        }
    }
}
