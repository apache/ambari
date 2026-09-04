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

import json
import os
import tempfile

from ambari_agent.ClusterConfigurationCache import ClusterConfigurationCache
from ambari_agent.ClusterCache import ClusterCache

from unittest.mock import patch
from unittest import TestCase


class TestClusterConfigurationCache(TestCase):
  def setUp(self):
    self.temp_directory = tempfile.TemporaryDirectory()

  def tearDown(self):
    self.temp_directory.cleanup()

  def test_base_cache_abstract_operations_raise_not_implemented_error(self):
    with self.assertRaises(NotImplementedError):
      ClusterCache(self.temp_directory.name)
    cache = dict.__new__(ClusterCache)
    with self.assertRaises(NotImplementedError):
      cache.cache_delete({}, None)

  def test_rewrite_persists_data_and_hash_for_restart(self):
    configuration = {"0": {"foo-site": {"foo": "bar", "foobar": "baz"}}}
    cache = ClusterConfigurationCache(self.temp_directory.name)

    cache.rewrite_cache(configuration, "hash-1")

    with open(
      os.path.join(self.temp_directory.name, "configurations.json"),
      encoding="utf-8",
    ) as stream:
      self.assertEqual(configuration, json.load(stream))
    with open(
      os.path.join(self.temp_directory.name, ".configurations.hash"),
      encoding="utf-8",
    ) as stream:
      self.assertEqual("hash-1", stream.read())

    restarted_cache = ClusterConfigurationCache(self.temp_directory.name)
    self.assertEqual("bar", restarted_cache["0"]["foo-site"]["foo"])
    self.assertEqual("hash-1", restarted_cache.hash)

  def test_json_compatibility_corpus_survives_cache_restart(self):
    configuration = {
      "0": {
        "ordered": {"first": 1, "second": 2},
        "unicode": "cluster-\u96c6\u7fa4",
        "largeInteger": 9223372036854775808,
        "notANumber": float("nan"),
      }
    }
    cache = ClusterConfigurationCache(self.temp_directory.name)

    cache.rewrite_cache(configuration, "compatibility-hash")
    restarted_cache = ClusterConfigurationCache(self.temp_directory.name)

    self.assertEqual(
      ["first", "second"], list(restarted_cache["0"]["ordered"].keys())
    )
    self.assertEqual("cluster-\u96c6\u7fa4", restarted_cache["0"]["unicode"])
    self.assertEqual(
      9223372036854775808, restarted_cache["0"]["largeInteger"]
    )
    self.assertNotEqual(
      restarted_cache["0"]["notANumber"],
      restarted_cache["0"]["notANumber"],
    )

  def test_persist_failure_restores_previous_in_memory_cache(self):
    cache = ClusterConfigurationCache(self.temp_directory.name)
    old_configuration = {"0": {"foo-site": {"value": "old"}}}
    cache.rewrite_cache(old_configuration, "old-hash")

    real_replace = os.replace

    def reject_new_json(source, destination):
      if destination.endswith("configurations.json") and "previous" not in source:
        raise OSError("disk full")
      return real_replace(source, destination)

    with patch("ambari_agent.ClusterCache.os.replace", side_effect=reject_new_json):
      with self.assertRaisesRegex(OSError, "disk full"):
        cache.rewrite_cache(
          {"0": {"foo-site": {"value": "new"}}}, "new-hash"
        )

    self.assertEqual("old", cache["0"]["foo-site"]["value"])
    self.assertEqual("old-hash", cache.hash)
    self.assertFalse(
      any(name.startswith(".cache-") for name in os.listdir(self.temp_directory.name))
    )

  def test_hash_commit_failure_restores_previous_disk_generation(self):
    cache = ClusterConfigurationCache(self.temp_directory.name)
    cache.rewrite_cache({"0": {"value": "old"}}, "old-hash")
    real_replace = os.replace

    def reject_new_hash(source, destination):
      if destination.endswith(".configurations.hash") and "previous" not in source:
        raise OSError("hash disk full")
      return real_replace(source, destination)

    with patch("ambari_agent.ClusterCache.os.replace", side_effect=reject_new_hash):
      with self.assertRaisesRegex(OSError, "hash disk full"):
        cache.rewrite_cache({"0": {"value": "new"}}, "new-hash")

    restarted_cache = ClusterConfigurationCache(self.temp_directory.name)
    self.assertEqual("old", restarted_cache["0"]["value"])
    self.assertEqual("old-hash", restarted_cache.hash)

  def test_removing_hash_is_persisted(self):
    cache = ClusterConfigurationCache(self.temp_directory.name)
    cache.rewrite_cache({"0": {}}, "hash-1")

    cache.rewrite_cache({"0": {}}, None)

    self.assertIsNone(cache.hash)
    self.assertFalse(
      os.path.exists(os.path.join(self.temp_directory.name, ".configurations.hash"))
    )

  def test_json_generation_is_persisted_before_hash_commit_marker(self):
    cache = ClusterConfigurationCache(self.temp_directory.name)
    events = []
    real_replace = os.replace

    def record_replace(source, destination):
      if destination.endswith("configurations.json"):
        events.append("replace-json")
      elif destination.endswith(".configurations.hash"):
        events.append("replace-hash")
      return real_replace(source, destination)

    with patch("ambari_agent.ClusterCache.os.replace", side_effect=record_replace):
      with patch.object(
        cache,
        "_fsync_cache_directory",
        side_effect=lambda: events.append("fsync-directory"),
      ):
        cache.rewrite_cache({"0": {"value": "new"}}, "new-hash")

    json_replace = events.index("replace-json")
    hash_replace = events.index("replace-hash")
    self.assertEqual("fsync-directory", events[json_replace + 1])
    self.assertLess(json_replace, hash_replace)
