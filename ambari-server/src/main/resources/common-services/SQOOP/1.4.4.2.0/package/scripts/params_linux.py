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

from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.get_kinit_path import get_kinit_path
from resource_management.libraries.script import Script
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.expect import expect
from resource_management.libraries.functions.setup_atlas_hook import has_atlas_in_cluster


# a map of the Ambari role to the component name
# for use with <stack-root>/current/<component>
SERVER_ROLE_DIRECTORY_MAP = {
  'SQOOP' : 'sqoop-client'
}

component_directory = Script.get_component_from_role(SERVER_ROLE_DIRECTORY_MAP, "SQOOP")

config = Script.get_config()
stack_root = Script.get_stack_root()

# Needed since this is an Atlas Hook service.
cluster_name = config['clusterName']

ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]

stack_name = default("/hostLevelParams/stack_name", None)

stack_version_unformatted = config['hostLevelParams']['stack_version']
stack_version_formatted = format_stack_version(stack_version_unformatted)

agent_stack_retry_on_unavailability = config['hostLevelParams']['agent_stack_retry_on_unavailability']
agent_stack_retry_count = expect("/hostLevelParams/agent_stack_retry_count", int)

# New Cluster Stack Version that is defined during the RESTART of a Rolling Upgrade
version = default("/commandParams/version", None)

# default hadoop params
sqoop_conf_dir = "/usr/lib/sqoop/conf"
sqoop_lib = "/usr/lib/sqoop/lib"
hadoop_home = '/usr/lib/hadoop'
hbase_home = "/usr/lib/hbase"
hive_home = "/usr/lib/hive"
sqoop_bin_dir = "/usr/bin"
zoo_conf_dir = "/etc/zookeeper"

# For stack versions supporting rolling upgrade
if stack_version_formatted and check_stack_feature(StackFeature.ROLLING_UPGRADE, stack_version_formatted):
  sqoop_conf_dir = format("{stack_root}/current/sqoop-client/conf")
  sqoop_lib = format("{stack_root}/current/sqoop-client/lib")
  hadoop_home = format("{stack_root}/current/hadoop-client")
  hbase_home = format("{stack_root}/current/hbase-client")
  hive_home = format("{stack_root}/current/hive-client")
  sqoop_bin_dir = format("{stack_root}/current/sqoop-client/bin/")
  zoo_conf_dir = format("{stack_root}/current/zookeeper-client/conf")

security_enabled = config['configurations']['cluster-env']['security_enabled']
smokeuser = config['configurations']['cluster-env']['smokeuser']
smokeuser_principal = config['configurations']['cluster-env']['smokeuser_principal_name']
user_group = config['configurations']['cluster-env']['user_group']
sqoop_env_sh_template = config['configurations']['sqoop-env']['content']

sqoop_user = config['configurations']['sqoop-env']['sqoop_user']

smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
#JDBC driver jar name
sqoop_jdbc_drivers_dict = []
sqoop_jdbc_drivers_name_dict = {}
sqoop_jdbc_drivers_to_remove = {}
if "jdbc_drivers" in config['configurations']['sqoop-env']:
  sqoop_jdbc_drivers = config['configurations']['sqoop-env']['jdbc_drivers'].split(',')

  for driver_name in sqoop_jdbc_drivers:
    previous_jdbc_jar_name = None
    driver_name = driver_name.strip()
    if driver_name and not driver_name == '':
      if driver_name == "com.microsoft.sqlserver.jdbc.SQLServerDriver":
        jdbc_name = default("/hostLevelParams/custom_mssql_jdbc_name", None)
        previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_mssql_jdbc_name", None)
        jdbc_driver_name = "mssql"
      elif driver_name == "com.mysql.jdbc.Driver":
        jdbc_name = default("/hostLevelParams/custom_mysql_jdbc_name", None)
        previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_mysql_jdbc_name", None)
        jdbc_driver_name = "mysql"
      elif driver_name == "org.postgresql.Driver":
        jdbc_name = default("/hostLevelParams/custom_postgres_jdbc_name", None)
        previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_postgres_jdbc_name", None)
        jdbc_driver_name = "postgres"
      elif driver_name == "oracle.jdbc.driver.OracleDriver":
        jdbc_name = default("/hostLevelParams/custom_oracle_jdbc_name", None)
        previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_oracle_jdbc_name", None)
        jdbc_driver_name = "oracle"
      elif driver_name == "org.hsqldb.jdbc.JDBCDriver":
        jdbc_name = default("/hostLevelParams/custom_hsqldb_jdbc_name", None)
        previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_hsqldb_jdbc_name", None)
        jdbc_driver_name = "hsqldb"
    else:
      continue
    sqoop_jdbc_drivers_dict.append(jdbc_name)
    sqoop_jdbc_drivers_to_remove[jdbc_name] = previous_jdbc_jar_name
    sqoop_jdbc_drivers_name_dict[jdbc_name] = jdbc_driver_name
jdk_location = config['hostLevelParams']['jdk_location']


########################################################
############# Atlas related params #####################
########################################################
#region Atlas Hooks
sqoop_atlas_application_properties = default('/configurations/sqoop-atlas-application.properties', {})
enable_atlas_hook = default('/configurations/sqoop-env/sqoop.atlas.hook', False)
atlas_hook_filename = default('/configurations/atlas-env/metadata_conf_file', 'atlas-application.properties')
#endregion
