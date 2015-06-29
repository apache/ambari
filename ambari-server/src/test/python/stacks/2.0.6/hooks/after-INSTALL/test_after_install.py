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

import json
from mock.mock import MagicMock, call, patch
from stacks.utils.RMFTestCase import *

@patch("os.path.exists", new = MagicMock(return_value=True))
class TestHookAfterInstall(RMFTestCase):

  def test_hook_default(self):

    self.executeScript("2.0.6/hooks/after-INSTALL/scripts/hook.py",
                       classname="AfterInstallHook",
                       command="hook",
                       config_file="default.json"
    )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['core-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['core-site'],
                              only_if="ls /etc/hadoop/conf")

    self.assertNoMoreResources()


  @patch("shared_initialization.load_version", new = MagicMock(return_value="2.3.0.0-1243"))
  @patch("resource_management.libraries.functions.conf_select.create")
  @patch("resource_management.libraries.functions.conf_select.select")
  @patch("os.symlink")
  @patch("shutil.rmtree")
  def test_hook_default_conf_select(self, rmtree_mock, symlink_mock, conf_select_select_mock, conf_select_create_mock):

    def mocked_conf_select(arg1, arg2, arg3, dry_run = False):
      return "/etc/{0}/{1}/0".format(arg2, arg3)

    conf_select_create_mock.side_effect = mocked_conf_select

    config_file = self.get_src_folder() + "/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)

    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version
    json_content['hostLevelParams']['stack_version'] = "2.3"

    self.executeScript("2.0.6/hooks/after-INSTALL/scripts/hook.py",
                       classname="AfterInstallHook",
                       command="hook",
                       config_dict = json_content)


    self.assertResourceCalled("Execute",
      "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E touch /var/lib/ambari-agent/data/hdp-select-set-all.performed ; " \
      "ambari-sudo.sh /usr/bin/hdp-select set all `ambari-python-wrap /usr/bin/hdp-select versions | grep ^2.3 | tail -1`",
      only_if = "ls -d /usr/hdp/2.3*",
      not_if = "test -f /var/lib/ambari-agent/data/hdp-select-set-all.performed")


    self.assertResourceCalled('XmlConfig', 'core-site.xml',
      owner = 'hdfs',
      group = 'hadoop',
      conf_dir = "/usr/hdp/current/hadoop-client/conf",
      configurations = self.getConfig()['configurations']['core-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['core-site'],
      only_if="ls /usr/hdp/current/hadoop-client/conf")

    self.assertNoMoreResources()
