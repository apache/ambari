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

import hdfs_process

from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import Execute
from resource_management.core import shell
from resource_management.libraries.functions.decorator import retry
from resource_management.core import ComponentIsNotRunning
from utils import get_dfsadmin_base_command


def pre_rolling_upgrade_shutdown(hdfs_binary):
  """
  Runs the "shutdownDatanode {ipc_address} upgrade" command to shutdown the
  DataNode in preparation for an upgrade. This will then periodically check
  "getDatanodeInfo" to ensure the DataNode has shutdown correctly.
  This function will obtain the Kerberos ticket if security is enabled.
  :param hdfs_binary: name/path of the HDFS binary to use
  :return: True when the graceful shutdown command succeeds, otherwise False
    so the caller can stop the DataNode forcefully.
  """
  import params

  Logger.info(
    'DataNode executing "shutdownDatanode" command in preparation for upgrade...'
  )
  if params.security_enabled:
    Execute(params.dn_kinit_cmd, user=params.hdfs_user)

  dfsadmin_base_command = get_dfsadmin_base_command(hdfs_binary)
  command = dfsadmin_base_command + (
    "-shutdownDatanode",
    params.dfs_dn_ipc_address,
    "upgrade",
  )

  code, output = shell.call(command, user=params.hdfs_user)
  if code != 0:
    Logger.warning(
      "DataNode graceful shutdown failed with exit code "
      f"{code}; falling back to the normal stop path. Output: {output!r}"
    )
    return False
  return True


def post_upgrade_check(hdfs_binary):
  """
  Verifies that the DataNode has rejoined the cluster. This function will
  obtain the Kerberos ticket if security is enabled.
  :param hdfs_binary: name/path of the HDFS binary to use
  :return:
  """
  import params

  Logger.info("Checking that the DataNode has rejoined the cluster after upgrade...")
  if params.security_enabled:
    Execute(params.dn_kinit_cmd, user=params.hdfs_user)

  # verify that the datanode has started and rejoined the HDFS cluster
  _check_datanode_startup(hdfs_binary)


def is_datanode_process_running():
  import params

  privileged = (
    params.security_enabled and params.secure_dn_ports_are_in_use
  )
  expected_user = params.root_user if privileged else params.hdfs_user
  try:
    hdfs_process.check_component_status(
      params.datanode_pid_file,
      expected_user,
      "datanode",
      privileged=privileged,
    )
    return True
  except ComponentIsNotRunning:
    return False


@retry(times=30, sleep_time=30, err_class=Fail)  # keep trying for 15 mins
def _check_datanode_startup(hdfs_binary):
  """
  Checks that a DataNode process is running and DataNode is reported as being alive via the
  "hdfs dfsadmin -fs {namenode_address} -report -live" command. Once the DataNode is found to be
  alive this method will return, otherwise it will raise a Fail(...) and retry
  automatically.
  :param hdfs_binary: name/path of the HDFS binary to use
  :return:
  """

  if not is_datanode_process_running():
    Logger.info("DataNode process is not running")
    raise Fail("DataNode process is not running")

  import params
  import socket

  try:
    dfsadmin_base_command = get_dfsadmin_base_command(hdfs_binary)
    command = dfsadmin_base_command + ("-report", "-live")
    return_code, hdfs_output = shell.call(command, user=params.hdfs_user)
  except Exception as error:
    raise Fail(
      "Unable to determine if the DataNode has started after upgrade."
    ) from error

  if return_code == 0:
    hostname = params.hostname.lower()
    hostname_ip = socket.gethostbyname(params.hostname.lower())
    if hostname in hdfs_output.lower() or hostname_ip in hdfs_output.lower():
      Logger.info(
        f"DataNode {params.hostname} reports that it has rejoined the cluster."
      )
      return
    else:
      raise Fail(
        f"DataNode {params.hostname} was not found in the list of live DataNodes"
      )

  # return_code is not 0, fail
  raise Fail(
    f"Unable to determine if the DataNode has started after upgrade (result code {str(return_code)})"
  )
