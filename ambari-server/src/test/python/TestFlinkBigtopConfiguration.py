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
from xml.etree import ElementTree


FLINK = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/FLINK"
)
FLINK_33 = FLINK.parents[2] / "3.3.0/services/FLINK"
FLINK_34 = FLINK.parents[2] / "3.4.0/services/FLINK"
SCRIPTS = FLINK / "package/scripts"


def load_module(module_name, path):
  spec = importlib.util.spec_from_file_location(module_name, path)
  module = importlib.util.module_from_spec(spec)
  spec.loader.exec_module(module)
  return module


def params_module(**values):
  module = ModuleType("params")
  for name, value in values.items():
    setattr(module, name, value)
  return module


SETUP_FLINK = load_module("bigtop_setup_flink", SCRIPTS / "setup_flink.py")
FLINK_ADVISOR = load_module("bigtop_flink_advisor", FLINK / "service_advisor.py")


def base_params():
  return params_module(
    flink_etc_dir="/etc/flink",
    flink_config_dir="/etc/flink/conf",
    flink_cli_log_dir="/var/log/flink-cli",
    flink_pid_dir="/var/run/flink",
    flink_log_dir="/var/log/flink",
    flink_user="flink",
    user_group="hadoop",
    flink_hdfs_user_dir="/user/flink",
    HdfsResource=MagicMock(),
    flink_conf_template="env.java.home: {{java_home_yaml}}",
    flink_log4j_properties="rootLogger.level = INFO",
    flink_log4j_cli_properties="rootLogger.level = INFO",
    flink_log4j_console_properties="rootLogger.level = INFO",
    flink_log4j_session_properties="rootLogger.level = INFO",
  )


class TestFlinkConfigurationResources(unittest.TestCase):
  def test_history_server_configuration_has_restrictive_ownership_and_modes(self):
    params = base_params()
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(SETUP_FLINK, "Directory") as directory, \
      patch.object(SETUP_FLINK, "File") as file_resource:
      SETUP_FLINK.setup_flink(None, "historyserver", action="config")

    directories = {
      resource_call.args[0]: resource_call
      for resource_call in directory.call_args_list
    }
    self.assertEqual(0o750, directories["/var/run/flink"].kwargs["mode"])
    self.assertEqual(0o750, directories["/var/log/flink"].kwargs["mode"])
    self.assertEqual(0o1770, directories["/var/log/flink-cli"].kwargs["mode"])
    config_call = next(
      resource_call
      for resource_call in file_resource.call_args_list
      if resource_call.args[0] == "/etc/flink/conf/flink-conf.yaml"
    )
    self.assertEqual("root", config_call.kwargs["owner"])
    self.assertEqual("hadoop", config_call.kwargs["group"])
    self.assertEqual(0o640, config_call.kwargs["mode"])
    standard_config_call = next(
      resource_call
      for resource_call in file_resource.call_args_list
      if resource_call.args[0] == "/etc/flink/conf/config.yaml"
    )
    self.assertEqual("delete", standard_config_call.kwargs["action"])
    hdfs_call = params.HdfsResource.call_args_list[0]
    self.assertEqual(0o750, hdfs_call.kwargs["mode"])
    self.assertEqual("hadoop", hdfs_call.kwargs["group"])

  def test_client_does_not_create_server_pid_log_or_hdfs_directories(self):
    params = base_params()
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(SETUP_FLINK, "Directory") as directory, \
      patch.object(SETUP_FLINK, "File"):
      SETUP_FLINK.setup_flink(None, "client", action="config")

    directories = {resource_call.args[0] for resource_call in directory.call_args_list}
    self.assertNotIn("/var/run/flink", directories)
    self.assertNotIn("/var/log/flink", directories)
    params.HdfsResource.assert_not_called()


class TestFlinkStackMetadata(unittest.TestCase):
  def test_versions_preserve_base_and_overlay_history(self):
    versions = []
    for service_dir in (FLINK, FLINK_33, FLINK_34):
      root = ElementTree.parse(service_dir / "metainfo.xml").getroot()
      versions.append(root.findtext("./services/service/version"))
    self.assertEqual(["1.15.3-1", "1.19.3-1", "1.20.0-1"], versions)

  def test_rpm_deb_and_client_yaml_contracts_are_declared(self):
    root = ElementTree.parse(FLINK / "metainfo.xml").getroot()
    packages = {
      os_specific.findtext("osFamily"): os_specific.findtext(
        "./packages/package/name"
      )
      for os_specific in root.findall("./services/service/osSpecifics/osSpecific")
    }
    self.assertEqual(
      "flink_${stack_version}",
      packages["redhat7,redhat8,redhat9,openeuler22"],
    )
    self.assertEqual(
      "flink-${stack_version}",
      packages["debian10,debian11,ubuntu20,ubuntu22"],
    )
    config_file = root.find(
      "./services/service/components/component[2]/configFiles/configFile"
    )
    self.assertEqual("env", config_file.findtext("type"))
    self.assertEqual("flink-conf.yaml", config_file.findtext("fileName"))

  def test_yaml_template_quotes_dynamic_strings_and_log4j_matches_flink_119(self):
    config_source = (FLINK / "configuration/flink-conf.xml").read_text()
    self.assertIn("env.java.home: {{java_home_yaml}}", config_source)
    self.assertIn(
      "env.java.opts.all: --add-exports=java.base/sun.net.util=ALL-UNNAMED",
      config_source,
    )
    self.assertIn(
      "historyserver.archive.fs.dir: {{historyserver_archive_fs_dir_yaml}}",
      config_source,
    )
    self.assertNotIn("env.java.home: {{java_home}}", config_source)

    for config_name in (
      "flink-log4j-cli-properties.xml",
      "flink-log4j-console-properties.xml",
      "flink-log4j-properties.xml",
      "flink-log4j-session-properties.xml",
    ):
      source = (FLINK / "configuration" / config_name).read_text()
      self.assertIn("rootLogger.level = ${env:ROOT_LOG_LEVEL:-INFO}", source)
    for config_name in (
      "flink-log4j-console-properties.xml",
      "flink-log4j-properties.xml",
    ):
      source = (FLINK / "configuration" / config_name).read_text()
      self.assertIn("logger.pekko.name = org.apache.pekko", source)
      self.assertNotIn("logger.akka.name", source)

  def test_advisor_rejects_unsafe_archives_and_service_directories(self):
    self.assertTrue(FLINK_ADVISOR._safe_hdfs_uri("hdfs:///completed-jobs/"))
    self.assertFalse(FLINK_ADVISOR._safe_hdfs_uri("hdfs://bad$(id)/jobs"))
    self.assertTrue(FLINK_ADVISOR._dedicated_directory("/var/run/flink"))
    self.assertFalse(FLINK_ADVISOR._dedicated_directory("/var/run"))
    self.assertFalse(FLINK_ADVISOR._dedicated_directory("/etc/flink"))


if __name__ == "__main__":
  unittest.main()
