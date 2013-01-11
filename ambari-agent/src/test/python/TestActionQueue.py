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
from ambari_agent.ActionQueue import ActionQueue
from ambari_agent.AmbariConfig import AmbariConfig
from ambari_agent.FileUtil import getFilePath
import os, errno, time, pprint, tempfile, threading

event = threading.Event()

class TestActionQueue(TestCase):
  def test_ActionQueueStartStop(self):
    actionQueue = ActionQueue(AmbariConfig().getConfig())
    actionQueue.IDLE_SLEEP_TIME = 0.01
    actionQueue.start()
    actionQueue.stop()
    actionQueue.join()
    self.assertEqual(actionQueue.stopped(), True, 'Action queue is not stopped.') 

#This feature is not yet implemented in ActionQueue
  def test_RetryAction(self):
    pass


  def test_command_in_progress(self):
    config = AmbariConfig().getConfig()
    tmpfile = tempfile.gettempdir()
    config.set('agent', 'prefix', tmpfile)
    actionQueue = ActionQueue(config)
    actionQueue.IDLE_SLEEP_TIME = 0.01
    executor_started_event = threading.Event()
    end_executor_event = threading.Event()
    actionQueue.executor = FakeExecutor(executor_started_event, end_executor_event)
    before_start_result = actionQueue.result()

    command = {
      'commandId': 17,
      'role' : "role",
      'taskId' : "taskId",
      'clusterName' : "clusterName",
      'serviceName' : "serviceName",
      'status' : 'IN_PROGRESS',
      'hostname' : "localhost.localdomain",
      'hostLevelParams': "hostLevelParams",
      'clusterHostInfo': "clusterHostInfo",
      'roleCommand': "roleCommand",
      'configurations': "configurations",
      'commandType': "EXECUTION_COMMAND"
    }
    actionQueue.put(command)

    actionQueue.start()
    executor_started_event.wait()
    #print ("ii: " + pprint.pformat(actionQueue.commandInProgress))
    in_progress_result = actionQueue.result()
    end_executor_event.set()
    actionQueue.stop()
    actionQueue.join()
    after_start_result = actionQueue.result()

    self.assertEquals(len(before_start_result['componentStatus']), 0)
    self.assertEquals(len(before_start_result['reports']), 0)

    self.assertEquals(len(in_progress_result['componentStatus']), 0)
    self.assertEquals(len(in_progress_result['reports']), 1)
    self.assertEquals(in_progress_result['reports'][0]['status'], "IN_PROGRESS")
    self.assertEquals(in_progress_result['reports'][0]['stdout'], "Dummy output")
    self.assertEquals(in_progress_result['reports'][0]['exitCode'], 777)
    self.assertEquals(in_progress_result['reports'][0]['stderr'], 'Dummy err')

    self.assertEquals(len(after_start_result['componentStatus']), 0)
    self.assertEquals(len(after_start_result['reports']), 1)
    self.assertEquals(after_start_result['reports'][0]['status'], "COMPLETED")
    self.assertEquals(after_start_result['reports'][0]['stdout'], "returned stdout")
    self.assertEquals(after_start_result['reports'][0]['exitCode'], 0)
    self.assertEquals(after_start_result['reports'][0]['stderr'], 'returned stderr')

    #print("tmpout: " + pprint.pformat(actionQueue.tmpdir))
    #print("before: " + pprint.pformat(before_start_result))
    #print("in_progress: " + pprint.pformat(in_progress_result))
    #print("after: " + pprint.pformat(after_start_result))


class FakeExecutor():

  def __init__(self, executor_started_event, end_executor_event):
    self.executor_started_event = executor_started_event
    self.end_executor_event = end_executor_event
    pass

  def runCommand(self, command, tmpoutpath, tmperrpath):
    tmpout= open(tmpoutpath, 'w')
    tmpout.write("Dummy output")
    tmpout.flush()

    tmperr= open(tmperrpath, 'w')
    tmperr.write("Dummy err")
    tmperr.flush()

    self.executor_started_event.set()
    self.end_executor_event.wait()
    return {
      "exitcode": 0,
      "stdout": "returned stdout",
      "stderr": "returned stderr"
    }