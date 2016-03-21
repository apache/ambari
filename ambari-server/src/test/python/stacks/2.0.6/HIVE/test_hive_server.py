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
import socket
import subprocess

from stacks.utils.RMFTestCase import *

from mock.mock import MagicMock, patch
from resource_management.libraries.functions import version
from resource_management.core import shell
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import copy_tarball
from resource_management.libraries import functions
from resource_management.core.logger import Logger

@patch.object(functions, "get_hdp_version", new = MagicMock(return_value="2.0.0.0-1234"))
@patch("resource_management.libraries.functions.check_thrift_port_sasl", new=MagicMock())
@patch("resource_management.libraries.functions.get_user_call_output.get_user_call_output", new=MagicMock(return_value=(0,'123','')))
class TestHiveServer(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "HIVE/0.12.0.2.0/package"
  STACK_VERSION = "2.0.6"
  UPGRADE_STACK_VERSION = "2.2"
  DEFAULT_IMMUTABLE_PATHS = ['/apps/hive/warehouse', '/apps/falcon', '/mr-history/done', '/app-logs', '/tmp']

  def setUp(self):
    Logger.logger = MagicMock()

  @patch("resource_management.libraries.functions.copy_tarball.copy_to_hdfs")
  @patch.object(Script, "is_hdp_stack_greater_or_equal", new = MagicMock(return_value=False))
  def test_configure_default(self, copy_to_hdfs_mock):
    copy_to_hdfs_mock.return_value = True
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "configure",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  @patch("resource_management.libraries.functions.copy_tarball.copy_to_hdfs")
  @patch("socket.socket")
  @patch.object(Script, "is_hdp_stack_greater_or_equal", new = MagicMock(return_value=False))
  def test_start_default(self, socket_mock, copy_to_hfds_mock):
    copy_to_hfds_mock.return_value = None
    s = socket_mock.return_value
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname="HiveServer",
                       command="start",
                       config_file="default.json",
                       hdp_stack_version=self.STACK_VERSION,
                       target=RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_default()

    self.assertResourceCalled('Execute',
                              'hive --config /etc/hive/conf.server --service metatool -updateLocation hdfs://c6401.ambari.apache.org:8020 OK.',
                              environment={'PATH': '/bin:/usr/lib/hive/bin:/usr/bin'},
                              user='hive'
    )
    self.assertResourceCalled('Execute', '/tmp/start_hiveserver2_script /var/log/hive/hive-server2.out /var/log/hive/hive-server2.log /var/run/hive/hive-server.pid /etc/hive/conf.server /var/log/hive',
        environment = {'HADOOP_HOME': '/usr',
           'HIVE_BIN': 'hive',
           'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        not_if = "ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1",
        user = 'hive',
        path = ['/bin:/usr/lib/hive/bin:/usr/bin'],
    )
    self.assertResourceCalled('Execute',
                              '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/lib/hive/lib//mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6402.ambari.apache.org/hive?createDatabaseIfNotExist=true\' hive \'!`"\'"\'"\' 1\' com.mysql.jdbc.Driver',
                              path=['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
                              tries=5,
                              try_sleep=10
    )
    self.assertNoMoreResources()

  @patch("resource_management.libraries.functions.copy_tarball.copy_to_hdfs")
  @patch("socket.socket")
  @patch.object(Script, "is_hdp_stack_greater_or_equal", new = MagicMock(return_value=False))
  def test_start_default_non_hdfs(self, socket_mock, copy_to_hfds_mock):
    copy_to_hfds_mock.return_value = None
    s = socket_mock.return_value
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname="HiveServer",
                       command="start",
                       config_file="default_hive_non_hdfs.json",
                       hdp_stack_version=self.STACK_VERSION,
                       target=RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_default(default_fs_default='hcfs://c6401.ambari.apache.org:8020')

    self.assertResourceCalled('Execute', '/tmp/start_hiveserver2_script /var/log/hive/hive-server2.out /var/log/hive/hive-server2.log /var/run/hive/hive-server.pid /etc/hive/conf.server /var/log/hive',
                              environment = {'HADOOP_HOME': '/usr',
                                             'HIVE_BIN': 'hive',
                                             'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
                              not_if = "ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1",
                              user = 'hive',
                              path = ['/bin:/usr/lib/hive/bin:/usr/bin'],
                              )
    self.assertResourceCalled('Execute',
                              '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/lib/hive/lib//mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6402.ambari.apache.org/hive?createDatabaseIfNotExist=true\' hive \'!`"\'"\'"\' 1\' com.mysql.jdbc.Driver',
                              path=['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
                              tries=5,
                              try_sleep=10
    )
    self.assertNoMoreResources()

  @patch.object(Script, "is_hdp_stack_greater_or_equal", new = MagicMock(return_value=False))
  def test_start_default_no_copy(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "start",
                       config_file="default_no_install.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_default()

    self.assertResourceCalled('Execute', 'hive --config /etc/hive/conf.server --service metatool -updateLocation hdfs://c6401.ambari.apache.org:8020 OK.',
                              environment = {'PATH': '/bin:/usr/lib/hive/bin:/usr/bin'},
                              user = 'hive',
                              )
    self.assertResourceCalled('Execute', '/tmp/start_hiveserver2_script /var/log/hive/hive-server2.out /var/log/hive/hive-server2.log /var/run/hive/hive-server.pid /etc/hive/conf.server /var/log/hive',
        environment = {'HADOOP_HOME': '/usr',
           'HIVE_BIN': 'hive',
           'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        not_if = "ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1",
        user = 'hive',
        path = ['/bin:/usr/lib/hive/bin:/usr/bin'],
    )

    self.assertResourceCalled('Execute', '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/lib/hive/lib//mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6402.ambari.apache.org/hive?createDatabaseIfNotExist=true\' hive \'!`"\'"\'"\' 1\' com.mysql.jdbc.Driver',
                              path=['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'], tries=5, try_sleep=10
    )
    self.assertNoMoreResources()

  @patch("resource_management.libraries.functions.copy_tarball.copy_to_hdfs")
  @patch.object(Script, "is_hdp_stack_greater_or_equal", new = MagicMock(return_value=False))
  def test_start_default_alt_tmp(self, copy_to_hfds_mock):
    copy_to_hfds_mock.return_value = None
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "start",
                       config_file="default_hive_nn_ha.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_default(no_tmp=True)

    self.assertResourceCalled('Execute', 'hive --config /etc/hive/conf.server --service metatool -updateLocation hdfs://c6401.ambari.apache.org:8020 OK.',
                              environment = {'PATH': '/bin:/usr/lib/hive/bin:/usr/bin'},
                              user = 'hive',
                              )
    self.assertResourceCalled('Execute', '/tmp/start_hiveserver2_script /var/log/hive/hive-server2.out /var/log/hive/hive-server2.log /var/run/hive/hive-server.pid /etc/hive/conf.server /var/log/hive',
        environment = {'HADOOP_HOME': '/usr',
           'HIVE_BIN': 'hive',
           'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        not_if = "ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1",
        user = 'hive',
        path = ['/bin:/usr/lib/hive/bin:/usr/bin'],
    )

    self.assertResourceCalled('Execute', '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/lib/hive/lib//mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6402.ambari.apache.org/hive?createDatabaseIfNotExist=true\' hive \'!`"\'"\'"\' 1\' com.mysql.jdbc.Driver',
                              path=['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'], tries=5, try_sleep=10
    )
    self.assertNoMoreResources()

  @patch("resource_management.libraries.functions.copy_tarball.copy_to_hdfs")
  @patch.object(Script, "is_hdp_stack_greater_or_equal", new = MagicMock(return_value=False))
  def test_start_default_alt_nn_ha_tmp(self, copy_to_hfds_mock):
    copy_to_hfds_mock.return_value = None
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "start",
                       config_file="default_hive_nn_ha_2.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_default(no_tmp=True)

    self.assertResourceCalled('Execute', 'hive --config /etc/hive/conf.server --service metatool -updateLocation hdfs://c6401.ambari.apache.org:8020 OK.',
                              environment = {'PATH': '/bin:/usr/lib/hive/bin:/usr/bin'},
                              user = 'hive',
                              )
    self.assertResourceCalled('Execute', '/tmp/start_hiveserver2_script /var/log/hive/hive-server2.out /var/log/hive/hive-server2.log /var/run/hive/hive-server.pid /etc/hive/conf.server /var/log/hive',
        environment = {'HADOOP_HOME': '/usr',
           'HIVE_BIN': 'hive',
           'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        not_if = "ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1",
        user = 'hive',
        path = ['/bin:/usr/lib/hive/bin:/usr/bin'],
    )

    self.assertResourceCalled('Execute', '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/lib/hive/lib//mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6402.ambari.apache.org/hive?createDatabaseIfNotExist=true\' hive \'!`"\'"\'"\' 1\' com.mysql.jdbc.Driver',
                              path=['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'], tries=5, try_sleep=10
    )
    self.assertNoMoreResources()

  @patch.object(Script, "is_hdp_stack_greater_or_equal", new = MagicMock(return_value=False))
  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "stop",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Execute', "ambari-sudo.sh kill 123",
        not_if = "! (ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1)",
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh kill -9 123",
        not_if = "! (ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1) || ( sleep 5 && ! (ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1) )",
    )
    self.assertResourceCalled('Execute', "! (ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1)",
        tries = 20,
        try_sleep = 3,
    )
    self.assertResourceCalled('File', '/var/run/hive/hive-server.pid',
      action = ['delete'],
    )
    
    self.assertNoMoreResources()

  @patch.object(Script, "is_hdp_stack_greater_or_equal", new = MagicMock(return_value=False))
  def test_configure_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "configure",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()

  @patch("resource_management.libraries.functions.copy_tarball.copy_to_hdfs")
  @patch("hive_service.check_fs_root")
  @patch("socket.socket")
  @patch.object(Script, "is_hdp_stack_greater_or_equal", new = MagicMock(return_value=False))
  def test_start_secured(self, socket_mock, check_fs_root_mock, copy_to_hfds_mock):
    s = socket_mock.return_value
    copy_to_hfds_mock.return_value = None

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "start",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_secured()
    self.assertResourceCalled('Execute',
                              '/usr/bin/kinit -kt /etc/security/keytabs/hive.service.keytab hive/c6401.ambari.apache.org@EXAMPLE.COM; ',
                              user = 'hive',
                              )
    self.assertResourceCalled('Execute', '/tmp/start_hiveserver2_script /var/log/hive/hive-server2.out /var/log/hive/hive-server2.log /var/run/hive/hive-server.pid /etc/hive/conf.server /var/log/hive',
        environment = {'HADOOP_HOME': '/usr',
           'HIVE_BIN': 'hive',
           'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        not_if = "ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1",
        user = 'hive',
        path = ['/bin:/usr/lib/hive/bin:/usr/bin'],
    )
    self.assertResourceCalled('Execute',
                              '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/lib/hive/lib//mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6402.ambari.apache.org/hive?createDatabaseIfNotExist=true\' hive \'!`"\'"\'"\' 1\' com.mysql.jdbc.Driver',
                              path=['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
                              tries=5,
                              try_sleep=10,
    )
    self.assertNoMoreResources()

    self.assertTrue(check_fs_root_mock.called)


  @patch("socket.socket")
  @patch.object(Script, "is_hdp_stack_greater_or_equal", new = MagicMock(return_value=False))
  def test_stop_secured(self, socket_mock):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "stop",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute',
                              '/usr/bin/kinit -kt /etc/security/keytabs/hive.service.keytab hive/c6401.ambari.apache.org@EXAMPLE.COM; ',
                              user = 'hive',
                              )

    self.assertResourceCalled('Execute', "ambari-sudo.sh kill 123",
        not_if = "! (ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1)",
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh kill -9 123",
        not_if = "! (ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1) || ( sleep 5 && ! (ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1) )",
    )
    self.assertResourceCalled('Execute', "! (ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1)",
        tries = 20,
        try_sleep = 3,
    )
    self.assertResourceCalled('File', '/var/run/hive/hive-server.pid',
      action = ['delete'],
    )
    
    self.assertNoMoreResources()

  def assert_configure_default(self, no_tmp = False, default_fs_default='hdfs://c6401.ambari.apache.org:8020'):
    # Verify creating of Hcat and Hive directories
    self.assertResourceCalled('HdfsResource', '/apps/webhcat',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'hcat',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='missing_principal', default_fs=default_fs_default,
        mode = 0755,
    )
    self.assertResourceCalled('HdfsResource', '/user/hcat',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'hcat',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='missing_principal', default_fs=default_fs_default,
        mode = 0755,
    )

    if self._testMethodName == "test_socket_timeout":
      # This test will not call any more resources.
      return

    self.assertResourceCalled('HdfsResource', '/apps/hive/warehouse',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'hive',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='missing_principal', default_fs=default_fs_default,
        mode = 0777,
    )
    self.assertResourceCalled('HdfsResource', '/user/hive',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'hive',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='missing_principal', default_fs=default_fs_default,
        mode = 0755,
    )
    if not no_tmp:
      self.assertResourceCalled('HdfsResource', '/custompath/tmp/hive',
          immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
          security_enabled = False,
          hadoop_conf_dir = '/etc/hadoop/conf',
          keytab = UnknownConfigurationMock(),
          kinit_path_local = '/usr/bin/kinit',
          user = 'hdfs',
          dfs_type = '',
          owner = 'hive',
          group = 'hdfs',
          hadoop_bin_dir = '/usr/bin',
          type = 'directory',
          action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='missing_principal', default_fs=default_fs_default,
          mode = 0777,
      )
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='missing_principal', default_fs=default_fs_default,
        hadoop_conf_dir = '/etc/hadoop/conf',
    )
    self.assertResourceCalled('Directory', '/etc/hive',
                              mode=0755,
    )
    self.assertResourceCalled('Directory', '/etc/hive/conf',
                              owner='hive',
                              group='hadoop',
                              recursive=True,
    )

    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
                              group='hadoop',
                              conf_dir='/etc/hive/conf',
                              mode=0644,
                              configuration_attributes={u'final': {u'mapred.healthChecker.script.path': u'true',
                                                                   u'mapreduce.jobtracker.staging.root.dir': u'true'}},
                              owner='hive',
                              configurations=self.getConfig()['configurations']['mapred-site'],
    )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-default.xml.template',
                              owner='hive',
                              group='hadoop',
    )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-env.sh.template',
                              owner='hive',
                              group='hadoop',
    )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-exec-log4j.properties',
                              content='log4jproperties\nline2',
                              owner='hive',
                              group='hadoop',
                              mode=0644,
    )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-log4j.properties',
                              content='log4jproperties\nline2',
                              owner='hive',
                              group='hadoop',
                              mode=0644,
    )
    self.assertResourceCalled('XmlConfig', 'hive-site.xml',
                              group='hadoop',
                              conf_dir='/etc/hive/conf.server',
                              mode=0644,
                              configuration_attributes={u'final': {u'hive.optimize.bucketmapjoin.sortedmerge': u'true',
                                                                   u'javax.jdo.option.ConnectionDriverName': u'true',
                                                                   u'javax.jdo.option.ConnectionPassword': u'true'}},
                              owner='hive',
                              configurations=self.getConfig()['configurations']['hive-site'],
    )
    self.assertResourceCalled('File', '/etc/hive/conf.server/hive-env.sh',
                              content=InlineTemplate(self.getConfig()['configurations']['hive-env']['content']),
                              owner='hive',
                              group='hadoop',
    )
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
                              owner='root',
                              group='root',
                              recursive=True,
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/hive.conf',
                              content=Template('hive.conf.j2'),
                              owner='root',
                              group='root',
                              mode=0644,
    )
    self.assertResourceCalled('Execute', ('cp',
                                          '--remove-destination',
                                          '/usr/share/java/mysql-connector-java.jar',
                                          '/usr/lib/hive/lib//mysql-connector-java.jar'),
                              path=['/bin', '/usr/bin/'],
                              sudo=True,
    )
    self.assertResourceCalled('File', '/usr/lib/hive/lib//mysql-connector-java.jar',
                              mode=0644,
    )
    self.assertResourceCalled('File', '/usr/lib/ambari-agent/DBConnectionVerification.jar',
                              content=DownloadSource('http://c6401.ambari.apache.org:8080/resources'
                                                     '/DBConnectionVerification.jar'),
                              mode=0644,
    )
    self.assertResourceCalled('File', '/tmp/start_hiveserver2_script',
                              content=Template('startHiveserver2.sh.j2'),
                              mode=0755,
    )
    self.assertResourceCalled('Directory', '/var/run/hive',
                              owner='hive',
                              mode=0755,
                              group='hadoop',
                              recursive=True,
                              cd_access='a',
    )
    self.assertResourceCalled('Directory', '/var/log/hive',
                              owner='hive',
                              mode=0755,
                              group='hadoop',
                              recursive=True,
                              cd_access='a',
    )
    self.assertResourceCalled('Directory', '/var/lib/hive',
                              owner='hive',
                              mode=0755,
                              group='hadoop',
                              recursive=True,
                              cd_access='a',
    )

  def assert_configure_secured(self):
    self.assertResourceCalled('HdfsResource', '/apps/webhcat',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_bin_dir = '/usr/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'hcat',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='hdfs', default_fs='hdfs://c6401.ambari.apache.org:8020',
        mode = 0755,
    )
    self.assertResourceCalled('HdfsResource', '/user/hcat',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_bin_dir = '/usr/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'hcat',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='hdfs', default_fs='hdfs://c6401.ambari.apache.org:8020',
        mode = 0755,
    )

    self.assertResourceCalled('HdfsResource', '/apps/hive/warehouse',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_bin_dir = '/usr/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'hive',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='hdfs', default_fs='hdfs://c6401.ambari.apache.org:8020',
        mode = 0777,
    )
    self.assertResourceCalled('HdfsResource', '/user/hive',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_bin_dir = '/usr/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'hive',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='hdfs', default_fs='hdfs://c6401.ambari.apache.org:8020',
        mode = 0755,
    )
    self.assertResourceCalled('HdfsResource', '/custompath/tmp/hive',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_conf_dir = '/etc/hadoop/conf',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'hive',
        group = 'hdfs',
        hadoop_bin_dir = '/usr/bin',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='hdfs', default_fs='hdfs://c6401.ambari.apache.org:8020',
        mode = 0777,
    )
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_bin_dir = '/usr/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='hdfs', default_fs='hdfs://c6401.ambari.apache.org:8020',
        hadoop_conf_dir = '/etc/hadoop/conf',
    )
    self.assertResourceCalled('Directory', '/etc/hive',
                              mode=0755,
    )
    self.assertResourceCalled('Directory', '/etc/hive/conf',
                              owner='hive',
                              group='hadoop',
                              recursive=True,
    )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
                              group='hadoop',
                              conf_dir='/etc/hive/conf',
                              mode=0644,
                              configuration_attributes={u'final': {u'mapred.healthChecker.script.path': u'true',
                                                                   u'mapreduce.jobtracker.staging.root.dir': u'true'}},
                              owner='hive',
                              configurations=self.getConfig()['configurations']['mapred-site'],
    )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-default.xml.template',
                              owner='hive',
                              group='hadoop',
    )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-env.sh.template',
                              owner='hive',
                              group='hadoop',
    )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-exec-log4j.properties',
                              content='log4jproperties\nline2',
                              owner='hive',
                              group='hadoop',
                              mode=0644,
    )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-log4j.properties',
                              content='log4jproperties\nline2',
                              owner='hive',
                              group='hadoop',
                              mode=0644,
    )
    self.assertResourceCalled('XmlConfig', 'hive-site.xml',
                              group='hadoop',
                              conf_dir='/etc/hive/conf.server',
                              mode=0644,
                              configuration_attributes={u'final': {u'hive.optimize.bucketmapjoin.sortedmerge': u'true',
                                                                   u'javax.jdo.option.ConnectionDriverName': u'true',
                                                                   u'javax.jdo.option.ConnectionPassword': u'true'}},
                              owner='hive',
                              configurations=self.getConfig()['configurations']['hive-site'],
    )
    self.assertResourceCalled('File', '/etc/hive/conf.server/hive-env.sh',
                              content=InlineTemplate(self.getConfig()['configurations']['hive-env']['content']),
                              owner='hive',
                              group='hadoop',
    )
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
                              owner='root',
                              group='root',
                              recursive=True,
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/hive.conf',
                              content=Template('hive.conf.j2'),
                              owner='root',
                              group='root',
                              mode=0644,
    )
    self.assertResourceCalled('Execute', ('cp',
                                          '--remove-destination',
                                          '/usr/share/java/mysql-connector-java.jar',
                                          '/usr/lib/hive/lib//mysql-connector-java.jar'),
                              path=['/bin', '/usr/bin/'],
                              sudo=True,
    )
    self.assertResourceCalled('File', '/usr/lib/hive/lib//mysql-connector-java.jar',
                              mode=0644,
    )
    self.assertResourceCalled('File', '/usr/lib/ambari-agent/DBConnectionVerification.jar',
                              content=DownloadSource(
                                'http://c6401.ambari.apache.org:8080/resources/DBConnectionVerification.jar'),
                              mode=0644,
    )
    self.assertResourceCalled('File', '/tmp/start_hiveserver2_script',
                              content=Template('startHiveserver2.sh.j2'),
                              mode=0755,
    )
    self.assertResourceCalled('Directory', '/var/run/hive',
                              owner='hive',
                              group='hadoop',
                              mode=0755,
                              recursive=True,
                              cd_access='a',
    )
    self.assertResourceCalled('Directory', '/var/log/hive',
                              owner='hive',
                              group='hadoop',
                              mode=0755,
                              recursive=True,
                              cd_access='a',
    )
    self.assertResourceCalled('Directory', '/var/lib/hive',
                              owner='hive',
                              group='hadoop',
                              mode=0755,
                              recursive=True,
                              cd_access='a',
    )

  @patch("time.time")
  @patch("socket.socket")
  def test_socket_timeout(self, socket_mock, time_mock):
    s = socket_mock.return_value
    s.connect = MagicMock()    
    s.connect.side_effect = socket.error("")
    
    time_mock.return_value = 1000
    
    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                           classname = "HiveServer",
                           command = "start",
                           config_file="default.json",
                           hdp_stack_version = self.STACK_VERSION,
                           target = RMFTestCase.TARGET_COMMON_SERVICES
      )
      
      self.fail("Script failure due to socket error was expected")
    except:
      self.assert_configure_default()

  @patch("resource_management.libraries.functions.copy_tarball.copy_to_hdfs")
  @patch.object(Script, "is_hdp_stack_greater_or_equal", new = MagicMock(return_value=True))
  @patch("os.path.exists", new = MagicMock(return_value=True))
  @patch("platform.linux_distribution", new = MagicMock(return_value="Linux"))
  def test_stop_during_upgrade(self, copy_to_hdfs_mock):

    hiveServerVersionOutput = """WARNING: Use "yarn jar" to launch YARN applications.
Hive 1.2.1.2.3.0.0-2434
Subversion git://ip-10-0-0-90.ec2.internal/grid/0/jenkins/workspace/HDP-dal-centos6/bigtop/build/hive/rpm/BUILD/hive-1.2.1.2.3.0.0 -r a77a00ae765a73b2957337e96ed5a0dbb2e60dfb
Compiled by jenkins on Sat Jun 20 11:50:41 EDT 2015
From source with checksum 150f554beae04f76f814f59549dead8b"""
    call_side_effects = [(0, hiveServerVersionOutput), (0, hiveServerVersionOutput)] * 4
    copy_to_hdfs_mock.return_value = True

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
     classname = "HiveServer", command = "restart", config_file = "hive-upgrade.json",
     hdp_stack_version = self.UPGRADE_STACK_VERSION,
     target = RMFTestCase.TARGET_COMMON_SERVICES,
     call_mocks = call_side_effects
    )

    self.assertResourceCalled('Execute', ('hdp-select', 'set', 'hive-server2', '2.2.1.0-2065'), sudo=True,)
    self.assertResourceCalledByIndex(31, 'Execute', 'hive --config /usr/hdp/current/hive-server2/conf/conf.server --service hiveserver2 --deregister 1.2.1.2.3.0.0-2434',
      path=['/bin:/usr/hdp/current/hive-server2/bin:/usr/hdp/current/hadoop-client/bin'],
      tries=1, user='hive')


  @patch("resource_management.libraries.functions.copy_tarball.copy_to_hdfs")
  @patch.object(Script, "is_hdp_stack_greater_or_equal", new = MagicMock(return_value=True))
  def test_stop_during_upgrade_with_default_conf_server(self, copy_to_hdfs_mock):
    hiveServerVersionOutput = """WARNING: Use "yarn jar" to launch YARN applications.
Hive 1.2.1.2.3.0.0-2434
Subversion git://ip-10-0-0-90.ec2.internal/grid/0/jenkins/workspace/HDP-dal-centos6/bigtop/build/hive/rpm/BUILD/hive-1.2.1.2.3.0.0 -r a77a00ae765a73b2957337e96ed5a0dbb2e60dfb
Compiled by jenkins on Sat Jun 20 11:50:41 EDT 2015
From source with checksum 150f554beae04f76f814f59549dead8b"""
    call_side_effects = [(0, hiveServerVersionOutput), (0, hiveServerVersionOutput)] * 4
    copy_to_hdfs_mock.return_value = True

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
     classname = "HiveServer", command = "restart", config_file = "hive-upgrade.json",
     hdp_stack_version = self.UPGRADE_STACK_VERSION,
     target = RMFTestCase.TARGET_COMMON_SERVICES,
     call_mocks = call_side_effects
    )

    self.assertResourceCalled('Execute', ('hdp-select', 'set', 'hive-server2', '2.2.1.0-2065'), sudo=True,)
    self.assertResourceCalledByIndex(33, 'Execute', 'hive --config /etc/hive/conf.server --service hiveserver2 --deregister 1.2.1.2.3.0.0-2434',
      path=['/bin:/usr/hdp/current/hive-server2/bin:/usr/hdp/current/hadoop-client/bin'],
      tries=1, user='hive')

  def test_stop_during_upgrade_bad_hive_version(self):
    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
       classname = "HiveServer", command = "restart", config_file = "hive-upgrade.json",
       hdp_stack_version = self.UPGRADE_STACK_VERSION,
       target = RMFTestCase.TARGET_COMMON_SERVICES,
       call_mocks = [(0,"BAD VERSION")])
      self.fail("Invalid hive version should have caused an exception")
    except:
      pass

    self.assertResourceCalled('Execute', ('hdp-select', 'set', 'hive-server2', '2.2.1.0-2065'), sudo=True,)
    self.assertNoMoreResources()

  @patch("resource_management.libraries.functions.security_commons.build_expectations")
  @patch("resource_management.libraries.functions.security_commons.get_params_from_filesystem")
  @patch("resource_management.libraries.functions.security_commons.validate_security_config_properties")
  @patch("resource_management.libraries.functions.security_commons.cached_kinit_executor")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  def test_security_status(self, put_structured_out_mock, cached_kinit_executor_mock, validate_security_config_mock, get_params_mock, build_exp_mock):
    # Test that function works when is called with correct parameters

    security_params = {
      'hive-site': {
        "hive.server2.authentication": "KERBEROS",
        "hive.metastore.sasl.enabled": "true",
        "hive.security.authorization.enabled": "true",
        "hive.server2.authentication.kerberos.keytab": "path/to/keytab",
        "hive.server2.authentication.kerberos.principal": "principal",
        "hive.server2.authentication.spnego.keytab": "path/to/spnego_keytab",
        "hive.server2.authentication.spnego.principal": "spnego_principal"
      }
    }
    result_issues = []
    props_value_check = {"hive.server2.authentication": "KERBEROS",
                         "hive.metastore.sasl.enabled": "true",
                         "hive.security.authorization.enabled": "true"}
    props_empty_check = ["hive.server2.authentication.kerberos.keytab",
                         "hive.server2.authentication.kerberos.principal",
                         "hive.server2.authentication.spnego.principal",
                         "hive.server2.authentication.spnego.keytab"]

    props_read_check = ["hive.server2.authentication.kerberos.keytab",
                        "hive.server2.authentication.spnego.keytab"]

    get_params_mock.return_value = security_params
    validate_security_config_mock.return_value = result_issues

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "security_status",
                       config_file="../../2.1/configs/secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    get_params_mock.assert_called_with('/etc/hive/conf', {'hive-site.xml': "XML"})
    build_exp_mock.assert_called_with('hive-site', props_value_check, props_empty_check, props_read_check)
    put_structured_out_mock.assert_called_with({"securityState": "SECURED_KERBEROS"})
    self.assertTrue(cached_kinit_executor_mock.call_count, 2)
    cached_kinit_executor_mock.assert_called_with('/usr/bin/kinit',
                                                  self.config_dict['configurations']['hive-env']['hive_user'],
                                                  security_params['hive-site']['hive.server2.authentication.spnego.keytab'],
                                                  security_params['hive-site']['hive.server2.authentication.spnego.principal'],
                                                  self.config_dict['hostname'],
                                                  '/tmp')

    # Testing that the exception throw by cached_executor is caught
    cached_kinit_executor_mock.reset_mock()
    cached_kinit_executor_mock.side_effect = Exception("Invalid command")

    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                         classname = "HiveServer",
                         command = "security_status",
                         config_file="../../2.1/configs/secured.json",
                         hdp_stack_version = self.STACK_VERSION,
                         target = RMFTestCase.TARGET_COMMON_SERVICES
      )
    except:
      self.assertTrue(True)

    # Testing with a security_params which doesn't contains startup
    empty_security_params = {}
    cached_kinit_executor_mock.reset_mock()
    get_params_mock.reset_mock()
    put_structured_out_mock.reset_mock()
    get_params_mock.return_value = empty_security_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "security_status",
                       config_file="../../2.1/configs/secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityIssuesFound": "Keytab file or principal are not set property."})

    # Testing with not empty result_issues
    result_issues_with_params = {}
    result_issues_with_params['hive-site']="Something bad happened"

    validate_security_config_mock.reset_mock()
    get_params_mock.reset_mock()
    validate_security_config_mock.return_value = result_issues_with_params
    get_params_mock.return_value = security_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "security_status",
                       config_file="../../2.1/configs/secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})

    # Testing with security_enable = false
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "security_status",
                       config_file="../../2.1/configs/default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})

  @patch.object(Script, "is_hdp_stack_greater_or_equal", new = MagicMock(return_value=True))
  @patch("resource_management.libraries.functions.copy_tarball.copy_to_hdfs")
  def test_pre_upgrade_restart(self, copy_to_hdfs_mock):
    copy_to_hdfs_mock.return_value = True

    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.2.1.0-3242'
    json_content['commandParams']['version'] = version
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)
    self.assertResourceCalled('Execute',
                              ('hdp-select', 'set', 'hive-server2', version), sudo=True,)

    copy_to_hdfs_mock.assert_any_call("mapreduce", "hadoop", "hdfs", host_sys_prepped=False)
    copy_to_hdfs_mock.assert_any_call("tez", "hadoop", "hdfs", host_sys_prepped=False)
    self.assertEquals(2, copy_to_hdfs_mock.call_count)
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='missing_principal', default_fs='hdfs://c6401.ambari.apache.org:8020',
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
    )
    self.assertNoMoreResources()

  @patch("os.path.exists")
  @patch("resource_management.core.shell.call")
  @patch.object(Script, "is_hdp_stack_greater_or_equal", new = MagicMock(return_value=True))
  @patch("resource_management.libraries.functions.copy_tarball.copy_to_hdfs")
  def test_pre_upgrade_restart_23(self, copy_to_hdfs_mock, call_mock, os_path__exists_mock):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    os_path__exists_mock.return_value = False
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version

    copy_to_hdfs_mock.return_value = True
    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = [(0, None, ''), (0, None, '')],
                       mocks_dict = mocks_dict)

    self.assertResourceCalled('Link', ('/etc/hive/conf'), to='/usr/hdp/current/hive-client/conf')
    self.assertResourceCalled('Execute',

                              ('hdp-select', 'set', 'hive-server2', version), sudo=True,)
    copy_to_hdfs_mock.assert_any_call("mapreduce", "hadoop", "hdfs", host_sys_prepped=False)
    copy_to_hdfs_mock.assert_any_call("tez", "hadoop", "hdfs", host_sys_prepped=False)
    self.assertEquals(2, copy_to_hdfs_mock.call_count)
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='missing_principal', default_fs='hdfs://c6401.ambari.apache.org:8020',
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
    )
    self.assertNoMoreResources()

    self.assertEquals(1, mocks_dict['call'].call_count)
    self.assertEquals(1, mocks_dict['checked_call'].call_count)
    self.assertEquals(
      ('conf-select', 'set-conf-dir', '--package', 'hive', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['checked_call'].call_args_list[0][0][0])
    self.assertEquals(
      ('conf-select', 'create-conf-dir', '--package', 'hive', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['call'].call_args_list[0][0][0])
