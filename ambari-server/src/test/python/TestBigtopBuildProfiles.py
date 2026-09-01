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


SERVER_MODULE = Path(__file__).resolve().parents[3]
MAVEN_NAMESPACE = {"m": "http://maven.apache.org/POM/4.0.0"}


class TestBigtopBuildProfiles(unittest.TestCase):
  def test_obsolete_hdp_repository_rewrite_is_removed(self):
    pom_source = (SERVER_MODULE / "pom.xml").read_text(encoding="utf-8")
    root = ElementTree.fromstring(pom_source)
    profile_ids = {
      profile.findtext("m:id", namespaces=MAVEN_NAMESPACE)
      for profile in root.findall("m:profiles/m:profile", MAVEN_NAMESPACE)
    }

    self.assertNotIn("replaceurl", profile_ids)
    self.assertNotIn("set-hdp-repo-url.sh", pom_source)
    self.assertNotIn("hdpUrlForCentos6", pom_source)
    self.assertNotIn("hdpLatestUrl", pom_source)
    self.assertFalse((SERVER_MODULE / "set-hdp-repo-url.sh").exists())

  def test_generic_bigtop_urlinfo_profile_remains_available(self):
    root = ElementTree.parse(SERVER_MODULE / "pom.xml").getroot()
    profile = root.find(
      "m:profiles/m:profile[m:id='replaceBaseUrl']", namespaces=MAVEN_NAMESPACE
    )

    self.assertIsNotNone(profile)
    arguments = [
      argument.text
      for argument in profile.findall(".//m:arguments/m:argument", MAVEN_NAMESPACE)
    ]
    self.assertIn("${urlinfo_processor_script_location}", arguments)
    self.assertIn("${stacksSrcLocation}", arguments)

  def test_obsolete_runtime_tools_are_not_packaged(self):
    repository = SERVER_MODULE.parent
    obsolete_files = (
      repository
      / "ambari-common/src/main/python/resource_management/libraries/functions/dynamic_variable_interpretation.py",
      repository
      / "ambari-common/src/main/python/resource_management/libraries/functions/hive_check.py",
      repository
      / "ambari-common/src/main/python/resource_management/libraries/functions/oozie_prepare_war.py",
      repository
      / "ambari-common/src/main/python/resource_management/libraries/functions/simulate_perf_cluster_alert_behaviour.py",
      SERVER_MODULE / "src/main/resources/scripts/Ambaripreupload.py",
      SERVER_MODULE
      / "src/test/java/org/apache/ambari/server/agent/LocalAgentSimulator.java",
    )

    for path in obsolete_files:
      self.assertFalse(path.exists(), path)

    functions_init = (
      repository
      / "ambari-common/src/main/python/resource_management/libraries/functions/__init__.py"
    ).read_text(encoding="utf-8")
    self.assertNotIn("hive_check", functions_init)


if __name__ == "__main__":
  unittest.main()
