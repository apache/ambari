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
from resource_management.core.exceptions import Fail
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import upgrade_summary
from resource_management.libraries.functions.constants import Direction
from resource_management.libraries.script import Script
from resource_management.core.resources.system import Execute
from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.libraries.functions.format import format
from resource_management.core.logger import Logger
from resource_management.core import shell
from ambari_commons.constants import UPGRADE_TYPE_NON_ROLLING, UPGRADE_TYPE_ROLLING
from ranger_service import ranger_service
import upgrade
import os, errno

class RangerAdmin(Script):

  upgrade_marker_file = '/tmp/rangeradmin_ru.inprogress'

  def install(self, env):
    self.install_packages(env)
    import params
    env.set_params(params)
    if params.xml_configurations_supported:
      from setup_ranger_xml import setup_ranger_db
      setup_ranger_db()

    self.configure(env)

    if params.xml_configurations_supported:
      from setup_ranger_xml import setup_java_patch
      setup_java_patch()

  def stop(self, env, upgrade_type=None):
    import params

    env.set_params(params)

    if upgrade_type == UPGRADE_TYPE_NON_ROLLING and params.upgrade_direction == Direction.UPGRADE:
      if params.stack_supports_rolling_upgrade and not params.stack_supports_config_versioning and os.path.isfile(format('{ranger_home}/ews/stop-ranger-admin.sh')):
        File(format('{ranger_home}/ews/stop-ranger-admin.sh'),
          owner=params.unix_user,
          group = params.unix_group
        )

    Execute(format('{params.ranger_stop}'), environment={'JAVA_HOME': params.java_home}, user=params.unix_user)


  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    upgrade.prestart(env, "ranger-admin")

#    if params.xml_configurations_supported:
#      from setup_ranger_xml import ranger, setup_ranger_db, setup_java_patch
#      ranger('ranger_admin', upgrade_type=upgrade_type)
#      setup_ranger_db(upgrade_type=upgrade_type)
#      setup_java_patch(upgrade_type=upgrade_type)

    self.set_ru_rangeradmin_in_progress()

  def post_upgrade_restart(self,env, upgrade_type=None):
    import params
    env.set_params(params)

    if os.path.isfile(RangerAdmin.upgrade_marker_file):
        os.remove(RangerAdmin.upgrade_marker_file) 

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env)
    ranger_service('ranger_admin')


  def status(self, env):
    cmd = 'ps -ef | grep proc_rangeradmin | grep -v grep'
    code, output = shell.call(cmd, timeout=20)

    if code != 0:
      if self.is_ru_rangeradmin_in_progress():
         Logger.info('Ranger admin process not running - skipping as rolling upgrade is in progress')
      else:
         Logger.debug('Ranger admin process not running')
         raise ComponentIsNotRunning()
    pass

  def configure(self, env):
    import params
    env.set_params(params)
    if params.xml_configurations_supported:
      from setup_ranger_xml import ranger
    else:
      from setup_ranger import ranger

    ranger('ranger_admin')

  def set_ru_rangeradmin_in_progress(self):
    config_dir = os.path.dirname(RangerAdmin.upgrade_marker_file)
    try:
       msg = "Starting Upgrade"
       if (not os.path.exists(config_dir)):
          os.makedirs(config_dir)
       ofp = open(RangerAdmin.upgrade_marker_file, 'w')
       ofp.write(msg)
       ofp.close()
    except OSError as exc:
       if exc.errno == errno.EEXIST and os.path.isdir(config_dir): 
          pass
       else:
          raise

  def is_ru_rangeradmin_in_progress(self):
    return os.path.isfile(RangerAdmin.upgrade_marker_file)

  def setup_ranger_database(self, env):
    import params
    env.set_params(params)

    upgrade_stack = stack_select._get_upgrade_stack()
    if upgrade_stack is None:
      raise Fail('Unable to determine the stack and stack version')

    stack_version = upgrade_stack[1]

    if params.xml_configurations_supported and params.upgrade_direction == Direction.UPGRADE:
      Logger.info(format('Setting Ranger database schema, using version {stack_version}'))

      from setup_ranger_xml import setup_ranger_db
      setup_ranger_db(stack_version=stack_version)

  def setup_ranger_java_patches(self, env):
    import params
    env.set_params(params)

    upgrade_stack = stack_select._get_upgrade_stack()
    if upgrade_stack is None:
      raise Fail('Unable to determine the stack and stack version')

    stack_version = upgrade_stack[1]

    if params.xml_configurations_supported and params.upgrade_direction == Direction.UPGRADE:
      Logger.info(format('Applying Ranger java patches, using version {stack_version}'))

      from setup_ranger_xml import setup_java_patch
      setup_java_patch(stack_version=stack_version)

  def set_pre_start(self, env):
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

    stack_select_packages = stack_select.get_packages(orchestration, service_name = "RANGER", component_name = "RANGER_ADMIN")
    if stack_select_packages is None:
      raise Fail("Unable to get packages for stack-select")

    Logger.info("RANGER_ADMIN component will be stack-selected to version {0} using a {1} orchestration".format(params.version, orchestration.upper()))

    for stack_select_package_name in stack_select_packages:
      stack_select.select(stack_select_package_name, params.version)

  def get_log_folder(self):
    import params
    return params.admin_log_dir
  
  def get_user(self):
    import params
    return params.unix_user

if __name__ == "__main__":
  RangerAdmin().execute()

