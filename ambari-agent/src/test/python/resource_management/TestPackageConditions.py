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

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
"""

from unittest import TestCase

from unittest.mock import patch

from resource_management.core.exceptions import Fail
from resource_management.libraries.functions import package_conditions


class TestPackageConditions(TestCase):
  RANGER_PLUGIN_CONDITIONS = (
    (
      package_conditions.should_install_ranger_hdfs_plugin,
      "/configurations/ranger-hdfs-plugin-properties/ranger-hdfs-plugin-enabled",
    ),
    (
      package_conditions.should_install_ranger_yarn_plugin,
      "/configurations/ranger-yarn-plugin-properties/ranger-yarn-plugin-enabled",
    ),
    (
      package_conditions.should_install_ranger_hive_plugin,
      "/configurations/ranger-hive-plugin-properties/ranger-hive-plugin-enabled",
    ),
    (
      package_conditions.should_install_ranger_hbase_plugin,
      "/configurations/ranger-hbase-plugin-properties/ranger-hbase-plugin-enabled",
    ),
    (
      package_conditions.should_install_ranger_kafka_plugin,
      "/configurations/ranger-kafka-plugin-properties/ranger-kafka-plugin-enabled",
    ),
  )

  def test_ranger_plugin_packages_are_installed_only_when_enabled(self):
    for condition, path in self.RANGER_PLUGIN_CONDITIONS:
      with self.subTest(condition=condition.__name__):
        with patch.object(package_conditions, "default", return_value="No") as default:
          self.assertFalse(condition())
          default.assert_called_once_with(path, "No")

        with patch.object(package_conditions, "default", return_value="Yes") as default:
          self.assertTrue(condition())
          default.assert_called_once_with(path, "No")

        for invalid in (None, 1, "true", "enabled", ""):
          with (
            self.subTest(condition=condition.__name__, invalid=invalid),
            patch.object(package_conditions, "default", return_value=invalid),
            self.assertRaisesRegex(Fail, "must be Yes or No"),
          ):
            condition()

  def test_infra_solr_packages_follow_only_bigtop_components(self):
    cases = (
      ("INFRA_SOLR", package_conditions.should_install_infra_solr, True),
      (
        "INFRA_SOLR_CLIENT",
        package_conditions.should_install_infra_solr_client,
        True,
      ),
      ("RANGER_ADMIN", package_conditions.should_install_infra_solr_client, True),
      ("UNRELATED_SERVER", package_conditions.should_install_infra_solr_client, False),
    )
    for role, condition, expected in cases:
      with (
        self.subTest(role=role),
        patch.object(
          package_conditions.Script,
          "get_config",
          return_value={"role": role},
        ),
      ):
        self.assertEqual(expected, condition())

  def test_infra_solr_conditions_are_public(self):
    self.assertIn("should_install_infra_solr", package_conditions.__all__)
    self.assertIn("should_install_infra_solr_client", package_conditions.__all__)

  def test_ranger_kafka_package_condition_is_public(self):
    self.assertIn("should_install_ranger_kafka_plugin", package_conditions.__all__)
