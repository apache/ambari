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

"""
import signal

import ambari_simplejson as json

from ambari_commons.os_check import OSCheck
from ambari_commons.shell import RepoCallContext
from ambari_commons.str_utils import cbool, cint
from ambari_commons.repo_manager import ManagerFactory
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.libraries.functions.repository_util import CommandRepository
from resource_management.libraries.script.script import Script


class MpackPackages(Script):
  """
  This script is a part of Upgrade workflow
  """

  def __init__(self):
    super(MpackPackages, self).__init__()

    self.repo_mgr = ManagerFactory.get()
    self.repo_files = {}

  def actionexecute(self, env):
    num_errors = 0

    # Parse parameters
    config = Script.get_config()

    try:
      command_repository = CommandRepository(config['repositoryFile'])
    except KeyError:
      raise Fail("The command repository indicated by 'repositoryFile' was not found")

    # Handle a SIGTERM and SIGINT gracefully
    signal.signal(signal.SIGTERM, self.abort_handler)
    signal.signal(signal.SIGINT, self.abort_handler)

    self.repository_version = command_repository.version_string

    # Select dict that contains parameters
    try:
      package_list = json.loads(config['roleParams']['package_list'])
    except KeyError:
      pass


    if self.repository_version is None:
      raise Fail("Cannot determine the repository version to install")

    try:
      if not command_repository.items:
        Logger.warning(
          "Repository list is empty. Ambari may not be managing the repositories for {0}.".format(
            self.repository_version))
      else:
        Logger.info(
          "Will install packages for repository version {0}".format(self.repository_version))
    except Exception, err:
      import traceback
      traceback.print_exc()
      Logger.logger.exception("Cannot install repository files. Error: {0}".format(str(err)))
      num_errors += 1

    # Build structured output with initial values
    self.structured_output = {
      'package_installation_result': 'FAIL',
      'mpack_id': command_repository.mpack_id
    }

    self.put_structured_out(self.structured_output)

    try:
      # check package manager non-completed transactions
      if self.repo_mgr.check_uncompleted_transactions():
        self.repo_mgr.print_uncompleted_transaction_hint()
        num_errors += 1
    except Exception as e:  # we need to ignore any exception
      Logger.warning("Failed to check for uncompleted package manager transactions: " + str(e))

    if num_errors > 0:
      raise Fail("Failed to distribute repositories/install packages")

    # Initial list of versions, used to compute the new version installed

    try:
      ret_code = self.install_packages(package_list)
      if ret_code == 0:
        self.structured_output['package_installation_result'] = 'SUCCESS'
        self.put_structured_out(self.structured_output)
      else:
        num_errors += 1
    except Exception as err:
      num_errors += 1
      Logger.logger.exception("Could not install packages. Error: {0}".format(str(err)))

    # Provide correct exit code
    if num_errors > 0:
      raise Fail("Failed to distribute repositories/install packages")

  def install_packages(self, package_list):
    """
    Actually install the packages using the package manager.
    :param package_list: List of package names to install
    :return: Returns 0 if no errors were found, and 1 otherwise.
    """
    ret_code = 0
    
    config = self.get_config()
    agent_stack_retry_on_unavailability = cbool(config['ambariLevelParams']['agent_stack_retry_on_unavailability'])
    agent_stack_retry_count = cint(config['ambariLevelParams']['agent_stack_retry_count'])

    # Install packages
    packages_were_checked = False
    packages_installed_before = []
#    stack_selector_package = stack_tools.get_stack_tool_package(stack_tools.STACK_SELECTOR_NAME)

    try:
      repositories = config['repositoryFile']['repositories']
      command_repos = CommandRepository(config['repositoryFile'])
      repository_ids = [repository['repoId'] for repository in repositories]
      repos_to_use = {}
      for repo_id in repository_ids:
        if repo_id in self.repo_files:
          repos_to_use[repo_id] = self.repo_files[repo_id]

      packages_installed_before = self.repo_mgr.installed_packages()
      packages_installed_before = [package[0] for package in packages_installed_before]
      packages_were_checked = True
      filtered_package_list = self.filter_package_list(package_list)
      try:
        available_packages_in_repos = self.repo_mgr.get_available_packages_in_repos(command_repos)
      except Exception:
        available_packages_in_repos = []
      for package in filtered_package_list:
        name = self.get_package_from_available(package['name'], available_packages_in_repos)

        # This enables upgrading non-versioned packages, despite the fact they exist.
        # Needed by 'mahout' which is non-version but have to be updated
        self.repo_mgr.upgrade_package(name, RepoCallContext(
          retry_on_repo_unavailability=agent_stack_retry_on_unavailability,
          retry_count=agent_stack_retry_count
        ))
    except Exception as err:
      ret_code = 1
      Logger.logger.exception("Package Manager failed to install packages. Error: {0}".format(str(err)))

      # Remove already installed packages in case of fail
      if packages_were_checked and packages_installed_before:
        packages_installed_after = self.repo_mgr.installed_packages()
        packages_installed_after = [package[0] for package in packages_installed_after]
        packages_installed_before = set(packages_installed_before)
        new_packages_installed = [package for package in packages_installed_after if package not in packages_installed_before]

        if OSCheck.is_ubuntu_family():
          package_version_string = self.repository_version.replace('.', '-')
        else:
          package_version_string = self.repository_version.replace('-', '_')
          package_version_string = package_version_string.replace('.', '_')

        for package in new_packages_installed:
          if package_version_string and (package_version_string in package):
            self.repo_mgr.remove_package(package, RepoCallContext())

    if not self.repo_mgr.verify_dependencies():
      ret_code = 1
      Logger.logger.error("Failure while verifying dependencies")
      Logger.logger.error("*******************************************************************************")
      Logger.logger.error("Manually verify and fix package dependencies and then re-run install_packages")
      Logger.logger.error("*******************************************************************************")

    return ret_code

  def abort_handler(self, signum, frame):
    pass

  def filter_package_list(self, package_list):
    """
    Note: that we have skipUpgrade option in metainfo.xml to filter packages,
    as well as condition option to filter them conditionally,
    so use this method only if, for some reason the metainfo option cannot be used.
  
    :param package_list: original list
    :return: filtered package_list
    """
    filtered_package_list = []
    for package in package_list:
      if self.check_package_condition(package):
        filtered_package_list.append(package)
    return filtered_package_list


if __name__ == "__main__":
  MpackPackages().execute()
