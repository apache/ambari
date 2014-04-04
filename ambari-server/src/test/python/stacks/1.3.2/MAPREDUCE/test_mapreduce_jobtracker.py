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

class TestJobtracker(RMFTestCase):

  def test_configure_default(self):
    self.executeScript("1.3.2/services/MAPREDUCE/package/scripts/jobtracker.py",
                       classname = "Jobtracker",
                       command = "configure",
                       config_file="default.json"
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript("1.3.2/services/MAPREDUCE/package/scripts/jobtracker.py",
                       classname = "Jobtracker",
                       command = "start",
                       config_file="default.json"
      )

    self.assert_configure_default()
    self.assertResourceCalled('Execute', 'export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop/bin/hadoop-daemon.sh --config /etc/hadoop/conf start jobtracker',
                       user = 'mapred',
                       not_if = 'ls /var/run/hadoop/mapred/hadoop-mapred-jobtracker.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop/mapred/hadoop-mapred-jobtracker.pid` >/dev/null 2>&1'
    )
    self.assertResourceCalled('Execute', 'ls /var/run/hadoop/mapred/hadoop-mapred-jobtracker.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop/mapred/hadoop-mapred-jobtracker.pid` >/dev/null 2>&1',
                       user = 'mapred',
                       initial_wait = 5,
                       not_if= 'ls /var/run/hadoop/mapred/hadoop-mapred-jobtracker.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop/mapred/hadoop-mapred-jobtracker.pid` >/dev/null 2>&1'
    )
    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript("1.3.2/services/MAPREDUCE/package/scripts/jobtracker.py",
                       classname = "Jobtracker",
                       command = "stop",
                       config_file="default.json"
    )

    self.assertResourceCalled('Execute', 'export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop/bin/hadoop-daemon.sh --config /etc/hadoop/conf stop jobtracker',
                              user = 'mapred'
    )
    self.assertResourceCalled('Execute', 'rm -f /var/run/hadoop/mapred/hadoop-mapred-jobtracker.pid')
    self.assertNoMoreResources()

  def test_decommission_default(self):

    self.executeScript("1.3.2/services/MAPREDUCE/package/scripts/jobtracker.py",
                       classname = "Jobtracker",
                       command = "decommission",
                       config_file="default.json"
    )

    self.assertResourceCalled('File', '/etc/hadoop/conf/mapred.exclude',
                       owner = 'mapred',
                       content = Template('exclude_hosts_list.j2'),
                       group = 'hadoop',
    )
    self.assertResourceCalled('ExecuteHadoop', 'mradmin -refreshNodes',
                       conf_dir = '/etc/hadoop/conf',
                       kinit_override = True,
                       user = 'mapred',
    )
    self.assertNoMoreResources()

  def test_decommission_default_no_refersh(self):

    self.executeScript("1.3.2/services/MAPREDUCE/package/scripts/jobtracker.py",
                       classname = "Jobtracker",
                       command = "decommission",
                       config_file="default.hbasedecom.json"
    )

    self.assertResourceCalled('File', '/etc/hadoop/conf/mapred.exclude',
                              owner = 'mapred',
                              content = Template('exclude_hosts_list.j2'),
                              group = 'hadoop',
                              )
    self.assertNoMoreResources()

  def test_configure_secured(self):

    self.executeScript("1.3.2/services/MAPREDUCE/package/scripts/jobtracker.py",
                       classname = "Jobtracker",
                       command = "configure",
                       config_file="secured.json"
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()

  def test_start_secured(self):
    self.executeScript("1.3.2/services/MAPREDUCE/package/scripts/jobtracker.py",
                       classname = "Jobtracker",
                       command = "start",
                       config_file="secured.json"
    )

    self.assert_configure_secured()
    self.assertResourceCalled('Execute', 'export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop/bin/hadoop-daemon.sh --config /etc/hadoop/conf start jobtracker',
                       user = 'mapred',
                       not_if = 'ls /var/run/hadoop/mapred/hadoop-mapred-jobtracker.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop/mapred/hadoop-mapred-jobtracker.pid` >/dev/null 2>&1'
    )
    self.assertResourceCalled('Execute', 'ls /var/run/hadoop/mapred/hadoop-mapred-jobtracker.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop/mapred/hadoop-mapred-jobtracker.pid` >/dev/null 2>&1',
                       user = 'mapred',
                       initial_wait = 5,
                       not_if= 'ls /var/run/hadoop/mapred/hadoop-mapred-jobtracker.pid >/dev/null 2>&1 && ps `cat /var/run/hadoop/mapred/hadoop-mapred-jobtracker.pid` >/dev/null 2>&1'
    )
    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript("1.3.2/services/MAPREDUCE/package/scripts/jobtracker.py",
                       classname = "Jobtracker",
                       command = "stop",
                       config_file="secured.json"
    )

    self.assertResourceCalled('Execute', 'export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop/bin/hadoop-daemon.sh --config /etc/hadoop/conf stop jobtracker',
                       user = 'mapred'
    )
    self.assertResourceCalled('Execute', 'rm -f /var/run/hadoop/mapred/hadoop-mapred-jobtracker.pid')
    self.assertNoMoreResources()

  def test_decommission_secured(self):
    self.executeScript("1.3.2/services/MAPREDUCE/package/scripts/jobtracker.py",
                       classname = "Jobtracker",
                       command = "decommission",
                       config_file="secured.json"
    )

    self.assertResourceCalled('File', '/etc/hadoop/conf/mapred.exclude',
                       owner = 'mapred',
                       content = Template('exclude_hosts_list.j2'),
                       group = 'hadoop',
    )

    self.assertResourceCalled('ExecuteHadoop', 'mradmin -refreshNodes',
                       conf_dir = '/etc/hadoop/conf',
                       kinit_override = True,
                       user = 'mapred',
    )
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('HdfsDirectory', '/mapred',
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = "/usr/bin/kinit",
                              owner = 'mapred',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', '/mapred/system',
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = "/usr/bin/kinit",
                              owner = 'mapred',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', '/mapred/history',
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = "/usr/bin/kinit",
                              owner = 'mapred',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', '/mr-history/tmp',
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = "/usr/bin/kinit",
                              mode = 0777,
                              owner = 'mapred',
                              group = 'hadoop',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', '/mapred/history/done',
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = "/usr/bin/kinit",
                              mode = 0777,
                              owner = 'mapred',
                              group = 'hadoop',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', None,
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = "/usr/bin/kinit",
                              action = ['create'],
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop/mapred',
      owner = 'mapred',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop/mapred',
      owner = 'mapred',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('File', '/var/log/hadoop/mapred/hadoop-mapreduce.jobsummary.log',
      owner = 'mapred',
      group = 'hadoop',
      mode = 0664
    )
    self.assertResourceCalled('Directory', '/hadoop/mapred',
      owner = 'mapred',
      recursive = True,
      mode = 0755,
      ignore_failures=True,
    )
    self.assertResourceCalled('Directory', '/hadoop/mapred1',
      owner = 'mapred',
      recursive = True,
      mode = 0755,
      ignore_failures=True,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/mapred.exclude',
      owner = 'mapred',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/mapred.include',
      owner = 'mapred',
      group = 'hadoop',
    )

  def assert_configure_secured(self):
    self.assertResourceCalled('HdfsDirectory', '/mapred',
                              security_enabled = True,
                              keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              owner = 'mapred',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', '/mapred/system',
                              security_enabled = True,
                              keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              owner = 'mapred',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', '/mapred/history',
                              security_enabled = True,
                              keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              owner = 'mapred',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', '/mr-history/tmp',
                              security_enabled = True,
                              keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              mode = 0777,
                              owner = 'mapred',
                              group = 'hadoop',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', '/mapred/history/done',
                              security_enabled = True,
                              keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              mode = 0777,
                              owner = 'mapred',
                              group = 'hadoop',
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
    self.assertResourceCalled('Directory', '/var/run/hadoop/mapred',
      owner = 'mapred',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop/mapred',
      owner = 'mapred',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('File', '/var/log/hadoop/mapred/hadoop-mapreduce.jobsummary.log',
      owner = 'mapred',
      group = 'hadoop',
      mode = 0664
    )
    self.assertResourceCalled('Directory', '/hadoop/mapred',
      owner = 'mapred',
      recursive = True,
      mode = 0755,
      ignore_failures=True,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/mapred.exclude',
      owner = 'mapred',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/mapred.include',
      owner = 'mapred',
      group = 'hadoop',
    )
