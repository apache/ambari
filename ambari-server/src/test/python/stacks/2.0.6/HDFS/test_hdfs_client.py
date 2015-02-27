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
import tempfile
import tarfile
import contextlib
from resource_management import *
from stacks.utils.RMFTestCase import *


@patch.object(tarfile,"open", new = MagicMock())
@patch.object(tempfile,"mkdtemp", new = MagicMock(return_value='/tmp/123'))
@patch.object(contextlib,"closing", new = MagicMock())
@patch("os.path.exists", new = MagicMock(return_value=True))
class Test(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "HDFS/2.1.0.2.0/package"
  STACK_VERSION = "2.0.6"

  def test_generate_configs_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hdfs_client.py",
                       classname = "HdfsClient",
                       command = "generate_configs",
                       config_file = "default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Directory', '/tmp',
                              recursive = True,
                              )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
                              conf_dir = '/tmp/123',
                              configuration_attributes = self.getConfig()['configuration_attributes']['hdfs-site'],
                              configurations = self.getConfig()['configurations']['hdfs-site'],
                              )
    self.assertResourceCalled('File', '/tmp/123/hadoop-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['hadoop-env']['content']),
                              )
    self.assertResourceCalled('File', '/tmp/123/log4j.properties',
                              content = InlineTemplate(self.getConfig()['configurations']['hdfs-log4j']['content']+
                                                       self.getConfig()['configurations']['yarn-log4j']['content']),
                              )
    self.assertResourceCalled('PropertiesFile', '/tmp/123/runtime.properties',
                              properties = UnknownConfigurationMock(),
    )
    self.assertResourceCalled('PropertiesFile', '/tmp/123/startup.properties',
                              properties = UnknownConfigurationMock(),
    )
    self.assertResourceCalled('Directory', '/tmp/123',
                              action = ['delete'],
                              )
    self.assertNoMoreResources()

  def test_upgrade(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hdfs_client.py",
                   classname = "HdfsClient",
                   command = "restart",
                   config_file="client-upgrade.json",
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertResourceCalled("Execute", "hdp-select set hadoop-client 2.2.1.0-2067")

    # for now, it's enough that hdp-select is confirmed

  @patch("resource_management.libraries.functions.security_commons.build_expectations")
  @patch("resource_management.libraries.functions.security_commons.get_params_from_filesystem")
  @patch("resource_management.libraries.functions.security_commons.validate_security_config_properties")
  @patch("resource_management.libraries.functions.security_commons.cached_kinit_executor")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  def test_security_status(self, put_structured_out_mock, cached_kinit_executor_mock, validate_security_config_mock, get_params_mock, build_exp_mock):
    # Test that function works when is called with correct parameters
    import status_params

    security_params = {}
    security_params['core-site'] = {}
    security_params['core-site']['hadoop.security.authentication'] = 'kerberos'

    props_value_check = {"hadoop.security.authentication": "kerberos",
                         "hadoop.security.authorization": "true"}
    props_empty_check = ["hadoop.security.auth_to_local"]
    props_read_check = None

    result_issues = []

    get_params_mock.return_value = security_params
    validate_security_config_mock.return_value = result_issues

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hdfs_client.py",
                       classname = "HdfsClient",
                       command = "security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    build_exp_mock.assert_called_with('core-site', props_value_check, props_empty_check, props_read_check)
    put_structured_out_mock.assert_called_with({"securityState": "SECURED_KERBEROS"})
    cached_kinit_executor_mock.called_with(status_params.kinit_path_local,
                                           status_params.hdfs_user,
                                           status_params.hdfs_user_keytab,
                                           status_params.hdfs_user_principal,
                                           status_params.hostname,
                                           status_params.tmp_dir)

    # Testing that the exception throw by cached_executor is caught
    cached_kinit_executor_mock.reset_mock()
    cached_kinit_executor_mock.side_effect = Exception("Invalid command")

    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hdfs_client.py",
                         classname = "HdfsClient",
                         command = "security_status",
                         config_file="secured.json",
                         hdp_stack_version = self.STACK_VERSION,
                         target = RMFTestCase.TARGET_COMMON_SERVICES
      )
    except:
      self.assertTrue(True)

    # Testing when hadoop.security.authentication is simple
    security_params['core-site']['hadoop.security.authentication'] = 'simple'

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hdfs_client.py",
                       classname = "HdfsClient",
                       command = "security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})
    security_params['core-site']['hadoop.security.authentication'] = 'kerberos'

    # Testing with not empty result_issues
    result_issues_with_params = {}
    result_issues_with_params['hdfs-site']="Something bad happened"

    validate_security_config_mock.reset_mock()
    get_params_mock.reset_mock()
    validate_security_config_mock.return_value = result_issues_with_params
    get_params_mock.return_value = security_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hdfs_client.py",
                       classname = "HdfsClient",
                       command = "security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})

    # Testing with empty hdfs_user_principal and hdfs_user_keytab
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hdfs_client.py",
                       classname = "HdfsClient",
                       command = "security_status",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})
