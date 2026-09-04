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

import os
import tempfile
import unittest

from pluggable_stack_definition.GenerateStackDefinition import (
  _named_dict,
  process_replacements,
)


class TestGenerateStackDefinition(unittest.TestCase):
  def test_process_replacements_preserves_unicode_and_selected_text(self):
    config = _named_dict(
      {
        "stackName": "ODP",
        "performCommonReplacements": True,
        "preservedText": ["HDP_VERSION"],
        "textReplacements": [["hdp-select", "distro-select"]],
      }
    )
    with tempfile.TemporaryDirectory() as temporary_directory:
      source_path = os.path.join(temporary_directory, "params.py")
      with open(source_path, "w", encoding="utf-8") as source:
        source.write("HDP 2.3 hdp-select caf\u00e9 HDP_VERSION\n")

      process_replacements(source_path, config, {"2.3": "0.9"})

      with open(source_path, "r", encoding="utf-8") as result:
        self.assertEqual(
          "ODP 0.9 distro-select caf\u00e9 HDP_VERSION\n", result.read()
        )

  def test_named_dict_reports_missing_attributes_normally(self):
    with self.assertRaises(AttributeError):
      _named_dict({}).missing


if __name__ == "__main__":
  unittest.main()
