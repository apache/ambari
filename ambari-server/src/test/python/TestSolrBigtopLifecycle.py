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
from unittest.mock import MagicMock, patch

from resource_management.core.exceptions import ComponentIsNotRunning, Fail
from resource_management.libraries.functions import safe_process as SAFE_PROCESS


SOLR = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/SOLR"
)
SCRIPTS = SOLR / "package/scripts"
PID_DIR = "/var/run/solr"
PID_FILE = f"{PID_DIR}/solr-8983.pid"
SOLR_PORT = "8983"
SOLR_HOME = "/var/lib/solr/data"
PROCESS_TOKENS = (
  "-Djetty.port=8983",
  "-Dsolr.solr.home=/var/lib/solr/data",
  "-jar",
  "start.jar",
)
PROCESS_IDENTITY = SAFE_PROCESS.ProcessIdentity(
  123, 1000, 456, ("/usr/bin/java", *PROCESS_TOKENS)
)


def dependency_module(name, **attributes):
  module = ModuleType(name)
  for attribute, value in attributes.items():
    setattr(module, attribute, value)
  return module


def load_module(module_name, path, dependencies=None):
  spec = importlib.util.spec_from_file_location(module_name, path)
  module = importlib.util.module_from_spec(spec)
  with patch.dict(sys.modules, dependencies or {}):
    spec.loader.exec_module(module)
  return module


SETUP_SOLR = dependency_module(
  "setup_solr", setup_solr=MagicMock(), setup_solr_znode_env=MagicMock()
)
SOLR_SCRIPT = load_module(
  "bigtop_solr_script",
  SCRIPTS / "solr.py",
  {"setup_solr": SETUP_SOLR},
)


def params_module(**values):
  return dependency_module("params", **values)


class TestSolrLifecycle(unittest.TestCase):
  def setUp(self):
    self.params = params_module(
      solr_pidfile=PID_FILE,
      solr_piddir=PID_DIR,
      solr_user="solr",
      user_group="hadoop",
      solr_bindir="/opt/solr/bin",
      solr_port=SOLR_PORT,
      solr_datadir=SOLR_HOME,
      solr_znode="/solr",
      zookeeper_quorum="zk1:2181,zk2:2181",
      security_enabled=False,
      solr_kerberos_name_rules="DEFAULT",
      solr_log_dir="/var/log/solr",
      solr_conf="/etc/solr/conf",
    )
    self.env = MagicMock()
    self.solr = SOLR_SCRIPT.Solr()
    self._uid_patcher = patch.object(
      SAFE_PROCESS.pwd, "getpwnam", return_value=SimpleNamespace(pw_uid=1000)
    )
    self._gid_patcher = patch.object(
      SAFE_PROCESS.grp, "getgrnam", return_value=SimpleNamespace(gr_gid=456)
    )
    self._uid_patcher.start()
    self._gid_patcher.start()

  def tearDown(self):
    self._uid_patcher.stop()
    self._gid_patcher.stop()

  def test_process_tokens_are_exact_and_reject_invalid_port(self):
    self.assertEqual(
      PROCESS_TOKENS,
      SOLR_SCRIPT.solr_process_tokens(SOLR_PORT, SOLR_HOME),
    )
    for port in (None, "", "0", "65536", "8983;$(id)", "\u0668\u0669"):
      with self.subTest(port=port):
        with self.assertRaises(Fail):
          SOLR_SCRIPT.solr_process_tokens(port, SOLR_HOME)

  def test_existing_pid_is_revalidated_and_secured_before_reuse(self):
    with patch.object(SAFE_PROCESS, "read_pid", return_value=123), \
      patch.object(
        SAFE_PROCESS, "inspect_process", return_value=PROCESS_IDENTITY
      ), \
      patch.object(SAFE_PROCESS, "is_process_running", return_value=True), \
      patch.object(
        SAFE_PROCESS,
        "publish_pid_file_for_identity",
        return_value=PROCESS_IDENTITY,
      ) as publish_pid, \
      patch.object(SAFE_PROCESS, "discover_running_process") as discover:
      result = SOLR_SCRIPT.read_or_discover_solr_process(
        PID_FILE,
        "solr",
        "hadoop",
        PROCESS_TOKENS,
      )

    self.assertIs(PROCESS_IDENTITY, result)
    publish_pid.assert_called_once_with(
      PID_FILE,
      PROCESS_IDENTITY,
      "solr",
      PROCESS_TOKENS,
      owner="solr",
      group="hadoop",
      mode=0o640,
    )
    discover.assert_not_called()

  def test_pidless_process_is_discovered_and_pid_file_is_restored(self):
    with patch.object(SAFE_PROCESS, "read_pid", return_value=None), \
      patch.object(
        SAFE_PROCESS,
        "discover_running_process",
        return_value=PROCESS_IDENTITY,
      ) as discover, \
      patch.object(
        SAFE_PROCESS,
        "publish_pid_file_for_identity",
        return_value=PROCESS_IDENTITY,
      ) as create_pid:
      result = SOLR_SCRIPT.read_or_discover_solr_process(
        PID_FILE,
        "solr",
        "hadoop",
        PROCESS_TOKENS,
      )

    self.assertIs(PROCESS_IDENTITY, result)
    discover.assert_called_once_with("solr", PROCESS_TOKENS)
    create_pid.assert_called_once_with(
      PID_FILE,
      PROCESS_IDENTITY,
      "solr",
      PROCESS_TOKENS,
      owner="solr",
      group="hadoop",
      mode=0o640,
    )

  def test_ambiguous_discovery_and_identity_change_fail_closed(self):
    for discover_error, create_error in (
      (Fail("ambiguous process discovery"), None),
      (None, Fail("process identity changed during PID restoration")),
    ):
      with self.subTest(
        discover_error=discover_error,
        create_error=create_error,
      ):
        discovered = PROCESS_IDENTITY if discover_error is None else None
        with patch.object(SAFE_PROCESS, "read_pid", return_value=None), \
          patch.object(
            SAFE_PROCESS,
            "discover_running_process",
            return_value=discovered,
            side_effect=discover_error,
          ), \
          patch.object(
            SAFE_PROCESS,
            "publish_pid_file_for_identity",
            side_effect=create_error,
          ) as create_pid:
          with self.assertRaises(Fail):
            SOLR_SCRIPT.read_or_discover_solr_process(
              PID_FILE,
              "solr",
              "hadoop",
              PROCESS_TOKENS,
            )
        if discover_error is not None:
          create_pid.assert_not_called()

  def test_start_accepts_a_discovered_pidless_process_without_duplication(self):
    with patch.dict(sys.modules, {"params": self.params}), \
      patch.object(
        SOLR_SCRIPT,
        "read_or_discover_solr_process",
        side_effect=(PROCESS_IDENTITY, PROCESS_IDENTITY),
      ) as resolve_process, \
      patch.object(self.solr, "configure"), \
      patch.object(SOLR_SCRIPT, "setup_solr_znode_env"), \
      patch.object(SOLR_SCRIPT, "Execute") as execute, \
      patch.object(SOLR_SCRIPT.Logger, "info"):
      self.solr.start(self.env)

    self.assertEqual(2, resolve_process.call_count)
    execute.assert_not_called()

  def test_start_does_not_launch_second_running_instance(self):
    with patch.dict(sys.modules, {"params": self.params}), \
      patch.object(SAFE_PROCESS, "read_pid", return_value=123), \
      patch.object(
        SAFE_PROCESS, "inspect_process", return_value=PROCESS_IDENTITY
      ), \
      patch.object(SAFE_PROCESS, "is_process_running", return_value=True), \
      patch.object(
        SAFE_PROCESS,
        "publish_pid_file_for_identity",
        return_value=PROCESS_IDENTITY,
      ), \
      patch.object(self.solr, "configure") as configure, \
      patch.object(SOLR_SCRIPT, "setup_solr_znode_env") as setup_znode, \
      patch.object(SOLR_SCRIPT, "Execute") as execute, \
      patch.object(SOLR_SCRIPT.Logger, "info"):
      self.solr.start(self.env)

    configure.assert_called_once_with(self.env)
    setup_znode.assert_called_once_with()
    execute.assert_not_called()

  def test_concurrent_pid_after_configuration_prevents_second_start(self):
    with patch.dict(sys.modules, {"params": self.params}), \
      patch.object(SAFE_PROCESS, "read_pid", side_effect=(None, 123, 123)), \
      patch.object(SAFE_PROCESS, "discover_running_process", return_value=None), \
      patch.object(
        SAFE_PROCESS, "inspect_process", return_value=PROCESS_IDENTITY
      ), \
      patch.object(SAFE_PROCESS, "is_process_running", return_value=True), \
      patch.object(
        SAFE_PROCESS,
        "publish_pid_file_for_identity",
        return_value=PROCESS_IDENTITY,
      ), \
      patch.object(self.solr, "configure") as configure, \
      patch.object(SOLR_SCRIPT, "setup_solr_znode_env") as setup_znode, \
      patch.object(SOLR_SCRIPT, "Execute") as execute, \
      patch.object(SOLR_SCRIPT.Logger, "info"):
      self.solr.start(self.env)

    configure.assert_called_once_with(self.env)
    setup_znode.assert_called_once_with()
    execute.assert_not_called()

  def test_start_uses_structured_argv_and_waits_for_verified_process(self):
    self.params.security_enabled = True
    self.params.zookeeper_quorum = "zk1:2181,zk2:2181;$(id)"
    self.params.solr_znode = "/solr $(id)"
    self.params.solr_kerberos_name_rules = "RULE:[1:$1@$0](.*);$(id)"
    with patch.dict(sys.modules, {"params": self.params}), \
      patch.object(SAFE_PROCESS, "read_pid", side_effect=(None, None)), \
      patch.object(SAFE_PROCESS, "discover_running_process", return_value=None), \
      patch.object(self.solr, "configure"), \
      patch.object(SOLR_SCRIPT, "setup_solr_znode_env"), \
      patch.object(SOLR_SCRIPT, "Execute") as execute, \
      patch.object(
        SOLR_SCRIPT,
        "wait_for_started_solr_process",
        return_value=PROCESS_IDENTITY,
      ) as wait:
      self.solr.start(self.env)

    execute.assert_called_once_with(
      (
        "/opt/solr/bin/solr",
        "start",
        "-cloud",
        "-noprompt",
        "-p",
        SOLR_PORT,
        "-s",
        SOLR_HOME,
        "-z",
        "zk1:2181,zk2:2181;$(id)/solr $(id)",
        "-Dsolr.kerberos.name.rules=RULE:[1:$1@$0](.*);$(id)",
      ),
      environment={"SOLR_INCLUDE": "/etc/solr/conf/solr-env.sh"},
      user="solr",
      logoutput=True,
      timeout=60,
      timeout_kill_strategy=SOLR_SCRIPT.TerminateStrategy.KILL_PROCESS_GROUP,
    )
    wait.assert_called_once_with(
      PID_FILE,
      "solr",
      "hadoop",
      PROCESS_TOKENS,
      attempts=10,
      sleep_seconds=1,
    )

  def test_start_command_failure_and_missing_process_both_fail_closed(self):
    for execute_error, wait_error in (
      (Fail("start failed"), None),
      (None, Fail("valid process never appeared")),
    ):
      with self.subTest(execute_error=execute_error, wait_error=wait_error):
        with patch.dict(sys.modules, {"params": self.params}), \
          patch.object(SAFE_PROCESS, "read_pid", side_effect=(None, None)), \
          patch.object(
            SAFE_PROCESS, "discover_running_process", return_value=None
          ), \
          patch.object(self.solr, "configure"), \
          patch.object(SOLR_SCRIPT, "setup_solr_znode_env"), \
          patch.object(SOLR_SCRIPT, "Execute", side_effect=execute_error), \
          patch.object(
            SOLR_SCRIPT,
            "wait_for_started_solr_process",
            side_effect=wait_error,
          ) as wait, \
          patch.object(
            SOLR_SCRIPT, "rollback_started_solr_process"
          ) as rollback:
          with self.assertRaises(Fail):
            self.solr.start(self.env)

        rollback.assert_not_called()
        if execute_error is not None:
          wait.assert_not_called()
        else:
          wait.assert_called_once()

  def test_pid_publication_failure_preserves_original_error(self):
    start_error = Fail("Solr process identity was not published")
    with patch.object(
      SAFE_PROCESS,
      "wait_for_discovered_process",
      return_value=PROCESS_IDENTITY,
    ), \
      patch.object(SOLR_SCRIPT, "publish_solr_process", side_effect=start_error), \
      patch.object(
        SOLR_SCRIPT,
        "rollback_started_solr_process",
        side_effect=Fail("rollback failed"),
      ) as rollback, \
      patch.object(SOLR_SCRIPT.Logger, "warning") as warning:
      with self.assertRaises(Fail) as raised:
        SOLR_SCRIPT.wait_for_started_solr_process(
          PID_FILE, "solr", "hadoop", PROCESS_TOKENS
        )

    self.assertIs(start_error, raised.exception)
    rollback.assert_called_once_with(
      PID_FILE, PROCESS_IDENTITY, "solr", PROCESS_TOKENS
    )
    warning.assert_called_once_with(
      "Could not roll back failed Solr PID publication: rollback failed"
    )

  def test_start_rollback_never_discovers_an_unrelated_solr_process(self):
    with patch.object(
        SAFE_PROCESS, "terminate_process"
      ) as terminate, \
      patch.object(SAFE_PROCESS, "read_pid", return_value=123), \
      patch.object(SAFE_PROCESS, "remove_pid_file_if_stopped") as remove, \
      patch.object(SAFE_PROCESS, "discover_running_process") as discover:
      self.assertTrue(
        SOLR_SCRIPT.rollback_started_solr_process(
          PID_FILE, PROCESS_IDENTITY, "solr", PROCESS_TOKENS
        )
      )

    discover.assert_not_called()
    terminate.assert_called_once_with(PROCESS_IDENTITY, "solr", PROCESS_TOKENS)
    remove.assert_called_once_with(
      PID_FILE,
      123,
      expected_user="solr",
      expected_cmdline=PROCESS_TOKENS,
    )

  def test_invalid_pid_and_wrong_identity_prevent_start(self):
    for read_error, inspect_error in (
      (Fail("invalid pid"), None),
      (None, Fail("command line does not match expected service identity")),
    ):
      with self.subTest(read_error=read_error, inspect_error=inspect_error):
        read_result = 123 if inspect_error is not None else read_error
        read_side_effect = None if inspect_error is not None else read_error
        with patch.dict(sys.modules, {"params": self.params}), \
          patch.object(
            SAFE_PROCESS,
            "read_pid",
            return_value=read_result,
            side_effect=read_side_effect,
          ), \
          patch.object(
            SAFE_PROCESS, "inspect_process", side_effect=inspect_error
          ), \
          patch.object(self.solr, "configure") as configure, \
          patch.object(SOLR_SCRIPT, "Execute") as execute, \
          patch.object(
            SAFE_PROCESS, "remove_pid_file_if_stopped"
          ) as remove_pid:
          with self.assertRaises(Fail):
            self.solr.start(self.env)

        configure.assert_not_called()
        execute.assert_not_called()
        remove_pid.assert_not_called()

  def test_stop_terminates_only_the_verified_process_identity(self):
    with patch.dict(sys.modules, {"params": self.params}), \
      patch.object(SAFE_PROCESS, "read_pid", side_effect=(123, 123)), \
      patch.object(
        SAFE_PROCESS,
        "publish_pid_file_for_identity",
        return_value=PROCESS_IDENTITY,
      ), \
      patch.object(
        SAFE_PROCESS, "inspect_process", return_value=PROCESS_IDENTITY
      ), \
      patch.object(SAFE_PROCESS, "is_process_running", return_value=True), \
      patch.object(SAFE_PROCESS, "terminate_process") as terminate_process, \
      patch.object(
        SAFE_PROCESS, "remove_pid_file_if_stopped", return_value=False
      ) as remove_pid:
      self.solr.stop(self.env)

    terminate_process.assert_called_once_with(
      PROCESS_IDENTITY,
      "solr",
      PROCESS_TOKENS,
    )
    remove_pid.assert_called_once_with(
      PID_FILE,
      123,
      expected_user="solr",
      expected_cmdline=PROCESS_TOKENS,
    )

  def test_stop_accepts_a_discovered_pidless_process(self):
    with patch.dict(sys.modules, {"params": self.params}), \
      patch.object(
        SOLR_SCRIPT,
        "read_or_discover_solr_process",
        return_value=PROCESS_IDENTITY,
      ), \
      patch.object(self.solr, "kill_process") as kill_process:
      self.solr.stop(self.env)

    kill_process.assert_called_once_with(
      PID_FILE,
      "solr",
      SOLR_PORT,
      SOLR_HOME,
      "/var/log/solr",
      expected_identity=PROCESS_IDENTITY,
    )

  def test_pid_file_replacement_prevents_stop(self):
    with patch.dict(sys.modules, {"params": self.params}), \
      patch.object(SAFE_PROCESS, "read_pid", side_effect=(123, 999)), \
      patch.object(
        SAFE_PROCESS, "inspect_process", return_value=PROCESS_IDENTITY
      ), \
      patch.object(SAFE_PROCESS, "is_process_running", return_value=True), \
      patch.object(SAFE_PROCESS, "terminate_process") as terminate_process, \
      patch.object(
        SAFE_PROCESS, "remove_pid_file_if_stopped"
      ) as remove_pid:
      with self.assertRaisesRegex(Fail, "identifies process 999, expected 123"):
        self.solr.stop(self.env)

    terminate_process.assert_not_called()
    remove_pid.assert_not_called()

  def test_missing_pid_file_makes_stop_idempotent(self):
    with patch.dict(sys.modules, {"params": self.params}), \
      patch.object(SAFE_PROCESS, "read_pid", return_value=None), \
      patch.object(SAFE_PROCESS, "discover_running_process", return_value=None), \
      patch.object(SAFE_PROCESS, "terminate_process") as terminate_process, \
      patch.object(
        SAFE_PROCESS, "remove_pid_file_if_stopped"
      ) as remove_pid, \
      patch.object(SOLR_SCRIPT.Logger, "info"):
      self.solr.stop(self.env)

    terminate_process.assert_not_called()
    remove_pid.assert_not_called()

  def test_invalid_pid_file_prevents_stop_signal_and_delete(self):
    with patch.dict(sys.modules, {"params": self.params}), \
      patch.object(SAFE_PROCESS, "read_pid", side_effect=Fail("invalid pid")), \
      patch.object(SAFE_PROCESS, "terminate_process") as terminate_process, \
      patch.object(
        SAFE_PROCESS, "remove_pid_file_if_stopped"
      ) as remove_pid:
      with self.assertRaisesRegex(Fail, "invalid pid"):
        self.solr.stop(self.env)

    terminate_process.assert_not_called()
    remove_pid.assert_not_called()

  def test_changed_or_reused_pid_is_neither_signaled_nor_deleted(self):
    reused = SAFE_PROCESS.ProcessIdentity(
      123, 1000, 999, ("/usr/bin/java", *PROCESS_TOKENS)
    )
    for current_pid, current_identity, message in (
      (999, PROCESS_IDENTITY, "pid file changed"),
      (123, reused, "reused Solr pid"),
    ):
      with self.subTest(message=message):
        with patch.object(SAFE_PROCESS, "read_pid", return_value=current_pid), \
          patch.object(
            SAFE_PROCESS, "inspect_process", return_value=current_identity
          ), \
          patch.object(SAFE_PROCESS, "terminate_process") as terminate_process, \
          patch.object(
            SAFE_PROCESS, "remove_pid_file_if_stopped"
          ) as remove_pid:
          with self.assertRaisesRegex(Fail, message):
            self.solr.kill_process(
              PID_FILE,
              "solr",
              SOLR_PORT,
              SOLR_HOME,
              "/var/log/solr",
              expected_identity=PROCESS_IDENTITY,
            )

        terminate_process.assert_not_called()
        remove_pid.assert_not_called()

  def test_kill_timeout_keeps_pid_file_and_shows_logs(self):
    with patch.object(SAFE_PROCESS, "read_pid", return_value=123), \
      patch.object(
        SAFE_PROCESS, "inspect_process", return_value=PROCESS_IDENTITY
      ), \
      patch.object(SAFE_PROCESS, "is_process_running", return_value=True), \
      patch.object(
        SAFE_PROCESS, "terminate_process", side_effect=Fail("timeout")
      ), \
      patch.object(SOLR_SCRIPT, "show_logs") as show_logs, \
      patch.object(
        SAFE_PROCESS, "remove_pid_file_if_stopped"
      ) as remove_pid:
      with self.assertRaisesRegex(Fail, "timeout"):
        self.solr.kill_process(
          PID_FILE,
          "solr",
          SOLR_PORT,
          SOLR_HOME,
          "/var/log/solr",
        )

    show_logs.assert_called_once_with("/var/log/solr", "solr")
    remove_pid.assert_not_called()

  def test_kill_preserves_primary_failure_when_log_collection_fails(self):
    primary_error = Fail("Solr process did not stop")
    with patch.object(SAFE_PROCESS, "read_pid", return_value=123), \
      patch.object(
        SAFE_PROCESS, "inspect_process", return_value=PROCESS_IDENTITY
      ), \
      patch.object(SAFE_PROCESS, "is_process_running", return_value=True), \
      patch.object(
        SAFE_PROCESS, "terminate_process", side_effect=primary_error
      ), \
      patch.object(
        SOLR_SCRIPT, "show_logs", side_effect=OSError("logs unavailable")
      ), \
      patch.object(SOLR_SCRIPT.Logger, "warning") as warning, \
      patch.object(
        SAFE_PROCESS, "remove_pid_file_if_stopped"
      ) as remove_pid:
      with self.assertRaises(Fail) as raised:
        self.solr.kill_process(
          PID_FILE,
          "solr",
          SOLR_PORT,
          SOLR_HOME,
          "/var/log/solr",
        )

    self.assertIs(primary_error, raised.exception)
    warning.assert_called_once_with(
      "Could not collect Solr logs after stop failure: logs unavailable"
    )
    remove_pid.assert_not_called()

  def test_status_reports_missing_process_without_hiding_invalid_pid(self):
    status_params = dependency_module(
      "status_params",
      solr_pidfile=PID_FILE,
      solr_piddir=PID_DIR,
      solr_user="solr",
      user_group="hadoop",
      solr_port=SOLR_PORT,
      solr_datadir=SOLR_HOME,
    )
    with patch.dict(sys.modules, {"status_params": status_params}), \
      patch.object(SAFE_PROCESS, "read_pid", return_value=None), \
      patch.object(SAFE_PROCESS, "discover_running_process", return_value=None):
      with self.assertRaises(ComponentIsNotRunning):
        self.solr.status(self.env)

    with patch.dict(sys.modules, {"status_params": status_params}), \
      patch.object(
        SAFE_PROCESS,
        "read_pid",
        side_effect=Fail("invalid pid"),
      ):
      with self.assertRaisesRegex(Fail, "invalid pid"):
        self.solr.status(self.env)

  def test_status_accepts_a_discovered_pidless_process(self):
    status_params = dependency_module(
      "status_params",
      solr_pidfile=PID_FILE,
      solr_user="solr",
      user_group="hadoop",
      solr_port=SOLR_PORT,
      solr_datadir=SOLR_HOME,
    )
    with patch.dict(sys.modules, {"status_params": status_params}), \
      patch.object(
        SOLR_SCRIPT,
        "read_or_discover_solr_process",
        return_value=PROCESS_IDENTITY,
      ) as resolve_process:
      self.solr.status(self.env)

    resolve_process.assert_called_once_with(
      PID_FILE,
      "solr",
      "hadoop",
      PROCESS_TOKENS,
    )


class TestSolrLifecycleSourceContracts(unittest.TestCase):
  def test_status_params_has_one_explicit_solr_pid_candidate(self):
    source = (SCRIPTS / "status_params.py").read_text(encoding="utf-8")
    self.assertIn(
      'solr_pidfile = format("{solr_piddir}/solr-{solr_port}.pid")',
      source,
    )
    self.assertIn('default("configurations/solr-env/solr_datadir"', source)
    self.assertIn("user_group = solr_utils.validate_user", source)
    self.assertNotIn("listdir", source)
    self.assertNotIn("prev_solr_pidfile", source)

  def test_lifecycle_uses_only_the_shared_identity_bound_process_helper(self):
    source = (SCRIPTS / "solr.py").read_text(encoding="utf-8")
    self.assertIn(
      "from resource_management.libraries.functions import safe_process",
      source,
    )
    self.assertIn("safe_process.terminate_process(identity", source)
    self.assertNotIn("solr_pid_utils", source)
    self.assertNotIn('"stop", "-p"', source)
    self.assertNotIn('"stop", "-all"', source)


if __name__ == "__main__":
  unittest.main()
