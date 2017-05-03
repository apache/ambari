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
from coilmq.util import frames
from coilmq.util.frames import Frame

from BaseStompServerTestCase import BaseStompServerTestCase

from ambari_agent import HeartbeatThread
from ambari_agent.InitializerModule import InitializerModule
from ambari_agent.ComponentStatusExecutor import ComponentStatusExecutor

from mock.mock import MagicMock, patch

class TestAgentStompResponses(BaseStompServerTestCase):
  def test_mock_server_can_start(self):
    self.init_stdout_logger()

    self.remove(['/tmp/cluster_cache/configurations.json', '/tmp/cluster_cache/metadata.json', '/tmp/cluster_cache/topology.json'])

    initializer_module = InitializerModule()
    heartbeat_thread = HeartbeatThread.HeartbeatThread(initializer_module)
    heartbeat_thread.heartbeat_interval = 0
    heartbeat_thread.start()

    connect_frame = self.server.frames_queue.get()
    users_subscribe_frame = self.server.frames_queue.get()
    commands_subscribe_frame = self.server.frames_queue.get()
    configurations_subscribe_frame = self.server.frames_queue.get()
    metadata_subscribe_frame = self.server.frames_queue.get()
    topologies_subscribe_frame = self.server.frames_queue.get()
    registration_frame = self.server.frames_queue.get()

    component_status_executor = ComponentStatusExecutor(initializer_module)
    component_status_executor.start()

    status_reports_frame = self.server.frames_queue.get()

    # server sends registration response
    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '0'}, body=self.get_json("registration_response.json"))
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/user/configs'}, body=self.get_json("configurations_update.json"))
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/user/commands'}, body=self.get_json("execution_commands.json"))
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/user/metadata'}, body=self.get_json("metadata_after_registration.json"))
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/user/topologies'}, body=self.get_json("topology_update.json"))
    self.server.topic_manager.send(f)

    heartbeat_frame = self.server.frames_queue.get()
    initializer_module.stop_event.set()

    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '2'}, body=json.dumps({'heartbeat-response':'true'}))
    self.server.topic_manager.send(f)

    heartbeat_thread.join()
    component_status_executor.join()

    self.assertEquals(initializer_module.topology_cache['0']['hosts'][0]['hostname'], 'c6401.ambari.apache.org')
    self.assertEquals(initializer_module.metadata_cache['0']['status_commands_to_run'], ('STATUS',))
    self.assertEquals(initializer_module.configurations_cache['0']['configurations']['zoo.cfg']['clientPort'], '2181')

    """
    ============================================================================================
    ============================================================================================
    """

    initializer_module = InitializerModule()
    self.server.frames_queue.queue.clear()

    heartbeat_thread = HeartbeatThread.HeartbeatThread(initializer_module)
    heartbeat_thread.heartbeat_interval = 0
    heartbeat_thread.start()

    connect_frame = self.server.frames_queue.get()
    users_subscribe_frame = self.server.frames_queue.get()
    commands_subscribe_frame = self.server.frames_queue.get()
    configurations_subscribe_frame = self.server.frames_queue.get()
    metadata_subscribe_frame = self.server.frames_queue.get()
    topologies_subscribe_frame = self.server.frames_queue.get()
    registration_frame_json = json.loads(self.server.frames_queue.get().body)
    clusters_hashes = registration_frame_json['clusters']['0']

    component_status_executor = ComponentStatusExecutor(initializer_module)
    component_status_executor.start()

    status_reports_frame = self.server.frames_queue.get()

    self.assertEquals(clusters_hashes['metadata_hash'], '21724f6ffa7aff0fe91a0c0c5b765dba')
    self.assertEquals(clusters_hashes['configurations_hash'], '04c968412ded7c8ffe7858036bae03ce')
    self.assertEquals(clusters_hashes['topology_hash'], '0de1df56fd594873fe594cf02ea61f4b')

    # server sends registration response
    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '0'}, body=self.get_json("registration_response.json"))
    self.server.topic_manager.send(f)

    heartbeat_frame = self.server.frames_queue.get()
    initializer_module.stop_event.set()

    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '2'}, body=json.dumps({'heartbeat-response':'true'}))
    self.server.topic_manager.send(f)

    heartbeat_thread.join()
    component_status_executor.join()

  def remove(self, filepathes):
    for filepath in filepathes:
      if os.path.isfile(filepath):
        os.remove(filepath)

  def init_stdout_logger(self):
    format='%(levelname)s %(asctime)s - %(message)s'

    logger = logging.getLogger()
    logger.setLevel(logging.INFO)
    formatter = logging.Formatter(format)
    chout = logging.StreamHandler(sys.stdout)
    chout.setLevel(logging.INFO)
    chout.setFormatter(formatter)
    cherr = logging.StreamHandler(sys.stderr)
    cherr.setLevel(logging.ERROR)
    cherr.setFormatter(formatter)
    logger.handlers = []
    logger.addHandler(cherr)
    logger.addHandler(chout)

    logging.getLogger('stomp.py').setLevel(logging.WARN)
    logging.getLogger('coilmq').setLevel(logging.INFO)