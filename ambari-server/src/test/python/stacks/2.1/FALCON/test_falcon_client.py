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

from mock.mock import patch
from stacks.utils.RMFTestCase import *


class TestFalconClient(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "FALCON/0.5.0.2.1/package"
  STACK_VERSION = "2.1"

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_client.py",
                       classname="FalconClient",
                       command="configure",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Directory', '/var/run/falcon',
                              owner = 'falcon',
                              create_parents = True,
                              mode = 0755,
                              cd_access = "a",
                              )
    self.assertResourceCalled('Directory', '/var/log/falcon',
                              owner = 'falcon',
                              create_parents = True,
                              mode = 0755,
                              cd_access = "a",
                              )
    self.assertResourceCalled('Directory', '/var/lib/falcon/webapp',
                              owner = 'falcon',
                              create_parents = True
                              )
    self.assertResourceCalled('Directory', '/usr/lib/falcon',
                              owner = 'falcon',
                              create_parents = True
                              )
    self.assertResourceCalled('Directory', '/etc/falcon',
                              mode = 0755,
                              create_parents = True
                              )
    self.assertResourceCalled('Directory', '/etc/falcon/conf',
                              owner = 'falcon',
                              create_parents = True
                              )
    self.assertResourceCalled('File', '/etc/falcon/conf/falcon-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['falcon-env']['content']),
                              owner = 'falcon',
                              group = 'hadoop'
                              )
    self.assertResourceCalled('File', '/etc/falcon/conf/client.properties',
                              content = Template('client.properties.j2'),
                              owner = 'falcon',
                              mode = 0644
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
    self.assertNoMoreResources()

  @patch("resource_management.libraries.script.Script.put_structured_out")
  def test_security_status(self, put_structured_out_mock):
    # Test that function works when is called with correct parameters
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_client.py",
                       classname="FalconClient",
                       command="security_status",
                       config_file="secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    put_structured_out_mock.assert_called_with({"securityState": "SECURED_KERBEROS"})

    # Testing with security_enable = false
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_client.py",
                       classname="FalconClient",
                       command="security_status",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})

  def test_pre_upgrade_restart(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.2.1.0-3242'
    json_content['commandParams']['version'] = version
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_client.py",
                       classname = "FalconClient",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)
    self.assertResourceCalled('Execute',
                              ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'falcon-client', version), sudo=True,)
    self.assertNoMoreResources()

  @patch("resource_management.core.shell.call")
  def test_pre_upgrade_restart_23(self, call_mock):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_client.py",
                       classname = "FalconClient",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = [(0, None, ''), (0, None)],
                       mocks_dict = mocks_dict)

    self.assertResourceCalledIgnoreEarlier('Execute',
                              ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'falcon-client', version), sudo=True,)
    self.assertNoMoreResources()

    self.assertEquals(1, mocks_dict['call'].call_count)
    self.assertEquals(1, mocks_dict['checked_call'].call_count)
    self.assertEquals(
      ('ambari-python-wrap', '/usr/bin/conf-select', 'set-conf-dir', '--package', 'falcon', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['checked_call'].call_args_list[0][0][0])
    self.assertEquals(
      ('ambari-python-wrap', '/usr/bin/conf-select', 'create-conf-dir', '--package', 'falcon', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['call'].call_args_list[0][0][0])
