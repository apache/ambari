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

from ambari_commons.os_check import OSCheck
from resource_management.libraries.script import Script
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.version import compare_versions
from resource_management.libraries.functions.version import format_hdp_stack_version
from resource_management.core import shell
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger

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

if __name__ == "__main__":
  UpgradeSetAll().execute()
