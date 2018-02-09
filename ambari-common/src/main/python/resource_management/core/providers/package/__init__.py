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

import time
import re
import logging

from resource_management.core.exceptions import ExecutionFailed
from resource_management.core.providers import Provider
from resource_management.core.logger import Logger
from resource_management.core import shell
from ambari_commons import shell as ac_shell


PACKAGE_MANAGER_LOCK_ACQUIRED_MSG = "Cannot obtain lock for Package manager. Retrying after {0} seconds. Reason: {1}"
PACKAGE_MANAGER_REPO_ERROR_MSG = "Cannot download the package due to repository unavailability. Retrying after {0} seconds. Reason: {1}"


class PackageProvider(Provider):
  def __init__(self, *args, **kwargs):
    super(PackageProvider, self).__init__(*args, **kwargs)   
  
  def install_package(self, name, use_repos={}, skip_repos=set(), is_upgrade=False):
    raise NotImplementedError()

  def remove_package(self, name, ignore_dependencies=False):
    raise NotImplementedError()

  def upgrade_package(self, name, use_repos={}, skip_repos=set(), is_upgrade=True):
    raise NotImplementedError()

  def action_install(self):
    package_name = self.get_package_name_with_version()
    self.install_package(package_name, self.resource.use_repos, self.resource.skip_repos)

  def action_upgrade(self):
    package_name = self.get_package_name_with_version()
    self.upgrade_package(package_name, self.resource.use_repos, self.resource.skip_repos)

  def action_remove(self):
    package_name = self.get_package_name_with_version()
    self.remove_package(package_name)

  def get_package_name_with_version(self):
    if self.resource.version:
      return self.resource.package_name + '-' + self.resource.version
    else:
      return self.resource.package_name

  def check_uncompleted_transactions(self):
    """
    Check package manager against uncompleted transactions.

    :rtype bool
    """
    return False

  def print_uncompleted_transaction_hint(self):
    """
    Print friendly message about they way to fix the issue

    """
    pass

  def get_available_packages_in_repos(self, repositories):
    """
    Gets all (both installed and available) packages that are available at given repositories.
    :type repositories resource_management.libraries.functions.repository_util.CommandRepository
    :return: installed and available packages from these repositories
    """
    raise NotImplementedError()

  def normalize_select_tool_versions(self, versions):
    """
    Function expect output from get_all_package_versions

    :type versions str|list|set
    :rtype list
    """
    raise NotImplementedError()

  def get_repo_update_cmd(self):
    raise NotImplementedError()

  def get_all_package_versions(self, pkg_name):
    """
    :rtype list[str]
    """
    raise NotImplementedError()

  def installed_pkgs_by_name(self, all_installed_packages, pkg_name):
    return list([i[0] for i in all_installed_packages if i[0].startswith(pkg_name)])

  def all_installed_packages(self, from_unknown_repo=False):
    """
    Return all installed packages in the system except packages in REPO_URL_EXCLUDE

    :arg from_unknown_repo return packages from unknown repos
    :type from_unknown_repo bool

    :return result_type formatted list of packages
    """
    raise NotImplementedError()

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
    raise NotImplementedError()

  def get_installed_repos(self, hint_packages, all_packages, ignore_repos):
    """
    Gets all installed repos by name based on repos that provide any package
    contained in hintPackages
    Repos starting with value in ignoreRepos will not be returned
    hintPackages must be regexps.
    """
    all_repos = []
    repo_list = []

    for hintPackage in hint_packages:
      for item in all_packages:
        if re.match(hintPackage, item[0]) and not item[2] in all_repos:
          all_repos.append(item[2])

    for repo in all_repos:
      ignore = False
      for ignoredRepo in ignore_repos:
        if self.name_match(ignoredRepo, repo):
          ignore = True
      if not ignore:
        repo_list.append(repo)

    return repo_list

  def get_installed_pkgs_by_repo(self, repos, ignore_packages, installed_packages):
    """
    Get all the installed packages from the repos listed in repos
    """
    packages_from_repo = []
    packages_to_remove = []
    for repo in repos:
      sub_result = []
      for item in installed_packages:
        if repo == item[2]:
          sub_result.append(item[0])
      packages_from_repo = list(set(packages_from_repo + sub_result))

    for package in packages_from_repo:
      keep_package = True
      for ignorePackage in ignore_packages:
        if self.name_match(ignorePackage, package):
          keep_package = False
          break
      if keep_package:
        packages_to_remove.append(package)
    return packages_to_remove

  def get_installed_pkgs_by_names(self, pkg_names, all_packages_list=None):
    """
    Gets all installed packages that start with names in pkgNames
    :type pkg_names list[str]
    :type all_packages_list list[str]
    """
    if not all_packages_list:
      all_packages_list = self.all_installed_packages()

    packages = []
    for pkg_name in pkg_names:
      sub_result = self.installed_pkgs_by_name(all_packages_list, pkg_name)
      packages.extend(sub_result)

    return list(set(packages))

  def get_package_details(self, installed_packages, found_packages):
    """
    Gets the name, version, and repoName for the packages
    :type installed_packages list[tuple[str,str,str]]
    :type found_packages list[str]
    """
    package_details = []

    for package in found_packages:
      pkg_detail = {}
      for installed_package in installed_packages:
        if package == installed_package[0]:
          pkg_detail['name'] = installed_package[0]
          pkg_detail['version'] = installed_package[1]
          pkg_detail['repoName'] = installed_package[2]

      package_details.append(pkg_detail)

    return package_details

  def get_repos_to_remove(self, repos, ignore_list):
    repos_to_remove = []
    for repo in repos:
      add_to_remove_list = True
      for ignore_repo in ignore_list:
        if self.name_match(ignore_repo, repo):
          add_to_remove_list = False
          continue
      if add_to_remove_list:
        repos_to_remove.append(repo)
    return repos_to_remove

  def get_installed_package_version(self, package_name):
    raise NotImplementedError()

  def verify_dependencies(self):
    """
    Verify that we have no dependency issues in package manager. Dependency issues could appear because of aborted or terminated
    package installation process or invalid packages state after manual modification of packages list on the host

    :return True if no dependency issues found, False if dependency issue present
    :rtype bool
    """
    raise NotImplementedError()

  def name_match(self, lookup_name, actual_name):
    tokens = actual_name.strip().lower()
    lookup_name = lookup_name.lower()

    return " " not in lookup_name and lookup_name in tokens

  def is_locked_output(self, out):
    return False

  def is_repo_error_output(self, out):
    return False

  def get_logoutput(self):
    return self.resource.logoutput is True and Logger.logger.isEnabledFor(logging.INFO) or self.resource.logoutput is None and Logger.logger.isEnabledFor(logging.DEBUG)

  def call_with_retries(self, cmd, **kwargs):
    return self._call_with_retries(cmd, is_checked=False, **kwargs)
  
  def checked_call_with_retries(self, cmd, **kwargs):
    return self._call_with_retries(cmd, is_checked=True, **kwargs)

  def checked_call(self, cmd, **kwargs):
    return shell.checked_call(cmd, **kwargs)

  def _call_with_retries(self, cmd, is_checked=True, **kwargs):
    func = shell.checked_call if is_checked else shell.call
    code, out = -1, None

    # at least do one retry, to run after repository is cleaned
    try_count = 2 if self.resource.retry_count < 2 else self.resource.retry_count

    for i in range(try_count):
      is_first_time = (i == 0)
      is_last_time = (i == try_count - 1)

      try:
        code, out = func(cmd, **kwargs)
      except ExecutionFailed as ex:
        should_stop_retries = self._handle_retries(cmd, ex.code, ex.out, is_first_time, is_last_time)
        if should_stop_retries:
          raise
      else:
        should_stop_retries = self._handle_retries(cmd, code, out, is_first_time, is_last_time)
        if should_stop_retries:
          break

      time.sleep(self.resource.retry_sleep)

    return code, out

  def _call_with_timeout(self, cmd):
    """
    :type cmd list[str]|str
    :rtype dict
    """
    try:
      return ac_shell.subprocess_with_timeout(cmd)
    except Exception as e:
      Logger.error("Unexpected error:" + str(e))

    return None

  def _executor_error_handler(self, command, error_log, exit_code):
    """
    Error handler for ac_shell.process_executor

    :type command list|str
    :type error_log list
    :type exit_code int
    """
    if isinstance(command, (list, tuple)):
      command = " ".join(command)

    Logger.error("Command execution error: command = \"{0}\", exit code = {1}, stderr = {2}".format(
                 command, exit_code, "\n".join(error_log)))

  def _handle_retries(self, cmd, code, out, is_first_time, is_last_time):
    # handle first failure in a special way (update repo metadata after it, so next try has a better chance to succeed)
    if is_first_time and code and not self.is_locked_output(out):
      self._update_repo_metadata_after_bad_try(cmd, code, out)
      return False

    handled_error_log_message = None
    if self.resource.retry_on_locked and self.is_locked_output(out):
      handled_error_log_message = PACKAGE_MANAGER_LOCK_ACQUIRED_MSG.format(self.resource.retry_sleep, out)
    elif self.resource.retry_on_repo_unavailability and self.is_repo_error_output(out):
      handled_error_log_message = PACKAGE_MANAGER_REPO_ERROR_MSG.format(self.resource.retry_sleep, out)

    is_handled_error = (handled_error_log_message is not None)
    if is_handled_error and not is_last_time:
      Logger.info(handled_error_log_message)

    return (is_last_time or not code or not is_handled_error)

  def _update_repo_metadata_after_bad_try(self, cmd, code, out):
    name = self.get_package_name_with_version()
    repo_update_cmd = self.get_repo_update_cmd()

    Logger.info("Execution of '%s' returned %d. %s" % (shell.string_cmd_from_args_list(cmd), code, out))
    Logger.info("Failed to install package %s. Executing '%s'" % (name, shell.string_cmd_from_args_list(repo_update_cmd)))
    code, out = shell.call(repo_update_cmd, sudo=True, logoutput=self.get_logoutput())

    if code:
      Logger.info("Execution of '%s' returned %d. %s" % (repo_update_cmd, code, out))

    Logger.info("Retrying to install package %s after %d seconds" % (name, self.resource.retry_sleep))


class RPMBasedPackageProvider(PackageProvider):
  """
   RPM Based abstract package provider
  """
  INSTALLED_PACKAGE_VERSION_COMMAND = "rpm -q --queryformat '%{{version}}-%{{release}}' \"{0}\""

  def rpm_check_package_available(self, name):
    import rpm # this is faster then calling 'rpm'-binary externally.
    ts = rpm.TransactionSet()
    packages = ts.dbMatch()

    name_regex = re.escape(name).replace("\\?", ".").replace("\\*", ".*") + '$'
    regex = re.compile(name_regex)

    for package in packages:
      if regex.match(package['name']):
        return True
    return False

  def get_installed_package_version(self, package_name):
    version = None

    result = self.checked_call(self.INSTALLED_PACKAGE_VERSION_COMMAND.format(package_name))
    try:
      if result[0] == 0:
        version = result[1].strip().partition(".el")[0]
    except IndexError:
      pass

    return version
