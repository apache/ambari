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
import ConfigParser
import glob

from ambari_commons.constants import AMBARI_SUDO_BINARY
from resource_management.core.providers.package import RPMBasedPackageProvider
from resource_management.core import shell
from resource_management.core.shell import string_cmd_from_args_list
from resource_management.core.logger import Logger
from resource_management.core.utils import suppress_stdout
from resource_management.core import sudo

from StringIO import StringIO

import re
import os

INSTALL_CMD = {
  True: ['/usr/bin/yum', '-y', 'install'],
  False: ['/usr/bin/yum', '-d', '0', '-e', '0', '-y', 'install'],
}

REMOVE_CMD = {
  True: ['/usr/bin/yum', '-y', 'erase'],
  False: ['/usr/bin/yum', '-d', '0', '-e', '0', '-y', 'erase'],
}

REMOVE_WITHOUT_DEPENDENCIES_CMD = ['rpm', '-e', '--nodeps']

YUM_LIB_DIR = "/var/lib/yum"
YUM_TR_PREFIX = "transaction-"

YUM_REPO_LOCATION = "/etc/yum.repos.d"
REPO_UPDATE_CMD = ['/usr/bin/yum', 'clean', 'metadata']
ALL_INSTALLED_PACKAGES_CMD = [AMBARI_SUDO_BINARY, "yum", "list", "installed"]
ALL_AVAILABLE_PACKAGES_CMD = [AMBARI_SUDO_BINARY, "yum", "list", "available"]
VERIFY_DEPENDENCY_CMD = ['/usr/bin/yum', '-d', '0', '-e', '0', 'check', 'dependencies']

# base command output sample:
# -----------------------------
# select.noarch                       2.5.6.0-40.el6            REPO-2.5
# select.noarch                       2.6.3.0-56                REPO-2.6.3.0-56
# select.noarch                       2.6.3.0-57                REPO-2.6.3.0-57
# select.noarch                       2.6.3.0-63                REPO-2.6.3.0
# select.noarch                       2.6.3.0-63                REPO-2.6.3.0-63

LIST_ALL_SELECT_TOOL_PACKAGES_CMD = "yum list all --showduplicates|grep -v '@' |grep '^{pkg_name}'|awk '{print $2}'"
SELECT_TOOL_VERSION_PATTERN = re.compile("(\d{1,2}\.\d{1,2}\.\d{1,2}\.\d{1,2}-*\d*).*")  # xx.xx.xx.xx(-xxxx)


class YumProvider(RPMBasedPackageProvider):

  def get_available_packages_in_repos(self, repos):
    """
    Gets all (both installed and available) packages that are available at given repositories.

    :type repos resource_management.libraries.functions.repository_util.CommandRepository
    :return: installed and available packages from these repositories
    """
    available_packages = []
    installed_packages = []
    repo_ids = [repo.repo_id for repo in repos.items]

    if repos.feat.scoped:
      Logger.info("Looking for matching packages in the following repositories: {0}".format(", ".join(repo_ids)))
    else:
      Logger.info("Packages will be queried using all available repositories on the system.")

    for repo in repo_ids:
      repo = repo if repos.feat.scoped else None
      available_packages.extend(self._get_available_packages(repo))
      installed_packages.extend(self._get_installed_packages(repo))

    # fallback logic

    if repos.feat.scoped:
      fallback_repo_ids = set(repo_ids) ^ self._build_repos_ids(repos)  # no reason to scan the same repos again
      if fallback_repo_ids:
        Logger.info("Adding fallback repositories: {0}".format(", ".join(fallback_repo_ids)))

        for repo in fallback_repo_ids:
          available_packages.extend(self._get_available_packages(repo))
          installed_packages.extend(self._get_installed_packages(repo))

    return [package[0] for package in available_packages + installed_packages]

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

  def normalize_select_tool_versions(self, versions):
    """
    Function expect output from get_all_package_versions

    :type versions str|list|set
    :rtype list
    """
    if isinstance(versions, str):
      versions = [versions]

    return [self.__parse_select_tool_version(i) for i in versions]

  def _get_available_packages(self, repo_filter=None):
    """
    Returning list of available packages with possibility to filter them by name
    :param repo_filter: repository name

    :type repo_filter str|None
    :rtype list[list,]
    """

    cmd = list(ALL_AVAILABLE_PACKAGES_CMD)

    if repo_filter:
      cmd.extend(["--disablerepo=*", "--enablerepo=" + repo_filter])

    return self._lookup_packages(cmd, 'Available Packages')

  def _get_installed_packages(self, repo_filter=None):
    """
    Returning list of the installed packages with possibility to filter them by name
    :param repo_filter: repository name

    :type repo_filter str|None
    :rtype list[list,]
    """

    packages = self._lookup_packages(list(ALL_INSTALLED_PACKAGES_CMD), "Installed Packages")
    if repo_filter:
      packages = [item for item in packages if item[2].lower() == repo_filter.lower()]

    return packages

  def _lookup_packages(self, command, skip_till):
    """
    :type command list[str]
    :type skip_till str|None
    """
    packages = []

    result = self._call_with_timeout(command)

    if result and 0 == result['retCode']:
      lines = result['out'].split('\n')
      lines = [line.strip() for line in lines]
      items = []
      if skip_till:
        skip_index = 3
        for index in range(len(lines)):
          if skip_till in lines[index]:
            skip_index = index + 1
            break
      else:
        skip_index = 0

      for line in lines[skip_index:]:
        items = items + line.strip(' \t\n\r').split()

      items_count = len(items)

      for i in range(0, items_count, 3):

        # check if we reach the end
        if i+3 > items_count:
          break

        if '.' in items[i]:
          items[i] = items[i][:items[i].rindex('.')]
        if items[i + 2].find('@') == 0:
          items[i + 2] = items[i + 2][1:]
        packages.append(items[i:i + 3])

    return packages

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
    return self._get_available_packages(None)

  def all_installed_packages(self, from_unknown_repo=False):
    """
    Return all installed packages in the system except packages in REPO_URL_EXCLUDE

    :arg from_unknown_repo return packages from unknown repos
    :type from_unknown_repo bool

    :return result_type formatted list of packages
    """
    #  ToDo: move to iterative package lookup (check apt provider for details)
    return self._get_installed_packages(None)

  def verify_dependencies(self):
    """
    Verify that we have no dependency issues in package manager. Dependency issues could appear because of aborted or terminated
    package installation process or invalid packages state after manual modification of packages list on the host

    :return True if no dependency issues found, False if dependency issue present
    :rtype bool
    """
    code, out = self.checked_call(VERIFY_DEPENDENCY_CMD, sudo=True)
    pattern = re.compile("has missing requires|Error:")

    if code or (out and pattern.search(out)):
      err_msg = Logger.filter_text("Failed to verify package dependencies. Execution of '%s' returned %s. %s" % (VERIFY_DEPENDENCY_CMD, code, out))
      Logger.error(err_msg)
      return False

    return True

  def install_package(self, name, use_repos={}, skip_repos=set(), is_upgrade=False):
    if is_upgrade or use_repos or not self._check_existence(name):
      cmd = INSTALL_CMD[self.get_logoutput()]
      if use_repos:
        enable_repo_option = '--enablerepo=' + ",".join(sorted(use_repos.keys()))
        disable_repo_option = '--disablerepo=' + "*" if len(skip_repos) == 0 else ','.join(skip_repos)
        cmd = cmd + [disable_repo_option, enable_repo_option]
      cmd = cmd + [name]
      Logger.info("Installing package %s ('%s')" % (name, string_cmd_from_args_list(cmd)))
      self.checked_call_with_retries(cmd, sudo=True, logoutput=self.get_logoutput())
    else:
      Logger.info("Skipping installation of existing package %s" % (name))

  def upgrade_package(self, name, use_repos={}, skip_repos=set(), is_upgrade=True):
    return self.install_package(name, use_repos, skip_repos, is_upgrade)

  def remove_package(self, name, ignore_dependencies=False):
    if self._check_existence(name):
      if ignore_dependencies:
        cmd = REMOVE_WITHOUT_DEPENDENCIES_CMD + [name]
      else:
        cmd = REMOVE_CMD[self.get_logoutput()] + [name]
      Logger.info("Removing package %s ('%s')" % (name, string_cmd_from_args_list(cmd)))
      shell.checked_call(cmd, sudo=True, logoutput=self.get_logoutput())
    else:
      Logger.info("Skipping removal of non-existing package %s" % (name))

  def _check_existence(self, name):
    """
    For regexp names:
    If only part of packages were installed during early canceling.
    Let's say:
    1. install hbase_2_3_*
    2. Only hbase_2_3_1234 is installed, but is not hbase_2_3_1234_regionserver yet.
    3. We cancel the yum

    In that case this is bug of packages we require.
    And hbase_2_3_*_regionserver should be added to metainfo.xml.

    Checking existence should never fail in such a case for hbase_2_3_*, otherwise it
    gonna break things like removing packages and some others.

    Note: this method SHOULD NOT use yum directly (yum.rpmdb doesn't use it). Because a lot of issues we have, when customer have
    yum in inconsistant state (locked, used, having invalid repo). Once packages are installed
    we should not rely on that.
    """
    if os.geteuid() == 0:
      return self.yum_check_package_available(name)
    else:
      return self.rpm_check_package_available(name)

  def yum_check_package_available(self, name):
    """
    Does the same as rpm_check_package_avaiable, but faster.
    However need root permissions.
    """
    import yum  # Python Yum API is much faster then other check methods. (even then "import rpm")
    yb = yum.YumBase()
    name_regex = re.escape(name).replace("\\?", ".").replace("\\*", ".*") + '$'
    regex = re.compile(name_regex)

    with suppress_stdout():
      package_list = yb.rpmdb.simplePkgList()

    for package in package_list:
      if regex.match(package[0]):
        return True

    return False

  def is_repo_error_output(self, out):
    return "Failure when receiving data from the peer" in out or \
           "Nothing to do" in out

  def get_repo_update_cmd(self):
    return REPO_UPDATE_CMD


  @staticmethod
  def _build_repos_ids(repos):
    """
    Gets a set of repository identifiers based on the supplied repository JSON structure as
    well as any matching repos defined in /etc/yum.repos.d.
    :type repos resource_management.libraries.functions.repository_util.CommandRepository
    :return:  the list of repo IDs from both the command and any matches found on the system
    with the same URLs.
    """

    repo_ids = []
    base_urls = []
    mirrors = []

    for repo in repos.items:
      repo_ids.append(repo.repo_id)

      if repo.base_url:
        base_urls.append(repo.base_url)

      if repo.mirrors_list:
        mirrors.append(repo.mirrors_list)

    # for every repo file, find any which match the base URLs we're trying to write out
    # if there are any matches, it means the repo already exists and we should use it to search
    # for packages to install
    for repo_file in glob.glob(os.path.join(YUM_REPO_LOCATION, "*.repo")):
      config_parser = ConfigParser.ConfigParser()
      config_parser.read(repo_file)
      sections = config_parser.sections()
      for section in sections:
        if config_parser.has_option(section, "baseurl"):
          base_url = config_parser.get(section, "baseurl")
          if base_url in base_urls:
            repo_ids.append(section)

        if config_parser.has_option(section, "mirrorlist"):
          mirror = config_parser.get(section, "mirrorlist")
          if mirror in mirrors:
            repo_ids.append(section)

    return set(repo_ids)

  def __extract_transaction_id(self, filename):
    """
    :type filename str
    """
    return filename.split(".", 1)[1]

  def __transaction_file_parser(self, f):
    """
    :type f file|BinaryIO|StringIO
    :rtype collections.Iterable(str)
    """
    for line in f:
      yield line.split(":", 1)[1].strip()

  def uncomplete_transactions(self):
    """
    Transactions reader

    :rtype collections.Iterable(YumTransactionItem)
    """
    transactions = {}

    prefix_len = len(YUM_TR_PREFIX)
    for item in sudo.listdir(YUM_LIB_DIR):
      if YUM_TR_PREFIX == item[:prefix_len]:
        tr_id = self.__extract_transaction_id(item)

        f = StringIO(sudo.read_file(os.path.join(YUM_LIB_DIR, item)))
        pkgs_in_transaction = list(self.__transaction_file_parser(f))

        if tr_id not in transactions:
          transactions[tr_id] = YumTransactionItem(tr_id)

        if RPMTransactions.all in item:
          transactions[tr_id].pkgs_all = pkgs_in_transaction
        elif RPMTransactions.done in item:
          transactions[tr_id].pkgs_done = pkgs_in_transaction

    for tr in transactions.values():
      if len(tr.pkgs_all) == 0:
        continue

      if isinstance(tr, YumTransactionItem):
        yield tr

  def check_uncompleted_transactions(self):
    """
    Check package manager against uncompleted transactions.

    :rtype bool
    """

    transactions = list(self.uncomplete_transactions())

    if len(transactions) > 0:
      Logger.info("Yum non-completed transactions check failed, found {0} non-completed transaction(s):".format(len(transactions)))
      for tr in transactions:
        Logger.info("[{0}] Packages broken: {1}; Packages not-installed {2}".format(
          tr.transaction_id,
          ", ".join(tr.pkgs_done),
          ", ".join(tr.pkgs_aborted)
        ))

      return True

    Logger.info("Yum non-completed transactions check passed")
    return False

  def print_uncompleted_transaction_hint(self):
    """
    Print friendly message about they way to fix the issue

    """
    help_msg = """*** Incomplete Yum Transactions ***
    
Ambari has detected that there are incomplete Yum transactions on this host. This will interfere with the installation process and must be resolved before continuing.

- Identify the pending transactions with the command 'yum history list <packages failed>'
- Revert each pending transaction with the command 'yum history undo'
- Flush the transaction log with 'yum-complete-transaction --cleanup-only'
"""

    for line in help_msg.split("\n"):
      Logger.error(line)


class YumTransactionItem(object):
  def __init__(self, transaction_id, pkgs_done=None, pkgs_all=None):
    self.transaction_id = transaction_id
    self.pkgs_done = pkgs_done if pkgs_done else []
    self.pkgs_all = pkgs_all if pkgs_all else []

  @property
  def pkgs_aborted(self):
    return set(self.pkgs_all) ^ set(self.pkgs_done)


class RPMTransactions(object):
  all = "all"
  done = "done"
  aborted = "aborted"  # custom one
