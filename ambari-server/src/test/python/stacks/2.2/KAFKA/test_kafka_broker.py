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
from stacks.utils.RMFTestCase import *
from mock.mock import patch

from only_for_platform import not_for_platform, PLATFORM_WINDOWS

@not_for_platform(PLATFORM_WINDOWS)
class TestKafkaBroker(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "KAFKA/0.8.1/package"
  STACK_VERSION = "2.2"

  CONFIG_OVERRIDES = {"serviceName":"KAFKA", "role":"KAFKA_BROKER"}

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/kafka_broker.py",
                         classname = "KafkaBroker",
                         command = "configure",
                         config_file="default.json",
                         stack_version = self.STACK_VERSION,
                         target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Directory', '/var/log/kafka',
                              owner = 'kafka',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0o755,
                              cd_access = 'a',
                              recursive_ownership = True,
    )

    self.assertResourceCalled('Directory', '/var/run/kafka',
                              owner = 'kafka',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0o755,
                              cd_access = 'a',
                              recursive_ownership = True,
    )

    self.assertResourceCalled('Directory', '/usr/hdp/current/kafka-broker/config',
                              owner = 'kafka',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0o755,
                              cd_access = 'a',
                              recursive_ownership = True,
    )
    self.assertResourceCalled('Directory', '/tmp/log/dir',
                              owner = 'kafka',
                              create_parents = True,
                              group = 'hadoop',
                              mode = 0o755,
                              cd_access = 'a',
                              recursive_ownership = True,
    )


  @patch("os.path.islink")
  @patch("os.path.realpath")
  def test_configure_custom_paths_default(self, realpath_mock, islink_mock):

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/kafka_broker.py",
                       classname = "KafkaBroker",
                       command = "configure",
                       config_file="default_custom_path_config.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Directory', '/customdisk/var/log/kafka',
                              owner = 'kafka',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0o755,
                              cd_access = 'a',
                              recursive_ownership = True,
    )

    self.assertResourceCalled('Directory', '/customdisk/var/run/kafka',
                              owner = 'kafka',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0o755,
                              cd_access = 'a',
                              recursive_ownership = True,
    )

    self.assertResourceCalled('Directory', '/usr/hdp/current/kafka-broker/config',
                              owner = 'kafka',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0o755,
                              cd_access = 'a',
                              recursive_ownership = True,
    )

    self.assertResourceCalled('Directory', '/tmp/log/dir',
                              owner = 'kafka',
                              create_parents = True,
                              group = 'hadoop',
                              mode = 0o755,
                              cd_access = 'a',
                              recursive_ownership = True,
    )

    self.assertTrue(islink_mock.called)
    self.assertTrue(realpath_mock.called)

  def test_pre_upgrade_restart(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.2/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.2.1.0-3242'
    json_content['commandParams']['version'] = version
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/kafka_broker.py",
                       classname = "KafkaBroker",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       config_overrides = self.CONFIG_OVERRIDES,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)
    self.assertResourceCalled('Execute',
                              ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'kafka-broker', version), sudo=True,)
    self.assertNoMoreResources()

  @patch("resource_management.core.shell.call")
  def test_pre_upgrade_restart_23(self, call_mock):
    config_file = self.get_src_folder()+"/test/python/stacks/2.2/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/kafka_broker.py",
                       classname = "KafkaBroker",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       config_overrides = self.CONFIG_OVERRIDES,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       mocks_dict = mocks_dict)

    self.assertResourceCalledIgnoreEarlier('Execute',
                              ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'kafka-broker', version), sudo=True,)

    self.assertNoMoreResources()
