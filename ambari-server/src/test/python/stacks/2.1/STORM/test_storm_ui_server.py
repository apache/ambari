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

  CONFIG_OVERRIDES = {"serviceName":"STORM", "role":"STORM_UI_SERVER"}

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ui_server.py",
                       classname = "UiServer",
                       command = "configure",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ui_server.py",
                       classname = "UiServer",
                       command = "start",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_default()

    self.assertResourceCalled('Link', '/usr/lib/storm/lib//ambari-metrics-storm-sink.jar',
                              action=['delete'],
                              )

    self.assertResourceCalled('Link', '/usr/lib/storm/lib/ambari-metrics-storm-sink.jar',
                              action=['delete'],
                              )

    self.assertResourceCalled('Execute', 'ambari-sudo.sh ln -s /usr/lib/storm/lib/ambari-metrics-storm-sink-legacy-with-common-*.jar '
                                         '/usr/lib/storm/lib//ambari-metrics-storm-sink.jar',
                              not_if=format("ls /usr/lib/storm/lib//ambari-metrics-storm-sink.jar"),
                              only_if=format("ls /usr/lib/storm/lib/ambari-metrics-storm-sink-legacy-with-common-*.jar")
                              )
    self.assertResourceCalled('Execute', 'source /etc/storm/conf/storm-env.sh ; export PATH=$JAVA_HOME/bin:$PATH ; storm ui > /var/log/storm/ui.out 2>&1 &\n echo $! > /var/run/storm/ui.pid',
        path = ['/usr/bin'],
        user = 'storm',
        not_if = "ambari-sudo.sh su storm -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/storm/ui.pid >/dev/null 2>&1 && ps -p `cat /var/run/storm/ui.pid` >/dev/null 2>&1'",
    )
    self.assertResourceCalled('File', '/var/run/storm/ui.pid',
        owner = 'storm',
        group = 'hadoop',
    )
    self.assertNoMoreResources()

  def test_start_with_apache_classes(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.1/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.5.0.0-1234'
    json_content['commandParams']['version'] = version



    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ui_server.py",
                       classname = "UiServer",
                       command = "start",
                       config_dict=json_content,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_default()

    self.assertResourceCalled('Link', '/usr/lib/storm/lib//ambari-metrics-storm-sink.jar',
                              action=['delete'],
                              )

    self.assertResourceCalled('Link', '/usr/lib/storm/lib/ambari-metrics-storm-sink.jar',
                              action=['delete'],
                              )

    self.assertResourceCalled('Execute', 'ambari-sudo.sh ln -s /usr/lib/storm/lib/ambari-metrics-storm-sink-with-common-*.jar '
                                         '/usr/lib/storm/lib//ambari-metrics-storm-sink.jar',
                              not_if=format("ls /usr/lib/storm/lib//ambari-metrics-storm-sink.jar"),
                              only_if=format("ls /usr/lib/storm/lib/ambari-metrics-storm-sink-with-common-*.jar")
                              )
    self.assertResourceCalled('Execute', 'source /etc/storm/conf/storm-env.sh ; export PATH=$JAVA_HOME/bin:$PATH ; storm ui > /var/log/storm/ui.out 2>&1 &\n echo $! > /var/run/storm/ui.pid',
        path = ['/usr/bin'],
        user = 'storm',
        not_if = "ambari-sudo.sh su storm -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/storm/ui.pid >/dev/null 2>&1 && ps -p `cat /var/run/storm/ui.pid` >/dev/null 2>&1'",
    )
    self.assertResourceCalled('File', '/var/run/storm/ui.pid',
        owner = 'storm',
        group = 'hadoop',
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
                       stack_version = self.STACK_VERSION,
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
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()

  def test_start_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ui_server.py",
                       classname = "UiServer",
                       command = "start",
                       config_file="secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_secured()


    self.assertResourceCalled('Link', '/usr/lib/storm/lib//ambari-metrics-storm-sink.jar',
                              action=['delete'],
                              )

    self.assertResourceCalled('Link', '/usr/lib/storm/lib/ambari-metrics-storm-sink.jar',
                              action=['delete'],
                              )

    self.assertResourceCalled('Execute', 'ambari-sudo.sh ln -s /usr/lib/storm/lib/ambari-metrics-storm-sink-legacy-with-common-*.jar '
                                         '/usr/lib/storm/lib//ambari-metrics-storm-sink.jar',
                              not_if=format("ls /usr/lib/storm/lib//ambari-metrics-storm-sink.jar"),
                              only_if=format("ls /usr/lib/storm/lib/ambari-metrics-storm-sink-legacy-with-common-*.jar")
                              )
    self.assertResourceCalled('Execute', 'source /etc/storm/conf/storm-env.sh ; export PATH=$JAVA_HOME/bin:$PATH ; storm ui > /var/log/storm/ui.out 2>&1 &\n echo $! > /var/run/storm/ui.pid',
        path = ['/usr/bin'],
        user = 'storm',
        not_if = "ambari-sudo.sh su storm -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/storm/ui.pid >/dev/null 2>&1 && ps -p `cat /var/run/storm/ui.pid` >/dev/null 2>&1'",
    )
    self.assertResourceCalled('File', '/var/run/storm/ui.pid',
        owner = 'storm',
        group = 'hadoop',
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
                       stack_version = self.STACK_VERSION,
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

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ui_server.py",
                       classname = "UiServer",
                       command = "pre_upgrade_restart",
                       config_overrides = self.CONFIG_OVERRIDES,
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertResourceCalled("Execute", ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'storm-client', '2.2.1.0-2067'), sudo=True)

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
                     config_overrides = self.CONFIG_OVERRIDES,
                     stack_version = self.STACK_VERSION,
                     target = RMFTestCase.TARGET_COMMON_SERVICES,
                     mocks_dict = mocks_dict)

    self.assertResourceCalledIgnoreEarlier("Execute", ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'storm-client', '2.3.0.0-1234'), sudo=True)
