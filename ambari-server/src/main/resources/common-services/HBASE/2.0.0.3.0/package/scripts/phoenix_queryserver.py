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

from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.script import Script
from phoenix_service import phoenix_service
from hbase import hbase

# Note: Phoenix Query Server is only applicable to stack version supporting Phoenix.
class PhoenixQueryServer(Script):

  def install(self, env):
    import params
    env.set_params(params)
    self.install_packages(env)


  def configure(self, env):
    import params
    env.set_params(params)
    hbase(name='queryserver')


  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env)
    phoenix_service('start')


  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    phoenix_service('stop')


  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    if params.stack_version_formatted and check_stack_feature(StackFeature.PHOENIX, params.stack_version_formatted):     
      # phoenix uses hbase configs
      stack_select.select_packages(params.version)


  def status(self, env):
    import status_params
    env.set_params(status_params)
    phoenix_service('status')


  def security_status(self, env):
    self.put_structured_out({"securityState": "UNSECURED"})
    
  def get_log_folder(self):
    import params
    return params.log_dir
  
  def get_user(self):
    import params
    return params.hbase_user

  def get_pid_files(self):
    import status_params
    return [status_params.phoenix_pid_file]

if __name__ == "__main__":
  PhoenixQueryServer().execute()