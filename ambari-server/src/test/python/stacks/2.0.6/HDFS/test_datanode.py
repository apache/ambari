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
import json
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
                              not_if='ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
                              )
    self.assertResourceCalled('Execute', 'ulimit -c unlimited;  su -s /bin/bash - hdfs -c \'export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf start datanode\'',
                              not_if = 'ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
                              )
    self.assertNoMoreResources()

  @patch("os.path.exists", new = MagicMock(return_value=False))
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
                              not_if='ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
                              )
    self.assertResourceCalled('Execute', 'ulimit -c unlimited;  su -s /bin/bash - hdfs -c \'export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf stop datanode\'',
                              not_if = None,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid',
                              action = ['delete'],
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
                              not_if='ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
                              )
    self.assertResourceCalled('Execute', 'ulimit -c unlimited;  su -s /bin/bash - root -c \'export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf start datanode\'',
                              not_if = 'ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
                              )
    self.assertNoMoreResources()

  def test_start_secured_HDP22_root(self):
    config_file = "stacks/2.0.6/configs/secured.json"
    with open(config_file, "r") as f:
      secured_json = json.load(f)

    secured_json['hostLevelParams']['stack_version']= '2.2'

    self.executeScript("2.0.6/services/HDFS/package/scripts/datanode.py",
                       classname = "DataNode",
                       command = "start",
                       config_dict = secured_json
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
    self.assertResourceCalled('Execute', 'ulimit -c unlimited;  su -s /bin/bash - root -c \'export HADOOP_LIBEXEC_DIR=/usr/hdp/current/hadoop-client/libexec && /usr/hdp/current/hadoop-client/sbin/hadoop-daemon.sh --config /etc/hadoop/conf start datanode\'',
                              not_if = 'ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
                              )
    self.assertNoMoreResources()

  def test_start_secured_HDP22_non_root_https_only(self):
    config_file="stacks/2.0.6/configs/secured.json"
    with open(config_file, "r") as f:
      secured_json = json.load(f)

    secured_json['hostLevelParams']['stack_version']= '2.2'
    secured_json['configurations']['hdfs-site']['dfs.http.policy']= 'HTTPS_ONLY'
    secured_json['configurations']['hdfs-site']['dfs.datanode.address']= '0.0.0.0:10000'
    secured_json['configurations']['hdfs-site']['dfs.datanode.https.address']= '0.0.0.0:50000'

    self.executeScript("2.0.6/services/HDFS/package/scripts/datanode.py",
                       classname = "DataNode",
                       command = "start",
                       config_dict = secured_json
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
    self.assertResourceCalled('Execute', 'ulimit -c unlimited;  su -s /bin/bash - hdfs -c \'export HADOOP_LIBEXEC_DIR=/usr/hdp/current/hadoop-client/libexec && /usr/hdp/current/hadoop-client/sbin/hadoop-daemon.sh --config /etc/hadoop/conf start datanode\'',
                              not_if = 'ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
                              )
    self.assertNoMoreResources()

  @patch("os.path.exists", new = MagicMock(return_value=False))
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
                              not_if='ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
                              )
    self.assertResourceCalled('Execute', 'ulimit -c unlimited;  su -s /bin/bash - root -c \'export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf stop datanode\'',
                              not_if = None,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid',
                              action = ['delete'],
                              )
    self.assertNoMoreResources()


  @patch("os.path.exists", new = MagicMock(return_value=False))
  def test_stop_secured_HDP22_root(self):
    config_file = "stacks/2.0.6/configs/secured.json"
    with open(config_file, "r") as f:
      secured_json = json.load(f)

    secured_json['hostLevelParams']['stack_version']= '2.2'

    self.executeScript("2.0.6/services/HDFS/package/scripts/datanode.py",
                       classname = "DataNode",
                       command = "stop",
                       config_dict = secured_json
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
    self.assertResourceCalled('Execute', 'ulimit -c unlimited;  su -s /bin/bash - root -c \'export HADOOP_LIBEXEC_DIR=/usr/hdp/current/hadoop-client/libexec && /usr/hdp/current/hadoop-client/sbin/hadoop-daemon.sh --config /etc/hadoop/conf stop datanode\'',
                              not_if = None,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid',
                              action = ['delete'],
                              )
    self.assertNoMoreResources()

  @patch("os.path.exists", new = MagicMock(return_value=False))
  def test_stop_secured_HDP22_non_root_https_only(self):
    config_file = "stacks/2.0.6/configs/secured.json"
    with open(config_file, "r") as f:
      secured_json = json.load(f)

    secured_json['hostLevelParams']['stack_version']= '2.2'
    secured_json['configurations']['hdfs-site']['dfs.http.policy']= 'HTTPS_ONLY'
    secured_json['configurations']['hdfs-site']['dfs.datanode.address']= '0.0.0.0:10000'
    secured_json['configurations']['hdfs-site']['dfs.datanode.https.address']= '0.0.0.0:50000'

    self.executeScript("2.0.6/services/HDFS/package/scripts/datanode.py",
                       classname = "DataNode",
                       command = "stop",
                       config_dict = secured_json
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
    self.assertResourceCalled('Execute', 'ulimit -c unlimited;  su -s /bin/bash - hdfs -c \'export HADOOP_LIBEXEC_DIR=/usr/hdp/current/hadoop-client/libexec && /usr/hdp/current/hadoop-client/sbin/hadoop-daemon.sh --config /etc/hadoop/conf stop datanode\'',
                              not_if=None,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid',
                              action=['delete'],
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
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['core-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['core-site'],
                              mode = 0644
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
    self.assertResourceCalled('Directory', '/hadoop/hdfs/data',
                              owner = 'hdfs',
                              ignore_failures = True,
                              group = 'hadoop',
                              mode = 0755,
                              recursive = True,
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
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['core-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['core-site'],
                              mode = 0644
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
    self.assertResourceCalled('Directory', '/hadoop/hdfs/data',
                              owner = 'hdfs',
                              ignore_failures = True,
                              group = 'hadoop',
                              mode = 0755,
                              recursive = True,
                              )
