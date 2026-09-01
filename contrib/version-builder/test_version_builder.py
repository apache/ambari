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

import unittest

import version_builder


class TestVersionBuilderArguments(unittest.TestCase):
  def test_parse_options_accepts_legacy_positional_arguments(self):
    options = version_builder.parse_options(
      ["--file", "version.xml", "legacy-release-name"]
    )

    self.assertEqual("version.xml", options.filename)
    self.assertEqual(["legacy-release-name"], options.arguments)

  def test_parse_options_rejects_unknown_option(self):
    with self.assertRaises(SystemExit) as raised:
      version_builder.parse_options(
        ["--file", "version.xml", "--relese-type", "STANDARD"]
      )

    self.assertEqual(2, raised.exception.code)


if __name__ == "__main__":
  unittest.main()
