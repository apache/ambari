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

from resource_management.libraries.script.script import Script
from ams import ams
from ams_service import ams_service
from status import check_service_status

class AmsMonitor(Script):
  def install(self, env):
    import params
    env.set_params(params)
    self.install_packages(env)
    self.configure(env) # for security

  def configure(self, env):
    import params
    env.set_params(params)
    ams(name='monitor')

  def start(self, env, upgrade_type=None):
    self.configure(env) # for security

    ams_service( 'monitor',
                 action = 'start'
    )

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    ams_service( 'monitor',
                 action = 'stop'
    )

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_service_status(env, name='monitor')
    
  def get_log_folder(self):
    import params
    return params.ams_monitor_log_dir

  def get_pid_files(self):
    import status_params
    return [status_params.monitor_pid_file]

  def get_user(self):
    import params
    return params.ams_user


if __name__ == "__main__":
  AmsMonitor().execute()

