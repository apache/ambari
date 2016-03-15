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

from resource_management.core.providers.package import PackageProvider
from resource_management.core import shell
from resource_management.core.shell import string_cmd_from_args_list
from resource_management.core.logger import Logger
from resource_management.core.utils import suppress_stdout

INSTALL_CMD = {
  True: ['/usr/bin/zypper', 'install', '--auto-agree-with-licenses', '--no-confirm'],
  False: ['/usr/bin/zypper', '--quiet', 'install', '--auto-agree-with-licenses', '--no-confirm'],
}
REMOVE_CMD = {
  True: ['/usr/bin/zypper', 'remove', '--no-confirm'],
  False: ['/usr/bin/zypper', '--quiet', 'remove', '--no-confirm'],
}

LIST_ACTIVE_REPOS_CMD = ['/usr/bin/zypper', 'repos']

class ZypperProvider(PackageProvider):
  def install_package(self, name, use_repos=[], skip_repos=[]):
    if use_repos or not self._check_existence(name):
      cmd = INSTALL_CMD[self.get_logoutput()]
      if use_repos:
        active_base_repos = self.get_active_base_repos()
        if 'base' in use_repos:
          # Remove 'base' from use_repos list
          use_repos = filter(lambda x: x != 'base', use_repos)
          use_repos.extend(active_base_repos)
        use_repos_options = []
        for repo in use_repos:
          use_repos_options = use_repos_options + ['--from', repo]
        cmd = cmd + use_repos_options

      cmd = cmd + [name]
      Logger.info("Installing package %s ('%s')" % (name, string_cmd_from_args_list(cmd)))
      self.checked_call_with_retries(cmd, sudo=True, logoutput=self.get_logoutput())
    else:
      Logger.info("Skipping installation of existing package %s" % (name))

  def upgrade_package(self, name, use_repos=[], skip_repos=[]):
    return self.install_package(name, use_repos, skip_repos)
  
  def remove_package(self, name):
    if self._check_existence(name):
      cmd = REMOVE_CMD[self.get_logoutput()] + [name]
      Logger.info("Removing package %s ('%s')" % (name, string_cmd_from_args_list(cmd)))
      self.checked_call_with_retries(cmd, sudo=True, logoutput=self.get_logoutput())
    else:
      Logger.info("Skipping removal of non-existing package %s" % (name))
      
  def get_active_base_repos(self):
    (code, output) = self.call_with_retries(LIST_ACTIVE_REPOS_CMD)
    enabled_repos = []
    if not code:
      for line in output.split('\n')[2:]:
        line_list = line.split('|')
        if len(line_list) < 5:
          continue  # Skip malformed line, such as "---+--------+---------+----------+--------"
                    # Handle good line such as "1 | HDP-2.3 | HDP-2.3 | No | No"
        if line_list[3].strip() == 'Yes' and line_list[2].strip().startswith("SUSE-"):
          enabled_repos.append(line_list[1].strip())
        if line_list[2].strip() == 'OpenSuse':
          return [line_list[1].strip()]
    return enabled_repos
      
  def is_locked_output(self, out):
    return "System management is locked by the application" in out

  def is_repo_error_output(self, out):
    return "Failure when receiving data from the peer" in out

  def _check_existence(self, name):
    """
    For regexp names:
    If only part of packages were installed during early canceling.
    Let's say:
    1. install hbase_2_3_*
    2. Only hbase_2_3_1234 is installed, but is not hbase_2_3_1234_regionserver yet.
    3. We cancel the zypper
    
    In that case this is bug of packages we require.
    And hbase_2_3_*_regionserver should be added to metainfo.xml.
    
    Checking existence should never fail in such a case for hbase_2_3_*, otherwise it
    gonna break things like removing packages and some other things.
    
    Note: this method SHOULD NOT use zypper. Because a lot of issues we have, when customer have
    zypper in inconsistant state (locked, used, having invalid repo). Once packages are installed
    we should not rely on that.
    """
    return self.rpm_check_package_available(name)
