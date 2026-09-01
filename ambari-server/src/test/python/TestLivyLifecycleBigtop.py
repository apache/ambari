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


LIVY = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/LIVY"
)
SCRIPTS = LIVY / "package/scripts"


def load_module(module_name, path, dependencies=None):
  spec = importlib.util.spec_from_file_location(module_name, path)
  module = importlib.util.module_from_spec(spec)
  with patch.dict(sys.modules, dependencies or {}):
    spec.loader.exec_module(module)
  return module


def dependency_module(name, **attributes):
  module = ModuleType(name)
  for attribute, value in attributes.items():
    setattr(module, attribute, value)
  return module


LIVY_SERVICE = load_module("bigtop_livy_service", SCRIPTS / "livy_service.py")
SETUP_STUB = dependency_module("setup_livy", setup_livy=MagicMock())
LIVY_SERVER = load_module(
  "bigtop_livy_server",
  SCRIPTS / "livy_server.py",
  {
    "livy_service": LIVY_SERVICE,
    "setup_livy": SETUP_STUB,
  },
)


def params_module(**values):
  return dependency_module("params", **values)


class TestLivyProcessLifecycle(unittest.TestCase):
  def setUp(self):
    self.identity = SimpleNamespace(pid=4217)
    self.pid_file = "/var/run/livy/livy-livy-server.pid"

  def test_existing_pid_requires_exact_running_identity(self):
    with patch.object(LIVY_SERVICE.safe_process, "read_pid", return_value=4217), \
      patch.object(
        LIVY_SERVICE.safe_process,
        "inspect_process",
        return_value=self.identity,
      ) as inspect_process, \
      patch.object(
        LIVY_SERVICE.safe_process,
        "is_process_running",
        return_value=True,
      ), \
      patch.object(
        LIVY_SERVICE.safe_process, "discover_running_process"
      ) as discover:
      result = LIVY_SERVICE.read_or_discover_livy_process(
        self.pid_file, "livy", "livy"
      )

    self.assertIs(self.identity, result)
    inspect_process.assert_called_once_with(
      4217, "livy", LIVY_SERVICE.LIVY_SERVER_PROCESS_TOKENS
    )
    discover.assert_not_called()

  def test_stale_pid_is_removed_before_unique_process_recovery(self):
    recovered = SimpleNamespace(pid=4220)
    with patch.object(LIVY_SERVICE.safe_process, "read_pid", return_value=4217), \
      patch.object(LIVY_SERVICE.safe_process, "inspect_process", return_value=None), \
      patch.object(
        LIVY_SERVICE.safe_process, "remove_pid_file_if_stopped"
      ) as remove_pid, \
      patch.object(
        LIVY_SERVICE.safe_process,
        "discover_running_process",
        return_value=recovered,
      ) as discover, \
      patch.object(
        LIVY_SERVICE.safe_process,
        "create_pid_file_for_identity",
        return_value=recovered,
      ) as create_pid:
      result = LIVY_SERVICE.read_or_discover_livy_process(
        self.pid_file, "livy", "livy"
      )

    self.assertIs(recovered, result)
    remove_pid.assert_called_once_with(
      self.pid_file,
      4217,
      expected_user="livy",
      expected_cmdline=LIVY_SERVICE.LIVY_SERVER_PROCESS_TOKENS,
    )
    discover.assert_called_once_with(
      "livy", LIVY_SERVICE.LIVY_SERVER_PROCESS_TOKENS
    )
    create_pid.assert_called_once_with(
      self.pid_file,
      recovered,
      "livy",
      LIVY_SERVICE.LIVY_SERVER_PROCESS_TOKENS,
      owner="livy",
      group="livy",
      mode=0o640,
    )

  def test_wrong_process_in_pid_file_fails_closed(self):
    mismatch = Fail("command line does not match")
    with patch.object(LIVY_SERVICE.safe_process, "read_pid", return_value=4217), \
      patch.object(
        LIVY_SERVICE.safe_process,
        "inspect_process",
        side_effect=mismatch,
      ), \
      patch.object(
        LIVY_SERVICE.safe_process, "remove_pid_file_if_stopped"
      ) as remove_pid, \
      patch.object(
        LIVY_SERVICE.safe_process, "discover_running_process"
      ) as discover:
      with self.assertRaises(Fail) as raised:
        LIVY_SERVICE.read_or_discover_livy_process(
          self.pid_file, "livy", "livy"
        )

    self.assertIs(mismatch, raised.exception)
    remove_pid.assert_not_called()
    discover.assert_not_called()

  def test_missing_pid_is_recovered_only_from_unique_exact_process(self):
    with patch.object(LIVY_SERVICE.safe_process, "read_pid", return_value=None), \
      patch.object(
        LIVY_SERVICE.safe_process,
        "discover_running_process",
        return_value=self.identity,
      ), \
      patch.object(
        LIVY_SERVICE.safe_process,
        "create_pid_file_for_identity",
        return_value=self.identity,
      ) as create_pid:
      result = LIVY_SERVICE.read_or_discover_livy_process(
        self.pid_file, "livy", "livy"
      )

    self.assertIs(self.identity, result)
    self.assertEqual(0o640, create_pid.call_args.kwargs["mode"])

  def _service_params(self):
    return params_module(
      livy_server_pid_file=self.pid_file,
      livy_user="livy",
      livy_group="livy",
      livy_server_command="/usr/lib/livy/bin/livy-server;$(id)",
      java_home="/usr/lib/jvm/java;$(id)",
      security_enabled=False,
      livy_server_kerberos_cache_file="/var/run/livy/.livy-server-krb5cc",
    )

  def test_start_is_idempotent_for_valid_identity(self):
    params = self._service_params()
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        LIVY_SERVICE,
        "read_or_discover_livy_process",
        return_value=self.identity,
      ), \
      patch.object(LIVY_SERVICE, "Execute") as execute, \
      patch.object(LIVY_SERVICE, "File") as file_resource, \
      patch.object(LIVY_SERVICE, "wait_for_livy_process") as wait:
      LIVY_SERVICE.livy_service("server", action="start")

    execute.assert_not_called()
    file_resource.assert_not_called()
    wait.assert_not_called()

  def test_start_uses_structured_official_command_and_validates_result(self):
    params = self._service_params()
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        LIVY_SERVICE, "read_or_discover_livy_process", return_value=None
      ), \
      patch.object(LIVY_SERVICE, "Execute") as execute, \
      patch.object(LIVY_SERVICE, "File") as file_resource, \
      patch.object(LIVY_SERVICE, "wait_for_livy_process") as wait:
      LIVY_SERVICE.livy_service("server", action="start")

    execute.assert_called_once_with(
      ("/usr/lib/livy/bin/livy-server;$(id)", "start"),
      user="livy",
      environment={"JAVA_HOME": "/usr/lib/jvm/java;$(id)"},
      logoutput=True,
    )
    file_resource.assert_called_once_with(
      "/var/run/livy/.livy-server-krb5cc", action="delete"
    )
    wait.assert_called_once_with(self.pid_file, "livy", "livy")

  def test_secure_start_gives_livy_periodic_kinit_a_dedicated_cache(self):
    params = self._service_params()
    params.security_enabled = True
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        LIVY_SERVICE, "read_or_discover_livy_process", return_value=None
      ), \
      patch.object(LIVY_SERVICE, "Execute") as execute, \
      patch.object(LIVY_SERVICE, "File"), \
      patch.object(LIVY_SERVICE, "wait_for_livy_process"):
      LIVY_SERVICE.livy_service("server", action="start")

    self.assertEqual(
      {
        "JAVA_HOME": "/usr/lib/jvm/java;$(id)",
        "KRB5CCNAME": "FILE:/var/run/livy/.livy-server-krb5cc",
      },
      execute.call_args.kwargs["environment"],
    )

  def test_start_rejects_missing_secure_cache_path(self):
    params = self._service_params()
    params.security_enabled = True
    params.livy_server_kerberos_cache_file = ""
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        LIVY_SERVICE, "read_or_discover_livy_process", return_value=None
      ), \
      patch.object(LIVY_SERVICE, "Execute") as execute:
      with self.assertRaisesRegex(Fail, "private Kerberos cache path"):
        LIVY_SERVICE.livy_service("server", action="start")

    execute.assert_not_called()

  def test_stop_terminates_pinned_identity_and_then_removes_pid(self):
    params = self._service_params()
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        LIVY_SERVICE,
        "read_or_discover_livy_process",
        return_value=self.identity,
      ), \
      patch.object(
        LIVY_SERVICE.safe_process, "terminate_process"
      ) as terminate, \
      patch.object(
        LIVY_SERVICE.safe_process, "remove_pid_file_if_stopped"
      ) as remove_pid, \
      patch.object(LIVY_SERVICE, "File") as file_resource:
      LIVY_SERVICE.livy_service("server", action="stop")

    terminate.assert_called_once_with(
      self.identity,
      "livy",
      LIVY_SERVICE.LIVY_SERVER_PROCESS_TOKENS,
    )
    remove_pid.assert_called_once_with(
      self.pid_file,
      4217,
      expected_user="livy",
      expected_cmdline=LIVY_SERVICE.LIVY_SERVER_PROCESS_TOKENS,
    )
    file_resource.assert_called_once_with(
      "/var/run/livy/.livy-server-krb5cc", action="delete"
    )

  def test_stop_failure_preserves_pid_and_server_cache(self):
    params = self._service_params()
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        LIVY_SERVICE,
        "read_or_discover_livy_process",
        return_value=self.identity,
      ), \
      patch.object(
        LIVY_SERVICE.safe_process,
        "terminate_process",
        side_effect=Fail("process did not stop"),
      ), \
      patch.object(
        LIVY_SERVICE.safe_process, "remove_pid_file_if_stopped"
      ) as remove_pid, \
      patch.object(LIVY_SERVICE, "File") as file_resource:
      with self.assertRaisesRegex(Fail, "process did not stop"):
        LIVY_SERVICE.livy_service("server", action="stop")

    remove_pid.assert_not_called()
    file_resource.assert_not_called()

  def test_stop_without_process_removes_leftover_kerberos_cache(self):
    params = self._service_params()
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        LIVY_SERVICE, "read_or_discover_livy_process", return_value=None
      ), \
      patch.object(LIVY_SERVICE, "File") as file_resource, \
      patch.object(LIVY_SERVICE.safe_process, "terminate_process") as terminate:
      LIVY_SERVICE.livy_service("server", action="stop")

    terminate.assert_not_called()
    file_resource.assert_called_once_with(
      "/var/run/livy/.livy-server-krb5cc", action="delete"
    )

  def test_wait_retries_until_exact_process_is_recovered(self):
    with patch.object(
        LIVY_SERVICE,
        "read_or_discover_livy_process",
        side_effect=(None, self.identity),
      ), \
      patch.object(LIVY_SERVICE.time, "sleep") as sleep:
      result = LIVY_SERVICE.wait_for_livy_process(
        self.pid_file, "livy", "livy", attempts=2
      )

    self.assertIs(self.identity, result)
    sleep.assert_called_once_with(1)

  def test_wait_fails_after_bounded_attempts(self):
    with patch.object(
        LIVY_SERVICE,
        "read_or_discover_livy_process",
        return_value=None,
      ), \
      patch.object(LIVY_SERVICE.time, "sleep") as sleep:
      with self.assertRaisesRegex(Fail, "valid process identity"):
        LIVY_SERVICE.wait_for_livy_process(
          self.pid_file, "livy", "livy", attempts=2, sleep_seconds=0
        )

    sleep.assert_called_once_with(0)

  def test_service_rejects_unknown_name_and_action(self):
    params = self._service_params()
    with patch.dict(sys.modules, {"params": params}):
      with self.assertRaisesRegex(Fail, "Unsupported Livy service name"):
        LIVY_SERVICE.livy_service("client", action="start")
      with patch.object(
          LIVY_SERVICE,
          "read_or_discover_livy_process",
          return_value=None,
        ), \
        self.assertRaisesRegex(Fail, "Unsupported Livy service action"):
        LIVY_SERVICE.livy_service("server", action="restart")

  def test_status_recovers_missing_pid_and_reports_stopped(self):
    status_params = dependency_module(
      "status_params",
      livy_server_pid_file=self.pid_file,
      livy_user="livy",
      livy_group="livy",
    )
    server = LIVY_SERVER.LivyServer()
    env = MagicMock()
    with patch.dict(sys.modules, {"status_params": status_params}), \
      patch.object(
        LIVY_SERVER, "read_or_discover_livy_process", return_value=None
      ):
      with self.assertRaises(ComponentIsNotRunning):
        server.status(env)


class TestLivyKerberosAndDfsLifecycle(unittest.TestCase):
  def test_secure_dfs_check_uses_structured_kinit_and_private_cache(self):
    cache = MagicMock()
    cache.cache_name = "FILE:/tmp/livy-cache/krb5cc"
    cache.environment = {"KRB5CCNAME": cache.cache_name}
    cache_context = MagicMock()
    cache_context.__enter__.return_value = cache
    params = params_module(
      hdfs_resource_ignore_file="/var/lib/ambari-agent/data/ignore",
      security_enabled=True,
      livy_kerberos_keytab="/etc/security/key tabs/livy;$(id)",
      livy_principal="livy/host@REALM;$(id)",
      livy_user="livy",
      livy_group="livy",
      kinit_path_local="/usr/bin/kinit;$(id)",
    )
    server = LIVY_SERVER.LivyServer()
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        LIVY_SERVER.HdfsResourceProvider,
        "get_ignored_resources_list",
        return_value=[],
      ), \
      patch.object(
        LIVY_SERVER, "PrivateKerberosCache", return_value=cache_context
      ), \
      patch.object(server, "wait_for_dfs_directory_created") as wait:
      server.wait_for_dfs_directories_created(["/ats/done", "/ats/active"])

    cache.kinit.assert_called_once_with(
      "/usr/bin/kinit;$(id)",
      "/etc/security/key tabs/livy;$(id)",
      "livy/host@REALM;$(id)",
      timeout=30,
    )
    self.assertEqual(2, wait.call_count)
    self.assertEqual(
      {"KRB5CCNAME": "FILE:/tmp/livy-cache/krb5cc"},
      wait.call_args.kwargs["kerberos_environment"],
    )

  def test_dfs_probe_uses_structured_hdfs_argv(self):
    params = params_module(
      hadoop_bin_dir="/usr/bin;$(id)",
      hadoop_conf_dir="/etc/hadoop/conf;$(id)",
      livy_user="livy",
    )
    server = LIVY_SERVER.LivyServer()
    kerberos_environment = {"KRB5CCNAME": "FILE:/tmp/cache"}
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        LIVY_SERVER.HdfsResourceProvider,
        "parse_path",
        return_value="/ats/path;$(id)",
      ), \
      patch.object(LIVY_SERVER.shell, "call", return_value=(0, "")) as shell_call:
      server.wait_for_dfs_directory_created(
        "/ats/path;$(id)", [], kerberos_environment
      )

    shell_call.assert_called_once_with(
      (
        "/usr/bin;$(id)/hdfs",
        "--config",
        "/etc/hadoop/conf;$(id)",
        "dfs",
        "-test",
        "-d",
        "/ats/path;$(id)",
      ),
      user="livy",
      env=kerberos_environment,
      timeout=30,
    )


if __name__ == "__main__":
  unittest.main()
