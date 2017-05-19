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
from resource_management import *

from yarn import yarn
from service import service

class HistoryServer(Script):
  def install(self, env):
    self.install_packages(env)

  def configure(self, env):
    import params
    env.set_params(params)
    yarn(name="historyserver")

  def start(self, env):
    import params
    env.set_params(params)
    self.configure(env) # FOR SECURITY
    service('historyserver', action='start', serviceName='mapreduce')

  def stop(self, env):
    import params
    env.set_params(params)
    service('historyserver', action='stop', serviceName='mapreduce')

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.mapred_historyserver_pid_file)

if __name__ == "__main__":
  HistoryServer().execute()
