#!/usr/bin/env python2.6
# -*- coding: utf-8 -*-

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

import os
import unittest
import tempfile
from mock.mock import patch, MagicMock, call
from ambari_agent.AmbariConfig import AmbariConfig
from ambari_agent import shell
from shell import shellRunner



class TestShell(unittest.TestCase):

  @patch("os.killpg")
  @patch("time.sleep")
  def test_kill_stale_process(self, timeSleepMock, os_killPgMock):
    temp_path = AmbariConfig().getConfig().get("stack", "installprefix") + '/9999.pid'
    file = open(temp_path, 'w')
    file.close()

    shell.killstaleprocesses()
    self.assertFalse(os.path.exists(temp_path))


  @patch("os.setuid")
  def test_changeUid(self, os_setUIDMock):
    shell.threadLocal.uid = 9999
    shell.changeUid()
    self.assertTrue(os_setUIDMock.called)


  @patch("pwd.getpwnam")
  def test_shellRunner_run(self, getpwnamMock):
    sh = shellRunner()
    result = sh.run(['echo'])
    self.assertEquals(result['exitCode'], 0)
    self.assertEquals(result['error'], '')

    getpwnamMock.return_value = [os.getuid(), os.getuid(), os.getuid()]
    result = sh.run(['echo'], 'non_exist_user_name')
    self.assertEquals(result['exitCode'], 0)
    self.assertEquals(result['error'], '')
