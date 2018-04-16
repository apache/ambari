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
import itertools
import json

from stacks.utils.RMFTestCase import *

from only_for_platform import not_for_platform, PLATFORM_WINDOWS

@not_for_platform(PLATFORM_WINDOWS)
class TestMahoutClient(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "MAHOUT/1.0.0.2.3/package"
  STACK_VERSION = "2.3"

  CONFIG_OVERRIDES = {"serviceName":"MAHOUT", "role":"MAHOUT"}

  def test_configure_default(self):
    self.executeScript(
      self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/mahout_client.py",
      classname = "MahoutClient",
      command = "configure",
      config_file = "default.json",
      stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertResourceCalled('Directory',
      '/usr/hdp/current/mahout-client/conf',
      owner = 'mahout',
      group = 'hadoop',
      create_parents = True)

    self.assertResourceCalled('XmlConfig', 'yarn-site.xml',
      owner = "yarn",
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/usr/hdp/2.2.1.0-2067/hadoop/conf',
      configurations = self.getConfig()['configurations']['yarn-site'],
      configuration_attributes = self.getConfig()['configurationAttributes']['yarn-site']
    )

    self.assertResourceCalled('File',
      '/usr/hdp/current/mahout-client/conf/log4j.properties',
      content = self.getConfig()['configurations']['mahout-log4j']['content'],
      owner = 'mahout',
      group = 'hadoop',
      mode = 0644 )

    self.assertNoMoreResources()

  def test_pre_upgrade_restart(self):
    config_file = self.get_src_folder() + "/test/python/stacks/2.2/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.2.1.0-3242'
    json_content['commandParams']['version'] = version
    self.executeScript(
      self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/mahout_client.py",
      classname = "MahoutClient",
      command = "pre_upgrade_restart",
      config_dict = json_content,
      config_overrides = self.CONFIG_OVERRIDES,
      stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertResourceCalled('Execute', ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'mahout-client', '2.2.1.0-3242'), sudo=True)
    self.assertNoMoreResources()

  def test_pre_upgrade_restart_23(self):
    config_file = self.get_src_folder() + "/test/python/stacks/2.2/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)

    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version

    # test to make sure conf_select is working correctly
    json_content['commandParams']['upgrade_direction'] = 'upgrade'
    json_content['hostLevelParams']['stack_name'] = 'HDP'
    json_content['clusterLevelParams']['stack_version'] = '2.3'

    mocks_dict = {}
    self.executeScript(
      self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/mahout_client.py",
      classname = "MahoutClient",
      command = "pre_upgrade_restart",
      config_dict = json_content,
      config_overrides = self.CONFIG_OVERRIDES,
      stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES,
      mocks_dict = mocks_dict)

    self.assertResourceCalledIgnoreEarlier('Execute', ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'mahout-client', '2.3.0.0-1234'),sudo = True)
    self.assertNoMoreResources()

    import sys

    self.assertEquals("/usr/hdp/2.3.0.0-1234/hadoop/conf",
      sys.modules["params"].hadoop_conf_dir)