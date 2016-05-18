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
import  resource_management.libraries.functions

origin_exists = os.path.exists
@patch("platform.linux_distribution", new = MagicMock(return_value="Linux"))
@patch.object(resource_management.libraries.functions, "check_process_status", new = MagicMock())
@patch.object(os.path, "exists", new=MagicMock(
    side_effect=lambda *args: origin_exists(args[0])
    if args[0][-2:] == "j2" else True))
class TestAppTimelineServer(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "YARN/2.1.0.2.0/package"
  STACK_VERSION = "2.0.6"
  DEFAULT_IMMUTABLE_PATHS = ['/apps/hive/warehouse', '/apps/falcon', '/mr-history/done', '/app-logs', '/tmp']

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/application_timeline_server.py",
                       classname="ApplicationTimelineServer",
                       command="configure",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/application_timeline_server.py",
                       classname="ApplicationTimelineServer",
                       command="start",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_default()

    self.assertResourceCalled('File', '/var/run/hadoop-yarn/yarn/yarn-yarn-timelineserver.pid',
        action = ['delete'],
        not_if = "ambari-sudo.sh su yarn -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/hadoop-yarn/yarn/yarn-yarn-timelineserver.pid && ps -p `cat /var/run/hadoop-yarn/yarn/yarn-yarn-timelineserver.pid`'",
    )

    self.assertResourceCalled('File', '/var/log/hadoop-yarn/timeline/leveldb-timeline-store.ldb/LOCK',
        only_if='ls /var/log/hadoop-yarn/timeline/leveldb-timeline-store.ldb/LOCK',
        action=['delete'],
        not_if="ambari-sudo.sh su yarn -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/hadoop-yarn/yarn/yarn-yarn-timelineserver.pid && ps -p `cat /var/run/hadoop-yarn/yarn/yarn-yarn-timelineserver.pid`'",
        ignore_failures=True
    )

    self.assertResourceCalled('Execute', 'ulimit -c unlimited; export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop-yarn/sbin/yarn-daemon.sh --config /etc/hadoop/conf start timelineserver',
        not_if = "ambari-sudo.sh su yarn -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/hadoop-yarn/yarn/yarn-yarn-timelineserver.pid && ps -p `cat /var/run/hadoop-yarn/yarn/yarn-yarn-timelineserver.pid`'",
        user = 'yarn',
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su yarn -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/hadoop-yarn/yarn/yarn-yarn-timelineserver.pid && ps -p `cat /var/run/hadoop-yarn/yarn/yarn-yarn-timelineserver.pid`'",
        not_if = "ambari-sudo.sh su yarn -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/hadoop-yarn/yarn/yarn-yarn-timelineserver.pid && ps -p `cat /var/run/hadoop-yarn/yarn/yarn-yarn-timelineserver.pid`'",
        tries = 5,
        try_sleep = 1,
    )
    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/application_timeline_server.py",
                       classname="ApplicationTimelineServer",
                       command="stop",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Execute', 'export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop-yarn/sbin/yarn-daemon.sh --config /etc/hadoop/conf stop timelineserver',
                              user='yarn')

    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/var/run/hadoop-yarn',
                              owner = 'yarn',
                              group = 'hadoop',
                              create_parents = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop-yarn/yarn',
                              owner = 'yarn',
                              group = 'hadoop',
                              create_parents = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop-yarn/yarn',
                              owner = 'yarn',
                              group = 'hadoop',
                              create_parents = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop-mapreduce',
                              owner = 'mapred',
                              group = 'hadoop',
                              create_parents = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop-mapreduce/mapred',
                              owner = 'mapred',
                              group = 'hadoop',
                              create_parents = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop-mapreduce',
                              owner = 'mapred',
                              group = 'hadoop',
                              create_parents = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop-mapreduce/mapred',
                              owner = 'mapred',
                              group = 'hadoop',
                              create_parents = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop-yarn',
                              owner = 'yarn',
                              group = 'hadoop',
                              create_parents = True,
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
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0644,
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['hdfs-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['hdfs-site']
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
    self.assertResourceCalled('Directory', '/var/log/hadoop-yarn/timeline',
                              owner = 'yarn',
                              group = 'hadoop',
                              create_parents = True,
                              cd_access='a'
                              )
    self.assertResourceCalled('HdfsResource', None,
                              immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
                              security_enabled = False,
                              hadoop_bin_dir = '/usr/bin',
                              keytab = UnknownConfigurationMock(),
                              default_fs = 'hdfs://c6401.ambari.apache.org:8020',
                              dfs_type = '',
                              hdfs_site = self.getConfig()['configurations']['hdfs-site'],
                              kinit_path_local = '/usr/bin/kinit',
                              principal_name = UnknownConfigurationMock(),
                              user = 'hdfs',
                              action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                              hadoop_conf_dir = '/etc/hadoop/conf',
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
                              mode = 02050,
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/container-executor.cfg',
                              content = Template('container-executor.cfg.j2'),
                              group = 'hadoop',
                              mode = 0644,
                              )
    self.assertResourceCalled('Directory', '/cgroups_test/cpu',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              cd_access="a"
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/mapred-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['mapred-env']['content']),
                              mode = 0755,
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


  def test_status(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/application_timeline_server.py",
                       classname="ApplicationTimelineServer",
                       command="status",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Execute', 'mv /var/run/hadoop-yarn/yarn/yarn-yarn-historyserver.pid /var/run/hadoop-yarn/yarn/yarn-yarn-timelineserver.pid',
        only_if = 'test -e /var/run/hadoop-yarn/yarn/yarn-yarn-historyserver.pid',
    )
    self.assertNoMoreResources()


  @patch("resource_management.libraries.functions.security_commons.build_expectations")
  @patch("resource_management.libraries.functions.security_commons.get_params_from_filesystem")
  @patch("resource_management.libraries.functions.security_commons.validate_security_config_properties")
  @patch("resource_management.libraries.functions.security_commons.cached_kinit_executor")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  def test_security_status(self, put_structured_out_mock, cached_kinit_executor_mock, validate_security_config_mock, get_params_mock, build_exp_mock):
    # Test that function works when is called with correct parameters

    security_params = {
      'yarn-site': {
        'yarn.timeline-service.keytab': '/path/to/applicationtimeline/keytab',
        'yarn.timeline-service.principal': 'applicationtimeline_principal',
        'yarn.timeline-service.http-authentication.kerberos.keytab': 'path/to/timeline/kerberos/keytab',
        'yarn.timeline-service.http-authentication.kerberos.principal': 'timeline_principal'
      }
    }
    result_issues = []
    props_value_check = {"yarn.timeline-service.enabled": "true",
                         "yarn.timeline-service.http-authentication.type": "kerberos",
                         "yarn.acl.enable": "true"}
    props_empty_check = ["yarn.timeline-service.principal",
                         "yarn.timeline-service.keytab",
                         "yarn.timeline-service.http-authentication.kerberos.principal",
                         "yarn.timeline-service.http-authentication.kerberos.keytab"]

    props_read_check = ["yarn.timeline-service.keytab",
                        "yarn.timeline-service.http-authentication.kerberos.keytab"]

    get_params_mock.return_value = security_params
    validate_security_config_mock.return_value = result_issues

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/application_timeline_server.py",
                       classname="ApplicationTimelineServer",
                       command="security_status",
                       config_file="secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    build_exp_mock.assert_called_with('yarn-site', props_value_check, props_empty_check, props_read_check)
    put_structured_out_mock.assert_called_with({"securityState": "SECURED_KERBEROS"})
    self.assertTrue(cached_kinit_executor_mock.call_count, 2)
    cached_kinit_executor_mock.assert_called_with('/usr/bin/kinit',
                                                  self.config_dict['configurations']['yarn-env']['yarn_user'],
                                                  security_params['yarn-site']['yarn.timeline-service.http-authentication.kerberos.keytab'],
                                                  security_params['yarn-site']['yarn.timeline-service.http-authentication.kerberos.principal'],
                                                  self.config_dict['hostname'],
                                                  '/tmp')

    # Testing that the exception throw by cached_executor is caught
    cached_kinit_executor_mock.reset_mock()
    cached_kinit_executor_mock.side_effect = Exception("Invalid command")

    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/application_timeline_server.py",
                         classname="ApplicationTimelineServer",
                         command="security_status",
                         config_file="secured.json",
                         stack_version = self.STACK_VERSION,
                         target = RMFTestCase.TARGET_COMMON_SERVICES
      )
    except:
      self.assertTrue(True)

    # Testing with a security_params which doesn't contains yarn-site
    empty_security_params = {}
    cached_kinit_executor_mock.reset_mock()
    get_params_mock.reset_mock()
    put_structured_out_mock.reset_mock()
    get_params_mock.return_value = empty_security_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/application_timeline_server.py",
                       classname="ApplicationTimelineServer",
                       command="security_status",
                       config_file="secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityIssuesFound": "Keytab file or principal are not set property."})

    # Testing with not empty result_issues
    result_issues_with_params = {
      'yarn-site': "Something bad happened"
    }

    validate_security_config_mock.reset_mock()
    get_params_mock.reset_mock()
    validate_security_config_mock.return_value = result_issues_with_params
    get_params_mock.return_value = security_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/application_timeline_server.py",
                       classname="ApplicationTimelineServer",
                       command="security_status",
                       config_file="secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})

    # Testing with security_enable = false
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/application_timeline_server.py",
                       classname="ApplicationTimelineServer",
                       command="security_status",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})

  @patch.object(resource_management.libraries.functions, "get_stack_version", new = MagicMock(return_value='2.3.0.0-1234'))
  def test_pre_upgrade_restart_23(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/application_timeline_server.py",
                       classname = "ApplicationTimelineServer",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = [(0, None, ''), (0, None, '')],
                       mocks_dict = mocks_dict)

    self.assertResourceCalledIgnoreEarlier('Execute', ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'hadoop-yarn-timelineserver', version), sudo=True)
    self.assertNoMoreResources()

    self.assertEquals(1, mocks_dict['call'].call_count)
    self.assertEquals(1, mocks_dict['checked_call'].call_count)
    self.assertEquals(
      ('ambari-python-wrap', '/usr/bin/conf-select', 'set-conf-dir', '--package', 'hadoop', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['checked_call'].call_args_list[0][0][0])
    self.assertEquals(
      ('ambari-python-wrap', '/usr/bin/conf-select', 'create-conf-dir', '--package', 'hadoop', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['call'].call_args_list[0][0][0])
