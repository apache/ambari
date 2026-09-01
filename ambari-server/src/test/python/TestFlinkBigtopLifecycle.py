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

from resource_management.core.exceptions import Fail
from resource_management.libraries.functions import safe_process


FLINK = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/FLINK"
)
SCRIPTS = FLINK / "package/scripts"
PID_FILE = "/var/run/flink/flink_history_server.pid"
CONFIG_DIR = "/etc/flink/conf"
TOKENS = (
  "org.apache.flink.runtime.webmonitor.history.HistoryServer",
  "--configDir",
  CONFIG_DIR,
)
IDENTITY = safe_process.ProcessIdentity(
  123, 1001, 456, ("/usr/bin/java", "-Xmx1024m", *TOKENS)
)


def load_module(module_name, path, dependencies=None):
  spec = importlib.util.spec_from_file_location(module_name, path)
  module = importlib.util.module_from_spec(spec)
  with patch.dict(sys.modules, dependencies or {}):
    spec.loader.exec_module(module)
  return module


def params_module(**values):
  module = ModuleType("params")
  for name, value in values.items():
    setattr(module, name, value)
  return module


FLINK_UTILS = load_module("bigtop_flink_utils", SCRIPTS / "flink_utils.py")
FLINK_PROCESS = load_module(
  "bigtop_flink_process",
  SCRIPTS / "flink_process.py",
  {"flink_utils": FLINK_UTILS},
)
FLINK_SERVICE = load_module(
  "bigtop_flink_service",
  SCRIPTS / "flink_service.py",
  {"flink_process": FLINK_PROCESS, "flink_utils": FLINK_UTILS},
)
FLINK_SERVICE_CHECK = load_module(
  "bigtop_flink_service_check",
  SCRIPTS / "service_check.py",
  {"flink_utils": FLINK_UTILS},
)


class TestFlinkUtilities(unittest.TestCase):
  def test_bigtop_paths_hdfs_uris_and_yaml_values_fail_closed(self):
    self.assertEqual(
      "3.2.0", FLINK_UTILS.validate_bigtop_stack("BIGTOP", "3.2.0")
    )
    self.assertEqual(
      "hdfs:///completed-jobs/",
      FLINK_UTILS.validate_hdfs_uri(
        "hdfs:///completed-jobs/", "archive directory"
      ),
    )
    self.assertEqual(
      '"/path/with\\\"quote"',
      FLINK_UTILS.yaml_string('/path/with"quote', "path"),
    )

    with self.assertRaises(Fail):
      FLINK_UTILS.validate_bigtop_stack("OTHER", "3.2.0")
    for path in ("relative", "/tmp/../etc", "/tmp/with space", "/tmp/$(id)"):
      with self.subTest(path=path), self.assertRaises(Fail):
        FLINK_UTILS.validate_absolute_path(path, "path")
    for path in ("/", "/etc/flink", "/home/flink", "/var/log", "/var/run"):
      with self.subTest(path=path), self.assertRaises(Fail):
        FLINK_UTILS.validate_service_directory(path, "directory")
    for uri in (
      "file:///completed-jobs",
      "hdfs:///../private",
      "hdfs:///jobs\ninjected: true",
      "hdfs://[invalid/jobs",
    ):
      with self.subTest(uri=uri), self.assertRaises(Fail):
        FLINK_UTILS.validate_hdfs_uri(uri, "archive directory")

  def test_files_and_classpath_must_be_unambiguous(self):
    with (
      patch.object(FLINK_UTILS.sudo, "path_lexists", return_value=True),
      patch.object(FLINK_UTILS.sudo, "path_isfile", return_value=True),
      patch.object(FLINK_UTILS.sudo, "path_islink", return_value=False),
      patch.object(
        FLINK_UTILS.sudo,
        "stat",
        return_value=SimpleNamespace(st_mode=0o100755),
      ),
      patch.object(
        FLINK_UTILS.shell,
        "checked_call",
        return_value=(0, "/usr/lib/hadoop/*:/etc/hadoop/conf\n"),
      ) as checked_call,
    ):
      self.assertEqual(
        "/usr/lib/hadoop/*:/etc/hadoop/conf",
        FLINK_UTILS.resolve_hadoop_classpath(
          "/usr/bin/hadoop", "flink", "/usr/lib/jvm/java-17"
        ),
      )
    checked_call.assert_called_once_with(
      ("/usr/bin/hadoop", "classpath"),
      user="flink",
      env={"JAVA_HOME": "/usr/lib/jvm/java-17"},
      timeout=60,
    )

    with (
      patch.object(FLINK_UTILS.sudo, "path_lexists", return_value=True),
      patch.object(FLINK_UTILS.sudo, "path_isfile", return_value=True),
      patch.object(FLINK_UTILS.sudo, "path_islink", return_value=False),
      patch.object(
        FLINK_UTILS.sudo,
        "stat",
        return_value=SimpleNamespace(st_mode=0o100755),
      ),
      patch.object(
        FLINK_UTILS.shell,
        "checked_call",
        return_value=(0, "warning\n/usr/lib/hadoop/*\n"),
      ),
      self.assertRaisesRegex(Fail, "exactly one"),
    ):
      FLINK_UTILS.resolve_hadoop_classpath(
        "/usr/bin/hadoop", "flink", "/usr/lib/jvm/java-17"
      )


class TestFlinkProcess(unittest.TestCase):
  def test_identity_uses_the_exact_upstream_main_and_config_arguments(self):
    self.assertEqual(TOKENS, FLINK_PROCESS.expected_process_tokens(CONFIG_DIR))
    for pid_file in (
      "relative.pid",
      "/flink_history_server.pid",
      "/tmp/flink_history_server.pid",
      "/etc/flink_history_server.pid",
      "/var/run/flink/other.pid",
    ):
      with self.subTest(pid_file=pid_file), self.assertRaises(Fail):
        FLINK_PROCESS.validate_pid_file(pid_file)

  def test_pidless_process_is_discovered_and_atomically_published_0640(self):
    with (
      patch.object(safe_process, "read_pid", return_value=None),
      patch.object(
        safe_process, "discover_running_process", return_value=IDENTITY
      ) as discover,
      patch.object(
        safe_process, "create_pid_file_for_identity", return_value=IDENTITY
      ) as create,
    ):
      result = FLINK_PROCESS.read_or_recover_process(
        PID_FILE, "flink", "hadoop", CONFIG_DIR
      )

    self.assertIs(IDENTITY, result)
    discover.assert_called_once_with("flink", TOKENS)
    create.assert_called_once_with(
      PID_FILE,
      IDENTITY,
      expected_user="flink",
      expected_cmdline=TOKENS,
      owner="flink",
      group="hadoop",
      mode=0o640,
    )

  def test_stale_pid_is_removed_and_ambiguous_recovery_fails_closed(self):
    with (
      patch.object(safe_process, "read_pid", return_value=123),
      patch.object(safe_process, "read_running_process", return_value=None),
      patch.object(
        safe_process, "remove_pid_file_if_stopped", return_value=True
      ) as remove,
      patch.object(safe_process, "discover_running_process", return_value=None),
    ):
      self.assertIsNone(
        FLINK_PROCESS.read_or_recover_process(
          PID_FILE, "flink", "hadoop", CONFIG_DIR
        )
      )
    remove.assert_called_once_with(
      PID_FILE,
      123,
      expected_user="flink",
      expected_cmdline=TOKENS,
    )

    with (
      patch.object(safe_process, "read_pid", return_value=None),
      patch.object(
        safe_process,
        "discover_running_process",
        side_effect=Fail("ambiguous process discovery"),
      ),
      self.assertRaisesRegex(Fail, "ambiguous"),
    ):
      FLINK_PROCESS.read_or_recover_process(
        PID_FILE, "flink", "hadoop", CONFIG_DIR
      )

  def test_stop_refuses_pid_reuse_and_uses_term_wait_kill_contract(self):
    with (
      patch.object(
        FLINK_PROCESS, "read_or_recover_process", return_value=IDENTITY
      ),
      patch.object(safe_process, "terminate_process") as terminate,
      patch.object(safe_process, "remove_pid_file_if_stopped") as remove,
    ):
      self.assertTrue(
        FLINK_PROCESS.stop_process(PID_FILE, "flink", "hadoop", CONFIG_DIR)
      )

    terminate.assert_called_once_with(
      IDENTITY,
      "flink",
      TOKENS,
      term_wait_attempts=30,
      term_wait_sleep=1,
      kill_wait_attempts=10,
      kill_wait_sleep=1,
    )
    remove.assert_called_once_with(
      PID_FILE,
      123,
      expected_user="flink",
      expected_cmdline=TOKENS,
    )


class TestFlinkService(unittest.TestCase):
  def test_start_is_idempotent_and_does_not_invoke_upstream_launcher(self):
    params = params_module(
      flink_history_server_pid_file=PID_FILE,
      flink_user="flink",
      user_group="hadoop",
      flink_config_dir=CONFIG_DIR,
    )
    with (
      patch.object(
        FLINK_PROCESS, "read_or_recover_process", return_value=IDENTITY
      ),
      patch.object(FLINK_SERVICE, "Execute") as execute,
    ):
      FLINK_SERVICE._start_history_server(params)
    execute.assert_not_called()

  def test_start_uses_foreground_argv_and_isolated_upstream_pid_directory(self):
    hdfs_resource = MagicMock()
    params = params_module(
      flink_history_server_pid_file=PID_FILE,
      flink_user="flink",
      user_group="hadoop",
      flink_config_dir=CONFIG_DIR,
      flink_pid_dir="/var/run/flink",
      flink_log_dir="/var/log/flink",
      jobmanager_archive_fs_dir="hdfs:///completed-jobs/",
      HdfsResource=hdfs_resource,
      historyserver_script="/usr/lib/flink/bin/historyserver.sh",
      security_enabled=False,
      hadoop_executable="/usr/bin/hadoop",
      java_home="/usr/lib/jvm/java-17",
      hadoop_conf_dir="/etc/hadoop/conf",
    )
    launcher_dir = "/var/run/flink/.ambari-historyserver-" + "a" * 32
    launcher_pid = launcher_dir + "/flink-ambari-historyserver-historyserver.pid"
    with (
      patch.object(FLINK_PROCESS, "read_or_recover_process", return_value=None),
      patch.object(
        FLINK_PROCESS, "wait_for_started_process", return_value=IDENTITY
      ),
      patch.object(FLINK_UTILS, "validate_executable"),
      patch.object(FLINK_UTILS, "resolve_hadoop_classpath", return_value="/hadoop/*"),
      patch.object(
        FLINK_SERVICE,
        "_launcher_pid_paths",
        return_value=(launcher_dir, launcher_pid),
      ),
      patch.object(FLINK_SERVICE, "_cleanup_launcher_pid") as cleanup,
      patch.object(FLINK_SERVICE, "Directory"),
      patch.object(FLINK_SERVICE, "Execute") as execute,
    ):
      FLINK_SERVICE._start_history_server(params)

    command = execute.call_args.args[0]
    self.assertEqual(
      ("/usr/lib/flink/bin/historyserver.sh", "start-foreground"),
      command,
    )
    self.assertFalse(execute.call_args.kwargs["wait_for_finish"])
    self.assertEqual(
      launcher_dir, execute.call_args.kwargs["environment"]["FLINK_PID_DIR"]
    )
    self.assertEqual(0o770, hdfs_resource.call_args_list[0].kwargs["mode"])
    cleanup.assert_called_once_with(launcher_dir, launcher_pid, "/var/run/flink")

  def test_start_failure_still_cleans_the_isolated_launcher_pid(self):
    params = params_module(
      flink_history_server_pid_file=PID_FILE,
      flink_user="flink",
      user_group="hadoop",
      flink_config_dir=CONFIG_DIR,
      flink_pid_dir="/var/run/flink",
      flink_log_dir="/var/log/flink",
      jobmanager_archive_fs_dir="hdfs:///completed-jobs/",
      HdfsResource=MagicMock(),
      historyserver_script="/usr/lib/flink/bin/historyserver.sh",
      security_enabled=False,
      hadoop_executable="/usr/bin/hadoop",
      java_home="/usr/lib/jvm/java-17",
      hadoop_conf_dir="/etc/hadoop/conf",
    )
    launcher_dir = "/var/run/flink/.ambari-historyserver-" + "b" * 32
    launcher_pid = launcher_dir + "/flink-ambari-historyserver-historyserver.pid"
    with (
      patch.object(FLINK_PROCESS, "read_or_recover_process", return_value=None),
      patch.object(FLINK_UTILS, "validate_executable"),
      patch.object(
        FLINK_UTILS, "resolve_hadoop_classpath", return_value="/hadoop/*"
      ),
      patch.object(
        FLINK_SERVICE,
        "_launcher_pid_paths",
        return_value=(launcher_dir, launcher_pid),
      ),
      patch.object(FLINK_SERVICE, "_cleanup_launcher_pid") as cleanup,
      patch.object(FLINK_SERVICE, "Directory"),
      patch.object(
        FLINK_SERVICE, "Execute", side_effect=RuntimeError("start failed")
      ),
      patch.object(FLINK_SERVICE, "show_logs"),
      self.assertRaisesRegex(RuntimeError, "start failed"),
    ):
      FLINK_SERVICE._start_history_server(params)
    cleanup.assert_called_once_with(launcher_dir, launcher_pid, "/var/run/flink")


class TestFlinkServiceCheck(unittest.TestCase):
  def test_service_check_uses_flink_119_yarn_application_mode_and_timeout(self):
    params = params_module(
      flink_cli="/usr/lib/flink/bin/flink",
      wordcount_jar="/usr/lib/flink/examples/batch/WordCount.jar",
      hadoop_executable="/usr/bin/hadoop",
      smokeuser="ambari-qa",
      java_home="/usr/lib/jvm/java-17",
      security_enabled=False,
      flink_config_dir=CONFIG_DIR,
      flink_cli_log_dir="/var/log/flink-cli",
      hadoop_conf_dir="/etc/hadoop/conf",
    )
    service_check = FLINK_SERVICE_CHECK.FlinkServiceCheck()
    env = SimpleNamespace(set_params=MagicMock())
    with (
      patch.dict(sys.modules, {"params": params}),
      patch.object(FLINK_UTILS, "validate_executable"),
      patch.object(FLINK_UTILS, "validate_regular_file"),
      patch.object(FLINK_UTILS, "resolve_hadoop_classpath", return_value="/hadoop/*"),
      patch.object(FLINK_SERVICE_CHECK.shell, "checked_call") as checked_call,
    ):
      service_check.service_check(env)

    self.assertEqual(
      (
        "/usr/lib/flink/bin/flink",
        "run-application",
        "--target",
        "yarn-application",
        "-Dclassloader.check-leaked-classloader=false",
        "/usr/lib/flink/examples/batch/WordCount.jar",
      ),
      checked_call.call_args.args[0],
    )
    self.assertEqual(240, checked_call.call_args.kwargs["timeout"])

  def test_secure_service_check_uses_one_private_cache_context(self):
    params = params_module(
      flink_cli="/usr/lib/flink/bin/flink",
      wordcount_jar="/usr/lib/flink/examples/batch/WordCount.jar",
      hadoop_executable="/usr/bin/hadoop",
      smokeuser="ambari-qa",
      user_group="hadoop",
      java_home="/usr/lib/jvm/java-17",
      security_enabled=True,
      smoke_user_keytab="/etc/security/keytabs/smoke.keytab",
      smokeuser_principal="ambari-qa@EXAMPLE.COM",
      kinit_path_local="/usr/bin/kinit",
      tmp_dir="/var/lib/ambari-agent/tmp",
      flink_config_dir=CONFIG_DIR,
      flink_cli_log_dir="/var/log/flink-cli",
      hadoop_conf_dir="/etc/hadoop/conf",
    )
    cache = MagicMock()
    cache.merge_environment.return_value = {
      "KRB5CCNAME": "FILE:/private/cache/krb5cc"
    }
    cache_context = MagicMock()
    cache_context.__enter__.return_value = cache
    cache_context.__exit__.return_value = False
    service_check = FLINK_SERVICE_CHECK.FlinkServiceCheck()
    env = SimpleNamespace(set_params=MagicMock())
    with (
      patch.dict(sys.modules, {"params": params}),
      patch.object(FLINK_UTILS, "validate_executable"),
      patch.object(FLINK_UTILS, "validate_regular_file"),
      patch.object(FLINK_UTILS, "validate_keytab"),
      patch.object(
        FLINK_UTILS, "resolve_hadoop_classpath", return_value="/hadoop/*"
      ),
      patch.object(
        FLINK_SERVICE_CHECK,
        "PrivateKerberosCache",
        return_value=cache_context,
      ) as private_cache,
      patch.object(FLINK_SERVICE_CHECK.shell, "checked_call") as checked_call,
    ):
      service_check.service_check(env)

    private_cache.assert_called_once_with(
      "ambari-qa",
      "hadoop",
      temp_dir="/var/lib/ambari-agent/tmp",
      prefix="ambari-flink-service-check-",
    )
    cache.kinit.assert_called_once_with(
      "/usr/bin/kinit",
      "/etc/security/keytabs/smoke.keytab",
      "ambari-qa@EXAMPLE.COM",
      timeout=60,
    )
    self.assertEqual(
      "FILE:/private/cache/krb5cc",
      checked_call.call_args.kwargs["env"]["KRB5CCNAME"],
    )


if __name__ == "__main__":
  unittest.main()
