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

from resource_management.libraries.functions.version import format_stack_version, compare_versions
from resource_management.libraries.functions.default import default
from resource_management import *

# a map of the Ambari role to the component name
# for use with /usr/iop/current/<component>
SERVER_ROLE_DIRECTORY_MAP = {
  'SQOOP' : 'sqoop-client'
}

component_directory = Script.get_component_from_role(SERVER_ROLE_DIRECTORY_MAP, "SQOOP")

config = Script.get_config()
ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]

stack_name = default("/hostLevelParams/stack_name", None)

stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
stack_version = format_stack_version(stack_version_unformatted)

# New Cluster Stack Version that is defined during the RESTART of a Rolling Upgrade
version = default("/commandParams/version", None)

#hadoop params
sqoop_conf_dir = "/usr/iop/current/sqoop-client/conf"
sqoop_lib = '/usr/iop/current/sqoop-client/lib'
hadoop_home = '/usr/iop/current/hadoop-client'
hbase_home = '/usr/iop/current/hbase-client'
hive_home = '/usr/iop/current/hive-client'
sqoop_bin_dir = '/usr/iop/current/sqoop-client/bin/'

zoo_conf_dir = "/usr/iop/current/zookeeper-client/conf"
security_enabled = config['configurations']['cluster-env']['security_enabled']
smokeuser = config['configurations']['cluster-env']['smokeuser']
smokeuser_principal = config['configurations']['cluster-env']['smokeuser_principal_name']
user_group = config['configurations']['cluster-env']['user_group']
sqoop_env_sh_template = config['configurations']['sqoop-env']['content']

sqoop_user = config['configurations']['sqoop-env']['sqoop_user']

smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
kinit_path_local = functions.get_kinit_path()
#JDBC driver jar name
sqoop_jdbc_drivers_dict = {}
sqoop_jdbc_drivers_name_dict = {}
if "jdbc_drivers" in config['configurations']['sqoop-env']:
  sqoop_jdbc_drivers = config['configurations']['sqoop-env']['jdbc_drivers'].split(',')

  for driver_name in sqoop_jdbc_drivers:
    driver_name = driver_name.strip()
    if driver_name and not driver_name == '':
      if driver_name == "com.microsoft.sqlserver.jdbc.SQLServerDriver":
        jdbc_jar_name = "sqljdbc4.jar"
        jdbc_symlink_name = "mssql-jdbc-driver.jar"
        jdbc_driver_name = "mssql"
      elif driver_name == "com.mysql.jdbc.Driver":
        jdbc_jar_name = "mysql-connector-java.jar"
        jdbc_symlink_name = "mysql-jdbc-driver.jar"
        jdbc_driver_name = "mysql"
      elif driver_name == "org.postgresql.Driver":
        jdbc_jar_name = "postgresql-jdbc.jar"
        jdbc_symlink_name = "postgres-jdbc-driver.jar"
        jdbc_driver_name = "postgres"
      elif driver_name == "oracle.jdbc.driver.OracleDriver":
        jdbc_jar_name = "ojdbc.jar"
        jdbc_symlink_name = "oracle-jdbc-driver.jar"
        jdbc_driver_name = "oracle"
      elif driver_name == "org.hsqldb.jdbc.JDBCDriver":
        jdbc_jar_name = "hsqldb.jar"
        jdbc_symlink_name = "hsqldb-jdbc-driver.jar"
        jdbc_driver_name = "hsqldb"
    else:
      continue
    sqoop_jdbc_drivers_dict[jdbc_jar_name] = jdbc_symlink_name
    sqoop_jdbc_drivers_name_dict[jdbc_jar_name] = jdbc_driver_name
jdk_location = config['hostLevelParams']['jdk_location']
