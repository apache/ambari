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
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions.format import format
from resource_management.core.shell import call
from resource_management.core.exceptions import Fail


class SAFEMODE:
  ON = "ON"
  OFF = "OFF"


safemode_to_instruction = {SAFEMODE.ON: "enter",
                           SAFEMODE.OFF: "leave"}


def reach_safemode_state(secure_user, safemode_state, in_ha):
  """
  Enter or leave safemode for the Namenode.
  @param secure_user: user to perform action as
  @param safemode_state: ON or OFF
  @param in_ha: bool indicating if Namenode High Availability is enabled
  @:return True if successful, false otherwise.
  """
  Logger.info("Prepare to leave safemode")
  import params

  hostname = params.hostname
  grep = format("Safe mode is {safemode_state} in {hostname}") if in_ha else format("Safe mode is {safemode_state}")
  safemode_check = format("su - {secure_user} -c 'hdfs dfsadmin -safemode get | grep \"{grep}\"'")
  code, out = call(safemode_check)
  Logger.info("Command: %s\nCode: %d." % (safemode_check, code))
  if code != 0:
    command = "hdfs dfsadmin -safemode %s" % (safemode_to_instruction[safemode_state])
    Execute(command,
            user=secure_user,
            logoutput=True)

    code, out = call(safemode_check)
    Logger.info("Command: %s\nCode: %d. Out: %s" % (safemode_check, code, out))
    if code != 0:
      return False
  return True


def prepare_rolling_upgrade():
  """
  Rolling Upgrade for HDFS Namenode requires the following.
  1. Leave safemode if the safemode status is not OFF
  2. Execute a rolling upgrade "prepare"
  3. Execute a rolling upgrade "query"
  """
  Logger.info("Executing Rolling Upgrade prepare")
  import params

  secure_user = params.hdfs_principal_name if params.security_enabled else params.hdfs_user

  if params.security_enabled:
    Execute(format("{kinit_path_local} -kt {hdfs_user_keytab} {hdfs_principal_name}"),
            user=secure_user)

  safemode_transition_successful = reach_safemode_state(secure_user, SAFEMODE.OFF, True)
  if not safemode_transition_successful:
    raise Fail("Could leave safemode")

  prepare = "hdfs dfsadmin -rollingUpgrade prepare"
  query = "hdfs dfsadmin -rollingUpgrade query"
  Execute(prepare,
          user=secure_user,
          logoutput=True)
  Execute(query,
          user=secure_user,
          logoutput=True)


def finalize_rolling_upgrade():
  """
  Finalize the Namenode upgrade, at which point it cannot be downgraded.
  """
  Logger.info("Executing Rolling Upgrade finalize")
  import params

  secure_user = params.hdfs_principal_name if params.security_enabled else params.hdfs_user
  finalize_cmd = "hdfs dfsadmin -rollingUpgrade finalize"
  Execute(finalize_cmd,
          user=secure_user,
          logoutput=True)

  safemode_transition_successful = reach_safemode_state(secure_user, SAFEMODE.OFF, True)
  if not safemode_transition_successful:
    Logger.warning("Could leave safemode")