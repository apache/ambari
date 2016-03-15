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
from metrics_grafana_util import create_ams_datasource, create_ams_dashboards

class AmsGrafana(Script):
  def install(self, env):
    self.install_packages(env, exclude_packages = ['ambari-metrics-collector'])

  def configure(self, env, action = None):
    import params
    env.set_params(params)
    ams(name='grafana', action=action)

  def start(self, env):
    import params
    env.set_params(params)
    self.configure(env, action = 'start')

    start_cmd = format("{ams_grafana_script} start")
    Execute(start_cmd,
            user=params.ams_user
            )
    # Create datasource
    create_ams_datasource()
    # Create pre-built dashboards
    create_ams_dashboards()

  def stop(self, env):
    import params
    env.set_params(params)
    self.configure(env, action = 'stop')
    Execute((format("{ams_grafana_script}"), 'stop'),
            sudo=True
            )

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_service_status(name='grafana')

if __name__ == "__main__":
  AmsGrafana().execute()
