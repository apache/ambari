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
from mock.mock import MagicMock, call, patch
from stacks.utils.RMFTestCase import *
import resource_management.core.source
from test_storm_base import TestStormBase

@patch("resource_management.libraries.functions.get_user_call_output.get_user_call_output", new=MagicMock(return_value=(0, '123', '')))
class TestStormUiServer(TestStormBase):

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ui_server.py",
                       classname = "UiServer",
                       command = "configure",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ui_server.py",
                       classname = "UiServer",
                       command = "start",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_default()

    self.assertResourceCalled('Link', '/usr/lib/storm/lib//ambari-metrics-storm-sink.jar',
                              action=['delete'],
                              )

    self.assertResourceCalled('Link', '/usr/lib/storm/lib/ambari-metrics-storm-sink.jar',
                              action=['delete'],
                              )

    self.assertResourceCalled('Execute', 'ambari-sudo.sh ln -s /usr/lib/storm/lib/ambari-metrics-storm-sink*.jar '
                                         '/usr/lib/storm/lib//ambari-metrics-storm-sink.jar',
                              not_if=format("ls /usr/lib/storm/lib//ambari-metrics-storm-sink.jar"),
                              only_if=format("ls /usr/lib/storm/lib/ambari-metrics-storm-sink*.jar")
                              )

    self.assertResourceCalled('Execute', 'source /etc/storm/conf/storm-env.sh ; export PATH=$JAVA_HOME/bin:$PATH ; storm ui > /var/log/storm/ui.out 2>&1',
        wait_for_finish = False,
        path = ['/usr/bin'],
        user = 'storm',
        not_if = "ambari-sudo.sh su storm -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/storm/ui.pid >/dev/null 2>&1 && ps -p `cat /var/run/storm/ui.pid` >/dev/null 2>&1'",
    )
    self.assertResourceCalled('Execute', "/usr/jdk64/jdk1.7.0_45/bin/jps -l  | grep backtype.storm.ui.core$ && /usr/jdk64/jdk1.7.0_45/bin/jps -l  | grep backtype.storm.ui.core$ | awk {'print $1'} > /var/run/storm/ui.pid",
        logoutput = True,
        path = ['/usr/bin'],
        tries = 12,
        user = 'storm',
        try_sleep = 10,
    )
    self.assertNoMoreResources()

  @patch("os.path.exists")
  def test_stop_default(self, path_exists_mock):
    # Bool for the pid file
    path_exists_mock.side_effect = [True]
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ui_server.py",
                       classname = "UiServer",
                       command = "stop",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh kill 123",
        not_if = "! (ambari-sudo.sh su storm -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/storm/ui.pid >/dev/null 2>&1 && ps -p `cat /var/run/storm/ui.pid` >/dev/null 2>&1')",
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh kill -9 123",
        not_if = "sleep 2; ! (ambari-sudo.sh su storm -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/storm/ui.pid >/dev/null 2>&1 && ps -p `cat /var/run/storm/ui.pid` >/dev/null 2>&1') || sleep 20; ! (ambari-sudo.sh su storm -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/storm/ui.pid >/dev/null 2>&1 && ps -p `cat /var/run/storm/ui.pid` >/dev/null 2>&1')",
        ignore_failures = True,
    )
    self.assertResourceCalled('File', '/var/run/storm/ui.pid',
        action = ['delete'],
    )
    self.assertNoMoreResources()

  def test_configure_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ui_server.py",
                       classname = "UiServer",
                       command = "configure",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()

  def test_start_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ui_server.py",
                       classname = "UiServer",
                       command = "start",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_secured()


    self.assertResourceCalled('Link', '/usr/lib/storm/lib//ambari-metrics-storm-sink.jar',
                              action=['delete'],
                              )

    self.assertResourceCalled('Link', '/usr/lib/storm/lib/ambari-metrics-storm-sink.jar',
                              action=['delete'],
                              )

    self.assertResourceCalled('Execute', 'ambari-sudo.sh ln -s /usr/lib/storm/lib/ambari-metrics-storm-sink*.jar '
                                         '/usr/lib/storm/lib//ambari-metrics-storm-sink.jar',
                              not_if=format("ls /usr/lib/storm/lib//ambari-metrics-storm-sink.jar"),
                              only_if=format("ls /usr/lib/storm/lib/ambari-metrics-storm-sink*.jar")
                              )

    self.assertResourceCalled('Execute', 'source /etc/storm/conf/storm-env.sh ; export PATH=$JAVA_HOME/bin:$PATH ; storm ui > /var/log/storm/ui.out 2>&1',
        wait_for_finish = False,
        path = ['/usr/bin'],
        user = 'storm',
        not_if = "ambari-sudo.sh su storm -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/storm/ui.pid >/dev/null 2>&1 && ps -p `cat /var/run/storm/ui.pid` >/dev/null 2>&1'",
    )
    self.assertResourceCalled('Execute', "/usr/jdk64/jdk1.7.0_45/bin/jps -l  | grep backtype.storm.ui.core$ && /usr/jdk64/jdk1.7.0_45/bin/jps -l  | grep backtype.storm.ui.core$ | awk {'print $1'} > /var/run/storm/ui.pid",
        logoutput = True,
        path = ['/usr/bin'],
        tries = 12,
        user = 'storm',
        try_sleep = 10,
    )
    self.assertNoMoreResources()

  @patch("os.path.exists")
  def test_stop_secured(self, path_exists_mock):
    # Bool for the pid file
    path_exists_mock.side_effect = [True]
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ui_server.py",
                       classname = "UiServer",
                       command = "stop",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh kill 123",
        not_if = "! (ambari-sudo.sh su storm -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/storm/ui.pid >/dev/null 2>&1 && ps -p `cat /var/run/storm/ui.pid` >/dev/null 2>&1')",
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh kill -9 123",
        not_if = "sleep 2; ! (ambari-sudo.sh su storm -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/storm/ui.pid >/dev/null 2>&1 && ps -p `cat /var/run/storm/ui.pid` >/dev/null 2>&1') || sleep 20; ! (ambari-sudo.sh su storm -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/storm/ui.pid >/dev/null 2>&1 && ps -p `cat /var/run/storm/ui.pid` >/dev/null 2>&1')",
        ignore_failures = True,
    )
    self.assertResourceCalled('File', '/var/run/storm/ui.pid',
        action = ['delete'],
    )
    self.assertNoMoreResources()

  def test_pre_upgrade_restart(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ui_server.py",
                       classname = "UiServer",
                       command = "pre_upgrade_restart",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertResourceCalled("Execute", ('hdp-select', 'set', 'storm-client', '2.2.1.0-2067'), sudo=True)

  def test_pre_upgrade_restart_23(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.1/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ui_server.py",
                     classname = "UiServer",
                     command = "pre_upgrade_restart",
                     config_dict = json_content,
                     hdp_stack_version = self.STACK_VERSION,
                     target = RMFTestCase.TARGET_COMMON_SERVICES,
                     call_mocks = [(0, None, ''), (0, None)],
                     mocks_dict = mocks_dict)

    self.assertResourceCalledIgnoreEarlier("Execute", ('hdp-select', 'set', 'storm-client', '2.3.0.0-1234'), sudo=True)

    self.assertEquals(1, mocks_dict['call'].call_count)
    self.assertEquals(1, mocks_dict['checked_call'].call_count)
    self.assertEquals(
      ('conf-select', 'set-conf-dir', '--package', 'storm', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['checked_call'].call_args_list[0][0][0])
    self.assertEquals(
      ('conf-select', 'create-conf-dir', '--package', 'storm', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['call'].call_args_list[0][0][0])


  @patch("resource_management.libraries.functions.security_commons.build_expectations")
  @patch("resource_management.libraries.functions.security_commons.validate_security_config_properties")
  @patch("resource_management.libraries.functions.security_commons.cached_kinit_executor")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  def test_security_status(self, put_structured_out_mock, cached_kinit_executor_mock, validate_security_config_mock, build_exp_mock):
    # Test that function works when is called with correct parameters
    result_issues = []

    security_params = {
      'storm_ui': {
        'storm_ui_principal_name': 'HTTP/_HOST',
        'storm_ui_keytab': '/etc/security/keytabs/spnego.service.keytab'
      }
    }
    props_value_check = None
    props_empty_check = ['storm_ui_principal_name', 'storm_ui_keytab']
    props_read_check = ['storm_ui_keytab']

    validate_security_config_mock.return_value = result_issues

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ui_server.py",
                       classname = "UiServer",
                       command = "security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    build_exp_mock.assert_called_with('storm_ui', props_value_check, props_empty_check, props_read_check)
    put_structured_out_mock.assert_called_with({"securityState": "SECURED_KERBEROS"})
    self.assertTrue(cached_kinit_executor_mock.call_count, 2)

    cached_kinit_executor_mock.assert_called_with('/usr/bin/kinit',
                                                  self.config_dict['configurations']['storm-env']['storm_user'],
                                                  security_params['storm_ui']['storm_ui_keytab'],
                                                  security_params['storm_ui']['storm_ui_principal_name'],
                                                  self.config_dict['hostname'],
                                                  '/tmp')

    # Testing that the exception throw by cached_executor is caught
    cached_kinit_executor_mock.reset_mock()
    cached_kinit_executor_mock.side_effect = Exception("Invalid command")

    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ui_server.py",
                        classname = "UiServer",
                        command = "security_status",
                        config_file="secured.json",
                        hdp_stack_version = self.STACK_VERSION,
                        target = RMFTestCase.TARGET_COMMON_SERVICES
      )
    except:
      self.assertTrue(True)

    # Testing with not empty result_issues
    result_issues_with_params = {}
    result_issues_with_params['storm_ui']="Something bad happened"

    validate_security_config_mock.reset_mock()
    validate_security_config_mock.return_value = result_issues_with_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ui_server.py",
                       classname = "UiServer",
                       command = "security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})

    # Testing with security_enable = false
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ui_server.py",
                       classname = "UiServer",
                       command = "security_status",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})

