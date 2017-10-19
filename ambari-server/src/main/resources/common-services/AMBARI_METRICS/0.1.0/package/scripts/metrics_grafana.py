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
from metrics_grafana_util import create_ams_datasource, create_ams_dashboards, create_grafana_admin_pwd
from resource_management.core.logger import Logger
from resource_management.core import sudo

class AmsGrafana(Script):
  def install(self, env):
    import params
    env.set_params(params)
    self.install_packages(env)
    self.configure(env) # for security

  def configure(self, env, action = None):
    import params
    env.set_params(params)
    ams(name='grafana', action=action)

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env, action = 'start')

    start_cmd = format("{ams_grafana_script} start")
    Execute(start_cmd,
            user=params.ams_user,
            not_if = params.grafana_process_exists_cmd,
            )
    pidfile = format("{ams_grafana_pid_dir}/grafana-server.pid")
    if not sudo.path_exists(pidfile):
      Logger.warning("Pid file doesn't exist after starting of the component.")
    else:
      Logger.info("Grafana Server has started with pid: {0}".format(sudo.read_file(pidfile).strip()))

    #Set Grafana admin pwd
    create_grafana_admin_pwd()
    # Create datasource
    create_ams_datasource()
    # Create pre-built dashboards
    create_ams_dashboards()

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env, action = 'stop')
    Execute((format("{ams_grafana_script}"), 'stop'),
            sudo=True,
            only_if = params.grafana_process_exists_cmd,
            )

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_service_status(env, name='grafana')

  def get_pid_files(self):
    import status_params
    return [status_params.grafana_pid_file]

if __name__ == "__main__":
  AmsGrafana().execute()
