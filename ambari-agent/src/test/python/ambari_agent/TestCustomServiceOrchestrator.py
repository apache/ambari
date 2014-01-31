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
from LiveStatus import LiveStatus
import manifestGenerator


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


  @patch.object(FileCache, "__init__")
  def test_add_reg_listener_to_controller(self, FileCache_mock):
    FileCache_mock.return_value = None
    dummy_controller = MagicMock()
    config = AmbariConfig().getConfig()
    tempdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tempdir)
    CustomServiceOrchestrator(config, dummy_controller)
    self.assertTrue(dummy_controller.registration_listeners.append.called)


  @patch.object(manifestGenerator, 'decompressClusterHostInfo')
  @patch("hostname.public_hostname")
  @patch("os.path.isfile")
  @patch("os.unlink")
  @patch.object(FileCache, "__init__")
  def test_dump_command_to_json(self, FileCache_mock, unlink_mock,
                                isfile_mock, hostname_mock,
                                decompress_cluster_host_info_mock):
    FileCache_mock.return_value = None
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
      'configurationTags':{'global' : { 'tag': 'v1' }},
      'clusterHostInfo':{'namenode_host' : ['1'],
                         'slave_hosts'   : ['0', '1'],
                         'all_hosts'     : ['h1.hortonworks.com', 'h2.hortonworks.com'],
                         'all_ping_ports': ['8670:0,1']}
    }
    
    decompress_cluster_host_info_mock.return_value = {'namenode_host' : ['h2.hortonworks.com'],
                         'slave_hosts'   : ['h1.hortonworks.com', 'h2.hortonworks.com'],
                         'all_hosts'     : ['h1.hortonworks.com', 'h2.hortonworks.com'],
                         'all_ping_ports': ['8670', '8670']}
    
    config = AmbariConfig().getConfig()
    tempdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tempdir)
    dummy_controller = MagicMock()
    orchestrator = CustomServiceOrchestrator(config, dummy_controller)
    isfile_mock.return_value = True
    # Test dumping EXECUTION_COMMAND
    json_file = orchestrator.dump_command_to_json(command)
    self.assertTrue(os.path.exists(json_file))
    self.assertTrue(os.path.getsize(json_file) > 0)
    self.assertEqual(oct(os.stat(json_file).st_mode & 0777), '0600')
    self.assertTrue(json_file.endswith("command-3.json"))
    self.assertTrue(decompress_cluster_host_info_mock.called)
    os.unlink(json_file)
    # Test dumping STATUS_COMMAND
    command['commandType']='STATUS_COMMAND'
    decompress_cluster_host_info_mock.reset_mock()
    json_file = orchestrator.dump_command_to_json(command)
    self.assertTrue(os.path.exists(json_file))
    self.assertTrue(os.path.getsize(json_file) > 0)
    self.assertEqual(oct(os.stat(json_file).st_mode & 0777), '0600')
    self.assertTrue(json_file.endswith("status_command.json"))
    self.assertFalse(decompress_cluster_host_info_mock.called)
    os.unlink(json_file)
    # Testing side effect of dump_command_to_json
    self.assertEquals(command['public_hostname'], "test.hst")
    self.assertTrue(unlink_mock.called)


  @patch("os.path.exists")
  @patch.object(FileCache, "__init__")
  def test_resolve_script_path(self, FileCache_mock, exists_mock):
    FileCache_mock.return_value = None
    dummy_controller = MagicMock()
    config = AmbariConfig().getConfig()
    orchestrator = CustomServiceOrchestrator(config, dummy_controller)
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
  @patch.object(FileCache, "__init__")
  def test_runCommand(self, FileCache_mock,
                      run_file_mock, dump_command_to_json_mock,
                      get_hook_base_dir_mock, get_service_base_dir_mock,
                      resolve_hook_script_path_mock, resolve_script_path_mock):
    FileCache_mock.return_value = None
    command = {
      'role' : 'REGION_SERVER',
      'hostLevelParams' : {
        'stack_name' : 'HDP',
        'stack_version' : '2.0.7',
        'jdk_location' : 'some_location'
      },
      'commandParams': {
        'script_type': 'PYTHON',
        'script': 'scripts/hbase_regionserver.py',
        'command_timeout': '600',
        'service_package_folder' : 'HBASE'
      },
      'taskId' : '3',
      'roleCommand': 'INSTALL'
    }
    get_service_base_dir_mock.return_value = "/basedir/"
    resolve_script_path_mock.return_value = "/basedir/scriptpath"
    resolve_hook_script_path_mock.return_value = \
      ('/hooks_dir/prefix-command/scripts/hook.py',
       '/hooks_dir/prefix-command')
    dummy_controller = MagicMock()
    orchestrator = CustomServiceOrchestrator(self.config, dummy_controller)
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

    # Case when we force another command
    run_file_mock.return_value = {
        'stdout' : 'sss',
        'stderr' : 'eee',
        'exitcode': 0,
      }
    ret = orchestrator.runCommand(command, "out.txt", "err.txt",
              forsed_command_name=CustomServiceOrchestrator.COMMAND_NAME_STATUS)
    ## Check that override_output_files was true only during first call
    self.assertEquals(run_file_mock.call_args_list[0][0][6], True)
    self.assertEquals(run_file_mock.call_args_list[1][0][6], False)
    self.assertEquals(run_file_mock.call_args_list[2][0][6], False)
    ## Check that forsed_command_name was taken into account
    self.assertEqual(run_file_mock.call_args_list[0][0][1][0],
                                  CustomServiceOrchestrator.COMMAND_NAME_STATUS)

    run_file_mock.reset_mock()

    # unknown script type case
    command['commandParams']['script_type'] = "PUPPET"
    ret = orchestrator.runCommand(command, "out.txt", "err.txt")
    self.assertEqual(ret['exitcode'], 1)
    self.assertFalse(run_file_mock.called)
    self.assertTrue("Unknown script type" in ret['stdout'])

    #By default returns empty dictionary
    self.assertEqual(ret['structuredOut'], '{}')

    pass

  @patch.object(CustomServiceOrchestrator, "dump_command_to_json")
  @patch.object(PythonExecutor, "run_file")
  @patch.object(FileCache, "__init__")
  @patch.object(FileCache, "get_custom_actions_base_dir")
  def test_runCommand_custom_action(self, get_custom_actions_base_dir_mock,
                                    FileCache_mock,
                                    run_file_mock, dump_command_to_json_mock):
    FileCache_mock.return_value = None
    get_custom_actions_base_dir_mock.return_value = "some path"
    _, script = tempfile.mkstemp()
    command = {
      'role' : 'any',
      'hostLevelParams' : {
        'jdk_location' : 'some_location'
      },
      'commandParams': {
        'script_type': 'PYTHON',
        'script': 'some_custom_action.py',
        'command_timeout': '600',
      },
      'taskId' : '3',
      'roleCommand': 'ACTIONEXECUTE'
    }
    dummy_controller = MagicMock()
    orchestrator = CustomServiceOrchestrator(self.config, dummy_controller)
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
  @patch.object(FileCache, "__init__")
  def test_resolve_hook_script_path(self, FileCache_mock, isfile_mock):
    FileCache_mock.return_value = None
    dummy_controller = MagicMock()
    orchestrator = CustomServiceOrchestrator(self.config, dummy_controller)
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


  @patch.object(CustomServiceOrchestrator, "runCommand")
  @patch.object(FileCache, "__init__")
  def test_requestComponentStatus(self, FileCache_mock, runCommand_mock):
    FileCache_mock.return_value = None
    status_command = {
      "serviceName" : 'HDFS',
      "commandType" : "STATUS_COMMAND",
      "clusterName" : "",
      "componentName" : "DATANODE",
      'configurations':{}
    }
    dummy_controller = MagicMock()
    orchestrator = CustomServiceOrchestrator(self.config, dummy_controller)
    # Test alive case
    runCommand_mock.return_value = {
      "exitcode" : 0
    }
    status = orchestrator.requestComponentStatus(status_command)
    self.assertEqual(LiveStatus.LIVE_STATUS, status)

    # Test dead case
    runCommand_mock.return_value = {
      "exitcode" : 1
    }
    status = orchestrator.requestComponentStatus(status_command)
    self.assertEqual(LiveStatus.DEAD_STATUS, status)


  def tearDown(self):
    # enable stdout
    sys.stdout = sys.__stdout__


