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
import json
from pathlib import Path
import sys
from types import ModuleType
import unittest
from unittest.mock import MagicMock, patch
from xml.etree import ElementTree

from resource_management.core.exceptions import Fail
from resource_management.core.environment import Environment
from resource_management.core.logger import Logger


LIVY = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/LIVY"
)
SCRIPTS = LIVY / "package/scripts"
LIVY_33 = LIVY.parents[2] / "3.3.0/services/LIVY"


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


SETUP_LIVY = load_module("bigtop_setup_livy", SCRIPTS / "setup_livy.py")


def params_module(**values):
  return dependency_module("params", **values)


class TestLivyConfigurationContract(unittest.TestCase):
  def setUp(self):
    Logger.initialize_logger()
    self._environment = Environment(str(LIVY / "package"), test_mode=True)
    self._environment.__enter__()

  def tearDown(self):
    self._environment.__exit__(None, None, None)
  def test_33_overlay_matches_bigtop_livy_version(self):
    root = ElementTree.parse(LIVY_33 / "metainfo.xml").getroot()
    self.assertEqual("LIVY", root.findtext("./services/service/name"))
    self.assertEqual("0.8.0-1", root.findtext("./services/service/version"))

    inherited_root = ElementTree.parse(LIVY / "metainfo.xml").getroot()
    self.assertEqual(
      "0.7.1-1", inherited_root.findtext("./services/service/version")
    )

  def test_package_metadata_matches_bigtop_livy_artifacts(self):
    root = ElementTree.parse(LIVY / "metainfo.xml").getroot()
    os_packages = {
      node.findtext("osFamily"): [
        package.findtext("name") for package in node.findall("packages/package")
      ]
      for node in root.findall("./services/service/osSpecifics/osSpecific")
    }
    self.assertEqual(
      ["spark_${stack_version}-core", "spark_${stack_version}-python", "livy"],
      os_packages["redhat8,redhat9,openeuler22"],
    )
    self.assertEqual(
      ["spark-${stack_version}-core", "spark-${stack_version}-python", "livy"],
      os_packages["ubuntu22"],
    )

    stack_packages = json.loads(
      (LIVY.parents[1] / "properties/stack_packages.json").read_text(
        encoding="utf-8"
      )
    )["BIGTOP"]["stack-select"]["LIVY"]
    self.assertEqual({"LIVY_SERVER"}, set(stack_packages))
    server_packages = stack_packages["LIVY_SERVER"]
    self.assertEqual("livy-server", server_packages["STACK-SELECT-PACKAGE"])
    for package_scope in ("INSTALL", "PATCH", "STANDARD"):
      self.assertEqual(["livy"], server_packages[package_scope])

  def test_metainfo_declares_only_configs_consumed_by_livy_scripts(self):
    root = ElementTree.parse(LIVY / "metainfo.xml").getroot()
    dependencies = {
      node.text
      for node in root.findall(
        "./services/service/configuration-dependencies/config-type"
      )
    }
    self.assertTrue(
      {"core-site", "hdfs-site", "hadoop-env", "yarn-site"}.issubset(
        dependencies
      )
    )
    self.assertFalse(
      {"spark-thrift-fairscheduler", "spark-hive-site-override"}
      & dependencies
    )

  def test_livy_conf_uses_080_canonical_property_names_without_duplicates(self):
    root = ElementTree.parse(LIVY / "configuration/livy-conf.xml").getroot()
    names = [property_node.findtext("name") for property_node in root]
    self.assertEqual(len(names), len(set(names)))
    self.assertIn("livy.repl.enable-hive-context", names)
    self.assertIn("livy.server.csrf-protection.enabled", names)
    self.assertNotIn("livy.repl.enableHiveContext", names)
    self.assertNotIn("livy.server.csrf_protection.enabled", names)
    self.assertEqual(1, names.count("livy.impersonation.enabled"))

  def test_env_template_quotes_all_configured_shell_values(self):
    root = ElementTree.parse(LIVY / "configuration/livy-env.xml").getroot()
    content = next(
      node.findtext("value")
      for node in root.findall("property")
      if node.findtext("name") == "content"
    )
    for value in (
      "spark_home_shell",
      "spark_conf_dir_shell",
      "java_home_shell",
      "hadoop_conf_dir_shell",
      "livy_log_dir_shell",
      "livy_pid_dir_shell",
      "livy_user_shell",
    ):
      self.assertIn("{{" + value + "}}", content)
    self.assertIn("umask 0027", content)
    self.assertIn("export LIVY_IDENT_STRING=", content)

  def test_params_no_longer_contains_copied_spark_hive_daemon_contracts(self):
    source = (SCRIPTS / "params.py").read_text(encoding="utf-8")
    for dead_name in (
      "spark_history_server_pid_file",
      "spark_thrift_server_pid_file",
      "spark_service_check_cmd",
      "hive_metastore_db_type",
      "spark_thrift_fairscheduler_content",
      "AMBARI_SUDO_BINARY",
    ):
      self.assertNotIn(dead_name, source)
    self.assertIn("quote_bash_args", source)

  def test_packaged_templates_match_livy_080_property_and_log_contracts(self):
    blacklist = (LIVY / "configuration/livy-spark-blacklist.xml").read_text(
      encoding="utf-8"
    )
    self.assertIn("livy.rsc.server.idle-timeout", blacklist)
    self.assertNotIn("livy.rsc.server.idle_timeout", blacklist)

    log_input = (
      LIVY / "package/templates/input.config-livy.json.j2"
    ).read_text(encoding="utf-8")
    self.assertIn("%d{yy/MM/dd HH:mm:ss} %p %c{1}: %m%n", log_input)
    self.assertIn("%{SPARK_DATESTAMP:logtime}", log_input)
    self.assertIn('"target_date_pattern":"yy/MM/dd HH:mm:ss"', log_input)

  def test_setup_protects_runtime_and_configuration_paths(self):
    hdfs_resource = MagicMock()
    params = params_module(
      livy_pid_dir="/var/run/livy",
      livy_log_dir="/var/log/livy",
      livy_conf="/etc/livy/conf",
      livy_user="livy",
      livy_group="livy",
      livy_hdfs_user_dir="/user/livy",
      livy_recovery_store="filesystem",
      livy_recovery_dir="/livy-recovery",
      HdfsResource=hdfs_resource,
      livy_env_sh="env",
      config={
        "configurations": {
          "livy-client-conf": {"client.key": "value"},
          "livy-conf": {"livy.server.port": "8999"},
        }
      },
      livy_log4j_properties="log4j",
      livy_spark_blacklist_properties="blacklist",
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(SETUP_LIVY, "Directory") as directory, \
      patch.object(SETUP_LIVY, "File") as file_resource, \
      patch.object(SETUP_LIVY, "PropertiesFile") as properties_file, \
      patch.object(SETUP_LIVY, "InlineTemplate", side_effect=lambda value: value), \
      patch.object(SETUP_LIVY, "generate_logfeeder_input_config"), \
      patch.object(SETUP_LIVY, "Template"):
      self._environment.set_params(params)
      SETUP_LIVY.setup_livy(MagicMock(), "server", action="config")

    runtime = directory.call_args_list[0]
    self.assertEqual(["/var/run/livy", "/var/log/livy"], runtime.args[0])
    self.assertEqual("livy", runtime.kwargs["group"])
    self.assertEqual(0o750, runtime.kwargs["mode"])
    config_dir = directory.call_args_list[1]
    self.assertEqual("root", config_dir.kwargs["owner"])
    self.assertEqual(0o750, config_dir.kwargs["mode"])

    env_file = file_resource.call_args_list[0]
    self.assertEqual("/etc/livy/conf/livy-env.sh", env_file.args[0])
    self.assertEqual("root", env_file.kwargs["owner"])
    self.assertEqual(0o640, env_file.kwargs["mode"])
    self.assertEqual(2, properties_file.call_count)
    for config_call in properties_file.call_args_list:
      self.assertEqual("root", config_call.kwargs["owner"])
      self.assertEqual("livy", config_call.kwargs["group"])
      self.assertEqual(0o640, config_call.kwargs["mode"])
    self.assertEqual(3, hdfs_resource.call_count)
    self.assertEqual(0o755, hdfs_resource.call_args_list[0].kwargs["mode"])
    self.assertEqual(0o700, hdfs_resource.call_args_list[1].kwargs["mode"])
    self.assertIsNone(hdfs_resource.call_args_list[-1].args[0])
    self.assertEqual("execute", hdfs_resource.call_args_list[-1].kwargs["action"])

  def test_setup_rejects_empty_filesystem_recovery_path(self):
    params = params_module(
      livy_pid_dir="/var/run/livy",
      livy_log_dir="/var/log/livy",
      livy_conf="/etc/livy/conf",
      livy_user="livy",
      livy_group="livy",
      livy_hdfs_user_dir="/user/livy",
      livy_recovery_store="filesystem",
      livy_recovery_dir=" ",
      HdfsResource=MagicMock(),
    )
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(SETUP_LIVY, "Directory"):
      with self.assertRaisesRegex(Fail, "non-empty state store URL"):
        SETUP_LIVY.setup_livy(MagicMock(), "server", action="config")


if __name__ == "__main__":
  unittest.main()
