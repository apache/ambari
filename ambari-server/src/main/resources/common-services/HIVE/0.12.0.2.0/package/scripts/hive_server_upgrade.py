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

import os
import re
from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import Execute
from resource_management.core import shell
from resource_management.libraries.functions import format
from resource_management.libraries.functions import hdp_select
from resource_management.libraries.functions.version import format_hdp_stack_version
from resource_management.libraries.functions.version import compare_versions


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

  # fallback when upgrading because /usr/hdp/current/hive-server2/conf/conf.server may not exist
  hive_server_conf_dir = params.hive_server_conf_dir
  if not os.path.exists(hive_server_conf_dir):
    hive_server_conf_dir = "/etc/hive/conf.server"

  # deregister
  hive_execute_path = params.execute_path
  # If upgrading, the upgrade-target hive binary should be used to call the --deregister command.
  # If downgrading, the downgrade-source hive binary should be used to call the --deregister command.
  if "upgrade" == params.upgrade_direction:
    # hive_bin
    upgrade_target_version = format_hdp_stack_version(params.version)
    if upgrade_target_version and compare_versions(upgrade_target_version, "2.2") >= 0:
      upgrade_target_hive_bin = format('/usr/hdp/{version}/hive/bin')
      if (os.pathsep + params.hive_bin) in hive_execute_path:
        hive_execute_path = hive_execute_path.replace(os.pathsep + params.hive_bin, os.pathsep + upgrade_target_hive_bin)
    # hadoop_bin_dir
    upgrade_target_hadoop_bin = hdp_select.get_hadoop_dir("bin", upgrade_stack_only=True)
    upgrade_source_hadoop_bin = params.hadoop_bin_dir
    if upgrade_target_hadoop_bin and len(upgrade_target_hadoop_bin) > 0 and (os.pathsep + upgrade_source_hadoop_bin) in hive_execute_path:
      hive_execute_path = hive_execute_path.replace(os.pathsep + upgrade_source_hadoop_bin, os.pathsep + upgrade_target_hadoop_bin)

  command = format('hive --config {hive_server_conf_dir} --service hiveserver2 --deregister ' + current_hiveserver_version)
  Execute(command, user=params.hive_user, path=hive_execute_path, tries=1 )


def _get_current_hiveserver_version():
  """
  Runs "hive --version" and parses the result in order
  to obtain the current version of hive.

  :return:  the hiveserver2 version, returned by "hive --version"
  """
  import params

  try:
    command = 'hive --version'
    return_code, hdp_output = shell.call(command, user=params.hive_user, path=params.execute_path)
  except Exception, e:
    Logger.error(str(e))
    raise Fail('Unable to execute hive --version command to retrieve the hiveserver2 version.')

  if return_code != 0:
    raise Fail('Unable to determine the current HiveServer2 version because of a non-zero return code of {0}'.format(str(return_code)))

  match = re.search('^(Hive) ([0-9]+.[0-9]+.\S+)', hdp_output, re.MULTILINE)

  if match:
    current_hive_server_version = match.group(2)
    return current_hive_server_version
  else:
    raise Fail('The extracted hiveserver2 version "{0}" does not matching any known pattern'.format(hdp_output))


