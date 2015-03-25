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
from mock.mock import MagicMock, call, patch
from stacks.utils.RMFTestCase import *

class TestTezClient(RMFTestCase):

  COMMON_SERVICES_PACKAGE_DIR = "TEZ/0.4.0.2.1/package"
  STACK_VERSION = "2.1"

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/tez_client.py",
                       classname = "TezClient",
                       command = "configure",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Directory', '/etc/tez',
      mode = 0755
    )

    self.assertResourceCalled('Directory', '/etc/tez/conf',
      owner = 'tez',
      group = 'hadoop',
      recursive = True
    )

    self.assertResourceCalled('XmlConfig', 'tez-site.xml',
      owner = 'tez',
      group = 'hadoop',
      conf_dir = '/etc/tez/conf',
      configurations = self.getConfig()['configurations']['tez-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['tez-site'],
      mode = 0664
    )

    self.assertResourceCalled('File', '/etc/tez/conf/tez-env.sh',
      owner = 'tez',
      content = InlineTemplate(self.getConfig()['configurations']['tez-env']['content'])
    )

    self.assertNoMoreResources()

  @patch("resource_management.libraries.functions.get_hdp_version")
  def test_upgrade(self, get_hdp_version_mock):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/tez_client.py",
                       classname = "TezClient",
                       command = "restart",
                       config_file="client-upgrade.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)

    get_hdp_version_mock.return_value = "2.2.1.0-2067"
    self.assertResourceCalled("Execute", "hdp-select set hadoop-client 2.2.1.0-2067")

    # for now, it's enough that hdp-select is confirmed
