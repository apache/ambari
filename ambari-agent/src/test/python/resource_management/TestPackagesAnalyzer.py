'''
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
'''
from unittest import TestCase
from mock.mock import patch, MagicMock, call
from ambari_commons.os_check import OSCheck, OSConst
from resource_management.core.exceptions import Fail
from resource_management.libraries.functions import packages_analyzer

class TestPackagesAnalyzer(TestCase):
  @patch("resource_management.libraries.functions.packages_analyzer.rmf_shell.checked_call")
  @patch.object(OSCheck, "is_ubuntu_family")
  def test_get_installed_package_version_ubuntu(self, is_ubuntu_family_mock, checked_call_mock):
    is_ubuntu_family_mock.return_value = True
    checked_call_mock.return_value = (0, '1.2.3','')
    result = packages_analyzer.getInstalledPackageVersion("package1")
    self.assertEqual(result, '1.2.3')
    self.assertEqual(checked_call_mock.call_args_list, [call("dpkg -s package1 | grep Version | awk '{print $2}'", stderr=-1)])
    
  @patch("resource_management.libraries.functions.packages_analyzer.rmf_shell.checked_call")
  @patch.object(OSCheck, "is_ubuntu_family")
  def test_get_installed_package_version_centos_suse(self, is_ubuntu_family_mock, checked_call_mock):
    is_ubuntu_family_mock.return_value = False
    checked_call_mock.return_value = (0, '0.0.1-SNAPSHOT','')
    result = packages_analyzer.getInstalledPackageVersion("package1")
    self.assertEqual(result, '0.0.1-SNAPSHOT')
    self.assertEqual(checked_call_mock.call_args_list, [call("rpm -q --queryformat '%{version}-%{release}' package1 | sed -e 's/\\.el[0-9]//g'", stderr=-1)])

  @patch("resource_management.libraries.functions.packages_analyzer.rmf_shell.checked_call")
  @patch.object(OSCheck, "is_in_family")
  @patch.object(packages_analyzer, "Logger")
  def test_vefify_dependency_suse(self, logger_mock, in_family_patch_mock, checked_call_mock):
    test_cases = [
      {
        "name": "SUSE Case",
        "os": OSConst.SUSE_FAMILY,
        "cheked_call_result": "5 new packages to install"
      },
      {
        "name": "REDHAT Case",
        "os": OSConst.REDHAT_FAMILY,
        "cheked_call_result": "Error:"
      },
      {
        "name": "UBUNTU Case",
        "os": OSConst.UBUNTU_FAMILY,
        "cheked_call_result": "E:"
      }
    ]

    for test_case in test_cases:
      in_family_patch_mock.side_effect = lambda current_family, family: family == test_case["os"]
      checked_call_mock.return_value = (0, "OK.")

      #  happy scenario
      self.assertTrue(packages_analyzer.verifyDependencies(), "test_verify_dependency failed on '%s'" % test_case["name"])

      #  unhappy scenario
      checked_call_mock.return_value = (0, test_case["cheked_call_result"])
      self.assertFalse(packages_analyzer.verifyDependencies(), "test_verify_dependency failed on '%s'" % test_case["name"])

      try:
        in_family_patch_mock.side_effect = lambda current_family, family: False
        packages_analyzer.verifyDependencies()
        self.assertTrue(False, "Wrong handling of unknown operation system")
      except Fail:
        pass

  @patch("resource_management.libraries.functions.packages_analyzer.rmf_shell.checked_call")
  @patch.object(OSCheck, "is_in_family")
  def test_vefify_dependency_redhat(self, in_family_patch_mock, checked_call_mock):
    in_family_patch_mock.side_effect = lambda current_family, family: family == OSConst.REDHAT_FAMILY
    checked_call_mock.return_value = (0, "OK.")
    pass

  @patch("resource_management.libraries.functions.packages_analyzer.rmf_shell.checked_call")
  @patch.object(OSCheck, "is_in_family")
  def test_vefify_dependency_ubuntu(self, in_family_patch_mock, checked_call_mock):
    in_family_patch_mock.side_effect = lambda current_family, family: family == OSConst.UBUNTU_FAMILY
    checked_call_mock.return_value = (0, "OK.")
    pass

  def test_perform_package_analysis(self):
    installedPackages = [
      ["hadoop-a", "2.3", "HDP"], ["zk", "3.1", "HDP"], ["webhcat", "3.1", "HDP"],
      ["hadoop-b", "2.3", "HDP-epel"], ["epel", "3.1", "HDP-epel"], ["epel-2", "3.1", "HDP-epel"],
      ["hadoop-c", "2.3", "Ambari"], ["ambari-s", "3.1", "Ambari"],
      ["ganglia", "2.3", "GANGLIA"], ["rrd", "3.1", "RRD"],
      ["keeper-1", "2.3", "GANGLIA"], ["keeper-2", "3.1", "base"],["def-def.x86", "2.2", "DEF.3"],
      ["def.1", "1.2", "NewDEF"]
    ]
    availablePackages = [
      ["hadoop-d", "2.3", "HDP"], ["zk-2", "3.1", "HDP"], ["pig", "3.1", "HDP"],
      ["epel-3", "2.3", "HDP-epel"], ["hadoop-e", "3.1", "HDP-epel"],
      ["ambari-a", "3.1", "Ambari"],
      ["keeper-3", "3.1", "base"]
    ]

    packagesToLook = ["^webhcat.*$", "^hadoop.*$", "^.+-def.*$"]
    reposToIgnore = ["ambari"]
    additionalPackages = ["ganglia", "rrd"]

    repos = []
    packages_analyzer.getInstalledRepos(packagesToLook, installedPackages + availablePackages, reposToIgnore, repos)
    self.assertEqual(3, len(repos))
    expected = ["HDP", "HDP-epel", "DEF.3"]
    for repo in expected:
      self.assertTrue(repo in repos)

    packagesInstalled = packages_analyzer.getInstalledPkgsByRepo(repos, ["epel"], installedPackages)
    self.assertEqual(5, len(packagesInstalled))
    expected = ["hadoop-a", "zk", "webhcat", "hadoop-b", "def-def.x86"]
    for repo in expected:
      self.assertTrue(repo in packagesInstalled)

    additionalPkgsInstalled = packages_analyzer.getInstalledPkgsByNames(
      additionalPackages, installedPackages)
    self.assertEqual(2, len(additionalPkgsInstalled))
    expected = ["ganglia", "rrd"]
    for additionalPkg in expected:
      self.assertTrue(additionalPkg in additionalPkgsInstalled)

    allPackages = list(set(packagesInstalled + additionalPkgsInstalled))
    self.assertEqual(7, len(allPackages))
    expected = ["hadoop-a", "zk", "webhcat", "hadoop-b", "ganglia", "rrd", "def-def.x86"]
    for package in expected:
      self.assertTrue(package in allPackages)