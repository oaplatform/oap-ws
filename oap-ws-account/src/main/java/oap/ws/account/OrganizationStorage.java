/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account;


import oap.id.Identifier;
import oap.storage.MemoryStorage;
import oap.system.Env;

import static oap.storage.Storage.Lock.SERIALIZED;

public class OrganizationStorage extends MemoryStorage<String, OrganizationData> {

    public static final String DEFAULT_ORGANIZATION_ID = "DFLT";
    public static final String DEFAULT_ORGANIZATION_NAME = "Default";
    public static final String DEFAULT_ORGANIZATION_DESCRIPTION = "Default organization";
    public static final String DEFAULT_ORGANIZATION_READONLY = "true";
    private final String defaultOrganizationId;
    private final String defaultOrganizationName;
    private final String defaultOrganizationDescription;
    private final boolean defaultOrganizationReadOnly;

    public OrganizationStorage() {
        this(
            Env.get( "DEFAULT_ORGANIZATION_ID", DEFAULT_ORGANIZATION_ID ),
            Env.get( "DEFAULT_ORGANIZATION_NAME", DEFAULT_ORGANIZATION_NAME ),
            Env.get( "DEFAULT_ORGANIZATION_DESCRIPTION", DEFAULT_ORGANIZATION_DESCRIPTION ),
            Boolean.parseBoolean( Env.get( "DEFAULT_ORGANIZATION_READONLY", DEFAULT_ORGANIZATION_READONLY ) )
        );
    }

    /**
     * @param defaultOrganizationId          default organization id
     * @param defaultOrganizationName        default organization name
     * @param defaultOrganizationDescription default organization description
     * @param defaultOrganizationReadOnly    if true, the storage modifies the default organization to the default values on startup
     */
    public OrganizationStorage( String defaultOrganizationId,
                                String defaultOrganizationName,
                                String defaultOrganizationDescription,
                                boolean defaultOrganizationReadOnly ) {
        super( Identifier.<OrganizationData>forId( o -> o.organization.id, ( o, id ) -> o.organization.id = id )
            .suggestion( o -> o.organization.name )
            .build(), SERIALIZED );

        this.defaultOrganizationId = defaultOrganizationId;
        this.defaultOrganizationName = defaultOrganizationName;
        this.defaultOrganizationDescription = defaultOrganizationDescription;
        this.defaultOrganizationReadOnly = defaultOrganizationReadOnly;
    }

    public void start() {
        update( defaultOrganizationId, d -> {
            if( defaultOrganizationReadOnly ) {
                d.organization.name = defaultOrganizationName;
                d.organization.description = defaultOrganizationDescription;
            }
            return d;
        }, () -> {
            var defaultOrganization = new Organization( defaultOrganizationId, defaultOrganizationName, defaultOrganizationDescription );
            return new OrganizationData( defaultOrganization );
        } );
    }

    public void deleteAllPermanently() {
        for( var organizationData : this ) {
            memory.removePermanently( organizationData.organization.id );
        }
    }
}
