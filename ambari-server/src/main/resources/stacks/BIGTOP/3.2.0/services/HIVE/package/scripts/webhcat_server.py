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
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.constants import StackFeature
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, \
  FILE_TYPE_XML
from resource_management.core.logger import Logger
from webhcat import webhcat
from webhcat_service import webhcat_service
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyImpl


class WebHCatServer(Script):
  def install(self, env):
    import params
    self.install_packages(env)

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env) # FOR SECURITY
    webhcat_service(action='start', upgrade_type=upgrade_type)

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    webhcat_service(action='stop')

  def configure(self, env):
    import params
    env.set_params(params)
    webhcat()


  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.webhcat_pid_file)

  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing WebHCat Stack Upgrade pre-restart")
    import params
    env.set_params(params)

    if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version): 
      stack_select.select_packages(params.version)

  def get_log_folder(self):
    import params
    return params.hcat_log_dir
  
  def get_user(self):
    import params
    return params.webhcat_user

  def get_pid_files(self):
    import status_params
    return [status_params.webhcat_pid_file]

if __name__ == "__main__":
  WebHCatServer().execute()
