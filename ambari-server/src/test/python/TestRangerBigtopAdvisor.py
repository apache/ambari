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
from xml.etree import ElementTree


RANGER = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.3.0/services/RANGER"
)


def load_module(name, path):
  spec = importlib.util.spec_from_file_location(name, path)
  module = importlib.util.module_from_spec(spec)
  spec.loader.exec_module(module)
  return module


ADVISOR = load_module("bigtop_ranger_service_advisor", RANGER / "service_advisor.py")


class TestRangerBigtopAdvisor(unittest.TestCase):
  def test_advisor_boolean_contract_is_strict(self):
    self.assertTrue(ADVISOR._strict_bool(" true ", "value"))
    self.assertFalse(ADVISOR._strict_bool("FALSE", "value"))
    self.assertTrue(ADVISOR._strict_yes_no(" Yes ", "value"))
    self.assertFalse(ADVISOR._strict_yes_no("NO", "value"))
    for invalid in ("", "1", "yes", None, 1):
      with self.subTest(invalid=invalid):
        with self.assertRaises(ValueError):
          ADVISOR._strict_bool(invalid, "value")

  def test_password_validation_accepts_special_characters(self):
    validator = object.__new__(ADVISOR.RangerValidator)
    validator.getNotApplicableItem = lambda message: message
    validator.toConfigurationValidationProblems = lambda items, _: items

    password_properties = {
      "admin_password": "Abc1234'\\`\"",
      "ranger_admin_password": "Xyz9876;|&$",
    }
    self.assertEqual(
      [],
      validator.validateRangerPasswordConfigurations(
        password_properties, {}, {}, {}, {}
      ),
    )
    self.assertEqual(
      1,
      len(
        validator.validateRangerPasswordConfigurations(
          {"admin_password": "letters-only"}, {}, {}, {}, {}
        )
      ),
    )

  def test_password_descriptions_match_runtime_contract(self):
    root = ElementTree.parse(RANGER / "configuration/ranger-env.xml").getroot()
    password_names = {
      "ranger_admin_password",
      "admin_password",
      "rangerusersync_user_password",
      "rangertagsync_user_password",
      "keyadmin_user_password",
    }
    descriptions = {
      item.findtext("name"): item.findtext("description") or ""
      for item in root.findall("property")
      if item.findtext("name") in password_names
    }
    self.assertEqual(password_names, set(descriptions))
    for description in descriptions.values():
      self.assertIn("Special characters are supported", description)
      self.assertNotIn("Unsupported special characters", description)

  def test_advisor_has_no_distribution_specific_legacy_names(self):
    source = (RANGER / "service_advisor.py").read_text()
    self.assertNotIn("HDP", source)
    self.assertNotIn("FromHDP", source)
    self.assertIn("recommendRangerUsersyncConfigurations", source)
    self.assertIn("recommendModernRangerConfigurations", source)


if __name__ == "__main__":
  unittest.main()
