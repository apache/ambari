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
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties,\
  FILE_TYPE_XML
from resource_management.libraries.functions.format import format
from yarn import yarn
from service import service
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyImpl


class ApplicationTimelineServer(Script):
  def install(self, env):
    self.install_packages(env)

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env) # FOR SECURITY
    service('timelineserver', action='start')

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    service('timelineserver', action='stop')

  def configure(self, env):
    import params
    env.set_params(params)
    yarn(name='apptimelineserver')


@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class ApplicationTimelineServerWindows(ApplicationTimelineServer):
  def status(self, env):
    service('timelineserver', action='status')


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class ApplicationTimelineServerDefault(ApplicationTimelineServer):
  def get_component_name(self):
    return "hadoop-yarn-timelineserver"

  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Stack Upgrade pre-restart")
    import params
    env.set_params(params)

    if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version):
      conf_select.select(params.stack_name, "hadoop", params.version)
      stack_select.select("hadoop-yarn-timelineserver", params.version)

  def status(self, env):
    import status_params
    env.set_params(status_params)
    Execute(format("mv {yarn_historyserver_pid_file_old} {yarn_historyserver_pid_file}"),
            only_if = format("test -e {yarn_historyserver_pid_file_old}", user=status_params.yarn_user))
    functions.check_process_status(status_params.yarn_historyserver_pid_file)

  def get_log_folder(self):
    import params
    return params.yarn_log_dir
  
  def get_user(self):
    import params
    return params.yarn_user

if __name__ == "__main__":
  ApplicationTimelineServer().execute()
