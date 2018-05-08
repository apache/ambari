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
from resource_management.libraries.functions.decorator import retry


def post_upgrade_check():
  '''
  Checks that the NodeManager has rejoined the cluster.
  This function will obtain the Kerberos ticket if security is enabled.
  :return:
  '''
  import params

  Logger.info('NodeManager executing "yarn node -list -states=RUNNING" to verify the node has rejoined the cluster...')
  if params.security_enabled and params.nodemanager_kinit_cmd:
    Execute(params.nodemanager_kinit_cmd, user = params.yarn_user)

  _check_nodemanager_startup()


@retry(times=12, sleep_time=10, err_class=Fail)
def _check_nodemanager_startup():
  '''
  Checks that a NodeManager is in a RUNNING state in the cluster via
  "yarn node -list -states=RUNNING" command. Once the NodeManager is found to be
  alive this method will return, otherwise it will raise a Fail(...) and retry
  automatically.
  :return:
  '''
  import params

  command = 'yarn node -list -states=RUNNING'

  try:
    # 'su - yarn -c "yarn node -status c6401.ambari.apache.org:45454"'
    return_code, yarn_output = shell.call(command, user=params.hdfs_user)
  except:
    raise Fail('Unable to determine if the NodeManager has started after upgrade.')

  if return_code == 0:
    hostname = params.hostname.lower()
    nodemanager_address = params.nm_address.lower()
    yarn_output = yarn_output.lower()

    if hostname in yarn_output or nodemanager_address in yarn_output:
      Logger.info('NodeManager with ID {0} has rejoined the cluster.'.format(nodemanager_address))
      return
    else:
      raise Fail('NodeManager with ID {0} was not found in the list of running NodeManagers'.format(nodemanager_address))

  raise Fail('Unable to determine if the NodeManager has started after upgrade (result code {0})'.format(str(return_code)))
