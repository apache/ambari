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
import os
from resource_management.libraries.script import Script
from resource_management.libraries.functions.version import format_hdp_stack_version
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.is_empty import is_empty
from resource_management.libraries.functions.version import compare_versions
from resource_management.libraries.functions.constants import Direction

# a map of the Ambari role to the component name
# for use with /usr/hdp/current/<component>
SERVER_ROLE_DIRECTORY_MAP = {
  'RANGER_ADMIN' : 'ranger-admin',
  'RANGER_USERSYNC' : 'ranger-usersync'
}

component_directory = Script.get_component_from_role(SERVER_ROLE_DIRECTORY_MAP, "RANGER_ADMIN")

config  = Script.get_config()
tmp_dir = Script.get_tmp_dir()

stack_name = default("/hostLevelParams/stack_name", None)
version = default("/commandParams/version", None)
host_sys_prepped = default("/hostLevelParams/host_sys_prepped", False)

stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
hdp_stack_version = format_hdp_stack_version(stack_version_unformatted)

upgrade_marker_file = format("{tmp_dir}/rangeradmin_ru.inprogress")

xml_configurations_supported = config['configurations']['ranger-env']['xml_configurations_supported']

create_db_dbuser = config['configurations']['ranger-env']['create_db_dbuser']

stack_is_hdp22_or_further = Script.is_hdp_stack_greater_or_equal("2.2")
stack_is_hdp23_or_further = Script.is_hdp_stack_greater_or_equal("2.3")

downgrade_from_version = default("/commandParams/downgrade_from_version", None)
upgrade_direction = default("/commandParams/upgrade_direction", None)

ranger_conf    = '/etc/ranger/admin/conf'
ranger_ugsync_conf = '/etc/ranger/usersync/conf'

if upgrade_direction == Direction.DOWNGRADE and compare_versions(format_hdp_stack_version(version),'2.3' ) < 0:
  stack_is_hdp22_or_further = True
  stack_is_hdp23_or_further = False

if stack_is_hdp22_or_further:
  ranger_home    = '/usr/hdp/current/ranger-admin'
  ranger_conf    = '/etc/ranger/admin/conf'
  ranger_stop    = '/usr/bin/ranger-admin-stop'
  ranger_start   = '/usr/bin/ranger-admin-start'
  usersync_home  = '/usr/hdp/current/ranger-usersync'
  usersync_start = '/usr/bin/ranger-usersync-start'
  usersync_stop  = '/usr/bin/ranger-usersync-stop'
  ranger_ugsync_conf = '/etc/ranger/usersync/conf'

if stack_is_hdp23_or_further:
  ranger_conf = '/usr/hdp/current/ranger-admin/conf'
  ranger_ugsync_conf = '/usr/hdp/current/ranger-usersync/conf'

usersync_services_file = "/usr/hdp/current/ranger-usersync/ranger-usersync-services.sh"

java_home = config['hostLevelParams']['java_home']
unix_user  = config['configurations']['ranger-env']['ranger_user']
unix_group = config['configurations']['ranger-env']['ranger_group']
ranger_pid_dir = config['configurations']['ranger-env']['ranger_pid_dir']
usersync_log_dir = default("/configurations/ranger-env/ranger_usersync_log_dir", "/var/log/ranger/usersync")
admin_log_dir = default("/configurations/ranger-env/ranger_admin_log_dir", "/var/log/ranger/admin")
ranger_admin_default_file = format('{ranger_conf}/ranger-admin-default-site.xml')
security_app_context_file = format('{ranger_conf}/security-applicationContext.xml')
ranger_ugsync_default_file = format('{ranger_ugsync_conf}/ranger-ugsync-default.xml')
usgsync_log4j_file = format('{ranger_ugsync_conf}/log4j.xml')
cred_validator_file = format('{usersync_home}/native/credValidator.uexe')

ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]

db_flavor =  (config['configurations']['admin-properties']['DB_FLAVOR']).lower()
usersync_exturl =  config['configurations']['admin-properties']['policymgr_external_url']
ranger_host = config['clusterHostInfo']['ranger_admin_hosts'][0]
ugsync_host = 'localhost'
usersync_host_info = config['clusterHostInfo']['ranger_usersync_hosts']
if not is_empty(usersync_host_info) and len(usersync_host_info) > 0:
  ugsync_host = config['clusterHostInfo']['ranger_usersync_hosts'][0]
ranger_external_url = config['configurations']['admin-properties']['policymgr_external_url']
ranger_db_name = config['configurations']['admin-properties']['db_name']
ranger_auditdb_name = config['configurations']['admin-properties']['audit_db_name']

sql_command_invoker = config['configurations']['admin-properties']['SQL_COMMAND_INVOKER']
db_root_user = config['configurations']['admin-properties']['db_root_user']
db_root_password = unicode(config['configurations']['admin-properties']['db_root_password'])
db_host =  config['configurations']['admin-properties']['db_host']
ranger_db_user = config['configurations']['admin-properties']['db_user']
ranger_audit_db_user = config['configurations']['admin-properties']['audit_db_user']
ranger_db_password = unicode(config['configurations']['admin-properties']['db_password'])

#ranger-env properties
oracle_home = default("/configurations/ranger-env/oracle_home", "-")

#For curl command in ranger to get db connector
jdk_location = config['hostLevelParams']['jdk_location'] 
java_share_dir = '/usr/share/java'
if db_flavor.lower() == 'mysql':
  jdbc_symlink_name = "mysql-jdbc-driver.jar"
  jdbc_jar_name = "mysql-connector-java.jar"
  audit_jdbc_url = format('jdbc:mysql://{db_host}/{ranger_auditdb_name}')
  jdbc_dialect = "org.eclipse.persistence.platform.database.MySQLPlatform"
elif db_flavor.lower() == 'oracle':
  jdbc_jar_name = "ojdbc6.jar"
  jdbc_symlink_name = "oracle-jdbc-driver.jar"
  jdbc_dialect = "org.eclipse.persistence.platform.database.OraclePlatform"
  colon_count = db_host.count(':')
  if colon_count == 2 or colon_count == 0:
    audit_jdbc_url = format('jdbc:oracle:thin:@{db_host}')
  else:
    audit_jdbc_url = format('jdbc:oracle:thin:@//{db_host}')
elif db_flavor.lower() == 'postgres':
  jdbc_jar_name = "postgresql.jar"
  jdbc_symlink_name = "postgres-jdbc-driver.jar"
  audit_jdbc_url = format('jdbc:postgresql://{db_host}/{ranger_auditdb_name}')
  jdbc_dialect = "org.eclipse.persistence.platform.database.PostgreSQLPlatform"
elif db_flavor.lower() == 'mssql':
  jdbc_jar_name = "sqljdbc4.jar"
  jdbc_symlink_name = "mssql-jdbc-driver.jar"
  audit_jdbc_url = format('jdbc:sqlserver://{db_host};databaseName={ranger_auditdb_name}')
  jdbc_dialect = "org.eclipse.persistence.platform.database.SQLServerPlatform"
elif db_flavor.lower() == 'sqla':
  jdbc_jar_name = "sajdbc4.jar"
  jdbc_symlink_name = "sqlanywhere-jdbc-driver.tar.gz"
  audit_jdbc_url = format('jdbc:sqlanywhere:database={ranger_auditdb_name};host={db_host}')
  jdbc_dialect = "org.eclipse.persistence.platform.database.SQLAnywherePlatform"

downloaded_custom_connector = format("{tmp_dir}/{jdbc_jar_name}")

driver_curl_source = format("{jdk_location}/{jdbc_symlink_name}")
driver_curl_target = format("{java_share_dir}/{jdbc_jar_name}")

if db_flavor.lower() == 'sqla':
  downloaded_custom_connector = format("{tmp_dir}/sqla-client-jdbc.tar.gz")
  jar_path_in_archive = format("{tmp_dir}/sqla-client-jdbc/java/{jdbc_jar_name}")
  libs_path_in_archive = format("{tmp_dir}/sqla-client-jdbc/native/lib64/*")
  jdbc_libs_dir = format("{ranger_home}/native/lib64")
  ld_lib_path = format("{jdbc_libs_dir}")

#for db connection
check_db_connection_jar_name = "DBConnectionVerification.jar"
check_db_connection_jar = format("/usr/lib/ambari-agent/{check_db_connection_jar_name}")
ranger_jdbc_connection_url = config["configurations"]["ranger-admin-site"]["ranger.jpa.jdbc.url"]
ranger_jdbc_driver = config["configurations"]["ranger-admin-site"]["ranger.jpa.jdbc.driver"]

ranger_credential_provider_path = config["configurations"]["ranger-admin-site"]["ranger.credential.provider.path"]
ranger_jpa_jdbc_credential_alias = config["configurations"]["ranger-admin-site"]["ranger.jpa.jdbc.credential.alias"]
ranger_ambari_db_password = unicode(config["configurations"]["admin-properties"]["db_password"])

ranger_jpa_audit_jdbc_credential_alias = config["configurations"]["ranger-admin-site"]["ranger.jpa.audit.jdbc.credential.alias"]
ranger_ambari_audit_db_password = unicode(config["configurations"]["admin-properties"]["audit_db_password"])

ugsync_jceks_path = config["configurations"]["ranger-ugsync-site"]["ranger.usersync.credstore.filename"]
cred_lib_path = os.path.join(ranger_home,"cred","lib","*")
cred_setup_prefix = (format('{ranger_home}/ranger_credential_helper.py'), '-l', cred_lib_path)
ranger_audit_source_type = config["configurations"]["ranger-admin-site"]["ranger.audit.source.type"]

if xml_configurations_supported:
  ranger_usersync_keystore_password = unicode(config["configurations"]["ranger-ugsync-site"]["ranger.usersync.keystore.password"])
  ranger_usersync_ldap_ldapbindpassword = unicode(config["configurations"]["ranger-ugsync-site"]["ranger.usersync.ldap.ldapbindpassword"])
  ranger_usersync_truststore_password = unicode(config["configurations"]["ranger-ugsync-site"]["ranger.usersync.truststore.password"])
  ranger_usersync_keystore_file = config["configurations"]["ranger-ugsync-site"]["ranger.usersync.keystore.file"]
  default_dn_name = 'cn=unixauthservice,ou=authenticator,o=mycompany,c=US'

ranger_admin_hosts = config['clusterHostInfo']['ranger_admin_hosts']
is_ranger_ha_enabled = True if len(ranger_admin_hosts) > 1 else False
ranger_ug_ldap_url = config["configurations"]["ranger-ugsync-site"]["ranger.usersync.ldap.url"]
ranger_ug_ldap_bind_dn = config["configurations"]["ranger-ugsync-site"]["ranger.usersync.ldap.binddn"]
ranger_ug_ldap_user_searchfilter = config["configurations"]["ranger-ugsync-site"]["ranger.usersync.ldap.user.searchfilter"]
ranger_ug_ldap_group_searchbase = config["configurations"]["ranger-ugsync-site"]["ranger.usersync.group.searchbase"]
ranger_ug_ldap_group_searchfilter = config["configurations"]["ranger-ugsync-site"]["ranger.usersync.group.searchfilter"]
ug_sync_source = config["configurations"]["ranger-ugsync-site"]["ranger.usersync.source.impl.class"]
current_host = config['hostname']
if current_host in ranger_admin_hosts:
  ranger_host = current_host
