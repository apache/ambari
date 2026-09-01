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


SPARK = Path(__file__).resolve().parents[2] / "main/resources/stacks/BIGTOP/3.2.0/services/SPARK"


class TestSparkMetadata(unittest.TestCase):
  def test_bigtop_metadata_tracks_current_bom_and_log4j2_contract(self):
    current_metadata = SPARK.parents[2] / "3.3.0/services/SPARK/metainfo.xml"
    current = ElementTree.parse(current_metadata).findtext("./services/service/version")
    self.assertEqual("3.5.6-1", current)
    service_source = "\n".join(
      path.read_text(encoding="utf-8")
      for path in SPARK.rglob("*")
      if path.suffix in (".json", ".py", ".xml", ".j2")
    )
    self.assertIn("log4j2.properties", service_source)


if __name__ == "__main__":
  unittest.main()
