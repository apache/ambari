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
from resource_management import *
from stacks.utils.RMFTestCase import *
from mock.mock import patch

class TestKnoxGateway(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "KNOX/0.5.0.2.2/package"
  STACK_VERSION = "2.2"

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/knox_gateway.py",
                       classname = "KnoxGateway",
                       command = "configure",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Directory', '/etc/knox/conf',
                              owner = 'knox',
                              group = 'knox',
                              recursive = True
    )

    self.assertResourceCalled('XmlConfig', 'gateway-site.xml',
                              owner = 'knox',
                              group = 'knox',
                              conf_dir = '/etc/knox/conf',
                              configurations = self.getConfig()['configurations']['gateway-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['gateway-site']
    )

    self.assertResourceCalled('File', '/etc/knox/conf/gateway-log4j.properties',
                              mode=0644,
                              group='knox',
                              owner = 'knox',
                              content = self.getConfig()['configurations']['gateway-log4j']['content']
    )
    self.assertResourceCalled('File', '/etc/knox/conf/topologies/default.xml',
                              group='knox',
                              owner = 'knox',
                              content = InlineTemplate(self.getConfig()['configurations']['topology']['content'])
    )
    self.assertResourceCalled('Execute', ('chown',
     '-R',
     'knox:knox',
     '/var/lib/knox/data',
     '/var/log/knox',
     '/var/log/knox',
     '/var/run/knox',
     '/etc/knox/conf'),
        sudo = True,
    )
    self.assertResourceCalled('Execute', '/usr/lib/knox/bin/knoxcli.sh create-master --master sa',
        environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        not_if = "ambari-sudo.sh su knox -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]test -f /var/lib/knox/data/security/master'",
        user = 'knox',
    )
    self.assertResourceCalled('Execute', '/usr/lib/knox/bin/knoxcli.sh create-cert --hostname c6401.ambari.apache.org',
        environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        not_if = "ambari-sudo.sh su knox -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]test -f /var/lib/knox/data/security/keystores/gateway.jks'",
        user = 'knox',
    )
    self.assertResourceCalled('File', '/etc/knox/conf/ldap-log4j.properties',
                              mode=0644,
                              group='knox',
                              owner = 'knox',
                              content = self.getConfig()['configurations']['ldap-log4j']['content']
    )
    self.assertResourceCalled('File', '/etc/knox/conf/users.ldif',
                              mode=0644,
                              group='knox',
                              owner = 'knox',
                              content = self.getConfig()['configurations']['users-ldif']['content']
    )

    self.assertNoMoreResources()


  @patch("resource_management.libraries.functions.security_commons.build_expectations")
  @patch("resource_management.libraries.functions.security_commons.get_params_from_filesystem")
  @patch("resource_management.libraries.functions.security_commons.validate_security_config_properties")
  @patch("resource_management.libraries.functions.security_commons.cached_kinit_executor")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  def test_security_status(self, put_structured_out_mock, cached_kinit_executor_mock,
                           validate_security_config_mock, get_params_mock, build_exp_mock):
    # Test that function works when is called with correct parameters

    security_params = {
      "krb5JAASLogin":
        {
          'keytab': "/path/to/keytab",
          'principal': "principal"
        },
      "gateway-site" : {
        "gateway.hadoop.kerberos.secured" : "true"
      }
    }

    result_issues = []

    get_params_mock.return_value = security_params
    validate_security_config_mock.return_value = result_issues

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/knox_gateway.py",
                       classname = "KnoxGateway",
                       command="security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    import status_params

    self.assertTrue(build_exp_mock.call_count, 2)
    build_exp_mock.assert_called_with('gateway-site', {"gateway.hadoop.kerberos.secured": "true"}, None, None)
    put_structured_out_mock.assert_called_with({"securityState": "SECURED_KERBEROS"})
    self.assertTrue(cached_kinit_executor_mock.call_count, 1)
    cached_kinit_executor_mock.assert_called_with(status_params.kinit_path_local,
                                                  status_params.knox_user,
                                                  security_params['krb5JAASLogin']['keytab'],
                                                  security_params['krb5JAASLogin']['principal'],
                                                  status_params.hostname,
                                                  status_params.temp_dir)

    # Testing that the exception throw by cached_executor is caught
    cached_kinit_executor_mock.reset_mock()
    cached_kinit_executor_mock.side_effect = Exception("Invalid command")

    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/knox_gateway.py",
                         classname = "KnoxGateway",
                         command="security_status",
                         config_file="secured.json",
                         hdp_stack_version = self.STACK_VERSION,
                         target = RMFTestCase.TARGET_COMMON_SERVICES
      )
    except:
      self.assertTrue(True)

    # Testing with a security_params which doesn't contains krb5JAASLogin
    empty_security_params = {"krb5JAASLogin" : {}}
    cached_kinit_executor_mock.reset_mock()
    get_params_mock.reset_mock()
    put_structured_out_mock.reset_mock()
    get_params_mock.return_value = empty_security_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/knox_gateway.py",
                       classname = "KnoxGateway",
                       command="security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityIssuesFound": "Keytab file and principal are not set."})

    # Testing with not empty result_issues
    result_issues_with_params = {'krb5JAASLogin': "Something bad happened"}
    validate_security_config_mock.reset_mock()
    get_params_mock.reset_mock()
    validate_security_config_mock.return_value = result_issues_with_params
    get_params_mock.return_value = security_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/knox_gateway.py",
                       classname = "KnoxGateway",
                       command="security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})

    # Testing with security_enable = false
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/knox_gateway.py",
                       classname = "KnoxGateway",
                       command="security_status",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})

  @patch("tarfile.open")
  @patch("os.path.isdir")
  def test_pre_rolling_restart(self, isdir_mock, tarfile_open_mock):
    isdir_mock.return_value = True

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/knox_gateway.py",
                       classname = "KnoxGateway",
                       command = "pre_rolling_restart",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertTrue(tarfile_open_mock.called)

    self.assertResourceCalled("Execute", "hdp-select set knox-server 2.2.1.0-2067")
