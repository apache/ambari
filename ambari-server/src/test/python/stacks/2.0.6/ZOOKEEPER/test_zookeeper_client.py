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

@patch("os.path.exists", new = MagicMock(return_value=True))
@patch("platform.linux_distribution", new = MagicMock(return_value="Linux"))
class TestZookeeperClient(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "ZOOKEEPER/3.4.5.2.0/package"
  STACK_VERSION = "2.0.6"

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/zookeeper_client.py",
                       classname = "ZookeeperClient",
                       command = "configure",
                       config_file = "default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Directory', '/etc/zookeeper/conf',
      owner = 'zookeeper',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('File', '/etc/zookeeper/conf/zookeeper-env.sh',
      owner = 'zookeeper',
      content = InlineTemplate(self.getConfig()['configurations']['zookeeper-env']['content']),
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/zookeeper/conf/zoo.cfg',
      owner = 'zookeeper',
      content = Template('zoo.cfg.j2'),
      group = 'hadoop',
      mode = None,
    )
    self.assertResourceCalled('File', '/etc/zookeeper/conf/configuration.xsl',
      owner = 'zookeeper',
      content = Template('configuration.xsl.j2'),
      group = 'hadoop',
      mode = None,
    )
    self.assertResourceCalled('Directory', '/var/run/zookeeper',
      owner = 'zookeeper',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/log/zookeeper',
      owner = 'zookeeper',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/hadoop/zookeeper',
      owner = 'zookeeper',
      group = 'hadoop',
      recursive = True,
      cd_access='a'
    )
    self.assertResourceCalled('File',
                              '/etc/zookeeper/conf/log4j.properties',
                              content='log4jproperties\nline2',
                              mode=0644,
                              group='hadoop',
                              owner='zookeeper'
    )
    self.assertResourceCalled('File', '/etc/zookeeper/conf/zoo_sample.cfg',
      owner = 'zookeeper',
      group = 'hadoop',
    )
    self.assertNoMoreResources()

  def test_configure_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/zookeeper_client.py",
                       classname = "ZookeeperClient",
                       command = "configure",
                       config_file = "secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Directory', '/etc/zookeeper/conf',
      owner = 'zookeeper',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('File', '/etc/zookeeper/conf/zookeeper-env.sh',
      owner = 'zookeeper',
      content = InlineTemplate(self.getConfig()['configurations']['zookeeper-env']['content']),
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/zookeeper/conf/zoo.cfg',
      owner = 'zookeeper',
      content = Template('zoo.cfg.j2'),
      group = 'hadoop',
      mode = None,
    )
    self.assertResourceCalled('File', '/etc/zookeeper/conf/configuration.xsl',
      owner = 'zookeeper',
      content = Template('configuration.xsl.j2'),
      group = 'hadoop',
      mode = None,
    )
    self.assertResourceCalled('Directory', '/var/run/zookeeper',
      owner = 'zookeeper',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/log/zookeeper',
      owner = 'zookeeper',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/hadoop/zookeeper',
      owner = 'zookeeper',
      group = 'hadoop',
      recursive = True,
      cd_access='a'
    )
    self.assertResourceCalled('File',
                              '/etc/zookeeper/conf/log4j.properties',
                              content='log4jproperties\nline2',
                              mode=0644,
                              group='hadoop',
                              owner='zookeeper'
    )
    self.assertResourceCalled('File', '/etc/zookeeper/conf/zookeeper_client_jaas.conf',
      owner = 'zookeeper',
      content = Template('zookeeper_client_jaas.conf.j2'),
      group = 'hadoop',
      mode = None,
    )
    self.assertResourceCalled('File', '/etc/zookeeper/conf/zoo_sample.cfg',
      owner = 'zookeeper',
      group = 'hadoop',
    )
    self.assertNoMoreResources()
