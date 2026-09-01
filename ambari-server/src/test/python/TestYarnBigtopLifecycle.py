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
from types import SimpleNamespace
import unittest
from unittest.mock import MagicMock, call, patch

from resource_management.core.exceptions import ComponentIsNotRunning, Fail
from resource_management.core.signal_utils import TerminateStrategy
from resource_management.libraries.functions import safe_process


YARN = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/YARN"
)
SCRIPTS = YARN / "package/scripts"
PID_FILE = "/run/hadoop-yarn/yarn/hadoop-yarn-resourcemanager.pid"
IDENTITY = safe_process.ProcessIdentity(
  123,
  1001,
  456,
  (
    "/usr/bin/java",
    "-Dproc_resourcemanager",
    "org.apache.hadoop.yarn.server.resourcemanager.ResourceManager",
  ),
)


def load_module(module_name, path, dependencies=None):
  spec = importlib.util.spec_from_file_location(module_name, path)
  module = importlib.util.module_from_spec(spec)
  with patch.dict(sys.modules, dependencies or {}):
    spec.loader.exec_module(module)
  return module


YARN_PROCESS_UTILS = load_module(
  "yarn_process_utils", SCRIPTS / "yarn_process_utils.py"
)
YARN_SERVICE = load_module(
  "bigtop_yarn_service",
  SCRIPTS / "service.py",
  {"yarn_process_utils": YARN_PROCESS_UTILS},
)
HBASE_SERVICE = load_module(
  "bigtop_yarn_hbase_service",
  SCRIPTS / "hbase_service.py",
  {"yarn_process_utils": YARN_PROCESS_UTILS},
)


class TestYarnProcessLifecycle(unittest.TestCase):
  def test_process_tokens_match_hadoop_336_entrypoints(self):
    self.assertEqual(
      (
        "-Dproc_resourcemanager",
        "org.apache.hadoop.yarn.server.resourcemanager.ResourceManager",
      ),
      YARN_PROCESS_UTILS.expected_cmdline("resourcemanager"),
    )
    self.assertEqual(
      (
        "-Dproc_registrydns",
        "org.apache.hadoop.registry.server.dns.RegistryDNSServer",
      ),
      YARN_PROCESS_UTILS.expected_cmdline("registrydns"),
    )
    self.assertEqual(
      (
        "-Dproc_registrydns",
        "org.apache.hadoop.registry.server.dns.PrivilegedRegistryDNSStarter",
      ),
      YARN_PROCESS_UTILS.expected_cmdline("registrydns", privileged=True),
    )
    with self.assertRaisesRegex(ValueError, "Unsupported"):
      YARN_PROCESS_UTILS.expected_cmdline("unknown")

  def test_valid_pid_is_idempotent_without_discovery_or_rewrite(self):
    with (
      patch.object(safe_process, "read_pid", return_value=123),
      patch.object(
        YARN_PROCESS_UTILS,
        "read_running_process",
        return_value=IDENTITY,
      ),
      patch.object(safe_process, "discover_running_process") as discover,
      patch.object(safe_process, "create_pid_file_for_identity") as create,
    ):
      result = YARN_PROCESS_UTILS.recover_running_process(
        PID_FILE, "yarn", "resourcemanager", "yarn", "hadoop"
      )

    self.assertIs(IDENTITY, result)
    discover.assert_not_called()
    create.assert_not_called()

  def test_pidless_process_is_discovered_and_atomically_restored(self):
    with (
      patch.object(safe_process, "read_pid", return_value=None),
      patch.object(
        safe_process, "discover_running_process", return_value=IDENTITY
      ) as discover,
      patch.object(
        safe_process,
        "create_pid_file_for_identity",
        return_value=IDENTITY,
      ) as create,
    ):
      result = YARN_PROCESS_UTILS.recover_running_process(
        PID_FILE, "yarn", "resourcemanager", "yarn", "hadoop"
      )

    tokens = YARN_PROCESS_UTILS.expected_cmdline("resourcemanager")
    self.assertIs(IDENTITY, result)
    discover.assert_called_once_with("yarn", tokens)
    create.assert_called_once_with(
      PID_FILE,
      IDENTITY,
      "yarn",
      tokens,
      "yarn",
      "hadoop",
      mode=0o640,
    )

  def test_stale_pid_is_removed_before_discovery(self):
    with (
      patch.object(safe_process, "read_pid", return_value=123),
      patch.object(
        YARN_PROCESS_UTILS, "read_running_process", return_value=None
      ),
      patch.object(
        safe_process, "remove_pid_file_if_stopped", return_value=True
      ) as remove,
      patch.object(
        safe_process, "discover_running_process", return_value=None
      ),
    ):
      self.assertIsNone(
        YARN_PROCESS_UTILS.recover_running_process(
          PID_FILE, "yarn", "resourcemanager", "yarn", "hadoop"
        )
      )

    remove.assert_called_once_with(
      PID_FILE,
      123,
      "yarn",
      YARN_PROCESS_UTILS.expected_cmdline("resourcemanager"),
    )

  def test_ambiguous_discovery_and_identity_mismatch_fail_closed(self):
    for failure in (
      Fail("ambiguous process discovery"),
      Fail("process identity changed while creating PID file"),
    ):
      with self.subTest(failure=failure):
        create_failure = "identity changed" in str(failure)
        with (
          patch.object(safe_process, "read_pid", return_value=None),
          patch.object(
            safe_process,
            "discover_running_process",
            return_value=IDENTITY if create_failure else None,
            side_effect=None if create_failure else failure,
          ),
          patch.object(
            safe_process,
            "create_pid_file_for_identity",
            side_effect=failure if create_failure else None,
          ),
        ):
          with self.assertRaises(Fail):
            YARN_PROCESS_UTILS.recover_running_process(
              PID_FILE, "yarn", "resourcemanager", "yarn", "hadoop"
            )

  def test_stop_terminates_pinned_identity_then_removes_same_pid_file(self):
    with (
      patch.object(
        YARN_PROCESS_UTILS,
        "recover_running_process",
        return_value=IDENTITY,
      ),
      patch.object(safe_process, "terminate_process") as terminate,
      patch.object(safe_process, "remove_pid_file_if_stopped") as remove,
    ):
      self.assertTrue(
        YARN_PROCESS_UTILS.stop_process(
          PID_FILE, "yarn", "resourcemanager", "yarn", "hadoop"
        )
      )

    tokens = YARN_PROCESS_UTILS.expected_cmdline("resourcemanager")
    terminate.assert_called_once_with(IDENTITY, "yarn", tokens)
    remove.assert_called_once_with(PID_FILE, 123, "yarn", tokens)

  def test_missing_status_maps_to_component_not_running(self):
    with patch.object(
      YARN_PROCESS_UTILS, "recover_running_process", return_value=None
    ):
      with self.assertRaises(ComponentIsNotRunning):
        YARN_PROCESS_UTILS.check_component_status(
          PID_FILE, "yarn", "resourcemanager", "yarn", "hadoop"
        )


class TestDaemonLaunchBounds(unittest.TestCase):
  def test_timeline_server_start_never_deletes_leveldb_lock_as_root(self):
    source = (SCRIPTS / "service.py").read_text(encoding="utf-8")
    self.assertNotIn("ats_leveldb_lock_file", source)
    self.assertNotIn('File(', source)

  def test_yarn_daemon_start_has_timeout_and_process_group_cleanup(self):
    params = SimpleNamespace(
      yarn_container_bin="/usr/lib/hadoop-yarn/bin",
      yarn_user="yarn",
      yarn_log_dir="/var/log/hadoop-yarn",
      yarn_pid_dir_prefix="/run/hadoop-yarn",
      yarn_pid_dir="/run/hadoop-yarn/yarn",
      user_group="hadoop",
      hadoop_libexec_dir="/usr/lib/hadoop/libexec",
      hadoop_conf_dir="/etc/hadoop/conf",
    )
    status = SimpleNamespace(
      registry_dns_needs_privileged_access=False,
      root_user="root",
    )
    with (
      patch.dict(sys.modules, {"params": params, "status_params": status}),
      patch.object(YARN_PROCESS_UTILS, "recover_running_process", return_value=None),
      patch.object(YARN_PROCESS_UTILS, "wait_for_running_process"),
      patch.object(YARN_SERVICE, "Execute") as execute,
    ):
      YARN_SERVICE.service("resourcemanager", "start")

    self.assertEqual(300, execute.call_args.kwargs["timeout"])
    self.assertEqual(
      TerminateStrategy.KILL_PROCESS_GROUP,
      execute.call_args.kwargs["timeout_kill_strategy"],
    )

  def test_secure_refresh_queues_uses_private_cache_and_bounded_command(self):
    params = SimpleNamespace(
      yarn_container_bin="/usr/lib/hadoop-yarn/bin",
      yarn_user="yarn",
      yarn_log_dir="/var/log/hadoop-yarn",
      yarn_pid_dir_prefix="/run/hadoop-yarn",
      yarn_pid_dir="/run/hadoop-yarn/yarn",
      user_group="hadoop",
      hadoop_libexec_dir="/usr/lib/hadoop/libexec",
      hadoop_conf_dir="/etc/hadoop/conf",
      security_enabled=True,
      kinit_path_local="/usr/bin/kinit",
      rm_keytab="/etc/security/keytabs/rm.keytab",
      rm_principal_name="rm/host@EXAMPLE.COM",
    )
    status = SimpleNamespace(
      registry_dns_needs_privileged_access=False,
      root_user="root",
    )
    cache = MagicMock()
    cache.merge_environment.return_value = {
      "KRB5CCNAME": "FILE:/run/ambari-agent/tmp/private-cache"
    }
    cache_context = MagicMock()
    cache_context.__enter__.return_value = cache
    with (
      patch.dict(sys.modules, {"params": params, "status_params": status}),
      patch.object(
        YARN_SERVICE,
        "PrivateKerberosCache",
        return_value=cache_context,
      ),
      patch.object(YARN_SERVICE, "Execute") as execute,
    ):
      YARN_SERVICE.service("resourcemanager", "refreshQueues")

    cache.kinit.assert_called_once_with(
      "/usr/bin/kinit",
      "/etc/security/keytabs/rm.keytab",
      "rm/host@EXAMPLE.COM",
    )
    execute.assert_called_once_with(
      (
        "/usr/lib/hadoop-yarn/bin/yarn",
        "--config",
        "/etc/hadoop/conf",
        "rmadmin",
        "-refreshQueues",
      ),
      user="yarn",
      environment={"KRB5CCNAME": "FILE:/run/ambari-agent/tmp/private-cache"},
      timeout=20,
      tries=5,
      try_sleep=5,
      timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
    )

  def test_failed_yarn_start_rolls_back_the_exact_component_identity(self):
    params = SimpleNamespace(
      yarn_container_bin="/usr/lib/hadoop-yarn/bin",
      yarn_user="yarn",
      yarn_log_dir="/var/log/hadoop-yarn/yarn",
      yarn_pid_dir_prefix="/run/hadoop-yarn",
      yarn_pid_dir="/run/hadoop-yarn/yarn",
      user_group="hadoop",
      hadoop_libexec_dir="/usr/lib/hadoop/libexec",
      hadoop_conf_dir="/etc/hadoop/conf",
    )
    status = SimpleNamespace(
      registry_dns_needs_privileged_access=False,
      root_user="root",
    )
    with (
      patch.dict(sys.modules, {"params": params, "status_params": status}),
      patch.object(YARN_PROCESS_UTILS, "recover_running_process", return_value=None),
      patch.object(
        YARN_PROCESS_UTILS,
        "wait_for_running_process",
        side_effect=Fail("invalid startup identity"),
      ),
      patch.object(YARN_PROCESS_UTILS, "stop_process") as stop,
      patch.object(YARN_SERVICE, "Execute"),
      patch.object(YARN_SERVICE, "show_logs"),
    ):
      with self.assertRaisesRegex(Fail, "invalid startup identity"):
        YARN_SERVICE.service("resourcemanager", "start")

    stop.assert_called_once_with(
      "/run/hadoop-yarn/yarn/hadoop-yarn-resourcemanager.pid",
      "yarn",
      "resourcemanager",
      "yarn",
      "hadoop",
      False,
    )

  def test_yarn_start_reports_primary_and_rollback_failures(self):
    params = SimpleNamespace(
      yarn_container_bin="/usr/lib/hadoop-yarn/bin",
      yarn_user="yarn",
      yarn_log_dir="/var/log/hadoop-yarn/yarn",
      yarn_pid_dir_prefix="/run/hadoop-yarn",
      yarn_pid_dir="/run/hadoop-yarn/yarn",
      user_group="hadoop",
      hadoop_libexec_dir="/usr/lib/hadoop/libexec",
      hadoop_conf_dir="/etc/hadoop/conf",
    )
    status = SimpleNamespace(
      registry_dns_needs_privileged_access=False,
      root_user="root",
    )
    with (
      patch.dict(sys.modules, {"params": params, "status_params": status}),
      patch.object(YARN_PROCESS_UTILS, "recover_running_process", return_value=None),
      patch.object(YARN_SERVICE, "Execute", side_effect=Fail("launch failed")),
      patch.object(
        YARN_PROCESS_UTILS,
        "stop_process",
        side_effect=Fail("rollback failed"),
      ),
      patch.object(YARN_SERVICE, "show_logs"),
    ):
      with self.assertRaisesRegex(
        RuntimeError, "launch failed.*rollback failed"
      ) as raised:
        YARN_SERVICE.service("resourcemanager", "start")
    self.assertIsInstance(raised.exception.__cause__, Fail)

  def test_embedded_hbase_start_has_timeout_and_process_group_cleanup(self):
    params = SimpleNamespace(
      yarn_hbase_bin="/usr/lib/hbase/bin",
      yarn_hbase_pid_dir="/run/hbase",
      yarn_hbase_user="yarn-ats",
      yarn_hbase_conf_dir="/etc/hbase/conf",
      yarn_hbase_log_dir="/var/log/hbase",
      user_group="hadoop",
    )
    with (
      patch.dict(sys.modules, {"params": params}),
      patch.object(YARN_PROCESS_UTILS, "recover_running_process", return_value=None),
      patch.object(YARN_PROCESS_UTILS, "wait_for_running_process"),
      patch.object(HBASE_SERVICE, "Execute") as execute,
    ):
      HBASE_SERVICE.hbase_service("master", "start")

    self.assertEqual(300, execute.call_args.kwargs["timeout"])
    self.assertEqual(
      TerminateStrategy.KILL_PROCESS_GROUP,
      execute.call_args.kwargs["timeout_kill_strategy"],
    )

  def test_embedded_hbase_start_rolls_back_before_failed_log_collection(self):
    params = SimpleNamespace(
      yarn_hbase_bin="/usr/lib/hbase/bin",
      yarn_hbase_pid_dir="/run/hbase",
      yarn_hbase_user="yarn-ats",
      yarn_hbase_conf_dir="/etc/hbase/conf",
      yarn_hbase_log_dir="/var/log/hbase",
      user_group="hadoop",
    )
    events = []

    def fail_log_collection(*_):
      events.append("logs")
      raise Fail("logs failed")

    with (
      patch.dict(sys.modules, {"params": params}),
      patch.object(YARN_PROCESS_UTILS, "recover_running_process", return_value=None),
      patch.object(HBASE_SERVICE, "Execute", side_effect=Fail("launch failed")),
      patch.object(
        YARN_PROCESS_UTILS,
        "stop_process",
        side_effect=lambda *args: events.append("rollback"),
      ),
      patch.object(
        HBASE_SERVICE,
        "show_logs",
        side_effect=fail_log_collection,
      ),
    ):
      with self.assertRaisesRegex(RuntimeError, "launch failed.*logs failed"):
        HBASE_SERVICE.hbase_service("master", "start")
    self.assertEqual(["rollback", "logs"], events)

  def test_embedded_hbase_start_reports_rollback_and_log_failures(self):
    params = SimpleNamespace(
      yarn_hbase_bin="/usr/lib/hbase/bin",
      yarn_hbase_pid_dir="/run/hbase",
      yarn_hbase_user="yarn-ats",
      yarn_hbase_conf_dir="/etc/hbase/conf",
      yarn_hbase_log_dir="/var/log/hbase",
      user_group="hadoop",
    )
    with (
      patch.dict(sys.modules, {"params": params}),
      patch.object(YARN_PROCESS_UTILS, "recover_running_process", return_value=None),
      patch.object(HBASE_SERVICE, "Execute", side_effect=Fail("launch failed")),
      patch.object(
        YARN_PROCESS_UTILS, "stop_process", side_effect=Fail("rollback failed")
      ),
      patch.object(HBASE_SERVICE, "show_logs", side_effect=Fail("logs failed")),
    ):
      with self.assertRaisesRegex(
        RuntimeError, "launch failed.*rollback failed.*logs failed"
      ):
        HBASE_SERVICE.hbase_service("master", "start")

  def test_embedded_hbase_stop_attempts_both_roles(self):
    with patch.object(
      HBASE_SERVICE,
      "hbase_service",
      side_effect=[Fail("regionserver failed"), None],
    ) as role_service:
      with self.assertRaisesRegex(RuntimeError, "regionserver failed"):
        HBASE_SERVICE.hbase("stop")
    self.assertEqual(
      [
        call("regionserver", action="stop"),
        call("master", action="stop"),
      ],
      role_service.call_args_list,
    )

  def test_regionserver_failure_rolls_back_new_master(self):
    with (
      patch.object(
        HBASE_SERVICE,
        "hbase_service",
        side_effect=[True, Fail("regionserver failed"), None],
      ) as role_service,
      patch.object(HBASE_SERVICE, "createTables"),
    ):
      with self.assertRaisesRegex(Fail, "regionserver failed"):
        HBASE_SERVICE.hbase("start")
    self.assertEqual(
      [
        call("master", action="start"),
        call("regionserver", action="start"),
        call("master", action="stop"),
      ],
      role_service.call_args_list,
    )

  def test_schema_failure_rolls_back_new_roles_in_reverse_order(self):
    with (
      patch.object(
        HBASE_SERVICE,
        "hbase_service",
        side_effect=[True, True, None, None],
      ) as role_service,
      patch.object(HBASE_SERVICE, "createTables", side_effect=Fail("schema failed")),
    ):
      with self.assertRaisesRegex(Fail, "schema failed"):
        HBASE_SERVICE.hbase("start")
    self.assertEqual(
      [
        call("master", action="start"),
        call("regionserver", action="start"),
        call("regionserver", action="stop"),
        call("master", action="stop"),
      ],
      role_service.call_args_list,
    )

  def test_preexisting_master_is_not_stopped_when_regionserver_fails(self):
    with (
      patch.object(
        HBASE_SERVICE,
        "hbase_service",
        side_effect=[False, Fail("regionserver failed")],
      ) as role_service,
      patch.object(HBASE_SERVICE, "createTables"),
    ):
      with self.assertRaisesRegex(Fail, "regionserver failed"):
        HBASE_SERVICE.hbase("start")
    self.assertEqual(
      [call("master", action="start"), call("regionserver", action="start")],
      role_service.call_args_list,
    )


class TestRegistryDnsContract(unittest.TestCase):
  def setUp(self):
    self.params = SimpleNamespace(
      yarn_container_bin="/usr/lib/hadoop-yarn/bin",
      yarn_user="yarn",
      yarn_log_dir="/var/log/hadoop-yarn/yarn",
      yarn_pid_dir_prefix="/run/hadoop-yarn",
      yarn_pid_dir="/run/hadoop-yarn/yarn",
      user_group="hadoop",
      hadoop_libexec_dir="/usr/lib/hadoop/libexec",
      hadoop_conf_dir="/etc/hadoop/conf",
    )
    self.status = SimpleNamespace(
      registry_dns_needs_privileged_access=True,
      root_user="root",
      yarn_registry_dns_pid_file=(
        "/run/hadoop-yarn/yarn/hadoop-yarn-registrydns.pid"
      ),
      yarn_registry_dns_secure_pid_file=(
        "/run/hadoop-yarn/root/hadoop-yarn-root-registrydns.pid"
      ),
      yarn_registry_dns_wrapper_pid_file=(
        "/run/hadoop-yarn/root/privileged-root-registrydns.pid"
      ),
    )

  def test_secure_spec_uses_official_daemon_pid_and_root_launcher(self):
    spec = YARN_SERVICE._process_spec(
      "registrydns", "yarn", self.params, self.status
    )
    self.assertEqual("root", spec["command_user"])
    self.assertEqual("yarn", spec["expected_user"])
    self.assertEqual("root", spec["pid_owner"])
    self.assertTrue(spec["privileged"])
    self.assertEqual(
      self.status.yarn_registry_dns_secure_pid_file, spec["pid_file"]
    )
    self.assertEqual("/run/hadoop-yarn/root", spec["pid_dir"])

  def test_mode_transition_stops_normal_secure_and_wrapper_identities(self):
    with patch.object(YARN_PROCESS_UTILS, "stop_process") as stop:
      YARN_SERVICE._stop_registry_dns_processes(self.params, self.status)

    self.assertEqual(
      [
        call(
          self.status.yarn_registry_dns_pid_file,
          "yarn",
          "registrydns",
          "yarn",
          "hadoop",
          False,
        ),
        call(
          self.status.yarn_registry_dns_secure_pid_file,
          "yarn",
          "registrydns",
          "root",
          "hadoop",
          True,
        ),
        call(
          self.status.yarn_registry_dns_wrapper_pid_file,
          "root",
          "registrydns",
          "root",
          "hadoop",
          True,
        ),
      ],
      stop.call_args_list,
    )

  def test_current_mode_pid_is_preserved_during_transition_cleanup(self):
    with patch.object(YARN_PROCESS_UTILS, "stop_process") as stop:
      YARN_SERVICE._stop_registry_dns_processes(
        self.params,
        self.status,
        keep_pid_file=self.status.yarn_registry_dns_secure_pid_file,
      )

    stopped_pid_files = [entry.args[0] for entry in stop.call_args_list]
    self.assertNotIn(
      self.status.yarn_registry_dns_secure_pid_file, stopped_pid_files
    )
    self.assertIn(self.status.yarn_registry_dns_pid_file, stopped_pid_files)
    self.assertIn(
      self.status.yarn_registry_dns_wrapper_pid_file, stopped_pid_files
    )

  def test_failed_secure_start_cleans_normal_secure_and_wrapper_identities(self):
    with (
      patch.dict(
        sys.modules,
        {"params": self.params, "status_params": self.status},
      ),
      patch.object(YARN_PROCESS_UTILS, "recover_running_process", return_value=None),
      patch.object(YARN_PROCESS_UTILS, "stop_process") as stop,
      patch.object(YARN_SERVICE, "Execute"),
      patch.object(
        YARN_PROCESS_UTILS,
        "wait_for_running_process",
        side_effect=Fail("secure Registry DNS did not start"),
      ),
      patch.object(YARN_SERVICE, "show_logs"),
    ):
      with self.assertRaisesRegex(Fail, "secure Registry DNS did not start"):
        YARN_SERVICE.service("registrydns", "start")

    rollback_pid_files = [entry.args[0] for entry in stop.call_args_list[-3:]]
    self.assertEqual(
      [
        self.status.yarn_registry_dns_pid_file,
        self.status.yarn_registry_dns_secure_pid_file,
        self.status.yarn_registry_dns_wrapper_pid_file,
      ],
      rollback_pid_files,
    )


if __name__ == "__main__":
  unittest.main()
