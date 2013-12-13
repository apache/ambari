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
import ConfigParser
import os

import pprint

from unittest import TestCase
import threading
import tempfile
import time
from threading import Thread

from PythonExecutor import PythonExecutor
from CustomServiceOrchestrator import CustomServiceOrchestrator
from AmbariConfig import AmbariConfig
from mock.mock import MagicMock, patch
import StringIO
import sys
from AgentException import AgentException
from FileCache import FileCache


class TestCustomServiceOrchestrator(TestCase):

  def setUp(self):
    # disable stdout
    out = StringIO.StringIO()
    sys.stdout = out
    # generate sample config
    tmpdir = tempfile.gettempdir()
    self.config = ConfigParser.RawConfigParser()
    self.config.add_section('agent')
    self.config.set('agent', 'prefix', tmpdir)
    self.config.set('agent', 'cache_dir', "/cachedir")
    self.config.add_section('python')
    self.config.set('python', 'custom_actions_dir', tmpdir)


  @patch("hostname.public_hostname")
  def test_dump_command_to_json(self, hostname_mock):
    hostname_mock.return_value = "test.hst"
    command = {
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
    config = AmbariConfig().getConfig()
    tempdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tempdir)
    orchestrator = CustomServiceOrchestrator(config)
    file = orchestrator.dump_command_to_json(command)
    self.assertTrue(os.path.exists(file))
    self.assertTrue(os.path.getsize(file) > 0)
    self.assertEqual(oct(os.stat(file).st_mode & 0777), '0600')
    os.unlink(file)
    # Testing side effect of dump_command_to_json
    self.assertEquals(command['public_hostname'], "test.hst")


  @patch("os.path.exists")
  def test_resolve_script_path(self, exists_mock):
    config = AmbariConfig().getConfig()
    orchestrator = CustomServiceOrchestrator(config)
    # Testing existing path
    exists_mock.return_value = True
    path = orchestrator.\
      resolve_script_path("/HBASE/package", "scripts/hbase_master.py", "PYTHON")
    self.assertEqual("/HBASE/package/scripts/hbase_master.py", path)
    # Testing not existing path
    exists_mock.return_value = False
    try:
      orchestrator.resolve_script_path("/HBASE",
                                       "scripts/hbase_master.py", "PYTHON")
      self.fail('ExpectedException not thrown')
    except AgentException:
      pass # Expected


  @patch.object(CustomServiceOrchestrator, "resolve_script_path")
  @patch.object(CustomServiceOrchestrator, "resolve_hook_script_path")
  @patch.object(FileCache, "get_service_base_dir")
  @patch.object(FileCache, "get_hook_base_dir")
  @patch.object(CustomServiceOrchestrator, "dump_command_to_json")
  @patch.object(PythonExecutor, "run_file")
  def test_runCommand(self, run_file_mock, dump_command_to_json_mock,
                      get_hook_base_dir_mock, get_service_base_dir_mock,
                      resolve_hook_script_path_mock, resolve_script_path_mock):
    command = {
      'role' : 'REGION_SERVER',
      'hostLevelParams' : {
        'stack_name' : 'HDP',
        'stack_version' : '2.0.7',
      },
      'commandParams': {
        'script_type': 'PYTHON',
        'script': 'scripts/hbase_regionserver.py',
        'command_timeout': '600',
        'service_metadata_folder' : 'HBASE'
      },
      'taskId' : '3',
      'roleCommand': 'INSTALL'
    }
    get_service_base_dir_mock.return_value = "/basedir/"
    resolve_script_path_mock.return_value = "/basedir/scriptpath"
    resolve_hook_script_path_mock.return_value = \
      ('/hooks_dir/prefix-command/scripts/hook.py',
       '/hooks_dir/prefix-command')
    orchestrator = CustomServiceOrchestrator(self.config)
    get_hook_base_dir_mock.return_value = "/hooks/"
    # normal run case
    run_file_mock.return_value = {
        'stdout' : 'sss',
        'stderr' : 'eee',
        'exitcode': 0,
      }
    ret = orchestrator.runCommand(command, "out.txt", "err.txt")
    self.assertEqual(ret['exitcode'], 0)
    self.assertTrue(run_file_mock.called)
    self.assertEqual(run_file_mock.call_count, 3)

    run_file_mock.reset_mock()
    # unknown script type case
    command['commandParams']['script_type'] = "PUPPET"
    ret = orchestrator.runCommand(command, "out.txt", "err.txt")
    self.assertEqual(ret['exitcode'], 1)
    self.assertFalse(run_file_mock.called)
    self.assertTrue("Unknown script type" in ret['stdout'])
    pass

  @patch.object(CustomServiceOrchestrator, "dump_command_to_json")
  @patch.object(PythonExecutor, "run_file")
  def test_runCommand_custom_action(self, run_file_mock, dump_command_to_json_mock):
    _, script = tempfile.mkstemp()
    command = {
      'role' : 'any',
      'commandParams': {
        'script_type': 'PYTHON',
        'script': 'some_custom_action.py',
        'command_timeout': '600',
      },
      'taskId' : '3',
      'roleCommand': 'ACTIONEXECUTE'
    }

    orchestrator = CustomServiceOrchestrator(self.config)
    # normal run case
    run_file_mock.return_value = {
      'stdout' : 'sss',
      'stderr' : 'eee',
      'exitcode': 0,
      }
    ret = orchestrator.runCommand(command, "out.txt", "err.txt")
    self.assertEqual(ret['exitcode'], 0)
    self.assertTrue(run_file_mock.called)
    # Hoooks are not supported for custom actions,
    # that's why run_file() should be called only once
    self.assertEqual(run_file_mock.call_count, 1)


  @patch("os.path.isfile")
  def test_resolve_hook_script_path(self, isfile_mock):

    orchestrator = CustomServiceOrchestrator(self.config)
    # Testing None param
    res1 = orchestrator.resolve_hook_script_path(None, "prefix", "command",
                                            "script_type")
    self.assertEqual(res1, None)
    # Testing existing hook script
    isfile_mock.return_value = True
    res2 = orchestrator.resolve_hook_script_path("/hooks_dir/", "prefix", "command",
                                            "script_type")
    self.assertEqual(res2, ('/hooks_dir/prefix-command/scripts/hook.py',
                            '/hooks_dir/prefix-command'))
    # Testing not existing hook script
    isfile_mock.return_value = False
    res3 = orchestrator.resolve_hook_script_path("/hooks_dir/", "prefix", "command",
                                                 "script_type")
    self.assertEqual(res3, None)
    pass


  def tearDown(self):
    # enable stdout
    sys.stdout = sys.__stdout__


