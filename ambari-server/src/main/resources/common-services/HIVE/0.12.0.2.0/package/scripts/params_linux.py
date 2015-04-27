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
from ambari_commons.os_check import OSCheck
from resource_management.libraries.functions.version import format_hdp_stack_version, compare_versions
from resource_management.libraries.functions.default import default
from resource_management import *
import status_params
import json
import os

# server configurations
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()
sudo = AMBARI_SUDO_BINARY

stack_name = default("/hostLevelParams/stack_name", None)

# node hostname
hostname = config["hostname"]

# This is expected to be of the form #.#.#.#
stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
hdp_stack_version = format_hdp_stack_version(stack_version_unformatted)
stack_is_hdp21 = hdp_stack_version != "" and compare_versions(hdp_stack_version, '2.1') >= 0 and compare_versions(hdp_stack_version, '2.2') < 0

# New Cluster Stack Version that is defined during the RESTART of a Rolling Upgrade
version = default("/commandParams/version", None)

# Hadoop params
# TODO, this logic should initialize these parameters in a file inside the HDP 2.2 stack.
if hdp_stack_version != "" and compare_versions(hdp_stack_version, '2.2') >=0:
  # start out with client libraries
  hadoop_bin_dir = "/usr/hdp/current/hadoop-client/bin"
  hadoop_home = '/usr/hdp/current/hadoop-client'
  hive_bin = '/usr/hdp/current/hive-client/bin'
  hive_lib = '/usr/hdp/current/hive-client/lib'

  # if this is a server action, then use the server binaries; smoke tests
  # use the client binaries
  command_role = default("/role", "")
  server_role_dir_mapping = { 'HIVE_SERVER' : 'hive-server2',
    'HIVE_METASTORE' : 'hive-metastore' }

  if command_role in server_role_dir_mapping:
    hive_server_root = server_role_dir_mapping[command_role]
    hive_bin = format('/usr/hdp/current/{hive_server_root}/bin')
    hive_lib = format('/usr/hdp/current/{hive_server_root}/lib')

  # there are no client versions of these, use server versions directly
  hcat_lib = '/usr/hdp/current/hive-webhcat/share/hcatalog'
  webhcat_bin_dir = '/usr/hdp/current/hive-webhcat/sbin'

  hive_specific_configs_supported = True
else:
  hadoop_bin_dir = "/usr/bin"
  hadoop_home = '/usr'
  hadoop_streeming_jars = '/usr/lib/hadoop-mapreduce/hadoop-streaming-*.jar'
  hive_bin = '/usr/lib/hive/bin'
  hive_lib = '/usr/lib/hive/lib/'
  pig_tar_file = '/usr/share/HDP-webhcat/pig.tar.gz'
  hive_tar_file = '/usr/share/HDP-webhcat/hive.tar.gz'
  sqoop_tar_file = '/usr/share/HDP-webhcat/sqoop*.tar.gz'

  if hdp_stack_version != "" and compare_versions(hdp_stack_version, "2.1.0.0") < 0:
    hcat_lib = '/usr/lib/hcatalog/share/hcatalog'
    webhcat_bin_dir = '/usr/lib/hcatalog/sbin'
  # for newer versions
  else:
    hcat_lib = '/usr/lib/hive-hcatalog/share/hcatalog'
    webhcat_bin_dir = '/usr/lib/hive-hcatalog/sbin'
    
  hive_specific_configs_supported = False

hadoop_conf_dir = "/etc/hadoop/conf"
hive_conf_dir_prefix = "/etc/hive"
hive_conf_dir = format("{hive_conf_dir_prefix}/conf")
hive_client_conf_dir = format("{hive_conf_dir_prefix}/conf")
hive_server_conf_dir = format("{hive_conf_dir_prefix}/conf.server")
limits_conf_dir = "/etc/security/limits.d"

if hdp_stack_version != "" and compare_versions(hdp_stack_version, "2.1.0.0") < 0:
  hcat_conf_dir = '/etc/hcatalog/conf'
  config_dir = '/etc/hcatalog/conf'
# for newer versions
else:
  hcat_conf_dir = '/etc/hive-hcatalog/conf'
  config_dir = '/etc/hive-webhcat/conf'

execute_path = os.environ['PATH'] + os.pathsep + hive_bin + os.pathsep + hadoop_bin_dir
hive_metastore_user_name = config['configurations']['hive-site']['javax.jdo.option.ConnectionUserName']
hive_jdbc_connection_url = config['configurations']['hive-site']['javax.jdo.option.ConnectionURL']

webhcat_conf_dir = status_params.webhcat_conf_dir
hive_metastore_user_passwd = config['configurations']['hive-site']['javax.jdo.option.ConnectionPassword']
hive_metastore_db_type = config['configurations']['hive-env']['hive_database_type']
#HACK Temporarily use dbType=azuredb while invoking schematool
if hive_metastore_db_type == "mssql":
  hive_metastore_db_type = "azuredb"

#users
hive_user = config['configurations']['hive-env']['hive_user']
#JDBC driver jar name
hive_jdbc_driver = config['configurations']['hive-site']['javax.jdo.option.ConnectionDriverName']
if hive_jdbc_driver == "com.microsoft.sqlserver.jdbc.SQLServerDriver":
  jdbc_jar_name = "sqljdbc4.jar"
  jdbc_symlink_name = "mssql-jdbc-driver.jar"
elif hive_jdbc_driver == "com.mysql.jdbc.Driver":
  jdbc_jar_name = "mysql-connector-java.jar"
  jdbc_symlink_name = "mysql-jdbc-driver.jar"
elif hive_jdbc_driver == "org.postgresql.Driver":
  jdbc_jar_name = "postgresql-jdbc.jar"
  jdbc_symlink_name = "postgres-jdbc-driver.jar"
elif hive_jdbc_driver == "oracle.jdbc.driver.OracleDriver":
  jdbc_jar_name = "ojdbc.jar"
  jdbc_symlink_name = "oracle-jdbc-driver.jar"

check_db_connection_jar_name = "DBConnectionVerification.jar"
check_db_connection_jar = format("/usr/lib/ambari-agent/{check_db_connection_jar_name}")
hive_jdbc_drivers_list = ["com.microsoft.sqlserver.jdbc.SQLServerDriver","com.mysql.jdbc.Driver","org.postgresql.Driver","oracle.jdbc.driver.OracleDriver"]
downloaded_custom_connector = format("{tmp_dir}/{jdbc_jar_name}")
prepackaged_ojdbc_symlink = format("{hive_lib}/ojdbc6.jar")
templeton_port = config['configurations']['webhcat-site']['templeton.port']


#common
hive_metastore_hosts = config['clusterHostInfo']['hive_metastore_host']
hive_metastore_host = hive_metastore_hosts[0]
hive_metastore_port = get_port_from_url(config['configurations']['hive-site']['hive.metastore.uris']) #"9083"
hive_var_lib = '/var/lib/hive'
ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]
hive_server_host = config['clusterHostInfo']['hive_server_host'][0]
hive_server_hosts = config['clusterHostInfo']['hive_server_host']
hive_transport_mode = config['configurations']['hive-site']['hive.server2.transport.mode']
if hive_transport_mode.lower() == "http":
  hive_server_port = config['configurations']['hive-site']['hive.server2.thrift.http.port']
else:
  hive_server_port = default('/configurations/hive-site/hive.server2.thrift.port',"10000")
hive_url = format("jdbc:hive2://{hive_server_host}:{hive_server_port}")
hive_server_principal = config['configurations']['hive-site']['hive.server2.authentication.kerberos.principal']
hive_server2_authentication = config['configurations']['hive-site']['hive.server2.authentication']

smokeuser = config['configurations']['cluster-env']['smokeuser']
smoke_test_sql = format("{tmp_dir}/hiveserver2.sql")
smoke_test_path = format("{tmp_dir}/hiveserver2Smoke.sh")
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
smokeuser_principal = config['configurations']['cluster-env']['smokeuser_principal_name']

fs_root = config['configurations']['core-site']['fs.defaultFS']
security_enabled = config['configurations']['cluster-env']['security_enabled']

kinit_path_local = functions.get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
hive_metastore_keytab_path =  config['configurations']['hive-site']['hive.metastore.kerberos.keytab.file']

hive_server2_keytab = config['configurations']['hive-site']['hive.server2.authentication.kerberos.keytab']

#hive_env
hive_dbroot = config['configurations']['hive-env']['hive_dbroot']
hive_log_dir = config['configurations']['hive-env']['hive_log_dir']
hive_pid_dir = status_params.hive_pid_dir
hive_pid = status_params.hive_pid
#Default conf dir for client
hive_conf_dirs_list = [hive_client_conf_dir]

if hostname in hive_metastore_hosts or hostname in hive_server_hosts:
  hive_conf_dirs_list.append(hive_server_conf_dir)

if 'role' in config and config['role'] in ["HIVE_SERVER", "HIVE_METASTORE"]:
  hive_config_dir = hive_server_conf_dir
else:
  hive_config_dir = hive_client_conf_dir

#hive-site
hive_database_name = config['configurations']['hive-env']['hive_database_name']
hive_database = config['configurations']['hive-env']['hive_database']

#Starting hiveserver2
start_hiveserver2_script = 'startHiveserver2.sh.j2'

##Starting metastore
start_metastore_script = 'startMetastore.sh'
hive_metastore_pid = status_params.hive_metastore_pid
java_share_dir = '/usr/share/java'
driver_curl_target = format("{java_share_dir}/{jdbc_jar_name}")

hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
user_group = config['configurations']['cluster-env']['user_group']
artifact_dir = format("{tmp_dir}/AMBARI-artifacts/")

target = format("{hive_lib}/{jdbc_jar_name}")

jdk_location = config['hostLevelParams']['jdk_location']
driver_curl_source = format("{jdk_location}/{jdbc_symlink_name}")

start_hiveserver2_path = format("{tmp_dir}/start_hiveserver2_script")
start_metastore_path = format("{tmp_dir}/start_metastore_script")

hadoop_heapsize = config['configurations']['hadoop-env']['hadoop_heapsize']

if 'role' in config and config['role'] in ["HIVE_SERVER", "HIVE_METASTORE"]:
  hive_heapsize = config['configurations']['hive-site']['hive.heapsize']
else:
  hive_heapsize = config['configurations']['hive-env']['hive.client.heapsize']

java64_home = config['hostLevelParams']['java_home']

##### MYSQL

db_name = config['configurations']['hive-env']['hive_database_name']
mysql_group = 'mysql'
mysql_host = config['clusterHostInfo']['hive_mysql_host']

mysql_adduser_path = format("{tmp_dir}/addMysqlUser.sh")
mysql_deluser_path = format("{tmp_dir}/removeMysqlUser.sh")

######## Metastore Schema
if hdp_stack_version != "" and compare_versions(hdp_stack_version, "2.1.0.0") < 0:
  init_metastore_schema = False
else:
  init_metastore_schema = True

########## HCAT

hcat_dbroot = hcat_lib

hcat_user = config['configurations']['hive-env']['hcat_user']
webhcat_user = config['configurations']['hive-env']['webhcat_user']

hcat_pid_dir = status_params.hcat_pid_dir
hcat_log_dir = config['configurations']['hive-env']['hcat_log_dir']
hcat_env_sh_template = config['configurations']['hcat-env']['content']

#hive-log4j.properties.template
if (('hive-log4j' in config['configurations']) and ('content' in config['configurations']['hive-log4j'])):
  log4j_props = config['configurations']['hive-log4j']['content']
else:
  log4j_props = None

#webhcat-log4j.properties.template
if (('webhcat-log4j' in config['configurations']) and ('content' in config['configurations']['webhcat-log4j'])):
  log4j_webhcat_props = config['configurations']['webhcat-log4j']['content']
else:
  log4j_webhcat_props = None

#hive-exec-log4j.properties.template
if (('hive-exec-log4j' in config['configurations']) and ('content' in config['configurations']['hive-exec-log4j'])):
  log4j_exec_props = config['configurations']['hive-exec-log4j']['content']
else:
  log4j_exec_props = None

daemon_name = status_params.daemon_name
process_name = status_params.process_name
hive_env_sh_template = config['configurations']['hive-env']['content']

hive_hdfs_user_dir = format("/user/{hive_user}")
hive_hdfs_user_mode = 0700
hive_apps_whs_dir = config['configurations']['hive-site']["hive.metastore.warehouse.dir"]
hive_exec_scratchdir = config['configurations']['hive-site']["hive.exec.scratchdir"]
#for create_hdfs_directory
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
hdfs_principal_name = default('/configurations/hadoop-env/hdfs_principal_name', 'missing_principal').replace("_HOST", hostname)

# Tez-related properties
tez_user = config['configurations']['tez-env']['tez_user']

# Tez jars
tez_local_api_jars = '/usr/lib/tez/tez*.jar'
tez_local_lib_jars = '/usr/lib/tez/lib/*.jar'
app_dir_files = {tez_local_api_jars:None}

# Tez libraries
tez_lib_uris = default("/configurations/tez-site/tez.lib.uris", None)

if OSCheck.is_ubuntu_family():
  mysql_configname = '/etc/mysql/my.cnf'
else:
  mysql_configname = '/etc/my.cnf'
  
mysql_user = 'mysql'

# Hive security
hive_authorization_enabled = config['configurations']['hive-site']['hive.security.authorization.enabled']

mysql_jdbc_driver_jar = "/usr/share/java/mysql-connector-java.jar"
hive_use_existing_db = hive_database.startswith('Existing')
hive_exclude_packages = []

# There are other packages that contain /usr/share/java/mysql-connector-java.jar (like libmysql-java),
# trying to install mysql-connector-java upon them can cause packages to conflict.
if hive_use_existing_db:
  hive_exclude_packages = ['mysql-connector-java', 'mysql', 'mysql-server']
else:
  if 'role' in config and config['role'] != "MYSQL_SERVER":
    hive_exclude_packages = ['mysql', 'mysql-server']
  if os.path.exists(mysql_jdbc_driver_jar):
    hive_exclude_packages.append('mysql-connector-java')

########################################################
########### WebHCat related params #####################
########################################################

webhcat_env_sh_template = config['configurations']['webhcat-env']['content']
templeton_log_dir = config['configurations']['hive-env']['hcat_log_dir']
templeton_pid_dir = status_params.hcat_pid_dir

webhcat_pid_file = status_params.webhcat_pid_file

templeton_jar = config['configurations']['webhcat-site']['templeton.jar']


webhcat_server_host = config['clusterHostInfo']['webhcat_server_host']

webhcat_apps_dir = "/apps/webhcat"

hcat_hdfs_user_dir = format("/user/{hcat_user}")
hcat_hdfs_user_mode = 0755
webhcat_hdfs_user_dir = format("/user/{webhcat_user}")
webhcat_hdfs_user_mode = 0755
#for create_hdfs_directory
security_param = "true" if security_enabled else "false"

import functools
#create partial functions with common arguments for every HdfsDirectory call
#to create hdfs directory we need to call params.HdfsDirectory in code
HdfsDirectory = functools.partial(
  HdfsDirectory,
  conf_dir = hadoop_conf_dir,
  hdfs_user = hdfs_user,
  security_enabled = security_enabled,
  keytab = hdfs_user_keytab,
  kinit_path_local = kinit_path_local,
  bin_dir = hadoop_bin_dir
)

# ranger host
ranger_admin_hosts = default("/clusterHostInfo/ranger_admin_hosts", [])
has_ranger_admin = not len(ranger_admin_hosts) == 0
if hdp_stack_version != "" and compare_versions(hdp_stack_version, '2.2') >=0:
  enable_ranger_hive = (config['configurations']['ranger-hive-plugin-properties']['ranger-hive-plugin-enabled'].lower() == 'yes')

#ranger hive properties
policymgr_mgr_url = config['configurations']['admin-properties']['policymgr_external_url']
sql_connector_jar = config['configurations']['admin-properties']['SQL_CONNECTOR_JAR']
xa_audit_db_flavor = config['configurations']['admin-properties']['DB_FLAVOR']
xa_audit_db_name = config['configurations']['admin-properties']['audit_db_name']
xa_audit_db_user = config['configurations']['admin-properties']['audit_db_user']
xa_audit_db_password = config['configurations']['admin-properties']['audit_db_password']
xa_db_host = config['configurations']['admin-properties']['db_host']
repo_name = str(config['clusterName']) + '_hive'

jdbc_driver_class_name = config['configurations']['ranger-hive-plugin-properties']['jdbc.driverClassName']
common_name_for_certificate = config['configurations']['ranger-hive-plugin-properties']['common.name.for.certificate']

repo_config_username = config['configurations']['ranger-hive-plugin-properties']['REPOSITORY_CONFIG_USERNAME']
repo_config_password = config['configurations']['ranger-hive-plugin-properties']['REPOSITORY_CONFIG_PASSWORD']

ranger_env = config['configurations']['ranger-env']
ranger_plugin_properties = config['configurations']['ranger-hive-plugin-properties']
policy_user = config['configurations']['ranger-hive-plugin-properties']['policy_user']

#For curl command in ranger plugin to get db connector
if has_ranger_admin:
  if xa_audit_db_flavor and xa_audit_db_flavor.lower() == 'mysql':
    ranger_jdbc_symlink_name = "mysql-jdbc-driver.jar"
    ranger_jdbc_jar_name = "mysql-connector-java.jar"
  elif xa_audit_db_flavor and xa_audit_db_flavor.lower() == 'oracle':
    ranger_jdbc_jar_name = "ojdbc6.jar"
    ranger_jdbc_symlink_name = "oracle-jdbc-driver.jar"
  elif xa_audit_db_flavor and xa_audit_db_flavor.lower() == 'postgres':
    ranger_jdbc_jar_name = "postgresql.jar"
    ranger_jdbc_symlink_name = "postgres-jdbc-driver.jar"
  elif xa_audit_db_flavor and xa_audit_db_flavor.lower() == 'sqlserver':
    ranger_jdbc_jar_name = "sqljdbc4.jar"
    ranger_jdbc_symlink_name = "mssql-jdbc-driver.jar"
  
  ranger_downloaded_custom_connector = format("{tmp_dir}/{ranger_jdbc_jar_name}")
  
  ranger_driver_curl_source = format("{jdk_location}/{ranger_jdbc_symlink_name}")
  ranger_driver_curl_target = format("{java_share_dir}/{ranger_jdbc_jar_name}")

if security_enabled:
  hive_principal = hive_server_principal.replace('_HOST',hostname.lower())
  
hive_ranger_plugin_config = {
  'username': repo_config_username,
  'password': repo_config_password,
  'jdbc.driverClassName': jdbc_driver_class_name,
  'jdbc.url': format("{hive_url}/default;principal={hive_principal}") if security_enabled else hive_url,
  'commonNameForCertificate': common_name_for_certificate
}

hive_ranger_plugin_repo = {
  'isActive': 'true',
  'config': json.dumps(hive_ranger_plugin_config),
  'description': 'hive repo',
  'name': repo_name,
  'repositoryType': 'hive',
  'assetType': '3'
}
