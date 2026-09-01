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

import copy
import json
import logging
import ssl
import threading
from urllib.parse import urlsplit

from stomp.adapter.ws import WSStompConnection
from stomp.exception import (
  ConnectFailedException,
  ConnectionClosedException,
  NotConnectedException,
  StompException,
)
from websocket import WebSocketConnectionClosedException

from ambari_commons.inet_utils import create_ssl_context

logger = logging.getLogger(__name__)

DEFAULT_CONNECTION_TIMEOUT = 10


class ConnectionResponseTimeout(StompException):
  """Raised when the server does not confirm the STOMP connection in time."""


class ConnectionIsAlreadyClosed(ConnectionClosedException):
  """Raised when an Agent operation targets a closed control connection."""


class AmbariStompConnection(WSStompConnection):
  def __init__(self, connection_url, ssl_options=None):
    parsed_url = urlsplit(connection_url)
    if parsed_url.scheme != "wss" or not parsed_url.hostname:
      raise ValueError(f"Invalid Ambari STOMP URL: {connection_url}")

    host_and_port = (parsed_url.hostname, parsed_url.port or 443)
    ws_path = parsed_url.path or "/"
    if parsed_url.query:
      ws_path = f"{ws_path}?{parsed_url.query}"

    super(AmbariStompConnection, self).__init__(
      host_and_ports=[host_and_port],
      prefer_localhost=False,
      try_loopback_connect=False,
      reconnect_attempts_max=1,
      timeout=10,
      vhost=parsed_url.hostname,
      ws_path=ws_path,
    )

    options = {
      "cert_reqs": ssl.CERT_REQUIRED,
      "check_hostname": True,
    }
    if ssl_options:
      options.update(ssl_options)
    ssl_version = options.get("ssl_version", ssl.PROTOCOL_TLS_CLIENT)
    ssl_context = options.get("context")
    if ssl_context is None:
      ssl_context = create_ssl_context(ssl_version, options.get("ca_certs"))
      if options.get("certfile"):
        ssl_context.load_cert_chain(
          options["certfile"],
          options.get("keyfile"),
          options.get("password"),
        )
    else:
      if ssl_context.minimum_version < ssl.TLSVersion.TLSv1_2:
        ssl_context.minimum_version = ssl.TLSVersion.TLSv1_2
      ssl_context.verify_mode = ssl.CERT_REQUIRED
      ssl_context.check_hostname = True
    options["context"] = ssl_context
    self.set_ssl(
      for_hosts=[host_and_port],
      key_file=options.get("keyfile"),
      cert_file=options.get("certfile"),
      ca_certs=options.get("ca_certs"),
      ssl_version=ssl_version,
      password=options.get("password"),
    )

    # stomp.py selects WSS through set_ssl(), while websocket-client consumes
    # these spellings directly as sslopt.
    websocket_ssl_options = self.transport.get_ssl(host_and_port)
    websocket_ssl_options.update(options)

    self.lock = threading.RLock()
    self.correlation_id = -1
    self._connect_event = threading.Event()
    self._connect_failed = False
    self.set_listener("ambari-connect-wait-listener", self)

  def connect(
    self,
    username=None,
    passcode=None,
    wait=False,
    headers=None,
    **keyword_headers,
  ):
    self._connect_event.clear()
    self._connect_failed = False
    super(AmbariStompConnection, self).connect(
      username=username,
      passcode=passcode,
      wait=False,
      headers=headers,
      **keyword_headers,
    )

    if not wait:
      return

    if not self._connect_event.wait(DEFAULT_CONNECTION_TIMEOUT):
      raise ConnectionResponseTimeout(
        "Waiting for STOMP connection confirmation timed out"
      )

    if self._connect_failed or not self.transport.is_connected():
      raise ConnectFailedException()

  def on_connected(self, frame):
    self._connect_failed = False
    self._connect_event.set()

  def on_error(self, frame):
    self._connect_failed = True
    self._connect_event.set()

  def on_disconnected(self):
    self._connect_failed = True
    self._connect_event.set()

  def send(
    self,
    destination,
    message,
    content_type=None,
    headers=None,
    log_message_function=lambda value: value,
    presend_hook=None,
    **keyword_headers,
  ):
    with self.lock:
      self.correlation_id += 1
      correlation_id = self.correlation_id
      send_failure_cleanup = None
      try:
        if presend_hook:
          send_failure_cleanup = presend_hook(correlation_id)

        logged_message = log_message_function(copy.deepcopy(message))
        logger.info(
          "Event to server at %s (correlation_id=%s): %s",
          destination,
          correlation_id,
          logged_message,
        )

        super(AmbariStompConnection, self).send(
          destination=destination,
          body=json.dumps(message),
          content_type=content_type,
          headers=headers,
          correlationId=correlation_id,
          **keyword_headers,
        )
      except (
        ConnectionClosedException,
        NotConnectedException,
        WebSocketConnectionClosedException,
      ) as exc:
        if callable(send_failure_cleanup):
          send_failure_cleanup()
        raise ConnectionIsAlreadyClosed(str(exc)) from exc
      except Exception:
        if callable(send_failure_cleanup):
          send_failure_cleanup()
        raise

      return correlation_id

  def disconnect(self, *args, **kwargs):
    try:
      super(AmbariStompConnection, self).disconnect(*args, **kwargs)
    except Exception:
      logger.debug("Exception while disconnecting STOMP", exc_info=True)
    finally:
      try:
        self.transport.disconnect_socket()
      except Exception:
        logger.debug("Exception while closing STOMP socket", exc_info=True)

      try:
        self.transport.stop()
      except Exception:
        logger.debug("Exception while stopping STOMP transport", exc_info=True)

  def add_listener(self, listener):
    self.set_listener(listener.__class__.__name__, listener)
