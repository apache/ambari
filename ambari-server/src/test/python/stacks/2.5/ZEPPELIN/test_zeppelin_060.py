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
import interpreter_json_generated

@patch.object(glob, "glob", new = MagicMock(return_value=["/tmp"]))
@patch.object(sudo, "read_file", new = MagicMock(return_value=interpreter_json_generated.template))
class TestZeppelin060(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "ZEPPELIN/0.6.0/package"
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
    self.assertResourceCalled('Execute', ('chown', '-R', u'zeppelin:zeppelin', '/var/run/zeppelin'), sudo = True)
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
        content = InlineTemplate(self.getConfig()['configurations']['zeppelin-shiro-ini']['shiro_ini_content']),
        group = 'zeppelin',
    )
    self.assertResourceCalled('File', '/etc/zeppelin/conf/log4j.properties',
        owner = u'zeppelin',
        content = u'log4j.rootLogger = INFO, dailyfile',
        group = u'zeppelin',
    )
    self.assertResourceCalled('Directory', '/etc/zeppelin/conf/external-dependency-conf',
        owner = 'zeppelin',
        group = 'zeppelin',
        create_parents = True,
        mode = 0755,
        cd_access = 'a',
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
    self.assertResourceCalled('Execute', ('chown', '-R', u'zeppelin:zeppelin', '/var/run/zeppelin'), sudo = True)
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
        content = InlineTemplate(self.getConfig()['configurations']['zeppelin-shiro-ini']['shiro_ini_content']),
        group = 'zeppelin',
    )
    self.assertResourceCalled('File', '/etc/zeppelin/conf/log4j.properties',
        owner = u'zeppelin',
        content = u'log4j.rootLogger = INFO, dailyfile',
        group = u'zeppelin',
    )
    self.assertResourceCalled('Directory', '/etc/zeppelin/conf/external-dependency-conf',
        owner = 'zeppelin',
        group = 'zeppelin',
        create_parents = True,
        mode = 0755,
        cd_access = 'a',
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
    self.assertResourceCalled('Execute', ('chown', '-R', u'zeppelin:zeppelin', '/var/run/zeppelin'),
        sudo = True,
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
    self.assertResourceCalled('Execute', ('chown', '-R', u'zeppelin:zeppelin', '/var/run/zeppelin'),
        sudo = True,
    )
    self.assertResourceCalled('Execute', '/usr/hdp/current/zeppelin-server/bin/zeppelin-daemon.sh stop >> /var/log/zeppelin/zeppelin-setup.log',
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

  @patch('os.path.exists', return_value = True)
  def test_start_secured(self, os_path_exists_mock):
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
        hadoop_bin_dir = '/usr/hdp/2.2.1.0-2067/hadoop/bin',
        keytab = UnknownConfigurationMock(),
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_resource_ignore_file = '/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hdfs_site = {u'a': u'b'},
        kinit_path_local = '/usr/bin/kinit',
        principal_name = UnknownConfigurationMock(),
        user = 'hdfs',
        owner = 'zeppelin',
        recursive_chown = True,
        hadoop_conf_dir = '/usr/hdp/2.2.1.0-2067/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'],
        recursive_chmod = True,
    )
    self.assertResourceCalled('HdfsResource', '/user/zeppelin/test',
        security_enabled = True,
        hadoop_bin_dir = '/usr/hdp/2.2.1.0-2067/hadoop/bin',
        keytab = UnknownConfigurationMock(),
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_resource_ignore_file = '/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hdfs_site = {u'a': u'b'},
        kinit_path_local = '/usr/bin/kinit',
        principal_name = UnknownConfigurationMock(),
        user = 'hdfs',
        owner = 'zeppelin',
        recursive_chown = True,
        hadoop_conf_dir = '/usr/hdp/2.2.1.0-2067/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'],
        recursive_chmod = True,
    )
    self.assertResourceCalled('HdfsResource', '/apps/zeppelin',
        security_enabled = True,
        hadoop_bin_dir = '/usr/hdp/2.2.1.0-2067/hadoop/bin',
        keytab = UnknownConfigurationMock(),
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_resource_ignore_file = '/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hdfs_site = {u'a': u'b'},
        kinit_path_local = '/usr/bin/kinit',
        principal_name = UnknownConfigurationMock(),
        user = 'hdfs',
        owner = 'zeppelin',
        recursive_chown = True,
        hadoop_conf_dir = '/usr/hdp/2.2.1.0-2067/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'],
        recursive_chmod = True,
    )
    self.assertResourceCalled('HdfsResource', '/apps/zeppelin/tmp',
        security_enabled = True,
        hadoop_bin_dir = '/usr/hdp/2.2.1.0-2067/hadoop/bin',
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
        hadoop_conf_dir = '/usr/hdp/2.2.1.0-2067/hadoop/conf',
        type = 'file',
        action = ['create_on_execute'],
        mode = 0444,
    )
    self.assertResourceCalled('HdfsResource', None,
        security_enabled = True,
        hadoop_bin_dir = '/usr/hdp/2.2.1.0-2067/hadoop/bin',
        keytab = UnknownConfigurationMock(),
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_resource_ignore_file = '/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hdfs_site = {u'a': u'b'},
        kinit_path_local = '/usr/bin/kinit',
        principal_name = UnknownConfigurationMock(),
        user = 'hdfs',
        action = ['execute'],
        hadoop_conf_dir = '/usr/hdp/2.2.1.0-2067/hadoop/conf',
    )
    self.assertResourceCalled('File', '/etc/zeppelin/conf/interpreter.json',
        content=interpreter_json_generated.template_after_base,
        owner = 'zeppelin',
        group = 'zeppelin',
    )
    self.assertResourceCalled('File', '/etc/zeppelin/conf/interpreter.json',
        content=interpreter_json_generated.template_after_without_spark_and_livy,
        owner = 'zeppelin',
        group = 'zeppelin')
    self.assertResourceCalled('File', '/etc/zeppelin/conf/interpreter.json',
                              content=interpreter_json_generated.template_after_kerberos,
                              owner = 'zeppelin',
                              group = 'zeppelin')
    self.assertResourceCalled('Execute', '/usr/hdp/current/zeppelin-server/bin/zeppelin-daemon.sh restart >> /var/log/zeppelin/zeppelin-setup.log',
        user = 'zeppelin'
    )
    self.assertNoMoreResources()

