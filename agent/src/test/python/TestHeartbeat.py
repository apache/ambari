#!/usr/bin/env python2.6

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

from unittest import TestCase
from ambari_agent.Heartbeat import Heartbeat
from ambari_agent.ActionQueue import ActionQueue
from ambari_agent.AmbariConfig import AmbariConfig
import socket

class TestHeartbeat(TestCase):
  def test_build(self):
    actionQueue = ActionQueue(AmbariConfig().getConfig())
    heartbeat = Heartbeat(actionQueue)
    result = heartbeat.build(100)
    self.assertEqual(result['hostname'], socket.gethostname(), 'hostname mismatched.')
    self.assertEqual(result['responseId'], 100, 'responseId mismatched.')
    self.assertEqual(result['idle'], True, 'Heartbeat should indicate Agent is idle.')
    self.assertEqual(result['installScriptHash'], -1, 'installScriptHash should be -1.')
    self.assertEqual(result['firstContact'], True, 'firstContact should be True.')
    result = heartbeat.build(101)
    self.assertEqual(result['firstContact'], False, 'firstContact should be False.')
