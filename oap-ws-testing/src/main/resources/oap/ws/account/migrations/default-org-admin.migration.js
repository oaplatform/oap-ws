/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

db.users.replaceOne(
    {"_id": "orgadmin@admin.com"},
    {
        "object": {
            "user": {
                "firstName": "Johnny",
                "lastName": "Walker",
                "email": "orgadmin@admin.com",
                "password": hex_md5("Xenoss123").toUpperCase(),
                "confirmed": true,
                "defaultOrganization": "DFLT",
                "apiKey": "pz7r93Hh8ssbcV1Qhxsopej18ng2Q"
            },
            "roles": {"DFLT": "ORGANIZATION_ADMIN"},
        },
        "object:type": "user",
        "modified": new Date().getTime()
    },
    {"upsert": true}
);

db.users.replaceOne(
    {"_id": "systemadmin@admin.com"},
    {
        "object": {
            "user": {
                "firstName": "System",
                "lastName": "Admin",
                "email": "systemadmin@admin.com",
                "password": hex_md5("Xenoss123").toUpperCase(),
                "confirmed": true,
                "defaultOrganization": "SYSTEM",
                "apiKey": "qwfqwrqfdsgrwqewgreh4t2wrge43K"
            },
            "roles": {"DFLT": "ORGANIZATION_ADMIN", "SYSTEM": "ADMIN"},
        },
        "object:type": "user",
        "modified": new Date().getTime()
    },
    {"upsert": true}
);

