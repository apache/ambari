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
import os
from mock.mock import patch, MagicMock
from stacks.utils.RMFTestCase import *
from resource_management.core.logger import Logger
from resource_management.libraries.functions import conf_select
from resource_management.libraries.script import Script

class TestConfSelect(RMFTestCase):

  def setUp(self):
    Logger.initialize_logger()

    # required for the test to run since the Execute calls need this
    from resource_management.core.environment import Environment
    self.env = Environment(test_mode=True)
    self.env.__enter__()

    Script.config = dict()
    Script.config.update( { "configurations" : { "cluster-env" : {} }, "clusterLevelParams": {} } )
    Script.config["configurations"]["cluster-env"]["stack_packages"] = RMFTestCase.get_stack_packages()
    Script.config["clusterLevelParams"] = { "stack_name" : "HDP" }


  def tearDown(self):
    self.env.__exit__(None,None,None)


  @patch("resource_management.libraries.functions.conf_select._valid", new = MagicMock(return_value=True))
  def test_select_throws_error(self):
    """
    Tests that conf-select throws errors correctly
    :return:
    """
    try:
      conf_select.select("foo", "bar", "version", ignore_errors = False)
      self.fail("Expected an error from conf-select")
    except:
      pass

    conf_select.select("foo", "bar", "version", ignore_errors = True)


  @patch("resource_management.core.shell.call")
  @patch("resource_management.libraries.functions.conf_select._valid", new = MagicMock(return_value=True))
  @patch("resource_management.libraries.functions.stack_tools.get_stack_tool_path", new = MagicMock(return_value="/usr/bin/conf-select"))
  def test_create_seeds_configuration_directories(self, shell_call_mock):
    """
    Tests that conf-select seeds new directories
    :return:
    """

    def mock_call(command, **kwargs):
      """
      Instead of shell.call, call a command whose output equals the command.
      :param command: Command that will be echoed.
      :return: Returns a tuple of (process output code, stdout, stderr)
      """
      return (0, "/etc/foo/conf", None)

    shell_call_mock.side_effect = mock_call
    conf_select.create("HDP", "oozie", "version")

    self.assertEqual(pprint.pformat(self.env.resource_list),
      "[Directory['/etc/foo/conf'],\n "
      "Execute['ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E cp -R -p -v /usr/hdp/current/oozie-client/conf/* /etc/foo/conf']]")


  def test_symlink_conversion_bad_linkto(self):
    """
    Tests that a bad enum throws an exception.
    :return:
    """
    packages = conf_select.get_package_dirs()

    try:
      conf_select.convert_conf_directories_to_symlinks("hadoop", "2.3.0.0-1234",
        packages["hadoop"], link_to = "INVALID")
      raise Exception("Expected failure when supplying a bad enum for link_to")
    except:
      pass


  @patch("resource_management.core.shell.call")
  @patch.object(os.path, "isdir")
  @patch.object(os.path, "exists")
  @patch.object(os.path, "islink")
  @patch("resource_management.libraries.functions.conf_select._valid", new = MagicMock(return_value = True))
  @patch("resource_management.libraries.functions.conf_select.create", new = MagicMock(return_value = ["/etc/hadoop/2.3.0.0-1234/0"]))
  @patch("resource_management.libraries.functions.conf_select.select", new = MagicMock())
  def test_symlink_conversion_to_current(self, islink_mock, path_mock, isdir_mock, shell_call_mock):
    """
    Tests that conf-select creates the correct symlink directories.
    :return:
    """

    def mock_call(command, **kwargs):
      """
      Instead of shell.call, call a command whose output equals the command.
      :param command: Command that will be echoed.
      :return: Returns a tuple of (process output code, stdout, stderr)
      """
      return (0, "/etc/hadoop/conf", None)

    def path_mock_call(path):
      if path == "/etc/hadoop/conf":
        return True

      if path == "/etc/hadoop/2.3.0.0-1234/0":
        return True

      return False

    def islink_mock_call(path):
      if path == "/etc/hadoop/conf":
        return False

      return False

    def isdir_mock_call(path):
      if path == "/etc/hadoop/conf":
        return True

      return False


    packages = conf_select.get_package_dirs()

    path_mock.side_effect = path_mock_call
    islink_mock.side_effect = islink_mock_call
    shell_call_mock.side_effect = mock_call
    conf_select.convert_conf_directories_to_symlinks("hadoop", "2.3.0.0-1234", packages["hadoop"])

    self.assertEqual(pprint.pformat(self.env.resource_list[0]), "Execute[('cp', '-R', '-p', u'/etc/hadoop/conf', u'/etc/hadoop/conf.backup')]")
    self.assertEqual(pprint.pformat(self.env.resource_list[1]), "Directory['/etc/hadoop/conf']")
    self.assertEqual(pprint.pformat(self.env.resource_list[2]), "Link['/etc/hadoop/conf']")


  @patch.object(os.path, "exists", new = MagicMock(return_value = True))
  @patch.object(os.path, "islink", new = MagicMock(return_value = True))
  @patch.object(os, "readlink", new = MagicMock(return_value = "/etc/component/invalid"))
  @patch("resource_management.libraries.functions.conf_select._valid", new = MagicMock(return_value = True))
  @patch("resource_management.libraries.functions.conf_select.create", new = MagicMock(return_value = ["/etc/hadoop/2.3.0.0-1234/0"]))
  @patch("resource_management.libraries.functions.conf_select.select", new = MagicMock())
  def test_symlink_conversion_relinks_wrong_link(self):
    """
    Tests that conf-select symlinking can detect a wrong directory
    :return:
    """
    packages = conf_select.get_package_dirs()

    conf_select.convert_conf_directories_to_symlinks("hadoop", "2.3.0.0-1234",
      packages["hadoop"])

    self.assertEqual(pprint.pformat(self.env.resource_list),
      "[Link['/etc/hadoop/conf'], Link['/etc/hadoop/conf']]")


  @patch.object(os.path, "exists", new = MagicMock(return_value = False))
  @patch("resource_management.libraries.functions.conf_select._valid", new = MagicMock(return_value = True))
  def test_symlink_noop(self):
    """
    Tests that conf-select symlinking does nothing if the directory doesn't exist
    :return:
    """
    packages = conf_select.get_package_dirs()

    conf_select.convert_conf_directories_to_symlinks("hadoop", "2.3.0.0-1234", packages["hadoop"])

    self.assertEqual(pprint.pformat(self.env.resource_list), "[]")


  def test_restrictions(self):

    Script.config.update({'roleParameters': {'cluster_version_summary': {'services': {'HIVE': {'upgrade': True}}}}})

    restricted = conf_select.get_restricted_packages()
    self.assertTrue("hive" in restricted)
    self.assertTrue("hive-hcatalog" in restricted)
    self.assertTrue("hive2" in restricted)
    self.assertTrue("tez_hive2" in restricted)
    self.assertTrue("hadoop" not in restricted)
