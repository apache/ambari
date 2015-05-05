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
  kms_config_dir = '/usr/hdp/current/ranger-kms/ews/webapp/config'
  

java_home = config['hostLevelParams']['java_home']
kms_user  = default("/configurations/kms-env/kms_user", "kms")
kms_group = default("/configurations/kms-env/kms_group", "kms")

jdk_location = config['hostLevelParams']['jdk_location']
kms_log4j = config['configurations']['kms-log4j']['content']

# ranger host
ranger_admin_hosts = default("/clusterHostInfo/ranger_admin_hosts", [])
has_ranger_admin = len(ranger_admin_hosts) > 0

#kms properties
db_flavor = default("/configurations/kms-properties/DB_FLAVOR", "MYSQL")
sql_command_invoker = default("/configurations/kms-properties/SQL_COMMAND_INVOKER", "mysql")
sql_connector_jar = default("/configurations/kms-properties/SQL_CONNECTOR_JAR", "/usr/share/java/mysql-connector-java.jar")
db_root_user = default("/configurations/kms-properties/db_root_user", "root")
db_root_password = unicode(default("/configurations/kms-properties/db_root_password", " "))
db_host = default("/configurations/kms-properties/db_host", "localhost")
db_name = default("/configurations/kms-properties/db_name", "ranger")
db_user = default("/configurations/kms-properties/db_user", "rangeradmin")
db_password = unicode(default("/configurations/kms-properties/db_password", "rangeradmin"))
kms_master_key_password = default("/configurations/kms-properties/KMS_MASTER_KEY_PASSWD", "Str0ngPassw0rd")
policymgr_mgr_url = default("/configurations/kms-properties/POLICY_MGR_URL", "http://localhost:6080")
repo_name = default("/configurations/kms-properties/REPOSITORY_NAME", "kms_repo")
xa_audit_db_flavor = default("/configurations/kms-properties/XAAUDIT.DB.FLAVOUR", "MYSQL")
xa_audit_db_name = default("/configurations/kms-properties/XAAUDIT.DB.DATABASE_NAME", "ranger_audit")
xa_audit_db_user = default("/configurations/kms-properties/XAAUDIT.DB.USER_NAME", "rangerlogger")
xa_audit_db_password = default("/configurations/kms-properties/XAAUDIT.DB.PASSWORD", "rangerlogger")
xa_db_host = default("/configurations/kms-properties/XAAUDIT.DB.HOSTNAME", "localhost")
db_enabled = default("/configurations/kms-properties/XAAUDIT.DB.IS_ENABLED", "false")
hdfs_enabled = default("/configurations/kms-properties/XAAUDIT.HDFS.IS_ENABLED", "false")
hdfs_dest_dir = default("/configurations/kms-properties/XAAUDIT.HDFS.DESTINATION_DIRECTORY", "hdfs://__REPLACE__NAME_NODE_HOST:8020/ranger/audit/app-type/time:yyyyMMdd")
hdfs_buffer_dir = default("/configurations/kms-properties/XAAUDIT.HDFS.LOCAL_BUFFER_DIRECTORY", "__REPLACE__LOG_DIR/hadoop/app-type/audit")
hdfs_archive_dir = default("/configurations/kms-properties/XAAUDIT.HDFS.LOCAL_ARCHIVE_DIRECTORY", "__REPLACE__LOG_DIR/hadoop/app-type/audit/archive")
hdfs_dest_file = default("/configurations/kms-properties/XAAUDIT.HDFS.DESTINTATION_FILE", "hostname-audit.log")
hdfs_dest_flush_int_sec = default("/configurations/kms-properties/XAAUDIT.HDFS.DESTINTATION_FLUSH_INTERVAL_SECONDS", "900")
hdfs_dest_rollover_int_sec = default("/configurations/kms-properties/XAAUDIT.HDFS.DESTINTATION_ROLLOVER_INTERVAL_SECONDS", "86400")
hdfs_dest_open_retry_int_sec = default("/configurations/kms-properties/XAAUDIT.HDFS.DESTINTATION_OPEN_RETRY_INTERVAL_SECONDS", "60")
hdfs_buffer_file = default("/configurations/kms-properties/XAAUDIT.HDFS.LOCAL_BUFFER_FILE", "time:yyyyMMdd-HHmm.ss.log")
hdfs_buffer_flush_int_sec = default("/configurations/kms-properties/XAAUDIT.HDFS.LOCAL_BUFFER_FLUSH_INTERVAL_SECONDS", "60")
hdfs_buffer_rollover_int_sec = default("/configurations/kms-properties/XAAUDIT.HDFS.LOCAL_BUFFER_ROLLOVER_INTERVAL_SECONDS", "600")
hdfs_archive_max_file_count = default("/configurations/kms-properties/XAAUDIT.HDFS.LOCAL_ARCHIVE_MAX_FILE_COUNT", "10")
ssl_keystore_file = default("/configurations/kms-properties/SSL_KEYSTORE_FILE_PATH", "/etc/hadoop/conf/ranger-plugin-keystore.jks")
ssl_keystore_password = default("/configurations/kms-properties/SSL_KEYSTORE_PASSWORD", "myKeyFilePassword")
ssl_truststore_file = default("/configurations/kms-properties/SSL_TRUSTSTORE_FILE_PATH", "/etc/hadoop/conf/ranger-plugin-truststore.jks")
ssl_truststore_password = default("/configurations/kms-properties/SSL_TRUSTSTORE_PASSWORD", "changeit")
solr_enabled = default("/configurations/kms-properties/XAAUDIT.SOLR.IS_ENABLED", "false")
solr_max_queue_size = default("/configurations/kms-properties/XAAUDIT.SOLR.MAX_QUEUE_SIZE", "1")
solr_max_flush_interval = default("/configurations/kms-properties/XAAUDIT.SOLR.MAX_FLUSH_INTERVAL_MS", "1000")
solr_url = default("/configurations/kms-properties/XAAUDIT.SOLR.SOLR_URL", "http://localhost:6083/solr/ranger_audits")

repo_config_username = default("/configurations/kms-properties/REPOSITORY_CONFIG_USERNAME", "kms")
repo_config_password = default("/configurations/kms-properties/REPOSITORY_CONFIG_PASSWORD", "kms")

kms_host_name = config['clusterHostInfo']['ranger_kms_server_hosts'][0]

admin_uname = default("/configurations/ranger-env/admin_username", "admin")
admin_password = default("/configurations/ranger-env/admin_password", "admin")
admin_uname_password = format("{admin_uname}:{admin_password}")

ambari_ranger_admin = default("/configurations/ranger-env/ranger_admin_username", "amb_ranger_admin")
ambari_ranger_password = default("/configurations/ranger-env/ranger_admin_password", "ambari123")

java_share_dir = '/usr/share/java'
if db_flavor and db_flavor.lower() == 'mysql':
  jdbc_symlink_name = "mysql-jdbc-driver.jar"
  jdbc_jar_name = "mysql-connector-java.jar"

downloaded_custom_connector = format("{tmp_dir}/{jdbc_jar_name}")

driver_curl_source = format("{jdk_location}/{jdbc_symlink_name}")
driver_curl_target = format("{java_share_dir}/{jdbc_jar_name}")
