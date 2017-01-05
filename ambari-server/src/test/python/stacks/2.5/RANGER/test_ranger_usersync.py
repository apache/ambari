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
from only_for_platform import not_for_platform, PLATFORM_WINDOWS

@not_for_platform(PLATFORM_WINDOWS)
class TestRangerUsersync(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "RANGER/0.4.0/package"
  STACK_VERSION = "2.5"

  @patch("os.path.isfile")
  def test_configure_default(self, isfile_mock):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_usersync.py",
                   classname = "RangerUsersync",
                   command = "configure",
                   config_file="ranger-admin-default.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertTrue(isfile_mock.called)
    self.assertNoMoreResources()
   
  @patch("os.path.isfile")
  def test_start_default(self, isfile_mock):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_usersync.py",
                   classname = "RangerUsersync",
                   command = "start",
                   config_file="ranger-admin-default.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertResourceCalled('Execute', '/usr/bin/ranger-usersync-start',
        environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        not_if = 'ps -ef | grep proc_rangerusersync | grep -v grep',
        user = 'ranger',
    )
    self.assertTrue(isfile_mock.called)
    self.assertNoMoreResources()
    
  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_usersync.py",
                   classname = "RangerUsersync",
                   command = "stop",
                   config_file="ranger-admin-default.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', ('/usr/bin/ranger-usersync-stop',),
      environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        sudo = True
    )
    self.assertResourceCalled('File', '/var/run/ranger/usersync.pid',
      action = ['delete']
    )
    self.assertNoMoreResources()

  @patch("os.path.isfile")    
  def test_configure_secured(self, isfile_mock):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_usersync.py",
                   classname = "RangerUsersync",
                   command = "configure",
                   config_file="ranger-admin-secured.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertTrue(isfile_mock.called)
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/var/run/ranger',
      mode=0755,
      owner = 'ranger',
      group = 'hadoop',
      cd_access = "a",
      create_parents=True
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-usersync/conf/ranger-usersync-env-piddir.sh',
      content = 'export USERSYNC_PID_DIR_PATH=/var/run/ranger\nexport UNIX_USERSYNC_USER=ranger',
      owner = 'ranger',
      group = 'ranger',
      mode = 0755
    )

    self.assertResourceCalled('Directory', '/var/log/ranger/usersync',
      owner='ranger',
      group='ranger',
      create_parents = True,
      cd_access = 'a',
      mode = 0755,
      recursive_ownership = True
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-usersync/conf/ranger-usersync-env-logdir.sh',
      content = 'export logdir=/var/log/ranger/usersync',
      owner = 'ranger',
      group = 'ranger',
      mode = 0755
    )

    self.assertResourceCalled('Directory', '/usr/hdp/current/ranger-usersync/conf/',
      owner = 'ranger'
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-usersync/conf/log4j.properties',
      owner = 'ranger',
      group = 'ranger',
      content = InlineTemplate(self.getConfig()['configurations']['usersync-log4j']['content']),
      mode = 0644
    )

    self.assertResourceCalled('XmlConfig', 'ranger-ugsync-site.xml',
      owner = 'ranger',
      group = 'ranger',
      conf_dir = '/usr/hdp/current/ranger-usersync/conf',
      configurations = self.getConfig()['configurations']['ranger-ugsync-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['ranger-ugsync-site'],
      mode = 0644
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-usersync/conf/ranger-ugsync-default.xml',
      owner = 'ranger',
      group = 'ranger'
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-usersync/conf/log4j.properties',
      owner = 'ranger',
      group = 'ranger'
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-usersync/native/credValidator.uexe',
      group = 'ranger',
      mode = 04555
    )

    self.assertResourceCalled('Execute', ('/usr/jdk64/jdk1.7.0_45/bin/java', '-cp', '/usr/hdp/current/ranger-usersync/lib/*', 'org.apache.ranger.credentialapi.buildks', 'create', 'usersync.ssl.key.password', '-value', 'UnIx529p', '-provider', 'jceks://file/usr/hdp/current/ranger-usersync/conf/ugsync.jceks'),
      environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
      logoutput=True,
      sudo = True
    )

    self.assertResourceCalled('Execute', ('/usr/jdk64/jdk1.7.0_45/bin/java', '-cp', '/usr/hdp/current/ranger-usersync/lib/*', 'org.apache.ranger.credentialapi.buildks', 'create', 'usersync.ssl.truststore.password', '-value', 'changeit', '-provider', 'jceks://file/usr/hdp/current/ranger-usersync/conf/ugsync.jceks'),
      environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
      logoutput=True,
      sudo = True
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-usersync/conf/ugsync.jceks',
      owner = 'ranger',
      group = 'ranger',
      mode = 0640
    )

    self.assertResourceCalled('File', '/usr/bin/ranger-usersync-start',
      owner = 'ranger',
      group = 'ranger'
    )

    self.assertResourceCalled('File', '/usr/bin/ranger-usersync-stop',
      owner = 'ranger',
      group = 'ranger'
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-usersync/ranger-usersync-services.sh',
      mode = 0755
    )

    self.assertResourceCalled('Execute', ('ln', '-sf', '/usr/hdp/current/ranger-usersync/ranger-usersync-services.sh', '/usr/bin/ranger-usersync'),
      not_if = 'ls /usr/bin/ranger-usersync',
      only_if = 'ls /usr/hdp/current/ranger-usersync/ranger-usersync-services.sh',
      sudo = True
    )

    self.assertResourceCalled('XmlConfig', 'core-site.xml',
      owner = 'ranger',
      group = 'ranger',
      conf_dir = '/usr/hdp/current/ranger-usersync/conf',
      configurations = self.getConfig()['configurations']['core-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['core-site'],
      mode = 0644
    )
