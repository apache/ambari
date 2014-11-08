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

import socket

class TestHiveServer(RMFTestCase):

  def test_configure_default(self):
    self.executeScript("1.3.2/services/HIVE/package/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "configure",
                       config_file="default.json"
    )
    self.assert_configure_default()
    self.assertResourceCalled('File',
                              '/etc/hive/conf/hive-exec-log4j.properties',
                              mode=0644,
                              group='hadoop',
                              owner='hive',
                              content='log4jproperties\nline2'
    )
    self.assertResourceCalled('File',
                              '/etc/hive/conf/hive-log4j.properties',
                              mode=0644,
                              group='hadoop',
                              owner='hive',
                              content='log4jproperties\nline2'
    )
    self.assertNoMoreResources()
  
  @patch("socket.socket")
  def test_start_default(self, socket_mock):
    s = socket_mock.return_value
    
    self.executeScript("1.3.2/services/HIVE/package/scripts/hive_server.py",
                         classname = "HiveServer",
                         command = "start",
                         config_file="default.json"
    )

    self.assert_configure_default()
    self.assertResourceCalled('File',
                              '/etc/hive/conf/hive-exec-log4j.properties',
                              mode=0644,
                              group='hadoop',
                              owner='hive',
                              content='log4jproperties\nline2'
    )
    self.assertResourceCalled('File',
                              '/etc/hive/conf/hive-log4j.properties',
                              mode=0644,
                              group='hadoop',
                              owner='hive',
                              content='log4jproperties\nline2'
    )
    self.assertResourceCalled('Execute', 'env JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /tmp/start_hiveserver2_script /var/log/hive/hive-server2.out /var/log/hive/hive-server2.log /var/run/hive/hive-server.pid /etc/hive/conf.server /var/log/hive',
                              not_if = 'ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps `cat /var/run/hive/hive-server.pid` >/dev/null 2>&1',
                              user = 'hive'
    )

    self.assertResourceCalled('Execute', '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/share/java/mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6402.ambari.apache.org/hive?createDatabaseIfNotExist=true\' hive \'!`"\'"\'"\' 1\' com.mysql.jdbc.Driver',
                              path=['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'], tries=5, try_sleep=10
    )

    self.assertNoMoreResources()

  @patch("socket.socket")
  def test_stop_default(self, socket_mock):
    self.executeScript("1.3.2/services/HIVE/package/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "stop",
                       config_file="default.json"
    )

    self.assertResourceCalled('Execute', 'kill `cat /var/run/hive/hive-server.pid` >/dev/null 2>&1 && rm -f /var/run/hive/hive-server.pid',
                              not_if = '! (ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps `cat /var/run/hive/hive-server.pid` >/dev/null 2>&1)'
    )
    
    self.assertNoMoreResources()
    self.assertFalse(socket_mock.called)

  def test_configure_secured(self):
    self.executeScript("1.3.2/services/HIVE/package/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "configure",
                       config_file="secured.json"
    )
    self.assert_configure_secured()
    self.assertResourceCalled('File',
                              '/etc/hive/conf/hive-exec-log4j.properties',
                              mode=0644,
                              group='hadoop',
                              owner='hive',
                              content='log4jproperties\nline2'
    )
    self.assertResourceCalled('File',
                              '/etc/hive/conf/hive-log4j.properties',
                              mode=0644,
                              group='hadoop',
                              owner='hive',
                              content='log4jproperties\nline2'
    )
    self.assertNoMoreResources()

  @patch("socket.socket")
  def test_start_secured(self, socket_mock):
    s = socket_mock.return_value
    
    self.executeScript("1.3.2/services/HIVE/package/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "start",
                       config_file="secured.json"
    )

    self.assert_configure_secured()
    self.assertResourceCalled('File',
                              '/etc/hive/conf/hive-exec-log4j.properties',
                              mode=0644,
                              group='hadoop',
                              owner='hive',
                              content='log4jproperties\nline2'
    )
    self.assertResourceCalled('File',
                              '/etc/hive/conf/hive-log4j.properties',
                              mode=0644,
                              group='hadoop',
                              owner='hive',
                              content='log4jproperties\nline2'
    )
    self.assertResourceCalled('Execute', 'env JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /tmp/start_hiveserver2_script /var/log/hive/hive-server2.out /var/log/hive/hive-server2.log /var/run/hive/hive-server.pid /etc/hive/conf.server /var/log/hive',
                              not_if = 'ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps `cat /var/run/hive/hive-server.pid` >/dev/null 2>&1',
                              user = 'hive'
    )

    self.assertResourceCalled('Execute', '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/share/java/mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6402.ambari.apache.org/hive?createDatabaseIfNotExist=true\' hive \'!`"\'"\'"\' 1\' com.mysql.jdbc.Driver',
                              path=['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'], tries=5, try_sleep=10
    )
    self.assertNoMoreResources()

  @patch("socket.socket")
  def test_stop_secured(self, socket_mock):
    self.executeScript("1.3.2/services/HIVE/package/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "stop",
                       config_file="secured.json"
    )

    self.assertResourceCalled('Execute', 'kill `cat /var/run/hive/hive-server.pid` >/dev/null 2>&1 && rm -f /var/run/hive/hive-server.pid',
                              not_if = '! (ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps `cat /var/run/hive/hive-server.pid` >/dev/null 2>&1)'
    )
    
    self.assertNoMoreResources()
    self.assertFalse(socket_mock.called)

  def assert_configure_default(self):
    self.assertResourceCalled('HdfsDirectory', '/apps/hive/warehouse',
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = "/usr/bin/kinit",
                              mode = 0777,
                              owner = 'hive',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', '/user/hive',
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = "/usr/bin/kinit",
                              mode = 0700,
                              owner = 'hive',
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
    self.assertResourceCalled('Execute', 'hive mkdir -p /tmp/AMBARI-artifacts/ ; cp /usr/share/java/mysql-connector-java.jar /usr/lib/hive/lib//mysql-connector-java.jar',
      creates = '/usr/lib/hive/lib//mysql-connector-java.jar',
      path = ['/bin', '/usr/bin/'],
      not_if = 'test -f /usr/lib/hive/lib//mysql-connector-java.jar',
    )
    self.assertResourceCalled('Directory', '/etc/hive/conf',
                              owner = 'hive',
                              group = 'hadoop',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/etc/hive/conf.server',
      owner = 'hive',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Execute', "/bin/sh -c 'cd /usr/lib/ambari-agent/ && curl -kf -x \"\" --retry 5 http://c6401.ambari.apache.org:8080/resources/DBConnectionVerification.jar -o DBConnectionVerification.jar'",
      not_if = '[ -f DBConnectionVerification.jar]',
      environment = {'no_proxy': 'c6401.ambari.apache.org'}
    )
    self.assertResourceCalled('File', '/tmp/start_hiveserver2_script',
      content = StaticFile('startHiveserver2.sh'),
      mode = 0755,
    )
    self.assertResourceCalled('Directory', '/var/run/hive',
      owner = 'hive',
      group = 'hadoop',
      mode = 0755,
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/log/hive',
      owner = 'hive',
      group = 'hadoop',
      mode = 0755,
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/lib/hive',
      owner = 'hive',
      group = 'hadoop',
      mode = 0755,
      recursive = True,
    )
    self.assertResourceCalled('XmlConfig', 'hive-site.xml',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0600,
                              conf_dir = '/etc/hive/conf.server',
                              configurations = self.getConfig()['configurations']['hive-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['hive-site']
    )
    self.assertResourceCalled('File', '/etc/hive/conf.server/hive-env.sh',
      content = InlineTemplate(self.getConfig()['configurations']['hive-env']['content']),
      owner = 'hive',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-default.xml.template',
      owner = 'hive',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-env.sh.template',
      owner = 'hive',
      group = 'hadoop',
    )

  def assert_configure_secured(self):
    self.assertResourceCalled('HdfsDirectory', '/apps/hive/warehouse',
                              security_enabled = True,
                              keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              mode = 0777,
                              owner = 'hive',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', '/user/hive',
                              security_enabled = True,
                              keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              mode = 0700,
                              owner = 'hive',
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
    self.assertResourceCalled('Execute', 'hive mkdir -p /tmp/AMBARI-artifacts/ ; cp /usr/share/java/mysql-connector-java.jar /usr/lib/hive/lib//mysql-connector-java.jar',
      creates = '/usr/lib/hive/lib//mysql-connector-java.jar',
      path = ['/bin', '/usr/bin/'],
      not_if = 'test -f /usr/lib/hive/lib//mysql-connector-java.jar',
    )
    self.assertResourceCalled('Directory', '/etc/hive/conf',
                              owner = 'hive',
                              group = 'hadoop',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/etc/hive/conf.server',
      owner = 'hive',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Execute', "/bin/sh -c 'cd /usr/lib/ambari-agent/ && curl -kf -x \"\" --retry 5 http://c6401.ambari.apache.org:8080/resources/DBConnectionVerification.jar -o DBConnectionVerification.jar'",
      not_if = '[ -f DBConnectionVerification.jar]',
      environment = {'no_proxy': 'c6401.ambari.apache.org'}
    )
    self.assertResourceCalled('File', '/tmp/start_hiveserver2_script',
      content = StaticFile('startHiveserver2.sh'),
      mode = 0755,
    )
    self.assertResourceCalled('Directory', '/var/run/hive',
      owner = 'hive',
      group = 'hadoop',
      mode = 0755,
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/log/hive',
      owner = 'hive',
      group = 'hadoop',
      mode = 0755,
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/lib/hive',
      owner = 'hive',
      group = 'hadoop',
      mode = 0755,
      recursive = True,
    )
    self.assertResourceCalled('XmlConfig', 'hive-site.xml',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0600,
                              conf_dir = '/etc/hive/conf.server',
                              configurations = self.getConfig()['configurations']['hive-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['hive-site']
    )
    self.assertResourceCalled('File', '/etc/hive/conf.server/hive-env.sh',
      content = InlineTemplate(self.getConfig()['configurations']['hive-env']['content']),
      owner = 'hive',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-default.xml.template',
      owner = 'hive',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-env.sh.template',
      owner = 'hive',
      group = 'hadoop',
    )

  @patch("time.time")
  @patch("socket.socket")
  def test_socket_timeout(self, socket_mock, time_mock):        
    s = socket_mock.return_value
    s.connect = MagicMock()    
    s.connect.side_effect = socket.error("")
    
    time_mock.side_effect = [0, 1000, 2000, 3000, 4000]
    
    try:
      self.executeScript("1.3.2/services/HIVE/package/scripts/hive_server.py",
                           classname = "HiveServer",
                           command = "start",
                           config_file="default.json"
      )
      
      self.fail("Script failure due to socket error was expected")
    except:
      self.assert_configure_default()
      self.assertFalse(socket_mock.called)
      self.assertFalse(s.close.called)
