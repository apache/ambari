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

from ambari_agent.listeners import EventListener
from ambari_agent import Constants

logger = logging.getLogger(__name__)

METADATA_DICTIONARY_KEY = 'metadataClusters'

class MetadataEventListener(EventListener):
  """
  Listener of Constants.METADATA_TOPIC events from server.
  """
  def __init__(self, metadata_cache, recovery_manager):
    self.metadata_cache = metadata_cache
    self.recovery_manager = recovery_manager

  def on_event(self, headers, message):
    """
    Is triggered when an event to Constants.METADATA_TOPIC topic is received from server.

    @param headers: headers dictionary
    @param message: message payload dictionary
    """
    # this kind of response is received if hash was identical. And server does not need to change anything
    if message == {}:
      return

    self.metadata_cache.rewrite_cache(message['clusters'])
    self.metadata_cache.hash = message['hash']

    # FIXME: Recovery manager does not support multiple cluster as of now.
    cluster_id = message['clusters'].keys()[0]

    if 'recoveryConfig' in message['clusters'][cluster_id]:
      logging.info("Updating recoveryConfig from metadata")
      self.recovery_manager.update_recovery_config(self.metadata_cache[cluster_id])
      self.recovery_manager.cluster_id = cluster_id

  def get_handled_path(self):
    return Constants.METADATA_TOPIC