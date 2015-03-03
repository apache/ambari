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
import json
from mock.mock import MagicMock, patch
from resource_management.core import shell
from resource_management.core.exceptions import Fail

class TestDatanode(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "HDFS/2.1.0.2.0/package"
  STACK_VERSION = "2.0.6"

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                       classname = "DataNode",
                       command = "configure",
                       config_file = "default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                       classname = "DataNode",
                       command = "start",
                       config_file = "default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid',
                              action = ['delete'],
                              not_if='ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps -p `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
                              )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf start datanode'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        not_if = 'ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps -p `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
    )
    self.assertNoMoreResources()

  @patch("os.path.exists", new = MagicMock(return_value=False))
  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                       classname = "DataNode",
                       command = "stop",
                       config_file = "default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid',
                              action = ['delete'],
                              not_if='ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps -p `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
                              )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf stop datanode'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        not_if = None,
    )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid',
                              action = ['delete'],
                              )
    self.assertNoMoreResources()

  def test_configure_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                       classname = "DataNode",
                       command = "configure",
                       config_file = "secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()

  def test_start_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                       classname = "DataNode",
                       command = "start",
                       config_file = "secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid',
                              action = ['delete'],
                              not_if='ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps -p `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
                              )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf start datanode',
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        not_if = 'ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps -p `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
    )
    self.assertNoMoreResources()

  def test_start_secured_HDP22_root(self):
    config_file = self._getSrcFolder()+"/test/python/stacks/2.0.6/configs/secured.json"
    with open(config_file, "r") as f:
      secured_json = json.load(f)

    secured_json['hostLevelParams']['stack_version']= '2.2'

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                       classname = "DataNode",
                       command = "start",
                       config_dict = secured_json,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid',
                              action = ['delete'],
                              not_if='ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps -p `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
                              )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E /usr/hdp/current/hadoop-client/sbin/hadoop-daemon.sh --config /etc/hadoop/conf start datanode',
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/hdp/current/hadoop-client/libexec'},
        not_if = 'ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps -p `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
    )
    self.assertNoMoreResources()

  def test_start_secured_HDP22_non_root_https_only(self):
    config_file = self._getSrcFolder()+"/test/python/stacks/2.0.6/configs/secured.json"
    with open(config_file, "r") as f:
      secured_json = json.load(f)

    secured_json['hostLevelParams']['stack_version']= '2.2'
    secured_json['configurations']['hdfs-site']['dfs.http.policy']= 'HTTPS_ONLY'
    secured_json['configurations']['hdfs-site']['dfs.datanode.address']= '0.0.0.0:10000'
    secured_json['configurations']['hdfs-site']['dfs.datanode.https.address']= '0.0.0.0:50000'

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                       classname = "DataNode",
                       command = "start",
                       config_dict = secured_json,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid',
                              action = ['delete'],
                              not_if='ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps -p `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
                              )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/hdp/current/hadoop-client/sbin/hadoop-daemon.sh --config /etc/hadoop/conf start datanode'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/hdp/current/hadoop-client/libexec'},
        not_if = 'ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps -p `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
    )
    self.assertNoMoreResources()

  @patch("os.path.exists", new = MagicMock(return_value=False))
  def test_stop_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                       classname = "DataNode",
                       command = "stop",
                       config_file = "secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid',
                              action = ['delete'],
                              not_if='ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps -p `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
                              )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf stop datanode',
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        not_if = None,
    )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid',
                              action = ['delete'],
                              )
    self.assertNoMoreResources()


  @patch("os.path.exists", new = MagicMock(return_value=False))
  def test_stop_secured_HDP22_root(self):
    config_file = self._getSrcFolder()+"/test/python/stacks/2.0.6/configs/secured.json"
    with open(config_file, "r") as f:
      secured_json = json.load(f)

    secured_json['hostLevelParams']['stack_version']= '2.2'

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                       classname = "DataNode",
                       command = "stop",
                       config_dict = secured_json,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid',
                              action = ['delete'],
                              not_if='ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps -p `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
                              )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E /usr/hdp/current/hadoop-client/sbin/hadoop-daemon.sh --config /etc/hadoop/conf stop datanode',
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/hdp/current/hadoop-client/libexec'},
        not_if = None,
    )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid',
                              action = ['delete'],
                              )
    self.assertNoMoreResources()

  @patch("os.path.exists", new = MagicMock(return_value=False))
  def test_stop_secured_HDP22_non_root_https_only(self):
    config_file = self._getSrcFolder()+"/test/python/stacks/2.0.6/configs/secured.json"
    with open(config_file, "r") as f:
      secured_json = json.load(f)

    secured_json['hostLevelParams']['stack_version']= '2.2'
    secured_json['configurations']['hdfs-site']['dfs.http.policy']= 'HTTPS_ONLY'
    secured_json['configurations']['hdfs-site']['dfs.datanode.address']= '0.0.0.0:10000'
    secured_json['configurations']['hdfs-site']['dfs.datanode.https.address']= '0.0.0.0:50000'

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                       classname = "DataNode",
                       command = "stop",
                       config_dict = secured_json,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid',
                              action = ['delete'],
                              not_if='ls /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid >/dev/null 2>&1 && ps -p `cat /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid` >/dev/null 2>&1',
                              )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/hdp/current/hadoop-client/sbin/hadoop-daemon.sh --config /etc/hadoop/conf stop datanode'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/hdp/current/hadoop-client/libexec'},
        not_if = None,
    )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid',
                              action=['delete'],
                              )
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
                              owner = 'root',
                              group = 'root',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/etc/security/limits.d/hdfs.conf',
                              content = Template('hdfs.conf.j2'),
                              owner = 'root',
                              group = 'root',
                              mode = 0644,
                              )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['hdfs-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['hdfs-site']
                              )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['core-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['core-site'],
                              mode = 0644
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/slaves',
                              content = Template('slaves.j2'),
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('Directory', '/var/lib/hadoop-hdfs',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0751,
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/hadoop/hdfs/data',
                              owner = 'hdfs',
                              ignore_failures = True,
                              group = 'hadoop',
                              mode = 0755,
                              recursive = True,
                              cd_access='a'
                              )

  def assert_configure_secured(self):
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
                              owner = 'root',
                              group = 'root',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/etc/security/limits.d/hdfs.conf',
                              content = Template('hdfs.conf.j2'),
                              owner = 'root',
                              group = 'root',
                              mode = 0644,
                              )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['hdfs-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['hdfs-site']
                              )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['core-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['core-site'],
                              mode = 0644
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/slaves',
                              content = Template('slaves.j2'),
                              owner = 'root',
                              )
    self.assertResourceCalled('Directory', '/var/lib/hadoop-hdfs',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0751,
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/hadoop/hdfs/data',
                              owner = 'hdfs',
                              ignore_failures = True,
                              group = 'hadoop',
                              mode = 0755,
                              recursive = True,
                              cd_access='a'
                              )


  @patch('time.sleep')
  @patch.object(shell, "call")
  def test_post_rolling_restart(self, process_mock, time_mock):
    process_output = """
      Live datanodes (2):

      Name: 192.168.64.102:50010 (c6401.ambari.apache.org)
      Hostname: c6401.ambari.apache.org
      Decommission Status : Normal
      Configured Capacity: 524208947200 (488.21 GB)
      DFS Used: 193069056 (184.13 MB)
      Non DFS Used: 29264986112 (27.26 GB)
      DFS Remaining: 494750892032 (460.77 GB)
      DFS Used%: 0.04%
      DFS Remaining%: 94.38%
      Configured Cache Capacity: 0 (0 B)
      Cache Used: 0 (0 B)
      Cache Remaining: 0 (0 B)
      Cache Used%: 100.00%
      Cache Remaining%: 0.00%
      Xceivers: 2
      Last contact: Fri Dec 12 20:47:21 UTC 2014
    """

    process_mock.return_value = (0, process_output)

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                       classname = "DataNode",
                       command = "post_rolling_restart",
                       config_file = "default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertTrue(process_mock.called)
    self.assertEqual(process_mock.call_count,1)


  @patch('time.sleep')
  @patch.object(shell, "call")
  def test_post_rolling_restart_datanode_not_ready(self, process_mock, time_mock):
    process_mock.return_value = (0, 'There are no DataNodes here!')

    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                         classname = "DataNode",
                         command = "post_rolling_restart",
                         config_file = "default.json",
                         hdp_stack_version = self.STACK_VERSION,
                         target = RMFTestCase.TARGET_COMMON_SERVICES
      )
      self.fail('Missing DataNode should have caused a failure')
    except Fail,fail:
      self.assertTrue(process_mock.called)
      self.assertEqual(process_mock.call_count,12)


  @patch('time.sleep')
  @patch.object(shell, "call")
  def test_post_rolling_restart_bad_returncode(self, process_mock, time_mock):
    process_mock.return_value = (0, 'some')

    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                         classname = "DataNode",
                         command = "post_rolling_restart",
                         config_file = "default.json",
                         hdp_stack_version = self.STACK_VERSION,
                         target = RMFTestCase.TARGET_COMMON_SERVICES
      )
      self.fail('Invalid return code should cause a failure')
    except Fail,fail:
      self.assertTrue(process_mock.called)
      self.assertEqual(process_mock.call_count,12)

  @patch("resource_management.libraries.functions.security_commons.build_expectations")
  @patch("resource_management.libraries.functions.security_commons.get_params_from_filesystem")
  @patch("resource_management.libraries.functions.security_commons.validate_security_config_properties")
  @patch("resource_management.libraries.functions.security_commons.cached_kinit_executor")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  def test_security_status(self, put_structured_out_mock, cached_kinit_executor_mock, validate_security_config_mock, get_params_mock, build_exp_mock):
    # Test that function works when is called with correct parameters

    security_params = {
      'core-site': {
        'hadoop.security.authentication': 'kerberos'
      },
      'hdfs-site': {
        'dfs.datanode.keytab.file': 'path/to/datanode/keytab/file',
        'dfs.datanode.kerberos.principal': 'datanode_principal'
      }
    }

    props_value_check = None
    props_empty_check = ['dfs.datanode.keytab.file',
                         'dfs.datanode.kerberos.principal']
    props_read_check = ['dfs.datanode.keytab.file']

    result_issues = []

    get_params_mock.return_value = security_params
    validate_security_config_mock.return_value = result_issues

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                       classname = "DataNode",
                       command = "security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    build_exp_mock.assert_called_with('hdfs-site', props_value_check, props_empty_check, props_read_check)
    put_structured_out_mock.assert_called_with({"securityState": "SECURED_KERBEROS"})
    cached_kinit_executor_mock.called_with('/usr/bin/kinit',
                                           self.config_dict['configurations']['hadoop-env']['hdfs_user'],
                                           security_params['hdfs-site']['dfs.datanode.keytab.file'],
                                           security_params['hdfs-site']['dfs.datanode.kerberos.principal'],
                                           self.config_dict['hostname'],
                                           '/tmp')

    # Testing when hadoop.security.authentication is simple
    security_params['core-site']['hadoop.security.authentication'] = 'simple'

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                       classname = "DataNode",
                       command = "security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})
    security_params['core-site']['hadoop.security.authentication'] = 'kerberos'

    # Testing that the exception throw by cached_executor is caught
    cached_kinit_executor_mock.reset_mock()
    cached_kinit_executor_mock.side_effect = Exception("Invalid command")

    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                         classname = "DataNode",
                         command = "security_status",
                         config_file="secured.json",
                         hdp_stack_version = self.STACK_VERSION,
                         target = RMFTestCase.TARGET_COMMON_SERVICES
      )
    except:
      self.assertTrue(True)

    # Testing with a security_params which doesn't contains hdfs-site
    empty_security_params = {}
    empty_security_params['core-site'] = {}
    empty_security_params['core-site']['hadoop.security.authentication'] = 'kerberos'
    cached_kinit_executor_mock.reset_mock()
    get_params_mock.reset_mock()
    put_structured_out_mock.reset_mock()
    get_params_mock.return_value = empty_security_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                       classname = "DataNode",
                       command = "security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    put_structured_out_mock.assert_called_with({"securityIssuesFound": "Keytab file or principal are not set property."})

    # Testing with not empty result_issues
    result_issues_with_params = {}
    result_issues_with_params['hdfs-site']="Something bad happened"

    validate_security_config_mock.reset_mock()
    get_params_mock.reset_mock()
    validate_security_config_mock.return_value = result_issues_with_params
    get_params_mock.return_value = security_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                       classname = "DataNode",
                       command = "security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})
