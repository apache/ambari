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
class TestRangerTagsync(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "RANGER/0.4.0/package"
  STACK_VERSION = "2.6"

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_tagsync.py",
                   classname = "RangerTagsync",
                   command = "configure",
                   config_file="ranger-admin-default.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_tagsync.py",
                   classname = "RangerTagsync",
                   command = "start",
                   config_file="ranger-admin-default.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertResourceCalled('Execute', '/usr/hdp/current/ranger-tagsync/ranger-tagsync-services.sh start',
        environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        not_if = 'ps -ef | grep proc_rangertagsync | grep -v grep',
        user = 'ranger',
    )
    self.assertNoMoreResources()
    
  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_tagsync.py",
                   classname = "RangerTagsync",
                   command = "stop",
                   config_file="ranger-admin-default.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', '/usr/hdp/current/ranger-tagsync/ranger-tagsync-services.sh stop',
        environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        user = 'ranger'
    )

    self.assertResourceCalled('File', '/var/run/ranger/tagsync.pid',
      action = ['delete']
    )
    self.assertNoMoreResources()

  def test_configure_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_tagsync.py",
                   classname = "RangerTagsync",
                   command = "configure",
                   config_file="ranger-admin-secured.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/usr/hdp/current/ranger-tagsync/conf',
      owner = 'ranger',
      group = 'ranger',
      create_parents = True
    )

    self.assertResourceCalled('Directory', '/var/run/ranger',
      mode=0755,
      owner = 'ranger',
      group = 'hadoop',
      cd_access = "a",
      create_parents=True
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-tagsync/conf/ranger-tagsync-env-piddir.sh',
      content = 'export TAGSYNC_PID_DIR_PATH=/var/run/ranger\nexport UNIX_TAGSYNC_USER=ranger',
      owner = 'ranger',
      group = 'ranger',
      mode = 0755
    )

    self.assertResourceCalled('Directory', '/var/log/ranger/tagsync',
      owner = 'ranger',
      group = 'ranger',
      cd_access = "a",
      mode=0755,
      create_parents = True
    )

    self.assertResourceCalled('File',
      '/usr/hdp/current/ranger-tagsync/conf/ranger-tagsync-env-logdir.sh',
      owner = 'ranger',
      content = 'export RANGER_TAGSYNC_LOG_DIR=/var/log/ranger/tagsync',
      group = 'ranger',
      mode=0755
    )

    self.assertResourceCalled('XmlConfig', 'ranger-tagsync-site.xml',
      owner = 'ranger',
      group = 'ranger',
      conf_dir = '/usr/hdp/current/ranger-tagsync/conf',
      configurations = self.getConfig()['configurations']['ranger-tagsync-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['ranger-tagsync-site'],
      mode=0644
    )

    self.assertResourceCalled('Directory', '/etc/security/serverKeys',
      create_parents = True,
      cd_access = 'a',
    )

    self.assertResourceCalled('Directory', '/etc/ranger/tagsync/',
      owner = 'ranger',
      group = 'ranger',
      create_parents = True,
      mode = 0775,
      cd_access = 'a',
    )

    ranger_tagsync_policymgr_ssl_copy = {}
    ranger_tagsync_policymgr_ssl_copy.update(self.getConfig()['configurations']['ranger-tagsync-policymgr-ssl'])
    ranger_tagsync_password_properties = ['xasecure.policymgr.clientssl.keystore.password', 'xasecure.policymgr.clientssl.truststore.password']
    for prop in ranger_tagsync_password_properties:
      if prop in ranger_tagsync_policymgr_ssl_copy:
        ranger_tagsync_policymgr_ssl_copy[prop] = "_"

    self.assertResourceCalled('XmlConfig', 'ranger-policymgr-ssl.xml',
      owner = 'ranger',
      group = 'ranger',
      conf_dir = '/usr/hdp/current/ranger-tagsync/conf',
      configurations = ranger_tagsync_policymgr_ssl_copy,
      configuration_attributes = self.getConfig()['configuration_attributes']['ranger-tagsync-policymgr-ssl'],
      mode = 0644,
    )

    self.assertResourceCalled('Execute', (u'/usr/jdk64/jdk1.7.0_45/bin/java',
      '-cp',
      u'/usr/hdp/current/ranger-tagsync/lib/*',
      'org.apache.ranger.credentialapi.buildks',
      'create',
      'sslKeyStore',
      '-value',
      'myKeyFilePassword',
      '-provider',
      'jceks://file/etc/ranger/tagsync/rangercred.jceks'),
      logoutput = True,
      environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
      sudo = True,
    )

    self.assertResourceCalled('Execute', (u'/usr/jdk64/jdk1.7.0_45/bin/java',
      '-cp',
      u'/usr/hdp/current/ranger-tagsync/lib/*',
      'org.apache.ranger.credentialapi.buildks',
      'create',
      'sslTrustStore',
      '-value',
      'changeit',
      '-provider',
      'jceks://file/etc/ranger/tagsync/rangercred.jceks'),
      logoutput = True,
      environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
      sudo = True,
    )

    self.assertResourceCalled('File', '/etc/ranger/tagsync/rangercred.jceks',
      owner = 'ranger',
      group = 'ranger',
      mode = 0640,
    )

    atlas_tagsync_ssl_copy = {}
    atlas_tagsync_ssl_copy.update(self.getConfig()['configurations']['atlas-tagsync-ssl'])
    for prop in ranger_tagsync_password_properties:
      if prop in atlas_tagsync_ssl_copy:
        atlas_tagsync_ssl_copy[prop] = "_"

    self.assertResourceCalled('XmlConfig', 'atlas-tagsync-ssl.xml',
      group = 'ranger',
      conf_dir = '/usr/hdp/current/ranger-tagsync/conf',
      mode = 0644,
      configuration_attributes = UnknownConfigurationMock(),
      owner = 'ranger',
      configurations = atlas_tagsync_ssl_copy
    )

    self.assertResourceCalled('Execute', (u'/usr/jdk64/jdk1.7.0_45/bin/java',
      '-cp',
      u'/usr/hdp/current/ranger-tagsync/lib/*',
      'org.apache.ranger.credentialapi.buildks',
      'create',
      'sslKeyStore',
      '-value',
      'myKeyFilePassword',
      '-provider',
      'jceks://file/etc/ranger/tagsync/atlascred.jceks'),
      logoutput = True,
      environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
      sudo = True,
    )

    self.assertResourceCalled('Execute', (u'/usr/jdk64/jdk1.7.0_45/bin/java',
      '-cp',
      u'/usr/hdp/current/ranger-tagsync/lib/*',
      'org.apache.ranger.credentialapi.buildks',
      'create',
      'sslTrustStore',
      '-value',
      'changeit',
      '-provider',
      'jceks://file/etc/ranger/tagsync/atlascred.jceks'),
      logoutput = True,
      environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
      sudo = True,
    )

    self.assertResourceCalled('File', '/etc/ranger/tagsync/atlascred.jceks',
      owner = 'ranger',
      group = 'ranger',
      mode = 0640,
    )


    self.assertResourceCalled('PropertiesFile', '/usr/hdp/current/ranger-tagsync/conf/atlas-application.properties',
      properties = self.getConfig()['configurations']['tagsync-application-properties'],
      mode=0755,
      owner='ranger',
      group='ranger'
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-tagsync/conf/log4j.properties',
      owner = 'ranger',
      group = 'ranger',
      content = InlineTemplate(self.getConfig()['configurations']['tagsync-log4j']['content']),
      mode = 0644
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-tagsync/ranger-tagsync-services.sh',
      mode = 0755,
    )

    self.assertResourceCalled('Execute', ('ln', '-sf', '/usr/hdp/current/ranger-tagsync/ranger-tagsync-services.sh', '/usr/bin/ranger-tagsync'),
      not_if='ls /usr/bin/ranger-tagsync',
      only_if='ls /usr/hdp/current/ranger-tagsync/ranger-tagsync-services.sh',
      sudo=True
    )

    self.assertResourceCalled('XmlConfig', 'core-site.xml',
      owner = 'ranger',
      group = 'ranger',
      conf_dir = '/usr/hdp/current/ranger-tagsync/conf',
      configurations = self.getConfig()['configurations']['core-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['core-site'],
      mode = 0644
    )
