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

from unittest.mock import MagicMock, patch
from unittest import TestCase

from ambari_agent.ConfigurationBuilder import ConfigurationBuilder
from ambari_agent.InitializerModule import InitializerModule


class TestConfigurationBuilder(TestCase):
  def test_background_execution_pins_one_complete_definition_generation(self):
    snapshot = "a" * 64
    global_metadata = {
      "mpack_definition_snapshot": snapshot,
      "mpack_resource_contract": "MPACK_RESOURCES_V1",
      "mpack_resource_references": "{}",
      "resource_archive_digests": "{}",
    }
    command = {
      "ambariLevelParams": global_metadata,
      "clusterLevelParams": {"mpack_definition_snapshot": snapshot, "hooks_folder": ""},
      "serviceLevelParams": {"mpack_definition_snapshot": snapshot},
    }
    ConfigurationBuilder._pin_definition_resources(command)
    first_identity = command["commandParams"]["mpack_execution_id"]
    self.assertEqual(snapshot, command["commandParams"]["mpack_definition_snapshot"])
    self.assertEqual("", command["commandParams"]["mpack_hooks_folder"])
    self.assertNotIn("mpack_execution_id", global_metadata)
    ConfigurationBuilder._pin_definition_resources(command)
    self.assertNotEqual(first_identity, command["commandParams"]["mpack_execution_id"])

  def test_background_execution_rejects_partial_metadata_publication(self):
    command = {
      "ambariLevelParams": {
        "mpack_definition_snapshot": "a" * 64,
        "mpack_resource_contract": "MPACK_RESOURCES_V1",
        "mpack_resource_references": "{}",
        "resource_archive_digests": "{}",
      },
      "clusterLevelParams": {"mpack_definition_snapshot": "b" * 64},
    }
    with self.assertRaises(ValueError):
      ConfigurationBuilder._pin_definition_resources(command)
    command["clusterLevelParams"]["mpack_definition_snapshot"] = "a" * 64
    command["serviceLevelParams"] = {"mpack_definition_snapshot": "b" * 64}
    with self.assertRaises(ValueError):
      ConfigurationBuilder._pin_definition_resources(command)

  @patch(
    "ambari_agent.hostname.public_hostname",
    new=MagicMock(return_value="c6401.ambari.apache.org"),
  )
  def test_public_fqdn(self):
    initializer_module = InitializerModule()

    config_builder = ConfigurationBuilder(initializer_module)
    self.assertEqual("c6401.ambari.apache.org", config_builder.public_fqdn)

  @patch("ambari_agent.ConfigurationBuilder.os.path.isfile", return_value=True)
  def test_component_java_home_override_takes_precedence(self, isfile_mock):
    command = self._command_with_overrides(
      '{"SOLR": {"home": "/java/service", "version": 11}, '
      '"SOLR_SERVER": {"home": "/java/component", "version": 17}}'
    )

    ConfigurationBuilder._apply_java_home_override(
      command, "SOLR", "SOLR_SERVER"
    )

    self.assertEqual("/java/component", command["ambariLevelParams"]["java_home"])
    self.assertEqual("17", command["ambariLevelParams"]["java_version"])
    self.assertEqual(
      "/java/ambari", command["ambariLevelParams"]["ambari_java_home"]
    )
    self.assertIsNone(command["ambariLevelParams"]["jdk_name"])
    isfile_mock.assert_called_once_with("/java/component/bin/java")

  @patch("ambari_agent.ConfigurationBuilder.os.path.isfile", return_value=True)
  def test_service_java_home_override_is_used_as_fallback(self, isfile_mock):
    command = self._command_with_overrides(
      '{"solr": {"home": "/java/service", "version": "11"}}'
    )

    ConfigurationBuilder._apply_java_home_override(
      command, "SOLR", "SOLR_SERVER"
    )

    self.assertEqual("/java/service", command["ambariLevelParams"]["java_home"])
    self.assertEqual("11", command["ambariLevelParams"]["java_version"])
    isfile_mock.assert_called_once_with("/java/service/bin/java")

  def test_invalid_java_home_override_keeps_stack_default(self):
    command = self._command_with_overrides(
      '{"SOLR": {"home": "/java/service"}}'
    )

    ConfigurationBuilder._apply_java_home_override(
      command, "SOLR", "SOLR_SERVER"
    )

    self.assertEqual("/java/stack", command["ambariLevelParams"]["java_home"])
    self.assertEqual("11", command["ambariLevelParams"]["java_version"])

  def test_invalid_java_home_override_version_type_keeps_stack_default(self):
    command = self._command_with_overrides(
      '{"SOLR": {"home": "/java/service", "version": true}}'
    )

    ConfigurationBuilder._apply_java_home_override(
      command, "SOLR", "SOLR_SERVER"
    )

    self.assertEqual("/java/stack", command["ambariLevelParams"]["java_home"])
    self.assertEqual("11", command["ambariLevelParams"]["java_version"])

  @patch("ambari_agent.ConfigurationBuilder.os.path.isfile", return_value=False)
  def test_missing_java_home_override_keeps_stack_default(self, isfile_mock):
    command = self._command_with_overrides(
      '{"SOLR": {"home": "/java/missing", "version": 17}}'
    )

    ConfigurationBuilder._apply_java_home_override(
      command, "SOLR", "SOLR_SERVER"
    )

    self.assertEqual("/java/stack", command["ambariLevelParams"]["java_home"])
    self.assertEqual("11", command["ambariLevelParams"]["java_version"])
    isfile_mock.assert_called_once_with("/java/missing/bin/java")

  @staticmethod
  def _command_with_overrides(overrides):
    return {
      "ambariLevelParams": {
        "ambari_java_home": "/java/ambari",
        "ambari_java_version": "17",
        "java_home": "/java/stack",
        "java_version": "11",
        "jdk_name": "stack-jdk.tar.gz",
        "jce_name": "stack-jce.zip",
      },
      "configurations": {
        "cluster-env": {"java_home_overrides": overrides},
      },
    }
