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
from ambari_commons.os_check import OSCheck
from resource_management.libraries.script import Script
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.version import compare_versions
from resource_management.libraries.functions.version import format_hdp_stack_version
from resource_management.core import shell
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute
from resource_management.core.shell import as_sudo

class UpgradeSetAll(Script):
  """
  This script is a part of Rolling Upgrade workflow and is used to set the
  component versions as a final step in the upgrade process
  """

  def actionexecute(self, env):
    config = Script.get_config()

    version = default('/commandParams/version', None)
    stack_name = default('/hostLevelParams/stack_name', "")

    if not version:
      raise Fail("Value is required for '/commandParams/version'")
  
    # other os?
    if OSCheck.is_redhat_family():
      cmd = "/usr/bin/yum clean all"
      code, out = shell.call(cmd)
      Logger.info("Command: {0}\nCode: {1}, Out: {2}".format(cmd, str(code), str(out)))

    min_ver = format_hdp_stack_version("2.2")
    real_ver = format_hdp_stack_version(version)
    if stack_name == "HDP":
      if compare_versions(real_ver, min_ver) >= 0:
        cmd = "hdp-select set all {0}".format(version)
        code, out = shell.call(cmd)
        Logger.info("Command: {0}\nCode: {1}, Out: {2}".format(cmd, str(code), str(out)))

      if compare_versions(real_ver, format_hdp_stack_version("2.3")) >= 0:
        # backup the old and symlink /etc/[component]/conf to /usr/hdp/current/[component]
        for k, v in conf_select.PACKAGE_DIRS.iteritems():
          link_config(v['conf_dir'], v['current_dir'])

def link_config(old_conf, link_conf):
  """
  Creates a config link following:
  1. Checks if the old_conf location exists
  2. If it does, check if it's a link already
  3. Make a copy to /etc/[component]/conf.backup
  4. Remove the old directory and create a symlink to link_conf
  :old_conf: the old config directory, ie /etc/[component]/config
  :link_conf: the new target for the config directory, ie /usr/hdp/current/[component-dir]/conf
  """
  if not os.path.exists(old_conf):
    Logger.debug("Skipping {0}; it does not exist".format(old_conf))
    return
  
  if os.path.islink(old_conf):
    Logger.debug("Skipping {0}; it is already a link".format(old_conf))
    return

  old_parent = os.path.abspath(os.path.join(old_conf, os.pardir))

  Logger.info("Linking {0} to {1}".format(old_conf, link_conf))

  old_conf_copy = os.path.join(old_parent, "conf.backup")
  if not os.path.exists(old_conf_copy):
    Execute(as_sudo(["cp", "-R", "-p", old_conf, old_conf_copy]), logoutput=True)

  shutil.rmtree(old_conf, ignore_errors=True)

  # link /etc/[component]/conf -> /usr/hdp/current/[component]-client/conf
  os.symlink(link_conf, old_conf)

if __name__ == "__main__":
  UpgradeSetAll().execute()
