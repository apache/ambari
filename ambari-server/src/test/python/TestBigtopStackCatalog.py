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


BIGTOP_STACKS = (
  Path(__file__).resolve().parents[2] / "main/resources/stacks/BIGTOP"
)


class TestBigtopStackCatalog(unittest.TestCase):
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


if __name__ == "__main__":
  unittest.main()
