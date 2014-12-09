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

import sys
import re
from resource_management import *
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions import get_unique_id_and_date
from resource_management.libraries.functions.decorator import retry
from resource_management.libraries.functions.version import compare_versions, format_hdp_stack_version
from resource_management.core.shell import call


from zookeeper import zookeeper
from zookeeper_service import zookeeper_service

@retry(times=10, sleep_time=2, err_class=Fail)
def call_and_match_output(command, regex_expression, err_message):
  """
  Call the command and performs a regex match on the output for the specified expression.
  :param command: Command to call
  :param regex_expression: Regex expression to search in the output
  """
  code, out = call(command, verbose=True)
  if not (out and re.search(regex_expression, out)):
    raise Fail(err_message)


class ZookeeperServer(Script):
  def install(self, env):
    self.install_packages(env)
    self.configure(env)

  def configure(self, env):
    import params
    env.set_params(params)
    zookeeper(type='server')

  def pre_rolling_restart(self, env):
    Logger.info("Executing Rolling Upgrade pre-restart")
    import params
    env.set_params(params)

    version = default("/commandParams/version", None)
    if version and compare_versions(format_hdp_stack_version(version), '2.2.0.0') >= 0:
      Execute(format("hdp-select set zookeeper-server {version}"))

  def start(self, env):
    import params
    env.set_params(params)
    self.configure(env)
    zookeeper_service(action = 'start')

  def post_rolling_restart(self, env):
    Logger.info("Executing Rolling Upgrade post-restart")
    import params
    env.set_params(params)

    # Ensure that a quorum is still formed.
    unique = get_unique_id_and_date()
    create_command = format("echo 'create /{unique} mydata' | {zk_cli_shell}")
    list_command = format("echo 'ls /' | {zk_cli_shell}")
    delete_command = format("echo 'delete /{unique} ' | {zk_cli_shell}")

    quorum_err_message = "Failed to establish zookeeper quorum"
    call_and_match_output(create_command, 'Created', quorum_err_message)
    call_and_match_output(list_command, r"\[.*?" + unique + ".*?\]", quorum_err_message)
    call(delete_command)

  def stop(self, env):
    import params
    env.set_params(params)
    zookeeper_service(action = 'stop')

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.zk_pid_file)

if __name__ == "__main__":
  ZookeeperServer().execute()
