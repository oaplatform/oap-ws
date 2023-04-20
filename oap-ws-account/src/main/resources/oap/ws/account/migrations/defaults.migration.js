db.organizations.replaceOne(
    {"_id": "DFLT"},
    {
        "object": {
            "organization": {
                "id": "DFLT",
                "name": "Default",
                "description": "Default organization"
            }
        },
        "object:type": "organization",
        "modified": new Date().getTime()
    },
    {"upsert": true}
);

db.users.replaceOne(
    {"_id": "xenoss@xenoss.io"},
    {
        "object": {
            "user": {
                "firstName": "System",
                "lastName": "Admin",
                "email": "xenoss@xenoss.io",
                "password": hex_md5("Xenoss123").toUpperCase(),
                "confirmed": true,
            },
            "roles": {"DFLT": "ADMIN", "SYSTEM": "ADMIN"},
        },
        "object:type": "user",
        "modified": new Date().getTime()
    },
    {"upsert": true}
);
