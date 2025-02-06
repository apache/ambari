#!/usr/bin/env python3
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

from resource_management.libraries.functions.check_process_status import (
  check_process_status,
)
from resource_management.libraries.script import Script
from resource_management.core.resources.system import Execute, File
from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.libraries.functions.format import format
from resource_management.core.logger import Logger
from resource_management.core import shell
from ranger_service import ranger_service
from resource_management.libraries.functions.constants import Direction
import upgrade
import setup_ranger_xml
import os


class RangerUsersync(Script):
  def install(self, env):
    self.install_packages(env)

  def initialize(self, env):
    import params

    env.set_params(params)
    ranger_ugsync_setup_marker = os.path.join(
      params.ranger_ugsync_conf, "usersync_setup"
    )
    if not os.path.exists(ranger_ugsync_setup_marker):
      setup_ranger_xml.validate_user_password("rangerusersync_user_password")
      if params.stack_supports_usersync_passwd:
        setup_ranger_xml.ranger_credential_helper(
          params.ugsync_cred_lib,
          params.ugsync_policymgr_alias,
          params.rangerusersync_user_password,
          params.ugsync_policymgr_keystore,
        )

        File(
          params.ugsync_policymgr_keystore,
          owner=params.unix_user,
          group=params.unix_group,
          mode=0o640,
        )
      File(
        ranger_ugsync_setup_marker,
        owner=params.unix_user,
        group=params.unix_group,
        mode=0o640,
      )

  def configure(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    self.initialize(env)
    setup_ranger_xml.ranger("ranger_usersync", upgrade_type=upgrade_type)

  def start(self, env, upgrade_type=None):
    import params

    env.set_params(params)

    self.configure(env, upgrade_type=upgrade_type)
    ranger_service("ranger_usersync")

  def stop(self, env, upgrade_type=None):
    import params

    env.set_params(params)

    Execute(
      format("{params.usersync_stop}"),
      environment={"JAVA_HOME": params.java_home},
      user=params.unix_user,
    )
    if params.stack_supports_pid:
      File(params.ranger_usersync_pid_file, action="delete")

  def status(self, env):
    import status_params

    env.set_params(status_params)

    if status_params.stack_supports_pid:
      check_process_status(status_params.ranger_usersync_pid_file)
      return

    cmd = "ps -ef | grep proc_rangerusersync | grep -v grep"
    code, output = shell.call(cmd, timeout=20)

    if code != 0:
      Logger.debug("Ranger usersync process not running")
      raise ComponentIsNotRunning()
    pass

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    upgrade.prestart(env)

  def post_upgrade_restart(self, env, upgrade_type=None):
    import params

    env.set_params(params)

    if (
      upgrade_type
      and params.upgrade_direction == Direction.UPGRADE
      and not params.stack_supports_multiple_env_sh_files
    ):
      files_name_list = [
        "ranger-usersync-env-piddir.sh",
        "ranger-usersync-env-logdir.sh",
      ]
      for file_name in files_name_list:
        File(format("{ranger_ugsync_conf}/{file_name}"), action="delete")

  def get_log_folder(self):
    import params

    return params.usersync_log_dir

  def get_user(self):
    import params

    return params.unix_user

  def get_pid_files(self):
    import status_params

    return [status_params.ranger_usersync_pid_file]


if __name__ == "__main__":
  RangerUsersync().execute()
