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

from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.constants import StackFeature
from resource_management.core.exceptions import ComponentIsNotRunning, Fail
from resource_management.core.signal_utils import TerminateStrategy
from resource_management.libraries.providers.hdfs_resource import HdfsResourceProvider
from resource_management import is_empty
from resource_management import shell
from resource_management.libraries.functions.decorator import retry
from resource_management.core.logger import Logger
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)

from livy_service import livy_service, read_or_discover_livy_process
from setup_livy import setup_livy


class LivyServer(Script):
  def install(self, env):
    import params

    env.set_params(params)

    self.install_packages(env)

  def configure(self, env, upgrade_type=None, config_dir=None):
    import params

    env.set_params(params)

    setup_livy(env, "server", upgrade_type=upgrade_type, action="config")

  def start(self, env, upgrade_type=None):
    import params

    env.set_params(params)

    if params.has_ats and params.has_livyserver:
      Logger.info(
        "Verifying DFS directories where ATS stores time line data for active and completed applications."
      )
      self.wait_for_dfs_directories_created(
        [params.entity_groupfs_store_dir, params.entity_groupfs_active_dir]
      )

    self.configure(env)
    livy_service("server", upgrade_type=upgrade_type, action="start")

  def stop(self, env, upgrade_type=None):
    import params

    env.set_params(params)

    livy_service("server", upgrade_type=upgrade_type, action="stop")

  def status(self, env):
    import status_params

    env.set_params(status_params)

    identity = read_or_discover_livy_process(
      status_params.livy_server_pid_file,
      status_params.livy_user,
      status_params.livy_group,
    )
    if identity is None:
      raise ComponentIsNotRunning("Livy Server is not running")

  def wait_for_dfs_directories_created(self, dirs):
    import params

    ignored_dfs_dirs = HdfsResourceProvider.get_ignored_resources_list(
      params.hdfs_resource_ignore_file
    )

    if params.security_enabled:
      if not all(
        str(value or "").strip()
        for value in (
          params.kinit_path_local,
          params.livy_kerberos_keytab,
          params.livy_principal,
        )
      ):
        raise Fail(
          "Secure Livy DFS checks require a launch principal and keytab"
        )
      with PrivateKerberosCache(
        params.livy_user,
        params.livy_group,
        prefix="ambari-livy-dfs-check-",
      ) as kerberos_cache:
        kerberos_cache.kinit(
          params.kinit_path_local,
          params.livy_kerberos_keytab,
          params.livy_principal,
          timeout=30,
        )
        for dir_path in dirs:
          self.wait_for_dfs_directory_created(
            dir_path,
            ignored_dfs_dirs,
            kerberos_environment=kerberos_cache.environment,
          )
      return

    for dir_path in dirs:
      self.wait_for_dfs_directory_created(dir_path, ignored_dfs_dirs)

  def get_pid_files(self):
    import status_params

    return [status_params.livy_server_pid_file]

  @retry(times=8, sleep_time=20, backoff_factor=1, err_class=Fail)
  def wait_for_dfs_directory_created(
    self, dir_path, ignored_dfs_dirs, kerberos_environment=None
  ):
    import params

    if not is_empty(dir_path):
      dir_path = HdfsResourceProvider.parse_path(dir_path)

      if dir_path in ignored_dfs_dirs:
        Logger.info(
          "Skipping DFS directory '" + dir_path + "' as it's marked to be ignored."
        )
        return

      Logger.info(f"Verifying if DFS directory '{dir_path}' exists.")

      dfs_ret_code = shell.call(
        (
          f"{params.hadoop_bin_dir}/hdfs",
          "--config",
          params.hadoop_conf_dir,
          "dfs",
          "-test",
          "-d",
          dir_path,
        ),
        user=params.livy_user,
        env=kerberos_environment,
        timeout=30,
        timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
      )[0]
      dir_exists = dfs_ret_code == 0

      if not dir_exists:
        raise Fail(f"DFS directory '{dir_path}' does not exist")
      else:
        Logger.info(f"DFS directory '{dir_path}' exists.")

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    if params.version and check_stack_feature(
      StackFeature.ROLLING_UPGRADE, params.version
    ):
      Logger.info("Executing Livy Server Stack Upgrade pre-restart")
      stack_select.select_packages(params.version)

  def get_log_folder(self):
    import params

    return params.livy_log_dir

  def get_user(self):
    import params

    return params.livy_user


if __name__ == "__main__":
  LivyServer().execute()
