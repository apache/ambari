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
from mock.mock import MagicMock, patch
import tempfile
import tarfile
import contextlib
from resource_management import *
from stacks.utils.RMFTestCase import *


@patch.object(tarfile,"open", new = MagicMock())
@patch.object(tempfile,"mkdtemp", new = MagicMock(return_value='/tmp/123'))
@patch.object(contextlib,"closing", new = MagicMock())
@patch("os.path.exists", new = MagicMock(return_value=True))
class Test(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "HDFS/2.1.0.2.0/package"
  STACK_VERSION = "2.0.6"

  def test_generate_configs_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hdfs_client.py",
                       classname = "HdfsClient",
                       command = "generate_configs",
                       config_file = "default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Directory', '/tmp',
                              recursive = True,
                              )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
                              conf_dir = '/tmp/123',
                              configuration_attributes = self.getConfig()['configuration_attributes']['hdfs-site'],
                              configurations = self.getConfig()['configurations']['hdfs-site'],
                              )
    self.assertResourceCalled('File', '/tmp/123/hadoop-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['hadoop-env']['content']),
                              )
    self.assertResourceCalled('File', '/tmp/123/log4j.properties',
                              content = InlineTemplate(self.getConfig()['configurations']['hdfs-log4j']['content']+
                                                       self.getConfig()['configurations']['yarn-log4j']['content']),
                              )
    self.assertResourceCalled('PropertiesFile', '/tmp/123/runtime.properties',
                              properties = UnknownConfigurationMock(),
    )
    self.assertResourceCalled('PropertiesFile', '/tmp/123/startup.properties',
                              properties = UnknownConfigurationMock(),
    )
    self.assertResourceCalled('Directory', '/tmp/123',
                              action = ['delete'],
                              )
    self.assertNoMoreResources()

  def test_upgrade(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hdfs_client.py",
                   classname = "HdfsClient",
                   command = "restart",
                   config_file="client-upgrade.json",
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertResourceCalled("Execute", "hdp-select set hadoop-client 2.2.1.0-2067")

    # for now, it's enough that hdp-select is confirmed
