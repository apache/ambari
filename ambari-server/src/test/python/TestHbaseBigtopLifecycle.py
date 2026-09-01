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
import sys
from types import ModuleType, SimpleNamespace
import unittest
from unittest.mock import patch

from resource_management.core.exceptions import ComponentIsNotRunning, Fail


HBASE = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/HBASE"
)
SCRIPTS = HBASE / "package/scripts"


def load_module(module_name, path):
  spec = importlib.util.spec_from_file_location(module_name, path)
  module = importlib.util.module_from_spec(spec)
  spec.loader.exec_module(module)
  return module


def params_module(**values):
  module = ModuleType("params")
  for name, value in values.items():
    setattr(module, name, value)
  return module


HBASE_SERVICE = load_module("bigtop_hbase_service", SCRIPTS / "hbase_service.py")


class TestHbaseProcessLifecycle(unittest.TestCase):
  def setUp(self):
    self.identity = SimpleNamespace(pid=4217)
    self.pid_file = "/var/run/hbase/hbase-hbase-regionserver.pid"

  def _params(self):
    return params_module(
      pid_dir="/var/run/hbase",
      hbase_user="hbase",
      user_group="hadoop",
      daemon_script="/usr/lib/hbase/bin/hbase-daemon.sh",
      hbase_conf_dir="/etc/hbase/conf",
      java64_home="/usr/lib/jvm/java-17-openjdk-amd64",
      hbase_regionserver_shutdown_timeout=30,
      log_dir="/var/log/hbase",
    )

  def test_roles_use_exact_upstream_jvm_main_classes(self):
    self.assertEqual(
      ("org.apache.hadoop.hbase.master.HMaster",),
      HBASE_SERVICE.expected_process_tokens("master"),
    )
    self.assertEqual(
      ("org.apache.hadoop.hbase.regionserver.HRegionServer",),
      HBASE_SERVICE.expected_process_tokens("regionserver"),
    )
    self.assertEqual(
      ("org.apache.hadoop.hbase.thrift.ThriftServer",),
      HBASE_SERVICE.expected_process_tokens("thrift"),
    )
    with self.assertRaises(Fail):
      HBASE_SERVICE.expected_process_tokens("queryserver")

  def test_valid_pid_requires_owner_tokens_and_pinned_identity(self):
    with patch.object(
      HBASE_SERVICE.safe_process, "read_pid", return_value=4217
    ), patch.object(
      HBASE_SERVICE.safe_process,
      "inspect_process",
      return_value=self.identity,
    ) as inspect_process, patch.object(
      HBASE_SERVICE.safe_process, "is_process_running", return_value=True
    ) as is_running, patch.object(
      HBASE_SERVICE.safe_process, "discover_running_process"
    ) as discover:
      result = HBASE_SERVICE.read_or_discover_hbase_process(
        self.pid_file, "hbase", "hadoop", "regionserver"
      )

    self.assertIs(self.identity, result)
    tokens = HBASE_SERVICE.HBASE_PROCESS_TOKENS["regionserver"]
    inspect_process.assert_called_once_with(4217, "hbase", tokens)
    is_running.assert_called_once_with(
      4217, "hbase", tokens, identity=self.identity
    )
    discover.assert_not_called()

  def test_stale_pid_is_removed_before_unique_atomic_recovery(self):
    recovered = SimpleNamespace(pid=4220)
    with patch.object(
      HBASE_SERVICE.safe_process, "read_pid", return_value=4217
    ), patch.object(
      HBASE_SERVICE.safe_process, "inspect_process", return_value=None
    ), patch.object(
      HBASE_SERVICE.safe_process, "remove_pid_file_if_stopped"
    ) as remove_pid, patch.object(
      HBASE_SERVICE.safe_process,
      "discover_running_process",
      return_value=recovered,
    ) as discover, patch.object(
      HBASE_SERVICE.safe_process,
      "create_pid_file_for_identity",
      return_value=recovered,
    ) as create_pid:
      result = HBASE_SERVICE.read_or_discover_hbase_process(
        self.pid_file, "hbase", "hadoop", "regionserver"
      )

    self.assertIs(recovered, result)
    tokens = HBASE_SERVICE.HBASE_PROCESS_TOKENS["regionserver"]
    remove_pid.assert_called_once_with(
      self.pid_file,
      4217,
      expected_user="hbase",
      expected_cmdline=tokens,
    )
    discover.assert_called_once_with("hbase", tokens)
    create_pid.assert_called_once_with(
      self.pid_file,
      recovered,
      "hbase",
      tokens,
      owner="hbase",
      group="hadoop",
      mode=0o640,
    )

  def test_pid_identity_mismatch_fails_closed(self):
    mismatch = Fail("process owner does not match")
    with patch.object(
      HBASE_SERVICE.safe_process, "read_pid", return_value=4217
    ), patch.object(
      HBASE_SERVICE.safe_process, "inspect_process", side_effect=mismatch
    ), patch.object(
      HBASE_SERVICE.safe_process, "remove_pid_file_if_stopped"
    ) as remove_pid, patch.object(
      HBASE_SERVICE.safe_process, "discover_running_process"
    ) as discover:
      with self.assertRaises(Fail) as raised:
        HBASE_SERVICE.read_or_discover_hbase_process(
          self.pid_file, "hbase", "hadoop", "regionserver"
        )

    self.assertIs(mismatch, raised.exception)
    remove_pid.assert_not_called()
    discover.assert_not_called()

  def test_status_without_pid_or_unique_process_reports_stopped(self):
    with patch.object(
      HBASE_SERVICE, "read_or_discover_hbase_process", return_value=None
    ):
      with self.assertRaises(ComponentIsNotRunning):
        HBASE_SERVICE.check_hbase_process_status(
          self.pid_file, "hbase", "hadoop", "regionserver"
        )

  def test_start_is_idempotent_for_existing_valid_process(self):
    params = self._params()
    with patch.dict(sys.modules, {"params": params}), patch.object(
      HBASE_SERVICE,
      "read_or_discover_hbase_process",
      return_value=self.identity,
    ), patch.object(HBASE_SERVICE, "Execute") as execute, patch.object(
      HBASE_SERVICE, "wait_for_hbase_process"
    ) as wait:
      HBASE_SERVICE.hbase_service("regionserver", action="start")

    execute.assert_not_called()
    wait.assert_not_called()

  def test_start_uses_structured_command_and_validates_new_identity(self):
    params = self._params()
    with patch.dict(sys.modules, {"params": params}), patch.object(
      HBASE_SERVICE, "read_or_discover_hbase_process", return_value=None
    ), patch.object(HBASE_SERVICE, "Execute") as execute, patch.object(
      HBASE_SERVICE, "wait_for_hbase_process", return_value=self.identity
    ) as wait:
      HBASE_SERVICE.hbase_service(
        "thrift", action="start", extra_args=("-p", "9090")
      )

    execute.assert_called_once_with(
      (
        "/usr/lib/hbase/bin/hbase-daemon.sh",
        "--config",
        "/etc/hbase/conf",
        "start",
        "thrift",
        "-p",
        "9090",
      ),
      user="hbase",
      environment={"JAVA_HOME": "/usr/lib/jvm/java-17-openjdk-amd64"},
      logoutput=True,
      timeout=60,
    )
    wait.assert_called_once_with(
      "/var/run/hbase/hbase-hbase-thrift.pid", "hbase", "hadoop", "thrift"
    )

  def test_stop_terminates_only_pinned_identity_and_removes_safe_pid(self):
    params = self._params()
    tokens = HBASE_SERVICE.HBASE_PROCESS_TOKENS["regionserver"]
    with patch.dict(sys.modules, {"params": params}), patch.object(
      HBASE_SERVICE,
      "read_or_discover_hbase_process",
      return_value=self.identity,
    ), patch.object(
      HBASE_SERVICE.safe_process, "terminate_process"
    ) as terminate, patch.object(
      HBASE_SERVICE.safe_process, "remove_pid_file_if_stopped"
    ) as remove_pid:
      HBASE_SERVICE.hbase_service("regionserver", action="stop")

    terminate.assert_called_once_with(
      self.identity,
      "hbase",
      tokens,
      term_wait_attempts=30,
      term_wait_sleep=1,
      kill_wait_attempts=10,
      kill_wait_sleep=1,
    )
    remove_pid.assert_called_once_with(
      self.pid_file,
      4217,
      expected_user="hbase",
      expected_cmdline=tokens,
    )

  def test_invalid_action_or_argv_token_is_rejected_before_execution(self):
    params = self._params()
    with patch.dict(sys.modules, {"params": params}), patch.object(
      HBASE_SERVICE, "Execute"
    ) as execute:
      with self.assertRaises(Fail):
        HBASE_SERVICE.hbase_service("master", action="restart")
      with self.assertRaises(Fail):
        HBASE_SERVICE.hbase_service(
          "thrift", action="start", extra_args=("-p", "")
        )

    execute.assert_not_called()


if __name__ == "__main__":
  unittest.main()
