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
from ambari_commons.constants import AMBARI_SUDO_BINARY
from resource_management.libraries.functions.version import format_hdp_stack_version, compare_versions
from resource_management.libraries.functions.default import default
from resource_management.libraries.script import Script
from resource_management.libraries.functions import default, format
import status_params
import re

def get_bare_principal(normalized_principal_name):
  """
  Given a normalized principal name (nimbus/c6501.ambari.apache.org@EXAMPLE.COM) returns just the
  primary component (nimbus)
  :param normalized_principal_name: a string containing the principal name to process
  :return: a string containing the primary component value or None if not valid
  """

  bare_principal = None

  if normalized_principal_name:
    match = re.match(r"([^/@]+)(?:/[^@])?(?:@.*)?", normalized_principal_name)

    if match:
      bare_principal = match.group(1)

  return bare_principal


# server configurations
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()
sudo = AMBARI_SUDO_BINARY

stack_name = default("/hostLevelParams/stack_name", None)

version = default("/commandParams/version", None)

stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
hdp_stack_version = format_hdp_stack_version(stack_version_unformatted)
stack_is_hdp22_or_further = hdp_stack_version != "" and compare_versions(hdp_stack_version, '2.2') >= 0

#hadoop params
if hdp_stack_version != "" and compare_versions(hdp_stack_version, '2.2') >= 0:
  rest_lib_dir = '/usr/hdp/current/storm-client/contrib/storm-rest'
  storm_bin_dir = "/usr/hdp/current/storm-client/bin"
  storm_lib_dir = "/usr/hdp/current/storm-client/lib"
else:
  rest_lib_dir = "/usr/lib/storm/contrib/storm-rest"
  storm_bin_dir = "/usr/bin"
  storm_lib_dir = "/usr/lib/storm/lib/"

storm_user = config['configurations']['storm-env']['storm_user']
log_dir = config['configurations']['storm-env']['storm_log_dir']
pid_dir = status_params.pid_dir
conf_dir = "/etc/storm/conf"
local_dir = config['configurations']['storm-site']['storm.local.dir']
user_group = config['configurations']['cluster-env']['user_group']
java64_home = config['hostLevelParams']['java_home']
jps_binary = format("{java64_home}/bin/jps")
nimbus_port = config['configurations']['storm-site']['nimbus.thrift.port']
nimbus_host = config['configurations']['storm-site']['nimbus.host']
rest_api_port = "8745"
rest_api_admin_port = "8746"
rest_api_conf_file = format("{conf_dir}/config.yaml")
storm_env_sh_template = config['configurations']['storm-env']['content']

if 'ganglia_server_host' in config['clusterHostInfo'] and \
    len(config['clusterHostInfo']['ganglia_server_host'])>0:
  ganglia_installed = True
  ganglia_server = config['clusterHostInfo']['ganglia_server_host'][0]
  ganglia_report_interval = 60
else:
  ganglia_installed = False

security_enabled = config['configurations']['cluster-env']['security_enabled']

if security_enabled:
  _hostname_lowercase = config['hostname'].lower()
  _storm_principal_name = config['configurations']['storm-env']['storm_principal_name']
  storm_jaas_principal = _storm_principal_name.replace('_HOST',_hostname_lowercase)
  storm_keytab_path = config['configurations']['storm-env']['storm_keytab']

  if hdp_stack_version != "" and compare_versions(hdp_stack_version, '2.2') >= 0:
    storm_ui_keytab_path = config['configurations']['storm-env']['storm_ui_keytab']
    _storm_ui_jaas_principal_name = config['configurations']['storm-env']['storm_ui_principal_name']
    storm_ui_host = default("/clusterHostInfo/storm_ui_server_hosts", [])
    storm_ui_jaas_principal = _storm_ui_jaas_principal_name.replace('_HOST',storm_ui_host[0].lower())
    
    storm_bare_jaas_principal = get_bare_principal(_storm_principal_name)

    _nimbus_principal_name = config['configurations']['storm-env']['nimbus_principal_name']
    nimbus_jaas_principal = _nimbus_principal_name.replace('_HOST', _hostname_lowercase)
    nimbus_bare_jaas_principal = get_bare_principal(_nimbus_principal_name)
    nimbus_keytab_path = config['configurations']['storm-env']['nimbus_keytab']

ams_collector_hosts = default("/clusterHostInfo/metrics_collector_hosts", [])
has_metric_collector = not len(ams_collector_hosts) == 0
if has_metric_collector:
  metric_collector_host = ams_collector_hosts[0]
  metric_collector_report_interval = 60
  metric_collector_app_id = "nimbus"
metric_collector_sink_jar = "/usr/lib/storm/lib/ambari-metrics-storm-sink*.jar"

# ranger host
ranger_admin_hosts = default("/clusterHostInfo/ranger_admin_hosts", [])
has_ranger_admin = not len(ranger_admin_hosts) == 0

if hdp_stack_version != "" and compare_versions(hdp_stack_version, '2.2') >= 0:
  # setting flag value for ranger hive plugin
  enable_ranger_storm = False
  ranger_plugin_enable = default("/configurations/ranger-storm-plugin-properties/ranger-storm-plugin-enabled", "no")
  if ranger_plugin_enable.lower() == 'yes':
    enable_ranger_storm = True
  elif ranger_plugin_enable.lower() == 'no':
    enable_ranger_storm = False

ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]

#ranger storm properties
policymgr_mgr_url = default("/configurations/admin-properties/policymgr_external_url", "http://localhost:6080")
sql_connector_jar = default("/configurations/admin-properties/SQL_CONNECTOR_JAR", "/usr/share/java/mysql-connector-java.jar")
xa_audit_db_flavor = default("/configurations/admin-properties/DB_FLAVOR", "MYSQL")
xa_audit_db_name = default("/configurations/admin-properties/audit_db_name", "ranger_audit")
xa_audit_db_user = default("/configurations/admin-properties/audit_db_user", "rangerlogger")
xa_audit_db_password = default("/configurations/admin-properties/audit_db_password", "rangerlogger")
xa_db_host = default("/configurations/admin-properties/db_host", "localhost")
repo_name = str(config['clusterName']) + '_storm'
db_enabled = default("/configurations/ranger-storm-plugin-properties/XAAUDIT.DB.IS_ENABLED", "false")
hdfs_enabled = default("/configurations/ranger-storm-plugin-properties/XAAUDIT.HDFS.IS_ENABLED", "false")
hdfs_dest_dir = default("/configurations/ranger-storm-plugin-properties/XAAUDIT.HDFS.DESTINATION_DIRECTORY", "hdfs://__REPLACE__NAME_NODE_HOST:8020/ranger/audit/app-type/time:yyyyMMdd")
hdfs_buffer_dir = default("/configurations/ranger-storm-plugin-properties/XAAUDIT.HDFS.LOCAL_BUFFER_DIRECTORY", "__REPLACE__LOG_DIR/hadoop/app-type/audit")
hdfs_archive_dir = default("/configurations/ranger-storm-plugin-properties/XAAUDIT.HDFS.LOCAL_ARCHIVE_DIRECTORY", "__REPLACE__LOG_DIR/hadoop/app-type/audit/archive")
hdfs_dest_file = default("/configurations/ranger-storm-plugin-properties/XAAUDIT.HDFS.DESTINTATION_FILE", "hostname-audit.log")
hdfs_dest_flush_int_sec = default("/configurations/ranger-storm-plugin-properties/XAAUDIT.HDFS.DESTINTATION_FLUSH_INTERVAL_SECONDS", "900")
hdfs_dest_rollover_int_sec = default("/configurations/ranger-storm-plugin-properties/XAAUDIT.HDFS.DESTINTATION_ROLLOVER_INTERVAL_SECONDS", "86400")
hdfs_dest_open_retry_int_sec = default("/configurations/ranger-storm-plugin-properties/XAAUDIT.HDFS.DESTINTATION_OPEN_RETRY_INTERVAL_SECONDS", "60")
hdfs_buffer_file = default("/configurations/ranger-storm-plugin-properties/XAAUDIT.HDFS.LOCAL_BUFFER_FILE", "time:yyyyMMdd-HHmm.ss.log")
hdfs_buffer_flush_int_sec = default("/configurations/ranger-storm-plugin-properties/XAAUDIT.HDFS.LOCAL_BUFFER_FLUSH_INTERVAL_SECONDS", "60")
hdfs_buffer_rollover_int_sec = default("/configurations/ranger-storm-plugin-properties/XAAUDIT.HDFS.LOCAL_BUFFER_ROLLOVER_INTERVAL_SECONDS", "600")
hdfs_archive_max_file_count = default("/configurations/ranger-storm-plugin-properties/XAAUDIT.HDFS.LOCAL_ARCHIVE_MAX_FILE_COUNT", "10")
ssl_keystore_file = default("/configurations/ranger-storm-plugin-properties/SSL_KEYSTORE_FILE_PATH", "/etc/hadoop/conf/ranger-plugin-keystore.jks")
ssl_keystore_password = default("/configurations/ranger-storm-plugin-properties/SSL_KEYSTORE_PASSWORD", "myKeyFilePassword")
ssl_truststore_file = default("/configurations/ranger-storm-plugin-properties/SSL_TRUSTSTORE_FILE_PATH", "/etc/hadoop/conf/ranger-plugin-truststore.jks")
ssl_truststore_password = default("/configurations/ranger-storm-plugin-properties/SSL_TRUSTSTORE_PASSWORD", "changeit")

common_name_for_certificate = default("/configurations/ranger-storm-plugin-properties/common.name.for.certificate", "-")

repo_config_username = default("/configurations/ranger-storm-plugin-properties/REPOSITORY_CONFIG_USERNAME", "hadoop")
repo_config_password = default("/configurations/ranger-storm-plugin-properties/REPOSITORY_CONFIG_PASSWORD", "hadoop")
storm_ui_port = config['configurations']['storm-site']['ui.port']

admin_uname = default("/configurations/ranger-env/admin_username", "admin")
admin_password = default("/configurations/ranger-env/admin_password", "admin")
admin_uname_password = format("{admin_uname}:{admin_password}")

ambari_ranger_admin = default("/configurations/ranger-env/ranger_admin_username", "amb_ranger_admin")
ambari_ranger_password = default("/configurations/ranger-env/ranger_admin_password", "ambari123")
policy_user = default("/configurations/ranger-storm-plugin-properties/policy_user", "storm")

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