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


  def test_dump_command_to_json(self):
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


  @patch("os.path.exists")
  def test_resolve_script_path(self, exists_mock):
    config = AmbariConfig().getConfig()
    orchestrator = CustomServiceOrchestrator(config)
    # Testing existing path
    exists_mock.return_value = True
    path = orchestrator.\
      resolve_script_path("/HBASE", "scripts/hbase_master.py", "PYTHON")
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
  @patch.object(FileCache, "get_service_base_dir")
  @patch.object(CustomServiceOrchestrator, "dump_command_to_json")
  @patch.object(PythonExecutor, "run_file")
  def test_runCommand(self, run_file_mock, dump_command_to_json_mock,
                      get_service_base_dir_mock, resolve_script_path_mock):
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
      'roleCommand': 'INSTALL'
    }
    get_service_base_dir_mock.return_value = "/basedir/"
    resolve_script_path_mock.return_value = "/basedir/scriptpath"
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

    run_file_mock.reset_mock()
    # unknown script type case
    command['commandParams']['script_type'] = "PUPPET"
    ret = orchestrator.runCommand(command, "out.txt", "err.txt")
    self.assertEqual(ret['exitcode'], 1)
    self.assertFalse(run_file_mock.called)
    self.assertTrue("Unknown script type" in ret['stdout'])
    pass


  def tearDown(self):
    # enable stdout
    sys.stdout = sys.__stdout__


