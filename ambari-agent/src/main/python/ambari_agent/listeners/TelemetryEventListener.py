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

import logging

from ambari_agent import Constants
from ambari_agent.listeners import EventListener


logger = logging.getLogger(__name__)


class TelemetryEventListener(EventListener):
  """Applies complete, hash-addressed component telemetry assignments."""

  def __init__(self, initializer_module):
    super(TelemetryEventListener, self).__init__(initializer_module)
    self.telemetry_cache = initializer_module.telemetry_cache

  def on_event(self, headers, message):
    if message == {}:
      return

    self.telemetry_cache.update(
      message["assignment"],
      message.get("profiles", {}),
      message["hash"],
    )

  def get_handled_path(self):
    return Constants.TELEMETRY_TOPIC

  def get_log_message(self, headers, message_json):
    if message_json:
      assignment = message_json.get("assignment", {})
      message_json["assignment"] = {
        "targetCount": len(assignment.get("targets", []))
      }
      message_json["profiles"] = list(message_json.get("profiles", {}))
    return super(TelemetryEventListener, self).get_log_message(headers, message_json)
