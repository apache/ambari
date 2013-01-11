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
from puppetExecutor import puppetExecutor
from Grep import Grep
from pprint import pformat
import socket, threading, tempfile
import os, time
import sys
from AmbariConfig import AmbariConfig
from threading import Thread

grep = Grep()

class TestPuppetExecutor(TestCase):


  def test_build(self):
    puppetexecutor = puppetExecutor("/tmp", "/x", "/y", "/z", AmbariConfig().getConfig())
    command = puppetexecutor.puppetCommand("site.pp")
    self.assertEquals("puppet", command[0], "puppet binary wrong")
    self.assertEquals("apply", command[1], "local apply called")
    self.assertEquals("--confdir=/tmp", command[2],"conf dir tmp")
    self.assertEquals("--detailed-exitcodes", command[3], "make sure output \
    correct")

  def test_condense_bad2(self):
    puppetexecutor = puppetExecutor("/tmp", "/x", "/y", "/z", AmbariConfig().getConfig())
    puppetexecutor.ERROR_LAST_LINES_BEFORE = 2
    puppetexecutor.ERROR_LAST_LINES_AFTER = 3
    string_err = open('dummy_puppet_output_error2.txt', 'r').read().replace("\n", os.linesep)
    result = puppetexecutor.condenseOutput(string_err, '', 1)
    stripped_string = string_err.strip()
    lines = stripped_string.splitlines(True)
    d = lines[1:6]
    result_check = True
    for l in d:
      result_check &= grep.filterMarkup(l) in result
    self.assertEquals(result_check, True, "Failed to condence fail log")
    self.assertEquals(len(result.splitlines(True)), 6, "Failed to condence fail log")

  def test_condense_bad3(self):
    puppetexecutor = puppetExecutor("/tmp", "/x", "/y", "/z", AmbariConfig().getConfig())
    string_err = open('dummy_puppet_output_error3.txt', 'r').read().replace("\n", os.linesep)
    result = puppetexecutor.condenseOutput(string_err, '', 1)
    stripped_string = string_err.strip()
    lines = stripped_string.splitlines(True)
    #sys.stderr.write(result)
    d = lines[0:31]
    result_check = True
    for l in d:
      result_check &= grep.filterMarkup(l) in result
    self.assertEquals(result_check, True, "Failed to condence fail log")
    self.assertEquals(len(result.splitlines(True)), 33, "Failed to condence fail log")

  def test_condense_good(self):
    puppetexecutor = puppetExecutor("/tmp", "/x", "/y", "/z", AmbariConfig().getConfig())
    puppetexecutor.OUTPUT_LAST_LINES = 2
    string_good = open('dummy_puppet_output_good.txt', 'r').read().replace("\n", os.linesep)
    result = puppetexecutor.condenseOutput(string_good, puppetExecutor.NO_ERROR, 0)
    stripped_string = string_good.strip()
    lines = stripped_string.splitlines(True)
    result_check = lines[45].strip() in result and lines[46].strip() in result
    self.assertEquals(result_check, True, "Failed to condence output log")
    self.assertEquals(len(result.splitlines(True)), 2, "Failed to condence output log")

  def test_watchdog_1(self):
    """
    Tests whether watchdog works
    """
    subproc_mock = self.Subprocess_mockup()
    executor_mock = self.PuppetExecutor_mock("/home/centos/ambari_repo_info/ambari-agent/src/main/puppet/",
      "/usr/",
      "/root/workspace/puppet-install/facter-1.6.10/",
      "/tmp", AmbariConfig().getConfig(), subproc_mock)
    _, tmpoutfile = tempfile.mkstemp()
    _, tmperrfile = tempfile.mkstemp()
    result = {  }
    puppetEnv = { "RUBYLIB" : ""}
    executor_mock.PUPPET_TIMEOUT_SECONDS = 0.1
    subproc_mock.returncode = None
    thread = Thread(target =  executor_mock.runPuppetFile, args = ("fake_puppetFile", result, puppetEnv, tmpoutfile, tmperrfile))
    thread.start()
    time.sleep(0.1)
    subproc_mock.finished_event.wait()
    self.assertEquals(subproc_mock.was_terminated, True, "Subprocess should be terminated due to timeout")


  def test_watchdog_2(self):
    """
    Tries to catch false positive watchdog invocations
    """
    subproc_mock = self.Subprocess_mockup()
    executor_mock = self.PuppetExecutor_mock("/home/centos/ambari_repo_info/ambari-agent/src/main/puppet/",
    "/usr/",
    "/root/workspace/puppet-install/facter-1.6.10/",
    "/tmp", AmbariConfig().getConfig(), subproc_mock)
    _, tmpoutfile = tempfile.mkstemp()
    _, tmperrfile = tempfile.mkstemp()
    result = {  }
    puppetEnv = { "RUBYLIB" : ""}
    executor_mock.PUPPET_TIMEOUT_SECONDS = 5
    subproc_mock.returncode = 0
    thread = Thread(target =  executor_mock.runPuppetFile, args = ("fake_puppetFile", result, puppetEnv, tmpoutfile, tmperrfile))
    thread.start()
    time.sleep(0.1)
    subproc_mock.should_finish_event.set()
    subproc_mock.finished_event.wait()
    print(subproc_mock.was_terminated)
    self.assertEquals(subproc_mock.was_terminated, False, "Subprocess should not be terminated before timeout")
    self.assertEquals(subproc_mock.returncode, 0, "Subprocess should not be terminated before timeout")


  class  PuppetExecutor_mock(puppetExecutor):



    def __init__(self, puppetModule, puppetInstall, facterInstall, tmpDir, config, subprocess_mockup):
      self.subprocess_mockup = subprocess_mockup
      puppetExecutor.__init__(self, puppetModule, puppetInstall, facterInstall, tmpDir, config)
      pass

    def lauch_puppet_subprocess(self, puppetcommand, tmpout, tmperr, puppetEnv):
      self.subprocess_mockup.tmpout = tmpout
      self.subprocess_mockup.tmperr = tmperr
      return self.subprocess_mockup


  class Subprocess_mockup():

    returncode = 0

    started_event = threading.Event()
    should_finish_event = threading.Event()
    finished_event = threading.Event()
    was_terminated = False
    tmpout = None
    tmperr = None

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

