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
from ambari_agent.LiveStatus import LiveStatus
from ambari_agent import AmbariConfig
import socket
import os
import time

class TestHeartbeat(TestCase):

  def setUp(self):
    testsPath = os.path.dirname(os.path.realpath(__file__))
    self.dictPath = testsPath + os.sep + '..' + os.sep + '..' + os.sep + 'main' + os.sep + 'python' + os.sep + 'ambari_agent' + os.sep + 'servicesToPidNames.dict'

  def test_build(self):
    AmbariConfig.config.set('services','serviceToPidMapFile', self.dictPath)
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
    self.assertEquals(len(result), 6)
    self.assertEquals(not heartbeat.reports, True, "Heartbeat should not contain task in progress")


  def test_heartbeat_with_status(self):
    AmbariConfig.config.set('services','serviceToPidMapFile', self.dictPath)
    actionQueue = ActionQueue(AmbariConfig.AmbariConfig().getConfig())
    heartbeat = Heartbeat(actionQueue)
    statusCommand = {
      "serviceName" : 'HDFS',
      "commandType" : "STATUS_COMMAND",
      "clusterName" : "",
      "componentName" : "DATANODE"
    }
    actionQueue.put(statusCommand)
    actionQueue.start()
    time.sleep(0.1)
    actionQueue.stop()
    actionQueue.join()
    result = heartbeat.build(101)
    self.assertEquals(len(result['componentStatus']) > 0, True, 'Heartbeat should contain status of HDFS components')

  def test_heartbeat_with_status_multiple(self):
    AmbariConfig.config.set('services','serviceToPidMapFile', self.dictPath)
    actionQueue = ActionQueue(AmbariConfig.AmbariConfig().getConfig())
    actionQueue.IDLE_SLEEP_TIME = 0.01
    heartbeat = Heartbeat(actionQueue)
    actionQueue.start()
    max_number_of_status_entries = 0
    for i in range(1,5):
      statusCommand = {
        "serviceName" : 'HDFS',
        "commandType" : "STATUS_COMMAND",
        "clusterName" : "",
        "componentName" : "DATANODE"
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
    AmbariConfig.config.set('services','serviceToPidMapFile', self.dictPath)
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
      'status' : 'IN_PROGRESS'
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
    pass
