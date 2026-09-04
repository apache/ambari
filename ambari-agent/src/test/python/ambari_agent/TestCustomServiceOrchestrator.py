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

from concurrent.futures import ThreadPoolExecutor
import os

import pprint

from ambari_agent.models.commands import CommandStatus
from ambari_commons import shell

from unittest import TestCase
import threading
import tempfile
import traceback
from threading import Thread

from unittest.mock import ANY, MagicMock, call, patch
from only_for_platform import os_distro_value
import io
import sys

from ambari_agent.ActionQueue import ActionQueue
from ambari_agent.AgentException import AgentException
from ambari_agent.AmbariConfig import AmbariConfig
from ambari_agent.BackgroundCommandExecutionHandle import (
  BackgroundCommandExecutionHandle,
)
from ambari_agent.CustomServiceOrchestrator import CustomServiceOrchestrator
from ambari_agent.FileCache import FileCache
from ambari_agent.PythonExecutor import PythonExecutor
from ambari_commons import OSCheck
from ambari_agent.InitializerModule import InitializerModule
from ambari_agent.ConfigurationBuilder import ConfigurationBuilder
from ambari_agent.CommandHooksOrchestrator import HooksOrchestrator, ResolvedHooks


def patch_executor_output(python_executor):
  def launch_python_subprocess(command, tmpout, tmperr, env=None):
    process = MagicMock()
    process.pid = 33
    process.returncode = 0
    return process

  python_executor.launch_python_subprocess = launch_python_subprocess
  python_executor.open_subprocess_files = MagicMock(
    return_value=(MagicMock(), MagicMock())
  )
  python_executor.read_result_from_files = MagicMock(
    return_value=("process_out", "process_err", '{"a": "b."}')
  )


def wrapped(func, before=None, after=None):
  def wrapper(*args, **kwargs):
    if before is not None:
      before(*args, **kwargs)
    ret = func(*args, **kwargs)
    if after is not None:
      after(*args, **kwargs)
    return ret

  return wrapper


class TestCustomServiceOrchestrator(TestCase):
  def setUp(self):
    # disable stdout
    out = io.StringIO()
    sys.stdout = out
    # generate sample config
    self.test_directory = tempfile.TemporaryDirectory()
    tmpdir = self.test_directory.name
    self.config = AmbariConfig()
    self.config.set("agent", "prefix", tmpdir)
    self.config.set("agent", "cache_dir", os.path.join(tmpdir, "cachedir"))
    self.config.set("python", "custom_actions_dir", tmpdir)
    self.config._recalculate_cache_paths()
    self.config_patcher = patch.object(
      AmbariConfig, "get_resolved_config", return_value=self.config
    )
    self.config_patcher.start()

  def tearDown(self):
    self.config_patcher.stop()
    self.test_directory.cleanup()
    sys.stdout = sys.__stdout__

  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  @patch("ambari_agent.hostname.public_hostname")
  @patch.object(FileCache, "__init__")
  def test_dump_command_to_json_with_retry(
    self, FileCache_mock, hostname_mock
  ):
    FileCache_mock.return_value = None
    hostname_mock.return_value = "test.hst"
    command = {
      "commandType": "EXECUTION_COMMAND",
      "role": "DATANODE",
      "roleCommand": "INSTALL",
      "commandId": "1-1",
      "taskId": 3,
      "clusterName": "cc",
      "serviceName": "HDFS",
      "configurations": {"global": {}},
      "configurationTags": {"global": {"tag": "v1"}},
      "clusterHostInfo": {
        "namenode_host": ["1"],
        "slave_hosts": ["0", "1"],
        "all_racks": ["/default-rack:0"],
        "ambari_server_host": "a.b.c",
        "ambari_server_port": "123",
        "ambari_server_use_ssl": "false",
        "all_ipv4_ips": ["192.168.12.101:0"],
        "all_hosts": ["h1.hortonworks.com", "h2.hortonworks.com"],
        "all_ping_ports": ["8670:0,1"],
      },
      "hostLevelParams": {},
    }

    tempdir = tempfile.gettempdir()
    initializer_module = InitializerModule()
    initializer_module.init()
    initializer_module.config.set("agent", "prefix", tempdir)
    orchestrator = CustomServiceOrchestrator(initializer_module)
    # Test dumping EXECUTION_COMMAND
    json_file = orchestrator.dump_command_to_json(command)
    self.assertTrue(os.path.exists(json_file))
    self.assertTrue(os.path.getsize(json_file) > 0)
    self.assertEqual(oct(os.stat(json_file).st_mode & 0o777), "0o600")
    self.assertTrue(json_file.endswith("command-3.json"))
    os.unlink(json_file)
    # Test dumping STATUS_COMMAND
    json_file = orchestrator.dump_command_to_json(command, True)
    self.assertTrue(os.path.exists(json_file))
    self.assertTrue(os.path.getsize(json_file) > 0)
    self.assertEqual(oct(os.stat(json_file).st_mode & 0o777), "0o600")
    self.assertTrue(json_file.endswith("command-3.json"))
    os.unlink(json_file)
    # Testing side effect of dump_command_to_json
    self.assertNotEqual(command["clusterHostInfo"], {})

  def test_dump_command_to_json_failure_preserves_existing_private_file(self):
    orchestrator = CustomServiceOrchestrator.__new__(CustomServiceOrchestrator)
    with tempfile.TemporaryDirectory() as command_directory:
      orchestrator.tmp_dir = command_directory
      command_path = os.path.join(command_directory, "command-3.json")
      with open(command_path, "w", encoding="utf-8") as stream:
        stream.write("previous-command")
      os.chmod(command_path, 0o644)
      command = {
        "commandType": "EXECUTION_COMMAND",
        "taskId": 3,
        "unserializable": {"set-value"},
      }

      with self.assertRaises(TypeError):
        orchestrator.dump_command_to_json(command)

      with open(command_path, encoding="utf-8") as stream:
        self.assertEqual("previous-command", stream.read())
      self.assertEqual(
        [],
        [
          name
          for name in os.listdir(command_directory)
          if name.startswith(".command-")
        ],
      )

  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  @patch("os.path.exists")
  @patch.object(FileCache, "__init__")
  def test_resolve_script_path(self, FileCache_mock, exists_mock):
    FileCache_mock.return_value = None
    orchestrator = CustomServiceOrchestrator.__new__(CustomServiceOrchestrator)
    # Testing existing path
    exists_mock.return_value = True
    path = orchestrator.resolve_script_path(
      os.path.join("HBASE", "package"), os.path.join("scripts", "hbase_master.py")
    )
    self.assertEqual(
      os.path.join("HBASE", "package", "scripts", "hbase_master.py"), path
    )
    # Testing not existing path
    exists_mock.return_value = False
    try:
      orchestrator.resolve_script_path(
        "/HBASE", os.path.join("scripts", "hbase_master.py")
      )
      self.fail("ExpectedException not thrown")
    except AgentException:
      pass  # Expected

  @patch.object(ConfigurationBuilder, "get_configuration")
  @patch.object(CustomServiceOrchestrator, "resolve_script_path")
  @patch.object(HooksOrchestrator, "resolve_hooks")
  @patch.object(FileCache, "get_host_scripts_base_dir")
  @patch.object(FileCache, "get_service_base_dir")
  @patch.object(CustomServiceOrchestrator, "dump_command_to_json")
  @patch.object(PythonExecutor, "run_file")
  @patch.object(FileCache, "__init__")
  def test_runCommand(
    self,
    FileCache_mock,
    run_file_mock,
    dump_command_to_json_mock,
    get_service_base_dir_mock,
    get_host_scripts_base_dir_mock,
    resolve_hooks_mock,
    resolve_script_path_mock,
    get_configuration_mock,
  ):
    FileCache_mock.return_value = None
    command = {
      "commandType": "EXECUTION_COMMAND",
      "role": "REGION_SERVER",
      "clusterLevelParams": {
        "stack_name": "HDP",
        "stack_version": "2.0.7",
      },
      "ambariLevelParams": {"jdk_location": "some_location"},
      "commandParams": {
        "script_type": "PYTHON",
        "script": "scripts/hbase_regionserver.py",
        "command_timeout": "600",
        "service_package_folder": "HBASE",
      },
      "taskId": "3",
      "roleCommand": "INSTALL",
      "clusterId": "-1",
    }
    get_configuration_mock.return_value = command

    get_host_scripts_base_dir_mock.return_value = "/host_scripts"
    get_service_base_dir_mock.return_value = "/basedir/"
    resolve_script_path_mock.return_value = "/basedir/scriptpath"
    hook = (
      "/hooks_dir/prefix-command/scripts/hook.py",
      "/hooks_dir/prefix-command",
    )
    resolve_hooks_mock.return_value = ResolvedHooks([hook], [hook])
    initializer_module = InitializerModule()
    initializer_module.init()
    orchestrator = CustomServiceOrchestrator(initializer_module)
    unix_process_id = 111
    orchestrator.commands_in_progress = {
      (command["taskId"], threading.get_ident()): unix_process_id
    }
    # normal run case
    run_file_mock.return_value = {
      "stdout": "sss",
      "stderr": "eee",
      "exitcode": 0,
    }
    ret = orchestrator.runCommand(command, "out.txt", "err.txt")
    self.assertEqual(ret["exitcode"], 0)
    self.assertTrue(run_file_mock.called)
    self.assertEqual(run_file_mock.call_count, 3)

    # running a status command
    run_file_mock.reset_mock()

    def return_traceback(*args, **kwargs):
      return {
        "stderr": traceback.format_exc(),
        "stdout": "",
        "exitcode": 0,
      }

    run_file_mock.side_effect = return_traceback

    status_command = dict(command)
    status_command["commandType"] = "STATUS_COMMAND"
    del status_command["taskId"]
    del status_command["roleCommand"]
    ret = orchestrator.runCommand(status_command, "out.txt", "err.txt")
    self.assertEqual("NoneType: None\n", ret["stderr"])

    run_file_mock.reset_mock()

    # Case when we force another command
    run_file_mock.return_value = {
      "stdout": "sss",
      "stderr": "eee",
      "exitcode": 0,
    }
    ret = orchestrator.runCommand(
      command,
      "out.txt",
      "err.txt",
      forced_command_name=CustomServiceOrchestrator.SCRIPT_TYPE_PYTHON,
    )
    ## Check that override_output_files was true only during first call
    print(run_file_mock)
    self.assertEqual(run_file_mock.call_args_list[0][0][8], True)
    self.assertEqual(run_file_mock.call_args_list[1][0][8], False)
    self.assertEqual(run_file_mock.call_args_list[2][0][8], False)
    ## Check that forced_command_name was taken into account
    self.assertEqual(
      run_file_mock.call_args_list[0][0][1][0],
      CustomServiceOrchestrator.SCRIPT_TYPE_PYTHON,
    )

    run_file_mock.reset_mock()

    # unknown script type case
    command["commandParams"]["script_type"] = "SOME_TYPE"
    ret = orchestrator.runCommand(command, "out.txt", "err.txt")
    self.assertEqual(ret["exitcode"], 1)
    self.assertFalse(run_file_mock.called)
    self.assertTrue("Unknown script type" in ret["stdout"])

    # By default returns empty dictionary
    self.assertEqual(ret["structuredOut"], "{}")

    pass

  @patch.object(ConfigurationBuilder, "get_configuration")
  @patch("ambari_commons.shell.kill_process_with_children")
  @patch.object(CustomServiceOrchestrator, "resolve_script_path")
  @patch.object(HooksOrchestrator, "resolve_hooks")
  @patch.object(FileCache, "get_host_scripts_base_dir")
  @patch.object(FileCache, "get_service_base_dir")
  @patch.object(CustomServiceOrchestrator, "dump_command_to_json")
  @patch.object(PythonExecutor, "run_file")
  @patch.object(FileCache, "__init__")
  def test_cancel_command(
    self,
    FileCache_mock,
    run_file_mock,
    dump_command_to_json_mock,
    get_service_base_dir_mock,
    get_host_scripts_base_dir_mock,
    resolve_hooks_mock,
    resolve_script_path_mock,
    kill_process_with_children_mock,
    get_configuration_mock,
  ):
    FileCache_mock.return_value = None
    command = {
      "role": "REGION_SERVER",
      "clusterLevelParams": {"stack_name": "HDP", "stack_version": "2.0.7"},
      "ambariLevelParams": {"jdk_location": "some_location"},
      "commandParams": {
        "script_type": "PYTHON",
        "script": "scripts/hbase_regionserver.py",
        "command_timeout": "600",
        "service_package_folder": "HBASE",
      },
      "taskId": "3",
      "roleCommand": "INSTALL",
      "clusterId": "-1",
    }
    get_configuration_mock.return_value = command

    get_host_scripts_base_dir_mock.return_value = "/host_scripts"
    get_service_base_dir_mock.return_value = "/basedir/"
    resolve_script_path_mock.return_value = "/basedir/scriptpath"
    resolve_hooks_mock.return_value = None
    initializer_module = InitializerModule()
    initializer_module.init()
    orchestrator = CustomServiceOrchestrator(initializer_module)
    unix_process_id = 111
    run_file_mock_return_value = {
      "stdout": "killed",
      "stderr": "killed",
      "exitcode": 1,
    }
    process_registered = threading.Event()
    allow_process_exit = threading.Event()

    def side_effect(*args, **kwargs):
      orchestrator.map_task_to_process(command["taskId"], unix_process_id)
      process_registered.set()
      if not allow_process_exit.wait(2):
        raise AssertionError("test did not release the mocked command process")
      return run_file_mock_return_value

    run_file_mock.side_effect = side_effect

    _, out = tempfile.mkstemp()
    _, err = tempfile.mkstemp()
    pool = ThreadPoolExecutor(max_workers=1)
    future = pool.submit(orchestrator.runCommand, command, out, err)

    self.assertTrue(process_registered.wait(2))
    orchestrator.cancel_command(command["taskId"], "reason")
    allow_process_exit.set()

    ret = future.result(timeout=2)
    pool.shutdown(wait=True)

    self.assertEqual(ret["exitcode"], 1)
    self.assertEqual(ret["stdout"], "killed\nCommand aborted. Reason: 'reason'")
    self.assertEqual(ret["stderr"], "killed\nCommand aborted. Reason: 'reason'")

    self.assertTrue(kill_process_with_children_mock.called)
    self.assertFalse(
      any(
        execution_key[0] == command["taskId"]
        for execution_key in orchestrator.commands_in_progress
      )
    )
    self.assertTrue(os.path.exists(out))
    self.assertTrue(os.path.exists(err))
    try:
      os.remove(out)
      os.remove(err)
    except:
      pass

  def test_run_command_preserves_early_parse_failure(self):
    initializer_module = InitializerModule()
    initializer_module.init()
    orchestrator = CustomServiceOrchestrator(initializer_module)
    orchestrator.generate_command = MagicMock(side_effect=ValueError("invalid command"))

    result = orchestrator.runCommand({}, "out.txt", "err.txt")

    self.assertEqual(1, result["exitcode"])
    self.assertIn("invalid command", result["stderr"])
    self.assertEqual({}, orchestrator.commands_in_progress)

  @patch("ambari_commons.shell.kill_process_with_children")
  def test_cancel_command_attempts_every_process_for_same_task(
    self, kill_process_with_children_mock
  ):
    orchestrator = CustomServiceOrchestrator.__new__(CustomServiceOrchestrator)
    orchestrator.commands_in_progress_lock = threading.RLock()
    orchestrator.commands_in_progress = {("3", 101): 111, ("3", 202): 222}
    kill_process_with_children_mock.side_effect = [OSError("already gone"), None]

    orchestrator.cancel_command("3", "rescheduled")

    self.assertEqual(
      [call(111), call(222)], kill_process_with_children_mock.call_args_list
    )
    self.assertEqual(
      {("3", 101): "rescheduled", ("3", 202): "rescheduled"},
      orchestrator.commands_in_progress,
    )

  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  @patch.object(ConfigurationBuilder, "get_configuration")
  @patch.object(CustomServiceOrchestrator, "get_py_executor")
  @patch("ambari_commons.shell.kill_process_with_children")
  @patch.object(FileCache, "__init__")
  @patch.object(CustomServiceOrchestrator, "resolve_script_path")
  @patch.object(HooksOrchestrator, "resolve_hooks")
  def test_cancel_backgound_command(
    self,
    resolve_hooks_mock,
    resolve_script_path_mock,
    FileCache_mock,
    kill_process_with_children_mock,
    get_py_executor_mock,
    get_configuration_mock,
  ):
    FileCache_mock.return_value = None
    FileCache_mock.cache_dir = MagicMock()
    resolve_hooks_mock.return_value = None
    initializer_module = InitializerModule()
    initializer_module.init()

    actionQueue = ActionQueue(initializer_module)
    orchestrator = CustomServiceOrchestrator(initializer_module)

    initializer_module.actionQueue = actionQueue

    orchestrator.file_cache = MagicMock()

    def f(command):
      return ""

    orchestrator.file_cache.get_service_base_dir = f
    actionQueue.customServiceOrchestrator = orchestrator

    import TestActionQueue
    import copy

    pyex = PythonExecutor(
      actionQueue.customServiceOrchestrator.tmp_dir,
      actionQueue.customServiceOrchestrator.config,
    )
    patch_executor_output(pyex)
    pyex.prepare_process_result = MagicMock()
    get_py_executor_mock.return_value = pyex
    orchestrator.dump_command_to_json = MagicMock()

    callback_started = threading.Event()
    allow_callback = threading.Event()

    def command_complete_w(process_condenced_result, handle):
      callback_started.set()
      if not allow_callback.wait(3):
        raise AssertionError("test did not release the background callback")

    actionQueue.on_background_command_complete_callback = wrapped(
      actionQueue.on_background_command_complete_callback, command_complete_w, None
    )
    execute_command = copy.deepcopy(TestActionQueue.TestActionQueue.background_command)
    get_configuration_mock.return_value = execute_command

    actionQueue.put([execute_command])
    actionQueue.process_background_queue_safe_empty()

    self.assertTrue(callback_started.wait(2))
    orchestrator.cancel_command(19, "reason")
    self.assertTrue(kill_process_with_children_mock.called)
    kill_process_with_children_mock.assert_called_with(33)

    allow_callback.set()
    execute_command["__handle"].thread.join(2)
    self.assertFalse(execute_command["__handle"].thread.is_alive())

    runningCommand = actionQueue.commandStatuses.get_command_status(19)
    self.assertTrue(runningCommand is not None)
    self.assertEqual(runningCommand["status"], CommandStatus.failed)

  @patch.object(ConfigurationBuilder, "get_configuration")
  @patch.object(HooksOrchestrator, "resolve_hooks", return_value=None)
  @patch.object(AmbariConfig, "get")
  @patch.object(CustomServiceOrchestrator, "dump_command_to_json")
  @patch.object(PythonExecutor, "run_file")
  @patch.object(FileCache, "__init__")
  @patch.object(FileCache, "get_custom_actions_base_dir")
  def test_runCommand_custom_action(
    self,
    get_custom_actions_base_dir_mock,
    FileCache_mock,
    run_file_mock,
    dump_command_to_json_mock,
    ambari_config_get,
    resolve_hooks_mock,
    get_configuration_mock,
  ):
    ambari_config_get.return_value = "0"
    FileCache_mock.return_value = None
    get_custom_actions_base_dir_mock.return_value = "some path"
    command = {
      "role": "any",
      "commandParams": {
        "script_type": "PYTHON",
        "script": "some_custom_action.py",
        "command_timeout": "600",
      },
      "ambariLevelParams": {"jdk_location": "some_location"},
      "taskId": "3",
      "roleCommand": "ACTIONEXECUTE",
      "clusterId": "-1",
    }
    get_configuration_mock.return_value = command

    initializer_module = InitializerModule()
    initializer_module.config = self.config
    initializer_module.init()

    orchestrator = CustomServiceOrchestrator(initializer_module)
    unix_process_id = 111
    orchestrator.commands_in_progress = {
      (command["taskId"], threading.get_ident()): unix_process_id
    }
    # normal run case
    run_file_mock.return_value = {
      "stdout": "sss",
      "stderr": "eee",
      "exitcode": 0,
    }
    ret = orchestrator.runCommand(command, "out.txt", "err.txt")
    self.assertEqual(ret["exitcode"], 0)
    self.assertTrue(run_file_mock.called)
    # Hoooks are not supported for custom actions,
    # that's why run_file() should be called only once
    self.assertEqual(run_file_mock.call_count, 1)

  @patch.object(CustomServiceOrchestrator, "runCommand")
  @patch.object(FileCache, "__init__")
  def test_requestComponentStatus(self, FileCache_mock, runCommand_mock):
    FileCache_mock.return_value = None
    status_command = {
      "serviceName": "HDFS",
      "commandType": "STATUS_COMMAND",
      "clusterName": "",
      "componentName": "DATANODE",
      "configurations": {},
    }
    initializer_module = InitializerModule()
    initializer_module.init()
    orchestrator = CustomServiceOrchestrator(initializer_module)
    # Test alive case
    runCommand_mock.return_value = {"exitcode": 0}

    status = orchestrator.requestComponentStatus(status_command)
    self.assertEqual(runCommand_mock.return_value, status)

    # Test dead case
    runCommand_mock.return_value = {"exitcode": 1}
    status = orchestrator.requestComponentStatus(status_command)
    self.assertEqual(runCommand_mock.return_value, status)

  @patch.object(ConfigurationBuilder, "get_configuration")
  @patch.object(HooksOrchestrator, "resolve_hooks", return_value=None)
  @patch.object(CustomServiceOrchestrator, "get_py_executor")
  @patch.object(CustomServiceOrchestrator, "dump_command_to_json")
  @patch.object(FileCache, "__init__")
  @patch.object(FileCache, "get_custom_actions_base_dir")
  def test_runCommand_background_action(
    self,
    get_custom_actions_base_dir_mock,
    FileCache_mock,
    dump_command_to_json_mock,
    get_py_executor_mock,
    resolve_hooks_mock,
    get_configuration_mock,
  ):
    FileCache_mock.return_value = None
    get_custom_actions_base_dir_mock.return_value = "some path"
    command = {
      "role": "any",
      "commandParams": {
        "script_type": "PYTHON",
        "script": "some_custom_action.py",
        "command_timeout": "600",
      },
      "ambariLevelParams": {"jdk_location": "some_location"},
      "clusterId": "-1",
      "taskId": "13",
      "roleCommand": "ACTIONEXECUTE",
      "commandType": "BACKGROUND_EXECUTION_COMMAND",
      "__handle": BackgroundCommandExecutionHandle(
        {"taskId": "13"}, 13, MagicMock(), MagicMock()
      ),
    }
    initializer_module = InitializerModule()
    initializer_module.init()
    orchestrator = CustomServiceOrchestrator(initializer_module)

    pyex = PythonExecutor(orchestrator.tmp_dir, orchestrator.config)
    patch_executor_output(pyex)
    pyex.condense_output = MagicMock()
    get_py_executor_mock.return_value = pyex
    orchestrator.dump_command_to_json = MagicMock()

    ret = orchestrator.runCommand(command, "out.txt", "err.txt")
    self.assertEqual(ret["exitcode"], 777)
    command["__handle"].thread.join(2)
    self.assertFalse(command["__handle"].thread.is_alive())

  @staticmethod
  def _credential_command():
    return {
      "roleCommand": "INSTALL",
      "taskId": 7,
      "role": "DATANODE",
      "serviceName": "HDFS",
      "ambariLevelParams": {
        "java_home": "/component-java",
        "ambari_java_home": "/ambari-java",
      },
      "serviceLevelParams": {
        "configuration_credentials": {"hdfs-site": {"password": "password"}}
      },
      "configurations": {
        "hdfs-site": {"password": "new-secret"},
        "cluster-env": {"user_group": "hadoop"},
      },
    }

  def test_generate_jceks_atomically_replaces_store_with_private_permissions(self):
    orchestrator = CustomServiceOrchestrator.__new__(CustomServiceOrchestrator)
    orchestrator.credential_shell_lib_path = "/credential/lib/*"
    orchestrator.encryption_key = None
    command = self._credential_command()

    with tempfile.TemporaryDirectory() as credential_directory:
      orchestrator.credential_conf_dir = credential_directory
      provider_directory = orchestrator.getProviderDirectory("DATANODE")
      os.makedirs(provider_directory)
      final_store = os.path.join(provider_directory, "hdfs-site.jceks")
      final_checksum = os.path.join(provider_directory, ".hdfs-site.jceks.crc")
      with open(final_store, "wb") as stream:
        stream.write(b"old-store")
      with open(final_checksum, "wb") as stream:
        stream.write(b"old-checksum")

      def create_store(
        java_bin, lib_path, alias, provider_path, password, overwrite=False
      ):
        self.assertEqual("/ambari-java/bin/java", java_bin)
        self.assertEqual("/credential/lib/*", lib_path)
        self.assertEqual("password", alias)
        self.assertEqual("new-secret", password)
        self.assertFalse(overwrite)
        temporary_store = provider_path.removeprefix("jceks://file")
        with open(temporary_store, "wb") as stream:
          stream.write(b"new-store")
        with open(
          os.path.join(os.path.dirname(temporary_store), ".hdfs-site.jceks.crc"),
          "wb",
        ) as stream:
          stream.write(b"new-checksum")
        return 0

      with patch(
        "ambari_agent.CustomServiceOrchestrator.create_credential_store_entry",
        side_effect=create_store,
      ), patch("ambari_agent.CustomServiceOrchestrator.shutil.chown") as chown_mock:
        self.assertEqual(0, orchestrator.generateJceks(command))

      with open(final_store, "rb") as stream:
        self.assertEqual(stream.read(), b"new-store")
      self.assertFalse(os.path.exists(final_checksum))
      self.assertEqual(oct(os.stat(final_store).st_mode & 0o777), "0o640")
      chown_mock.assert_has_calls(
        [
          call(provider_directory, group="hadoop"),
          call(ANY, group="hadoop"),
        ]
      )
      self.assertEqual(oct(os.stat(provider_directory).st_mode & 0o777), "0o750")
      self.assertEqual(
        command["configurations"]["hdfs-site"][
          CustomServiceOrchestrator.CREDENTIAL_PROVIDER_PROPERTY_NAME
        ],
        f"jceks://file{final_store}",
      )
      self.assertNotIn("password", command["configurations"]["hdfs-site"])

  def test_generate_jceks_failure_preserves_existing_store(self):
    orchestrator = CustomServiceOrchestrator.__new__(CustomServiceOrchestrator)
    orchestrator.credential_shell_lib_path = "/credential/lib/*"
    orchestrator.encryption_key = None
    command = self._credential_command()

    with tempfile.TemporaryDirectory() as credential_directory:
      orchestrator.credential_conf_dir = credential_directory
      provider_directory = orchestrator.getProviderDirectory("DATANODE")
      os.makedirs(provider_directory)
      final_store = os.path.join(provider_directory, "hdfs-site.jceks")
      with open(final_store, "wb") as stream:
        stream.write(b"old-store")

      with patch(
        "ambari_agent.CustomServiceOrchestrator.create_credential_store_entry",
        return_value=9,
      ), patch("ambari_agent.CustomServiceOrchestrator.shutil.chown"):
        with self.assertRaisesRegex(AgentException, "exit code 9"):
          orchestrator.generateJceks(command)

      with open(final_store, "rb") as stream:
        self.assertEqual(stream.read(), b"old-store")
      self.assertEqual(
        "new-secret", command["configurations"]["hdfs-site"]["password"]
      )
      self.assertFalse(
        any(name.startswith(".jceks-") for name in os.listdir(provider_directory))
      )

  def test_generate_jceks_replace_failure_preserves_readable_existing_store(self):
    orchestrator = CustomServiceOrchestrator.__new__(CustomServiceOrchestrator)
    orchestrator.credential_shell_lib_path = "/credential/lib/*"
    orchestrator.encryption_key = None
    command = self._credential_command()

    with tempfile.TemporaryDirectory() as credential_directory:
      orchestrator.credential_conf_dir = credential_directory
      provider_directory = orchestrator.getProviderDirectory("DATANODE")
      os.makedirs(provider_directory)
      final_store = os.path.join(provider_directory, "hdfs-site.jceks")
      final_checksum = os.path.join(provider_directory, ".hdfs-site.jceks.crc")
      with open(final_store, "wb") as stream:
        stream.write(b"old-store")
      with open(final_checksum, "wb") as stream:
        stream.write(b"old-checksum")

      def create_store(
        java_bin, lib_path, alias, provider_path, password, overwrite=False
      ):
        temporary_store = provider_path.removeprefix("jceks://file")
        with open(temporary_store, "wb") as stream:
          stream.write(b"new-store")
        return 0

      with patch(
        "ambari_agent.CustomServiceOrchestrator.create_credential_store_entry",
        side_effect=create_store,
      ), patch(
        "ambari_agent.CustomServiceOrchestrator.shutil.chown"
      ), patch(
        "ambari_agent.CustomServiceOrchestrator.os.replace",
        side_effect=OSError("replace failed"),
      ):
        with self.assertRaisesRegex(OSError, "replace failed"):
          orchestrator.generateJceks(command)

      with open(final_store, "rb") as stream:
        self.assertEqual(stream.read(), b"old-store")
      self.assertFalse(os.path.exists(final_checksum))
      self.assertEqual(
        "new-secret", command["configurations"]["hdfs-site"]["password"]
      )
      self.assertFalse(
        any(name.startswith(".jceks-") for name in os.listdir(provider_directory))
      )

  def test_generate_jceks_requires_service_group(self):
    orchestrator = CustomServiceOrchestrator.__new__(CustomServiceOrchestrator)
    orchestrator.credential_shell_lib_path = "/credential/lib/*"
    orchestrator.encryption_key = None
    command = self._credential_command()
    del command["configurations"]["cluster-env"]["user_group"]

    with tempfile.TemporaryDirectory() as credential_directory:
      orchestrator.credential_conf_dir = credential_directory
      with self.assertRaisesRegex(AgentException, "user_group is required"):
        orchestrator.generateJceks(command)

    self.assertEqual(
      "new-secret", command["configurations"]["hdfs-site"]["password"]
    )

  def test_command_encryption_key_scope_detects_only_encrypted_values(self):
    encrypted = "${enc=aes256_gcm_hex, value=00}"

    self.assertTrue(
      CustomServiceOrchestrator.command_requires_encryption_key(
        {"configurations": {"site": {"password": encrypted}}}
      )
    )
    self.assertTrue(
      CustomServiceOrchestrator.command_requires_encryption_key(["plain", encrypted])
    )
    self.assertFalse(
      CustomServiceOrchestrator.command_requires_encryption_key(
        {"configurations": {"site": {"password": "plain"}}}
      )
    )
