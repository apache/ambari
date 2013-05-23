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

from unittest import TestCase
from ambari_agent.Heartbeat import Heartbeat
from ambari_agent.ActionQueue import ActionQueue
from ambari_agent.LiveStatus import LiveStatus
from ambari_agent import AmbariConfig
import socket
import os
import time
from mock.mock import patch, MagicMock, call
from ambari_agent.StackVersionsFileHandler import StackVersionsFileHandler
import StringIO
import sys

class TestHeartbeat(TestCase):

  def setUp(self):
    # disable stdout
    out = StringIO.StringIO()
    sys.stdout = out


  def tearDown(self):
    # enable stdout
    sys.stdout = sys.__stdout__


  def test_build(self):
    actionQueue = ActionQueue(AmbariConfig.AmbariConfig().getConfig())
    heartbeat = Heartbeat(actionQueue)
    result = heartbeat.build(100)
    print "Heartbeat: " + str(result)
    self.assertEquals(result['hostname'] != '', True, "hostname should not be empty")
    self.assertEquals(result['responseId'], 100)
    self.assertEquals(result['componentStatus'] is not None, True, "Heartbeat should contain componentStatus")
    self.assertEquals(result['reports'] is not None, True, "Heartbeat should contain reports")
    self.assertEquals(result['timestamp'] >= 1353679373880L, True)
    self.assertEquals(len(result['nodeStatus']), 2)
    self.assertEquals(result['nodeStatus']['cause'], "NONE")
    self.assertEquals(result['nodeStatus']['status'], "HEALTHY")
    # result may or may NOT have an agentEnv structure in it
    self.assertEquals((len(result) is 6) or (len(result) is 7), True)
    self.assertEquals(not heartbeat.reports, True, "Heartbeat should not contain task in progress")


  @patch.object(StackVersionsFileHandler, "read_stack_version")
  def test_heartbeat_with_status(self, read_stack_version_method):
    actionQueue = ActionQueue(AmbariConfig.AmbariConfig().getConfig())
    read_stack_version_method.return_value="1.3.0"
    heartbeat = Heartbeat(actionQueue)
    statusCommand = {
      "serviceName" : 'HDFS',
      "commandType" : "STATUS_COMMAND",
      "clusterName" : "",
      "componentName" : "DATANODE",
      'configurations':{'global' : {}}
    }
    actionQueue.put(statusCommand)
    actionQueue.start()
    time.sleep(0.1)
    actionQueue.stop()
    actionQueue.join()
    result = heartbeat.build(101)
    self.assertEquals(len(result['componentStatus']) > 0, True, 'Heartbeat should contain status of HDFS components')

  @patch.object(StackVersionsFileHandler, "read_stack_version")
  def test_heartbeat_with_status_multiple(self, read_stack_version_method):
    actionQueue = ActionQueue(AmbariConfig.AmbariConfig().getConfig())
    actionQueue.IDLE_SLEEP_TIME = 0.01
    read_stack_version_method.return_value="1.3.0"
    heartbeat = Heartbeat(actionQueue)
    actionQueue.start()
    max_number_of_status_entries = 0
    for i in range(1,5):
      statusCommand = {
        "serviceName" : 'HDFS',
        "commandType" : "STATUS_COMMAND",
        "clusterName" : "",
        "componentName" : "DATANODE",
        'configurations':{'global' : {}}
      }
      actionQueue.put(statusCommand)
      time.sleep(0.1)
      result = heartbeat.build(101)
      number_of_status_entries = len(result['componentStatus'])
#      print "Heartbeat with status: " + str(result) + " XXX " + str(number_of_status_entries)
      if max_number_of_status_entries < number_of_status_entries:
        max_number_of_status_entries = number_of_status_entries
    actionQueue.stop()
    actionQueue.join()

    NUMBER_OF_COMPONENTS = 1
    self.assertEquals(max_number_of_status_entries == NUMBER_OF_COMPONENTS, True)

  def test_heartbeat_with_task_in_progress(self):
    actionQueue = ActionQueue(AmbariConfig.AmbariConfig().getConfig())
    actionQueue.commandInProgress= {
      'role' : "role",
      'actionId' : "actionId",
      'taskId' : "taskId",
      'stdout' : "stdout",
      'clusterName' : "clusterName",
      'stderr' : 'none',
      'exitCode' : 777,
      'serviceName' : "serviceName",
      'status' : 'IN_PROGRESS',
      'configurations':{'global' : {}},
      'roleCommand' : 'START'
    }
    heartbeat = Heartbeat(actionQueue)
    result = heartbeat.build(100)
    #print "Heartbeat: " + str(result)
    self.assertEquals(len(result['reports']), 1)
    self.assertEquals(result['reports'][0]['role'], "role")
    self.assertEquals(result['reports'][0]['actionId'], "actionId")
    self.assertEquals(result['reports'][0]['taskId'], "taskId")
    self.assertEquals(result['reports'][0]['stdout'], "...")
    self.assertEquals(result['reports'][0]['clusterName'], "clusterName")
    self.assertEquals(result['reports'][0]['stderr'], "...")
    self.assertEquals(result['reports'][0]['exitCode'], 777)
    self.assertEquals(result['reports'][0]['serviceName'], "serviceName")
    self.assertEquals(result['reports'][0]['status'], "IN_PROGRESS")
    self.assertEquals(result['reports'][0]['roleCommand'], "START")
    pass
