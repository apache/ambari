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
import subprocess
from stacks.utils.RMFTestCase import *
from resource_management import Script, ConfigDictionary
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions import conf_select
from resource_management.core.logger import Logger
from ambari_agent.AmbariConfig import AmbariConfig
from ambari_agent.FileCache import FileCache
from ambari_commons.os_check import OSCheck
from resource_management.core import shell
import pprint


def fake_call(command, **kwargs):
  """
  Instead of shell.call, call a command whose output equals the command.
  :param command: Command that will be echoed.
  :return: Returns a tuple of (process output code, output)
  """
  return (0, command)


class TestRUSetAll(RMFTestCase):
  def get_custom_actions_dir(self):
    return os.path.join(self.get_src_folder(), "test/resources/custom_actions/")

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
    json_file_path = os.path.join(self.get_custom_actions_dir(), "ru_execute_tasks_namenode_prepare.json")
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

    call_mock.assert_called_with(('hdp-select', 'set', 'all', u'2.2.1.0-2260'), sudo=True)

  @patch("resource_management.core.shell.call")
  @patch.object(Script, 'get_config')
  @patch.object(OSCheck, 'is_redhat_family')
  @patch("ru_set_all.link_config")
  def test_execution_23(self, link_mock, family_mock, get_config_mock, call_mock):
    # Mock the config objects
    json_file_path = os.path.join(self.get_custom_actions_dir(), "ru_execute_tasks_namenode_prepare.json")
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
    call_mock.assert_called_with(('hdp-select', 'set', 'all', '2.3.0.0-1234'), sudo=True)


  @patch("os.path.islink")
  @patch("os.path.isdir")
  @patch("resource_management.core.shell.call")
  @patch.object(Script, 'get_config')
  @patch.object(OSCheck, 'is_redhat_family')
  def test_downgrade_unlink_configs(self, family_mock, get_config_mock, call_mock,
                                    isdir_mock, islink_mock):
    """
    Tests downgrading from 2.3 to 2.2 to ensure that conf symlinks are removed and the backup
    directories restored.
    """

    isdir_mock.return_value = True

    # required for the test to run since the Execute calls need this
    from resource_management.core.environment import Environment
    env = Environment(test_mode=True)
    with env:
      # Mock the config objects
      json_file_path = os.path.join(self.get_custom_actions_dir(), "ru_execute_tasks_namenode_prepare.json")
      self.assertTrue(os.path.isfile(json_file_path))
      with open(json_file_path, "r") as json_file:
        json_payload = json.load(json_file)

      # alter JSON for a downgrade from 2.3 to 2.2
      json_payload['commandParams']['version'] = "2.2.0.0-1234"
      json_payload['commandParams']['downgrade_from_version'] = "2.3.0.0-1234"
      json_payload['commandParams']['original_stack'] = "HDP-2.2"
      json_payload['commandParams']['target_stack'] = "HDP-2.3"
      json_payload['commandParams']['upgrade_direction'] = "downgrade"
      json_payload['hostLevelParams']['stack_version'] = "2.2"

      config_dict = ConfigDictionary(json_payload)

      family_mock.return_value = True
      get_config_mock.return_value = config_dict
      call_mock.side_effect = fake_call   # echo the command

      # test the function
      ru_execute = UpgradeSetAll()
      ru_execute.unlink_all_configs(None)

      # verify that os.path.islink was called for each conf
      self.assertTrue(islink_mock.called)
      for key, value in conf_select.PACKAGE_DIRS.iteritems():
        for directory_mapping in value:
          original_config_directory = directory_mapping['conf_dir']
          is_link_called = False

          for call in islink_mock.call_args_list:
            call_tuple = call[0]
            if original_config_directory in call_tuple:
              is_link_called = True

          if not is_link_called:
            self.fail("os.path.islink({0}) was never called".format(original_config_directory))

      # alter JSON for a downgrade from 2.3 to 2.3
      with open(json_file_path, "r") as json_file:
        json_payload = json.load(json_file)

      json_payload['commandParams']['version'] = "2.3.0.0-1234"
      json_payload['commandParams']['downgrade_from_version'] = "2.3.0.0-5678"
      json_payload['commandParams']['original_stack'] = "HDP-2.3"
      json_payload['commandParams']['target_stack'] = "HDP-2.3"
      json_payload['commandParams']['upgrade_direction'] = "downgrade"
      json_payload['hostLevelParams']['stack_version'] = "2.3"

      # reset config
      config_dict = ConfigDictionary(json_payload)
      family_mock.return_value = True
      get_config_mock.return_value = config_dict

      # reset mock
      islink_mock.reset_mock()

      # test the function
      ru_execute = UpgradeSetAll()
      ru_execute.unlink_all_configs(None)

      # ensure it wasn't called this time
      self.assertFalse(islink_mock.called)

      with open(json_file_path, "r") as json_file:
        json_payload = json.load(json_file)

      # alter JSON for a downgrade from 2.2 to 2.2
      json_payload['commandParams']['version'] = "2.2.0.0-1234"
      json_payload['commandParams']['downgrade_from_version'] = "2.2.0.0-5678"
      json_payload['commandParams']['original_stack'] = "HDP-2.2"
      json_payload['commandParams']['target_stack'] = "HDP-2.2"
      json_payload['commandParams']['upgrade_direction'] = "downgrade"
      json_payload['hostLevelParams']['stack_version'] = "2.2"

      # reset config
      config_dict = ConfigDictionary(json_payload)
      family_mock.return_value = True
      get_config_mock.return_value = config_dict

      # reset mock
      islink_mock.reset_mock()

      # test the function
      ru_execute = UpgradeSetAll()
      ru_execute.unlink_all_configs(None)

      # ensure it wasn't called this time
      self.assertFalse(islink_mock.called)

  @patch("os.path.isdir")
  @patch("os.path.exists")
  def test_unlink_configs_missing_backup(self, exists_mock, isdir_mock):

    # required for the test to run since the Execute calls need this
    from resource_management.core.environment import Environment
    env = Environment(test_mode=True)
    with env:
      # Case: missing backup directory
      isdir_mock.return_value = False
      ru_execute = UpgradeSetAll()
      self.assertEqual(len(env.resource_list), 0)
      # Case: missing symlink
      isdir_mock.reset_mock()
      isdir_mock.return_value = True
      exists_mock.return_value = False
      ru_execute._unlink_config("/fake/config")
      self.assertEqual(len(env.resource_list), 2)
      # Case: missing symlink
      isdir_mock.reset_mock()
      isdir_mock.return_value = True
      exists_mock.reset_mock()
      exists_mock.return_value = True

      ru_execute._unlink_config("/fake/config")

      self.assertEqual(pprint.pformat(env.resource_list),
                       "[Directory['/fake/config'],\n "
                       "Execute[('mv', '/fake/conf.backup', '/fake/config')],\n "
                       "Directory['/fake/config'],\n "
                       "Execute[('mv', '/fake/conf.backup', '/fake/config')]]")