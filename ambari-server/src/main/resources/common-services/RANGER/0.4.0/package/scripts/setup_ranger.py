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

def ranger(name=None, upgrade_type=None):
  if name == 'ranger_admin':
    setup_ranger_admin(upgrade_type=upgrade_type)

  if name == 'ranger_usersync':
    setup_usersync(upgrade_type=upgrade_type)

def setup_ranger_admin(upgrade_type=None):
  import params

  check_db_connnection()
  
  File(params.downloaded_custom_connector,
      content = DownloadSource(params.driver_curl_source),
      mode = 0644
  )

  Execute(('cp', '--remove-destination', params.downloaded_custom_connector, params.driver_curl_target),
          path=["/bin", "/usr/bin/"],
          sudo=True)

  File(params.driver_curl_target, mode=0644)

  ModifyPropertiesFile(format("{ranger_home}/install.properties"),
    properties = params.config['configurations']['admin-properties']
  )

  custom_config = dict()
  custom_config['unix_user'] = params.unix_user
  custom_config['unix_group'] = params.unix_group

  ModifyPropertiesFile(format("{ranger_home}/install.properties"),
    properties=custom_config
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

  Directory(params.admin_log_dir,
    owner = params.unix_user,
    group = params.unix_group
  )

def setup_usersync(upgrade_type=None):
  import params

  PropertiesFile(format("{usersync_home}/install.properties"),
    properties = params.config['configurations']['usersync-properties'],
  )

  custom_config = dict()
  custom_config['unix_user'] = params.unix_user
  custom_config['unix_group'] = params.unix_group

  ModifyPropertiesFile(format("{usersync_home}/install.properties"),
    properties=custom_config
  )

  cmd = format("cd {usersync_home} && ") + as_sudo([format('{usersync_home}/setup.sh')])
  Execute(cmd, environment={'JAVA_HOME': params.java_home}, logoutput=True)
  
  File([params.usersync_start, params.usersync_stop],
       owner = params.unix_user
  )
  File(params.usersync_services_file,
    mode = 0755,
  )

  Directory(params.usersync_log_dir,
    owner = params.unix_user,
    group = params.unix_group
  )

def check_db_connnection():
  import params

  Logger.info('Checking DB connection')
  env_dict = {}
  if params.db_flavor.lower() == 'mysql':
    cmd = format('{sql_command_invoker} -u {db_root_user} --password={db_root_password!p} -h {db_host}  -s -e "select version();"')
  elif params.db_flavor.lower() == 'oracle':
    cmd = format("{sql_command_invoker} '{db_root_user}/\"{db_root_password}\"@{db_host}' AS SYSDBA")
    env_dict = {'ORACLE_HOME':params.oracle_home, 'LD_LIBRARY_PATH':params.oracle_home}
  elif params.db_flavor.lower() == 'postgres':
    cmd = 'true'
  elif params.db_flavor.lower() == 'mssql':
    cmd = 'true'

  try:
    Execute(cmd,
      environment=env_dict,
      logoutput=True)
  except Fail as ex:
    Logger.error(str(ex))
    raise Fail('Ranger Database connection check failed')
