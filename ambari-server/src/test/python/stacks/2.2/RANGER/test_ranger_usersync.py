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
from mock.mock import MagicMock, patch
from stacks.utils.RMFTestCase import *

class TestRangerUsersync(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "RANGER/0.4.0/package"
  STACK_VERSION = "2.2"

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_usersync.py",
                   classname = "RangerUsersync",
                   command = "configure",
                   config_file="ranger-admin-default.json",
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()
    
  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_usersync.py",
                   classname = "RangerUsersync",
                   command = "start",
                   config_file="ranger-admin-default.json",
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertResourceCalled('Execute', ('/usr/bin/ranger-usersync-start',),
        environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        not_if = 'ps -ef | grep proc_rangerusersync | grep -v grep',
        sudo = True,
    )
    self.assertNoMoreResources()
    
  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_usersync.py",
                   classname = "RangerUsersync",
                   command = "stop",
                   config_file="ranger-admin-default.json",
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', ('/usr/bin/ranger-usersync-stop',),
	    environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        sudo = True
    )
    self.assertNoMoreResources()
    
  def test_configure_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_usersync.py",
                   classname = "RangerUsersync",
                   command = "configure",
                   config_file="ranger-admin-secured.json",
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()
    
  def test_start_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_usersync.py",
                   classname = "RangerUsersync",
                   command = "start",
                   config_file="ranger-admin-secured.json",
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertResourceCalled('Execute', ('/usr/bin/ranger-usersync-start',),
        environment= {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        not_if = 'ps -ef | grep proc_rangerusersync | grep -v grep',
        sudo = True
    )
    self.assertNoMoreResources()
    
  def test_stop_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_usersync.py",
                   classname = "RangerUsersync",
                   command = "stop",
                   config_file="ranger-admin-secured.json",
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', ('/usr/bin/ranger-usersync-stop',),
	    environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        sudo = True
    )
    self.assertNoMoreResources()
    
  @patch("setup_ranger.setup_usersync")
  def test_upgrade(self, setup_usersync_mock):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_usersync.py",
                       classname = "RangerUsersync",
                       command = "restart",
                       config_file="ranger-usersync-upgrade.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertTrue(setup_usersync_mock.called)
    self.assertResourceCalled("Execute", ("/usr/bin/ranger-usersync-stop",),
                              environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_67'},
                              sudo = True
    )
    self.assertResourceCalled("Execute", ('hdp-select', 'set', 'ranger-usersync', '2.2.2.0-2399'), sudo=True)

  @patch("setup_ranger.setup_usersync")
  def test_upgrade_23(self, setup_usersync_mock):
    config_file = self.get_src_folder()+"/test/python/stacks/2.2/configs/ranger-usersync-upgrade.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    json_content['commandParams']['version'] = '2.3.0.0-1234'

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_usersync.py",
                       classname = "RangerUsersync",
                       command = "restart",
                       config_dict = json_content,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = [(0, None, ''), (0, None)],
                       mocks_dict = mocks_dict)

    self.assertTrue(setup_usersync_mock.called)
    self.assertResourceCalled("Execute", ("/usr/bin/ranger-usersync-stop",),
                              environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_67'},
                              sudo = True)
    self.assertResourceCalledIgnoreEarlier("Execute", ('hdp-select', 'set', 'ranger-usersync', '2.3.0.0-1234'), sudo=True)

    self.assertEquals(2, mocks_dict['call'].call_count)
    self.assertEquals(1, mocks_dict['checked_call'].call_count)
    self.assertEquals(
      ('conf-select', 'set-conf-dir', '--package', 'ranger-usersync', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['checked_call'].call_args_list[0][0][0])
    self.assertEquals(
      ('conf-select', 'create-conf-dir', '--package', 'ranger-usersync', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['call'].call_args_list[0][0][0])

  def assert_configure_default(self):
    self.assertResourceCalled('PropertiesFile', '/usr/hdp/current/ranger-usersync/install.properties',
        properties = self.getConfig()['configurations']['usersync-properties'],
    )

    custom_config=dict()
    custom_config['unix_user'] = "ranger"
    custom_config['unix_group'] = "ranger"
    self.assertResourceCalled('ModifyPropertiesFile', '/usr/hdp/current/ranger-usersync/install.properties',
        properties = custom_config,
    )

    self.assertResourceCalled('Execute', 'cd /usr/hdp/current/ranger-usersync && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E /usr/hdp/current/ranger-usersync/setup.sh',
        logoutput = True,
        environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
    )
    self.assertResourceCalled('File', '/usr/bin/ranger-usersync-start',
        owner = 'ranger',
    )
    self.assertResourceCalled('File', '/usr/bin/ranger-usersync-stop',
        owner = 'ranger',
    )
    self.assertResourceCalled('File', '/usr/hdp/current/ranger-usersync/ranger-usersync-services.sh',
        mode = 0755,
    )
    self.assertResourceCalled('Directory', '/var/log/ranger/usersync',
        owner = custom_config['unix_user'],
        group = custom_config['unix_group']
    )

      
  def assert_configure_secured(self):
    self.assertResourceCalled('PropertiesFile', '/usr/hdp/current/ranger-usersync/install.properties',
        properties = self.getConfig()['configurations']['usersync-properties'],
    )
    custom_config=dict()
    custom_config['unix_user'] = "ranger"
    custom_config['unix_group'] = "ranger"
    self.assertResourceCalled('ModifyPropertiesFile', '/usr/hdp/current/ranger-usersync/install.properties',
        properties = custom_config,
    )
    self.assertResourceCalled('Execute', 'cd /usr/hdp/current/ranger-usersync && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E /usr/hdp/current/ranger-usersync/setup.sh',
        logoutput = True,
        environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
    )
    self.assertResourceCalled('File', '/usr/bin/ranger-usersync-start',
        owner = 'ranger',
    )
    self.assertResourceCalled('File', '/usr/bin/ranger-usersync-stop',
        owner = 'ranger',
    )
    self.assertResourceCalled('File', '/usr/hdp/current/ranger-usersync/ranger-usersync-services.sh',
        mode = 0755,
    )
    self.assertResourceCalled('Directory', '/var/log/ranger/usersync',
        owner = custom_config['unix_user'],
        group = custom_config['unix_group']
    )
