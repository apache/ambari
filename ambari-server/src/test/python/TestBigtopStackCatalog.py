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
from xml.etree import ElementTree


BIGTOP_STACKS = (
  Path(__file__).resolve().parents[2] / "main/resources/stacks/BIGTOP"
)
BIGTOP_BASE_SERVICES = BIGTOP_STACKS / "3.2.0/services"


class TestBigtopStackCatalog(unittest.TestCase):
  def test_stack_feature_names_are_unique(self):
    feature_file = BIGTOP_STACKS / "3.2.0/properties/stack_features.json"
    features = json.loads(feature_file.read_text(encoding="utf-8"))["BIGTOP"][
      "stack_features"
    ]
    names = [feature["name"] for feature in features]

    self.assertEqual(len(names), len(set(names)))

  def test_only_bom_backed_stack_is_active(self):
    active = {}
    for version in ("3.2.0", "3.3.0", "3.4.0"):
      metadata = ElementTree.parse(BIGTOP_STACKS / version / "metainfo.xml")
      active[version] = metadata.findtext("./versions/active")

    self.assertEqual(
      {"3.2.0": "false", "3.3.0": "true", "3.4.0": "false"}, active
    )

  def test_active_stack_inherits_the_service_implementation_baseline(self):
    metadata = ElementTree.parse(BIGTOP_STACKS / "3.3.0/metainfo.xml")
    self.assertEqual("3.2.0", metadata.findtext("./extends"))

  def test_service_advisors_use_the_python3_entrypoint(self):
    advisors = list(BIGTOP_BASE_SERVICES.glob("*/service_advisor.py"))
    common_services = (
      Path(__file__).resolve().parents[2] / "main/resources/common-services"
    )
    advisors.extend(
      (
        common_services / "AMBARI_INFRA_SOLR/3.0.0/service_advisor.py",
        common_services / "AMBARI_METRICS/3.0.0/service_advisor.py",
      )
    )
    excluded = {"ALLUXIO", "HBASE"}
    for advisor in advisors:
      if advisor.parent.name in excluded:
        continue
      self.assertEqual(
        "#!/usr/bin/env python3",
        advisor.read_text(encoding="utf-8").splitlines()[0],
        advisor,
      )

  def test_bigtop_services_only_declare_supported_os_families(self):
    rpm_families = {"redhat8", "redhat9", "openeuler22"}
    supported = rpm_families | {"ubuntu22"}
    metadata_files = []
    for stack_version in ("3.2.0", "3.3.0", "3.4.0"):
      metadata_files.extend(
        (BIGTOP_STACKS / stack_version / "services").glob("*/metainfo.xml")
      )
    common_services = (
      Path(__file__).resolve().parents[2] / "main/resources/common-services"
    )
    metadata_files.extend(
      (
        common_services / "AMBARI_INFRA_SOLR/3.0.0/metainfo.xml",
        common_services / "AMBARI_METRICS/3.0.0/metainfo.xml",
      )
    )

    for metadata_file in metadata_files:
      root = ElementTree.parse(metadata_file).getroot()
      os_families = root.findall(".//osSpecific/osFamily")
      if not os_families:
        continue
      declared_groups = set()
      for os_family in os_families:
        declared = frozenset(
          item.strip()
          for item in (os_family.text or "").split(",")
          if item.strip()
        )
        self.assertTrue(declared, metadata_file)
        self.assertLessEqual(declared, supported, metadata_file)
        declared_groups.add(declared)
      self.assertEqual(
        {frozenset(rpm_families), frozenset({"ubuntu22"})},
        declared_groups,
        metadata_file,
      )

    for stack_version in ("3.2.0", "3.3.0", "3.4.0"):
      repository = ElementTree.parse(
        BIGTOP_STACKS / stack_version / "repos/repoinfo.xml"
      ).getroot()
      repository_families = {
        node.attrib["family"] for node in repository.findall("./os")
      }
      self.assertEqual(supported, repository_families, stack_version)
      ubuntu_repository = repository.find("./os[@family='ubuntu22']/repo")
      self.assertIsNotNone(ubuntu_repository, stack_version)
      self.assertEqual(
        f"BIGTOP-{stack_version}",
        ubuntu_repository.findtext("repoid"),
        stack_version,
      )
      self.assertEqual(
        "https://repo.invalid/bigtop/",
        ubuntu_repository.findtext("baseurl"),
        stack_version,
      )
      self.assertEqual(
        "jammy", ubuntu_repository.findtext("reponame"), stack_version
      )

  def test_hive_and_yarn_declare_bigtop_ubuntu_packages(self):
    expected = {
      "HIVE": {
        "hive-${stack_version}",
        "hive-${stack_version}-hcatalog",
        "hive-${stack_version}-webhcat",
        "mariadb-server",
        "ranger-${stack_version}-hive-plugin",
      },
      "YARN": {
        "hadoop-${stack_version}-yarn",
        "hadoop-${stack_version}-mapreduce",
        "hadoop-${stack_version}-hdfs",
        "ranger-${stack_version}-yarn-plugin",
        "hbase-${stack_version}",
      },
      "MAPREDUCE2": {"hadoop-${stack_version}-mapreduce"},
    }

    for service_name, expected_packages in expected.items():
      metadata_path = BIGTOP_BASE_SERVICES / service_name / "metainfo.xml"
      if service_name == "MAPREDUCE2":
        metadata_path = BIGTOP_BASE_SERVICES / "YARN/metainfo.xml"
      root = ElementTree.parse(metadata_path).getroot()
      service = root.find(f"./services/service[name='{service_name}']")
      ubuntu = service.find("./osSpecifics/osSpecific[osFamily='ubuntu22']")
      self.assertIsNotNone(ubuntu, service_name)
      actual_packages = {
        package.findtext("name") for package in ubuntu.findall("./packages/package")
      }
      self.assertEqual(expected_packages, actual_packages, service_name)


if __name__ == "__main__":
  unittest.main()
