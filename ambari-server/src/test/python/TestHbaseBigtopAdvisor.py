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
from unittest.mock import MagicMock, call


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

  def test_obsolete_atlas_hook_is_removed_without_losing_other_coprocessors(
    self,
  ):
    put_hbase_site = MagicMock()
    put_hbase_env_attribute = MagicMock()
    self.recommender.putProperty = MagicMock(return_value=put_hbase_site)
    self.recommender.putPropertyAttribute = MagicMock(
      return_value=put_hbase_env_attribute
    )
    services = {
      "configurations": {
        "hbase-site": {
          "properties": {
            "hbase.coprocessor.master.classes": (
              "com.example.CustomCoprocessor,"
              "org.apache.atlas.hbase.hook.HBaseAtlasCoprocessor"
            )
          }
        },
        "hbase-env": {"properties": {"hbase.atlas.hook": "true"}},
      }
    }

    self.recommender.removeObsoleteAtlasHook({}, {}, services, {})

    put_hbase_site.assert_called_once_with(
      "hbase.coprocessor.master.classes", "com.example.CustomCoprocessor"
    )
    put_hbase_env_attribute.assert_called_once_with(
      "hbase.atlas.hook", "delete", "true"
    )

  def test_obsolete_atlas_cleanup_prefers_updated_coprocessors(self):
    put_hbase_site = MagicMock()
    self.recommender.putProperty = MagicMock(return_value=put_hbase_site)
    self.recommender.putPropertyAttribute = MagicMock()
    configurations = {
      "hbase-site": {
        "properties": {
          "hbase.coprocessor.master.classes": "com.example.UpdatedCoprocessor"
        }
      }
    }
    services = {
      "configurations": {
        "hbase-site": {
          "properties": {
            "hbase.coprocessor.master.classes": (
              "org.apache.atlas.hbase.hook.HBaseAtlasCoprocessor"
            )
          }
        }
      }
    }

    self.recommender.removeObsoleteAtlasHook(configurations, {}, services, {})

    put_hbase_site.assert_not_called()

  def test_phoenix_recommendations_prefer_updated_configuration(self):
    services = {
      "configurations": {
        "hbase-env": {"properties": {"phoenix_sql_enabled": "false"}}
      }
    }
    self.assertFalse(self.recommender.isPhoenixEnabled({}, services))
    self.assertTrue(
      self.recommender.isPhoenixEnabled(
        {"hbase-env": {"properties": {"phoenix_sql_enabled": True}}},
        services,
      )
    )
    self.assertFalse(
      self.recommender.isPhoenixEnabled(
        {"hbase-env": {"properties": {"phoenix_sql_enabled": "invalid"}}},
        services,
      )
    )

  def test_phoenix_advisor_preserves_secure_udf_default_and_updated_config(self):
    put_hbase_site = MagicMock()
    put_hbase_env = MagicMock()
    put_hbase_site_attribute = MagicMock()
    put_hbase_env_attribute = MagicMock()
    self.recommender.putProperty = MagicMock(
      side_effect=lambda configs, config_type, current: (
        put_hbase_site if config_type == "hbase-site" else put_hbase_env
      )
    )
    self.recommender.putPropertyAttribute = MagicMock(
      side_effect=lambda configs, config_type: (
        put_hbase_site_attribute
        if config_type == "hbase-site"
        else put_hbase_env_attribute
      )
    )
    configurations = {
      "hbase-env": {"properties": {"phoenix_sql_enabled": "true"}},
      "hbase-site": {
        "properties": {
          "hbase.rpc.controllerfactory.class": (
            "org.apache.hadoop.hbase.ipc.controller.ServerRpcControllerFactory"
          )
        }
      },
    }
    services = {
      "configurations": {
        "hbase-env": {"properties": {"phoenix_sql_enabled": "false"}},
        "hbase-site": {
          "properties": {
            "hbase.rpc.controllerfactory.class": "com.example.CurrentFactory"
          }
        },
      }
    }

    self.recommender.recommendOffheapAndPhoenix(
      configurations, {"hbaseRam": 8}, services, {}
    )

    self.assertIn(
      call("hbase.rpc.controllerfactory.class", "delete", "true"),
      put_hbase_site_attribute.call_args_list,
    )
    self.assertIn(
      call(
        "hbase.region.server.rpc.scheduler.factory.class",
        "org.apache.hadoop.hbase.ipc.PhoenixRpcSchedulerFactory",
      ),
      put_hbase_site.call_args_list,
    )
    self.assertNotIn(
      call("phoenix.functions.allowUserDefinedFunctions", "true"),
      put_hbase_site.call_args_list,
    )

  def test_phoenix_validation_rejects_ambiguous_values(self):
    validator = object.__new__(ADVISOR.HBASEValidator)
    for value in (True, False, " true ", "FALSE"):
      with self.subTest(value=value):
        self.assertEqual(
          [],
          validator.validatePhoenixEnablement(
            {"phoenix_sql_enabled": value}, {}, {}, {}, {}
          ),
        )
    for value in (None, "", "yes", "1", 1):
      with self.subTest(value=value):
        problems = validator.validatePhoenixEnablement(
          {"phoenix_sql_enabled": value}, {}, {}, {}, {}
        )
        self.assertEqual(1, len(problems))
        self.assertIn("must be true or false", problems[0]["message"])

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
