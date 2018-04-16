#!/usr/bin/env python
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

import ambari_simplejson as json
import ambari_stomp
import logging
import copy

logger = logging.getLogger(__name__)

class EventListener(ambari_stomp.ConnectionListener):
  """
  Base abstract class for event listeners on specific topics.
  """
  def on_message(self, headers, message):
    """
    This method is triggered by stomp when message from serve is received.

    Here we handle some decode the message to json and check if it addressed to this specific event listener.
    """
    if not 'destination' in headers:
      logger.warn("Received event from server which does not contain 'destination' header")
      return

    destination = headers['destination']
    if destination.rstrip('/') == self.get_handled_path().rstrip('/'):
      try:
        message_json = json.loads(message)
      except ValueError:
        logger.exception("Received from server event is not a valid message json. Message is:\n{0}".format(message))
        return

      logger.info("Event from server at {0}{1}".format(destination, self.get_log_message(headers, copy.deepcopy(message_json))))
      try:
        self.on_event(headers, message_json)
      except:
        logger.exception("Exception while handing event from {0} {1}".format(destination, headers, message))

  def on_event(self, headers, message):
    """
    Is triggered when an event for specific listener is received:

    @param headers: headers dictionary
    @param message: message payload dictionary
    """
    raise NotImplementedError()

  def get_log_message(self, headers, message_json):
    """
    This string will be used to log received messsage of this type
    """
    return ": " + str(message_json)