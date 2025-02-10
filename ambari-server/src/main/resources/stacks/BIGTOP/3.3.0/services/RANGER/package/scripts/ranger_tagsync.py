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

from resource_management.libraries.script import Script
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import upgrade_summary
from resource_management.libraries.functions.constants import Direction
from resource_management.core.resources.system import Execute, File
from resource_management.libraries.functions.check_process_status import (
  check_process_status,
)
from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.libraries.functions.format import format
from resource_management.core.logger import Logger
from resource_management.core import shell
from ranger_service import ranger_service
from resource_management.core.exceptions import Fail
import setup_ranger_xml
import upgrade
import os


class RangerTagsync(Script):
  def install(self, env):
    self.install_packages(env)

  def initialize(self, env):
    import params

    env.set_params(params)
    ranger_tagsync_setup_marker = os.path.join(
      params.ranger_tagsync_conf, "tagsync_setup"
    )
    if not os.path.exists(ranger_tagsync_setup_marker):
      setup_ranger_xml.validate_user_password("rangertagsync_user_password")

      setup_ranger_xml.ranger_credential_helper(
        params.tagsync_cred_lib,
        "tagadmin.user.password",
        params.rangertagsync_user_password,
        params.tagsync_jceks_path,
      )
      File(
        params.tagsync_jceks_path,
        owner=params.unix_user,
        group=params.unix_group,
        only_if=format("test -e {tagsync_jceks_path}"),
        mode=0o640,
      )

      setup_ranger_xml.update_dot_jceks_crc_ownership(
        credential_provider_path=params.tagsync_jceks_path,
        user=params.unix_user,
        group=params.unix_group,
      )

      if params.stack_supports_ranger_tagsync_ssl_xml_support:
        Logger.info("Stack support Atlas user for Tagsync, creating keystore for same.")
        self.create_atlas_user_keystore(env)
      else:
        Logger.info(
          "Stack does not support Atlas user for Tagsync, skipping keystore creation for same."
        )

      File(
        ranger_tagsync_setup_marker,
        owner=params.unix_user,
        group=params.unix_group,
        mode=0o640,
      )

  def configure(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    self.initialize(env)
    setup_ranger_xml.ranger("ranger_tagsync", upgrade_type=upgrade_type)

  def start(self, env, upgrade_type=None):
    import params

    env.set_params(params)

    self.configure(env, upgrade_type=upgrade_type)
    ranger_service("ranger_tagsync")

  def stop(self, env, upgrade_type=None):
    import params

    env.set_params(params)

    Execute(
      format("{tagsync_services_file} stop"),
      environment={"JAVA_HOME": params.java_home},
      user=params.unix_user,
    )
    File(params.tagsync_pid_file, action="delete")

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

  def post_upgrade_restart(self, env, upgrade_type=None):
    import params

    env.set_params(params)

    if (
      upgrade_type
      and params.upgrade_direction == Direction.UPGRADE
      and not params.stack_supports_multiple_env_sh_files
    ):
      files_name_list = ["ranger-tagsync-env-piddir.sh", "ranger-tagsync-env-logdir.sh"]
      for file_name in files_name_list:
        File(format("{ranger_tagsync_conf}/{file_name}"), action="delete")

  def get_log_folder(self):
    import params

    return params.tagsync_log_dir

  def get_user(self):
    import params

    return params.unix_user

  def get_pid_files(self):
    import status_params

    return [status_params.tagsync_pid_file]

  def create_atlas_user_keystore(self, env):
    import params

    env.set_params(params)

    setup_ranger_xml.ranger_credential_helper(
      params.tagsync_cred_lib,
      "atlas.user.password",
      params.atlas_admin_password,
      params.atlas_tagsync_jceks_path,
    )
    File(
      params.atlas_tagsync_jceks_path,
      owner=params.unix_user,
      group=params.unix_group,
      only_if=format("test -e {atlas_tagsync_jceks_path}"),
      mode=0o640,
    )

    setup_ranger_xml.update_dot_jceks_crc_ownership(
      credential_provider_path=params.atlas_tagsync_jceks_path,
      user=params.unix_user,
      group=params.unix_group,
    )


if __name__ == "__main__":
  RangerTagsync().execute()
