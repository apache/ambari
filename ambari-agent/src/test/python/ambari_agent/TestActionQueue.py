#!/usr/bin/env python3

"""
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
"""

from unittest import TestCase
from ambari_agent.LiveStatus import LiveStatus
from ambari_agent.ActionQueue import ActionQueue, hide_passwords
from ambari_agent.AmbariConfig import AmbariConfig
import os, errno, pprint, tempfile, threading
import sys
from threading import Thread
import copy
import signal

from ambari_agent.models.commands import CommandStatus, AgentCommand
from unittest.mock import patch, MagicMock, call
from ambari_agent.CustomServiceOrchestrator import CustomServiceOrchestrator
from ambari_agent.ActualConfigHandler import ActualConfigHandler
from ambari_agent.RecoveryManager import RecoveryManager
from ambari_commons import OSCheck
from only_for_platform import (
  not_for_platform,
  os_distro_value,
  PLATFORM_LINUX,
)
from ambari_agent.InitializerModule import InitializerModule

import logging

CLUSTER_ID = "0"


class TestActionQueue(TestCase):
  def setUp(self):
    # save original open() method for later use
    self.original_open = open

  def tearDown(self):
    sys.stdout = sys.__stdout__

  logger = logging.getLogger()

  def create_action_queue(self, parallel_execution=0, max_parallel_actions=5):
    initializer_module = MagicMock()
    initializer_module.config = AmbariConfig()
    initializer_module.config.set("agent", "parallel_execution", parallel_execution)
    initializer_module.config.set(
      "agent", "max_parallel_actions", max_parallel_actions
    )
    initializer_module.stop_event = threading.Event()
    initializer_module.recovery_manager.enabled.return_value = False
    initializer_module.recovery_manager.has_active_command.return_value = False
    return ActionQueue(initializer_module), initializer_module

  datanode_install_command = {
    "commandType": "EXECUTION_COMMAND",
    "role": "DATANODE",
    "roleCommand": "INSTALL",
    "commandId": "1-1",
    "taskId": 3,
    "clusterName": "cc",
    "serviceName": "HDFS",
    "hostLevelParams": {},
    "configurations": {"global": {}},
    "configurationTags": {"global": {"tag": "v1"}},
    "commandParams": {"command_retry_enabled": "true"},
    "clusterId": CLUSTER_ID,
  }

  datanode_install_no_retry_command = {
    "commandType": "EXECUTION_COMMAND",
    "role": "DATANODE",
    "roleCommand": "INSTALL",
    "commandId": "1-1",
    "taskId": 3,
    "clusterName": "cc",
    "serviceName": "HDFS",
    "hostLevelParams": {},
    "configurations": {"global": {}},
    "configurationTags": {"global": {"tag": "v1"}},
    "commandParams": {"command_retry_enabled": "false"},
    "clusterId": CLUSTER_ID,
  }

  datanode_auto_start_command = {
    "commandType": "AUTO_EXECUTION_COMMAND",
    "role": "DATANODE",
    "roleCommand": "START",
    "commandId": "1-1",
    "taskId": 3,
    "clusterName": "cc",
    "serviceName": "HDFS",
    "hostLevelParams": {},
    "configurations": {"global": {}},
    "configurationTags": {"global": {"tag": "v1"}},
    "clusterId": CLUSTER_ID,
  }

  datanode_upgrade_command = {
    "commandId": 17,
    "role": "role",
    "taskId": "taskId",
    "clusterName": "clusterName",
    "serviceName": "serviceName",
    "roleCommand": "UPGRADE",
    "hostname": "localhost.localdomain",
    "hostLevelParams": {},
    "clusterHostInfo": "clusterHostInfo",
    "commandType": "EXECUTION_COMMAND",
    "configurations": {"global": {}},
    "roleParams": {},
    "commandParams": {
      "source_stack_version": "HDP-1.2.1",
      "target_stack_version": "HDP-1.3.0",
    },
    "clusterId": CLUSTER_ID,
  }

  namenode_install_command = {
    "commandType": "EXECUTION_COMMAND",
    "role": "NAMENODE",
    "roleCommand": "INSTALL",
    "commandId": "1-1",
    "taskId": 4,
    "clusterName": "cc",
    "serviceName": "HDFS",
    "hostLevelParams": {},
    "clusterId": CLUSTER_ID,
  }

  snamenode_install_command = {
    "commandType": "EXECUTION_COMMAND",
    "role": "SECONDARY_NAMENODE",
    "roleCommand": "INSTALL",
    "commandId": "1-1",
    "taskId": 5,
    "clusterName": "cc",
    "serviceName": "HDFS",
    "hostLevelParams": {},
    "clusterId": CLUSTER_ID,
  }

  hbase_install_command = {
    "commandType": "EXECUTION_COMMAND",
    "role": "HBASE",
    "roleCommand": "INSTALL",
    "commandId": "1-1",
    "taskId": 7,
    "clusterName": "cc",
    "serviceName": "HDFS",
    "hostLevelParams": {},
    "commandParams": {"command_retry_enabled": "true"},
    "clusterId": CLUSTER_ID,
  }

  status_command = {
    "serviceName": "HDFS",
    "commandType": "STATUS_COMMAND",
    "clusterName": "",
    "componentName": "DATANODE",
    "configurations": {},
    "hostLevelParams": {},
    "clusterId": CLUSTER_ID,
  }

  datanode_restart_command = {
    "commandType": "EXECUTION_COMMAND",
    "role": "DATANODE",
    "roleCommand": "CUSTOM_COMMAND",
    "commandId": "1-1",
    "taskId": 9,
    "clusterName": "cc",
    "serviceName": "HDFS",
    "configurations": {"global": {}},
    "configurationTags": {"global": {"tag": "v123"}},
    "hostLevelParams": {"custom_command": "RESTART", "clientsToUpdateConfigs": []},
    "clusterId": CLUSTER_ID,
  }

  datanode_restart_command_no_logging = {
    "commandType": "EXECUTION_COMMAND",
    "role": "DATANODE",
    "roleCommand": "CUSTOM_COMMAND",
    "commandId": "1-1",
    "taskId": 9,
    "clusterName": "cc",
    "serviceName": "HDFS",
    "configurations": {"global": {}},
    "configurationTags": {"global": {"tag": "v123"}},
    "commandParams": {"log_output": "false"},
    "hostLevelParams": {"custom_command": "RESTART", "clientsToUpdateConfigs": []},
    "clusterId": CLUSTER_ID,
  }

  datanode_restart_command_no_clients_update = {
    "commandType": "EXECUTION_COMMAND",
    "role": "DATANODE",
    "roleCommand": "CUSTOM_COMMAND",
    "commandId": "1-1",
    "taskId": 9,
    "clusterName": "cc",
    "serviceName": "HDFS",
    "configurations": {"global": {}},
    "configurationTags": {"global": {"tag": "v123"}},
    "hostLevelParams": {"custom_command": "RESTART"},
    "clusterId": CLUSTER_ID,
  }

  datanode_start_custom_command = {
    "clusterId": CLUSTER_ID,
    "commandType": "EXECUTION_COMMAND",
    "role": "DATANODE",
    "roleCommand": "CUSTOM_COMMAND",
    "commandId": "1-1",
    "taskId": 9,
    "clusterName": "cc",
    "serviceName": "HDFS",
    "configurations": {"global": {}},
    "configurationTags": {"global": {"tag": "v123"}},
    "hostLevelParams": {"custom_command": "START"},
  }

  yarn_refresh_queues_custom_command = {
    "commandType": "EXECUTION_COMMAND",
    "role": "RESOURCEMANAGER",
    "roleCommand": "CUSTOM_COMMAND",
    "commandId": "1-1",
    "taskId": 9,
    "clusterName": "cc",
    "serviceName": "YARN",
    "commandParams": {"forceRefreshConfigTags": "capacity-scheduler"},
    "configurations": {"global": {}},
    "configurationTags": {
      "global": {"tag": "v123"},
      "capacity-scheduler": {"tag": "v123"},
    },
    "hostLevelParams": {"custom_command": "REFRESHQUEUES"},
    "clusterId": CLUSTER_ID,
  }

  status_command_for_alerts = {
    "serviceName": "FLUME",
    "commandType": "STATUS_COMMAND",
    "clusterName": "",
    "componentName": "FLUME_HANDLER",
    "configurations": {},
    "hostLevelParams": {},
    "clusterId": CLUSTER_ID,
  }

  retryable_command = {
    "commandType": "EXECUTION_COMMAND",
    "role": "NAMENODE",
    "roleCommand": "INSTALL",
    "commandId": "1-1",
    "taskId": 19,
    "clusterName": "c1",
    "serviceName": "HDFS",
    "configurations": {"global": {}},
    "configurationTags": {"global": {"tag": "v123"}},
    "commandParams": {
      "script_type": "PYTHON",
      "script": "script.py",
      "command_timeout": "600",
      "jdk_location": ".",
      "service_package_folder": ".",
      "command_retry_enabled": "true",
      "max_duration_for_retries": "5",
    },
    "hostLevelParams": {},
    "clusterId": CLUSTER_ID,
  }

  background_command = {
    "commandType": "BACKGROUND_EXECUTION_COMMAND",
    "role": "NAMENODE",
    "roleCommand": "CUSTOM_COMMAND",
    "commandId": "1-1",
    "taskId": 19,
    "clusterName": "c1",
    "serviceName": "HDFS",
    "configurations": {"global": {}},
    "configurationTags": {"global": {"tag": "v123"}},
    "commandParams": {
      "script_type": "PYTHON",
      "script": "script.py",
      "command_timeout": "600",
      "service_package_folder": ".",
      "custom_command": "REBALANCE_HDFS",
    },
    "ambariLevelParams": {"jdk_location": "test"},
    "clusterId": CLUSTER_ID,
  }
  cancel_background_command = {
    "commandType": "EXECUTION_COMMAND",
    "role": "NAMENODE",
    "roleCommand": "ACTIONEXECUTE",
    "commandId": "1-1",
    "taskId": 20,
    "clusterName": "c1",
    "serviceName": "HDFS",
    "configurations": {"global": {}},
    "configurationTags": {"global": {}},
    "hostLevelParams": {},
    "commandParams": {
      "script_type": "PYTHON",
      "script": "cancel_background_task.py",
      "before_system_hook_function": "fetch_bg_pid_by_taskid",
      "jdk_location": ".",
      "command_timeout": "600",
      "service_package_folder": ".",
      "cancel_policy": "SIGKILL",
      "cancel_task_id": "19",
    },
    "clusterId": CLUSTER_ID,
  }

  @patch.object(AmbariConfig, "get_parallel_exec_option")
  @patch.object(ActionQueue, "process_command")
  @patch.object(CustomServiceOrchestrator, "__init__")
  def test_ActionQueueStartStop(
    self,
    CustomServiceOrchestrator_mock,
    process_command_mock,
    get_parallel_exec_option_mock,
  ):
    CustomServiceOrchestrator_mock.return_value = None
    get_parallel_exec_option_mock.return_value = 0
    actionQueue, initializer_module = self.create_action_queue(parallel_execution=0)
    commands_processed = threading.Event()
    processed_count = 0
    processed_count_lock = threading.Lock()

    def process_command(_command):
      nonlocal processed_count
      with processed_count_lock:
        processed_count += 1
        if processed_count >= 2:
          commands_processed.set()

    process_command_mock.side_effect = process_command
    actionQueue.put(
      [copy.deepcopy(self.datanode_install_command), copy.deepcopy(self.hbase_install_command)]
    )
    try:
      actionQueue.start()
      self.assertTrue(
        commands_processed.wait(2), "Action queue did not process commands"
      )
    finally:
      initializer_module.stop_event.set()
      actionQueue.interrupt()
      actionQueue.join(3)
    self.assertEqual(actionQueue.is_alive(), False, "Action queue is not stopped.")
    self.assertEqual(2, process_command_mock.call_count)

  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  @patch("logging.RootLogger.exception")
  @patch.object(ActionQueue, "execute_command")
  def test_process_command(self, execute_command_mock, log_exc_mock):
    dummy_controller = MagicMock()
    config = AmbariConfig()
    config.set("agent", "tolerate_download_failures", "true")

    initializer_module = InitializerModule()
    initializer_module.init()

    actionQueue = ActionQueue(initializer_module)
    execution_command = {
      "commandType": AgentCommand.execution,
    }
    status_command = {
      "commandType": AgentCommand.status,
    }
    wrong_command = {
      "commandType": "SOME_WRONG_COMMAND",
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
  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  @patch.object(CustomServiceOrchestrator, "runCommand")
  @patch("CommandStatusDict.CommandStatusDict")
  def test_log_execution_commands(
    self, command_status_dict_mock, cso_runCommand_mock, mock_log_command_output
  ):
    custom_service_orchestrator_execution_result_dict = {
      "stdout": "out",
      "stderr": "stderr",
      "structuredOut": "",
      "exitcode": 0,
    }
    cso_runCommand_mock.return_value = custom_service_orchestrator_execution_result_dict

    config = AmbariConfig()
    tempdir = tempfile.gettempdir()
    config.set("agent", "prefix", tempdir)
    config.set("agent", "cache_dir", "/var/lib/ambari-agent/cache")
    config.set("agent", "tolerate_download_failures", "true")
    config.set("logging", "log_command_executes", 1)
    dummy_controller = MagicMock()

    initializer_module = InitializerModule()
    initializer_module.init()
    initializer_module.config = config

    actionQueue = ActionQueue(initializer_module)
    actionQueue.execute_command(self.datanode_restart_command)
    reports = actionQueue.commandStatuses.generate_report()[CLUSTER_ID]
    expected = {
      "status": "COMPLETED",
      "stderr": "stderr",
      "stdout": "out\n\nCommand completed successfully!\n",
      "clusterId": CLUSTER_ID,
      "structuredOut": '""',
      "roleCommand": "CUSTOM_COMMAND",
      "serviceName": "HDFS",
      "role": "DATANODE",
      "actionId": "1-1",
      "taskId": 9,
      "exitCode": 0,
    }
    # Agent caches configurationTags if custom_command RESTART completed
    mock_log_command_output.assert_has_calls(
      [call("out\n\nCommand completed successfully!\n", "9"), call("stderr", "9")],
      any_order=True,
    )
    self.assertEqual(len(reports), 1)
    self.assertEqual(expected, reports[0])

  @patch.object(ActionQueue, "log_command_output")
  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  @patch.object(CustomServiceOrchestrator, "runCommand")
  @patch("CommandStatusDict.CommandStatusDict")
  def test_do_not_log_execution_commands(
    self, command_status_dict_mock, cso_runCommand_mock, mock_log_command_output
  ):
    custom_service_orchestrator_execution_result_dict = {
      "stdout": "out",
      "stderr": "stderr",
      "structuredOut": "",
      "exitcode": 0,
    }
    cso_runCommand_mock.return_value = custom_service_orchestrator_execution_result_dict

    config = AmbariConfig()
    tempdir = tempfile.gettempdir()
    config.set("agent", "prefix", tempdir)
    config.set("agent", "cache_dir", "/var/lib/ambari-agent/cache")
    config.set("agent", "tolerate_download_failures", "true")
    config.set("logging", "log_command_executes", 1)
    dummy_controller = MagicMock()

    initializer_module = InitializerModule()
    initializer_module.init()

    actionQueue = ActionQueue(initializer_module)
    actionQueue.execute_command(self.datanode_restart_command_no_logging)
    reports = actionQueue.commandStatuses.generate_report()[CLUSTER_ID]
    expected = {
      "status": "COMPLETED",
      "stderr": "stderr",
      "stdout": "out\n\nCommand completed successfully!\n",
      "clusterId": CLUSTER_ID,
      "structuredOut": '""',
      "roleCommand": "CUSTOM_COMMAND",
      "serviceName": "HDFS",
      "role": "DATANODE",
      "actionId": "1-1",
      "taskId": 9,
      "exitCode": 0,
    }
    # Agent caches configurationTags if custom_command RESTART completed
    mock_log_command_output.assert_not_called()
    self.assertEqual(len(reports), 1)
    self.assertEqual(expected, reports[0])

  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  def test_auto_execute_command(self):
    config = AmbariConfig()
    tempdir = tempfile.gettempdir()
    config.set("agent", "prefix", tempdir)
    config.set("agent", "cache_dir", "/var/lib/ambari-agent/cache")
    config.set("agent", "tolerate_download_failures", "true")

    initializer_module = InitializerModule()
    initializer_module.init()
    initializer_module.config = config
    initializer_module.recovery_manager = RecoveryManager(MagicMock())
    initializer_module.recovery_manager.update_config(5, 5, 1, 11, True, False, False)

    with patch("builtins.open") as open_mock:
      # Make file read calls visible
      def open_side_effect(file, mode, *args, **kwargs):
        if mode == "r":
          file_mock = MagicMock()
          file_content = "Read from " + str(file)
          file_mock.read.return_value = file_content
          file_mock.__enter__.return_value.read.return_value = file_content
          return file_mock
        else:
          return self.original_open(file, mode, *args, **kwargs)

      open_mock.side_effect = open_side_effect
      actionQueue = ActionQueue(initializer_module)
      unfreeze_flag = threading.Event()
      command_started = threading.Event()
      command_finished = threading.Event()
      python_execution_result_dict = {
        "stdout": "out",
        "stderr": "stderr",
        "structuredOut": "",
      }

    def side_effect(
      command,
      tmpoutfile,
      tmperrfile,
      override_output_files=True,
      retry=False,
      cancel_event=None,
    ):
      command_started.set()
      if not unfreeze_flag.wait(2):
        raise RuntimeError("Timed out waiting to release auto execution command")
      return python_execution_result_dict

    def patched_aq_execute_command(command):
      # We have to perform patching for separate thread in the same thread
      try:
        with patch.object(CustomServiceOrchestrator, "runCommand") as runCommand_mock:
          runCommand_mock.side_effect = side_effect
          actionQueue.process_command(command)
      finally:
        command_finished.set()

    python_execution_result_dict["status"] = "COMPLETE"
    python_execution_result_dict["exitcode"] = 0
    self.assertFalse(actionQueue.tasks_in_progress_or_pending())
    # We call method in a separate thread
    execution_thread = Thread(
      target=patched_aq_execute_command, args=(self.datanode_auto_start_command,)
    )
    execution_thread.start()
    self.assertTrue(command_started.wait(2))
    # Continue command execution
    unfreeze_flag.set()
    self.assertTrue(command_finished.wait(2))
    execution_thread.join(2)
    self.assertFalse(execution_thread.is_alive())
    reports = actionQueue.commandStatuses.generate_report()[CLUSTER_ID]

    self.assertEqual(len(reports), 0)

    # # Test failed execution
    python_execution_result_dict["status"] = "FAILED"
    python_execution_result_dict["exitcode"] = 13
    unfreeze_flag = threading.Event()
    command_started = threading.Event()
    command_finished = threading.Event()
    # We call method in a separate thread
    execution_thread = Thread(
      target=patched_aq_execute_command, args=(self.datanode_auto_start_command,)
    )
    execution_thread.start()
    self.assertTrue(command_started.wait(2))
    unfreeze_flag.set()
    self.assertTrue(command_finished.wait(2))
    execution_thread.join(2)
    self.assertFalse(execution_thread.is_alive())

  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  # @patch("__builtin__.open")
  def test_execute_command(self):
    config = AmbariConfig()
    tempdir = tempfile.gettempdir()
    config.set("agent", "prefix", tempdir)
    config.set("agent", "cache_dir", "/var/lib/ambari-agent/cache")
    config.set("agent", "tolerate_download_failures", "true")
    config.set("heartbeat", "log_symbols_count", "900000")
    initializer_module = InitializerModule()
    initializer_module.init()
    initializer_module.config = config

    with patch("builtins.open") as open_mock, patch(
      "os.path.exists", return_value=True
    ):
      # Make file read calls visible
      def open_side_effect(file, mode, *args, **kwargs):
        if mode == "r":
          file_mock = MagicMock()
          file_content = "Read from " + str(file)
          file_mock.read.return_value = file_content
          file_mock.__enter__.return_value.read.return_value = file_content
          return file_mock
        else:
          return self.original_open(file, mode, *args, **kwargs)

      open_mock.side_effect = open_side_effect

      actionQueue = ActionQueue(initializer_module)
      unfreeze_flag = threading.Event()
      command_started = threading.Event()
      command_finished = threading.Event()
      python_execution_result_dict = {
        "stdout": "out",
        "stderr": "stderr",
        "structuredOut": "",
      }

      def side_effect(
        command,
        tmpoutfile,
        tmperrfile,
        override_output_files=True,
        retry=False,
        cancel_event=None,
      ):
        command_started.set()
        if not unfreeze_flag.wait(2):
          raise RuntimeError("Timed out waiting to release execution command")
        return python_execution_result_dict

      def patched_aq_execute_command(command):
        # We have to perform patching for separate thread in the same thread
        try:
          with patch.object(CustomServiceOrchestrator, "runCommand") as runCommand_mock:
            runCommand_mock.side_effect = side_effect
            actionQueue.execute_command(command)
        finally:
          command_finished.set()

      ### Test install/start/stop command ###
      # # Test successful execution with configuration tags
      python_execution_result_dict["status"] = "COMPLETE"
      python_execution_result_dict["exitcode"] = 0
      # We call method in a separate thread
      execution_thread = Thread(
        target=patched_aq_execute_command, args=(self.datanode_install_command,)
      )
      execution_thread.start()
      self.assertTrue(command_started.wait(2))
      try:
        reports = actionQueue.commandStatuses.generate_report()[CLUSTER_ID]
        expected = {
          "status": "IN_PROGRESS",
          "stderr": f"Read from {os.path.join(tempdir, 'errors-3.txt')}",
          "stdout": f"Read from {os.path.join(tempdir, 'output-3.txt')}",
          "structuredOut": f"Read from {os.path.join(tempdir, 'structured-out-3.json')}",
          "clusterId": CLUSTER_ID,
          "roleCommand": "INSTALL",
          "serviceName": "HDFS",
          "role": "DATANODE",
          "actionId": "1-1",
          "taskId": 3,
          "exitCode": 777,
        }
        self.assertEqual([expected], reports)
      finally:
        unfreeze_flag.set()
        command_finished.wait(2)
        execution_thread.join(2)

      self.assertFalse(execution_thread.is_alive())
      reports = actionQueue.commandStatuses.generate_report()[CLUSTER_ID]

      # check report
      expected = {
        "status": "COMPLETED",
        "stderr": "stderr",
        "stdout": "out\n\nCommand completed successfully!\n",
        "clusterId": CLUSTER_ID,
        "structuredOut": '""',
        "roleCommand": "INSTALL",
        "serviceName": "HDFS",
        "role": "DATANODE",
        "actionId": "1-1",
        "taskId": 3,
        "exitCode": 0,
      }
      self.assertEqual(len(reports), 1)
      self.assertEqual(reports[0], expected)

      # now should not have reports (read complete/failed reports are deleted)
      actionQueue.commandStatuses.clear_reported_reports({CLUSTER_ID: reports})
      reports = actionQueue.commandStatuses.generate_report()[CLUSTER_ID]
      self.assertEqual(len(reports), 0)

      # # Test failed execution
      python_execution_result_dict["status"] = "FAILED"
      python_execution_result_dict["exitcode"] = 13
      unfreeze_flag = threading.Event()
      command_started = threading.Event()
      command_finished = threading.Event()
      # We call method in a separate thread
      execution_thread = Thread(
        target=patched_aq_execute_command, args=(self.datanode_install_command,)
      )
      execution_thread.start()
      self.assertTrue(command_started.wait(2))
      unfreeze_flag.set()
      self.assertTrue(command_finished.wait(2))
      execution_thread.join(2)
      self.assertFalse(execution_thread.is_alive())
      reports = actionQueue.commandStatuses.generate_report()[CLUSTER_ID]

      # check report
      expected = {
        "status": "FAILED",
        "stderr": "stderr",
        "stdout": "out\n\nCommand completed successfully!\n\n\nCommand failed after 1 tries\n",
        "clusterId": CLUSTER_ID,
        "structuredOut": '""',
        "roleCommand": "INSTALL",
        "serviceName": "HDFS",
        "role": "DATANODE",
        "actionId": "1-1",
        "taskId": 3,
        "exitCode": 13,
      }
      self.assertEqual(len(reports), 1)
      self.assertEqual(reports[0], expected)

      # now should not have reports (read complete/failed reports are deleted)
      actionQueue.commandStatuses.clear_reported_reports({CLUSTER_ID: reports})
      reports = actionQueue.commandStatuses.generate_report()[CLUSTER_ID]
      self.assertEqual(len(reports), 0)

      ### Test upgrade command ###
      python_execution_result_dict["status"] = "COMPLETE"
      python_execution_result_dict["exitcode"] = 0
      unfreeze_flag = threading.Event()
      command_started = threading.Event()
      command_finished = threading.Event()
      execution_thread = Thread(
        target=patched_aq_execute_command, args=(self.datanode_upgrade_command,)
      )
      execution_thread.start()
      self.assertTrue(command_started.wait(2))
      unfreeze_flag.set()
      self.assertTrue(command_finished.wait(2))
      execution_thread.join(2)
      self.assertFalse(execution_thread.is_alive())
      reports = actionQueue.commandStatuses.generate_report()[CLUSTER_ID]
      # check report
      expected = {
        "status": "COMPLETED",
        "stderr": "stderr",
        "stdout": "out\n\nCommand completed successfully!\n\n\nCommand failed after 1 tries\n\n\nCommand completed successfully!\n",
        "clusterId": CLUSTER_ID,
        "structuredOut": '""',
        "roleCommand": "UPGRADE",
        "serviceName": "serviceName",
        "role": "role",
        "actionId": 17,
        "taskId": "taskId",
        "exitCode": 0,
      }
      self.assertEqual(len(reports), 1)
      self.assertEqual(reports[0], expected)

      # now should not have reports (read complete/failed reports are deleted)
      actionQueue.commandStatuses.clear_reported_reports({CLUSTER_ID: reports})
      reports = actionQueue.commandStatuses.generate_report()[CLUSTER_ID]
      self.assertEqual(len(reports), 0)

  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  @patch.object(CustomServiceOrchestrator, "runCommand")
  @patch("CommandStatusDict.CommandStatusDict")
  def test_store_configuration_tags(
    self, command_status_dict_mock, cso_runCommand_mock
  ):
    custom_service_orchestrator_execution_result_dict = {
      "stdout": "out",
      "stderr": "stderr",
      "structuredOut": "",
      "exitcode": 0,
    }
    cso_runCommand_mock.return_value = custom_service_orchestrator_execution_result_dict

    config = AmbariConfig()
    tempdir = tempfile.gettempdir()
    config.set("agent", "prefix", tempdir)
    config.set("agent", "cache_dir", "/var/lib/ambari-agent/cache")
    config.set("agent", "tolerate_download_failures", "true")
    dummy_controller = MagicMock()

    initializer_module = InitializerModule()
    initializer_module.init()

    actionQueue = ActionQueue(initializer_module)
    actionQueue.execute_command(self.datanode_restart_command)
    reports = actionQueue.commandStatuses.generate_report()[CLUSTER_ID]
    expected = {
      "status": "COMPLETED",
      "stderr": "stderr",
      "stdout": "out\n\nCommand completed successfully!\n",
      "structuredOut": '""',
      "roleCommand": "CUSTOM_COMMAND",
      "serviceName": "HDFS",
      "role": "DATANODE",
      "actionId": "1-1",
      "taskId": 9,
      "clusterId": CLUSTER_ID,
      "exitCode": 0,
    }
    # Agent caches configurationTags if custom_command RESTART completed
    self.assertEqual(len(reports), 1)
    self.assertEqual(expected, reports[0])

  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  @patch.object(ActualConfigHandler, "write_client_components")
  @patch.object(CustomServiceOrchestrator, "runCommand")
  @patch("CommandStatusDict.CommandStatusDict")
  def test_store_configuration_tags_no_clients(
    self, command_status_dict_mock, cso_runCommand_mock, write_client_components_mock
  ):
    custom_service_orchestrator_execution_result_dict = {
      "stdout": "out",
      "stderr": "stderr",
      "structuredOut": "",
      "exitcode": 0,
    }
    cso_runCommand_mock.return_value = custom_service_orchestrator_execution_result_dict

    config = AmbariConfig()
    tempdir = tempfile.gettempdir()
    config.set("agent", "prefix", tempdir)
    config.set("agent", "cache_dir", "/var/lib/ambari-agent/cache")
    config.set("agent", "tolerate_download_failures", "true")
    dummy_controller = MagicMock()

    initializer_module = InitializerModule()
    initializer_module.init()

    actionQueue = ActionQueue(initializer_module)
    actionQueue.execute_command(self.datanode_restart_command_no_clients_update)
    reports = actionQueue.commandStatuses.generate_report()[CLUSTER_ID]
    expected = {
      "status": "COMPLETED",
      "stderr": "stderr",
      "stdout": "out\n\nCommand completed successfully!\n",
      "clusterId": CLUSTER_ID,
      "structuredOut": '""',
      "roleCommand": "CUSTOM_COMMAND",
      "serviceName": "HDFS",
      "role": "DATANODE",
      "actionId": "1-1",
      "taskId": 9,
      "exitCode": 0,
    }
    # Agent caches configurationTags if custom_command RESTART completed
    self.assertEqual(len(reports), 1)
    self.assertEqual(expected, reports[0])

  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  @patch.object(CustomServiceOrchestrator, "runCommand")
  @patch("CommandStatusDict.CommandStatusDict")
  def test_refresh_queues_custom_command(
    self, command_status_dict_mock, cso_runCommand_mock
  ):
    custom_service_orchestrator_execution_result_dict = {
      "stdout": "out",
      "stderr": "stderr",
      "structuredOut": "",
      "exitcode": 0,
    }
    cso_runCommand_mock.return_value = custom_service_orchestrator_execution_result_dict

    config = AmbariConfig()
    tempdir = tempfile.gettempdir()
    config.set("agent", "prefix", tempdir)
    config.set("agent", "cache_dir", "/var/lib/ambari-agent/cache")
    config.set("agent", "tolerate_download_failures", "true")
    dummy_controller = MagicMock()

    initializer_module = InitializerModule()
    initializer_module.init()

    actionQueue = ActionQueue(initializer_module)
    actionQueue.execute_command(self.yarn_refresh_queues_custom_command)

    reports = actionQueue.commandStatuses.generate_report()[CLUSTER_ID]
    expected = {
      "status": "COMPLETED",
      "stderr": "stderr",
      "stdout": "out\n\nCommand completed successfully!\n",
      "clusterId": CLUSTER_ID,
      "structuredOut": '""',
      "roleCommand": "CUSTOM_COMMAND",
      "serviceName": "YARN",
      "role": "RESOURCEMANAGER",
      "actionId": "1-1",
      "taskId": 9,
      "exitCode": 0,
    }
    self.assertEqual(len(reports), 1)
    self.assertEqual(expected, reports[0])

  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  @patch.object(CustomServiceOrchestrator, "runCommand")
  @patch("CommandStatusDict.CommandStatusDict")
  def test_store_configuration_tags_on_custom_start_command(
    self, command_status_dict_mock, cso_runCommand_mock
  ):
    custom_service_orchestrator_execution_result_dict = {
      "stdout": "out",
      "stderr": "stderr",
      "structuredOut": "",
      "exitcode": 0,
    }
    cso_runCommand_mock.return_value = custom_service_orchestrator_execution_result_dict

    config = AmbariConfig()
    tempdir = tempfile.gettempdir()
    config.set("agent", "prefix", tempdir)
    config.set("agent", "cache_dir", "/var/lib/ambari-agent/cache")
    config.set("agent", "tolerate_download_failures", "true")

    initializer_module = InitializerModule()
    initializer_module.init()

    actionQueue = ActionQueue(initializer_module)
    actionQueue.execute_command(self.datanode_start_custom_command)
    reports = actionQueue.commandStatuses.generate_report()[CLUSTER_ID]
    expected = {
      "status": "COMPLETED",
      "stderr": "stderr",
      "stdout": "out\n\nCommand completed successfully!\n",
      "structuredOut": '""',
      "roleCommand": "CUSTOM_COMMAND",
      "serviceName": "HDFS",
      "role": "DATANODE",
      "actionId": "1-1",
      "taskId": 9,
      "exitCode": 0,
      "clusterId": CLUSTER_ID,
    }
    self.assertEqual(len(reports), 1)
    self.assertEqual(expected, reports[0])

  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  @patch.object(CustomServiceOrchestrator, "runCommand")
  @patch("CommandStatusDict.CommandStatusDict")
  def test_store_config_tags_on_install_client_command(
    self, command_status_dict_mock, cso_runCommand_mock
  ):
    custom_service_orchestrator_execution_result_dict = {
      "stdout": "out",
      "stderr": "stderr",
      "structuredOut": "",
      "exitcode": 0,
    }
    cso_runCommand_mock.return_value = custom_service_orchestrator_execution_result_dict

    tez_client_install_command = {
      "commandType": "EXECUTION_COMMAND",
      "role": "TEZ_CLIENT",
      "roleCommand": "INSTALL",
      "commandId": "1-1",
      "taskId": 9,
      "clusterName": "cc",
      "serviceName": "TEZ",
      "configurations": {"global": {}},
      "configurationTags": {"global": {"tag": "v123"}},
      "hostLevelParams": {},
      "clusterId": CLUSTER_ID,
    }
    LiveStatus.CLIENT_COMPONENTS = (
      {"serviceName": "TEZ", "componentName": "TEZ_CLIENT"},
    )

    config = AmbariConfig()
    tempdir = tempfile.gettempdir()
    config.set("agent", "prefix", tempdir)
    config.set("agent", "cache_dir", "/var/lib/ambari-agent/cache")
    config.set("agent", "tolerate_download_failures", "true")
    dummy_controller = MagicMock()

    initializer_module = InitializerModule()
    initializer_module.init()

    actionQueue = ActionQueue(initializer_module)
    actionQueue.execute_command(tez_client_install_command)

  @patch.object(AmbariConfig, "get_parallel_exec_option")
  @patch.object(ActionQueue, "process_command")
  @patch.object(CustomServiceOrchestrator, "__init__")
  def test_parallel_exec(
    self, CustomServiceOrchestrator_mock, process_command_mock, gpeo_mock
  ):
    CustomServiceOrchestrator_mock.return_value = None

    gpeo_mock.return_value = 1
    actionQueue, initializer_module = self.create_action_queue(parallel_execution=1)
    commands_processed = threading.Event()

    def process_command(_command):
      if process_command_mock.call_count >= 2:
        commands_processed.set()

    process_command_mock.side_effect = process_command
    actionQueue.put([self.datanode_install_command, self.hbase_install_command])
    self.assertEqual(2, actionQueue.commandQueue.qsize())
    actionQueue.start()
    self.assertTrue(commands_processed.wait(2))
    initializer_module.stop_event.set()
    actionQueue.interrupt()
    actionQueue.join(3)
    self.assertEqual(actionQueue.is_alive(), False, "Action queue is not stopped.")
    self.assertEqual(2, process_command_mock.call_count)
    process_command_mock.assert_has_calls(
      [call(self.datanode_install_command), call(self.hbase_install_command)],
      any_order=True,
    )

  @patch.object(AmbariConfig, "get_parallel_exec_option")
  @patch.object(ActionQueue, "process_command")
  @patch.object(CustomServiceOrchestrator, "__init__")
  def test_parallel_exec_no_retry(
    self,
    CustomServiceOrchestrator_mock,
    process_command_mock,
    gpeo_mock,
  ):
    CustomServiceOrchestrator_mock.return_value = None
    gpeo_mock.return_value = 1
    actionQueue, initializer_module = self.create_action_queue(parallel_execution=1)
    commands_processed = threading.Event()

    def process_command(_command):
      if process_command_mock.call_count >= 2:
        commands_processed.set()

    process_command_mock.side_effect = process_command
    actionQueue.put(
      [self.datanode_install_no_retry_command, self.snamenode_install_command]
    )
    self.assertEqual(2, actionQueue.commandQueue.qsize())
    actionQueue.start()
    self.assertTrue(commands_processed.wait(2))
    initializer_module.stop_event.set()
    actionQueue.interrupt()
    actionQueue.join(3)
    self.assertEqual(actionQueue.is_alive(), False, "Action queue is not stopped.")
    self.assertEqual(2, process_command_mock.call_count)
    self.assertFalse(actionQueue.worker_futures)
    process_command_mock.assert_has_calls(
      [
        call(self.datanode_install_no_retry_command),
        call(self.snamenode_install_command),
      ]
    )

  def test_parallel_executor_has_no_unbounded_submission_queue(self):
    action_queue, initializer_module = self.create_action_queue(
      parallel_execution=1, max_parallel_actions=2
    )
    release_workers = threading.Event()
    two_workers_started = threading.Event()
    all_commands_processed = threading.Event()
    state_lock = threading.Lock()
    state = {"active": 0, "completed": 0, "maximum": 0}

    def process_command(command):
      with state_lock:
        state["active"] += 1
        state["maximum"] = max(state["maximum"], state["active"])
        if state["active"] == 2:
          two_workers_started.set()
      release_workers.wait(5)
      with state_lock:
        state["active"] -= 1
        state["completed"] += 1
        if state["completed"] == 5:
          all_commands_processed.set()
      action_queue._finish_control(command, action_queue._control_for(command))

    commands = []
    for task_id in range(5):
      command = copy.deepcopy(self.datanode_install_command)
      command["taskId"] = task_id
      commands.append(command)

    action_queue.process_command = MagicMock(side_effect=process_command)
    action_queue.put(commands)
    action_queue.start()
    self.assertTrue(two_workers_started.wait(2))
    self.assertEqual(action_queue.process_command.call_count, 2)
    self.assertEqual(state["maximum"], 2)

    release_workers.set()
    self.assertTrue(all_commands_processed.wait(3))
    initializer_module.stop_event.set()
    action_queue.interrupt()
    action_queue.join(3)

    self.assertFalse(action_queue.is_alive())
    self.assertEqual(action_queue.process_command.call_count, len(commands))
    self.assertEqual(state["maximum"], 2)

  def test_synchronous_action_count_is_released_on_success_and_failure(self):
    action_queue, _initializer_module = self.create_action_queue(
      parallel_execution=1, max_parallel_actions=2
    )
    command = copy.deepcopy(self.datanode_install_no_retry_command)
    action_queue.process_command = MagicMock()

    action_queue._run_synchronous_command(command)

    self.assertEqual(0, action_queue.synchronous_action_count)

    action_queue.process_command.side_effect = RuntimeError("command failed")
    with self.assertRaisesRegex(RuntimeError, "command failed"):
      action_queue._run_synchronous_command(command)

    self.assertEqual(0, action_queue.synchronous_action_count)

  def test_interrupt_cancels_running_synchronous_command_before_join(self):
    action_queue, initializer_module = self.create_action_queue(parallel_execution=0)
    command = copy.deepcopy(self.datanode_install_no_retry_command)
    command_started = threading.Event()
    allow_command_to_return = threading.Event()
    cancel_all_called = threading.Event()

    def process_command(running_command):
      command_started.set()
      allow_command_to_return.wait(5)
      action_queue._finish_control(
        running_command, action_queue._control_for(running_command)
      )

    initializer_module.customServiceOrchestrator.cancel_all_commands.side_effect = (
      lambda _reason: cancel_all_called.set()
    )
    action_queue.process_command = MagicMock(side_effect=process_command)
    action_queue.put([command])
    control = action_queue._control_for(command)
    action_queue.start()

    try:
      self.assertTrue(command_started.wait(2))
      initializer_module.stop_event.set()
      action_queue.interrupt()

      self.assertTrue(control["cancel_event"].wait(1))
      self.assertTrue(cancel_all_called.wait(1))
      self.assertTrue(action_queue.is_alive())
    finally:
      allow_command_to_return.set()
      action_queue.join(3)

    self.assertFalse(action_queue.is_alive())
    self.assertGreaterEqual(
      initializer_module.customServiceOrchestrator.cancel_all_commands.call_count, 1
    )

  def test_cancel_does_not_cancel_later_generation(self):
    action_queue, _initializer_module = self.create_action_queue()
    old_command = copy.deepcopy(self.datanode_install_command)
    new_command = copy.deepcopy(self.datanode_install_command)
    new_command["commandId"] = "2-1"

    action_queue.put([old_command])
    old_control = action_queue._control_for(old_command)
    action_queue.cancel(
      [{"target_task_id": old_command["taskId"], "reason": "rescheduled"}]
    )
    action_queue.put([new_command])
    new_control = action_queue._control_for(new_command)

    self.assertTrue(old_control["cancel_event"].is_set())
    self.assertFalse(new_control["cancel_event"].is_set())
    action_queue.customServiceOrchestrator.cancel_command.assert_called_once_with(
      old_command["taskId"], "rescheduled"
    )

  def test_queued_command_cancellation_reports_failure_without_starting_process(self):
    action_queue, initializer_module = self.create_action_queue()
    command = copy.deepcopy(self.datanode_install_no_retry_command)
    initializer_module.commandStatuses.generate_report_template.side_effect = [{}, {}]
    action_queue.put([command])
    queued_command = action_queue.commandQueue.get_nowait()

    action_queue.cancel(
      [{"target_task_id": command["taskId"], "reason": "operator canceled"}]
    )
    action_queue.process_command(queued_command)

    initializer_module.customServiceOrchestrator.runCommand.assert_not_called()
    final_report = initializer_module.commandStatuses.put_command_status.call_args_list[
      -1
    ].args[1]
    self.assertEqual(CommandStatus.failed, final_report["status"])
    self.assertEqual(-signal.SIGTERM, final_report["exitCode"])
    self.assertIn("operator canceled", final_report["stderr"])
    self.assertFalse(action_queue.tasks_in_progress_or_pending())

  def test_cancel_interrupts_only_target_retry_wait(self):
    action_queue, initializer_module = self.create_action_queue()
    canceled_command = copy.deepcopy(self.datanode_install_command)
    canceled_command["commandParams"]["max_duration_for_retries"] = "60"
    other_command = copy.deepcopy(self.datanode_install_command)
    other_command["taskId"] = 4
    action_queue.put([canceled_command, other_command])
    queued_command = action_queue.commandQueue.get_nowait()
    canceled_control = action_queue._control_for(canceled_command)
    other_control = action_queue._control_for(other_command)
    first_attempt_finished = threading.Event()

    def fail_first_attempt(*_args, **_kwargs):
      first_attempt_finished.set()
      return {"stdout": "", "stderr": "first attempt failed", "exitcode": 1}

    initializer_module.customServiceOrchestrator.runCommand.side_effect = (
      fail_first_attempt
    )
    initializer_module.commandStatuses.generate_report_template.side_effect = [{}, {}]
    action_queue.get_retry_delay = MagicMock(return_value=60)
    worker = threading.Thread(target=action_queue.process_command, args=(queued_command,))
    worker.start()
    self.assertTrue(first_attempt_finished.wait(2))

    action_queue.cancel(
      [
        {
          "target_task_id": canceled_command["taskId"],
          "reason": "operator canceled during retry wait",
        }
      ]
    )
    worker.join(2)

    self.assertFalse(worker.is_alive())
    self.assertTrue(canceled_control["cancel_event"].is_set())
    self.assertFalse(other_control["cancel_event"].is_set())
    initializer_module.customServiceOrchestrator.runCommand.assert_called_once()
    final_report = initializer_module.commandStatuses.put_command_status.call_args_list[
      -1
    ].args[1]
    self.assertEqual(CommandStatus.failed, final_report["status"])
    self.assertIn("operator canceled during retry wait", final_report["stderr"])

  def test_finished_newer_generation_still_suppresses_old_failure(self):
    action_queue, _initializer_module = self.create_action_queue()
    old_command = copy.deepcopy(self.datanode_install_command)
    new_command = copy.deepcopy(self.datanode_install_command)
    new_command["commandId"] = "2-1"

    action_queue.put([old_command])
    old_control = action_queue._control_for(old_command)
    action_queue.cancel(
      [{"target_task_id": old_command["taskId"], "reason": "rescheduled"}]
    )
    action_queue.put([new_command])
    new_control = action_queue._control_for(new_command)

    action_queue._finish_control(new_command, new_control)

    self.assertTrue(
      action_queue._has_newer_generation(old_command["taskId"], old_control)
    )
    action_queue._finish_control(old_command, old_control)
    self.assertNotIn(str(old_command["taskId"]), action_queue.task_generations)

  def test_cancel_arriving_during_successful_process_exit_reports_failure(self):
    action_queue, initializer_module = self.create_action_queue()
    command = copy.deepcopy(self.datanode_install_no_retry_command)
    control = action_queue._register_command(command)
    initializer_module.commandStatuses.generate_report_template.side_effect = [{}, {}]

    def finish_after_cancel(*_args, **_kwargs):
      control["reason"] = "operator canceled command"
      control["cancel_event"].set()
      return {"stdout": "partial output", "stderr": "", "exitcode": 0}

    initializer_module.customServiceOrchestrator.runCommand.side_effect = (
      finish_after_cancel
    )

    action_queue.execute_command(command, control)

    final_report = initializer_module.commandStatuses.put_command_status.call_args_list[
      -1
    ].args[1]
    self.assertEqual(CommandStatus.failed, final_report["status"])
    self.assertEqual(-signal.SIGTERM, final_report["exitCode"])
    self.assertIn("operator canceled command", final_report["stderr"])

  def test_shutdown_reports_and_cleans_unstarted_background_command(self):
    action_queue, initializer_module = self.create_action_queue()
    command = copy.deepcopy(self.background_command)
    initializer_module.commandStatuses.generate_report_template.side_effect = [
      {},
      {},
    ]
    action_queue.put([command])

    action_queue._finish_queued_commands()

    self.assertTrue(action_queue.backgroundCommandQueue.empty())
    self.assertFalse(action_queue.tasks_in_progress_or_pending())
    final_report = initializer_module.commandStatuses.put_command_status.call_args_list[
      -1
    ].args[1]
    self.assertEqual(CommandStatus.failed, final_report["status"])
    self.assertEqual(-signal.SIGTERM, final_report["exitCode"])
    self.assertIn("Ambari Agent is stopping", final_report["stderr"])

  def test_background_start_failure_cleans_handle_and_task_registration(self):
    action_queue, initializer_module = self.create_action_queue()
    command = copy.deepcopy(self.background_command)
    initializer_module.commandStatuses.generate_report_template.side_effect = [
      {},
      {},
    ]
    initializer_module.customServiceOrchestrator.runCommand.return_value = {
      "stdout": "",
      "stderr": "thread start failed",
      "structuredOut": {},
      "exitcode": 1,
    }
    action_queue.put([command])
    handle = command["__handle"]

    action_queue.process_background_queue_safe_empty()

    self.assertNotIn(handle, action_queue.background_handles)
    self.assertFalse(action_queue.tasks_in_progress_or_pending())
    self.assertNotIn(id(command), action_queue.command_controls)
    final_report = initializer_module.commandStatuses.put_command_status.call_args_list[
      -1
    ].args[1]
    self.assertEqual(CommandStatus.failed, final_report["status"])
    self.assertEqual(1, final_report["exitCode"])
    self.assertIn("thread start failed", final_report["stderr"])

  def test_background_launch_race_reports_control_cancellation_reason(self):
    action_queue, initializer_module = self.create_action_queue()
    command = copy.deepcopy(self.background_command)
    action_queue.put([command])
    queued_command = action_queue.backgroundCommandQueue.get_nowait()
    handle = queued_command["__handle"]
    handle.exitCode = -signal.SIGTERM
    control = handle.control
    control["reason"] = "operator canceled during launch"
    control["cancel_event"].set()
    initializer_module.customServiceOrchestrator.command_canceled_reason.return_value = (
      None
    )
    initializer_module.commandStatuses.generate_report_template.return_value = {}

    action_queue.on_background_command_complete_callback(
      {
        "stdout": "",
        "stderr": "",
        "structuredOut": {},
        "exitcode": -signal.SIGTERM,
      },
      handle,
    )

    report = initializer_module.commandStatuses.put_command_status.call_args.args[1]
    self.assertEqual(CommandStatus.failed, report["status"])
    self.assertIn("operator canceled during launch", report["stderr"])
    self.assertFalse(action_queue.tasks_in_progress_or_pending())
    self.assertNotIn(handle, action_queue.background_handles)

  def test_stop_interrupts_worker_slot_wait_and_cleans_queued_commands(self):
    action_queue, initializer_module = self.create_action_queue(
      parallel_execution=1, max_parallel_actions=1
    )
    worker_started = threading.Event()
    release_worker = threading.Event()

    def process_command(command):
      control = action_queue._control_for(command)
      if control["cancel_event"].is_set():
        action_queue._finish_control(command, control)
        return
      worker_started.set()
      release_worker.wait(5)
      action_queue._finish_control(command, control)

    initializer_module.customServiceOrchestrator.cancel_all_commands.side_effect = (
      lambda _reason: release_worker.set()
    )
    action_queue.process_command = MagicMock(side_effect=process_command)
    commands = []
    for task_id in range(3):
      command = copy.deepcopy(self.datanode_install_command)
      command["taskId"] = task_id
      commands.append(command)
    action_queue.put(commands)
    action_queue.start()
    self.assertTrue(worker_started.wait(2))

    initializer_module.stop_event.set()
    action_queue.interrupt()
    action_queue.join(3)

    self.assertFalse(action_queue.is_alive())
    initializer_module.customServiceOrchestrator.cancel_all_commands.assert_called()
    self.assertFalse(action_queue.tasks_in_progress_or_pending())

  def test_background_commands_respect_the_configured_concurrency_limit(self):
    action_queue, _initializer_module = self.create_action_queue(
      parallel_execution=1, max_parallel_actions=2
    )
    commands = []
    for task_id in range(3):
      command = copy.deepcopy(self.background_command)
      command["taskId"] = task_id
      action_queue.put([command])
      commands.append(command)

    alive_thread = MagicMock()
    alive_thread.is_alive.return_value = True

    def start_background(command):
      command["__handle"].thread = alive_thread

    action_queue.process_command = MagicMock(side_effect=start_background)

    action_queue.process_background_queue_safe_empty()

    self.assertEqual(2, action_queue.process_command.call_count)
    self.assertEqual(1, action_queue.backgroundCommandQueue.qsize())

  def test_background_and_worker_commands_share_the_concurrency_limit(self):
    action_queue, _initializer_module = self.create_action_queue(
      parallel_execution=1, max_parallel_actions=2
    )
    worker = MagicMock()
    worker.done.return_value = False
    action_queue.worker_futures.add(worker)
    commands = []
    for task_id in range(2):
      command = copy.deepcopy(self.background_command)
      command["taskId"] = task_id
      action_queue.put([command])
      commands.append(command)

    alive_thread = MagicMock()
    alive_thread.is_alive.return_value = True

    def start_background(command):
      command["__handle"].thread = alive_thread

    action_queue.process_command = MagicMock(side_effect=start_background)

    action_queue.process_background_queue_safe_empty()

    action_queue.process_command.assert_called_once_with(commands[0])
    self.assertEqual(1, action_queue.backgroundCommandQueue.qsize())

  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  @patch.object(CustomServiceOrchestrator, "runCommand")
  @patch.object(CustomServiceOrchestrator, "__init__")
  def test_execute_background_command(
    self,
    CustomServiceOrchestrator_mock,
    runCommand_mock,
  ):
    CustomServiceOrchestrator_mock.return_value = None

    background_thread = MagicMock()
    background_thread.is_alive.return_value = True

    def launch_background(command, *_args, **_kwargs):
      command["__handle"].thread = background_thread
      return {"exitcode": 777, "stdout": "", "stderr": ""}

    runCommand_mock.side_effect = launch_background

    initializer_module = InitializerModule()
    initializer_module.init()

    actionQueue = ActionQueue(initializer_module)

    execute_command = copy.deepcopy(self.background_command)
    actionQueue.put([execute_command])
    actionQueue.process_background_queue_safe_empty()
    # actionQueue.controller.statusCommandExecutor.process_results();

    # assert that python execturor start
    self.assertTrue(runCommand_mock.called)
    runningCommand = actionQueue.commandStatuses.current_state.get(
      execute_command["taskId"]
    )
    self.assertTrue(runningCommand is not None)
    self.assertEqual(runningCommand[1]["status"], CommandStatus.in_progress)

    reports = actionQueue.commandStatuses.generate_report()[CLUSTER_ID]
    self.assertEqual(len(reports), 1)

    handle = execute_command["__handle"]
    actionQueue._finish_control(execute_command, handle.control)
    actionQueue.background_handles.discard(handle)

  cancel_background_command = {
    "commandType": "CANCEL_COMMAND",
    "role": "AMBARI_SERVER_ACTION",
    "roleCommand": "ABORT",
    "commandId": "2--1",
    "taskId": 20,
    "clusterName": "c1",
    "serviceName": "",
    "hostname": "c6401",
    "roleParams": {"cancelTaskIdTargets": "13,14"},
  }

  def test_hide_passwords_no_matching_password(self):
    self.assertEqual(hide_passwords(None), None)
    self.assertEqual(
      hide_passwords("No password in this text"), "No password in this text"
    )
    self.assertEqual(
      hide_passwords("No 'password' 'in' this text'"), "No 'password' 'in' this text'"
    )
    self.assertEqual(
      hide_passwords("No 'password': in this text"), "No 'password': in this text"
    )
    self.assertEqual(
      hide_passwords("No u'password': u'' in this text"),
      "No u'password': u'' in this text",
    )

  def test_hide_passwords(self):
    self.assertEqual(
      hide_passwords("u'password': u'changeIT!'"), "u'password': u'[PROTECTED]'"
    )
    self.assertEqual(
      hide_passwords("'password': 'password'"), "'password': '[PROTECTED]'"
    )
    self.assertEqual(
      hide_passwords("'some.password': 'password', 'other.password': 'password',"),
      "'some.password': '[PROTECTED]', 'other.password': '[PROTECTED]',",
    )
    self.assertEqual(
      hide_passwords("u'metrics_grafana_password': u'mypassword123!'"),
      "u'metrics_grafana_password': u'[PROTECTED]'",
    )

    self.assertEqual(
      hide_passwords(
        "u'metrics_grafana_username': u'admin', u'metrics_grafana_password': u'mypassword123!', some text, u'clientssl.keystore.password': u'myKeyFilePassword', another text, "
      ),
      "u'metrics_grafana_username': u'admin', u'metrics_grafana_password': u'[PROTECTED]', some text, u'clientssl.keystore.password': u'[PROTECTED]', another text, ",
    )
    self.assertEqual(
      hide_passwords(
        '{"api_token": "abc-123", "client_secret": "secret-value"}'
      ),
      '{"api_token": "[PROTECTED]", "client_secret": "[PROTECTED]"}',
    )
    self.assertEqual(
      hide_passwords("db.password=changeit --access-token bearer-value"),
      "db.password=[PROTECTED] --access-token [PROTECTED]",
    )
