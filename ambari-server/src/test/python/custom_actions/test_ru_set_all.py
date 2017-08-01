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

from stacks.utils.RMFTestCase import experimental_mock
patch('resource_management.libraries.functions.decorator.experimental', experimental_mock).start()

# Module imports
from stacks.utils.RMFTestCase import *
from resource_management import Script, ConfigDictionary
from resource_management.libraries.functions.default import default
from resource_management.core.logger import Logger
from ambari_commons.os_check import OSCheck
from resource_management.core.environment import Environment
import pprint


def fake_call(command, **kwargs):
  """
  Instead of shell.call, call a command whose output equals the command.
  :param command: Command that will be echoed.
  :return: Returns a tuple of (process output code, output)
  """
  return (0, str(command))

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
    from ru_set_all import link_config
    global link_config

  def tearDown(self):
    Logger.logger = None

  @patch("os.path.exists")
  @patch("resource_management.core.shell.call")
  @patch.object(Script, 'get_config')
  @patch.object(OSCheck, 'is_redhat_family')
  def test_execution(self, family_mock, get_config_mock, call_mock, exists_mock):
    # Mock the config objects
    json_file_path = os.path.join(self.get_custom_actions_dir(), "ru_execute_tasks_namenode_prepare.json")
    self.assertTrue(os.path.isfile(json_file_path))
    
    with open(json_file_path, "r") as json_file:
      json_payload = json.load(json_file)

    json_payload["configurations"]["cluster-env"]["stack_tools"] = self.get_stack_tools()
    json_payload["configurations"]["cluster-env"]["stack_features"] = self.get_stack_features()

    config_dict = ConfigDictionary(json_payload)

    family_mock.return_value = True
    get_config_mock.return_value = config_dict
    call_mock.side_effect = fake_call   # echo the command
    exists_mock.return_value = True

    # Ensure that the json file was actually read.
    stack_name = default("/hostLevelParams/stack_name", None)
    stack_version = default("/hostLevelParams/stack_version", None)
    service_package_folder = default('/roleParams/service_package_folder', None)

    self.assertEqual(stack_name, "HDP")
    self.assertEqual(stack_version, '2.2')
    self.assertEqual(service_package_folder, "common-services/HDFS/2.1.0.2.0/package")

    # Begin the test
    ru_execute = UpgradeSetAll()
    ru_execute.actionexecute(None)

    call_mock.assert_called_with(('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'all', u'2.2.1.0-2260'), sudo=True)

  @patch("os.path.exists")
  @patch("resource_management.core.shell.call")
  @patch.object(Script, 'get_config')
  @patch.object(OSCheck, 'is_redhat_family')
  @patch("ru_set_all.link_config")
  def test_execution_23(self, link_mock, family_mock, get_config_mock, call_mock, exists_mock):
    # Mock the config objects
    json_file_path = os.path.join(self.get_custom_actions_dir(), "ru_execute_tasks_namenode_prepare.json")
    self.assertTrue(os.path.isfile(json_file_path))
    with open(json_file_path, "r") as json_file:
      json_payload = json.load(json_file)

    json_payload['hostLevelParams']['stack_name'] = "HDP"
    json_payload['hostLevelParams']['stack_version'] = "2.3"
    json_payload['commandParams']['version'] = "2.3.0.0-1234"
    json_payload["configurations"]["cluster-env"]["stack_tools"] = self.get_stack_tools()
    json_payload["configurations"]["cluster-env"]["stack_features"] = self.get_stack_features()
    json_payload["configurations"]["cluster-env"]["stack_packages"] = self.get_stack_packages()

    config_dict = ConfigDictionary(json_payload)

    family_mock.return_value = True
    get_config_mock.return_value = config_dict
    call_mock.side_effect = fake_call   # echo the command
    exists_mock.return_value = True

    # Ensure that the json file was actually read.
    stack_name = default("/hostLevelParams/stack_name", None)
    stack_version = default("/hostLevelParams/stack_version", None)
    service_package_folder = default('/roleParams/service_package_folder', None)

    self.assertEqual(stack_name, "HDP")
    self.assertEqual(stack_version, '2.3')
    self.assertEqual(service_package_folder, "common-services/HDFS/2.1.0.2.0/package")

    # Begin the test
    ru_execute = UpgradeSetAll()
    ru_execute.actionexecute(None)

    self.assertTrue(link_mock.called)
    call_mock.assert_called_with(('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'all', '2.3.0.0-1234'), sudo=True)

  @patch("os.path.exists")
  @patch("resource_management.core.shell.call")
  @patch.object(Script, 'get_config')
  @patch.object(OSCheck, 'is_redhat_family')
  def test_skippable_hosts(self, family_mock, get_config_mock, call_mock, exists_mock):
    """
    Tests that hosts are skippable if they don't have stack components installed
    :return:
    """
    # Mock the config objects
    json_file_path = os.path.join(self.get_custom_actions_dir(),
      "ru_execute_tasks_namenode_prepare.json")
    self.assertTrue(os.path.isfile(json_file_path))

    with open(json_file_path, "r") as json_file:
      json_payload = json.load(json_file)

    json_payload["configurations"]["cluster-env"]["stack_tools"] = self.get_stack_tools()
    json_payload["configurations"]["cluster-env"]["stack_features"] = self.get_stack_features()

    config_dict = ConfigDictionary(json_payload)

    family_mock.return_value = False
    get_config_mock.return_value = config_dict
    exists_mock.return_value = True

    def hdp_select_call(command, **kwargs):
      # return no versions
      if "versions" in command:
        return (0,"")

      return (0,command)

    call_mock.side_effect = hdp_select_call

    # Ensure that the json file was actually read.
    stack_name = default("/hostLevelParams/stack_name", None)
    stack_version = default("/hostLevelParams/stack_version", None)
    service_package_folder = default('/roleParams/service_package_folder', None)

    self.assertEqual(stack_name, "HDP")
    self.assertEqual(stack_version, '2.2')
    self.assertEqual(service_package_folder, "common-services/HDFS/2.1.0.2.0/package")

    # Begin the test
    ru_execute = UpgradeSetAll()
    ru_execute.actionexecute(None)

    call_mock.assert_called_with(('ambari-python-wrap', u'/usr/bin/hdp-select', 'versions'), sudo = True)
    self.assertEqual(call_mock.call_count, 1)


  @patch("os.path.exists")
  @patch("os.path.islink")
  @patch("os.path.isdir")
  @patch("os.path.isfile")
  @patch("os.path.realpath")
  @patch("shutil.rmtree")
  def test_link_config(self, shutil_rmtree_mock, os_path_realpath_mock, os_path_isfile_mock,
                       os_path_isdir_mock, os_path_islink_mock,
                       os_path_exists_mock):
    # Test normal flow
    os_path_islink_mock.return_value = False
    os_path_realpath_mock.return_value = "/some/another/path"
    os_path_exists_mock.side_effect = [True, False]
    old_config = "/old/config"
    link_conf = "/link/config"

    with Environment(test_mode=True) as RMFTestCase.env:
      link_config(old_config, link_conf)
      self.assertTrue(shutil_rmtree_mock.called)
      self.assertEquals(shutil_rmtree_mock.call_args_list[0][0][0], old_config)
      self.assertResourceCalled('Execute', ('cp', '-R', '-p', '/old/config', '/old/conf.backup'),
                                logoutput = True,
                                sudo = True,
                                )
      self.assertResourceCalled('Link', '/old/config',
                                to = '/link/config',
                                )
      self.assertNoMoreResources()

    # Test case when link exists but is wrong
    shutil_rmtree_mock.reset_mock()
    os_path_islink_mock.return_value = True
    with Environment(test_mode=True) as RMFTestCase.env:
      link_config(old_config, link_conf)
      self.assertFalse(shutil_rmtree_mock.called)
      self.assertResourceCalled('Link', '/old/config',
                                to = '/link/config',
                                )
      self.assertNoMoreResources()

    # Test case when link exists and is correct
    shutil_rmtree_mock.reset_mock()
    os_path_islink_mock.return_value = True
    os_path_realpath_mock.return_value = link_conf

    with Environment(test_mode=True) as RMFTestCase.env:
      link_config(old_config, link_conf)
      self.assertFalse(shutil_rmtree_mock.called)
      self.assertNoMoreResources()

    # Test case when old link does not exist at all
    shutil_rmtree_mock.reset_mock()
    os_path_islink_mock.return_value = False
    os_path_exists_mock.side_effect = [False]

    with Environment(test_mode=True) as RMFTestCase.env:
      link_config(old_config, link_conf)
      self.assertFalse(shutil_rmtree_mock.called)
      self.assertNoMoreResources()

    # Test case when backup directory already exists
    shutil_rmtree_mock.reset_mock()
    os_path_islink_mock.return_value = False
    os_path_exists_mock.side_effect = [True, True]

    with Environment(test_mode=True) as RMFTestCase.env:
      link_config(old_config, link_conf)
      self.assertTrue(shutil_rmtree_mock.called)
      self.assertEquals(shutil_rmtree_mock.call_args_list[0][0][0], old_config)
      self.assertResourceCalled('Link', '/old/config',
                                to = '/link/config',
                                )
      self.assertNoMoreResources()
