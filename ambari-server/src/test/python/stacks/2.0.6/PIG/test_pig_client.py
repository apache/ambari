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

from stacks.utils.RMFTestCase import *
import json

class TestPigClient(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "PIG/0.12.0.2.0/package"
  STACK_VERSION = "2.0.6"

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/pig_client.py",
                       classname = "PigClient",
                       command = "configure",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Directory', '/etc/pig/conf',
      create_parents = True,
      owner = 'hdfs',
      group = 'hadoop'
    )
    self.assertResourceCalled('File', '/etc/pig/conf/pig-env.sh',
      owner = 'hdfs',
      mode=0755,
      content = InlineTemplate(self.getConfig()['configurations']['pig-env']['content'])
    )
    self.assertResourceCalled('File', '/etc/pig/conf/pig.properties',
      owner = 'hdfs',
      group = 'hadoop',
      mode = 0644,
      content = 'pigproperties\nline2'
    )
    self.assertResourceCalled('File', '/etc/pig/conf/log4j.properties',
      owner = 'hdfs',
      group = 'hadoop',
      mode = 0644,
      content = 'log4jproperties\nline2'
    )
    self.assertNoMoreResources()



  def test_configure_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/pig_client.py",
                       classname = "PigClient",
                       command = "configure",
                       config_file="secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    
    self.assertResourceCalled('Directory', '/etc/pig/conf',
      create_parents = True,
      owner = 'hdfs',
      group = 'hadoop'
    )
    self.assertResourceCalled('File', '/etc/pig/conf/pig-env.sh',
      owner = 'hdfs',
      mode=0755,
      content = InlineTemplate(self.getConfig()['configurations']['pig-env']['content'])
    )
    self.assertResourceCalled('File', '/etc/pig/conf/pig.properties',
      owner = 'hdfs',
      group = 'hadoop',
      mode = 0644,
      content = 'pigproperties\nline2'
    )
    self.assertResourceCalled('File', '/etc/pig/conf/log4j.properties',
      owner = 'hdfs',
      group = 'hadoop',
      mode = 0644,
      content = 'log4jproperties\nline2'
    )
    self.assertNoMoreResources()

  def test_configure_default_hdp22(self):

    config_file = "stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      default_json = json.load(f)

    default_json['hostLevelParams']['stack_version'] = '2.2'

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/pig_client.py",
                       classname = "PigClient",
                       command = "configure",
                       config_dict=default_json,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Directory', '/usr/hdp/current/pig-client/conf',
                              create_parents = True,
                              owner = 'hdfs',
                              group = 'hadoop'
    )
    self.assertResourceCalled('File', '/usr/hdp/current/pig-client/conf/pig-env.sh',
                              owner = 'hdfs',
                              mode=0755,
                              content = InlineTemplate(self.getConfig()['configurations']['pig-env']['content'])
    )
    self.assertResourceCalled('File', '/usr/hdp/current/pig-client/conf/pig.properties',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0644,
                              content = 'pigproperties\nline2'
    )
    self.assertResourceCalled('File', '/usr/hdp/current/pig-client/conf/log4j.properties',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0644,
                              content = 'log4jproperties\nline2'
    )
    self.assertNoMoreResources()

  def test_pre_upgrade_restart(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.2.1.0-3242'
    json_content['commandParams']['version'] = version
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/pig_client.py",
                       classname = "PigClient",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)
    self.assertResourceCalled('Execute',
                              ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'hadoop-client', version), sudo=True)
    self.assertNoMoreResources()

  def test_pre_upgrade_restart_23(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/pig_client.py",
                       classname = "PigClient",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = [(0, None, ''), (0, None, '')],
                       mocks_dict = mocks_dict)

    self.assertResourceCalledIgnoreEarlier('Execute',
                              ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'hadoop-client', version), sudo=True)
    self.assertNoMoreResources()

    self.assertEquals(2, mocks_dict['call'].call_count)
    self.assertEquals(2, mocks_dict['checked_call'].call_count)
    self.assertEquals(
      ('ambari-python-wrap', '/usr/bin/conf-select', 'set-conf-dir', '--package', 'pig', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['checked_call'].call_args_list[0][0][0])
    self.assertEquals(
      ('ambari-python-wrap', '/usr/bin/conf-select', 'set-conf-dir', '--package', 'hadoop', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['checked_call'].call_args_list[1][0][0])
    self.assertEquals(
      ('ambari-python-wrap', '/usr/bin/conf-select', 'create-conf-dir', '--package', 'pig', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['call'].call_args_list[0][0][0])
    self.assertEquals(
      ('ambari-python-wrap', '/usr/bin/conf-select', 'create-conf-dir', '--package', 'hadoop', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['call'].call_args_list[1][0][0])
