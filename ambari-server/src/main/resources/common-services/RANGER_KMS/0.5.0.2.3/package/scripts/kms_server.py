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
from resource_management.core.exceptions import Fail
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import upgrade_summary
from resource_management.libraries.script import Script
from resource_management.core.resources.system import Execute, File
from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.libraries.functions.format import format
from resource_management.core.logger import Logger
from resource_management.core import shell
from resource_management.libraries.functions.default import default
from kms_service import kms_service

import kms

class KmsServer(Script):

  def install(self, env):
    self.install_packages(env)
    import params
    env.set_params(params)

    # taking backup of install.properties file
    Execute(('cp', '-f', format('{kms_home}/install.properties'), format('{kms_home}/install-backup.properties')),
      not_if = format('ls {kms_home}/install-backup.properties'),
      only_if = format('ls {kms_home}/install.properties'),
      sudo = True
    )

    kms.setup_kms_db()
    self.configure(env)
    kms.setup_java_patch()

  def stop(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    kms_service(action = 'stop', upgrade_type=upgrade_type)
    if params.stack_supports_pid:
      File(params.ranger_kms_pid_file,
        action = "delete"
      )

  def start(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    self.configure(env)
    kms.enable_kms_plugin()
    kms.setup_kms_jce()
    kms.update_password_configs()
    kms_service(action = 'start', upgrade_type=upgrade_type)

  def status(self, env):
    import status_params
    env.set_params(status_params)

    if status_params.stack_supports_pid:
      check_process_status(status_params.ranger_kms_pid_file)
      return

    cmd = 'ps -ef | grep proc_rangerkms | grep -v grep'
    code, output = shell.call(cmd, timeout=20)
    if code != 0:
      Logger.debug('KMS process not running')
      raise ComponentIsNotRunning()
    pass

  def configure(self, env):
    import params

    env.set_params(params)
    kms.kms()

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    stack_select.select_packages(params.version)
    kms.kms(upgrade_type=upgrade_type)
    kms.setup_java_patch()

  def setup_ranger_kms_database(self, env):
    import params
    env.set_params(params)

    upgrade_stack = stack_select._get_upgrade_stack()
    if upgrade_stack is None:
      raise Fail('Unable to determine the stack and stack version')

    stack_version = upgrade_stack[1]
    target_version = upgrade_summary.get_target_version("RANGER_KMS", default_version = stack_version)
    Logger.info(format('Setting Ranger KMS database schema, using version {target_version}'))
    kms.setup_kms_db(stack_version = target_version)
    
  def get_log_folder(self):
    import params
    return params.kms_log_dir
  
  def get_user(self):
    import params
    return params.kms_user

if __name__ == "__main__":
  KmsServer().execute()
