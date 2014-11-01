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
import os
from mock.mock import MagicMock, call, patch
from stacks.utils.RMFTestCase import *

class TestHiveMetastore(RMFTestCase):

  def test_configure_default(self):
    self.executeScript("2.0.6/services/HIVE/package/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "configure",
                       config_file="../../2.1/configs/default.json"
    )
    self.assert_configure_default()

  def test_start_default(self):
    self.executeScript("2.0.6/services/HIVE/package/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "start",
                       config_file="../../2.1/configs/default.json"
    )

    self.assert_configure_default()
    self.assertResourceCalled('Execute', 'env HADOOP_HOME=/usr JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /tmp/start_metastore_script /var/log/hive/hive.out /var/log/hive/hive.log /var/run/hive/hive.pid /etc/hive/conf.server /var/log/hive',
                              not_if = 'ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps `cat /var/run/hive/hive.pid` >/dev/null 2>&1',
                              environment = {'HADOOP_HOME': '/usr'},
                              path = [os.environ['PATH'] + os.pathsep + "/usr/lib/hive/bin" + os.pathsep + "/usr/bin"],
                              user = 'hive'
    )

    self.assertResourceCalled('Execute', '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/share/java/mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6402.ambari.apache.org/hive?createDatabaseIfNotExist=true\' hive aaa com.mysql.jdbc.Driver',
        path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
        tries = 5,
        try_sleep = 10,
    )

    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript("2.0.6/services/HIVE/package/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "stop",
                       config_file="../../2.1/configs/default.json"
    )

    self.assertResourceCalled('Execute', 'kill `cat /var/run/hive/hive.pid` >/dev/null 2>&1 && rm -f /var/run/hive/hive.pid',
                              not_if = '! (ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps `cat /var/run/hive/hive.pid` >/dev/null 2>&1)'
    )
    self.assertNoMoreResources()

  def test_configure_secured(self):
    self.executeScript("2.0.6/services/HIVE/package/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "configure",
                       config_file="../../2.1/configs/secured.json"
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()

  def test_start_secured(self):
    self.executeScript("2.0.6/services/HIVE/package/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "start",
                       config_file="../../2.1/configs/secured.json"
    )

    self.assert_configure_secured()
    self.assertResourceCalled('Execute', 'env HADOOP_HOME=/usr JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /tmp/start_metastore_script /var/log/hive/hive.out /var/log/hive/hive.log /var/run/hive/hive.pid /etc/hive/conf.server /var/log/hive',
                              not_if = 'ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps `cat /var/run/hive/hive.pid` >/dev/null 2>&1',
                              environment = {'HADOOP_HOME' : '/usr'},
                              path = [os.environ['PATH'] + os.pathsep + "/usr/lib/hive/bin" + os.pathsep + "/usr/bin"],
                              user = 'hive'
    )

    self.assertResourceCalled('Execute', '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/share/java/mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6402.ambari.apache.org/hive?createDatabaseIfNotExist=true\' hive asd com.mysql.jdbc.Driver',
                              path=['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'], tries=5, try_sleep=10
    )

    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript("2.0.6/services/HIVE/package/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "stop",
                       config_file="../../2.1/configs/secured.json"
    )

    self.assertResourceCalled('Execute', 'kill `cat /var/run/hive/hive.pid` >/dev/null 2>&1 && rm -f /var/run/hive/hive.pid',
                              not_if = '! (ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps `cat /var/run/hive/hive.pid` >/dev/null 2>&1)'
    )
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/etc/hive/conf.server',
        owner = 'hive',
        group = 'hadoop',
        recursive = True,
    )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
        group = 'hadoop',
        conf_dir = '/etc/hive/conf.server',
        mode = 0644,
        configuration_attributes = self.getConfig()['configuration_attributes']['mapred-site'],
        owner = 'hive',
        configurations = self.getConfig()['configurations']['mapred-site'],
    )
    self.assertResourceCalled('File', '/etc/hive/conf.server/hive-default.xml.template',
        owner = 'hive',
        group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hive/conf.server/hive-env.sh.template',
        owner = 'hive',
        group = 'hadoop',
    )
    self.assertResourceCalled('Directory', '/etc/hive/conf',
        owner = 'hive',
        group = 'hadoop',
        recursive = True,
    )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
        group = 'hadoop',
        conf_dir = '/etc/hive/conf',
        mode = 0644,
        configuration_attributes = self.getConfig()['configuration_attributes']['mapred-site'],
        owner = 'hive',
        configurations = self.getConfig()['configurations']['mapred-site'],
    )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-default.xml.template',
        owner = 'hive',
        group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-env.sh.template',
        owner = 'hive',
        group = 'hadoop',
    )
    self.assertResourceCalled('XmlConfig', 'hive-site.xml',
                              group = 'hadoop',
                              conf_dir = '/etc/hive/conf.server',
                              mode = 0644,
                              configuration_attributes = self.getConfig()['configuration_attributes']['hive-site'],
                              owner = 'hive',
                              configurations = self.getConfig()['configurations']['hive-site'],
                              )
    self.assertResourceCalled('File', '/etc/hive/conf.server/hive-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['hive-env']['content']),
                              owner = 'hive',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Execute', 'hive mkdir -p /tmp/AMBARI-artifacts/ ; rm -f /usr/lib/hive/lib//mysql-connector-java.jar ; cp /usr/share/java/mysql-connector-java.jar /usr/lib/hive/lib//mysql-connector-java.jar',
        creates = '/usr/lib/hive/lib//mysql-connector-java.jar',
        path = ['/bin', '/usr/bin/'],
        environment = {'PATH' : os.environ['PATH'] + os.pathsep + "/usr/lib/hive/bin" + os.pathsep + "/usr/bin"},
        not_if = 'test -f /usr/lib/hive/lib//mysql-connector-java.jar',
    )
    self.assertResourceCalled('Execute', '/bin/sh -c \'cd /usr/lib/ambari-agent/ && curl -kf -x "" --retry 5 http://c6401.ambari.apache.org:8080/resources/DBConnectionVerification.jar -o DBConnectionVerification.jar\'',
        environment = {'no_proxy': 'c6401.ambari.apache.org'},
        not_if = '[ -f DBConnectionVerification.jar]',
    )
    self.assertResourceCalled('File', '/tmp/start_metastore_script',
        content = StaticFile('startMetastore.sh'),
        mode = 0755,
    )
    self.assertResourceCalled('Execute', 'export HIVE_CONF_DIR=/etc/hive/conf.server ; /usr/lib/hive/bin/schematool -initSchema -dbType mysql -userName hive -passWord aaa',
        not_if = 'export HIVE_CONF_DIR=/etc/hive/conf.server ; /usr/lib/hive/bin/schematool -info -dbType mysql -userName hive -passWord aaa',
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

  def assert_configure_secured(self):
    self.assertResourceCalled('Directory', '/etc/hive/conf.server',
        owner = 'hive',
        group = 'hadoop',
        recursive = True,
    )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
        group = 'hadoop',
        conf_dir = '/etc/hive/conf.server',
        mode = 0644,
        configuration_attributes = self.getConfig()['configuration_attributes']['mapred-site'],
        owner = 'hive',
        configurations = self.getConfig()['configurations']['mapred-site'],
    )
    self.assertResourceCalled('File', '/etc/hive/conf.server/hive-default.xml.template',
        owner = 'hive',
        group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hive/conf.server/hive-env.sh.template',
        owner = 'hive',
        group = 'hadoop',
    )
    self.assertResourceCalled('Directory', '/etc/hive/conf',
        owner = 'hive',
        group = 'hadoop',
        recursive = True,
    )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
        group = 'hadoop',
        conf_dir = '/etc/hive/conf',
        mode = 0644,
        configuration_attributes = self.getConfig()['configuration_attributes']['mapred-site'],
        owner = 'hive',
        configurations = self.getConfig()['configurations']['mapred-site'],
    )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-default.xml.template',
        owner = 'hive',
        group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-env.sh.template',
        owner = 'hive',
        group = 'hadoop',
    )
    self.assertResourceCalled('XmlConfig', 'hive-site.xml',
                              group = 'hadoop',
                              conf_dir = '/etc/hive/conf.server',
                              mode = 0644,
                              configuration_attributes = self.getConfig()['configuration_attributes']['hive-site'],
                              owner = 'hive',
                              configurations = self.getConfig()['configurations']['hive-site'],
                              )
    self.assertResourceCalled('File', '/etc/hive/conf.server/hive-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['hive-env']['content']),
                              owner = 'hive',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Execute', 'hive mkdir -p /tmp/AMBARI-artifacts/ ; rm -f /usr/lib/hive/lib//mysql-connector-java.jar ; cp /usr/share/java/mysql-connector-java.jar /usr/lib/hive/lib//mysql-connector-java.jar',
        creates = '/usr/lib/hive/lib//mysql-connector-java.jar',
        path = ['/bin', '/usr/bin/'],
        environment = {'PATH' : os.environ['PATH'] + os.pathsep + "/usr/lib/hive/bin" + os.pathsep + "/usr/bin"},
        not_if = 'test -f /usr/lib/hive/lib//mysql-connector-java.jar',
    )
    self.assertResourceCalled('Execute', '/bin/sh -c \'cd /usr/lib/ambari-agent/ && curl -kf -x "" --retry 5 http://c6401.ambari.apache.org:8080/resources/DBConnectionVerification.jar -o DBConnectionVerification.jar\'',
        environment = {'no_proxy': 'c6401.ambari.apache.org'},
        not_if = '[ -f DBConnectionVerification.jar]',
    )
    self.assertResourceCalled('File', '/tmp/start_metastore_script',
        content = StaticFile('startMetastore.sh'),
        mode = 0755,
    )
    self.assertResourceCalled('Execute', 'export HIVE_CONF_DIR=/etc/hive/conf.server ; /usr/lib/hive/bin/schematool -initSchema -dbType mysql -userName hive -passWord asd',
        not_if = 'export HIVE_CONF_DIR=/etc/hive/conf.server ; /usr/lib/hive/bin/schematool -info -dbType mysql -userName hive -passWord asd',
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
