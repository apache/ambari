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
from ambari_commons.constants import AMBARI_SUDO_BINARY
from resource_management.core.providers.package import RPMBasedPackageProvider
from resource_management.core.shell import string_cmd_from_args_list
from resource_management.core.logger import Logger

import re
import os

INSTALL_CMD = {
  True: ['/usr/bin/zypper', 'install', '--auto-agree-with-licenses', '--no-confirm'],
  False: ['/usr/bin/zypper', '--quiet', 'install', '--auto-agree-with-licenses', '--no-confirm'],
}
REMOVE_CMD = {
  True: ['/usr/bin/zypper', 'remove', '--no-confirm'],
  False: ['/usr/bin/zypper', '--quiet', 'remove', '--no-confirm'],
}

REPO_UPDATE_CMD = ['/usr/bin/zypper', 'clean']

LIST_ACTIVE_REPOS_CMD = ['/usr/bin/zypper', 'repos']
ALL_INSTALLED_PACKAGES_CMD = [AMBARI_SUDO_BINARY, "zypper", "--no-gpg-checks", "search", "--installed-only", "--details"]
ALL_AVAILABLE_PACKAGES_CMD = [AMBARI_SUDO_BINARY, "zypper", "--no-gpg-checks", "search", "--uninstalled-only", "--details"]
VERIFY_DEPENDENCY_CMD = ['/usr/bin/zypper', '--quiet', '--non-interactive', 'verify', '--dry-run']

# base command output sample:
# -----------------------------
#
# S | Name   | Type    | Version     | Arch   | Repository
# --+--------+---------+-------------+--------+---------------
# i | select | package | 2.6.3.0-60  | noarch | REPO-2.6.3.0-60
# v | select | package | 2.6.3.0-57  | noarch | REPO-2.6.3.0-57
# v | select | package | 2.6.1.0-129 | noarch | REPO-2.6.1.0
# v | select | package | 2.5.6.0-40  | noarch | REPO-2.5

LIST_ALL_SELECT_TOOL_PACKAGES_CMD = "zypper -q search -s {pkg_name}|grep '|' | grep -v 'Repository'| cut -d '|' -f 4"
SELECT_TOOL_VERSION_PATTERN = re.compile("(\d{1,2}\.\d{1,2}\.\d{1,2}\.\d{1,2}-*\d*).*")  # xx.xx.xx.xx(-xxxx)


class ZypperProvider(RPMBasedPackageProvider):

  def get_available_packages_in_repos(self, repos):
    """
    Gets all (both installed and available) packages that are available at given repositories.
    :type repos resource_management.libraries.functions.repository_util.CommandRepository
    :return: installed and available packages from these repositories
    """

    available_packages = []
    repo_ids = [repository.repo_id for repository in repos.items]

    # zypper cant tell from which repository were installed package, as repo r matching by pkg_name
    # as result repository would be matched if it contains package with same meta info
    if repos.feat.scoped:
      Logger.info("Looking for matching packages in the following repositories: {0}".format(", ".join(repo_ids)))
    else:
      Logger.info("Packages will be queried using all available repositories on the system.")

    for repo in repo_ids:
      repo = repo if repos.feat.scoped else None
      available_packages.extend(self._get_available_packages(repo))

    return [package[0] for package in available_packages]

  def get_all_package_versions(self, pkg_name):
    """
    :type pkg_name str
    """
    command = LIST_ALL_SELECT_TOOL_PACKAGES_CMD.replace("{pkg_name}", pkg_name)
    result = self._call_with_timeout(command)

    if result["retCode"] == 0:
      return result["out"].split(os.linesep)

    return None

  def __parse_select_tool_version(self, v):
    """
    :type v str
    """
    matches = SELECT_TOOL_VERSION_PATTERN.findall(v.strip())
    return matches[0] if matches else None

  def _get_available_packages(self, repo_filter=None):
    """
    Returning list of available packages with possibility to filter them by name
    :param repo_filter: repository name

    :type repo_filter str|None
    :rtype list[list,]
    """

    cmd = [AMBARI_SUDO_BINARY, "zypper", "--no-gpg-checks", "search", "--details"]

    if repo_filter:
      cmd.extend(["--repo=" + repo_filter])

    return self._lookup_packages(cmd)

  def normalize_select_tool_versions(self, versions):
    """
    Function expect output from get_all_package_versions

    :type versions str|list|set
    :rtype list
    """
    if isinstance(versions, str):
      versions = [versions]

    return [self.__parse_select_tool_version(i) for i in versions]

  def _lookup_packages(self, command):
    """
    :type command list[str]
    """
    packages = []
    skip_index = None

    result = self._call_with_timeout(command)

    if result and 0 == result['retCode']:
      lines = result['out'].strip().split('\n')
      lines = [line.strip() for line in lines]
      for index in range(len(lines)):
        if "--+--" in lines[index]:
          skip_index = index + 1
          break

      if skip_index:
        for line in lines[skip_index:]:
          items = line.strip(' \t\n\r').split('|')
          packages.append([items[1].strip(), items[3].strip(), items[5].strip()])

    return packages

  def all_installed_packages(self, from_unknown_repo=False):
    """
    Return all installed packages in the system except packages in REPO_URL_EXCLUDE

    :arg from_unknown_repo return packages from unknown repos
    :type from_unknown_repo bool

    :return result_type formatted list of packages
    """
    #  ToDo: move to iterative package lookup (check apt provider for details)
    return self._lookup_packages(ALL_INSTALLED_PACKAGES_CMD)

  def all_available_packages(self, result_type=list, group_by_index=-1):
    """
    Return all available packages in the system except packages in REPO_URL_EXCLUDE

    :arg result_type Could be list or dict, defines type of returning value
    :arg group_by_index index of element in the __packages_reader result, which would be used as key
    :return result_type formatted list of packages, including installed and available in repos

    :type result_type type
    :type group_by_index int
    :rtype list|dict
    """
    #  ToDo: move to iterative package lookup (check apt provider for details)
    return self._lookup_packages(ALL_AVAILABLE_PACKAGES_CMD)

  def verify_dependencies(self):
    """
    Verify that we have no dependency issues in package manager. Dependency issues could appear because of aborted or terminated
    package installation process or invalid packages state after manual modification of packages list on the host

    :return True if no dependency issues found, False if dependency issue present
    :rtype bool
    """
    code, out = self.checked_call(VERIFY_DEPENDENCY_CMD, sudo=True)
    pattern = re.compile("\d+ new package(s)? to install")

    if code or (out and pattern.search(out)):
      err_msg = Logger.filter_text("Failed to verify package dependencies. Execution of '%s' returned %s. %s" % (VERIFY_DEPENDENCY_CMD, code, out))
      Logger.error(err_msg)
      return False

    return True

  def install_package(self, name, use_repos={}, skip_repos=[], is_upgrade=False):
    if is_upgrade or use_repos or not self._check_existence(name):
      cmd = INSTALL_CMD[self.get_logoutput()]
      use_repos = use_repos.keys()
      if use_repos:
        active_base_repos = self.get_active_base_repos()
        if 'base' in use_repos:
          # Remove 'base' from use_repos list
          use_repos = filter(lambda x: x != 'base', use_repos)
          use_repos.extend(active_base_repos)
        use_repos_options = []
        for repo in sorted(use_repos):
          use_repos_options = use_repos_options + ['--repo', repo]
        cmd = cmd + use_repos_options

      cmd = cmd + [name]
      Logger.info("Installing package %s ('%s')" % (name, string_cmd_from_args_list(cmd)))
      self.checked_call_with_retries(cmd, sudo=True, logoutput=self.get_logoutput())
    else:
      Logger.info("Skipping installation of existing package %s" % (name))

  def upgrade_package(self, name, use_repos={}, skip_repos=[], is_upgrade=True):
    return self.install_package(name, use_repos, skip_repos, is_upgrade)
  
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

  def get_repo_update_cmd(self):
    return REPO_UPDATE_CMD

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
