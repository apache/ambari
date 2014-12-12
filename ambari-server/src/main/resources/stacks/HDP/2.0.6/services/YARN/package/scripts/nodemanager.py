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

from resource_management import *
from resource_management.libraries.functions.version import compare_versions, format_hdp_stack_version
from resource_management.libraries.functions.format import format

from yarn import yarn
from service import service

class Nodemanager(Script):
  def install(self, env):
    self.install_packages(env)

  def configure(self, env):
    import params
    env.set_params(params)
    yarn(name="nodemanager")

  def pre_rolling_restart(self, env):
    Logger.info("Executing NodeManager Rolling Upgrade pre-restart")
    import params
    env.set_params(params)

    if params.version and compare_versions(format_hdp_stack_version(params.version), '2.2.0.0') >= 0:
      Execute(format("hdp-select set hadoop-yarn-nodemanager {version}"))

  def start(self, env, rolling_restart=False):
    import params
    env.set_params(params)
    self.configure(env) # FOR SECURITY
    service('nodemanager',action='start')

  def post_rolling_restart(self, env):
    Logger.info("Executing NodeManager Rolling Upgrade post-restart")
    import params
    env.set_params(params)

    nodemanager_upgrade.post_upgrade_check()

  def stop(self, env, rolling_restart=False):
    import params
    env.set_params(params)

    service('nodemanager',action='stop')

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.nodemanager_pid_file)

if __name__ == "__main__":
  Nodemanager().execute()
