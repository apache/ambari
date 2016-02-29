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
import tempfile
import shutil
import re

from resource_management.core.providers.package import PackageProvider
from resource_management.core import shell
from resource_management.core import sudo
from resource_management.core.shell import string_cmd_from_args_list
from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail

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

APT_SOURCES_LIST_DIR = "/etc/apt/sources.list.d"

CHECK_CMD = "dpkg --get-selections | grep -v deinstall | awk '{print $1}' | grep ^%s$"

def replace_underscores(function_to_decorate):
  def wrapper(*args):
    self = args[0]
    name = args[1].replace("_", "-")
    return function_to_decorate(self, name, *args[2:])
  return wrapper


class AptProvider(PackageProvider):

  @replace_underscores
  def install_package(self, name, use_repos=[], skip_repos=[]):
    if use_repos or not self._check_existence(name):
      cmd = INSTALL_CMD[self.get_logoutput()]
      copied_sources_files = []
      is_tmp_dir_created = False
      if use_repos:
        is_tmp_dir_created = True
        apt_sources_list_tmp_dir = tempfile.mkdtemp(suffix="-ambari-apt-sources-d")
        Logger.info("Temporal sources directory was created: %s" % apt_sources_list_tmp_dir)
        if 'base' not in use_repos:
          cmd = cmd + ['-o', 'Dir::Etc::SourceList=%s' % EMPTY_FILE]
        for repo in use_repos:
          if repo != 'base':
            new_sources_file = os.path.join(apt_sources_list_tmp_dir, repo + '.list')
            Logger.info("Temporal sources file will be copied: %s" % new_sources_file)
            sudo.copy(os.path.join(APT_SOURCES_LIST_DIR, repo + '.list'), new_sources_file)
            copied_sources_files.append(new_sources_file)
        cmd = cmd + ['-o', 'Dir::Etc::SourceParts=%s' % apt_sources_list_tmp_dir]

      cmd = cmd + [name]
      Logger.info("Installing package %s ('%s')" % (name, string_cmd_from_args_list(cmd)))
      code, out = self.call_with_retries(cmd, sudo=True, env=INSTALL_CMD_ENV, logoutput=self.get_logoutput())
      
      if self.is_locked_output(out):
        err_msg = Logger.filter_text("Execution of '%s' returned %d. %s" % (cmd, code, out))
        raise Fail(err_msg)
      
      # apt-get update wasn't done too long maybe?
      if code:
        Logger.info("Execution of '%s' returned %d. %s" % (cmd, code, out))
        Logger.info("Failed to install package %s. Executing `%s`" % (name, string_cmd_from_args_list(REPO_UPDATE_CMD)))
        code, out = self.call_with_retries(REPO_UPDATE_CMD, sudo=True, logoutput=self.get_logoutput())
        
        if code:
          Logger.info("Execution of '%s' returned %d. %s" % (REPO_UPDATE_CMD, code, out))
          
        Logger.info("Retrying to install package %s" % (name))
        self.checked_call_with_retries(cmd, sudo=True, env=INSTALL_CMD_ENV, logoutput=self.get_logoutput())

      if is_tmp_dir_created:
        for temporal_sources_file in copied_sources_files:
          Logger.info("Removing temporal sources file: %s" % temporal_sources_file)
          os.remove(temporal_sources_file)
        Logger.info("Removing temporal sources directory: %s" % apt_sources_list_tmp_dir)
        os.rmdir(apt_sources_list_tmp_dir)
    else:
      Logger.info("Skipping installation of existing package %s" % (name))
      
  def is_locked_output(self, out):
    return "Unable to lock the administration directory" in out

  def is_repo_error_output(self, out):
    return "Failure when receiving data from the peer" in out

  @replace_underscores
  def upgrade_package(self, name, use_repos=[], skip_repos=[]):
    return self.install_package(name, use_repos, skip_repos)

  @replace_underscores
  def remove_package(self, name):
    if self._check_existence(name):
      cmd = REMOVE_CMD[self.get_logoutput()] + [name]
      Logger.info("Removing package %s ('%s')" % (name, string_cmd_from_args_list(cmd)))
      self.checked_call_with_retries(cmd, sudo=True, logoutput=self.get_logoutput())
    else:
      Logger.info("Skipping removal of non-existing package %s" % (name))

  @replace_underscores
  def _check_existence(self, name): 
    """
    For regexp names:
    If only part of packages were installed during early canceling.
    Let's say:
    1. install hbase-2-3-.*
    2. Only hbase-2-3-1234 is installed, but is not hbase-2-3-1234-regionserver yet.
    3. We cancel the apt-get
    
    In that case this is bug of packages we require.
    And hbase-2-3-*-regionserver should be added to metainfo.xml.
    
    Checking existence should never fail in such a case for hbase-2-3-.*, otherwise it
    gonna break things like removing packages and some other things.
    
    Note: this method SHOULD NOT use apt-get (apt.cache is using dpkg not apt). Because a lot of issues we have, when customer have
    apt-get in inconsistant state (locked, used, having invalid repo). Once packages are installed
    we should not rely on that.
    """
    code, out = shell.call(CHECK_CMD % name)
    return not bool(code)