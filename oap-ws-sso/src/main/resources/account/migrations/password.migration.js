db.users.find().forEach(function(u) {
    db.users.update({_id: u._id}, {$set: {"object.password": u.object.password != null ? hex_md5(u.object.password).toUpperCase() : null}});
});
