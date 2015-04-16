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
import tempfile
import unittest
from mock.mock import patch, MagicMock
from ambari_agent import ProcessHelper

from only_for_platform import not_for_platform, PLATFORM_WINDOWS

@not_for_platform(PLATFORM_WINDOWS)
class TestProcessHelper(unittest.TestCase):

  @patch.object(ProcessHelper, "getTempFiles")
  def test_clean(self, getTempFilesMock):

    tf1 = tempfile.NamedTemporaryFile(delete=False)
    tf2 = tempfile.NamedTemporaryFile(delete=False)
    tf3 = tempfile.NamedTemporaryFile(delete=False)

    getTempFilesMock.return_value = [tf2.name, tf3.name]
    ProcessHelper.pidfile = tf1.name
    ProcessHelper.logger = MagicMock()

    ProcessHelper._clean()

    self.assertFalse(os.path.exists(tf1.name))
    self.assertFalse(os.path.exists(tf2.name))
    self.assertFalse(os.path.exists(tf3.name))


  @patch("sys.exit")
  @patch.object(ProcessHelper, "_clean")
  def test_stopAgent(self, _clean_mock, sys_exit_mock):

    ProcessHelper.stopAgent()
    self.assertTrue(_clean_mock.called)
    self.assertTrue(sys_exit_mock.called)


  @patch("os.execvp")
  @patch.object(ProcessHelper, "_clean")
  def test_restartAgent(self, _clean_mock, execMock):

    ProcessHelper.logger = MagicMock()
    ProcessHelper.restartAgent()

    self.assertTrue(_clean_mock.called)
    self.assertTrue(execMock.called)
    self.assertEqual(2, len(execMock.call_args_list[0]))

