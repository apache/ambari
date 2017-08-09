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

class TestHcatClient(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "HIVE/0.12.0.2.0/package"
  STACK_VERSION = "2.0.6"

  CONFIG_OVERRIDES = {"serviceName":"HIVE", "role":"HCAT"}

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hcat_client.py",
                       classname = "HCatClient",
                       command = "configure",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Directory', '/usr/hdp/current/hive-server2/conf',
                              owner = 'hive',
                              group = 'hadoop',
                              create_parents = True,
    )
    self.assertResourceCalled('Directory', '/etc/hive-hcatalog/conf',
      owner = 'hcat',
      group = 'hadoop',
      create_parents = True,
    )
    self.assertResourceCalled('Directory', '/var/run/webhcat',
      owner = 'hcat',
      create_parents = True,
    )
    self.assertResourceCalled('XmlConfig', 'hive-site.xml',
      owner = 'hive',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/usr/hdp/current/hive-server2/conf',
      configurations = self.getConfig()['configurations']['hive-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['hive-site']
    )
    self.assertResourceCalled('File', '/etc/hive-hcatalog/conf/hcat-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['hcat-env']['content']),
                              owner = 'hcat',
                              group = 'hadoop',
                              )
    self.assertNoMoreResources()



  def test_configure_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hcat_client.py",
                         classname = "HCatClient",
                         command = "configure",
                         config_file="secured.json",
                         stack_version = self.STACK_VERSION,
                         target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Directory', '/usr/hdp/current/hive-server2/conf',
                              create_parents = True,
                              owner = 'hive',
                              group = 'hadoop',
    )
    self.assertResourceCalled('Directory', '/etc/hive-hcatalog/conf',
      create_parents = True,
      owner = 'hcat',
      group = 'hadoop',
    )
    self.assertResourceCalled('Directory', '/var/run/webhcat',
      owner = 'hcat',
      create_parents = True,
    )
    self.assertResourceCalled('XmlConfig', 'hive-site.xml',
      owner = 'hive',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/usr/hdp/current/hive-server2/conf',
      configurations = self.getConfig()['configurations']['hive-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['hive-site']
    )
    self.assertResourceCalled('File', '/etc/hive-hcatalog/conf/hcat-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['hcat-env']['content']),
                              owner = 'hcat',
                              group = 'hadoop',
                              )

    self.assertNoMoreResources()


  @patch("resource_management.core.shell.call")
  def test_pre_upgrade_restart(self, call_mock):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)

    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hcat_client.py",
      classname = "HCatClient",
      command = "pre_upgrade_restart",
      config_dict = json_content,
      config_overrides = self.CONFIG_OVERRIDES,
      stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES,
      call_mocks = [(0, None, ''), (0, None, ''), (0, None, ''), (0, None, '')],
      mocks_dict = mocks_dict)

    self.assertResourceCalled('Execute',('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'hive-webhcat', version), sudo=True,)
    self.assertNoMoreResources()