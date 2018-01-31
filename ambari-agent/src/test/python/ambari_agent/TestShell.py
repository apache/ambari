#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
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
"""
from contextlib import contextmanager

import unittest
import signal
from mock.mock import patch, MagicMock, call
from ambari_commons import shell
from ambari_commons import OSCheck
from StringIO import StringIO

ROOT_PID = 10
ROOT_PID_CHILDRENS = [10, 11, 12, 13]
shell.logger = MagicMock()  # suppress any log output

__proc_fs = {
  "/proc/10/task/10/children": "11 12",
  "/proc/10/comm": "a",
  "/proc/10/cmdline": "",

  "/proc/11/task/11/children": "13",
  "/proc/11/comm": "b",
  "/proc/11/cmdline": "",

  "/proc/12/task/12/children": "",
  "/proc/12/comm": "c",
  "/proc/12/cmdline": "",

  "/proc/13/task/13/children": "",
  "/proc/13/comm": "d",
  "/proc/13/cmdline": ""
}

__proc_fs_yum = {
  "/proc/10/task/10/children": "11",
  "/proc/10/comm": "a",
  "/proc/10/cmdline": "",

  "/proc/11/task/11/children": "",
  "/proc/11/comm": "yum",
  "/proc/11/cmdline": "yum install something"
}


class FakeSignals(object):
  SIGTERM = signal.SIG_IGN
  SIGKILL = signal.SIG_IGN


@contextmanager
def _open_mock(path, open_mode):
  if path in __proc_fs:
    yield StringIO(__proc_fs[path])
  else:
    yield StringIO("")


@contextmanager
def _open_mock_yum(path, open_mode):
  if path in __proc_fs:
    yield StringIO(__proc_fs_yum[path])
  else:
    yield StringIO("")


class TestShell(unittest.TestCase):

  @patch("__builtin__.open", new=MagicMock(side_effect=_open_mock))
  def test_get_all_children(self):

    pid_list = [item[0] for item in shell.get_all_childrens(ROOT_PID)]

    self.assertEquals(len(ROOT_PID_CHILDRENS), len(pid_list))
    self.assertEquals(ROOT_PID, pid_list[0])

    for i in ROOT_PID_CHILDRENS:
      self.assertEquals(True, i in pid_list)

  @patch("__builtin__.open", new=MagicMock(side_effect=_open_mock))
  @patch.object(OSCheck, "get_os_family", new=MagicMock(return_value="redhat"))
  @patch.object(shell, "signal", new_callable=FakeSignals)
  @patch.object(shell, "is_pid_life")
  @patch("os.kill")
  def test_kill_process_with_children(self, os_kill_mock, is_pid_life_mock, fake_signals):
    pid_list = [item[0] for item in shell.get_all_childrens(ROOT_PID)]
    reverse_pid_list = sorted(pid_list, reverse=True)
    shell.gracefull_kill_delay = 0.1
    is_pid_life_clean_kill = [True] * len(pid_list) + [False] * len(pid_list)
    is_pid_life_not_clean_kill = [True] * (len(pid_list) * 2)

    is_pid_life_mock.side_effect = is_pid_life_clean_kill
    shell.kill_process_with_children(ROOT_PID)

    # test clean pid by SIGTERM
    os_kill_pids = [item[0][0] for item in os_kill_mock.call_args_list]
    self.assertEquals(len(os_kill_pids), len(pid_list))
    self.assertEquals(reverse_pid_list, os_kill_pids)

    os_kill_mock.reset_mock()
    is_pid_life_mock.reset_mock()

    is_pid_life_mock.side_effect = is_pid_life_not_clean_kill
    shell.kill_process_with_children(ROOT_PID)

    # test clean pid by SIGKILL
    os_kill_pids = [item[0][0] for item in os_kill_mock.call_args_list]
    self.assertEquals(len(os_kill_pids), len(pid_list)*2)
    self.assertEquals(reverse_pid_list + reverse_pid_list, os_kill_pids)

  @patch("__builtin__.open", new=MagicMock(side_effect=_open_mock_yum))
  @patch.object(OSCheck, "get_os_family", new=MagicMock(return_value="redhat"))
  @patch.object(shell, "signal", new_callable=FakeSignals)
  @patch.object(shell, "is_pid_life")
  @patch("os.kill")
  def test_kill_process_with_children_except_yum(self, os_kill_mock, is_pid_life_mock, fake_signals):
    shell.gracefull_kill_delay = 0.1
    is_pid_life_clean_kill = [True, False, True, False]  # used here only first pair

    is_pid_life_mock.side_effect = is_pid_life_clean_kill
    shell.kill_process_with_children(ROOT_PID)

    # test clean pid by SIGTERM
    os_kill_pids = [item[0][0] for item in os_kill_mock.call_args_list]
    self.assertEquals(len(os_kill_pids), 1)
    self.assertEquals([10], os_kill_pids)

