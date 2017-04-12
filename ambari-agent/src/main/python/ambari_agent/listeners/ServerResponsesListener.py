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
from ambari_agent import Utils
from ambari_agent import Constants

logger = logging.getLogger(__name__)

class ServerResponsesListener(EventListener):
  """
  Listener of Constants.SERVER_RESPONSES_TOPIC events from server.
  """
  def __init__(self):
    self.responses = Utils.BlockingDictionary()

  def on_event(self, headers, message):
    """
    Is triggered when an event to Constants.SERVER_RESPONSES_TOPIC topic is received from server.
    This type of event is general response to the agent request and contains 'correlationId', which is an int value
    of the request it responds to.

    @param headers: headers dictionary
    @param message: message payload dictionary
    """
    if Constants.CORRELATION_ID_STRING in headers:
      self.responses.put(headers[Constants.CORRELATION_ID_STRING], message)
    else:
      logger.warn("Received a message from server without a '{0}' header. Ignoring the message".format(Constants.CORRELATION_ID_STRING))\

  def get_handled_path(self):
    return Constants.SERVER_RESPONSES_TOPIC