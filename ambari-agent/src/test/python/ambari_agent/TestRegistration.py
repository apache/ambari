#!/usr/bin/env python3

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
import tempfile
from mock.mock import patch
from mock.mock import MagicMock
from only_for_platform import not_for_platform, PLATFORM_WINDOWS
from ambari_commons.os_check import OSCheck
from ambari_agent.AmbariConfig import AmbariConfig
from ambari_agent.Hardware import Hardware
from ambari_agent.Facter import FacterLinux

@not_for_platform(PLATFORM_WINDOWS)
class TestRegistration(TestCase):

  @patch("subprocess.Popen")
  @patch.object(Hardware, "_chk_writable_mount", new = MagicMock(return_value=True))
  @patch("builtins.open", new=MagicMock())
  @patch.object(FacterLinux, "facterInfo", new = MagicMock(return_value={}))
  @patch.object(FacterLinux, "__init__", new = MagicMock(return_value = None))
  @patch("resource_management.core.shell.call")
  @patch.object(OSCheck, "get_os_family")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_version")
  def test_registration_build(self, get_os_version_mock, get_os_family_mock, get_os_type_mock, run_os_cmd_mock, Popen_mock):
    config = AmbariConfig()
    tmpdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tmpdir)
    config.set('agent', 'current_ping_port', '33777')
    get_os_family_mock.return_value = "suse"
    get_os_type_mock.return_value = "suse"
    get_os_version_mock.return_value = "11"
    run_os_cmd_mock.return_value = (3, "", "")
    from ambari_agent.Register import Register
    register = Register(config)
    data = register.build()
    self.assertEqual(len(data['hardwareProfile']) > 0, True, "hardwareProfile should contain content")
    self.assertEqual(data['hostname'] != "", True, "hostname should not be empty")
    self.assertEqual(data['publicHostname'] != "", True, "publicHostname should not be empty")
    self.assertEqual(data['id'], -1)
    self.assertEqual(data['timestamp'] > 1353678475465, True, "timestamp should not be empty")
    self.assertEqual(len(data['agentEnv']) > 0, True, "agentEnv should not be empty")
    self.assertEqual(not data['agentEnv']['umask']== "", True, "agents umask should not be empty")
    self.assertEqual(data['prefix'], config.get('agent', 'prefix'), 'The prefix path does not match')
    self.assertEqual(len(data), 10)


