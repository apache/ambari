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
import re

from resource_management import *
from resource_management.libraries.functions.decorator import retry
from resource_management.libraries.functions.version import compare_versions, format_hdp_stack_version
from resource_management.libraries.functions.format import format
from resource_management.core.shell import call

from yarn import yarn
from service import service


@retry(times=10, sleep_time=2, err_class=Fail)
def call_and_match_output(command, regex_expression, err_message):
  """
  Call the command and performs a regex match on the output for the specified expression.
  :param command: Command to call
  :param regex_expression: Regex expression to search in the output
  """
  # TODO Rolling Upgrade, does this work in Ubuntu? If it doesn't see dynamic_variable_interpretation.py to see how stdout was redirected
  # to a temporary file, which was then read.
  code, out = call(command, verbose=True)
  if not (out and re.search(regex_expression, out, re.IGNORECASE)):
    raise Fail(err_message)


class Nodemanager(Script):
  def install(self, env):
    self.install_packages(env)

  def configure(self, env):
    import params
    env.set_params(params)
    yarn(name="nodemanager")

  def pre_rolling_restart(self, env):
    Logger.info("Executing Rolling Upgrade post-restart")
    import params
    env.set_params(params)

    if params.version and compare_versions(format_hdp_stack_version(params.version), '2.2.0.0') >= 0:
      Execute(format("hdp-select set hadoop-yarn-nodemanager {version}"))

  def start(self, env, rolling_restart=False):
    import params
    env.set_params(params)
    self.configure(env) # FOR SECURITY
    service('nodemanager',
            action='start'
    )

  def post_rolling_restart(self, env):
    Logger.info("Executing Rolling Upgrade post-restart")
    import params
    env.set_params(params)

    nm_status_command = format("yarn node -status {nm_address}")
    call_and_match_output(nm_status_command, 'Node-State : RUNNING',  "Failed to check NodeManager status")

  def stop(self, env, rolling_restart=False):
    import params
    env.set_params(params)

    service('nodemanager',
            action='stop'
    )

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.nodemanager_pid_file)

if __name__ == "__main__":
  Nodemanager().execute()
