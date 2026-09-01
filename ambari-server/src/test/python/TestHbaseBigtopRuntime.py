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

import importlib.util
from pathlib import Path
import unittest

from resource_management.core.exceptions import Fail


HBASE = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/HBASE"
)


def load_module(module_name, path):
  spec = importlib.util.spec_from_file_location(module_name, path)
  module = importlib.util.module_from_spec(spec)
  spec.loader.exec_module(module)
  return module


FUNCTIONS = load_module(
  "bigtop_hbase_functions", HBASE / "package/scripts/functions.py"
)


class TestHbaseRuntimeContract(unittest.TestCase):
  def test_heap_parser_normalizes_supported_jvm_units(self):
    self.assertEqual("1024m", FUNCTIONS.ensure_unit_for_memory("1024"))
    self.assertEqual("2g", FUNCTIONS.ensure_unit_for_memory(" 2 G "))
    self.assertEqual("512m", FUNCTIONS.calc_xmn_from_xms("2g", 0.25, 512))
    self.assertEqual("200m", FUNCTIONS.calc_xmn_from_xms("1000m", 0.2, 512))

  def test_heap_parser_rejects_invalid_or_unsafe_values(self):
    for value in ("", "0", "-1g", "1.5g", "1t", "512m;id"):
      with self.subTest(value=value):
        with self.assertRaises(Fail):
          FUNCTIONS.ensure_unit_for_memory(value)
    with self.assertRaises(Fail):
      FUNCTIONS.calc_xmn_from_xms("2g", 0, 512)
    with self.assertRaises(Fail):
      FUNCTIONS.calc_xmn_from_xms("2g", "invalid", 512)
    with self.assertRaises(Fail):
      FUNCTIONS.calc_xmn_from_xms("8m", 0.5, 512)

  def test_boolean_parser_handles_ambari_values_without_truthy_strings(self):
    for value in (True, "true", "TRUE", "yes", "1"):
      with self.subTest(value=value):
        self.assertTrue(FUNCTIONS.as_bool(value))
    for value in (False, None, "false", "no", "0", "disabled"):
      with self.subTest(value=value):
        self.assertFalse(FUNCTIONS.as_bool(value))

  def test_strict_boolean_parser_rejects_ambiguous_values(self):
    for value, expected in (
      (True, True),
      (False, False),
      (" true ", True),
      ("FALSE", False),
    ):
      with self.subTest(value=value):
        self.assertEqual(
          expected, FUNCTIONS.strict_bool(value, "hbase-env/phoenix_sql_enabled")
        )
    for value in (None, "", "yes", "1", 1):
      with self.subTest(value=value), self.assertRaisesRegex(
        Fail, "phoenix_sql_enabled"
      ):
        FUNCTIONS.strict_bool(value, "hbase-env/phoenix_sql_enabled")

  def test_external_ranger_credentials_must_be_explicit_and_nonempty(self):
    valid = {
      "external_admin_username": "external-admin",
      "external_admin_password": "secret-one",
      "external_ranger_admin_username": "service-account",
      "external_ranger_admin_password": "secret-two",
    }
    self.assertEqual(
      valid, FUNCTIONS.require_external_ranger_credentials(valid)
    )
    for missing_property in valid:
      for invalid_value in (None, "", "   ", 1):
        with self.subTest(
          missing_property=missing_property, invalid_value=invalid_value
        ):
          invalid = dict(valid)
          invalid[missing_property] = invalid_value
          with self.assertRaisesRegex(Fail, missing_property):
            FUNCTIONS.require_external_ranger_credentials(invalid)

    params_source = (HBASE / "package/scripts/params_linux.py").read_text()
    self.assertNotIn('external_admin_username", "admin"', params_source)
    self.assertNotIn('"amb_ranger_admin"', params_source)


if __name__ == "__main__":
  unittest.main()
