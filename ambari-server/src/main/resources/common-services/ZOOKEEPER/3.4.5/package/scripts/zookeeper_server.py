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
import random

from ambari_commons.constants import UPGRADE_TYPE_NON_ROLLING

from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import get_unique_id_and_date
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, \
  FILE_TYPE_JAAS_CONF
from resource_management.core import shell
from resource_management.core.logger import Logger
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.validate import call_and_match_output
from zookeeper import zookeeper
from zookeeper_service import zookeeper_service
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyImpl


class ZookeeperServer(Script):

  def configure(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    zookeeper(type='server', upgrade_type=upgrade_type)

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env, upgrade_type=upgrade_type)
    zookeeper_service(action='start', upgrade_type=upgrade_type)

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    zookeeper_service(action='stop', upgrade_type=upgrade_type)

@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class ZookeeperServerLinux(ZookeeperServer):

  def install(self, env):
    self.install_packages(env)
    self.configure(env)

  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Stack Upgrade pre-restart")
    import params
    env.set_params(params)

    if check_stack_feature(StackFeature.ROLLING_UPGRADE, format_stack_version(params.version)):
      stack_select.select_packages(params.version)

  def post_upgrade_restart(self, env, upgrade_type=None):
    # during an express upgrade, there is no quorum, so don't try to perform the check
    if upgrade_type == UPGRADE_TYPE_NON_ROLLING:
      return

    Logger.info("Executing Stack Upgrade post-restart")
    import params
    env.set_params(params)
    zk_server_host = random.choice(params.zookeeper_hosts)
    cli_shell = format("{zk_cli_shell} -server {zk_server_host}:{client_port}")
    # Ensure that a quorum is still formed.
    unique = get_unique_id_and_date()
    create_command = format("echo 'create /{unique} mydata' | {cli_shell}")
    list_command = format("echo 'ls /' | {cli_shell}")
    delete_command = format("echo 'delete /{unique} ' | {cli_shell}")

    quorum_err_message = "Failed to establish zookeeper quorum"
    call_and_match_output(create_command, 'Created', quorum_err_message, user=params.zk_user)
    call_and_match_output(list_command, r"\[.*?" + unique + ".*?\]", quorum_err_message, user=params.zk_user)
    shell.call(delete_command, user=params.zk_user)

    if params.client_port:
      check_leader_command = format("echo stat | nc localhost {client_port} | grep Mode")
      code, out = shell.call(check_leader_command, logoutput=False)
      if code == 0 and out:
        Logger.info(out)

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.zk_pid_file)
      
  def get_log_folder(self):
    import params
    return params.zk_log_dir
  
  def get_user(self):
    import params
    return params.zk_user

  def get_pid_files(self):
    import status_params
    return [status_params.zk_pid_file]


@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class ZookeeperServerWindows(ZookeeperServer):
  def install(self, env):
    from resource_management.libraries.functions.windows_service_utils import check_windows_service_exists
    import params
    if not check_windows_service_exists(params.zookeeper_win_service_name):
      self.install_packages(env)
    self.configure(env)

  def status(self, env):
    from resource_management.libraries.functions.windows_service_utils import check_windows_service_status
    import status_params
    check_windows_service_status(status_params.zookeeper_win_service_name)

if __name__ == "__main__":
  ZookeeperServer().execute()
