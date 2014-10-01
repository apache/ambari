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

from resource_management import *
import status_params
import os

# server configurations
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

hive_metastore_user_name = config['configurations']['hive-site']['javax.jdo.option.ConnectionUserName']
hive_jdbc_connection_url = config['configurations']['hive-site']['javax.jdo.option.ConnectionURL']

hive_metastore_user_passwd = config['configurations']['hive-site']['javax.jdo.option.ConnectionPassword']
hive_metastore_db_type = config['configurations']['hive-env']['hive_database_type']

#users
hive_user = config['configurations']['hive-env']['hive_user']
hive_lib = '/usr/lib/hive/lib/'
#JDBC driver jar name
hive_jdbc_driver = config['configurations']['hive-site']['javax.jdo.option.ConnectionDriverName']
if hive_jdbc_driver == "com.mysql.jdbc.Driver":
  jdbc_jar_name = "mysql-connector-java.jar"
  jdbc_symlink_name = "mysql-jdbc-driver.jar"
elif hive_jdbc_driver == "org.postgresql.Driver":
  jdbc_jar_name = "postgresql-jdbc.jar"
  jdbc_symlink_name = "postgres-jdbc-driver.jar"
elif hive_jdbc_driver == "oracle.jdbc.driver.OracleDriver":
  jdbc_jar_name = "ojdbc6.jar"
  jdbc_symlink_name = "oracle-jdbc-driver.jar"

check_db_connection_jar_name = "DBConnectionVerification.jar"
check_db_connection_jar = format("/usr/lib/ambari-agent/{check_db_connection_jar_name}")

#common
hdp_stack_version = config['hostLevelParams']['stack_version']
hive_metastore_port = get_port_from_url(config['configurations']['hive-site']['hive.metastore.uris']) #"9083"
hive_var_lib = '/var/lib/hive'
ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]
hive_bin = '/usr/lib/hive/bin'
hive_server_host = config['clusterHostInfo']['hive_server_host'][0]
hive_server_port = default('/configurations/hive-site/hive.server2.thrift.port',"10000")
hive_url = format("jdbc:hive2://{hive_server_host}:{hive_server_port}")

smokeuser = config['configurations']['cluster-env']['smokeuser']
smoke_test_sql = format("{tmp_dir}/hiveserver2.sql")
smoke_test_path = format("{tmp_dir}/hiveserver2Smoke.sh")
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']

fs_root = config['configurations']['core-site']['fs.defaultFS']
security_enabled = config['configurations']['cluster-env']['security_enabled']

kinit_path_local = functions.get_kinit_path(["/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])
hive_metastore_keytab_path =  config['configurations']['hive-site']['hive.metastore.kerberos.keytab.file']

#hive_env
hive_dbroot = config['configurations']['hive-env']['hive_dbroot']
hive_log_dir = config['configurations']['hive-env']['hive_log_dir']
hive_pid_dir = status_params.hive_pid_dir
hive_pid = status_params.hive_pid
#Default conf dir for client
hive_client_conf_dir = "/etc/hive/conf"
hive_server_conf_dir = "/etc/hive/conf"
hive_conf_dirs_list = [hive_server_conf_dir, hive_client_conf_dir]

if 'role' in config and config['role'] in ["HIVE_SERVER", "HIVE_METASTORE"]:
  hive_config_dir = hive_server_conf_dir
else:
  hive_config_dir = hive_client_conf_dir

#hive-site
hive_database_name = config['configurations']['hive-env']['hive_database_name']

#Starting hiveserver2
start_hiveserver2_script = 'startHiveserver2.sh.j2'

hadoop_home = '/usr/lib/hadoop'

##Starting metastore
start_metastore_script = 'startMetastore.sh'
hive_metastore_pid = status_params.hive_metastore_pid
java_share_dir = '/usr/share/java'
driver_curl_target = format("{java_share_dir}/{jdbc_jar_name}")

hdfs_user =  config['configurations']['hadoop-env']['hdfs_user']
user_group = config['configurations']['cluster-env']['user_group']
artifact_dir = format("{tmp_dir}/AMBARI-artifacts/")

target = format("{hive_lib}/{jdbc_jar_name}")

jdk_location = config['hostLevelParams']['jdk_location']
driver_curl_source = format("{jdk_location}/{jdbc_symlink_name}")

start_hiveserver2_path = format("{tmp_dir}/start_hiveserver2_script")
start_metastore_path = format("{tmp_dir}/start_metastore_script")

hadoop_heapsize = config['configurations']['hadoop-env']['hadoop_heapsize']
hive_heapsize = config['configurations']['hive-site']['hive.heapsize']
java64_home = config['hostLevelParams']['java_home']

##### MYSQL

db_name = config['configurations']['hive-env']['hive_database_name']
mysql_user = "mysql"
mysql_group = 'mysql'
mysql_host = config['clusterHostInfo']['hive_mysql_host']

mysql_adduser_path = format("{tmp_dir}/addMysqlUser.sh")

##### POSTGRES
postgresql_adduser_file = "addPostgreSQLUser.sh"
postgresql_adduser_path = format("{tmp_dir}/{postgresql_adduser_file}")
postgresql_host = config['clusterHostInfo']['hive_postgresql_host']
postgresql_pghba_conf_path = "/var/lib/pgsql/data/pg_hba.conf"
postgresql_conf_path = "/var/lib/pgsql/data/postgresql.conf"
postgresql_daemon_name = status_params.postgresql_daemon_name

######## Metastore Schema
init_metastore_schema = True

########## HCAT
hcat_conf_dir = '/etc/hive-hcatalog/conf'
hcat_lib = '/usr/lib/hive-hcatalog/share/hcatalog'

hcat_dbroot = hcat_lib

hcat_user = config['configurations']['hive-env']['hcat_user']
webhcat_user = config['configurations']['hive-env']['webhcat_user']

hcat_pid_dir = status_params.hcat_pid_dir
hcat_log_dir = config['configurations']['hive-env']['hcat_log_dir']

hadoop_conf_dir = '/etc/hadoop/conf'

#hive-log4j.properties.template
if (('hive-log4j' in config['configurations']) and ('content' in config['configurations']['hive-log4j'])):
  log4j_props = config['configurations']['hive-log4j']['content']
else:
  log4j_props = None

#hive-exec-log4j.properties.template
if (('hive-exec-log4j' in config['configurations']) and ('content' in config['configurations']['hive-exec-log4j'])):
  log4j_exec_props = config['configurations']['hive-exec-log4j']['content']
else:
  log4j_exec_props = None

daemon_name = status_params.daemon_name
hive_env_sh_template = config['configurations']['hive-env']['content']

hive_hdfs_user_dir = format("/user/{hive_user}")
hive_hdfs_user_mode = 0700
hive_apps_whs_dir = config['configurations']['hive-site']["hive.metastore.warehouse.dir"]
#for create_hdfs_directory
hostname = config["hostname"]
hadoop_conf_dir = "/etc/hadoop/conf"
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name']
kinit_path_local = functions.get_kinit_path(["/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])

# Tez libraries
tez_lib_uris = default("/configurations/tez-site/tez.lib.uris", None)
tez_local_api_jars = '/usr/lib/tez/tez*.jar'
tez_local_lib_jars = '/usr/lib/tez/lib/*.jar'
tez_user = config['configurations']['tez-env']['tez_user']

if System.get_instance().os_family == "ubuntu":
  mysql_configname = '/etc/mysql/my.cnf'
else:
  mysql_configname = '/etc/my.cnf'
  

# Hive security
hive_authorization_enabled = config['configurations']['hive-site']['hive.security.authorization.enabled']

mysql_jdbc_driver_jar = "/usr/share/java/mysql-connector-java.jar"

# There are other packages that contain /usr/share/java/mysql-connector-java.jar (like libmysql-java),
# trying to install mysql-connector-java upon them can cause packages to conflict.
if os.path.exists(mysql_jdbc_driver_jar):
  hive_exclude_packages = ['mysql-connector-java']
else:  
  hive_exclude_packages = []

import functools
#create partial functions with common arguments for every HdfsDirectory call
#to create hdfs directory we need to call params.HdfsDirectory in code
#create partial functions with common arguments for every HdfsDirectory call
#to create hdfs directory we need to call params.HdfsDirectory in code
HdfsDirectory = functools.partial(
  HdfsDirectory,
  conf_dir=hadoop_conf_dir,
  hdfs_user=hdfs_user,
  security_enabled = security_enabled,
  keytab = hdfs_user_keytab,
  kinit_path_local = kinit_path_local
)
