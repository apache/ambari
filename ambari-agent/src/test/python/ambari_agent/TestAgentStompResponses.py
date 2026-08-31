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

import json
from unittest import TestCase
from unittest.mock import MagicMock

from stomp.utils import Frame

from ambari_agent import Constants
from ambari_agent.listeners import EventListener
from ambari_agent.listeners.ServerResponsesListener import ServerResponsesListener


class FakeConnection:
  def __init__(self):
    self.sent = []

  def send(self, **kwargs):
    self.sent.append(kwargs)


class RecordingEventListener(EventListener):
  def __init__(self, initializer_module):
    super(RecordingEventListener, self).__init__(initializer_module)
    self.events = []

  def on_event(self, headers, message):
    self.events.append((headers, message))

  def get_handled_path(self):
    return "/events/test"


class TestAgentStompResponses(TestCase):
  def setUp(self):
    self.initializer_module = MagicMock()
    self.initializer_module.connection = FakeConnection()

  def test_official_frame_is_dispatched_and_confirmed(self):
    listener = RecordingEventListener(self.initializer_module)
    frame = Frame(
      "MESSAGE",
      headers={
        "destination": "/events/test",
        Constants.MESSAGE_ID: "message-1",
      },
      body=json.dumps({"eventType": "UPDATE"}),
    )

    listener.on_message(frame)

    self.assertEqual(
      listener.events,
      [
        (
          frame.headers,
          {"eventType": "UPDATE"},
        )
      ],
    )
    self.assertEqual(
      self.initializer_module.connection.sent,
      [
        {
          "message": {Constants.MESSAGE_ID: "message-1", "status": "OK"},
          "destination": Constants.AGENT_RESPONSES_TOPIC,
        }
      ],
    )

  def test_invalid_json_reports_error_without_dispatch(self):
    listener = RecordingEventListener(self.initializer_module)
    frame = Frame(
      "MESSAGE",
      headers={
        "destination": "/events/test",
        Constants.MESSAGE_ID: "message-2",
      },
      body="not-json",
    )

    listener.on_message(frame)

    self.assertEqual(listener.events, [])
    response = self.initializer_module.connection.sent[0]["message"]
    self.assertEqual(response[Constants.MESSAGE_ID], "message-2")
    self.assertEqual(response["status"], "ERROR")
    self.assertIn("JSONDecodeError", response["reason"])

  def test_server_response_frame_is_correlated(self):
    listener = ServerResponsesListener(self.initializer_module)
    frame = Frame(
      "MESSAGE",
      headers={
        "destination": Constants.SERVER_RESPONSES_TOPIC,
        Constants.CORRELATION_ID_STRING: "7",
      },
      body=json.dumps({"status": "OK", "id": 1}),
    )

    listener.on_message(frame)

    self.assertEqual(
      listener.responses.blocking_pop(7, timeout=0.1),
      {"status": "OK", "id": 1},
    )

  def test_legacy_direct_listener_call_remains_supported(self):
    listener = RecordingEventListener(self.initializer_module)
    headers = {"destination": "/events/test"}

    listener.on_message(headers, json.dumps({"eventType": "CREATE"}))

    self.assertEqual(listener.events, [(headers, {"eventType": "CREATE"})])
