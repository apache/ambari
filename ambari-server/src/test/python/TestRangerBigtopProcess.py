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
from types import SimpleNamespace
import unittest
from unittest.mock import patch

from resource_management.core.exceptions import Fail


RANGER = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.3.0/services/RANGER"
)


def load_module(name, path):
  spec = importlib.util.spec_from_file_location(name, path)
  module = importlib.util.module_from_spec(spec)
  spec.loader.exec_module(module)
  return module


class TestRangerBigtopProcess(unittest.TestCase):
  def test_lifecycle_uses_exact_process_identity(self):
    path = RANGER / "package/scripts/ranger_process.py"
    source = path.read_text()
    for token in (
      "-Dproc_rangeradmin",
      "-Dproc_rangerusersync",
      "-Dproc_rangertagsync",
    ):
      self.assertIn(token, source)
    self.assertIn("safe_process.terminate_process", source)
    self.assertIn("safe_process.publish_pid_file_for_identity", source)
    self.assertIn("PID directory ownership or permissions are unsafe", source)

    lifecycle_sources = "\n".join(
      path.read_text() for path in (RANGER / "package/scripts").glob("*.py")
    )
    for obsolete in ("ps -ef", "grep proc_ranger", "check_process_status"):
      self.assertNotIn(obsolete, lifecycle_sources)
    self.assertIn("rollback_started_process", lifecycle_sources)

  def test_lifecycle_reuses_only_an_exact_pid_identity(self):
    module = load_module(
      "ranger_process_existing_test",
      RANGER / "package/scripts/ranger_process.py",
    )
    identity = SimpleNamespace(pid=4217)
    with patch.object(module, "_validate_pid_file"), \
      patch.object(module, "_validate_pid_directory"), \
      patch.object(module, "_validate_pid_directory"), \
      patch.object(
        module.safe_process,
        "read_running_process",
        return_value=identity,
      ) as read_running_process, \
      patch.object(
        module.safe_process,
        "publish_pid_file_for_identity",
        return_value=identity,
      ) as publish_pid, \
      patch.object(module.safe_process, "discover_running_process") as discover:
      self.assertIs(
        identity,
        module.find_process(
          "ranger_admin",
          "/run/ranger/rangeradmin.pid",
          "ranger",
          "ranger",
        ),
      )

    read_running_process.assert_called_once_with(
      "/run/ranger/rangeradmin.pid",
      "ranger",
      ("-Dproc_rangeradmin",),
    )
    publish_pid.assert_called_once_with(
      "/run/ranger/rangeradmin.pid",
      identity,
      "ranger",
      ("-Dproc_rangeradmin",),
      "ranger",
      "ranger",
    )
    discover.assert_not_called()

  def test_lifecycle_rejects_a_launcher_pid_mismatch(self):
    module = load_module(
      "ranger_process_launcher_test",
      RANGER / "package/scripts/ranger_process.py",
    )
    identity = SimpleNamespace(pid=4217)
    with patch.object(module, "_validate_pid_file"), \
      patch.object(module, "_validate_pid_directory"), \
      patch.object(
        module.safe_process,
        "wait_for_discovered_process",
        return_value=identity,
      ), \
      patch.object(
        module.safe_process,
        "publish_pid_file_for_identity",
        side_effect=Fail("PID file identifies process 4218, expected 4217"),
      ) as publish_pid, \
      patch.object(module, "rollback_started_process") as rollback, \
      self.assertRaisesRegex(Fail, "identifies process"):
      module.secure_started_process(
        "ranger_admin",
        "/run/ranger/rangeradmin.pid",
        "ranger",
        "ranger",
      )
    publish_pid.assert_called_once_with(
      "/run/ranger/rangeradmin.pid",
      identity,
      "ranger",
      ("-Dproc_rangeradmin",),
      "ranger",
      "ranger",
    )
    rollback.assert_called_once_with(
      "ranger_admin",
      "/run/ranger/rangeradmin.pid",
      identity,
      "ranger",
    )

  def test_start_timeout_terminates_the_process_group(self):
    source = (RANGER / "package/scripts/ranger_service.py").read_text()
    self.assertIn("timeout=60", source)
    self.assertIn(
      "timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP", source
    )

  def test_start_rollback_uses_only_the_pinned_identity(self):
    module = load_module(
      "ranger_process_rollback_test",
      RANGER / "package/scripts/ranger_process.py",
    )
    identity = SimpleNamespace(pid=4217)
    with patch.object(module, "_validate_pid_file"), \
      patch.object(module, "_validate_pid_directory"), \
      patch.object(module.safe_process, "terminate_process") as terminate, \
      patch.object(module.safe_process, "read_pid", return_value=4217), \
      patch.object(module.safe_process, "remove_pid_file_if_stopped") as remove, \
      patch.object(module.safe_process, "discover_running_process") as discover:
      module.rollback_started_process(
        "ranger_admin",
        "/run/ranger/rangeradmin.pid",
        identity,
        "ranger",
      )

    discover.assert_not_called()
    terminate.assert_called_once_with(identity, "ranger", ("-Dproc_rangeradmin",))
    remove.assert_called_once_with(
      "/run/ranger/rangeradmin.pid",
      4217,
      "ranger",
      ("-Dproc_rangeradmin",),
    )

  def test_lifecycle_stops_and_removes_only_the_matching_pid(self):
    module = load_module(
      "ranger_process_stop_test",
      RANGER / "package/scripts/ranger_process.py",
    )
    identity = SimpleNamespace(pid=4217)
    with patch.object(module, "find_process", return_value=identity), \
      patch.object(module.safe_process, "terminate_process") as terminate, \
      patch.object(module.safe_process, "read_pid", return_value=4217), \
      patch.object(module.safe_process, "remove_pid_file_if_stopped") as remove:
      module.stop_process(
        "ranger_usersync",
        "/run/ranger/usersync.pid",
        "ranger",
        "ranger",
      )

    terminate.assert_called_once_with(
      identity,
      "ranger",
      ("-Dproc_rangerusersync",),
    )
    remove.assert_called_once_with(
      "/run/ranger/usersync.pid",
      4217,
      "ranger",
      ("-Dproc_rangerusersync",),
    )


if __name__ == "__main__":
  unittest.main()
