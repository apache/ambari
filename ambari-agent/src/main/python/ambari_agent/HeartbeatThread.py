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
from collections import defaultdict

from ambari_agent import Constants
from ambari_agent.ClusterConfigurationCache import  ClusterConfigurationCache
from ambari_agent.ClusterTopologyCache import ClusterTopologyCache
from ambari_agent.ClusterMetadataCache import ClusterMetadataCache
from ambari_agent.listeners.ServerResponsesListener import ServerResponsesListener
from ambari_agent.listeners.TopologyEventListener import TopologyEventListener
from ambari_agent.listeners.ConfigurationEventListener import ConfigurationEventListener
from ambari_agent.listeners.MetadataEventListener import MetadataEventListener

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

    # TODO STOMP: change this once is integrated with ambari config
    cluster_cache_dir = '/tmp'

    # caches
    self.metadata_cache = ClusterMetadataCache(cluster_cache_dir)
    self.topology_cache = ClusterTopologyCache(cluster_cache_dir)
    self.configurations_cache = ClusterConfigurationCache(cluster_cache_dir)
    self.caches = [self.metadata_cache, self.topology_cache, self.configurations_cache]

    # listeners
    self.server_responses_listener = ServerResponsesListener()
    self.metadata_events_listener = MetadataEventListener(self.metadata_cache)
    self.topology_events_listener = TopologyEventListener(self.topology_cache)
    self.configuration_events_listener = ConfigurationEventListener(self.configurations_cache)
    self.listeners = [self.server_responses_listener, self.metadata_events_listener, self.topology_events_listener, self.configuration_events_listener]

  def run(self):
    """
    Run an endless loop of hearbeat with registration upon init or exception in heartbeating.
    """
    # TODO STOMP: stop the thread on SIGTERM
    while not self._stop.is_set():
      try:
        if not self.is_registered:
          self.register()

        heartbeat_body = self.get_heartbeat_body()
        logger.debug("Heartbeat body is {0}".format(heartbeat_body))
        response = self.blocking_request(heartbeat_body, Constants.HEARTBEAT_ENDPOINT)
        logger.debug("Heartbeat response is {0}".format(response))

        time.sleep(self.heartbeat_interval)
        # TODO STOMP: handle heartbeat reponse
      except:
        logger.exception("Exception in HeartbeatThread. Re-running the registration")
        # TODO STOMP: re-connect here
        self.is_registered = False
        pass
    logger.info("HeartbeatThread has successfully finished")

  def register(self):
    """
    Subscribe to topics, register with server, wait for server's response.
    """
    self.subscribe_and_listen()

    registration_request = self.get_registration_request()
    logger.info("Registration request received")
    logger.debug("Registration request is {0}".format(registration_request))

    response = self.blocking_request(registration_request, Constants.REGISTRATION_ENDPOINT)

    logger.info("Registration response received")
    logger.debug("Registration response is {0}".format(response))

    self.registration_response = response
    self.registered = True

  def get_registration_request(self):
    """
    Get registration request body to send it to server
    """
    request = {'clusters':defaultdict(lambda:{})}

    for cache in self.caches:
      cache_key_name = cache.get_cache_name() + '_hash'
      for cluster_name in cache.get_cluster_names():
        request['clusters'][cluster_name][cache_key_name] = cache.get_md5_hashsum(cluster_name)

    return request

  def get_heartbeat_body(self):
    """
    Heartbeat body to be send to server
    """
    return {'heartbeat-request-test':'true'}

  def subscribe_and_listen(self):
    """
    Subscribe to topics and set listener classes.
    """
    for listener in self.listeners:
      self.stomp_connector.add_listener(listener)

    for topic_name in Constants.TOPICS_TO_SUBSCRIBE:
      self.stomp_connector.subscribe(destination=topic_name, id='sub', ack='client-individual')

  def blocking_request(self, body, destination):
    """
    Send a request to server and waits for the response from it. The response it detected by the correspondence of correlation_id.
    """
    self.stomp_connector.send(body=json.dumps(body), destination=destination)
    return self.server_responses_listener.responses.blocking_pop(str(self.stomp_connector.correlation_id))