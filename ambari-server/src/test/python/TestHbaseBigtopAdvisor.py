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
from unittest.mock import MagicMock


HBASE = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/HBASE"
)


def load_module(module_name, path):
  spec = importlib.util.spec_from_file_location(module_name, path)
  module = importlib.util.module_from_spec(spec)
  spec.loader.exec_module(module)
  return module


ADVISOR = load_module("bigtop_hbase_service_advisor", HBASE / "service_advisor.py")


class TestHbaseServiceAdvisor(unittest.TestCase):
  def setUp(self):
    self.recommender = object.__new__(ADVISOR.HBASERecommender)

  def test_coprocessors_are_deduplicated_without_reordering(self):
    custom = "com.example.CustomCoprocessor"
    configurations = {
      "hbase-site": {
        "properties": {
          "hbase.security.authorization": "true",
          "hbase.coprocessor.master.classes": (
            f"{custom},{custom},{{{{hbase_coprocessor_master_classes}}}}"
          ),
        }
      }
    }
    services = {"configurations": {}, "services": []}

    coprocessors, attributes = (
      self.recommender.calculateCoprocessorConfigurations(
        services,
        configurations,
        ranger_hbase_plugin_enabled=False,
        is_kerberos_enabled=True,
      )
    )

    access_controller = (
      "org.apache.hadoop.hbase.security.access.AccessController"
    )
    self.assertEqual(
      [custom, access_controller],
      coprocessors["hbase.coprocessor.master.classes"],
    )
    self.assertEqual(
      access_controller,
      coprocessors["hbase.coprocessor.regionserver.classes"][0],
    )
    self.assertNotIn("com.xasecure", str(coprocessors))
    self.assertEqual([], attributes["hbase.coprocessor.master.classes"])

  def test_ranger_replaces_native_authorization_in_all_roles(self):
    configurations = {
      "hbase-site": {
        "properties": {"hbase.security.authorization": "true"}
      }
    }
    services = {"configurations": {}, "services": []}
    coprocessors, _ = self.recommender.calculateCoprocessorConfigurations(
      services,
      configurations,
      ranger_hbase_plugin_enabled=True,
      is_kerberos_enabled=True,
    )
    ranger = (
      "org.apache.ranger.authorization.hbase.RangerAuthorizationCoprocessor"
    )
    native = "org.apache.hadoop.hbase.security.access.AccessController"
    for classes in coprocessors.values():
      self.assertIn(ranger, classes)
      self.assertNotIn(native, classes)

  def test_kerberos_does_not_enable_native_authorization(self):
    configurations = {
      "hbase-site": {
        "properties": {"hbase.security.authorization": "false"}
      }
    }
    services = {"configurations": {}, "services": []}
    coprocessors, _ = self.recommender.calculateCoprocessorConfigurations(
      services,
      configurations,
      ranger_hbase_plugin_enabled=False,
      is_kerberos_enabled=True,
    )
    native = "org.apache.hadoop.hbase.security.access.AccessController"
    for classes in coprocessors.values():
      self.assertNotIn(native, classes)

  def test_atlas_recommendation_preserves_existing_coprocessors(self):
    custom = "com.example.CustomCoprocessor"
    configurations = {
      "hbase-site": {"properties": {}},
      "hbase-env": {"properties": {}},
    }
    services = {
      "services": [{"StackServices": {"service_name": "ATLAS"}}],
      "configurations": {
        "hbase-site": {
          "properties": {"hbase.coprocessor.master.classes": custom}
        },
        "hbase-env": {"properties": {"hbase.atlas.hook": "false"}},
      },
    }

    def put_property(config_type):
      def put(name, value):
        configurations.setdefault(config_type, {}).setdefault(
          "properties", {}
        )[name] = value

      return put

    self.recommender.putProperty = MagicMock(
      side_effect=lambda configs, config_type, current: put_property(config_type)
    )
    self.recommender.logger = MagicMock()

    self.recommender.recommendAtlasHook(
      configurations, {}, services, {"items": []}
    )

    master_classes = configurations["hbase-site"]["properties"][
      "hbase.coprocessor.master.classes"
    ].split(",")
    self.assertEqual(
      [custom, "org.apache.atlas.hbase.hook.HBaseAtlasCoprocessor"],
      master_classes,
    )

  def test_boolean_and_number_validation_reject_ambiguous_values(self):
    validator = object.__new__(ADVISOR.HBASEValidator)
    self.assertTrue(validator.is_enabled("Yes"))
    self.assertTrue(validator.is_enabled(" true "))
    self.assertFalse(validator.is_enabled("enabled"))
    self.assertTrue(validator.is_number("0.4"))
    self.assertFalse(validator.is_number("NaN"))
    self.assertFalse(validator.is_number("Infinity"))
    self.assertFalse(validator.is_number(None))


if __name__ == "__main__":
  unittest.main()
