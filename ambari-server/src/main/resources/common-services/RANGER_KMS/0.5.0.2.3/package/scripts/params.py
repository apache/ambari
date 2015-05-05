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
from resource_management.libraries.functions.version import format_hdp_stack_version
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.default import default

config  = Script.get_config()
tmp_dir = Script.get_tmp_dir()

stack_name = default("/hostLevelParams/stack_name", None)
version = default("/commandParams/version", None)

stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
hdp_stack_version = format_hdp_stack_version(stack_version_unformatted)

stack_is_hdp23_or_further = Script.is_hdp_stack_greater_or_equal("2.3")

if stack_is_hdp23_or_further:
  kms_home = '/usr/hdp/current/ranger-kms'
  kms_conf_dir = '/etc/ranger/kms/conf'
  

java_home = config['hostLevelParams']['java_home']
kms_user  = default("/configurations/kms-env/kms_user", "kms")
kms_group = default("/configurations/kms-env/kms_group", "kms")

jdk_location = config['hostLevelParams']['jdk_location']
kms_log4j = config['configurations']['kms-log4j']['content']

# ranger host
ranger_admin_hosts = config['clusterHostInfo']['ranger_admin_hosts'][0]
has_ranger_admin = len(ranger_admin_hosts) > 0
kms_host = config['clusterHostInfo']['ranger_kms_server_hosts'][0]
kms_port = config['configurations']['kms-env']['kms_port']

#kms properties
policymgr_mgr_url = format('http://{ranger_admin_hosts}:6080')
sql_connector_jar = config['configurations']['kms-properties']['SQL_CONNECTOR_JAR']
db_flavor = config['configurations']['kms-properties']['DB_FLAVOR']
xa_audit_db_flavor = config['configurations']['admin-properties']['DB_FLAVOR']
xa_audit_db_name = config['configurations']['admin-properties']['audit_db_name']
xa_audit_db_user = config['configurations']['admin-properties']['audit_db_user']
xa_audit_db_password = config['configurations']['admin-properties']['audit_db_password']
xa_db_host = config['configurations']['admin-properties']['db_host']
repo_name = str(config['clusterName']) + '_kms'

repo_config_username = config['configurations']['kms-properties']['REPOSITORY_CONFIG_USERNAME']
repo_config_password = config['configurations']['kms-properties']['REPOSITORY_CONFIG_PASSWORD']

admin_uname = config['configurations']['ranger-env']['admin_username']
admin_password = config['configurations']['ranger-env']['admin_password']

ambari_ranger_admin = config['configurations']['ranger-env']['ranger_admin_username']
ambari_ranger_password = config['configurations']['ranger-env']['ranger_admin_password']

admin_uname_password = format("{admin_uname}:{admin_password}")

java_share_dir = '/usr/share/java'
if has_ranger_admin:
  if db_flavor.lower() == 'mysql':
    jdbc_symlink_name = "mysql-jdbc-driver.jar"
    jdbc_jar_name = "mysql-connector-java.jar"
  elif db_flavor.lower() == 'oracle':
    jdbc_jar_name = "ojdbc6.jar"
    jdbc_symlink_name = "oracle-jdbc-driver.jar"
  elif db_flavor.lower() == 'postgres':
    jdbc_jar_name = "postgresql.jar"
    jdbc_symlink_name = "postgres-jdbc-driver.jar"
  elif db_flavor.lower() == 'sqlserver':
    jdbc_jar_name = "sqljdbc4.jar"
    jdbc_symlink_name = "mssql-jdbc-driver.jar"   

downloaded_custom_connector = format("{tmp_dir}/{jdbc_jar_name}")

driver_curl_source = format("{jdk_location}/{jdbc_symlink_name}")
driver_curl_target = format("{java_share_dir}/{jdbc_jar_name}")

if has_ranger_admin:
  if xa_audit_db_flavor.lower() == 'mysql':
    jdbc_symlink = "mysql-jdbc-driver.jar"
    jdbc_jar = "mysql-connector-java.jar"
  elif xa_audit_db_flavor.lower() == 'oracle':
    jdbc_jar = "ojdbc6.jar"
    jdbc_symlink = "oracle-jdbc-driver.jar"
  elif xa_audit_db_flavor.lower() == 'postgres':
    jdbc_jar = "postgresql.jar"
    jdbc_symlink = "postgres-jdbc-driver.jar"
  elif xa_audit_db_flavor.lower() == 'sqlserver':
    jdbc_jar = "sqljdbc4.jar"
    jdbc_symlink = "mssql-jdbc-driver.jar"

downloaded_connector_path = format("{tmp_dir}/{jdbc_jar}")

driver_source = format("{jdk_location}/{jdbc_symlink}")
driver_target = format("{java_share_dir}/{jdbc_jar}")    

kms_plugin_config = {
  'username' : repo_config_username,
  'password' : repo_config_password,
  'provider' : format('kms://http@{kms_host}:{kms_port}/kms') 
}

kms_ranger_plugin_repo = {
  'isEnabled' : 'true',
  'configs' : kms_plugin_config,
  'description' : 'kms repo',
  'name' : repo_name,
  'type' : 'kms'
}
