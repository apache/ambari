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
import  resource_management.core.source
from test_storm_base import TestStormBase

class TestStormDrpcServer(TestStormBase):

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/drpc_server.py",
                       classname = "DrpcServer",
                       command = "configure",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default(self):

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/drpc_server.py",
                       classname = "DrpcServer",
                       command = "start",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()

    self.assertResourceCalled('Execute', 'source /etc/storm/conf/storm-env.sh ; export PATH=$JAVA_HOME/bin:$PATH ; storm drpc > /var/log/storm/drpc.out 2>&1',
        wait_for_finish = False,
        path = ['/usr/bin'],
        user = 'storm',
        not_if = 'ls /var/run/storm/drpc.pid >/dev/null 2>&1 && ps -p `cat /var/run/storm/drpc.pid` >/dev/null 2>&1',
    )
    self.assertResourceCalled('Execute', "/usr/jdk64/jdk1.7.0_45/bin/jps -l  | grep storm.daemon.drpc$ && /usr/jdk64/jdk1.7.0_45/bin/jps -l  | grep storm.daemon.drpc$ | awk {'print $1'} > /var/run/storm/drpc.pid",
        logoutput = True,
        path = ['/usr/bin'],
        tries = 12,
        user = 'storm',
        try_sleep = 10,
    )

    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/drpc_server.py",
                       classname = "DrpcServer",
                       command = "stop",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh kill `cat /var/run/storm/drpc.pid`',
        not_if = '! (ls /var/run/storm/drpc.pid >/dev/null 2>&1 && ps -p `cat /var/run/storm/drpc.pid` >/dev/null 2>&1)',
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh kill -9 `cat /var/run/storm/drpc.pid`',
        not_if = 'sleep 2; ! (ls /var/run/storm/drpc.pid >/dev/null 2>&1 && ps -p `cat /var/run/storm/drpc.pid` >/dev/null 2>&1) || sleep 20; ! (ls /var/run/storm/drpc.pid >/dev/null 2>&1 && ps -p `cat /var/run/storm/drpc.pid` >/dev/null 2>&1)',
        ignore_failures = True,
    )
    self.assertResourceCalled('File', '/var/run/storm/drpc.pid',
        action = ['delete'],
    )
    self.assertNoMoreResources()

  def test_configure_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/drpc_server.py",
                       classname = "DrpcServer",
                       command = "configure",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()

  def test_start_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/drpc_server.py",
                       classname = "DrpcServer",
                       command = "start",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_secured()

    self.assertResourceCalled('Execute', 'source /etc/storm/conf/storm-env.sh ; export PATH=$JAVA_HOME/bin:$PATH ; storm drpc > /var/log/storm/drpc.out 2>&1',
        wait_for_finish = False,
        path = ['/usr/bin'],
        user = 'storm',
        not_if = 'ls /var/run/storm/drpc.pid >/dev/null 2>&1 && ps -p `cat /var/run/storm/drpc.pid` >/dev/null 2>&1',
    )
    self.assertResourceCalled('Execute', "/usr/jdk64/jdk1.7.0_45/bin/jps -l  | grep storm.daemon.drpc$ && /usr/jdk64/jdk1.7.0_45/bin/jps -l  | grep storm.daemon.drpc$ | awk {'print $1'} > /var/run/storm/drpc.pid",
        logoutput = True,
        path = ['/usr/bin'],
        tries = 12,
        user = 'storm',
        try_sleep = 10,
    )
    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/drpc_server.py",
                       classname = "DrpcServer",
                       command = "stop",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh kill `cat /var/run/storm/drpc.pid`',
        not_if = '! (ls /var/run/storm/drpc.pid >/dev/null 2>&1 && ps -p `cat /var/run/storm/drpc.pid` >/dev/null 2>&1)',
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh kill -9 `cat /var/run/storm/drpc.pid`',
        not_if = 'sleep 2; ! (ls /var/run/storm/drpc.pid >/dev/null 2>&1 && ps -p `cat /var/run/storm/drpc.pid` >/dev/null 2>&1) || sleep 20; ! (ls /var/run/storm/drpc.pid >/dev/null 2>&1 && ps -p `cat /var/run/storm/drpc.pid` >/dev/null 2>&1)',
        ignore_failures = True,
    )
    self.assertResourceCalled('File', '/var/run/storm/drpc.pid',
        action = ['delete'],
    )
    self.assertNoMoreResources()

  def test_pre_rolling_restart(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/drpc_server.py",
                     classname = "DrpcServer",
                     command = "pre_rolling_restart",
                     config_file="default.json",
                     hdp_stack_version = self.STACK_VERSION,
                     target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertResourceCalled("Execute", "hdp-select set storm-client 2.2.1.0-2067")


  @patch("resource_management.libraries.functions.security_commons.build_expectations")
  @patch("resource_management.libraries.functions.security_commons.get_params_from_filesystem")
  @patch("resource_management.libraries.functions.security_commons.validate_security_config_properties")
  @patch("resource_management.libraries.functions.security_commons.cached_kinit_executor")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  def test_security_status(self, put_structured_out_mock, cached_kinit_executor_mock, validate_security_config_mock, get_params_mock, build_exp_mock):
    # Test that function works when is called with correct parameters

    security_params = {
      'storm_jaas': {
        'StormServer': {
          'keyTab': 'path/to/storm/service/keytab',
          'principal': 'storm_keytab'
        }
      }
    }
    result_issues = []

    props_value_check = None
    props_empty_check = ['StormServer/keyTab', 'StormServer/principal']
    props_read_check = ['StormServer/keyTab']

    get_params_mock.return_value = security_params
    validate_security_config_mock.return_value = result_issues

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/drpc_server.py",
                       classname = "DrpcServer",
                       command = "security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    build_exp_mock.assert_called_with('storm_jaas', props_value_check, props_empty_check, props_read_check)
    put_structured_out_mock.assert_called_with({"securityState": "SECURED_KERBEROS"})
    self.assertTrue(cached_kinit_executor_mock.call_count, 2)
    cached_kinit_executor_mock.assert_called_with('/usr/bin/kinit',
                                                  self.config_dict['configurations']['storm-env']['storm_user'],
                                                  security_params['storm_jaas']['StormServer']['keyTab'],
                                                  security_params['storm_jaas']['StormServer']['principal'],
                                                  self.config_dict['hostname'],
                                                  '/tmp')

    # Testing that the exception throw by cached_executor is caught
    cached_kinit_executor_mock.reset_mock()
    cached_kinit_executor_mock.side_effect = Exception("Invalid command")

    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/drpc_server.py",
                        classname = "DrpcServer",
                        command = "security_status",
                        config_file="secured.json",
                        hdp_stack_version = self.STACK_VERSION,
                        target = RMFTestCase.TARGET_COMMON_SERVICES
      )
    except:
      self.assertTrue(True)

    # Testing with a security_params which doesn't contains storm_jaas
    empty_security_params = {}
    cached_kinit_executor_mock.reset_mock()
    get_params_mock.reset_mock()
    put_structured_out_mock.reset_mock()
    get_params_mock.return_value = empty_security_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/drpc_server.py",
                       classname = "DrpcServer",
                       command = "security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityIssuesFound": "Keytab file or principal are not set property."})

    # Testing with not empty result_issues
    result_issues_with_params = {
      'storm_jaas': "Something bad happened"
    }

    validate_security_config_mock.reset_mock()
    get_params_mock.reset_mock()
    validate_security_config_mock.return_value = result_issues_with_params
    get_params_mock.return_value = security_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/drpc_server.py",
                       classname = "DrpcServer",
                       command = "security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})

    # Testing with security_enable = false
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/drpc_server.py",
                       classname = "DrpcServer",
                       command = "security_status",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})
