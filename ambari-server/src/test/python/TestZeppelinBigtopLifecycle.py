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

import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch

from resource_management.core.exceptions import Fail
from resource_management.libraries.functions import safe_process


SCRIPTS = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/ZEPPELIN/package/scripts"
)
PID_FILE = "/var/run/zeppelin/zeppelin-zeppelin.pid"
TOKENS = ("org.apache.zeppelin.server.ZeppelinServer",)
IDENTITY = safe_process.ProcessIdentity(
  123,
  1001,
  456,
  ("/usr/bin/java", *TOKENS),
)


def load_module(name, path):
  spec = importlib.util.spec_from_file_location(name, path)
  module = importlib.util.module_from_spec(spec)
  spec.loader.exec_module(module)
  return module


ZEPPELIN_PROCESS = load_module(
  "bigtop_zeppelin_process", SCRIPTS / "zeppelin_process.py"
)


class TestZeppelinProcessLifecycle(unittest.TestCase):
  def test_pid_publication_failure_rolls_back_only_waited_identity(self):
    with patch.object(safe_process, "wait_for_discovered_process", return_value=IDENTITY), \
      patch.object(
        ZEPPELIN_PROCESS,
        "_publish_zeppelin_process",
        side_effect=Fail("publish failed"),
      ), \
      patch.object(
        ZEPPELIN_PROCESS, "rollback_started_zeppelin_process"
      ) as rollback, \
      self.assertRaisesRegex(Fail, "publish failed"):
      ZEPPELIN_PROCESS.wait_for_zeppelin_process(
        PID_FILE, "zeppelin", "hadoop", attempts=5, sleep_seconds=2
      )
    rollback.assert_called_once_with(PID_FILE, IDENTITY, "zeppelin")

  def test_rollback_failure_preserves_pid_publication_error(self):
    with patch.object(safe_process, "wait_for_discovered_process", return_value=IDENTITY), \
      patch.object(
        ZEPPELIN_PROCESS,
        "_publish_zeppelin_process",
        side_effect=Fail("publish failed"),
      ), \
      patch.object(
        ZEPPELIN_PROCESS,
        "rollback_started_zeppelin_process",
        side_effect=Fail("rollback failed"),
      ), \
      self.assertRaisesRegex(Fail, "publish failed"):
      ZEPPELIN_PROCESS.wait_for_zeppelin_process(
        PID_FILE, "zeppelin", "hadoop"
      )

  def test_rollback_terminates_and_removes_only_pinned_identity(self):
    with patch.object(safe_process, "discover_running_process") as discover, \
      patch.object(safe_process, "terminate_process") as terminate, \
      patch.object(safe_process, "remove_pid_file_if_stopped") as remove:
      ZEPPELIN_PROCESS.rollback_started_zeppelin_process(
        PID_FILE, IDENTITY, "zeppelin"
      )
    discover.assert_not_called()
    terminate.assert_called_once_with(IDENTITY, "zeppelin", TOKENS)
    remove.assert_called_once_with(
      PID_FILE,
      123,
      expected_user="zeppelin",
      expected_cmdline=TOKENS,
    )


if __name__ == "__main__":
  unittest.main()
