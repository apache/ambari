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

class TestHiveMetastore(RMFTestCase):

  def test_configure_default(self):
    self.executeScript("2.0.6/services/HIVE/package/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "configure",
                       config_file="default.json"
    )
    self.assert_configure_default()

  def test_start_default(self):
    self.executeScript("2.0.6/services/HIVE/package/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
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
    self.assertResourceCalled('Execute', 'env HADOOP_HOME=/usr JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /tmp/start_metastore_script /var/log/hive/hive.out /var/log/hive/hive.log /var/run/hive/hive.pid /etc/hive/conf.server /var/log/hive',
                              not_if = 'ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps `cat /var/run/hive/hive.pid` >/dev/null 2>&1',
                              user = 'hive'
    )

    self.assertResourceCalled('Execute', '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/share/java/mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification jdbc:mysql://c6402.ambari.apache.org/hive?createDatabaseIfNotExist=true hive \'!`"\'"\'"\' 1\' com.mysql.jdbc.Driver',
                              path=['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin']
    )

    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript("2.0.6/services/HIVE/package/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "stop",
                       config_file="default.json"
    )

    self.assertResourceCalled('Execute', 'kill `cat /var/run/hive/hive.pid` >/dev/null 2>&1 && rm -f /var/run/hive/hive.pid')
    self.assertNoMoreResources()

  def test_configure_secured(self):
    self.executeScript("2.0.6/services/HIVE/package/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "configure",
                       config_file="secured.json"
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

  def test_start_secured(self):
    self.executeScript("2.0.6/services/HIVE/package/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
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
    self.assertResourceCalled('Execute', 'env HADOOP_HOME=/usr JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /tmp/start_metastore_script /var/log/hive/hive.out /var/log/hive/hive.log /var/run/hive/hive.pid /etc/hive/conf.server /var/log/hive',
                              not_if = 'ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps `cat /var/run/hive/hive.pid` >/dev/null 2>&1',
                              user = 'hive'
    )

    self.assertResourceCalled('Execute', '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/share/java/mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification jdbc:mysql://c6402.ambari.apache.org/hive?createDatabaseIfNotExist=true hive \'!`"\'"\'"\' 1\' com.mysql.jdbc.Driver',
                              path=['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin']
    )

    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript("2.0.6/services/HIVE/package/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "stop",
                       config_file="secured.json"
    )

    self.assertResourceCalled('Execute', 'kill `cat /var/run/hive/hive.pid` >/dev/null 2>&1 && rm -f /var/run/hive/hive.pid')
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Execute', 'hive mkdir -p /tmp/HDP-artifacts/ ; cp /usr/share/java/mysql-connector-java.jar /usr/lib/hive/lib//mysql-connector-java.jar',
      creates = '/usr/lib/hive/lib//mysql-connector-java.jar',
      path = ['/bin', '/usr/bin/'],
      not_if = 'test -f /usr/lib/hive/lib//mysql-connector-java.jar',
    )
    self.assertResourceCalled('Directory', '/etc/hive/conf.server',
      owner = 'hive',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
      owner = 'hive',
      group = 'hadoop',
      mode = 0600,
      conf_dir = '/etc/hive/conf.server',
      configurations = self.getConfig()['configurations']['mapred-site'],
    )
    self.assertResourceCalled('XmlConfig', 'hive-site.xml',
      owner = 'hive',
      group = 'hadoop',
      mode = 0600,
      conf_dir = '/etc/hive/conf.server',
      configurations = self.getConfig()['configurations']['hive-site'],
    )
    self.assertResourceCalled('Execute', "/bin/sh -c 'cd /usr/lib/ambari-agent/ && curl -kf --retry 5 http://c6401.ambari.apache.org:8080/resources/DBConnectionVerification.jar -o DBConnectionVerification.jar'",
      not_if = '[ -f DBConnectionVerification.jar]',
    )
    self.assertResourceCalled('File', '/tmp/start_metastore_script',
      content = StaticFile('startMetastore.sh'),
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
    self.assertResourceCalled('File', '/etc/hive/conf.server/hive-env.sh',
      content = Template('hive-env.sh.j2', conf_dir="/etc/hive/conf.server"),
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
    self.assertResourceCalled('Execute', 'hive mkdir -p /tmp/HDP-artifacts/ ; cp /usr/share/java/mysql-connector-java.jar /usr/lib/hive/lib//mysql-connector-java.jar',
      creates = '/usr/lib/hive/lib//mysql-connector-java.jar',
      path = ['/bin', '/usr/bin/'],
      not_if = 'test -f /usr/lib/hive/lib//mysql-connector-java.jar',
    )
    self.assertResourceCalled('Directory', '/etc/hive/conf.server',
      owner = 'hive',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
      owner = 'hive',
      group = 'hadoop',
      mode = 0600,
      conf_dir = '/etc/hive/conf.server',
      configurations = self.getConfig()['configurations']['mapred-site'],
    )
    self.assertResourceCalled('XmlConfig', 'hive-site.xml',
      owner = 'hive',
      group = 'hadoop',
      mode = 0600,
      conf_dir = '/etc/hive/conf.server',
      configurations = self.getConfig()['configurations']['hive-site'],
    )
    self.assertResourceCalled('Execute', "/bin/sh -c 'cd /usr/lib/ambari-agent/ && curl -kf --retry 5 http://c6401.ambari.apache.org:8080/resources/DBConnectionVerification.jar -o DBConnectionVerification.jar'",
      not_if = '[ -f DBConnectionVerification.jar]',
    )
    self.assertResourceCalled('File', '/tmp/start_metastore_script',
      content = StaticFile('startMetastore.sh'),
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
    self.assertResourceCalled('File', '/etc/hive/conf.server/hive-env.sh',
      content = Template('hive-env.sh.j2', conf_dir="/etc/hive/conf.server"),
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

