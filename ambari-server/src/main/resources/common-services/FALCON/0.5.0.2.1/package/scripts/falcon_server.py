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

import falcon_server_upgrade

from resource_management.core.logger import Logger
from resource_management.libraries.script import Script
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import check_process_status
from resource_management.libraries.functions.security_commons import build_expectations
from resource_management.libraries.functions.security_commons import cached_kinit_executor
from resource_management.libraries.functions.security_commons import get_params_from_filesystem
from resource_management.libraries.functions.security_commons import validate_security_config_properties
from resource_management.libraries.functions.security_commons import FILE_TYPE_PROPERTIES
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.constants import StackFeature, Direction

from falcon import falcon
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl


class FalconServer(Script):
  def configure(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    falcon('server', action='config', upgrade_type=upgrade_type)

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env, upgrade_type=upgrade_type)
    falcon('server', action='start', upgrade_type=upgrade_type)

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    falcon('server', action='stop', upgrade_type=upgrade_type)

    # if performing an upgrade (ROLLING / NON_ROLLING), backup some directories after stopping falcon
    if upgrade_type is not None:
      falcon_server_upgrade.post_stop_backup()

@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class FalconServerLinux(FalconServer):
  def install(self, env):
    import params
    self.install_packages(env)
    env.set_params(params)

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.server_pid_file)

  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Stack Upgrade pre-restart")
    import params
    env.set_params(params)

    # this function should not execute if the version can't be determined or
    # the stack does not support rolling upgrade
    if not (params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version)):
      return

    Logger.info("Executing Falcon Server Stack Upgrade pre-restart")
    stack_select.select_packages(params.version)

    falcon_server_upgrade.pre_start_restore()

  def get_log_folder(self):
    import params
    return params.falcon_log_dir
  
  def get_user(self):
    import params
    return params.falcon_user

  def get_pid_files(self):
    import status_params
    return [status_params.server_pid_file]

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class FalconServerWindows(FalconServer):

  def install(self, env):
    import params
    from resource_management.libraries.functions.windows_service_utils import check_windows_service_exists

    if not check_windows_service_exists(params.falcon_win_service_name):
      self.install_packages(env)

  def status(self, env):
    import status_params
    from resource_management.libraries.functions.windows_service_utils import check_windows_service_status

    check_windows_service_status(status_params.falcon_win_service_name)

if __name__ == "__main__":
  FalconServer().execute()
