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

import re
import os.path

import ambari_simplejson as json  # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.

from resource_management import *
from resource_management.libraries.functions.list_ambari_managed_repos import list_ambari_managed_repos
from ambari_commons.os_check import OSCheck, OSConst
from ambari_commons.str_utils import cbool, cint
from resource_management.libraries.functions.packages_analyzer import allInstalledPackages
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions.hdp_select import get_hdp_versions
from resource_management.libraries.functions.version import compare_versions, format_hdp_stack_version
from resource_management.libraries.functions.repo_version_history \
  import read_actual_version_from_history_file, write_actual_version_to_history_file, REPO_VERSION_HISTORY_FILE
from resource_management.core.resources.system import Execute

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

    # current stack information
    self.current_hdp_stack_version = None
    if 'stack_version' in config['hostLevelParams']:
      current_stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
      self.current_hdp_stack_version = format_hdp_stack_version(current_stack_version_unformatted)


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
    if self.repository_version is None:
      raise Fail("Cannot determine the repository version to install")

    self.repository_version = self.repository_version.strip()


    # Install/update repositories
    installed_repositories = []
    self.current_repositories = []
    self.current_repo_files = set()

    # Enable base system repositories
    # We don't need that for RHEL family, because we leave all repos enabled
    # except disabled HDP* ones
    if OSCheck.is_suse_family():
      self.current_repositories.append('base')
    elif OSCheck.is_ubuntu_family():
      self.current_repo_files.add('base')

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

    # Initial list of versions, used to compute the new version installed
    self.old_versions = get_hdp_versions(self.stack_root_folder)

    try:
      is_package_install_successful = False
      ret_code = self.install_packages(package_list)
      if ret_code == 0:
        self.structured_output['package_installation_result'] = 'SUCCESS'
        self.put_structured_out(self.structured_output)
        is_package_install_successful = True
      else:
        num_errors += 1
    except Exception, err:
      num_errors += 1
      Logger.logger.exception("Could not install packages. Error: {0}".format(str(err)))

    # Provide correct exit code
    if num_errors > 0:
      raise Fail("Failed to distribute repositories/install packages")

    # if installing a version of HDP that needs some symlink love, then create them
    if is_package_install_successful and 'actual_version' in self.structured_output:
      self._create_config_links_if_necessary(stack_id, self.structured_output['actual_version'])

  def _clear_package_manager_cache(self):
    package_manager_cmd = ""

    if OSCheck.is_redhat_family():
      package_manager_cmd = ("/usr/bin/yum", "clean", "metadata")

    if OSCheck.is_suse_family():
      package_manager_cmd = ("/usr/bin/zypper", "-q", "-n", "clean")

    if OSCheck.is_ubuntu_family():
      return

    Logger.debug("Clearing repo manager metadata")
    Execute(package_manager_cmd, logoutput=False, sudo=True)

  def _create_config_links_if_necessary(self, stack_id, stack_version):
    """
    Sets up the required structure for /etc/<component>/conf symlinks and /usr/hdp/current
    configuration symlinks IFF the current stack is < HDP 2.3+ and the new stack is >= HDP 2.3

    stack_id:  stack id, ie HDP-2.3
    stack_version:  version to set, ie 2.3.0.0-1234
    """
    if stack_id is None:
      Logger.info("Cannot create config links when stack_id is not defined")
      return

    args = stack_id.upper().split('-')
    if len(args) != 2:
      Logger.info("Unrecognized stack id {0}, cannot create config links".format(stack_id))
      return

    if args[0] != "HDP":
      Logger.info("Unrecognized stack name {0}, cannot create config links".format(args[0]))

    if compare_versions(format_hdp_stack_version(args[1]), "2.3.0.0") < 0:
      Logger.info("Configuration symlinks are not needed for {0}, only HDP-2.3+".format(stack_version))
      return

    for package_name, directories in conf_select.PACKAGE_DIRS.iteritems():
      # if already on HDP 2.3, then we should skip making conf.backup folders
      if self.current_hdp_stack_version and compare_versions(self.current_hdp_stack_version, '2.3') >= 0:
        Logger.info("The current cluster stack of {0} does not require backing up configurations; "
                    "only conf-select versioned config directories will be created.".format(stack_version))
        # only link configs for all known packages
        conf_select.select("HDP", package_name, stack_version, ignore_errors = True)
      else:
        # link configs and create conf.backup folders for all known packages
        # this will also call conf-select select
        conf_select.convert_conf_directories_to_symlinks(package_name, stack_version, directories,
          skip_existing_links = False, link_to = "backup")


  def compute_actual_version(self):
    """
    After packages are installed, determine what the new actual version is.
    """

    # If the repo contains a build number, optimistically assume it to be the actual_version. It will get changed
    # to correct value if it is not
    self.actual_version = None
    self.repo_version_with_build_number = None
    if self.repository_version:
      m = re.search("[\d\.]+-\d+", self.repository_version)
      if m:
        # Contains a build number
        self.repo_version_with_build_number = self.repository_version
        self.structured_output['actual_version'] = self.repo_version_with_build_number  # This is the best value known so far.
        self.put_structured_out(self.structured_output)

    Logger.info("Attempting to determine actual version with build number.")
    Logger.info("Old versions: {0}".format(self.old_versions))

    new_versions = get_hdp_versions(self.stack_root_folder)
    Logger.info("New versions: {0}".format(new_versions))

    deltas = set(new_versions) - set(self.old_versions)
    Logger.info("Deltas: {0}".format(deltas))

    # Get version without build number
    normalized_repo_version = self.repository_version.split('-')[0]

    if 1 == len(deltas):
      self.actual_version = next(iter(deltas)).strip()
      self.structured_output['actual_version'] = self.actual_version
      self.put_structured_out(self.structured_output)
      write_actual_version_to_history_file(normalized_repo_version, self.actual_version)
      Logger.info(
        "Found actual version {0} by checking the delta between versions before and after installing packages".format(
          self.actual_version))
    else:
      # If the first install attempt does a partial install and is unable to report this to the server,
      # then a subsequent attempt will report an empty delta. For this reason, we search for a best fit version for the repo version
      Logger.info("Cannot determine actual version installed by checking the delta between versions "
                  "before and after installing package")
      Logger.info("Will try to find for the actual version by searching for best possible match in the list of versions installed")
      self.actual_version = self.find_best_fit_version(new_versions, self.repository_version)
      if self.actual_version is not None:
        self.actual_version = self.actual_version.strip()
        self.structured_output['actual_version'] = self.actual_version
        self.put_structured_out(self.structured_output)
        Logger.info("Found actual version {0} by searching for best possible match".format(self.actual_version))
      else:
        # It's likely that this host does not have any Stack Components installed, so only contains AMS.
        # So just use repo version value provided by server (we already put it to structured output)
        if not os.path.exists(self.stack_root_folder):
          # Special case when this host does not contain any HDP components, but still contains other components like AMS.
          msg = "Could not determine actual version. This stack's root directory ({0}) is not present on this host, so this host does not contain any versionable components. " \
                "Therefore, ignore this host and allow other hosts to report the correct repository version.".format(self.stack_root_folder)
          Logger.info(msg)
        else:
          msg = "Could not determine actual version. This stack's root directory ({0}) exists but was not able to determine the actual repository version installed. " \
                "Try reinstalling packages again.".format(self.stack_root_folder)
          raise Fail(msg)

  def check_partial_install(self):
    """
    If an installation did not complete successfully, check if installation was partially complete and
    log the partially completed version to REPO_VERSION_HISTORY_FILE.
    :return:
    """
    Logger.info("Installation of packages failed. Checking if installation was partially complete")
    Logger.info("Old versions: {0}".format(self.old_versions))

    new_versions = get_hdp_versions(self.stack_root_folder)
    Logger.info("New versions: {0}".format(new_versions))

    deltas = set(new_versions) - set(self.old_versions)
    Logger.info("Deltas: {0}".format(deltas))

    # Get version without build number
    normalized_repo_version = self.repository_version.split('-')[0]

    if 1 == len(deltas):
      # Some packages were installed successfully. Log this version to REPO_VERSION_HISTORY_FILE
      partial_install_version = next(iter(deltas)).strip()
      write_actual_version_to_history_file(normalized_repo_version, partial_install_version)
      Logger.info("Version {0} was partially installed. ".format(partial_install_version))

  def find_best_fit_version(self, versions, repo_version):
    """
    Given a list of installed versions and a repo version, search for a version that best fits the repo version
    If the repo version is found in the list of installed versions, return the repo version itself.
    If the repo version is not found in the list of installed versions
    normalize the repo version and use the REPO_VERSION_HISTORY_FILE file to search the list.

    :param versions: List of versions installed
    :param repo_version: Repo version to search
    :return: Matching version, None if no match was found.
    """
    if versions is None or repo_version is None:
      return None

    build_num_match = re.search("[\d\.]+-\d+", repo_version)
    if build_num_match and repo_version in versions:
      # If repo version has build number and is found in the list of versions, return it as the matching version
      Logger.info("Best Fit Version: Resolved from repo version with valid build number: {0}".format(repo_version))
      return repo_version

    # Get version without build number
    normalized_repo_version = repo_version.split('-')[0]

    # Find all versions that match the normalized repo version
    match_versions = filter(lambda x: x.startswith(normalized_repo_version), versions)
    if match_versions:

      if len(match_versions) == 1:
        # Resolved without conflicts
        Logger.info("Best Fit Version: Resolved from normalized repo version without conflicts: {0}".format(match_versions[0]))
        return match_versions[0]

      # Resolve conflicts using REPO_VERSION_HISTORY_FILE
      history_version = read_actual_version_from_history_file(normalized_repo_version)

      # Validate history version retrieved is valid
      if history_version in match_versions:
        Logger.info("Best Fit Version: Resolved from normalized repo version using {0}: {1}".format(REPO_VERSION_HISTORY_FILE, history_version))
        return history_version

    # No matching version
    return None


  def install_packages(self, package_list):
    """
    Actually install the packages using the package manager.
    :param package_list: List of package names to install
    :return: Returns 0 if no errors were found, and 1 otherwise.
    """
    ret_code = 0

    # Clear cache of package manager right before installation of the packages
    self._clear_package_manager_cache()

    config = self.get_config()
    agent_stack_retry_on_unavailability = cbool(config['hostLevelParams']['agent_stack_retry_on_unavailability'])
    agent_stack_retry_count = cint(config['hostLevelParams']['agent_stack_retry_count'])

    # Install packages
    packages_were_checked = False
    try:
      Package(self.get_base_packages_to_install(),
              retry_on_repo_unavailability=agent_stack_retry_on_unavailability,
              retry_count=agent_stack_retry_count)

      packages_installed_before = []
      allInstalledPackages(packages_installed_before)
      packages_installed_before = [package[0] for package in packages_installed_before]
      packages_were_checked = True
      filtered_package_list = self.filter_package_list(package_list)
      for package in filtered_package_list:
        name = self.format_package_name(package['name'], self.repository_version)
        Package(name,
                use_repos=list(self.current_repo_files) if OSCheck.is_ubuntu_family() else self.current_repositories,
                skip_repos=[self.REPO_FILE_NAME_PREFIX + "*"] if OSCheck.is_redhat_family() else [],
                retry_on_repo_unavailability=agent_stack_retry_on_unavailability,
                retry_count=agent_stack_retry_count)
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
    # Compute the actual version in order to save it in structured out
    try:
      if ret_code == 0:
         self.compute_actual_version()
      else:
        self.check_partial_install()
    except Fail, err:
      ret_code = 1
      Logger.logger.exception("Failure while computing actual version. Error: {0}".format(str(err)))
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

  def abort_handler(self, signum, frame):
    Logger.error("Caught signal {0}, will handle it gracefully. Compute the actual version if possible before exiting.".format(signum))
    self.check_partial_install()

  def get_base_packages_to_install(self):
    """
    HACK: list packages which should be installed without disabling any repos. (This is planned to fix in Ambari-2.2)
    """
    base_packages_to_install = ['fuse']

    if OSCheck.is_suse_family() or OSCheck.is_ubuntu_family():
      base_packages_to_install.append('libfuse2')
    else:
      base_packages_to_install.append('fuse-libs')

    return base_packages_to_install


  def filter_package_list(self, package_list):
    """
    Note: that we have skipUpgrade option in metainfo.xml to filter packages,
    so use this method only if, for some reason the metainfo option cannot be used.

    Here we filter packages that are managed with custom logic in package
    scripts. Usually this packages come from system repositories, and either
     are not available when we restrict repository list, or should not be
    installed on host at all.
    :param package_list: original list
    :return: filtered package_list
    """
    filtered_package_list = []
    for package in package_list:
      skip_package = False

      # skip upgrade for hadooplzo* versioned package, only if lzo is disabled 
      io_compression_codecs = default("/configurations/core-site/io.compression.codecs", None)
      if not io_compression_codecs or "com.hadoop.compression.lzo" not in io_compression_codecs:
        skip_package = package['name'].startswith('hadooplzo')

      if not skip_package:
        filtered_package_list.append(package)
    return filtered_package_list


if __name__ == "__main__":
  InstallPackages().execute()
