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
from types import ModuleType
import unittest
from unittest.mock import MagicMock, patch

from resource_management.core.exceptions import Fail
from resource_management.libraries.functions import safe_process


SPARK = Path(__file__).resolve().parents[2] / "main/resources/stacks/BIGTOP/3.2.0/services/SPARK"
SCRIPTS = SPARK / "package/scripts"
PID_FILE = "/var/run/spark/spark_history_server.pid"
THRIFT_PID_FILE = "/var/run/spark/spark_thrift_server.pid"
CONF_FILE = "/etc/spark/conf/spark-defaults.conf"
HISTORY_TOKENS = ("org.apache.spark.deploy.history.HistoryServer", "--properties-file", CONF_FILE)
THRIFT_TOKENS = (
  "org.apache.spark.sql.hive.thriftserver.HiveThriftServer2",
  "--properties-file",
  CONF_FILE,
)
IDENTITY = safe_process.ProcessIdentity(123, 1001, 456, ("/usr/bin/java", *HISTORY_TOKENS))


def load_module(name, path, dependencies=None):
  spec = importlib.util.spec_from_file_location(name, path)
  module = importlib.util.module_from_spec(spec)
  with patch.dict(sys.modules, dependencies or {}):
    spec.loader.exec_module(module)
  return module


def params_module(**values):
  module = ModuleType("params")
  for name, value in values.items():
    setattr(module, name, value)
  return module


SPARK_UTILS = load_module("bigtop_spark_utils", SCRIPTS / "spark_utils.py")
SPARK_PROCESS = load_module(
  "bigtop_spark_process",
  SCRIPTS / "spark_process.py",
  {"spark_utils": SPARK_UTILS},
)
SPARK_SERVICE = load_module(
  "bigtop_spark_service",
  SCRIPTS / "spark_service.py",
  {"spark_utils": SPARK_UTILS, "spark_process": SPARK_PROCESS},
)
SPARK_SETUP = load_module(
  "bigtop_spark_setup",
  SCRIPTS / "setup_spark.py",
  {"spark_utils": SPARK_UTILS},
)


class TestSparkUtilities(unittest.TestCase):
  def test_bigtop_paths_uris_and_options_fail_closed(self):
    self.assertEqual("3.3.0", SPARK_UTILS.validate_bigtop_stack("BIGTOP", "3.3.0"))
    self.assertEqual(
      "hdfs:///spark-history/",
      SPARK_UTILS.validate_hdfs_uri("hdfs:///spark-history/", "history"),
    )
    self.assertEqual(
      ("--master", "yarn", "--conf", "spark.a=b c"),
      SPARK_UTILS.parse_command_options('--master yarn --conf "spark.a=b c"', "options"),
    )
    with self.assertRaises(Fail):
      SPARK_UTILS.validate_bigtop_stack("OTHER", "3.3.0")
    for value in ("relative", "/tmp/../etc", "/tmp/with space", "/tmp/$(id)"):
      with self.subTest(value=value), self.assertRaises(Fail):
        SPARK_UTILS.validate_absolute_path(value, "path")
    with self.assertRaises(Fail):
      SPARK_UTILS.parse_command_options("'unterminated", "options")
    blocked_options = (
      ("--properties-file=/tmp/other.conf",),
      ("--conf", "spark.kerberos.keytab=/tmp/other.keytab"),
    )
    for options in blocked_options:
      with self.subTest(options=options), self.assertRaises(Fail):
        SPARK_UTILS.validate_thrift_options(options)
    with self.assertRaisesRegex(Fail, "one line"):
      SPARK_UTILS.validate_properties({"spark.master": "yarn\ninjected=true"}, "Spark defaults")


class TestSparkProcess(unittest.TestCase):
  def test_exact_process_contract_and_pid_names(self):
    self.assertEqual(
      HISTORY_TOKENS,
      SPARK_PROCESS.expected_process_tokens("jobhistoryserver", CONF_FILE),
    )
    self.assertEqual(
      THRIFT_TOKENS,
      SPARK_PROCESS.expected_process_tokens("sparkthriftserver", CONF_FILE),
    )
    invalid_pid_files = (
      ("jobhistoryserver", THRIFT_PID_FILE),
      ("sparkthriftserver", PID_FILE),
    )
    for component, pid_file in invalid_pid_files:
      with self.subTest(component=component), self.assertRaises(Fail):
        SPARK_PROCESS.validate_pid_file(component, pid_file)

  def test_pidless_process_is_uniquely_discovered_and_published_0640(self):
    with (
      patch.object(safe_process, "read_pid", return_value=None),
      patch.object(safe_process, "discover_running_process", return_value=IDENTITY) as discover,
      patch.object(safe_process, "create_pid_file_for_identity", return_value=IDENTITY) as create,
    ):
      self.assertIs(
        IDENTITY,
        SPARK_PROCESS.read_or_recover_process(
          "jobhistoryserver", PID_FILE, "spark", "hadoop", CONF_FILE
        ),
      )
    discover.assert_called_once_with("spark", HISTORY_TOKENS)
    create.assert_called_once_with(
      PID_FILE,
      IDENTITY,
      expected_user="spark",
      expected_cmdline=HISTORY_TOKENS,
      owner="spark",
      group="hadoop",
      mode=0o640,
    )

  def test_stop_uses_term_wait_kill_and_removes_only_stopped_identity(self):
    with (
      patch.object(SPARK_PROCESS, "read_or_recover_process", return_value=IDENTITY),
      patch.object(safe_process, "terminate_process") as terminate,
      patch.object(safe_process, "remove_pid_file_if_stopped") as remove,
    ):
      self.assertTrue(
        SPARK_PROCESS.stop_process(
          "jobhistoryserver", PID_FILE, "spark", "hadoop", CONF_FILE
        )
      )
    terminate.assert_called_once_with(
      IDENTITY,
      "spark",
      HISTORY_TOKENS,
      term_wait_attempts=30,
      term_wait_sleep=1,
      kill_wait_attempts=10,
      kill_wait_sleep=1,
    )
    remove.assert_called_once_with(
      PID_FILE,
      123,
      expected_user="spark",
      expected_cmdline=HISTORY_TOKENS,
    )

  def test_stale_pid_is_removed_and_ambiguous_recovery_fails_closed(self):
    with (
      patch.object(safe_process, "read_pid", return_value=321),
      patch.object(safe_process, "read_running_process", return_value=None),
      patch.object(safe_process, "remove_pid_file_if_stopped") as remove,
      patch.object(safe_process, "discover_running_process", return_value=None),
    ):
      self.assertIsNone(
        SPARK_PROCESS.read_or_recover_process(
          "jobhistoryserver", PID_FILE, "spark", "hadoop", CONF_FILE
        )
      )
    remove.assert_called_once_with(
      PID_FILE,
      321,
      expected_user="spark",
      expected_cmdline=HISTORY_TOKENS,
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
      SPARK_PROCESS.read_or_recover_process(
        "jobhistoryserver", PID_FILE, "spark", "hadoop", CONF_FILE
      )


class TestSparkService(unittest.TestCase):
  def test_start_is_idempotent(self):
    params = params_module(
      spark_history_server_pid_file=PID_FILE,
      spark_defaults_file=CONF_FILE,
      spark_user="spark",
      user_group="hadoop",
    )
    with (
      patch.object(SPARK_PROCESS, "read_or_recover_process", return_value=IDENTITY),
      patch.object(SPARK_SERVICE, "Execute") as execute,
    ):
      SPARK_SERVICE._start(params, "jobhistoryserver")
    execute.assert_not_called()

  def test_thrift_start_uses_foreground_structured_submit_contract(self):
    params = params_module(
      spark_thrift_server_pid_file=THRIFT_PID_FILE,
      spark_defaults_file=CONF_FILE,
      spark_user="spark",
      user_group="hadoop",
      security_enabled=False,
      spark_submit="/usr/lib/spark/bin/spark-submit",
      spark_thrift_cmd_opts=("--master", "yarn"),
      hadoop_conf_dir="/etc/hadoop/conf",
      java_home="/usr/lib/jvm/java-17",
      spark_conf_dir="/etc/spark/conf",
      spark_log_dir="/var/log/spark",
    )
    with (
      patch.object(SPARK_PROCESS, "read_or_recover_process", return_value=None),
      patch.object(SPARK_PROCESS, "wait_for_started_process", return_value=IDENTITY),
      patch.object(SPARK_UTILS, "validate_executable"),
      patch.object(SPARK_SERVICE.sudo, "path_lexists", return_value=False),
      patch.object(SPARK_SERVICE, "File"),
      patch.object(SPARK_SERVICE, "Execute") as execute,
    ):
      SPARK_SERVICE._start(params, "sparkthriftserver")
    command = execute.call_args.args[0]
    self.assertEqual("/usr/lib/spark/bin/spark-submit", command[0])
    self.assertIn("org.apache.spark.sql.hive.thriftserver.HiveThriftServer2", command)
    self.assertEqual(("--properties-file", CONF_FILE), command[5:7])
    self.assertFalse(execute.call_args.kwargs["wait_for_finish"])

  def test_start_failure_attempts_identity_safe_cleanup(self):
    params = params_module(
      spark_history_server_pid_file=PID_FILE,
      spark_defaults_file=CONF_FILE,
      spark_user="spark",
      user_group="hadoop",
      security_enabled=False,
      spark_history_dir="hdfs:///spark-history/",
      HdfsResource=MagicMock(),
      spark_class="/usr/lib/spark/bin/spark-class",
      hadoop_conf_dir="/etc/hadoop/conf",
      java_home="/usr/lib/jvm/java-17",
      spark_conf_dir="/etc/spark/conf",
      spark_log_dir="/var/log/spark",
    )
    with (
      patch.object(SPARK_PROCESS, "read_or_recover_process", return_value=None),
      patch.object(
        SPARK_PROCESS,
        "wait_for_started_process",
        side_effect=Fail("ambiguous process discovery"),
      ),
      patch.object(SPARK_PROCESS, "stop_process", return_value=True) as stop,
      patch.object(SPARK_UTILS, "validate_executable"),
      patch.object(SPARK_SERVICE.sudo, "path_lexists", return_value=False),
      patch.object(SPARK_SERVICE, "File"),
      patch.object(SPARK_SERVICE, "Execute"),
      patch.object(SPARK_SERVICE, "show_logs"),
      self.assertRaisesRegex(Fail, "ambiguous"),
    ):
      SPARK_SERVICE._start(params, "jobhistoryserver")
    stop.assert_called_once_with(
      "jobhistoryserver", PID_FILE, "spark", "hadoop", CONF_FILE
    )


class TestSparkConfiguration(unittest.TestCase):
  def test_managed_files_use_log4j2_and_non_executable_restricted_modes(self):
    params = params_module(
      spark_pid_dir="/var/run/spark",
      spark_log_dir="/var/log/spark",
      spark_user="spark",
      user_group="hadoop",
      spark_lib_dir="/var/lib/spark",
      spark_history_store_path="/var/lib/spark/shs_db",
      spark_hdfs_user_dir="/user/spark",
      HdfsResource=MagicMock(),
      spark_defaults={"spark.history.fs.logDirectory": "hdfs:///spark-history/"},
      security_enabled=False,
      spark_warehouse_dir=None,
      is_hive_installed=False,
      spark_conf_dir="/etc/spark/conf",
      spark_env_sh="export SPARK_CONF_DIR={{spark_conf_dir}}",
      spark_log4j2_properties="rootLogger.level = info",
      spark_metrics_properties="*.sink.jmx.class=x",
      spark_thrift_fairscheduler_content="<allocations/>",
    )
    with (
      patch.dict(sys.modules, {"params": params}),
      patch.object(SPARK_SETUP, "Directory"),
      patch.object(SPARK_SETUP, "PropertiesFile") as properties_file,
      patch.object(SPARK_SETUP, "File") as file_resource,
      patch.object(SPARK_SETUP, "generate_logfeeder_input_config"),
    ):
      SPARK_SETUP.setup_spark(MagicMock(), "historyserver", action="config")
    self.assertEqual(0o640, properties_file.call_args.kwargs["mode"])
    file_by_path = {call.args[0]: call.kwargs for call in file_resource.call_args_list}
    self.assertEqual(0o640, file_by_path["/etc/spark/conf/spark-env.sh"]["mode"])
    self.assertEqual(0o644, file_by_path["/etc/spark/conf/log4j2.properties"]["mode"])
    self.assertEqual(0o644, file_by_path["/etc/spark/conf/spark-thrift-fairscheduler.xml"]["mode"])

if __name__ == "__main__":
  unittest.main()
