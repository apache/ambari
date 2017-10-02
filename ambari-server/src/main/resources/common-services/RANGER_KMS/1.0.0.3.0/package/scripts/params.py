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
from resource_management.libraries.functions import conf_select
from resource_management.libraries.script import Script
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.stack_features import get_stack_feature_version
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.get_bare_principal import get_bare_principal
from resource_management.libraries.functions.is_empty import is_empty
from resource_management.libraries.functions.setup_ranger_plugin_xml import generate_ranger_service_config
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import get_kinit_path
from resource_management.core.exceptions import Fail

config  = Script.get_config()
tmp_dir = Script.get_tmp_dir()
stack_root = Script.get_stack_root()

stack_name = default("/hostLevelParams/stack_name", None)
version = default("/commandParams/version", None)
upgrade_direction = default("/commandParams/upgrade_direction", None)

stack_version_unformatted = config['hostLevelParams']['stack_version']
stack_version_formatted = format_stack_version(stack_version_unformatted)

# get the correct version to use for checking stack features
version_for_stack_feature_checks = get_stack_feature_version(config)

stack_supports_config_versioning = check_stack_feature(StackFeature.CONFIG_VERSIONING, version_for_stack_feature_checks)
stack_support_kms_hsm = check_stack_feature(StackFeature.RANGER_KMS_HSM_SUPPORT, version_for_stack_feature_checks)
stack_supports_ranger_kerberos = check_stack_feature(StackFeature.RANGER_KERBEROS_SUPPORT, version_for_stack_feature_checks)
stack_supports_pid = check_stack_feature(StackFeature.RANGER_KMS_PID_SUPPORT, version_for_stack_feature_checks)
stack_supports_ranger_audit_db = check_stack_feature(StackFeature.RANGER_AUDIT_DB_SUPPORT, version_for_stack_feature_checks)
stack_supports_ranger_kms_ssl = check_stack_feature(StackFeature.RANGER_KMS_SSL, version_for_stack_feature_checks)

hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
security_enabled = config['configurations']['cluster-env']['security_enabled']

if stack_supports_config_versioning:
  kms_home = format('{stack_root}/current/ranger-kms')
  kms_conf_dir = format('{stack_root}/current/ranger-kms/conf')

kms_log_dir = default("/configurations/kms-env/kms_log_dir", "/var/log/ranger/kms")
java_home = config['hostLevelParams']['java_home']
kms_user  = default("/configurations/kms-env/kms_user", "kms")
kms_group = default("/configurations/kms-env/kms_group", "kms")

ranger_kms_audit_log_maxfilesize = default('/configurations/kms-log4j/ranger_kms_audit_log_maxfilesize',256)
ranger_kms_audit_log_maxbackupindex = default('/configurations/kms-log4j/ranger_kms_audit_log_maxbackupindex',20)
ranger_kms_log_maxfilesize = default('/configurations/kms-log4j/ranger_kms_log_maxfilesize',256)
ranger_kms_log_maxbackupindex = default('/configurations/kms-log4j/ranger_kms_log_maxbackupindex',20)

jdk_location = config['hostLevelParams']['jdk_location']
kms_log4j = config['configurations']['kms-log4j']['content']

# ranger host
ranger_admin_hosts = config['clusterHostInfo']['ranger_admin_hosts'][0]
has_ranger_admin = len(ranger_admin_hosts) > 0
kms_host = config['clusterHostInfo']['ranger_kms_server_hosts'][0]
kms_port = config['configurations']['kms-env']['kms_port']

create_db_user = config['configurations']['kms-env']['create_db_user']

#kms properties
db_flavor = (config['configurations']['kms-properties']['DB_FLAVOR']).lower()
db_host = config['configurations']['kms-properties']['db_host']
db_name = config['configurations']['kms-properties']['db_name']
db_user = config['configurations']['kms-properties']['db_user']
db_password = unicode(config['configurations']['kms-properties']['db_password'])
kms_master_key_password = unicode(config['configurations']['kms-properties']['KMS_MASTER_KEY_PASSWD'])
credential_provider_path = config['configurations']['dbks-site']['ranger.ks.jpa.jdbc.credential.provider.path']
jdbc_alias = config['configurations']['dbks-site']['ranger.ks.jpa.jdbc.credential.alias']
masterkey_alias = config['configurations']['dbks-site']['ranger.ks.masterkey.credential.alias']
repo_name = str(config['clusterName']) + '_kms'
repo_name_value = config['configurations']['ranger-kms-security']['ranger.plugin.kms.service.name']
if not is_empty(repo_name_value) and repo_name_value != "{{repo_name}}":
  repo_name = repo_name_value
cred_lib_path = os.path.join(kms_home,"cred","lib","*")
cred_setup_prefix = (format('{kms_home}/ranger_credential_helper.py'), '-l', cred_lib_path)
credential_file = format('/etc/ranger/{repo_name}/cred.jceks')

if has_ranger_admin:
  policymgr_mgr_url = config['configurations']['admin-properties']['policymgr_external_url']
  if 'admin-properties' in config['configurations'] and 'policymgr_external_url' in config['configurations']['admin-properties'] and policymgr_mgr_url.endswith('/'):
    policymgr_mgr_url = policymgr_mgr_url.rstrip('/')
  xa_audit_db_flavor = (config['configurations']['admin-properties']['DB_FLAVOR']).lower()
  xa_audit_db_name = default('/configurations/admin-properties/audit_db_name', 'ranger_audits')
  xa_audit_db_user = default('/configurations/admin-properties/audit_db_user', 'rangerlogger')
  xa_audit_db_password = ''
  if not is_empty(config['configurations']['admin-properties']['audit_db_password']) and stack_supports_ranger_audit_db:
    xa_audit_db_password = config['configurations']['admin-properties']['audit_db_password']
  xa_db_host = config['configurations']['admin-properties']['db_host']

  admin_uname = config['configurations']['ranger-env']['admin_username']
  admin_password = config['configurations']['ranger-env']['admin_password']
  ambari_ranger_admin = config['configurations']['ranger-env']['ranger_admin_username']
  ambari_ranger_password = config['configurations']['ranger-env']['ranger_admin_password']
  admin_uname_password = format("{admin_uname}:{admin_password}")
  ranger_audit_solr_urls = config['configurations']['ranger-admin-site']['ranger.audit.solr.urls']

default_connectors_map = { "mssql":"sqljdbc4.jar",
                           "mysql":"mysql-connector-java.jar",
                           "postgres":"postgresql-jdbc.jar",
                           "oracle":"ojdbc.jar",
                           "sqla":"sajdbc4.jar"}

java_share_dir = '/usr/share/java'
jdbc_jar_name = None
previous_jdbc_jar_name = None
if db_flavor == 'mysql':
  jdbc_jar_name = default("/hostLevelParams/custom_mysql_jdbc_name", None)
  previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_mysql_jdbc_name", None)
  db_jdbc_url = format('jdbc:log4jdbc:mysql://{db_host}/{db_name}')
  db_jdbc_driver = "com.mysql.jdbc.Driver"
  jdbc_dialect = "org.eclipse.persistence.platform.database.MySQLPlatform"
elif db_flavor == 'oracle':
  jdbc_jar_name = default("/hostLevelParams/custom_oracle_jdbc_name", None)
  previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_oracle_jdbc_name", None)
  colon_count = db_host.count(':')
  if colon_count == 2 or colon_count == 0:
    db_jdbc_url = format('jdbc:oracle:thin:@{db_host}')
  else:
    db_jdbc_url = format('jdbc:oracle:thin:@//{db_host}')
  db_jdbc_driver = "oracle.jdbc.OracleDriver"
  jdbc_dialect = "org.eclipse.persistence.platform.database.OraclePlatform"
elif db_flavor == 'postgres':
  jdbc_jar_name = default("/hostLevelParams/custom_postgres_jdbc_name", None)
  previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_postgres_jdbc_name", None)
  db_jdbc_url = format('jdbc:postgresql://{db_host}/{db_name}')
  db_jdbc_driver = "org.postgresql.Driver"
  jdbc_dialect = "org.eclipse.persistence.platform.database.PostgreSQLPlatform"
elif db_flavor == 'mssql':
  jdbc_jar_name = default("/hostLevelParams/custom_mssql_jdbc_name", None)
  previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_mssql_jdbc_name", None)
  db_jdbc_url = format('jdbc:sqlserver://{db_host};databaseName={db_name}')
  db_jdbc_driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
  jdbc_dialect = "org.eclipse.persistence.platform.database.SQLServerPlatform"
elif db_flavor == 'sqla':
  jdbc_jar_name = default("/hostLevelParams/custom_sqlanywhere_jdbc_name", None)
  previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_sqlanywhere_jdbc_name", None)
  db_jdbc_url = format('jdbc:sqlanywhere:database={db_name};host={db_host}')
  db_jdbc_driver = "sap.jdbc4.sqlanywhere.IDriver"
  jdbc_dialect = "org.eclipse.persistence.platform.database.SQLAnywherePlatform"
else: raise Fail(format("'{db_flavor}' db flavor not supported."))

downloaded_custom_connector = format("{tmp_dir}/{jdbc_jar_name}")

driver_curl_source = format("{jdk_location}/{jdbc_jar_name}")
driver_curl_target = format("{kms_home}/ews/webapp/lib/{jdbc_jar_name}")
previous_jdbc_jar = format("{kms_home}/ews/webapp/lib/{previous_jdbc_jar_name}")
ews_lib_jar_path = format("{kms_home}/ews/webapp/lib/{jdbc_jar_name}")

if db_flavor == 'sqla':
  downloaded_custom_connector = format("{tmp_dir}/sqla-client-jdbc.tar.gz")
  jar_path_in_archive = format("{tmp_dir}/sqla-client-jdbc/java/sajdbc4.jar")
  libs_path_in_archive = format("{tmp_dir}/sqla-client-jdbc/native/lib64/*")
  jdbc_libs_dir = format("{kms_home}/native/lib64")
  ld_library_path = format("{jdbc_libs_dir}")

if has_ranger_admin:
  xa_previous_jdbc_jar_name = None
  if stack_supports_ranger_audit_db:
    if xa_audit_db_flavor == 'mysql':
      jdbc_jar = default("/hostLevelParams/custom_mysql_jdbc_name", None)
      xa_previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_mysql_jdbc_name", None)
      audit_jdbc_url = format('jdbc:mysql://{xa_db_host}/{xa_audit_db_name}')
      jdbc_driver = "com.mysql.jdbc.Driver"
    elif xa_audit_db_flavor == 'oracle':
      jdbc_jar = default("/hostLevelParams/custom_oracle_jdbc_name", None)
      xa_previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_oracle_jdbc_name", None)
      colon_count = xa_db_host.count(':')
      if colon_count == 2 or colon_count == 0:
        audit_jdbc_url = format('jdbc:oracle:thin:@{xa_db_host}')
      else:
        audit_jdbc_url = format('jdbc:oracle:thin:@//{xa_db_host}')
      jdbc_driver = "oracle.jdbc.OracleDriver"
    elif xa_audit_db_flavor == 'postgres':
      jdbc_jar = default("/hostLevelParams/custom_postgres_jdbc_name", None)
      xa_previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_postgres_jdbc_name", None)
      audit_jdbc_url = format('jdbc:postgresql://{xa_db_host}/{xa_audit_db_name}')
      jdbc_driver = "org.postgresql.Driver"
    elif xa_audit_db_flavor == 'mssql':
      jdbc_jar = default("/hostLevelParams/custom_mssql_jdbc_name", None)
      xa_previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_mssql_jdbc_name", None)
      audit_jdbc_url = format('jdbc:sqlserver://{xa_db_host};databaseName={xa_audit_db_name}')
      jdbc_driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
    elif xa_audit_db_flavor == 'sqla':
      jdbc_jar = default("/hostLevelParams/custom_sqlanywhere_jdbc_name", None)
      xa_previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_sqlanywhere_jdbc_name", None)
      audit_jdbc_url = format('jdbc:sqlanywhere:database={xa_audit_db_name};host={xa_db_host}')
      jdbc_driver = "sap.jdbc4.sqlanywhere.IDriver"
    else: raise Fail(format("'{xa_audit_db_flavor}' db flavor not supported."))

  downloaded_connector_path = format("{tmp_dir}/{jdbc_jar}") if stack_supports_ranger_audit_db else None
  driver_source = format("{jdk_location}/{jdbc_jar}") if stack_supports_ranger_audit_db else None
  driver_target = format("{kms_home}/ews/webapp/lib/{jdbc_jar}") if stack_supports_ranger_audit_db else None
  xa_previous_jdbc_jar = format("{kms_home}/ews/webapp/lib/{previous_jdbc_jar_name}") if stack_supports_ranger_audit_db else None

repo_config_username = config['configurations']['kms-properties']['REPOSITORY_CONFIG_USERNAME']
repo_config_password = unicode(config['configurations']['kms-properties']['REPOSITORY_CONFIG_PASSWORD'])

kms_plugin_config = {
  'username' : repo_config_username,
  'password' : repo_config_password,
  'provider' : format('kms://http@{kms_host}:{kms_port}/kms')
}

xa_audit_db_is_enabled = False
if stack_supports_ranger_audit_db:
  xa_audit_db_is_enabled = config['configurations']['ranger-kms-audit']['xasecure.audit.destination.db']
ssl_keystore_password = unicode(config['configurations']['ranger-kms-policymgr-ssl']['xasecure.policymgr.clientssl.keystore.password'])
ssl_truststore_password = unicode(config['configurations']['ranger-kms-policymgr-ssl']['xasecure.policymgr.clientssl.truststore.password'])

#For SQLA explicitly disable audit to DB for Ranger
if xa_audit_db_flavor == 'sqla':
  xa_audit_db_is_enabled = False

current_host = config['hostname']
ranger_kms_hosts = config['clusterHostInfo']['ranger_kms_server_hosts']
if current_host in ranger_kms_hosts:
  kms_host = current_host

check_db_connection_jar_name = "DBConnectionVerification.jar"
check_db_connection_jar = format("/usr/lib/ambari-agent/{check_db_connection_jar_name}")
ranger_kms_jdbc_connection_url = config['configurations']['dbks-site']['ranger.ks.jpa.jdbc.url']
ranger_kms_jdbc_driver = config['configurations']['dbks-site']['ranger.ks.jpa.jdbc.driver']

jce_name = default("/hostLevelParams/jce_name", None)
jce_source_dir = format('{tmp_dir}/jce_dir')

#kms hsm support
enable_kms_hsm = default("/configurations/dbks-site/ranger.ks.hsm.enabled", False)
hms_partition_alias = default("/configurations/dbks-site/ranger.ks.hsm.partition.password.alias", "ranger.kms.hsm.partition.password")
hms_partition_passwd = default("/configurations/kms-env/hsm_partition_password", None)

# kms kerberos from stack 2.5 onward
rangerkms_bare_principal = 'rangerkms'

if stack_supports_ranger_kerberos:
  if security_enabled:
    rangerkms_principal = config['configurations']['dbks-site']['ranger.ks.kerberos.principal']
    rangerkms_keytab = config['configurations']['dbks-site']['ranger.ks.kerberos.keytab']
    if not is_empty(rangerkms_principal) and rangerkms_principal != '':
      rangerkms_bare_principal = get_bare_principal(rangerkms_principal)
      rangerkms_principal = rangerkms_principal.replace('_HOST', kms_host.lower())
  kms_plugin_config['policy.download.auth.users'] = format('keyadmin,{rangerkms_bare_principal}')

custom_ranger_service_config = generate_ranger_service_config(config['configurations']['kms-properties'])
if len(custom_ranger_service_config) > 0:
  kms_plugin_config.update(custom_ranger_service_config)

kms_ranger_plugin_repo = {
  'isEnabled' : 'true',
  'configs' : kms_plugin_config,
  'description' : 'kms repo',
  'name' : repo_name,
  'type' : 'kms'
}

# ranger kms pid
user_group = config['configurations']['cluster-env']['user_group']
ranger_kms_pid_dir = default("/configurations/kms-env/ranger_kms_pid_dir", "/var/run/ranger_kms")
ranger_kms_pid_file = format('{ranger_kms_pid_dir}/rangerkms.pid')

if security_enabled:
  spengo_keytab = config['configurations']['kms-site']['hadoop.kms.authentication.signer.secret.provider.zookeeper.kerberos.keytab']
  spnego_principal = config['configurations']['kms-site']['hadoop.kms.authentication.signer.secret.provider.zookeeper.kerberos.principal']
  spnego_principal = spnego_principal.replace('_HOST', current_host.lower())

plugin_audit_password_property = 'xasecure.audit.destination.db.password'
kms_plugin_password_properties = ['xasecure.policymgr.clientssl.keystore.password', 'xasecure.policymgr.clientssl.truststore.password']
dbks_site_password_properties = ['ranger.db.encrypt.key.password', 'ranger.ks.jpa.jdbc.password', 'ranger.ks.hsm.partition.password']
ranger_kms_site_password_properties = ['ranger.service.https.attrib.keystore.pass']
ranger_kms_cred_ssl_path = config['configurations']['ranger-kms-site']['ranger.credential.provider.path']
ranger_kms_ssl_keystore_alias = config['configurations']['ranger-kms-site']['ranger.service.https.attrib.keystore.credential.alias']
ranger_kms_ssl_passwd = config['configurations']['ranger-kms-site']['ranger.service.https.attrib.keystore.pass']
ranger_kms_ssl_enabled = config['configurations']['ranger-kms-site']['ranger.service.https.attrib.ssl.enabled']

xa_audit_hdfs_is_enabled = default("/configurations/ranger-kms-audit/xasecure.audit.destination.hdfs", False)
namenode_host = default("/clusterHostInfo/namenode_host", [])

# need this to capture cluster name from where ranger kms plugin is enabled
cluster_name = config['clusterName']

has_namenode = len(namenode_host) > 0

hdfs_user = default("/configurations/hadoop-env/hdfs_user", None)
hdfs_user_keytab = default("/configurations/hadoop-env/hdfs_user_keytab", None)
hdfs_principal_name = default("/configurations/hadoop-env/hdfs_principal_name", None)
default_fs = default("/configurations/core-site/fs.defaultFS", None)
hdfs_site = config['configurations']['hdfs-site'] if has_namenode else None
hadoop_bin_dir = stack_select.get_hadoop_dir("bin") if has_namenode else None
kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))

import functools
# create partial functions with common arguments for every HdfsResource call
# to create/delete hdfs directory/file/copyfromlocal we need to call params.HdfsResource in code
HdfsResource = functools.partial(
  HdfsResource,
  user=hdfs_user,
  security_enabled = security_enabled,
  keytab = hdfs_user_keytab,
  kinit_path_local = kinit_path_local,
  hadoop_bin_dir = hadoop_bin_dir,
  hadoop_conf_dir = hadoop_conf_dir,
  principal_name = hdfs_principal_name,
  hdfs_site = hdfs_site,
  default_fs = default_fs
)

local_component_list = default("/localComponents", [])
has_hdfs_client_on_node = 'HDFS_CLIENT' in local_component_list