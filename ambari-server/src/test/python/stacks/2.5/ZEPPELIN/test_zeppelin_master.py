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
from ambari_commons import OSCheck
'''

from stacks.utils.RMFTestCase import *
from mock.mock import MagicMock, patch, call
import time
from resource_management.core import sudo
import glob

@patch.object(glob, "glob", new = MagicMock(return_value=["/tmp"]))
@patch.object(sudo, "read_file", new = MagicMock(return_value='{"interpreterSettings":[]}'))
class TestZeppelinMaster(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "ZEPPELIN/0.6.0.2.5/package"
  STACK_VERSION = "2.5"

  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/var/log/zeppelin',
        owner = 'zeppelin',
        group = 'zeppelin',
        create_parents = True,
        mode = 0755,
        cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/run/zeppelin',
        owner = 'zeppelin',
        create_parents = True,
        group = 'zeppelin',
        mode = 0755,
        cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/usr/hdp/current/zeppelin-server',
        owner = 'zeppelin',
        group = 'zeppelin',
        create_parents = True,
        mode = 0755,
        cd_access = 'a',
    )
    self.assertResourceCalled('XmlConfig', 'zeppelin-site.xml',
        owner = 'zeppelin',
        group = 'zeppelin',
        conf_dir = '/etc/zeppelin/conf',
        configurations = self.getConfig()['configurations']['zeppelin-config'],
    )
    self.assertResourceCalled('File', '/etc/zeppelin/conf/zeppelin-env.sh',
        owner = 'zeppelin',
        content = InlineTemplate(self.getConfig()['configurations']['zeppelin-env']['zeppelin_env_content']),
        group = 'zeppelin',
    )
    self.assertResourceCalled('File', '/etc/zeppelin/conf/shiro.ini',
        owner = 'zeppelin',
        content = InlineTemplate(self.getConfig()['configurations']['zeppelin-env']['shiro_ini_content']),
        group = 'zeppelin',
    )
    self.assertResourceCalled('File', '/etc/zeppelin/conf/log4j.properties',
        owner = 'zeppelin',
        content = '\nlog4j.rootLogger = INFO, dailyfile\nlog4j.appender.stdout = org.apache.log4j.ConsoleAppender\nlog4j.appender.stdout.layout = org.apache.log4j.PatternLayout\nlog4j.appender.stdout.layout.ConversionPattern=%5p [%d] ({%t} %F[%M]:%L) - %m%n\nlog4j.appender.dailyfile.DatePattern=.yyyy-MM-dd\nlog4j.appender.dailyfile.Threshold = INFO\nlog4j.appender.dailyfile = org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.dailyfile.File = ${zeppelin.log.file}\nlog4j.appender.dailyfile.layout = org.apache.log4j.PatternLayout\nlog4j.appender.dailyfile.layout.ConversionPattern=%5p [%d] ({%t} %F[%M]:%L) - %m%n',
        group = 'zeppelin',
    )
    self.assertResourceCalled('File', '/etc/zeppelin/conf/hive-site.xml',
        owner = 'zeppelin',
        content = StaticFile('/etc/spark/conf/hive-site.xml'),
        group = 'zeppelin',
    )
 
  def assert_configure_secured(self):
    self.assertResourceCalled('Directory', '/var/log/zeppelin',
        owner = 'zeppelin',
        group = 'zeppelin',
        create_parents = True,
        mode = 0755,
        cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/run/zeppelin',
        owner = 'zeppelin',
        create_parents = True,
        group = 'zeppelin',
        mode = 0755,
        cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/usr/hdp/current/zeppelin-server',
        owner = 'zeppelin',
        group = 'zeppelin',
        create_parents = True,
        mode = 0755,
        cd_access = 'a',
    )
    self.assertResourceCalled('XmlConfig', 'zeppelin-site.xml',
        owner = 'zeppelin',
        group = 'zeppelin',
        conf_dir = '/etc/zeppelin/conf',
        configurations = self.getConfig()['configurations']['zeppelin-config'],
    )
    self.assertResourceCalled('File', '/etc/zeppelin/conf/zeppelin-env.sh',
        owner = 'zeppelin',
        content = InlineTemplate(self.getConfig()['configurations']['zeppelin-env']['zeppelin_env_content']),
        group = 'zeppelin',
    )
    self.assertResourceCalled('File', '/etc/zeppelin/conf/shiro.ini',
        owner = 'zeppelin',
        content = InlineTemplate(self.getConfig()['configurations']['zeppelin-env']['shiro_ini_content']),
        group = 'zeppelin',
    )
    self.assertResourceCalled('File', '/etc/zeppelin/conf/log4j.properties',
        owner = 'zeppelin',
        content = '\nlog4j.rootLogger = INFO, dailyfile\nlog4j.appender.stdout = org.apache.log4j.ConsoleAppender\nlog4j.appender.stdout.layout = org.apache.log4j.PatternLayout\nlog4j.appender.stdout.layout.ConversionPattern=%5p [%d] ({%t} %F[%M]:%L) - %m%n\nlog4j.appender.dailyfile.DatePattern=.yyyy-MM-dd\nlog4j.appender.dailyfile.Threshold = INFO\nlog4j.appender.dailyfile = org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.dailyfile.File = ${zeppelin.log.file}\nlog4j.appender.dailyfile.layout = org.apache.log4j.PatternLayout\nlog4j.appender.dailyfile.layout.ConversionPattern=%5p [%d] ({%t} %F[%M]:%L) - %m%n',
        group = 'zeppelin',
    )
    self.assertResourceCalled('File', '/etc/zeppelin/conf/hive-site.xml',
        owner = 'zeppelin',
        content = StaticFile('/etc/spark/conf/hive-site.xml'),
        group = 'zeppelin',
    )

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/master.py",
                       classname = "Master",
                       command = "configure",
                       config_file = "default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_configure_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/master.py",
                       classname = "Master",
                       command = "configure",
                       config_file = "secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()
    
  def test_stop_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/master.py",
                       classname = "Master",
                       command = "stop",
                       config_file = "secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Directory', '/var/log/zeppelin',
        owner = 'zeppelin',
        group = 'zeppelin',
        create_parents = True,
        mode = 0755,
        cd_access = 'a',
    )
    self.assertResourceCalled('Execute', '/usr/hdp/current/zeppelin-server/bin/zeppelin-daemon.sh stop >> /var/log/zeppelin/zeppelin-setup.log',
        user = 'zeppelin',
    )
    self.assertNoMoreResources()
 
  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/master.py",
                       classname = "Master",
                       command = "stop",
                       config_file = "default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Directory', '/var/log/zeppelin',
        owner = 'zeppelin',
        group = 'zeppelin',
        create_parents = True,
        mode = 0755,
        cd_access = 'a',
    )
    self.assertResourceCalled('Execute', '/usr/hdp/current/zeppelin-server/bin/zeppelin-daemon.sh stop >> /var/log/zeppelin/zeppelin-setup.log',
        user = 'zeppelin',
    )
    self.assertNoMoreResources()
    

  def test_start_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/master.py",
                       classname = "Master",
                       command = "start",
                       config_file = "secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertResourceCalled('Execute', ('chown', '-R', u'zeppelin:zeppelin', '/etc/zeppelin'),
        sudo = True,
    )
    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/zeppelin.server.kerberos.keytab zeppelin@EXAMPLE.COM; ',
        user = 'zeppelin',
    )
    self.assertResourceCalled('HdfsResource', '/user/zeppelin',
        security_enabled = True,
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        keytab = UnknownConfigurationMock(),
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_resource_ignore_file = '/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hdfs_site = {u'a': u'b'},
        kinit_path_local = '/usr/bin/kinit',
        principal_name = UnknownConfigurationMock(),
        user = 'hdfs',
        owner = 'zeppelin',
        recursive_chown = True,
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
        type = 'directory',
        action = ['create_on_execute'],
        recursive_chmod = True,
    )
    self.assertResourceCalled('HdfsResource', '/user/zeppelin/test',
        security_enabled = True,
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        keytab = UnknownConfigurationMock(),
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_resource_ignore_file = '/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hdfs_site = {u'a': u'b'},
        kinit_path_local = '/usr/bin/kinit',
        principal_name = UnknownConfigurationMock(),
        user = 'hdfs',
        owner = 'zeppelin',
        recursive_chown = True,
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
        type = 'directory',
        action = ['create_on_execute'],
        recursive_chmod = True,
    )
    self.assertResourceCalled('HdfsResource', '/apps/zeppelin',
        security_enabled = True,
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        keytab = UnknownConfigurationMock(),
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_resource_ignore_file = '/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hdfs_site = {u'a': u'b'},
        kinit_path_local = '/usr/bin/kinit',
        principal_name = UnknownConfigurationMock(),
        user = 'hdfs',
        owner = 'zeppelin',
        recursive_chown = True,
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
        type = 'directory',
        action = ['create_on_execute'],
        recursive_chmod = True,
    )
    self.assertResourceCalled('HdfsResource', '/apps/zeppelin/tmp',
        security_enabled = True,
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        keytab = UnknownConfigurationMock(),
        source = '/tmp',
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        replace_existing_files = True,
        hdfs_resource_ignore_file = '/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hdfs_site = {u'a': u'b'},
        kinit_path_local = '/usr/bin/kinit',
        principal_name = UnknownConfigurationMock(),
        user = 'hdfs',
        owner = 'zeppelin',
        group = 'zeppelin',
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
        type = 'file',
        action = ['create_on_execute'],
        mode = 0444,
    )
    self.assertResourceCalled('HdfsResource', None,
        security_enabled = True,
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        keytab = UnknownConfigurationMock(),
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_resource_ignore_file = '/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hdfs_site = {u'a': u'b'},
        kinit_path_local = '/usr/bin/kinit',
        principal_name = UnknownConfigurationMock(),
        user = 'hdfs',
        action = ['execute'],
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
    )
    self.assertResourceCalled('File', '/etc/zeppelin/conf/interpreter.json',
        content = '{\n  "interpreterSettings": []\n}',
        owner = 'zeppelin',
        group = 'zeppelin',
    )
    self.assertResourceCalled('Execute', '/usr/hdp/current/zeppelin-server/bin/zeppelin-daemon.sh restart >> /var/log/zeppelin/zeppelin-setup.log',
        user = 'zeppelin',
    )
    self.assertNoMoreResources()
 
  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/master.py",
                       classname = "Master",
                       command = "start",
                       config_file = "default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertResourceCalled('Execute', ('chown', '-R', u'zeppelin:zeppelin', '/etc/zeppelin'),
        sudo = True,
    )
  def test_start_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/master.py",
                       classname = "Master",
                       command = "start",
                       config_file = "secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertResourceCalled('Execute', ('chown', '-R', u'zeppelin:zeppelin', '/etc/zeppelin'),
        sudo = True,
    )
    self.assertResourceCalled('Execute', ('chown', '-R', 'zeppelin:zeppelin', '/usr/hdp/current/zeppelin-server/notebook'),
        sudo = True,
    )
    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/zeppelin.server.kerberos.keytab zeppelin@EXAMPLE.COM; ',
        user = 'zeppelin',
    )
    self.assertResourceCalled('HdfsResource', '/user/zeppelin',
        security_enabled = True,
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        keytab = UnknownConfigurationMock(),
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_resource_ignore_file = '/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hdfs_site = {u'a': u'b'},
        kinit_path_local = '/usr/bin/kinit',
        principal_name = UnknownConfigurationMock(),
        user = 'hdfs',
        owner = 'zeppelin',
        recursive_chown = True,
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
        type = 'directory',
        action = ['create_on_execute'],
        recursive_chmod = True,
    )
    self.assertResourceCalled('HdfsResource', '/user/zeppelin/test',
        security_enabled = True,
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        keytab = UnknownConfigurationMock(),
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_resource_ignore_file = '/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hdfs_site = {u'a': u'b'},
        kinit_path_local = '/usr/bin/kinit',
        principal_name = UnknownConfigurationMock(),
        user = 'hdfs',
        owner = 'zeppelin',
        recursive_chown = True,
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
        type = 'directory',
        action = ['create_on_execute'],
        recursive_chmod = True,
    )
    self.assertResourceCalled('HdfsResource', '/apps/zeppelin',
        security_enabled = True,
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        keytab = UnknownConfigurationMock(),
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_resource_ignore_file = '/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hdfs_site = {u'a': u'b'},
        kinit_path_local = '/usr/bin/kinit',
        principal_name = UnknownConfigurationMock(),
        user = 'hdfs',
        owner = 'zeppelin',
        recursive_chown = True,
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
        type = 'directory',
        action = ['create_on_execute'],
        recursive_chmod = True,
    )
    self.assertResourceCalled('HdfsResource', '/apps/zeppelin/tmp',
        security_enabled = True,
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        keytab = UnknownConfigurationMock(),
        source = '/tmp',
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        replace_existing_files = True,
        hdfs_resource_ignore_file = '/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hdfs_site = {u'a': u'b'},
        kinit_path_local = '/usr/bin/kinit',
        principal_name = UnknownConfigurationMock(),
        user = 'hdfs',
        owner = 'zeppelin',
        group = 'zeppelin',
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
        type = 'file',
        action = ['create_on_execute'],
        mode = 0444,
    )
    self.assertResourceCalled('HdfsResource', None,
        security_enabled = True,
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        keytab = UnknownConfigurationMock(),
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_resource_ignore_file = '/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hdfs_site = {u'a': u'b'},
        kinit_path_local = '/usr/bin/kinit',
        principal_name = UnknownConfigurationMock(),
        user = 'hdfs',
        action = ['execute'],
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
    )
    self.assertResourceCalled('File', '/etc/zeppelin/conf/interpreter.json',
        content = '{\n  "interpreterSettings": []\n}',
        owner = 'zeppelin',
        group = 'zeppelin',
    )
    self.assertResourceCalled('Execute', '/usr/hdp/current/zeppelin-server/bin/zeppelin-daemon.sh restart >> /var/log/zeppelin/zeppelin-setup.log',
        user = 'zeppelin',
    )
    self.assertNoMoreResources()
    
    
    