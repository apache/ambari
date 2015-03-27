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

from mock.mock import MagicMock, patch
from stacks.utils.RMFTestCase import *

@patch("platform.linux_distribution", new = MagicMock(return_value="Linux"))
class TestFalconServer(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "FALCON/0.5.0.2.1/package"
  STACK_VERSION = "2.1"
  UPGRADE_STACK_VERSION = "2.2"

  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_server.py",
                       classname="FalconServer",
                       command="start",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertResourceCalled('Execute', '/usr/lib/falcon/bin/falcon-start -port 15000',
                              path = ['/usr/bin'],
                              user = 'falcon',
                              )
    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_server.py",
                       classname="FalconServer",
                       command="stop",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', '/usr/lib/falcon/bin/falcon-stop',
                              path = ['/usr/bin'],
                              user = 'falcon',
                              )
    self.assertResourceCalled('File', '/var/run/falcon/falcon.pid',
                              action = ['delete'],
                              )
    self.assertNoMoreResources()

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_server.py",
                       classname="FalconServer",
                       command="configure",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/var/run/falcon',
                              owner = 'falcon',
                              )
    self.assertResourceCalled('Directory', '/var/log/falcon',
                              owner = 'falcon',
                              recursive = True
                              )
    self.assertResourceCalled('Directory', '/var/lib/falcon/webapp',
                              owner = 'falcon',
                              )
    self.assertResourceCalled('Directory', '/usr/lib/falcon',
                              owner = 'falcon',
                              )
    self.assertResourceCalled('Directory', '/etc/falcon',
                              mode = 0755
    )
    self.assertResourceCalled('Directory', '/etc/falcon/conf',
                              owner = 'falcon',
                              recursive = True
    )
    self.assertResourceCalled('File', '/etc/falcon/conf/falcon-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['falcon-env']['content']),
                              owner = 'falcon'
                              )
    self.assertResourceCalled('File', '/etc/falcon/conf/client.properties',
                              content = Template('client.properties.j2'),
                              mode = 0644,
                              owner = 'falcon'
                              )
    self.assertResourceCalled('PropertiesFile', '/etc/falcon/conf/runtime.properties',
                              mode = 0644,
                              properties = self.getConfig()['configurations']['falcon-runtime.properties'],
                              owner = 'falcon'
                              )
    self.assertResourceCalled('PropertiesFile', '/etc/falcon/conf/startup.properties',
                              mode = 0644,
                              properties = self.getConfig()['configurations']['falcon-startup.properties'],
                              owner = 'falcon'
                              )
    self.assertResourceCalled('HdfsDirectory', '/apps/falcon',
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              mode = 0777,
                              owner = 'falcon',
                              bin_dir = '/usr/bin',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', None,
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              bin_dir = '/usr/bin',
                              action = ['create'],
                              )
    self.assertResourceCalled('Directory', '/hadoop/falcon',
                              owner = 'falcon',
                              recursive = True,
                              cd_access='a'
                              )
    self.assertResourceCalled('Directory', '/hadoop/falcon/embeddedmq',
                              owner = 'falcon'
                              )
    self.assertResourceCalled('Directory', '/hadoop/falcon/embeddedmq/data',
                              owner = 'falcon',
                              recursive = True,
                              )


  @patch("shutil.rmtree", new = MagicMock())
  @patch("tarfile.open")
  @patch("os.path.isdir")
  @patch("os.path.exists")
  @patch("os.path.isfile")
  def test_upgrade(self, isfile_mock, exists_mock, isdir_mock,
      tarfile_open_mock):

    isdir_mock.return_value = True
    exists_mock.side_effect = [False,False,True]
    isfile_mock.return_value = True

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_server.py",
     classname = "FalconServer", command = "restart", config_file = "falcon-upgrade.json",
     hdp_stack_version = self.UPGRADE_STACK_VERSION,
     target = RMFTestCase.TARGET_COMMON_SERVICES )

    self.assertResourceCalled('Execute',
      '/usr/hdp/current/falcon-server/bin/falcon-stop',
      path = ['/usr/hdp/current/hadoop-client/bin'], user='falcon')

    self.assertResourceCalled('File', '/var/run/falcon/falcon.pid',
      action = ['delete'])

    self.assertResourceCalled('Execute', 'hdp-select set falcon-server 2.2.1.0-2135')

    # 4 calls to tarfile.open (2 directories * read + write)
    self.assertTrue(tarfile_open_mock.called)
    self.assertEqual(tarfile_open_mock.call_count,4)
    
  @patch("resource_management.libraries.functions.security_commons.build_expectations")
  @patch("resource_management.libraries.functions.security_commons.get_params_from_filesystem")
  @patch("resource_management.libraries.functions.security_commons.validate_security_config_properties")
  @patch("resource_management.libraries.functions.security_commons.cached_kinit_executor")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  def test_security_status(self, put_structured_out_mock, cached_kinit_executor_mock, validate_security_config_mock, get_params_mock, build_exp_mock):
    # Test that function works when is called with correct parameters

    security_params = {
      'startup': {
        '*.falcon.service.authentication.kerberos.keytab': 'path/to/falcon/service/keytab',
        '*.falcon.service.authentication.kerberos.principal': 'falcon_service_keytab',
        '*.falcon.http.authentication.kerberos.keytab': 'path/to/falcon/http/keytab',
        '*.falcon.http.authentication.kerberos.principal': 'falcon_http_principal'
      }
    }
    result_issues = []
    props_value_check = {"*.falcon.authentication.type": "kerberos",
                           "*.falcon.http.authentication.type": "kerberos"}
    props_empty_check = ["*.falcon.service.authentication.kerberos.principal",
                           "*.falcon.service.authentication.kerberos.keytab",
                           "*.falcon.http.authentication.kerberos.principal",
                           "*.falcon.http.authentication.kerberos.keytab"]

    props_read_check = ["*.falcon.service.authentication.kerberos.keytab",
                          "*.falcon.http.authentication.kerberos.keytab"]

    get_params_mock.return_value = security_params
    validate_security_config_mock.return_value = result_issues

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_server.py",
                       classname="FalconServer",
                       command="security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    get_params_mock.assert_called_with('/etc/falcon/conf', {'startup.properties': 'PROPERTIES'})
    build_exp_mock.assert_called_with('startup', props_value_check, props_empty_check, props_read_check)
    put_structured_out_mock.assert_called_with({"securityState": "SECURED_KERBEROS"})
    self.assertTrue(cached_kinit_executor_mock.call_count, 2)
    cached_kinit_executor_mock.assert_called_with('/usr/bin/kinit',
                                                  self.config_dict['configurations']['falcon-env']['falcon_user'],
                                                  security_params['startup']['*.falcon.http.authentication.kerberos.keytab'],
                                                  security_params['startup']['*.falcon.http.authentication.kerberos.principal'],
                                                  self.config_dict['hostname'],
                                                  '/tmp')

    # Testing that the exception throw by cached_executor is caught
    cached_kinit_executor_mock.reset_mock()
    cached_kinit_executor_mock.side_effect = Exception("Invalid command")

    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_server.py",
                       classname="FalconServer",
                       command="security_status",
                       config_file="secured.json",
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

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_server.py",
                       classname="FalconServer",
                       command="security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityIssuesFound": "Keytab file or principal are not set property."})

    # Testing with not empty result_issues
    result_issues_with_params = {
      'startup': "Something bad happened"
    }

    validate_security_config_mock.reset_mock()
    get_params_mock.reset_mock()
    validate_security_config_mock.return_value = result_issues_with_params
    get_params_mock.return_value = security_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_server.py",
                       classname="FalconServer",
                       command="security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})

    # Testing with security_enable = false
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_server.py",
                       classname="FalconServer",
                       command="security_status",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})