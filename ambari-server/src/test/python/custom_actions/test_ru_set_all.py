# !/usr/bin/env python

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

# Python Imports
import os
import json

from mock.mock import patch
from mock.mock import MagicMock

# Module imports
from stacks.utils.RMFTestCase import *
from resource_management import Script, ConfigDictionary
from resource_management.libraries.functions.default import default
from resource_management.core.logger import Logger
from ambari_agent.AmbariConfig import AmbariConfig
from ambari_agent.FileCache import FileCache
from ambari_commons.os_check import OSCheck


def fake_call(command):
  """
  Instead of shell.call, call a command whose output equals the command.
  :param command: Command that will be echoed.
  :return: Returns a tuple of (process output code, output)
  """
  return (0, command)


class TestRUSetAll(RMFTestCase):
  CUSTOM_ACTIONS_DIR = os.path.join(os.getcwd(), "../resources/custom_actions/")

  @patch.object(Logger, "info")
  @patch.object(Logger, "error")
  def setUp(self, error_mock, info_mock):

    Logger.logger = MagicMock()

    # Import the class under test. This is done here as opposed to the rest of the imports because the get_os_type()
    # method needs to be patched first.
    from ru_set_all import UpgradeSetAll
    global UpgradeSetAll

  def tearDown(self):
    Logger.logger = None

  @patch("resource_management.core.shell.call")
  @patch.object(Script, 'get_config')
  @patch.object(OSCheck, 'is_redhat_family')
  def test_execution(self, family_mock, get_config_mock, call_mock):
    # Mock the config objects
    json_file_path = os.path.join(self.CUSTOM_ACTIONS_DIR, "ru_execute_tasks_namenode_prepare.json")
    self.assertTrue(os.path.isfile(json_file_path))
    with open(json_file_path, "r") as json_file:
      json_payload = json.load(json_file)

    config_dict = ConfigDictionary(json_payload)

    family_mock.return_value = True
    get_config_mock.return_value = config_dict
    call_mock.side_effect = fake_call   # echo the command

    # Ensure that the json file was actually read.
    stack_name = default("/hostLevelParams/stack_name", None)
    stack_version = default("/hostLevelParams/stack_version", None)
    service_package_folder = default('/roleParams/service_package_folder', None)

    self.assertEqual(stack_name, "HDP")
    self.assertEqual(stack_version, 2.2)
    self.assertEqual(service_package_folder, "common-services/HDFS/2.1.0.2.0/package")

    # Begin the test
    ru_execute = UpgradeSetAll()
    ru_execute.actionexecute(None)

    call_mock.assert_called_with("hdp-select set all 2.2.1.0-2260")

  @patch("resource_management.core.shell.call")
  @patch.object(Script, 'get_config')
  @patch.object(OSCheck, 'is_redhat_family')
  @patch("ru_set_all.link_config")
  def test_execution_23(self, link_mock, family_mock, get_config_mock, call_mock):
    # Mock the config objects
    json_file_path = os.path.join(self.CUSTOM_ACTIONS_DIR, "ru_execute_tasks_namenode_prepare.json")
    self.assertTrue(os.path.isfile(json_file_path))
    with open(json_file_path, "r") as json_file:
      json_payload = json.load(json_file)

    json_payload['hostLevelParams']['stack_version'] = "2.3"
    json_payload['commandParams']['version'] = "2.3.0.0-1234"

    config_dict = ConfigDictionary(json_payload)

    family_mock.return_value = True
    get_config_mock.return_value = config_dict
    call_mock.side_effect = fake_call   # echo the command

    # Ensure that the json file was actually read.
    stack_name = default("/hostLevelParams/stack_name", None)
    stack_version = default("/hostLevelParams/stack_version", None)
    service_package_folder = default('/roleParams/service_package_folder', None)

    self.assertEqual(stack_name, "HDP")
    self.assertEqual(stack_version, 2.3)
    self.assertEqual(service_package_folder, "common-services/HDFS/2.1.0.2.0/package")

    # Begin the test
    ru_execute = UpgradeSetAll()
    ru_execute.actionexecute(None)

    self.assertTrue(link_mock.called)
    call_mock.assert_called_with("hdp-select set all 2.3.0.0-1234")

