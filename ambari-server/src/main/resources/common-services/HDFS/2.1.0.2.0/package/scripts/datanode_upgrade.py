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

from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import Execute
from resource_management.core import shell
from resource_management.libraries.functions import format
from resource_management.libraries.functions.decorator import retry


def pre_upgrade_shutdown():
  """
  Runs the "shutdownDatanode {ipc_address} upgrade" command to shutdown the
  DataNode in preparation for an upgrade. This will then periodically check
  "getDatanodeInfo" to ensure the DataNode has shutdown correctly.
  This function will obtain the Kerberos ticket if security is enabled.
  :return:
  """
  import params

  Logger.info('DataNode executing "shutdownDatanode" command in preparation for upgrade...')
  if params.security_enabled:
    Execute(params.dn_kinit_cmd, user = params.hdfs_user)

  command = format('hdfs dfsadmin -shutdownDatanode {dfs_dn_ipc_address} upgrade')
  Execute(command, user=params.hdfs_user, tries=1 )

  # verify that the datanode is down
  _check_datanode_shutdown()


def post_upgrade_check():
  """
  Verifies that the DataNode has rejoined the cluster. This function will
  obtain the Kerberos ticket if security is enabled.
  :return:
  """
  import params

  Logger.info("Checking that the DataNode has rejoined the cluster after upgrade...")
  if params.security_enabled:
    Execute(params.dn_kinit_cmd,user = params.hdfs_user)

  # verify that the datanode has started and rejoined the HDFS cluster
  _check_datanode_startup()


@retry(times=12, sleep_time=10, err_class=Fail)
def _check_datanode_shutdown():
  """
  Checks that a DataNode is down by running "hdfs dfsamin getDatanodeInfo"
  several times, pausing in between runs. Once the DataNode stops responding
  this method will return, otherwise it will raise a Fail(...) and retry
  automatically.
  :return:
  """
  import params

  command = format('hdfs dfsadmin -getDatanodeInfo {dfs_dn_ipc_address}')

  try:
    Execute(command, user=params.hdfs_user, tries=1)
  except:
    Logger.info("DataNode has successfully shutdown for upgrade.")
    return

  Logger.info("DataNode has not shutdown.")
  raise Fail('DataNode has not shutdown.')


@retry(times=12, sleep_time=10, err_class=Fail)
def _check_datanode_startup():
  """
  Checks that a DataNode is reported as being alive via the
  "hdfs dfsadmin -report -live" command. Once the DataNode is found to be
  alive this method will return, otherwise it will raise a Fail(...) and retry
  automatically.
  :return:
  """
  import params

  try:
    # 'su - hdfs -c "hdfs dfsadmin -report -live"'
    command = 'hdfs dfsadmin -report -live'
    return_code, hdfs_output = shell.call(command, user=params.hdfs_user)
  except:
    raise Fail('Unable to determine if the DataNode has started after upgrade.')

  if return_code == 0:
    if params.hostname.lower() in hdfs_output.lower():
      Logger.info("DataNode {0} reports that it has rejoined the cluster.".format(params.hostname))
      return
    else:
      raise Fail("DataNode {0} was not found in the list of live DataNodes".format(params.hostname))

  # return_code is not 0, fail
  raise Fail("Unable to determine if the DataNode has started after upgrade (result code {0})".format(str(return_code)))
