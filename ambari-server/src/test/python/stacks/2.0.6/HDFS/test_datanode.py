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


class TestDatanode(RMFTestCase):

  def test_configure_default(self):
    self.executeScript("2.0.6/services/HDFS/package/scripts/datanode.py",
                       classname = "DataNode",
                       command = "configure",
                       config_file="default.json"
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript("2.0.6/services/HDFS/package/scripts/datanode.py",
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
                              ignore_failures = True,
                              not_if='ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
                              )
    self.assertResourceCalled('Execute', 'ulimit -c unlimited;  export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf start datanode',
                              not_if = 'ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
                              user = 'hdfs',
                              )
    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript("2.0.6/services/HDFS/package/scripts/datanode.py",
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
                              ignore_failures = True,
                              not_if='ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
                              )
    self.assertResourceCalled('Execute', 'ulimit -c unlimited;  export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf stop datanode',
                              not_if = None,
                              user = 'hdfs',
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid',
                              action = ['delete'],
                              ignore_failures = True,
                              )
    self.assertNoMoreResources()

  def test_configure_secured(self):
    self.executeScript("2.0.6/services/HDFS/package/scripts/datanode.py",
                       classname = "DataNode",
                       command = "configure",
                       config_file="secured.json"
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()

  def test_start_secured(self):
    self.executeScript("2.0.6/services/HDFS/package/scripts/datanode.py",
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
                              ignore_failures = True,
                              not_if='ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
                              )
    self.assertResourceCalled('Execute', 'ulimit -c unlimited;  export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf start datanode',
                              not_if = 'ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
                              user = 'root',
                              )
    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript("2.0.6/services/HDFS/package/scripts/datanode.py",
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
                              ignore_failures = True,
                              not_if='ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
                              )
    self.assertResourceCalled('Execute', 'ulimit -c unlimited;  export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf stop datanode',
                              not_if = None,
                              user = 'root',
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid',
                              action = ['delete'],
                              ignore_failures = True,
                              )
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/var/lib/hadoop-hdfs',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0751,
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/hadoop/hdfs/data',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755,
                              recursive = True,
                              ignore_failures=True,
                              )

  def assert_configure_secured(self):
    self.assertResourceCalled('Directory', '/var/lib/hadoop-hdfs',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0751,
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/hadoop/hdfs/data',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755,
                              recursive = True,
                              ignore_failures=True,
                              )
