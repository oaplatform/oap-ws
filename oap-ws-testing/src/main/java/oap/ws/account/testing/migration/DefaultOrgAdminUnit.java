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

package oap.ws.account.testing.migration;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import oap.util.Hash;
import org.bson.Document;

import java.util.Map;

@ChangeUnit( id = "DefaultOrgAdminUnit", order = "1", systemVersion = "1" )
public class DefaultOrgAdminUnit {
    @Execution
    public void execution( MongoDatabase mongoDatabase ) {
        mongoDatabase.getCollection( "users" )
            .replaceOne( Filters.eq( "_id", "orgadmin@admin.com" ),
                new Document( Map.of(
                    "object", new Document( Map.of(
                        "user", new Document( Map.of(
                            "accessKey", "HMWDDRMHNGKU",
                            "firstName", "Johnny",
                            "lastName", "Walker",
                            "email", "orgadmin@admin.com",
                            "password", Hash.md5( "Xenoss123" ).toUpperCase(),
                            "confirmed", true,
                            "defaultOrganization", "DFLT",
                            "apiKey", "pz7r93Hh8ssbcV1Qhxsopej18ng2Q"
                        ) ),
                        "roles", new Document( Map.of( "DFLT", "ORGANIZATION_ADMIN" ) )
                    ) ),
                    "object:type", "user",
                    "modified", System.currentTimeMillis()
                ) ), new ReplaceOptions().upsert( true ) );

        mongoDatabase.getCollection( "users" )
            .replaceOne( Filters.eq( "_id", "systemadmin@admin.com" ),
                new Document( Map.of(
                    "object", new Document( Map.of(
                        "user", new Document( Map.of(
                            "firstName", "System",
                            "lastName", "Admin",
                            "email", "systemadmin@admin.com",
                            "password", Hash.md5( "Xenoss123" ).toUpperCase(),
                            "confirmed", true,
                            "defaultOrganization", "SYSTEM",
                            "apiKey", "qwfqwrqfdsgrwqewgreh4t2wrge43K"
                        ) ),
                        "roles", new Document( Map.of( "DFLT", "ORGANIZATION_ADMIN", "SYSTEM", "ADMIN" ) )
                    ) ),
                    "object:type", "user",
                    "modified", System.currentTimeMillis()
                ) ), new ReplaceOptions().upsert( true ) );
    }

    @RollbackExecution
    public void rollback( MongoDatabase mongoDatabase ) {

    }
}
