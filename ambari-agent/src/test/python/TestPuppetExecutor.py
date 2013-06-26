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
from PuppetExecutor import PuppetExecutor
from RepoInstaller import RepoInstaller
from Grep import Grep
from pprint import pformat
import socket, threading, tempfile
import os, time
import sys
import json
from AmbariConfig import AmbariConfig
from mock.mock import patch, MagicMock, call
from threading import Thread

class TestPuppetExecutor(TestCase):


  def test_build(self):
    puppetexecutor = PuppetExecutor("/tmp", "/x", "/y", "/z", AmbariConfig().getConfig())
    command = puppetexecutor.puppetCommand("site.pp")
    self.assertEquals("puppet", command[0], "puppet binary wrong")
    self.assertEquals("apply", command[1], "local apply called")
    self.assertEquals("--confdir=/tmp", command[2],"conf dir tmp")
    self.assertEquals("--detailed-exitcodes", command[3], "make sure output \
    correct")

  @patch.object(PuppetExecutor, 'runPuppetFile')
  def test_run_command(self, runPuppetFileMock):
    tmpdir = AmbariConfig().getConfig().get("stack", "installprefix")
    puppetInstance = PuppetExecutor("/tmp", "/x", "/y", tmpdir, AmbariConfig().getConfig())
    jsonFile = open('../../main/python/ambari_agent/test.json', 'r')
    jsonStr = jsonFile.read()
    parsedJson = json.loads(jsonStr)
    parsedJson["taskId"] = 1
    def side_effect1(puppetFile, result, puppetEnv, tmpoutfile, tmperrfile):
        result["exitcode"] = 0
    runPuppetFileMock.side_effect = side_effect1
    puppetInstance.reposInstalled = False
    res = puppetInstance.runCommand(parsedJson, tmpdir + '/out.txt', tmpdir + '/err.txt')
    self.assertEquals(res["exitcode"], 0)
    self.assertTrue(puppetInstance.reposInstalled)

    def side_effect2(puppetFile, result, puppetEnv, tmpoutfile, tmperrfile):
        result["exitcode"] = 999
    runPuppetFileMock.side_effect = side_effect2
    puppetInstance.reposInstalled = False
    res = puppetInstance.runCommand(parsedJson, tmpdir + '/out.txt', tmpdir + '/err.txt')
    self.assertEquals(res["exitcode"], 999)
    self.assertFalse(puppetInstance.reposInstalled)
    os.unlink(tmpdir + os.sep + 'site-' + str(parsedJson["taskId"]) + '.pp')

  @patch.object(RepoInstaller, 'generate_repo_manifests')
  @patch.object(PuppetExecutor, 'runPuppetFile')
  def test_overwrite_repos(self, runPuppetFileMock, generateRepoManifestMock):
    tmpdir = AmbariConfig().getConfig().get("stack", "installprefix")
    puppetInstance = PuppetExecutor("/tmp", "/x", "/y", tmpdir, AmbariConfig().getConfig())
    jsonFile = open('../../main/python/ambari_agent/test.json', 'r')
    jsonStr = jsonFile.read()
    parsedJson = json.loads(jsonStr)
    parsedJson["taskId"] = 77
    def side_effect(puppetFile, result, puppetEnv, tmpoutfile, tmperrfile):
      result["exitcode"] = 0
    runPuppetFileMock.side_effect = side_effect

    #If ambari-agent has been just started and no any commands were executed by
    # PuppetExecutor.runCommand, then no repo files were updated by
    # RepoInstaller.generate_repo_manifests
    self.assertEquals(0, generateRepoManifestMock.call_count)
    self.assertFalse(puppetInstance.reposInstalled)

    # After executing of the first command, RepoInstaller.generate_repo_manifests
    # generates a .pp file for updating repo files
    puppetInstance.runCommand(parsedJson, tmpdir + '/out.txt', tmpdir + '/err.txt')
    self.assertTrue(puppetInstance.reposInstalled)
    self.assertEquals(1, generateRepoManifestMock.call_count)

    # After executing of the next commands, repo manifest aren't generated again
    puppetInstance.runCommand(parsedJson, tmpdir + '/out.txt', tmpdir + '/err.txt')
    self.assertTrue(puppetInstance.reposInstalled)
    self.assertEquals(1, generateRepoManifestMock.call_count)
    puppetInstance.runCommand(parsedJson, tmpdir + '/out.txt', tmpdir + '/err.txt')
    self.assertTrue(puppetInstance.reposInstalled)
    self.assertEquals(1, generateRepoManifestMock.call_count)

  @patch("os.path.exists")
  def test_configure_environ(self, osPathExistsMock):
    config = AmbariConfig().getConfig()
    tmpdir = config.get("stack", "installprefix")
    puppetInstance = PuppetExecutor("/tmp", "/x", "/y", tmpdir, config)
    environ = puppetInstance.configureEnviron({})
    self.assertEquals(environ, {})

    config.set('puppet','ruby_home',"test/ruby_home")
    puppetInstance = PuppetExecutor("/tmp", "/x", "/y", tmpdir, config)
    osPathExistsMock.return_value = True
    environ = puppetInstance.configureEnviron({"PATH" : "test_path"})
    self.assertEquals(environ["PATH"], "test/ruby_home/bin:test_path")
    self.assertEquals(environ["MY_RUBY_HOME"], "test/ruby_home")

  def test_condense_bad2(self):
    puppetexecutor = PuppetExecutor("/tmp", "/x", "/y", "/z", AmbariConfig().getConfig())
    grep = Grep()
    puppetexecutor.grep = grep
    grep.ERROR_LAST_LINES_BEFORE = 2
    grep.ERROR_LAST_LINES_AFTER = 3
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
    puppetexecutor = PuppetExecutor("/tmp", "/x", "/y", "/z", AmbariConfig().getConfig())
    grep = Grep()
    puppetexecutor.grep = grep
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
    puppetexecutor = PuppetExecutor("/tmp", "/x", "/y", "/z", AmbariConfig().getConfig())
    grep = Grep()
    puppetexecutor.grep = grep
    grep.OUTPUT_LAST_LINES = 2
    string_good = open('dummy_puppet_output_good.txt', 'r').read().replace("\n", os.linesep)
    result = puppetexecutor.condenseOutput(string_good, PuppetExecutor.NO_ERROR, 0)
    stripped_string = string_good.strip()
    lines = stripped_string.splitlines(True)
    result_check = lines[45].strip() in result and lines[46].strip() in result
    self.assertEquals(result_check, True, "Failed to condence output log")
    self.assertEquals(len(result.splitlines(True)), 2, "Failed to condence output log")

  @patch("shell.kill_process_with_children")
  def test_watchdog_1(self, kill_process_with_children_mock):
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
    kill_process_with_children_mock.side_effect = lambda pid : subproc_mock.terminate()
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
    self.assertEquals(subproc_mock.was_terminated, False, "Subprocess should not be terminated before timeout")
    self.assertEquals(subproc_mock.returncode, 0, "Subprocess should not be terminated before timeout")


  class  PuppetExecutor_mock(PuppetExecutor):



    def __init__(self, puppetModule, puppetInstall, facterInstall, tmpDir, config, subprocess_mockup):
      self.subprocess_mockup = subprocess_mockup
      PuppetExecutor.__init__(self, puppetModule, puppetInstall, facterInstall, tmpDir, config)
      pass

    def lauch_puppet_subprocess(self, puppetcommand, tmpout, tmperr, puppetEnv):
      self.subprocess_mockup.tmpout = tmpout
      self.subprocess_mockup.tmperr = tmperr
      return self.subprocess_mockup

    def runShellKillPgrp(self, puppet):
      puppet.terminate()  # note: In real code, subprocess.terminate() is not called
      pass

  class Subprocess_mockup():

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

