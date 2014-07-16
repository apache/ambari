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
from stacks.utils.RMFTestCase import *

@patch("os.path.exists", new = MagicMock(return_value=True))
class TestZookeeperServer(RMFTestCase):

  def test_configure_default(self):
    self.executeScript("1.3.2/services/ZOOKEEPER/package/scripts/zookeeper_server.py",
                   classname = "ZookeeperServer",
                   command = "configure",
                   config_file="default.json"
    )

    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript("1.3.2/services/ZOOKEEPER/package/scripts/zookeeper_server.py",
                   classname = "ZookeeperServer",
                   command = "start",
                   config_file="default.json"
    )

    self.assert_configure_default()
    self.assertResourceCalled('Execute', 'source /etc/zookeeper/conf/zookeeper-env.sh ; env ZOOCFGDIR=/etc/zookeeper/conf ZOOCFG=zoo.cfg /usr/lib/zookeeper/bin/zkServer.sh start',
                    not_if = 'ls /var/run/zookeeper/zookeeper_server.pid >/dev/null 2>&1 && ps `cat /var/run/zookeeper/zookeeper_server.pid` >/dev/null 2>&1',
                    user = 'zookeeper'
    )
    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript("1.3.2/services/ZOOKEEPER/package/scripts/zookeeper_server.py",
                  classname = "ZookeeperServer",
                  command = "stop",
                  config_file="default.json"
    )

    self.assertResourceCalled('Execute', 'source /etc/zookeeper/conf/zookeeper-env.sh ; env ZOOCFGDIR=/etc/zookeeper/conf ZOOCFG=zoo.cfg /usr/lib/zookeeper/bin/zkServer.sh stop',
      user = 'zookeeper',
    )
    self.assertResourceCalled('Execute', 'rm -f /var/run/zookeeper/zookeeper_server.pid')
    self.assertNoMoreResources()

  def test_configure_secured(self):
    self.executeScript("1.3.2/services/ZOOKEEPER/package/scripts/zookeeper_server.py",
                  classname = "ZookeeperServer",
                  command = "configure",
                  config_file="secured.json"
    )

    self.assert_configure_secured()
    self.assertNoMoreResources()

  def test_start_secured(self):
    self.executeScript("1.3.2/services/ZOOKEEPER/package/scripts/zookeeper_server.py",
                  classname = "ZookeeperServer",
                  command = "start",
                  config_file="secured.json"
    )

    self.assert_configure_secured()
    self.assertResourceCalled('Execute', 'source /etc/zookeeper/conf/zookeeper-env.sh ; env ZOOCFGDIR=/etc/zookeeper/conf ZOOCFG=zoo.cfg /usr/lib/zookeeper/bin/zkServer.sh start',
                  not_if = 'ls /var/run/zookeeper/zookeeper_server.pid >/dev/null 2>&1 && ps `cat /var/run/zookeeper/zookeeper_server.pid` >/dev/null 2>&1',
                  user = 'zookeeper'
    )
    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript("1.3.2/services/ZOOKEEPER/package/scripts/zookeeper_server.py",
                  classname = "ZookeeperServer",
                  command = "stop",
                  config_file="secured.json"
    )

    self.assertResourceCalled('Execute', 'source /etc/zookeeper/conf/zookeeper-env.sh ; env ZOOCFGDIR=/etc/zookeeper/conf ZOOCFG=zoo.cfg /usr/lib/zookeeper/bin/zkServer.sh stop',
                  user = 'zookeeper',
    )

    self.assertResourceCalled('Execute', 'rm -f /var/run/zookeeper/zookeeper_server.pid')
    self.assertNoMoreResources()

  def assert_configure_default(self):

    self.assertResourceCalled('Directory', '/etc/zookeeper/conf',
      owner = 'zookeeper',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('File', '/etc/zookeeper/conf/zoo.cfg',
      owner = 'zookeeper',
      content = Template('zoo.cfg.j2'),
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/zookeeper/conf/zookeeper-env.sh',
      owner = 'zookeeper',
      content = InlineTemplate(self.getConfig()['configurations']['zookeeper-env']['content']),
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/zookeeper/conf/configuration.xsl',
      owner = 'zookeeper',
      content = Template('configuration.xsl.j2'),
      group = 'hadoop',
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
    )
    self.assertResourceCalled('File', '/hadoop/zookeeper/myid',
      content = '1',
      mode = 0644,
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

  def assert_configure_secured(self):

    self.assertResourceCalled('Directory', '/etc/zookeeper/conf',
      owner = 'zookeeper',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('File', '/etc/zookeeper/conf/zoo.cfg',
      owner = 'zookeeper',
      content = Template('zoo.cfg.j2'),
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/zookeeper/conf/zookeeper-env.sh',
      owner = 'zookeeper',
      content = InlineTemplate(self.getConfig()['configurations']['zookeeper-env']['content']),
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/zookeeper/conf/configuration.xsl',
      owner = 'zookeeper',
      content = Template('configuration.xsl.j2'),
      group = 'hadoop',
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
    )
    self.assertResourceCalled('File', '/hadoop/zookeeper/myid',
      content = '1',
      mode = 0644,
    )
    self.assertResourceCalled('File',
                              '/etc/zookeeper/conf/log4j.properties',
                              content='log4jproperties\nline2',
                              mode=0644,
                              group='hadoop',
                              owner='zookeeper'
    )
    self.assertResourceCalled('File', '/etc/zookeeper/conf/zookeeper_jaas.conf',
      owner = 'zookeeper',
      content = Template('zookeeper_jaas.conf.j2'),
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/zookeeper/conf/zookeeper_client_jaas.conf',
      owner = 'zookeeper',
      content = Template('zookeeper_client_jaas.conf.j2'),
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/zookeeper/conf/zoo_sample.cfg',
      owner = 'zookeeper',
      group = 'hadoop',
    )