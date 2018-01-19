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
from resource_management.libraries.script import Script
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import upgrade_summary
from resource_management.core.resources.system import Execute, File
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.libraries.functions.format import format
from resource_management.core.logger import Logger
from resource_management.core import shell
from ranger_service import ranger_service
from setup_ranger_xml import ranger, ranger_credential_helper
from resource_management.core.exceptions import Fail
import upgrade

class RangerTagsync(Script):

  def install(self, env):
    self.install_packages(env)
    import params
    env.set_params(params)

    ranger_credential_helper(params.tagsync_cred_lib, 'tagadmin.user.password', 'rangertagsync', params.tagsync_jceks_path)
    File(params.tagsync_jceks_path,
       owner = params.unix_user,
       group = params.unix_group,
       mode = 0640
    )
    if params.stack_supports_ranger_tagsync_ssl_xml_support:
      Logger.info("Stack support Atlas user for Tagsync, creating keystore for same.")
      self.create_atlas_user_keystore(env)
    else:
      Logger.info("Stack does not support Atlas user for Tagsync, skipping keystore creation for same.")

    self.configure(env)

  def configure(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    ranger('ranger_tagsync', upgrade_type=upgrade_type)

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    self.configure(env, upgrade_type=upgrade_type)
    ranger_service('ranger_tagsync')

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    Execute(format('{tagsync_services_file} stop'), environment={'JAVA_HOME': params.java_home}, user=params.unix_user)
    File(params.tagsync_pid_file,
      action = "delete"
    )

  def status(self, env):
    import status_params
    env.set_params(status_params)

    check_process_status(status_params.tagsync_pid_file)

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    if params.stack_supports_ranger_tagsync:
      Logger.info("Executing Ranger Tagsync Stack Upgrade pre-restart")
      stack_select.select_packages(params.version)


  def get_log_folder(self):
    import params
    return params.tagsync_log_dir

  def get_user(self):
    import params
    return params.unix_user

  def get_pid_files(self):
    import status_params
    return [status_params.tagsync_pid_file]

  def configure_atlas_user_for_tagsync(self, env):
    Logger.info("Configuring Atlas user for Tagsync service.")
    import params
    env.set_params(params)

    orchestration = stack_select.PACKAGE_SCOPE_STANDARD
    summary = upgrade_summary.get_upgrade_summary()

    if summary is not None:
      orchestration = summary.orchestration
      if orchestration is None:
        raise Fail("The upgrade summary does not contain an orchestration type")

      if orchestration.upper() in stack_select._PARTIAL_ORCHESTRATION_SCOPES:
        orchestration = stack_select.PACKAGE_SCOPE_PATCH

    stack_select_packages = stack_select.get_packages(orchestration, service_name = "RANGER", component_name = "RANGER_TAGSYNC")
    if stack_select_packages is None:
      raise Fail("Unable to get packages for stack-select")

    Logger.info("RANGER_TAGSYNC component will be stack-selected to version {0} using a {1} orchestration".format(params.version, orchestration.upper()))

    for stack_select_package_name in stack_select_packages:
      stack_select.select(stack_select_package_name, params.version)

    if params.stack_supports_ranger_tagsync_ssl_xml_support:
      Logger.info("Upgrading Tagsync, stack support Atlas user for Tagsync, creating keystore for same.")
      self.create_atlas_user_keystore(env)
    else:
      Logger.info("Upgrading Tagsync, stack does not support Atlas user for Tagsync, skipping keystore creation for same.")

    Logger.info("Configuring Atlas user for Tagsync service done.")

  def create_atlas_user_keystore(self,env):
    import params
    env.set_params(params)
    ranger_credential_helper(params.tagsync_cred_lib, 'atlas.user.password', 'admin', params.atlas_tagsync_jceks_path)
    File(params.atlas_tagsync_jceks_path,
         owner = params.unix_user,
         group = params.unix_group,
         mode = 0640
    )

if __name__ == "__main__":
  RangerTagsync().execute()
