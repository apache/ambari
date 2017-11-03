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

import status_params
import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
import os

from ambari_commons.constants import AMBARI_SUDO_BINARY
from ambari_commons.os_check import OSCheck

from resource_management.libraries.functions.constants import Direction
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.version import compare_versions, format_stack_version
from resource_management.libraries.functions.copy_tarball import STACK_VERSION_PATTERN
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import upgrade_summary
from resource_management.libraries.functions.stack_features import get_stack_feature_version
from resource_management.libraries.functions.get_port_from_url import get_port_from_url
from resource_management.libraries import functions

# server configurations
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()
sudo = AMBARI_SUDO_BINARY

stack_name = default("/hostLevelParams/stack_name", None)

# node hostname
hostname = config["hostname"]

# This is expected to be of the form #.#.#.#
stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
stack_version = format_stack_version(stack_version_unformatted)
stack_is_21 = False

# New Cluster Stack Version that is defined during the RESTART of a Rolling Upgrade.
# It cannot be used during the initial Cluser Install because the version is not yet known.
version = default("/commandParams/version", None)

# Upgrade direction
upgrade_direction = default("/commandParams/upgrade_direction", None)

# When downgrading the 'version' is pointing to the downgrade-target version
# downgrade_from_version provides the source-version the downgrade is happening from
downgrade_from_version = upgrade_summary.get_downgrade_from_version("HIVE")

# get the correct version to use for checking stack features
version_for_stack_feature_checks = get_stack_feature_version(config)

component_directory = status_params.component_directory
hadoop_bin_dir = "/usr/bin"
hadoop_home = '/usr'

#Hbase params keep hbase lib here,if not,mapreduce job doesn't work for hive.
hbase_lib = '/usr/iop/current/hbase-client/lib'

# Hadoop params
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")
hadoop_home = '/usr/iop/current/hadoop-client'
hive_bin = format('/usr/iop/current/{component_directory}/bin')
hive_lib = format('/usr/iop/current/{component_directory}/lib')
hive_var_lib = '/var/lib/hive'

# if this is a server action, then use the server binaries; smoke tests
# use the client binaries
command_role = default("/role", "")
server_role_dir_mapping = { 'HIVE_SERVER' : 'hive-server2',
  'HIVE_METASTORE' : 'hive-metastore' }

if command_role in server_role_dir_mapping:
  hive_server_root = server_role_dir_mapping[command_role]
  hive_bin = format('/usr/iop/current/{hive_server_root}/bin')
  hive_lib = format('/usr/iop/current/{hive_server_root}/lib')

hive_cmd = os.path.join(hive_bin, "hive")
hive_specific_configs_supported = False
hive_etc_dir_prefix = "/etc/hive"
limits_conf_dir = "/etc/security/limits.d"
hcat_conf_dir = '/etc/hive-hcatalog/conf'
config_dir = '/etc/hive-webhcat/conf'
hcat_lib = '/usr/iop/current/hive-webhcat/share/hcatalog'
webhcat_bin_dir = '/usr/iop/current/hive-webhcat/sbin'

# use the directories from status_params as they are already calculated for
# the correct version of BigInsights
hadoop_conf_dir = status_params.hadoop_conf_dir
hadoop_bin_dir = status_params.hadoop_bin_dir
webhcat_conf_dir = status_params.webhcat_conf_dir
hive_conf_dir = status_params.hive_conf_dir
hive_config_dir = status_params.hive_config_dir
hive_client_conf_dir = status_params.hive_client_conf_dir
hive_server_conf_dir = status_params.hive_server_conf_dir

hcat_lib = '/usr/iop/current/hive-webhcat/share/hcatalog'
webhcat_bin_dir = '/usr/iop/current/hive-webhcat/sbin'
component_directory = status_params.component_directory
hadoop_home = '/usr/iop/current/hadoop-client'

# there are no client versions of these, use server versions directly
hcat_lib = '/usr/iop/current/hive-webhcat/share/hcatalog'
webhcat_bin_dir = '/usr/iop/current/hive-webhcat/sbin'

# --- Tarballs ---
# DON'T CHANGE THESE VARIABLE NAMES
# Values don't change from those in copy_tarball.py

hive_tar_source = "/usr/iop/{0}/hive/hive.tar.gz".format(STACK_VERSION_PATTERN)
pig_tar_source = "/usr/iop/{0}/pig/pig.tar.gz".format(STACK_VERSION_PATTERN)
hive_tar_dest_file = "/iop/apps/{0}/hive/hive.tar.gz".format(STACK_VERSION_PATTERN)
pig_tar_dest_file = "/iop/apps/{0}/pig/pig.tar.gz".format(STACK_VERSION_PATTERN)

hadoop_streaming_tar_source = "/usr/iop/{0}/hadoop-mapreduce/hadoop-streaming.jar".format(STACK_VERSION_PATTERN)
sqoop_tar_source = "/usr/iop/{0}/sqoop/sqoop.tar.gz".format(STACK_VERSION_PATTERN)
hadoop_streaming_tar_dest_dir = "/iop/apps/{0}/mapreduce/".format(STACK_VERSION_PATTERN)
sqoop_tar_dest_dir = "/iop/apps/{0}/sqoop/".format(STACK_VERSION_PATTERN)

tarballs_mode = 0444

if Script.is_stack_greater_or_equal("4.1.0.0"):
  # this is NOT a typo.  BigInsights-4.1 configs for hcatalog/webhcat point to a
  # specific directory which is NOT called 'conf'
  hcat_conf_dir = '/usr/iop/current/hive-webhcat/etc/hcatalog'
  config_dir = '/usr/iop/current/hive-webhcat/etc/webhcat'
if Script.is_stack_greater_or_equal("4.2.0.0"):
  # need to set it to false if it is to downgrade from 4.2 to 4.1
  if upgrade_direction is not None and upgrade_direction == Direction.DOWNGRADE and version is not None and compare_versions(format_stack_version(version), '4.2.0.0') < 0:
    hive_specific_configs_supported = False
  else:
    #means it's either an upgrade or a fresh install of 4.2
    hive_specific_configs_supported = True

else: #BI 4.0
  #still need to use current dir due to rolling upgrade restrictions
  # --- Tarballs ---
  webhcat_apps_dir = "/apps/webhcat"

execute_path = os.environ['PATH'] + os.pathsep + hive_bin + os.pathsep + hadoop_bin_dir
hive_metastore_user_name = config['configurations']['hive-site']['javax.jdo.option.ConnectionUserName']
hive_jdbc_connection_url = config['configurations']['hive-site']['javax.jdo.option.ConnectionURL']

version_for_source_jdbc_file = upgrade_summary.get_source_version(default_version = version_for_stack_feature_checks)

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
ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]
hive_server_host = config['clusterHostInfo']['hive_server_host'][0]
hive_server_hosts = config['clusterHostInfo']['hive_server_host']
hive_transport_mode = config['configurations']['hive-site']['hive.server2.transport.mode']

if hive_transport_mode.lower() == "http":
  hive_server_port = config['configurations']['hive-site']['hive.server2.thrift.http.port']
else:
  hive_server_port = default('/configurations/hive-site/hive.server2.thrift.port',"10000")

hive_url = format("jdbc:hive2://{hive_server_host}:{hive_server_port}")
hive_http_endpoint = default('/configurations/hive-site/hive.server2.thrift.http.path', "cliservice")
hive_server_principal = config['configurations']['hive-site']['hive.server2.authentication.kerberos.principal']
hive_server2_authentication = config['configurations']['hive-site']['hive.server2.authentication']

# ssl options
hive_ssl = default('/configurations/hive-site/hive.server2.use.SSL', False)
hive_ssl_keystore_path = default('/configurations/hive-site/hive.server2.keystore.path', None)
hive_ssl_keystore_password = default('/configurations/hive-site/hive.server2.keystore.password', None)

smokeuser = config['configurations']['cluster-env']['smokeuser']
smoke_test_sql = format("{tmp_dir}/hiveserver2.sql")
smoke_test_path = format("{tmp_dir}/hiveserver2Smoke.sh")
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
smokeuser_principal = config['configurations']['cluster-env']['smokeuser_principal_name']

fs_root = config['configurations']['core-site']['fs.defaultFS']
security_enabled = config['configurations']['cluster-env']['security_enabled']

kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
hive_metastore_keytab_path =  config['configurations']['hive-site']['hive.metastore.kerberos.keytab.file']

hive_server2_keytab = config['configurations']['hive-site']['hive.server2.authentication.kerberos.keytab']

#hive_env
hive_log_dir = config['configurations']['hive-env']['hive_log_dir']
hive_pid_dir = status_params.hive_pid_dir
hive_pid = status_params.hive_pid

#Default conf dir for client
hive_conf_dirs_list = [hive_client_conf_dir]

if hostname in hive_metastore_hosts or hostname in hive_server_hosts:
  hive_conf_dirs_list.append(hive_server_conf_dir)

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
yarn_user = config['configurations']['yarn-env']['yarn_user']
user_group = config['configurations']['cluster-env']['user_group']
artifact_dir = format("{tmp_dir}/AMBARI-artifacts/")

target = format("{hive_lib}/{jdbc_jar_name}")

jdk_location = config['hostLevelParams']['jdk_location']
driver_curl_source = format("{jdk_location}/{jdbc_symlink_name}")

start_hiveserver2_path = format("{tmp_dir}/start_hiveserver2_script")
start_metastore_path = format("{tmp_dir}/start_metastore_script")

hadoop_heapsize = config['configurations']['hadoop-env']['hadoop_heapsize']

if 'role' in config and config['role'] in ["HIVE_SERVER", "HIVE_METASTORE"]:
  hive_heapsize = config['configurations']['hive-env']['hive.heapsize']
else:
  hive_heapsize = config['configurations']['hive-env']['hive.client.heapsize']

hive_metastore_heapsize = config['configurations']['hive-env']['hive.metastore.heapsize']

java64_home = config['hostLevelParams']['java_home']
java_version = int(config['hostLevelParams']['java_version'])

##### MYSQL
db_name = config['configurations']['hive-env']['hive_database_name']
mysql_group = 'mysql'
mysql_host = config['clusterHostInfo']['hive_mysql_host']

mysql_adduser_path = format("{tmp_dir}/addMysqlUser.sh")
mysql_deluser_path = format("{tmp_dir}/removeMysqlUser.sh")

######## Metastore Schema
init_metastore_schema = False
if Script.is_stack_greater_or_equal("4.1.0.0"):
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
tez_user = None #config['configurations']['tez-env']['tez_user']

# Tez jars
tez_local_api_jars = None #'/usr/lib/tez/tez*.jar'
tez_local_lib_jars = None #'/usr/lib/tez/lib/*.jar'

# Tez libraries
tez_lib_uris = None #default("/configurations/tez-site/tez.lib.uris", None)

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
  hive_exclude_packages = ['mysql-connector-java', 'mysql', 'mysql-server',
                           'mysql-community-release', 'mysql-community-server']
else:
  if 'role' in config and config['role'] != "MYSQL_SERVER":
    hive_exclude_packages = ['mysql', 'mysql-server', 'mysql-community-release',
                             'mysql-community-server']
  if os.path.exists(mysql_jdbc_driver_jar):
    hive_exclude_packages.append('mysql-connector-java')


hive_site_config = dict(config['configurations']['hive-site'])

########################################################
########### WebHCat related params #####################
########################################################

webhcat_env_sh_template = config['configurations']['webhcat-env']['content']
templeton_log_dir = config['configurations']['hive-env']['hcat_log_dir']
templeton_pid_dir = status_params.hcat_pid_dir

webhcat_pid_file = status_params.webhcat_pid_file

templeton_jar = config['configurations']['webhcat-site']['templeton.jar']


webhcat_server_host = config['clusterHostInfo']['webhcat_server_host']

hcat_hdfs_user_dir = format("/user/{hcat_user}")
hcat_hdfs_user_mode = 0755
webhcat_hdfs_user_dir = format("/user/{webhcat_user}")
webhcat_hdfs_user_mode = 0755
#for create_hdfs_directory
security_param = "true" if security_enabled else "false"



hdfs_site = config['configurations']['hdfs-site']
default_fs = config['configurations']['core-site']['fs.defaultFS']

import functools
#create partial functions with common arguments for every HdfsResource call
#to create hdfs directory we need to call params.HdfsResource in code
HdfsResource = functools.partial(
  HdfsResource,
  user = hdfs_user,
  security_enabled = security_enabled,
  keytab = hdfs_user_keytab,
  kinit_path_local = kinit_path_local,
  hadoop_bin_dir = hadoop_bin_dir,
  hadoop_conf_dir = hadoop_conf_dir,
  principal_name = hdfs_principal_name,
  hdfs_site = hdfs_site,
  default_fs = default_fs
)

if security_enabled:
  hive_principal = hive_server_principal.replace('_HOST',hostname.lower())
