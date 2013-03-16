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

import pprint

from unittest import TestCase
import threading
import tempfile
import time
from threading import Thread

from PythonExecutor import PythonExecutor
from AmbariConfig import AmbariConfig
from mock.mock import MagicMock


class TestPythonExecutor(TestCase):


  def test_watchdog_1(self):
    """
    Tests whether watchdog works
    """
    subproc_mock = self.Subprocess_mockup()
    executor = PythonExecutor("/tmp", AmbariConfig().getConfig())
    _, tmpoutfile = tempfile.mkstemp()
    _, tmperrfile = tempfile.mkstemp()
    executor.PYTHON_TIMEOUT_SECONDS = 0.1

    def lauch_python_subprocess_method(command, tmpout, tmperr):
      subproc_mock.tmpout = tmpout
      subproc_mock.tmperr = tmperr
      return subproc_mock
    executor.lauch_python_subprocess = lauch_python_subprocess_method
    runShellKillPgrp_method = MagicMock()
    runShellKillPgrp_method.side_effect = lambda python : python.terminate()
    executor.runShellKillPgrp = runShellKillPgrp_method
    subproc_mock.returncode = None
    thread = Thread(target =  executor.run_file, args = ("fake_command",
                                    "fake_puppetFile", tmpoutfile, tmperrfile))
    thread.start()
    time.sleep(0.1)
    subproc_mock.finished_event.wait()
    self.assertEquals(subproc_mock.was_terminated, True, "Subprocess should be terminated due to timeout")


  def test_watchdog_2(self):
    """
    Tries to catch false positive watchdog invocations
    """
    subproc_mock = self.Subprocess_mockup()
    executor = PythonExecutor("/tmp", AmbariConfig().getConfig())
    _, tmpoutfile = tempfile.mkstemp()
    _, tmperrfile = tempfile.mkstemp()
    executor.PYTHON_TIMEOUT_SECONDS =  5

    def lauch_python_subprocess_method(command, tmpout, tmperr):
      subproc_mock.tmpout = tmpout
      subproc_mock.tmperr = tmperr
      return subproc_mock
    executor.lauch_python_subprocess = lauch_python_subprocess_method
    runShellKillPgrp_method = MagicMock()
    runShellKillPgrp_method.side_effect = lambda python : python.terminate()
    executor.runShellKillPgrp = runShellKillPgrp_method
    subproc_mock.returncode = 0
    thread = Thread(target =  executor.run_file, args = ("fake_command",
                                                         "fake_puppetFile", tmpoutfile, tmperrfile))
    thread.start()
    time.sleep(0.1)
    subproc_mock.should_finish_event.set()
    subproc_mock.finished_event.wait()
    self.assertEquals(subproc_mock.was_terminated, False, "Subprocess should not be terminated before timeout")
    self.assertEquals(subproc_mock.returncode, 0, "Subprocess should not be terminated before timeout")


  def test_execution_results(self):
    subproc_mock = self.Subprocess_mockup()
    executor = PythonExecutor("/tmp", AmbariConfig().getConfig())
    _, tmpoutfile = tempfile.mkstemp()
    _, tmperrfile = tempfile.mkstemp()
    executor.PYTHON_TIMEOUT_SECONDS =  5

    def lauch_python_subprocess_method(command, tmpout, tmperr):
      subproc_mock.tmpout = tmpout
      subproc_mock.tmperr = tmperr
      return subproc_mock
    executor.lauch_python_subprocess = lauch_python_subprocess_method
    runShellKillPgrp_method = MagicMock()
    runShellKillPgrp_method.side_effect = lambda python : python.terminate()
    executor.runShellKillPgrp = runShellKillPgrp_method
    subproc_mock.returncode = 0
    subproc_mock.should_finish_event.set()
    result = executor.run_file("command", "file", tmpoutfile, tmperrfile)
    self.assertEquals(result, {'exitcode': 0, 'stderr': 'Dummy err', 'stdout': 'Dummy output'})


  class Subprocess_mockup():
    """
    It's not trivial to use PyMock instead of class here because we need state
    and complex logics
    """

    returncode = 0

    started_event = threading.Event()
    should_finish_event = threading.Event()
    finished_event = threading.Event()
    was_terminated = False
    tmpout = None
    tmperr = None
    pid=-1

    def communicate(self):
      self.started_event.set()
      self.tmpout.write("Dummy output")
      self.tmpout.flush()

      self.tmperr.write("Dummy err")
      self.tmperr.flush()
      self.should_finish_event.wait()
      self.finished_event.set()
      pass

    def terminate(self):
      self.was_terminated = True
      self.returncode = 17
      self.should_finish_event.set()

