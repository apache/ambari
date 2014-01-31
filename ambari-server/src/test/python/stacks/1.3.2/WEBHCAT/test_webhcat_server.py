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

class TestWebHCatServer(RMFTestCase):

  def test_configure_default(self):
    self.executeScript("1.3.2/services/WEBHCAT/package/scripts/webhcat_server.py",
                       classname = "WebHCatServer",
                       command = "configure",
                       config_file="default.json"
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript("1.3.2/services/WEBHCAT/package/scripts/webhcat_server.py",
                       classname = "WebHCatServer",
                       command = "start",
                       config_file="default.json"
    )

    self.assert_configure_default()
    self.assertResourceCalled('Execute', 'env HADOOP_HOME=/usr /usr/lib/hcatalog/sbin/webhcat_server.sh start',
                              not_if = 'ls /etc/run/webhcat/webhcat.pid >/dev/null 2>&1 && ps `cat /etc/run/webhcat/webhcat.pid` >/dev/null 2>&1',
                              user = 'hcat'
    )
    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript("1.3.2/services/WEBHCAT/package/scripts/webhcat_server.py",
                       classname = "WebHCatServer",
                       command = "stop",
                       config_file="default.json"
    )

    self.assertResourceCalled('Execute', 'env HADOOP_HOME=/usr /usr/lib/hcatalog/sbin/webhcat_server.sh stop',
                              user = 'hcat',
                              )
    self.assertResourceCalled('Execute', 'rm -f /etc/run/webhcat/webhcat.pid')
    self.assertNoMoreResources()

    def test_configure_secured(self):
      self.executeScript("1.3.2/services/WEBHCAT/package/scripts/webhcat_server.py",
                         classname = "WebHCatServer",
                         command = "configure",
                         config_file="secured.json"
      )

      self.assert_configure_secured()
      self.assertNoMoreResources()

  def test_start_secured(self):
    self.executeScript("1.3.2/services/WEBHCAT/package/scripts/webhcat_server.py",
                       classname = "WebHCatServer",
                       command = "start",
                       config_file="secured.json"
    )

    self.assert_configure_secured()
    self.assertResourceCalled('Execute', 'env HADOOP_HOME=/usr /usr/lib/hcatalog/sbin/webhcat_server.sh start',
                              not_if = 'ls /etc/run/webhcat/webhcat.pid >/dev/null 2>&1 && ps `cat /etc/run/webhcat/webhcat.pid` >/dev/null 2>&1',
                              user = 'hcat'
    )
    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript("1.3.2/services/WEBHCAT/package/scripts/webhcat_server.py",
                       classname = "WebHCatServer",
                       command = "stop",
                       config_file="secured.json"
    )

    self.assertResourceCalled('Execute', 'env HADOOP_HOME=/usr /usr/lib/hcatalog/sbin/webhcat_server.sh stop',
                              user = 'hcat',
                              )
    self.assertResourceCalled('Execute', 'rm -f /etc/run/webhcat/webhcat.pid')
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/etc/run/webhcat',
      owner = 'hcat',
      group = 'hadoop',
      recursive = True,
      mode = 493,
    )
    self.assertResourceCalled('Directory', '/var/log/webhcat',
      owner = 'hcat',
      group = 'hadoop',
      recursive = True,
      mode = 493,
    )
    self.assertResourceCalled('Directory', '/etc/hcatalog/conf',
      owner = 'hcat',
      group = 'hadoop',
    )
    self.assertResourceCalled('XmlConfig', 'webhcat-site.xml',
      owner = 'hcat',
      group = 'hadoop',
      conf_dir = '/etc/hcatalog/conf',
      configurations = self.getConfig()['configurations']['webhcat-site'], # don't hardcode all the properties
    )
    self.assertResourceCalled('File', '/etc/hcatalog/conf/webhcat-env.sh',
      content = Template('webhcat-env.sh.j2'),
      owner = 'hcat',
      group = 'hadoop',
    )
    self.assertResourceCalled('ExecuteHadoop', 'fs -copyFromLocal /usr/lib/hadoop/contrib/streaming/hadoop-streaming*.jar /apps/webhcat/hadoop-streaming.jar',
      not_if = ' hadoop fs -ls /apps/webhcat/hadoop-streaming.jar >/dev/null 2>&1',
      user = 'hcat',
      conf_dir = '/etc/hadoop/conf',
    )
    self.assertResourceCalled('ExecuteHadoop', 'fs -copyFromLocal /usr/share/HDP-webhcat/pig.tar.gz /apps/webhcat/pig.tar.gz',
      not_if = ' hadoop fs -ls /apps/webhcat/pig.tar.gz >/dev/null 2>&1',
      user = 'hcat',
      conf_dir = '/etc/hadoop/conf',
    )
    self.assertResourceCalled('ExecuteHadoop', 'fs -copyFromLocal /usr/share/HDP-webhcat/hive.tar.gz /apps/webhcat/hive.tar.gz',
      not_if = ' hadoop fs -ls /apps/webhcat/hive.tar.gz >/dev/null 2>&1',
      user = 'hcat',
      conf_dir = '/etc/hadoop/conf',
    )

  def assert_configure_secured(self):
    self.assertResourceCalled('Directory', '/etc/run/webhcat',
      owner = 'hcat',
      group = 'hadoop',
      recursive = True,
      mode = 493,
    )
    self.assertResourceCalled('Directory', '/var/log/webhcat',
      owner = 'hcat',
      group = 'hadoop',
      recursive = True,
      mode = 493,
    )
    self.assertResourceCalled('Directory', '/etc/hcatalog/conf',
      owner = 'hcat',
      group = 'hadoop',
    )
    self.assertResourceCalled('XmlConfig', 'webhcat-site.xml',
      owner = 'hcat',
      group = 'hadoop',
      conf_dir = '/etc/hcatalog/conf',
      configurations = self.getConfig()['configurations']['webhcat-site'], # don't hardcode all the properties
    )
    self.assertResourceCalled('File', '/etc/hcatalog/conf/webhcat-env.sh',
      content = Template('webhcat-env.sh.j2'),
      owner = 'hcat',
      group = 'hadoop',
    )
    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/smokeuser.headless.keytab ambari-qa;',
      path = ['/bin'],
      user = 'hcat',
    )
    self.assertResourceCalled('ExecuteHadoop', 'fs -copyFromLocal /usr/lib/hadoop/contrib/streaming/hadoop-streaming*.jar /apps/webhcat/hadoop-streaming.jar',
      not_if = '/usr/bin/kinit -kt /etc/security/keytabs/smokeuser.headless.keytab ambari-qa; hadoop fs -ls /apps/webhcat/hadoop-streaming.jar >/dev/null 2>&1',
      user = 'hcat',
      conf_dir = '/etc/hadoop/conf',
    )
    self.assertResourceCalled('ExecuteHadoop', 'fs -copyFromLocal /usr/share/HDP-webhcat/pig.tar.gz /apps/webhcat/pig.tar.gz',
      not_if = ' hadoop fs -ls /apps/webhcat/pig.tar.gz >/dev/null 2>&1',
      user = 'hcat',
      conf_dir = '/etc/hadoop/conf',
    )
    self.assertResourceCalled('ExecuteHadoop', 'fs -copyFromLocal /usr/share/HDP-webhcat/hive.tar.gz /apps/webhcat/hive.tar.gz',
      not_if = ' hadoop fs -ls /apps/webhcat/hive.tar.gz >/dev/null 2>&1',
      user = 'hcat',
      conf_dir = '/etc/hadoop/conf',
    )
