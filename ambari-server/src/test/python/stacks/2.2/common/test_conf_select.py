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
from mock.mock import patch, MagicMock
from stacks.utils.RMFTestCase import *
from resource_management.core.logger import Logger
from resource_management.libraries.functions import conf_select

class TestConfSelect(RMFTestCase):

  def setUp(self):
    Logger.initialize_logger()

    # required for the test to run since the Execute calls need this
    from resource_management.core.environment import Environment
    self.env = Environment(test_mode=True)
    self.env._instances.append(self.env)


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