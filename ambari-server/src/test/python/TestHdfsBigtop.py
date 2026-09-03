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

import argparse
import ast
from contextlib import contextmanager
import importlib.util
import json
import os
from pathlib import Path
import sys
import tempfile
from types import ModuleType
import unittest
from unittest.mock import MagicMock, call, mock_open, patch
import xml.etree.ElementTree as ET

from ambari_commons import import_utils
from resource_management.core.exceptions import ComponentIsNotRunning, Fail
from resource_management.core.environment import Environment
from resource_management.core.logger import Logger
from resource_management.libraries.functions import jmx, namenode_ha_utils


HDFS = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/HDFS"
)
SCRIPTS = HDFS / "package/scripts"


def dependency_module(name, **attributes):
  module = ModuleType(name)
  for attribute, value in attributes.items():
    setattr(module, attribute, value)
  return module


def load_script(module_name, filename, dependencies=None):
  spec = importlib.util.spec_from_file_location(module_name, SCRIPTS / filename)
  module = importlib.util.module_from_spec(spec)
  with patch.dict(sys.modules, dependencies or {}):
    spec.loader.exec_module(module)
  return module


def load_module(module_name, path):
  spec = importlib.util.spec_from_file_location(module_name, path)
  module = importlib.util.module_from_spec(spec)
  spec.loader.exec_module(module)
  return module


HDFS_PROCESS = load_script("bigtop_hdfs_process", "hdfs_process.py")
HDFS_KERBEROS = load_script("bigtop_hdfs_kerberos", "hdfs_kerberos.py")
HDFS_ADVISOR = load_module("bigtop_hdfs_service_advisor", HDFS / "service_advisor.py")
ROUTER_UTILS = load_script("bigtop_hdfs_router_utils", "router_utils.py")
HDFS_ROUTER_SUPPORT = dependency_module(
  "utils", set_up_zkfc_security=MagicMock(), service=MagicMock()
)
HDFS_ROUTER = load_script(
  "bigtop_hdfs_router",
  "hdfs_router.py",
  {"hdfs_process": HDFS_PROCESS, "utils": HDFS_ROUTER_SUPPORT},
)

HDFS_UTILS = dependency_module(
  "utils",
  get_dfsadmin_base_command=MagicMock(
    return_value=("hdfs", "dfsadmin", "-fs", "hdfs://cluster")
  ),
  set_up_zkfc_security=MagicMock(),
  service=MagicMock(),
  safe_zkfc_op=MagicMock(),
  is_previous_fs_image=MagicMock(return_value=False),
)
HDFS_NAMENODE = load_script(
  "bigtop_hdfs_namenode",
  "hdfs_namenode.py",
  {
    "hdfs_process": HDFS_PROCESS,
    "hdfs_kerberos": HDFS_KERBEROS,
    "utils": HDFS_UTILS,
    "setup_ranger_hdfs": dependency_module(
      "setup_ranger_hdfs",
      setup_ranger_hdfs=MagicMock(),
      create_ranger_audit_hdfs_directories=MagicMock(),
    ),
    "namenode_upgrade": dependency_module("namenode_upgrade"),
  },
)

NAMENODE_UPGRADE = load_script(
  "bigtop_namenode_upgrade",
  "namenode_upgrade.py",
  {
    "hdfs_kerberos": HDFS_KERBEROS,
    "utils": dependency_module(
      "utils",
      get_dfsadmin_base_command=MagicMock(
        return_value=("hdfs", "dfsadmin", "-fs", "hdfs://cluster")
      ),
    ),
    "namenode_ha_state": dependency_module(
      "namenode_ha_state", NamenodeHAState=MagicMock
    ),
  },
)

SERVICE_CHECK = load_script(
  "bigtop_hdfs_service_check",
  "service_check.py",
  {"hdfs_process": HDFS_PROCESS},
)
CHECK_WEB_UI = load_module(
  "bigtop_hdfs_check_web_ui", HDFS / "package/files/checkWebUI.py"
)
HDFS_RUNTIME_UTILS = load_script(
  "bigtop_hdfs_runtime_utils",
  "utils.py",
  {
    "hdfs_process": HDFS_PROCESS,
    "hdfs_kerberos": HDFS_KERBEROS,
    "zkfc_slave": dependency_module("zkfc_slave", ZkfcSlaveDefault=MagicMock),
  },
)
DATANODE_UPGRADE = load_script(
  "bigtop_hdfs_datanode_upgrade",
  "datanode_upgrade.py",
  {
    "hdfs_process": HDFS_PROCESS,
    "hdfs_kerberos": HDFS_KERBEROS,
    "utils": dependency_module(
      "utils",
      get_dfsadmin_base_command=MagicMock(
        return_value=("hdfs", "dfsadmin", "-fs", "hdfs://cluster")
      ),
    ),
  },
)

DATANODE = load_script(
  "bigtop_datanode",
  "datanode.py",
  {
    "datanode_upgrade": DATANODE_UPGRADE,
    "hdfs_datanode": dependency_module("hdfs_datanode", datanode=MagicMock()),
    "hdfs": dependency_module("hdfs", hdfs=MagicMock(), reconfig=MagicMock()),
    "utils": dependency_module(
      "utils",
      get_hdfs_binary=MagicMock(return_value="hdfs"),
      get_dfsadmin_base_command=MagicMock(
        return_value=("hdfs", "dfsadmin", "-fs", "hdfs://cluster")
      ),
    ),
    "hdfs_kerberos": HDFS_KERBEROS,
  },
)

NAMENODE = load_script(
  "bigtop_namenode",
  "namenode.py",
  {
    "hdfs_kerberos": HDFS_KERBEROS,
    "namenode_upgrade": NAMENODE_UPGRADE,
    "hdfs_namenode": dependency_module(
      "hdfs_namenode",
      namenode=MagicMock(),
      wait_for_safemode_off=MagicMock(),
      refreshProxyUsers=MagicMock(),
      format_namenode=MagicMock(),
    ),
    "hdfs": dependency_module("hdfs", hdfs=MagicMock(), reconfig=MagicMock()),
    "hdfs_rebalance": dependency_module(
      "hdfs_rebalance", is_balancer_running=MagicMock(return_value=False)
    ),
    "utils": dependency_module(
      "utils",
      initiate_safe_zkfc_failover=MagicMock(),
      get_hdfs_binary=MagicMock(return_value="hdfs"),
      get_dfsadmin_base_command=MagicMock(
        return_value=("hdfs", "dfsadmin", "-fs", "hdfs://cluster")
      ),
    ),
  },
)


def params_module(**values):
  module = dependency_module("params", **values)
  if Environment.has_instance():
    Environment.get_instance().set_params(module)
  return module


class TestHdfsBigtop(unittest.TestCase):
  def setUp(self):
    Logger.initialize_logger()
    self._environment = Environment(str(HDFS / "package"), test_mode=True)
    self._environment.__enter__()

  def tearDown(self):
    self._environment.__exit__(None, None, None)

  def test_synchronous_execute_calls_are_bounded_by_process_group_timeout(self):
    for script_path in SCRIPTS.glob("*.py"):
      tree = ast.parse(script_path.read_text(encoding="utf-8"))
      for node in ast.walk(tree):
        if not (
          isinstance(node, ast.Call)
          and isinstance(node.func, ast.Name)
          and node.func.id == "Execute"
        ):
          continue
        keywords = {keyword.arg: keyword.value for keyword in node.keywords}
        wait_for_finish = keywords.get("wait_for_finish")
        if (
          isinstance(wait_for_finish, ast.Constant)
          and wait_for_finish.value is False
        ):
          continue
        self.assertIn(
          "timeout",
          keywords,
          f"{script_path.name}:{node.lineno} lacks a timeout",
        )
        strategy = keywords.get("timeout_kill_strategy")
        self.assertIsInstance(
          strategy,
          ast.Attribute,
          f"{script_path.name}:{node.lineno} lacks a process-group strategy",
        )
        self.assertEqual(
          "KILL_PROCESS_GROUP",
          strategy.attr,
          f"{script_path.name}:{node.lineno} has the wrong timeout strategy",
        )

  def test_router_client_configuration_is_deterministic(self):
    hdfs_site = {"dfs.nameservices": "ns1,ns2"}
    core_site = {
      "fs.defaultFS": "hdfs://ns1",
      "ha.zookeeper.quorum": "zk1:2181,zk2:2181",
    }
    router_hdfs_site, router_core_site = ROUTER_UTILS.build_router_client_sites(
      hdfs_site,
      core_site,
      ["router1.example.com", "2001:db8::2"],
      20010,
    )

    self.assertEqual("ns1,ns2,ns-fed", router_hdfs_site["dfs.nameservices"])
    self.assertEqual(
      "router1.example.com:20010",
      router_hdfs_site["dfs.namenode.rpc-address.ns-fed.r1"],
    )
    self.assertEqual(
      "[2001:db8::2]:20010",
      router_hdfs_site["dfs.namenode.rpc-address.ns-fed.r2"],
    )
    self.assertEqual("hdfs://ns-fed", router_core_site["fs.defaultFS"])
    self.assertEqual({"dfs.nameservices": "ns1,ns2"}, hdfs_site)
    self.assertEqual("hdfs://ns1", core_site["fs.defaultFS"])

    updated_hdfs_site, _ = ROUTER_UTILS.build_router_client_sites(
      router_hdfs_site,
      router_core_site,
      ["router1.example.com"],
      20010,
    )
    self.assertEqual("ns1,ns2,ns-fed", updated_hdfs_site["dfs.nameservices"])

  def test_router_client_configuration_rejects_incomplete_or_unsafe_inputs(self):
    valid_hdfs = {"dfs.nameservices": "ns1"}
    valid_core = {"ha.zookeeper.quorum": "zk1:2181"}
    for hdfs_site, core_site, hosts, port in (
      ({}, valid_core, ["router1"], 20010),
      (valid_hdfs, {}, ["router1"], 20010),
      (valid_hdfs, valid_core, [], 20010),
      (valid_hdfs, valid_core, ["router1;touch /tmp/x"], 20010),
      (valid_hdfs, valid_core, ["router1"], 0),
      (valid_hdfs, valid_core, ["router1"], 65536),
    ):
      with self.subTest(
        hdfs_site=hdfs_site,
        core_site=core_site,
        hosts=hosts,
        port=port,
      ):
        with self.assertRaises(Fail):
          ROUTER_UTILS.build_router_client_sites(
            hdfs_site, core_site, hosts, port
          )

  def test_router_start_does_not_create_unused_default_kerberos_cache(self):
    params = params_module(
      security_enabled=True,
      hdfs_user="hdfs",
      kinit_path_local="/usr/bin/kinit",
      hdfs_user_keytab="/etc/security/keytabs/hdfs.service.keytab",
      hdfs_principal_name="hdfs/router1.example.com@EXAMPLE.COM",
    )
    HDFS_ROUTER_SUPPORT.service.reset_mock()
    with patch.dict(sys.modules, {"params": params}):
      HDFS_ROUTER.router(action="start")

    HDFS_ROUTER_SUPPORT.service.assert_called_once()
    with self.assertRaisesRegex(Fail, "Unsupported HDFS Router action"):
      HDFS_ROUTER.router(action="invalid")

  def test_rebalance_rejects_non_numeric_threshold_before_execution(self):
    script = object.__new__(NAMENODE.NameNodeDefault)
    for threshold in ("DEBUG", "10; touch /tmp/unsafe", "NaN", 0, 101):
      params = params_module(
        name_node_params=json.dumps({"threshold": threshold})
      )
      env = MagicMock()
      with self.subTest(threshold=threshold), \
        patch.dict(sys.modules, {"params": params}), \
        patch.object(NAMENODE, "Execute") as execute:
        with self.assertRaises(Fail):
          script.rebalancehdfs(env)
      execute.assert_not_called()

    for serialized_params in (None, "not-json", "{}", "[]"):
      with self.subTest(serialized_params=serialized_params):
        with self.assertRaises(Fail):
          NAMENODE.parse_balancer_threshold(serialized_params)

  def test_rebalance_uses_argument_vector(self):
    script = object.__new__(NAMENODE.NameNodeDefault)
    params = params_module(
      name_node_params=json.dumps({"threshold": "12.5"}),
      hadoop_bin_dir="/usr/bin",
      hadoop_conf_dir="/etc/hadoop/conf",
      security_enabled=False,
      hdfs_user="hdfs",
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(NAMENODE, "Execute") as execute:
      script.rebalancehdfs(MagicMock())

    self.assertEqual(
      (
        "hdfs",
        "--config",
        "/etc/hadoop/conf",
        "balancer",
        "-threshold",
        "12.5",
      ),
      execute.call_args.args[0],
    )
    self.assertEqual("hdfs", execute.call_args.kwargs["user"])

  def test_namenode_storage_check_is_shell_free_and_fail_closed(self):
    with tempfile.TemporaryDirectory() as name_dir:
      params = params_module(
        namenode_formatted_old_mark_dirs=[],
        namenode_formatted_mark_dirs=[],
        dfs_name_dir=name_dir,
      )
      self.assertFalse(HDFS_NAMENODE.is_namenode_formatted(params))

      Path(name_dir, "VERSION").write_text("layoutVersion=-66", encoding="utf-8")
      self.assertTrue(HDFS_NAMENODE.is_namenode_formatted(params))

      params.dfs_name_dir = Path(name_dir).as_uri()
      self.assertTrue(HDFS_NAMENODE.is_namenode_formatted(params))

    params.dfs_name_dir = "/missing/name/dir"
    self.assertTrue(HDFS_NAMENODE.is_namenode_formatted(params))
    params.dfs_name_dir = "hdfs://remote/name/dir"
    self.assertTrue(HDFS_NAMENODE.is_namenode_formatted(params))

  def test_failed_ha_format_never_deletes_name_directory_contents(self):
    params = params_module(
      namenode_formatted_old_mark_dirs=[],
      namenode_formatted_mark_dirs=["/name/.formatted"],
      dfs_name_dir="/name",
      hdfs_user="hdfs",
      hadoop_conf_dir="/etc/hadoop/conf",
      hadoop_bin_dir="/usr/bin",
      dfs_ha_enabled=True,
      dfs_ha_namenode_active="namenode.example.com",
      hostname="namenode.example.com",
      public_hostname="namenode.example.com",
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(HDFS_NAMENODE, "is_namenode_formatted", return_value=False), \
      patch.object(
        HDFS_NAMENODE, "Execute", side_effect=Fail("format failed")
      ) as execute:
      with self.assertRaisesRegex(Fail, "format failed"):
        HDFS_NAMENODE.format_namenode()

    execute.assert_called_once()
    self.assertEqual(
      (
        "hdfs",
        "--config",
        "/etc/hadoop/conf",
        "namenode",
        "-format",
        "-nonInteractive",
      ),
      execute.call_args.args[0],
    )

  def test_external_commands_use_argument_vectors(self):
    service_check_source = (SCRIPTS / "service_check.py").read_text(
      encoding="utf-8"
    )
    self.assertIn('checkWebUICmd = (', service_check_source)
    self.assertNotIn('checkWebUICmd = format(', service_check_source)
    self.assertIn("sys.executable,", service_check_source)
    self.assertNotIn('"ambari-python-wrap",', service_check_source)

    nfs_source = (SCRIPTS / "hdfs_nfsgateway.py").read_text(encoding="utf-8")
    self.assertIn('("pgrep", "-x", "nfsd")', nfs_source)
    self.assertNotIn('shell.call("service ', nfs_source)

    self.assertFalse((SCRIPTS / "balancer-emulator").exists())
    rebalance_source = (SCRIPTS / "hdfs_rebalance.py").read_text(
      encoding="utf-8"
    )
    self.assertNotIn("HdfsParser", rebalance_source)

  def test_process_identity_uses_component_marker_and_hadoop_class(self):
    self.assertEqual(
      (
        "-Dproc_zkfc",
        "org.apache.hadoop.hdfs.tools.DFSZKFailoverController",
      ),
      HDFS_PROCESS.expected_cmdline("zkfc"),
    )
    self.assertEqual(
      (
        "-Dproc_datanode",
        "org.apache.hadoop.hdfs.server.datanode.SecureDataNodeStarter",
      ),
      HDFS_PROCESS.expected_cmdline("datanode", privileged=True),
    )
    with self.assertRaisesRegex(Fail, "Unsupported HDFS process"):
      HDFS_PROCESS.expected_cmdline("unknown")

  def test_status_revalidates_and_hardens_the_pid_file(self):
    identity = MagicMock(pid=1234)
    with patch.object(
      HDFS_PROCESS, "recover_running_process", return_value=identity
    ) as recover:
      actual = HDFS_PROCESS.check_component_status(
        "/run/hadoop/hdfs-namenode.pid",
        "hdfs",
        "namenode",
        owner="hdfs",
        group="hadoop",
      )

    self.assertIs(identity, actual)
    recover.assert_called_once_with(
      "/run/hadoop/hdfs-namenode.pid",
      "hdfs",
      "namenode",
      owner="hdfs",
      group="hadoop",
      privileged=False,
    )

  def test_status_fails_closed_when_no_valid_process_can_be_recovered(self):
    with patch.object(HDFS_PROCESS, "recover_running_process", return_value=None), \
      self.assertRaises(ComponentIsNotRunning):
      HDFS_PROCESS.check_component_status(
        "/run/hadoop/hdfs-namenode.pid",
        "hdfs",
        "namenode",
        owner="hdfs",
        group="hadoop",
      )

  def test_zkfc_recovery_publishes_a_private_pid_file_before_termination(self):
    params = params_module(
      dfs_ha_enabled=True,
      zkfc_pid_file="/run/hadoop/hdfs-zkfc.pid",
      user_group="hadoop",
    )
    identity = MagicMock(pid=4321)
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        HDFS_RUNTIME_UTILS.hdfs_process,
        "recover_running_process",
        return_value=identity,
      ) as recover, \
      patch.object(HDFS_RUNTIME_UTILS.hdfs_process, "terminate_process"), \
      patch.object(HDFS_RUNTIME_UTILS.hdfs_process, "remove_pid_file_if_stopped"):
      self.assertTrue(HDFS_RUNTIME_UTILS.kill_zkfc("hdfs"))

    recover.assert_called_once_with(
      "/run/hadoop/hdfs-zkfc.pid",
      "hdfs",
      "zkfc",
      owner="hdfs",
      group="hadoop",
    )

  def test_sensitive_defaults_require_operator_input_and_enabled_runtime_checks(self):
    ssl_root = ET.parse(HDFS / "configuration/ssl-server.xml")
    for name in (
      "ssl.server.truststore.password",
      "ssl.server.keystore.password",
      "ssl.server.keystore.keypassword",
    ):
      property_element = next(
        element
        for element in ssl_root.findall("property")
        if element.findtext("name") == name
      )
      self.assertEqual("", property_element.findtext("value", ""))
      self.assertEqual(
        "false",
        property_element.findtext("value-attributes/empty-value-valid"),
      )

    ranger_root = ET.parse(
      HDFS / "configuration/ranger-hdfs-plugin-properties.xml"
    )
    ranger_password = next(
      element
      for element in ranger_root.findall("property")
      if element.findtext("name") == "REPOSITORY_CONFIG_PASSWORD"
    )
    self.assertEqual("", ranger_password.findtext("value", ""))
    self.assertEqual(
      "false",
      ranger_password.findtext("value-attributes/empty-value-valid"),
    )
    params_source = (SCRIPTS / "params_linux.py").read_text(encoding="utf-8")
    self.assertIn(
      "must not be empty when HDFS HTTPS is enabled", params_source
    )
    self.assertIn(
      "must not be empty when the Ranger HDFS plugin is enabled",
      params_source,
    )
    self.assertIn("require_external_ranger_credentials", params_source)
    self.assertNotIn('external_admin_password", "admin"', params_source)
    self.assertNotIn('external_ranger_admin_password",', params_source)

  def test_kerberos_configuration_files_are_group_readable_only(self):
    hdfs_source = (SCRIPTS / "hdfs.py").read_text(encoding="utf-8")
    self.assertEqual(3, hdfs_source.count("mode=0o640"))
    utils_source = (SCRIPTS / "utils.py").read_text(encoding="utf-8")
    self.assertIn('"hdfs_jaas.conf"),', utils_source)
    self.assertIn("mode=0o640", utils_source)

  def test_advisor_requires_hdfs_tls_and_ranger_secrets_only_when_enabled(self):
    validator = object.__new__(HDFS_ADVISOR.HDFSValidator)
    https_configurations = {
      "hdfs-site": {"properties": {"dfs.http.policy": "HTTP_AND_HTTPS"}}
    }
    tls_problems = validator.validateSslServerConfigurations(
      {}, {}, https_configurations, {"configurations": {}}, {}
    )
    self.assertEqual(
      {
        "ssl.server.truststore.password",
        "ssl.server.keystore.password",
        "ssl.server.keystore.keypassword",
      },
      {problem["config-name"] for problem in tls_problems},
    )
    self.assertEqual(
      [],
      validator.validateSslServerConfigurations(
        {},
        {},
        {"hdfs-site": {"properties": {"dfs.http.policy": "HTTP_ONLY"}}},
        {"configurations": {}},
        {},
      ),
    )

    ranger_configurations = {
      "ranger-hdfs-plugin-properties": {
        "properties": {"ranger-hdfs-plugin-enabled": "Yes"}
      }
    }
    ranger_services = {
      "configurations": {
        "ranger-env": {
          "properties": {"ranger-hdfs-plugin-enabled": "yes"}
        }
      }
    }
    ranger_problems = validator.validateRangerPluginConfigurations(
      ranger_configurations["ranger-hdfs-plugin-properties"]["properties"],
      {},
      ranger_configurations,
      ranger_services,
      {},
    )
    self.assertEqual(
      ["REPOSITORY_CONFIG_PASSWORD"],
      [problem["config-name"] for problem in ranger_problems],
    )

  def test_process_recovery_rejects_wrong_pid_identity(self):
    with patch.object(HDFS_PROCESS.safe_process, "read_pid", return_value=8123), \
      patch.object(
        HDFS_PROCESS.safe_process,
        "read_running_process",
        side_effect=Fail("command line does not match"),
      ), \
      patch.object(
        HDFS_PROCESS.safe_process, "remove_pid_file_if_stopped"
      ) as remove_pid:
      with self.assertRaisesRegex(Fail, "command line does not match"):
        HDFS_PROCESS.recover_running_process(
          "/run/hdfs.pid", "hdfs", "namenode"
        )

    remove_pid.assert_not_called()

  def test_process_recovery_secures_existing_pid_file(self):
    running = MagicMock(pid=8123)
    with patch.object(HDFS_PROCESS.safe_process, "read_pid", return_value=8123), \
      patch.object(
        HDFS_PROCESS.safe_process,
        "read_running_process",
        return_value=running,
      ), \
      patch.object(
        HDFS_PROCESS.safe_process,
        "publish_pid_file_for_identity",
        return_value=running,
      ) as publish:
      result = HDFS_PROCESS.recover_running_process(
        "/run/hdfs.pid",
        "hdfs",
        "namenode",
        owner="hdfs",
        group="hadoop",
      )

    self.assertIs(running, result)
    publish.assert_called_once_with(
      "/run/hdfs.pid",
      running,
      "hdfs",
      (
        "-Dproc_namenode",
        "org.apache.hadoop.hdfs.server.namenode.NameNode",
      ),
      "hdfs",
      "hadoop",
      mode=0o640,
    )

  def test_pid_publication_failure_rolls_back_only_pinned_identity(self):
    running = MagicMock(pid=8123)
    with patch.object(HDFS_PROCESS.safe_process, "read_pid", return_value=None), \
      patch.object(
        HDFS_PROCESS.safe_process,
        "discover_running_process",
        return_value=running,
      ), \
      patch.object(
        HDFS_PROCESS.safe_process,
        "publish_pid_file_for_identity",
        side_effect=Fail("publication failed"),
      ), \
      patch.object(HDFS_PROCESS.safe_process, "terminate_process") as terminate, \
      patch.object(
        HDFS_PROCESS.safe_process, "remove_pid_file_if_stopped"
      ) as remove:
      with self.assertRaisesRegex(Fail, "publication failed"):
        HDFS_PROCESS.wait_for_running_process(
          "/run/hdfs.pid", "hdfs", "namenode", "hdfs", "hadoop"
        )

    terminate.assert_called_once_with(
      running,
      "hdfs",
      (
        "-Dproc_namenode",
        "org.apache.hadoop.hdfs.server.namenode.NameNode",
      ),
    )
    remove.assert_called_once_with(
      "/run/hdfs.pid",
      8123,
      "hdfs",
      (
        "-Dproc_namenode",
        "org.apache.hadoop.hdfs.server.namenode.NameNode",
      ),
    )

  def test_service_start_does_not_duplicate_discovered_process(self):
    params = params_module(
      hadoop_pid_dir_prefix="/run/hadoop",
      hdfs_log_dir_prefix="/var/log/hadoop",
      hadoop_libexec_dir="/usr/lib/hadoop/libexec",
      hadoop_bin="/usr/lib/hadoop/sbin",
      hadoop_conf_dir="/etc/hadoop/conf",
      security_enabled=False,
      user_group="hadoop",
      hdfs_user="hdfs",
      root_user="root",
      root_group="root",
    )
    identity = MagicMock()
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        HDFS_RUNTIME_UTILS.hdfs_process,
        "recover_running_process",
        return_value=identity,
      ), \
      patch.object(HDFS_RUNTIME_UTILS, "Execute") as execute:
      HDFS_RUNTIME_UTILS.service(
        action="start", name="namenode", user="hdfs"
      )

    execute.assert_not_called()

  def test_service_stop_never_signals_unvalidated_pid(self):
    params = params_module(
      hadoop_pid_dir_prefix="/run/hadoop",
      hdfs_log_dir_prefix="/var/log/hadoop",
      hadoop_libexec_dir="/usr/lib/hadoop/libexec",
      hadoop_bin="/usr/lib/hadoop/sbin",
      hadoop_conf_dir="/etc/hadoop/conf",
      security_enabled=False,
      user_group="hadoop",
      hdfs_user="hdfs",
      root_user="root",
      root_group="root",
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        HDFS_RUNTIME_UTILS.hdfs_process,
        "recover_running_process",
        side_effect=Fail("owner does not match"),
      ), \
      patch.object(HDFS_RUNTIME_UTILS, "Execute") as execute, \
      patch.object(
        HDFS_RUNTIME_UTILS.hdfs_process, "terminate_process"
      ) as terminate:
      with self.assertRaisesRegex(Fail, "owner does not match"):
        HDFS_RUNTIME_UTILS.service(
          action="stop", name="namenode", user="hdfs"
        )

    execute.assert_not_called()
    terminate.assert_not_called()

  def test_service_stop_revalidates_identity_before_forced_signal(self):
    params = params_module(
      hadoop_pid_dir_prefix="/run/hadoop",
      hdfs_log_dir_prefix="/var/log/hadoop",
      hadoop_libexec_dir="/usr/lib/hadoop/libexec",
      hadoop_bin="/usr/lib/hadoop/sbin",
      hadoop_conf_dir="/etc/hadoop/conf",
      security_enabled=False,
      user_group="hadoop",
      hdfs_user="hdfs",
      root_user="root",
      root_group="root",
    )
    identity = MagicMock(pid=8123)
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        HDFS_RUNTIME_UTILS.hdfs_process,
        "recover_running_process",
        return_value=identity,
      ), \
      patch.object(
        HDFS_RUNTIME_UTILS.hdfs_process,
        "wait_for_process_stopped",
        return_value=False,
      ), \
      patch.object(
        HDFS_RUNTIME_UTILS.hdfs_process, "terminate_process"
      ) as terminate, \
      patch.object(
        HDFS_RUNTIME_UTILS.hdfs_process, "remove_pid_file_if_stopped"
      ) as cleanup, \
      patch.object(HDFS_RUNTIME_UTILS, "Execute"):
      HDFS_RUNTIME_UTILS.service(
        action="stop", name="namenode", user="hdfs"
      )

    terminate.assert_called_once_with(
      identity, "hdfs", "namenode", privileged=False
    )
    cleanup.assert_called_once_with(
      "/run/hadoop/hdfs/hadoop-hdfs-namenode.pid",
      identity,
      "hdfs",
      "namenode",
      privileged=False,
    )

  def test_service_rejects_untrusted_command_fields_before_execution(self):
    params = params_module()
    for arguments in (
      {"action": "restart", "name": "namenode", "user": "hdfs"},
      {"action": "start", "name": "namenode; id", "user": "hdfs"},
      {
        "action": "start",
        "name": "namenode",
        "user": "hdfs",
        "options": "-rollingUpgrade started; id",
      },
      {"action": "start", "name": "namenode", "user": "hdfs; id"},
    ):
      with self.subTest(arguments=arguments), \
        patch.dict(sys.modules, {"params": params}), \
        patch.object(HDFS_RUNTIME_UTILS, "Execute") as execute:
        with self.assertRaises(Fail):
          HDFS_RUNTIME_UTILS.service(**arguments)
        execute.assert_not_called()

    with self.assertRaisesRegex(Fail, "Unsupported ZKFC action"):
      HDFS_RUNTIME_UTILS.safe_zkfc_op("restart", MagicMock())

  def test_service_uses_positional_argv_for_non_root_daemon_options(self):
    params = params_module(
      hadoop_pid_dir_prefix="/run/hadoop",
      hdfs_log_dir_prefix="/var/log/hadoop",
      hadoop_libexec_dir="/usr/lib/hadoop/libexec",
      hadoop_bin="/usr/lib/hadoop/sbin;$(id)",
      hadoop_conf_dir="/etc/hadoop/conf;$(id)",
      security_enabled=False,
      user_group="hadoop",
      hdfs_user="hdfs",
      root_user="root",
      root_group="root",
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(HDFS_RUNTIME_UTILS, "Directory"), \
      patch.object(
        HDFS_RUNTIME_UTILS.hdfs_process,
        "recover_running_process",
        return_value=None,
      ), \
      patch.object(
        HDFS_RUNTIME_UTILS.hdfs_process, "wait_for_running_process"
      ), \
      patch.object(HDFS_RUNTIME_UTILS, "Execute") as execute:
      HDFS_RUNTIME_UTILS.service(
        action="start",
        name="namenode",
        user="hdfs",
        options="-rollingUpgrade started",
      )

    execute.assert_called_once_with(
      (
        "bash",
        "-c",
        'ulimit -c unlimited; exec "$@"',
        "ambari-hdfs-daemon",
        "/usr/lib/hadoop/sbin;$(id)/hadoop-daemon.sh",
        "--config",
        "/etc/hadoop/conf;$(id)",
        "start",
        "namenode",
        "-rollingUpgrade",
        "started",
      ),
      environment={"HADOOP_LIBEXEC_DIR": "/usr/lib/hadoop/libexec"},
      timeout=60,
      timeout_kill_strategy=(
        HDFS_RUNTIME_UTILS.TerminateStrategy.KILL_PROCESS_GROUP
      ),
      user="hdfs",
    )

  def test_service_start_failure_logs_and_skips_process_wait(self):
    params = params_module(
      hadoop_pid_dir_prefix="/run/hadoop",
      hdfs_log_dir_prefix="/var/log/hadoop",
      hadoop_libexec_dir="/usr/lib/hadoop/libexec",
      hadoop_bin="/usr/lib/hadoop/sbin",
      hadoop_conf_dir="/etc/hadoop/conf",
      security_enabled=False,
      user_group="hadoop",
      hdfs_user="hdfs",
      root_user="root",
      root_group="root",
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(HDFS_RUNTIME_UTILS, "Directory"), \
      patch.object(
        HDFS_RUNTIME_UTILS.hdfs_process,
        "recover_running_process",
        return_value=None,
      ), \
      patch.object(
        HDFS_RUNTIME_UTILS.hdfs_process, "wait_for_running_process"
      ) as wait, \
      patch.object(
        HDFS_RUNTIME_UTILS, "Execute", side_effect=Fail("daemon failed")
      ), \
      patch.object(HDFS_RUNTIME_UTILS, "show_logs") as show_logs:
      with self.assertRaisesRegex(Fail, "daemon failed"):
        HDFS_RUNTIME_UTILS.service(
          action="start", name="namenode", user="hdfs"
        )

    show_logs.assert_called_once_with("/var/log/hadoop/hdfs", "hdfs")
    wait.assert_not_called()

  def test_log_collection_failure_does_not_mask_start_failure(self):
    params = params_module(
      hadoop_pid_dir_prefix="/run/hadoop",
      hdfs_log_dir_prefix="/var/log/hadoop",
      hadoop_libexec_dir="/usr/lib/hadoop/libexec",
      hadoop_bin="/usr/lib/hadoop/sbin",
      hadoop_conf_dir="/etc/hadoop/conf",
      security_enabled=False,
      user_group="hadoop",
      hdfs_user="hdfs",
      root_user="root",
      root_group="root",
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(HDFS_RUNTIME_UTILS, "Directory"), \
      patch.object(
        HDFS_RUNTIME_UTILS.hdfs_process,
        "recover_running_process",
        return_value=None,
      ), \
      patch.object(
        HDFS_RUNTIME_UTILS, "Execute", side_effect=Fail("start failed")
      ), \
      patch.object(
        HDFS_RUNTIME_UTILS, "show_logs", side_effect=Fail("logs failed")
      ):
      with self.assertRaisesRegex(Fail, "start failed"):
        HDFS_RUNTIME_UTILS.service(
          action="start", name="namenode", user="hdfs"
        )

  def test_failed_graceful_datanode_shutdown_uses_stop_fallback(self):
    params = params_module(
      security_enabled=False,
      hdfs_user="hdfs",
      dfs_dn_ipc_address="datanode.example.com:9867",
    )
    base_command = ("hdfs", "dfsadmin", "-fs", "hdfs://cluster")
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        DATANODE_UPGRADE,
        "get_dfsadmin_base_command",
        return_value=base_command,
      ), \
      patch.object(
        DATANODE_UPGRADE.shell,
        "call",
        return_value=(1, "permission denied"),
      ) as shell_call:
      self.assertFalse(
        DATANODE_UPGRADE.pre_rolling_upgrade_shutdown("hdfs")
      )

    shell_call.assert_called_once_with(
      base_command
      + ("-shutdownDatanode", "datanode.example.com:9867", "upgrade"),
      user="hdfs",
      env=None,
      timeout=120,
      timeout_kill_strategy=DATANODE_UPGRADE.TerminateStrategy.KILL_PROCESS_GROUP,
      shell=False,
    )

  def test_datanode_shutdown_accepts_only_explicit_deregistration_status(self):
    params = params_module(
      security_enabled=False,
      hdfs_user="hdfs",
      dfs_dn_ipc_address="datanode.example.com:9867",
    )
    base_command = ("hdfs", "dfsadmin", "-fs", "hdfs://cluster")
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        DATANODE,
        "get_dfsadmin_base_command",
        return_value=base_command,
      ), \
      patch.object(DATANODE.shell, "call", return_value=(1, "not registered")):
      self.assertTrue(DATANODE.DataNode().check_datanode_shutdown("hdfs"))

  def test_datanode_shutdown_fails_closed_on_command_exception(self):
    params = params_module(
      security_enabled=False,
      hdfs_user="hdfs",
      dfs_dn_ipc_address="datanode.example.com:9867",
    )
    base_command = ("hdfs", "dfsadmin", "-fs", "hdfs://cluster")
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        DATANODE,
        "get_dfsadmin_base_command",
        return_value=base_command,
      ), \
      patch.object(
        DATANODE.shell, "call", side_effect=RuntimeError("timeout")
      ), \
      patch(
        "resource_management.libraries.functions.decorator.time.sleep"
      ):
      with self.assertRaisesRegex(Fail, "Unable to determine DataNode shutdown state"):
        DATANODE.DataNode().check_datanode_shutdown("hdfs")

  def test_hdfs_network_port_parser_rejects_partial_and_unsafe_values(self):
    self.assertEqual(9866, HDFS_RUNTIME_UTILS.get_port("0.0.0.0:9866"))
    self.assertEqual(9867, HDFS_RUNTIME_UTILS.get_port("https://[::1]:9867"))
    for address in (
      "host.example.com:9866 trailing",
      "host.example.com:9866/path",
      "host.example.com:70000",
      "host.example.com",
      "hdfs://host.example.com:9866",
      " user:9866",
      "user@host.example.com:9866",
    ):
      with self.subTest(address=address):
        with self.assertRaisesRegex(Fail, "Invalid HDFS network address"):
          HDFS_RUNTIME_UTILS.get_port(address)

  def test_secure_rebalance_uses_private_runtime_ccache_directory(self):
    script = object.__new__(NAMENODE.NameNodeDefault)
    params = params_module(
      name_node_params=json.dumps({"threshold": 10}),
      hadoop_bin_dir="/usr/bin",
      hadoop_conf_dir="/etc/hadoop/conf",
      hadoop_pid_dir_prefix="/run/hadoop",
      security_enabled=True,
      hdfs_user="hdfs",
      user_group="hadoop",
      hdfs_principal_name="hdfs/namenode.example.com@EXAMPLE.COM",
      hdfs_user_keytab="/etc/security/keytabs/nn.service.keytab",
      klist_path_local="/usr/bin/klist",
      kinit_path_local="/usr/bin/kinit",
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(NAMENODE, "Directory") as directory, \
      patch.object(NAMENODE.shell, "call", return_value=(1, "")), \
      patch.object(NAMENODE, "Execute"), \
      patch.object(
        NAMENODE.hdfs_rebalance, "is_balancer_running", return_value=False
      ):
      script.rebalancehdfs(MagicMock())

    directory.assert_called_once_with(
      "/run/hadoop/hdfs/ambari-ccache",
      owner="hdfs",
      group="hadoop",
      mode=0o700,
      create_parents=True,
    )

  def test_dfsadmin_base_command_keeps_filesystem_as_one_argument(self):
    params = params_module(
      dfs_ha_enabled=False,
      namenode_address="namenode.example.com:8020; touch /tmp/unsafe",
      namenode_rpc="namenode.example.com:8020",
    )
    with patch.dict(sys.modules, {"params": params}):
      command = HDFS_RUNTIME_UTILS.get_dfsadmin_base_command("hdfs")
    self.assertEqual(
      (
        "hdfs",
        "dfsadmin",
        "-fs",
        "namenode.example.com:8020; touch /tmp/unsafe",
      ),
      command,
    )

    params.dfs_ha_enabled = True
    with patch.dict(sys.modules, {"params": params}):
      command = HDFS_RUNTIME_UTILS.get_dfsadmin_base_command(
        "hdfs", use_specific_namenode=True
      )
    self.assertEqual(
      ("hdfs", "dfsadmin", "-fs", "hdfs://namenode.example.com:8020"),
      command,
    )

  def test_save_namespace_executes_and_propagates_failure(self):
    params = params_module(hdfs_user="hdfs", hadoop_bin_dir="/usr/bin")
    command = (
      "hdfs",
      "dfsadmin",
      "-fs",
      "hdfs://cluster",
      "-saveNamespace",
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        NAMENODE_UPGRADE,
        "get_dfsadmin_base_command",
        return_value=command[:-1],
      ), \
      patch.object(NAMENODE_UPGRADE, "Execute") as execute:
      NAMENODE_UPGRADE.prepare_upgrade_save_namespace("hdfs")

    execute.assert_called_once_with(
      command,
      user="hdfs",
      environment={"PATH": "/usr/bin"},
      logoutput=True,
      timeout=300,
      timeout_kill_strategy=NAMENODE_UPGRADE.TerminateStrategy.KILL_PROCESS_GROUP,
    )

    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        NAMENODE_UPGRADE,
        "get_dfsadmin_base_command",
        return_value=command[:-1],
      ), \
      patch.object(
        NAMENODE_UPGRADE, "Execute", side_effect=OSError("checkpoint failed")
      ):
      with self.assertRaisesRegex(Fail, "Could not save the NameSpace"):
        NAMENODE_UPGRADE.prepare_upgrade_save_namespace("hdfs")

  def test_safemode_wait_retries_with_argument_vector(self):
    params = params_module(security_enabled=False, hdfs_user="hdfs")
    command = (
      "hdfs",
      "dfsadmin",
      "-fs",
      "hdfs://cluster",
      "-safemode",
      "get",
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        HDFS_NAMENODE,
        "get_dfsadmin_base_command",
        return_value=command[:-2],
      ), \
      patch.object(
        HDFS_NAMENODE.shell,
        "call",
        side_effect=((0, "Safe mode is ON"), (0, "Safe mode is OFF")),
      ) as shell_call, \
      patch.object(HDFS_NAMENODE.time, "sleep") as sleep:
      HDFS_NAMENODE.wait_for_safemode_off(
        "hdfs", retries=2, sleep_seconds=1
      )

    self.assertEqual(
      [
        call(
          command,
          user="hdfs",
          logoutput=True,
          env=None,
          timeout=60,
          timeout_kill_strategy=HDFS_NAMENODE.TerminateStrategy.KILL_PROCESS_GROUP,
          shell=False,
        )
      ]
      * 2,
      shell_call.call_args_list,
    )
    self.assertEqual([call(1), call(0)], sleep.call_args_list)

  def test_refresh_proxy_users_uses_configured_hdfs_argv(self):
    params = params_module(
      security_enabled=False,
      dfs_ha_enabled=True,
      namenode_rpc="namenode.example.com:8020; touch /tmp/unsafe",
      namenode_address="hdfs://cluster",
      hadoop_conf_dir="/etc/hadoop/conf",
      hadoop_bin_dir="/usr/bin",
      hdfs_user="hdfs",
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(HDFS_NAMENODE, "Execute") as execute:
      HDFS_NAMENODE.refreshProxyUsers()

    execute.assert_called_once_with(
      (
        "hdfs",
        "--config",
        "/etc/hadoop/conf",
        "dfsadmin",
        "-fs",
        "hdfs://namenode.example.com:8020; touch /tmp/unsafe",
        "-refreshSuperUserGroupsConfiguration",
      ),
      user="hdfs",
      path=["/usr/bin"],
      environment=None,
      timeout=120,
      timeout_kill_strategy=HDFS_NAMENODE.TerminateStrategy.KILL_PROCESS_GROUP,
    )

  def test_hdfs_kerberos_environment_cleans_up_after_command_failure(self):
    params = params_module(
      security_enabled=True,
      hdfs_user="hdfs",
      user_group="hadoop",
      tmp_dir="/var/lib/ambari-agent/tmp",
      kinit_path_local="/usr/bin/kinit",
      hdfs_user_keytab="/etc/security/keytabs/hdfs.headless.keytab",
      hdfs_principal_name="hdfs@example.com",
    )
    cache = MagicMock(environment={"KRB5CCNAME": "FILE:/private/krb5cc"})
    cache_context = MagicMock()
    cache_context.__enter__.return_value = cache
    with patch.object(
      HDFS_KERBEROS,
      "PrivateKerberosCache",
      return_value=cache_context,
    ) as private_cache:
      with self.assertRaisesRegex(Fail, "command failed"):
        with HDFS_KERBEROS.hdfs_kerberos_environment(
          params, "ambari-hdfs-test-"
        ) as environment:
          self.assertEqual(
            {"KRB5CCNAME": "FILE:/private/krb5cc"}, environment
          )
          raise Fail("command failed")

    private_cache.assert_called_once_with(
      "hdfs",
      "hadoop",
      "/var/lib/ambari-agent/tmp",
      "ambari-hdfs-test-",
    )
    cache.kinit.assert_called_once_with(
      "/usr/bin/kinit",
      "/etc/security/keytabs/hdfs.headless.keytab",
      "hdfs@example.com",
    )
    self.assertIs(cache_context.__exit__.call_args.args[0], Fail)

  def test_private_cache_environment_reaches_jmx_process(self):
    environment = {"KRB5CCNAME": "FILE:/private/krb5cc"}
    with patch.object(
      jmx,
      "get_user_call_output",
      return_value=(0, '{"beans": [{"ClusterId": "CID-1"}]}', ""),
    ) as get_output:
      self.assertEqual(
        "CID-1",
        jmx.get_value_from_jmx(
          "https://namenode.example.com/jmx",
          "ClusterId",
          True,
          "hdfs",
          True,
          environment=environment,
        ),
      )

    get_output.assert_called_once_with(
      [
        "curl",
        "--negotiate",
        "-u",
        ":",
        "-s",
        "--connect-timeout",
        "10",
        "--max-time",
        "12",
        "https://namenode.example.com/jmx",
      ],
      user="hdfs",
      quiet=False,
      env=environment,
    )

    hdfs_site = {
      "dfs.nameservices": "ns1",
      "dfs.ha.namenodes.ns1": "nn1",
      "dfs.http.policy": "HTTP_ONLY",
      "dfs.https.enable": "false",
      "dfs.namenode.http-address.ns1.nn1": "namenode.example.com:9870",
    }
    with patch.object(
      namenode_ha_utils,
      "get_value_from_jmx",
      return_value="CID-1",
    ) as get_jmx:
      self.assertEqual(
        "CID-1",
        namenode_ha_utils.get_hdfs_cluster_id_from_jmx(
          hdfs_site, True, "hdfs", environment=environment
        ),
      )
    self.assertEqual(environment, get_jmx.call_args.kwargs["environment"])

    with patch.object(
      namenode_ha_utils,
      "all_jmx_namenode_addresses",
      return_value=[
        (
          "nn1",
          "namenode.example.com:8020",
          "http://namenode.example.com:9870/jmx?qry={0}",
        )
      ],
    ), patch.object(
      namenode_ha_utils,
      "get_value_from_jmx",
      return_value=None,
    ), patch.object(
      namenode_ha_utils.shell,
      "call",
      return_value=(0, "active"),
    ) as fallback:
      active, standby, unknown = (
        namenode_ha_utils.get_namenode_states_noretries(
          hdfs_site,
          True,
          "hdfs",
          environment=environment,
        )
      )

    self.assertEqual([("nn1", "namenode.example.com:8020")], active)
    self.assertEqual([], standby)
    self.assertEqual([], unknown)
    self.assertEqual(environment, fallback.call_args.kwargs["env"])

  def test_private_cache_preserves_service_specific_credentials(self):
    params = params_module(
      security_enabled=True,
      hdfs_user="hdfs",
      user_group="hadoop",
      tmp_dir="/var/lib/ambari-agent/tmp",
      kinit_path_local="/usr/bin/kinit",
      hdfs_user_keytab="/etc/security/keytabs/hdfs.headless.keytab",
      hdfs_principal_name="hdfs@example.com",
    )
    cache = MagicMock(environment={"KRB5CCNAME": "FILE:/private/nn-krb5cc"})
    cache_context = MagicMock()
    cache_context.__enter__.return_value = cache
    with patch.object(
      HDFS_KERBEROS,
      "PrivateKerberosCache",
      return_value=cache_context,
    ):
      with HDFS_KERBEROS.hdfs_kerberos_environment(
        params,
        "ambari-hdfs-nn-test-",
        keytab="/etc/security/keytabs/nn.service.keytab",
        principal="nn/host.example.com@EXAMPLE.COM",
      ):
        pass

    cache.kinit.assert_called_once_with(
      "/usr/bin/kinit",
      "/etc/security/keytabs/nn.service.keytab",
      "nn/host.example.com@EXAMPLE.COM",
    )

  def test_secure_datanode_upgrade_passes_private_cache_to_command(self):
    params = params_module(
      security_enabled=True,
      hdfs_user="hdfs",
      dn_keytab="/etc/security/keytabs/dn.service.keytab",
      dn_principal_name="dn/host.example.com@EXAMPLE.COM",
      dfs_dn_ipc_address="host.example.com:9867",
    )

    @contextmanager
    def private_environment(*args, **kwargs):
      yield {"KRB5CCNAME": "FILE:/private/dn-krb5cc"}

    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        DATANODE_UPGRADE,
        "get_dfsadmin_base_command",
        return_value=("hdfs", "dfsadmin"),
      ), \
      patch.object(
        DATANODE_UPGRADE,
        "hdfs_kerberos_environment",
        side_effect=private_environment,
      ), \
      patch.object(
        DATANODE_UPGRADE.shell,
        "call",
        return_value=(1, "denied"),
      ) as shell_call:
      self.assertFalse(
        DATANODE_UPGRADE.pre_rolling_upgrade_shutdown("hdfs")
      )

    shell_call.assert_called_once_with(
      (
        "hdfs",
        "dfsadmin",
        "-shutdownDatanode",
        "host.example.com:9867",
        "upgrade",
      ),
      user="hdfs",
      env={"KRB5CCNAME": "FILE:/private/dn-krb5cc"},
      timeout=120,
      timeout_kill_strategy=DATANODE_UPGRADE.TerminateStrategy.KILL_PROCESS_GROUP,
      shell=False,
    )

  def test_safemode_wait_failure_is_not_reported_as_success(self):
    params = params_module(security_enabled=False, hdfs_user="hdfs")
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        HDFS_NAMENODE,
        "get_dfsadmin_base_command",
        return_value=("hdfs", "dfsadmin"),
      ), \
      patch.object(
        HDFS_NAMENODE.shell,
        "call",
        return_value=(1, "connection failed"),
      ):
      with self.assertRaisesRegex(Fail, "did not leave safemode"):
        HDFS_NAMENODE.wait_for_safemode_off(
          "hdfs", retries=1, sleep_seconds=1
        )

  def test_hdfs_scripts_have_no_retired_command_wrappers(self):
    script_sources = "\n".join(
      path.read_text(encoding="utf-8") for path in SCRIPTS.glob("*.py")
    )
    self.assertNotIn("ExecuteHDFS", script_sources)
    self.assertNotIn("as_user(save_namespace_cmd", script_sources)
    self.assertNotIn("restore_snapshot", script_sources)
    self.assertNotIn("cached_kinit_executor", script_sources)
    self.assertNotIn("nn_kinit_cmd", script_sources)
    self.assertNotIn("dn_kinit_cmd", script_sources)
    self.assertNotIn("hdfs_kinit_cmd", script_sources)

  def test_metadata_matches_bigtop_hadoop_and_os_packages(self):
    root = ET.parse(HDFS / "metainfo.xml").getroot()
    service = root.find("./services/service")
    self.assertEqual("3.3.6-1", service.findtext("version"))

    packages_by_family = {}
    for os_specific in service.findall("./osSpecifics/osSpecific"):
      packages_by_family[os_specific.findtext("osFamily")] = [
        package.findtext("name")
        for package in os_specific.findall("./packages/package")
      ]

    rpm_packages = packages_by_family["redhat8,redhat9,openeuler22"]
    self.assertIn("hadoop_${stack_version}-libhdfs", rpm_packages)
    self.assertNotIn("snappy-devel", rpm_packages)
    self.assertNotIn("libtirpc-devel", rpm_packages)
    self.assertEqual(
      [
        "hadoop-${stack_version}",
        "hadoop-${stack_version}-client",
        "libhdfs0",
        "ranger-${stack_version}-hdfs-plugin",
      ],
      packages_by_family["ubuntu22"],
    )

  def test_core_site_has_no_hortonworks_or_hdp_user_agent(self):
    core_site = (HDFS / "configuration/core-site.xml").read_text(
      encoding="utf-8"
    )
    self.assertNotIn("Hortonworks", core_site)
    self.assertNotIn("HDP", core_site)
    self.assertEqual(3, core_site.count("Apache Ambari"))

  def test_hadoop_env_uses_jdk17_runtime_options(self):
    hadoop_env = (HDFS / "configuration/hadoop-env.xml").read_text(
      encoding="utf-8"
    )
    self.assertIn("-XX:+UseG1GC", hadoop_env)
    self.assertIn("-Xlog:gc*:", hadoop_env)
    for obsolete in (
      "UseConcMarkSweepGC",
      "CMSInitiatingOccupancyFraction",
      "MaxPermSize",
      "HADOOP_JOBTRACKER_OPTS",
      "HADOOP_TASKTRACKER_OPTS",
      "namenode_opt_permsize",
      "namenode_opt_maxpermsize",
    ):
      with self.subTest(obsolete=obsolete):
        self.assertNotIn(obsolete, hadoop_env)

  def test_hadoop_policy_matches_hadoop_336_rpc_providers(self):
    root = ET.parse(HDFS / "configuration/hadoop-policy.xml").getroot()
    policy_names = {item.findtext("name") for item in root.findall("property")}
    for required in (
      "security.datanode.lifeline.protocol.acl",
      "security.ha.service.protocol.acl",
      "security.interqjournal.service.protocol.acl",
      "security.mrhs.admin.refresh.protocol.acl",
      "security.mrhs.client.protocol.acl",
      "security.qjournal.service.protocol.acl",
      "security.reconfiguration.protocol.acl",
      "security.refresh.callqueue.protocol.acl",
      "security.refresh.generic.protocol.acl",
      "security.refresh.user.mappings.protocol.acl",
      "security.zkfc.protocol.acl",
    ):
      with self.subTest(required=required):
        self.assertIn(required, policy_names)
    for obsolete in (
      "security.admin.operations.protocol.acl",
      "security.inter.tracker.protocol.acl",
      "security.refresh.usertogroups.mappings.protocol.acl",
    ):
      with self.subTest(obsolete=obsolete):
        self.assertNotIn(obsolete, policy_names)

    core_site = (HDFS / "configuration/core-site.xml").read_text(
      encoding="utf-8"
    )
    self.assertNotIn("mapreduce.jobtracker.webinterface.trusted", core_site)

  def test_hdfs_params_do_not_require_unrelated_service_configs(self):
    params_source = (SCRIPTS / "params_linux.py").read_text(encoding="utf-8")
    for obsolete in (
      'config["configurations"]["falcon-env"]',
      'config["configurations"]["hbase-env"]',
      'config["configurations"]["hive-env"]',
      'config["configurations"]["oozie-env"]',
      'config["configurations"]["yarn-env"]',
      'default("/clusterHostInfo/ganglia_server_hosts"',
      'default("/clusterHostInfo/jtnode_hosts"',
    ):
      with self.subTest(obsolete=obsolete):
        self.assertNotIn(obsolete, params_source)
    for heap_property in (
      "namenode_heapsize",
      "namenode_opt_newsize",
      "namenode_opt_maxnewsize",
      "dtnode_heapsize",
    ):
      with self.subTest(heap_property=heap_property):
        self.assertIn(
          f'"/configurations/hadoop-env/{heap_property}"', params_source
        )

  def test_hdfs_advisor_uses_bigtop_names_and_current_topology(self):
    advisor_source = (HDFS / "service_advisor.py").read_text(encoding="utf-8")
    self.assertNotIn("HDP", advisor_source)
    self.assertNotIn("Hortonworks", advisor_source)
    self.assertNotIn("validateDuplicateHeapConfigurations", advisor_source)
    self.assertIn(
      '("dfs.namenode.name.dir", "NAMENODE", '
      '"/hadoop/hdfs/namenode", "multi")',
      advisor_source,
    )
    for property_name in (
      "dfs.datanode.address",
      "dfs.datanode.http.address",
      "dfs.datanode.https.address",
      "dfs.datanode.ipc.address",
      "dfs.journalnode.http-address",
      "dfs.journalnode.https-address",
    ):
      with self.subTest(property_name=property_name):
        self.assertIn(f'      "{property_name}",', advisor_source)
    for rejected_authority_check in (
      'target != target.strip()',
      'parsed.username is None',
      'not parsed.path',
      '0 < port <= 65535',
    ):
      with self.subTest(rejected_authority_check=rejected_authority_check):
        self.assertIn(rejected_authority_check, advisor_source)
    self.assertIn(
      'str(services.get("gpl-license-accepted", "false")).lower() == "true"',
      advisor_source,
    )
    self.assertIn('datanode_https_address = "dfs.datanode.https.address"', advisor_source)

  def test_hdfs_advisor_validates_host_port_authorities(self):
    for authority in (
      "namenode.example.com:8020",
      "0.0.0.0:9866",
      "[::1]:9870",
    ):
      with self.subTest(authority=authority):
        self.assertTrue(
          HDFS_ADVISOR.HDFSValidator.is_valid_host_port_authority(authority)
        )

    for authority in (
      None,
      "",
      " namenode.example.com:8020",
      "http://namenode.example.com:8020",
      "user@namenode.example.com:8020",
      "namenode.example.com:8020/path",
      "namenode.example.com",
      "namenode.example.com:0",
      "namenode.example.com:65536",
      "name node.example.com:8020",
    ):
      with self.subTest(authority=authority):
        self.assertFalse(
          HDFS_ADVISOR.HDFSValidator.is_valid_host_port_authority(authority)
        )

  def test_hdfs_advisor_parent_load_failure_preserves_original_cause(self):
    advisor_path = HDFS / "service_advisor.py"
    with patch.dict(
        os.environ, {"BASE_SERVICE_ADVISOR": "/missing/service_advisor.py"}
      ), \
      patch("builtins.open", mock_open(read_data=b"")), \
      patch.object(import_utils, "load_module", side_effect=ValueError("bad parent")):
      with self.assertRaisesRegex(
        RuntimeError,
        "Failed to load parent service advisor /missing/service_advisor.py",
      ) as raised:
        load_module("bigtop_hdfs_broken_advisor", advisor_path)

    self.assertIsInstance(raised.exception.__cause__, ValueError)

  def test_rolling_restart_timeout_uses_integer_ceiling(self):
    for timeout, expected_retries in ((1, 1), (29, 1), (30, 1), (31, 2)):
      with self.subTest(timeout=timeout):
        options = HDFS_NAMENODE.get_safemode_wait_options(True, str(timeout))
        self.assertEqual(30, options["afterwait_sleep"])
        self.assertEqual(expected_retries, options["retries"])
        self.assertIsInstance(options["retries"], int)
        self.assertEqual(30, options["sleep_seconds"])

  def test_rolling_restart_timeout_rejects_invalid_values(self):
    for timeout in ("not-a-number", "30.5", 0, -1, True):
      with self.subTest(timeout=timeout):
        with self.assertRaisesRegex(Fail, "must be a positive integer"):
          HDFS_NAMENODE.get_safemode_wait_options(True, timeout)

  def test_rolling_restart_without_timeout_uses_legacy_wait_defaults(self):
    for timeout in (None, "", "  "):
      with self.subTest(timeout=timeout):
        self.assertEqual(
          {}, HDFS_NAMENODE.get_safemode_wait_options(True, timeout)
        )

  def test_non_rolling_restart_does_not_consume_timeout(self):
    self.assertEqual(
      {}, HDFS_NAMENODE.get_safemode_wait_options(False, "not-a-number")
    )

  def test_shared_hdfs_tmp_directory_has_sticky_bit(self):
    hdfs_resource = MagicMock()
    params = params_module(
      HdfsResource=hdfs_resource,
      hdfs_tmp_dir="/tmp",
      hdfs_user="hdfs",
      smoke_hdfs_user_dir="/user/ambari-qa",
      smoke_user="ambari-qa",
      smoke_hdfs_user_mode=0o770,
    )

    with patch.dict(sys.modules, {"params": params}):
      HDFS_NAMENODE.create_hdfs_directories("nameservice1")

    self.assertEqual(
      call(
        "/tmp",
        type="directory",
        action="create_on_execute",
        owner="hdfs",
        mode=0o1777,
        nameservices=["nameservice1"],
      ),
      hdfs_resource.call_args_list[0],
    )

  def test_secure_journalnode_failure_preserves_shared_tmp_sticky_bit(self):
    hdfs_resource = MagicMock()
    params = params_module(
      hdfs_tmp_dir="/tmp",
      security_enabled=True,
      kinit_path_local="/usr/bin/kinit",
      hdfs_user_keytab="/etc/security/keytabs/hdfs.headless.keytab",
      hdfs_principal_name="hdfs@example.com",
      hdfs_user="hdfs",
      HdfsResource=hdfs_resource,
      has_journalnode_hosts=True,
      journalnode_hosts=["journalnode.example.com"],
      https_only=False,
      journalnode_port=8480,
      tmp_dir="/tmp",
      smoke_user_keytab="/etc/security/keytabs/smokeuser.headless.keytab",
      smokeuser_principal="ambari-qa@example.com",
      smoke_user="ambari-qa",
      is_namenode_master=False,
    )
    env = MagicMock()

    def render(template):
      return template.format(
        hdfs_dir=params.hdfs_tmp_dir,
        unique="check-id",
        kinit_path_local=params.kinit_path_local,
        hdfs_user_keytab=params.hdfs_user_keytab,
        hdfs_principal_name=params.hdfs_principal_name,
        host=params.journalnode_hosts[0],
        journalnode_port=params.journalnode_port,
      )

    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        SERVICE_CHECK.functions, "get_unique_id_and_date", return_value="check-id"
      ), \
      patch.object(SERVICE_CHECK, "Execute"), \
      patch.object(SERVICE_CHECK, "format", side_effect=render), \
      patch.object(
        SERVICE_CHECK,
        "curl_krb_request",
        return_value=(False, "connection refused", 1),
      ) as curl:
      with self.assertRaisesRegex(
        Fail,
        "Cannot access WEB UI on: http://journalnode.example.com:8480",
      ):
        SERVICE_CHECK.HdfsServiceCheckDefault().service_check(env)

    self.assertEqual(
      call("/tmp", type="directory", action="create_on_execute", mode=0o1777),
      hdfs_resource.call_args_list[0],
    )
    self.assertEqual(
      SERVICE_CHECK.JOURNALNODE_CONNECTION_TIMEOUT,
      curl.call_args.kwargs["connection_timeout"],
    )

  def test_journalnode_web_check_uses_bounded_connection_timeout(self):
    response = MagicMock(status=200)
    connection = MagicMock()
    connection.getresponse.return_value = response
    with patch.object(
      CHECK_WEB_UI.http.client,
      "HTTPConnection",
      return_value=connection,
    ) as http_connection:
      self.assertEqual(
        200,
        CHECK_WEB_UI.make_connection(
          "journalnode.example.com", 8480, False, 7
        ),
      )

    http_connection.assert_called_once_with(
      "journalnode.example.com", 8480, timeout=7
    )
    connection.close.assert_called_once_with()
    for timeout in (0, -1, "invalid", float("inf")):
      with self.subTest(timeout=timeout), \
        self.assertRaises(argparse.ArgumentTypeError):
        CHECK_WEB_UI.positive_timeout(timeout)

  def test_namenode_backup_failure_is_fail_closed(self):
    params = params_module(
      dfs_name_dir="/namenode",
      namenode_backup_dir="/backup",
      stack_version_unformatted="3.2.0",
    )

    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        NAMENODE_UPGRADE.os.path,
        "isdir",
        side_effect=lambda path: path == "/namenode/current",
      ), \
      patch.object(NAMENODE_UPGRADE.os, "makedirs"), \
      patch.object(NAMENODE_UPGRADE, "Execute", side_effect=OSError("disk full")), \
      patch.object(NAMENODE_UPGRADE, "Directory") as cleanup_directory, \
      patch.object(
        NAMENODE_UPGRADE, "get_unique_id_and_date", return_value="backup-id"
      ), \
      patch.object(NAMENODE_UPGRADE.Logger, "info"), \
      patch.object(NAMENODE_UPGRADE.Logger, "error") as logger_error:
      with self.assertRaisesRegex(Fail, "Could not backup the NameNode Name Dir"):
        NAMENODE_UPGRADE.prepare_upgrade_backup_namenode_dir()

    logger_error.assert_called_once()
    self.assertIn("/namenode/current", logger_error.call_args.args[0])
    cleanup_directory.assert_called_once_with(
      "/backup/3.2.0/namenode_backup-id_1/", action="delete"
    )

  def test_incomplete_backup_is_not_reused_on_same_identifier_retry(self):
    params = params_module(
      dfs_name_dir="/namenode",
      namenode_backup_dir="/backup",
      stack_version_unformatted="3.2.0",
    )
    backup_destination = "/backup/3.2.0/namenode_backup-id_1/"
    destination_state = {"exists": False}

    def create_destination(path):
      self.assertEqual(backup_destination, path)
      destination_state["exists"] = True

    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        NAMENODE_UPGRADE.os.path,
        "isdir",
        side_effect=lambda path: path == "/namenode/current",
      ), \
      patch.object(
        NAMENODE_UPGRADE.os.path,
        "lexists",
        side_effect=lambda path: destination_state["exists"],
      ), \
      patch.object(
        NAMENODE_UPGRADE.os, "makedirs", side_effect=create_destination
      ) as makedirs, \
      patch.object(
        NAMENODE_UPGRADE, "Execute", side_effect=OSError("copy failed")
      ) as execute, \
      patch.object(
        NAMENODE_UPGRADE,
        "Directory",
        side_effect=OSError("cleanup failed"),
      ) as cleanup_directory, \
      patch.object(
        NAMENODE_UPGRADE, "get_unique_id_and_date", return_value="backup-id"
      ), \
      patch.object(NAMENODE_UPGRADE.Logger, "info"), \
      patch.object(NAMENODE_UPGRADE.Logger, "warning"), \
      patch.object(NAMENODE_UPGRADE.Logger, "error"):
      for attempt in (1, 2):
        with self.subTest(attempt=attempt):
          with self.assertRaisesRegex(
            Fail, "Could not backup the NameNode Name Dir"
          ):
            NAMENODE_UPGRADE.prepare_upgrade_backup_namenode_dir()

    makedirs.assert_called_once_with(backup_destination)
    execute.assert_called_once_with(
      ("cp", "-ar", "/namenode/current", backup_destination),
      sudo=True,
      timeout=300,
      timeout_kill_strategy=NAMENODE_UPGRADE.TerminateStrategy.KILL_PROCESS_GROUP,
    )
    cleanup_directory.assert_called_once_with(backup_destination, action="delete")

  def test_backup_failure_stops_upgrade_before_finalize_and_marker(self):
    params = params_module(
      security_enabled=False,
      skip_namenode_save_namespace_express=False,
      skip_namenode_namedir_backup_express=False,
    )
    env = MagicMock()
    namenode = NAMENODE.NameNodeDefault()

    with patch.dict(sys.modules, {"params": params}), \
      patch.object(namenode, "get_hdfs_binary", return_value="hdfs"), \
      patch.object(NAMENODE.Logger, "info"), \
      patch.object(NAMENODE_UPGRADE, "prepare_upgrade_check_for_previous_dir"), \
      patch.object(NAMENODE_UPGRADE, "prepare_upgrade_enter_safe_mode"), \
      patch.object(NAMENODE_UPGRADE, "prepare_upgrade_save_namespace"), \
      patch.object(
        NAMENODE_UPGRADE,
        "prepare_upgrade_backup_namenode_dir",
        side_effect=Fail("backup failed"),
      ) as backup, \
      patch.object(
        NAMENODE_UPGRADE, "prepare_upgrade_finalize_previous_upgrades"
      ) as finalize, \
      patch.object(NAMENODE_UPGRADE, "prepare_rolling_upgrade") as rolling_upgrade, \
      patch.object(NAMENODE_UPGRADE, "create_upgrade_marker") as create_marker:
      with self.assertRaisesRegex(Fail, "backup failed"):
        namenode.prepare_express_upgrade(env)

    backup.assert_called_once_with()
    finalize.assert_not_called()
    rolling_upgrade.assert_not_called()
    create_marker.assert_not_called()

  def test_previous_upgrade_finalize_accepts_only_success_or_no_upgrade(self):
    params = params_module(hdfs_user="hdfs")
    expected_command = (
      "hdfs",
      "dfsadmin",
      "-fs",
      "hdfs://cluster",
      "-rollingUpgrade",
      "finalize",
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(NAMENODE_UPGRADE.Logger, "info"), \
      patch.object(NAMENODE_UPGRADE.shell, "call") as shell_call:
      for return_code in (0, 255):
        with self.subTest(return_code=return_code):
          shell_call.reset_mock()
          shell_call.return_value = (return_code, "")
          NAMENODE_UPGRADE.prepare_upgrade_finalize_previous_upgrades(
            "hdfs", environment={"KRB5CCNAME": "FILE:/private/cache"}
          )
          shell_call.assert_called_once_with(
            expected_command,
            logoutput=True,
            user="hdfs",
            env={"KRB5CCNAME": "FILE:/private/cache"},
            timeout=120,
            timeout_kill_strategy=(
              NAMENODE_UPGRADE.TerminateStrategy.KILL_PROCESS_GROUP
            ),
            shell=False,
          )

      shell_call.return_value = (1, "finalize failed")
      with self.assertRaisesRegex(Fail, "exited with status 1"):
        NAMENODE_UPGRADE.prepare_upgrade_finalize_previous_upgrades("hdfs")

  def test_upgrade_marker_creation_failure_is_fail_closed(self):
    with patch.object(
      NAMENODE_UPGRADE,
      "get_upgrade_in_progress_marker",
      return_value="/private/namenode-upgrade-marker",
    ), patch.object(NAMENODE_UPGRADE.os.path, "isfile", return_value=False), \
      patch.object(
        NAMENODE_UPGRADE, "File", side_effect=OSError("read-only filesystem")
      ):
      with self.assertRaisesRegex(Fail, "Unable to create NameNode upgrade marker"):
        NAMENODE_UPGRADE.create_upgrade_marker()

  def test_service_advisor_has_intact_asf_license_header(self):
    header = (HDFS / "service_advisor.py").read_text(encoding="utf-8")[:800]
    self.assertIn("distributed with this work for additional information", header)
    self.assertNotIn("disass HDFSRecommender", header)


if __name__ == "__main__":
  unittest.main()
