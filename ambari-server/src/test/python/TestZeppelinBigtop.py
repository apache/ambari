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
from types import SimpleNamespace
from unittest import TestCase
from unittest.mock import MagicMock, patch
import xml.etree.ElementTree as ET

from ambari_commons import import_utils


RESOURCES = Path(__file__).resolve().parents[2] / "main" / "resources"
STACKS = RESOURCES / "stacks"
ZEPPELIN = STACKS / "BIGTOP" / "3.2.0" / "services" / "ZEPPELIN"
SCRIPTS = ZEPPELIN / "package" / "scripts"


def load_module(name, path):
  spec = importlib.util.spec_from_file_location(name, path)
  module = importlib.util.module_from_spec(spec)
  spec.loader.exec_module(module)
  return module


class TestZeppelinBigtop(TestCase):
  @classmethod
  def setUpClass(cls):
    for module_name in ("ambari_configuration", "stack_advisor"):
      module_path = STACKS / f"{module_name}.py"
      with module_path.open("rb") as module_file:
        import_utils.load_module(
          module_name,
          module_file,
          str(module_path),
          (".py", "rb", import_utils.PY_SOURCE),
        )

    cls.advisor_module = load_module(
      "bigtop_zeppelin_service_advisor", ZEPPELIN / "service_advisor.py"
    )
    cls.contract_module = load_module(
      "bigtop_zeppelin_service_contract", SCRIPTS / "bigtop_service_contract.py"
    )
    cls.server_module = load_module(
      "bigtop_zeppelin_server", SCRIPTS / "zeppelin_server.py"
    )

  def test_metadata_uses_bigtop_spark_service_and_client(self):
    root = ET.parse(ZEPPELIN / "metainfo.xml").getroot()
    dependencies = {element.text for element in root.iter("name")}
    required_services = {element.text for element in root.iter("service")}

    self.assertIn("SPARK/SPARK_CLIENT", dependencies)
    self.assertIn("SPARK", required_services)
    self.assertNotIn("SPARK2/SPARK2_CLIENT", dependencies)
    self.assertNotIn("SPARK2", required_services)

  def test_advisor_removes_stale_atlas_classpath_for_all_service_states(self):
    stale_content = (
      'export ZEPPELIN_INTP_CLASSPATH_OVERRIDES="{{external_dependency_conf}}:'
      "/usr/hdp/current/spark-atlas-connector/*:"
      '/usr/hdp/current/spark-atlas-connector/*"'
    )
    service_states = (
      ("true", True),
      ("false", True),
      (None, False),
    )

    for atlas_enabled, spark_installed in service_states:
      with self.subTest(atlas_enabled=atlas_enabled, spark_installed=spark_installed):
        services = {
          "services": [],
          "configurations": {
            "zeppelin-env": {"properties": {"zeppelin_env_content": stale_content}}
          },
        }
        if spark_installed:
          services["services"].append({"StackServices": {"service_name": "SPARK"}})
          services["configurations"]["spark-env"] = {"properties": {}}
          services["configurations"]["spark-atlas-application-properties-override"] = {
            "properties": {"atlas.spark.enabled": atlas_enabled}
          }

        configurations = {}
        recommender = self.advisor_module.ZeppelinRecommender()
        recommender.recommendZeppelinConfigurationsFromHDP30(
          configurations, {}, services, {}
        )

        updated = configurations["zeppelin-env"]["properties"]["zeppelin_env_content"]
        self.assertEqual(
          updated,
          'export ZEPPELIN_INTP_CLASSPATH_OVERRIDES="{{external_dependency_conf}}"',
        )

  def test_advisor_does_not_rewrite_clean_classpath(self):
    content = 'export ZEPPELIN_INTP_CLASSPATH_OVERRIDES="{{external_dependency_conf}}"'
    services = {
      "services": [{"StackServices": {"service_name": "SPARK"}}],
      "configurations": {
        "spark-env": {"properties": {}},
        "zeppelin-env": {"properties": {"zeppelin_env_content": content}},
      },
    }
    configurations = {}

    self.advisor_module.ZeppelinRecommender().recommendZeppelinConfigurationsFromHDP30(
      configurations, {}, services, {}
    )

    self.assertEqual({}, configurations)

  def test_secure_livy_superuser_uses_actual_config_and_deduplicates(self):
    services = {
      "services": [{"StackServices": {"service_name": "LIVY"}}],
      "configurations": {
        "zeppelin-env": {"properties": {"zeppelin.kerberos.enabled": "true"}},
        "zeppelin-site": {
          "properties": {"zeppelin.server.kerberos.principal": "zeppelin@example.com"}
        },
        "livy-conf": {"properties": {"livy.superusers": "alice"}},
      },
    }
    recommender = self.advisor_module.ZeppelinRecommender()
    configurations = {}

    recommender.recommendZeppelinConfigurationsFromHDP25(
      configurations, {}, services, {}
    )

    self.assertEqual(
      "alice,zeppelin",
      configurations["livy-conf"]["properties"]["livy.superusers"],
    )

    services["configurations"]["livy-conf"]["properties"]["livy.superusers"] = (
      "alice, zeppelin"
    )
    configurations = {}
    recommender.recommendZeppelinConfigurationsFromHDP25(
      configurations, {}, services, {}
    )
    self.assertNotIn("livy-conf", configurations)

  def test_service_contract_uses_actual_spark_and_secure_livy_keys(self):
    configurations = {
      "spark-hive-site-override": {
        "hive.server2.thrift.port": "10016",
        "hive.server2.thrift.http.port": "10002",
        "hive.server2.http.endpoint": "cliservice",
        "hive.server2.transport.mode": "http",
        "hive.server2.use.SSL": "true",
        "hive.server2.authentication.kerberos.principal": "spark/_HOST@EXAMPLE.COM",
      },
      "livy-conf": {
        "livy.server.port": "8999",
        "livy.keystore": "/etc/security/livy.jks",
      },
    }
    cluster_host_info = {
      "spark_thriftserver_hosts": ["spark.example.com"],
      "livy_server_hosts": ["livy.example.com"],
    }

    spark = self.contract_module.get_spark_thriftserver_settings(
      configurations, cluster_host_info
    )
    livy = self.contract_module.get_livy_server_settings(
      configurations, cluster_host_info
    )

    self.assertEqual("spark.example.com", spark["host"])
    self.assertEqual("10002", spark["port"])
    self.assertEqual("http", spark["transport_mode"])
    self.assertEqual("cliservice", spark["http_path"])
    self.assertTrue(spark["ssl"])
    self.assertEqual("livy.example.com", livy["host"])
    self.assertEqual("8999", livy["port"])
    self.assertEqual("https", livy["protocol"])

  def test_spark_endpoint_is_disabled_when_port_is_missing(self):
    for port in (None, "", "   "):
      with self.subTest(port=port):
        spark_config = {"hive.server2.transport.mode": "binary"}
        if port is not None:
          spark_config["hive.server2.thrift.port"] = port
        settings = self.contract_module.get_spark_thriftserver_settings(
          {"spark-hive-site-override": spark_config},
          {"spark_thriftserver_hosts": ["spark.example.com"]},
        )

        self.assertIsNone(settings["host"])
        self.assertIsNone(settings["port"])

  def test_spark_home_preserves_fallback_until_current_path_is_reliable(self):
    get_spark_home = self.contract_module.get_spark_home

    self.assertEqual(
      "/opt/custom/spark",
      get_spark_home("/opt/custom/spark", "/usr/bigtop", False),
    )
    self.assertEqual(
      "/usr/bigtop/current/spark-client",
      get_spark_home("/opt/custom/spark", "/usr/bigtop", True),
    )

  def test_livy_endpoint_is_disabled_without_actual_configuration(self):
    settings = self.contract_module.get_livy_server_settings(
      {}, {"livy_server_hosts": ["livy.example.com"]}
    )

    self.assertIsNone(settings["host"])

  def test_interpreter_ids_use_bigtop_paths_and_secure_livy_endpoint(self):
    config_data = {
      "interpreterSettings": {
        "spark2": {"group": "spark", "name": "spark2", "properties": {}},
        "livy2": {"group": "livy", "name": "livy2", "properties": {}},
      }
    }
    params = SimpleNamespace(
      config={
        "configurations": {
          "spark-env": {},
          "zeppelin-site": {},
        }
      },
      exclude_interpreter_autoconfig=None,
      zeppelin_interpreter=None,
      spark_home="/usr/bigtop/current/spark-client",
      livy2_livyserver_host="livy.example.com",
      livy2_livyserver_port="8999",
      livy2_livyserver_protocol="https",
    )
    server = self.server_module.ZeppelinServer()
    server.get_interpreter_settings = MagicMock(return_value=config_data)
    server.set_interpreter_settings = MagicMock()
    server.update_kerberos_properties = MagicMock()

    with patch.dict(sys.modules, {"params": params}):
      server.update_zeppelin_interpreter()

    spark_properties = config_data["interpreterSettings"]["spark2"]["properties"]
    livy_properties = config_data["interpreterSettings"]["livy2"]["properties"]
    self.assertEqual("yarn-client", spark_properties["master"]["value"])
    self.assertEqual(
      "/usr/bigtop/current/spark-client",
      spark_properties["SPARK_HOME"]["value"],
    )
    self.assertEqual(
      "https://livy.example.com:8999",
      livy_properties["zeppelin.livy.url"]["value"],
    )
    self.assertIn("spark2", config_data["interpreterSettings"])
    self.assertIn("livy2", config_data["interpreterSettings"])

  def test_interpreters_are_removed_when_bigtop_services_are_unavailable(self):
    config_data = {
      "interpreterSettings": {
        "spark2": {"group": "spark", "name": "spark2", "properties": {}},
        "livy2": {"group": "livy", "name": "livy2", "properties": {}},
      }
    }
    params = SimpleNamespace(
      config={"configurations": {"zeppelin-site": {}}},
      exclude_interpreter_autoconfig=None,
      zeppelin_interpreter=None,
      spark_home="/usr/bigtop/current/spark-client",
      livy2_livyserver_host=None,
      livy2_livyserver_port="8999",
      livy2_livyserver_protocol="http",
    )
    server = self.server_module.ZeppelinServer()
    server.get_interpreter_settings = MagicMock(return_value=config_data)
    server.set_interpreter_settings = MagicMock()
    server.update_kerberos_properties = MagicMock()

    with patch.dict(sys.modules, {"params": params}):
      server.update_zeppelin_interpreter()

    self.assertEqual({}, config_data["interpreterSettings"])

  def test_no_removed_ambari_contract_names_remain(self):
    sources = (
      ZEPPELIN / "metainfo.xml",
      ZEPPELIN / "service_advisor.py",
      SCRIPTS / "params.py",
      SCRIPTS / "zeppelin_server.py",
    )
    forbidden = (
      "SPARK2/SPARK2_CLIENT",
      "<service>SPARK2</service>",
      "spark2-env",
      "spark2-hive-site-override",
      "spark2-defaults",
      "spark2_thriftserver_hosts",
      "livy2-conf",
      "livy2_server_hosts",
      "/usr/hdp",
    )

    combined_source = "\n".join(path.read_text() for path in sources)
    for removed_name in forbidden:
      with self.subTest(removed_name=removed_name):
        self.assertNotIn(removed_name, combined_source)
