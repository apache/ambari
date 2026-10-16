#!/usr/bin/env python3
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

from enum import Enum, IntEnum
import os
import signal
from types import SimpleNamespace
from unittest import TestCase
from unittest.mock import Mock, patch

from resource_management.core import signal_utils


class LegacySignals(IntEnum):
  # Python before 3.11 formats IntEnum as its qualified name, not its value.
  SIGKILL = signal.SIGKILL.value
  __str__ = Enum.__str__


class TestSignalUtils(TestCase):
  @patch("resource_management.core.shell.checked_call")
  def test_tree_timeout_passes_numeric_signal(self, checked_call):
    proc = Mock(pid=12345)
    proc.poll.return_value = None

    signal_utils.terminate_process(
      proc, signal_utils.TerminateStrategy.KILL_PROCESS_TREE
    )

    script = os.path.join(
      os.path.dirname(signal_utils.__file__), "files", "killtree.sh"
    )
    checked_call.assert_called_once_with(["bash", script, "12345", "9"])

  @patch("resource_management.core.shell.checked_call")
  def test_tree_timeout_with_legacy_signal_enum(self, checked_call):
    proc = Mock(pid=12345)
    proc.poll.return_value = None
    with patch.object(
      signal_utils, "signal", SimpleNamespace(SIGKILL=LegacySignals.SIGKILL)
    ):
      signal_utils.terminate_process(
        proc, signal_utils.TerminateStrategy.KILL_PROCESS_TREE
      )

    script = os.path.join(
      os.path.dirname(signal_utils.__file__), "files", "killtree.sh"
    )
    checked_call.assert_called_once_with(["bash", script, "12345", "9"])

  @patch("resource_management.core.shell.checked_call")
  def test_tree_timeout_skips_exited_process(self, checked_call):
    proc = Mock(pid=12345)
    proc.poll.return_value = 0

    signal_utils.terminate_process(
      proc, signal_utils.TerminateStrategy.KILL_PROCESS_TREE
    )

    checked_call.assert_not_called()
