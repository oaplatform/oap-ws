/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account;

@SuppressWarnings( "checkstyle:InterfaceIsType" )
public interface Permissions {
    String ORGANIZATION_USER_PASSWD = "organization:user_passwd";
    String ORGANIZATION_APIKEY = "organization:user_apikey";
    String ORGANIZATION_STORE_USER = "organization:store_user";
    String ORGANIZATION_READ = "organization:read";
    String ORGANIZATION_UPDATE = "organization:update";
    String ORGANIZATION_STORE = "organization:store";
    String ORGANIZATION_LIST_USERS = "organization:list_users";
    String ASSIGN_ROLE = "organization:assign_role";
    String ACCOUNT_LIST = "account:list";
    String ACCOUNT_READ = "account:read";
    String ACCOUNT_STORE = "account:store";
    String ACCOUNT_ADD = "organization:user_account";
    String ACCOUNT_DELETE = "account:delete";
    String BAN_USER = "ban:user";
    String UNBAN_USER = "unban:user";
    String MANAGE_SELF = "user:edit_self";
    String USER_READ = "user:read";
    String USER_PASSWD = "user:passwd";
    String USER_APIKEY = "user:apikey";
    String CAMPAIGN_MANAGE = "campaign:manage";
    String PUBLISHER_MANAGE = "publisher:manage";
}
