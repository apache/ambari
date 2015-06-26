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

from resource_management.core.providers.package import PackageProvider
from resource_management.core.logger import Logger
from ambari_commons.shell import shellRunner

import os

INSTALL_CMD = {
  True: ['cmd', '/c', 'choco', 'install', '--pre', '-y', '-v'],
  False: ['cmd', '/c', 'choco', 'install', '--pre', '-y'],
}

UPGRADE_CMD = {
  True: ['cmd', '/c', 'choco', 'upgrade', '--pre', '-y', '-f', '-v'],
  False: ['cmd', '/c', 'choco', 'upgrade', '--pre', '-y', '-f'],
}

REMOVE_CMD = {
  True: ['cmd', '/c', 'choco', 'uninstall', '-y', '-v'],
  False: ['cmd', '/c', 'choco', 'uninstall', '-y'],
}

CHECK_CMD = {
  True: ['cmd', '/c', 'choco', 'list', '--pre', '--local-only', '-v'],
  False: ['cmd', '/c', 'choco', 'list', '--pre', '--local-only'],
}

class ChocoProvider(PackageProvider):
  def install_package(self, name, use_repos=[], skip_repos=[]):
    if not self._check_existence(name) or use_repos:
      cmd = INSTALL_CMD[self.get_logoutput()]
      if use_repos:
        enable_repo_option = '-s' + ",".join(use_repos)
        cmd = cmd + [enable_repo_option]
      cmd = cmd + [name]
      cmdString = " ".join(cmd)
      Logger.info("Installing package %s ('%s')" % (name, cmdString))
      runner = shellRunner()
      res = runner.run(cmd)
      if res['exitCode'] != 0:
        raise Exception("Error while installing choco package " + name + ". " + res['error'] + res['output'])
    else:
      Logger.info("Skipping installation of existing package %s" % (name))

  def upgrade_package(self, name, use_repos=[], skip_repos=[]):
    cmd = UPGRADE_CMD[self.get_logoutput()]
    if use_repos:
      enable_repo_option = '-s' + ",".join(use_repos)
      cmd = cmd + [enable_repo_option]
    cmd = cmd + [name]
    cmdString = " ".join(cmd)
    Logger.info("Upgrading package %s ('%s')" % (name, cmdString))
    runner = shellRunner()
    res = runner.run(cmd)
    if res['exitCode'] != 0:
      raise Exception("Error while upgrading choco package " + name + ". " + res['error'] + res['output'])

  def remove_package(self, name):
    if self._check_existence(name):
      cmd = REMOVE_CMD[self.get_logoutput()] + [name]
      cmdString = " ".join(cmd)
      Logger.info("Removing package %s ('%s')" % (name, " ".join(cmd)))
      runner = shellRunner()
      res = runner.run(cmd)
      if res['exitCode'] != 0:
        raise Exception("Error while upgrading choco package " + name + ". " + res['error'] + res['output'])
    else:
      Logger.info("Skipping removal of non-existing package %s" % (name))

  def _check_existence(self, name):
    cmd = CHECK_CMD[self.get_logoutput()] + [name]
    runner = shellRunner()
    res = runner.run(cmd)
    if name in res['output']:
      return True
    return False