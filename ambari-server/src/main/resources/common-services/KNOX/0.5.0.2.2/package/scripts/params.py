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

Ambari Agent

"""

from resource_management.libraries.functions.version import format_hdp_stack_version, compare_versions
from resource_management.libraries.functions.default import default
from resource_management import *
import status_params
from ambari_commons import OSCheck

if OSCheck.is_windows_family():
  from params_windows import *
else:
  from params_linux import *

config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

stack_name = default("/hostLevelParams/stack_name", None)

version = default("/commandParams/version", None)

stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
hdp_stack_version = format_hdp_stack_version(stack_version_unformatted)

namenode_hosts = default("/clusterHostInfo/namenode_host", None)
if type(namenode_hosts) is list:
    namenode_host = namenode_hosts[0]
else:
    namenode_host = namenode_hosts

has_namenode = not namenode_host == None
namenode_http_port = "50070"
namenode_rpc_port = "8020"

if has_namenode:
    if 'dfs.namenode.http-address' in config['configurations']['hdfs-site']:
        namenode_http_port = get_port_from_url(config['configurations']['hdfs-site']['dfs.namenode.http-address'])
    if 'dfs.namenode.rpc-address' in config['configurations']['hdfs-site']:
        namenode_rpc_port = get_port_from_url(config['configurations']['hdfs-site']['dfs.namenode.rpc-address'])

rm_hosts = default("/clusterHostInfo/rm_host", None)
if type(rm_hosts) is list:
    rm_host = rm_hosts[0]
else:
    rm_host = rm_hosts
has_rm = not rm_host == None

jt_rpc_port = "8050"
rm_port = "8080"

if has_rm:
    if 'yarn.resourcemanager.address' in config['configurations']['yarn-site']:
        jt_rpc_port = get_port_from_url(config['configurations']['yarn-site']['yarn.resourcemanager.address'])

    if 'yarn.resourcemanager.webapp.address' in config['configurations']['yarn-site']:
        rm_port = get_port_from_url(config['configurations']['yarn-site']['yarn.resourcemanager.webapp.address'])

hive_http_port = default('/configurations/hive-site/hive.server2.thrift.http.port', "10001")
hive_http_path = default('/configurations/hive-site/hive.server2.thrift.http.path', "cliservice")
hive_server_hosts = default("/clusterHostInfo/hive_server_host", None)
if type(hive_server_hosts) is list:
    hive_server_host = hive_server_hosts[0]
else:
    hive_server_host = hive_server_hosts

templeton_port = default('/configurations/webhcat-site/templeton.port', "50111")
webhcat_server_hosts = default("/clusterHostInfo/webhcat_server_host", None)
if type(webhcat_server_hosts) is list:
    webhcat_server_host = webhcat_server_hosts[0]
else:
    webhcat_server_host = webhcat_server_hosts

hbase_master_port = default('/configurations/hbase-site/hbase.rest.port', "8080")
hbase_master_hosts = default("/clusterHostInfo/hbase_master_hosts", None)
if type(hbase_master_hosts) is list:
    hbase_master_host = hbase_master_hosts[0]
else:
    hbase_master_host = hbase_master_hosts

oozie_server_hosts = default("/clusterHostInfo/oozie_server", None)
if type(oozie_server_hosts) is list:
    oozie_server_host = oozie_server_hosts[0]
else:
    oozie_server_host = oozie_server_hosts

has_oozie = not oozie_server_host == None
oozie_server_port = "11000"

if has_oozie:
    if 'oozie.base.url' in config['configurations']['oozie-site']:
        oozie_server_port = get_port_from_url(config['configurations']['oozie-site']['oozie.base.url'])


# server configurations
knox_pid_dir = status_params.knox_pid_dir
knox_pid_file = status_params.knox_pid_file
ldap_pid_file = status_params.ldap_pid_file
knox_master_secret = config['configurations']['knox-env']['knox_master_secret']
knox_host_name = config['clusterHostInfo']['knox_gateway_hosts'][0]
knox_host_name_in_cluster = config['hostname']
knox_host_port = config['configurations']['gateway-site']['gateway.port']
topology_template = config['configurations']['topology']['content']
gateway_log4j = config['configurations']['gateway-log4j']['content']
ldap_log4j = config['configurations']['ldap-log4j']['content']
users_ldif = config['configurations']['users-ldif']['content']
java_home = config['hostLevelParams']['java_home']
security_enabled = config['configurations']['cluster-env']['security_enabled']
smokeuser = config['configurations']['cluster-env']['smokeuser']
smokeuser_principal = config['configurations']['cluster-env']['smokeuser_principal_name']
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
kinit_path_local = functions.get_kinit_path()
if security_enabled:
  knox_keytab_path = config['configurations']['knox-env']['knox_keytab_path']
  _hostname_lowercase = config['hostname'].lower()
  knox_principal_name = config['configurations']['knox-env']['knox_principal_name'].replace('_HOST',_hostname_lowercase)

# ranger host
ranger_admin_hosts = default("/clusterHostInfo/ranger_admin_hosts", [])
has_ranger_admin = not len(ranger_admin_hosts) == 0

if hdp_stack_version != "" and compare_versions(hdp_stack_version, '2.2') >= 0:
  # Setting Flag value for ranger hbase plugin
  enable_ranger_knox = False
  ranger_plugin_enable = default("/configurations/ranger-knox-plugin-properties/ranger-knox-plugin-enabled", "no")
  if ranger_plugin_enable.lower() == 'yes':
    enable_ranger_knox = True
  elif ranger_plugin_enable.lower() == 'no':
    enable_ranger_knox = False

ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]

# ranger knox properties
policymgr_mgr_url = default("/configurations/admin-properties/policymgr_external_url", "http://localhost:6080")
sql_connector_jar = default("/configurations/admin-properties/SQL_CONNECTOR_JAR", "/usr/share/java/mysql-connector-java.jar")
xa_audit_db_flavor = default("/configurations/admin-properties/DB_FLAVOR", "MYSQL")
xa_audit_db_name = default("/configurations/admin-properties/audit_db_name", "ranger_audit")
xa_audit_db_user = default("/configurations/admin-properties/audit_db_user", "rangerlogger")
xa_audit_db_password = default("/configurations/admin-properties/audit_db_password", "rangerlogger")
xa_db_host = default("/configurations/admin-properties/db_host", "localhost")
repo_name = str(config['clusterName']) + '_knox'
db_enabled = default("/configurations/ranger-knox-plugin-properties/XAAUDIT.DB.IS_ENABLED", "false")
hdfs_enabled = default("/configurations/ranger-knox-plugin-properties/XAAUDIT.HDFS.IS_ENABLED", "false")
hdfs_dest_dir = default("/configurations/ranger-knox-plugin-properties/XAAUDIT.HDFS.DESTINATION_DIRECTORY", "hdfs://__REPLACE__NAME_NODE_HOST:8020/ranger/audit/app-type/time:yyyyMMdd")
hdfs_buffer_dir = default("/configurations/ranger-knox-plugin-properties/XAAUDIT.HDFS.LOCAL_BUFFER_DIRECTORY", "__REPLACE__LOG_DIR/hadoop/app-type/audit")
hdfs_archive_dir = default("/configurations/ranger-knox-plugin-properties/XAAUDIT.HDFS.LOCAL_ARCHIVE_DIRECTORY", "__REPLACE__LOG_DIR/hadoop/app-type/audit/archive")
hdfs_dest_file = default("/configurations/ranger-knox-plugin-properties/XAAUDIT.HDFS.DESTINTATION_FILE", "hostname-audit.log")
hdfs_dest_flush_int_sec = default("/configurations/ranger-knox-plugin-properties/XAAUDIT.HDFS.DESTINTATION_FLUSH_INTERVAL_SECONDS", "900")
hdfs_dest_rollover_int_sec = default("/configurations/ranger-knox-plugin-properties/XAAUDIT.HDFS.DESTINTATION_ROLLOVER_INTERVAL_SECONDS", "86400")
hdfs_dest_open_retry_int_sec = default("/configurations/ranger-knox-plugin-properties/XAAUDIT.HDFS.DESTINTATION_OPEN_RETRY_INTERVAL_SECONDS", "60")
hdfs_buffer_file = default("/configurations/ranger-knox-plugin-properties/XAAUDIT.HDFS.LOCAL_BUFFER_FILE", "time:yyyyMMdd-HHmm.ss.log")
hdfs_buffer_flush_int_sec = default("/configurations/ranger-knox-plugin-properties/XAAUDIT.HDFS.LOCAL_BUFFER_FLUSH_INTERVAL_SECONDS", "60")
hdfs_buffer_rollover_int_sec = default("/configurations/ranger-knox-plugin-properties/XAAUDIT.HDFS.LOCAL_BUFFER_ROLLOVER_INTERVAL_SECONDS", "600")
hdfs_archive_max_file_count = default("/configurations/ranger-knox-plugin-properties/XAAUDIT.HDFS.LOCAL_ARCHIVE_MAX_FILE_COUNT", "10")
ssl_keystore_file = default("/configurations/ranger-knox-plugin-properties/SSL_KEYSTORE_FILE_PATH", "/etc/hadoop/conf/ranger-plugin-keystore.jks")
ssl_keystore_password = default("/configurations/ranger-knox-plugin-properties/SSL_KEYSTORE_PASSWORD", "myKeyFilePassword")
ssl_truststore_file = default("/configurations/ranger-knox-plugin-properties/SSL_TRUSTSTORE_FILE_PATH", "/etc/hadoop/conf/ranger-plugin-truststore.jks")
ssl_truststore_password = default("/configurations/ranger-knox-plugin-properties/SSL_TRUSTSTORE_PASSWORD", "changeit")

knox_home = default("/configurations/ranger-knox-plugin-properties/KNOX_HOME", "/usr/hdp/current/knox-server")
common_name_for_certificate = default("/configurations/ranger-knox-plugin-properties/common.name.for.certificate", "-")

repo_config_username = default("/configurations/ranger-knox-plugin-properties/REPOSITORY_CONFIG_USERNAME", "hbase")
repo_config_password = default("/configurations/ranger-knox-plugin-properties/REPOSITORY_CONFIG_PASSWORD", "hbase")

admin_uname = default("/configurations/ranger-env/admin_username", "admin")
admin_password = default("/configurations/ranger-env/admin_password", "admin")
admin_uname_password = format("{admin_uname}:{admin_password}")

ambari_ranger_admin = default("/configurations/ranger-env/ranger_admin_username", "amb_ranger_admin")
ambari_ranger_password = default("/configurations/ranger-env/ranger_admin_password", "ambari123")
policy_user = default("/configurations/ranger-knox-plugin-properties/policy_user", "ambari-qa")

#For curl command in ranger plugin to get db connector
jdk_location = config['hostLevelParams']['jdk_location']
java_share_dir = '/usr/share/java'
if xa_audit_db_flavor and xa_audit_db_flavor.lower() == 'mysql':
  jdbc_symlink_name = "mysql-jdbc-driver.jar"
  jdbc_jar_name = "mysql-connector-java.jar"
elif xa_audit_db_flavor and xa_audit_db_flavor.lower() == 'oracle':
  jdbc_jar_name = "ojdbc6.jar"
  jdbc_symlink_name = "oracle-jdbc-driver.jar"

downloaded_custom_connector = format("{tmp_dir}/{jdbc_jar_name}")

driver_curl_source = format("{jdk_location}/{jdbc_symlink_name}")
driver_curl_target = format("{java_share_dir}/{jdbc_jar_name}")
