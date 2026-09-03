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

from ambari_agent import main

main.MEMORY_LEAK_DEBUG_FILEPATH = "/tmp/memory_leak_debug.out"
import os
import tempfile
import shutil
from unittest import TestCase

from ambari_agent.ClusterCache import ClusterCache
from mock.mock import patch, MagicMock
from ambari_commons import OSCheck
from only_for_platform import os_distro_value


class TestClusterCache(TestCase):
  """
  Test suite for verifying encryption behavior of ClusterCache.

  It covers:
  - encryption/decryption round-trip when secret are provided
  - behavior when encryption is effectively disabled (no secret)
  """

  # so that ClusterCache initialization is OS-agnostic in this test.
  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  def setUp(self):
    # Create a temporary directory that will be cleaned up after each test.
    self.tmpdir = tempfile.mkdtemp()
    cluster_cache_dir = self.tmpdir + "/cluster_cache_dir"

    # Instance with encryption enabled (secret provided).
    self.cluster_cache_encrypted = DummyClusterCache(
      cluster_cache_dir,
      "super_secret"
    )

    # Instance with encryption disabled (no secret).
    self.cluster_cache_unencrypted = DummyClusterCache(cluster_cache_dir)

  @patch.object(os, "chmod")
  def test_enc(self, chmod_mock):
    """
    Verify that:
    - encrypted instance changes the data and can restore it back
    - unencrypted instance is a no-op for encrypt/decrypt
    """
    string_json = '{"a": 1, "b": 2}'

    # Encrypted cache should not store raw JSON.
    encrypted = self.cluster_cache_encrypted._encrypt_data(string_json)
    self.assertNotEqual(string_json, encrypted)
    # Round-trip must produce original JSON.
    decrypted = self.cluster_cache_encrypted._decrypt_data(encrypted)
    self.assertEqual(string_json, decrypted)

    # For unencrypted cache, encrypt/decrypt should behave as pass-through.
    string_json = '{"a": 1, "b": 2}'
    encrypted = self.cluster_cache_unencrypted._encrypt_data(string_json)
    self.assertEqual(string_json, encrypted)
    decrypted = self.cluster_cache_unencrypted._decrypt_data(encrypted)
    self.assertEqual(string_json, decrypted)

  @patch.object(os, "chmod")
  def test_encryption_enable(self, chmod_mock):
    """
    Verify that _is_encryption_enabled reflects whether secret were provided.
    """
    # When secret are given, encryption flag should reflect enabled status.
    self.assertFalse(self.cluster_cache_encrypted._is_encryption_enabled())

    # When no secret, encryption should be reported as disabled.
    self.assertTrue(self.cluster_cache_unencrypted._is_encryption_enabled())

  def tearDown(self):
    shutil.rmtree(self.tmpdir)

class DummyClusterCache(ClusterCache):
  """
  Minimal ClusterCache subclass used only for unit testing.

  It overrides get_cache_name to avoid depending on real production cache names.
  """
  def get_cache_name(self):
    # Dummy implementation just for tests.
    return "configuration"
