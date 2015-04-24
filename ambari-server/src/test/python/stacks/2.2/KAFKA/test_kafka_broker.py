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

class TestKafkaBroker(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "KAFKA/0.8.1.2.2/package"
  STACK_VERSION = "2.2"

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/kafka_broker.py",
                         classname = "KafkaBroker",
                         command = "configure",
                         config_file="default.json",
                         hdp_stack_version = self.STACK_VERSION,
                         target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Directory', '/var/log/kafka',
                              owner = 'kafka',
                              group = 'hadoop',
                              recursive = True,
                              mode = 0755,
                              cd_access = 'a'
    )

    self.assertResourceCalled('Directory', '/var/run/kafka',
                              owner = 'kafka',
                              group = 'hadoop',
                              recursive = True,
                              mode = 0755,
                              cd_access = 'a'
    )

    self.assertResourceCalled('Directory', '/etc/kafka/conf',
                              owner = 'kafka',
                              group = 'hadoop',
                              recursive = True,
                              mode = 0755,
                              cd_access = 'a'
    )

  @patch("os.path.islink")
  @patch("os.path.realpath")
  def test_configure_custom_paths_default(self, realpath_mock, islink_mock):

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/kafka_broker.py",
                       classname = "KafkaBroker",
                       command = "configure",
                       config_file="default_custom_path_config.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Directory', '/customdisk/var/log/kafka',
                              owner = 'kafka',
                              group = 'hadoop',
                              recursive = True,
                              mode = 0755,
                              cd_access = 'a'
    )

    self.assertResourceCalled('Directory', '/customdisk/var/run/kafka',
                              owner = 'kafka',
                              group = 'hadoop',
                              recursive = True,
                              mode = 0755,
                              cd_access = 'a'
    )

    self.assertResourceCalled('Directory', '/etc/kafka/conf',
                              owner = 'kafka',
                              group = 'hadoop',
                              recursive = True,
                              mode = 0755,
                              cd_access = 'a'
    )

    self.assertTrue(islink_mock.called)
    self.assertTrue(realpath_mock.called)

  def test_pre_rolling_restart(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.2/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.2.1.0-3242'
    json_content['commandParams']['version'] = version
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/kafka_broker.py",
                       classname = "KafkaBroker",
                       command = "pre_rolling_restart",
                       config_dict = json_content,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)
    self.assertResourceCalled('Execute',
                              'hdp-select set kafka-broker %s' % version,)
    self.assertNoMoreResources()