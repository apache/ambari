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

from stacks.utils.RMFTestCase import *

from mock.mock import MagicMock, patch
from resource_management.libraries import functions
from resource_management.core.logger import Logger
from resource_management.libraries.script.config_dictionary import UnknownConfiguration
from hive_server_interactive import HiveServerInteractiveDefault

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
    self.testDirectory = os.path.dirname(os.path.abspath(__file__))
    # llap state related tests.
    self.hsi = HiveServerInteractiveDefault()
    self.llap_app_name='llap'
    self.num_times_to_iterate = 3
    self.wait_time = 1

  def load_json(self, filename):
    file = os.path.join(self.testDirectory, filename)
    with open(file, 'rb') as f:
      data = json.load(f)
    return data

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
  @patch("time.sleep")
  def test_start_default(self, sleep_mock, socket_mock, copy_to_hfds_mock, is_file_mock):
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
                       checked_call_mocks=[(0, "Prepared llap-slider-05Apr2016/run.sh for running LLAP", ""),
                                           (0, "{\"state\":\"RUNNING_ALL\"}", ""), (0, "OK.", "")],
    )

    self.assert_configure_default()

    self.assertResourceCalled('Execute',
                              '/home/hive/llap-slider-05Apr2016/run.sh',
                              user='hive'
    )
    self.assertResourceCalled('Execute',
                              'hive --config /usr/hdp/current/hive-server2-hive2/conf/conf.server --service metatool -updateLocation hdfs://c6401.ambari.apache.org:8020 OK.',
                              environment={'PATH': '/usr/hdp/current/hadoop-client/bin'},
                              user='hive'
    )
    self.assertResourceCalled('Execute',
                              '/tmp/start_hiveserver2_interactive_script /var/run/hive/hive-server2-interactive.out /var/log/hive/hive-server2-interactive.err /var/run/hive/hive-interactive.pid /usr/hdp/current/hive-server2-hive2/conf/conf.server /var/log/hive',
                              environment={'HADOOP_HOME': '/usr/hdp/current/hadoop-client',
                                           'HIVE_BIN': 'hive2',
                                           'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
                              not_if="ls /var/run/hive/hive-interactive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1",
                              user='hive',
                              path=['/bin:/usr/hdp/current/hive-server2-hive2/bin:/usr/hdp/current/hadoop-client/bin'],
    )
    self.assertResourceCalled('Execute',
                              '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/hdp/current/hive-server2-hive2/lib/mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6402.ambari.apache.org/hive?createDatabaseIfNotExist=true\' hive \'!`"\'"\'"\' 1\' com.mysql.jdbc.Driver',
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
    self.assertResourceCalled('Directory', '/usr/hdp/current/hive-server2-hive2/conf',
                              owner='hive',
                              group='hadoop',
                              create_parents=True,
                              )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
                              group='hadoop',
                              conf_dir='/usr/hdp/current/hive-server2-hive2/conf',
                              mode=0644,
                              configuration_attributes={u'final': {u'mapred.healthChecker.script.path': u'true',
                                                                   u'mapreduce.jobtracker.staging.root.dir': u'true'}},
                              owner='hive',
                              configurations=self.getConfig()['configurations']['mapred-site'],
    )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2-hive2/conf/hive-default.xml.template',
                              owner='hive',
                              group='hadoop',
    )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2-hive2/conf/hive-env.sh.template',
                              owner='hive',
                              group='hadoop',
    )
    hive_site_conf = {}
    hive_site_conf.update(self.getConfig()['configurations']['hive-site'])
    hive_site_conf.update(self.getConfig()['configurations']['hive-interactive-site'])
    del hive_site_conf['hive.enforce.bucketing']
    del hive_site_conf['hive.enforce.sorting']

    mapred_site_conf = {}
    mapred_site_conf.update(self.getConfig()['configurations']['mapred-site'])

    self.assertResourceCalled("Directory", "/usr/hdp/current/hive-server2-hive2/conf/conf.server",
                              owner=u"hive",
                              group=u"hadoop",
                              create_parents=True)

    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
                              group='hadoop',
                              conf_dir='/usr/hdp/current/hive-server2-hive2/conf/conf.server',
                              mode=0644,
                              configuration_attributes={u'final': {u'mapred.healthChecker.script.path': u'true',
                                                                   u'mapreduce.jobtracker.staging.root.dir': u'true'}},
                              owner='hive',
                              configurations=mapred_site_conf,
    )

    self.assertResourceCalled("File", "/usr/hdp/current/hive-server2-hive2/conf/conf.server/hive-default.xml.template",
                              owner=u"hive",
                              group=u"hadoop")

    self.assertResourceCalled("File", "/usr/hdp/current/hive-server2-hive2/conf/conf.server/hive-env.sh.template",
                              owner=u"hive",
                              group=u"hadoop")
    self.assertResourceCalled('XmlConfig', 'tez-site.xml',
                              group='hadoop',
                              conf_dir='/etc/tez_hive2/conf',
                              mode=0664,
                              configuration_attributes=UnknownConfigurationMock(),
                              owner='tez',
                              configurations=self.getConfig()['configurations']['tez-interactive-site'],
    )
    # Verify that config files got created under /etc/hive2/conf and /etc/hive2/conf/conf.server
    hive_conf_dirs_list = ['/usr/hdp/current/hive-server2-hive2/conf', '/usr/hdp/current/hive-server2-hive2/conf/conf.server']

    for conf_dir in hive_conf_dirs_list:
        self.assertResourceCalled('XmlConfig', 'hive-site.xml',
                                  group='hadoop',
                                  conf_dir=conf_dir,
                                  mode=0644,
                                  configuration_attributes={u'final': {u'hive.optimize.bucketmapjoin.sortedmerge': u'true',
                                                                       u'javax.jdo.option.ConnectionDriverName': u'true',
                                                                       u'javax.jdo.option.ConnectionPassword': u'true'}},
                                  owner='hive',
                                  configurations=hive_site_conf,
        )
        self.assertResourceCalled('File', os.path.join(conf_dir, 'hive-env.sh'),
                                  content=InlineTemplate(self.getConfig()['configurations']['hive-interactive-env']['content']),
                                  owner='hive',
                                  group='hadoop',
        )
        self.assertResourceCalled('File', os.path.join(conf_dir, 'llap-daemon-log4j2.properties'),
                                  content='con\ntent',
                                  owner='hive',
                                  group='hadoop',
                                  mode=0644,
        )
        self.assertResourceCalled('File', os.path.join(conf_dir, 'llap-cli-log4j2.properties'),
                                  content='con\ntent',
                                  owner='hive',
                                  group='hadoop',
                                  mode=0644,
        )
        self.assertResourceCalled('File', os.path.join(conf_dir, 'hive-log4j2.properties'),
                                  content='con\ntent',  # Test new line
                                  owner='hive',
                                  group='hadoop',
                                  mode=0644,
        )
        self.assertResourceCalled('File', os.path.join(conf_dir, 'hive-exec-log4j2.properties'),
                                  content='con\ntent',  # Test new line
                                  owner='hive',
                                  group='hadoop',
                                  mode=0644,
        )
        self.assertResourceCalled('File', os.path.join(conf_dir, 'beeline-log4j2.properties'),
                                  content='con\ntent',  # Test new line
                                  owner='hive',
                                  group='hadoop',
                                  mode=0644,
        )
        pass

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
                                          '/usr/hdp/current/hive-server2-hive2/lib/mysql-connector-java.jar'),
                              path=['/bin', '/usr/bin/'],
                              sudo=True,
    )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2-hive2/lib/mysql-connector-java.jar',
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



  # llap app 'status check' related tests

  # Status : RUNNING
  @patch("time.sleep")
  @patch('hive_server_interactive.HiveServerInteractiveDefault._get_llap_app_status_info')
  def test_check_llap_app_status_running_all_wait_negative(self, mock_get_llap_app_status_data, sleep_mock):
    sleep_mock.return_value = 1

    llap_app_json = self.load_json('running.json')
    mock_get_llap_app_status_data.return_value = llap_app_json

    status = self.hsi.check_llap_app_status(self.llap_app_name, -1)
    self.assertEqual(status, True)

  @patch("time.sleep")
  @patch('hive_server_interactive.HiveServerInteractiveDefault._get_llap_app_status_info')
  def test_check_llap_app_status_running_all_wait_0(self, mock_get_llap_app_status_data, sleep_mock):
    sleep_mock.return_value = 1

    llap_app_json = self.load_json('running.json')
    mock_get_llap_app_status_data.return_value = llap_app_json

    status = self.hsi.check_llap_app_status(self.llap_app_name, 0)
    self.assertEqual(status, True)


  @patch("time.sleep")
  @patch('hive_server_interactive.HiveServerInteractiveDefault._get_llap_app_status_info')
  def test_check_llap_app_status_running_all_wait_2(self, mock_get_llap_app_status_data, sleep_mock):
    sleep_mock.return_value = 1

    llap_app_json = self.load_json('running.json')
    mock_get_llap_app_status_data.return_value = llap_app_json

    status = self.hsi.check_llap_app_status(self.llap_app_name, 2)
    self.assertEqual(status, True)




  # Status : RUNNING_PARTIAL (2 out of 3 running -> < 80% instances ON)
  @patch("time.sleep")
  @patch('hive_server_interactive.HiveServerInteractiveDefault._get_llap_app_status_info')
  def test_check_llap_app_status_one_container_down_wait_negative(self, mock_get_llap_app_status_data, sleep_mock):
    sleep_mock.return_value = 1

    llap_app_json = self.load_json('oneContainerDown.json')
    mock_get_llap_app_status_data.return_value = llap_app_json

    status = self.hsi.check_llap_app_status(self.llap_app_name, -1)
    self.assertEqual(status, False)

  @patch("time.sleep")
  @patch('hive_server_interactive.HiveServerInteractiveDefault._get_llap_app_status_info')
  def test_check_llap_app_status_one_container_down_wait_0(self, mock_get_llap_app_status_data, sleep_mock):
    sleep_mock.return_value = 1

    llap_app_json = self.load_json('oneContainerDown.json')
    mock_get_llap_app_status_data.return_value = llap_app_json

    status = self.hsi.check_llap_app_status(self.llap_app_name, 0)
    self.assertEqual(status, False)

  @patch("time.sleep")
  @patch('hive_server_interactive.HiveServerInteractiveDefault._get_llap_app_status_info')
  def test_check_llap_app_status_one_container_down_wait_2(self, mock_get_llap_app_status_data, sleep_mock):
    sleep_mock.return_value = 1

    llap_app_json = self.load_json('oneContainerDown.json')
    mock_get_llap_app_status_data.return_value = llap_app_json

    status = self.hsi.check_llap_app_status(self.llap_app_name, 2)
    self.assertEqual(status, False)




  # Status : RUNNING_PARTIAL (4 out of 5 running -> > 80% instances ON)
  @patch("time.sleep")
  @patch('hive_server_interactive.HiveServerInteractiveDefault._get_llap_app_status_info')
  def test_check_llap_app_status_two_container_down_1_wait_negative(self, mock_get_llap_app_status_data, sleep_mock):
    sleep_mock.return_value = 1

    llap_app_json = self.load_json('oneContainerDown1.json')
    mock_get_llap_app_status_data.return_value = llap_app_json

    status = self.hsi.check_llap_app_status(self.llap_app_name, -1)
    self.assertEqual(status, True)

  @patch("time.sleep")
  @patch('hive_server_interactive.HiveServerInteractiveDefault._get_llap_app_status_info')
  def test_check_llap_app_status_two_container_down_1_wait_0(self, mock_get_llap_app_status_data, sleep_mock):
    sleep_mock.return_value = 1

    llap_app_json = self.load_json('oneContainerDown1.json')
    mock_get_llap_app_status_data.return_value = llap_app_json

    status = self.hsi.check_llap_app_status(self.llap_app_name, 0)
    self.assertEqual(status, True)


  @patch("time.sleep")
  @patch('hive_server_interactive.HiveServerInteractiveDefault._get_llap_app_status_info')
  def test_check_llap_app_status_two_container_down_1_wait_2(self, mock_get_llap_app_status_data, sleep_mock):
    sleep_mock.return_value = 1

    llap_app_json = self.load_json('oneContainerDown1.json')
    mock_get_llap_app_status_data.return_value = llap_app_json

    status = self.hsi.check_llap_app_status(self.llap_app_name, 2)
    self.assertEqual(status, True)




  # Status : LAUNCHING
  @patch("time.sleep")
  @patch('hive_server_interactive.HiveServerInteractiveDefault._get_llap_app_status_info')
  def test_check_llap_app_status_starting_wait_negative(self, mock_get_llap_app_status_data, sleep_mock):
    sleep_mock.return_value = 1

    llap_app_json = self.load_json('starting.json')
    mock_get_llap_app_status_data.return_value = llap_app_json

    status = self.hsi.check_llap_app_status(self.llap_app_name, -1)
    self.assertEqual(status, False)

  @patch("time.sleep")
  @patch('hive_server_interactive.HiveServerInteractiveDefault._get_llap_app_status_info')
  def test_check_llap_app_status_starting_wait_0(self, mock_get_llap_app_status_data, sleep_mock):
    sleep_mock.return_value = 1

    llap_app_json = self.load_json('starting.json')
    mock_get_llap_app_status_data.return_value = llap_app_json

    status = self.hsi.check_llap_app_status(self.llap_app_name, 0)
    self.assertEqual(status, False)

  @patch("time.sleep")
  @patch('hive_server_interactive.HiveServerInteractiveDefault._get_llap_app_status_info')
  def test_check_llap_app_status_starting_wait_2(self, mock_get_llap_app_status_data, sleep_mock):
    sleep_mock.return_value = 1

    llap_app_json = self.load_json('starting.json')
    mock_get_llap_app_status_data.return_value = llap_app_json

    status = self.hsi.check_llap_app_status(self.llap_app_name, 2)
    self.assertEqual(status, False)





  # Status : COMPLETE
  @patch("time.sleep")
  @patch('hive_server_interactive.HiveServerInteractiveDefault._get_llap_app_status_info')
  def test_check_llap_app_status_complete_wait_negative(self, mock_get_llap_app_status_data, sleep_mock):
    sleep_mock.return_value = 1

    llap_app_json = self.load_json('appComplete.json')
    mock_get_llap_app_status_data.return_value = llap_app_json

    status = self.hsi.check_llap_app_status(self.llap_app_name, -1)
    self.assertEqual(status, False)

  @patch("time.sleep")
  @patch('hive_server_interactive.HiveServerInteractiveDefault._get_llap_app_status_info')
  def test_check_llap_app_status_complete_wait_0(self, mock_get_llap_app_status_data, sleep_mock):
    sleep_mock.return_value = 1

    llap_app_json = self.load_json('appComplete.json')
    mock_get_llap_app_status_data.return_value = llap_app_json

    status = self.hsi.check_llap_app_status(self.llap_app_name, 0)
    self.assertEqual(status, False)

  @patch("time.sleep")
  @patch('hive_server_interactive.HiveServerInteractiveDefault._get_llap_app_status_info')
  def test_check_llap_app_status_complete_wait_2(self, mock_get_llap_app_status_data, sleep_mock):
    sleep_mock.return_value = 1

    llap_app_json = self.load_json('appComplete.json')
    mock_get_llap_app_status_data.return_value = llap_app_json

    status = self.hsi.check_llap_app_status(self.llap_app_name, 2)
    self.assertEqual(status, False)




  # Status : APP_NOT_FOUND
  @patch("time.sleep")
  @patch('hive_server_interactive.HiveServerInteractiveDefault._get_llap_app_status_info')
  def test_check_llap_app_status_invalid_wait_negative(self, mock_get_llap_app_status_data, sleep_mock):
    sleep_mock.return_value = 1

    llap_app_json = self.load_json('invalidApp.json')
    mock_get_llap_app_status_data.return_value = llap_app_json

    status = self.hsi.check_llap_app_status(self.llap_app_name, -1)
    self.assertEqual(status, False)


  @patch("time.sleep")
  @patch('hive_server_interactive.HiveServerInteractiveDefault._get_llap_app_status_info')
  def test_check_llap_app_status_invalid_wait_0(self, mock_get_llap_app_status_data, sleep_mock):
    sleep_mock.return_value = 1

    llap_app_json = self.load_json('invalidApp.json')
    mock_get_llap_app_status_data.return_value = llap_app_json

    status = self.hsi.check_llap_app_status(self.llap_app_name, 0)
    self.assertEqual(status, False)


  @patch("time.sleep")
  @patch('hive_server_interactive.HiveServerInteractiveDefault._get_llap_app_status_info')
  def test_check_llap_app_status_invalid_wait_2(self, mock_get_llap_app_status_data, sleep_mock):
    sleep_mock.return_value = 1

    llap_app_json = self.load_json('invalidApp.json')
    mock_get_llap_app_status_data.return_value = llap_app_json

    status = self.hsi.check_llap_app_status(self.llap_app_name, 2)
    self.assertEqual(status, False)