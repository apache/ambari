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

import ssl
import threading
import unittest
from unittest.mock import MagicMock, patch

from ambari_agent import NetUtil
from ambari_commons import inet_utils
from ambari_commons.exceptions import TimeoutError as AmbariTimeoutError


class TestNetUtil(unittest.TestCase):
  def setUp(self):
    self.config = MagicMock()
    self.config.get.side_effect = lambda section, option, default=None: default
    self.ssl_context = MagicMock()
    self.config.get_server_ssl_context.return_value = self.ssl_context
    self.stop_event = threading.Event()

  @patch("ambari_agent.NetUtil.http.client.HTTPSConnection")
  def test_check_url_uses_configured_tls_context_and_full_request_path(
    self, https_connection_mock
  ):
    connection = https_connection_mock.return_value
    response = connection.getresponse.return_value
    response.status = 200
    response.read.return_value = b"server-ca"

    with patch.object(NetUtil.logger, "debug") as logger_mock:
      result = NetUtil.NetUtil(self.config, self.stop_event).checkURL(
        "https://server.example:8441/ca?cluster=one"
      )

    self.assertEqual((True, b"server-ca"), result)
    https_connection_mock.assert_called_once_with(
      "server.example", 8441, context=self.ssl_context, timeout=10.0
    )
    connection.request.assert_called_once_with("GET", "/ca?cluster=one")
    connection.close.assert_called_once_with()
    logger_mock.assert_called_once_with(
      NetUtil.LOG_REQUEST_MESSAGE,
      "https://server.example:8441/ca?cluster=one",
      "200",
      len(b"server-ca"),
    )
    self.assertNotIn(b"server-ca", logger_mock.call_args.args)

  @patch("ambari_agent.NetUtil.http.client.HTTPSConnection")
  def test_check_url_rejects_non_success_and_closes_connection(
    self, https_connection_mock
  ):
    connection = https_connection_mock.return_value
    connection.getresponse.return_value.status = 503

    result = NetUtil.NetUtil(self.config, self.stop_event).checkURL(
      "https://server.example:8441"
    )

    self.assertEqual((False, ""), result)
    connection.request.assert_called_once_with("GET", "/")
    connection.close.assert_called_once_with()

  @patch("ambari_agent.NetUtil.http.client.HTTPSConnection")
  def test_check_url_reports_tls_failure(self, https_connection_mock):
    https_connection_mock.side_effect = ssl.SSLError("certificate verify failed")

    result = NetUtil.NetUtil(self.config, self.stop_event).checkURL(
      "https://wrong-ca.example:8441/ca"
    )

    self.assertEqual((False, ""), result)

  def test_try_to_connect(self):
    netutil = NetUtil.NetUtil(self.config, self.stop_event)
    netutil.connect_retry_delay = 0
    netutil.checkURL = MagicMock(return_value=(True, "test"))

    self.assertEqual((0, True, False), netutil.try_to_connect("url", 10))

    netutil.checkURL.side_effect = [(False, ""), (False, ""), (True, "")]
    self.assertEqual((2, True, False), netutil.try_to_connect("url", 10))

    netutil.checkURL.side_effect = None
    netutil.checkURL.return_value = (False, "test")
    self.assertEqual((5, False, False), netutil.try_to_connect("url", 5))

  def test_try_to_connect_honors_stop_event(self):
    netutil = NetUtil.NetUtil(self.config, self.stop_event)
    netutil.checkURL = MagicMock(return_value=(False, ""))
    self.stop_event.set()

    self.assertEqual((1, False, False), netutil.try_to_connect("url", -1))

  def test_get_agent_heartbeat_idle_interval_sec(self):
    netutil = NetUtil.NetUtil(self.config, self.stop_event)

    heartbeat_interval = netutil.get_agent_heartbeat_idle_interval_sec(1, 10, 32)

    self.assertEqual(heartbeat_interval, 3)

  def test_get_agent_heartbeat_idle_interval_sec_max(self):
    netutil = NetUtil.NetUtil(self.config, self.stop_event)

    heartbeat_interval = netutil.get_agent_heartbeat_idle_interval_sec(1, 10, 1500)

    self.assertEqual(
      heartbeat_interval, netutil.HEARTBEAT_IDLE_INTERVAL_DEFAULT_MAX_SEC
    )

  def test_get_agent_heartbeat_idle_interval_sec_min(self):
    netutil = NetUtil.NetUtil(self.config, self.stop_event)

    heartbeat_interval = netutil.get_agent_heartbeat_idle_interval_sec(1, 10, 5)

    self.assertEqual(heartbeat_interval, 1)

  def test_ambari_timeout_error_formats_python3_exception_arguments(self):
    self.assertEqual(
      "'Timeout error: connection timed out'",
      str(AmbariTimeoutError("connection timed out")),
    )

  @patch.object(inet_utils, "os_run_os_command", return_value=(0, "", ""))
  @patch.object(inet_utils, "force_download_file", side_effect=OSError("failed"))
  @patch.object(inet_utils.os.path, "exists", side_effect=[False, False, True])
  def test_download_fallback_keeps_tls_verification_enabled(
    self, _exists_mock, _force_download_mock, run_command_mock
  ):
    inet_utils.download_file_anyway(
      "https://server.example/management-pack.tar.gz", "/tmp/management-pack.tar.gz"
    )

    command = run_command_mock.call_args.args[0]
    self.assertIn("curl --fail -o", command)
    self.assertNotIn(" -k ", f" {command} ")
