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
from resource_management.libraries.functions import hdp_select
from resource_management.libraries.script import Script
from resource_management.core.resources.system import Execute
from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.libraries.functions.format import format
from resource_management.core.logger import Logger
from resource_management.core import shell
from ranger_service import ranger_service
import upgrade
import os, errno

class RangerAdmin(Script):

  def get_stack_to_component(self):
    return {"HDP": "ranger-admin"}

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
    Execute(format('{params.ranger_stop}'), environment={'JAVA_HOME': params.java_home}, user=params.unix_user)


  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    upgrade.prestart(env, "ranger-admin")

    self.set_ru_rangeradmin_in_progress(params.upgrade_marker_file)

  def post_upgrade_restart(self,env, upgrade_type=None):
    import params
    env.set_params(params)

    if os.path.isfile(params.upgrade_marker_file):
      os.remove(params.upgrade_marker_file)

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env, upgrade_type=upgrade_type)
    ranger_service('ranger_admin')


  def status(self, env):
    import status_params

    env.set_params(status_params)
    cmd = 'ps -ef | grep proc_rangeradmin | grep -v grep'
    code, output = shell.call(cmd, timeout=20)

    if code != 0:
      if self.is_ru_rangeradmin_in_progress(status_params.upgrade_marker_file):
        Logger.info('Ranger admin process not running - skipping as stack upgrade is in progress')
      else:
        Logger.debug('Ranger admin process not running')
        raise ComponentIsNotRunning()
    pass

  def configure(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    if params.xml_configurations_supported:
      from setup_ranger_xml import ranger
    else:
      from setup_ranger import ranger

    ranger('ranger_admin', upgrade_type=upgrade_type)

  def set_ru_rangeradmin_in_progress(self, upgrade_marker_file):
    config_dir = os.path.dirname(upgrade_marker_file)
    try:
      msg = "Starting Upgrade"
      if (not os.path.exists(config_dir)):
        os.makedirs(config_dir)
      ofp = open(upgrade_marker_file, 'w')
      ofp.write(msg)
      ofp.close()
    except OSError as exc:
      if exc.errno == errno.EEXIST and os.path.isdir(config_dir):
        pass
      else:
        raise

  def is_ru_rangeradmin_in_progress(self, upgrade_marker_file):
    return os.path.isfile(upgrade_marker_file)

  def setup_ranger_database(self, env):
    import params
    env.set_params(params)

    upgrade_stack = hdp_select._get_upgrade_stack()
    if upgrade_stack is None:
      raise Fail('Unable to determine the stack and stack version')

    stack_version = upgrade_stack[1]

    if params.xml_configurations_supported:
      Logger.info(format('Setting Ranger database schema, using version {stack_version}'))

      from setup_ranger_xml import setup_ranger_db
      setup_ranger_db(stack_version=stack_version)

  def setup_ranger_java_patches(self, env):
    import params
    env.set_params(params)

    upgrade_stack = hdp_select._get_upgrade_stack()
    if upgrade_stack is None:
      raise Fail('Unable to determine the stack and stack version')

    stack_version = upgrade_stack[1]

    if params.xml_configurations_supported:
      Logger.info(format('Applying Ranger java patches, using version {stack_version}'))

      from setup_ranger_xml import setup_java_patch
      setup_java_patch(stack_version=stack_version)

if __name__ == "__main__":
  RangerAdmin().execute()

