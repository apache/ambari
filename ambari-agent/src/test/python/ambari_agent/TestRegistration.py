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

with patch("platform.linux_distribution", return_value = ('Suse','11','Final')):
  from ambari_agent.Register import Register
  from ambari_agent.AmbariConfig import AmbariConfig
  from ambari_agent.HostInfo import HostInfo
  from common_functions import OSCheck

class TestRegistration(TestCase):
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_version")
  def test_registration_build(self, get_os_version_mock, get_os_type_mock):
    config = AmbariConfig().getConfig()
    tmpdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tmpdir)
    config.set('agent', 'current_ping_port', '33777')
    get_os_type_mock.return_value = "suse"
    get_os_version_mock.return_value = "11"
    ver_file = os.path.join(tmpdir, "version")
    with open(ver_file, "w") as text_file:
      text_file.write("1.3.0")

    register = Register(config)
    data = register.build(1)
    #print ("Register: " + pprint.pformat(data))
    self.assertEqual(len(data['hardwareProfile']) > 0, True, "hardwareProfile should contain content")
    self.assertEqual(data['hostname'] != "", True, "hostname should not be empty")
    self.assertEqual(data['publicHostname'] != "", True, "publicHostname should not be empty")
    self.assertEqual(data['responseId'], 1)
    self.assertEqual(data['timestamp'] > 1353678475465, True, "timestamp should not be empty")
    self.assertEqual(len(data['agentEnv']) > 0, True, "agentEnv should not be empty")
    self.assertEqual(data['agentVersion'], '1.3.0', "agentVersion should not be empty")
    print((data['agentEnv']['umask']))
    self.assertEqual(not data['agentEnv']['umask']== "", True, "agents umask should not be empty")
    self.assertEqual(data['currentPingPort'] == 33777, True, "current ping port should be 33777")
    self.assertEqual(len(data), 8)

    os.remove(ver_file)
