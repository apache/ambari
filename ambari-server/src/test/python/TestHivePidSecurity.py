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
import signal
import sys
from types import ModuleType, SimpleNamespace
import unittest
from unittest.mock import call, patch

from resource_management.core.exceptions import Fail


HIVE_SCRIPTS = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/HIVE/package/scripts"
)


def load_script(module_name, filename):
  spec = importlib.util.spec_from_file_location(module_name, HIVE_SCRIPTS / filename)
  module = importlib.util.module_from_spec(spec)
  sys.modules[module_name] = module
  spec.loader.exec_module(module)
  return module


HIVE_PID_UTILS = load_script("hive_pid_utils", "hive_pid_utils.py")
HIVE_SERVICE = load_script("bigtop_hive_service", "hive_service.py")
WEBHCAT_SERVICE = load_script("bigtop_webhcat_service", "webhcat_service.py")


def params_module(**values):
  module = ModuleType("params")
  for name, value in values.items():
    setattr(module, name, value)
  return module


class HiveSecurityTestCase(unittest.TestCase):
  def setUp(self):
    super().setUp()
    info_patcher = patch.object(HIVE_PID_UTILS.Logger, "info")
    warning_patcher = patch.object(HIVE_PID_UTILS.Logger, "warning")
    path_exists_patcher = patch.object(
      HIVE_PID_UTILS.sudo, "path_exists", return_value=True
    )
    path_isfile_patcher = patch.object(
      HIVE_PID_UTILS.sudo, "path_isfile", return_value=True
    )
    info_patcher.start()
    warning_patcher.start()
    path_exists_patcher.start()
    path_isfile_patcher.start()
    self.addCleanup(info_patcher.stop)
    self.addCleanup(warning_patcher.stop)
    self.addCleanup(path_exists_patcher.stop)
    self.addCleanup(path_isfile_patcher.stop)


class TestHivePidUtils(HiveSecurityTestCase):
  def test_read_pid_accepts_one_ascii_positive_integer(self):
    with patch.object(HIVE_PID_UTILS.sudo, "read_file", return_value=b" 123\n"):
      self.assertEqual(123, HIVE_PID_UTILS.read_pid("/tmp/hive.pid"))

  def test_read_pid_rejects_unsafe_and_non_positive_values(self):
    invalid_values = (
      "",
      " \n\t",
      "0",
      "000",
      "-1",
      "+1",
      "1 2",
      "1\n2",
      "1;touch /tmp/injected",
      "1$(touch /tmp/injected)",
      "\u0661",
    )

    for value in invalid_values:
      with self.subTest(value=value):
        with patch.object(HIVE_PID_UTILS.sudo, "read_file", return_value=value):
          self.assertIsNone(HIVE_PID_UTILS.read_pid("/tmp/hive.pid"))

  def test_read_pid_handles_missing_and_unreadable_files(self):
    with (
      patch.object(HIVE_PID_UTILS.sudo, "path_exists", return_value=False),
      patch.object(HIVE_PID_UTILS.sudo, "read_file") as read_file,
    ):
      self.assertIsNone(HIVE_PID_UTILS.read_pid("/tmp/missing.pid"))
      read_file.assert_not_called()

    with patch.object(
      HIVE_PID_UTILS.sudo, "read_file", side_effect=OSError("denied")
    ):
      self.assertIsNone(HIVE_PID_UTILS.read_pid("/tmp/unreadable.pid"))

  def test_read_pid_strict_mode_fails_for_an_existing_invalid_file(self):
    with patch.object(
      HIVE_PID_UTILS.sudo,
      "read_file",
      return_value=b"1;touch /tmp/injected",
    ):
      with self.assertRaisesRegex(Fail, "does not contain a valid process id"):
        HIVE_PID_UTILS.read_pid("/tmp/hive.pid", fail_on_invalid=True)

  def test_read_pid_strict_mode_rejects_an_existing_non_file(self):
    with (
      patch.object(HIVE_PID_UTILS.sudo, "path_isfile", return_value=False),
      patch.object(HIVE_PID_UTILS.sudo, "read_file") as read_file,
    ):
      with self.assertRaisesRegex(Fail, "is not a regular file"):
        HIVE_PID_UTILS.read_pid("/run/hive.pid", fail_on_invalid=True)

    read_file.assert_not_called()

  def test_process_owner_must_match_expected_service_user(self):
    with (
      patch.object(
        HIVE_PID_UTILS.pwd,
        "getpwnam",
        return_value=SimpleNamespace(pw_uid=1001),
      ),
      patch.object(
        HIVE_PID_UTILS.sudo,
        "stat",
        return_value=SimpleNamespace(st_uid=1002),
      ),
      patch.object(HIVE_PID_UTILS.sudo, "kill") as sudo_kill,
    ):
      self.assertFalse(HIVE_PID_UTILS.is_process_running(123, "hive"))

    sudo_kill.assert_not_called()

  def test_process_owner_mismatch_fails_before_stop_signal(self):
    with (
      patch.object(
        HIVE_PID_UTILS.pwd,
        "getpwnam",
        return_value=SimpleNamespace(pw_uid=1001),
      ),
      patch.object(
        HIVE_PID_UTILS.sudo,
        "stat",
        return_value=SimpleNamespace(st_uid=1002),
      ),
      patch.object(HIVE_PID_UTILS.sudo, "kill") as sudo_kill,
    ):
      with self.assertRaisesRegex(Fail, "process owner does not match hive"):
        HIVE_PID_UTILS.force_stop_process(123, "hive")

    sudo_kill.assert_not_called()

  def test_matching_process_owner_allows_stop_signal(self):
    with (
      patch.object(
        HIVE_PID_UTILS.pwd,
        "getpwnam",
        return_value=SimpleNamespace(pw_uid=1001),
      ),
      patch.object(
        HIVE_PID_UTILS.sudo,
        "stat",
        return_value=SimpleNamespace(st_uid=1001),
      ),
      patch.object(
        HIVE_PID_UTILS.sudo,
        "kill",
        side_effect=(None, None, ProcessLookupError(3, "stopped")),
      ) as sudo_kill,
    ):
      HIVE_PID_UTILS.force_stop_process(123, "hive", wait_attempts=1)

    self.assertEqual(
      [call(123, 0), call(123, signal.SIGKILL), call(123, 0)],
      sudo_kill.call_args_list,
    )

  def test_missing_process_never_reaches_kill(self):
    with (
      patch.object(HIVE_PID_UTILS.sudo, "path_exists", return_value=False),
      patch.object(HIVE_PID_UTILS.sudo, "stat") as sudo_stat,
      patch.object(HIVE_PID_UTILS.sudo, "kill") as sudo_kill,
    ):
      self.assertFalse(HIVE_PID_UTILS.is_process_running(123, "hive"))

    sudo_stat.assert_not_called()
    sudo_kill.assert_not_called()

  def test_process_inspection_failure_fails_before_stop_signal(self):
    with (
      patch.object(HIVE_PID_UTILS.sudo, "stat", side_effect=OSError("denied")),
      patch.object(HIVE_PID_UTILS.sudo, "kill") as sudo_kill,
    ):
      with self.assertRaisesRegex(Fail, "Unable to inspect owner of pid 123"):
        HIVE_PID_UTILS.force_stop_process(123, "hive")

    sudo_kill.assert_not_called()

  def test_process_probe_permission_failure_is_not_treated_as_stopped(self):
    with (
      patch.object(HIVE_PID_UTILS, "_is_process_owned_by_user", return_value=True),
      patch.object(
        HIVE_PID_UTILS.sudo,
        "kill",
        side_effect=PermissionError(1, "denied"),
      ) as sudo_kill,
    ):
      with self.assertRaisesRegex(Fail, "Unable to inspect running state"):
        HIVE_PID_UTILS.force_stop_process(123, "hive")

    sudo_kill.assert_called_once_with(123, 0)

  def test_missing_service_user_fails_before_process_access(self):
    with (
      patch.object(HIVE_PID_UTILS.sudo, "stat") as sudo_stat,
      patch.object(HIVE_PID_UTILS.sudo, "kill") as sudo_kill,
    ):
      with self.assertRaisesRegex(Fail, "service user is empty"):
        HIVE_PID_UTILS.force_stop_process(123, "")

    sudo_stat.assert_not_called()
    sudo_kill.assert_not_called()

  def test_terminate_process_stops_after_sigterm(self):
    with (
      patch.object(
        HIVE_PID_UTILS.sudo,
        "kill",
        side_effect=(None, None, ProcessLookupError(3, "stopped")),
      ) as sudo_kill,
      patch.object(HIVE_PID_UTILS, "_is_process_owned_by_user", return_value=True),
      patch.object(HIVE_PID_UTILS.time, "sleep") as sleep,
    ):
      HIVE_PID_UTILS.terminate_process(123, "hive", 2, 5)

    self.assertEqual(
      [call(123, 0), call(123, signal.SIGTERM), call(123, 0)],
      sudo_kill.call_args_list,
    )
    sleep.assert_not_called()

  def test_terminate_process_uses_sigkill_after_grace_period(self):
    with (
      patch.object(
        HIVE_PID_UTILS.sudo,
        "kill",
        side_effect=(
          None,
          None,
          None,
          None,
          None,
          None,
          ProcessLookupError(3, "stopped"),
        ),
      ) as sudo_kill,
      patch.object(HIVE_PID_UTILS, "_is_process_owned_by_user", return_value=True),
      patch.object(HIVE_PID_UTILS.time, "sleep") as sleep,
    ):
      HIVE_PID_UTILS.terminate_process(
        123,
        "hive",
        graceful_wait_attempts=2,
        graceful_wait_sleep=5,
        force_wait_attempts=1,
      )

    self.assertIn(call(123, signal.SIGTERM), sudo_kill.call_args_list)
    self.assertIn(call(123, signal.SIGKILL), sudo_kill.call_args_list)
    sleep.assert_called_once_with(5)

  def test_force_stop_raises_when_process_remains_running(self):
    with (
      patch.object(HIVE_PID_UTILS.sudo, "kill", return_value=None) as sudo_kill,
      patch.object(HIVE_PID_UTILS, "_is_process_owned_by_user", return_value=True),
      patch.object(HIVE_PID_UTILS.time, "sleep") as sleep,
    ):
      with self.assertRaisesRegex(Fail, "Process with pid 123 did not stop"):
        HIVE_PID_UTILS.force_stop_process(123, "hive", wait_attempts=1)

    self.assertIn(call(123, signal.SIGKILL), sudo_kill.call_args_list)
    sleep.assert_not_called()


class TestHiveServicePidSecurity(HiveSecurityTestCase):
  def setUp(self):
    super().setUp()
    self.params = params_module(
      hive_log_dir="/var/log/hive",
      hive_user="hive",
      security_enabled=False,
    )
    self.status_params = params_module(
      hive_metastore_pid="/run/hive/metastore.pid",
      hive_pid="/run/hive/hiveserver2.pid",
    )

  def test_stop_uses_service_specific_grace_periods(self):
    expected = (
      ("metastore", 2, 5),
      ("hiveserver2", 11, 3),
    )
    with patch.dict(
      sys.modules, {"params": self.params, "status_params": self.status_params}
    ):
      for name, attempts, sleep_seconds in expected:
        with self.subTest(name=name):
          with (
            patch.object(HIVE_SERVICE, "format", return_value="daemon"),
            patch.object(HIVE_SERVICE, "read_pid", return_value=123),
            patch.object(HIVE_SERVICE, "terminate_process") as terminate,
            patch.object(HIVE_SERVICE, "File"),
          ):
            HIVE_SERVICE.hive_service(name, action="stop")

          terminate.assert_called_once_with(
            123,
            "hive",
            graceful_wait_attempts=attempts,
            graceful_wait_sleep=sleep_seconds,
          )

  def test_invalid_pid_never_reaches_a_signal(self):
    invalid_values = ("", "0", "-1", "1;touch /tmp/injected", "1\n2")
    with patch.dict(
      sys.modules, {"params": self.params, "status_params": self.status_params}
    ):
      for value in invalid_values:
        with self.subTest(value=value):
          with (
            patch.object(HIVE_SERVICE, "format", return_value="daemon"),
            patch.object(HIVE_SERVICE, "File") as file_resource,
            patch.object(HIVE_SERVICE, "show_logs"),
            patch.object(HIVE_PID_UTILS.sudo, "read_file", return_value=value),
            patch.object(HIVE_PID_UTILS.sudo, "kill") as sudo_kill,
          ):
            with self.assertRaises(Fail):
              HIVE_SERVICE.hive_service("metastore", action="stop")

          sudo_kill.assert_not_called()
          file_resource.assert_not_called()

  def test_missing_pid_file_keeps_stop_idempotent(self):
    with (
      patch.dict(
        sys.modules, {"params": self.params, "status_params": self.status_params}
      ),
      patch.object(HIVE_SERVICE, "format", return_value="daemon"),
      patch.object(HIVE_SERVICE, "File") as file_resource,
      patch.object(HIVE_PID_UTILS.sudo, "path_exists", return_value=False),
      patch.object(HIVE_PID_UTILS.sudo, "kill") as sudo_kill,
    ):
      HIVE_SERVICE.hive_service("metastore", action="stop")

    sudo_kill.assert_not_called()
    file_resource.assert_called_once_with(
      "/run/hive/metastore.pid", action="delete"
    )

  def test_stop_logs_and_propagates_failure(self):
    with (
      patch.dict(
        sys.modules, {"params": self.params, "status_params": self.status_params}
      ),
      patch.object(HIVE_SERVICE, "format", return_value="daemon"),
      patch.object(HIVE_SERVICE, "read_pid", return_value=123),
      patch.object(HIVE_SERVICE, "terminate_process", side_effect=Fail("stuck")),
      patch.object(HIVE_SERVICE, "show_logs") as show_logs,
    ):
      with self.assertRaisesRegex(Fail, "stuck"):
        HIVE_SERVICE.hive_service("metastore", action="stop")

    show_logs.assert_called_once_with("/var/log/hive", "hive")

  def test_start_condition_uses_validated_pid_check(self):
    self.params.hive_jdbc_driver = "none"
    self.params.hadoop_home = "/usr/lib/hadoop"
    self.params.java64_home = "/usr/lib/jvm/java"
    self.params.execute_path = "/usr/bin"
    with (
      patch.dict(
        sys.modules, {"params": self.params, "status_params": self.status_params}
      ),
      patch.object(HIVE_SERVICE, "format", return_value="daemon"),
      patch.object(HIVE_SERVICE, "Execute") as execute,
      patch.object(
        HIVE_SERVICE, "is_pid_file_process_running", return_value=True
      ) as is_running,
    ):
      HIVE_SERVICE.hive_service("metastore", action="start")
      condition = execute.call_args.kwargs["not_if"]
      self.assertTrue(condition())

    is_running.assert_called_once_with("/run/hive/metastore.pid", "hive")

  def test_start_condition_fails_for_an_existing_invalid_pid(self):
    self.params.hive_jdbc_driver = "none"
    self.params.hadoop_home = "/usr/lib/hadoop"
    self.params.java64_home = "/usr/lib/jvm/java"
    self.params.execute_path = "/usr/bin"
    with (
      patch.dict(
        sys.modules, {"params": self.params, "status_params": self.status_params}
      ),
      patch.object(HIVE_SERVICE, "format", return_value="daemon"),
      patch.object(HIVE_SERVICE, "Execute") as execute,
      patch.object(
        HIVE_PID_UTILS.sudo,
        "read_file",
        return_value=b"1;touch /tmp/not-executed",
      ),
      patch.object(HIVE_PID_UTILS.sudo, "kill") as sudo_kill,
    ):
      HIVE_SERVICE.hive_service("metastore", action="start")
      condition = execute.call_args.kwargs["not_if"]
      with self.assertRaises(Fail):
        condition()

    sudo_kill.assert_not_called()


class TestHiveDbConnectionSecurity(HiveSecurityTestCase):
  def setUp(self):
    super().setUp()
    self.params = params_module(
      ambari_java_home="/ambari/java",
      check_db_connection_jar="/agent/DBConnectionVerification.jar",
      hive_jdbc_connection_url="jdbc:test:'; touch /tmp/not-executed",
      hive_jdbc_driver="example.Driver",
      hive_log_dir="/var/log/hive",
      hive_metastore_user_name="user name; echo not-executed",
      hive_metastore_user_passwd="secret; never-in-argv",
      hive_user="hive",
      jdbc_jar_name="driver.jar",
    )

  def test_validate_connection_uses_helper_without_a_shell_command(self):
    rendered_values = {
      "{check_db_connection_jar}": "/agent/DBConnectionVerification.jar",
      "{ambari_java_home}/bin/java": "/ambari/java/bin/java",
    }
    with (
      patch.dict(sys.modules, {"params": self.params}),
      patch.object(
        HIVE_SERVICE, "format", side_effect=lambda value: rendered_values[value]
      ),
      patch.object(HIVE_SERVICE, "verify_db_connection") as verify,
      patch.object(HIVE_SERVICE, "Execute") as execute,
    ):
      HIVE_SERVICE.validate_connection(
        "/drivers/*; touch /tmp/not-executed", "/usr/lib/hive/lib"
      )

    verify.assert_called_once_with(
      "/ambari/java/bin/java",
      "/agent/DBConnectionVerification.jar:/drivers/*; touch /tmp/not-executed",
      "jdbc:test:'; touch /tmp/not-executed",
      "user name; echo not-executed",
      "secret; never-in-argv",
      "example.Driver",
      tries=5,
      try_sleep=10,
    )
    execute.assert_not_called()

  def test_validate_connection_logs_and_propagates_helper_failure(self):
    rendered_values = {
      "{check_db_connection_jar}": "/agent/DBConnectionVerification.jar",
      "{ambari_java_home}/bin/java": "/ambari/java/bin/java",
    }
    with (
      patch.dict(sys.modules, {"params": self.params}),
      patch.object(
        HIVE_SERVICE, "format", side_effect=lambda value: rendered_values[value]
      ),
      patch.object(
        HIVE_SERVICE, "verify_db_connection", side_effect=Fail("connection failed")
      ),
      patch.object(HIVE_SERVICE, "show_logs") as show_logs,
    ):
      with self.assertRaisesRegex(Fail, "connection failed"):
        HIVE_SERVICE.validate_connection("/drivers/jdbc.jar", "/usr/lib/hive/lib")

    show_logs.assert_called_once_with("/var/log/hive", "hive")


class TestWebHCatServicePidSecurity(HiveSecurityTestCase):
  def setUp(self):
    super().setUp()
    self.params = params_module(
      hcat_log_dir="/var/log/webhcat",
      hive_home="/usr/lib/hive",
      webhcat_bin_dir="/usr/lib/hive-hcatalog/sbin",
      webhcat_pid_file="/run/webhcat/webhcat.pid",
      webhcat_user="hcat",
    )

  def test_stop_force_kills_only_a_valid_pid(self):
    with (
      patch.dict(sys.modules, {"params": self.params}),
      patch.object(WEBHCAT_SERVICE, "format", return_value="webhcat"),
      patch.object(WEBHCAT_SERVICE, "graceful_stop"),
      patch.object(WEBHCAT_SERVICE, "read_pid", return_value=123),
      patch.object(WEBHCAT_SERVICE, "force_stop_process") as force_stop,
      patch.object(WEBHCAT_SERVICE, "File"),
    ):
      WEBHCAT_SERVICE.webhcat_service(action="stop")

    force_stop.assert_called_once_with(123, "hcat")

  def test_invalid_pid_never_reaches_a_signal(self):
    invalid_values = ("", "0", "-1", "1;touch /tmp/injected", "1\n2")
    with patch.dict(sys.modules, {"params": self.params}):
      for value in invalid_values:
        with self.subTest(value=value):
          with (
            patch.object(WEBHCAT_SERVICE, "format", return_value="webhcat"),
            patch.object(WEBHCAT_SERVICE, "graceful_stop"),
            patch.object(WEBHCAT_SERVICE, "File") as file_resource,
            patch.object(WEBHCAT_SERVICE, "show_logs"),
            patch.object(HIVE_PID_UTILS.sudo, "read_file", return_value=value),
            patch.object(HIVE_PID_UTILS.sudo, "kill") as sudo_kill,
          ):
            with self.assertRaises(Fail):
              WEBHCAT_SERVICE.webhcat_service(action="stop")

          sudo_kill.assert_not_called()
          file_resource.assert_not_called()

  def test_missing_pid_file_keeps_stop_idempotent(self):
    with (
      patch.dict(sys.modules, {"params": self.params}),
      patch.object(WEBHCAT_SERVICE, "format", return_value="webhcat"),
      patch.object(WEBHCAT_SERVICE, "graceful_stop"),
      patch.object(WEBHCAT_SERVICE, "File") as file_resource,
      patch.object(HIVE_PID_UTILS.sudo, "path_exists", return_value=False),
      patch.object(HIVE_PID_UTILS.sudo, "kill") as sudo_kill,
    ):
      WEBHCAT_SERVICE.webhcat_service(action="stop")

    sudo_kill.assert_not_called()
    file_resource.assert_called_once_with(
      "/run/webhcat/webhcat.pid", action="delete"
    )

  def test_stop_logs_and_propagates_force_stop_failure(self):
    with (
      patch.dict(sys.modules, {"params": self.params}),
      patch.object(WEBHCAT_SERVICE, "format", return_value="webhcat"),
      patch.object(WEBHCAT_SERVICE, "graceful_stop"),
      patch.object(WEBHCAT_SERVICE, "read_pid", return_value=123),
      patch.object(
        WEBHCAT_SERVICE, "force_stop_process", side_effect=Fail("stuck")
      ),
      patch.object(WEBHCAT_SERVICE, "show_logs") as show_logs,
    ):
      with self.assertRaisesRegex(Fail, "stuck"):
        WEBHCAT_SERVICE.webhcat_service(action="stop")

    show_logs.assert_called_once_with("/var/log/webhcat", "hcat")

  def test_start_condition_uses_validated_pid_check(self):
    with (
      patch.dict(sys.modules, {"params": self.params}),
      patch.object(WEBHCAT_SERVICE, "format", return_value="webhcat"),
      patch.object(WEBHCAT_SERVICE, "Execute") as execute,
      patch.object(
        WEBHCAT_SERVICE, "is_pid_file_process_running", return_value=True
      ) as is_running,
    ):
      WEBHCAT_SERVICE.webhcat_service(action="start")
      condition = execute.call_args.kwargs["not_if"]
      self.assertTrue(condition())

    is_running.assert_called_once_with("/run/webhcat/webhcat.pid", "hcat")

  def test_start_condition_fails_for_a_different_process_owner(self):
    with (
      patch.dict(sys.modules, {"params": self.params}),
      patch.object(WEBHCAT_SERVICE, "format", return_value="webhcat"),
      patch.object(WEBHCAT_SERVICE, "Execute") as execute,
      patch.object(HIVE_PID_UTILS.sudo, "read_file", return_value=b"123"),
      patch.object(
        HIVE_PID_UTILS.sudo,
        "stat",
        return_value=SimpleNamespace(st_uid=1002),
      ),
      patch.object(
        HIVE_PID_UTILS.pwd,
        "getpwnam",
        return_value=SimpleNamespace(pw_uid=1001),
      ),
      patch.object(HIVE_PID_UTILS.sudo, "kill") as sudo_kill,
    ):
      WEBHCAT_SERVICE.webhcat_service(action="start")
      condition = execute.call_args.kwargs["not_if"]
      with self.assertRaisesRegex(Fail, "process owner does not match hcat"):
        condition()

    sudo_kill.assert_not_called()


if __name__ == "__main__":
  unittest.main()
