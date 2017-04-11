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
import os
import sys
import logging
import json
import time
from coilmq.util import frames
from coilmq.util.frames import Frame

from BaseStompServerTestCase import BaseStompServerTestCase

from ambari_agent import HeartbeatThread

from mock.mock import MagicMock, patch

# TODO: where agent sends?

class TestAgentStompResponses(BaseStompServerTestCase):
  @patch.object(HeartbeatThread, "time")
  def test_mock_server_can_start(self, time_mock):
    self.init_stdout_logger()

    heartbeat_thread = HeartbeatThread.HeartbeatThread()
    heartbeat_thread.start()

    connect_frame = self.server.frames_queue.get()
    users_subscribe_frame = self.server.frames_queue.get()
    registration_frame = self.server.frames_queue.get()

    # server sends registration response
    f = Frame(frames.MESSAGE, headers={'destination': '/user', 'correlationId': '0'}, body=self.get_json("registration_response.json"))
    self.server.topic_manager.send(f)

    heartbeat_frame = self.server.frames_queue.get()

    heartbeat_thread._stop.set()

    # server sends heartbeat response
    f = Frame(frames.MESSAGE, headers={'destination': '/user', 'correlationId': '1'}, body=json.dumps({'heartbeat-response':'true'}))
    self.server.topic_manager.send(f)

    heartbeat_thread.join()
    print "Thread successfully finished"

  def _other(self):
    f = Frame(frames.MESSAGE, headers={'destination': '/events/configurations'}, body=self.get_json("configurations_update.json"))
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/events/commands'}, body=self.get_json("execution_commands.json"))
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/events/metadata'}, body=self.get_json("metadata_update.json"))
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/events/topologies'}, body=self.get_json("topology_update.json"))
    self.server.topic_manager.send(f)

  def init_stdout_logger(self):
    format='%(levelname)s %(asctime)s - %(message)s'

    logger = logging.getLogger()
    logger.setLevel(logging.DEBUG)
    formatter = logging.Formatter(format)
    chout = logging.StreamHandler(sys.stdout)
    chout.setLevel(logging.DEBUG)
    chout.setFormatter(formatter)
    cherr = logging.StreamHandler(sys.stderr)
    cherr.setLevel(logging.ERROR)
    cherr.setFormatter(formatter)
    logger.handlers = []
    logger.addHandler(cherr)
    logger.addHandler(chout)

    logging.getLogger('stomp.py').setLevel(logging.WARN)
    logging.getLogger('coilmq').setLevel(logging.INFO)