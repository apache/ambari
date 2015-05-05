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
from resource_management.libraries.functions import format
from resource_management.libraries.script import Script
from resource_management.libraries.functions.version import format_hdp_stack_version
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.default import default

# a map of the Ambari role to the component name
# for use with /usr/hdp/current/<component>
SERVER_ROLE_DIRECTORY_MAP = {
  'RANGER_ADMIN' : 'ranger-admin',
  'RANGER_USERSYNC' : 'ranger-usersync'
}

component_directory = Script.get_component_from_role(SERVER_ROLE_DIRECTORY_MAP, "RANGER_ADMIN")

config  = Script.get_config()
tmp_dir = Script.get_tmp_dir()

stack_name = default("/hostLevelParams/stack_name", None)
version = default("/commandParams/version", None)
host_sys_prepped = default("/hostLevelParams/host_sys_prepped", False)

stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
hdp_stack_version = format_hdp_stack_version(stack_version_unformatted)

stack_is_hdp22_or_further = Script.is_hdp_stack_greater_or_equal("2.2")
stack_is_hdp23_or_further = Script.is_hdp_stack_greater_or_equal("2.3")

if stack_is_hdp22_or_further:
  ranger_home    = '/usr/hdp/current/ranger-admin'
  ranger_conf    = '/usr/hdp/current/ranger-admin/conf'
  ranger_stop    = '/usr/bin/ranger-admin-stop'
  ranger_start   = '/usr/bin/ranger-admin-start'
  usersync_home  = '/usr/hdp/current/ranger-usersync'
  usersync_start = '/usr/bin/ranger-usersync-start'
  usersync_stop  = '/usr/bin/ranger-usersync-stop'
  
usersync_services_file = "/usr/hdp/current/ranger-usersync/ranger-usersync-services.sh"

java_home = config['hostLevelParams']['java_home']
unix_user  = config['configurations']['ranger-env']['ranger_user']
unix_group = config['configurations']['ranger-env']['ranger_group']

ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]

db_flavor =  config['configurations']['admin-properties']['DB_FLAVOR']

usersync_exturl =  config['configurations']['admin-properties']['policymgr_external_url']

sql_command_invoker = config['configurations']['admin-properties']['SQL_COMMAND_INVOKER']
db_root_user = config['configurations']['admin-properties']['db_root_user']
db_root_password = unicode(config['configurations']['admin-properties']['db_root_password'])
db_host =  config['configurations']['admin-properties']['db_host']

#ranger-env properties
oracle_home = default("/configurations/ranger-env/oracle_home", "-")

#For curl command in ranger to get db connector
jdk_location = config['hostLevelParams']['jdk_location'] 
java_share_dir = '/usr/share/java'
if db_flavor and db_flavor.lower() == 'mysql':
  jdbc_symlink_name = "mysql-jdbc-driver.jar"
  jdbc_jar_name = "mysql-connector-java.jar"
elif db_flavor and db_flavor.lower() == 'oracle':
  jdbc_jar_name = "ojdbc6.jar"
  jdbc_symlink_name = "oracle-jdbc-driver.jar"
elif db_flavor and db_flavor.lower() == 'postgres':
  jdbc_jar_name = "postgresql.jar"
  jdbc_symlink_name = "postgres-jdbc-driver.jar"
elif db_flavor and db_flavor.lower() == 'sqlserver':
  jdbc_jar_name = "sqljdbc4.jar"
  jdbc_symlink_name = "mssql-jdbc-driver.jar"

downloaded_custom_connector = format("{tmp_dir}/{jdbc_jar_name}")

driver_curl_source = format("{jdk_location}/{jdbc_symlink_name}")
driver_curl_target = format("{java_share_dir}/{jdbc_jar_name}")