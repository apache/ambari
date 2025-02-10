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

from resource_management.core.exceptions import Fail, ExecutionFailed
from resource_management.libraries.functions.check_process_status import (
  check_process_status,
)
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import upgrade_summary
from resource_management.libraries.functions.constants import Direction
from resource_management.libraries.script import Script
from resource_management.core.resources.system import Execute, File
from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.libraries.functions.format import format
from resource_management.core.logger import Logger
from resource_management.core import shell
from ranger_service import ranger_service
from resource_management.libraries.functions import solr_cloud_util
from ambari_commons.constants import UPGRADE_TYPE_NON_ROLLING, UPGRADE_TYPE_ROLLING
import upgrade
import os, errno
import ambari_simplejson as json
import setup_ranger_xml


class RangerAdmin(Script):
  def install(self, env):
    self.install_packages(env)

  def initialize(self, env):
    import params

    env.set_params(params)
    ranger_admin_setup_marker = os.path.join(params.ranger_conf, "admin_setup")
    if not os.path.exists(ranger_admin_setup_marker):
      # taking backup of install.properties file
      Execute(
        (
          "cp",
          "-f",
          format("{ranger_home}/install.properties"),
          format("{ranger_home}/install-backup.properties"),
        ),
        not_if=format("ls {ranger_home}/install-backup.properties"),
        only_if=format("ls {ranger_home}/install.properties"),
        sudo=True,
      )
      File(
        ranger_admin_setup_marker,
        owner=params.unix_user,
        group=params.unix_group,
        mode=0o640,
      )

  def stop(self, env, upgrade_type=None):
    import params

    env.set_params(params)

    Execute(
      format("{params.ranger_stop}"),
      environment={"JAVA_HOME": params.java_home},
      user=params.unix_user,
    )
    if params.stack_supports_pid:
      File(params.ranger_admin_pid_file, action="delete")

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params

    env.set_params(params)

    upgrade.prestart(env)

    self.set_ru_rangeradmin_in_progress(params.upgrade_marker_file)

  def post_upgrade_restart(self, env, upgrade_type=None):
    import params

    env.set_params(params)

    if os.path.isfile(params.upgrade_marker_file):
      os.remove(params.upgrade_marker_file)

    if (
      upgrade_type
      and params.upgrade_direction == Direction.UPGRADE
      and not params.stack_supports_multiple_env_sh_files
    ):
      files_name_list = ["ranger-admin-env-piddir.sh", "ranger-admin-env-logdir.sh"]
      for file_name in files_name_list:
        File(format("{ranger_conf}/{file_name}"), action="delete")

    if (
      upgrade_type
      and params.upgrade_direction == Direction.UPGRADE
      and params.stack_supports_ranger_zone_feature
    ):
      if (
        params.has_infra_solr
        and params.audit_solr_enabled
        and params.is_solrCloud_enabled
        and not params.is_external_solrCloud_enabled
      ):
        add_field_json = json.dumps(params.add_zoneName_field)
        add_field_cmd = format(
          "curl -k -X POST -H 'Content-type:application/json' --data-binary '{add_field_json}' {infra_solr_protocol}://{infra_solr_host}:{infra_solr_port}/solr/{ranger_solr_collection_name}/schema"
        )
        if params.security_enabled:
          kinit_cmd = format(
            "{kinit_path_local} -kt {ranger_admin_keytab} {ranger_admin_jaas_principal};"
          )
          add_field_cmd = format(
            "{kinit_cmd} curl -k --negotiate -u : -X POST -H 'Content-type:application/json' --data-binary '{add_field_json}' {infra_solr_protocol}://{infra_solr_host}:{infra_solr_port}/solr/{ranger_solr_collection_name}/schema"
          )
        try:
          Execute(
            add_field_cmd, tries=3, try_sleep=5, user=params.unix_user, logoutput=True
          )
        except ExecutionFailed as execution_exception:
          Logger.error(
            f"Error adding field to Ranger Audits Solr Collection. Kindly check Infra Solr service to be up and running {execution_exception}"
          )

  def start(self, env, upgrade_type=None):
    import params

    env.set_params(params)

    if upgrade_type is None:
      setup_ranger_xml.validate_user_password()

    # setup db only if in case stack version is > 2.6
    self.configure(
      env,
      upgrade_type=upgrade_type,
      setup_db=params.stack_supports_ranger_setup_db_on_start,
    )

    if (
      params.stack_supports_infra_client
      and params.audit_solr_enabled
      and params.is_solrCloud_enabled
    ):
      solr_cloud_util.setup_solr_client(params.config, custom_log4j=params.custom_log4j)
      setup_ranger_xml.setup_ranger_audit_solr()

    setup_ranger_xml.update_password_configs()
    ranger_service("ranger_admin")

  def status(self, env):
    import status_params

    env.set_params(status_params)

    if status_params.stack_supports_pid:
      check_process_status(status_params.ranger_admin_pid_file)
      return

    cmd = "ps -ef | grep proc_rangeradmin | grep -v grep"
    code, output = shell.call(cmd, timeout=20)

    if code != 0:
      if self.is_ru_rangeradmin_in_progress(status_params.upgrade_marker_file):
        Logger.info(
          "Ranger admin process not running - skipping as stack upgrade is in progress"
        )
      else:
        Logger.debug("Ranger admin process not running")
        raise ComponentIsNotRunning()
    pass

  def configure(self, env, upgrade_type=None, setup_db=False):
    import params

    env.set_params(params)

    # set up db if we are not upgrading and setup_db is true
    if setup_db and upgrade_type is None:
      setup_ranger_xml.setup_ranger_db()

    setup_ranger_xml.ranger("ranger_admin", upgrade_type=upgrade_type)

    # set up java patches if we are not upgrading and setup_db is true
    if setup_db and upgrade_type is None:
      setup_ranger_xml.setup_java_patch()
      if params.stack_supports_ranger_all_admin_change_default_password:
        setup_ranger_xml.setup_ranger_all_admin_password_change(
          params.admin_username,
          params.default_admin_password,
          params.admin_password,
          params.rangerusersync_username,
          params.default_rangerusersync_user_password,
          params.rangerusersync_user_password,
          params.rangertagsync_username,
          params.default_rangertagsync_user_password,
          params.rangertagsync_user_password,
          params.keyadmin_username,
          params.default_keyadmin_user_password,
          params.keyadmin_user_password,
        )
      else:
        # Updating password for Ranger Admin user
        setup_ranger_xml.setup_ranger_admin_passwd_change(
          params.admin_username, params.admin_password, params.default_admin_password
        )
        # Updating password for Ranger Usersync user
        setup_ranger_xml.setup_ranger_admin_passwd_change(
          params.rangerusersync_username,
          params.rangerusersync_user_password,
          params.default_rangerusersync_user_password,
        )
        # Updating password for Ranger Tagsync user
        setup_ranger_xml.setup_ranger_admin_passwd_change(
          params.rangertagsync_username,
          params.rangertagsync_user_password,
          params.default_rangertagsync_user_password,
        )
        # Updating password for Ranger Keyadmin user
        setup_ranger_xml.setup_ranger_admin_passwd_change(
          params.keyadmin_username,
          params.keyadmin_user_password,
          params.default_keyadmin_user_password,
        )

  def set_ru_rangeradmin_in_progress(self, upgrade_marker_file):
    config_dir = os.path.dirname(upgrade_marker_file)
    try:
      msg = "Starting Upgrade"
      if not os.path.exists(config_dir):
        os.makedirs(config_dir)
      ofp = open(upgrade_marker_file, "w")
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

    upgrade_stack = stack_select._get_upgrade_stack()
    if upgrade_stack is None:
      raise Fail("Unable to determine the stack and stack version")

    stack_version = upgrade_stack[1]

    if params.upgrade_direction == Direction.UPGRADE:
      target_version = upgrade_summary.get_target_version(
        "RANGER", default_version=stack_version
      )
      Logger.info(
        format("Setting Ranger database schema, using version {target_version}")
      )

      setup_ranger_xml.setup_ranger_db(stack_version=target_version)

  def setup_ranger_java_patches(self, env):
    import params

    env.set_params(params)

    upgrade_stack = stack_select._get_upgrade_stack()
    if upgrade_stack is None:
      raise Fail("Unable to determine the stack and stack version")

    stack_version = upgrade_stack[1]

    if params.upgrade_direction == Direction.UPGRADE:
      target_version = upgrade_summary.get_target_version(
        "RANGER", default_version=stack_version
      )
      Logger.info(
        format("Applying Ranger java patches, using version {target_version}")
      )

      setup_ranger_xml.setup_java_patch(stack_version=target_version)

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

    stack_select_packages = stack_select.get_packages(
      orchestration, service_name="RANGER", component_name="RANGER_ADMIN"
    )
    if stack_select_packages is None:
      raise Fail("Unable to get packages for stack-select")

    Logger.info(
      f"RANGER_ADMIN component will be stack-selected to version {params.version} using a {orchestration.upper()} orchestration"
    )

    for stack_select_package_name in stack_select_packages:
      stack_select.select(stack_select_package_name, params.version)

  def get_log_folder(self):
    import params

    return params.admin_log_dir

  def get_user(self):
    import params

    return params.unix_user

  def get_pid_files(self):
    import status_params

    return [status_params.ranger_admin_pid_file]


if __name__ == "__main__":
  RangerAdmin().execute()
