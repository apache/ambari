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

import datanode_upgrade

from ambari_commons.constants import UPGRADE_TYPE_ROLLING

from hdfs_datanode import datanode
from resource_management import Script, Fail, shell, Logger
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.decorator import retry
from resource_management.libraries.functions.security_commons import (
  build_expectations,
  get_params_from_filesystem,
  validate_security_config_properties,
  FILE_TYPE_XML,
)
from resource_management.core.logger import Logger
from resource_management.core.signal_utils import TerminateStrategy
from hdfs import hdfs, reconfig
from ambari_commons.os_family_impl import OsFamilyImpl
from ambari_commons import OSConst
from utils import get_hdfs_binary
from utils import get_dfsadmin_base_command
from hdfs_kerberos import hdfs_kerberos_environment


class DataNode(Script):
  def get_hdfs_binary(self):
    """
    Get the name or path to the hdfs binary depending on the component name.
    """
    return get_hdfs_binary("hadoop-hdfs-datanode")

  def install(self, env):
    import params

    env.set_params(params)
    self.install_packages(env)

  def configure(self, env):
    import params

    env.set_params(params)
    hdfs("datanode")
    datanode(action="configure")

  def save_configs(self, env):
    import params

    env.set_params(params)
    hdfs("datanode")

  def reload_configs(self, env):
    import params

    env.set_params(params)
    Logger.info("RELOAD CONFIGS")
    reconfig("datanode", params.dfs_dn_ipc_address)

  def start(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    self.configure(env)
    datanode(action="start")

  def stop(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    # pre-upgrade steps shutdown the datanode, so there's no need to call

    hdfs_binary = self.get_hdfs_binary()
    if upgrade_type == UPGRADE_TYPE_ROLLING:
      stopped = datanode_upgrade.pre_rolling_upgrade_shutdown(hdfs_binary)
      if not stopped:
        datanode(action="stop")
    else:
      datanode(action="stop")
    # verify that the datanode is down
    self.check_datanode_shutdown(hdfs_binary)

  def status(self, env):
    import status_params

    env.set_params(status_params)
    datanode(action="status")

  @retry(times=24, sleep_time=5, err_class=Fail)
  def check_datanode_shutdown(self, hdfs_binary):
    """
    Checks that a DataNode is down by running "hdfs dfsamin getDatanodeInfo"
    several times, pausing in between runs. Once the DataNode stops responding
    this method will return, otherwise it will raise a Fail(...) and retry
    automatically.
    The stack defaults for retrying for HDFS are also way too slow for this
    command; they are set to wait about 45 seconds between client retries. As
    a result, a single execution of dfsadmin will take 45 seconds to retry and
    the DataNode may be marked as dead, causing problems with HBase.
    https://issues.apache.org/jira/browse/HDFS-8510 tracks reducing the
    times for ipc.client.connect.retry.interval. In the meantime, override them
    here, but only for RU.
    :param hdfs_binary: name/path of the HDFS binary to use
    :return:
    """
    import params

    # override stock retry timeouts since after 30 seconds, the datanode is
    # marked as dead and can affect HBase during RU
    dfsadmin_base_command = get_dfsadmin_base_command(hdfs_binary)
    command = dfsadmin_base_command + (
      "-D",
      "ipc.client.connect.max.retries=5",
      "-D",
      "ipc.client.connect.retry.interval=1000",
      "-getDatanodeInfo",
      params.dfs_dn_ipc_address,
    )

    with hdfs_kerberos_environment(
      params,
      "ambari-hdfs-datanode-shutdown-check-",
      keytab=params.dn_keytab if params.security_enabled else None,
      principal=params.dn_principal_name if params.security_enabled else None,
    ) as command_environment:
      try:
        return_code, output = shell.call(
          command,
          user=params.hdfs_user,
          tries=1,
          env=command_environment,
          timeout=30,
          timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
          shell=False,
        )
      except Exception as error:
        Logger.warning(
          "Unable to determine whether the DataNode deregistered: %s" % error
        )
        raise Fail("Unable to determine DataNode shutdown state") from error

    # getDatanodeInfo returns a non-zero status when the DataNode is no longer
    # registered.  A successful command means it is still serving requests.
    if return_code == 0:
      Logger.info("DataNode has not yet deregistered from the NameNode...")
      raise Fail("DataNode has not yet deregistered from the NameNode...")

    Logger.info("DataNode shutdown check returned %s: %s" % (return_code, output))
    Logger.info("DataNode has successfully shutdown.")
    return True


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class DataNodeDefault(DataNode):
  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing DataNode Stack Upgrade pre-restart")
    import params

    env.set_params(params)
    if params.version and check_stack_feature(
      StackFeature.ROLLING_UPGRADE, params.version
    ):
      stack_select.select_packages(params.version)

  def post_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing DataNode Stack Upgrade post-restart")
    import params

    env.set_params(params)
    hdfs_binary = self.get_hdfs_binary()
    # ensure the DataNode has started and rejoined the cluster
    datanode_upgrade.post_upgrade_check(hdfs_binary)

  def get_log_folder(self):
    import params

    return params.hdfs_log_dir

  def get_user(self):
    import params

    return params.hdfs_user

  def get_pid_files(self):
    import status_params

    return [status_params.datanode_pid_file]


if __name__ == "__main__":
  DataNode().execute()
