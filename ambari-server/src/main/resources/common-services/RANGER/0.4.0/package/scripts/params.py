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
from resource_management.libraries.functions.version import format_stack_version, get_major_version
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.is_empty import is_empty
from resource_management.libraries.functions.constants import Direction
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.stack_features import get_stack_feature_version
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.get_bare_principal import get_bare_principal
from resource_management.core.exceptions import Fail

# a map of the Ambari role to the component name
# for use with <stack-root>/current/<component>
SERVER_ROLE_DIRECTORY_MAP = {
  'RANGER_ADMIN' : 'ranger-admin',
  'RANGER_USERSYNC' : 'ranger-usersync',
  'RANGER_TAGSYNC' : 'ranger-tagsync'
}

component_directory = Script.get_component_from_role(SERVER_ROLE_DIRECTORY_MAP, "RANGER_ADMIN")

config  = Script.get_config()
tmp_dir = Script.get_tmp_dir()
stack_root = Script.get_stack_root()

stack_name = default("/hostLevelParams/stack_name", None)
version = default("/commandParams/version", None)

stack_version_unformatted = config['hostLevelParams']['stack_version']
stack_version_formatted = format_stack_version(stack_version_unformatted)
major_stack_version = get_major_version(stack_version_formatted)

upgrade_marker_file = format("{tmp_dir}/rangeradmin_ru.inprogress")

xml_configurations_supported = config['configurations']['ranger-env']['xml_configurations_supported']

create_db_dbuser = config['configurations']['ranger-env']['create_db_dbuser']

# get the correct version to use for checking stack features
version_for_stack_feature_checks = get_stack_feature_version(config)

stack_supports_rolling_upgrade = check_stack_feature(StackFeature.ROLLING_UPGRADE, version_for_stack_feature_checks)
stack_supports_config_versioning = check_stack_feature(StackFeature.CONFIG_VERSIONING, version_for_stack_feature_checks)
stack_supports_usersync_non_root = check_stack_feature(StackFeature.RANGER_USERSYNC_NON_ROOT, version_for_stack_feature_checks)
stack_supports_ranger_tagsync = check_stack_feature(StackFeature.RANGER_TAGSYNC_COMPONENT, version_for_stack_feature_checks)
stack_supports_ranger_audit_db = check_stack_feature(StackFeature.RANGER_AUDIT_DB_SUPPORT, version_for_stack_feature_checks)
stack_supports_ranger_log4j = check_stack_feature(StackFeature.RANGER_LOG4J_SUPPORT, version_for_stack_feature_checks)
stack_supports_ranger_kerberos = check_stack_feature(StackFeature.RANGER_KERBEROS_SUPPORT, version_for_stack_feature_checks)
stack_supports_usersync_passwd = check_stack_feature(StackFeature.RANGER_USERSYNC_PASSWORD_JCEKS, version_for_stack_feature_checks)
stack_supports_infra_client = check_stack_feature(StackFeature.RANGER_INSTALL_INFRA_CLIENT, version_for_stack_feature_checks)
stack_supports_pid = check_stack_feature(StackFeature.RANGER_PID_SUPPORT, version_for_stack_feature_checks)
stack_supports_ranger_admin_password_change = check_stack_feature(StackFeature.RANGER_ADMIN_PASSWD_CHANGE, version_for_stack_feature_checks)
stack_supports_ranger_setup_db_on_start = check_stack_feature(StackFeature.RANGER_SETUP_DB_ON_START, version_for_stack_feature_checks)
stack_supports_ranger_tagsync_ssl_xml_support = check_stack_feature(StackFeature.RANGER_TAGSYNC_SSL_XML_SUPPORT, version_for_stack_feature_checks)
stack_supports_ranger_solr_configs = check_stack_feature(StackFeature.RANGER_SOLR_CONFIG_SUPPORT, version_for_stack_feature_checks)
stack_supports_secure_ssl_password = check_stack_feature(StackFeature.SECURE_RANGER_SSL_PASSWORD, version_for_stack_feature_checks)

upgrade_direction = default("/commandParams/upgrade_direction", None)

ranger_conf    = '/etc/ranger/admin/conf'
ranger_ugsync_conf = '/etc/ranger/usersync/conf'
ranger_tagsync_home  = format('{stack_root}/current/ranger-tagsync')
ranger_tagsync_conf = format('{stack_root}/current/ranger-tagsync/conf')
tagsync_bin = '/usr/bin/ranger-tagsync'
tagsync_services_file = format('{stack_root}/current/ranger-tagsync/ranger-tagsync-services.sh')
security_store_path = '/etc/security/serverKeys'
tagsync_etc_path = '/etc/ranger/tagsync/'
ranger_tagsync_credential_file= os.path.join(tagsync_etc_path,'rangercred.jceks')
atlas_tagsync_credential_file= os.path.join(tagsync_etc_path,'atlascred.jceks')
ranger_tagsync_keystore_password = config['configurations']['ranger-tagsync-policymgr-ssl']['xasecure.policymgr.clientssl.keystore.password']
ranger_tagsync_truststore_password = config['configurations']['ranger-tagsync-policymgr-ssl']['xasecure.policymgr.clientssl.truststore.password']
atlas_tagsync_keystore_password = config['configurations']['atlas-tagsync-ssl']['xasecure.policymgr.clientssl.keystore.password']
atlas_tagsync_truststore_password = config['configurations']['atlas-tagsync-ssl']['xasecure.policymgr.clientssl.truststore.password']

if upgrade_direction == Direction.DOWNGRADE and not check_stack_feature(StackFeature.CONFIG_VERSIONING, version_for_stack_feature_checks):
  stack_supports_rolling_upgrade = True
  stack_supports_config_versioning = False

if upgrade_direction == Direction.DOWNGRADE and not check_stack_feature(StackFeature.RANGER_USERSYNC_NON_ROOT, version_for_stack_feature_checks):
  stack_supports_usersync_non_root = False

if stack_supports_rolling_upgrade:
  ranger_home    = format('{stack_root}/current/ranger-admin')
  ranger_conf    = '/etc/ranger/admin/conf'
  ranger_stop    = '/usr/bin/ranger-admin-stop'
  ranger_start   = '/usr/bin/ranger-admin-start'
  usersync_home  = format('{stack_root}/current/ranger-usersync')
  usersync_start = '/usr/bin/ranger-usersync-start'
  usersync_stop  = '/usr/bin/ranger-usersync-stop'
  ranger_ugsync_conf = '/etc/ranger/usersync/conf'

if stack_supports_config_versioning:
  ranger_conf = format('{stack_root}/current/ranger-admin/conf')
  ranger_ugsync_conf = format('{stack_root}/current/ranger-usersync/conf')

if stack_supports_ranger_tagsync:
  ranger_tagsync_home  = format('{stack_root}/current/ranger-tagsync')
  tagsync_bin = '/usr/bin/ranger-tagsync'
  ranger_tagsync_conf = format('{stack_root}/current/ranger-tagsync/conf')
  tagsync_services_file = format('{stack_root}/current/ranger-tagsync/ranger-tagsync-services.sh')

usersync_services_file = format('{stack_root}/current/ranger-usersync/ranger-usersync-services.sh')

java_home = config['hostLevelParams']['java_home']
ambari_java_home = default("/commandParams/ambari_java_home", java_home)
unix_user  = config['configurations']['ranger-env']['ranger_user']
unix_group = config['configurations']['ranger-env']['ranger_group']
ranger_pid_dir = default("/configurations/ranger-env/ranger_pid_dir", "/var/run/ranger")
usersync_log_dir = default("/configurations/ranger-env/ranger_usersync_log_dir", "/var/log/ranger/usersync")
admin_log_dir = default("/configurations/ranger-env/ranger_admin_log_dir", "/var/log/ranger/admin")
ranger_admin_default_file = format('{ranger_conf}/ranger-admin-default-site.xml')
security_app_context_file = format('{ranger_conf}/security-applicationContext.xml')
ranger_ugsync_default_file = format('{ranger_ugsync_conf}/ranger-ugsync-default.xml')
usgsync_log4j_file = format('{ranger_ugsync_conf}/log4j.xml')
if stack_supports_ranger_log4j:
  usgsync_log4j_file = format('{ranger_ugsync_conf}/log4j.properties')
cred_validator_file = format('{usersync_home}/native/credValidator.uexe')

ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]

db_flavor =  (config['configurations']['admin-properties']['DB_FLAVOR']).lower()
usersync_exturl =  config['configurations']['admin-properties']['policymgr_external_url']
if usersync_exturl.endswith('/'):
  usersync_exturl = usersync_exturl.rstrip('/')
ranger_host = config['clusterHostInfo']['ranger_admin_hosts'][0]
ugsync_host = 'localhost'
usersync_host_info = config['clusterHostInfo']['ranger_usersync_hosts']
if not is_empty(usersync_host_info) and len(usersync_host_info) > 0:
  ugsync_host = config['clusterHostInfo']['ranger_usersync_hosts'][0]
ranger_external_url = config['configurations']['admin-properties']['policymgr_external_url']
if ranger_external_url.endswith('/'):
  ranger_external_url = ranger_external_url.rstrip('/')
ranger_db_name = config['configurations']['admin-properties']['db_name']
ranger_auditdb_name = default('/configurations/admin-properties/audit_db_name', 'ranger_audits')

sql_command_invoker = config['configurations']['admin-properties']['SQL_COMMAND_INVOKER']
db_root_user = config['configurations']['admin-properties']['db_root_user']
db_root_password = unicode(config['configurations']['admin-properties']['db_root_password'])
db_host =  config['configurations']['admin-properties']['db_host']
ranger_db_user = config['configurations']['admin-properties']['db_user']
ranger_audit_db_user = default('/configurations/admin-properties/audit_db_user', 'rangerlogger')
ranger_db_password = unicode(config['configurations']['admin-properties']['db_password'])

#ranger-env properties
oracle_home = default("/configurations/ranger-env/oracle_home", "-")

#For curl command in ranger to get db connector
jdk_location = config['hostLevelParams']['jdk_location'] 
java_share_dir = '/usr/share/java'
jdbc_jar_name = None
previous_jdbc_jar_name = None
if db_flavor.lower() == 'mysql':
  jdbc_jar_name = default("/hostLevelParams/custom_mysql_jdbc_name", None)
  previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_mysql_jdbc_name", None)
  audit_jdbc_url = format('jdbc:mysql://{db_host}/{ranger_auditdb_name}') if stack_supports_ranger_audit_db else None
  jdbc_dialect = "org.eclipse.persistence.platform.database.MySQLPlatform"
elif db_flavor.lower() == 'oracle':
  jdbc_jar_name = default("/hostLevelParams/custom_oracle_jdbc_name", None)
  previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_oracle_jdbc_name", None)
  jdbc_dialect = "org.eclipse.persistence.platform.database.OraclePlatform"
  colon_count = db_host.count(':')
  if colon_count == 2 or colon_count == 0:
    audit_jdbc_url = format('jdbc:oracle:thin:@{db_host}') if stack_supports_ranger_audit_db else None
  else:
    audit_jdbc_url = format('jdbc:oracle:thin:@//{db_host}') if stack_supports_ranger_audit_db else None
elif db_flavor.lower() == 'postgres':
  jdbc_jar_name = default("/hostLevelParams/custom_postgres_jdbc_name", None)
  previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_postgres_jdbc_name", None)
  audit_jdbc_url = format('jdbc:postgresql://{db_host}/{ranger_auditdb_name}') if stack_supports_ranger_audit_db else None
  jdbc_dialect = "org.eclipse.persistence.platform.database.PostgreSQLPlatform"
elif db_flavor.lower() == 'mssql':
  jdbc_jar_name = default("/hostLevelParams/custom_mssql_jdbc_name", None)
  previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_mssql_jdbc_name", None)
  audit_jdbc_url = format('jdbc:sqlserver://{db_host};databaseName={ranger_auditdb_name}') if stack_supports_ranger_audit_db else None
  jdbc_dialect = "org.eclipse.persistence.platform.database.SQLServerPlatform"
elif db_flavor.lower() == 'sqla':
  jdbc_jar_name = default("/hostLevelParams/custom_sqlanywhere_jdbc_name", None)
  previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_sqlanywhere_jdbc_name", None)
  audit_jdbc_url = format('jdbc:sqlanywhere:database={ranger_auditdb_name};host={db_host}') if stack_supports_ranger_audit_db else None
  jdbc_dialect = "org.eclipse.persistence.platform.database.SQLAnywherePlatform"
else: raise Fail(format("'{db_flavor}' db flavor not supported."))

downloaded_custom_connector = format("{tmp_dir}/{jdbc_jar_name}")

driver_curl_source = format("{jdk_location}/{jdbc_jar_name}")
driver_curl_target = format("{java_share_dir}/{jdbc_jar_name}")
previous_jdbc_jar = format("{java_share_dir}/{previous_jdbc_jar_name}")
if stack_supports_config_versioning:
  driver_curl_target = format("{ranger_home}/ews/lib/{jdbc_jar_name}")
  previous_jdbc_jar = format("{ranger_home}/ews/lib/{previous_jdbc_jar_name}")

if db_flavor.lower() == 'sqla':
  downloaded_custom_connector = format("{tmp_dir}/sqla-client-jdbc.tar.gz")
  jar_path_in_archive = format("{tmp_dir}/sqla-client-jdbc/java/sajdbc4.jar")
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

ranger_jpa_audit_jdbc_credential_alias = default('/configurations/ranger-admin-site/ranger.jpa.audit.jdbc.credential.alias', 'rangeraudit')
ranger_ambari_audit_db_password = ''
if not is_empty(config["configurations"]["admin-properties"]["audit_db_password"]) and stack_supports_ranger_audit_db:
  ranger_ambari_audit_db_password = unicode(config["configurations"]["admin-properties"]["audit_db_password"])

ugsync_jceks_path = config["configurations"]["ranger-ugsync-site"]["ranger.usersync.credstore.filename"]
ugsync_cred_lib = os.path.join(usersync_home,"lib","*")
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

# ranger-tagsync
ranger_tagsync_hosts = default("/clusterHostInfo/ranger_tagsync_hosts", [])
has_ranger_tagsync = len(ranger_tagsync_hosts) > 0

tagsync_log_dir = default("/configurations/ranger-tagsync-site/ranger.tagsync.logdir", "/var/log/ranger/tagsync")
tagsync_jceks_path = config["configurations"]["ranger-tagsync-site"]["ranger.tagsync.keystore.filename"]
atlas_tagsync_jceks_path = config["configurations"]["ranger-tagsync-site"]["ranger.tagsync.source.atlasrest.keystore.filename"]
tagsync_application_properties = dict(config["configurations"]["tagsync-application-properties"]) if has_ranger_tagsync else None
tagsync_pid_file = format('{ranger_pid_dir}/tagsync.pid')
tagsync_cred_lib = os.path.join(ranger_tagsync_home, "lib", "*")

ranger_usersync_log_maxfilesize = default('/configurations/usersync-log4j/ranger_usersync_log_maxfilesize',256) 
ranger_usersync_log_maxbackupindex = default('/configurations/usersync-log4j/ranger_usersync_log_maxbackupindex',20)
ranger_tagsync_log_maxfilesize = default('/configurations/tagsync-log4j/ranger_tagsync_log_maxfilesize',256)
ranger_tagsync_log_number_of_backup_files = default('/configurations/tagsync-log4j/ranger_tagsync_log_number_of_backup_files',20)
ranger_xa_log_maxfilesize = default('/configurations/admin-log4j/ranger_xa_log_maxfilesize',256)
ranger_xa_log_maxbackupindex = default('/configurations/admin-log4j/ranger_xa_log_maxbackupindex',20)

# ranger log4j.properties
admin_log4j = config['configurations']['admin-log4j']['content']
usersync_log4j = config['configurations']['usersync-log4j']['content']
tagsync_log4j = config['configurations']['tagsync-log4j']['content']

# ranger kerberos
security_enabled = config['configurations']['cluster-env']['security_enabled']
namenode_hosts = default("/clusterHostInfo/namenode_host", [])
has_namenode = len(namenode_hosts) > 0

ugsync_policymgr_alias = config["configurations"]["ranger-ugsync-site"]["ranger.usersync.policymgr.alias"]
ugsync_policymgr_keystore = config["configurations"]["ranger-ugsync-site"]["ranger.usersync.policymgr.keystore"]

# get comma separated list of zookeeper hosts
zookeeper_port = default('/configurations/zoo.cfg/clientPort', None)
zookeeper_hosts = default("/clusterHostInfo/zookeeper_hosts", [])
index = 0
zookeeper_quorum = ""
for host in zookeeper_hosts:
  zookeeper_quorum += host + ":" + str(zookeeper_port)
  index += 1
  if index < len(zookeeper_hosts):
    zookeeper_quorum += ","

# ranger solr
audit_solr_enabled = default('/configurations/ranger-env/xasecure.audit.destination.solr', False)
ranger_solr_config_set = config['configurations']['ranger-env']['ranger_solr_config_set']
ranger_solr_collection_name = config['configurations']['ranger-env']['ranger_solr_collection_name']
ranger_solr_shards = config['configurations']['ranger-env']['ranger_solr_shards']
replication_factor = config['configurations']['ranger-env']['ranger_solr_replication_factor']
ranger_solr_conf = format('{ranger_home}/contrib/solr_for_audit_setup/conf')
infra_solr_hosts = default("/clusterHostInfo/infra_solr_hosts", [])
has_infra_solr = len(infra_solr_hosts) > 0
is_solrCloud_enabled = default('/configurations/ranger-env/is_solrCloud_enabled', False)
is_external_solrCloud_enabled = default('/configurations/ranger-env/is_external_solrCloud_enabled', False)
solr_znode = '/ranger_audits'
if stack_supports_infra_client and is_solrCloud_enabled:
  solr_zookeeper_connect_string = default('/configurations/ranger-admin-site/ranger.audit.solr.zookeepers', 'NONE')
  if solr_zookeeper_connect_string != '' and solr_zookeeper_connect_string.upper() != 'NONE':
    pos = solr_zookeeper_connect_string.index("/")
    solr_znode = solr_zookeeper_connect_string[pos:]
    zookeeper_quorum = solr_zookeeper_connect_string[:pos]
  if has_infra_solr and not is_external_solrCloud_enabled:
    solr_znode = config['configurations']['infra-solr-env']['infra_solr_znode']
solr_user = unix_user
if has_infra_solr and not is_external_solrCloud_enabled:
  solr_user = default('/configurations/infra-solr-env/infra_solr_user', unix_user)
  infra_solr_role_ranger_admin = default('configurations/infra-solr-security-json/infra_solr_role_ranger_admin', 'ranger_user')
  infra_solr_role_ranger_audit = default('configurations/infra-solr-security-json/infra_solr_role_ranger_audit', 'ranger_audit_user')
  infra_solr_role_dev = default('configurations/infra-solr-security-json/infra_solr_role_dev', 'dev')
custom_log4j = has_infra_solr and not is_external_solrCloud_enabled

ranger_audit_max_retention_days = config['configurations']['ranger-solr-configuration']['ranger_audit_max_retention_days']
ranger_audit_logs_merge_factor = config['configurations']['ranger-solr-configuration']['ranger_audit_logs_merge_factor']
ranger_solr_config_content = config['configurations']['ranger-solr-configuration']['content']

# solr kerberised
solr_jaas_file = None
is_external_solrCloud_kerberos = default('/configurations/ranger-env/is_external_solrCloud_kerberos', False)

if security_enabled:
  if has_ranger_tagsync:
    ranger_tagsync_principal = config['configurations']['ranger-tagsync-site']['ranger.tagsync.kerberos.principal']
    if not is_empty(ranger_tagsync_principal) and ranger_tagsync_principal != '':
      tagsync_jaas_principal = ranger_tagsync_principal.replace('_HOST', current_host.lower())
    tagsync_keytab_path = config['configurations']['ranger-tagsync-site']['ranger.tagsync.kerberos.keytab']

  if stack_supports_ranger_kerberos:
    ranger_admin_keytab = config['configurations']['ranger-admin-site']['ranger.admin.kerberos.keytab']
    ranger_admin_principal = config['configurations']['ranger-admin-site']['ranger.admin.kerberos.principal']
    if not is_empty(ranger_admin_principal) and ranger_admin_principal != '':
      ranger_admin_jaas_principal = ranger_admin_principal.replace('_HOST', ranger_host.lower())
      if stack_supports_infra_client and is_solrCloud_enabled and is_external_solrCloud_enabled and is_external_solrCloud_kerberos:
        solr_jaas_file = format('{ranger_home}/conf/ranger_solr_jaas.conf')
        solr_kerberos_principal = ranger_admin_jaas_principal
        solr_kerberos_keytab = ranger_admin_keytab
      if stack_supports_infra_client and is_solrCloud_enabled and not is_external_solrCloud_enabled and not is_external_solrCloud_kerberos:
        solr_jaas_file = format('{ranger_home}/conf/ranger_solr_jaas.conf')
        solr_kerberos_principal = ranger_admin_jaas_principal
        solr_kerberos_keytab = ranger_admin_keytab

# logic to create core-site.xml if hdfs not installed
if stack_supports_ranger_kerberos and not has_namenode:
  core_site_property = {
    'hadoop.security.authentication': 'kerberos' if security_enabled else 'simple'
  }

  if security_enabled:
    realm = 'EXAMPLE.COM'
    ranger_admin_bare_principal = 'rangeradmin'
    ranger_usersync_bare_principal = 'rangerusersync'
    ranger_tagsync_bare_principal = 'rangertagsync'

    ranger_usersync_principal = config['configurations']['ranger-ugsync-site']['ranger.usersync.kerberos.principal']
    if not is_empty(ranger_admin_principal) and ranger_admin_principal != '':
      ranger_admin_bare_principal = get_bare_principal(ranger_admin_principal)
    if not is_empty(ranger_usersync_principal) and ranger_usersync_principal != '':
      ranger_usersync_bare_principal = get_bare_principal(ranger_usersync_principal)
    realm = config['configurations']['kerberos-env']['realm']

    rule_dict = [
      {'principal': ranger_admin_bare_principal, 'user': unix_user},
      {'principal': ranger_usersync_bare_principal, 'user': 'rangerusersync'},
    ]

    if has_ranger_tagsync:
      if not is_empty(ranger_tagsync_principal) and ranger_tagsync_principal != '':
        ranger_tagsync_bare_principal = get_bare_principal(ranger_tagsync_principal)
      rule_dict.append({'principal': ranger_tagsync_bare_principal, 'user': 'rangertagsync'})

    core_site_auth_to_local_property = ''
    for item in range(len(rule_dict)):
      rule_line = 'RULE:[2:$1@$0]({0}@{1})s/.*/{2}/\n'.format(rule_dict[item]['principal'], realm, rule_dict[item]['user'])
      core_site_auth_to_local_property = rule_line + core_site_auth_to_local_property

    core_site_auth_to_local_property = core_site_auth_to_local_property + 'DEFAULT'
    core_site_property['hadoop.security.auth_to_local'] = core_site_auth_to_local_property

upgrade_type = Script.get_upgrade_type(default("/commandParams/upgrade_type", ""))

# ranger service pid
user_group = config['configurations']['cluster-env']['user_group']
ranger_admin_pid_file = format('{ranger_pid_dir}/rangeradmin.pid')
ranger_usersync_pid_file = format('{ranger_pid_dir}/usersync.pid')

# admin credential
admin_username = config['configurations']['ranger-env']['admin_username']
admin_password = config['configurations']['ranger-env']['admin_password']
default_admin_password = 'admin'

ranger_is_solr_kerberised = "false"
if audit_solr_enabled and is_solrCloud_enabled:
  # Check internal solrCloud
  if security_enabled and not is_external_solrCloud_enabled:
    ranger_is_solr_kerberised = "true"
  # Check external solrCloud
  if is_external_solrCloud_enabled and is_external_solrCloud_kerberos:
    ranger_is_solr_kerberised = "true"

hbase_master_hosts = default("/clusterHostInfo/hbase_master_hosts", [])
is_hbase_ha_enabled = True if len(hbase_master_hosts) > 1 else False
is_namenode_ha_enabled = True if len(namenode_hosts) > 1 else False
ranger_hbase_plugin_enabled = False
ranger_hdfs_plugin_enabled = False


if is_hbase_ha_enabled:
  if not is_empty(config['configurations']['ranger-hbase-plugin-properties']['ranger-hbase-plugin-enabled']):
    ranger_hbase_plugin_enabled = config['configurations']['ranger-hbase-plugin-properties']['ranger-hbase-plugin-enabled'].lower() == 'yes'
if is_namenode_ha_enabled:
  if not is_empty(config['configurations']['ranger-hdfs-plugin-properties']['ranger-hdfs-plugin-enabled']):
    ranger_hdfs_plugin_enabled = config['configurations']['ranger-hdfs-plugin-properties']['ranger-hdfs-plugin-enabled'].lower() == 'yes'

ranger_admin_password_properties = ['ranger.jpa.jdbc.password', 'ranger.jpa.audit.jdbc.password', 'ranger.ldap.bind.password', 'ranger.ldap.ad.bind.password']
ranger_usersync_password_properties = ['ranger.usersync.ldap.ldapbindpassword']
ranger_tagsync_password_properties = ['xasecure.policymgr.clientssl.keystore.password', 'xasecure.policymgr.clientssl.truststore.password']
if stack_supports_secure_ssl_password:
  ranger_admin_password_properties.extend(['ranger.service.https.attrib.keystore.pass', 'ranger.truststore.password'])
  ranger_usersync_password_properties.extend(['ranger.usersync.keystore.password', 'ranger.usersync.truststore.password'])

ranger_auth_method = config['configurations']['ranger-admin-site']['ranger.authentication.method']
ranger_ldap_password_alias = default('/configurations/ranger-admin-site/ranger.ldap.binddn.credential.alias', 'ranger.ldap.bind.password')
ranger_ad_password_alias = default('/configurations/ranger-admin-site/ranger.ldap.ad.binddn.credential.alias', 'ranger.ldap.ad.bind.password')
ranger_https_keystore_alias = default('/configurations/ranger-admin-site/ranger.service.https.attrib.keystore.credential.alias', 'keyStoreCredentialAlias')
ranger_truststore_alias = default('/configurations/ranger-admin-site/ranger.truststore.alias', 'trustStoreAlias')
https_enabled = config['configurations']['ranger-admin-site']['ranger.service.https.attrib.ssl.enabled']
http_enabled = config['configurations']['ranger-admin-site']['ranger.service.http.enabled']
https_keystore_password = config['configurations']['ranger-admin-site']['ranger.service.https.attrib.keystore.pass']
truststore_password = config['configurations']['ranger-admin-site']['ranger.truststore.password']

# need this to capture cluster name for ranger tagsync
cluster_name = config['clusterName']
ranger_ldap_bind_auth_password = config['configurations']['ranger-admin-site']['ranger.ldap.bind.password']
ranger_ad_bind_auth_password = config['configurations']['ranger-admin-site']['ranger.ldap.ad.bind.password']
