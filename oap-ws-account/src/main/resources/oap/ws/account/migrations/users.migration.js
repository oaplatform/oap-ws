db.users.find().forEach(function (u) {
    if (u.object.user)
        return;
    var user = {};
    if (u.object.accessKey) user.accessKey = u.object.accessKey;
    if (u.object.apiKey) user.apiKey = u.object.apiKey;
    if (u.object.confirmed) user.confirmed = u.object.confirmed;
    if (u.object.email) user.email = u.object.email;
    if (u.object.firstName) user.firstName = u.object.firstName;
    if (u.object.lastName) user.lastName = u.object.lastName;
    if (u.object.password) user.password = u.object.password;
    if (u.object.ext) user.ext = u.object.ext

    u.object.user = user;

    delete u.object.accessKey;
    delete u.object.apiKey;
    delete u.object.confirmed;
    delete u.object.email;
    delete u.object.firstName;
    delete u.object.lastName;
    delete u.object.password;
    delete u.object.ext;

    db.users.update({ _id: u._id }, u);
});
