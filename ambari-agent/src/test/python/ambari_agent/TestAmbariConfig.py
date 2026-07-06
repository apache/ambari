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
from unittest import TestCase
from ambari_agent.AmbariConfig import AmbariConfig
import sys

import logging


class TestAmbariConfig(TestCase):
  def setUp(self):
    # save original open() method for later use
    self.original_open = open

  def tearDown(self):
    sys.stdout = sys.__stdout__

  logger = logging.getLogger()

  def test_ambari_config_get(self):
    config = AmbariConfig()
    # default
    self.assertEqual(config.get("security", "keysdir"), "/tmp/ambari-agent")
    # non-default
    config.set("security", "keysdir", "/tmp/non-default-path")
    self.assertEqual(config.get("security", "keysdir"), "/tmp/non-default-path")
    # whitespace handling
    config.set("security", "keysdir", " /tmp/non-stripped")
    self.assertEqual(config.get("security", "keysdir"), "/tmp/non-stripped")

    # test default value
    open_files_ulimit = config.get_ulimit_open_files()
    self.assertEqual(open_files_ulimit, 0)

    # set a value
    open_files_ulimit = 128000
    config.set_ulimit_open_files(open_files_ulimit)
    self.assertEqual(config.get_ulimit_open_files(), open_files_ulimit)

  def test_ambari_config_get_command_file_retention_policy(self):
    config = AmbariConfig()

    # unset value yields, "keep"
    if config.has_option("agent", AmbariConfig.COMMAND_FILE_RETENTION_POLICY_PROPERTY):
      config.remove_option("agent", AmbariConfig.COMMAND_FILE_RETENTION_POLICY_PROPERTY)
    self.assertEqual(
      config.command_file_retention_policy,
      AmbariConfig.COMMAND_FILE_RETENTION_POLICY_REMOVE,
    )

    config.set(
      "agent",
      AmbariConfig.COMMAND_FILE_RETENTION_POLICY_PROPERTY,
      AmbariConfig.COMMAND_FILE_RETENTION_POLICY_KEEP,
    )
    self.assertEqual(
      config.command_file_retention_policy,
      AmbariConfig.COMMAND_FILE_RETENTION_POLICY_KEEP,
    )

    config.set(
      "agent",
      AmbariConfig.COMMAND_FILE_RETENTION_POLICY_PROPERTY,
      AmbariConfig.COMMAND_FILE_RETENTION_POLICY_REMOVE,
    )
    self.assertEqual(
      config.command_file_retention_policy,
      AmbariConfig.COMMAND_FILE_RETENTION_POLICY_REMOVE,
    )

    config.set(
      "agent",
      AmbariConfig.COMMAND_FILE_RETENTION_POLICY_PROPERTY,
      AmbariConfig.COMMAND_FILE_RETENTION_POLICY_REMOVE_ON_SUCCESS,
    )
    self.assertEqual(
      config.command_file_retention_policy,
      AmbariConfig.COMMAND_FILE_RETENTION_POLICY_REMOVE_ON_SUCCESS,
    )

    # Invalid value yields, "keep"
    config.set(
      "agent", AmbariConfig.COMMAND_FILE_RETENTION_POLICY_PROPERTY, "invalid_value"
    )
    self.assertEqual(
      config.command_file_retention_policy,
      AmbariConfig.COMMAND_FILE_RETENTION_POLICY_KEEP,
    )

  def test_cache_dir_and_derived_paths(self):
    """
    Test that cache_dir and derived cache paths (stacks_dir, alerts_cachedir, etc.)
    are correctly initialized and can be individually updated via their setters.
    """
    config = AmbariConfig()

    # Initial state - cache_dir uses built-in default (/tmp)
    self.assertEqual(config.cache_dir, "/tmp")

    # Derived paths should be based on default cache_dir (/tmp)
    self.assertEqual(config.stacks_dir, os.path.join("/tmp", "stacks"))
    self.assertEqual(config.alerts_cachedir, os.path.join("/tmp", "alerts"))
    self.assertEqual(config.cluster_cache_dir, os.path.join("/tmp", "cluster_cache"))
    self.assertEqual(config.common_services_dir, os.path.join("/tmp", "common-services"))
    self.assertEqual(config.extensions_dir, os.path.join("/tmp", "extensions"))
    self.assertEqual(config.host_scripts_dir, os.path.join("/tmp", "host_scripts"))

    # Test that derived path setters work correctly
    new_stacks_dir = "/custom/stacks"
    config.stacks_dir = new_stacks_dir
    self.assertEqual(config.stacks_dir, new_stacks_dir)

    new_alerts_dir = "/custom/alerts"
    config.alerts_cachedir = new_alerts_dir
    self.assertEqual(config.alerts_cachedir, new_alerts_dir)

    new_cluster_cache_dir = "/custom/cluster_cache"
    config.cluster_cache_dir = new_cluster_cache_dir
    self.assertEqual(config.cluster_cache_dir, new_cluster_cache_dir)

    new_common_services_dir = "/custom/common-services"
    config.common_services_dir = new_common_services_dir
    self.assertEqual(config.common_services_dir, new_common_services_dir)

    new_extensions_dir = "/custom/extensions"
    config.extensions_dir = new_extensions_dir
    self.assertEqual(config.extensions_dir, new_extensions_dir)

    new_host_scripts_dir = "/custom/host_scripts"
    config.host_scripts_dir = new_host_scripts_dir
    self.assertEqual(config.host_scripts_dir, new_host_scripts_dir)
