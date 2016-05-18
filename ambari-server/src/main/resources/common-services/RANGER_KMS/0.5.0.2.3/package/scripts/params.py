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
from resource_management.libraries.functions import StackFeature

config  = Script.get_config()
tmp_dir = Script.get_tmp_dir()
stack_root = Script.get_stack_root()

stack_name = default("/hostLevelParams/stack_name", None)
version = default("/commandParams/version", None)

stack_version_unformatted = config['hostLevelParams']['stack_version']
stack_version_formatted = format_stack_version(stack_version_unformatted)

stack_supports_config_versioning =  stack_version_formatted and check_stack_feature(StackFeature.CONFIG_VERSIONING, stack_version_formatted)
stack_support_kms_hsm = stack_version_formatted and check_stack_feature(StackFeature.RANGER_KMS_HSM_SUPPORT, stack_version_formatted)
stack_supports_ranger_kerberos = stack_version_formatted and check_stack_feature(StackFeature.RANGER_KERBEROS_SUPPORT, stack_version_formatted)
hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
security_enabled = config['configurations']['cluster-env']['security_enabled']

if stack_supports_config_versioning:
  kms_home = format('{stack_root}/current/ranger-kms')
  kms_conf_dir = format('{stack_root}/current/ranger-kms/conf')

kms_log_dir = default("/configurations/kms-env/kms_log_dir", "/var/log/ranger/kms")
java_home = config['hostLevelParams']['java_home']
kms_user  = default("/configurations/kms-env/kms_user", "kms")
kms_group = default("/configurations/kms-env/kms_group", "kms")

jdk_location = config['hostLevelParams']['jdk_location']
kms_log4j = config['configurations']['kms-log4j']['content']

# ranger host
stack_supports_ranger_audit_db = stack_version_formatted and check_stack_feature(StackFeature.RANGER_AUDIT_DB_SUPPORT, stack_version_formatted)
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
cred_lib_path = os.path.join(kms_home,"cred","lib","*")
cred_setup_prefix = (format('{kms_home}/ranger_credential_helper.py'), '-l', cred_lib_path)
credential_file = format('/etc/ranger/{repo_name}/cred.jceks')

if has_ranger_admin:
  policymgr_mgr_url = config['configurations']['admin-properties']['policymgr_external_url']
  xa_audit_db_flavor = (config['configurations']['admin-properties']['DB_FLAVOR']).lower()
  xa_audit_db_name = config['configurations']['admin-properties']['audit_db_name']
  xa_audit_db_user = config['configurations']['admin-properties']['audit_db_user']
  xa_audit_db_password = config['configurations']['admin-properties']['audit_db_password']
  xa_db_host = config['configurations']['admin-properties']['db_host']

  admin_uname = config['configurations']['ranger-env']['admin_username']
  admin_password = config['configurations']['ranger-env']['admin_password']
  ambari_ranger_admin = config['configurations']['ranger-env']['ranger_admin_username']
  ambari_ranger_password = config['configurations']['ranger-env']['ranger_admin_password']
  admin_uname_password = format("{admin_uname}:{admin_password}")
  ranger_audit_solr_urls = config['configurations']['ranger-admin-site']['ranger.audit.solr.urls']

java_share_dir = '/usr/share/java'

if db_flavor == 'mysql':
  jdbc_jar_name = default("/hostLevelParams/custom_mysql_jdbc_name", None)
  db_jdbc_url = format('jdbc:log4jdbc:mysql://{db_host}/{db_name}')
  db_jdbc_driver = "com.mysql.jdbc.Driver"
  jdbc_dialect = "org.eclipse.persistence.platform.database.MySQLPlatform"
elif db_flavor == 'oracle':
  jdbc_jar_name = default("/hostLevelParams/custom_oracle_jdbc_name", None)
  colon_count = db_host.count(':')
  if colon_count == 2 or colon_count == 0:
    db_jdbc_url = format('jdbc:oracle:thin:@{db_host}')
  else:
    db_jdbc_url = format('jdbc:oracle:thin:@//{db_host}')
  db_jdbc_driver = "oracle.jdbc.OracleDriver"
  jdbc_dialect = "org.eclipse.persistence.platform.database.OraclePlatform"
elif db_flavor == 'postgres':
  jdbc_jar_name = default("/hostLevelParams/custom_postgres_jdbc_name", None)
  db_jdbc_url = format('jdbc:postgresql://{db_host}/{db_name}')
  db_jdbc_driver = "org.postgresql.Driver"
  jdbc_dialect = "org.eclipse.persistence.platform.database.PostgreSQLPlatform"
elif db_flavor == 'mssql':
  jdbc_jar_name = default("/hostLevelParams/custom_mssql_jdbc_name", None)
  db_jdbc_url = format('jdbc:sqlserver://{db_host};databaseName={db_name}')
  db_jdbc_driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
  jdbc_dialect = "org.eclipse.persistence.platform.database.SQLServerPlatform"
elif db_flavor == 'sqla':
  jdbc_jar_name = default("/hostLevelParams/custom_sqlanywhere_jdbc_name", None)
  db_jdbc_url = format('jdbc:sqlanywhere:database={db_name};host={db_host}')
  db_jdbc_driver = "sap.jdbc4.sqlanywhere.IDriver"
  jdbc_dialect = "org.eclipse.persistence.platform.database.SQLAnywherePlatform"

downloaded_custom_connector = format("{tmp_dir}/{jdbc_jar_name}")

driver_curl_source = format("{jdk_location}/{jdbc_jar_name}")
driver_curl_target = format("{kms_home}/ews/webapp/lib/{jdbc_jar_name}")
ews_lib_jar_path = format("{kms_home}/ews/webapp/lib/{jdbc_jar_name}")

if db_flavor == 'sqla':
  downloaded_custom_connector = format("{tmp_dir}/sqla-client-jdbc.tar.gz")
  jar_path_in_archive = format("{tmp_dir}/sqla-client-jdbc/java/sajdbc4.jar")
  libs_path_in_archive = format("{tmp_dir}/sqla-client-jdbc/native/lib64/*")
  jdbc_libs_dir = format("{kms_home}/native/lib64")
  ld_library_path = format("{jdbc_libs_dir}")

if has_ranger_admin:
  if stack_supports_ranger_audit_db:
    if xa_audit_db_flavor == 'mysql':
      jdbc_jar = default("/hostLevelParams/custom_mysql_jdbc_name", None)
      audit_jdbc_url = format('jdbc:mysql://{xa_db_host}/{xa_audit_db_name}')
      jdbc_driver = "com.mysql.jdbc.Driver"
    elif xa_audit_db_flavor == 'oracle':
      jdbc_jar = default("/hostLevelParams/custom_oracle_jdbc_name", None)
      colon_count = xa_db_host.count(':')
      if colon_count == 2 or colon_count == 0:
        audit_jdbc_url = format('jdbc:oracle:thin:@{xa_db_host}')
      else:
        audit_jdbc_url = format('jdbc:oracle:thin:@//{xa_db_host}')
      jdbc_driver = "oracle.jdbc.OracleDriver"
    elif xa_audit_db_flavor == 'postgres':
      jdbc_jar = default("/hostLevelParams/custom_postgres_jdbc_name", None)
      audit_jdbc_url = format('jdbc:postgresql://{xa_db_host}/{xa_audit_db_name}')
      jdbc_driver = "org.postgresql.Driver"
    elif xa_audit_db_flavor == 'mssql':
      jdbc_jar = default("/hostLevelParams/custom_mssql_jdbc_name", None)
      audit_jdbc_url = format('jdbc:sqlserver://{xa_db_host};databaseName={xa_audit_db_name}')
      jdbc_driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
    elif xa_audit_db_flavor == 'sqla':
      jdbc_jar = default("/hostLevelParams/custom_sqlanywhere_jdbc_name", None)
      audit_jdbc_url = format('jdbc:sqlanywhere:database={xa_audit_db_name};host={xa_db_host}')
      jdbc_driver = "sap.jdbc4.sqlanywhere.IDriver"

  downloaded_connector_path = format("{tmp_dir}/{jdbc_jar}") if stack_supports_ranger_audit_db else None
  driver_source = format("{jdk_location}/{jdbc_jar}") if stack_supports_ranger_audit_db else None
  driver_target = format("{kms_home}/ews/webapp/lib/{jdbc_jar}") if stack_supports_ranger_audit_db else None

repo_config_username = config['configurations']['kms-properties']['REPOSITORY_CONFIG_USERNAME']
repo_config_password = unicode(config['configurations']['kms-properties']['REPOSITORY_CONFIG_PASSWORD'])

kms_plugin_config = {
  'username' : repo_config_username,
  'password' : repo_config_password,
  'provider' : format('kms://http@{kms_host}:{kms_port}/kms') 
}

if stack_supports_ranger_kerberos:
  kms_plugin_config['policy.download.auth.users'] = 'keyadmin'

kms_ranger_plugin_repo = {
  'isEnabled' : 'true',
  'configs' : kms_plugin_config,
  'description' : 'kms repo',
  'name' : repo_name,
  'type' : 'kms'
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
rangerkms_keytab = config['configurations']['dbks-site']['ranger.ks.kerberos.keytab']
if stack_supports_ranger_kerberos and security_enabled:
  rangerkms_principal = default("/configurations/dbks-site/ranger.ks.kerberos.principal", None)
  if rangerkms_principal is not None:
    rangerkms_principal = rangerkms_principal.replace('_HOST', kms_host.lower())
