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
import signal

import sys
import re
import os.path

import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.

from resource_management import *
from resource_management.libraries.functions.list_ambari_managed_repos import list_ambari_managed_repos
from ambari_commons.os_check import OSCheck, OSConst
from resource_management.libraries.functions.packages_analyzer import allInstalledPackages
from resource_management.libraries.functions import conf_select
from resource_management.core.shell import call

from resource_management.core.logger import Logger


class InstallPackages(Script):
  """
  This script is a part of Rolling Upgrade workflow and is described at
  appropriate design doc.
  It installs repositories to the node and then installs packages.
  For now, repositories are installed into individual files.
  """

  UBUNTU_REPO_COMPONENTS_POSTFIX = ["main"]
  REPO_FILE_NAME_PREFIX = 'HDP-'
  STACK_TO_ROOT_FOLDER = {"HDP": "/usr/hdp"}
  
  # Mapping file used to store repository versions without a build number, and the actual version it corresponded to.
  # E.g., HDP 2.2.0.0 => HDP 2.2.0.0-2041
  REPO_VERSION_HISTORY_FILE = "/var/lib/ambari-agent/data/repo_version_history.json"

  def actionexecute(self, env):
    num_errors = 0

    # Parse parameters
    config = Script.get_config()

    repo_rhel_suse = config['configurations']['cluster-env']['repo_suse_rhel_template']
    repo_ubuntu = config['configurations']['cluster-env']['repo_ubuntu_template']
    template = repo_rhel_suse if OSCheck.is_redhat_family() or OSCheck.is_suse_family() else repo_ubuntu

    # Handle a SIGTERM and SIGINT gracefully
    signal.signal(signal.SIGTERM, self.abort_handler)
    signal.signal(signal.SIGINT, self.abort_handler)

    # Select dict that contains parameters
    try:
      self.repository_version = config['roleParams']['repository_version']
      base_urls = json.loads(config['roleParams']['base_urls'])
      package_list = json.loads(config['roleParams']['package_list'])
      stack_id = config['roleParams']['stack_id']
    except KeyError:
      # Last try
      self.repository_version = config['commandParams']['repository_version']
      base_urls = json.loads(config['commandParams']['base_urls'])
      package_list = json.loads(config['commandParams']['package_list'])
      stack_id = config['commandParams']['stack_id']

    stack_name = None
    self.stack_root_folder = None
    if stack_id and "-" in stack_id:
      stack_split = stack_id.split("-")
      if len(stack_split) == 2:
        stack_name = stack_split[0].upper()
        if stack_name in self.STACK_TO_ROOT_FOLDER:
          self.stack_root_folder = self.STACK_TO_ROOT_FOLDER[stack_name]
    if self.stack_root_folder is None:
      raise Fail("Cannot determine the stack's root directory by parsing the stack_id property, {0}".format(str(stack_id)))

    self.repository_version = self.repository_version.strip()

    # Install/update repositories
    installed_repositories = []
    self.current_repositories = []
    self.current_repo_files = set()

    Logger.info("Will install packages for repository version {0}".format(self.repository_version))
    try:
      append_to_file = False
      for url_info in base_urls:
        repo_name, repo_file = self.install_repository(url_info, append_to_file, template)
        self.current_repositories.append(repo_name)
        self.current_repo_files.add(repo_file)
        append_to_file = True

      installed_repositories = list_ambari_managed_repos()
    except Exception, err:
      Logger.logger.exception("Cannot distribute repositories. Error: {0}".format(str(err)))
      num_errors += 1

    # Build structured output with initial values
    self.structured_output = {
      'ambari_repositories': installed_repositories,
      'installed_repository_version': self.repository_version,
      'stack_id': stack_id,
      'package_installation_result': 'FAIL'
    }
    self.put_structured_out(self.structured_output)

    if num_errors > 0:
      raise Fail("Failed to distribute repositories/install packages")

    # If the repo contains a build number, optimistically assume it to be the actual_version. It will get changed
    # to correct value if it is not
    self.actual_version = None
    if self.repository_version:
      m = re.search("[\d\.]+-\d+", self.repository_version)
      if m:
        # Contains a build number
        self.structured_output['actual_version'] = self.repository_version  # This is the best value known so far.
        self.put_structured_out(self.structured_output)

    # Initial list of versions, used to compute the new version installed
    self.old_versions = self.hdp_versions()

    try:
      # It's possible for the process to receive a SIGTERM while installing the packages
      ret_code = self.install_packages(package_list)
      if ret_code == 0:
        self.structured_output['package_installation_result'] = 'SUCCESS'
        self.put_structured_out(self.structured_output)
      else:
        num_errors += 1
    except Exception, err:
      Logger.logger.exception("Could not install packages. Error: {0}".format(str(err)))

    # Provide correct exit code
    if num_errors > 0:
      raise Fail("Failed to distribute repositories/install packages")

    if 'package_installation_result' in self.structured_output and \
      'actual_version' in self.structured_output and \
      self.structured_output['package_installation_result'] == 'SUCCESS':
      conf_select.create_config_links(stack_id, self.structured_output['actual_version'])

  def get_actual_version_from_file(self):
    """
    Search the repository version history file for a line that contains repository_version,actual_version
    Notice that the parts are delimited by a comma.
    :return: Return the actual_version if found, otherwise, return None.
    """
    actual_version = None
    if os.path.isfile(self.REPO_VERSION_HISTORY_FILE):
      with open(self.REPO_VERSION_HISTORY_FILE, "r") as f:
        for line in f.readlines():
          line_parts = line.split(",")
          if line_parts and len(line_parts) == 2 and line_parts[0] == self.repository_version:
            item = line_parts[1].strip()
            if item != "":
              actual_version = item
              break
    return actual_version

  def write_actual_version_to_file(self, actual_version):
    """
    Save the tuple of repository_version,actual_version to the repo version history file if the repository_version
    doesn't already exist
    :param actual_version: Repo version with the build number
    :returns Return True if appended the values to the file, otherwise, return False.
    """
    wrote_value = False
    if self.repository_version is None or actual_version is None:
      return

    if self.repository_version == "" or actual_version == "":
      return

    value = self.repository_version + "," + actual_version
    key_exists = False
    try:
      if os.path.isfile(self.REPO_VERSION_HISTORY_FILE):
        with open(self.REPO_VERSION_HISTORY_FILE, "r") as f:
          for line in f.readlines():
            line_parts = line.split(",")
            if line_parts and len(line_parts) == 2 and line_parts[0] == self.repository_version:
              key_exists = True
              break

      if not key_exists:
        with open(self.REPO_VERSION_HISTORY_FILE, "a") as f:
          f.write(self.repository_version + "," + actual_version + "\n")
          wrote_value = True
      if wrote_value:
        Logger.info("Appended value \"{0}\" to file {1} to track this as a new version.".format(value, self.REPO_VERSION_HISTORY_FILE))
    except Exception, err:
      Logger.error("Failed to write to file {0} the value: {1}. Error: {2}".format(self.REPO_VERSION_HISTORY_FILE, value, str(err)))

    return wrote_value

  def compute_actual_version(self):
    """
    After packages are installed, determine what the new actual version is, in order to save it.
    """
    Logger.info("Attempting to determine actual version with build number.")
    Logger.info("Old versions: {0}".format(self.old_versions))

    new_versions = self.hdp_versions()
    Logger.info("New versions: {0}".format(new_versions))

    deltas = set(new_versions) - set(self.old_versions)
    Logger.info("Deltas: {0}".format(deltas))

    if 1 == len(deltas):
      self.actual_version = next(iter(deltas)).strip()
      self.structured_output['actual_version'] = self.actual_version
      self.put_structured_out(self.structured_output)
      self.write_actual_version_to_file(self.actual_version)
    else:
      Logger.info("Cannot determine a new actual version installed by using the delta method.")
      # If the first install attempt does a partial install and is unable to report this to the server,
      # then a subsequent attempt will report an empty delta. For this reason, it is important to search the
      # repo version history file to determine if we previously did write an actual_version.
      self.actual_version = self.get_actual_version_from_file()
      if self.actual_version is not None:
        self.actual_version = self.actual_version.strip()
        self.structured_output['actual_version'] = self.actual_version
        self.put_structured_out(self.structured_output)
        Logger.info("Found actual version {0} by parsing file {1}".format(self.actual_version, self.REPO_VERSION_HISTORY_FILE))
      else:
        # It's likely that this host does not have any Stack Components installed, so only contains AMS.
        if not os.path.exists(self.stack_root_folder):
          # Special case when this host does not contain any HDP components, but still contains other components like AMS.
          msg = "Could not determine actual version. This stack's root directory ({0}) is not present on this host, so this host does not contain any versionable components. " \
                "Therefore, ignore this host and allow other hosts to report the correct repository version.".format(self.stack_root_folder)
          Logger.info(msg)
        else:
          msg = "Could not determine actual version. This stack's root directory ({0}) exists but was not able to determine the actual repository version installed. " \
                "Try reinstalling packages again.".format(self.stack_root_folder)
          raise Fail(msg)


  def install_packages(self, package_list):
    """
    Actually install the packages using the package manager.
    :param package_list: List of package names to install
    :return: Returns 0 if no errors were found, and 1 otherwise.
    """
    ret_code = 0
    # Install packages
    packages_were_checked = False
    try:
      packages_installed_before = []
      allInstalledPackages(packages_installed_before)
      packages_installed_before = [package[0] for package in packages_installed_before]
      packages_were_checked = True
      for package in package_list:
        name = self.format_package_name(package['name'], self.repository_version)
        Package(name,
                use_repos=list(self.current_repo_files) if OSCheck.is_ubuntu_family() else self.current_repositories,
                skip_repos=[self.REPO_FILE_NAME_PREFIX + "*"] if OSCheck.is_redhat_family() else [])
    except Exception, err:
      ret_code = 1
      Logger.logger.exception("Package Manager failed to install packages. Error: {0}".format(str(err)))

      # Remove already installed packages in case of fail
      if packages_were_checked and packages_installed_before:
        packages_installed_after = []
        allInstalledPackages(packages_installed_after)
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
            Package(package, action="remove")
    else:
      # Compute the actual version in order to save it in structured out
      try:
        self.compute_actual_version()
      except Exception, err:
        ret_code = 1
        Logger.logger.exception("Failure while computing actual version. Error: {0}".format(str(err)))

    pass
    return ret_code

  def install_repository(self, url_info, append_to_file, template):

    repo = {
      'repoName': "{0}-{1}".format(url_info['name'], self.repository_version)
    }

    if not 'baseUrl' in url_info:
      repo['baseurl'] = None
    else:
      repo['baseurl'] = url_info['baseUrl']

    if not 'mirrorsList' in url_info:
      repo['mirrorsList'] = None
    else:
      repo['mirrorsList'] = url_info['mirrorsList']

    ubuntu_components = [url_info['name']] + self.UBUNTU_REPO_COMPONENTS_POSTFIX
    file_name = self.REPO_FILE_NAME_PREFIX + self.repository_version

    Repository(repo['repoName'],
      action = "create",
      base_url = repo['baseurl'],
      mirror_list = repo['mirrorsList'],
      repo_file_name = file_name,
      repo_template = template,
      append_to_file = append_to_file,
      components = ubuntu_components,  # ubuntu specific
    )
    return repo['repoName'], file_name

  def format_package_name(self, package_name, repo_id):
    """
    This method overcomes problems at SLES SP3. Zypper here behaves differently
    than at SP1, and refuses to install packages by mask if there is any installed package that
    matches this mask.
    So we preppend concrete HDP version to mask under Suse
    """
    if OSCheck.is_suse_family() and '*' in package_name:
      mask_version = re.search(r'((_\d+)*(_)?\*)', package_name).group(0)
      formatted_version = '_' + repo_id.replace('.', '_').replace('-', '_') + '*'
      return package_name.replace(mask_version, formatted_version)
    else:
      return package_name

  def hdp_versions(self):
    code, out = call("hdp-select versions")
    if 0 == code:
      versions = []
      for line in out.splitlines():
        versions.append(line.rstrip('\n'))
      return versions
    else:
      return []

  def abort_handler(self, signum, frame):
    Logger.error("Caught signal {0}, will handle it gracefully. Compute the actual version if possible before exiting.".format(signum))
    self.compute_actual_version()


if __name__ == "__main__":
  InstallPackages().execute()
