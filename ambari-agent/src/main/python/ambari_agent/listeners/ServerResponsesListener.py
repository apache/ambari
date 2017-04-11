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

from ambari_agent import Utils
from ambari_agent.Constants import CORRELATION_ID_STRING

logging = logging.getLogger(__name__)

class ServerResponsesListener(ambari_stomp.ConnectionListener):
  def __init__(self):
    self.responses = Utils.BlockingDictionary()

  def on_message(self, headers, message):
    logging.debug("Received headers={0} ; message={1}".format(headers, message))

    if CORRELATION_ID_STRING in headers:
      self.responses.put(headers[CORRELATION_ID_STRING], message)
    else:
      logging.warn("Received a message from server without a '{0}' header. Ignoring the message".format(CORRELATION_ID_STRING))