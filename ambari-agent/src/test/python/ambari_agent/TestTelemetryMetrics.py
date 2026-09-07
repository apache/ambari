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
import threading
import unittest
from unittest.mock import MagicMock, patch
from urllib.error import HTTPError
from urllib.request import urlopen

from ambari_agent.TelemetryConfigCache import TelemetryConfigCache
from ambari_agent.listeners.TelemetryEventListener import TelemetryEventListener
from ambari_agent.metrics.core import CollectorRegistry
from ambari_agent.metrics.server import PrometheusMetricsServer
from ambari_agent.metrics.telemetry import (
  TelemetryConfigError,
  TelemetryHttpClient,
  TelemetryResponse,
  TelemetryRouteRegistry,
  TelemetryScrapeError,
  convert_jmx_json,
  parse_object_name,
  profile_digest,
  validate_profile,
  validate_prometheus_text,
)


TEST_DIR = os.path.dirname(os.path.abspath(__file__))
REPOSITORY_ROOT = os.path.abspath(os.path.join(TEST_DIR, "../../../../.."))
TELEMETRY_FIXTURES = os.path.join(TEST_DIR, "fixtures", "telemetry")
STACK_SERVICES = os.path.join(
  REPOSITORY_ROOT,
  "ambari-server",
  "src",
  "main",
  "resources",
  "stacks",
  "BIGTOP",
  "3.2.0",
  "services",
)


def load_json(path):
  with open(path, "r", encoding="utf-8") as stream:
    return json.load(stream)


def load_fixture(name):
  with open(os.path.join(TELEMETRY_FIXTURES, name), "rb") as stream:
    return stream.read()


def load_stack_profile(service, name):
  return load_json(
    os.path.join(STACK_SERVICES, service, "telemetry-profiles", name)
  )


def hbase_profile():
  return {
    "schemaVersion": 1,
    "id": "hbase-regionserver-2.4",
    "maxSeries": 10,
    "rules": [
      {
        "bean": {
          "domain": "Hadoop",
          "properties": {
            "service": "HBase",
            "name": "RegionServer",
            "sub": "Server",
          },
        },
        "labels": {"role": {"value": "regionserver"}},
        "attributes": {
          "regionCount": {
            "name": "hbase_regionserver_regions",
            "type": "gauge",
            "unit": "regions",
            "help": "Number of online regions.",
          },
          "totalRequestCount": {
            "name": "hbase_regionserver_requests_total",
            "type": "counter",
            "unit": "requests",
            "help": "Total number of requests.",
          },
          "requestTimeMs": {
            "name": "hbase_regionserver_request_time_seconds",
            "type": "gauge",
            "unit": "seconds",
            "scale": 0.001,
            "help": "Latest request time in seconds.",
          },
        },
      }
    ],
  }


def jmx_assignment(profile_hash):
  return {
    "schemaVersion": 1,
    "targets": [
      {
        "id": "hbase-regionserver",
        "service": "HBASE",
        "component": "HBASE_REGIONSERVER",
        "format": "jmx_json",
        "url": "http://host.example.com:16030/jmx",
        "profileHash": profile_hash,
        "timeoutSeconds": 5,
        "maxResponseBytes": 4096,
      }
    ],
  }


def jmx_payload():
  return json.dumps(
    {
      "beans": [
        {
          "name": "Hadoop:service=HBase,name=RegionServer,sub=Server",
          "regionCount": 12,
          "totalRequestCount": 450,
          "requestTimeMs": 250,
          "ignoredText": "not numeric",
        }
      ]
    }
  ).encode("utf-8")


class FakeHttpClient:
  def __init__(self, responses):
    self.responses = responses
    self.requests = []

  def fetch(self, target):
    self.requests.append(target["id"])
    response = self.responses[target["id"]]
    if isinstance(response, Exception):
      raise response
    return TelemetryResponse(response)


class TestTelemetryConfigCache(unittest.TestCase):
  def test_persists_and_recovers_a_valid_assignment(self):
    profile = hbase_profile()
    digest = profile_digest(profile)

    with tempfile.TemporaryDirectory() as cache_dir:
      cache = TelemetryConfigCache(cache_dir)
      cache.update(jmx_assignment(digest), {digest: profile}, "assignment-v1")

      recovered = TelemetryConfigCache(cache_dir)
      snapshot = recovered.snapshot()

    self.assertEqual("assignment-v1", recovered.hash)
    self.assertEqual("hbase-regionserver", snapshot["assignment"]["targets"][0]["id"])
    self.assertEqual(profile, snapshot["profiles"][digest])
    self.assertTrue(recovered.last_reload_successful)

  def test_invalid_update_keeps_last_known_good_assignment(self):
    profile = hbase_profile()
    digest = profile_digest(profile)

    with tempfile.TemporaryDirectory() as cache_dir:
      cache = TelemetryConfigCache(cache_dir)
      cache.update(jmx_assignment(digest), {digest: profile}, "assignment-v1")
      before = cache.snapshot()

      invalid = jmx_assignment("sha256:" + "0" * 64)
      with self.assertRaises(TelemetryConfigError):
        cache.update(invalid, {}, "assignment-v2")

      self.assertEqual("assignment-v1", cache.hash)
      self.assertEqual(before, cache.snapshot())
      self.assertFalse(cache.last_reload_successful)

  def test_profile_digest_is_canonical(self):
    first = {"schemaVersion": 1, "id": "profile", "rules": []}
    second = {"rules": [], "id": "profile", "schemaVersion": 1}
    self.assertEqual(profile_digest(first), profile_digest(second))

  def test_concurrent_updates_keep_memory_and_disk_on_the_same_version(self):
    first_entered_persist = threading.Event()
    release_first = threading.Event()

    class CoordinatedCache(TelemetryConfigCache):
      def _persist_profiles(self, profiles):
        if not first_entered_persist.is_set():
          first_entered_persist.set()
          release_first.wait(2)
        return super()._persist_profiles(profiles)

    with tempfile.TemporaryDirectory() as cache_dir:
      cache = CoordinatedCache(cache_dir)
      first = {"schemaVersion": 1, "targets": []}
      second = {"schemaVersion": 1, "targets": []}
      failures = []

      def apply_update(assignment, cache_hash):
        try:
          cache.update(assignment, {}, cache_hash)
        except Exception as err:
          failures.append(err)

      first_worker = threading.Thread(
        target=apply_update, args=(first, "assignment-v1")
      )
      second_worker = threading.Thread(
        target=apply_update, args=(second, "assignment-v2")
      )
      first_worker.start()
      self.assertTrue(first_entered_persist.wait(2))
      second_worker.start()
      release_first.set()
      first_worker.join(2)
      second_worker.join(2)

      recovered = TelemetryConfigCache(cache_dir)

    self.assertFalse(failures)
    self.assertEqual("assignment-v2", cache.hash)
    self.assertEqual(cache.hash, recovered.hash)


class TestJmxTelemetryConversion(unittest.TestCase):
  def test_converts_typed_scaled_metrics(self):
    rendered = convert_jmx_json(jmx_payload(), hbase_profile()).decode("utf-8")

    self.assertIn(
      'hbase_regionserver_regions{role="regionserver"} 12', rendered
    )
    self.assertIn(
      'hbase_regionserver_requests_total{role="regionserver"} 450', rendered
    )
    self.assertIn(
      'hbase_regionserver_request_time_seconds{role="regionserver"} 0.25',
      rendered,
    )
    self.assertIn("# TYPE hbase_regionserver_requests_total counter", rendered)

  def test_preserves_large_unscaled_integer_counters(self):
    profile = hbase_profile()
    payload = json.dumps(
      {
        "beans": [
          {
            "name": "Hadoop:service=HBase,name=RegionServer,sub=Server",
            "totalRequestCount": 9007199254740993,
          }
        ]
      }
    ).encode("utf-8")

    rendered = convert_jmx_json(payload, profile).decode("utf-8")

    self.assertIn(
      'hbase_regionserver_requests_total{role="regionserver"} 9007199254740993',
      rendered,
    )

  def test_parses_quoted_object_name_properties(self):
    domain, properties = parse_object_name(
      'Hadoop:service=ResourceManager,name=QueueMetrics,queue="root,a"'
    )

    self.assertEqual("Hadoop", domain)
    self.assertEqual("root,a", properties["queue"])

  def test_rejects_counter_without_total_suffix(self):
    profile = hbase_profile()
    definition = profile["rules"][0]["attributes"]["totalRequestCount"]
    definition["name"] = "hbase_regionserver_requests"

    with self.assertRaises(TelemetryConfigError):
      validate_profile(profile)

  def test_no_matching_metrics_marks_scrape_failed(self):
    payload = b'{"beans": [{"name": "java.lang:type=Runtime"}]}'

    with self.assertRaises(TelemetryScrapeError):
      convert_jmx_json(payload, hbase_profile())

  def test_converts_live_nodemanager_fixture_with_stack_profile(self):
    profile = load_stack_profile("YARN", "nodemanager-3.3.json")
    rendered = convert_jmx_json(
      load_fixture("nodemanager-jmx.json"), profile
    ).decode("utf-8")

    self.assertIn("yarn_nodemanager_available_memory_bytes 1073741824", rendered)
    self.assertIn("yarn_nodemanager_local_directory_utilization_ratio 0.1", rendered)

  def test_converts_live_hbase_master_fixture_with_stack_profile(self):
    profile = load_stack_profile("HBASE", "hbase-master-2.4.json")
    rendered = convert_jmx_json(
      load_fixture("hbase-master-jmx.json"), profile
    ).decode("utf-8")

    self.assertIn("hbase_master_active 1", rendered)
    self.assertIn("hbase_master_regionservers 1", rendered)
    self.assertIn("hbase_master_rpc_process_call_duration_p99_seconds 0.001", rendered)
    self.assertIn("hbase_master_jvm_gc_time_seconds_total 0.816", rendered)

  def test_converts_live_hbase_regionserver_fixture_with_stack_profile(self):
    profile = load_stack_profile("HBASE", "hbase-regionserver-2.4.json")
    rendered = convert_jmx_json(
      load_fixture("hbase-regionserver-jmx.json"), profile
    ).decode("utf-8")

    self.assertIn("hbase_regionserver_store_file_locality_ratio 1", rendered)
    self.assertIn("# TYPE hbase_regionserver_read_requests_per_second gauge", rendered)
    self.assertIn("hbase_regionserver_read_requests_per_second 0", rendered)
    self.assertIn("# TYPE hbase_regionserver_write_requests_per_second gauge", rendered)
    self.assertIn("hbase_regionserver_write_requests_per_second 0", rendered)
    self.assertIn("hbase_regionserver_scan_duration_p99_seconds 0.001", rendered)
    self.assertIn("hbase_regionserver_rpc_received_bytes_total 261174", rendered)
    self.assertIn("hbase_regionserver_jvm_heap_max_bytes 536870912", rendered)

  def test_converts_live_hiveserver2_fixture_with_stack_profile(self):
    profile = load_stack_profile("HIVE", "hiveserver2-3.1.json")
    rendered = convert_jmx_json(
      load_fixture("hiveserver2-jmx.json"), profile
    ).decode("utf-8")

    self.assertIn("hive_server2_connections_total 28", rendered)
    self.assertIn("hive_server2_async_pool_size 1", rendered)
    self.assertIn("hive_server2_open_sessions 0", rendered)
    self.assertIn("hive_server2_jvm_heap_used_bytes 203948312", rendered)
    self.assertIn("hive_server2_query_cache_max_bytes 2147483648", rendered)


class TestTelemetryHttpClient(unittest.TestCase):
  class Response:
    def __init__(self, body):
      self.body = body
      self.headers = {"Content-Type": "text/plain"}

    def __enter__(self):
      return self

    def __exit__(self, exception_type, exception, traceback):
      return False

    def read(self, limit):
      return self.body[:limit]

  class Opener:
    def __init__(self, body):
      self.body = body

    def open(self, request, timeout):
      return TestTelemetryHttpClient.Response(self.body)

  def test_rejects_response_larger_than_target_limit(self):
    target = {
      "url": "http://host.example.com/prom",
      "maxResponseBytes": 1024,
      "timeoutSeconds": 5,
    }
    client = TelemetryHttpClient(opener=self.Opener(b"x" * 1025))

    with self.assertRaises(TelemetryScrapeError):
      client.fetch(target)

  def test_rejects_malformed_native_prometheus_text(self):
    with self.assertRaises(TelemetryScrapeError):
      validate_prometheus_text(b"this is not an exposition response\n")

  def test_accepts_prometheus_text_with_labels_and_timestamp(self):
    validate_prometheus_text(
      b'# TYPE hadoop_requests_total counter\n'
      b'hadoop_requests_total{method="get block"} 3.5e2 1700000000\n'
    )

class TestTelemetryRoutes(unittest.TestCase):
  def _cache(self, cache_dir):
    profile = hbase_profile()
    digest = profile_digest(profile)
    cache = TelemetryConfigCache(cache_dir)
    assignment = jmx_assignment(digest)
    assignment["targets"].append(
      {
        "id": "namenode",
        "format": "prometheus_text",
        "url": "http://host.example.com:9870/prom",
      }
    )
    cache.update(assignment, {digest: profile}, "assignment-v1")
    return cache

  def test_routes_native_and_jmx_targets_independently(self):
    with tempfile.TemporaryDirectory() as cache_dir:
      cache = self._cache(cache_dir)
      client = FakeHttpClient(
        {
          "namenode": b"# TYPE namenode_capacity gauge\nnamenode_capacity 10\n",
          "hbase-regionserver": jmx_payload(),
        }
      )
      registry = TelemetryRouteRegistry(cache, http_client=client)

      native = registry.scrape("namenode").body
      converted = registry.scrape("hbase-regionserver").body

    self.assertIn(b"namenode_capacity 10", native)
    self.assertIn(b"hbase_regionserver_regions", converted)
    self.assertEqual(["namenode", "hbase-regionserver"], client.requests)

  def test_malformed_native_response_fails_only_its_route(self):
    with tempfile.TemporaryDirectory() as cache_dir:
      cache = self._cache(cache_dir)
      registry = TelemetryRouteRegistry(
        cache,
        http_client=FakeHttpClient(
          {
            "namenode": b"upstream login page",
            "hbase-regionserver": jmx_payload(),
          }
        ),
      )

      with self.assertRaises(TelemetryScrapeError):
        registry.scrape("namenode")
      converted = registry.scrape("hbase-regionserver").body

    self.assertIn(b"hbase_regionserver_regions", converted)

  def test_rejects_a_concurrent_scrape_above_the_route_limit(self):
    entered = threading.Event()
    release = threading.Event()

    class BlockingClient:
      def fetch(self, target):
        entered.set()
        release.wait(2)
        return TelemetryResponse(b"namenode_capacity 10\n")

    with tempfile.TemporaryDirectory() as cache_dir:
      cache = self._cache(cache_dir)
      assignment = cache.snapshot()["assignment"]
      assignment["targets"][1]["maxConcurrentRequests"] = 1
      cache.update(assignment, {}, "assignment-v2")
      registry = TelemetryRouteRegistry(cache, http_client=BlockingClient())
      first_error = []

      def scrape_first():
        try:
          registry.scrape("namenode")
        except Exception as err:
          first_error.append(err)

      worker = threading.Thread(target=scrape_first)
      worker.start()
      self.assertTrue(entered.wait(2))
      try:
        with self.assertRaises(TelemetryScrapeError) as context:
          registry.scrape("namenode")
        self.assertEqual(503, context.exception.status)
      finally:
        release.set()
        worker.join(2)

    self.assertFalse(first_error)

  def test_component_http_path_returns_upstream_failure(self):
    with tempfile.TemporaryDirectory() as cache_dir:
      cache = self._cache(cache_dir)
      routes = TelemetryRouteRegistry(
        cache,
        http_client=FakeHttpClient(
          {
            "namenode": TelemetryScrapeError("upstream unavailable"),
            "hbase-regionserver": jmx_payload(),
          }
        ),
      )

      class Config:
        prometheus_metrics_enabled = True
        prometheus_metrics_bind_address = "127.0.0.1"
        prometheus_metrics_port = 0

      class Initializer:
        config = Config()
        stop_event = threading.Event()

      server = PrometheusMetricsServer(
        Initializer(),
        registry=CollectorRegistry(),
        telemetry_routes=routes,
      )
      server.start()
      self.assertTrue(server.ready_event.wait(2))
      port = server.server_address[1]
      try:
        with self.assertRaises(HTTPError) as context:
          urlopen(
            "http://127.0.0.1:{}/metrics/components/namenode".format(port),
            timeout=2,
          )
        self.assertEqual(502, context.exception.code)

        with urlopen(
          "http://127.0.0.1:{}/metrics/components/hbase-regionserver".format(
            port
          ),
          timeout=2,
        ) as response:
          self.assertIn(b"hbase_regionserver_regions", response.read())
      finally:
        server.stop()
        server.join(2)


class TestTelemetryDelivery(unittest.TestCase):
  def test_listener_applies_complete_updates_and_ignores_empty_updates(self):
    cache = MagicMock()
    initializer = MagicMock()
    initializer.telemetry_cache = cache
    listener = TelemetryEventListener(initializer)
    message = {
      "hash": "assignment-v1",
      "assignment": {"schemaVersion": 1, "targets": []},
      "profiles": {},
    }

    listener.on_event({}, message)
    listener.on_event({}, {})

    cache.update.assert_called_once_with(
      message["assignment"], message["profiles"], message["hash"]
    )

if __name__ == "__main__":
  unittest.main()
