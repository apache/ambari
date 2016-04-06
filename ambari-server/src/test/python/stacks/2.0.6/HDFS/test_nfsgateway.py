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
import os
import json
from stacks.utils.RMFTestCase import *
from mock.mock import MagicMock, patch

# NFS GATEWAY is always started by root using jsvc due to rpcbind bugs
# on Linux such as CentOS6.2. https://bugzilla.redhat.com/show_bug.cgi?id=731542
class TestNFSGateway(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "HDFS/2.1.0.2.0/package"
  STACK_VERSION = "2.0.6"
  UPGRADE_STACK_VERSION = "2.2"

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/nfsgateway.py",
                       classname = "NFSGateway",
                       command = "configure",
                       config_file = "default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  @patch("hdfs_nfsgateway.prepare_rpcbind")
  def test_start_default(self, prepare_rpcbind_mock):
    prepare_rpcbind_mock.returnvalue = 0
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/nfsgateway.py",
                       classname = "NFSGateway",
                       command = "start",
                       config_file = "default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'root',
                              group = 'root',
                              mode = 0755
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop/root',
                              owner = 'root',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/root',
                              owner = 'root',
                              group = 'hadoop',
                              mode = 0775
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/root/hadoop_privileged_nfs3.pid',
        action = ['delete'],
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/root/hadoop_privileged_nfs3.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/root/hadoop_privileged_nfs3.pid",
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf start nfs3',
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec',
           'HADOOP_PRIVILEGED_NFS_LOG_DIR': u'/var/log/hadoop/root',
           'HADOOP_PRIVILEGED_NFS_PID_DIR': u'/var/run/hadoop/root',
           'HADOOP_PRIVILEGED_NFS_USER': u'hdfs'},
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/root/hadoop_privileged_nfs3.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/root/hadoop_privileged_nfs3.pid",
    )
    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/nfsgateway.py",
                       classname = "NFSGateway",
                       command = "stop",
                       config_file = "default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf stop nfs3',
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec',
           'HADOOP_PRIVILEGED_NFS_LOG_DIR': u'/var/log/hadoop/root',
           'HADOOP_PRIVILEGED_NFS_PID_DIR': u'/var/run/hadoop/root',
           'HADOOP_PRIVILEGED_NFS_USER': u'hdfs'},
        only_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/root/hadoop_privileged_nfs3.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/root/hadoop_privileged_nfs3.pid")

    self.assertResourceCalled('File', '/var/run/hadoop/root/hadoop_privileged_nfs3.pid', action = ['delete'])
    self.assertNoMoreResources()

  def test_configure_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/nfsgateway.py",
                       classname = "NFSGateway",
                       command = "configure",
                       config_file = "secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()

  @patch("hdfs_nfsgateway.prepare_rpcbind")
  def test_start_secured(self, prepare_rpcbind_mock):
    prepare_rpcbind_mock.returnvalue = 0
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/nfsgateway.py",
                       classname = "NFSGateway",
                       command = "start",
                       config_file = "secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'root',
                              group = 'root',
                              mode = 0755
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop/root',
                              owner = 'root',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/root',
                              owner = 'root',
                              group = 'hadoop',
                              mode = 0775
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/root/hadoop_privileged_nfs3.pid',
        action = ['delete'],
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/root/hadoop_privileged_nfs3.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/root/hadoop_privileged_nfs3.pid",
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf start nfs3',
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec',
           'HADOOP_PRIVILEGED_NFS_LOG_DIR': u'/var/log/hadoop/root',
           'HADOOP_PRIVILEGED_NFS_PID_DIR': u'/var/run/hadoop/root',
           'HADOOP_PRIVILEGED_NFS_USER': u'hdfs'},
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/root/hadoop_privileged_nfs3.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/root/hadoop_privileged_nfs3.pid",
    )
    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/nfsgateway.py",
                       classname = "NFSGateway",
                       command = "stop",
                       config_file = "secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf stop nfs3',
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec',
           'HADOOP_PRIVILEGED_NFS_LOG_DIR': u'/var/log/hadoop/root',
           'HADOOP_PRIVILEGED_NFS_PID_DIR': u'/var/run/hadoop/root',
           'HADOOP_PRIVILEGED_NFS_USER': u'hdfs'},
        only_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/root/hadoop_privileged_nfs3.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/root/hadoop_privileged_nfs3.pid")

    self.assertResourceCalled('File', '/var/run/hadoop/root/hadoop_privileged_nfs3.pid', action = ['delete'])
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
    self.assertResourceCalled('Directory', '/tmp/.hdfs-nfs',
        owner = 'hdfs',
        group = 'hadoop',
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
    self.assertResourceCalled('Directory', '/tmp/.hdfs-nfs',
        owner = 'hdfs',
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
      'core-site': {
        'hadoop.security.authentication': 'kerberos'
      },
      'hdfs-site': {
        'nfs.keytab.file': 'path/to/nfsgateway/keytab/file',
        'nfs.kerberos.principal': 'nfs_principal'
      }
    }

    props_value_check = None
    props_empty_check = ['nfs.keytab.file',
                         'nfs.kerberos.principal']
    props_read_check = ['nfs.keytab.file']

    result_issues = []

    get_params_mock.return_value = security_params
    validate_security_config_mock.return_value = result_issues

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/nfsgateway.py",
                       classname = "NFSGateway",
                       command = "security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    build_exp_mock.assert_called_with('hdfs-site', props_value_check, props_empty_check, props_read_check)
    put_structured_out_mock.assert_called_with({"securityState": "SECURED_KERBEROS"})
    cached_kinit_executor_mock.called_with('/usr/bin/kinit',
                                           self.config_dict['configurations']['hadoop-env']['hdfs_user'],
                                           security_params['hdfs-site']['nfs.keytab.file'],
                                           security_params['hdfs-site']['nfs.kerberos.principal'],
                                           self.config_dict['hostname'],
                                           '/tmp')

    # Testing when hadoop.security.authentication is simple
    security_params['core-site']['hadoop.security.authentication'] = 'simple'

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/nfsgateway.py",
                       classname = "NFSGateway",
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
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/nfsgateway.py",
                         classname = "NFSGateway",
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

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/nfsgateway.py",
                       classname = "NFSGateway",
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

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/nfsgateway.py",
                       classname = "NFSGateway",
                       command = "security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})
    self.assertNoMoreResources()

  @patch("resource_management.core.shell.call")
  def test_pre_upgrade_restart(self, call_mock):
    call_mock.side_effects = [(0, None), (0, None)]
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.3.1.0-3242'
    json_content['commandParams']['version'] = version
    stack_version = '2.3'
    json_content['hostLevelParams']['stack_version'] = stack_version
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/nfsgateway.py",
                       classname = "NFSGateway",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = [(0, None, ''), (0, None), (0, None), (0, None)])
    self.assertResourceCalled('Link', ('/etc/hadoop/conf'), to='/usr/hdp/current/hadoop-client/conf')
    self.assertResourceCalled('Execute',
                              ('hdp-select', 'set', 'hadoop-hdfs-nfs3', version), sudo=True,)
    self.assertNoMoreResources()
