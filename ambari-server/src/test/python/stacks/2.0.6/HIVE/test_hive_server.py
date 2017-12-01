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
from resource_management.libraries.functions.constants import StackFeature
from resource_management.libraries.functions import stack_features
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import conf_select

# used for faking out stack features when the config files used by unit tests use older stacks
def mock_stack_feature(stack_feature, stack_version):
  if stack_feature == StackFeature.ROLLING_UPGRADE:
    return True
  if stack_feature == StackFeature.CONFIG_VERSIONING:
    return True

  return False

@patch.object(functions, "get_stack_version", new = MagicMock(return_value="2.0.0.0-1234"))
@patch("resource_management.libraries.functions.check_thrift_port_sasl", new=MagicMock())
@patch("resource_management.libraries.functions.get_user_call_output.get_user_call_output", new=MagicMock(return_value=(0,'123','')))
@patch.object(stack_select, "get_hadoop_dir", new=MagicMock(return_value="mock_hadoop_dir"))
@patch.object(conf_select, "get_hadoop_conf_dir", new=MagicMock(return_value="/usr/hdp/current/hadoop-client/conf"))
@patch.object(stack_features, "check_stack_feature", new=MagicMock(side_effect=mock_stack_feature))
class TestHiveServer(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "HIVE/0.12.0.2.0/package"
  STACK_VERSION = "2.0.6"
  UPGRADE_STACK_VERSION = "2.2"
  DEFAULT_IMMUTABLE_PATHS = ['/apps/hive/warehouse', '/apps/falcon', '/mr-history/done', '/app-logs', '/tmp']

  def setUp(self):
    Logger.logger = MagicMock()

  @patch("resource_management.libraries.functions.copy_tarball.copy_to_hdfs")
  def test_configure_default(self, copy_to_hdfs_mock):
    copy_to_hdfs_mock.return_value = True
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "configure",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  @patch("resource_management.libraries.functions.copy_tarball.copy_to_hdfs")
  @patch("socket.socket")
  def test_start_default(self, socket_mock, copy_to_hfds_mock):
    copy_to_hfds_mock.return_value = None
    s = socket_mock.return_value
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname="HiveServer",
                       command="start",
                       config_file="default.json",
                       stack_version=self.STACK_VERSION,
                       target=RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_default()

    self.assertResourceCalled('Execute',
                              'hive --config /usr/hdp/current/hive-server2/conf/conf.server --service metatool -updateLocation hdfs://c6401.ambari.apache.org:8020 OK.',
                              environment={'PATH': '/bin:/usr/hdp/current/hive-server2/bin:mock_hadoop_dir'},
                              user='hive'
    )
    self.assertResourceCalled('Execute', '/tmp/start_hiveserver2_script /var/log/hive/hive-server2.out /var/log/hive/hive-server2.err /var/run/hive/hive-server.pid /usr/hdp/current/hive-server2/conf/conf.server /var/log/hive',
        environment = { 'HIVE_CMD': '/usr/hdp/current/hive-server2/bin/hive',
           'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        not_if = "ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1",
        user = 'hive',
        path = ['/bin:/usr/hdp/current/hive-server2/bin:mock_hadoop_dir'],
    )
    self.assertResourceCalled('Execute',
                              '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/hdp/current/hive-server2/lib/mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6402.ambari.apache.org/hive?createDatabaseIfNotExist=true\' hive \'!`"\'"\'"\' 1\' com.mysql.jdbc.Driver',
                              path=['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
                              tries=5,
                              try_sleep=10
    )
    self.assertNoMoreResources()

  @patch("resource_management.libraries.functions.copy_tarball.copy_to_hdfs")
  @patch("socket.socket")
  def test_start_default_non_hdfs(self, socket_mock, copy_to_hfds_mock):
    copy_to_hfds_mock.return_value = None
    s = socket_mock.return_value
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname="HiveServer",
                       command="start",
                       config_file="default_hive_non_hdfs.json",
                       stack_version=self.STACK_VERSION,
                       target=RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_default(default_fs_default='hcfs://c6401.ambari.apache.org:8020')

    self.assertResourceCalled('Execute', '/tmp/start_hiveserver2_script /var/log/hive/hive-server2.out /var/log/hive/hive-server2.err /var/run/hive/hive-server.pid /usr/hdp/current/hive-server2/conf/conf.server /var/log/hive',
                              environment = {'HIVE_CMD': '/usr/hdp/current/hive-server2/bin/hive',
                                             'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
                              not_if = "ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1",
                              user = 'hive',
                              path = ['/bin:/usr/hdp/current/hive-server2/bin:mock_hadoop_dir'],
                              )
    self.assertResourceCalled('Execute',
                              '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/hdp/current/hive-server2/lib/mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6402.ambari.apache.org/hive?createDatabaseIfNotExist=true\' hive \'!`"\'"\'"\' 1\' com.mysql.jdbc.Driver',
                              path=['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
                              tries=5,
                              try_sleep=10
    )
    self.assertNoMoreResources()

  def test_start_default_no_copy(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "start",
                       config_file="default_no_install.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_default()

    self.assertResourceCalled('Execute', 'hive --config /usr/hdp/current/hive-server2/conf/conf.server --service metatool -updateLocation hdfs://c6401.ambari.apache.org:8020 OK.',
                              environment = {'PATH': '/bin:/usr/hdp/current/hive-server2/bin:mock_hadoop_dir'},
                              user = 'hive',
                              )
    self.assertResourceCalled('Execute', '/tmp/start_hiveserver2_script /var/log/hive/hive-server2.out /var/log/hive/hive-server2.err /var/run/hive/hive-server.pid /usr/hdp/current/hive-server2/conf/conf.server /var/log/hive',
        environment = { 'HIVE_CMD': '/usr/hdp/current/hive-server2/bin/hive',
           'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        not_if = "ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1",
        user = 'hive',
        path = ['/bin:/usr/hdp/current/hive-server2/bin:mock_hadoop_dir'],
    )

    self.assertResourceCalled('Execute', '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/hdp/current/hive-server2/lib/mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6402.ambari.apache.org/hive?createDatabaseIfNotExist=true\' hive \'!`"\'"\'"\' 1\' com.mysql.jdbc.Driver',
                              path=['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'], tries=5, try_sleep=10
    )
    self.assertNoMoreResources()

  @patch("resource_management.libraries.functions.copy_tarball.copy_to_hdfs")
  def test_start_default_alt_tmp(self, copy_to_hfds_mock):
    copy_to_hfds_mock.return_value = None
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "start",
                       config_file="default_hive_nn_ha.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_default(no_tmp=True)

    self.assertResourceCalled('Execute', 'hive --config /usr/hdp/current/hive-server2/conf/conf.server --service metatool -updateLocation hdfs://c6401.ambari.apache.org:8020 OK.',
                              environment = {'PATH': '/bin:/usr/hdp/current/hive-server2/bin:mock_hadoop_dir'},
                              user = 'hive',
                              )
    self.assertResourceCalled('Execute', '/tmp/start_hiveserver2_script /var/log/hive/hive-server2.out /var/log/hive/hive-server2.err /var/run/hive/hive-server.pid /usr/hdp/current/hive-server2/conf/conf.server /var/log/hive',
        environment = { 'HIVE_CMD': '/usr/hdp/current/hive-server2/bin/hive',
           'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        not_if = "ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1",
        user = 'hive',
        path = ['/bin:/usr/hdp/current/hive-server2/bin:mock_hadoop_dir'],
    )

    self.assertResourceCalled('Execute', '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/hdp/current/hive-server2/lib/mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6402.ambari.apache.org/hive?createDatabaseIfNotExist=true\' hive \'!`"\'"\'"\' 1\' com.mysql.jdbc.Driver',
                              path=['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'], tries=5, try_sleep=10
    )
    self.assertNoMoreResources()

  @patch("resource_management.libraries.functions.copy_tarball.copy_to_hdfs")
  def test_start_default_alt_nn_ha_tmp(self, copy_to_hfds_mock):
    copy_to_hfds_mock.return_value = None
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "start",
                       config_file="default_hive_nn_ha_2.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_default(no_tmp=True)

    self.assertResourceCalled('Execute', 'hive --config /usr/hdp/current/hive-server2/conf/conf.server --service metatool -updateLocation hdfs://c6401.ambari.apache.org:8020 OK.',
                              environment = {'PATH': '/bin:/usr/hdp/current/hive-server2/bin:mock_hadoop_dir'},
                              user = 'hive',
                              )
    self.assertResourceCalled('Execute', '/tmp/start_hiveserver2_script /var/log/hive/hive-server2.out /var/log/hive/hive-server2.err /var/run/hive/hive-server.pid /usr/hdp/current/hive-server2/conf/conf.server /var/log/hive',
        environment = { 'HIVE_CMD': '/usr/hdp/current/hive-server2/bin/hive',
           'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        not_if = "ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1",
        user = 'hive',
        path = ['/bin:/usr/hdp/current/hive-server2/bin:mock_hadoop_dir'],
    )

    self.assertResourceCalled('Execute', '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/hdp/current/hive-server2/lib/mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6402.ambari.apache.org/hive?createDatabaseIfNotExist=true\' hive \'!`"\'"\'"\' 1\' com.mysql.jdbc.Driver',
                              path=['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'], tries=5, try_sleep=10
    )
    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "stop",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Execute', "ambari-sudo.sh kill 123",
        not_if = "! (ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1)",
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh kill -9 123",
        not_if = "! (ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1) || ( sleep 5 && ! (ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1) )",
        ignore_failures = True
    )
    self.assertResourceCalled('Execute', "! (ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1)",
        tries = 20,
        try_sleep = 3,
    )
    self.assertResourceCalled('File', '/var/run/hive/hive-server.pid',
      action = ['delete'],
    )

    self.assertNoMoreResources()

  def test_configure_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "configure",
                       config_file="secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()

  @patch("resource_management.libraries.functions.copy_tarball.copy_to_hdfs")
  @patch("hive_service.check_fs_root")
  @patch("socket.socket")
  def test_start_secured(self, socket_mock, check_fs_root_mock, copy_to_hfds_mock):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/secured.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)

    json_content['commandParams']['version'] = '2.3.0.0-1234'

    s = socket_mock.return_value
    copy_to_hfds_mock.return_value = None

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "start",
                       config_dict = json_content,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_secured()
    self.assertResourceCalled('Execute', '/tmp/start_hiveserver2_script /var/log/hive/hive-server2.out /var/log/hive/hive-server2.err /var/run/hive/hive-server.pid /usr/hdp/current/hive-server2/conf/conf.server /var/log/hive',
        environment = { 'HIVE_CMD': '/usr/hdp/current/hive-server2/bin/hive',
           'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        not_if = "ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1",
        user = 'hive',
        path = ['/bin:/usr/hdp/current/hive-server2/bin:mock_hadoop_dir'],
    )
    self.assertResourceCalled('Execute',
                              '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/hdp/current/hive-server2/lib/mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6402.ambari.apache.org/hive?createDatabaseIfNotExist=true\' hive \'!`"\'"\'"\' 1\' com.mysql.jdbc.Driver',
                              path=['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
                              tries=5,
                              try_sleep=10,
    )
    self.assertNoMoreResources()

    self.assertTrue(check_fs_root_mock.called)


  @patch("socket.socket")
  def test_stop_secured(self, socket_mock):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "stop",
                       config_file="secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Execute', "ambari-sudo.sh kill 123",
        not_if = "! (ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1)",
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh kill -9 123",
        not_if = "! (ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1) || ( sleep 5 && ! (ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1) )",
        ignore_failures = True
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

    if self._testMethodName == "test_socket_timeout":
      # This test will not call any more resources.
      return
    
    self.assertResourceCalled('Directory', '/etc/hive',
                              mode=0755,
    )
    self.assertResourceCalled('Directory', '/usr/hdp/current/hive-server2/conf',
                              owner='hive',
                              group='hadoop',
                              create_parents = True,
                              mode = 0755
    )

    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
                              group='hadoop',
                              conf_dir='/usr/hdp/current/hive-server2/conf',
                              mode=0644,
                              configuration_attributes={u'final': {u'mapred.healthChecker.script.path': u'true',
                                                                   u'mapreduce.jobtracker.staging.root.dir': u'true'}},
                              owner='hive',
                              configurations=self.getConfig()['configurations']['mapred-site'],
    )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/hive-default.xml.template',
                              owner='hive',
                              group='hadoop',
                              mode=0644
    )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/hive-env.sh.template',
                              owner='hive',
                              group='hadoop',
                              mode=0644
    )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/hive-exec-log4j.properties',
                              content=InlineTemplate('log4jproperties\nline2'),
                              owner='hive',
                              group='hadoop',
                              mode=0644,
    )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/hive-log4j.properties',
                              content=InlineTemplate('log4jproperties\nline2'),
                              owner='hive',
                              group='hadoop',
                              mode=0644,
    )
    self.assertResourceCalled('XmlConfig', 'hive-site.xml',
                              group='hadoop',
                              conf_dir='/usr/hdp/current/hive-server2/conf/conf.server',
                              mode=0600,
                              configuration_attributes={u'final': {u'hive.optimize.bucketmapjoin.sortedmerge': u'true',
                                                                   u'javax.jdo.option.ConnectionDriverName': u'true',
                                                                   u'javax.jdo.option.ConnectionPassword': u'true'}},
                              owner='hive',
                              configurations=self.getConfig()['configurations']['hive-site'],
    )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/conf.server/hive-env.sh',
                              content=InlineTemplate(self.getConfig()['configurations']['hive-env']['content']),
                              owner='hive',
                              group='hadoop',
                              mode = 0600
    )
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
                              owner='root',
                              group='root',
                              create_parents = True,
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/hive.conf',
                              content=Template('hive.conf.j2'),
                              owner='root',
                              group='root',
                              mode=0644,
    )
    self.assertResourceCalled('File', '/usr/lib/ambari-agent/DBConnectionVerification.jar',
                              content=DownloadSource('http://c6401.ambari.apache.org:8080/resources'
                                                     '/DBConnectionVerification.jar'),
                              mode=0644,
    )
    self.assertResourceCalled('Directory', '/var/run/hive',
                              owner='hive',
                              mode=0755,
                              group='hadoop',
                              create_parents = True,
                              cd_access='a',
    )
    self.assertResourceCalled('Directory', '/var/log/hive',
                              owner='hive',
                              mode=0755,
                              group='hadoop',
                              create_parents = True,
                              cd_access='a',
    )
    self.assertResourceCalled('Directory', '/var/lib/hive',
                              owner='hive',
                              mode=0755,
                              group='hadoop',
                              create_parents = True,
                              cd_access='a',
    )
    self.assertResourceCalled('Execute', ('cp',
                                          '--remove-destination',
                                          '/usr/share/java/mysql-connector-java.jar',
                                          '/usr/hdp/current/hive-server2/lib/mysql-connector-java.jar'),
                              path=['/bin', '/usr/bin/'],
                              sudo=True,
    )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/lib/mysql-connector-java.jar',
                              mode=0644,
    )
    self.assertResourceCalled('File', '/tmp/start_hiveserver2_script',
                              content=Template('startHiveserver2.sh.j2'),
                              mode=0755,
    )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/conf.server/hadoop-metrics2-hiveserver2.properties',
                              owner = 'hive',
                              group = 'hadoop',
                              content = Template('hadoop-metrics2-hiveserver2.properties.j2'),
                              mode = 0600,
                              )
    self.assertResourceCalled('XmlConfig', 'hiveserver2-site.xml',
      group = 'hadoop',
      conf_dir = '/usr/hdp/current/hive-server2/conf/conf.server',
      mode = 0600,
      owner = 'hive',
      configuration_attributes = self.getConfig()['configuration_attributes']['hiveserver2-site'],
      configurations = self.getConfig()['configurations']['hiveserver2-site'],
    )
    # Verify creating of Hcat and Hive directories
    self.assertResourceCalled('HdfsResource', '/apps/webhcat',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = 'mock_hadoop_dir',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'hcat',
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='missing_principal', default_fs=default_fs_default,
        mode = 0755,
    )
    self.assertResourceCalled('HdfsResource', '/user/hcat',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = 'mock_hadoop_dir',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'hcat',
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='missing_principal', default_fs=default_fs_default,
        mode = 0755,
    )

    self.assertResourceCalled('HdfsResource', '/apps/hive/warehouse',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = 'mock_hadoop_dir',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'hive',
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='missing_principal', default_fs=default_fs_default,
        group = 'hadoop',
        mode = 0777,
    )
    self.assertResourceCalled('HdfsResource', '/user/hive',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = 'mock_hadoop_dir',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'hive',
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='missing_principal', default_fs=default_fs_default,
        mode = 0755,
    )
    if not no_tmp:
      self.assertResourceCalled('HdfsResource', '/custompath/tmp/hive',
          immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
          security_enabled = False,
          hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
          keytab = UnknownConfigurationMock(),
          kinit_path_local = '/usr/bin/kinit',
          user = 'hdfs',
          dfs_type = '',
          owner = 'hive',
          group = 'hdfs',
          hadoop_bin_dir = 'mock_hadoop_dir',
          type = 'directory',
          action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='missing_principal', default_fs=default_fs_default,
          mode = 0777,
      )
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = 'mock_hadoop_dir',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='missing_principal', default_fs=default_fs_default,
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
    )

  def assert_configure_secured(self):
    self.assertResourceCalled('Directory', '/etc/hive',
                              mode=0755,
    )
    self.assertResourceCalled('Directory', '/usr/hdp/current/hive-server2/conf',
                              owner='hive',
                              group='hadoop',
                              create_parents = True,
                              mode = 0755,
    )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
                              group='hadoop',
                              conf_dir='/usr/hdp/current/hive-server2/conf',
                              mode=0644,
                              configuration_attributes={u'final': {u'mapred.healthChecker.script.path': u'true',
                                                                   u'mapreduce.jobtracker.staging.root.dir': u'true'}},
                              owner='hive',
                              configurations=self.getConfig()['configurations']['mapred-site'],
    )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/hive-default.xml.template',
                              owner='hive',
                              group='hadoop',
                              mode = 0644,
    )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/hive-env.sh.template',
                              owner='hive',
                              group='hadoop',
                              mode = 0644,
    )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/hive-exec-log4j.properties',
                              content=InlineTemplate('log4jproperties\nline2'),
                              owner='hive',
                              group='hadoop',
                              mode=0644,
    )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/hive-log4j.properties',
                              content=InlineTemplate('log4jproperties\nline2'),
                              owner='hive',
                              group='hadoop',
                              mode=0644,
    )
    self.assertResourceCalled('XmlConfig', 'hive-site.xml',
                              group='hadoop',
                              conf_dir='/usr/hdp/current/hive-server2/conf/conf.server',
                              mode=0600,
                              configuration_attributes={u'final': {u'hive.optimize.bucketmapjoin.sortedmerge': u'true',
                                                                   u'javax.jdo.option.ConnectionDriverName': u'true',
                                                                   u'javax.jdo.option.ConnectionPassword': u'true'}},
                              owner='hive',
                              configurations=self.getConfig()['configurations']['hive-site'],
    )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/conf.server/hive-env.sh',
                              content=InlineTemplate(self.getConfig()['configurations']['hive-env']['content']),
                              owner='hive',
                              group='hadoop',
                              mode = 0600,
    )
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
                              owner='root',
                              group='root',
                              create_parents = True,
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/hive.conf',
                              content=Template('hive.conf.j2'),
                              owner='root',
                              group='root',
                              mode=0644,
    )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/conf.server/zkmigrator_jaas.conf',
                              content = Template('zkmigrator_jaas.conf.j2'),
                              owner = 'hive',
                              group = 'hadoop',
    )
    self.assertResourceCalled('File', '/usr/lib/ambari-agent/DBConnectionVerification.jar',
                              content=DownloadSource(
                                'http://c6401.ambari.apache.org:8080/resources/DBConnectionVerification.jar'),
                              mode=0644,
    )
    self.assertResourceCalled('Directory', '/var/run/hive',
                              owner='hive',
                              group='hadoop',
                              mode = 0755,
                              create_parents = True,
                              cd_access='a',
    )
    self.assertResourceCalled('Directory', '/var/log/hive',
                              owner='hive',
                              group='hadoop',
                              mode = 0755,
                              create_parents = True,
                              cd_access='a',
    )
    self.assertResourceCalled('Directory', '/var/lib/hive',
                              owner='hive',
                              group='hadoop',
                              mode = 0755,
                              create_parents = True,
                              cd_access='a',
    )
    self.assertResourceCalled('Execute', ('cp',
                                          '--remove-destination',
                                          '/usr/share/java/mysql-connector-java.jar',
                                          '/usr/hdp/current/hive-server2/lib/mysql-connector-java.jar'),
                              path=['/bin', '/usr/bin/'],
                              sudo=True,
    )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/lib/mysql-connector-java.jar',
                              mode=0644,
    )
    self.assertResourceCalled('File', '/tmp/start_hiveserver2_script',
                              content=Template('startHiveserver2.sh.j2'),
                              mode=0755,
    )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/conf.server/hadoop-metrics2-hiveserver2.properties',
                              owner = 'hive',
                              group = 'hadoop',
                              content = Template('hadoop-metrics2-hiveserver2.properties.j2'),
                              mode = 0600,
    )
    self.assertResourceCalled('XmlConfig', 'hiveserver2-site.xml',
      group = 'hadoop',
      conf_dir = '/usr/hdp/current/hive-server2/conf/conf.server',
      mode = 0600,
      owner = 'hive',
      configuration_attributes = self.getConfig()['configuration_attributes']['hiveserver2-site'],
      configurations = self.getConfig()['configurations']['hiveserver2-site'],
    )
    self.assertResourceCalled('HdfsResource', '/apps/webhcat',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_bin_dir = 'mock_hadoop_dir',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'hcat',
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='hdfs', default_fs='hdfs://c6401.ambari.apache.org:8020',
        mode = 0755,
    )
    self.assertResourceCalled('HdfsResource', '/user/hcat',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_bin_dir = 'mock_hadoop_dir',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'hcat',
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='hdfs', default_fs='hdfs://c6401.ambari.apache.org:8020',
        mode = 0755,
    )

    self.assertResourceCalled('HdfsResource', '/apps/hive/warehouse',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_bin_dir = 'mock_hadoop_dir',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'hive',
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='hdfs', default_fs='hdfs://c6401.ambari.apache.org:8020',
        group = 'hadoop',
        mode = 0777,
    )
    self.assertResourceCalled('HdfsResource', '/user/hive',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_bin_dir = 'mock_hadoop_dir',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'hive',
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='hdfs', default_fs='hdfs://c6401.ambari.apache.org:8020',
        mode = 0755,
    )
    self.assertResourceCalled('HdfsResource', '/custompath/tmp/hive',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'hive',
        group = 'hdfs',
        hadoop_bin_dir = 'mock_hadoop_dir',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='hdfs', default_fs='hdfs://c6401.ambari.apache.org:8020',
        mode = 0777,
    )
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_bin_dir = 'mock_hadoop_dir',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='hdfs', default_fs='hdfs://c6401.ambari.apache.org:8020',
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
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
                           stack_version = self.STACK_VERSION,
                           target = RMFTestCase.TARGET_COMMON_SERVICES
      )

      self.fail("Script failure due to socket error was expected")
    except:
      self.assert_configure_default()

  @patch("resource_management.libraries.script.Script.post_start")
  @patch("resource_management.libraries.functions.copy_tarball.copy_to_hdfs")
  @patch("os.path.exists", new = MagicMock(return_value=True))
  @patch("platform.linux_distribution", new = MagicMock(return_value="Linux"))
  def test_stop_during_upgrade(self, copy_to_hdfs_mock, post_start_mock):

    hiveServerVersionOutput = """WARNING: Use "yarn jar" to launch YARN applications.
Hive 1.2.1.2.3.0.0-2434
Subversion git://ip-10-0-0-90.ec2.internal/grid/0/jenkins/workspace/HDP-dal-centos6/bigtop/build/hive/rpm/BUILD/hive-1.2.1.2.3.0.0 -r a77a00ae765a73b2957337e96ed5a0dbb2e60dfb
Compiled by jenkins on Sat Jun 20 11:50:41 EDT 2015
From source with checksum 150f554beae04f76f814f59549dead8b"""
    call_side_effects = [(0, hiveServerVersionOutput), (0, hiveServerVersionOutput)] * 4
    copy_to_hdfs_mock.return_value = True

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
     classname = "HiveServer", command = "restart", config_file = "hive-upgrade.json",
     stack_version = self.UPGRADE_STACK_VERSION,
     target = RMFTestCase.TARGET_COMMON_SERVICES,
     call_mocks = call_side_effects
    )

    # ensure deregister is called
    self.assertResourceCalledIgnoreEarlier('Execute', 'hive --config /usr/hdp/current/hive-server2/conf/conf.server --service hiveserver2 --deregister 1.2.1.2.3.0.0-2434',
      path=['/bin:/usr/hdp/current/hive-server2/bin:mock_hadoop_dir'],
      tries=1, user='hive')

    # ensure stop is called
    self.assertResourceCalled('Execute', "ambari-sudo.sh kill 123",
        not_if = "! (ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1)")

    # skip any other stop stuff since it's covered by other tests and verify hdp-select
    self.assertResourceCalledIgnoreEarlier('Execute',
      ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'hive-server2', '2.2.1.0-2065'),
      sudo=True)


  @patch("resource_management.libraries.script.Script.post_start")
  @patch("resource_management.libraries.functions.copy_tarball.copy_to_hdfs")
  def test_stop_during_upgrade_with_default_conf_server(self, copy_to_hdfs_mock, post_start_mock):
    hiveServerVersionOutput = """WARNING: Use "yarn jar" to launch YARN applications.
Hive 1.2.1.2.3.0.0-2434
Subversion git://ip-10-0-0-90.ec2.internal/grid/0/jenkins/workspace/HDP-dal-centos6/bigtop/build/hive/rpm/BUILD/hive-1.2.1.2.3.0.0 -r a77a00ae765a73b2957337e96ed5a0dbb2e60dfb
Compiled by jenkins on Sat Jun 20 11:50:41 EDT 2015
From source with checksum 150f554beae04f76f814f59549dead8b"""
    call_side_effects = [(0, hiveServerVersionOutput), (0, hiveServerVersionOutput)] * 4
    copy_to_hdfs_mock.return_value = True

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
     classname = "HiveServer", command = "restart", config_file = "hive-upgrade.json",
     stack_version = self.UPGRADE_STACK_VERSION,
     target = RMFTestCase.TARGET_COMMON_SERVICES,
     call_mocks = call_side_effects
    )

    # ensure that deregister is called
    self.assertResourceCalledIgnoreEarlier( 'Execute', 'hive --config /usr/hdp/current/hive-server2/conf/conf.server --service hiveserver2 --deregister 1.2.1.2.3.0.0-2434',
      path=['/bin:/usr/hdp/current/hive-server2/bin:mock_hadoop_dir'],
      tries=1, user='hive')

    # ensure hdp-select is called
    self.assertResourceCalledIgnoreEarlier('Execute',
      ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'hive-server2', '2.2.1.0-2065'),
      sudo = True, )


  def test_stop_during_upgrade_bad_hive_version(self):
    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
       classname = "HiveServer", command = "restart", config_file = "hive-upgrade.json",
       stack_version = self.UPGRADE_STACK_VERSION,
       target = RMFTestCase.TARGET_COMMON_SERVICES,
       call_mocks = [(0,"BAD VERSION")])
      self.fail("Invalid hive version should have caused an exception")
    except:
      pass

    self.assertResourceCalled('Execute', "ambari-sudo.sh kill 123",
        not_if = "! (ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1)")

    self.assertResourceCalledIgnoreEarlier('Execute',
      ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'hive-server2', '2.2.1.0-2065'),
      sudo=True)


  @patch("resource_management.libraries.functions.copy_tarball.copy_to_hdfs")
  def test_pre_upgrade_restart(self, copy_to_hdfs_mock):
    copy_to_hdfs_mock.return_value = True

    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.2.1.0-3242'
    json_content['commandParams']['version'] = version
    json_content['hostLevelParams']['stack_version'] = '2.2'
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)
    self.assertResourceCalled('Execute',
                              ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'hive-server2', version), sudo=True,)

    copy_to_hdfs_mock.assert_any_call("mapreduce", "hadoop", "hdfs", skip=False)
    copy_to_hdfs_mock.assert_any_call("tez", "hadoop", "hdfs", skip=False)
    self.assertEquals(2, copy_to_hdfs_mock.call_count)
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = 'mock_hadoop_dir',
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
  @patch("resource_management.libraries.functions.copy_tarball.copy_to_hdfs")
  def test_pre_upgrade_restart_23(self, copy_to_hdfs_mock, call_mock, os_path__exists_mock):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    os_path__exists_mock.return_value = False
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version
    json_content['hostLevelParams']['stack_version'] = '2.3'

    copy_to_hdfs_mock.return_value = True
    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       mocks_dict = mocks_dict)

    self.assertResourceCalled('Execute',

                              ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'hive-server2', version), sudo=True,)
    copy_to_hdfs_mock.assert_any_call("mapreduce", "hadoop", "hdfs", skip=False)
    copy_to_hdfs_mock.assert_any_call("tez", "hadoop", "hdfs", skip=False)
    self.assertEquals(2, copy_to_hdfs_mock.call_count)
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = 'mock_hadoop_dir',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='missing_principal', default_fs='hdfs://c6401.ambari.apache.org:8020',
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
    )
    self.assertNoMoreResources()
