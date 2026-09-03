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


RESOURCES = Path(__file__).resolve().parents[2] / "main/resources"

PUBLIC_TRUSTSTORE_DEFAULTS = {
  "common-services/AMBARI_METRICS/3.0.0/configuration/ams-ssl-client.xml",
  "common-services/AMBARI_METRICS/3.0.0/configuration/ams-ssl-server.xml",
  "stacks/BIGTOP/3.2.0/services/HBASE/configuration/"
  "ranger-hbase-policymgr-ssl.xml",
  "stacks/BIGTOP/3.2.0/services/HDFS/configuration/"
  "ranger-hdfs-policymgr-ssl.xml",
  "stacks/BIGTOP/3.2.0/services/HIVE/configuration/"
  "ranger-hive-policymgr-ssl.xml",
  "stacks/BIGTOP/3.2.0/services/KAFKA/configuration/"
  "ranger-kafka-policymgr-ssl.xml",
  "stacks/BIGTOP/3.2.0/services/YARN/configuration/"
  "ranger-yarn-policymgr-ssl.xml",
  "stacks/BIGTOP/3.3.0/services/RANGER/configuration/atlas-tagsync-ssl.xml",
  "stacks/BIGTOP/3.3.0/services/RANGER/configuration/ranger-admin-site.xml",
  "stacks/BIGTOP/3.3.0/services/RANGER/configuration/"
  "ranger-tagsync-policymgr-ssl.xml",
  "stacks/BIGTOP/3.3.0/services/RANGER/configuration/ranger-ugsync-site.xml",
  "stacks/BIGTOP/3.3.0/services/RANGER_KMS/configuration/"
  "ranger-kms-policymgr-ssl.xml",
}

HTTPS_KEYSTORE_ALIAS_DEFAULTS = {
  "stacks/BIGTOP/3.3.0/services/RANGER/configuration/ranger-admin-site.xml",
  "stacks/BIGTOP/3.3.0/services/RANGER_KMS/configuration/ranger-kms-site.xml",
}


def properties(path):
  return {
    (property_element.findtext("name") or "").strip(): (
      property_element.findtext("value") or ""
    ).strip()
    for property_element in ET.parse(path).findall(".//property")
  }


class TestBigtopServiceCredentialContracts(unittest.TestCase):
  def test_fixed_truststore_integrity_defaults_are_the_reviewed_public_set(self):
    found = {}
    for path in RESOURCES.rglob("*.xml"):
      for name, value in properties(path).items():
        if (
          name.endswith("truststore.password")
          and value
          and value != "_"
          and "{{" not in value
          and "}}" not in value
        ):
          found[str(path.relative_to(RESOURCES))] = value

    self.assertEqual(PUBLIC_TRUSTSTORE_DEFAULTS, set(found))
    self.assertEqual({"bigdata", "changeit"}, set(found.values()))

  def test_https_keystore_alias_defaults_are_identifiers_not_secrets(self):
    property_name = "ranger.service.https.attrib.keystore.credential.alias"
    found = {}
    for path in RESOURCES.rglob("*.xml"):
      value = properties(path).get(property_name, "")
      if value:
        found[str(path.relative_to(RESOURCES))] = value

    self.assertEqual(HTTPS_KEYSTORE_ALIAS_DEFAULTS, set(found))
    self.assertEqual(1, len(set(found.values())))

  def test_yarn_audit_database_password_has_no_fixed_default(self):
    path = (
      RESOURCES
      / "stacks/BIGTOP/3.2.0/services/YARN/configuration/ranger-yarn-audit.xml"
    )
    self.assertEqual("", properties(path)["xasecure.audit.destination.db.password"])


if __name__ == "__main__":
  unittest.main()
