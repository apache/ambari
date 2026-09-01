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


AGENT_MODULE = Path(__file__).resolve().parents[3]
PROJECT_ROOT = AGENT_MODULE.parent
MAVEN_NAMESPACE = {"m": "http://maven.apache.org/POM/4.0.0"}


class TestAgentBigtopPackaging(unittest.TestCase):
  def test_unix_packaging_inherits_the_bigtop_distribution(self):
    root_pom = ElementTree.parse(PROJECT_ROOT / "pom.xml").getroot()
    agent_pom = ElementTree.parse(AGENT_MODULE / "pom.xml").getroot()

    self.assertEqual(
      "BIGTOP",
      root_pom.findtext(
        "m:properties/m:stack.distribution", namespaces=MAVEN_NAMESPACE
      ),
    )
    linux_profile = agent_pom.find(
      "m:profiles/m:profile[m:id='linux']", namespaces=MAVEN_NAMESPACE
    )
    self.assertIsNotNone(linux_profile)
    self.assertIsNone(
      linux_profile.find("m:properties/m:stack.distribution", MAVEN_NAMESPACE)
    )

  def test_agent_resources_have_no_hdp_stack_contract(self):
    source = (AGENT_MODULE / "pom.xml").read_text(encoding="utf-8")

    self.assertIn("<include>stacks/${stack.distribution}/**/*</include>", source)
    self.assertNotIn("/cache/stacks/HDP/", source)
    self.assertNotIn("wordCount.jar", source)


if __name__ == "__main__":
  unittest.main()
