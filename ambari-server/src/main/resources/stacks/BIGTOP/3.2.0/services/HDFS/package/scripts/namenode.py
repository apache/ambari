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

import sys
import os
import json
import hashlib
from decimal import Decimal, InvalidOperation

from ambari_commons import constants

from resource_management.libraries.script.script import Script
from resource_management.core.resources.system import Directory, Execute
from resource_management.core import shell
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import upgrade_summary
from resource_management.libraries.functions.security_commons import (
  build_expectations,
  cached_kinit_executor,
  get_params_from_filesystem,
  validate_security_config_properties,
  FILE_TYPE_XML,
)

from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger


from ambari_commons.os_family_impl import OsFamilyImpl
from ambari_commons import OSConst


import namenode_upgrade
from hdfs_namenode import (
  namenode,
  wait_for_safemode_off,
  refreshProxyUsers,
  format_namenode,
)
from hdfs import hdfs, reconfig
import hdfs_rebalance
from utils import (
  initiate_safe_zkfc_failover,
  get_hdfs_binary,
  get_dfsadmin_base_command,
)
from resource_management.libraries.functions.namenode_ha_utils import (
  get_hdfs_cluster_id_from_jmx,
)

# The hash algorithm to use to generate digests/hashes
HASH_ALGORITHM = hashlib.sha224


def parse_balancer_threshold(name_node_params):
  try:
    parameters = json.loads(name_node_params)
    raw_threshold = parameters["threshold"]
  except (KeyError, TypeError, json.JSONDecodeError) as error:
    raise Fail("Missing or invalid HDFS balancer threshold") from error

  try:
    threshold_value = Decimal(str(raw_threshold))
  except (InvalidOperation, ValueError) as error:
    raise Fail(f"Invalid HDFS balancer threshold: {raw_threshold!r}") from error
  if not threshold_value.is_finite() or not 0 < threshold_value <= 100:
    raise Fail(
      "HDFS balancer threshold must be a finite number greater than 0 "
      f"and no greater than 100, got {raw_threshold!r}"
    )
  return f"{threshold_value:f}"


class NameNode(Script):
  def get_hdfs_binary(self):
    """
    Get the name or path to the hdfs binary depending on the component name.
    """
    return get_hdfs_binary("hadoop-hdfs-namenode")

  def install(self, env):
    import params

    env.set_params(params)
    self.install_packages(env)
    # HA deployment workflows require the NameNode configuration during install.
    self.configure(env)

  def configure(self, env):
    import params

    env.set_params(params)
    hdfs("namenode")
    hdfs_binary = self.get_hdfs_binary()
    namenode(action="configure", hdfs_binary=hdfs_binary, env=env)

  def save_configs(self, env):
    import params

    env.set_params(params)
    hdfs()

  def reload_configs(self, env):
    import params

    env.set_params(params)
    Logger.info("RELOAD CONFIGS")
    reconfig("namenode", params.namenode_address)

  def reloadproxyusers(self, env):
    import params

    env.set_params(params)
    Logger.info("RELOAD HDFS PROXY USERS")
    refreshProxyUsers()

  def format(self, env):
    import params

    env.set_params(params)

    if params.security_enabled:
      Execute(params.nn_kinit_cmd, user=params.hdfs_user)

    hdfs_cluster_id = get_hdfs_cluster_id_from_jmx(
      params.hdfs_site, params.security_enabled, params.hdfs_user
    )
    if not isinstance(hdfs_cluster_id, str) or not hdfs_cluster_id.strip():
      raise Fail("Could not determine the HDFS cluster ID before formatting")

    # this is run on a new namenode, format needs to be forced
    Execute(
      (
        "hdfs",
        "--config",
        params.hadoop_conf_dir,
        "namenode",
        "-format",
        "-nonInteractive",
        "-clusterId",
        hdfs_cluster_id,
      ),
      user=params.hdfs_user,
      path=[params.hadoop_bin_dir],
      logoutput=True,
    )

  def bootstrap_standby(self, env):
    import params

    env.set_params(params)

    if params.security_enabled:
      Execute(params.nn_kinit_cmd, user=params.hdfs_user)

    Execute(
      ("hdfs", "namenode", "-bootstrapStandby", "-nonInteractive"),
      user=params.hdfs_user,
      logoutput=True,
      path=[params.hadoop_bin_dir],
    )

  def start(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    self.configure(env)
    hdfs_binary = self.get_hdfs_binary()

    if (
      not params.hdfs_tmp_dir
      or params.hdfs_tmp_dir == None
      or params.hdfs_tmp_dir.lower() == "null"
    ):
      Logger.error(
        "WARNING: HDFS tmp dir property (hdfs_tmp_dir) is empty or invalid. Ambari will change permissions for the folder on regular basis."
      )

    namenode(
      action="start",
      hdfs_binary=hdfs_binary,
      upgrade_type=upgrade_type,
      upgrade_suspended=params.upgrade_suspended,
      env=env,
    )

    # after starting NN in an upgrade, touch the marker file - but only do this for certain
    # upgrade types - not all upgrades actually tell NN about the upgrade (like HOU)
    if upgrade_type in (
      constants.UPGRADE_TYPE_ROLLING,
      constants.UPGRADE_TYPE_NON_ROLLING,
    ):
      # place a file on the system indicating that we've submitting the command that
      # instructs NN that it is now part of an upgrade
      namenode_upgrade.create_upgrade_marker()

  def stop(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    hdfs_binary = self.get_hdfs_binary()
    if upgrade_type == constants.UPGRADE_TYPE_ROLLING and params.dfs_ha_enabled:
      if params.dfs_ha_automatic_failover_enabled:
        initiate_safe_zkfc_failover()
      else:
        raise Fail(
          "Rolling Upgrade - dfs.ha.automatic-failover.enabled must be enabled to perform a rolling restart"
        )
    namenode(action="stop", hdfs_binary=hdfs_binary, upgrade_type=upgrade_type, env=env)

  def status(self, env):
    import status_params

    env.set_params(status_params)
    namenode(action="status", env=env)

  def decommission(self, env):
    import params

    env.set_params(params)
    hdfs_binary = self.get_hdfs_binary()
    namenode(action="decommission", hdfs_binary=hdfs_binary)
    self.configure(env)

  def print_topology(self, env):
    import params

    env.set_params(params)
    Execute(
      ("hdfs", "dfsadmin", "-printTopology"),
      user=params.hdfs_user,
      path=[params.hadoop_bin_dir],
      logoutput=True,
    )


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class NameNodeDefault(NameNode):
  def prepare_express_upgrade(self, env):
    """
    During an Express Upgrade.
    If in HA, on the Active NameNode only, examine the directory dfs.namenode.name.dir and
    make sure that there is no "/previous" directory.

    Create a list of all the DataNodes in the cluster.
    hdfs dfsadmin -report > dfs-old-report-1.log

    hdfs dfsadmin -safemode enter
    hdfs dfsadmin -saveNamespace

    Copy the checkpoint files located in ${dfs.namenode.name.dir}/current into a backup directory.

    Finalize any prior HDFS upgrade,
    hdfs dfsadmin -finalizeUpgrade

    Prepare for a NameNode rolling upgrade in order to not lose any data.
    hdfs dfsadmin -rollingUpgrade prepare
    """
    import params

    Logger.info("Preparing the NameNodes for a NonRolling (aka Express) Upgrade.")

    if params.security_enabled:
      kinit_command = (
        params.kinit_path_local,
        "-kt",
        params.hdfs_user_keytab,
        params.hdfs_principal_name,
      )
      Execute(kinit_command, user=params.hdfs_user, logoutput=True)

    hdfs_binary = self.get_hdfs_binary()
    namenode_upgrade.prepare_upgrade_check_for_previous_dir()
    namenode_upgrade.prepare_upgrade_enter_safe_mode(hdfs_binary)
    if not params.skip_namenode_save_namespace_express:
      namenode_upgrade.prepare_upgrade_save_namespace(hdfs_binary)
    if not params.skip_namenode_namedir_backup_express:
      namenode_upgrade.prepare_upgrade_backup_namenode_dir()
    namenode_upgrade.prepare_upgrade_finalize_previous_upgrades(hdfs_binary)

    summary = upgrade_summary.get_upgrade_summary()

    if summary is not None and summary.is_downgrade_allowed:
      # Call -rollingUpgrade prepare
      namenode_upgrade.prepare_rolling_upgrade(hdfs_binary)
    else:
      Logger.info("Downgrade will not be possible. Skipping '-rollingUpgrade prepare'")

  def prepare_rolling_upgrade(self, env):
    hfds_binary = self.get_hdfs_binary()
    namenode_upgrade.prepare_rolling_upgrade(hfds_binary)

  def wait_for_safemode_off(self, env):
    wait_for_safemode_off(
      self.get_hdfs_binary(), afterwait_sleep=30, execute_kinit=True
    )

  def finalize_non_rolling_upgrade(self, env):
    hfds_binary = self.get_hdfs_binary()
    namenode_upgrade.finalize_upgrade(constants.UPGRADE_TYPE_NON_ROLLING, hfds_binary)

  def finalize_rolling_upgrade(self, env):
    hfds_binary = self.get_hdfs_binary()
    namenode_upgrade.finalize_upgrade(constants.UPGRADE_TYPE_ROLLING, hfds_binary)

  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Stack Upgrade pre-restart")
    import params

    env.set_params(params)

    stack_select.select_packages(params.version)

  def post_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Stack Upgrade post-restart")
    import params

    env.set_params(params)

    hdfs_binary = self.get_hdfs_binary()
    dfsadmin_base_command = get_dfsadmin_base_command(hdfs_binary)
    dfsadmin_cmd = dfsadmin_base_command + ("-report", "-live")
    Execute(dfsadmin_cmd, user=params.hdfs_user, tries=60, try_sleep=10)

  def rebalancehdfs(self, env):
    import params

    env.set_params(params)

    threshold = parse_balancer_threshold(params.name_node_params)
    _print(f"Starting balancer with threshold = {threshold}\n")

    rebalance_env = {"PATH": params.hadoop_bin_dir}

    if params.security_enabled:
      # Create the kerberos credentials cache (ccache) file and set it in the environment to use
      # when executing HDFS rebalance command. Use the sha224 hash of the combination of the principal and keytab file
      # to generate a (relatively) unique cache filename so that we can use it as needed.
      ccache_file_name = (
        "hdfs_rebalance_cc_"
        + HASH_ALGORITHM(
          f"{params.hdfs_principal_name}|{params.hdfs_user_keytab}".encode()
        ).hexdigest()
      )
      ccache_dir = os.path.join(
        params.hadoop_pid_dir_prefix, params.hdfs_user, "ambari-ccache"
      )
      Directory(
        ccache_dir,
        owner=params.hdfs_user,
        group=params.user_group,
        mode=0o700,
        create_parents=True,
      )
      ccache_file_path = os.path.join(ccache_dir, ccache_file_name)
      rebalance_env["KRB5CCNAME"] = ccache_file_path

      # If there are no tickets in the cache or they are expired, perform a kinit, else use what
      # is in the cache
      klist_cmd = (params.klist_path_local, "-s", ccache_file_path)
      kinit_cmd = (
        params.kinit_path_local,
        "-c",
        ccache_file_path,
        "-kt",
        params.hdfs_user_keytab,
        params.hdfs_principal_name,
      )
      if shell.call(klist_cmd, user=params.hdfs_user)[0] != 0:
        Execute(kinit_cmd, user=params.hdfs_user)

    command = (
      "hdfs",
      "--config",
      params.hadoop_conf_dir,
      "balancer",
      "-threshold",
      threshold,
    )

    _print(f"Executing command {command}\n")

    if not hdfs_rebalance.is_balancer_running():
      # The balancer may run for hours or days, so start it asynchronously.
      Execute(
        command,
        user=params.hdfs_user,
        environment=rebalance_env,
        wait_for_finish=False,
      )

      _print("The rebalance process has been triggered")
    else:
      _print(
        "There is another balancer running. This means you or another Ambari user may have triggered the "
        "operation earlier. The process may take a long time to finish (hours, even days). If the problem persists "
        "please consult with the HDFS administrators if they have triggred or killed the operation."
      )

  def get_log_folder(self):
    import params

    return params.hdfs_log_dir

  def get_user(self):
    import params

    return params.hdfs_user

  def get_pid_files(self):
    import status_params

    return [status_params.namenode_pid_file]


def _print(line):
  sys.stdout.write(line)
  sys.stdout.flush()


if __name__ == "__main__":
  NameNode().execute()
