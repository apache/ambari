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
import sys
from types import ModuleType, SimpleNamespace
import unittest
from unittest.mock import MagicMock, patch

from resource_management.core.exceptions import Fail


SERVICE = Path(__file__).resolve().parents[2] / (
  "main/resources/common-services/AMBARI_METRICS/3.0.0"
)
SCRIPTS = SERVICE / "package/scripts"


def dependency_module(name, **attributes):
  module = ModuleType(name)
  for attribute, value in attributes.items():
    setattr(module, attribute, value)
  return module


def load_module(module_name, path, dependencies=None):
  spec = importlib.util.spec_from_file_location(module_name, path)
  module = importlib.util.module_from_spec(spec)
  with patch.dict(sys.modules, dependencies or {}):
    spec.loader.exec_module(module)
  return module


METRICS_UTILS = load_module(
  "ambari_metrics_integration_utils", SCRIPTS / "metrics_utils.py"
)
SERVICE_CHECK_STUB = dependency_module(
  "service_check", post_metrics_to_collector=MagicMock()
)
GRAFANA_UTIL = load_module(
  "ambari_metrics_grafana_util",
  SCRIPTS / "metrics_grafana_util.py",
  {"metrics_utils": METRICS_UTILS, "service_check": SERVICE_CHECK_STUB},
)
AMS_SERVICE_CHECK = load_module(
  "ambari_metrics_service_check",
  SCRIPTS / "service_check.py",
  {"metrics_utils": METRICS_UTILS},
)


class TestAmbariMetricsHttpContract(unittest.TestCase):
  def setUp(self):
    self.params = SimpleNamespace(
      ams_grafana_ca_cert=None,
      grafana_connect_attempts=1,
      grafana_connect_retry_delay=1,
      grafana_request_timeout=7,
    )
    self.server = GRAFANA_UTIL.Server("http", "grafana.example", 3000, "admin", "secret")

  def test_grafana_response_is_buffered_and_connection_is_closed(self):
    response = SimpleNamespace(status=200, reason="OK", read=MagicMock(return_value=b"{}"))
    connection = MagicMock()
    connection.getresponse.return_value = response
    with patch.dict(sys.modules, {"params": self.params}), \
      patch.object(
        GRAFANA_UTIL.network, "get_http_connection", return_value=connection
      ):
      buffered = GRAFANA_UTIL.perform_grafana_get_call("/api/user", self.server)
    self.assertEqual(200, buffered.status)
    self.assertEqual(b"{}", buffered.read())
    self.assertEqual(7, connection.timeout)
    connection.close.assert_called_once_with()

  def test_metrics_response_rejects_invalid_json_shapes(self):
    for payload in (b"not-json", b"{}", b'{"metrics": {}}', b'{"metrics": [{}]}'):
      with self.subTest(payload=payload):
        with self.assertRaises(Fail):
          AMS_SERVICE_CHECK.metrics_response_contains_values(payload, 1000, 1.5)

  def test_metrics_response_matches_both_smoke_test_samples(self):
    payload = b'{"metrics": [{"metrics": {"1000": 1.5, "2000": 1000}}]}'
    self.assertTrue(
      AMS_SERVICE_CHECK.metrics_response_contains_values(payload, 1000, 1.5)
    )

  def test_grafana_connection_is_closed_on_failure(self):
    connection = MagicMock()
    connection.request.side_effect = OSError("unreachable")
    with patch.dict(sys.modules, {"params": self.params}), \
      patch.object(
        GRAFANA_UTIL.network, "get_http_connection", return_value=connection
      ):
      with self.assertRaises(Fail):
        GRAFANA_UTIL.perform_grafana_get_call("/api/user", self.server)
    connection.close.assert_called_once_with()

  def test_grafana_does_not_try_default_password_after_server_failure(self):
    params = SimpleNamespace(
      ams_grafana_protocol="http",
      ams_grafana_host="grafana.example",
      ams_grafana_port=3000,
      ams_grafana_admin_user="admin",
      ams_grafana_admin_pwd="secret",
    )
    response = GRAFANA_UTIL.GrafanaResponse(503, "Unavailable", b"")
    with patch.dict(sys.modules, {"params": params}), \
      patch.object(
        GRAFANA_UTIL, "perform_grafana_get_call", return_value=response
      ), \
      patch.object(GRAFANA_UTIL, "perform_grafana_put_call") as update_password:
      with self.assertRaises(Fail):
        GRAFANA_UTIL.create_grafana_admin_pwd()
    update_password.assert_not_called()

if __name__ == "__main__":
  unittest.main()
