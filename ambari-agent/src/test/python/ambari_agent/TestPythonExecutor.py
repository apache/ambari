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

import pprint

from unittest import TestCase
import threading
import tempfile
from threading import Thread
import os
import signal

from ambari_agent.BackgroundCommandExecutionHandle import (
  BackgroundCommandExecutionHandle,
)
from ambari_agent.PythonExecutor import PythonExecutor
from ambari_agent.AmbariConfig import AmbariConfig
from unittest.mock import MagicMock, patch
from ambari_commons import OSCheck
from only_for_platform import os_distro_value


@patch.object(
  PythonExecutor, "open_subprocess_files", new=MagicMock(return_value=("", ""))
)
class TestPythonExecutor(TestCase):
  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  @patch("ambari_commons.shell.kill_process_with_children")
  def test_watchdog_1(self, kill_process_with_children_mock):
    """
    Tests whether watchdog works
    """
    subproc_mock = self.subprocess_mockup()
    executor = PythonExecutor("/tmp", AmbariConfig())
    _, tmpoutfile = tempfile.mkstemp()
    _, tmperrfile = tempfile.mkstemp()
    _, tmpstrucout = tempfile.mkstemp()
    PYTHON_TIMEOUT_SECONDS = 0.1
    kill_process_with_children_mock.side_effect = lambda pid: subproc_mock.terminate()

    def launch_python_subprocess_method(command, tmpout, tmperr, env=None):
      subproc_mock.tmpout = tmpout
      subproc_mock.tmperr = tmperr
      return subproc_mock

    executor.launch_python_subprocess = launch_python_subprocess_method
    runShellKillPgrp_method = MagicMock()
    runShellKillPgrp_method.side_effect = lambda python: python.terminate()
    executor.runShellKillPgrp = runShellKillPgrp_method
    subproc_mock.returncode = None
    callback_method = MagicMock()
    thread = Thread(
      target=executor.run_file,
      args=(
        "fake_puppetFile",
        ["arg1", "arg2"],
        tmpoutfile,
        tmperrfile,
        PYTHON_TIMEOUT_SECONDS,
        tmpstrucout,
        callback_method,
        "1",
      ),
    )
    thread.start()
    self.assertTrue(subproc_mock.started_event.wait(2))
    self.assertTrue(subproc_mock.finished_event.wait(2))
    thread.join(2)
    self.assertFalse(thread.is_alive())
    self.assertEqual(
      subproc_mock.was_terminated,
      True,
      "subprocess should be terminated due to timeout",
    )
    self.assertTrue(callback_method.called)

  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  def test_watchdog_2(self):
    """
    Tries to catch false positive watchdog invocations
    """
    subproc_mock = self.subprocess_mockup()
    executor = PythonExecutor("/tmp", AmbariConfig())
    _, tmpoutfile = tempfile.mkstemp()
    _, tmperrfile = tempfile.mkstemp()
    _, tmpstrucout = tempfile.mkstemp()
    PYTHON_TIMEOUT_SECONDS = 5

    def launch_python_subprocess_method(command, tmpout, tmperr, env=None):
      subproc_mock.tmpout = tmpout
      subproc_mock.tmperr = tmperr
      return subproc_mock

    executor.launch_python_subprocess = launch_python_subprocess_method
    runShellKillPgrp_method = MagicMock()
    runShellKillPgrp_method.side_effect = lambda python: python.terminate()
    executor.runShellKillPgrp = runShellKillPgrp_method
    subproc_mock.returncode = 0
    callback_method = MagicMock()
    thread = Thread(
      target=executor.run_file,
      args=(
        "fake_puppetFile",
        ["arg1", "arg2"],
        tmpoutfile,
        tmperrfile,
        PYTHON_TIMEOUT_SECONDS,
        tmpstrucout,
        callback_method,
        "1-1",
      ),
    )
    thread.start()
    self.assertTrue(subproc_mock.started_event.wait(2))
    subproc_mock.should_finish_event.set()
    self.assertTrue(subproc_mock.finished_event.wait(2))
    thread.join(2)
    self.assertFalse(thread.is_alive())
    self.assertEqual(
      subproc_mock.was_terminated,
      False,
      "subprocess should not be terminated before timeout",
    )
    self.assertEqual(
      subproc_mock.returncode, 0, "subprocess should not be terminated before timeout"
    )
    self.assertTrue(callback_method.called)

  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  def test_execution_results(self):
    subproc_mock = self.subprocess_mockup()
    executor = PythonExecutor("/tmp", AmbariConfig())
    _, tmpoutfile = tempfile.mkstemp()
    _, tmperrfile = tempfile.mkstemp()

    tmp_file = tempfile.NamedTemporaryFile()  # the structured out file should be preserved across calls to the hooks and script.
    tmpstructuredoutfile = tmp_file.name
    tmp_file.close()

    PYTHON_TIMEOUT_SECONDS = 5

    def launch_python_subprocess_method(command, tmpout, tmperr, env=None):
      subproc_mock.tmpout = tmpout
      subproc_mock.tmperr = tmperr
      return subproc_mock

    executor.launch_python_subprocess = launch_python_subprocess_method
    runShellKillPgrp_method = MagicMock()
    runShellKillPgrp_method.side_effect = lambda python: python.terminate()
    executor.runShellKillPgrp = runShellKillPgrp_method
    subproc_mock.returncode = 0
    subproc_mock.should_finish_event.set()
    callback_method = MagicMock()
    result = executor.run_file(
      "file",
      ["arg1", "arg2"],
      tmpoutfile,
      tmperrfile,
      PYTHON_TIMEOUT_SECONDS,
      tmpstructuredoutfile,
      callback_method,
      "1-1",
    )
    self.assertEqual(
      result, {"exitcode": 0, "stderr": "", "stdout": "", "structuredOut": {}}
    )
    self.assertTrue(callback_method.called)

  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  def test_is_successfull(self):
    executor = PythonExecutor("/tmp", AmbariConfig())

    executor.python_process_has_been_killed = False
    self.assertTrue(executor.is_successful(0))
    self.assertFalse(executor.is_successful(1))

    executor.python_process_has_been_killed = True
    self.assertFalse(executor.is_successful(0))
    self.assertFalse(executor.is_successful(1))

  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  def test_python_command(self):
    executor = PythonExecutor("/tmp", AmbariConfig())
    command = executor.python_command("script", ["script_param1"])
    self.assertEqual(3, len(command))
    self.assertTrue("python" in command[0].lower())
    self.assertEqual("script", command[1])
    self.assertEqual("script_param1", command[2])

  @patch("ambari_agent.PythonExecutor.subprocess.Popen")
  def test_subprocess_environment_excludes_enrollment_passphrase(
    self, popen_mock
  ):
    executor = PythonExecutor("/tmp", AmbariConfig())

    with patch.dict(
      os.environ,
      {"AMBARI_PASSPHRASE": "enrollment-secret", "PRESERVED": "value"},
      clear=True,
    ):
      executor.launch_python_subprocess(
        ["python3", "script.py"],
        MagicMock(),
        MagicMock(),
        env={
          "AGENT_ENCRYPTION_KEY": "command-key",
          "AMBARI_PASSPHRASE": "injected-secret",
        },
      )

    command_environment = popen_mock.call_args.kwargs["env"]
    self.assertNotIn("AMBARI_PASSPHRASE", command_environment)
    self.assertEqual(command_environment["AGENT_ENCRYPTION_KEY"], "command-key")
    self.assertEqual(command_environment["PRESERVED"], "value")
    self.assertTrue(popen_mock.call_args.kwargs["start_new_session"])
    self.assertNotIn("preexec_fn", popen_mock.call_args.kwargs)

  @patch("ambari_agent.PythonExecutor.subprocess.Popen")
  def test_plain_command_does_not_inherit_process_encryption_key(self, popen_mock):
    executor = PythonExecutor("/tmp", AmbariConfig())

    with patch.dict(
      os.environ,
      {"AGENT_ENCRYPTION_KEY": "stale-process-key", "PRESERVED": "value"},
      clear=True,
    ):
      executor.launch_python_subprocess(
        ["python3", "script.py"], MagicMock(), MagicMock()
      )

    command_environment = popen_mock.call_args.kwargs["env"]
    self.assertNotIn("AGENT_ENCRYPTION_KEY", command_environment)
    self.assertEqual("value", command_environment["PRESERVED"])

  @patch("ambari_agent.PythonExecutor.subprocess.Popen")
  def test_subprocess_environment_honors_disabled_proxy_setting(self, popen_mock):
    config = AmbariConfig()
    config.set("network", "use_system_proxy_settings", "false")
    executor = PythonExecutor("/tmp", config)

    with patch.dict(
      os.environ,
      {"HTTPS_PROXY": "http://system-proxy", "PRESERVED": "value"},
      clear=True,
    ):
      executor.launch_python_subprocess(
        ["python3", "script.py"],
        MagicMock(),
        MagicMock(),
        env={"http_proxy": "http://command-proxy"},
      )

    command_environment = popen_mock.call_args.kwargs["env"]
    self.assertNotIn("HTTPS_PROXY", command_environment)
    self.assertNotIn("http_proxy", command_environment)
    self.assertEqual("value", command_environment["PRESERVED"])

  @patch("ambari_agent.PythonExecutor.threading.Thread")
  def test_background_thread_start_failure_clears_handle_thread(self, thread_mock):
    executor = PythonExecutor("/tmp", AmbariConfig())
    thread_mock.return_value.start.side_effect = RuntimeError("thread start failed")
    handle = BackgroundCommandExecutionHandle(
      {"taskId": 1}, 1, MagicMock(), MagicMock()
    )

    with self.assertRaisesRegex(RuntimeError, "thread start failed"):
      executor.run_file(
        "script.py",
        [],
        "/tmp/stdout",
        "/tmp/stderr",
        60,
        "/tmp/structured-out.json",
        MagicMock(),
        1,
        handle=handle,
      )

    self.assertIsNone(handle.thread)

  def test_background_setup_failure_invokes_completion_callback(self):
    executor = PythonExecutor("/tmp", AmbariConfig())
    executor.open_subprocess_files = MagicMock(
      side_effect=OSError("cannot open output files")
    )
    completed = threading.Event()
    callback = MagicMock(side_effect=lambda *_args: completed.set())
    handle = BackgroundCommandExecutionHandle({"taskId": 1}, 1, MagicMock(), callback)

    result = executor.run_file(
      "script.py",
      [],
      "/tmp/stdout",
      "/tmp/stderr",
      60,
      "/tmp/structured-out.json",
      MagicMock(),
      1,
      handle=handle,
    )

    self.assertEqual({"exitcode": 777}, result)
    self.assertTrue(completed.wait(2))
    handle.thread.join(2)
    self.assertFalse(handle.thread.is_alive())
    process_result = callback.call_args.args[0]
    self.assertEqual(1, process_result["exitcode"])
    self.assertIn("cannot open output files", process_result["stderr"])

  @patch("ambari_commons.shell.kill_process_with_children")
  def test_background_command_timeout_kills_process_and_completes_callback(
    self, kill_process_mock
  ):
    executor = PythonExecutor("/tmp", AmbariConfig())
    process = MagicMock(pid=1234, returncode=None)
    process_finished = threading.Event()

    def communicate():
      if not process_finished.wait(2):
        raise AssertionError("background watchdog did not terminate the process")

    def terminate(_pid):
      process.returncode = -signal.SIGTERM
      process_finished.set()

    process.communicate.side_effect = communicate
    kill_process_mock.side_effect = terminate
    executor.launch_python_subprocess = MagicMock(return_value=process)
    completed = threading.Event()
    callback = MagicMock(side_effect=lambda *_args: completed.set())
    handle = BackgroundCommandExecutionHandle({"taskId": 1}, 1, MagicMock(), callback)

    with tempfile.NamedTemporaryFile() as tmpout:
      with tempfile.NamedTemporaryFile() as tmperr:
        with tempfile.NamedTemporaryFile() as structured_out:
          result = executor.run_file(
            "script.py",
            [],
            tmpout.name,
            tmperr.name,
            0.01,
            structured_out.name,
            MagicMock(),
            1,
            handle=handle,
          )

          self.assertEqual({"exitcode": 777}, result)
          self.assertTrue(completed.wait(2))
          handle.thread.join(2)

    self.assertFalse(handle.thread.is_alive())
    kill_process_mock.assert_called_once_with(1234)
    process_result = callback.call_args.args[0]
    self.assertEqual(999, process_result["exitcode"])
    self.assertIn("killed due to timeout", process_result["stderr"])

  @patch("ambari_commons.shell.kill_process_with_children")
  def test_cancellation_during_process_launch_kills_registered_process(
    self, kill_process_mock
  ):
    executor = PythonExecutor("/tmp", AmbariConfig())
    process = MagicMock(pid=1234, returncode=-signal.SIGTERM)
    executor.launch_python_subprocess = MagicMock(return_value=process)
    executor.prepare_process_result = MagicMock(
      return_value={
        "exitcode": -signal.SIGTERM,
        "stdout": "",
        "stderr": "",
        "structuredOut": {},
      }
    )
    cancel_event = threading.Event()

    def cancel_as_pid_is_registered(_task_id, _pid):
      cancel_event.set()

    result = executor.run_file(
      "script.py",
      [],
      "/tmp/stdout",
      "/tmp/stderr",
      60,
      "/tmp/structured-out.json",
      cancel_as_pid_is_registered,
      1,
      cancel_event=cancel_event,
    )

    kill_process_mock.assert_called_once_with(1234)
    self.assertEqual(-signal.SIGTERM, result["exitcode"])

  @patch.object(os.path, "isfile")
  @patch.object(os, "rename")
  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  def test_back_up_log_file_if_exists(self, rename_mock, isfile_mock):
    # Test case when previous log file is absent
    isfile_mock.return_value = False
    log_file = "/var/lib/ambari-agent/data/output-13.txt"
    executor = PythonExecutor("/tmp", AmbariConfig())
    executor.back_up_log_file_if_exists(log_file)
    self.assertEqual(isfile_mock.called, True)
    self.assertEqual(rename_mock.called, False)

    isfile_mock.reset_mock()

    # Test case when 3 previous log files are absent
    isfile_mock.side_effect = [True, True, True, False]
    log_file = "/var/lib/ambari-agent/data/output-13.txt"
    executor = PythonExecutor("/tmp", AmbariConfig())
    executor.back_up_log_file_if_exists(log_file)
    self.assertEqual(isfile_mock.called, True)
    self.assertEqual(
      rename_mock.call_args_list[0][0][0], "/var/lib/ambari-agent/data/output-13.txt"
    )
    self.assertEqual(
      rename_mock.call_args_list[0][0][1], "/var/lib/ambari-agent/data/output-13.txt.2"
    )
    pass

  class subprocess_mockup:
    """
    It's not trivial to use PyMock instead of class here because we need state
    and complex logics
    """

    returncode = 0

    def __init__(self):
      self.started_event = threading.Event()
      self.should_finish_event = threading.Event()
      self.finished_event = threading.Event()
      self.was_terminated = False
      self.tmpout = None
      self.tmperr = None
      self.pid = -1

    def communicate(self):
      self.started_event.set()

      if not self.should_finish_event.wait(5):
        raise RuntimeError("Timed out waiting to finish mocked subprocess")
      self.finished_event.set()
      pass

    def terminate(self):
      self.was_terminated = True
      self.returncode = 17
      self.should_finish_event.set()
