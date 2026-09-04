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

import importlib
import unittest
from unittest.mock import MagicMock, patch

from ambari_agent import HeartbeatThread
from ambari_agent.metrics.telemetry import KerberosTelemetryFetcher


class TestTelemetryHeartbeat(unittest.TestCase):
  def test_kerberos_fetcher_disables_redirects_and_verifies_tls(self):
    curl_module = importlib.import_module(
      "resource_management.libraries.functions.curl_krb_request"
    )
    config = MagicMock()
    config.get.side_effect = lambda section, key, default=None: {
      ("agent", "tmp_dir"): "/tmp",
      ("agent", "run_as_user"): "ambari-agent",
    }.get((section, key), default)
    target = {
      "id": "namenode",
      "url": "https://host.example.com:9871/prom",
      "timeoutSeconds": 5,
      "maxResponseBytes": 4096,
      "auth": {
        "type": "kerberos",
        "principal": "HTTP/host.example.com@EXAMPLE.COM",
        "keytab": "/etc/security/keytabs/spnego.service.keytab",
      },
    }

    with patch.object(curl_module, "curl_krb_request") as request:
      request.return_value = ("metric_total 1\n", None, 0.1)
      response = KerberosTelemetryFetcher(config)(target)

      self.assertEqual(b"metric_total 1\n", response.body)
      request_options = request.call_args[1]
      self.assertFalse(request_options["follow_redirects"])
      self.assertTrue(request_options["fail_on_http_error"])
      self.assertTrue(request_options["verify_ssl"])
      self.assertEqual(4096, request_options["max_response_bytes"])
      self.assertIsNone(request_options["ca_certs"])

  def test_registration_capability_gates_telemetry_sync(self):
    heartbeat = object.__new__(HeartbeatThread.HeartbeatThread)

    heartbeat.handle_registration_response(
      {"id": 1, "serverCapabilities": ["telemetry-v1"]}
    )
    self.assertTrue(heartbeat.telemetry_supported)

    heartbeat.handle_registration_response({"id": 2})
    self.assertFalse(heartbeat.telemetry_supported)

  @patch("ambari_agent.HeartbeatThread.time.monotonic", return_value=600)
  def test_reconciliation_failure_keeps_cached_assignment(self, monotonic):
    heartbeat = object.__new__(HeartbeatThread.HeartbeatThread)
    heartbeat.telemetry_supported = True
    heartbeat.last_telemetry_reconciliation = 0
    heartbeat.initializer_module = MagicMock()
    heartbeat.initializer_module.telemetry_cache.hash = "assignment-v1"
    heartbeat.telemetry_events_listener = MagicMock()
    heartbeat.blocking_request = MagicMock(side_effect=RuntimeError("offline"))

    with self.assertLogs("ambari_agent.HeartbeatThread", level="ERROR"):
      heartbeat.reconcile_telemetry_if_due()

    heartbeat.telemetry_events_listener.on_event.assert_not_called()
    self.assertEqual("assignment-v1", heartbeat.initializer_module.telemetry_cache.hash)
    self.assertEqual(600, heartbeat.last_telemetry_reconciliation)


if __name__ == "__main__":
  unittest.main()
