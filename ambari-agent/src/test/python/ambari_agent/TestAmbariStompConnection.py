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
from unittest import TestCase
from unittest.mock import MagicMock, patch

from stomp.adapter.ws import WSStompConnection
from stomp.exception import ConnectFailedException, NotConnectedException

from ambari_agent.AmbariStompConnection import (
  AmbariStompConnection,
  ConnectionIsAlreadyClosed,
  ConnectionResponseTimeout,
  DEFAULT_CONNECTION_TIMEOUT,
)


class TestAmbariStompConnection(TestCase):
  def test_maps_wss_url_and_client_certificate_options(self):
    transport = MagicMock()
    transport.get_ssl.return_value = {}

    def initialize(connection, *args, **kwargs):
      connection.transport = transport

    with patch.object(
      WSStompConnection, "__init__", autospec=True, side_effect=initialize
    ) as init_mock:
      AmbariStompConnection(
        "wss://server.example:8441/agent/stomp/v1?tenant=cluster-a",
        ssl_options={
          "keyfile": "/keys/agent.key",
          "certfile": "/keys/agent.crt",
          "ca_certs": "/keys/ca.crt",
          "cert_reqs": ssl.CERT_REQUIRED,
        },
      )

    self.assertEqual(init_mock.call_args.kwargs["host_and_ports"], [("server.example", 8441)])
    self.assertEqual(
      init_mock.call_args.kwargs["ws_path"],
      "/agent/stomp/v1?tenant=cluster-a",
    )
    self.assertEqual(init_mock.call_args.kwargs["reconnect_attempts_max"], 1)
    transport.set_ssl.assert_called_once_with(
      for_hosts=[("server.example", 8441)],
      key_file="/keys/agent.key",
      cert_file="/keys/agent.crt",
      ca_certs="/keys/ca.crt",
      ssl_version=ssl.PROTOCOL_TLS_CLIENT,
      password=None,
    )
    self.assertEqual(transport.get_ssl.return_value["keyfile"], "/keys/agent.key")
    self.assertEqual(transport.get_ssl.return_value["cert_reqs"], ssl.CERT_REQUIRED)

  def test_one_way_tls_preserves_current_no_verify_policy(self):
    transport = MagicMock()
    transport.get_ssl.return_value = {}

    def initialize(connection, *args, **kwargs):
      connection.transport = transport

    with patch.object(
      WSStompConnection, "__init__", autospec=True, side_effect=initialize
    ):
      AmbariStompConnection("wss://server.example/agent/stomp/v1")

    self.assertEqual(transport.get_ssl.return_value["cert_reqs"], ssl.CERT_NONE)

  def test_connect_waits_for_protocol_confirmation_without_restarting_transport(self):
    connection = object.__new__(AmbariStompConnection)
    connection._connect_event = MagicMock()
    connection._connect_event.wait.return_value = True
    connection._connect_failed = False
    connection.transport = MagicMock()
    connection.transport.is_connected.return_value = True

    with patch.object(WSStompConnection, "connect") as connect_mock:
      connection.connect(wait=True)

    connect_mock.assert_called_once_with(
      username=None,
      passcode=None,
      wait=False,
      headers=None,
    )
    connection._connect_event.clear.assert_called_once_with()
    connection._connect_event.wait.assert_called_once_with(DEFAULT_CONNECTION_TIMEOUT)
    connection.transport.start.assert_not_called()

  def test_connect_raises_when_protocol_confirmation_times_out(self):
    connection = object.__new__(AmbariStompConnection)
    connection._connect_event = MagicMock()
    connection._connect_event.wait.return_value = False
    connection._connect_failed = False
    connection.transport = MagicMock()

    with patch.object(WSStompConnection, "connect"):
      with self.assertRaises(ConnectionResponseTimeout):
        connection.connect(wait=True)

  def test_connect_raises_when_protocol_connection_fails(self):
    connection = object.__new__(AmbariStompConnection)
    connection._connect_event = MagicMock()
    connection._connect_event.wait.return_value = True
    connection._connect_failed = False
    connection.transport = MagicMock()
    connection.transport.is_connected.return_value = False

    with patch.object(WSStompConnection, "connect"):
      with self.assertRaises(ConnectFailedException):
        connection.connect(wait=True)

  def test_connect_raises_when_server_sends_error_frame(self):
    connection = object.__new__(AmbariStompConnection)
    connection._connect_event = threading.Event()
    connection._connect_failed = False
    connection.transport = MagicMock()
    connection.transport.is_connected.return_value = True

    def report_error(*args, **kwargs):
      connection.on_error(MagicMock())

    with patch.object(WSStompConnection, "connect", side_effect=report_error):
      with self.assertRaises(ConnectFailedException):
        connection.connect(wait=True)

  def test_connected_callback_clears_previous_failure(self):
    connection = object.__new__(AmbariStompConnection)
    connection._connect_event = threading.Event()
    connection._connect_failed = True

    connection.on_connected(MagicMock())

    self.assertFalse(connection._connect_failed)
    self.assertTrue(connection._connect_event.is_set())

  def test_rejects_non_wss_url(self):
    with self.assertRaisesRegex(ValueError, "Invalid Ambari STOMP URL"):
      AmbariStompConnection("http://server.example/agent/stomp/v1")

  def test_send_serializes_message_and_assigns_correlation_id(self):
    connection = object.__new__(AmbariStompConnection)
    connection.lock = threading.RLock()
    connection.correlation_id = -1
    presend_hook = MagicMock()

    with patch.object(WSStompConnection, "send") as send_mock:
      correlation_id = connection.send(
        destination="/agent/heartbeat",
        message={"id": 1},
        headers={"custom": "value"},
        presend_hook=presend_hook,
      )

    self.assertEqual(correlation_id, 0)
    presend_hook.assert_called_once_with(0)
    send_mock.assert_called_once_with(
      destination="/agent/heartbeat",
      body='{"id": 1}',
      content_type=None,
      headers={"custom": "value"},
      correlationId=0,
    )

  def test_send_normalizes_closed_connection_exception(self):
    connection = object.__new__(AmbariStompConnection)
    connection.lock = threading.RLock()
    connection.correlation_id = -1

    with patch.object(
      WSStompConnection, "send", side_effect=NotConnectedException()
    ):
      with self.assertRaises(ConnectionIsAlreadyClosed):
        connection.send(destination="/agent/heartbeat", message={"id": 1})

  def test_disconnect_always_closes_socket_and_stops_transport(self):
    connection = object.__new__(AmbariStompConnection)
    connection.transport = MagicMock()

    with patch.object(WSStompConnection, "disconnect") as disconnect_mock:
      connection.disconnect()

    disconnect_mock.assert_called_once_with()
    connection.transport.disconnect_socket.assert_called_once_with()
    connection.transport.stop.assert_called_once_with()

  def test_disconnect_is_idempotent_when_each_close_step_fails(self):
    connection = object.__new__(AmbariStompConnection)
    connection.transport = MagicMock()
    connection.transport.disconnect_socket.side_effect = RuntimeError("already closed")
    connection.transport.stop.side_effect = RuntimeError("already stopped")

    with patch.object(
      WSStompConnection, "disconnect", side_effect=NotConnectedException()
    ):
      connection.disconnect()

    connection.transport.disconnect_socket.assert_called_once_with()
    connection.transport.stop.assert_called_once_with()

  def test_add_listener_uses_class_name_for_backward_compatibility(self):
    class Listener:
      pass

    connection = object.__new__(AmbariStompConnection)
    connection.set_listener = MagicMock()
    listener = Listener()

    connection.add_listener(listener)

    connection.set_listener.assert_called_once_with("Listener", listener)
