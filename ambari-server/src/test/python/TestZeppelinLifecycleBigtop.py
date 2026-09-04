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
from resource_management.core.logger import Logger


ZEPPELIN = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/ZEPPELIN"
)
SCRIPTS = ZEPPELIN / "package/scripts"

Logger.initialize_logger()


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


PROCESS = load_module("bigtop_zeppelin_process", SCRIPTS / "zeppelin_process.py")
SERVER = load_module(
  "bigtop_zeppelin_server",
  SCRIPTS / "zeppelin_server.py",
  {"zeppelin_process": PROCESS},
)


class TestZeppelinProcessLifecycle(unittest.TestCase):
  def setUp(self):
    self.pid_file = "/var/run/zeppelin/zeppelin-zeppelin.pid"
    self.identity = SimpleNamespace(pid=4123)

  def test_existing_pid_requires_exact_running_identity(self):
    with patch.object(PROCESS.safe_process, "read_pid", return_value=4123), \
      patch.object(
        PROCESS.safe_process, "inspect_process", return_value=self.identity
      ) as inspect, \
      patch.object(PROCESS.safe_process, "is_process_running", return_value=True), \
      patch.object(
        PROCESS.safe_process,
        "publish_pid_file_for_identity",
        return_value=self.identity,
      ) as publish_pid, \
      patch.object(PROCESS.safe_process, "discover_running_process") as discover:
      result = PROCESS.read_or_discover_zeppelin_process(
        self.pid_file, "zeppelin", "zeppelin"
      )

    self.assertIs(self.identity, result)
    inspect.assert_called_once_with(
      4123, "zeppelin", PROCESS.ZEPPELIN_PROCESS_TOKENS
    )
    publish_pid.assert_called_once_with(
      self.pid_file,
      self.identity,
      "zeppelin",
      PROCESS.ZEPPELIN_PROCESS_TOKENS,
      owner="zeppelin",
      group="zeppelin",
      mode=0o640,
    )
    discover.assert_not_called()

  def test_stale_pid_is_removed_before_unique_process_recovery(self):
    recovered = SimpleNamespace(pid=4124)
    with patch.object(PROCESS.safe_process, "read_pid", return_value=4123), \
      patch.object(PROCESS.safe_process, "inspect_process", return_value=None), \
      patch.object(
        PROCESS.safe_process, "remove_pid_file_if_stopped"
      ) as remove_pid, \
      patch.object(
        PROCESS.safe_process, "discover_running_process", return_value=recovered
      ), \
      patch.object(
        PROCESS.safe_process,
        "publish_pid_file_for_identity",
        return_value=recovered,
      ) as create_pid:
      result = PROCESS.read_or_discover_zeppelin_process(
        self.pid_file, "zeppelin", "zeppelin"
      )

    self.assertIs(recovered, result)
    remove_pid.assert_called_once_with(
      self.pid_file,
      4123,
      expected_user="zeppelin",
      expected_cmdline=PROCESS.ZEPPELIN_PROCESS_TOKENS,
    )
    self.assertEqual(0o640, create_pid.call_args.kwargs["mode"])

  def test_pid_for_wrong_process_fails_closed(self):
    mismatch = Fail("command line does not match")
    with patch.object(PROCESS.safe_process, "read_pid", return_value=4123), \
      patch.object(
        PROCESS.safe_process, "inspect_process", side_effect=mismatch
      ), \
      patch.object(PROCESS.safe_process, "discover_running_process") as discover:
      with self.assertRaises(Fail) as raised:
        PROCESS.read_or_discover_zeppelin_process(
          self.pid_file, "zeppelin", "zeppelin"
        )

    self.assertIs(mismatch, raised.exception)
    discover.assert_not_called()

  def test_start_uses_official_argv_and_waits_for_exact_process(self):
    with patch.object(
        PROCESS, "read_or_discover_zeppelin_process", return_value=None
      ), \
      patch.object(PROCESS, "Execute") as execute, \
      patch.object(
        PROCESS, "wait_for_zeppelin_process", return_value=self.identity
      ) as wait:
      result = PROCESS.start_zeppelin(
        "/usr/lib/zeppelin/bin/zeppelin-daemon.sh;$(id)",
        "/etc/zeppelin/conf;$(id)",
        self.pid_file,
        "zeppelin",
        "zeppelin",
        "/usr/lib/jvm/java;$(id)",
      )

    self.assertIs(self.identity, result)
    execute.assert_called_once_with(
      (
        "/usr/lib/zeppelin/bin/zeppelin-daemon.sh;$(id)",
        "--config",
        "/etc/zeppelin/conf;$(id)",
        "start",
      ),
      user="zeppelin",
      environment={"JAVA_HOME": "/usr/lib/jvm/java;$(id)"},
      timeout=60,
      timeout_kill_strategy=PROCESS.TerminateStrategy.KILL_PROCESS_GROUP,
      logoutput=True,
    )
    wait.assert_called_once_with(self.pid_file, "zeppelin", "zeppelin")

  def test_stop_terminates_only_verified_identity(self):
    with patch.object(
        PROCESS,
        "read_or_discover_zeppelin_process",
        return_value=self.identity,
      ), \
      patch.object(PROCESS.safe_process, "terminate_process") as terminate, \
      patch.object(
        PROCESS.safe_process, "remove_pid_file_if_stopped"
      ) as remove_pid:
      PROCESS.stop_zeppelin(self.pid_file, "zeppelin", "zeppelin")

    terminate.assert_called_once_with(
      self.identity, "zeppelin", PROCESS.ZEPPELIN_PROCESS_TOKENS
    )
    remove_pid.assert_called_once_with(
      self.pid_file,
      4123,
      expected_user="zeppelin",
      expected_cmdline=PROCESS.ZEPPELIN_PROCESS_TOKENS,
    )

  def test_status_recovers_missing_pid_and_reports_stopped(self):
    status_params = params_module(
      zeppelin_pid_file=self.pid_file,
      zeppelin_user="zeppelin",
      zeppelin_group="zeppelin",
    )
    with patch.dict(sys.modules, {"status_params": status_params}), \
      patch.object(SERVER, "read_or_discover_zeppelin_process", return_value=None):
      with self.assertRaises(ComponentIsNotRunning):
        SERVER.ZeppelinServer().status(MagicMock())


class TestZeppelinHdfsOperations(unittest.TestCase):
  def _params(self, secure=False):
    return params_module(
      security_enabled=secure,
      kinit_path_local="/usr/bin/kinit;$(id)",
      zeppelin_kerberos_keytab="/etc/security/key tabs/zeppelin;$(id)",
      zeppelin_kerberos_principal="zeppelin@REALM;$(id)",
      zeppelin_group="zeppelin",
      hadoop_bin_dir="/usr/bin;$(id)",
      hadoop_conf_dir="/etc/hadoop/conf;$(id)",
      zeppelin_user="zeppelin",
      conf_stored_in_hdfs=True,
      zeppelin_conf_dir="/etc/zeppelin/conf",
    )

  def test_hdfs_probe_preserves_argument_boundaries(self):
    params = self._params()
    server = SERVER.ZeppelinServer()
    with patch.object(SERVER.shell, "call", return_value=(0, "")) as call:
      result = server.call_hdfs(
        params, ("-test", "-d", "/path;$(id)"), "zeppelin"
      )

    self.assertEqual((0, ""), result)
    call.assert_called_once_with(
      (
        "/usr/bin;$(id)/hdfs",
        "--config",
        "/etc/hadoop/conf;$(id)",
        "dfs",
        "-test",
        "-d",
        "/path;$(id)",
      ),
      user="zeppelin",
      env=None,
      timeout=30,
      timeout_kill_strategy=SERVER.TerminateStrategy.KILL_PROCESS_GROUP,
    )

  def test_secure_hdfs_probe_uses_private_cache(self):
    params = self._params(secure=True)
    cache = MagicMock()
    cache.environment = {"KRB5CCNAME": "FILE:/tmp/zeppelin-hdfs/krb5cc"}
    context = MagicMock()
    context.__enter__.return_value = cache
    server = SERVER.ZeppelinServer()
    with patch.object(SERVER, "PrivateKerberosCache", return_value=context), \
      patch.object(SERVER.shell, "call", return_value=(0, "")) as call:
      server.call_hdfs(params, ("-test", "-f", "/config"), "zeppelin")

    cache.kinit.assert_called_once_with(
      "/usr/bin/kinit;$(id)",
      "/etc/security/key tabs/zeppelin;$(id)",
      "zeppelin@REALM;$(id)",
      timeout=30,
    )
    self.assertEqual(cache.environment, call.call_args.kwargs["env"])

  def test_interpreter_load_rejects_failed_cat_and_protects_file(self):
    params = self._params()
    server = SERVER.ZeppelinServer()
    with patch.object(server, "get_zeppelin_conf_fs", return_value="/config/interpreter.json"), \
      patch.object(server, "is_nonempty_hdfs_file", return_value=True), \
      patch.object(server, "call_hdfs", return_value=(1, "failure")), \
      patch.object(SERVER, "File") as file_resource:
      with self.assertRaisesRegex(Fail, "Could not read"):
        server.load_interpreter_from_hdfs(params)
    file_resource.assert_not_called()


if __name__ == "__main__":
  unittest.main()
