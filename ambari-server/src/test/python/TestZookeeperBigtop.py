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
from unittest.mock import MagicMock, call, patch

from resource_management.core.exceptions import Fail
from resource_management.libraries.functions import safe_process


ZOOKEEPER = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/ZOOKEEPER"
)
SCRIPTS = ZOOKEEPER / "package/scripts"
PID_FILE = "/var/run/zookeeper/zookeeper_server.pid"
CONFIG_FILE = "/etc/zookeeper/conf/zoo.cfg"
TOKENS = (
  "org.apache.zookeeper.server.quorum.QuorumPeerMain",
  CONFIG_FILE,
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


ZOOKEEPER_UTILS = load_module(
  "bigtop_zookeeper_utils", SCRIPTS / "zookeeper_utils.py"
)
ZOOKEEPER_PROCESS = load_module(
  "bigtop_zookeeper_process",
  SCRIPTS / "zookeeper_process.py",
  {"zookeeper_utils": ZOOKEEPER_UTILS},
)
ZOOKEEPER_CLI = load_module(
  "bigtop_zookeeper_cli",
  SCRIPTS / "zookeeper_cli.py",
  {"zookeeper_utils": ZOOKEEPER_UTILS},
)
ZOOKEEPER_SERVICE = load_module(
  "bigtop_zookeeper_service",
  SCRIPTS / "zookeeper_service.py",
  {
    "zookeeper_process": ZOOKEEPER_PROCESS,
    "zookeeper_utils": ZOOKEEPER_UTILS,
  },
)
ZOOKEEPER_SERVICE_CHECK = load_module(
  "bigtop_zookeeper_service_check",
  SCRIPTS / "service_check.py",
  {
    "zookeeper_cli": ZOOKEEPER_CLI,
    "zookeeper_utils": ZOOKEEPER_UTILS,
  },
)


class TestZookeeperUtils(unittest.TestCase):
  def test_bigtop_identity_boole_paths_and_properties_fail_closed(self):
    self.assertEqual(
      "3.3.0", ZOOKEEPER_UTILS.validate_bigtop_stack("BIGTOP", "3.3.0")
    )
    self.assertTrue(ZOOKEEPER_UTILS.as_bool("true", "flag"))
    self.assertFalse(ZOOKEEPER_UTILS.as_bool(False, "flag"))
    self.assertEqual(
      {"clientPort": "2181", "admin.enableServer": "true"},
      ZOOKEEPER_UTILS.sanitize_zoo_cfg(
        {"clientPort": 2181, "admin.enableServer": True}
      ),
    )

    with self.assertRaises(Fail):
      ZOOKEEPER_UTILS.validate_bigtop_stack("OTHER", "3.3.0")
    for path in ("relative", "/tmp/../etc", "/tmp/with space", "/tmp/$(id)"):
      with self.subTest(path=path), self.assertRaises(Fail):
        ZOOKEEPER_UTILS.validate_absolute_path(path, "path")
    for path in (
      "/",
      "/data",
      "/etc/zookeeper",
      "/home/zookeeper",
      "/var/log",
      "/var/run",
    ):
      with self.subTest(path=path), self.assertRaises(Fail):
        ZOOKEEPER_UTILS.validate_service_directory(path, "directory")
    with self.assertRaisesRegex(Fail, "server entries"):
      ZOOKEEPER_UTILS.sanitize_zoo_cfg({"server.1": "host:2888:3888"})
    with self.assertRaisesRegex(Fail, "control character"):
      ZOOKEEPER_UTILS.sanitize_zoo_cfg({"dataDir": "/data\ninjected=true"})
    with self.assertRaisesRegex(Fail, "must not enable every"):
      ZOOKEEPER_UTILS.sanitize_zoo_cfg({"4lw.commands.whitelist": "*"})
    with self.assertRaisesRegex(Fail, "between 1 and 65535"):
      ZOOKEEPER_UTILS.sanitize_zoo_cfg({"clientPort": "70000"})

  def test_keytabs_and_cli_scripts_must_be_regular_non_symlink_files(self):
    with patch.object(ZOOKEEPER_UTILS.sudo, "path_lexists", return_value=True), \
      patch.object(ZOOKEEPER_UTILS.sudo, "path_isfile", return_value=True), \
      patch.object(ZOOKEEPER_UTILS.sudo, "path_islink", return_value=False), \
      patch.object(
        ZOOKEEPER_UTILS.sudo,
        "stat",
        return_value=SimpleNamespace(st_mode=0o100755),
      ):
      self.assertEqual(
        "/usr/lib/zookeeper/bin/zkCli.sh",
        ZOOKEEPER_UTILS.validate_executable(
          "/usr/lib/zookeeper/bin/zkCli.sh", "CLI"
        ),
      )
      self.assertEqual(
        "/etc/security/keytabs/zk.keytab",
        ZOOKEEPER_UTILS.validate_keytab(
          "/etc/security/keytabs/zk.keytab", "keytab"
        ),
      )

    with patch.object(ZOOKEEPER_UTILS.sudo, "path_lexists", return_value=True), \
      patch.object(ZOOKEEPER_UTILS.sudo, "path_isfile", return_value=True), \
      patch.object(ZOOKEEPER_UTILS.sudo, "path_islink", return_value=True), \
      self.assertRaisesRegex(Fail, "non-symlink"):
      ZOOKEEPER_UTILS.validate_keytab(
        "/etc/security/keytabs/zk.keytab", "keytab"
      )


class TestZookeeperProcess(unittest.TestCase):
  def test_identity_is_exact_upstream_main_class_and_config(self):
    self.assertEqual(TOKENS, ZOOKEEPER_PROCESS.expected_process_tokens(CONFIG_FILE))
    for pid_file in (
      "relative.pid",
      "/zookeeper_server.pid",
      "/tmp/zookeeper_server.pid",
      "/etc/zookeeper_server.pid",
      "/var/run/zookeeper/other.pid",
    ):
      with self.subTest(pid_file=pid_file), self.assertRaises(Fail):
        ZOOKEEPER_PROCESS.validate_pid_file(pid_file)

  def test_pidless_process_is_discovered_and_atomically_published_0640(self):
    with patch.object(safe_process, "read_pid", return_value=None), \
      patch.object(
        safe_process, "discover_running_process", return_value=IDENTITY
      ) as discover, \
      patch.object(
        safe_process,
        "create_pid_file_for_identity",
        return_value=IDENTITY,
      ) as create:
      result = ZOOKEEPER_PROCESS.read_or_recover_process(
        PID_FILE, "zookeeper", "hadoop", CONFIG_FILE
      )

    self.assertIs(IDENTITY, result)
    discover.assert_called_once_with("zookeeper", TOKENS)
    create.assert_called_once_with(
      PID_FILE,
      IDENTITY,
      expected_user="zookeeper",
      expected_cmdline=TOKENS,
      owner="zookeeper",
      group="hadoop",
      mode=0o640,
    )

  def test_stale_pid_is_removed_but_wrong_identity_fails_closed(self):
    with patch.object(safe_process, "read_pid", return_value=123), \
      patch.object(safe_process, "read_running_process", return_value=None), \
      patch.object(
        safe_process, "remove_pid_file_if_stopped", return_value=True
      ) as remove, \
      patch.object(safe_process, "discover_running_process", return_value=None):
      self.assertIsNone(
        ZOOKEEPER_PROCESS.read_or_recover_process(
          PID_FILE, "zookeeper", "hadoop", CONFIG_FILE
        )
      )
    remove.assert_called_once_with(
      PID_FILE,
      123,
      expected_user="zookeeper",
      expected_cmdline=TOKENS,
    )

    with patch.object(safe_process, "read_pid", return_value=123), \
      patch.object(
        safe_process,
        "read_running_process",
        side_effect=Fail("owner mismatch"),
      ), \
      patch.object(safe_process, "remove_pid_file_if_stopped") as remove, \
      self.assertRaisesRegex(Fail, "owner mismatch"):
      ZOOKEEPER_PROCESS.read_or_recover_process(
        PID_FILE, "zookeeper", "hadoop", CONFIG_FILE
      )
    remove.assert_not_called()

    with patch.object(safe_process, "read_pid", return_value=None), \
      patch.object(
        safe_process,
        "discover_running_process",
        side_effect=Fail("ambiguous process discovery"),
      ), \
      patch.object(safe_process, "create_pid_file_for_identity") as create, \
      self.assertRaisesRegex(Fail, "ambiguous process discovery"):
      ZOOKEEPER_PROCESS.read_or_recover_process(
        PID_FILE, "zookeeper", "hadoop", CONFIG_FILE
      )
    create.assert_not_called()

  def test_stop_pins_identity_and_uses_term_wait_kill(self):
    with patch.object(
        ZOOKEEPER_PROCESS, "read_or_recover_process", return_value=IDENTITY
      ), \
      patch.object(safe_process, "terminate_process") as terminate, \
      patch.object(safe_process, "remove_pid_file_if_stopped") as remove:
      self.assertTrue(
        ZOOKEEPER_PROCESS.stop_process(
          PID_FILE, "zookeeper", "hadoop", CONFIG_FILE
        )
      )

    terminate.assert_called_once_with(
      IDENTITY,
      "zookeeper",
      TOKENS,
      term_wait_attempts=30,
      term_wait_sleep=1,
      kill_wait_attempts=10,
      kill_wait_sleep=1,
    )
    remove.assert_called_once_with(
      PID_FILE,
      123,
      expected_user="zookeeper",
      expected_cmdline=TOKENS,
    )

  def test_identity_publication_failure_rolls_back_only_discovered_identity(self):
    start_error = Fail("ZooKeeper PID publication failed")
    with patch.object(
        safe_process, "wait_for_discovered_process", return_value=IDENTITY
      ), \
      patch.object(
        ZOOKEEPER_PROCESS, "_publish_identity", side_effect=start_error
      ), \
      patch.object(
        ZOOKEEPER_PROCESS,
        "rollback_started_process",
        side_effect=Fail("rollback failed"),
      ) as rollback, \
      patch.object(ZOOKEEPER_PROCESS.Logger, "warning") as warning:
      with self.assertRaises(Fail) as raised:
        ZOOKEEPER_PROCESS.wait_for_started_process(
          PID_FILE, "zookeeper", "hadoop", CONFIG_FILE
        )

    self.assertIs(start_error, raised.exception)
    rollback.assert_called_once_with(
      PID_FILE,
      "zookeeper",
      CONFIG_FILE,
      expected_identity=IDENTITY,
    )
    warning.assert_called_once()


class TestZookeeperCli(unittest.TestCase):
  def test_cli_uses_positional_arguments_and_unique_0600_input(self):
    with patch.object(ZOOKEEPER_CLI.zookeeper_utils, "validate_executable"), \
      patch.object(
        ZOOKEEPER_CLI.uuid, "uuid4", return_value=SimpleNamespace(hex="unique")
      ), \
      patch.object(ZOOKEEPER_CLI, "File") as file_resource, \
      patch.object(
        ZOOKEEPER_CLI.shell,
        "checked_call",
        return_value=(0, "Created /check\n"),
      ) as checked_call:
      output = ZOOKEEPER_CLI.run_cli_command(
        "/usr/lib/zookeeper/bin/zkCli.sh",
        "zk1.example.com:2181",
        "create /check data",
        "zookeeper",
        "hadoop",
        "/var/lib/ambari-agent/tmp",
        {"KRB5CCNAME": "FILE:/private/cache"},
      )

    self.assertEqual("Created /check\n", output)
    input_file = "/var/lib/ambari-agent/tmp/ambari-zookeeper-cli-unique.txt"
    file_resource.assert_has_calls(
      [
        call(
          input_file,
          content="create /check data\n",
          owner="zookeeper",
          group="hadoop",
          mode=0o600,
          replace=False,
        ),
        call(input_file, action="delete"),
      ]
    )
    command = checked_call.call_args.args[0]
    self.assertEqual("/bin/bash", command[0])
    self.assertEqual("zk1.example.com:2181", command[-2])
    self.assertEqual(input_file, command[-1])

  def test_cli_rejects_multiline_commands_and_reports_cleanup_failures(self):
    with patch.object(ZOOKEEPER_CLI.zookeeper_utils, "validate_executable"), \
      self.assertRaisesRegex(Fail, "printable line"):
      ZOOKEEPER_CLI.run_cli_command(
        "/usr/lib/zookeeper/bin/zkCli.sh",
        "zk1:2181",
        "get /\nquit",
        "zookeeper",
        "hadoop",
        "/var/lib/ambari-agent/tmp",
        {},
      )

  def test_ensemble_check_uses_unique_node_and_reports_cleanup_failure(self):
    created = "Created /ambari-zookeeper-service-check-unique\n"
    with patch.object(
        ZOOKEEPER_CLI.uuid,
        "uuid4",
        return_value=SimpleNamespace(hex="unique"),
      ), \
      patch.object(
        ZOOKEEPER_CLI,
        "run_cli_command",
        side_effect=(created, "ambari-zookeeper-data-unique\n")
        + ("ambari-zookeeper-data-unique\n",) * 2
        + ("Deleted\n",),
      ) as run_cli:
      ZOOKEEPER_CLI.verify_ensemble(
        ("zk1", "zk2", "zk3"),
        2181,
        "/usr/lib/zookeeper/bin/zkCli.sh",
        "zookeeper",
        "hadoop",
        "/var/lib/ambari-agent/tmp",
        {},
      )

    self.assertEqual(5, run_cli.call_count)
    self.assertEqual(
      "create /ambari-zookeeper-service-check-unique "
      "ambari-zookeeper-data-unique",
      run_cli.call_args_list[0].args[2],
    )
    self.assertEqual(
      "delete /ambari-zookeeper-service-check-unique",
      run_cli.call_args_list[-1].args[2],
    )

    with patch.object(
        ZOOKEEPER_CLI,
        "run_cli_command",
        side_effect=(Fail("create failed"), Fail("delete failed")),
      ), \
      self.assertRaisesRegex(Fail, "create failed.*delete failed"):
      ZOOKEEPER_CLI.verify_ensemble(
        ("zk1",),
        2181,
        "/usr/lib/zookeeper/bin/zkCli.sh",
        "zookeeper",
        "hadoop",
        "/var/lib/ambari-agent/tmp",
        {},
      )

  def test_cli_reports_input_cleanup_failure_with_operation_error(self):
    with patch.object(ZOOKEEPER_CLI.zookeeper_utils, "validate_executable"), \
      patch.object(
        ZOOKEEPER_CLI, "File", side_effect=(None, RuntimeError("cleanup failed"))
      ), \
      patch.object(
        ZOOKEEPER_CLI.shell,
        "checked_call",
        side_effect=Fail("connection failed"),
      ), \
      self.assertRaisesRegex(Fail, "connection failed.*cleanup failed"):
      ZOOKEEPER_CLI.run_cli_command(
        "/usr/lib/zookeeper/bin/zkCli.sh",
        "zk1:2181",
        "get /check",
        "zookeeper",
        "hadoop",
        "/var/lib/ambari-agent/tmp",
        {},
      )


class TestZookeeperService(unittest.TestCase):
  def test_start_is_idempotent_and_uses_packaged_foreground_contract(self):
    params = params_module(
      zk_pid_file=PID_FILE,
      zk_user="zookeeper",
      user_group="hadoop",
      zk_config_file=CONFIG_FILE,
      security_enabled=False,
      zk_server_script="/usr/lib/zookeeper/bin/zkServer.sh",
      java64_home="/usr/lib/jvm/java-17",
      config_dir="/etc/zookeeper/conf",
      zk_log_dir="/var/log/zookeeper",
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        ZOOKEEPER_SERVICE.zookeeper_process,
        "read_or_recover_process",
        return_value=IDENTITY,
      ), \
      patch.object(ZOOKEEPER_SERVICE, "Execute") as execute:
      ZOOKEEPER_SERVICE.zookeeper_service("start")
    execute.assert_not_called()

    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        ZOOKEEPER_SERVICE.zookeeper_process,
        "read_or_recover_process",
        return_value=None,
      ), \
      patch.object(ZOOKEEPER_SERVICE.zookeeper_utils, "validate_executable"), \
      patch.object(ZOOKEEPER_SERVICE, "Execute") as execute, \
      patch.object(
        ZOOKEEPER_SERVICE.zookeeper_process,
        "wait_for_started_process",
        return_value=IDENTITY,
      ) as wait:
      ZOOKEEPER_SERVICE.zookeeper_service("start")

    execute.assert_called_once_with(
      ("/usr/lib/zookeeper/bin/zkServer.sh", "start-foreground"),
      user="zookeeper",
      environment={
        "JAVA_HOME": "/usr/lib/jvm/java-17",
        "ZOOCFGDIR": "/etc/zookeeper/conf",
        "ZOOCFG": "zoo.cfg",
      },
      wait_for_finish=False,
    )
    wait.assert_called_once_with(
      PID_FILE, "zookeeper", "hadoop", CONFIG_FILE
    )

    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        ZOOKEEPER_SERVICE.zookeeper_process,
        "read_or_recover_process",
        return_value=None,
      ), \
      patch.object(ZOOKEEPER_SERVICE.zookeeper_utils, "validate_executable"), \
      patch.object(
        ZOOKEEPER_SERVICE, "Execute", side_effect=Fail("launcher failed")
      ), \
      patch.object(ZOOKEEPER_SERVICE, "show_logs") as show_logs, \
      self.assertRaisesRegex(Fail, "launcher failed"):
      ZOOKEEPER_SERVICE.zookeeper_service("start")
    show_logs.assert_called_once_with("/var/log/zookeeper", "zookeeper")

  def test_secure_service_check_uses_private_cache_and_all_hosts(self):
    params = params_module(
      security_enabled=True,
      smoke_user_keytab="/etc/security/keytabs/smoke.keytab",
      smokeuser="ambari-qa",
      user_group="hadoop",
      tmp_dir="/var/lib/ambari-agent/tmp",
      java64_home="/usr/lib/jvm/java-17",
      config_dir="/etc/zookeeper/conf",
      kinit_path_local="/usr/bin/kinit",
      smokeuser_principal="ambari-qa@EXAMPLE.COM",
      zookeeper_hosts=("zk1", "zk2", "zk3"),
      client_port=2181,
      zk_cli_script="/usr/lib/zookeeper/bin/zkCli.sh",
    )
    kerberos_cache = MagicMock()
    kerberos_cache.merge_environment.return_value = {
      "JAVA_HOME": "/usr/lib/jvm/java-17",
      "KRB5CCNAME": "FILE:/private/cache",
    }
    cache_context = MagicMock()
    cache_context.__enter__.return_value = kerberos_cache
    service_check = object.__new__(
      ZOOKEEPER_SERVICE_CHECK.ZookeeperServiceCheckLinux
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        ZOOKEEPER_SERVICE_CHECK.zookeeper_utils, "validate_keytab"
      ) as validate_keytab, \
      patch.object(
        ZOOKEEPER_SERVICE_CHECK,
        "PrivateKerberosCache",
        return_value=cache_context,
      ) as cache_factory, \
      patch.object(
        ZOOKEEPER_SERVICE_CHECK.zookeeper_cli, "verify_ensemble"
      ) as verify:
      service_check.service_check(MagicMock())

    validate_keytab.assert_called_once_with(
      "/etc/security/keytabs/smoke.keytab", "ZooKeeper service-check keytab"
    )
    cache_factory.assert_called_once_with(
      "ambari-qa",
      "hadoop",
      temp_dir="/var/lib/ambari-agent/tmp",
      prefix="ambari-zookeeper-service-check-",
    )
    kerberos_cache.kinit.assert_called_once_with(
      "/usr/bin/kinit",
      "/etc/security/keytabs/smoke.keytab",
      "ambari-qa@EXAMPLE.COM",
      timeout=60,
    )
    self.assertEqual(("zk1", "zk2", "zk3"), verify.call_args.args[0])


if __name__ == "__main__":
  unittest.main()
