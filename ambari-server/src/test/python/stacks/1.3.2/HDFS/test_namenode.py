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


class TestNamenode(RMFTestCase):

  def test_configure_default(self):
    self.executeScript("1.3.2/services/HDFS/package/scripts/namenode.py",
                       classname = "NameNode",
                       command = "configure",
                       config_file="default.json"
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript("1.3.2/services/HDFS/package/scripts/namenode.py",
                       classname = "NameNode",
                       command = "start",
                       config_file="default.json"
    )
    self.assert_configure_default()
    self.assertResourceCalled('File', '/tmp/checkForFormat.sh',
                              content = StaticFile('checkForFormat.sh'),
                              mode = 0o755,
                              )
    self.assertResourceCalled('Execute', 'sh /tmp/checkForFormat.sh hdfs /etc/hadoop/conf /var/run/hadoop/hdfs/namenode/formatted/ /hadoop/hdfs/namenode',
                              path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
                              not_if = 'test -d /var/run/hadoop/hdfs/namenode/formatted/',
                              )
    self.assertResourceCalled('Execute', 'mkdir -p /var/run/hadoop/hdfs/namenode/formatted/',
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('Execute', 'ulimit -c unlimited;  export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop/bin/hadoop-daemon.sh --config /etc/hadoop/conf start namenode',
                              not_if = 'ls /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid` >/dev/null 2>&1',
                              user = 'hdfs',
                              )
    self.assertResourceCalled('Execute', "su - hdfs -c 'hadoop dfsadmin -safemode get' | grep 'Safe mode is OFF'",
                              tries = 40,
                              try_sleep = 10,
                              )
    self.assertResourceCalled('HdfsDirectory', '/tmp',
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              mode = 0o777,
                              owner = 'hdfs',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', '/user/ambari-qa',
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              mode = 0o770,
                              owner = 'ambari-qa',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', None,
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              action = ['create'],
                              )
    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript("1.3.2/services/HDFS/package/scripts/namenode.py",
                       classname = "NameNode",
                       command = "stop",
                       config_file="default.json"
    )
    self.assertResourceCalled('Execute', 'ulimit -c unlimited;  export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop/bin/hadoop-daemon.sh --config /etc/hadoop/conf stop namenode',
                              not_if = None,
                              user = 'hdfs',
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid',
                              action = ['delete'],
                              ignore_failures = True,
                              )
    self.assertNoMoreResources()

  def test_configure_secured(self):
    self.executeScript("1.3.2/services/HDFS/package/scripts/namenode.py",
                       classname = "NameNode",
                       command = "configure",
                       config_file="secured.json"
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()

  def test_start_secured(self):
    self.executeScript("1.3.2/services/HDFS/package/scripts/namenode.py",
                       classname = "NameNode",
                       command = "start",
                       config_file="secured.json"
    )
    self.assert_configure_secured()
    self.assertResourceCalled('File', '/tmp/checkForFormat.sh',
                              content = StaticFile('checkForFormat.sh'),
                              mode = 0o755,
                              )
    self.assertResourceCalled('Execute', 'sh /tmp/checkForFormat.sh hdfs /etc/hadoop/conf /var/run/hadoop/hdfs/namenode/formatted/ /hadoop/hdfs/namenode',
                              path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
                              not_if = 'test -d /var/run/hadoop/hdfs/namenode/formatted/',
                              )
    self.assertResourceCalled('Execute', 'mkdir -p /var/run/hadoop/hdfs/namenode/formatted/',
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('Execute', 'ulimit -c unlimited;  export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop/bin/hadoop-daemon.sh --config /etc/hadoop/conf start namenode',
                              not_if = 'ls /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid` >/dev/null 2>&1',
                              user = 'hdfs',
                              )
    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/hdfs.headless.keytab hdfs',
                              user = 'hdfs',
                              )
    self.assertResourceCalled('Execute', "su - hdfs -c 'hadoop dfsadmin -safemode get' | grep 'Safe mode is OFF'",
                              tries = 40,
                              try_sleep = 10,
                              )
    self.assertResourceCalled('HdfsDirectory', '/tmp',
                              security_enabled = True,
                              keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              mode = 0o777,
                              owner = 'hdfs',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', '/user/ambari-qa',
                              security_enabled = True,
                              keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              mode = 0o770,
                              owner = 'ambari-qa',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', None,
                              security_enabled = True,
                              keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              action = ['create'],
                              )
    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript("1.3.2/services/HDFS/package/scripts/namenode.py",
                       classname = "NameNode",
                       command = "stop",
                       config_file="secured.json"
    )
    self.assertResourceCalled('Execute', 'ulimit -c unlimited;  export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop/bin/hadoop-daemon.sh --config /etc/hadoop/conf stop namenode',
                              not_if = None,
                              user = 'hdfs',
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid',
                              action = ['delete'],
                              ignore_failures = True,
                              )
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/hadoop/hdfs/namenode',
                              owner = 'hdfs',
                              group = 'hadoop',
                              recursive = True,
                              mode = 0o755,
                              )

  def assert_configure_secured(self):
    self.assertResourceCalled('Directory', '/hadoop/hdfs/namenode',
                              owner = 'hdfs',
                              group = 'hadoop',
                              recursive = True,
                              mode = 0o755,
                              )