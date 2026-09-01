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

from unittest import TestCase
from alerts.ams_alert import AmsAlert
from unittest.mock import Mock, MagicMock, patch
from ambari_agent.AmbariConfig import AmbariConfig


class TestAmsAlert(TestCase):
  def setUp(self):
    self.config = AmbariConfig()
    self.ssl_context = MagicMock()
    self.config.get_server_ssl_context = MagicMock(return_value=self.ssl_context)
    self.configuration_builder = MagicMock()
    self.configuration_builder.get_configuration.return_value = {
      "configurations": {"cluster-env": {"security_enabled": "false"}}
    }

  @patch("alerts.metric_alert.build_url_opener")
  def test_collect_ok_low_values(self, build_url_opener):
    alert_meta = {
      "definitionId": 1,
      "name": "alert1",
      "label": "label1",
      "serviceName": "service1",
      "componentName": "component1",
      "uuid": "123",
      "enabled": "true",
    }
    alert_source_meta = {
      "ams": {
        "metric_list": ["metric1"],
        "app_id": "APP_ID",
        "interval": 60,
        "minimum_value": -1,
        "compute": "mean",
        "value": "{0}",
      },
      "uri": {
        "http": "192.168.0.10:8080",
        "https_property": "{{ams-site/timeline.metrics.service.http.policy}}",
        "https_property_value": "HTTPS_ONLY",
      },
      "reporting": {
        "ok": {"text": "OK: {0}"},
        "warning": {"text": "Warn: {0}", "value": 3},
        "critical": {"text": "Crit: {0}", "value": 5},
      },
    }
    cluster = "c1"
    cluster_id = "0"
    host = "host1"
    expected_text = "OK: 2.0"

    def collector_side_effect(clus, data):
      self.assertEqual(data["name"], alert_meta["name"])
      self.assertEqual(data["text"], expected_text)
      self.assertEqual(data["clusterId"], cluster_id)
      self.assertEqual(data["definitionId"], alert_meta["definitionId"])
      self.assertEqual(clus, cluster)

    response = MagicMock()
    response.getcode.return_value = 200
    build_url_opener.return_value.open.return_value = response
    response.read.return_value = '{"metrics":[{"metricname":"metric1","metrics":{"1459966360838":1,"1459966370838":3}}]}'

    mock_collector = MagicMock()
    mock_collector.put = Mock(side_effect=collector_side_effect)

    alert = AmsAlert(alert_meta, alert_source_meta, self.config)
    alert.set_helpers(mock_collector, MagicMock(), self.configuration_builder)
    alert.set_cluster(cluster, cluster_id, host)

    alert.collect()
    response.close.assert_called_once_with()

  @patch("alerts.metric_alert.build_url_opener")
  def test_collect_warn(self, build_url_opener):
    alert_meta = {
      "definitionId": 1,
      "name": "alert1",
      "label": "label1",
      "serviceName": "service1",
      "componentName": "component1",
      "uuid": "123",
      "enabled": "true",
    }
    alert_source_meta = {
      "ams": {
        "metric_list": ["metric1"],
        "app_id": "APP_ID",
        "interval": 60,
        "minimum_value": -1,
        "compute": "mean",
        "value": "{0}",
      },
      "uri": {
        "http": "192.168.0.10:8080",
        "https_property": "{{ams-site/timeline.metrics.service.http.policy}}",
        "https_property_value": "HTTPS_ONLY",
      },
      "reporting": {
        "ok": {"text": "OK: {0}"},
        "warning": {"text": "Warn: {0}", "value": 3},
        "critical": {"text": "Crit: {0}", "value": 5},
      },
    }
    cluster = "c1"
    host = "host1"
    cluster_id = "0"
    expected_text = "Warn: 4.0"

    def collector_side_effect(clus, data):
      self.assertEqual(data["name"], alert_meta["name"])
      self.assertEqual(data["text"], expected_text)
      self.assertEqual(data["clusterId"], cluster_id)
      self.assertEqual(clus, cluster)

    response = MagicMock()
    response.getcode.return_value = 200
    build_url_opener.return_value.open.return_value = response
    response.read.return_value = '{"metrics":[{"metricname":"metric1","metrics":{"1459966360838":3,"1459966370838":5}}]}'

    mock_collector = MagicMock()
    mock_collector.put = Mock(side_effect=collector_side_effect)

    alert = AmsAlert(alert_meta, alert_source_meta, self.config)
    alert.set_helpers(
      mock_collector, MagicMock(), MagicMock()
    )  # {'foo-site/bar': 12, 'foo-site/baz': 'asd'})
    alert.set_cluster(cluster, cluster_id, host)

    alert.collect()

  @patch("alerts.metric_alert.build_url_opener")
  def test_collect_ok_with_cluster_id(self, build_url_opener):
    alert_meta = {
      "definitionId": 1,
      "name": "alert1",
      "label": "label1",
      "serviceName": "service1",
      "componentName": "component1",
      "uuid": "123",
      "enabled": "true",
    }
    alert_source_meta = {
      "ams": {
        "metric_list": ["metric1"],
        "app_id": "APP_ID",
        "interval": 60,
        "minimum_value": -1,
        "compute": "mean",
        "value": "{0}",
      },
      "uri": {
        "http": "192.168.0.10:8080",
        "https_property": "{{ams-site/timeline.metrics.service.http.policy}}",
        "https_property_value": "HTTPS_ONLY",
      },
      "reporting": {
        "ok": {"text": "OK: {0}"},
        "warning": {"text": "Warn: {0}", "value": 3},
        "critical": {"text": "Crit: {0}", "value": 5},
      },
    }
    cluster = "c1"
    host = "host1"
    cluster_id = "0"
    expected_text = "Crit: 10.0"

    def collector_side_effect(clus, data):
      self.assertEqual(data["name"], alert_meta["name"])
      self.assertEqual(data["text"], expected_text)
      self.assertEqual(data["clusterId"], cluster_id)
      self.assertEqual(clus, cluster)

    response = MagicMock()
    response.getcode.return_value = 200
    build_url_opener.return_value.open.return_value = response
    response.read.return_value = '{"metrics":[{"metricname":"metric1","metrics":{"1459966360838":10,"1459966370838":10}}]}'

    mock_collector = MagicMock()
    mock_collector.put = Mock(side_effect=collector_side_effect)

    alert = AmsAlert(alert_meta, alert_source_meta, self.config)
    alert.set_helpers(
      mock_collector, MagicMock(), MagicMock()
    )  # {'foo-site/bar': 12, 'foo-site/baz': 'asd'})
    alert.set_cluster(cluster, cluster_id, host)

    alert.collect()

  @patch("alerts.metric_alert.build_url_opener")
  def test_load_metric_uses_https_and_request_scoped_proxy_policy(
    self, build_url_opener
  ):
    self.config.set("network", "use_system_proxy_settings", "false")
    response = MagicMock()
    response.getcode.return_value = 200
    response.read.return_value = b'{"metrics":[]}'
    build_url_opener.return_value.open.return_value = response
    alert = AmsAlert(
      {"name": "alert1"},
      {
        "ams": {
          "metric_list": ["metric1"],
          "app_id": "APP_ID",
          "interval": 1,
          "minimum_value": -1,
        }
      },
      self.config,
    )
    alert.set_cluster("c1", "0", "host1")

    metrics, status = alert._load_metric(True, "ams.example", 6188, alert.metric_info)

    self.assertEqual([], metrics)
    self.assertEqual(200, status)
    build_url_opener.assert_called_once()
    self.assertTrue(build_url_opener.call_args.args[0])
    self.assertIs(self.ssl_context, build_url_opener.call_args.args[1])
    request_url = build_url_opener.return_value.open.call_args.args[0]
    self.assertTrue(request_url.startswith("https://ams.example:6188/"))
    response.close.assert_called_once_with()

  @patch("alerts.metric_alert.build_url_opener")
  def test_load_metric_returns_unknown_on_network_failure(self, build_url_opener):
    build_url_opener.return_value.open.side_effect = OSError("connection failed")
    alert = AmsAlert(
      {"name": "alert1"},
      {
        "ams": {
          "metric_list": ["metric1"],
          "app_id": "APP_ID",
          "interval": 1,
          "minimum_value": -1,
        }
      },
      self.config,
    )
    alert.set_cluster("c1", "0", "host1")

    metrics, status = alert._load_metric(False, "ams.example", 6188, alert.metric_info)

    self.assertIsNone(metrics)
    self.assertIsNone(status)
