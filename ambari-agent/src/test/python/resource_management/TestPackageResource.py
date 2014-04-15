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
from mock.mock import patch, MagicMock

from resource_management.core import Environment, Fail
from resource_management.core.system import System
from resource_management.core.resources import Package

from resource_management.core import shell

class TestPackageResource(TestCase):
  
  @patch.object(shell, "call")
  @patch.object(shell, "checked_call")
  @patch.object(System, "os_family", new = 'debian')
  def test_action_install_debian(self, shell_mock, call_mock):
    call_mock.return_value= (1, None)
    with Environment('/') as env:
      Package("some_package",
      )
    call_mock.assert_called_with('dpkg --get-selections some_package | grep -v deinstall')    
    shell_mock.assert_called_with("/usr/bin/apt-get --force-yes --assume-yes install some_package")


  @patch.object(shell, "call")
  @patch.object(shell, "checked_call")
  @patch.object(System, "os_family", new = 'redhat')
  def test_action_install_rhel(self, shell_mock, call_mock):
    call_mock.return_value= (1, None)
    with Environment('/') as env:
      Package("some_package",
      )
    call_mock.assert_called_with('rpm -q --quiet some_package')    
    shell_mock.assert_called_with("/usr/bin/yum -d 0 -e 0 -y install some_package")

  @patch.object(shell, "call")
  @patch.object(shell, "checked_call")
  @patch.object(System, "os_family", new = 'suse')
  def test_action_install_suse(self, shell_mock, call_mock):
    call_mock.return_value= (1, None)
    with Environment('/') as env:
      Package("some_package",
      )
    call_mock.assert_called_with('rpm -q --quiet some_package')    
    shell_mock.assert_called_with("/usr/bin/zypper --quiet install --auto-agree-with-licenses --no-confirm some_package")

  @patch.object(shell, "call", new = MagicMock(return_value=(0, None)))
  @patch.object(shell, "checked_call")
  @patch.object(System, "os_family", new = 'redhat')
  def test_action_install_existent_rhel(self, shell_mock):
    with Environment('/') as env:
      Package("some_package",
              )
    self.assertFalse(shell_mock.mock_calls)

  @patch.object(shell, "call", new = MagicMock(return_value=(0, None)))
  @patch.object(shell, "checked_call")
  @patch.object(System, "os_family", new = 'suse')
  def test_action_install_existent_suse(self, shell_mock):
    with Environment('/') as env:
      Package("some_package",
              )
    self.assertFalse(shell_mock.mock_calls)

  @patch.object(shell, "call", new = MagicMock(return_value=(0, None)))
  @patch.object(shell, "checked_call")
  @patch.object(System, "os_family", new = 'redhat')
  def test_action_remove_rhel(self, shell_mock):
    with Environment('/') as env:
      Package("some_package",
              action = "remove"
      )    
    shell_mock.assert_called_with("/usr/bin/yum -d 0 -e 0 -y erase some_package")

  @patch.object(shell, "call", new = MagicMock(return_value=(0, None)))
  @patch.object(shell, "checked_call")
  @patch.object(System, "os_family", new = 'suse')
  def test_action_remove_suse(self, shell_mock):
    with Environment('/') as env:
      Package("some_package",
              action = "remove"
      )    
    shell_mock.assert_called_with("/usr/bin/zypper --quiet remove --no-confirm some_package")

  @patch.object(shell, "call", new = MagicMock(return_value=(1, None)))
  @patch.object(shell, "checked_call")
  @patch.object(System, "os_family", new = 'redhat')
  def test_action_install_version_attr(self, shell_mock):
    with Environment('/') as env:
      Package("some_package",
              version = "3.5.0"
      )    
    shell_mock.assert_called_with("/usr/bin/yum -d 0 -e 0 -y install some_package-3.5.0")
