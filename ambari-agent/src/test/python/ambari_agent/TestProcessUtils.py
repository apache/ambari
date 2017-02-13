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

from ambari_agent import main

main.MEMORY_LEAK_DEBUG_FILEPATH = "/tmp/memory_leak_debug.out"
import unittest
import signal
import subprocess, time
from mock.mock import patch, MagicMock, PropertyMock, call
from ambari_commons import process_utils

process_tree = {"111": "222\n 22",
                "222": "333\n 33",
                "22": "44\n 444",}


class TestProcessUtils(unittest.TestCase):
  @patch("subprocess.Popen")
  def test_kill(self, popen_mock):
    process_mock = MagicMock()
    process_mock.communicate.return_value = (None, None)
    returncode_mock = PropertyMock()
    returncode_mock.return_value = 0
    type(process_mock).returncode = returncode_mock
    popen_mock.return_value = process_mock
    process_utils.kill_pids(["12321113230", "2312415453"], signal.SIGTERM)
    expected = [call(['kill', '-15', '12321113230', '2312415453'], stderr=-1, stdout=-1)]
    self.assertEquals(popen_mock.call_args_list, expected)

  @patch("subprocess.Popen")
  def test_get_children(self, popen_mock):

    process_mock = MagicMock()
    process_mock.communicate.return_value = ("123 \n \n 321\n", None)
    popen_mock.return_value = process_mock
    returncode_mock = PropertyMock()
    returncode_mock.return_value = 0
    type(process_mock).returncode = returncode_mock
    result = process_utils.get_children("2312415453")

    self.assertEquals(result, ["123", "321"])

    expected = [
      call(['ps', '-o', 'pid', '--no-headers', '--ppid', '2312415453'], stderr=subprocess.PIPE, stdout=subprocess.PIPE)]
    self.assertEquals(popen_mock.call_args_list, expected)

  @patch("subprocess.Popen")
  def test_get_flat_process_tree(self, popen_mock):
    def side_effect(*args, **kwargs):
      process_mock = MagicMock()
      returncode_mock = PropertyMock()
      returncode_mock.return_value = 0
      type(process_mock).returncode = returncode_mock
      if args[0][5] in process_tree.keys():
        process_mock.communicate.return_value = (process_tree[args[0][5]], None)
      else:
        process_mock.communicate.return_value = ("", None)
      return process_mock

    popen_mock.side_effect = side_effect
    result = process_utils.get_flat_process_tree("111")
    self.assertEquals(result, ['111', '222', '333', '33', '22', '44', '444'])

    expected = [call(['ps', '-o', 'pid', '--no-headers', '--ppid', '111'], stderr=-1, stdout=-1),
                call(['ps', '-o', 'pid', '--no-headers', '--ppid', '222'], stderr=-1, stdout=-1),
                call(['ps', '-o', 'pid', '--no-headers', '--ppid', '333'], stderr=-1, stdout=-1),
                call(['ps', '-o', 'pid', '--no-headers', '--ppid', '33'], stderr=-1, stdout=-1),
                call(['ps', '-o', 'pid', '--no-headers', '--ppid', '22'], stderr=-1, stdout=-1),
                call(['ps', '-o', 'pid', '--no-headers', '--ppid', '44'], stderr=-1, stdout=-1),
                call(['ps', '-o', 'pid', '--no-headers', '--ppid', '444'], stderr=-1, stdout=-1)]
    self.assertEquals(popen_mock.call_args_list, expected)

  @patch("subprocess.Popen")
  def test_get_command_by_pid(self, popen_mock):

    process_mock = MagicMock()
    process_mock.communicate.return_value = ("yum something", None)
    returncode_mock = PropertyMock()
    returncode_mock.return_value = 0
    type(process_mock).returncode = returncode_mock
    popen_mock.return_value = process_mock

    result = process_utils.get_command_by_pid("2312415453")

    self.assertEquals(result, "yum something")

    expected = [call(['ps', '-p', '2312415453', '-o', 'command', '--no-headers'], stderr=-1, stdout=-1)]
    self.assertEquals(popen_mock.call_args_list, expected)

  @patch("subprocess.Popen")
  def test_get_command_by_pid_not_exist(self, popen_mock):

    process_mock = MagicMock()
    process_mock.communicate.return_value = ("", None)
    returncode_mock = PropertyMock()
    returncode_mock.return_value = 1
    type(process_mock).returncode = returncode_mock
    popen_mock.return_value = process_mock

    result = process_utils.get_command_by_pid("2312415453")

    self.assertEquals(result, "NOT_FOUND[2312415453]")

    expected = [call(['ps', '-p', '2312415453', '-o', 'command', '--no-headers'], stderr=-1, stdout=-1)]
    self.assertEquals(popen_mock.call_args_list, expected)

  @patch("subprocess.Popen")
  def test_is_process_running(self, popen_mock):

    process_mock = MagicMock()
    process_mock.communicate.return_value = ("2312415453", None)
    returncode_mock = PropertyMock()
    returncode_mock.return_value = 0
    type(process_mock).returncode = returncode_mock
    popen_mock.return_value = process_mock

    result = process_utils.is_process_running("2312415453")

    self.assertEquals(result, True)

    expected = [call(['ps', '-p', '2312415453', '-o', 'pid', '--no-headers'], stderr=-1, stdout=-1)]
    self.assertEquals(popen_mock.call_args_list, expected)

  @patch("subprocess.Popen")
  def test_is_process_not_running(self, popen_mock):

    process_mock = MagicMock()
    process_mock.communicate.return_value = ("", None)
    returncode_mock = PropertyMock()
    returncode_mock.return_value = 1
    type(process_mock).returncode = returncode_mock
    popen_mock.return_value = process_mock

    result = process_utils.is_process_running("2312415453")

    self.assertEquals(result, False)

    expected = [call(['ps', '-p', '2312415453', '-o', 'pid', '--no-headers'], stderr=-1, stdout=-1)]
    self.assertEquals(popen_mock.call_args_list, expected)

  @patch("subprocess.Popen")
  def test_get_processes_running(self, popen_mock):
    def side_effect(*args, **kwargs):
      process_mock = MagicMock()
      returncode_mock = PropertyMock()
      if args[0][2] == "4321":
        returncode_mock.return_value = 0
        process_mock.communicate.return_value = ("4321", None)
      else:
        returncode_mock.return_value = 1
        process_mock.communicate.return_value = (None, None)
      type(process_mock).returncode = returncode_mock
      return process_mock

    popen_mock.side_effect = side_effect

    result = process_utils.get_processes_running(["1234", "4321"])

    self.assertEquals(result, ["4321"])

    expected = [call(['ps', '-p', '1234', '-o', 'pid', '--no-headers'], stderr=-1, stdout=-1),
                call(['ps', '-p', '4321', '-o', 'pid', '--no-headers'], stderr=-1, stdout=-1)]
    self.assertEquals(popen_mock.call_args_list, expected)

  @patch("time.sleep")
  @patch("subprocess.Popen")
  def test_wait_for_process_death(self, popen_mock, sleep_mock):

    process_mock = MagicMock()
    process_mock.communicate.side_effect = [("4321", None),("4321", None),(None, None)]
    returncode_mock = PropertyMock()
    returncode_mock.side_effect = [0, 0, 1]
    type(process_mock).returncode = returncode_mock
    popen_mock.return_value = process_mock

    process_utils.wait_for_process_death("4321")

    expected = [call(['ps', '-p', '4321', '-o', 'pid', '--no-headers'], stderr=-1, stdout=-1),
                call(['ps', '-p', '4321', '-o', 'pid', '--no-headers'], stderr=-1, stdout=-1),
                call(['ps', '-p', '4321', '-o', 'pid', '--no-headers'], stderr=-1, stdout=-1)]
    self.assertEquals(popen_mock.call_args_list, expected)
    expected = [call(0.1), call(0.1)]
    self.assertEquals(sleep_mock.call_args_list, expected)

  @patch("time.sleep")
  @patch("subprocess.Popen")
  def test_wait_for_entire_process_tree_death(self, popen_mock, sleep_mock):

    process_mock = MagicMock()
    process_mock.communicate.side_effect = [("1234", None), (None, None), ("4321", None), ("4321", None), (None, None)]
    returncode_mock = PropertyMock()
    returncode_mock.side_effect = [0, 1, 0, 0, 1]
    type(process_mock).returncode = returncode_mock
    popen_mock.return_value = process_mock

    process_utils.wait_for_entire_process_tree_death(["1234", "4321"])

    expected = [call(['ps', '-p', '1234', '-o', 'pid', '--no-headers'], stderr=-1, stdout=-1),
                call(['ps', '-p', '1234', '-o', 'pid', '--no-headers'], stderr=-1, stdout=-1),
                call(['ps', '-p', '4321', '-o', 'pid', '--no-headers'], stderr=-1, stdout=-1),
                call(['ps', '-p', '4321', '-o', 'pid', '--no-headers'], stderr=-1, stdout=-1),
                call(['ps', '-p', '4321', '-o', 'pid', '--no-headers'], stderr=-1, stdout=-1)]
    self.assertEquals(popen_mock.call_args_list, expected)
    expected = [call(0.1), call(0.1), call(0.1)]
    self.assertEquals(sleep_mock.call_args_list, expected)
