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

import tempfile
from unittest import TestCase
from unittest.mock import MagicMock, patch

from ambari_agent.ClusterTopologyCache import ClusterTopologyCache


class TestClusterTopologyCache(TestCase):
  def _new_cache(self, cache_directory, topology):
    with patch(
      "ambari_agent.ClusterTopologyCache.hostname.hostname",
      return_value="agent.example",
    ):
      cache = ClusterTopologyCache(cache_directory, MagicMock())
    cache.rewrite_cache(topology, "hash-1")
    return cache

  def test_deleted_cluster_is_removed_from_all_derived_indexes(self):
    topology = {
      "1": {
        "hosts": [
          {
            "hostId": 10,
            "hostName": "agent.example",
            "rackName": "/default-rack",
            "ipv4": "192.0.2.10",
          }
        ],
        "components": [
          {
            "serviceName": "HDFS",
            "componentName": "DATANODE",
            "hostIds": [10],
            "commandParams": {"version": "3.3.6"},
          }
        ],
      }
    }

    with tempfile.TemporaryDirectory() as cache_directory:
      cache = self._new_cache(cache_directory, topology)

      self.assertEqual(10, cache.current_host_ids_to_cluster["1"])
      self.assertEqual(["DATANODE"], cache.cluster_local_components["1"])
      self.assertEqual(
        "3.3.6", cache.component_version_map["1"]["HDFS"]["DATANODE"]
      )

      cache.cache_delete({"1": {}}, "hash-2")

      self.assertNotIn("1", cache.current_host_ids_to_cluster)
      self.assertNotIn("1", cache.cluster_local_components)
      self.assertNotIn("1", cache.component_version_map)
      self.assertNotIn("1", cache.hosts_to_id)
      self.assertNotIn("1", cache.components_by_key)

  def test_cluster_host_info_is_cached_per_cluster(self):
    topology = {
      cluster_id: {
        "hosts": [
          {
            "hostId": host_id,
            "hostName": host_name,
            "rackName": "/default-rack",
            "ipv4": ip,
          }
        ],
        "components": [
          {
            "serviceName": "HDFS",
            "componentName": "DATANODE",
            "hostIds": [host_id],
            "commandParams": {},
          }
        ],
      }
      for cluster_id, host_id, host_name, ip in (
        ("1", 10, "agent.example", "192.0.2.10"),
        ("2", 20, "other.example", "192.0.2.20"),
      )
    }

    with tempfile.TemporaryDirectory() as cache_directory:
      cache = self._new_cache(cache_directory, topology)

      first = cache.get_cluster_host_info("1")
      second = cache.get_cluster_host_info("2")

      self.assertEqual(["agent.example"], first["all_hosts"])
      self.assertEqual(["other.example"], second["all_hosts"])
      self.assertIs(first, cache.get_cluster_host_info("1"))

  def test_deleting_unknown_component_preserves_cache(self):
    topology = {
      "1": {
        "hosts": [],
        "components": [
          {
            "serviceName": "HDFS",
            "componentName": "DATANODE",
            "hostIds": [],
            "commandParams": {},
          }
        ],
      }
    }

    with tempfile.TemporaryDirectory() as cache_directory:
      cache = self._new_cache(cache_directory, topology)

      cache.cache_delete(
        {
          "1": {
            "components": [
              {
                "serviceName": "YARN",
                "componentName": "NODEMANAGER",
                "hostIds": [],
              }
            ]
          }
        },
        "hash-2",
      )

      self.assertIsNotNone(
        cache.get_component_info_by_key("1", "HDFS", "DATANODE")
      )
