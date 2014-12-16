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

import json
import sys
import traceback
from resource_management import *
from resource_management.libraries.functions.list_ambari_managed_repos import *
from ambari_commons.os_check import OSCheck, OSConst
from resource_management.libraries.functions import packages_analyzer


class InstallPackages(Script):
  """
  This script is a part of Rolling Upgrade workflow and is described at
  appropriate design doc.
  It installs repositories to the node and then installs packages.
  For now, repositories are installed into individual files.
  """

  UBUNTU_REPO_COMPONENTS_POSTFIX = ["main"]
  REPO_FILE_NAME_PREFIX = 'HDP-'

  def actionexecute(self, env):
    delayed_fail = False
    package_install_result = False

    # Parse parameters
    config = Script.get_config()

    # Select dict that contains parameters
    try:
      repository_version = config['roleParams']['repository_version']
      base_urls = json.loads(config['roleParams']['base_urls'])
      package_list = json.loads(config['roleParams']['package_list'])
    except KeyError:
      # Last try
      repository_version = config['commandParams']['repository_version']
      base_urls = json.loads(config['commandParams']['base_urls'])
      package_list = json.loads(config['commandParams']['package_list'])

    # Install/update repositories
    installed_repositories = []
    current_repositories = ['base']  # Some our packages are installed from the base repo
    current_repo_files = set(['base'])
    try:
      append_to_file = False
      for url_info in base_urls:
        repo_name, repo_file = self.install_repository(url_info, repository_version, append_to_file)
        current_repositories.append(repo_name)
        current_repo_files.add(repo_file)
        append_to_file = True

      installed_repositories = list_ambari_managed_repos()
    except Exception, err:
      print "Can not distribute repositories."
      print traceback.format_exc()
      delayed_fail = True

    # Install packages
    if not delayed_fail:
      packages_were_checked = False
      try:
        packages_installed_before = []
        packages_analyzer.allInstalledPackages(packages_installed_before)
        packages_installed_before = [package[0] for package in packages_installed_before]
        packages_were_checked = True
        for package in package_list:
          Package(package['name'], use_repos=list(current_repo_files) if OSCheck.is_ubuntu_family() else current_repositories)
        package_install_result = True
      except Exception, err:
        print "Can not install packages."
        print traceback.format_exc()
        delayed_fail = True

        # Remove already installed packages in case of fail
        if packages_were_checked and packages_installed_before:
          packages_installed_after = []
          packages_analyzer.allInstalledPackages(packages_installed_after)
          packages_installed_after = [package[0] for package in packages_installed_after]
          packages_installed_before = set(packages_installed_before)
          new_packages_installed = [package for package in packages_installed_after if package not in packages_installed_before]
          for package in new_packages_installed:
            Package(package, action="remove")

    # Build structured output
    structured_output = {
      'ambari_repositories': installed_repositories,
      'installed_repository_version': repository_version,
      'package_installation_result': 'SUCCESS' if package_install_result else 'FAIL'
    }
    self.put_structured_out(structured_output)

    # Provide correct exit code
    if delayed_fail:
      raise Fail("Failed to distribute repositories/install packages")

  def install_repository(self, url_info, repository_version, append_to_file):
    template = "repo_suse_rhel.j2" if OSCheck.is_redhat_family() or OSCheck.is_suse_family() else "repo_ubuntu.j2"

    repo = {
      'repoName': "{0}-{1}".format(url_info['name'], repository_version)
    }

    if not 'baseUrl' in url_info:
      repo['baseurl'] = None
    else:
      repo['baseurl'] = url_info['baseUrl']

    if not 'mirrorsList' in url_info:
      repo['mirrorsList'] = None
    else:
      repo['mirrorsList'] = url_info['mirrorsList']

    ubuntu_components = [url_info['repositoryId']] + self.UBUNTU_REPO_COMPONENTS_POSTFIX
    file_name = self.REPO_FILE_NAME_PREFIX + repository_version

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

if __name__ == "__main__":
  InstallPackages().execute()
