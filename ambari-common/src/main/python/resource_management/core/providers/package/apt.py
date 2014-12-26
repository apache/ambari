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
from resource_management.core import shell
from resource_management.core.shell import string_cmd_from_args_list
from resource_management.core.logger import Logger

INSTALL_CMD_ENV = {'DEBIAN_FRONTEND':'noninteractive'}
INSTALL_CMD = {
  True: ['/usr/bin/apt-get', '-o', "Dpkg::Options::=--force-confdef", '--allow-unauthenticated', '--assume-yes', 'install'],
  False: ['/usr/bin/apt-get', '-q', '-o', "Dpkg::Options::=--force-confdef", '--allow-unauthenticated', '--assume-yes', 'install'],
}
REMOVE_CMD = {
  True: ['/usr/bin/apt-get', '-y', 'remove'],
  False: ['/usr/bin/apt-get', '-y', '-q', 'remove'],
}
REPO_UPDATE_CMD = ['/usr/bin/apt-get', 'update','-qq']

CHECK_CMD = "dpkg --get-selections | grep -v deinstall | awk '{print $1}' | grep ^%s$"

def replace_underscores(function_to_decorate):
  def wrapper(*args):
    self = args[0]
    name = args[1].replace("_", "-")
    return function_to_decorate(self, name)
  return wrapper

class AptProvider(PackageProvider):

  @replace_underscores
  def install_package(self, name, use_repos=[]):
    if not self._check_existence(name):
      cmd = INSTALL_CMD[self.get_logoutput()]  + [name]
      Logger.info("Installing package %s ('%s')" % (name, string_cmd_from_args_list(cmd)))
      code, out = shell.call(cmd, sudo=True, env=INSTALL_CMD_ENV, logoutput=self.get_logoutput())
      
      # apt-get update wasn't done too long
      if code:
        Logger.info("Execution of '%s' returned %d. %s" % (cmd, code, out))
        Logger.info("Failed to install package %s. Executing `%s`" % (name, string_cmd_from_args_list(REPO_UPDATE_CMD)))
        code, out = shell.call(REPO_UPDATE_CMD, sudo=True, logoutput=self.get_logoutput())
        
        if code:
          Logger.info("Execution of '%s' returned %d. %s" % (REPO_UPDATE_CMD, code, out))
          
        Logger.info("Retrying to install package %s" % (name))
        shell.checked_call(cmd, sudo=True, logoutput=self.get_logoutput())
    else:
      Logger.info("Skipping installing existent package %s" % (name))

  @replace_underscores
  def upgrade_package(self, name, use_repos=[]):
    return self.install_package(name, use_repos)

  @replace_underscores
  def remove_package(self, name):
    if self._check_existence(name):
      cmd = REMOVE_CMD[self.get_logoutput()] + [name]
      Logger.info("Removing package %s ('%s')" % (name, string_cmd_from_args_list(cmd)))
      shell.checked_call(cmd, sudo=True, logoutput=self.get_logoutput())
    else:
      Logger.info("Skipping removing non-existent package %s" % (name))

  @replace_underscores
  def _check_existence(self, name):
    code, out = shell.call(CHECK_CMD % name)
    return not bool(code)
