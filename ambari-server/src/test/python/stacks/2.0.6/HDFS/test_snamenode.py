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
from ambari_commons import OSCheck
from mock.mock import MagicMock, patch

class TestSNamenode(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "HDFS/2.1.0.2.0/package"
  STACK_VERSION = "2.0.6"

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/snamenode.py",
                       classname = "SNameNode",
                       command = "configure",
                       config_file = "default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertResourceCalled('File', '/etc/hadoop/conf/dfs.exclude',
                              owner = 'hdfs',
                              content = Template('exclude_hosts_list.j2'),
                              group = 'hadoop',
                              )
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/snamenode.py",
                       classname = "SNameNode",
                       command = "start",
                       config_file = "default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertResourceCalled('File', '/etc/hadoop/conf/dfs.exclude',
                              owner = 'hdfs',
                              content = Template('exclude_hosts_list.j2'),
                              group = 'hadoop',
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
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-secondarynamenode.pid',
        action = ['delete'],
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-secondarynamenode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-secondarynamenode.pid",
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf start secondarynamenode'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-secondarynamenode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-secondarynamenode.pid",
    )
    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/snamenode.py",
                       classname = "SNameNode",
                       command = "stop",
                       config_file = "default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf stop secondarynamenode'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        only_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-secondarynamenode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-secondarynamenode.pid")

    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-secondarynamenode.pid',action = ['delete'])
    self.assertNoMoreResources()

  def test_configure_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/snamenode.py",
                       classname = "SNameNode",
                       command = "configure",
                       config_file = "secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertResourceCalled('File', '/etc/hadoop/conf/dfs.exclude',
                              owner = 'hdfs',
                              content = Template('exclude_hosts_list.j2'),
                              group = 'hadoop',
                              )
    self.assertNoMoreResources()

  def test_start_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/snamenode.py",
                       classname = "SNameNode",
                       command = "start",
                       config_file = "secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertResourceCalled('File', '/etc/hadoop/conf/dfs.exclude',
                              owner = 'hdfs',
                              content = Template('exclude_hosts_list.j2'),
                              group = 'hadoop',
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
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-secondarynamenode.pid',
        action = ['delete'],
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-secondarynamenode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-secondarynamenode.pid",
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf start secondarynamenode'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-secondarynamenode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-secondarynamenode.pid",
    )
    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/snamenode.py",
                       classname = "SNameNode",
                       command = "stop",
                       config_file = "secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf stop secondarynamenode'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        only_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-secondarynamenode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-secondarynamenode.pid")

    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-secondarynamenode.pid',action = ['delete'])
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/usr/lib/hadoop/lib/native/Linux-i386-32',
        recursive = True,
    )
    self.assertResourceCalled('Directory', '/usr/lib/hadoop/lib/native/Linux-amd64-64',
        recursive = True,
    )
    self.assertResourceCalled('Link', '/usr/lib/hadoop/lib/native/Linux-i386-32/libsnappy.so',
        to = '/usr/lib/hadoop/lib/libsnappy.so',
    )
    self.assertResourceCalled('Link', '/usr/lib/hadoop/lib/native/Linux-amd64-64/libsnappy.so',
        to = '/usr/lib/hadoop/lib64/libsnappy.so',
    )
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

    self.assertResourceCalled('Directory', '/hadoop/hdfs/namesecondary',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755,
                              recursive = True,
                              cd_access='a'
                              )
    self.assertResourceCalled('Directory', '/hadoop/hdfs/namesecondary2',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755,
                              recursive = True,
                              cd_access='a'
    )

  def assert_configure_secured(self):
    self.assertResourceCalled('Directory', '/usr/lib/hadoop/lib/native/Linux-i386-32',
        recursive = True,
    )
    self.assertResourceCalled('Directory', '/usr/lib/hadoop/lib/native/Linux-amd64-64',
        recursive = True,
    )
    self.assertResourceCalled('Link', '/usr/lib/hadoop/lib/native/Linux-i386-32/libsnappy.so',
        to = '/usr/lib/hadoop/lib/libsnappy.so',
    )
    self.assertResourceCalled('Link', '/usr/lib/hadoop/lib/native/Linux-amd64-64/libsnappy.so',
        to = '/usr/lib/hadoop/lib64/libsnappy.so',
    )
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

    self.assertResourceCalled('Directory', '/hadoop/hdfs/namesecondary',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755,
                              recursive = True,
                              cd_access='a'
                              )

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
        'dfs.secondary.namenode.keytab.file': 'path/to/snamenode/keytab/file',
        'dfs.secondary.namenode.kerberos.principal': 'snamenode_principal'
      }
    }

    props_value_check = None
    props_empty_check = ['dfs.secondary.namenode.kerberos.internal.spnego.principal',
                         'dfs.secondary.namenode.keytab.file',
                         'dfs.secondary.namenode.kerberos.principal']
    props_read_check = ['dfs.secondary.namenode.keytab.file']

    result_issues = []

    get_params_mock.return_value = security_params
    validate_security_config_mock.return_value = result_issues

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/snamenode.py",
                       classname = "SNameNode",
                       command = "security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    build_exp_mock.assert_called_with('hdfs-site', props_value_check, props_empty_check, props_read_check)
    put_structured_out_mock.assert_called_with({"securityState": "SECURED_KERBEROS"})
    cached_kinit_executor_mock.called_with('/usr/bin/kinit',
                                           self.config_dict['configurations']['hadoop-env']['hdfs_user'],
                                           security_params['hdfs-site']['dfs.secondary.namenode.keytab.file'],
                                           security_params['hdfs-site']['dfs.secondary.namenode.kerberos.principal'],
                                           self.config_dict['hostname'],
                                           '/tmp')

    # Testing when hadoop.security.authentication is simple
    security_params['core-site']['hadoop.security.authentication'] = 'simple'

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/snamenode.py",
                       classname = "SNameNode",
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
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/snamenode.py",
                         classname = "SNameNode",
                         command = "security_status",
                         config_file="secured.json",
                         hdp_stack_version = self.STACK_VERSION,
                         target = RMFTestCase.TARGET_COMMON_SERVICES
      )
    except:
      self.assertTrue(True)

    # Testing with a security_params which doesn't contains hdfs-site
    empty_security_params = {
      'core-site': {
        'hadoop.security.authentication': 'kerberos'
      }
    }
    cached_kinit_executor_mock.reset_mock()
    get_params_mock.reset_mock()
    put_structured_out_mock.reset_mock()
    get_params_mock.return_value = empty_security_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/snamenode.py",
                       classname = "SNameNode",
                       command = "security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    put_structured_out_mock.assert_called_with({"securityIssuesFound": "Keytab file or principal are not set property."})

    # Testing with not empty result_issues
    result_issues_with_params = {
      'hdfs-site': "Something bad happened"
    }

    validate_security_config_mock.reset_mock()
    get_params_mock.reset_mock()
    validate_security_config_mock.return_value = result_issues_with_params
    get_params_mock.return_value = security_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/snamenode.py",
                       classname = "SNameNode",
                       command = "security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})