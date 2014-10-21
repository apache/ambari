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

@patch("os.path.isfile", new = MagicMock(return_value=True))
@patch("glob.glob", new = MagicMock(return_value=["one", "two"]))
class TestWebHCatServer(RMFTestCase):

  def test_configure_default(self):
    self.executeScript("2.0.6/services/HIVE/package/scripts/webhcat_server.py",
                       classname = "WebHCatServer",
                       command = "configure",
                       config_file="default.json"
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript("2.0.6/services/HIVE/package/scripts/webhcat_server.py",
                       classname = "WebHCatServer",
                       command = "start",
                       config_file="default.json"
    )

    self.assert_configure_default()
    self.assertResourceCalled('Execute', 'env HADOOP_HOME=/usr /usr/lib/hcatalog/sbin/webhcat_server.sh start',
                              not_if = 'ls /var/run/webhcat/webhcat.pid >/dev/null 2>&1 && ps `cat /var/run/webhcat/webhcat.pid` >/dev/null 2>&1',
                              user = 'hcat'
    )
    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript("2.0.6/services/HIVE/package/scripts/webhcat_server.py",
                       classname = "WebHCatServer",
                       command = "stop",
                       config_file="default.json"
    )

    self.assertResourceCalled('Execute', 'env HADOOP_HOME=/usr /usr/lib/hcatalog/sbin/webhcat_server.sh stop',
                              user = 'hcat',
                              )
    self.assertResourceCalled('Execute', 'rm -f /var/run/webhcat/webhcat.pid')
    self.assertNoMoreResources()

    def test_configure_secured(self):
      self.executeScript("2.0.6/services/HIVE/package/scripts/webhcat_server.py",
                         classname = "WebHCatServer",
                         command = "configure",
                         config_file="secured.json"
      )

      self.assert_configure_secured()
      self.assertNoMoreResources()

  def test_start_secured(self):
    self.executeScript("2.0.6/services/HIVE/package/scripts/webhcat_server.py",
                       classname = "WebHCatServer",
                       command = "start",
                       config_file="secured.json"
    )

    self.assert_configure_secured()
    self.assertResourceCalled('Execute', 'env HADOOP_HOME=/usr /usr/lib/hcatalog/sbin/webhcat_server.sh start',
                              not_if = 'ls /var/run/webhcat/webhcat.pid >/dev/null 2>&1 && ps `cat /var/run/webhcat/webhcat.pid` >/dev/null 2>&1',
                              user = 'hcat'
    )
    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript("2.0.6/services/HIVE/package/scripts/webhcat_server.py",
                       classname = "WebHCatServer",
                       command = "stop",
                       config_file="secured.json"
    )

    self.assertResourceCalled('Execute', 'env HADOOP_HOME=/usr /usr/lib/hcatalog/sbin/webhcat_server.sh stop',
                              user = 'hcat',
                              )
    self.assertResourceCalled('Execute', 'rm -f /var/run/webhcat/webhcat.pid')
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('HdfsDirectory', '/apps/webhcat',
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = "/usr/bin/kinit",
                              mode = 0755,
                              owner = 'hcat',
                              bin_dir = '/usr/bin',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', '/user/hcat',
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = "/usr/bin/kinit",
                              mode = 0755,
                              owner = 'hcat',
                              bin_dir = '/usr/bin',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', None,
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = "/usr/bin/kinit",
                              bin_dir = '/usr/bin',
                              action = ['create'],
                              )
    self.assertResourceCalled('Directory', '/var/run/webhcat',
                              owner = 'hcat',
                              group = 'hadoop',
                              recursive = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/log/webhcat',
                              owner = 'hcat',
                              group = 'hadoop',
                              recursive = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/etc/hcatalog/conf',
                              owner = 'hcat',
                              group = 'hadoop',
                              recursive = True,
                              )
    self.assertResourceCalled('CopyFromLocal', '/usr/lib/hadoop-mapreduce/hadoop-streaming-*.jar',
                              owner='hcat',
                              mode=0755,
                              dest_dir='/apps/webhcat',
                              kinnit_if_needed='',
                              hadoop_conf_dir='/etc/hadoop/conf',
                              hadoop_bin_dir='/usr/bin',
                              hdfs_user='hdfs'
    )
    self.assertResourceCalled('CopyFromLocal', '/usr/share/HDP-webhcat/pig.tar.gz',
                              owner='hcat',
                              mode=0755,
                              dest_dir='/apps/webhcat',
                              kinnit_if_needed='',
                              hadoop_conf_dir='/etc/hadoop/conf',
                              hadoop_bin_dir='/usr/bin',
                              hdfs_user='hdfs'
    )
    self.assertResourceCalled('CopyFromLocal', '/usr/share/HDP-webhcat/hive.tar.gz',
                              owner='hcat',
                              mode=0755,
                              dest_dir='/apps/webhcat',
                              kinnit_if_needed='',
                              hadoop_bin_dir='/usr/bin',
                              hadoop_conf_dir='/etc/hadoop/conf',
                              hdfs_user='hdfs'
    )
    self.assertResourceCalled('CopyFromLocal', '/usr/share/HDP-webhcat/sqoop*.tar.gz',
                              owner='hcat',
                              mode=0755,
                              dest_dir='/apps/webhcat',
                              kinnit_if_needed='',
                              hadoop_bin_dir='/usr/bin',
                              hadoop_conf_dir='/etc/hadoop/conf',
                              hdfs_user='hdfs'
    )
    self.assertResourceCalled('XmlConfig', 'webhcat-site.xml',
                              owner = 'hcat',
                              group = 'hadoop',
                              conf_dir = '/etc/hcatalog/conf',
                              configurations = self.getConfig()['configurations']['webhcat-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['webhcat-site']
    )
    self.assertResourceCalled('File', '/etc/hcatalog/conf/webhcat-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['webhcat-env']['content']),
                              owner = 'hcat',
                              group = 'hadoop',
                              )

  def assert_configure_secured(self):
    self.assertResourceCalled('HdfsDirectory', '/apps/webhcat',
                              security_enabled = True,
                              keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              mode = 0755,
                              owner = 'hcat',
                              bin_dir = '/usr/bin',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', '/user/hcat',
                              security_enabled = True,
                              keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              mode = 0755,
                              owner = 'hcat',
                              bin_dir = '/usr/bin',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', None,
                              security_enabled = True,
                              keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              bin_dir = '/usr/bin',
                              action = ['create'],
                              )
    self.assertResourceCalled('Directory', '/var/run/webhcat',
                              owner = 'hcat',
                              group = 'hadoop',
                              recursive = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/log/webhcat',
                              owner = 'hcat',
                              group = 'hadoop',
                              recursive = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/etc/hcatalog/conf',
                              owner = 'hcat',
                              group = 'hadoop',
                              recursive = True,
                              )
    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/hdfs.headless.keytab hdfs;',
                              path = ['/bin'],
                              user = 'hcat',
                              )
    self.assertResourceCalled('CopyFromLocal', '/usr/lib/hadoop-mapreduce/hadoop-streaming-*.jar',
                              owner='hcat',
                              mode=0755,
                              dest_dir='/apps/webhcat',
                              kinnit_if_needed='/usr/bin/kinit -kt /etc/security/keytabs/hdfs.headless.keytab hdfs;',
                              hadoop_conf_dir='/etc/hadoop/conf',
                              hadoop_bin_dir='/usr/bin',
                              hdfs_user='hdfs'
    )
    self.assertResourceCalled('CopyFromLocal', '/usr/share/HDP-webhcat/pig.tar.gz',
                              owner='hcat',
                              mode=0755,
                              dest_dir='/apps/webhcat',
                              kinnit_if_needed='/usr/bin/kinit -kt /etc/security/keytabs/hdfs.headless.keytab hdfs;',
                              hadoop_conf_dir='/etc/hadoop/conf',
                              hadoop_bin_dir='/usr/bin',
                              hdfs_user='hdfs'
    )
    self.assertResourceCalled('CopyFromLocal', '/usr/share/HDP-webhcat/hive.tar.gz',
                              owner='hcat',
                              mode=0755,
                              dest_dir='/apps/webhcat',
                              kinnit_if_needed='/usr/bin/kinit -kt /etc/security/keytabs/hdfs.headless.keytab hdfs;',
                              hadoop_conf_dir='/etc/hadoop/conf',
                              hadoop_bin_dir='/usr/bin',
                              hdfs_user='hdfs'
    )
    self.assertResourceCalled('CopyFromLocal', '/usr/share/HDP-webhcat/sqoop*.tar.gz',
                              owner='hcat',
                              mode=0755,
                              dest_dir='/apps/webhcat',
                              kinnit_if_needed='/usr/bin/kinit -kt /etc/security/keytabs/hdfs.headless.keytab hdfs;',
                              hadoop_conf_dir='/etc/hadoop/conf',
                              hadoop_bin_dir='/usr/bin',
                              hdfs_user='hdfs'
    )
    self.assertResourceCalled('XmlConfig', 'webhcat-site.xml',
                              owner = 'hcat',
                              group = 'hadoop',
                              conf_dir = '/etc/hcatalog/conf',
                              configurations = self.getConfig()['configurations']['webhcat-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['webhcat-site']
    )
    self.assertResourceCalled('File', '/etc/hcatalog/conf/webhcat-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['webhcat-env']['content']),
                              owner = 'hcat',
                              group = 'hadoop',
                              )