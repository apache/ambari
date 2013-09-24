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
from ambari_agent.LiveStatus import LiveStatus
from ambari_agent.PuppetExecutor import PuppetExecutor
from ambari_agent.ActionQueue import ActionQueue
from ambari_agent.AmbariConfig import AmbariConfig
from ambari_agent.ActionDependencyManager import ActionDependencyManager
import os, errno, time, pprint, tempfile, threading
import StringIO
import sys
from threading import Thread

from mock.mock import patch, MagicMock, call
from ambari_agent.StackVersionsFileHandler import StackVersionsFileHandler
from ambari_agent.UpgradeExecutor import UpgradeExecutor


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


  @patch.object(ActionDependencyManager, "read_dependencies")
  @patch.object(ActionDependencyManager, "get_next_action_group")
  @patch.object(ActionQueue, "process_portion_of_actions")
  def test_ActionQueueStartStop(self, process_portion_of_actions_mock,
                          get_next_action_group_mock, read_dependencies_mock):
    actionQueue = ActionQueue(AmbariConfig().getConfig(), 'dummy_controller')
    actionQueue.start()
    time.sleep(0.1)
    actionQueue.stop()
    actionQueue.join()
    self.assertEqual(actionQueue.stopped(), True, 'Action queue is not stopped.')
    self.assertTrue(get_next_action_group_mock.call_count > 1)
    self.assertTrue(process_portion_of_actions_mock.call_count > 1)


  @patch.object(ActionDependencyManager, "read_dependencies")
  @patch.object(ActionQueue, "execute_command")
  @patch.object(ActionQueue, "execute_status_command")
  def test_process_portion_of_actions(self, execute_status_command_mock,
            executeCommand_mock, read_dependencies_mock):
    actionQueue = ActionQueue(AmbariConfig().getConfig(), 'dummy_controller')
    # Test execution of EXECUTION_COMMANDs
    max = 3
    actionQueue.MAX_CONCURRENT_ACTIONS = max
    unfreeze_flag = threading.Event()
    sync_lock = threading.RLock()
    stats = {
      'waiting_threads' : 0
    }
    def side_effect(self):
      with sync_lock: # Synchtonized to avoid race effects during test execution
        stats['waiting_threads'] += 1
      unfreeze_flag.wait()
    executeCommand_mock.side_effect = side_effect
    portion = [self.datanode_install_command,
               self.namenode_install_command,
               self.snamenode_install_command,
               self.nagios_install_command,
               self.hbase_install_command]

    # We call method in a separate thread
    action_thread = Thread(target =  actionQueue.process_portion_of_actions, args = (portion, ))
    action_thread.start()
    # Now we wait to check that only MAX_CONCURRENT_ACTIONS threads are running
    while stats['waiting_threads'] != max:
      time.sleep(0.1)
    self.assertEqual(stats['waiting_threads'], max)
    # unfreezing waiting threads
    unfreeze_flag.set()
    # wait until all threads are finished
    action_thread.join()
    self.assertTrue(executeCommand_mock.call_count == 5)
    self.assertFalse(execute_status_command_mock.called)
    executeCommand_mock.reset_mock()
    execute_status_command_mock.reset_mock()

    # Test execution of STATUS_COMMANDs
    n = 5
    portion = []
    for i in range(0, n):
      status_command = {
        'componentName': 'DATANODE',
        'commandType': 'STATUS_COMMAND',
      }
      portion.append(status_command)
    actionQueue.process_portion_of_actions(portion)
    self.assertTrue(execute_status_command_mock.call_count == n)
    self.assertFalse(executeCommand_mock.called)

    # Test execution of unknown command
    unknown_command = {
      'commandType': 'WRONG_COMMAND',
    }
    portion = [unknown_command]
    actionQueue.process_portion_of_actions(portion)
    # no exception expected
    pass


  @patch("traceback.print_exc")
  @patch.object(ActionDependencyManager, "read_dependencies")
  @patch.object(ActionQueue, "execute_command")
  def test_execute_command_safely(self, execute_command_mock,
                                  read_dependencies_mock, print_exc_mock):
    actionQueue = ActionQueue(AmbariConfig().getConfig(), 'dummy_controller')
    # Try normal execution
    actionQueue.execute_command_safely('command')
    # Try exception ro check proper logging
    def side_effect(self):
      raise Exception("TerribleException")
    execute_command_mock.side_effect = side_effect
    actionQueue.execute_command_safely('command')
    self.assertTrue(print_exc_mock.called)


  @patch("__builtin__.open")
  @patch.object(ActionQueue, "status_update_callback")
  @patch.object(ActionDependencyManager, "read_dependencies")
  def test_execute_command(self, read_dependencies_mock,
                           status_update_callback_mock, open_mock):
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
        with patch.object(UpgradeExecutor, "perform_stack_upgrade") \
              as perform_stack_upgrade_mock:
          runCommand_mock.side_effect = side_effect
          perform_stack_upgrade_mock.side_effect = side_effect
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
  @patch.object(ActionDependencyManager, "read_dependencies")
  @patch.object(ActionQueue, "execute_command")
  @patch.object(LiveStatus, "build")
  def test_execute_status_command(self, build_mock, execute_command_mock,
                                  read_dependencies_mock, read_stack_version_mock,
                                  status_update_callback):
    actionQueue = ActionQueue(AmbariConfig().getConfig(), 'dummy_controller')
    build_mock.return_value = "dummy report"
    # Try normal execution
    actionQueue.execute_status_command(self.status_command)
    report = actionQueue.result()
    expected = 'dummy report'
    self.assertEqual(len(report['componentStatus']), 1)
    self.assertEqual(report['componentStatus'][0], expected)