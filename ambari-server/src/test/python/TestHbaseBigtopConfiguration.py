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

import json
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

HBASE = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/HBASE"
)
HBASE_33 = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.3.0/services/HBASE"
)
def properties(path):
  root = ET.parse(path).getroot()
  return {
    property_element.findtext("name"): property_element.findtext("value") or ""
    for property_element in root.findall("property")
  }


class TestHbaseConfigurationContract(unittest.TestCase):
  def test_hbase_env_is_jdk17_safe_and_shell_quoted(self):
    env_properties = properties(HBASE / "configuration/hbase-env.xml")
    content = env_properties["content"]
    self.assertIn("umask 0027", content)
    self.assertIn("export HBASE_IDENT_STRING={{hbase_user_shell}}", content)
    self.assertIn("export JAVA_HOME={{java64_home_shell}}", content)
    self.assertIn("${HBASE_OPTS:-}", content)
    self.assertIn("${HBASE_THRIFT_OPTS:-}", content)
    self.assertIn("hbase_thrift_jaas.conf", content)
    self.assertIn("${HBASE_LOGFILE:-hbase-server}.gc", content)
    for obsolete in (
      "MaxPermSize",
      "UseConcMarkSweepGC",
      "Java 1.6",
      "HBASE_SERVER_JAAS_OPTS",
      "`date",
      "umask 022",
    ):
      with self.subTest(obsolete=obsolete):
        self.assertNotIn(obsolete, content)
    self.assertEqual("30", env_properties["hbase_regionserver_shutdown_timeout"])
    self.assertEqual("540", env_properties["hbase_region_mover_timeout"])

    site_properties = properties(HBASE / "configuration/hbase-site.xml")
    self.assertEqual("30", site_properties["hbase.master.wait.on.service.seconds"])
    for obsolete_key in (
      "hbase.defaults.for.version.skip",
      "hbase.zookeeper.useMulti",
      "hbase.master.namespace.init.timeout",
      "hbase.master.wait.on.regionservers.timeout",
      "dfs.domain.socket.path",
    ):
      with self.subTest(obsolete_key=obsolete_key):
        self.assertNotIn(obsolete_key, site_properties)

  def test_thrift_jaas_uses_thrift_service_identity(self):
    template = (HBASE / "package/templates/hbase_thrift_jaas.conf.j2").read_text()
    self.assertIn('keyTab="{{thrift_keytab_path}}"', template)
    self.assertIn('principal="{{thrift_jaas_princ}}"', template)
    self.assertNotIn("regionserver", template.lower())

    configuration_source = (SCRIPTS / "hbase.py").read_text()
    self.assertIn('hbase_TemplateConfig("hbase_client_jaas.conf")', configuration_source)
    self.assertIn('if name != "client":', configuration_source)

  def test_metadata_declares_runtime_dependencies_and_bounded_commands(self):
    metadata = ET.parse(HBASE / "metainfo.xml").getroot()
    dependencies = {
      node.text
      for node in metadata.findall(
        "./services/service/configuration-dependencies/config-type"
      )
    }
    self.assertTrue(
      {
        "core-site",
        "hdfs-site",
        "hadoop-env",
        "zookeeper-env",
        "ams-ssl-client",
        "hbase-site",
        "hbase-thrift-site",
      }.issubset(dependencies)
    )
    self.assertEqual(
      "900",
      metadata.findtext("./services/service/commandScript/timeout"),
    )
    self.assertEqual(
      "7200",
      metadata.findtext(
        ".//component[name='HBASE_MASTER']/customCommands/"
        "customCommand[name='DECOMMISSION']/commandScript/timeout"
      ),
    )

  def test_bigtop_33_overlay_selects_bom_hbase_version(self):
    metadata = ET.parse(HBASE_33 / "metainfo.xml").getroot()
    self.assertEqual("HBASE", metadata.findtext("./services/service/name"))
    self.assertEqual("2.6.3-1", metadata.findtext("./services/service/version"))

  def test_alert_and_quicklink_ports_match_hbase_2_6_defaults(self):
    alerts = json.loads((HBASE / "alerts.json").read_text())
    components = alerts["HBASE"]
    self.assertEqual(
      16000, components["HBASE_MASTER"][0]["source"]["default_port"]
    )
    self.assertEqual(
      16030, components["HBASE_REGIONSERVER"][0]["source"]["default_port"]
    )
    self.assertEqual(
      9095, components["HBASE_THRIFT"][0]["source"]["default_port"]
    )
    quicklinks = json.loads(
      (HBASE / "quicklinks/quicklinks.json").read_text()
    )
    for link in quicklinks["configuration"]["links"]:
      if link["component_name"] == "HBASE_MASTER":
        self.assertEqual("16010", link["port"]["http_default_port"])

  def test_kerberos_descriptor_has_thrift_service_and_spnego_identities(self):
    kerberos = json.loads((HBASE / "kerberos.json").read_text())
    components = {
      component["name"]: component
      for component in kerberos["services"][0]["components"]
    }
    thrift_identities = {
      identity["name"]: identity
      for identity in components["HBASE_THRIFT"]["identities"]
    }
    self.assertIn("hbase_thrift_hbase", thrift_identities)
    self.assertIn("hbase_hbase_thrift_spnego", thrift_identities)
    service_identity = thrift_identities["hbase_thrift_hbase"]
    self.assertEqual(
      "hbase-site/hbase.thrift.kerberos.principal",
      service_identity["principal"]["configuration"],
    )
    self.assertEqual(
      "hbase-site/hbase.thrift.keytab.file",
      service_identity["keytab"]["configuration"],
    )

if __name__ == "__main__":
  unittest.main()
