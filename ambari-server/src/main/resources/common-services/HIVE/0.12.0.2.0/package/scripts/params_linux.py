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

from urlparse import urlparse

from ambari_commons.constants import AMBARI_SUDO_BINARY
from ambari_commons.os_check import OSCheck
from ambari_commons.str_utils import cbool, cint

from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.is_empty import is_empty
from resource_management.libraries.functions.version import format_hdp_stack_version
from resource_management.libraries.functions.copy_tarball import STACK_VERSION_PATTERN
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions.get_not_managed_resources import get_not_managed_resources
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.get_port_from_url import get_port_from_url
from resource_management.libraries import functions

# server configurations
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()
sudo = AMBARI_SUDO_BINARY

stack_name = default("/hostLevelParams/stack_name", None)
agent_stack_retry_on_unavailability = cbool(default("/hostLevelParams/agent_stack_retry_on_unavailability", None))
agent_stack_retry_count = cint(default("/hostLevelParams/agent_stack_retry_count", None))

# node hostname
hostname = config["hostname"]

# This is expected to be of the form #.#.#.#
stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
hdp_stack_version_major = format_hdp_stack_version(stack_version_unformatted)
stack_is_hdp21 = Script.is_hdp_stack_less_than("2.2")

# this is not available on INSTALL action because hdp-select is not available
hdp_stack_version = functions.get_hdp_version('hive-server2')

# New Cluster Stack Version that is defined during the RESTART of a Rolling Upgrade.
# It cannot be used during the initial Cluser Install because the version is not yet known.
version = default("/commandParams/version", None)

# current host stack version
current_version = default("/hostLevelParams/current_version", None)

# When downgrading the 'version' and 'current_version' are both pointing to the downgrade-target version
# downgrade_from_version provides the source-version the downgrade is happening from 
downgrade_from_version = default("/commandParams/downgrade_from_version", None)

# Upgrade direction
upgrade_direction = default("/commandParams/upgrade_direction", None)

hadoop_bin_dir = "/usr/bin"
hadoop_home = '/usr'
hive_bin = '/usr/lib/hive/bin'
hive_lib = '/usr/lib/hive/lib/'
hive_var_lib = '/var/lib/hive'

# These tar folders were used in HDP 2.1
hadoop_streaming_jars = '/usr/lib/hadoop-mapreduce/hadoop-streaming-*.jar'
pig_tar_file = '/usr/share/HDP-webhcat/pig.tar.gz'
hive_tar_file = '/usr/share/HDP-webhcat/hive.tar.gz'
sqoop_tar_file = '/usr/share/HDP-webhcat/sqoop*.tar.gz'

hive_specific_configs_supported = False
hive_etc_dir_prefix = "/etc/hive"
limits_conf_dir = "/etc/security/limits.d"

hive_user_nofile_limit = default("/configurations/hive-env/hive_user_nofile_limit", "32000")
hive_user_nproc_limit = default("/configurations/hive-env/hive_user_nproc_limit", "16000")

# use the directories from status_params as they are already calculated for
# the correct version of HDP
hadoop_conf_dir = status_params.hadoop_conf_dir
hadoop_bin_dir = status_params.hadoop_bin_dir
webhcat_conf_dir = status_params.webhcat_conf_dir
hive_conf_dir = status_params.hive_conf_dir
hive_config_dir = status_params.hive_config_dir
hive_client_conf_dir = status_params.hive_client_conf_dir
hive_server_conf_dir = status_params.hive_server_conf_dir

hcat_conf_dir = '/etc/hive-hcatalog/conf'
config_dir = '/etc/hive-webhcat/conf'
hcat_lib = '/usr/lib/hive-hcatalog/share/hcatalog'
webhcat_bin_dir = '/usr/lib/hive-hcatalog/sbin'

# Starting from HDP2.3 drop should be executed with purge suffix
purge_tables = "false"
if Script.is_hdp_stack_greater_or_equal("2.3"):
  purge_tables = 'true'

  # this is NOT a typo.  HDP-2.3 configs for hcatalog/webhcat point to a
  # specific directory which is NOT called 'conf'
  hcat_conf_dir = '/usr/hdp/current/hive-webhcat/etc/hcatalog'
  config_dir = '/usr/hdp/current/hive-webhcat/etc/webhcat'

if Script.is_hdp_stack_greater_or_equal("2.2"):
  hive_specific_configs_supported = True

  component_directory = status_params.component_directory
  hadoop_home = '/usr/hdp/current/hadoop-client'
  hive_bin = format('/usr/hdp/current/{component_directory}/bin')
  hive_lib = format('/usr/hdp/current/{component_directory}/lib')

  # there are no client versions of these, use server versions directly
  hcat_lib = '/usr/hdp/current/hive-webhcat/share/hcatalog'
  webhcat_bin_dir = '/usr/hdp/current/hive-webhcat/sbin'
  
  # --- Tarballs ---
  # DON'T CHANGE THESE VARIABLE NAMES
  # Values don't change from those in copy_tarball.py
  hive_tar_source = "/usr/hdp/{0}/hive/hive.tar.gz".format(STACK_VERSION_PATTERN)
  pig_tar_source = "/usr/hdp/{0}/pig/pig.tar.gz".format(STACK_VERSION_PATTERN)
  hive_tar_dest_file = "/hdp/apps/{0}/hive/hive.tar.gz".format(STACK_VERSION_PATTERN)
  pig_tar_dest_file = "/hdp/apps/{0}/pig/pig.tar.gz".format(STACK_VERSION_PATTERN)

  hadoop_streaming_tar_source = "/usr/hdp/{0}/hadoop-mapreduce/hadoop-streaming.jar".format(STACK_VERSION_PATTERN)
  sqoop_tar_source = "/usr/hdp/{0}/sqoop/sqoop.tar.gz".format(STACK_VERSION_PATTERN)
  hadoop_streaming_tar_dest_dir = "/hdp/apps/{0}/mapreduce/".format(STACK_VERSION_PATTERN)
  sqoop_tar_dest_dir = "/hdp/apps/{0}/sqoop/".format(STACK_VERSION_PATTERN)

  tarballs_mode = 0444
else:
  # --- Tarballs ---
  webhcat_apps_dir = "/apps/webhcat"

  # In HDP 2.1, the tarballs were copied from and to different locations.
  # DON'T CHANGE THESE VARIABLE NAMES
  hive_tar_source = hive_tar_file
  pig_tar_source = pig_tar_file
  hive_tar_dest_file = webhcat_apps_dir + "/hive.tar.gz"
  pig_tar_dest_file = webhcat_apps_dir + "/pig.tar.gz"

  hadoop_streaming_tar_source = hadoop_streaming_jars   # this contains *
  sqoop_tar_source = sqoop_tar_file                     # this contains *
  hadoop_streaming_tar_dest_dir = webhcat_apps_dir
  sqoop_tar_dest_dir = webhcat_apps_dir

  tarballs_mode = 0755

execute_path = os.environ['PATH'] + os.pathsep + hive_bin + os.pathsep + hadoop_bin_dir
hive_metastore_user_name = config['configurations']['hive-site']['javax.jdo.option.ConnectionUserName']
hive_jdbc_connection_url = config['configurations']['hive-site']['javax.jdo.option.ConnectionURL']

hive_metastore_user_passwd = config['configurations']['hive-site']['javax.jdo.option.ConnectionPassword']
hive_metastore_user_passwd = unicode(hive_metastore_user_passwd) if not is_empty(hive_metastore_user_passwd) else hive_metastore_user_passwd
hive_metastore_db_type = config['configurations']['hive-env']['hive_database_type']
#HACK Temporarily use dbType=azuredb while invoking schematool
if hive_metastore_db_type == "mssql":
  hive_metastore_db_type = "azuredb"

#users
hive_user = config['configurations']['hive-env']['hive_user']
#JDBC driver jar name
hive_jdbc_driver = config['configurations']['hive-site']['javax.jdo.option.ConnectionDriverName']
# NOT SURE THAT IT'S A GOOD IDEA TO USE PATH TO CLASS IN DRIVER, MAYBE IT WILL BE BETTER TO USE DB TYPE.
# BECAUSE PATH TO CLASSES COULD BE CHANGED
sqla_db_used = False
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
elif hive_jdbc_driver == "sap.jdbc4.sqlanywhere.IDriver":
  jdbc_jar_name = "sajdbc4.jar"
  jdbc_symlink_name = "sqlanywhere-jdbc-driver.tar.gz"
  sqla_db_used = True

check_db_connection_jar_name = "DBConnectionVerification.jar"
check_db_connection_jar = format("/usr/lib/ambari-agent/{check_db_connection_jar_name}")
hive_jdbc_drivers_list = ["com.microsoft.sqlserver.jdbc.SQLServerDriver","com.mysql.jdbc.Driver",
                          "org.postgresql.Driver","oracle.jdbc.driver.OracleDriver","sap.jdbc4.sqlanywhere.IDriver"]
downloaded_custom_connector = format("{tmp_dir}/{jdbc_jar_name}")
prepackaged_ojdbc_symlink = format("{hive_lib}/ojdbc6.jar")
templeton_port = config['configurations']['webhcat-site']['templeton.port']

#constants for type2 jdbc
jdbc_libs_dir = format("{hive_lib}/native/lib64")
lib_dir_available = os.path.exists(jdbc_libs_dir)

if sqla_db_used:
  jars_path_in_archive = format("{tmp_dir}/sqla-client-jdbc/java/*")
  libs_path_in_archive = format("{tmp_dir}/sqla-client-jdbc/native/lib64/*")
  downloaded_custom_connector = format("{tmp_dir}/sqla-client-jdbc.tar.gz")
  libs_in_hive_lib = format("{jdbc_libs_dir}/*")

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
# Need this for yarn.nodemanager.recovery.dir in yarn-site
yarn_log_dir_prefix = config['configurations']['yarn-env']['yarn_log_dir_prefix']

target = format("{hive_lib}/{jdbc_jar_name}")
jars_in_hive_lib = format("{hive_lib}/*.jar")


if Script.is_hdp_stack_less_than("2.2"):
  source_jdbc_file = target
else:
  # normally, the JDBC driver would be referenced by /usr/hdp/current/.../foo.jar
  # but in RU if hdp-select is called and the restart fails, then this means that current pointer
  # is now pointing to the upgraded version location; that's bad for the cp command
  source_jdbc_file = format("/usr/hdp/{current_version}/hive/lib/{jdbc_jar_name}")

jdk_location = config['hostLevelParams']['jdk_location']
driver_curl_source = format("{jdk_location}/{jdbc_symlink_name}")

start_hiveserver2_path = format("{tmp_dir}/start_hiveserver2_script")
start_metastore_path = format("{tmp_dir}/start_metastore_script")

hadoop_heapsize = config['configurations']['hadoop-env']['hadoop_heapsize']

if 'role' in config and config['role'] in ["HIVE_SERVER", "HIVE_METASTORE"]:
  if Script.is_hdp_stack_less_than("2.2"):
    hive_heapsize = config['configurations']['hive-site']['hive.heapsize']
  else:
    hive_heapsize = config['configurations']['hive-env']['hive.heapsize']
else:
  hive_heapsize = config['configurations']['hive-env']['hive.client.heapsize']

hive_metastore_heapsize = config['configurations']['hive-env']['hive.metastore.heapsize']

java64_home = config['hostLevelParams']['java_home']

##### MYSQL
db_name = config['configurations']['hive-env']['hive_database_name']
mysql_group = 'mysql'
mysql_host = config['clusterHostInfo']['hive_mysql_host']

mysql_adduser_path = format("{tmp_dir}/addMysqlUser.sh")
mysql_deluser_path = format("{tmp_dir}/removeMysqlUser.sh")

#### Metastore
# initialize the schema only if not in an upgrade/downgrade
init_metastore_schema = upgrade_direction is None

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
hive_hdfs_user_mode = 0755
hive_apps_whs_dir = config['configurations']['hive-site']["hive.metastore.warehouse.dir"]
whs_dir_protocol = urlparse(hive_apps_whs_dir).scheme
hive_exec_scratchdir = config['configurations']['hive-site']["hive.exec.scratchdir"]
#for create_hdfs_directory
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
hdfs_principal_name = default('/configurations/hadoop-env/hdfs_principal_name', 'missing_principal').replace("_HOST", hostname)

# Tez-related properties
tez_user = config['configurations']['tez-env']['tez_user']

# Tez jars
tez_local_api_jars = '/usr/lib/tez/tez*.jar'
tez_local_lib_jars = '/usr/lib/tez/lib/*.jar'

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
############# Atlas related params #####################
########################################################

atlas_hosts = default('/clusterHostInfo/atlas_server_hosts', [])
has_atlas = len(atlas_hosts) > 0
classpath_addition = ""
atlas_plugin_package = "atlas-metadata*-hive-plugin"
atlas_ubuntu_plugin_package = "atlas-metadata.*-hive-plugin"

if not has_atlas:
  hive_exclude_packages.append(atlas_plugin_package)
  hive_exclude_packages.append(atlas_ubuntu_plugin_package)
else:
  # client.properties
  atlas_client_props = {}
  auth_enabled = config['configurations']['application-properties'].get(
    'atlas.http.authentication.enabled', False)
  atlas_client_props['atlas.http.authentication.enabled'] = auth_enabled
  if auth_enabled:
    atlas_client_props['atlas.http.authentication.type'] = config['configurations']['application-properties'].get('atlas.http.authentication.type', 'simple')

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

dfs_type = default("/commandParams/dfs_type", "")

import functools
#create partial functions with common arguments for every HdfsResource call
#to create hdfs directory we need to call params.HdfsResource in code
HdfsResource = functools.partial(
 HdfsResource,
  user = hdfs_user,
  hdfs_resource_ignore_file = "/var/lib/ambari-agent/data/.hdfs_resource_ignore",
  security_enabled = security_enabled,
  keytab = hdfs_user_keytab,
  kinit_path_local = kinit_path_local,
  hadoop_bin_dir = hadoop_bin_dir,
  hadoop_conf_dir = hadoop_conf_dir,
  principal_name = hdfs_principal_name,
  hdfs_site = hdfs_site,
  default_fs = default_fs,
  immutable_paths = get_not_managed_resources(),
  dfs_type = dfs_type
 )


# ranger host
ranger_admin_hosts = default("/clusterHostInfo/ranger_admin_hosts", [])
has_ranger_admin = not len(ranger_admin_hosts) == 0
xml_configurations_supported = config['configurations']['ranger-env']['xml_configurations_supported']

#ranger hive properties
policymgr_mgr_url = config['configurations']['admin-properties']['policymgr_external_url']
sql_connector_jar = config['configurations']['admin-properties']['SQL_CONNECTOR_JAR']
xa_audit_db_name = config['configurations']['admin-properties']['audit_db_name']
xa_audit_db_user = config['configurations']['admin-properties']['audit_db_user']
xa_db_host = config['configurations']['admin-properties']['db_host']
repo_name = str(config['clusterName']) + '_hive'

jdbc_driver_class_name = config['configurations']['ranger-hive-plugin-properties']['jdbc.driverClassName']
common_name_for_certificate = config['configurations']['ranger-hive-plugin-properties']['common.name.for.certificate']

repo_config_username = config['configurations']['ranger-hive-plugin-properties']['REPOSITORY_CONFIG_USERNAME']

ranger_env = config['configurations']['ranger-env']
ranger_plugin_properties = config['configurations']['ranger-hive-plugin-properties']
policy_user = config['configurations']['ranger-hive-plugin-properties']['policy_user']

if security_enabled:
  hive_principal = hive_server_principal.replace('_HOST',hostname.lower())

#For curl command in ranger plugin to get db connector
if has_ranger_admin:
  enable_ranger_hive = (config['configurations']['hive-env']['hive_security_authorization'].lower() == 'ranger')
  repo_config_password = unicode(config['configurations']['ranger-hive-plugin-properties']['REPOSITORY_CONFIG_PASSWORD'])
  xa_audit_db_flavor = (config['configurations']['admin-properties']['DB_FLAVOR']).lower()

  if xa_audit_db_flavor and xa_audit_db_flavor == 'mysql':
    ranger_jdbc_symlink_name = "mysql-jdbc-driver.jar"
    ranger_jdbc_jar_name = "mysql-connector-java.jar"
    audit_jdbc_url = format('jdbc:mysql://{xa_db_host}/{xa_audit_db_name}')
    jdbc_driver = "com.mysql.jdbc.Driver"
  elif xa_audit_db_flavor and xa_audit_db_flavor == 'oracle':
    ranger_jdbc_jar_name = "ojdbc6.jar"
    ranger_jdbc_symlink_name = "oracle-jdbc-driver.jar"
    colon_count = xa_db_host.count(':')
    if colon_count == 2 or colon_count == 0:
      audit_jdbc_url = format('jdbc:oracle:thin:@{xa_db_host}')
    else:
      audit_jdbc_url = format('jdbc:oracle:thin:@//{xa_db_host}')
    jdbc_driver = "oracle.jdbc.OracleDriver"
  elif xa_audit_db_flavor and xa_audit_db_flavor == 'postgres':
    ranger_jdbc_jar_name = "postgresql.jar"
    ranger_jdbc_symlink_name = "postgres-jdbc-driver.jar"
    audit_jdbc_url = format('jdbc:postgresql://{xa_db_host}/{xa_audit_db_name}')
    jdbc_driver = "org.postgresql.Driver"
  elif xa_audit_db_flavor and xa_audit_db_flavor == 'mssql':
    ranger_jdbc_jar_name = "sqljdbc4.jar"
    ranger_jdbc_symlink_name = "mssql-jdbc-driver.jar"
    audit_jdbc_url = format('jdbc:sqlserver://{xa_db_host};databaseName={xa_audit_db_name}')
    jdbc_driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
  elif xa_audit_db_flavor and xa_audit_db_flavor == 'sqla':
    ranger_jdbc_jar_name = "sajdbc4.jar"
    ranger_jdbc_symlink_name = "sqlanywhere-jdbc-driver.tar.gz"
    audit_jdbc_url = format('jdbc:sqlanywhere:database={xa_audit_db_name};host={xa_db_host}')
    jdbc_driver = "sap.jdbc4.sqlanywhere.IDriver"
  
  ranger_downloaded_custom_connector = format("{tmp_dir}/{ranger_jdbc_jar_name}")
  
  ranger_driver_curl_source = format("{jdk_location}/{ranger_jdbc_symlink_name}")
  ranger_driver_curl_target = format("{hive_lib}/{ranger_jdbc_jar_name}")

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

  xa_audit_db_password = unicode(config['configurations']['admin-properties']['audit_db_password'])
  ranger_audit_solr_urls = config['configurations']['ranger-admin-site']['ranger.audit.solr.urls']
  xa_audit_db_is_enabled = config['configurations']['ranger-hive-audit']['xasecure.audit.destination.db'] if xml_configurations_supported else None
  xa_audit_hdfs_is_enabled = config['configurations']['ranger-hive-audit']['xasecure.audit.destination.hdfs'] if xml_configurations_supported else None
  ssl_keystore_password = unicode(config['configurations']['ranger-hive-policymgr-ssl']['xasecure.policymgr.clientssl.keystore.password']) if xml_configurations_supported else None
  ssl_truststore_password = unicode(config['configurations']['ranger-hive-policymgr-ssl']['xasecure.policymgr.clientssl.truststore.password']) if xml_configurations_supported else None
  credential_file = format('/etc/ranger/{repo_name}/cred.jceks') if xml_configurations_supported else None

  #For SQLA explicitly disable audit to DB for Ranger
  if xa_audit_db_flavor == 'sqla':
    xa_audit_db_is_enabled = False

