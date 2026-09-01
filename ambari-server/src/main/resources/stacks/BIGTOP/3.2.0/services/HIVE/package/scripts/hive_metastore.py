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

# Python Imports
import os

# Local Imports
from hive import (
  refresh_yarn,
  create_hive_hdfs_dirs,
  create_hive_metastore_schema,
  create_metastore_schema,
  hive,
  jdbc_connector,
)
from hive_service import check_hive_process_status, hive_service
from setup_ranger_hive import setup_ranger_hive_metastore_service

# Ambari Commons & Resource Management Imports
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute
from resource_management.core.resources.system import File
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.constants import Direction
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.script import Script


class HiveMetastore(Script):
  def install(self, env):
    import params

    self.install_packages(env)

  def start(self, env, upgrade_type=None):
    import params

    env.set_params(params)

    refresh_yarn()
    create_hive_hdfs_dirs()

    # Write configuration immediately before starting the daemon.
    self.configure(env)
    if params.init_metastore_schema:
      create_metastore_schema()  # execute without config lock

    create_hive_metastore_schema()  # before starting metastore create info schema

    hive_service("metastore", action="start", upgrade_type=upgrade_type)

    # Register the metastore in Ranger after a successful daemon start.
    setup_ranger_hive_metastore_service()

  def stop(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    hive_service("metastore", action="stop", upgrade_type=upgrade_type)

  def configure(self, env):
    import params

    env.set_params(params)
    hive(name="metastore")

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_hive_process_status(
      status_params.hive_metastore_pid,
      status_params.hive_user,
      status_params.user_group,
      "metastore",
    )

  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Metastore Stack Upgrade pre-restart")
    import params

    env.set_params(params)

    is_upgrade = params.upgrade_direction == Direction.UPGRADE

    if params.version and check_stack_feature(
      StackFeature.ROLLING_UPGRADE, params.version
    ):
      stack_select.select_packages(params.version)

    if (
      is_upgrade
      and params.stack_version_formatted_major
      and check_stack_feature(
        StackFeature.HIVE_METASTORE_UPGRADE_SCHEMA, params.stack_version_formatted_major
      )
    ):
      self.upgrade_schema(env)

  def upgrade_schema(self, env):
    """
    Executes the schema upgrade binary.  This is its own function because it could
    be called as a standalone task from the upgrade pack, but is safe to run it for each
    metastore instance. The schema upgrade on an already upgraded metastore is a NOOP.

    The metastore schema upgrade requires a database driver library for most
    databases. During an upgrade, it's possible that the library is not present,
    so this will also attempt to copy/download the appropriate driver.

    This function will also ensure that configurations are written out to disk before running
    since the new configs will most likely not yet exist on an upgrade.

    Should not be invoked for a DOWNGRADE; Metastore only supports schema upgrades.
    """
    Logger.info("Upgrading Hive Metastore Schema")
    import params

    env.set_params(params)

    # ensure that configurations are written out before trying to upgrade the schema
    # since the schematool needs configs and doesn't know how to use the hive conf override
    self.configure(env)

    # Ensure that the JDBC driver is present for the schema tool.
    if params.hive_jdbc_driver in params.hive_jdbc_drivers_list:
      if params.hive_jdbc_target and not os.path.exists(params.hive_jdbc_target):
        jdbc_connector(params.hive_jdbc_target, params.hive_previous_jdbc_jar)
      if params.hive_jdbc_target:
        File(params.hive_jdbc_target, owner="root", group="root", mode=0o644)

    # build the schema tool command
    binary = format("{hive_bin_dir}/schematool")

    command = (binary, "-dbType", params.hive_metastore_db_type, "-upgradeSchema")
    environment = {"HIVE_CONF_DIR": params.hive_conf_dir}
    if params.security_enabled:
      with PrivateKerberosCache(
        params.hive_user,
        params.user_group,
        params.tmp_dir,
        "ambari-hive-schema-upgrade-",
      ) as cache:
        cache.kinit(
          params.kinit_path_local,
          params.hive_metastore_keytab_path,
          params.hive_metastore_principal.replace("_HOST", params.hostname),
        )
        Execute(
          command,
          user=params.hive_user,
          tries=1,
          environment=cache.merge_environment(environment),
          logoutput=True,
        )
    else:
      Execute(
        command,
        user=params.hive_user,
        tries=1,
        environment=environment,
        logoutput=True,
      )

  def get_log_folder(self):
    import params

    return params.hive_log_dir

  def get_user(self):
    import params

    return params.hive_user

  def get_pid_files(self):
    import status_params

    return [status_params.hive_metastore_pid]


if __name__ == "__main__":
  HiveMetastore().execute()
