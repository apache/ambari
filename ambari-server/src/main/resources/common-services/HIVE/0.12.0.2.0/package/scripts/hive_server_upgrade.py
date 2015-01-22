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

import re
from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import Execute
from resource_management.core import shell
from resource_management.libraries.functions import format


def pre_upgrade_deregister():
  """
  Runs the "hive --service hiveserver2 --deregister <version>" command to
  de-provision the server in preparation for an upgrade. This will contact
  ZooKeeper to remove the server so that clients that attempt to connect
  will be directed to other servers automatically. Once all
  clients have drained, the server will shutdown automatically; this process
  could take a very long time.
  This function will obtain the Kerberos ticket if security is enabled.
  :return:
  """
  import params

  Logger.info('HiveServer2 executing "deregister" command in preparation for upgrade...')

  if params.security_enabled:
    kinit_command=format("{kinit_path_local} -kt {smoke_user_keytab} {smokeuser_principal}; ")
    Execute(kinit_command,user=params.smokeuser)

  # calculate the current hive server version
  current_hiveserver_version = _get_current_hiveserver_version()
  if current_hiveserver_version is None:
    raise Fail('Unable to determine the current HiveServer2 version to deregister.')

  # deregister
  command = 'hive --service hiveserver2 --deregister ' + current_hiveserver_version
  Execute(command, user=params.hive_user, path=params.execute_path, tries=1 )


def _get_current_hiveserver_version():
  """
  Runs an "hdp-select status hive-server2" check and parses the result in order
  to obtain the current version of hive.

  :return:  the hiveserver2 version, such as "hdp-select status hive-server2"
  """
  import params

  try:
    command = 'hdp-select status hive-server2'
    return_code, hdp_output = shell.call(command, user=params.hive_user)
  except Exception, e:
    Logger.error(str(e))
    raise Fail('Unable to execute hdp-select command to retrieve the hiveserver2 version.')

  if return_code != 0:
    raise Fail('Unable to determine the current HiveServer2 version because of a non-zero return code of {0}'.format(str(return_code)))

  # strip "hive-server2 - " off of result and test the version
  current_hive_server_version = re.sub('hive-server2 - ', '', hdp_output)
  match = re.match('[0-9]+.[0-9]+.[0-9]+.[0-9]+-[0-9]+', current_hive_server_version)

  if match:
    return current_hive_server_version
  else:
    raise Fail('The extracted hiveserver2 version "{0}" does not matching any known pattern'.format(current_hive_server_version))


