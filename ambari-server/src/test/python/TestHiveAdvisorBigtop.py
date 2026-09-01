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


HIVE = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/HIVE"
)


def load_module(module_name, path):
  spec = importlib.util.spec_from_file_location(module_name, path)
  module = importlib.util.module_from_spec(spec)
  spec.loader.exec_module(module)
  return module


ADVISOR = load_module("bigtop_hive_service_advisor", HIVE / "service_advisor.py")


def property_writer(configurations, config_type):
  def write(name, value):
    configurations.setdefault(config_type, {}).setdefault("properties", {})[
      name
    ] = value

  return write


class TestHiveServiceAdvisor(unittest.TestCase):
  def setUp(self):
    self.recommender = object.__new__(ADVISOR.HiveRecommender)
    self.recommender.logger = MagicMock()

  def configure_mocks(self, configurations, metastore_hosts=None, hs2_hosts=None):
    self.recommender.putProperty = MagicMock(
      side_effect=lambda configs, config_type, services: property_writer(
        configurations, config_type
      )
    )
    self.recommender.putPropertyAttribute = MagicMock(
      return_value=lambda name, attribute, value: configurations.setdefault(
        "hiveserver2-site", {}
      ).setdefault("property_attributes", {}).setdefault(name, {}).__setitem__(
        attribute, value
      )
    )
    self.recommender.getHostsWithComponent = MagicMock(
      side_effect=lambda service, component, services, hosts: (
        metastore_hosts if component == "HIVE_METASTORE" else hs2_hosts
      )
    )

  def test_ranger_recommendation_is_deterministic_and_preserves_custom_restrictions(self):
    configurations = {}
    services = {
      "services": [],
      "configurations": {
        "hive-env": {
          "properties": {
            "hive_security_authorization": "Ranger",
            "hive_user": "hive",
          }
        },
        "hiveserver2-site": {
          "properties": {"hive.conf.restricted.list": "custom.key,custom.key"}
        },
      },
    }
    self.configure_mocks(configurations)

    self.recommender.recommendHiveConfigurations(
      configurations, {"cpu": 8}, services, {"items": []}
    )

    hs2 = configurations["hiveserver2-site"]["properties"]
    self.assertEqual("true", hs2["hive.security.authorization.enabled"])
    self.assertEqual(ADVISOR.RANGER_AUTHORIZER, hs2["hive.security.authorization.manager"])
    self.assertEqual(
      [
        "custom.key",
        "hive.security.authorization.enabled",
        "hive.security.authorization.manager",
        "hive.security.authenticator.manager",
      ],
      hs2["hive.conf.restricted.list"].split(","),
    )
    self.assertEqual(
      "hive",
      configurations["ranger-hive-plugin-properties"]["properties"][
        "REPOSITORY_CONFIG_USERNAME"
      ],
    )

  def test_atlas_hook_is_added_once_without_losing_existing_hooks(self):
    configurations = {}
    services = {
      "services": [{"StackServices": {"service_name": "ATLAS"}}],
      "configurations": {
        "hive-env": {"properties": {"hive_security_authorization": "None"}},
        "hive-site": {
          "properties": {
            "hive.exec.post.hooks": f"example.Custom,{ADVISOR.ATLAS_HOOK},{ADVISOR.ATLAS_HOOK}"
          }
        },
      },
    }
    self.configure_mocks(configurations)

    self.recommender.recommendHiveConfigurations(
      configurations, {"cpu": 4}, services, {"items": []}
    )

    hooks = configurations["hive-site"]["properties"][
      "hive.exec.post.hooks"
    ].split(",")
    self.assertEqual(["example.Custom", ADVISOR.ATLAS_HOOK], hooks)
    self.assertEqual(
      "true", configurations["hive-env"]["properties"]["hive.atlas.hook"]
    )

  def test_bigtop_database_mapping_uses_mysql_host(self):
    configurations = {}
    services = {
      "services": [],
      "configurations": {
        "hive-env": {
          "properties": {
            "hive_database": "New MySQL Database",
            "hive_security_authorization": "None",
          }
        },
        "hive-site": {
          "properties": {"ambari.hive.db.schema.name": "hive_metastore"}
        },
      },
    }
    self.configure_mocks(configurations)
    self.recommender.getHostsWithComponent.side_effect = (
      lambda service, component, services, hosts: (
        [{"Hosts": {"host_name": "mysql.example.test"}}]
        if component == "MYSQL_SERVER"
        else []
      )
    )

    self.recommender.recommendHiveConfigurations(
      configurations, {"cpu": 4}, services, {"items": []}
    )

    self.assertEqual(
      "mysql", configurations["hive-env"]["properties"]["hive_database_type"]
    )
    hive_site = configurations["hive-site"]["properties"]
    self.assertEqual(
      "org.mariadb.jdbc.Driver",
      hive_site["javax.jdo.option.ConnectionDriverName"],
    )
    self.assertEqual(
      "jdbc:mariadb://mysql.example.test/hive_metastore?createDatabaseIfNotExist=true",
      hive_site["javax.jdo.option.ConnectionURL"],
    )

  def test_metastore_uris_and_heap_use_actual_bigtop_hosts(self):
    configurations = {}
    services = {
      "services": [],
      "configurations": {
        "hive-site": {
          "properties": {
            "hive.metastore.uris": "thrift://old-metastore:19083"
          }
        }
      },
    }
    self.configure_mocks(
      configurations,
      metastore_hosts=[
        {"Hosts": {"host_name": "meta-a"}},
        {"Hosts": {"host_name": "meta-b"}},
      ],
      hs2_hosts=[{"Hosts": {"host_name": "hs2", "total_mem": 16 * 1024 * 1024}}],
    )

    self.recommender.recommendHiveConfigurations(
      configurations, {"cpu": 16}, services, {"items": []}
    )

    self.assertEqual(
      "thrift://meta-a:19083,thrift://meta-b:19083",
      configurations["hive-site"]["properties"]["hive.metastore.uris"],
    )
    self.assertEqual("2048", configurations["hive-env"]["properties"]["hive.heapsize"])
    self.assertEqual(
      "2", configurations["hive-site"]["properties"]["hive.compactor.worker.threads"]
    )

  def test_source_contains_no_hdp_or_interactive_advisor_branches(self):
    source = (HIVE / "service_advisor.py").read_text().lower()
    for obsolete in ("hdp", "hive_interactive", "hive-interactive", "llap"):
      with self.subTest(obsolete=obsolete):
        self.assertNotIn(obsolete, source)


if __name__ == "__main__":
  unittest.main()
