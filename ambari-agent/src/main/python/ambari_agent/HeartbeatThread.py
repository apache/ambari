#!/usr/bin/env python

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

import json
import time
import logging
import ambari_stomp
import threading
import security

from ambari_agent.listeners.ServerResponsesListener import ServerResponsesListener

HEARTBEAT_ENDPOINT = '/agent/heartbeat'
REGISTRATION_ENDPOINT = '/agent/registration'
SERVER_RESPONSES_ENDPOINT = '/user'
HEARTBEAT_INTERVAL = 10

logger = logging.getLogger(__name__)

class HeartbeatThread(threading.Thread):
  """
  This thread handles registration and heartbeating routine.
  """
  def __init__(self):
    threading.Thread.__init__(self)
    self.stomp_connector = security.StompConnector()
    self.is_registered = False
    self.heartbeat_interval = HEARTBEAT_INTERVAL
    self._stop = threading.Event()

  def run(self):
    while not self._stop.is_set():
      try:
        if not self.is_registered:
          self.register()

        heartbeat_body = self.get_heartbeat_body()
        logger.debug("Heartbeat body is {0}".format(heartbeat_body))
        response = self.blocking_request(heartbeat_body, HEARTBEAT_ENDPOINT)
        logger.debug("Heartbeat response is {0}".format(response))

        time.sleep(self.heartbeat_interval)
        # TODO STOMP: handle heartbeat reponse
      except:
        logger.exception("Exception in HeartbeatThread. Re-running the registration")
        # TODO STOMP: re-connect here
        self.is_registered = False
        pass

  def blocking_request(self, body, destination):
    self.stomp_connector.send(body=json.dumps(body), destination=destination)
    return self.server_responses_listener.responses.blocking_pop(str(self.stomp_connector.correlation_id))

  def register(self):
    # TODO STOMP: prepare data to register
    data = {'registration-test':'true'}
    self.server_responses_listener = ServerResponsesListener()
    self.stomp_connector._connection = self.stomp_connector._create_new_connection(self.server_responses_listener)
    self.stomp_connector.add_listener(self.server_responses_listener)
    self.stomp_connector.subscribe(destination=SERVER_RESPONSES_ENDPOINT, id=1, ack='client-individual')

    logger.debug("Registration request is {0}".format(data))
    response = self.blocking_request(data, REGISTRATION_ENDPOINT)
    logger.debug("Registration response is {0}".format(response))

    # TODO STOMP: handle registration response
    self.registered = True

  def get_heartbeat_body(self):
    return {'heartbeat-request-test':'true'}