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

from contextlib import nullcontext

from ambari_commons.constants import UPGRADE_TYPE_NON_ROLLING
from ambari_commons.os_family_impl import OsFamilyImpl
from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.core.logger import Logger
from resource_management.libraries.functions import StackFeature, stack_select
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.script.script import Script

import zookeeper_cli
import zookeeper_process
import zookeeper_utils
from zookeeper import zookeeper
from zookeeper_service import zookeeper_service


class ZookeeperServer(Script):
  def configure(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    zookeeper(type="server", upgrade_type=upgrade_type)

  def start(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    self.configure(env, upgrade_type=upgrade_type)
    zookeeper_service(action="start", upgrade_type=upgrade_type)

  def stop(self, env, upgrade_type=None):
    import status_params

    env.set_params(status_params)
    zookeeper_service(action="stop", upgrade_type=upgrade_type)


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class ZookeeperServerLinux(ZookeeperServer):
  def install(self, env):
    self.install_packages(env)
    self.configure(env)

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    Logger.info("Executing ZooKeeper stack upgrade pre-restart")
    if params.version and check_stack_feature(
      StackFeature.ROLLING_UPGRADE, format_stack_version(params.version)
    ):
      stack_select.select_packages(params.version)

  def post_upgrade_restart(self, env, upgrade_type=None):
    if upgrade_type == UPGRADE_TYPE_NON_ROLLING:
      return

    import params

    env.set_params(params)
    Logger.info("Verifying the ZooKeeper quorum after stack upgrade")

    cache_context = nullcontext(None)
    if params.security_enabled:
      zookeeper_utils.validate_keytab(
        params.zk_keytab_path, "ZooKeeper server keytab"
      )
      cache_context = PrivateKerberosCache(
        params.zk_user,
        params.user_group,
        temp_dir=params.tmp_dir,
        prefix="ambari-zookeeper-upgrade-",
      )

    with cache_context as kerberos_cache:
      command_environment = {
        "JAVA_HOME": params.java64_home,
        "ZOOCFGDIR": params.config_dir,
        "ZOOCFG": "zoo.cfg",
      }
      if kerberos_cache is not None:
        command_environment = kerberos_cache.merge_environment(
          command_environment
        )
        kerberos_cache.kinit(
          params.kinit_path_local,
          params.zk_keytab_path,
          params.zk_principal,
          timeout=60,
        )
      zookeeper_cli.verify_ensemble(
        params.zookeeper_hosts,
        params.client_port,
        params.zk_cli_script,
        params.zk_user,
        params.user_group,
        params.tmp_dir,
        command_environment,
      )

  def status(self, env):
    import status_params

    env.set_params(status_params)
    identity = zookeeper_process.read_or_recover_process(
      status_params.zk_pid_file,
      status_params.zk_user,
      status_params.user_group,
      status_params.zk_config_file,
    )
    if identity is None:
      raise ComponentIsNotRunning()

  def get_log_folder(self):
    import params

    return params.zk_log_dir

  def get_user(self):
    import params

    return params.zk_user

  def get_pid_files(self):
    import status_params

    return [status_params.zk_pid_file]


if __name__ == "__main__":
  ZookeeperServer().execute()
