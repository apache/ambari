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
import resource_management.core.source
from test_storm_base import TestStormBase

class TestStormJaasConfiguration(TestStormBase):

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/nimbus.py",
                       classname = "Nimbus",
                       command = "configure",
                       config_file = "default-storm-start.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
  def test_start_default(self):

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/nimbus.py",
                       classname = "Nimbus",
                       command = "start",
                       config_file = "default-storm-start.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()


  @patch("storm._find_real_user_min_uid")
  def test_configure_secured(self, find_real_user_max_pid):
    find_real_user_max_pid.return_value = 500
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/nimbus.py",
                       classname = "Nimbus",
                       command = "configure",
                       config_file = "secured-storm-start.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
  @patch("storm._find_real_user_min_uid")
  def test_start_secured(self, find_real_user_max_pid):
    find_real_user_max_pid.return_value = 500
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/nimbus.py",
                       classname = "Nimbus",
                       command = "start",
                       config_file = "secured-storm-start.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()

  def assert_configure_default(self):
    storm_yarn_content = super(TestStormJaasConfiguration, self).assert_configure_default(confDir="/usr/hdp/current/storm-nimbus/conf")
    
    self.assertTrue(storm_yarn_content.find('_JAAS_PLACEHOLDER') == -1, 'Placeholder have to be substituted')
      
    self.assertTrue(storm_yarn_content.find('-Djava.security.auth.login.config') == -1, 'JAAS security settings has not to be present')
    self.assertTrue(storm_yarn_content.find('NON_SECURED_TRANSPORT_CLASS') >= 0, 'Non secured transport class should be used')
  

  def assert_configure_secured(self):

    storm_yarn_content = super(TestStormJaasConfiguration, self).assert_configure_secured(confDir="/usr/hdp/current/storm-nimbus/conf")
    self.assertResourceCalled('TemplateConfig', '/usr/hdp/current/storm-nimbus/conf/client_jaas.conf',
      owner = 'storm',
      mode = 0o644
    )
    self.assertResourceCalled('File', '/usr/hdp/current/storm-nimbus/conf/worker-launcher.cfg',
      owner = 'root',
      content = Template('worker-launcher.cfg.j2', min_user_ruid = 500),
      group = 'hadoop',
    )
    
    self.assertTrue(storm_yarn_content.find('_JAAS_PLACEHOLDER') == -1, 'Placeholder have to be substituted')
    self.assertTrue(storm_yarn_content.find('_storm') == -1, 'pairs start with _strom has to be removed')
    
    self.assertTrue(storm_yarn_content.find('-Djava.security.auth.login.config') >= 0, 'JAAS security settings has to be present')
    self.assertTrue(storm_yarn_content.find('SECURED_TRANSPORT_CLASS') >= 0, 'Secured transport class should be used')
