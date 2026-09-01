#!/usr/bin/env python3
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

Ambari Agent

"""

import nodemanager_upgrade

from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.constants import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.core.logger import Logger
from yarn import yarn
from service import service
from ambari_commons.os_family_impl import OsFamilyImpl
import yarn_process_utils


class Nodemanager(Script):
  def install(self, env):
    self.install_packages(env)

  def stop(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    service("nodemanager", action="stop")

  def start(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    self.configure(env)  # FOR SECURITY
    service("nodemanager", action="start")

  def configure(self, env):
    import params

    env.set_params(params)
    yarn(name="nodemanager")


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class NodemanagerDefault(Nodemanager):
  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing NodeManager Stack Upgrade pre-restart")
    import params

    env.set_params(params)

    if params.version and check_stack_feature(
      StackFeature.ROLLING_UPGRADE, params.version
    ):
      stack_select.select_packages(params.version)

  def post_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing NodeManager Stack Upgrade post-restart")
    import params

    env.set_params(params)

    nodemanager_upgrade.post_upgrade_check()

  def status(self, env):
    import status_params

    env.set_params(status_params)
    yarn_process_utils.check_component_status(
      status_params.nodemanager_pid_file,
      status_params.yarn_user,
      "nodemanager",
      status_params.yarn_user,
      status_params.user_group,
    )

  def get_log_folder(self):
    import params

    return params.yarn_log_dir

  def get_user(self):
    import params

    return params.yarn_user

  def get_pid_files(self):
    import status_params

    return [status_params.nodemanager_pid_file]


if __name__ == "__main__":
  Nodemanager().execute()
