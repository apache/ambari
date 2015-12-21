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
from ambari_commons.os_check import OSCheck
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