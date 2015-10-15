#!/usr/bin/env python

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
import os
import tempfile
from mock.mock import patch
from mock.mock import MagicMock
from only_for_platform import not_for_platform, PLATFORM_WINDOWS
from ambari_commons.os_check import OSCheck
from ambari_agent.Register import Register
from ambari_agent.AmbariConfig import AmbariConfig
from ambari_agent.Hardware import Hardware

@not_for_platform(PLATFORM_WINDOWS)
class TestRegistration(TestCase):

  @patch.object(Hardware, "osdisks", new = MagicMock(return_value=[]))
  @patch.object(Hardware, "_chk_mount", new = MagicMock(return_value=True))
  @patch("ambari_commons.firewall.run_os_command")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_version")
  def test_registration_build(self, get_os_version_mock, get_os_type_mock, run_os_cmd_mock):
    config = AmbariConfig().getConfig()
    tmpdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tmpdir)
    config.set('agent', 'current_ping_port', '33777')
    get_os_type_mock.return_value = "suse"
    get_os_version_mock.return_value = "11"
    run_os_cmd_mock.return_value = (3, "", "")
    register = Register(config)
    reference_version = '2.1.0'
    data = register.build(reference_version, 1)
    #print ("Register: " + pprint.pformat(data))
    self.assertEquals(len(data['hardwareProfile']) > 0, True, "hardwareProfile should contain content")
    self.assertEquals(data['hostname'] != "", True, "hostname should not be empty")
    self.assertEquals(data['publicHostname'] != "", True, "publicHostname should not be empty")
    self.assertEquals(data['responseId'], 1)
    self.assertEquals(data['timestamp'] > 1353678475465L, True, "timestamp should not be empty")
    self.assertEquals(len(data['agentEnv']) > 0, True, "agentEnv should not be empty")
    self.assertEquals(data['agentVersion'], reference_version, "agentVersion should not be empty")
    print data['agentEnv']['umask']
    self.assertEquals(not data['agentEnv']['umask']== "", True, "agents umask should not be empty")
    self.assertEquals(data['currentPingPort'] == 33777, True, "current ping port should be 33777")
    self.assertEquals(data['prefix'], config.get('agent', 'prefix'), 'The prefix path does not match')
    self.assertEquals(len(data), 9)


