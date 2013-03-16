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
from ambari_agent.UpgradeExecutor import UpgradeExecutor
from ambari_agent.StackVersionsFileHandler import StackVersionsFileHandler
import os, errno, time, pprint, tempfile, threading
import TestStackVersionsFileHandler

from mock.mock import patch, MagicMock, call

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
    actionQueue.puppetExecutor = FakeExecutor(executor_started_event, end_executor_event)
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
      'commandType': "EXECUTION_COMMAND",
      'configurations':{'global' : {}}
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

  @patch.object(ActionQueue, "executeCommand")
  @patch.object(ActionQueue, "stopped")
  def test_upgradeCommand_dispatching(self, stopped_method, executeCommand_method):
    queue = ActionQueue(config = MagicMock())
    command = {
      'commandId': 17,
      'role' : "role",
      'taskId' : "taskId",
      'clusterName' : "clusterName",
      'serviceName' : "serviceName",
      'roleCommand' : 'UPGRADE',
      'hostname' : "localhost.localdomain",
      'hostLevelParams': "hostLevelParams",
      'clusterHostInfo': "clusterHostInfo",
      'configurations': "configurations",
      'commandType': "EXECUTION_COMMAND",
      'configurations':{'global' : {}},
      'roleParams': {},
      'commandParams' :	{
        'source_stack_version' : 'HDP-1.2.1',
        'target_stack_version' : 'HDP-1.3.0'
      }
    }
    result = [{
      'exitcode' : 0,
      'stdout'   : 'abc',
      'stderr'   : 'def'
    }]
    executeCommand_method.return_value = result
    stopped_method.side_effect = [False, False, True, True, True]
    queue.stopped = stopped_method
    queue.IDLE_SLEEP_TIME = 0.001
    queue.put(command)
    queue.run()
    self.assertTrue(executeCommand_method.called)
    self.assertEquals(queue.resultQueue.qsize(), 1)
    returned_result = queue.resultQueue.get()
    self.assertTrue(returned_result[1] is result[0])


  @patch.object(UpgradeExecutor, "perform_stack_upgrade")
  def test_upgradeCommand_executeCommand(self, perform_stack_upgrade_method):
    queue = ActionQueue(config = MagicMock())
    command = {
      'commandId': 17,
      'role' : "role",
      'taskId' : "taskId",
      'clusterName' : "clusterName",
      'serviceName' : "serviceName",
      'roleCommand' : 'UPGRADE',
      'hostname' : "localhost.localdomain",
      'hostLevelParams': "hostLevelParams",
      'clusterHostInfo': "clusterHostInfo",
      'configurations': "configurations",
      'commandType': "EXECUTION_COMMAND",
      'configurations':{'global' : {}},
      'roleParams': {},
      'commandParams' :	{
        'source_stack_version' : 'HDP-1.2.1',
        'target_stack_version' : 'HDP-1.3.0'
      }
    }
    perform_stack_upgrade_method.return_value = {
      'exitcode' : 0,
      'stdout'   : 'abc',
      'stderr'   : 'def'
    }
    result = queue.executeCommand(command)
    expected_result = [{'actionId': 17,
                        'clusterName': 'clusterName',
                        'exitCode': 0,
                        'role': 'role',
                        'serviceName': 'serviceName',
                        'status': 'COMPLETED',
                        'stderr': 'def',
                        'stdout': 'abc',
                        'taskId': 'taskId'}]
    self.assertEquals(result, expected_result)


  @patch.object(StackVersionsFileHandler, "read_stack_version")
  @patch.object(ActionQueue, "stopped")
  def test_status_command_without_globals_section(self, stopped_method,
                                                  read_stack_version_method):
    config = AmbariConfig().getConfig()
    config.set('agent', 'prefix', TestStackVersionsFileHandler.dummyVersionsFile)
    queue = ActionQueue(config)
    statusCommand = {
      "serviceName" : 'HDFS',
      "commandType" : "STATUS_COMMAND",
      "clusterName" : "",
      "componentName" : "DATANODE",
      'configurations':{}
    }
    queue.stopped = stopped_method
    stopped_method.side_effect = [False, False, True, True, True]
    read_stack_version_method.return_value="1.3.0"
    queue.IDLE_SLEEP_TIME = 0.001
    queue.put(statusCommand)
    queue.run()
    returned_result = queue.resultQueue.get()
    returned_result[1]['status'] = 'INSTALLED' # Patch live value
    self.assertEquals(returned_result, ('STATUS_COMMAND',
                                        {'clusterName': '',
                                         'componentName': 'DATANODE',
                                         'msg': '',
                                         'serviceName': 'HDFS',
                                         'stackVersion': '1.3.0',
                                         'status': 'INSTALLED'}))


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
