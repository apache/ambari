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
from resource_management.core.logger import Logger
from resource_management.libraries.functions import format
from resource_management.libraries.functions import check_process_status
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.security_commons import build_expectations
from resource_management.libraries.functions.security_commons import cached_kinit_executor
from resource_management.libraries.functions.security_commons import get_params_from_filesystem
from resource_management.libraries.functions.security_commons import validate_security_config_properties
from resource_management.libraries.functions.security_commons import FILE_TYPE_XML
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions import StackFeature

from accumulo_configuration import setup_conf_dir
from accumulo_service import accumulo_service

class AccumuloScript(Script):

  def __init__(self, component):
    Script.__init__(self)
    self.component = component


  def install(self, env):
    self.install_packages(env)


  def configure(self, env):
    import params
    env.set_params(params)

    setup_conf_dir(name=self.component)


  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env) # for security

    accumulo_service( self.component, action = 'start')


  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    accumulo_service( self.component, action = 'stop')


  def status(self, env):
    import status_params
    env.set_params(status_params)

    pid_file = self.get_pid_files()[0]
    check_process_status(pid_file)


  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    # this function should not execute if the version can't be determined or
    # the stack does not support rolling upgrade
    if not (params.stack_version_formatted and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.stack_version_formatted)):
      return

    stack_component = stack_select.get_package_name()

    Logger.info("Executing Accumulo Upgrade pre-restart for {0}".format(stack_component))
    stack_select.select_packages(params.version)

  def get_log_folder(self):
    import params
    return params.log_dir

  def get_pid_files(self):
    import status_params

    pid_file = "{pid_dir}/accumulo-{accumulo_user}-{component}.pid".format(
        pid_dir = status_params.pid_dir,
        accumulo_user = status_params.accumulo_user,
        component = self.component)
    return [pid_file]

  def get_user(self):
    import params
    return params.accumulo_user

if __name__ == "__main__":
  AccumuloScript().fail_with_error('component unspecified')
