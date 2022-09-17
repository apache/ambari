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
# Python Imports
import os

# Local Imports
from hive import refresh_yarn, create_hive_hdfs_dirs, create_hive_metastore_schema, create_metastore_schema, hive, jdbc_connector
from hive_service import hive_service
from setup_ranger_hive import setup_ranger_hive_metastore_service

# Ambari Commons & Resource Management Imports
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute, Directory
from resource_management.core.resources.system import File
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions import upgrade_summary
from resource_management.libraries.functions.constants import Direction
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.security_commons import cached_kinit_executor
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.script import Script


# the legacy conf.server location in previous stack versions
LEGACY_HIVE_SERVER_CONF = "/etc/hive/conf.server"

class HiveMetastore(Script):
  def install(self, env):
    import params
    self.install_packages(env)


  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    refresh_yarn()
    create_hive_hdfs_dirs()

    # writing configurations on start required for securtity
    self.configure(env)
    if params.init_metastore_schema:
      create_metastore_schema() # execute without config lock

    create_hive_metastore_schema() # before starting metastore create info schema

    hive_service('metastore', action='start', upgrade_type=upgrade_type)

    # below function call is used for cluster depolyed in cloud env to create ranger hive service in ranger admin.
    setup_ranger_hive_metastore_service()

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    hive_service('metastore', action='stop', upgrade_type=upgrade_type)


  def configure(self, env):
    import params
    env.set_params(params)
    hive(name = 'metastore')

  def status(self, env):
    import status_params
    from resource_management.libraries.functions import check_process_status
    env.set_params(status_params)

    # Recursively check all existing gmetad pid files
    check_process_status(status_params.hive_metastore_pid)

  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Metastore Stack Upgrade pre-restart")
    import params

    env.set_params(params)

    is_upgrade = params.upgrade_direction == Direction.UPGRADE

    if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version):
      stack_select.select_packages(params.version)

    if is_upgrade and params.stack_version_formatted_major and \
            check_stack_feature(StackFeature.HIVE_METASTORE_UPGRADE_SCHEMA, params.stack_version_formatted_major):
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
    import status_params
    import params
    env.set_params(params)

    # ensure that configurations are written out before trying to upgrade the schema
    # since the schematool needs configs and doesn't know how to use the hive conf override
    self.configure(env)

    if params.security_enabled:
      cached_kinit_executor(status_params.kinit_path_local,
        status_params.hive_user,
        params.hive_metastore_keytab_path,
        params.hive_metastore_principal,
        status_params.hostname,
        status_params.tmp_dir)
      
    # ensure that the JDBC drive is present for the schema tool; if it's not
    # present, then download it first
    if params.hive_jdbc_driver in params.hive_jdbc_drivers_list:
      target_directory = format("{stack_root}/{version}/hive/lib")

      # download it if it does not exist
      if not os.path.exists(params.source_jdbc_file):
        jdbc_connector(params.hive_jdbc_target, params.hive_previous_jdbc_jar)

      target_directory_and_filename = os.path.join(target_directory, os.path.basename(params.source_jdbc_file))

      if params.sqla_db_used:
        target_native_libs_directory = format("{target_directory}/native/lib64")

        Execute(format("yes | {sudo} cp {jars_in_hive_lib} {target_directory}"))

        Directory(target_native_libs_directory, create_parents = True)

        Execute(format("yes | {sudo} cp {libs_in_hive_lib} {target_native_libs_directory}"))

        Execute(format("{sudo} chown -R {hive_user}:{user_group} {hive_lib}/*"))
      else:
        # copy the JDBC driver from the older metastore location to the new location only
        # if it does not already exist
        if not os.path.exists(target_directory_and_filename):
          Execute(('cp', params.source_jdbc_file, target_directory),
            path=["/bin", "/usr/bin/"], sudo = True)

      File(target_directory_and_filename, mode = 0644)

    # build the schema tool command
    binary = format("{hive_schematool_ver_bin}/schematool")

    # the conf.server directory changed locations between stack versions
    # since the configurations have not been written out yet during an upgrade
    # we need to choose the original legacy location
    schematool_hive_server_conf_dir = params.hive_server_conf_dir

    upgrade_from_version = upgrade_summary.get_source_version("HIVE",
      default_version = params.version_for_stack_feature_checks)

    if not(check_stack_feature(StackFeature.CONFIG_VERSIONING, upgrade_from_version)):
      schematool_hive_server_conf_dir = LEGACY_HIVE_SERVER_CONF

    env_dict = {
      'HIVE_CONF_DIR': schematool_hive_server_conf_dir
    }

    command = format("{binary} -dbType {hive_metastore_db_type} -upgradeSchema")
    Execute(command, user=params.hive_user, tries=1, environment=env_dict, logoutput=True)
    
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
