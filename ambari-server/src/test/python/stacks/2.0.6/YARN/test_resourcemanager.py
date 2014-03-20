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

class TestResourceManager(RMFTestCase):

  def test_configure_default(self):
    self.executeScript("2.0.6/services/YARN/package/scripts/resourcemanager.py",
                       classname = "Resourcemanager",
                       command = "configure",
                       config_file="default.json"
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript("2.0.6/services/YARN/package/scripts/resourcemanager.py",
                       classname = "Resourcemanager",
                       command = "start",
                       config_file="default.json"
    )

    self.assert_configure_default()
    self.assertResourceCalled('Execute', 'export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop-yarn/sbin/yarn-daemon.sh --config /etc/hadoop/conf start resourcemanager',
                              not_if = 'ls /var/run/hadoop-yarn/yarn/yarn-yarn-resourcemanager.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop-yarn/yarn/yarn-yarn-resourcemanager.pid` >/dev/null 2>&1',
                              user = 'yarn'
    )
    self.assertResourceCalled('Execute', 'ls /var/run/hadoop-yarn/yarn/yarn-yarn-resourcemanager.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop-yarn/yarn/yarn-yarn-resourcemanager.pid` >/dev/null 2>&1',
                              user = 'yarn',
                              not_if = 'ls /var/run/hadoop-yarn/yarn/yarn-yarn-resourcemanager.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop-yarn/yarn/yarn-yarn-resourcemanager.pid` >/dev/null 2>&1',
                              initial_wait=5
    )
    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript("2.0.6/services/YARN/package/scripts/resourcemanager.py",
                       classname = "Resourcemanager",
                       command = "stop",
                       config_file="default.json"
    )

    self.assertResourceCalled('Execute', 'export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop-yarn/sbin/yarn-daemon.sh --config /etc/hadoop/conf stop resourcemanager',
                              user = 'yarn'
    )
    self.assertResourceCalled('Execute', 'rm -f /var/run/hadoop-yarn/yarn/yarn-yarn-resourcemanager.pid',
                              user = 'yarn'
    )
    self.assertNoMoreResources()

  def test_configure_secured(self):

    self.executeScript("2.0.6/services/YARN/package/scripts/resourcemanager.py",
                       classname = "Resourcemanager",
                       command = "configure",
                       config_file="secured.json"
    )
    self.assert_configure_secured()

  def test_start_secured(self):
    self.executeScript("2.0.6/services/YARN/package/scripts/resourcemanager.py",
                       classname = "Resourcemanager",
                       command = "start",
                       config_file="secured.json"
    )

    self.assert_configure_secured()
    self.assertResourceCalled('Execute', 'export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop-yarn/sbin/yarn-daemon.sh --config /etc/hadoop/conf start resourcemanager',
                              not_if = 'ls /var/run/hadoop-yarn/yarn/yarn-yarn-resourcemanager.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop-yarn/yarn/yarn-yarn-resourcemanager.pid` >/dev/null 2>&1',
                              user = 'yarn'
    )
    self.assertResourceCalled('Execute', 'ls /var/run/hadoop-yarn/yarn/yarn-yarn-resourcemanager.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop-yarn/yarn/yarn-yarn-resourcemanager.pid` >/dev/null 2>&1',
                              user = 'yarn',
                              not_if = 'ls /var/run/hadoop-yarn/yarn/yarn-yarn-resourcemanager.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop-yarn/yarn/yarn-yarn-resourcemanager.pid` >/dev/null 2>&1',
                              initial_wait=5
    )
    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript("2.0.6/services/YARN/package/scripts/resourcemanager.py",
                       classname = "Resourcemanager",
                       command = "stop",
                       config_file="secured.json"
    )

    self.assertResourceCalled('Execute', 'export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop-yarn/sbin/yarn-daemon.sh --config /etc/hadoop/conf stop resourcemanager',
                              user = 'yarn'
    )
    self.assertResourceCalled('Execute', 'rm -f /var/run/hadoop-yarn/yarn/yarn-yarn-resourcemanager.pid',
                              user = 'yarn'
    )
    self.assertNoMoreResources()

  def assert_configure_default(self):

    self.assertResourceCalled('Directory', '/var/run/hadoop-yarn/yarn',
      owner = 'yarn',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-yarn/yarn',
      owner = 'yarn',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop-mapreduce/mapred',
      owner = 'mapred',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-mapreduce/mapred',
      owner = 'mapred',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/hadoop/yarn/local',
      owner = 'yarn',
      recursive = True,
      ignore_failures = True,
    )
    self.assertResourceCalled('Directory', '/hadoop/yarn/local1',
      owner = 'yarn',
      recursive = True,
      ignore_failures = True,
    )
    self.assertResourceCalled('Directory', '/hadoop/yarn/log',
      owner = 'yarn',
      recursive = True,
      ignore_failures = True,
    )
    self.assertResourceCalled('Directory', '/hadoop/yarn/log1',
      owner = 'yarn',
      recursive = True,
      ignore_failures = True,
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-yarn',
      owner = 'yarn',
      recursive = True,
      ignore_failures = True,
    )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
      owner = 'hdfs',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['core-site'],
    )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['mapred-site'],
    )
    self.assertResourceCalled('XmlConfig', 'yarn-site.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['yarn-site'],
    )
    self.assertResourceCalled('XmlConfig', 'capacity-scheduler.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['capacity-scheduler'],
    )
    self.assertResourceCalled('File', '/var/log/hadoop-yarn/yarn/hadoop-mapreduce.jobsummary.log',
      owner = 'yarn',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/yarn.exclude',
      owner = 'yarn',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/yarn.conf',
      content = Template('yarn.conf.j2'),
      mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/mapreduce.conf',
      content = Template('mapreduce.conf.j2'),
      mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/yarn-env.sh',
      content = Template('yarn-env.sh.j2'),
      owner = 'yarn',
      group = 'hadoop',
      mode = 0755,
    )

  def assert_configure_secured(self):

    self.assertResourceCalled('Directory', '/var/run/hadoop-yarn/yarn',
      owner = 'yarn',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-yarn/yarn',
      owner = 'yarn',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop-mapreduce/mapred',
      owner = 'mapred',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-mapreduce/mapred',
      owner = 'mapred',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/hadoop/yarn/local',
      owner = 'yarn',
      recursive = True,
      ignore_failures = True,
    )
    self.assertResourceCalled('Directory', '/hadoop/yarn/log',
      owner = 'yarn',
      recursive = True,
      ignore_failures = True,
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-yarn',
      owner = 'yarn',
      recursive = True,
      ignore_failures = True,
    )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
      owner = 'hdfs',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['core-site'],
    )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['mapred-site'],
    )
    self.assertResourceCalled('XmlConfig', 'yarn-site.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['yarn-site'],
    )
    self.assertResourceCalled('XmlConfig', 'capacity-scheduler.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['capacity-scheduler'],
    )
    self.assertResourceCalled('File', '/var/log/hadoop-yarn/yarn/hadoop-mapreduce.jobsummary.log',
      owner = 'yarn',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/yarn.exclude',
      owner = 'yarn',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/yarn.conf',
      content = Template('yarn.conf.j2'),
      mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/mapreduce.conf',
      content = Template('mapreduce.conf.j2'),
      mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/yarn-env.sh',
      content = Template('yarn-env.sh.j2'),
      owner = 'yarn',
      group = 'hadoop',
      mode = 0755,
    )
    self.assertResourceCalled('File', '/usr/lib/hadoop-yarn/bin/container-executor',
      group = 'hadoop',
      mode = 06050,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/container-executor.cfg',
      content = Template('container-executor.cfg.j2'),
      group = 'hadoop',
      mode = 0644,
    )