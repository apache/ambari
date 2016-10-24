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
from resource_management import Script
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.check_process_status import check_process_status

from resource_management.libraries.functions.show_logs import show_logs
from druid import druid, get_daemon_cmd, getPid


class DruidBase(Script):
  def __init__(self, nodeType=None):
    self.nodeType = nodeType

  def get_component_name(self):
    node_type_lower = self.nodeType.lower()
    return format("druid-{node_type_lower}")

  def install(self, env):
    self.install_packages(env)

  def configure(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    druid(upgrade_type=upgrade_type, nodeType=self.nodeType)

  def pre_upgrade_restart(self, env, upgrade_type=None):
    return

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env, upgrade_type=upgrade_type)
    daemon_cmd = get_daemon_cmd(params, self.nodeType, "start")
    try:
      Execute(daemon_cmd,
              user=params.druid_user
              )
    except:
      show_logs(params.druid_log_dir, params.druid_user)
      raise

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    daemon_cmd = get_daemon_cmd(params, self.nodeType, "stop")
    try:
      Execute(daemon_cmd,
              user=params.druid_user
              )
    except:
      show_logs(params.druid_log_dir, params.druid_user)
      raise

  def status(self, env):
    import status_params
    env.set_params(status_params)
    pid_file = getPid(status_params, self.nodeType)
    check_process_status(pid_file)

  def get_log_folder(self):
    import params
    return params.druid_log_dir

  def get_user(self):
    import params
    return params.druid_user
