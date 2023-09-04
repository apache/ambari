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
import os

from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute
from resource_management.core.resources.system import File
from resource_management.core import shell
from resource_management.core.shell import as_user
from resource_management.core.exceptions import Fail
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions import get_unique_id_and_date
from resource_management.libraries.functions import Direction, SafeMode
from utils import get_dfsadmin_base_command

from namenode_ha_state import NamenodeHAState


safemode_to_instruction = {SafeMode.ON: "enter",
                           SafeMode.OFF: "leave"}

NAMENODE_UPGRADE_IN_PROGRESS_MARKER_FILE = "namenode-upgrade-in-progress"

def prepare_upgrade_check_for_previous_dir():
  """
  During a NonRolling (aka Express Upgrade), preparing the NameNode requires backing up some data.
  Check that there is no "previous" folder inside the NameNode Name Dir.
  """
  import params

  if params.dfs_ha_enabled:
    namenode_ha = NamenodeHAState()
    if namenode_ha.is_active(params.hostname) or namenode_ha.is_active(params.public_hostname):
      Logger.info("NameNode High Availability is enabled and this is the Active NameNode.")

      problematic_previous_namenode_dirs = set()
      nn_name_dirs = params.dfs_name_dir.split(',')
      for nn_dir in nn_name_dirs:
        if os.path.isdir(nn_dir):
          # Check for a previous folder, which is not allowed.
          previous_dir = os.path.join(nn_dir, "previous")
          if os.path.isdir(previous_dir):
            problematic_previous_namenode_dirs.add(previous_dir)

      if len(problematic_previous_namenode_dirs) > 0:
        message = 'WARNING. The following NameNode Name Dir(s) have a "previous" folder from an older version.\n' \
                  'Please back it up first, and then delete it, OR Finalize (E.g., "hdfs dfsadmin -finalizeUpgrade").\n' \
                  'NameNode Name Dir(s): {0}\n' \
                  '***** Then, retry this step. *****'.format(", ".join(problematic_previous_namenode_dirs))
        Logger.error(message)
        raise Fail(message)

def prepare_upgrade_enter_safe_mode(hdfs_binary):
  """
  During a NonRolling (aka Express Upgrade), preparing the NameNode requires first entering Safemode.
  :param hdfs_binary: name/path of the HDFS binary to use
  """
  import params

  dfsadmin_base_command = get_dfsadmin_base_command(hdfs_binary)
  safe_mode_enter_cmd = dfsadmin_base_command + " -safemode enter"
  try:
    # Safe to call if already in Safe Mode
    desired_state = SafeMode.ON
    safemode_transition_successful, original_state = reach_safemode_state(params.hdfs_user, desired_state, params.dfs_ha_enabled, hdfs_binary)
    Logger.info("Transition successful: {0}, original state: {1}".format(str(safemode_transition_successful), str(original_state)))
    if not safemode_transition_successful:
      raise Fail("Could not transition to safemode state %s. Please check logs to make sure namenode is up." % str(desired_state))
  except Exception as e:
    message = "Could not enter safemode. Error: {0}. As the HDFS user, call this command: {1}".format(str(e), safe_mode_enter_cmd)
    Logger.error(message)
    raise Fail(message)

def prepare_upgrade_save_namespace(hdfs_binary):
  """
  During a NonRolling (aka Express Upgrade), preparing the NameNode requires saving the namespace.
  :param hdfs_binary: name/path of the HDFS binary to use
  """
  import params

  dfsadmin_base_command = get_dfsadmin_base_command(hdfs_binary)
  save_namespace_cmd = dfsadmin_base_command + " -saveNamespace"
  try:
    Logger.info("Checkpoint the current namespace.")
    as_user(save_namespace_cmd, params.hdfs_user, env={'PATH': params.hadoop_bin_dir})
  except Exception as e:
    message = format("Could not save the NameSpace. As the HDFS user, call this command: {save_namespace_cmd}")
    Logger.error(message)
    raise Fail(message)

def prepare_upgrade_backup_namenode_dir():
  """
  During a NonRolling (aka Express Upgrade), preparing the NameNode requires backing up the NameNode Name Dirs.
  """
  import params

  i = 0
  failed_paths = []
  nn_name_dirs = params.dfs_name_dir.split(',')
  backup_destination_root_dir = "{0}/{1}".format(params.namenode_backup_dir, params.stack_version_unformatted)
  if len(nn_name_dirs) > 0:
    Logger.info("Backup the NameNode name directory's CURRENT folder.")
  for nn_dir in nn_name_dirs:
    i += 1
    namenode_current_image = os.path.join(nn_dir, "current")
    unique = get_unique_id_and_date() + "_" + str(i)
    # Note that /tmp may not be writeable.
    backup_current_folder = "{0}/namenode_{1}/".format(backup_destination_root_dir, unique)

    if os.path.isdir(namenode_current_image) and not os.path.isdir(backup_current_folder):
      try:
        os.makedirs(backup_current_folder)
        Execute(('cp', '-ar', namenode_current_image, backup_current_folder),
                sudo=True
        )
      except Exception as e:
        failed_paths.append(namenode_current_image)
  if len(failed_paths) > 0:
    Logger.error("Could not backup the NameNode Name Dir(s) to {0}, make sure that the destination path is "
                 "writeable and copy the directories on your own. Directories: {1}".format(backup_destination_root_dir,
                                                                                           ", ".join(failed_paths)))

def prepare_upgrade_finalize_previous_upgrades(hdfs_binary):
  """
  During a NonRolling (aka Express Upgrade), preparing the NameNode requires Finalizing any upgrades that are in progress.
  :param hdfs_binary: name/path of the HDFS binary to use
  """
  import params

  dfsadmin_base_command = get_dfsadmin_base_command(hdfs_binary)
  finalize_command = dfsadmin_base_command + " -rollingUpgrade finalize"
  try:
    Logger.info("Attempt to Finalize if there are any in-progress upgrades. "
                "This will return 255 if no upgrades are in progress.")
    code, out = shell.checked_call(finalize_command, logoutput=True, user=params.hdfs_user)
    if out:
      expected_substring = "there is no rolling upgrade in progress"
      if expected_substring not in out.lower():
        Logger.warning('Finalize command did not contain substring: %s' % expected_substring)
    else:
      Logger.warning("Finalize command did not return any output.")
  except Exception as e:
    Logger.warning("Ensure no upgrades are in progress.")

def reach_safemode_state(user, safemode_state, in_ha, hdfs_binary):
  """
  Enter or leave safemode for the Namenode.
  :param user: user to perform action as
  :param safemode_state: Desired state of ON or OFF
  :param in_ha: bool indicating if Namenode High Availability is enabled
  :param hdfs_binary: name/path of the HDFS binary to use
  :return: Returns a tuple of (transition success, original state). If no change is needed, the indicator of
  success will be True
  """
  Logger.info("Prepare to transition into safemode state %s" % safemode_state)
  import params
  original_state = SafeMode.UNKNOWN

  dfsadmin_base_command = get_dfsadmin_base_command(hdfs_binary)
  safemode_base_command = dfsadmin_base_command + " -safemode "
  safemode_check_cmd = safemode_base_command + " get"

  grep_pattern = format("Safe mode is {safemode_state}")
  safemode_check_with_grep = format("{safemode_check_cmd} | grep '{grep_pattern}'")

  code, out = shell.call(safemode_check_cmd, user=user, logoutput=True)
  Logger.info("Command: %s\nCode: %d." % (safemode_check_cmd, code))
  if code == 0 and out is not None:
    Logger.info(out)
    re_pattern = r"Safe mode is (\S*)"
    Logger.info("Pattern to search: {0}".format(re_pattern))
    m = re.search(re_pattern, out, re.IGNORECASE)
    if m and len(m.groups()) >= 1:
      original_state = m.group(1).upper()

      if original_state == safemode_state:
        return (True, original_state)
      else:
        # Make a transition
        command = safemode_base_command + safemode_to_instruction[safemode_state]
        Execute(command,
                user=user,
                logoutput=True,
                path=[params.hadoop_bin_dir])

        code, out = shell.call(safemode_check_with_grep, user=user)
        Logger.info("Command: %s\nCode: %d. Out: %s" % (safemode_check_with_grep, code, out))
        if code == 0:
          return (True, original_state)
  return (False, original_state)


def prepare_rolling_upgrade(hdfs_binary):
  """
  This can be called during either Rolling Upgrade or Express Upgrade (aka nonrolling)

  Rolling Upgrade for HDFS Namenode requires the following.
  0. Namenode must be up
  1. If HA: leave safemode if the safemode status is not OFF
  2. Execute a rolling upgrade "prepare"
  3. Execute a rolling upgrade "query"
  :param hdfs_binary: name/path of the HDFS binary to use
  """
  import params

  if not params.upgrade_direction or params.upgrade_direction not in [Direction.UPGRADE, Direction.DOWNGRADE]:
    raise Fail("Could not retrieve upgrade direction: %s" % str(params.upgrade_direction))
  Logger.info(format("Performing a(n) {params.upgrade_direction} of HDFS"))

  if params.security_enabled:
    kinit_command = format("{params.kinit_path_local} -kt {params.hdfs_user_keytab} {params.hdfs_principal_name}") 
    Execute(kinit_command, user=params.hdfs_user, logoutput=True)

  if params.upgrade_direction == Direction.UPGRADE:
    if params.dfs_ha_enabled:
      Logger.info('High Availability is enabled, must leave safemode before calling "-rollingUpgrade prepare"')
      desired_state = SafeMode.OFF
      safemode_transition_successful, original_state = reach_safemode_state(params.hdfs_user, desired_state, True, hdfs_binary)
      if not safemode_transition_successful:
        raise Fail("Could not transition to safemode state %s. Please check logs to make sure namenode is up." % str(desired_state))

    dfsadmin_base_command = get_dfsadmin_base_command(hdfs_binary)
    prepare = dfsadmin_base_command + " -rollingUpgrade prepare"
    query = dfsadmin_base_command + " -rollingUpgrade query"
    Execute(prepare,
            user=params.hdfs_user,
            logoutput=True)
    Execute(query,
            user=params.hdfs_user,
            logoutput=True)

def finalize_upgrade(upgrade_type, hdfs_binary):
  """
  Finalize the Namenode upgrade, at which point it cannot be downgraded.
  :param upgrade_type rolling or nonrolling
  :param hdfs_binary: name/path of the HDFS binary to use
  """
  Logger.info("Executing Rolling Upgrade finalize")
  import params

  if params.security_enabled:
    kinit_command = format("{params.kinit_path_local} -kt {params.hdfs_user_keytab} {params.hdfs_principal_name}") 
    Execute(kinit_command, user=params.hdfs_user, logoutput=True)

  dfsadmin_base_command = get_dfsadmin_base_command(hdfs_binary)
  finalize_cmd = dfsadmin_base_command + " -rollingUpgrade finalize"
  query_cmd = dfsadmin_base_command + " -rollingUpgrade query"

  Execute(query_cmd,
        user=params.hdfs_user,
        logoutput=True)
  Execute(finalize_cmd,
          user=params.hdfs_user,
          logoutput=True)
  Execute(query_cmd,
          user=params.hdfs_user,
          logoutput=True)

  # upgrade is finalized; remove the upgrade marker
  delete_upgrade_marker()


def get_upgrade_in_progress_marker():
  """
  Gets the full path of the file which indicates that NameNode has begun its stack upgrade.
  :return:
  """
  from resource_management.libraries.script.script import Script
  return os.path.join(Script.get_tmp_dir(), NAMENODE_UPGRADE_IN_PROGRESS_MARKER_FILE)


def create_upgrade_marker():
  """
  Creates the marker file indicating that NameNode has begun participating in a stack upgrade.
  If the file already exists, nothing will be done. This will silently log exceptions on failure.
  :return:
  """
  # create the marker file which indicates
  try:
    namenode_upgrade_in_progress_marker = get_upgrade_in_progress_marker()
    if not os.path.isfile(namenode_upgrade_in_progress_marker):
      File(namenode_upgrade_in_progress_marker)
  except:
    Logger.warning("Unable to create NameNode upgrade marker file {0}".format(namenode_upgrade_in_progress_marker))


def delete_upgrade_marker():
  """
  Removes the marker file indicating that NameNode has begun participating in a stack upgrade.
  If the file does not exist, then nothing will be done.
  Failure to remove this file could cause problems with restarts in the future. That's why
  checking to see if there is a suspended upgrade is also advised. This function will raise
  an exception if the file can't be removed.
  :return:
  """
  # create the marker file which indicates
  try:
    namenode_upgrade_in_progress_marker = get_upgrade_in_progress_marker()
    if os.path.isfile(namenode_upgrade_in_progress_marker):
      File(namenode_upgrade_in_progress_marker, action='delete')
  except:
    error_message = "Unable to remove NameNode upgrade marker file {0}".format(namenode_upgrade_in_progress_marker)
    Logger.error(error_message)
    raise Fail(error_message)

