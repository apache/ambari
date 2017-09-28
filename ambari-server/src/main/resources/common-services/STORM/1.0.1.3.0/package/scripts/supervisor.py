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
from resource_management.libraries.functions import check_process_status
from resource_management.libraries.script import Script
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import format
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions import StackFeature
from storm import storm
from service import service
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyImpl
from resource_management.core.resources.service import Service


class Supervisor(Script):
  def install(self, env):
    self.install_packages(env)
    self.configure(env)

  def configure(self, env):
    import params
    env.set_params(params)
    storm("supervisor")


@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class SupervisorWindows(Supervisor):
  def start(self, env):
    import status_params
    env.set_params(status_params)
    self.configure(env)
    Service(status_params.supervisor_win_service_name, action="start")

  def stop(self, env):
    import status_params
    env.set_params(status_params)
    Service(status_params.supervisor_win_service_name, action="stop")

  def status(self, env):
    import status_params
    from resource_management.libraries.functions.windows_service_utils import check_windows_service_status
    env.set_params(status_params)
    check_windows_service_status(status_params.supervisor_win_service_name)


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class SupervisorDefault(Supervisor):

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version):
      stack_select.select_packages(params.version)

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env)

    service("supervisor", action="start")
    service("logviewer", action="start")

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    service("supervisor", action="stop")
    service("logviewer", action="stop")

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.pid_supervisor)

  def get_log_folder(self):
    import params
    return params.log_dir
  
  def get_user(self):
    import params
    return params.storm_user

  def get_pid_files(self):
    import status_params
    return [status_params.pid_supervisor]

if __name__ == "__main__":
  Supervisor().execute()

