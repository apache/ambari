#!/usr/bin/env python
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

Ambari Agent

"""

import os
import shutil
import socket
from ambari_commons.os_check import OSCheck
from resource_management.libraries.script import Script
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_tools
from resource_management.libraries.functions.constants import Direction
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.version import format_stack_version
from resource_management.core import shell
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute, Link, Directory
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions import StackFeature

class UpgradeSetAll(Script):
  """
  This script is a part of stack upgrade workflow and is used to set the
  all of the component versions as a final step in the upgrade process
  """

  def actionexecute(self, env):
    version = default('/commandParams/version', None)

    if not version:
      raise Fail("Value is required for '/commandParams/version'")
  
    # other os?
    if OSCheck.is_redhat_family():
      cmd = ('/usr/bin/yum', 'clean', 'all')
      code, out = shell.call(cmd, sudo=True)

    formatted_version = format_stack_version(version)
    if not formatted_version:
      raise Fail("Unable to determine a properly formatted stack version from {0}".format(version))

    stack_selector_path = stack_tools.get_stack_tool_path(stack_tools.STACK_SELECTOR_NAME)

    # this script runs on all hosts; if this host doesn't have stack components,
    # then don't invoke the stack tool
    # (no need to log that it's skipped - the function will do that)
    if is_host_skippable(stack_selector_path, formatted_version):
      return

    # invoke "set all"
    cmd = ('ambari-python-wrap', stack_selector_path, 'set', 'all', version)
    code, out = shell.call(cmd, sudo=True)
    if code != 0:
      raise Exception("Command '{0}' exit code is nonzero".format(cmd))

    if check_stack_feature(StackFeature.CONFIG_VERSIONING, formatted_version):
      # backup the old and symlink /etc/[component]/conf to <stack-root>/current/[component]
      for k, v in conf_select.get_package_dirs().iteritems():
        for dir_def in v:
          link_config(dir_def['conf_dir'], dir_def['current_dir'])


  def unlink_all_configs(self, env):
    """
    Reverses the work performed in link_config. This should only be used when downgrading from
    HDP 2.3 to 2.2 in order to under the symlink work required for 2.3.
    """
    stack_name = default('/hostLevelParams/stack_name', "").upper()
    downgrade_to_version = default('/commandParams/version', None)
    downgrade_from_version = default('/commandParams/downgrade_from_version', None)
    upgrade_direction = default("/commandParams/upgrade_direction", Direction.UPGRADE)

    # downgrade only
    if upgrade_direction != Direction.DOWNGRADE:
      Logger.warning("Unlinking configurations should only be performed on a downgrade.")
      return

    if downgrade_to_version is None or downgrade_from_version is None:
      Logger.warning("Both 'commandParams/version' and 'commandParams/downgrade_from_version' must be specified to unlink configs on downgrade.")
      return

    Logger.info("Unlinking all configs when downgrading from {0} {1} to {2}".format(
        stack_name, downgrade_from_version, downgrade_to_version))

    # normalize the versions
    downgrade_to_version = format_stack_version(downgrade_to_version)
    downgrade_from_version = format_stack_version(downgrade_from_version)

    # downgrade-to-version must be 2.2 (less than 2.3)
    if downgrade_to_version and check_stack_feature(StackFeature.CONFIG_VERSIONING, downgrade_to_version):
      Logger.warning("Unlinking configurations should not be performed when downgrading {0} {1} to {2}".format(
          stack_name, downgrade_from_version, downgrade_to_version))
      return

    # downgrade-from-version must be 2.3+
    if not( downgrade_from_version and check_stack_feature(StackFeature.CONFIG_VERSIONING, downgrade_from_version) ):
      Logger.warning("Unlinking configurations should not be performed when downgrading {0} {1} to {2}".format(
          stack_name, downgrade_from_version, downgrade_to_version))
      return

    # iterate through all directory conf mappings and undo the symlinks
    for key, value in conf_select.get_package_dirs().iteritems():
      for directory_mapping in value:
        original_config_directory = directory_mapping['conf_dir']
        self._unlink_config(original_config_directory)


  def _unlink_config(self, original_conf_directory):
    """
    Reverses the work performed in link_config. This should only be used when downgrading from
    HDP 2.3 to 2.2 in order to undo the conf symlink work required for 2.3.

    1. Checks if conf.backup exists, if not then do no work
    2. Check for existance of 'etc' symlink and remove it
    3. Rename conf.back back to conf

    :original_conf_directory: the original conf directory that was made into a symlink (/etc/component/conf)
    """
    # calculate the parent and backup directories
    original_conf_parent_directory = os.path.abspath(os.path.join(original_conf_directory, os.pardir))
    backup_conf_directory = os.path.join(original_conf_parent_directory, "conf.backup")
    Logger.info("Analyzing potential link {0}".format(original_conf_directory))

    if os.path.islink(original_conf_directory):
      # remove the old symlink
      Execute(("rm", original_conf_directory), sudo=True)
    elif os.path.isdir(original_conf_directory):
      Directory(original_conf_directory, action="delete")
    else:
      Logger.info("  Skipping the unlink of {0}; it is not a symlink or does not exist".format(original_conf_directory))

    if os.path.isdir(backup_conf_directory):
      # rename the backup to the original name
      Logger.info("  Unlinking {0} and restoring {1}".format(original_conf_directory, backup_conf_directory))
      Execute(("mv", backup_conf_directory, original_conf_directory), sudo=True)
    else:
      Logger.info("  Skipping restoring config from backup {0} since it does not exist".format(backup_conf_directory))


def is_host_skippable(stack_selector_path, formatted_version):
  """
  Gets whether this host should not have the stack select tool called.
  :param stack_selector_path  the path to the stack selector tool.
  :param formatted_version: the version to use with the stack selector tool.
  :return: True if this host should be skipped, False otherwise.
  """
  if not os.path.exists(stack_selector_path):
    Logger.info("{0} does not have any stack components installed and will not invoke {1}".format(
      socket.gethostname(), stack_selector_path))

    return True

  # invoke the tool, checking its output
  cmd = ('ambari-python-wrap', stack_selector_path, "versions")
  code, out = shell.call(cmd, sudo=True)

  if code != 0:
    Logger.info("{0} is unable to determine which stack versions are available using {1}".format(
        socket.gethostname(), stack_selector_path))

    return True

  # check to see if the output is empty, indicating no versions installed
  if not out.strip():
    Logger.info("{0} has no stack versions installed".format(socket.gethostname()))
    return True

  return False


def link_config(old_conf, link_conf):
  """
  Creates a config link following:
  1. Checks if the old_conf location exists
  2. If it does, check if it's a link already
  3. Make a copy to /etc/[component]/conf.backup
  4. Remove the old directory and create a symlink to link_conf

  :old_conf: the old config directory, ie /etc/[component]/conf
  :link_conf: the new target for the config directory, ie <stack-root>/current/[component-dir]/conf
  """
  if os.path.islink(old_conf):
    # if the link exists but is wrong, then change it
    if os.path.realpath(old_conf) != link_conf:
      Link(old_conf, to = link_conf)
    else:
      Logger.debug("Skipping {0}; it is already a link".format(old_conf))
    return

  if not os.path.exists(old_conf):
    Logger.debug("Skipping {0}; it does not exist".format(old_conf))
    return

  old_parent = os.path.abspath(os.path.join(old_conf, os.pardir))

  Logger.info("Linking {0} to {1}".format(old_conf, link_conf))

  old_conf_copy = os.path.join(old_parent, "conf.backup")
  if not os.path.exists(old_conf_copy):
    Execute(("cp", "-R", "-p", old_conf, old_conf_copy), sudo=True, logoutput=True)

  shutil.rmtree(old_conf, ignore_errors=True)

  # link /etc/[component]/conf -> <stack-root>/current/[component]-client/conf
  Link(old_conf, to = link_conf)


if __name__ == "__main__":
  UpgradeSetAll().execute()
