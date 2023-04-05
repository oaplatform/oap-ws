db.users.find().forEach(function (u) {
    if (u.object.accounts) {
        var prevAccounts = u.object.accounts;
        var newAccounts = u.object.roles ? Object.assign({}, u.object.roles) : {};
        Object.keys(newAccounts).forEach(function (org) {
            newAccounts[org] = u.object.canAccessAnyAccount ? ["*"] : prevAccounts;
        });
        u.object.accounts = newAccounts
        delete u.object.canAccessAnyAccount;
        db.users.update({_id: u._id}, u);
    }
});