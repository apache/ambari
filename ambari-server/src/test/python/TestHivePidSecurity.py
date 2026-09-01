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


SCRIPTS = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/HIVE/package/scripts"
)


def load_script(module_name, filename):
  spec = importlib.util.spec_from_file_location(module_name, SCRIPTS / filename)
  module = importlib.util.module_from_spec(spec)
  sys.modules[module_name] = module
  spec.loader.exec_module(module)
  return module


HIVE_SERVICE = load_script("hive_service", "hive_service.py")
WEBHCAT_SERVICE = load_script("bigtop_webhcat_service", "webhcat_service.py")


def module_with(**values):
  module = ModuleType("test_params")
  for name, value in values.items():
    setattr(module, name, value)
  return module


def identity(pid=123):
  return SimpleNamespace(pid=pid)


class TestHiveProcessIdentity(unittest.TestCase):
  def test_roles_use_exact_upstream_java_class_tokens(self):
    self.assertEqual(
      ("org.apache.hive.service.server.HiveServer2",),
      HIVE_SERVICE.expected_process_tokens("hiveserver2"),
    )
    self.assertEqual(
      ("org.apache.hadoop.hive.metastore.HiveMetaStore",),
      HIVE_SERVICE.expected_process_tokens("metastore"),
    )
    self.assertEqual(
      ("org.apache.hive.hcatalog.templeton.Main",),
      HIVE_SERVICE.expected_process_tokens("webhcat"),
    )
    with self.assertRaisesRegex(Fail, "Unsupported Hive service role"):
      HIVE_SERVICE.expected_process_tokens("interactive")

  def test_valid_pid_identity_is_reused_without_discovery(self):
    running = identity()
    with (
      patch.object(HIVE_SERVICE.safe_process, "read_pid", return_value=123),
      patch.object(HIVE_SERVICE.safe_process, "inspect_process", return_value=running),
      patch.object(HIVE_SERVICE.safe_process, "is_process_running", return_value=True),
      patch.object(HIVE_SERVICE.safe_process, "discover_running_process") as discover,
    ):
      result = HIVE_SERVICE.read_or_discover_hive_process(
        "/run/hive/server.pid", "hive", "hadoop", "hiveserver2"
      )
    self.assertIs(running, result)
    discover.assert_not_called()

  def test_stale_pid_is_removed_before_unique_discovery(self):
    with (
      patch.object(HIVE_SERVICE.safe_process, "read_pid", return_value=123),
      patch.object(HIVE_SERVICE.safe_process, "inspect_process", return_value=None),
      patch.object(HIVE_SERVICE.safe_process, "remove_pid_file_if_stopped") as remove,
      patch.object(HIVE_SERVICE.safe_process, "discover_running_process", return_value=None),
    ):
      result = HIVE_SERVICE.read_or_discover_hive_process(
        "/run/hive/metastore.pid", "hive", "hadoop", "metastore"
      )
    self.assertIsNone(result)
    remove.assert_called_once_with(
      "/run/hive/metastore.pid",
      123,
      expected_user="hive",
      expected_cmdline=("org.apache.hadoop.hive.metastore.HiveMetaStore",),
    )

  def test_missing_pid_is_recovered_atomically_with_restricted_mode(self):
    running = identity()
    with (
      patch.object(HIVE_SERVICE.safe_process, "read_pid", return_value=None),
      patch.object(
        HIVE_SERVICE.safe_process,
        "discover_running_process",
        return_value=running,
      ),
      patch.object(
        HIVE_SERVICE.safe_process,
        "create_pid_file_for_identity",
        return_value=running,
      ) as create,
    ):
      result = HIVE_SERVICE.read_or_discover_hive_process(
        "/run/hive/server.pid", "hive", "hadoop", "hiveserver2"
      )
    self.assertIs(running, result)
    create.assert_called_once_with(
      "/run/hive/server.pid",
      running,
      "hive",
      ("org.apache.hive.service.server.HiveServer2",),
      owner="hive",
      group="hadoop",
      mode=0o640,
    )

  def test_missing_status_raises_component_not_running(self):
    with patch.object(
      HIVE_SERVICE, "read_or_discover_hive_process", return_value=None
    ):
      with self.assertRaises(ComponentIsNotRunning):
        HIVE_SERVICE.check_hive_process_status(
          "/run/hive/server.pid", "hive", "hadoop", "hiveserver2"
        )


class TestHiveLifecycle(unittest.TestCase):
  def setUp(self):
    self.params = module_with(
      hive_user="hive",
      user_group="hadoop",
      hive_log_dir="/var/log/hive",
      hive_jdbc_driver="unsupported",
      hive_jdbc_drivers_list=(),
      start_metastore_path="/tmp/start-metastore",
      start_hiveserver2_path="/tmp/start-hiveserver2",
      hive_conf_dir="/etc/hive/conf",
      tez_conf_dir="/etc/tez/conf",
      hadoop_home="/usr/lib/hadoop",
      java64_home="/usr/lib/jvm/java-17",
      hive_bin_dir="/usr/lib/hive/bin",
      execute_path="/usr/bin",
    )
    self.status = module_with(
      hive_metastore_pid="/run/hive/metastore.pid",
      hive_pid="/run/hive/server.pid",
    )

  def test_start_is_idempotent_for_validated_identity(self):
    running = identity()
    with (
      patch.dict(sys.modules, {"params": self.params, "status_params": self.status}),
      patch.object(
        HIVE_SERVICE,
        "read_or_discover_hive_process",
        return_value=running,
      ),
      patch.object(HIVE_SERVICE, "Execute") as execute,
    ):
      HIVE_SERVICE.hive_service("metastore", "start")
    execute.assert_not_called()

  def test_start_uses_structured_wrapper_and_fixes_pid_metadata(self):
    running = identity()
    with (
      patch.dict(sys.modules, {"params": self.params, "status_params": self.status}),
      patch.object(HIVE_SERVICE, "read_or_discover_hive_process", return_value=None),
      patch.object(HIVE_SERVICE, "wait_for_hive_process", return_value=running),
      patch.object(HIVE_SERVICE, "Execute") as execute,
      patch.object(HIVE_SERVICE, "File") as file_resource,
    ):
      HIVE_SERVICE.hive_service("metastore", "start")

    command = execute.call_args.args[0]
    self.assertEqual("/tmp/start-metastore", command[0])
    self.assertEqual("/run/hive/metastore.pid", command[3])
    self.assertNotIsInstance(command, str)
    file_resource.assert_called_once_with(
      "/run/hive/metastore.pid",
      owner="hive",
      group="hadoop",
      mode=0o640,
    )

  def test_readiness_failure_terminates_the_started_identity(self):
    running = identity()
    with (
      patch.dict(sys.modules, {"params": self.params, "status_params": self.status}),
      patch.object(HIVE_SERVICE, "read_or_discover_hive_process", return_value=None),
      patch.object(HIVE_SERVICE, "wait_for_hive_process", return_value=running),
      patch.object(HIVE_SERVICE, "_wait_for_secure_znode", side_effect=Fail("not ready")),
      patch.object(HIVE_SERVICE, "Execute"),
      patch.object(HIVE_SERVICE, "File"),
      patch.object(HIVE_SERVICE.safe_process, "terminate_process") as terminate,
      patch.object(HIVE_SERVICE.safe_process, "remove_pid_file_if_stopped") as remove,
      patch.object(HIVE_SERVICE, "show_logs"),
    ):
      with self.assertRaisesRegex(Fail, "not ready"):
        HIVE_SERVICE.hive_service("hiveserver2", "start")

    terminate.assert_called_once_with(
      running,
      "hive",
      ("org.apache.hive.service.server.HiveServer2",),
      term_wait_attempts=10,
      term_wait_sleep=1,
      kill_wait_attempts=10,
      kill_wait_sleep=1,
    )
    remove.assert_called_once()

  def test_stop_pins_identity_through_term_wait_and_kill(self):
    running = identity()
    with (
      patch.dict(sys.modules, {"params": self.params, "status_params": self.status}),
      patch.object(
        HIVE_SERVICE,
        "read_or_discover_hive_process",
        return_value=running,
      ),
      patch.object(HIVE_SERVICE.safe_process, "terminate_process") as terminate,
      patch.object(HIVE_SERVICE.safe_process, "remove_pid_file_if_stopped") as remove,
    ):
      HIVE_SERVICE.hive_service("metastore", "stop")
    terminate.assert_called_once_with(
      running,
      "hive",
      ("org.apache.hadoop.hive.metastore.HiveMetaStore",),
      term_wait_attempts=10,
      term_wait_sleep=1,
      kill_wait_attempts=10,
      kill_wait_sleep=1,
    )
    remove.assert_called_once_with(
      "/run/hive/metastore.pid",
      123,
      expected_user="hive",
      expected_cmdline=("org.apache.hadoop.hive.metastore.HiveMetaStore",),
    )


class TestWebHCatLifecycle(unittest.TestCase):
  def test_failed_start_rolls_back_exact_webhcat_process(self):
    params = module_with(
      webhcat_pid_file="/var/lib/hive-hcatalog/webhcat.pid",
      webhcat_user="hive",
      user_group="hadoop",
      webhcat_bin_dir="/usr/lib/hive-hcatalog/sbin",
      hadoop_home="/usr/lib/hadoop",
      hive_conf_dir="/etc/hive/conf",
      hive_home="/usr/lib/hive",
      java64_home="/usr/lib/jvm/java-17",
      webhcat_conf_dir="/etc/hive-webhcat/conf",
      hcat_pid_dir="/var/lib/hive-hcatalog",
      hcat_log_dir="/var/log/hive-hcatalog",
    )
    running = identity()
    with (
      patch.dict(sys.modules, {"params": params}),
      patch.object(WEBHCAT_SERVICE, "read_or_discover_hive_process", return_value=None),
      patch.object(WEBHCAT_SERVICE, "wait_for_hive_process", return_value=running),
      patch.object(WEBHCAT_SERVICE, "File", side_effect=Fail("metadata failed")),
      patch.object(WEBHCAT_SERVICE, "Execute") as execute,
      patch.object(WEBHCAT_SERVICE.safe_process, "terminate_process") as terminate,
      patch.object(WEBHCAT_SERVICE.safe_process, "remove_pid_file_if_stopped"),
      patch.object(WEBHCAT_SERVICE, "show_logs"),
    ):
      with self.assertRaisesRegex(Fail, "metadata failed"):
        WEBHCAT_SERVICE.webhcat_service("start")

    self.assertEqual(
      ("/usr/lib/hive-hcatalog/sbin/webhcat_server.sh", "start"),
      execute.call_args.args[0],
    )
    terminate.assert_called_once_with(
      running,
      "hive",
      ("org.apache.hive.hcatalog.templeton.Main",),
      term_wait_attempts=10,
      term_wait_sleep=1,
      kill_wait_attempts=10,
      kill_wait_sleep=1,
    )


if __name__ == "__main__":
  unittest.main()
