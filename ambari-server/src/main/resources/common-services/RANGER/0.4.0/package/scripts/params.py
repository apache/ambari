#!/usr/bin/env python
"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

"""
from resource_management.libraries.script import Script
from resource_management.libraries.functions.version import format_hdp_stack_version, compare_versions
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.default import default

config  = Script.get_config()
tmp_dir = Script.get_tmp_dir()

stack_name = default("/hostLevelParams/stack_name", None)
version = default("/commandParams/version", None)

stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
hdp_stack_version = format_hdp_stack_version(stack_version_unformatted)

stack_is_hdp22_or_further = hdp_stack_version != "" and compare_versions(hdp_stack_version, '2.2') >= 0

if stack_is_hdp22_or_further:
  ranger_home    = '/usr/hdp/current/ranger-admin'
  ranger_conf    = '/etc/ranger/admin/conf'
  ranger_stop    = '/usr/bin/ranger-admin-stop'
  ranger_start   = '/usr/bin/ranger-admin-start'
  usersync_home  = '/usr/hdp/current/ranger-usersync'
  usersync_start = '/usr/bin/ranger-usersync-start'
  usersync_stop  = '/usr/bin/ranger-usersync-stop'

java_home = config['hostLevelParams']['java_home']
unix_user  = default("/configurations/ranger-env/ranger_user", "ranger")
unix_group = default("/configurations/ranger-env/ranger_group", "ranger")

ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]

# admin-properties
db_flavor = default("/configurations/admin-properties/DB_FLAVOR", "MYSQL")
sql_command_invoker = default("/configurations/admin-properties/SQL_COMMAND_INVOKER", "mysql")
sql_connector_jar = default("/configurations/admin-properties/SQL_CONNECTOR_JAR", "/usr/share/java/mysql-connector-java.jar")
db_root_user = default("/configurations/admin-properties/db_root_user", "root")
db_root_password = default("/configurations/admin-properties/db_root_password", " ")
db_host = default("/configurations/admin-properties/db_host", "localhost")
db_name = default("/configurations/admin-properties/db_name", "ranger")
db_user = default("/configurations/admin-properties/db_user", "rangeradmin")
db_password = default("/configurations/admin-properties/db_password", "rangeradmin")
audit_db_name = default("/configurations/admin-properties/audit_db_name", "ranger_audit")
audit_db_user = default("/configurations/admin-properties/audit_db_user", "rangerlogger")
audit_db_password = default("/configurations/admin-properties/audit_db_password", "rangerlogger")
policymgr_external_url = default("/configurations/admin-properties/policymgr_external_url", "http://localhost:6080")
policymgr_http_enabled = default("/configurations/admin-properties/policymgr_http_enabled", "true")
authentication_method = default("/configurations/admin-properties/authentication_method", "UNIX")
remoteLoginEnabled = default("/configurations/admin-properties/remoteLoginEnabled", "true")
authServiceHostName = default("/configurations/admin-properties/authServiceHostName", "localhost")
authServicePort = default("/configurations/admin-properties/authServicePort", "5151")
xa_ldap_url = default("/configurations/admin-properties/xa_ldap_url", "ldap://71.127.43.33:389")
xa_ldap_userDNpattern = default("/configurations/admin-properties/xa_ldap_userDNpattern", "uid={0},ou=users,dc=xasecure,dc=net")
xa_ldap_groupSearchBase = default("/configurations/admin-properties/xa_ldap_groupSearchBase", "ou=groups,dc=xasecure,dc=net")
xa_ldap_groupSearchFilter = default("/configurations/admin-properties/xa_ldap_groupSearchFilter", "(member=uid={0},ou=users,dc=xasecure,dc=net)")
xa_ldap_groupRoleAttribute = default("/configurations/admin-properties/xa_ldap_groupRoleAttribute", "cn")
xa_ldap_ad_domain = default("/configurations/admin-properties/xa_ldap_ad_domain", "xasecure.net")
xa_ldap_ad_url = default("/configurations/admin-properties/xa_ldap_ad_url", "ldap://ad.xasecure.net:389")

# usersync-properties
sync_source = default("/configurations/usersync-properties/SYNC_SOURCE", "unix")
min_unix_user_id_to_sync = default("/configurations/usersync-properties/MIN_UNIX_USER_ID_TO_SYNC", "1000")
sync_interval = default("/configurations/usersync-properties/SYNC_INTERVAL", "1")
sync_ldap_url = default("/configurations/usersync-properties/SYNC_LDAP_URL", "ldap://localhost:389")
sync_ldap_bind_dn = default("/configurations/usersync-properties/SYNC_LDAP_BIND_DN", "cn=admin,dc=xasecure,dc=net")
sync_ldap_bind_password = default("/configurations/usersync-properties/SYNC_LDAP_BIND_PASSWORD", "admin321")
cred_keystore_filename = default("/configurations/usersync-properties/CRED_KEYSTORE_FILENAME", "/usr/lib/xausersync/.jceks/xausersync.jceks")
sync_ldap_user_search_base = default("/configurations/usersync-properties/SYNC_LDAP_USER_SEARCH_BASE", "ou=users,dc=xasecure,dc=net")
sync_ldap_user_search_scope = default("/configurations/usersync-properties/SYNC_LDAP_USER_SEARCH_SCOPE", "sub")
sync_ldap_user_object_class = default("/configurations/usersync-properties/SYNC_LDAP_USER_OBJECT_CLASS", "person")
sync_ldap_user_search_filter = default("/configurations/usersync-properties/SYNC_LDAP_USER_SEARCH_FILTER", "-")
sync_ldap_user_name_attribute = default("/configurations/usersync-properties/SYNC_LDAP_USER_NAME_ATTRIBUTE", "cn")
sync_ldap_user_group_name_attribute = default("/configurations/usersync-properties/SYNC_LDAP_USER_GROUP_NAME_ATTRIBUTE", "memberof,ismemberof")
sync_ldap_username_case_conversion = default("/configurations/usersync-properties/SYNC_LDAP_USERNAME_CASE_CONVERSION", "lower")
sync_ldap_groupname_case_conversion = default("/configurations/usersync-properties/SYNC_LDAP_GROUPNAME_CASE_CONVERSION", "lower")
logdir = default("/configurations/usersync-properties/logdir", "logs")

# ranger-site
http_enabled = default("/configurations/ranger-site/HTTP_ENABLED", "true")
http_service_port = default("/configurations/ranger-site/HTTP_SERVICE_PORT", "6080")
https_service_port = default("/configurations/ranger-site/HTTPS_SERVICE_PORT", "6182")
https_attrib_keystoreFile = default("/configurations/ranger-site/HTTPS_KEYSTORE_FILE", "/etc/ranger/admin/keys/server.jks")
https_attrib_keystorePass = default("/configurations/ranger-site/HTTPS_KEYSTORE_PASS", "ranger")
https_attrib_keyAlias = default("/configurations/ranger-site/HTTPS_KEY_ALIAS", "mykey")
https_attrib_clientAuth = default("/configurations/ranger-site/HTTPS_CLIENT_AUTH", "want")

#ranger-env properties
oracle_home = default("/configurations/ranger-env/oracle_home", "-")

#For curl command in ranger to get db connector
jdk_location = config['hostLevelParams']['jdk_location'] 
java_share_dir = '/usr/share/java'
if db_flavor and db_flavor.lower() == 'mysql':
  jdbc_symlink_name = "mysql-jdbc-driver.jar"
  jdbc_jar_name = "mysql-connector-java.jar"
elif db_flavor and db_flavor.lower() == 'oracle':
  jdbc_jar_name = "ojdbc6.jar"
  jdbc_symlink_name = "oracle-jdbc-driver.jar"

downloaded_custom_connector = format("{tmp_dir}/{jdbc_jar_name}")

driver_curl_source = format("{jdk_location}/{jdbc_symlink_name}")
driver_curl_target = format("{java_share_dir}/{jdbc_jar_name}")