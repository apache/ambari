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
import json
import os
from mock.mock import MagicMock, call, patch
from stacks.utils.RMFTestCase import *

@patch("resource_management.libraries.functions.get_user_call_output.get_user_call_output", new=MagicMock(return_value=(0,'123','')))
class TestHiveMetastore(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "HIVE/0.12.0.2.0/package"
  STACK_VERSION = "2.0.6"

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "configure",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()

  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "start",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_default()
    self.assertResourceCalled('Execute', '/tmp/start_metastore_script /var/log/hive/hive.out /var/log/hive/hive.err /var/run/hive/hive.pid /etc/hive/conf.server /var/log/hive',
        environment = {'HADOOP_HOME': '/usr/hdp/current/hadoop-client',
           'HIVE_BIN': 'hive',
           'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        not_if = "ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1",
        user = 'hive',
        path = ['/bin:/usr/hdp/current/hive-server2/bin:/usr/bin'],
    )
    self.assertResourceCalled('Execute', '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/hdp/current/hive-server2/lib/mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6402.ambari.apache.org/hive?createDatabaseIfNotExist=true\' hive \'!`"\'"\'"\' 1\' com.mysql.jdbc.Driver',
        path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
        tries = 5,
        try_sleep = 10,
    )
    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "stop",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Execute', "ambari-sudo.sh kill 123",
        not_if = "! (ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1)",
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh kill -9 123",
        not_if = "! (ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1) || ( sleep 5 && ! (ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1) )",
        ignore_failures = True
    )
    self.assertResourceCalled('Execute', "! (ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1)",
        tries = 20,
        try_sleep = 3,
    )
    self.assertResourceCalled('File', '/var/run/hive/hive.pid',
      action = ['delete'],
    )
    self.assertNoMoreResources()

  def test_configure_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "configure",
                       config_file="secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "start",
                       config_file="secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertResourceCalled('Execute', '/tmp/start_metastore_script /var/log/hive/hive.out /var/log/hive/hive.err /var/run/hive/hive.pid /etc/hive/conf.server /var/log/hive',
        environment = {'HADOOP_HOME': '/usr/hdp/current/hadoop-client',
           'HIVE_BIN': 'hive',
           'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        not_if = "ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1",
        user = 'hive',
        path = ['/bin:/usr/hdp/current/hive-server2/bin:/usr/bin'],
    )
    self.assertResourceCalled('Execute', '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/hdp/current/hive-server2/lib/mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6402.ambari.apache.org/hive?createDatabaseIfNotExist=true\' hive \'!`"\'"\'"\' 1\' com.mysql.jdbc.Driver',
        path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
        tries = 5,
        try_sleep = 10,
    )
    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "stop",
                       config_file="secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Execute', "ambari-sudo.sh kill 123",
        not_if = "! (ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1)",
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh kill -9 123",
        not_if = "! (ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1) || ( sleep 5 && ! (ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1) )",
        ignore_failures = True
    )
    self.assertResourceCalled('Execute', "! (ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1)",
        tries = 20,
        try_sleep = 3,
    )
    self.assertResourceCalled('File', '/var/run/hive/hive.pid',
     action = ['delete'],
    )

    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/etc/hive',
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/usr/hdp/current/hive-server2/conf',
                              owner = 'hive',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
                              group = 'hadoop',
                              conf_dir = '/usr/hdp/current/hive-server2/conf',
                              mode = 0644,
                              configuration_attributes = {u'final': {u'mapred.healthChecker.script.path': u'true',
                                                                     u'mapreduce.jobtracker.staging.root.dir': u'true'}},
                              owner = 'hive',
                              configurations = self.getConfig()['configurations']['mapred-site'],
                              )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/hive-default.xml.template',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0644,
                              )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/hive-env.sh.template',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0644,
                              )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/hive-exec-log4j.properties',
                              content = 'log4jproperties\nline2',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0644,
                              )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/hive-log4j.properties',
                              content = 'log4jproperties\nline2',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0644,
                              )
    self.assertResourceCalled('XmlConfig', 'hive-site.xml',
                              group = 'hadoop',
                              conf_dir = '/etc/hive/conf.server',
                              mode = 0600,
                              configuration_attributes = {u'final': {u'hive.optimize.bucketmapjoin.sortedmerge': u'true',
                                                                     u'javax.jdo.option.ConnectionDriverName': u'true',
                                                                     u'javax.jdo.option.ConnectionPassword': u'true'}},
                              owner = 'hive',
                              configurations = self.getConfig()['configurations']['hive-site'],
                              )
    self.assertResourceCalled('File', '/etc/hive/conf.server/hive-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['hive-env']['content']),
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0600,
                              )
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
                              owner = 'root',
                              group = 'root',
                              create_parents = True,
                              )
    self.assertResourceCalled('File', '/etc/security/limits.d/hive.conf',
                              content = Template('hive.conf.j2'),
                              owner = 'root',
                              group = 'root',
                              mode = 0644,
                              )

    self.assertResourceCalled('File', '/usr/lib/ambari-agent/DBConnectionVerification.jar',
        content = DownloadSource('http://c6401.ambari.apache.org:8080/resources/DBConnectionVerification.jar'),
        mode = 0644,
    )

    self.assertResourceCalled('Directory', '/var/run/hive',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0755,
                              create_parents = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/log/hive',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0755,
                              create_parents = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/lib/hive',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0755,
                              create_parents = True,
                              cd_access = 'a',
                              )
    
    self.assertResourceCalled('Execute', ('cp',
                                          '--remove-destination',
                                          '/usr/share/java/mysql-connector-java.jar',
                                          '/usr/hdp/current/hive-server2/lib/mysql-connector-java.jar'),
                              path = ['/bin', '/usr/bin/'],
                              sudo = True,
                              )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/lib/mysql-connector-java.jar',
        mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/hive/conf.server/hadoop-metrics2-hivemetastore.properties',
                              owner = 'hive',
                              group = 'hadoop',
                              content = Template('hadoop-metrics2-hivemetastore.properties.j2'),
                              mode = 0600,
                              )
    self.assertResourceCalled('File', '/tmp/start_metastore_script',
                              content = StaticFile('startMetastore.sh'),
                              mode = 0755,
                              )
    self.assertResourceCalled('Execute', 'export HIVE_CONF_DIR=/etc/hive/conf.server ; /usr/hdp/current/hive-server2/bin/schematool -initSchema -dbType mysql -userName hive -passWord \'!`"\'"\'"\' 1\' -verbose',
        not_if = 'ambari-sudo.sh su hive -l -s /bin/bash -c \'[RMF_EXPORT_PLACEHOLDER]export HIVE_CONF_DIR=/etc/hive/conf.server ; /usr/hdp/current/hive-server2/bin/schematool -info -dbType mysql -userName hive -passWord \'"\'"\'!`"\'"\'"\'"\'"\'"\'"\'"\'"\' 1\'"\'"\' -verbose\'',
        user = 'hive',
    )

  def assert_configure_secured(self):
    self.assertResourceCalled('Directory', '/etc/hive',
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/usr/hdp/current/hive-server2/conf',
                              owner = 'hive',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
                              group = 'hadoop',
                              conf_dir = '/usr/hdp/current/hive-server2/conf',
                              mode = 0644,
                              configuration_attributes = {u'final': {u'mapred.healthChecker.script.path': u'true',
                                                                     u'mapreduce.jobtracker.staging.root.dir': u'true'}},
                              owner = 'hive',
                              configurations = self.getConfig()['configurations']['mapred-site'],
                              )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/hive-default.xml.template',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0644,
                              )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/hive-env.sh.template',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0644,
                              )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/hive-exec-log4j.properties',
                              content = 'log4jproperties\nline2',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0644,
                              )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/hive-log4j.properties',
                              content = 'log4jproperties\nline2',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0644,
                              )
    self.assertResourceCalled('XmlConfig', 'hive-site.xml',
                              group = 'hadoop',
                              conf_dir = '/etc/hive/conf.server',
                              mode = 0600,
                              configuration_attributes = {u'final': {u'hive.optimize.bucketmapjoin.sortedmerge': u'true',
                                                                     u'javax.jdo.option.ConnectionDriverName': u'true',
                                                                     u'javax.jdo.option.ConnectionPassword': u'true'}},
                              owner = 'hive',
                              configurations = self.getConfig()['configurations']['hive-site'],
                              )
    self.assertResourceCalled('File', '/etc/hive/conf.server/hive-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['hive-env']['content']),
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0600,
                              )
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
                              owner = 'root',
                              group = 'root',
                              create_parents = True,
                              )
    self.assertResourceCalled('File', '/etc/security/limits.d/hive.conf',
                              content = Template('hive.conf.j2'),
                              owner = 'root',
                              group = 'root',
                              mode = 0644,
                              )
    self.assertResourceCalled('File', '/usr/lib/ambari-agent/DBConnectionVerification.jar',
        content = DownloadSource('http://c6401.ambari.apache.org:8080/resources/DBConnectionVerification.jar'),
        mode = 0644,
    )
    self.assertResourceCalled('Directory', '/var/run/hive',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0755,
                              create_parents = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/log/hive',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0755,
                              create_parents = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/lib/hive',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0755,
                              create_parents = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Execute', ('cp',
                                          '--remove-destination',
                                          '/usr/share/java/mysql-connector-java.jar',
                                          '/usr/hdp/current/hive-server2/lib/mysql-connector-java.jar'),
                              path = ['/bin', '/usr/bin/'],
                              sudo = True,
                              )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/lib/mysql-connector-java.jar',
        mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/hive/conf.server/hadoop-metrics2-hivemetastore.properties',
                              owner = 'hive',
                              group = 'hadoop',
                              content = Template('hadoop-metrics2-hivemetastore.properties.j2'),
                              mode = 0600,
                              )
    self.assertResourceCalled('File', '/tmp/start_metastore_script',
                              content = StaticFile('startMetastore.sh'),
                              mode = 0755,
                              )
    self.assertResourceCalled('Execute', 'export HIVE_CONF_DIR=/etc/hive/conf.server ; /usr/hdp/current/hive-server2/bin/schematool -initSchema -dbType mysql -userName hive -passWord \'!`"\'"\'"\' 1\' -verbose',
        not_if = 'ambari-sudo.sh su hive -l -s /bin/bash -c \'[RMF_EXPORT_PLACEHOLDER]export HIVE_CONF_DIR=/etc/hive/conf.server ; /usr/hdp/current/hive-server2/bin/schematool -info -dbType mysql -userName hive -passWord \'"\'"\'!`"\'"\'"\'"\'"\'"\'"\'"\'"\' 1\'"\'"\' -verbose\'',
        user = 'hive',
    )

  @patch("resource_management.core.shell.call")
  @patch("resource_management.libraries.functions.get_stack_version")
  def test_start_ru(self, call_mock, get_stack_version_mock):
    from ambari_commons.constants import UPGRADE_TYPE_ROLLING

    get_stack_version_mock.return_value = '2.3.0.0-1234'

    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    
    version = "2.3.0.0-1234"
    json_content['commandParams']['version'] = version
    json_content['hostLevelParams']['stack_name'] = "HDP"
    json_content['hostLevelParams']['stack_version'] = "2.3"
    json_content['role'] = "HIVE_SERVER"
    json_content['configurations']['hive-site']['javax.jdo.option.ConnectionPassword'] = "aaa"

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "start",
                       command_args = [UPGRADE_TYPE_ROLLING],
                       config_dict = json_content,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertResourceCalled('Directory', '/etc/hive',
                              mode = 0755)

    self.assertResourceCalled('Directory', '/usr/hdp/current/hive-server2/conf',
                              owner = 'hive',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755)

    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
                              group = 'hadoop',
                              conf_dir = '/usr/hdp/current/hive-server2/conf',
                              mode = 0644,
                              configuration_attributes = {u'final': {u'mapred.healthChecker.script.path': u'true',
                                                                     u'mapreduce.jobtracker.staging.root.dir': u'true'}},
                              owner = 'hive',
                              configurations = self.getConfig()['configurations']['mapred-site'])

    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/hive-default.xml.template',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0644,
                              )

    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/hive-env.sh.template',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0644)

    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/hive-exec-log4j.properties',
      content = 'log4jproperties\nline2',
      mode = 420,
      group = 'hadoop',
      owner = 'hive')

    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/hive-log4j.properties',
      content = 'log4jproperties\nline2',
      mode = 420,
      group = 'hadoop',
      owner = 'hive')

    self.assertResourceCalled('XmlConfig', 'hive-site.xml',
                              group = 'hadoop',
                              conf_dir = '/usr/hdp/current/hive-server2/conf/conf.server',
                              mode = 0600,
                              configuration_attributes = {u'final': {u'hive.optimize.bucketmapjoin.sortedmerge': u'true',
                                                                     u'javax.jdo.option.ConnectionDriverName': u'true',
                                                                     u'javax.jdo.option.ConnectionPassword': u'true'}},
                              owner = 'hive',
                              configurations = self.getConfig()['configurations']['hive-site'])

    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/conf.server/hive-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['hive-env']['content']),
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0600)

    self.assertResourceCalled('Directory', '/etc/security/limits.d',
                              owner = 'root',
                              group = 'root',
                              create_parents = True)

    self.assertResourceCalled('File', '/etc/security/limits.d/hive.conf',
                              content = Template('hive.conf.j2'),
                              owner = 'root',
                              group = 'root',
                              mode = 0644)

    self.assertResourceCalled('File', '/usr/lib/ambari-agent/DBConnectionVerification.jar',
        content = DownloadSource('http://c6401.ambari.apache.org:8080/resources/DBConnectionVerification.jar'),
        mode = 0644,
    )
    
    self.assertResourceCalled('Directory', '/var/run/hive',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0755,
                              create_parents = True,
                              cd_access = 'a')

    self.assertResourceCalled('Directory', '/var/log/hive',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0755,
                              create_parents = True,
                              cd_access = 'a')

    self.assertResourceCalled('Directory', '/var/lib/hive',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0755,
                              create_parents = True,
                              cd_access = 'a')
    
    self.assertResourceCalled('Execute', ('cp',
                                          '--remove-destination',
                                          '/usr/share/java/mysql-connector-java.jar',
                                          '/usr/hdp/current/hive-server2/lib/mysql-connector-java.jar'),
                              path = ['/bin', '/usr/bin/'],
                              sudo = True)

    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/lib/mysql-connector-java.jar',
        mode = 0644)

    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/conf.server/hadoop-metrics2-hivemetastore.properties',
                              owner = 'hive',
                              group = 'hadoop',
                              content = Template('hadoop-metrics2-hivemetastore.properties.j2'),
                              mode = 0600,
                              )
    self.assertResourceCalled('File', '/tmp/start_metastore_script',
                              content = StaticFile('startMetastore.sh'),
                              mode = 0755)

    self.maxDiff = None

    self.assertResourceCalled('Execute', 'export HIVE_CONF_DIR=/usr/hdp/current/hive-server2/conf/conf.server ; /usr/hdp/current/hive-server2/bin/schematool -initSchema -dbType mysql -userName hive -passWord aaa -verbose',
        not_if = "ambari-sudo.sh su hive -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]export HIVE_CONF_DIR=/usr/hdp/current/hive-server2/conf/conf.server ; /usr/hdp/current/hive-server2/bin/schematool -info -dbType mysql -userName hive -passWord aaa -verbose'",
        user = 'hive')

    self.assertResourceCalled('Execute', '/tmp/start_metastore_script /var/log/hive/hive.out /var/log/hive/hive.err /var/run/hive/hive.pid /usr/hdp/current/hive-server2/conf/conf.server /var/log/hive',
        environment = {'HADOOP_HOME': '/usr/hdp/2.3.0.0-1234/hadoop', 'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45', 'HIVE_BIN': '/usr/hdp/current/hive-server2/bin/hive'},
        not_if = None,
        user = 'hive',
        path = ['/bin:/usr/hdp/current/hive-server2/bin:/usr/hdp/current/hadoop-client/bin'])

    self.assertResourceCalled('Execute', '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/hdp/current/hive-server2/lib/mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6402.ambari.apache.org/hive?createDatabaseIfNotExist=true\' hive aaa com.mysql.jdbc.Driver',
        path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
        tries = 5,
        try_sleep = 10)

    self.assertNoMoreResources()
