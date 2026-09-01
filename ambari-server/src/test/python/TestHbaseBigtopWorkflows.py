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
import socket
import sys
from types import ModuleType, SimpleNamespace
import unittest
from unittest.mock import MagicMock, call, patch

from resource_management.core.exceptions import Fail


HBASE = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/HBASE"
)
SCRIPTS = HBASE / "package/scripts"


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


HBASE_DECOMMISSION = load_module(
  "bigtop_hbase_decommission", SCRIPTS / "hbase_decommission.py"
)
SERVICE_CHECK = load_module(
  "bigtop_hbase_service_check", SCRIPTS / "service_check.py"
)
HBASE_SERVICE_STUB = dependency_module(
  "hbase_service", read_or_discover_hbase_process=MagicMock()
)
UPGRADE = load_module(
  "bigtop_hbase_upgrade",
  SCRIPTS / "upgrade.py",
  {"hbase_service": HBASE_SERVICE_STUB},
)


class TestHbaseDecommissionWorkflow(unittest.TestCase):
  def _params(self, security_enabled=False):
    return SimpleNamespace(
      hbase_cmd="/usr/lib/hbase/bin/hbase",
      hbase_conf_dir="/etc/hbase/conf",
      hbase_user="hbase",
      user_group="hadoop",
      hbase_region_mover_timeout=540,
      security_enabled=security_enabled,
      kinit_path_local="/usr/bin/kinit",
      master_keytab_path="/etc/security/keytabs/hbase.service.keytab",
      master_jaas_princ="hbase/master.example.com@EXAMPLE.COM",
      exec_tmp_dir="/var/lib/ambari-agent/tmp",
    )

  def test_hosts_are_trimmed_deduplicated_and_required(self):
    params = SimpleNamespace(
      hbase_excluded_hosts="rs1.example.com, rs2.example.com,rs1.example.com",
      hbase_included_hosts="",
    )
    self.assertEqual(
      ["rs1.example.com", "rs2.example.com"],
      HBASE_DECOMMISSION._hosts(params),
    )
    params.hbase_excluded_hosts = ""
    with self.assertRaises(Fail):
      HBASE_DECOMMISSION._hosts(params)

  def test_shell_commands_use_official_hbase_2_6_apis_and_safe_quoting(self):
    hosts = ["rs1.example.com", "rs'2\\node\n.example.com"]
    self.assertEqual(
      "decommission_regionservers ['rs1.example.com', "
      "'rs\\'2\\\\node\\n.example.com'], false\nexit\n",
      HBASE_DECOMMISSION._shell_command(None, hosts, False),
    )
    self.assertEqual(
      "recommission_regionserver 'rs1.example.com'\nexit\n",
      HBASE_DECOMMISSION._shell_command(None, hosts[:1], True),
    )

  def test_region_mover_uses_structured_upstream_java_command(self):
    params = self._params()
    environment = {"KRB5CCNAME": "FILE:/tmp/hbase-cache"}
    with patch.object(HBASE_DECOMMISSION, "Execute") as execute:
      HBASE_DECOMMISSION._move_regions(
        params, "rs1.example.com", "unload", environment
      )

    execute.assert_called_once_with(
      (
        "/usr/lib/hbase/bin/hbase",
        "--config",
        "/etc/hbase/conf",
        "org.apache.hadoop.hbase.util.RegionMover",
        "--maxthreads",
        "24",
        "--operation",
        "unload",
        "--regionserverhost",
        "rs1.example.com",
      ),
      user="hbase",
      environment=environment,
      logoutput=True,
      timeout=540,
    )
    with self.assertRaises(Fail):
      HBASE_DECOMMISSION._move_regions(params, "rs1", "delete")

  def test_secure_admin_operation_uses_unique_private_cache(self):
    params = self._params(security_enabled=True)
    operation = MagicMock(return_value="completed")
    cache = MagicMock()
    cache.environment = {"KRB5CCNAME": "FILE:/tmp/cache"}
    cache_factory = MagicMock()
    cache_factory.return_value.__enter__.return_value = cache

    with patch.object(
      HBASE_DECOMMISSION, "PrivateKerberosCache", cache_factory
    ):
      result = HBASE_DECOMMISSION._with_kerberos(
        params, "ambari-hbase-test-", operation
      )

    self.assertEqual("completed", result)
    cache_factory.assert_called_once_with(
      "hbase",
      "hadoop",
      temp_dir="/var/lib/ambari-agent/tmp",
      prefix="ambari-hbase-test-",
    )
    cache.kinit.assert_called_once_with(
      "/usr/bin/kinit",
      "/etc/security/keytabs/hbase.service.keytab",
      "hbase/master.example.com@EXAMPLE.COM",
      timeout=30,
    )
    operation.assert_called_once_with(cache.environment)
    cache_factory.return_value.__exit__.assert_called_once()

  def test_secure_admin_operation_rejects_missing_credentials(self):
    params = self._params(security_enabled=True)
    params.master_keytab_path = ""
    operation = MagicMock()
    with self.assertRaises(Fail):
      HBASE_DECOMMISSION._with_kerberos(params, "unused-", operation)
    operation.assert_not_called()


class TestHbaseServiceCheckWorkflow(unittest.TestCase):
  def _params(self, security_enabled=False):
    return SimpleNamespace(
      hbase_cmd="/usr/lib/hbase/bin/hbase",
      hbase_conf_dir="/etc/hbase/conf",
      security_enabled=security_enabled,
      user_group="hadoop",
      exec_tmp_dir="/var/lib/ambari-agent/tmp",
      kinit_path_local="/usr/bin/kinit",
    )

  def test_nonsecure_check_uses_argv_and_bounded_retry(self):
    params = self._params()
    with patch.object(
      SERVICE_CHECK.shell, "checked_call", return_value=(0, "row01 value")
    ) as checked_call:
      output = SERVICE_CHECK.run_hbase_shell(
        params, "ambari-qa", "/tmp/check.hbase", tries=3
      )

    self.assertEqual("row01 value", output)
    checked_call.assert_called_once_with(
      (
        "/usr/lib/hbase/bin/hbase",
        "--config",
        "/etc/hbase/conf",
        "shell",
        "-n",
        "/tmp/check.hbase",
      ),
      user="ambari-qa",
      timeout=60,
      tries=3,
      try_sleep=5,
    )

  def test_secure_check_passes_only_private_cache_environment(self):
    params = self._params(security_enabled=True)
    cache = MagicMock()
    cache.environment = {"KRB5CCNAME": "FILE:/tmp/smoke-cache"}
    cache_factory = MagicMock()
    cache_factory.return_value.__enter__.return_value = cache
    with patch.object(
      SERVICE_CHECK, "PrivateKerberosCache", cache_factory
    ), patch.object(
      SERVICE_CHECK.shell, "checked_call", return_value=(0, "value")
    ) as checked_call:
      output = SERVICE_CHECK.run_hbase_shell(
        params,
        "ambari-qa",
        "/tmp/check.hbase",
        "/etc/security/keytabs/smoke.keytab",
        "ambari-qa@EXAMPLE.COM",
      )

    self.assertEqual("value", output)
    cache.kinit.assert_called_once_with(
      "/usr/bin/kinit",
      "/etc/security/keytabs/smoke.keytab",
      "ambari-qa@EXAMPLE.COM",
      timeout=30,
    )
    self.assertEqual(cache.environment, checked_call.call_args.kwargs["env"])
    cache_factory.return_value.__exit__.assert_called_once()

  def test_ruby_quote_neutralizes_shell_file_injection(self):
    self.assertEqual(
      "'row\\'\\\\name\\nexit'",
      SERVICE_CHECK.ruby_quote("row'\\name\nexit"),
    )

  def test_table_cleanup_is_safe_for_missing_or_disabled_table(self):
    self.assertEqual(
      "if exists 'ambarismoketest'\n"
      "  disable 'ambarismoketest' if is_enabled 'ambarismoketest'\n"
      "  drop 'ambarismoketest'\n"
      "end\n",
      SERVICE_CHECK.drop_table_commands("ambarismoketest"),
    )

  def test_partial_command_file_creation_is_always_cleaned_up(self):
    params = self._params()
    params.hbase_user = "hbase"
    params.smoke_test_user = "ambari-qa"
    params.smokeuser_permissions = "RWXCA"
    params.service_check_data = "value"
    params.smoke_user_keytab = ""
    params.smokeuser_principal = ""
    env = MagicMock()
    created_files = 0

    def create_or_delete(path, **kwargs):
      nonlocal created_files
      if kwargs.get("action") == "delete":
        return
      created_files += 1
      if created_files == 2:
        raise OSError("simulated command file creation failure")

    file_resource = MagicMock(side_effect=create_or_delete)
    with patch.dict(sys.modules, {"params": params}), patch.object(
      SERVICE_CHECK, "File", file_resource
    ), patch.object(SERVICE_CHECK, "run_hbase_shell") as run_hbase_shell:
      with self.assertRaisesRegex(OSError, "creation failure"):
        object.__new__(SERVICE_CHECK.HbaseServiceCheckDefault).service_check(env)

    run_hbase_shell.assert_not_called()
    deleted_files = [
      positional[0]
      for positional, keywords in file_resource.call_args_list
      if keywords.get("action") == "delete"
    ]
    self.assertEqual(3, len(deleted_files))
    self.assertEqual(3, len(set(deleted_files)))


class TestHbaseUpgradeWorkflow(unittest.TestCase):
  def test_registration_uses_literal_hostname_and_ip_tokens(self):
    self.assertTrue(
      UPGRADE._server_is_registered(
        "1 active servers: rs1.example.com:16020", "rs1.example.com"
      )
    )
    with patch.object(
      UPGRADE.socket, "gethostbyname", return_value="192.0.2.17"
    ):
      self.assertTrue(
        UPGRADE._server_is_registered(
          "1 active servers: 192.0.2.17:16020", "rs1.example.com"
        )
      )
    with patch.object(
      UPGRADE.socket,
      "gethostbyname",
      side_effect=socket.error("not found"),
    ):
      self.assertFalse(
        UPGRADE._server_is_registered(
          "1 active servers: rs2.example.com:16020", "rs1.example.com"
        )
      )
    with self.assertRaises(Fail):
      UPGRADE._server_is_registered("", "rs1.example.com")

  def test_regionserver_process_check_uses_exact_lifecycle_helper(self):
    params = SimpleNamespace(
      regionserver_pid_file="/var/run/hbase/hbase-hbase-regionserver.pid",
      hbase_user="hbase",
      user_group="hadoop",
    )
    identity = SimpleNamespace(pid=4217)
    with patch.object(
      UPGRADE,
      "read_or_discover_hbase_process",
      return_value=identity,
    ) as process_check:
      self.assertTrue(UPGRADE.is_region_server_process_running(params))
    process_check.assert_called_once_with(
      params.regionserver_pid_file,
      "hbase",
      "hadoop",
      "regionserver",
    )


class TestHbaseRegionServerRollingWorkflow(unittest.TestCase):
  def test_graceful_start_calls_imported_region_mover_after_start(self):
    hbase_service = MagicMock()
    move_regions = MagicMock()
    decommission_module = dependency_module(
      "hbase_decommission",
      hbase_decommission=MagicMock(),
      move_regions=move_regions,
    )
    dependencies = {
      "hbase": dependency_module("hbase", hbase=MagicMock()),
      "hbase_service": dependency_module(
        "hbase_service",
        check_hbase_process_status=MagicMock(),
        hbase_service=hbase_service,
      ),
      "upgrade": dependency_module("upgrade"),
      "setup_ranger_hbase": dependency_module(
        "setup_ranger_hbase", setup_ranger_hbase=MagicMock()
      ),
      "hbase_decommission": decommission_module,
    }
    regionserver = load_module(
      "bigtop_hbase_regionserver_workflow",
      SCRIPTS / "hbase_regionserver.py",
      dependencies,
    )
    params = dependency_module("params", hostname="rs1.example.com")
    env = MagicMock()

    with patch.dict(sys.modules, {"params": params}):
      regionserver.HbaseRegionServer().graceful_start(env)

    hbase_service.assert_called_once_with("regionserver", action="start")
    move_regions.assert_called_once_with(params, "rs1.example.com", "load")
    self.assertEqual(
      [call.set_params(params)],
      env.mock_calls,
    )


if __name__ == "__main__":
  unittest.main()
