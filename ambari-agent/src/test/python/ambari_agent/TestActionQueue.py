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
from Queue import Queue

from unittest import TestCase
from ambari_agent.LiveStatus import LiveStatus
from ambari_agent.ActionQueue import ActionQueue
from ambari_agent.AmbariConfig import AmbariConfig
import os, errno, time, pprint, tempfile, threading
import sys
from threading import Thread
import copy
import signal

from mock.mock import patch, MagicMock, call
from ambari_agent.CustomServiceOrchestrator import CustomServiceOrchestrator
from ambari_agent.PythonExecutor import PythonExecutor
from ambari_agent.ActualConfigHandler import ActualConfigHandler
from ambari_agent.RecoveryManager import RecoveryManager
from ambari_commons import OSCheck
from only_for_platform import not_for_platform, os_distro_value, PLATFORM_WINDOWS, PLATFORM_LINUX

import logging

class TestActionQueue(TestCase):
  def setUp(self):
    # save original open() method for later use
    self.original_open = open


  def tearDown(self):
    sys.stdout = sys.__stdout__

  logger = logging.getLogger()

  datanode_install_command = {
    'commandType': 'EXECUTION_COMMAND',
    'role': u'DATANODE',
    'roleCommand': u'INSTALL',
    'commandId': '1-1',
    'taskId': 3,
    'clusterName': u'cc',
    'serviceName': u'HDFS',
    'hostLevelParams': {},
    'configurations':{'global' : {}},
    'configurationTags':{'global' : { 'tag': 'v1' }},
    'commandParams': {
      'command_retry_enabled': 'true'
    }
  }

  datanode_install_no_retry_command = {
    'commandType': 'EXECUTION_COMMAND',
    'role': u'DATANODE',
    'roleCommand': u'INSTALL',
    'commandId': '1-1',
    'taskId': 3,
    'clusterName': u'cc',
    'serviceName': u'HDFS',
    'hostLevelParams': {},
    'configurations':{'global' : {}},
    'configurationTags':{'global' : { 'tag': 'v1' }},
    'commandParams': {
      'command_retry_enabled': 'false'
    }
  }

  datanode_auto_start_command = {
    'commandType': 'AUTO_EXECUTION_COMMAND',
    'role': u'DATANODE',
    'roleCommand': u'START',
    'commandId': '1-1',
    'taskId': 3,
    'clusterName': u'cc',
    'serviceName': u'HDFS',
    'hostLevelParams': {},
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
      'hostLevelParams': {},
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
    'hostLevelParams': {}
    }

  snamenode_install_command = {
    'commandType': 'EXECUTION_COMMAND',
    'role': u'SECONDARY_NAMENODE',
    'roleCommand': u'INSTALL',
    'commandId': '1-1',
    'taskId': 5,
    'clusterName': u'cc',
    'serviceName': u'HDFS',
    'hostLevelParams': {}
    }

  hbase_install_command = {
    'commandType': 'EXECUTION_COMMAND',
    'role': u'HBASE',
    'roleCommand': u'INSTALL',
    'commandId': '1-1',
    'taskId': 7,
    'clusterName': u'cc',
    'serviceName': u'HDFS',
    'hostLevelParams': {},
    'commandParams': {
      'command_retry_enabled': 'true'
    }
  }

  status_command = {
    "serviceName" : 'HDFS',
    "commandType" : "STATUS_COMMAND",
    "clusterName" : "",
    "componentName" : "DATANODE",
    'configurations':{},
    'hostLevelParams': {}
  }

  datanode_restart_command = {
    'commandType': 'EXECUTION_COMMAND',
    'role': u'DATANODE',
    'roleCommand': u'CUSTOM_COMMAND',
    'commandId': '1-1',
    'taskId': 9,
    'clusterName': u'cc',
    'serviceName': u'HDFS',
    'configurations':{'global' : {}},
    'configurationTags':{'global' : { 'tag': 'v123' }},
    'hostLevelParams':{'custom_command': 'RESTART', 'clientsToUpdateConfigs': []}
  }

  datanode_restart_command_no_logging = {
    'commandType': 'EXECUTION_COMMAND',
    'role': u'DATANODE',
    'roleCommand': u'CUSTOM_COMMAND',
    'commandId': '1-1',
    'taskId': 9,
    'clusterName': u'cc',
    'serviceName': u'HDFS',
    'configurations': {'global': {}},
    'configurationTags': {'global': {'tag': 'v123'}},
    'commandParams': {
      'log_output': 'false'
    },
    'hostLevelParams': {'custom_command': 'RESTART', 'clientsToUpdateConfigs': []}
  }

  datanode_restart_command_no_clients_update = {
    'commandType': 'EXECUTION_COMMAND',
    'role': u'DATANODE',
    'roleCommand': u'CUSTOM_COMMAND',
    'commandId': '1-1',
    'taskId': 9,
    'clusterName': u'cc',
    'serviceName': u'HDFS',
    'configurations':{'global' : {}},
    'configurationTags':{'global' : { 'tag': 'v123' }},
    'hostLevelParams':{'custom_command': 'RESTART'}
  }

  datanode_start_custom_command = {
    'commandType': 'EXECUTION_COMMAND',
    'role': u'DATANODE',
    'roleCommand': u'CUSTOM_COMMAND',
    'commandId': '1-1',
    'taskId': 9,
    'clusterName': u'cc',
    'serviceName': u'HDFS',
    'configurations':{'global' : {}},
    'configurationTags':{'global' : { 'tag': 'v123' }},
    'hostLevelParams':{'custom_command': 'START'}
  }

  yarn_refresh_queues_custom_command = {
    'commandType': 'EXECUTION_COMMAND',
    'role': u'RESOURCEMANAGER',
    'roleCommand': u'CUSTOM_COMMAND',
    'commandId': '1-1',
    'taskId': 9,
    'clusterName': u'cc',
    'serviceName': u'YARN',
    'commandParams' : {'forceRefreshConfigTags' : 'capacity-scheduler'},
    'configurations':{'global' : {}},
    'configurationTags':{'global' : { 'tag': 'v123' }, 'capacity-scheduler' : {'tag': 'v123'}},
    'hostLevelParams':{'custom_command': 'REFRESHQUEUES'}
  }

  status_command_for_alerts = {
    "serviceName" : 'FLUME',
    "commandType" : "STATUS_COMMAND",
    "clusterName" : "",
    "componentName" : "FLUME_HANDLER",
    'configurations':{},
    'hostLevelParams': {}
  }

  retryable_command = {
    'commandType': 'EXECUTION_COMMAND',
    'role': 'NAMENODE',
    'roleCommand': 'INSTALL',
    'commandId': '1-1',
    'taskId': 19,
    'clusterName': 'c1',
    'serviceName': 'HDFS',
    'configurations':{'global' : {}},
    'configurationTags':{'global' : { 'tag': 'v123' }},
    'commandParams' :  {
      'script_type' : 'PYTHON',
      'script' : 'script.py',
      'command_timeout' : '600',
      'jdk_location' : '.',
      'service_package_folder' : '.',
      'command_retry_enabled' : 'true',
      'max_duration_for_retries' : '5'
    },
    'hostLevelParams' : {}
  }

  background_command = {
    'commandType': 'BACKGROUND_EXECUTION_COMMAND',
    'role': 'NAMENODE',
    'roleCommand': 'CUSTOM_COMMAND',
    'commandId': '1-1',
    'taskId': 19,
    'clusterName': 'c1',
    'serviceName': 'HDFS',
    'configurations':{'global' : {}},
    'configurationTags':{'global' : { 'tag': 'v123' }},
    'hostLevelParams':{'custom_command': 'REBALANCE_HDFS'},
    'commandParams' :  {
      'script_type' : 'PYTHON',
      'script' : 'script.py',
      'command_timeout' : '600',
      'jdk_location' : '.',
      'service_package_folder' : '.'
      }
  }
  cancel_background_command = {
    'commandType': 'EXECUTION_COMMAND',
    'role': 'NAMENODE',
    'roleCommand': 'ACTIONEXECUTE',
    'commandId': '1-1',
    'taskId': 20,
    'clusterName': 'c1',
    'serviceName': 'HDFS',
    'configurations':{'global' : {}},
    'configurationTags':{'global' : {}},
    'hostLevelParams':{},
    'commandParams' :  {
      'script_type' : 'PYTHON',
      'script' : 'cancel_background_task.py',
      'before_system_hook_function' : 'fetch_bg_pid_by_taskid',
      'jdk_location' : '.',
      'command_timeout' : '600',
      'service_package_folder' : '.',
      'cancel_policy': 'SIGKILL',
      'cancel_task_id': "19",
      }
  }


  @patch.object(AmbariConfig, "get_parallel_exec_option")
  @patch.object(ActionQueue, "process_command")
  @patch.object(Queue, "get")
  @patch.object(CustomServiceOrchestrator, "__init__")
  def test_ActionQueueStartStop(self, CustomServiceOrchestrator_mock,
                                get_mock, process_command_mock, get_parallel_exec_option_mock):
    CustomServiceOrchestrator_mock.return_value = None
    dummy_controller = MagicMock()
    config = MagicMock()
    get_parallel_exec_option_mock.return_value = 0
    config.get_parallel_exec_option = get_parallel_exec_option_mock
    actionQueue = ActionQueue(config, dummy_controller)
    actionQueue.start()
    time.sleep(0.1)
    actionQueue.stop()
    actionQueue.join()
    self.assertEqual(actionQueue.stopped(), True, 'Action queue is not stopped.')
    self.assertTrue(process_command_mock.call_count > 1)


  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch("logging.RootLogger.exception")
  @patch.object(ActionQueue, "execute_command")
  def test_process_command(self, execute_command_mock, log_exc_mock):
    dummy_controller = MagicMock()
    config = AmbariConfig()
    config.set('agent', 'tolerate_download_failures', "true")
    actionQueue = ActionQueue(config, dummy_controller)
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
    self.assertFalse(log_exc_mock.called)

    execute_command_mock.reset_mock()
    log_exc_mock.reset_mock()
    # Try normal execution
    actionQueue.process_command(execution_command)
    self.assertTrue(execute_command_mock.called)
    self.assertFalse(log_exc_mock.called)

    execute_command_mock.reset_mock()
    log_exc_mock.reset_mock()

    execute_command_mock.reset_mock()
    log_exc_mock.reset_mock()

    # Try exception to check proper logging
    def side_effect(self):
      raise Exception("TerribleException")
    execute_command_mock.side_effect = side_effect
    actionQueue.process_command(execution_command)
    self.assertTrue(log_exc_mock.called)

    log_exc_mock.reset_mock()

    actionQueue.process_command(execution_command)
    self.assertTrue(log_exc_mock.called)

  @patch.object(ActionQueue, "log_command_output")
  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(CustomServiceOrchestrator, "runCommand")
  @patch("CommandStatusDict.CommandStatusDict")
  @patch.object(ActionQueue, "status_update_callback")
  def test_log_execution_commands(self, status_update_callback_mock,
                                  command_status_dict_mock,
                                  cso_runCommand_mock, mock_log_command_output):
    custom_service_orchestrator_execution_result_dict = {
        'stdout': 'out',
        'stderr': 'stderr',
        'structuredOut' : '',
        'exitcode' : 0
    }
    cso_runCommand_mock.return_value = custom_service_orchestrator_execution_result_dict

    config = AmbariConfig()
    tempdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tempdir)
    config.set('agent', 'cache_dir', "/var/lib/ambari-agent/cache")
    config.set('agent', 'tolerate_download_failures', "true")
    config.set('logging', 'log_command_executes', 1)
    dummy_controller = MagicMock()
    actionQueue = ActionQueue(config, dummy_controller)
    actionQueue.execute_command(self.datanode_restart_command)
    report = actionQueue.result()
    expected = {'status': 'COMPLETED',
                'configurationTags': {'global': {'tag': 'v123'}},
                'stderr': 'stderr',
                'stdout': 'out\n\nCommand completed successfully!\n',
                'clusterName': u'cc',
                'structuredOut': '""',
                'roleCommand': u'CUSTOM_COMMAND',
                'serviceName': u'HDFS',
                'role': u'DATANODE',
                'actionId': '1-1',
                'taskId': 9,
                'customCommand': 'RESTART',
                'exitCode': 0}
    # Agent caches configurationTags if custom_command RESTART completed
    mock_log_command_output.assert_has_calls([call("out\n\nCommand completed successfully!\n", "9"), call("stderr", "9")], any_order=True)
    self.assertEqual(len(report['reports']), 1)
    self.assertEqual(expected, report['reports'][0])


  @patch.object(ActionQueue, "log_command_output")
  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  @patch.object(CustomServiceOrchestrator, "runCommand")
  @patch("CommandStatusDict.CommandStatusDict")
  @patch.object(ActionQueue, "status_update_callback")
  def test_do_not_log_execution_commands(self, status_update_callback_mock,
                                         command_status_dict_mock,
                                         cso_runCommand_mock, mock_log_command_output):
    custom_service_orchestrator_execution_result_dict = {
      'stdout': 'out',
      'stderr': 'stderr',
      'structuredOut': '',
      'exitcode': 0
    }
    cso_runCommand_mock.return_value = custom_service_orchestrator_execution_result_dict

    config = AmbariConfig()
    tempdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tempdir)
    config.set('agent', 'cache_dir', "/var/lib/ambari-agent/cache")
    config.set('agent', 'tolerate_download_failures', "true")
    config.set('logging', 'log_command_executes', 1)
    dummy_controller = MagicMock()
    actionQueue = ActionQueue(config, dummy_controller)
    actionQueue.execute_command(self.datanode_restart_command_no_logging)
    report = actionQueue.result()
    expected = {'status': 'COMPLETED',
                'configurationTags': {'global': {'tag': 'v123'}},
                'stderr': 'stderr',
                'stdout': 'out\n\nCommand completed successfully!\n',
                'clusterName': u'cc',
                'structuredOut': '""',
                'roleCommand': u'CUSTOM_COMMAND',
                'serviceName': u'HDFS',
                'role': u'DATANODE',
                'actionId': '1-1',
                'taskId': 9,
                'customCommand': 'RESTART',
                'exitCode': 0}
    # Agent caches configurationTags if custom_command RESTART completed
    mock_log_command_output.assert_not_called(
      [call("out\n\nCommand completed successfully!\n", "9"), call("stderr", "9")], any_order=True)
    self.assertEqual(len(report['reports']), 1)
    self.assertEqual(expected, report['reports'][0])


  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch("__builtin__.open")
  @patch.object(ActionQueue, "status_update_callback")
  def test_auto_execute_command(self, status_update_callback_mock, open_mock):
    # Make file read calls visible
    def open_side_effect(file, mode):
      if mode == 'r':
        file_mock = MagicMock()
        file_mock.read.return_value = "Read from " + str(file)
        return file_mock
      else:
        return self.original_open(file, mode)
    open_mock.side_effect = open_side_effect

    config = AmbariConfig()
    tempdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tempdir)
    config.set('agent', 'cache_dir', "/var/lib/ambari-agent/cache")
    config.set('agent', 'tolerate_download_failures', "true")
    dummy_controller = MagicMock()
    dummy_controller.recovery_manager = RecoveryManager(tempfile.mktemp())
    dummy_controller.recovery_manager.update_config(5, 5, 1, 11, True, False, False, "", -1)

    actionQueue = ActionQueue(config, dummy_controller)
    unfreeze_flag = threading.Event()
    python_execution_result_dict = {
      'stdout': 'out',
      'stderr': 'stderr',
      'structuredOut' : ''
    }

    def side_effect(command, tmpoutfile, tmperrfile, override_output_files=True, retry=False):
      unfreeze_flag.wait()
      return python_execution_result_dict
    def patched_aq_execute_command(command):
      # We have to perform patching for separate thread in the same thread
      with patch.object(CustomServiceOrchestrator, "runCommand") as runCommand_mock:
        runCommand_mock.side_effect = side_effect
        actionQueue.process_command(command)

    python_execution_result_dict['status'] = 'COMPLETE'
    python_execution_result_dict['exitcode'] = 0
    self.assertFalse(actionQueue.tasks_in_progress_or_pending())
    # We call method in a separate thread
    execution_thread = Thread(target = patched_aq_execute_command ,
                              args = (self.datanode_auto_start_command, ))
    execution_thread.start()
    #  check in progress report
    # wait until ready
    while True:
      time.sleep(0.1)
      if actionQueue.tasks_in_progress_or_pending():
        break
    # Continue command execution
    unfreeze_flag.set()
    # wait until ready
    check_queue = True
    while check_queue:
      report = actionQueue.result()
      if not actionQueue.tasks_in_progress_or_pending():
        break
      time.sleep(0.1)

    self.assertEqual(len(report['reports']), 0)

    ## Test failed execution
    python_execution_result_dict['status'] = 'FAILED'
    python_execution_result_dict['exitcode'] = 13
    # We call method in a separate thread
    execution_thread = Thread(target = patched_aq_execute_command ,
                              args = (self.datanode_auto_start_command, ))
    execution_thread.start()
    unfreeze_flag.set()
    #  check in progress report
    # wait until ready
    while check_queue:
      report = actionQueue.result()
      if not actionQueue.tasks_in_progress_or_pending():
        break
      time.sleep(0.1)

    self.assertEqual(len(report['reports']), 0)

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
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

    config = AmbariConfig()
    tempdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tempdir)
    config.set('agent', 'cache_dir', "/var/lib/ambari-agent/cache")
    config.set('agent', 'tolerate_download_failures', "true")
    dummy_controller = MagicMock()
    actionQueue = ActionQueue(config, dummy_controller)
    unfreeze_flag = threading.Event()
    python_execution_result_dict = {
      'stdout': 'out',
      'stderr': 'stderr',
      'structuredOut' : ''
      }

    def side_effect(command, tmpoutfile, tmperrfile, override_output_files=True, retry=False):
      unfreeze_flag.wait()
      return python_execution_result_dict
    def patched_aq_execute_command(command):
      # We have to perform patching for separate thread in the same thread
      with patch.object(CustomServiceOrchestrator, "runCommand") as runCommand_mock:
          runCommand_mock.side_effect = side_effect
          actionQueue.execute_command(command)
    ### Test install/start/stop command ###
    ## Test successful execution with configuration tags
    python_execution_result_dict['status'] = 'COMPLETE'
    python_execution_result_dict['exitcode'] = 0
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
                'stderr': 'Read from {0}'.format(os.path.join(tempdir, "errors-3.txt")),
                'stdout': 'Read from {0}'.format(os.path.join(tempdir, "output-3.txt")),
                'structuredOut' : 'Read from {0}'.format(os.path.join(tempdir, "structured-out-3.json")),
                'clusterName': u'cc',
                'roleCommand': u'INSTALL',
                'serviceName': u'HDFS',
                'role': u'DATANODE',
                'actionId': '1-1',
                'taskId': 3,
                'exitCode': 777}
    self.assertEqual(report['reports'][0], expected)
    self.assertTrue(actionQueue.tasks_in_progress_or_pending())

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
                'stdout': 'out\n\nCommand completed successfully!\n',
                'clusterName': u'cc',
                'structuredOut': '""',
                'roleCommand': u'INSTALL',
                'serviceName': u'HDFS',
                'role': u'DATANODE',
                'actionId': '1-1',
                'taskId': 3,
                'configurationTags': {'global': {'tag': 'v1'}},
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
    python_execution_result_dict['status'] = 'FAILED'
    python_execution_result_dict['exitcode'] = 13
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
                'stdout': 'out\n\nCommand completed successfully!\n\n\nCommand failed after 1 tries\n',
                'clusterName': u'cc',
                'structuredOut': '""',
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
    python_execution_result_dict['status'] = 'COMPLETE'
    python_execution_result_dict['exitcode'] = 0
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
                'stdout': 'out\n\nCommand completed successfully!\n\n\nCommand failed after 1 tries\n\n\nCommand completed successfully!\n',
                'clusterName': 'clusterName',
                'structuredOut': '""',
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

  def test_cancel_with_reschedule_command(self):
    config = AmbariConfig()
    tempdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tempdir)
    config.set('agent', 'cache_dir', "/var/lib/ambari-agent/cache")
    config.set('agent', 'tolerate_download_failures', "true")
    dummy_controller = MagicMock()
    actionQueue = ActionQueue(config, dummy_controller)
    unfreeze_flag = threading.Event()
    python_execution_result_dict = {
      'stdout': 'out',
      'stderr': 'stderr',
      'structuredOut' : '',
      'status' : '',
      'exitcode' : -signal.SIGTERM
    }

    def side_effect(command, tmpoutfile, tmperrfile, override_output_files=True, retry=False):
      unfreeze_flag.wait()
      return python_execution_result_dict
    def patched_aq_execute_command(command):
      # We have to perform patching for separate thread in the same thread
      with patch.object(CustomServiceOrchestrator, "runCommand") as runCommand_mock:
        runCommand_mock.side_effect = side_effect
        actionQueue.execute_command(command)

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

    unfreeze_flag.set()
    # wait until ready
    while len(report['reports']) != 0:
      time.sleep(0.1)
      report = actionQueue.result()

    # check report
    self.assertEqual(len(report['reports']), 0)


  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(CustomServiceOrchestrator, "runCommand")
  @patch("CommandStatusDict.CommandStatusDict")
  @patch.object(ActionQueue, "status_update_callback")
  def test_store_configuration_tags(self, status_update_callback_mock,
                                    command_status_dict_mock,
                                    cso_runCommand_mock):
    custom_service_orchestrator_execution_result_dict = {
      'stdout': 'out',
      'stderr': 'stderr',
      'structuredOut' : '',
      'exitcode' : 0
    }
    cso_runCommand_mock.return_value = custom_service_orchestrator_execution_result_dict

    config = AmbariConfig()
    tempdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tempdir)
    config.set('agent', 'cache_dir', "/var/lib/ambari-agent/cache")
    config.set('agent', 'tolerate_download_failures', "true")
    dummy_controller = MagicMock()
    actionQueue = ActionQueue(config, dummy_controller)
    actionQueue.execute_command(self.datanode_restart_command)
    report = actionQueue.result()
    expected = {'status': 'COMPLETED',
                'configurationTags': {'global': {'tag': 'v123'}},
                'stderr': 'stderr',
                'stdout': 'out\n\nCommand completed successfully!\n',
                'clusterName': u'cc',
                'structuredOut': '""',
                'roleCommand': u'CUSTOM_COMMAND',
                'serviceName': u'HDFS',
                'role': u'DATANODE',
                'actionId': '1-1',
                'taskId': 9,
                'customCommand': 'RESTART',
                'exitCode': 0}
    # Agent caches configurationTags if custom_command RESTART completed
    self.assertEqual(len(report['reports']), 1)
    self.assertEqual(expected, report['reports'][0])

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(ActualConfigHandler, "write_client_components")
  @patch.object(CustomServiceOrchestrator, "runCommand")
  @patch("CommandStatusDict.CommandStatusDict")
  @patch.object(ActionQueue, "status_update_callback")
  def test_store_configuration_tags_no_clients(self, status_update_callback_mock,
                                    command_status_dict_mock,
                                    cso_runCommand_mock, write_client_components_mock):
    custom_service_orchestrator_execution_result_dict = {
      'stdout': 'out',
      'stderr': 'stderr',
      'structuredOut' : '',
      'exitcode' : 0
    }
    cso_runCommand_mock.return_value = custom_service_orchestrator_execution_result_dict

    config = AmbariConfig()
    tempdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tempdir)
    config.set('agent', 'cache_dir', "/var/lib/ambari-agent/cache")
    config.set('agent', 'tolerate_download_failures', "true")
    dummy_controller = MagicMock()
    actionQueue = ActionQueue(config, dummy_controller)
    actionQueue.execute_command(self.datanode_restart_command_no_clients_update)
    report = actionQueue.result()
    expected = {'status': 'COMPLETED',
                'configurationTags': {'global': {'tag': 'v123'}},
                'stderr': 'stderr',
                'stdout': 'out\n\nCommand completed successfully!\n',
                'clusterName': u'cc',
                'structuredOut': '""',
                'roleCommand': u'CUSTOM_COMMAND',
                'serviceName': u'HDFS',
                'role': u'DATANODE',
                'actionId': '1-1',
                'taskId': 9,
                'customCommand': 'RESTART',
                'exitCode': 0}
    # Agent caches configurationTags if custom_command RESTART completed
    self.assertEqual(len(report['reports']), 1)
    self.assertEqual(expected, report['reports'][0])
    self.assertFalse(write_client_components_mock.called)

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(ActualConfigHandler, "write_client_components")
  @patch.object(ActualConfigHandler, "write_actual_component")
  @patch.object(ActualConfigHandler, "update_component_tag")
  @patch.object(CustomServiceOrchestrator, "runCommand")
  @patch("CommandStatusDict.CommandStatusDict")
  @patch.object(ActionQueue, "status_update_callback")
  def test_refresh_queues_custom_command(self, status_update_callback_mock,
                                                            command_status_dict_mock,
                                                            cso_runCommand_mock, update_component_tag, write_actual_component_mock, write_client_components_mock):
    custom_service_orchestrator_execution_result_dict = {
      'stdout': 'out',
      'stderr': 'stderr',
      'structuredOut' : '',
      'exitcode' : 0
    }
    cso_runCommand_mock.return_value = custom_service_orchestrator_execution_result_dict

    config = AmbariConfig()
    tempdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tempdir)
    config.set('agent', 'cache_dir', "/var/lib/ambari-agent/cache")
    config.set('agent', 'tolerate_download_failures', "true")
    dummy_controller = MagicMock()
    actionQueue = ActionQueue(config, dummy_controller)
    actionQueue.execute_command(self.yarn_refresh_queues_custom_command)

    report = actionQueue.result()
    expected = {'status': 'COMPLETED',
                'configurationTags': None,
                'stderr': 'stderr',
                'stdout': 'out\n\nCommand completed successfully!\n',
                'clusterName': u'cc',
                'structuredOut': '""',
                'roleCommand': u'CUSTOM_COMMAND',
                'serviceName': u'YARN',
                'role': u'RESOURCEMANAGER',
                'actionId': '1-1',
                'taskId': 9,
                'customCommand': 'RESTART',
                'exitCode': 0}
    self.assertEqual(len(report['reports']), 1)
    self.assertEqual(expected, report['reports'][0])

    # Configuration tags should be updated
    self.assertTrue(update_component_tag.called)

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(ActualConfigHandler, "write_client_components")
  @patch.object(ActualConfigHandler, "write_actual_component")
  @patch.object(CustomServiceOrchestrator, "runCommand")
  @patch("CommandStatusDict.CommandStatusDict")
  @patch.object(ActionQueue, "status_update_callback")
  def test_store_configuration_tags_on_custom_start_command(self, status_update_callback_mock,
                                    command_status_dict_mock,
                                    cso_runCommand_mock, write_actual_component_mock, write_client_components_mock):
    custom_service_orchestrator_execution_result_dict = {
      'stdout': 'out',
      'stderr': 'stderr',
      'structuredOut' : '',
      'exitcode' : 0
    }
    cso_runCommand_mock.return_value = custom_service_orchestrator_execution_result_dict

    config = AmbariConfig()
    tempdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tempdir)
    config.set('agent', 'cache_dir', "/var/lib/ambari-agent/cache")
    config.set('agent', 'tolerate_download_failures', "true")
    dummy_controller = MagicMock()
    actionQueue = ActionQueue(config, dummy_controller)
    actionQueue.execute_command(self.datanode_start_custom_command)
    report = actionQueue.result()
    expected = {'status': 'COMPLETED',
                'configurationTags': {'global': {'tag': 'v123'}},
                'stderr': 'stderr',
                'stdout': 'out\n\nCommand completed successfully!\n',
                'clusterName': u'cc',
                'structuredOut': '""',
                'roleCommand': u'CUSTOM_COMMAND',
                'serviceName': u'HDFS',
                'role': u'DATANODE',
                'actionId': '1-1',
                'taskId': 9,
                'customCommand': 'START',
                'exitCode': 0}
    self.assertEqual(len(report['reports']), 1)
    self.assertEqual(expected, report['reports'][0])

    # Configuration tags should be updated on custom start command
    self.assertTrue(write_actual_component_mock.called)

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(ActualConfigHandler, "write_actual_component")
  @patch.object(CustomServiceOrchestrator, "runCommand")
  @patch("CommandStatusDict.CommandStatusDict")
  @patch.object(ActionQueue, "status_update_callback")
  def test_store_config_tags_on_install_client_command(self, status_update_callback_mock,
      command_status_dict_mock,
      cso_runCommand_mock, write_actual_component_mock):

    custom_service_orchestrator_execution_result_dict = {
      'stdout': 'out',
      'stderr': 'stderr',
      'structuredOut' : '',
      'exitcode' : 0
    }
    cso_runCommand_mock.return_value = custom_service_orchestrator_execution_result_dict

    tez_client_install_command = {
      'commandType': 'EXECUTION_COMMAND',
      'role': u'TEZ_CLIENT',
      'roleCommand': u'INSTALL',
      'commandId': '1-1',
      'taskId': 9,
      'clusterName': u'cc',
      'serviceName': u'TEZ',
      'configurations': {'global' : {}},
      'configurationTags': {'global' : { 'tag': 'v123' }},
      'hostLevelParams': {}
    }
    LiveStatus.CLIENT_COMPONENTS = ({'serviceName': 'TEZ', 'componentName': 'TEZ_CLIENT'}, )

    config = AmbariConfig()
    tempdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tempdir)
    config.set('agent', 'cache_dir', "/var/lib/ambari-agent/cache")
    config.set('agent', 'tolerate_download_failures', "true")
    dummy_controller = MagicMock()
    actionQueue = ActionQueue(config, dummy_controller)
    actionQueue.execute_command(tez_client_install_command)

    # Configuration tags should be updated on install client command
    self.assertTrue(write_actual_component_mock.called)

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(ActionQueue, "status_update_callback")
  @patch.object(ActionQueue, "execute_command")
  @patch.object(LiveStatus, "build")
  @patch.object(CustomServiceOrchestrator, "__init__")
  def test_execute_status_command(self, CustomServiceOrchestrator_mock,
                                  build_mock, execute_command_mock,
                                  status_update_callback):
    CustomServiceOrchestrator_mock.return_value = None
    dummy_controller = MagicMock()
    actionQueue = ActionQueue(AmbariConfig(), dummy_controller)

    build_mock.return_value = {'dummy report': '' }

    dummy_controller.recovery_manager = RecoveryManager(tempfile.mktemp())

    result = (self.status_command, {'exitcode': 0 })

    actionQueue.process_status_command_result(result)
    report = actionQueue.result()
    expected = {'dummy report': ''}

    self.assertEqual(len(report['componentStatus']), 1)
    self.assertEqual(report['componentStatus'][0], expected)

  @patch.object(RecoveryManager, "command_exists")
  @patch.object(RecoveryManager, "requires_recovery")
  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(ActionQueue, "status_update_callback")
  @patch.object(ActionQueue, "execute_command")
  @patch.object(LiveStatus, "build")
  @patch.object(CustomServiceOrchestrator, "__init__")
  def test_process_status_command_result_recovery(self, CustomServiceOrchestrator_mock,
                                  build_mock, execute_command_mock,
                                  status_update_callback, requires_recovery_mock,
                                  command_exists_mock):
    CustomServiceOrchestrator_mock.return_value = None
    dummy_controller = MagicMock()
    actionQueue = ActionQueue(AmbariConfig(), dummy_controller)

    build_mock.return_value = {'dummy report': '' }
    requires_recovery_mock.return_value = True
    command_exists_mock.return_value = False

    dummy_controller.recovery_manager = RecoveryManager(tempfile.mktemp(), True, False)

    result = (self.status_command, {'exitcode': 0 })

    actionQueue.process_status_command_result(result)
    report = actionQueue.result()
    expected = {'dummy report': '',
                'sendExecCmdDet': 'True'}

    self.assertEqual(len(report['componentStatus']), 1)
    self.assertEqual(report['componentStatus'][0], expected)

    requires_recovery_mock.return_value = True
    command_exists_mock.return_value = True
    
    result = (self.status_command, {'exitcode': 0 })

    actionQueue.process_status_command_result(result)
    report = actionQueue.result()
    expected = {'dummy report': '',
                'sendExecCmdDet': 'False'}

    self.assertEqual(len(report['componentStatus']), 1)
    self.assertEqual(report['componentStatus'][0], expected)

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(ActionQueue, "status_update_callback")
  @patch.object(ActionQueue, "execute_command")
  @patch.object(LiveStatus, "build")
  @patch.object(CustomServiceOrchestrator, "__init__")
  def test_process_status_command_result_with_alerts(self, CustomServiceOrchestrator_mock,
                                  build_mock, execute_command_mock,
                                  status_update_callback):
    CustomServiceOrchestrator_mock.return_value = None
    dummy_controller = MagicMock()
    actionQueue = ActionQueue(AmbariConfig(), dummy_controller)
    command_return_value = {
      'exitcode': 0,
      'stdout': 'out',
      'stderr': 'err',
      'structuredOut': {'alerts': [ {'name': 'flume_alert'} ] }
    }
    
    result = (self.status_command_for_alerts, command_return_value)
    
    build_mock.return_value = {'somestatusresult': 'aresult'}

    actionQueue.process_status_command_result(result)

    report = actionQueue.result()

    self.assertEqual(len(report['componentStatus']), 1)
    self.assertTrue(report['componentStatus'][0].has_key('alerts'))

  @patch.object(AmbariConfig, "get_parallel_exec_option")
  @patch.object(ActionQueue, "process_command")
  @patch.object(Queue, "get")
  @patch.object(CustomServiceOrchestrator, "__init__")
  def test_reset_queue(self, CustomServiceOrchestrator_mock,
                                get_mock, process_command_mock, gpeo_mock):
    CustomServiceOrchestrator_mock.return_value = None
    dummy_controller = MagicMock()
    dummy_controller.recovery_manager = RecoveryManager(tempfile.mktemp())
    config = MagicMock()
    gpeo_mock.return_value = 0
    config.get_parallel_exec_option = gpeo_mock
    actionQueue = ActionQueue(config, dummy_controller)
    actionQueue.start()
    actionQueue.put([self.datanode_install_command, self.hbase_install_command])
    self.assertEqual(2, actionQueue.commandQueue.qsize())
    self.assertTrue(actionQueue.tasks_in_progress_or_pending())
    actionQueue.reset()
    self.assertTrue(actionQueue.commandQueue.empty())
    self.assertFalse(actionQueue.tasks_in_progress_or_pending())
    time.sleep(0.1)
    actionQueue.stop()
    actionQueue.join()
    self.assertEqual(actionQueue.stopped(), True, 'Action queue is not stopped.')

  @patch.object(AmbariConfig, "get_parallel_exec_option")
  @patch.object(ActionQueue, "process_command")
  @patch.object(Queue, "get")
  @patch.object(CustomServiceOrchestrator, "__init__")
  def test_cancel(self, CustomServiceOrchestrator_mock,
                       get_mock, process_command_mock, gpeo_mock):
    CustomServiceOrchestrator_mock.return_value = None
    dummy_controller = MagicMock()
    config = MagicMock()
    gpeo_mock.return_value = 0
    config.get_parallel_exec_option = gpeo_mock
    actionQueue = ActionQueue(config, dummy_controller)
    actionQueue.start()
    actionQueue.put([self.datanode_install_command, self.hbase_install_command])
    self.assertEqual(2, actionQueue.commandQueue.qsize())
    actionQueue.reset()
    self.assertTrue(actionQueue.commandQueue.empty())
    time.sleep(0.1)
    actionQueue.stop()
    actionQueue.join()
    self.assertEqual(actionQueue.stopped(), True, 'Action queue is not stopped.')

  @patch.object(AmbariConfig, "get_parallel_exec_option")
  @patch.object(ActionQueue, "process_command")
  @patch.object(CustomServiceOrchestrator, "__init__")
  def test_parallel_exec(self, CustomServiceOrchestrator_mock,
                         process_command_mock, gpeo_mock):
    CustomServiceOrchestrator_mock.return_value = None
    dummy_controller = MagicMock()
    config = MagicMock()
    gpeo_mock.return_value = 1
    config.get_parallel_exec_option = gpeo_mock
    actionQueue = ActionQueue(config, dummy_controller)
    actionQueue.put([self.datanode_install_command, self.hbase_install_command])
    self.assertEqual(2, actionQueue.commandQueue.qsize())
    actionQueue.start()
    time.sleep(1)
    actionQueue.stop()
    actionQueue.join()
    self.assertEqual(actionQueue.stopped(), True, 'Action queue is not stopped.')
    self.assertEqual(2, process_command_mock.call_count)
    process_command_mock.assert_any_calls([call(self.datanode_install_command), call(self.hbase_install_command)])

  @patch("threading.Thread")
  @patch.object(AmbariConfig, "get_parallel_exec_option")
  @patch.object(ActionQueue, "process_command")
  @patch.object(CustomServiceOrchestrator, "__init__")
  def test_parallel_exec_no_retry(self, CustomServiceOrchestrator_mock,
                         process_command_mock, gpeo_mock, threading_mock):
    CustomServiceOrchestrator_mock.return_value = None
    dummy_controller = MagicMock()
    config = MagicMock()
    gpeo_mock.return_value = 1
    config.get_parallel_exec_option = gpeo_mock
    actionQueue = ActionQueue(config, dummy_controller)
    actionQueue.put([self.datanode_install_no_retry_command, self.snamenode_install_command])
    self.assertEqual(2, actionQueue.commandQueue.qsize())
    actionQueue.start()
    time.sleep(1)
    actionQueue.stop()
    actionQueue.join()
    self.assertEqual(actionQueue.stopped(), True, 'Action queue is not stopped.')
    self.assertEqual(2, process_command_mock.call_count)
    self.assertEqual(0, threading_mock.call_count)
    process_command_mock.assert_any_calls([call(self.datanode_install_command), call(self.hbase_install_command)])

  @not_for_platform(PLATFORM_LINUX)
  @patch("time.sleep")
  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  @patch.object(CustomServiceOrchestrator, "__init__")
  def test_execute_retryable_command(self, CustomServiceOrchestrator_mock,
                                     sleep_mock
  ):
    CustomServiceOrchestrator_mock.return_value = None
    dummy_controller = MagicMock()
    actionQueue = ActionQueue(AmbariConfig(), dummy_controller)
    python_execution_result_dict = {
      'exitcode': 1,
      'stdout': 'out',
      'stderr': 'stderr',
      'structuredOut': '',
      'status': 'FAILED'
    }

    def side_effect(command, tmpoutfile, tmperrfile, override_output_files=True, retry=False):
      return python_execution_result_dict

    command = copy.deepcopy(self.retryable_command)
    with patch.object(CustomServiceOrchestrator, "runCommand") as runCommand_mock:
      runCommand_mock.side_effect = side_effect
      actionQueue.execute_command(command)

    #assert that python executor start
    self.assertTrue(runCommand_mock.called)
    self.assertEqual(3, runCommand_mock.call_count)
    self.assertEqual(2, sleep_mock.call_count)
    sleep_mock.assert_has_calls([call(2), call(3)], False)
    runCommand_mock.assert_has_calls([
      call(command, os.sep + 'tmp' + os.sep + 'ambari-agent' + os.sep + 'output-19.txt',
           os.sep + 'tmp' + os.sep + 'ambari-agent' + os.sep + 'errors-19.txt', override_output_files=True, retry=False),
      call(command, os.sep + 'tmp' + os.sep + 'ambari-agent' + os.sep + 'output-19.txt',
           os.sep + 'tmp' + os.sep + 'ambari-agent' + os.sep + 'errors-19.txt', override_output_files=False, retry=True),
      call(command, os.sep + 'tmp' + os.sep + 'ambari-agent' + os.sep + 'output-19.txt',
           os.sep + 'tmp' + os.sep + 'ambari-agent' + os.sep + 'errors-19.txt', override_output_files=False, retry=True)])


  @patch("time.time")
  @patch("time.sleep")
  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  @patch.object(CustomServiceOrchestrator, "__init__")
  def test_execute_retryable_command_with_time_lapse(self, CustomServiceOrchestrator_mock,
                                     sleep_mock, time_mock
  ):
    CustomServiceOrchestrator_mock.return_value = None
    dummy_controller = MagicMock()
    dummy_controller.recovery_manager = RecoveryManager(tempfile.mktemp())
    actionQueue = ActionQueue(AmbariConfig(), dummy_controller)
    python_execution_result_dict = {
      'exitcode': 1,
      'stdout': 'out',
      'stderr': 'stderr',
      'structuredOut': '',
      'status': 'FAILED'
    }

    times_arr = [8, 10, 14, 18, 22, 26, 30, 34]
    if self.logger.isEnabledFor(logging.INFO):
      times_arr.insert(0, 4)
    time_mock.side_effect = times_arr

    def side_effect(command, tmpoutfile, tmperrfile, override_output_files=True, retry=False):
      return python_execution_result_dict

    command = copy.deepcopy(self.retryable_command)
    with patch.object(CustomServiceOrchestrator, "runCommand") as runCommand_mock:
      runCommand_mock.side_effect = side_effect
      actionQueue.execute_command(command)

    #assert that python executor start
    self.assertTrue(runCommand_mock.called)
    self.assertEqual(2, runCommand_mock.call_count)
    self.assertEqual(1, sleep_mock.call_count)
    sleep_mock.assert_has_calls([call(1)], False)
    runCommand_mock.assert_has_calls([
      call(command, os.sep + 'tmp' + os.sep + 'ambari-agent' + os.sep + 'output-19.txt',
           os.sep + 'tmp' + os.sep + 'ambari-agent' + os.sep + 'errors-19.txt', override_output_files=True, retry=False),
      call(command, os.sep + 'tmp' + os.sep + 'ambari-agent' + os.sep + 'output-19.txt',
           os.sep + 'tmp' + os.sep + 'ambari-agent' + os.sep + 'errors-19.txt', override_output_files=False, retry=True)])

  #retryable_command
  @not_for_platform(PLATFORM_LINUX)
  @patch("time.sleep")
  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  @patch.object(CustomServiceOrchestrator, "__init__")
  def test_execute_retryable_command_fail_and_succeed(self, CustomServiceOrchestrator_mock,
                                                      sleep_mock
  ):
    CustomServiceOrchestrator_mock.return_value = None
    dummy_controller = MagicMock()
    actionQueue = ActionQueue(AmbariConfig(), dummy_controller)
    execution_result_fail_dict = {
      'exitcode': 1,
      'stdout': 'out',
      'stderr': 'stderr',
      'structuredOut': '',
      'status': 'FAILED'
    }
    execution_result_succ_dict = {
      'exitcode': 0,
      'stdout': 'out',
      'stderr': 'stderr',
      'structuredOut': '',
      'status': 'COMPLETED'
    }

    command = copy.deepcopy(self.retryable_command)
    self.assertFalse('commandBeingRetried' in command)
    with patch.object(CustomServiceOrchestrator, "runCommand") as runCommand_mock:
      runCommand_mock.side_effect = [execution_result_fail_dict, execution_result_succ_dict]
      actionQueue.execute_command(command)

    #assert that python executor start
    self.assertTrue(runCommand_mock.called)
    self.assertEqual(2, runCommand_mock.call_count)
    self.assertEqual(1, sleep_mock.call_count)
    self.assertEqual(command['commandBeingRetried'], "true")
    sleep_mock.assert_any_call(2)

  @not_for_platform(PLATFORM_LINUX)
  @patch("time.sleep")
  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  @patch.object(CustomServiceOrchestrator, "__init__")
  def test_execute_retryable_command_succeed(self, CustomServiceOrchestrator_mock,
                                             sleep_mock
  ):
    CustomServiceOrchestrator_mock.return_value = None
    dummy_controller = MagicMock()
    actionQueue = ActionQueue(AmbariConfig(), dummy_controller)
    execution_result_succ_dict = {
      'exitcode': 0,
      'stdout': 'out',
      'stderr': 'stderr',
      'structuredOut': '',
      'status': 'COMPLETED'
    }

    command = copy.deepcopy(self.retryable_command)
    with patch.object(CustomServiceOrchestrator, "runCommand") as runCommand_mock:
      runCommand_mock.side_effect = [execution_result_succ_dict]
      actionQueue.execute_command(command)

    #assert that python executor start
    self.assertTrue(runCommand_mock.called)
    self.assertFalse(sleep_mock.called)
    self.assertEqual(1, runCommand_mock.call_count)

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(CustomServiceOrchestrator, "runCommand")
  @patch.object(CustomServiceOrchestrator, "__init__")
  def test_execute_background_command(self, CustomServiceOrchestrator_mock,
                                  runCommand_mock,
                                  ):
    CustomServiceOrchestrator_mock.return_value = None
    CustomServiceOrchestrator.runCommand.return_value = {'exitcode' : 0,
                                                         'stdout': 'out-11',
                                                         'stderr' : 'err-13'}
    
    dummy_controller = MagicMock()
    actionQueue = ActionQueue(AmbariConfig(), dummy_controller)

    execute_command = copy.deepcopy(self.background_command)
    actionQueue.put([execute_command])
    actionQueue.processBackgroundQueueSafeEmpty();
    actionQueue.controller.statusCommandExecutor.process_results();
    
    #assert that python execturor start
    self.assertTrue(runCommand_mock.called)
    runningCommand = actionQueue.commandStatuses.current_state.get(execute_command['taskId'])
    self.assertTrue(runningCommand is not None)
    self.assertEqual(runningCommand[1]['status'], ActionQueue.IN_PROGRESS_STATUS)
    
    report = actionQueue.result()
    self.assertEqual(len(report['reports']),1)

  @patch.object(CustomServiceOrchestrator, "get_py_executor")
  @patch.object(CustomServiceOrchestrator, "resolve_script_path")
  def test_execute_python_executor(self, resolve_script_path_mock,
                                   get_py_executor_mock):
    
    dummy_controller = MagicMock()
    cfg = AmbariConfig()
    cfg.set('agent', 'tolerate_download_failures', 'true')
    cfg.set('agent', 'prefix', '.')
    cfg.set('agent', 'cache_dir', 'background_tasks')
    
    actionQueue = ActionQueue(cfg, dummy_controller)
    pyex = PythonExecutor(actionQueue.customServiceOrchestrator.tmp_dir, actionQueue.customServiceOrchestrator.config)
    patch_output_file(pyex)
    get_py_executor_mock.return_value = pyex
    actionQueue.customServiceOrchestrator.dump_command_to_json = MagicMock()
   
    result = {}
    lock = threading.RLock()
    complete_done = threading.Condition(lock)
    
    def command_complete_w(process_condensed_result, handle):
      with lock:
        result['command_complete'] = {'condensed_result' : copy.copy(process_condensed_result),
                                      'handle' : copy.copy(handle),
                                      'command_status' : actionQueue.commandStatuses.get_command_status(handle.command['taskId'])
                                      }
        complete_done.notifyAll()

    actionQueue.on_background_command_complete_callback = wraped(actionQueue.on_background_command_complete_callback,
                                                                 None, command_complete_w)
    actionQueue.put([self.background_command])
    actionQueue.processBackgroundQueueSafeEmpty();
    actionQueue.controller.statusCommandExecutor.process_results();
    
    with lock:
      complete_done.wait(0.1)
      
      finished_status = result['command_complete']['command_status']
      self.assertEqual(finished_status['status'], ActionQueue.COMPLETED_STATUS)
      self.assertEqual(finished_status['stdout'], 'process_out')
      self.assertEqual(finished_status['stderr'], 'process_err')
      self.assertEqual(finished_status['exitCode'], 0)
      
    
    runningCommand = actionQueue.commandStatuses.current_state.get(self.background_command['taskId'])
    self.assertTrue(runningCommand is not None)
    
    report = actionQueue.result()
    self.assertEqual(len(report['reports']),1)
    self.assertEqual(report['reports'][0]['stdout'],'process_out')
#    self.assertEqual(report['reports'][0]['structuredOut'],'{"a": "b."}')
    
    
  
  cancel_background_command = {
    "commandType":"CANCEL_COMMAND",
    "role":"AMBARI_SERVER_ACTION",
    "roleCommand":"ABORT",
    "commandId":"2--1",
    "taskId":20,
    "clusterName":"c1",
    "serviceName":"",
    "hostname":"c6401",
    "roleParams":{
      "cancelTaskIdTargets":"13,14"
    },
  }

def patch_output_file(pythonExecutor):
  def windows_py(command, tmpout, tmperr):
    proc = MagicMock()
    proc.pid = 33
    proc.returncode = 0
    with tmpout:
      tmpout.write('process_out')
    with tmperr:
      tmperr.write('process_err')
    return proc
  def open_subprocess_files_win(fout, ferr, f):
    return MagicMock(), MagicMock()
  def read_result_from_files(out_path, err_path, structured_out_path):
    return 'process_out', 'process_err', '{"a": "b."}'
  pythonExecutor.launch_python_subprocess = windows_py
  pythonExecutor.open_subprocess_files = open_subprocess_files_win
  pythonExecutor.read_result_from_files = read_result_from_files

def wraped(func, before = None, after = None):
    def wrapper(*args, **kwargs):
      if(before is not None):
        before(*args, **kwargs)
      ret =  func(*args, **kwargs)
      if(after is not None):
        after(*args, **kwargs)
      return ret
    return wrapper   
  
