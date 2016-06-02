#!/usr/bin/python
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

from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.check_process_status import check_process_status

from livy_service import livy_service
from setup_livy import setup_livy

class LivyServer(Script):

  def install(self, env):
    import params
    env.set_params(params)

    self.install_packages(env)

  def configure(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    setup_livy(env, 'server', upgrade_type=upgrade_type, action = 'config')

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    self.configure(env)
    livy_service('server', upgrade_type=upgrade_type, action='start')

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    livy_service('server', upgrade_type=upgrade_type, action='stop')

  def status(self, env):
    import status_params
    env.set_params(status_params)

    check_process_status(status_params.livy_server_pid_file)


  def get_component_name(self):
    return "livy-server"

  def pre_upgrade_restart(self, env, upgrade_type=None):
    pass

if __name__ == "__main__":
  LivyServer().execute()