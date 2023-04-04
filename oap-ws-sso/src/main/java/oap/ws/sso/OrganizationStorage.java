/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.sso;


import oap.id.Identifier;
import oap.storage.MemoryStorage;
import oap.ws.sso.model.OrganizationData;

import static oap.storage.Storage.Lock.SERIALIZED;

public class OrganizationStorage extends MemoryStorage<String, OrganizationData> {
    public OrganizationStorage() {
        super( Identifier.<OrganizationData>forId( o -> o.organization.id, ( o, id ) -> o.organization.id = id )
            .suggestion( o -> o.organization.name )
            .build(), SERIALIZED );
    }

    public void deleteAllPermanently() {
        for( var organizationData : this ) {
            memory.removePermanently( organizationData.organization.id );
        }
    }
}
