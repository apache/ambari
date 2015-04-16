#!/usr/bin/env python
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
from ambari_commons import shell
from ambari_commons.shell import shellRunner
from sys import platform as _platform
from only_for_platform import not_for_platform, PLATFORM_WINDOWS
import subprocess, time

@not_for_platform(PLATFORM_WINDOWS)
class TestShell(unittest.TestCase):


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

  def test_kill_process_with_children(self):
    if _platform == "linux" or _platform == "linux2": # Test is Linux-specific
      gracefull_kill_delay_old = shell.gracefull_kill_delay
      shell.gracefull_kill_delay = 0.1
      sleep_cmd = "sleep 314159265"
      test_cmd = """ (({0}) & ({0} & {0})) """.format(sleep_cmd)
      # Starting process tree (multiple process groups)
      test_process = subprocess.Popen(test_cmd, stderr=subprocess.PIPE, stdout=subprocess.PIPE, shell=True)
      time.sleep(0.3) # Delay to allow subprocess to start
      # Check if processes are running
      ps_cmd = """ps aux """
      ps_process = subprocess.Popen(ps_cmd, stderr=subprocess.PIPE, stdout=subprocess.PIPE, shell=True)
      (out, err) = ps_process.communicate()
      self.assertTrue(sleep_cmd in out)
      # Kill test process
      shell.kill_process_with_children(test_process.pid)
      test_process.communicate()
      # Now test process should not be running
      ps_process = subprocess.Popen(ps_cmd, stderr=subprocess.PIPE, stdout=subprocess.PIPE, shell=True)
      (out, err) = ps_process.communicate()
      self.assertFalse(sleep_cmd in out)
      shell.gracefull_kill_delay = gracefull_kill_delay_old
    else:
      # Do not run under other systems
      pass
