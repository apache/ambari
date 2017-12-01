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
from resource_management import Script, Execute
from resource_management.libraries.functions import format
from status import check_service_status
from ams import ams
from resource_management.core.logger import Logger
from resource_management.core import sudo

class AmsADManager(Script):
  def install(self, env):
    import params
    env.set_params(params)
    self.install_packages(env)
    self.configure(env) # for security

  def configure(self, env, action = None):
    import params
    env.set_params(params)
    ams(name='admanager', action=action)

  def start(self, env):
    import params
    env.set_params(params)
    self.configure(env, action = 'start')

    start_cmd = format("{ams_admanager_script} start")
    Execute(start_cmd,
            user=params.ams_user
            )
    pidfile = format("{ams_ad_pid_dir}/ambari-metrics-admanager.pid")
    if not sudo.path_exists(pidfile):
      Logger.warning("Pid file doesn't exist after starting of the component.")
    else:
      Logger.info("AD Manager Server has started with pid: {0}".format(sudo.read_file(pidfile).strip()))


  def stop(self, env):
    import params
    env.set_params(params)
    self.configure(env, action = 'stop')
    Execute((format("{ams_admanager_script}"), 'stop'),
            user=params.ams_user
            )

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_service_status(env, name='admanager')

  def get_pid_files(self):
    import status_params
    return [status_params.ams_ad_pid_file]

if __name__ == "__main__":
  AmsADManager().execute()
