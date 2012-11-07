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
from ambari_agent import AmbariConfig
import socket
import os

class TestHeartbeat(TestCase):
  def test_build(self):
    testsPath = os.path.dirname(os.path.realpath(__file__))
    dictPath = testsPath + os.sep + '..' + os.sep + '..' + os.sep + 'main' + os.sep + 'python' + os.sep + 'ambari_agent' + os.sep + 'servicesToPidNames.dict'
    AmbariConfig.config.set('services','serviceToPidMapFile', dictPath)
    actionQueue = ActionQueue(AmbariConfig.AmbariConfig().getConfig())
    heartbeat = Heartbeat(actionQueue)
    result = heartbeat.build(100)
  
