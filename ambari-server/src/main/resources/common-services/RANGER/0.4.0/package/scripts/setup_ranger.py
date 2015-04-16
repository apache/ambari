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
import sys
import fileinput
import os
from resource_management import *
from resource_management.core.logger import Logger

def setup_ranger_admin():
  import params

  check_db_connnection()
  
  File(params.downloaded_custom_connector,
       content = DownloadSource(params.driver_curl_source)
  )

  Execute(('cp', '--remove-destination', params.downloaded_custom_connector, params.driver_curl_target),
          path=["/bin", "/usr/bin/"],
          not_if=format("test -f {driver_curl_target}"),
          sudo=True)
  
  ModifyPropertiesFile(format("{ranger_home}/install.properties"),
    properties = params.config['configurations']['admin-properties']
  )

  ##if db flavor == oracle - set oracle home env variable
  if params.db_flavor.lower() == 'oracle' and params.oracle_home:
    env_dict = {'JAVA_HOME': params.java_home, 'ORACLE_HOME':params.oracle_home, 'LD_LIBRARY_PATH':params.oracle_home} 
  else: 
    env_dict = {'JAVA_HOME': params.java_home}
  
  setup_sh = format("cd {ranger_home} && ") + as_sudo([format('{ranger_home}/setup.sh')])
  Execute(setup_sh, 
          environment=env_dict, 
          logoutput=True,
  )
  
  ModifyPropertiesFile(format("{ranger_conf}/xa_system.properties"),
       properties = params.config['configurations']['ranger-site'],
  )

  ModifyPropertiesFile(format("{ranger_conf}/ranger_webserver.properties"),
    properties = params.config['configurations']['ranger-site'],
    mode=0744
  )

def setup_usersync():
  import params

  PropertiesFile(format("{usersync_home}/install.properties"),
    properties = params.config['configurations']['usersync-properties'],
  )

  cmd = format("cd {usersync_home} && ") + as_sudo([format('{usersync_home}/setup.sh')])
  Execute(cmd, environment={'JAVA_HOME': params.java_home}, logoutput=True)
  
  File([params.usersync_start, params.usersync_stop],
       owner = params.unix_user
  )
  File(params.usersync_services_file,
    mode = 0755,
  )

def check_db_connnection():
  import params

  Logger.info('Checking DB connection')

  if params.db_flavor.lower() == 'mysql':
    cmd = format('{sql_command_invoker} -u {db_root_user} --password={db_root_password} -h {db_host}  -s -e "select version();"')
  elif params.db_flavor.lower() == 'oracle':
    cmd = format('{sql_command_invoker} {db_root_user}/{db_root_password}@{db_host} AS SYSDBA')

  try:
    Execute(cmd)
  except Fail as ex:
    Logger.info(ex)
    raise Fail('Ranger Admin installation Failed! Ranger requires DB client installed on Ranger Host, DB administrative privileges configured for connectivity from the Ranger Admin host to the configured DB host/instance and the DB server up and running on the DB host.')
