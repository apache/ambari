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

from pathlib import Path
import unittest
from xml.etree import ElementTree


STACKS = Path(__file__).resolve().parents[2] / "main/resources/stacks/BIGTOP"
BASE_SERVICES = STACKS / "3.2.0/services"
EXPECTED_VERSIONS = {
  "3.2.0": {
    "HIVE": "3.1.3-1",
    "ZEPPELIN": "0.10.1-1",
    "FLINK": "1.15.3-1",
    "SPARK": "3.2.3-1",
    "ZOOKEEPER": "3.5.9-2",
  },
  "3.3.0": {
    "HIVE": "3.1.3-1",
    "ZEPPELIN": "0.10.1-1",
    "FLINK": "1.19.3-1",
    "SPARK": "3.5.6-1",
    "ZOOKEEPER": "3.7.2-1",
  },
  "3.4.0": {
    "HIVE": "4.0.1-1",
    "ZEPPELIN": "0.11.2-1",
    "FLINK": "1.20.0-1",
    "SPARK": "3.5.3-1",
    "ZOOKEEPER": "3.8.4-1",
  },
}


def source(service, relative_path):
  return (BASE_SERVICES / service / relative_path).read_text(encoding="utf-8")


class TestBigtopServiceVersionContracts(unittest.TestCase):
  def test_declared_component_versions_match_supported_matrix(self):
    for stack_version, services in EXPECTED_VERSIONS.items():
      for service_name, expected_version in services.items():
        with self.subTest(stack=stack_version, service=service_name):
          root = ElementTree.parse(
            STACKS / stack_version / "services" / service_name / "metainfo.xml"
          ).getroot()
          service = root.find("./services/service")
          self.assertIsNotNone(service)
          self.assertEqual(service_name, service.findtext("name"))
          self.assertEqual(expected_version, service.findtext("version"))

  def test_newer_stacks_inherit_the_reviewed_base_scripts(self):
    for stack_version in ("3.3.0", "3.4.0"):
      for service_name in EXPECTED_VERSIONS[stack_version]:
        with self.subTest(stack=stack_version, service=service_name):
          self.assertFalse(
            (STACKS / stack_version / "services" / service_name / "package").exists()
          )

  def test_hive_31_and_40_share_supported_launchers(self):
    metastore_launcher = source("HIVE", "package/files/startMetastore.sh")
    hiveserver_launcher = source(
      "HIVE", "package/templates/startHiveserver2.sh.j2"
    )
    hive_service = source("HIVE", "package/scripts/hive_service.py")
    hive_metastore = source("HIVE", "package/scripts/hive_metastore.py")
    service_check = source("HIVE", "package/scripts/service_check.py")

    self.assertIn('"$HIVE_BIN" --service metastore', metastore_launcher)
    self.assertIn("{{hive_bin_dir}}/hiveserver2", hiveserver_launcher)
    self.assertIn("org.apache.hadoop.hive.metastore.HiveMetaStore", hive_service)
    self.assertIn("org.apache.hive.service.server.HiveServer2", hive_service)
    self.assertIn('format("{hive_bin_dir}/schematool")', hive_metastore)
    self.assertIn('os.path.join(params.hive_bin_dir, "beeline")', service_check)

  def test_flink_legacy_yaml_and_history_server_contract_cover_115_to_120(self):
    setup = source("FLINK", "package/scripts/setup_flink.py")
    service = source("FLINK", "package/scripts/flink_service.py")
    process = source("FLINK", "package/scripts/flink_process.py")

    self.assertIn('"config.yaml"), action="delete"', setup)
    self.assertIn('"flink-conf.yaml"', setup)
    self.assertIn('(params.historyserver_script, "start-foreground")', service)
    self.assertIn(
      "org.apache.flink.runtime.webmonitor.history.HistoryServer", process
    )
    self.assertIn('"--configDir", config_dir', process)

  def test_zeppelin_spark_and_zookeeper_launch_contracts_span_declared_versions(self):
    zeppelin_params = source("ZEPPELIN", "package/scripts/params.py")
    zeppelin_process = source("ZEPPELIN", "package/scripts/zeppelin_process.py")
    spark_process = source("SPARK", "package/scripts/spark_process.py")
    spark_service = source("SPARK", "package/scripts/spark_service.py")
    zk_params = source("ZOOKEEPER", "package/scripts/params_linux.py")
    zk_process = source("ZOOKEEPER", "package/scripts/zookeeper_process.py")
    zk_service = source("ZOOKEEPER", "package/scripts/zookeeper_service.py")

    self.assertIn('"bin", "zeppelin-daemon.sh"', zeppelin_params)
    self.assertIn('daemon, "--config", conf_dir, "start"', zeppelin_process)
    self.assertIn("org.apache.zeppelin.server.ZeppelinServer", zeppelin_process)
    self.assertIn("org.apache.spark.deploy.history.HistoryServer", spark_process)
    self.assertIn(
      "org.apache.spark.sql.hive.thriftserver.HiveThriftServer2", spark_process
    )
    self.assertIn('params.spark_class,', spark_service)
    self.assertIn('params.spark_submit,', spark_service)
    self.assertIn('"bin", "zkServer.sh"', zk_params)
    self.assertIn("org.apache.zookeeper.server.quorum.QuorumPeerMain", zk_process)
    self.assertIn('params.zk_server_script, "start-foreground"', zk_service)


if __name__ == "__main__":
  unittest.main()
