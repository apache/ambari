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
import xml.etree.ElementTree as ET


SERVICE = Path(__file__).resolve().parents[2] / (
  "main/resources/common-services/AMBARI_METRICS/3.0.0"
)


class TestAmbariMetricsMetadataContract(unittest.TestCase):
  def test_common_service_packages_match_bigtop_package_names(self):
    root = ET.parse(SERVICE / "metainfo.xml").getroot()
    packages = {
      package.findtext("name")
      for package in root.findall(".//osSpecific/packages/package")
    }
    self.assertTrue(
      {
        "ambari-metrics-collector",
        "ambari-metrics-monitor",
        "ambari-metrics-hadoop-sink",
        "ambari-metrics-grafana",
      }.issubset(packages)
    )
    self.assertNotIn("ambari-metrics-assembly", packages)
    self.assertNotIn("gcc", packages)

  def test_service_declares_all_themes_in_one_metadata_block(self):
    service = ET.parse(SERVICE / "metainfo.xml").getroot().find("./services/service")
    theme_blocks = service.findall("themes")
    self.assertEqual(1, len(theme_blocks))
    self.assertEqual(
      {"theme.json", "credentials.json", "directories.json"},
      {theme.findtext("fileName") for theme in theme_blocks[0].findall("theme")},
    )

  def test_python_392_and_jdk17_configuration_contract_is_declared(self):
    ams_env = (SERVICE / "configuration/ams-env.xml").read_text(encoding="utf-8")
    hbase_env = (SERVICE / "configuration/ams-hbase-env.xml").read_text(
      encoding="utf-8"
    )
    self.assertIn("metrics_monitor_python_binary", ams_env)
    self.assertIn("/usr/bin/python3.9", ams_env)
    self.assertIn("-Xlog:gc*", ams_env)
    self.assertIn("-Xlog:gc*", hbase_env)
    for legacy_option in ("UseConcMarkSweepGC", "MaxPermSize", "PermSize"):
      with self.subTest(option=legacy_option):
        self.assertNotIn(legacy_option, ams_env)
        self.assertNotIn(legacy_option, hbase_env)

  def test_advisor_counts_mapreduce_historyserver_under_its_actual_service(self):
    advisor = (SERVICE / "service_advisor.py").read_text(encoding="utf-8")
    self.assertIn(
      '"MAPREDUCE2": {"HISTORYSERVER": HEAP_PER_MASTER_COMPONENT}', advisor
    )

  def test_hbase_2413_memstore_configuration_keeps_equivalent_ams_aliases(self):
    hbase_site = ET.parse(SERVICE / "configuration/ams-hbase-site.xml").getroot()
    properties = {
      property_element.findtext("name"): property_element.findtext("value")
      for property_element in hbase_site.findall("property")
      if property_element.findtext("deleted") != "true"
    }
    self.assertEqual("0.5", properties["hbase.regionserver.global.memstore.size"])
    self.assertEqual(
      "0.8", properties["hbase.regionserver.global.memstore.size.lower.limit"]
    )
    self.assertEqual(
      properties["hbase.regionserver.global.memstore.size"],
      properties["hbase.regionserver.global.memstore.upperLimit"],
    )
    self.assertEqual(
      float(properties["hbase.regionserver.global.memstore.size"])
      * float(properties["hbase.regionserver.global.memstore.size.lower.limit"]),
      float(properties["hbase.regionserver.global.memstore.lowerLimit"]),
    )

  def test_obsolete_ambari_metrics_entries_are_removed(self):
    removed = (
      "package/scripts/hbase_master.py",
      "package/scripts/hbase_regionserver.py",
      "package/scripts/service_mapping.py",
      "package/scripts/split_points.py",
      "package/files/hbaseSmokeVerify.sh",
      "package/templates/hbase_grant_permissions.j2",
    )
    for relative_path in removed:
      with self.subTest(path=relative_path):
        self.assertFalse((SERVICE / relative_path).exists())


if __name__ == "__main__":
  unittest.main()
