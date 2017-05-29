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
from ambari_agent.InitializerModule import InitializerModule
from ambari_agent.ComponentStatusExecutor import ComponentStatusExecutor
from ambari_agent.CommandStatusReporter import CommandStatusReporter
from ambari_agent.CustomServiceOrchestrator import CustomServiceOrchestrator

from mock.mock import MagicMock, patch

class TestAgentStompResponses(BaseStompServerTestCase):
  @patch.object(CustomServiceOrchestrator, "runCommand")
  def test_mock_server_can_start(self, runCommand_mock):
    runCommand_mock.return_value = {'stdout':'...', 'stderr':'...', 'structuredOut' : '{}', 'exitcode':1}

    self.remove_files(['/tmp/cluster_cache/configurations.json', '/tmp/cluster_cache/metadata.json', '/tmp/cluster_cache/topology.json'])

    if not os.path.exists("/tmp/ambari-agent"):
      os.mkdir("/tmp/ambari-agent")

    initializer_module = InitializerModule()
    heartbeat_thread = HeartbeatThread.HeartbeatThread(initializer_module)
    heartbeat_thread.start()

    action_queue = initializer_module.action_queue
    action_queue.start()

    component_status_executor = ComponentStatusExecutor(initializer_module)
    component_status_executor.start()

    command_status_reporter = CommandStatusReporter(initializer_module)
    command_status_reporter.start()

    connect_frame = self.server.frames_queue.get()
    users_subscribe_frame = self.server.frames_queue.get()
    registration_frame = self.server.frames_queue.get()

    # server sends registration response
    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '0'}, body=self.get_json("registration_response.json"))
    self.server.topic_manager.send(f)


    # response to /initial_topology
    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '1'}, body=self.get_json("topology_update.json"))
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '2'}, body=self.get_json("metadata_after_registration.json"))
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '3'}, body=self.get_json("configurations_update.json"))
    self.server.topic_manager.send(f)

    initial_topology_request = self.server.frames_queue.get()
    initial_metadata_request = self.server.frames_queue.get()
    initial_configs_request = self.server.frames_queue.get()

    while not initializer_module.is_registered:
      time.sleep(0.1)

    f = Frame(frames.MESSAGE, headers={'destination': '/user/commands'}, body=self.get_json("execution_commands.json"))
    self.server.topic_manager.send(f)

    commands_subscribe_frame = self.server.frames_queue.get()
    configurations_subscribe_frame = self.server.frames_queue.get()
    metadata_subscribe_frame = self.server.frames_queue.get()
    topologies_subscribe_frame = self.server.frames_queue.get()
    heartbeat_frame = self.server.frames_queue.get()
    dn_start_in_progress_frame = json.loads(self.server.frames_queue.get().body)
    dn_start_failed_frame = json.loads(self.server.frames_queue.get().body)
    zk_start_in_progress_frame = json.loads(self.server.frames_queue.get().body)
    zk_start_failed_frame = json.loads(self.server.frames_queue.get().body)
    action_status_in_progress_frame = json.loads(self.server.frames_queue.get().body)
    action_status_failed_frame = json.loads(self.server.frames_queue.get().body)
    initializer_module.stop_event.set()

    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '4'}, body=json.dumps({'heartbeat-response':'true'}))
    self.server.topic_manager.send(f)

    heartbeat_thread.join()
    component_status_executor.join()
    command_status_reporter.join()
    action_queue.join()

    self.assertEquals(initializer_module.topology_cache['0']['hosts'][0]['hostname'], 'c6401.ambari.apache.org')
    self.assertEquals(initializer_module.metadata_cache['0']['status_commands_to_run'], ('STATUS',))
    self.assertEquals(initializer_module.configurations_cache['0']['configurations']['zoo.cfg']['clientPort'], '2181')
    self.assertEquals(dn_start_in_progress_frame[0]['roleCommand'], 'START')
    self.assertEquals(dn_start_in_progress_frame[0]['role'], 'DATANODE')
    self.assertEquals(dn_start_in_progress_frame[0]['status'], 'IN_PROGRESS')
    self.assertEquals(dn_start_failed_frame[0]['status'], 'FAILED')

    """
    ============================================================================================
    ============================================================================================
    """

    initializer_module = InitializerModule()
    self.server.frames_queue.queue.clear()

    heartbeat_thread = HeartbeatThread.HeartbeatThread(initializer_module)
    heartbeat_thread.start()

    action_queue = initializer_module.action_queue
    action_queue.start()

    component_status_executor = ComponentStatusExecutor(initializer_module)
    component_status_executor.start()

    command_status_reporter = CommandStatusReporter(initializer_module)
    command_status_reporter.start()

    connect_frame = self.server.frames_queue.get()
    users_subscribe_frame = self.server.frames_queue.get()
    registration_frame = self.server.frames_queue.get()

    # server sends registration response
    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '0'}, body=self.get_json("registration_response.json"))
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '1'}, body='{}')
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '2'}, body='{}')
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '3'}, body='{}')
    self.server.topic_manager.send(f)

    commands_subscribe_frame = self.server.frames_queue.get()
    configurations_subscribe_frame = self.server.frames_queue.get()
    metadata_subscribe_frame = self.server.frames_queue.get()
    topologies_subscribe_frame = self.server.frames_queue.get()
    heartbeat_frame = self.server.frames_queue.get()
    status_reports_frame = self.server.frames_queue.get()

    initializer_module.stop_event.set()

    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '4'}, body=json.dumps({'heartbeat-response':'true'}))
    self.server.topic_manager.send(f)

    heartbeat_thread.join()
    component_status_executor.join()
    command_status_reporter.join()
    action_queue.join()