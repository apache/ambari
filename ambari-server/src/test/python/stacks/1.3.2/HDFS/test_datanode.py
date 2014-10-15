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
from ambari_commons import OSCheck
from mock.mock import MagicMock, patch
from stacks.utils.RMFTestCase import *

class TestDatanode(RMFTestCase):

  def test_configure_default(self):
    self.executeScript("1.3.2/services/HDFS/package/scripts/datanode.py",
                       classname = "DataNode",
                       command = "configure",
                       config_file="default.json"
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript("1.3.2/services/HDFS/package/scripts/datanode.py",
                       classname = "DataNode",
                       command = "start",
                       config_file="default.json"
    )
    self.assert_configure_default()
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid',
                              action = ['delete'],
                              not_if='ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
                              )
    self.assertResourceCalled('Execute', 'ulimit -c unlimited;  su -s /bin/bash - hdfs -c \'export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop/bin/hadoop-daemon.sh --config /etc/hadoop/conf start datanode\'',
                              not_if = 'ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
                              )
    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript("1.3.2/services/HDFS/package/scripts/datanode.py",
                       classname = "DataNode",
                       command = "stop",
                       config_file="default.json"
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid',
                              action = ['delete'],
                              not_if='ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
                              )
    self.assertResourceCalled('Execute', 'ulimit -c unlimited;  su -s /bin/bash - hdfs -c \'export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop/bin/hadoop-daemon.sh --config /etc/hadoop/conf stop datanode\'',
                              not_if = None,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid',
                              action = ['delete'],
                              )
    self.assertNoMoreResources()

  def test_configure_secured(self):
    self.executeScript("1.3.2/services/HDFS/package/scripts/datanode.py",
                       classname = "DataNode",
                       command = "configure",
                       config_file="secured.json"
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()

  def test_start_secured(self):
    self.executeScript("1.3.2/services/HDFS/package/scripts/datanode.py",
                       classname = "DataNode",
                       command = "start",
                       config_file="secured.json"
    )
    self.assert_configure_secured()
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid',
                              action = ['delete'],
                              not_if='ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
                              )
    self.assertResourceCalled('Execute', 'ulimit -c unlimited;  su -s /bin/bash - root -c \'export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop/bin/hadoop-daemon.sh --config /etc/hadoop/conf start datanode\'',
                              not_if = 'ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
                              )
    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript("1.3.2/services/HDFS/package/scripts/datanode.py",
                       classname = "DataNode",
                       command = "stop",
                       config_file="secured.json"
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid',
                              action = ['delete'],
                              not_if='ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
                              )
    self.assertResourceCalled('Execute', 'ulimit -c unlimited;  su -s /bin/bash - root -c \'export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop/bin/hadoop-daemon.sh --config /etc/hadoop/conf stop datanode\'',
                              not_if = None,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid',
                              action = ['delete'],
                              )
    self.assertNoMoreResources()
  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
                              owner = 'root',
                              group = 'root',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/etc/security/limits.d/hdfs.conf',
                              content = Template('hdfs.conf.j2'),
                              owner = 'root',
                              group = 'root',
                              mode = 0644,
                              )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['hdfs-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['hdfs-site']
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/slaves',
                              content = Template('slaves.j2'),
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('Directory', '/var/lib/hadoop-hdfs',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0751,
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/hadoop/hdfs',
                              ignore_failures = True,
                              mode = 0755,
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/hadoop/hdfs/data',
                              owner = 'hdfs',
                              ignore_failures = True,
                              group = 'hadoop',
                              mode = 0750,
                              recursive = False,
                              )

  def assert_configure_secured(self):
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
                              owner = 'root',
                              group = 'root',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/etc/security/limits.d/hdfs.conf',
                              content = Template('hdfs.conf.j2'),
                              owner = 'root',
                              group = 'root',
                              mode = 0644,
                              )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['hdfs-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['hdfs-site']
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/slaves',
                              content = Template('slaves.j2'),
                              owner = 'root',
                              )
    self.assertResourceCalled('Directory', '/var/lib/hadoop-hdfs',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0751,
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/hadoop/hdfs',
                              mode = 0755,
                              recursive = True,
                              ignore_failures=True,
                              )
    self.assertResourceCalled('Directory', '/hadoop/hdfs/data',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0750,
                              recursive = False,
                              ignore_failures=True,
                              )
