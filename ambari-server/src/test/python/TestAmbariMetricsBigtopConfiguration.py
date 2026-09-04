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

import ast
import importlib.util
from pathlib import Path
import re
import sys
from types import SimpleNamespace
import unittest
from unittest.mock import MagicMock, patch
from xml.etree import ElementTree


SERVICE = Path(__file__).resolve().parents[2] / (
  "main/resources/common-services/AMBARI_METRICS/3.0.0"
)
SCRIPTS = SERVICE / "package/scripts"


def load_module(module_name, path):
  spec = importlib.util.spec_from_file_location(module_name, path)
  module = importlib.util.module_from_spec(spec)
  spec.loader.exec_module(module)
  return module


AMS = load_module("ambari_metrics_configuration", SCRIPTS / "ams.py")
ADVISOR = load_module(
  "ambari_metrics_configuration_advisor", SERVICE / "service_advisor.py"
)


class TestAmbariMetricsConfiguration(unittest.TestCase):
  def test_synchronous_commands_use_process_group_timeouts(self):
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
        self.assertIn("timeout", keywords, f"{script_path.name}:{node.lineno}")
        strategy = keywords.get("timeout_kill_strategy")
        self.assertIsInstance(strategy, ast.Attribute)
        self.assertEqual("KILL_PROCESS_GROUP", strategy.attr)

  def test_generated_password_and_jaas_files_are_private(self):
    ams_source = (SCRIPTS / "ams.py").read_text(encoding="utf-8")
    hbase_source = (SCRIPTS / "hbase.py").read_text(encoding="utf-8")

    ssl_resources = re.findall(
      r'XmlConfig\(\s*"ssl-server\.xml".*?\n\s*\)',
      ams_source,
      flags=re.DOTALL,
    )
    self.assertEqual(2, len(ssl_resources))
    for resource in ssl_resources:
      self.assertIn("mode=0o600", resource)

    metrics_properties = re.search(
      r'File\(\s*os\.path\.join\('
      r'params\.hbase_conf_dir, "hadoop-metrics2-hbase\.properties"\)'
      r'.*?\n\s*\)',
      hbase_source,
      flags=re.DOTALL,
    )
    self.assertIsNotNone(metrics_properties)
    self.assertIn("mode=0o600", metrics_properties.group(0))
    self.assertEqual(3, hbase_source.count("user=params.hbase_user, mode=0o600"))
    collector_jaas = re.search(
      r'"ams_collector_jaas\.conf"\)[\s\S]{0,200}?mode=0o600',
      ams_source,
    )
    self.assertIsNotNone(collector_jaas)
    monitor_ini = re.search(
      r'"\{ams_monitor_conf_dir\}/metric_monitor\.ini"\)'
      r'[\s\S]{0,200}?mode=0o600',
      ams_source,
    )
    self.assertIsNotNone(monitor_ini)

  def test_jks_password_is_passed_only_through_the_command_environment(self):
    params = SimpleNamespace(
      metric_truststore_ca_certs="ca.pem",
      metric_truststore_path="/etc/security/ams/truststore.jks",
      metric_truststore_type="jks",
      metric_truststore_password="do-not-log-this-secret",
      java64_home="/usr/lib/jvm/java-17",
      ams_user="ams",
      user_group="hadoop",
    )
    execute = MagicMock()
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(AMS.os.path, "isfile", return_value=True), \
      patch.object(AMS.os.path, "islink", return_value=False), \
      patch("tempfile.mkdtemp", return_value="/tmp/ams-truststore-private"), \
      patch.object(AMS, "Execute", execute), \
      patch.object(AMS, "File"), \
      patch.object(AMS, "Directory") as directory, \
      patch.object(AMS, "StaticFile"):
      AMS.export_ca_certs("/etc/ambari-metrics-monitor/conf")

    self.assertEqual(2, execute.call_count)
    for command_call in execute.call_args_list:
      command = command_call.args[0]
      self.assertIsInstance(command, tuple)
      self.assertNotIn(params.metric_truststore_password, command)
      self.assertEqual(
        params.metric_truststore_password,
        command_call.kwargs["environment"]["AMS_TRUSTSTORE_PASSWORD"],
      )
    directory.assert_called_once_with(
      "/tmp/ams-truststore-private", action="delete"
    )

  def test_private_key_passwords_require_input_when_ams_https_is_enabled(self):
    ssl_root = ElementTree.parse(SERVICE / "configuration/ams-ssl-server.xml")
    values = {
      element.findtext("name"): element
      for element in ssl_root.findall("property")
    }
    self.assertEqual(
      "bigdata",
      values["ssl.server.truststore.password"].findtext("value"),
    )
    for name in (
      "ssl.server.keystore.password",
      "ssl.server.keystore.keypassword",
    ):
      self.assertEqual("", values[name].findtext("value", ""))
      self.assertEqual(
        "false", values[name].findtext("value-attributes/empty-value-valid")
      )

    validator = object.__new__(ADVISOR.AMBARI_METRICSValidator)
    problems = validator.validateAmsSslServerConfigurations(
      {},
      {},
      {
        "ams-site": {
          "properties": {"timeline.metrics.service.http.policy": "HTTPS_ONLY"}
        }
      },
      {"configurations": {}},
      {},
    )
    self.assertEqual(
      {"ssl.server.keystore.password", "ssl.server.keystore.keypassword"},
      {problem["config-name"] for problem in problems},
    )
    self.assertEqual(
      [],
      validator.validateAmsSslServerConfigurations(
        {},
        {},
        {
          "ams-site": {
            "properties": {"timeline.metrics.service.http.policy": "HTTP_ONLY"}
          }
        },
        {"configurations": {}},
        {},
      ),
    )
    params_source = (SCRIPTS / "params.py").read_text(encoding="utf-8")
    self.assertIn(
      "must not be empty when Ambari Metrics HTTPS is enabled", params_source
    )


if __name__ == "__main__":
  unittest.main()
