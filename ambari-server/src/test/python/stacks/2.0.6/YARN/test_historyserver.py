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
import os

origin_exists = os.path.exists
@patch.object(os.path, "exists", new=MagicMock(
  side_effect=lambda *args: origin_exists(args[0])
  if args[0][-2:] == "j2" else True))
class TestHistoryServer(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "YARN/2.1.0.2.0/package"
  STACK_VERSION = "2.0.6"
  
  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/historyserver.py",
                       classname="HistoryServer",
                       command="configure",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/historyserver.py",
                       classname="HistoryServer",
                       command="start",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()

    pid_check_cmd = 'ls /var/run/hadoop-mapreduce/mapred/mapred-mapred-historyserver.pid >/dev/null 2>&1 && ps -p `cat /var/run/hadoop-mapreduce/mapred/mapred-mapred-historyserver.pid` >/dev/null 2>&1'

    self.assertResourceCalled('File', '/var/run/hadoop-mapreduce/mapred/mapred-mapred-historyserver.pid',
                              not_if=pid_check_cmd,
                              action=['delete'])
    self.assertResourceCalled('Execute', 'ulimit -c unlimited; export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop-mapreduce/sbin/mr-jobhistory-daemon.sh --config /etc/hadoop/conf start historyserver',
                              not_if=pid_check_cmd,
                              user='mapred')
    self.assertResourceCalled('Execute', pid_check_cmd,
                              not_if=pid_check_cmd,
                              initial_wait=5,
                              user='mapred')
    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/historyserver.py",
                       classname="HistoryServer",
                       command="stop",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Execute', 'export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop-mapreduce/sbin/mr-jobhistory-daemon.sh --config /etc/hadoop/conf stop historyserver',
                              user='mapred')
    self.assertResourceCalled('File', '/var/run/hadoop-mapreduce/mapred/mapred-mapred-historyserver.pid',
                              action=['delete'])
    self.assertNoMoreResources()

  def test_configure_secured(self):

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/historyserver.py",
                       classname="HistoryServer",
                       command="configure",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()

  def test_start_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/historyserver.py",
                       classname="HistoryServer",
                       command="start",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_secured()

    pid_check_cmd = 'ls /var/run/hadoop-mapreduce/mapred/mapred-mapred-historyserver.pid >/dev/null 2>&1 && ps -p `cat /var/run/hadoop-mapreduce/mapred/mapred-mapred-historyserver.pid` >/dev/null 2>&1'

    self.assertResourceCalled('File', '/var/run/hadoop-mapreduce/mapred/mapred-mapred-historyserver.pid',
                              not_if=pid_check_cmd,
                              action=['delete'])
    self.assertResourceCalled('Execute', 'ulimit -c unlimited; export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop-mapreduce/sbin/mr-jobhistory-daemon.sh --config /etc/hadoop/conf start historyserver',
                              not_if=pid_check_cmd,
                              user='mapred')
    self.assertResourceCalled('Execute', pid_check_cmd,
                              user='mapred',
                              not_if=pid_check_cmd,
                              initial_wait=5)
    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/historyserver.py",
                       classname="HistoryServer",
                       command="stop",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Execute', 'export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop-mapreduce/sbin/mr-jobhistory-daemon.sh --config /etc/hadoop/conf stop historyserver',
                              user='mapred')

    self.assertResourceCalled('File', '/var/run/hadoop-mapreduce/mapred/mapred-mapred-historyserver.pid',
                              action=['delete'])
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('HdfsDirectory', '/app-logs',
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = "/usr/bin/kinit",
                              recursive_chmod = True,
                              owner = 'yarn',
                              group = 'hadoop',
                              action = ['create_delayed'],
                              mode = 0777,
                              bin_dir = '/usr/bin'
                              )
    self.assertResourceCalled('HdfsDirectory', '/mapred',
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = "/usr/bin/kinit",
                              owner = 'mapred',
                              bin_dir = '/usr/bin',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', '/mapred/system',
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = "/usr/bin/kinit",
                              owner = 'hdfs',
                              bin_dir = '/usr/bin',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', '/mr-history/tmp',
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = "/usr/bin/kinit",
                              mode = 0777,
                              owner = 'mapred',
                              group = 'hadoop',
                              bin_dir = '/usr/bin',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', '/mr-history/done',
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = "/usr/bin/kinit",
                              mode = 01777,
                              owner = 'mapred',
                              group = 'hadoop',
                              bin_dir = '/usr/bin',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', None,
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = "/usr/bin/kinit",
                              bin_dir = '/usr/bin',
                              action = ['create'],
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop-yarn',
      owner = 'yarn',
      group = 'hadoop',
      recursive = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop-yarn/yarn',
      owner = 'yarn',
      group = 'hadoop',
      recursive = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-yarn/yarn',
      owner = 'yarn',
      group = 'hadoop',
      recursive = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop-mapreduce',
      owner = 'mapred',
      group = 'hadoop',
      recursive = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop-mapreduce/mapred',
      owner = 'mapred',
      group = 'hadoop',
      recursive = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-mapreduce',
      owner = 'mapred',
      group = 'hadoop',
      recursive = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-mapreduce/mapred',
      owner = 'mapred',
      group = 'hadoop',
      recursive = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-yarn',
      owner = 'yarn',
      recursive = True,
      ignore_failures = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
      owner = 'hdfs',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['core-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['core-site']
    )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['mapred-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['mapred-site']
    )
    self.assertResourceCalled('XmlConfig', 'yarn-site.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['yarn-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['yarn-site']
    )
    self.assertResourceCalled('XmlConfig', 'capacity-scheduler.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['capacity-scheduler'],
      configuration_attributes = self.getConfig()['configuration_attributes']['capacity-scheduler']
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/yarn.exclude',
      owner = 'yarn',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/yarn.conf',
      content = Template('yarn.conf.j2'),
      mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/mapreduce.conf',
      content = Template('mapreduce.conf.j2'),
      mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/yarn-env.sh',
      content = InlineTemplate(self.getConfig()['configurations']['yarn-env']['content']),
      owner = 'yarn',
      group = 'hadoop',
      mode = 0755,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/mapred-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['mapred-env']['content']),
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/taskcontroller.cfg',
                              content = Template('taskcontroller.cfg.j2'),
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
                              owner = 'mapred',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['mapred-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['mapred-site']
                              )
    self.assertResourceCalled('XmlConfig', 'capacity-scheduler.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['capacity-scheduler'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['capacity-scheduler']
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/fair-scheduler.xml',
                              owner = 'mapred',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/ssl-client.xml.example',
                              owner = 'mapred',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/ssl-server.xml.example',
                              owner = 'mapred',
                              group = 'hadoop',
                              )

  def assert_configure_secured(self):
    self.assertResourceCalled('HdfsDirectory', '/app-logs',
                              security_enabled = True,
                              keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              recursive_chmod = True,
                              owner = 'yarn',
                              group = 'hadoop',
                              action = ['create_delayed'],
                              bin_dir = '/usr/bin',
                              mode = 0777,
                              )
    self.assertResourceCalled('HdfsDirectory', '/mapred',
                              security_enabled = True,
                              keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              owner = 'mapred',
                              bin_dir = '/usr/bin',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', '/mapred/system',
                              security_enabled = True,
                              keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              owner = 'hdfs',
                              bin_dir = '/usr/bin',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', '/mr-history/tmp',
                              security_enabled = True,
                              keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              mode = 0777,
                              owner = 'mapred',
                              group = 'hadoop',
                              bin_dir = '/usr/bin',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', '/mr-history/done',
                              security_enabled = True,
                              keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              mode = 01777,
                              owner = 'mapred',
                              group = 'hadoop',
                              bin_dir = '/usr/bin',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', None,
                              security_enabled = True,
                              keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              bin_dir = '/usr/bin',
                              action = ['create'],
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop-yarn',
      owner = 'yarn',
      group = 'hadoop',
      recursive = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop-yarn/yarn',
      owner = 'yarn',
      group = 'hadoop',
      recursive = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-yarn/yarn',
      owner = 'yarn',
      group = 'hadoop',
      recursive = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop-mapreduce',
      owner = 'mapred',
      group = 'hadoop',
      recursive = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop-mapreduce/mapred',
      owner = 'mapred',
      group = 'hadoop',
      recursive = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-mapreduce',
      owner = 'mapred',
      group = 'hadoop',
      recursive = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-mapreduce/mapred',
      owner = 'mapred',
      group = 'hadoop',
      recursive = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-yarn',
      owner = 'yarn',
      recursive = True,
      ignore_failures = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
      owner = 'hdfs',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['core-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['core-site']
    )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['mapred-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['mapred-site']
    )
    self.assertResourceCalled('XmlConfig', 'yarn-site.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['yarn-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['yarn-site']
    )
    self.assertResourceCalled('XmlConfig', 'capacity-scheduler.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['capacity-scheduler'],
      configuration_attributes = self.getConfig()['configuration_attributes']['capacity-scheduler']
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/yarn.exclude',
      owner = 'yarn',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/yarn.conf',
      content = Template('yarn.conf.j2'),
      mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/mapreduce.conf',
      content = Template('mapreduce.conf.j2'),
      mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/yarn-env.sh',
      content = InlineTemplate(self.getConfig()['configurations']['yarn-env']['content']),
      owner = 'yarn',
      group = 'hadoop',
      mode = 0755,
    )
    self.assertResourceCalled('File', '/usr/lib/hadoop-yarn/bin/container-executor',
      group = 'hadoop',
      mode = 06050,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/container-executor.cfg',
      content = Template('container-executor.cfg.j2'),
      group = 'hadoop',
      mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/mapred-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['mapred-env']['content']),
                              owner = 'root',
                              )
    self.assertResourceCalled('File', '/usr/lib/hadoop/sbin/task-controller',
                              owner = 'root',
                              group = 'hadoop',
                              mode = 06050,
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/taskcontroller.cfg',
                              content = Template('taskcontroller.cfg.j2'),
                              owner = 'root',
                              group = 'hadoop',
                              mode = 0644,
                              )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
                              owner = 'mapred',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['mapred-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['mapred-site']
                              )
    self.assertResourceCalled('XmlConfig', 'capacity-scheduler.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['capacity-scheduler'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['capacity-scheduler']
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/fair-scheduler.xml',
                              owner = 'mapred',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/ssl-client.xml.example',
                              owner = 'mapred',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/ssl-server.xml.example',
                              owner = 'mapred',
                              group = 'hadoop',
                              )

  @patch("resource_management.libraries.functions.security_commons.build_expectations")
  @patch("resource_management.libraries.functions.security_commons.get_params_from_filesystem")
  @patch("resource_management.libraries.functions.security_commons.validate_security_config_properties")
  @patch("resource_management.libraries.functions.security_commons.cached_kinit_executor")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  def test_security_status(self, put_structured_out_mock, cached_kinit_executor_mock, validate_security_config_mock, get_params_mock, build_exp_mock):
    # Test that function works when is called with correct parameters

    security_params = {
      "mapred-site": {
        'mapreduce.jobhistory.keytab': "/path/to/keytab1",
        'mapreduce.jobhistory.principal': "principal1",
        'mapreduce.jobhistory.webapp.spnego-keytab-file': "/path/to/keytab2",
        'mapreduce.jobhistory.webapp.spnego-principal': "principal2"
      }
    }
    result_issues = []

    get_params_mock.return_value = security_params
    validate_security_config_mock.return_value = result_issues

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/historyserver.py",
                       classname="HistoryServer",
                       command="security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    get_params_mock.assert_called_with("/etc/hadoop/conf", {'mapred-site.xml': 'XML'})
    build_exp_mock.assert_called_with('mapred-site',
                                      None,
                                      [
                                        'mapreduce.jobhistory.keytab',
                                        'mapreduce.jobhistory.principal',
                                        'mapreduce.jobhistory.webapp.spnego-keytab-file',
                                        'mapreduce.jobhistory.webapp.spnego-principal'
                                        ],
                                      None)
    put_structured_out_mock.assert_called_with({"securityState": "SECURED_KERBEROS"})
    self.assertTrue(cached_kinit_executor_mock.call_count, 2)
    cached_kinit_executor_mock.assert_called_with('/usr/bin/kinit',
                                                  self.config_dict['configurations']['mapred-env']['mapred_user'],
                                                  security_params['mapred-site']['mapreduce.jobhistory.webapp.spnego-keytab-file'],
                                                  security_params['mapred-site']['mapreduce.jobhistory.webapp.spnego-principal'],
                                                  self.config_dict['hostname'],
                                                  '/tmp')

    # Testing that the exception throw by cached_executor is caught
    cached_kinit_executor_mock.reset_mock()
    cached_kinit_executor_mock.side_effect = Exception("Invalid command")

    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/historyserver.py",
                         classname="HistoryServer",
                         command="security_status",
                         config_file="secured.json",
                         hdp_stack_version = self.STACK_VERSION,
                         target = RMFTestCase.TARGET_COMMON_SERVICES
      )
    except:
      self.assertTrue(True)

    # Testing with a security_params which doesn't contain mapred-site
    empty_security_params = {}
    cached_kinit_executor_mock.reset_mock()
    get_params_mock.reset_mock()
    put_structured_out_mock.reset_mock()
    get_params_mock.return_value = empty_security_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/historyserver.py",
                       classname="HistoryServer",
                       command="security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityIssuesFound": "Keytab file or principal not set."})

    # Testing with not empty result_issues
    result_issues_with_params = {'mapred-site': "Something bad happened"}

    validate_security_config_mock.reset_mock()
    get_params_mock.reset_mock()
    validate_security_config_mock.return_value = result_issues_with_params
    get_params_mock.return_value = security_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/historyserver.py",
                       classname="HistoryServer",
                       command="security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})

    # Testing with security_enable = false
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/historyserver.py",
                       classname="HistoryServer",
                       command="security_status",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})
