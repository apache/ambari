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
from Queue import Queue

from unittest import TestCase
from ambari_agent.LiveStatus import LiveStatus
from ambari_agent.PuppetExecutor import PuppetExecutor
from ambari_agent.ActionQueue import ActionQueue
from ambari_agent.AmbariConfig import AmbariConfig
import os, errno, time, pprint, tempfile, threading
import StringIO
import sys
from threading import Thread

from mock.mock import patch, MagicMock, call
from ambari_agent.StackVersionsFileHandler import StackVersionsFileHandler
from ambari_agent.CustomServiceOrchestrator import CustomServiceOrchestrator


class TestActionQueue(TestCase):

  def setUp(self):
    out = StringIO.StringIO()
    sys.stdout = out
    # save original open() method for later use
    self.original_open = open


  def tearDown(self):
    sys.stdout = sys.__stdout__

  datanode_install_command = {
    'commandType': 'EXECUTION_COMMAND',
    'role': u'DATANODE',
    'roleCommand': u'INSTALL',
    'commandId': '1-1',
    'taskId': 3,
    'clusterName': u'cc',
    'serviceName': u'HDFS',
    'configurations':{'global' : {}},
    'configurationTags':{'global' : { 'tag': 'v1' }}
  }

  datanode_upgrade_command = {
      'commandId': 17,
      'role' : "role",
      'taskId' : "taskId",
      'clusterName' : "clusterName",
      'serviceName' : "serviceName",
      'roleCommand' : 'UPGRADE',
      'hostname' : "localhost.localdomain",
      'hostLevelParams': "hostLevelParams",
      'clusterHostInfo': "clusterHostInfo",
      'commandType': "EXECUTION_COMMAND",
      'configurations':{'global' : {}},
      'roleParams': {},
      'commandParams' :	{
        'source_stack_version' : 'HDP-1.2.1',
        'target_stack_version' : 'HDP-1.3.0'
      }
    }

  namenode_install_command = {
    'commandType': 'EXECUTION_COMMAND',
    'role': u'NAMENODE',
    'roleCommand': u'INSTALL',
    'commandId': '1-1',
    'taskId': 4,
    'clusterName': u'cc',
    'serviceName': u'HDFS',
    }

  snamenode_install_command = {
    'commandType': 'EXECUTION_COMMAND',
    'role': u'SECONDARY_NAMENODE',
    'roleCommand': u'INSTALL',
    'commandId': '1-1',
    'taskId': 5,
    'clusterName': u'cc',
    'serviceName': u'HDFS',
    }

  nagios_install_command = {
    'commandType': 'EXECUTION_COMMAND',
    'role': u'NAGIOS',
    'roleCommand': u'INSTALL',
    'commandId': '1-1',
    'taskId': 6,
    'clusterName': u'cc',
    'serviceName': u'HDFS',
    }

  hbase_install_command = {
    'commandType': 'EXECUTION_COMMAND',
    'role': u'HBASE',
    'roleCommand': u'INSTALL',
    'commandId': '1-1',
    'taskId': 7,
    'clusterName': u'cc',
    'serviceName': u'HDFS',
    }

  status_command = {
    "serviceName" : 'HDFS',
    "commandType" : "STATUS_COMMAND",
    "clusterName" : "",
    "componentName" : "DATANODE",
    'configurations':{}
  }


  @patch.object(ActionQueue, "process_command")
  @patch.object(Queue, "get")
  def test_ActionQueueStartStop(self, get_mock, process_command_mock):
    actionQueue = ActionQueue(AmbariConfig().getConfig(), 'dummy_controller')
    actionQueue.start()
    time.sleep(0.1)
    actionQueue.stop()
    actionQueue.join()
    self.assertEqual(actionQueue.stopped(), True, 'Action queue is not stopped.')
    self.assertTrue(process_command_mock.call_count > 1)


  @patch("traceback.print_exc")
  @patch.object(ActionQueue, "execute_command")
  @patch.object(ActionQueue, "execute_status_command")
  def test_process_command(self, execute_status_command_mock,
                           execute_command_mock, print_exc_mock):
    actionQueue = ActionQueue(AmbariConfig().getConfig(), 'dummy_controller')
    execution_command = {
      'commandType' : ActionQueue.EXECUTION_COMMAND,
    }
    status_command = {
      'commandType' : ActionQueue.STATUS_COMMAND,
    }
    wrong_command = {
      'commandType' : "SOME_WRONG_COMMAND",
    }
    # Try wrong command
    actionQueue.process_command(wrong_command)
    self.assertFalse(execute_command_mock.called)
    self.assertFalse(execute_status_command_mock.called)
    self.assertFalse(print_exc_mock.called)

    execute_command_mock.reset_mock()
    execute_status_command_mock.reset_mock()
    print_exc_mock.reset_mock()
    # Try normal execution
    actionQueue.process_command(execution_command)
    self.assertTrue(execute_command_mock.called)
    self.assertFalse(execute_status_command_mock.called)
    self.assertFalse(print_exc_mock.called)

    execute_command_mock.reset_mock()
    execute_status_command_mock.reset_mock()
    print_exc_mock.reset_mock()

    actionQueue.process_command(status_command)
    self.assertFalse(execute_command_mock.called)
    self.assertTrue(execute_status_command_mock.called)
    self.assertFalse(print_exc_mock.called)

    execute_command_mock.reset_mock()
    execute_status_command_mock.reset_mock()
    print_exc_mock.reset_mock()

    # Try exception to check proper logging
    def side_effect(self):
      raise Exception("TerribleException")
    execute_command_mock.side_effect = side_effect
    actionQueue.process_command(execution_command)
    self.assertTrue(print_exc_mock.called)

    print_exc_mock.reset_mock()

    execute_status_command_mock.side_effect = side_effect
    actionQueue.process_command(execution_command)
    self.assertTrue(print_exc_mock.called)



  @patch("__builtin__.open")
  @patch.object(ActionQueue, "status_update_callback")
  def test_execute_command(self, status_update_callback_mock, open_mock):
    # Make file read calls visible
    def open_side_effect(file, mode):
      if mode == 'r':
        file_mock = MagicMock()
        file_mock.read.return_value = "Read from " + str(file)
        return file_mock
      else:
        return self.original_open(file, mode)
    open_mock.side_effect = open_side_effect

    config = AmbariConfig().getConfig()
    tempdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tempdir)
    actionQueue = ActionQueue(config, 'dummy_controller')
    unfreeze_flag = threading.Event()
    puppet_execution_result_dict = {
      'stdout': 'out',
      'stderr': 'stderr',
      }
    def side_effect(command, tmpoutfile, tmperrfile):
      unfreeze_flag.wait()
      return puppet_execution_result_dict
    def patched_aq_execute_command(command):
      # We have to perform patching for separate thread in the same thread
      with patch.object(PuppetExecutor, "runCommand") as runCommand_mock:
          runCommand_mock.side_effect = side_effect
          actionQueue.execute_command(command)
    ### Test install/start/stop command ###
    ## Test successful execution with configuration tags
    puppet_execution_result_dict['status'] = 'COMPLETE'
    puppet_execution_result_dict['exitcode'] = 0
    # We call method in a separate thread
    execution_thread = Thread(target = patched_aq_execute_command ,
                              args = (self.datanode_install_command, ))
    execution_thread.start()
    #  check in progress report
    # wait until ready
    while True:
      time.sleep(0.1)
      report = actionQueue.result()
      if len(report['reports']) != 0:
        break
    expected = {'status': 'IN_PROGRESS',
                'stderr': 'Read from {0}/errors-3.txt'.format(tempdir),
                'stdout': 'Read from {0}/output-3.txt'.format(tempdir),
                'clusterName': u'cc',
                'roleCommand': u'INSTALL',
                'serviceName': u'HDFS',
                'role': u'DATANODE',
                'actionId': '1-1',
                'taskId': 3,
                'exitCode': 777}
    self.assertEqual(report['reports'][0], expected)
    # Continue command execution
    unfreeze_flag.set()
    # wait until ready
    while report['reports'][0]['status'] == 'IN_PROGRESS':
      time.sleep(0.1)
      report = actionQueue.result()
    # check report
    configname = os.path.join(tempdir, 'config.json')
    expected = {'status': 'COMPLETED',
                'stderr': 'stderr',
                'stdout': 'out',
                'clusterName': u'cc',
                'configurationTags': {'global': {'tag': 'v1'}},
                'roleCommand': u'INSTALL',
                'serviceName': u'HDFS',
                'role': u'DATANODE',
                'actionId': '1-1',
                'taskId': 3,
                'exitCode': 0}
    self.assertEqual(len(report['reports']), 1)
    self.assertEqual(report['reports'][0], expected)
    self.assertTrue(os.path.isfile(configname))
    # Check that we had 2 status update calls ( IN_PROGRESS and COMPLETE)
    self.assertEqual(status_update_callback_mock.call_count, 2)
    os.remove(configname)

    # now should not have reports (read complete/failed reports are deleted)
    report = actionQueue.result()
    self.assertEqual(len(report['reports']), 0)

    ## Test failed execution
    puppet_execution_result_dict['status'] = 'FAILED'
    puppet_execution_result_dict['exitcode'] = 13
    # We call method in a separate thread
    execution_thread = Thread(target = patched_aq_execute_command ,
                              args = (self.datanode_install_command, ))
    execution_thread.start()
    unfreeze_flag.set()
    #  check in progress report
    # wait until ready
    report = actionQueue.result()
    while len(report['reports']) == 0 or \
                    report['reports'][0]['status'] == 'IN_PROGRESS':
      time.sleep(0.1)
      report = actionQueue.result()
      # check report
    expected = {'status': 'FAILED',
                'stderr': 'stderr',
                'stdout': 'out',
                'clusterName': u'cc',
                'roleCommand': u'INSTALL',
                'serviceName': u'HDFS',
                'role': u'DATANODE',
                'actionId': '1-1',
                'taskId': 3,
                'exitCode': 13}
    self.assertEqual(len(report['reports']), 1)
    self.assertEqual(report['reports'][0], expected)

    # now should not have reports (read complete/failed reports are deleted)
    report = actionQueue.result()
    self.assertEqual(len(report['reports']), 0)

    ### Test upgrade command ###
    puppet_execution_result_dict['status'] = 'COMPLETE'
    puppet_execution_result_dict['exitcode'] = 0
    execution_thread = Thread(target = patched_aq_execute_command ,
                              args = (self.datanode_upgrade_command, ))
    execution_thread.start()
    unfreeze_flag.set()
    # wait until ready
    report = actionQueue.result()
    while len(report['reports']) == 0 or \
                    report['reports'][0]['status'] == 'IN_PROGRESS':
      time.sleep(0.1)
      report = actionQueue.result()
    # check report
    expected = {'status': 'COMPLETED',
                'stderr': 'stderr',
                'stdout': 'out',
                'clusterName': 'clusterName',
                'roleCommand': 'UPGRADE',
                'serviceName': 'serviceName',
                'role': 'role',
                'actionId': 17,
                'taskId': 'taskId',
                'exitCode': 0}
    self.assertEqual(len(report['reports']), 1)
    self.assertEqual(report['reports'][0], expected)

    # now should not have reports (read complete/failed reports are deleted)
    report = actionQueue.result()
    self.assertEqual(len(report['reports']), 0)


  @patch.object(ActionQueue, "status_update_callback")
  @patch.object(StackVersionsFileHandler, "read_stack_version")
  @patch.object(ActionQueue, "execute_command")
  @patch.object(LiveStatus, "build")
  def test_execute_status_command(self, build_mock, execute_command_mock,
                                  read_stack_version_mock,
                                  status_update_callback):
    actionQueue = ActionQueue(AmbariConfig().getConfig(), 'dummy_controller')
    build_mock.return_value = "dummy report"
    # Try normal execution
    actionQueue.execute_status_command(self.status_command)
    report = actionQueue.result()
    expected = 'dummy report'
    self.assertEqual(len(report['componentStatus']), 1)
    self.assertEqual(report['componentStatus'][0], expected)


  def test_determine_command_format_version(self):
    v1_command = {
      'commandParams': {
        'schema_version': '1.0'
      }
    }
    v2_command = {
      'commandParams': {
        'schema_version': '2.0'
      }
    }
    current_command = {
      # Absent 'commandParams' section
    }
    actionQueue = ActionQueue(AmbariConfig().getConfig(), 'dummy_controller')
    self.assertEqual(actionQueue.determine_command_format_version(v1_command),
                     ActionQueue.COMMAND_FORMAT_V1)
    self.assertEqual(actionQueue.determine_command_format_version(v2_command),
                     ActionQueue.COMMAND_FORMAT_V2)
    self.assertEqual(actionQueue.determine_command_format_version(current_command),
                     ActionQueue.COMMAND_FORMAT_V1)


  @patch.object(ActionQueue, "determine_command_format_version")
  @patch("__builtin__.open")
  @patch.object(PuppetExecutor, "runCommand")
  @patch.object(CustomServiceOrchestrator, "runCommand")
  @patch.object(ActionQueue, "status_update_callback")
  def test_command_execution_depending_on_command_format(self,
                                status_update_callback_mock,
                                custom_ex_runCommand_mock,
                                puppet_runCommand_mock, open_mock,
                                determine_command_format_version_mock):
    actionQueue = ActionQueue(AmbariConfig().getConfig(), 'dummy_controller')
    ret = {
      'stdout' : '',
      'stderr' : '',
      'exitcode': 1,
      }
    puppet_runCommand_mock.return_value = ret
    determine_command_format_version_mock.return_value = \
                                  ActionQueue.COMMAND_FORMAT_V1
    actionQueue.execute_command(self.datanode_install_command)
    self.assertTrue(puppet_runCommand_mock.called)
    self.assertFalse(custom_ex_runCommand_mock.called)

    puppet_runCommand_mock.reset_mock()

    custom_ex_runCommand_mock.return_value = ret
    determine_command_format_version_mock.return_value = \
      ActionQueue.COMMAND_FORMAT_V2
    actionQueue.execute_command(self.datanode_install_command)
    self.assertFalse(puppet_runCommand_mock.called)
    self.assertTrue(custom_ex_runCommand_mock.called)
