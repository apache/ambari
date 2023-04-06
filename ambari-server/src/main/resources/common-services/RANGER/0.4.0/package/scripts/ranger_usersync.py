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
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.script import Script
from resource_management.core.resources.system import Execute, File
from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions import stack_select
from resource_management.core.logger import Logger
from resource_management.core import shell
from ranger_service import ranger_service
from ambari_commons.constants import UPGRADE_TYPE_NON_ROLLING, UPGRADE_TYPE_ROLLING
from resource_management.libraries.functions.constants import Direction
import os

class RangerUsersync(Script):
  
  def install(self, env):
    self.install_packages(env)
    import params
    env.set_params(params)

    if params.stack_supports_usersync_passwd:
      from setup_ranger_xml import ranger_credential_helper
      ranger_credential_helper(params.ugsync_cred_lib, params.ugsync_policymgr_alias, 'rangerusersync', params.ugsync_policymgr_keystore)

      File(params.ugsync_policymgr_keystore,
        owner = params.unix_user,
        group = params.unix_group,
        mode = 0o640
      )

    self.configure(env)
    
  def configure(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    if params.xml_configurations_supported:
      from setup_ranger_xml import ranger
    else:
      from setup_ranger import ranger    
    
    ranger('ranger_usersync', upgrade_type=upgrade_type)
    
  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    
    self.configure(env, upgrade_type=upgrade_type)
    ranger_service('ranger_usersync')
    
  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    if upgrade_type == UPGRADE_TYPE_NON_ROLLING and params.upgrade_direction == Direction.UPGRADE:
      if params.stack_supports_usersync_non_root and os.path.isfile(params.usersync_services_file):
        File(params.usersync_services_file,
          mode = 0o755
        )
        Execute(('ln','-sf', format('{usersync_services_file}'),'/usr/bin/ranger-usersync'),
          not_if=format("ls /usr/bin/ranger-usersync"),
          only_if=format("ls {usersync_services_file}"),
          sudo=True
        )

    Execute((params.usersync_stop,), environment={'JAVA_HOME': params.java_home}, sudo=True)
    if params.stack_supports_pid:
      File(params.ranger_usersync_pid_file,
        action = "delete"
      )
    
  def status(self, env):
    import status_params
    env.set_params(status_params)

    if status_params.stack_supports_pid:
      check_process_status(status_params.ranger_usersync_pid_file)
      return

    cmd = 'ps -ef | grep proc_rangerusersync | grep -v grep'
    code, output = shell.call(cmd, timeout=20)

    if code != 0:
      Logger.debug('Ranger usersync process not running')
      raise ComponentIsNotRunning()
    pass

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    stack_select.select_packages(params.version)

  def get_log_folder(self):
    import params
    return params.usersync_log_dir
  
  def get_user(self):
    import params
    return params.unix_user

if __name__ == "__main__":
  RangerUsersync().execute()
