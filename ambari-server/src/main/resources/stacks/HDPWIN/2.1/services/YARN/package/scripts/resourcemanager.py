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

from resource_management import *
from yarn import yarn
import service_mapping

class Resourcemanager(Script):

  def install(self, env):
    import params
    if not check_windows_service_exists(service_mapping.resourcemanager_win_service_name):
      self.install_packages(env)
    self.configure(env)

  def configure(self, env):
    import params
    env.set_params(params)
    yarn()

  def start(self, env):
    import params
    env.set_params(params)
    self.configure(env)
    Service(service_mapping.resourcemanager_win_service_name, action="start")

  def stop(self, env):
    import params
    env.set_params(params)
    Service(service_mapping.resourcemanager_win_service_name, action="stop")

  def status(self, env):
    check_windows_service_status(service_mapping.resourcemanager_win_service_name)

  def refreshqueues(self, env):
    pass

  def decommission(self, env):
      import params

      env.set_params(params)
      yarn_user = params.yarn_user

      yarn_refresh_cmd = format("cmd /c yarn rmadmin -refreshNodes")

      File(params.exclude_file_path,
           content=Template("exclude_hosts_list.j2"),
           owner=yarn_user,
           mode="f"
      )

      if params.update_exclude_file_only == False:
          Execute(yarn_refresh_cmd,
                  user=yarn_user)
          pass
      pass

if __name__ == "__main__":
  Resourcemanager().execute()
