'''
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
'''

import copy
import logging

from Queue import Queue

from ambari_stomp.connect import BaseConnection
from ambari_stomp.protocol import Protocol12
from ambari_stomp.transport import Transport, DEFAULT_SSL_VERSION

from ambari_ws4py.client.threadedclient import WebSocketClient

logger = logging.getLogger(__name__)

class QueuedWebSocketClient(WebSocketClient):
  def __init__(self, *args, **kwargs):
    WebSocketClient.__init__(self, *args, **kwargs)
    self.messages = Queue()

  def received_message(self, message):
    """
    Override the base class to store the incoming message
    in the `messages` queue.
    """
    self.messages.put(copy.deepcopy(message))

  def receive(self):
    """
    Returns messages that were stored into the
    `messages` queue and returns `None` when the
    websocket is terminated or closed.
    """
    # If the websocket was terminated and there are no messages
    # left in the queue, return None immediately otherwise the client
    # will block forever
    if self.terminated and self.messages.empty():
      return None
    message = self.messages.get()
    if message is StopIteration:
      return None
    return message

  def closed(self, code, reason=None):
    self.messages.put(StopIteration)

class WsTransport(Transport):
  def __init__(self, url):
    Transport.__init__(self, (0, 0), False, False, 0.0, 0.0, 0.0, 0.0, 0, False, None, None, None, None, False,
    DEFAULT_SSL_VERSION, None, None, None)
    self.current_host_and_port = (0, 0) # mocking
    self.ws = QueuedWebSocketClient(url, protocols=['http-only', 'chat'])
    self.ws.daemon = False

  def is_connected(self):
    return self.connected

  def attempt_connection(self):
    self.ws.connect()

  def send(self, encoded_frame):
    logger.debug("Outgoing STOMP message:\n>>> " + encoded_frame)
    self.ws.send(encoded_frame)

  def receive(self):
    try:
      msg = str(self.ws.receive())
      logger.debug("Incoming STOMP message:\n<<< " + msg)
      return msg
    except:
      # exceptions from this method are hidden by the framework so implementing logging by ourselves
      logger.exception("Exception while handling incoming STOMP message:")
    return None

  def stop(self):
    self.running = False
    self.ws.close_connection()
    self.disconnect_socket()
    Transport.stop(self)

class WsConnection(BaseConnection, Protocol12):
  def __init__(self, url):
    self.transport = WsTransport(url)
    self.transport.set_listener('ws-listener', self)
    self.transactions = {}
    Protocol12.__init__(self, self.transport, (0, 0))

  def disconnect(self, receipt=None, headers=None, **keyword_headers):
    Protocol12.disconnect(self, receipt, headers, **keyword_headers)
    self.transport.stop()
