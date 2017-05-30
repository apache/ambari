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

import logging
import ambari_stomp
import threading

from ambari_agent import Constants
from ambari_agent.listeners.ServerResponsesListener import ServerResponsesListener
from ambari_agent.listeners.TopologyEventListener import TopologyEventListener
from ambari_agent.listeners.ConfigurationEventListener import ConfigurationEventListener
from ambari_agent.listeners.MetadataEventListener import MetadataEventListener
from ambari_agent.listeners.CommandsEventListener import CommandsEventListener

HEARTBEAT_INTERVAL = 10

logger = logging.getLogger(__name__)

class HeartbeatThread(threading.Thread):
  """
  This thread handles registration and heartbeating routine.
  """
  def __init__(self, initializer_module):
    threading.Thread.__init__(self)
    self.heartbeat_interval = HEARTBEAT_INTERVAL
    self.stop_event = initializer_module.stop_event

    self.initializer_module = initializer_module
    self.caches = [initializer_module.metadata_cache, initializer_module.topology_cache, initializer_module.configurations_cache]

    # listeners
    self.server_responses_listener = ServerResponsesListener()
    self.commands_events_listener = CommandsEventListener(initializer_module.action_queue)
    self.metadata_events_listener = MetadataEventListener(initializer_module.metadata_cache)
    self.topology_events_listener = TopologyEventListener(initializer_module.topology_cache)
    self.configuration_events_listener = ConfigurationEventListener(initializer_module.configurations_cache)
    self.listeners = [self.server_responses_listener, self.commands_events_listener, self.metadata_events_listener, self.topology_events_listener, self.configuration_events_listener]
    self.post_registration_requests = [
    (Constants.TOPOLOGY_REQUEST_ENDPOINT, initializer_module.topology_cache, self.topology_events_listener),
    (Constants.METADATA_REQUEST_ENDPOINT, initializer_module.metadata_cache, self.metadata_events_listener),
    (Constants.CONFIGURATIONS_REQUEST_ENDPOINT, initializer_module.configurations_cache, self.configuration_events_listener)
    ]


  def run(self):
    """
    Run an endless loop of hearbeat with registration upon init or exception in heartbeating.
    """
    # TODO STOMP: stop the thread on SIGTERM
    while not self.stop_event.is_set():
      try:
        if not self.initializer_module.is_registered:
          self.register()

        heartbeat_body = self.get_heartbeat_body()
        logger.debug("Heartbeat body is {0}".format(heartbeat_body))
        response = self.blocking_request(heartbeat_body, Constants.HEARTBEAT_ENDPOINT)
        logger.debug("Heartbeat response is {0}".format(response))

        self.stop_event.wait(self.heartbeat_interval)
        # TODO STOMP: handle heartbeat reponse
      except:
        logger.exception("Exception in HeartbeatThread. Re-running the registration")
        self.stop_event.wait(self.heartbeat_interval)
        self.initializer_module.is_registered = False
        self.initializer_module.connection.disconnect()
        pass

    self.initializer_module.connection.disconnect()
    logger.info("HeartbeatThread has successfully finished")

  def register(self):
    """
    Subscribe to topics, register with server, wait for server's response.
    """
    self.add_listeners()
    self.subscribe_to_topics(Constants.PRE_REGISTRATION_TOPICS_TO_SUBSCRIBE)

    registration_request = self.get_registration_request()
    logger.info("Sending registration request")
    logger.debug("Registration request is {0}".format(registration_request))

    response = self.blocking_request(registration_request, Constants.REGISTRATION_ENDPOINT)

    logger.info("Registration response received")
    logger.debug("Registration response is {0}".format(response))

    self.registration_response = response

    for endpoint, cache, listener in self.post_registration_requests:
      response = self.blocking_request({'hash': cache.hash}, endpoint)
      listener.on_event({}, response)

    self.subscribe_to_topics(Constants.POST_REGISTRATION_TOPICS_TO_SUBSCRIBE)
    self.initializer_module.is_registered = True

  def get_registration_request(self):
    """
    Get registration request body to send it to server
    """
    return {'registration-response':'true'}

  def get_heartbeat_body(self):
    """
    Heartbeat body to be send to server
    """
    return {'hostname':'true'}

  def add_listeners(self):
    """
    Subscribe to topics and set listener classes.
    """
    for listener in self.listeners:
      self.initializer_module.connection.add_listener(listener)

  def subscribe_to_topics(self, topics_list):
    for topic_name in topics_list:
      self.initializer_module.connection.subscribe(destination=topic_name, id='sub', ack='client-individual')

  def blocking_request(self, message, destination):
    """
    Send a request to server and waits for the response from it. The response it detected by the correspondence of correlation_id.
    """
    correlation_id = self.initializer_module.connection.send(message=message, destination=destination)
    return self.server_responses_listener.responses.blocking_pop(str(correlation_id))