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

from resource_management import *
from resource_management.libraries.functions.version import *
from falcon import falcon

class FalconServer(Script):
  def install(self, env):
    import params

    self.install_packages(env)
    env.set_params(params)


  def start(self, env, rolling_restart=False):
    import params

    env.set_params(params)
    self.configure(env)

    falcon('server', action='start')


  def stop(self, env, rolling_restart=False):
    import params

    env.set_params(params)

    falcon('server', action='stop')

    # if performing an upgrade, backup some directories after stopping falcon
    if rolling_restart:
      falcon_server_upgrade.post_stop_backup()


  def configure(self, env):
    import params

    env.set_params(params)

    falcon('server', action='config')


  def status(self, env):
    import status_params

    env.set_params(status_params)
    check_process_status(status_params.server_pid_file)


  def pre_rolling_restart(self, env):
    import params
    env.set_params(params)

    # this function should not execute if the version can't be determined or
    # is not at least HDP 2.2.0.0
    if not params.version or compare_versions(format_hdp_stack_version(params.version), '2.2.0.0') < 0:
      return

    Logger.info("Executing Falcon Server Rolling Upgrade pre-restart")
    Execute(format("hdp-select set falcon-server {version}"))
    falcon_server_upgrade.pre_start_restore()


if __name__ == "__main__":
  FalconServer().execute()
