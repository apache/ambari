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

from stacks.utils.RMFTestCase import *

from mock.mock import MagicMock, patch
from resource_management.libraries import functions
from resource_management.core.logger import Logger
from resource_management.libraries.script.config_dictionary import UnknownConfiguration


@patch.object(functions, "get_stack_version", new=MagicMock(return_value="2.0.0.0-1234"))
@patch("resource_management.libraries.functions.check_thrift_port_sasl", new=MagicMock())
@patch("resource_management.libraries.functions.get_user_call_output.get_user_call_output",
       new=MagicMock(return_value=(0, '123', '')))
class TestHiveServerInteractive(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "HIVE/0.12.0.2.0/package"
  STACK_VERSION = "2.0.6"
  UPGRADE_STACK_VERSION = "2.2"
  DEFAULT_IMMUTABLE_PATHS = ['/apps/hive/warehouse', '/apps/falcon', '/mr-history/done', '/app-logs', '/tmp']

  def setUp(self):
    Logger.logger = MagicMock()

  @patch("resource_management.libraries.functions.copy_tarball.copy_to_hdfs")
  def test_configure_default(self, copy_to_hdfs_mock):
    self.maxDiff = None
    copy_to_hdfs_mock.return_value = False
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server_interactive.py",
                       classname="HiveServerInteractive",
                       command="configure",
                       config_file=self.get_src_folder() + "/test/python/stacks/2.5/configs/hsi_default.json",
                       stack_version=self.STACK_VERSION,
                       target=RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  @patch("os.path.isfile")
  @patch("resource_management.libraries.functions.copy_tarball.copy_to_hdfs")
  @patch("socket.socket")
  def test_start_default(self, socket_mock, copy_to_hfds_mock, is_file_mock):
    self.maxDiff = None
    copy_to_hfds_mock.return_value = False
    s = socket_mock.return_value
    is_file_mock.return_value = True
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server_interactive.py",
                       classname="HiveServerInteractive",
                       command="start",
                       config_file=self.get_src_folder() + "/test/python/stacks/2.5/configs/hsi_default.json",
                       stack_version=self.STACK_VERSION,
                       target=RMFTestCase.TARGET_COMMON_SERVICES,
                       checked_call_mocks=[(0, "Prepared llap-slider-05Apr2016/run.sh for running LLAP", ""), (0, "OK.", "")],
    )

    self.assert_configure_default()

    self.assertResourceCalled('Execute',
                              '/home/hive/llap-slider-05Apr2016/run.sh',
                              user='hive'
    )
    self.assertResourceCalled('Execute',
                              'hive --config /etc/hive2/conf.server --service metatool -updateLocation hdfs://c6401.ambari.apache.org:8020 OK.',
                              environment={'PATH': '/usr/bin'},
                              user='hive'
    )
    self.assertResourceCalled('Execute',
                              '/tmp/start_hiveserver2_interactive_script /var/run/hive/hive-server2-interactive.out /var/log/hive/hive-server2-interactive.err /var/run/hive/hive-interactive.pid /etc/hive2/conf.server /var/log/hive',
                              environment={'HADOOP_HOME': '/usr',
                                           'HIVE_BIN': 'hive2',
                                           'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
                              not_if="ls /var/run/hive/hive-interactive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1",
                              user='hive',
                              path=['/bin:/usr/lib/hive/bin:/usr/bin'],
    )
    self.assertResourceCalled('Execute',
                              '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/lib/hive/lib//mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6402.ambari.apache.org/hive?createDatabaseIfNotExist=true\' hive \'!`"\'"\'"\' 1\' com.mysql.jdbc.Driver',
                              path=['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
                              tries=5,
                              try_sleep=10
    )
    self.assertNoMoreResources()


  def test_stop_default(self):
    self.maxDiff = None
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server_interactive.py",
                       classname="HiveServerInteractive",
                       command="stop",
                       config_file=self.get_src_folder() + "/test/python/stacks/2.5/configs/hsi_default.json",
                       stack_version=self.STACK_VERSION,
                       target=RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks=[(0, "OK.", ""), (0, "OK.", "")],
    )

    self.assertResourceCalled('Execute', "ambari-sudo.sh kill 123",
                              not_if="! (ls /var/run/hive/hive-interactive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1)",
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh kill -9 123",
                              not_if="! (ls /var/run/hive/hive-interactive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1) || ( sleep 5 && ! (ls /var/run/hive/hive-interactive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1) )",
    )
    self.assertResourceCalled('Execute',
                              "! (ls /var/run/hive/hive-interactive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1)",
                              tries=20,
                              try_sleep=3,
    )
    self.assertResourceCalled('File', '/var/run/hive/hive-interactive.pid',
                              action=['delete'],
    )

    self.assertNoMoreResources()


  def assert_configure_default(self, no_tmp=False, default_fs_default='hdfs://c6401.ambari.apache.org:8020'):

    self.assertResourceCalled('Directory', '/etc/hive2',
                              mode=0755,
    )
    self.assertResourceCalled('Directory', '/etc/hive2/conf.server',
                              owner='hive',
                              group='hadoop',
                              create_parents=True,
    )

    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
                              group='hadoop',
                              conf_dir='/etc/hive2/conf.server',
                              mode=0644,
                              configuration_attributes={u'final': {u'mapred.healthChecker.script.path': u'true',
                                                                   u'mapreduce.jobtracker.staging.root.dir': u'true'}},
                              owner='hive',
                              configurations=self.getConfig()['configurations']['mapred-site'],
    )
    self.assertResourceCalled('File', '/etc/hive2/conf.server/hive-default.xml.template',
                              owner='hive',
                              group='hadoop',
    )
    self.assertResourceCalled('File', '/etc/hive2/conf.server/hive-env.sh.template',
                              owner='hive',
                              group='hadoop',
    )
    self.assertResourceCalled('File', '/etc/hive2/conf.server/hive-exec-log4j.properties',
                              content='log4jproperties\nline2',
                              owner='hive',
                              group='hadoop',
                              mode=0644,
    )
    self.assertResourceCalled('File', '/etc/hive2/conf.server/hive-log4j.properties',
                              content='log4jproperties\nline2',
                              owner='hive',
                              group='hadoop',
                              mode=0644,
    )
    hive_site_conf = {}
    hive_site_conf.update(self.getConfig()['configurations']['hive-site'])
    hive_site_conf.update(self.getConfig()['configurations']['hive-interactive-site'])
    del hive_site_conf['hive.enforce.bucketing']
    del hive_site_conf['hive.enforce.sorting']
    self.assertResourceCalled('XmlConfig', 'hive-site.xml',
                              group='hadoop',
                              conf_dir='/etc/hive2/conf.server',
                              mode=0644,
                              configuration_attributes={u'final': {u'hive.optimize.bucketmapjoin.sortedmerge': u'true',
                                                                   u'javax.jdo.option.ConnectionDriverName': u'true',
                                                                   u'javax.jdo.option.ConnectionPassword': u'true'}},
                              owner='hive',
                              configurations=hive_site_conf,
    )
    self.assertResourceCalled('XmlConfig', 'tez-site.xml',
                              group='hadoop',
                              conf_dir='/etc/tez_hive2/conf',
                              mode=0664,
                              configuration_attributes=UnknownConfigurationMock(),
                              owner='tez',
                              configurations=self.getConfig()['configurations']['tez-interactive-site'],
                              )
    self.assertResourceCalled('File', '/etc/hive2/conf.server/hive-env.sh',
                              content=InlineTemplate(self.getConfig()['configurations']['hive-interactive-env']['content']),
                              owner='hive',
                              group='hadoop',
    )
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
                              owner='root',
                              group='root',
                              create_parents=True,
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
    self.assertResourceCalled('File', '/tmp/start_hiveserver2_interactive_script',
                              content=Template('startHiveserver2Interactive.sh.j2'),
                              mode=0755,
    )
    self.assertResourceCalled('Directory', '/var/run/hive',
                              owner='hive',
                              mode=0755,
                              group='hadoop',
                              create_parents=True,
                              cd_access='a',
    )
    self.assertResourceCalled('Directory', '/var/log/hive',
                              owner='hive',
                              mode=0755,
                              group='hadoop',
                              create_parents=True,
                              cd_access='a',
    )
    self.assertResourceCalled('Directory', '/var/lib/hive2',
                              owner='hive',
                              mode=0755,
                              group='hadoop',
                              create_parents=True,
                              cd_access='a',
    )