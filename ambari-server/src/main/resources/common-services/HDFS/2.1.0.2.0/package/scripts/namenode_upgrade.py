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
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions.format import format
from resource_management.core.shell import call
from resource_management.core.exceptions import Fail


class SafeMode:
  ON = "ON"
  OFF = "OFF"
  UNKNOWN = "UNKNOWN"

safemode_to_instruction = {SafeMode.ON: "enter",
                           SafeMode.OFF: "leave"}

def reach_safemode_state(user, safemode_state, in_ha):
  """
  Enter or leave safemode for the Namenode.
  @param user: user to perform action as
  @param safemode_state: Desired state of ON or OFF
  @param in_ha: bool indicating if Namenode High Availability is enabled
  @:return Returns a tuple of (transition success, original state). If no change is needed, the indicator of
  success will be True
  """
  Logger.info("Prepare to transition into safemode state %s" % safemode_state)
  import params
  original_state = SafeMode.UNKNOWN

  hostname = params.hostname
  safemode_check = format("su - {user} -c 'hdfs dfsadmin -safemode get'")

  grep_pattern = format("Safe mode is {safemode_state} in {hostname}") if in_ha else format("Safe mode is {safemode_state}")
  safemode_check_with_grep = format("su - {user} -c 'hdfs dfsadmin -safemode get | grep \"{grep_pattern}\"'")
  code, out = call(safemode_check)
  Logger.info("Command: %s\nCode: %d." % (safemode_check, code))
  if code == 0 and out is not None:
    Logger.info(out)
    re_pattern = r"Safe mode is (\S*) in " + hostname.replace(".", "\\.") if in_ha else r"Safe mode is (\S*)"
    m = re.search(re_pattern, out, re.IGNORECASE)
    if m and len(m.groups()) >= 1:
      original_state = m.group(1).upper()

      if original_state == safemode_state:
        return (True, original_state)
      else:
        # Make a transition
        command = "hdfs dfsadmin -safemode %s" % (safemode_to_instruction[safemode_state])
        Execute(command,
                user=user,
                logoutput=True,
                path=[params.hadoop_bin_dir])

        code, out = call(safemode_check_with_grep)
        Logger.info("Command: %s\nCode: %d. Out: %s" % (safemode_check_with_grep, code, out))
        if code == 0:
          return (True, original_state)
  return (False, original_state)


def prepare_rolling_upgrade():
  """
  Rolling Upgrade for HDFS Namenode requires the following.
  0. Namenode must be up
  1. Leave safemode if the safemode status is not OFF
  2. Execute a rolling upgrade "prepare"
  3. Execute a rolling upgrade "query"
  """
  Logger.info("Executing Rolling Upgrade prepare")
  import params

  if params.security_enabled:
    Execute(format("{params.kinit_path_local} -kt {params.hdfs_user_keytab} {params.hdfs_principal_name}"))

  safemode_transition_successful, original_state = reach_safemode_state(params.hdfs_user, SafeMode.OFF, True)
  if not safemode_transition_successful:
    raise Fail("Could not transition to safemode state %s. Please check logs to make sure namenode is up." % str(SafeMode.OFF))

  prepare = "hdfs dfsadmin -rollingUpgrade prepare"
  query = "hdfs dfsadmin -rollingUpgrade query"
  Execute(prepare,
          user=params.hdfs_user,
          logoutput=True)
  Execute(query,
          user=params.hdfs_user,
          logoutput=True)


def finalize_rolling_upgrade():
  """
  Finalize the Namenode upgrade, at which point it cannot be downgraded.
  """
  Logger.info("Executing Rolling Upgrade finalize")
  import params

  if params.security_enabled:
    Execute(format("{params.kinit_path_local} -kt {params.hdfs_user_keytab} {params.hdfs_principal_name}"))

  finalize_cmd = "hdfs dfsadmin -rollingUpgrade finalize"
  query_cmd = "hdfs dfsadmin -rollingUpgrade query"

  Execute(query_cmd,
        user=params.hdfs_user,
        logoutput=True)
  Execute(finalize_cmd,
          user=params.hdfs_user,
          logoutput=True)
  Execute(query_cmd,
          user=params.hdfs_user,
          logoutput=True)