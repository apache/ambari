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


class InstallPackages(Script):
  """
  This script is a part of Rolling Upgrade workflow and is described at
  appropriate design doc.
  It installs repositories to the node and then installs packages.
  For now, repositories are installed into individual files.
  """

  UBUNTU_REPO_COMPONENTS_POSTFIX = ["main"]

  def actionexecute(self, env):
    delayed_fail = False
    package_install_result = False

    # Parse parameters
    config = Script.get_config()
    base_urls = json.loads(config['commandParams']['base_urls'])
    package_list = json.loads(config['commandParams']['package_list'])

    # Install/update repositories
    installed_repositories = []
    try:
      for url_info in base_urls:
        self.install_repository(url_info)

      installed_repositories = list_ambari_managed_repos()
    except Exception, err:
      print "Can not distribute repositories."
      print traceback.format_exc()
      delayed_fail = True

    # Install packages
    if not delayed_fail:
      try:
        for package in package_list:
          Package(package)
        package_install_result = True
      except Exception, err:
        print "Can not install packages."
        print traceback.format_exc()
        delayed_fail = True
        # TODO : remove already installed packages in case of fail

    # Build structured output
    structured_output = {
      'ambari_repositories': installed_repositories,
      'package_installation_result': 'SUCCESS' if package_install_result else 'FAIL'
    }
    self.put_structured_out(structured_output)

    # Provide correct exit code
    if delayed_fail:
      raise Fail("Failed to distribute repositories/install packages")


  def install_repository(self, url_info):
    template = "repo_suse_rhel.j2" if OSCheck.is_redhat_family() or OSCheck.is_suse_family() else "repo_ubuntu.j2"

    repo = {
      'repoName': url_info['id']
    }

    if not 'baseurl' in url_info:
      repo['baseurl'] = None
    else:
      repo['baseurl'] = url_info['baseurl']

    if not 'mirrorsList' in url_info:
      repo['mirrorsList'] = None
    else:
      repo['mirrorsList'] = url_info['mirrorslist']

    ubuntu_components = [repo['repoName']] + self.UBUNTU_REPO_COMPONENTS_POSTFIX

    Repository(repo['repoName'],
      action = "create",
      base_url = repo['baseurl'],
      mirror_list = repo['mirrorsList'],
      repo_file_name = repo['repoName'],
      repo_template = template,
      components = ubuntu_components,  # ubuntu specific
    )


if __name__ == "__main__":
  InstallPackages().execute()
