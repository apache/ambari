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

import unittest
import subprocess
import os
import sys
from mock.mock import MagicMock, patch, ANY
with patch("platform.linux_distribution", return_value = ('Suse','11','Final')):
  from ambari_agent.Controller import AGENT_AUTO_RESTART_EXIT_CODE
  from ambari_agent import AmbariAgent


class TestAmbariAgent(unittest.TestCase):

  @patch.object(subprocess, "Popen")
  @patch("os.path.isfile")
  @patch("os.remove")
  def test_main(self, os_remove_mock, os_path_isfile_mock, subprocess_popen_mock):
    facter1 = MagicMock()
    facter2 = MagicMock()
    subprocess_popen_mock.side_effect = [facter1, facter2]
    facter1.returncode = 77
    facter2.returncode = 55
    os_path_isfile_mock.return_value = True
    if not (os.environ.has_key("PYTHON")):
      os.environ['PYTHON'] = "test/python/path"
    sys.argv[0] = "test data"
    AmbariAgent.main()

    self.assertTrue(subprocess_popen_mock.called)
    self.assertTrue(subprocess_popen_mock.call_count == 2)
    self.assertTrue(facter1.communicate.called)
    self.assertTrue(facter2.communicate.called)
    self.assertTrue(os_path_isfile_mock.called)
    self.assertTrue(os_path_isfile_mock.call_count == 2)
    self.assertTrue(os_remove_mock.called)
