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
class TestRangerAdmin(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "RANGER/0.4.0/package"
  STACK_VERSION = "2.2"

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_admin.py",
                   classname = "RangerAdmin",
                   command = "configure",
                   config_file="ranger-admin-default.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()
    
  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_admin.py",
                   classname = "RangerAdmin",
                   command = "start",
                   config_file="ranger-admin-default.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertResourceCalled('ModifyPropertiesFile', '/usr/hdp/current/ranger-admin/install.properties',
      owner = 'ranger',
      properties = {'db_password': '_', 'db_root_password': '_', 'audit_db_password': '_'}
    )
    self.assertResourceCalled('Execute', '/usr/bin/ranger-admin-start',
        environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        not_if = 'ps -ef | grep proc_rangeradmin | grep -v grep',
        user = 'ranger',
    )
    self.assertNoMoreResources()
    
  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_admin.py",
                   classname = "RangerAdmin",
                   command = "stop",
                   config_file="ranger-admin-default.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', '/usr/bin/ranger-admin-stop',
        environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        user = 'ranger'
    )
    self.assertNoMoreResources()
    
  def test_configure_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_admin.py",
                   classname = "RangerAdmin",
                   command = "configure",
                   config_file="ranger-admin-secured.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()
    
  def test_start_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_admin.py",
                   classname = "RangerAdmin",
                   command = "start",
                   config_file="ranger-admin-secured.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertResourceCalled('ModifyPropertiesFile', '/usr/hdp/current/ranger-admin/install.properties',
      owner = 'ranger',
      properties = {'db_password': '_', 'db_root_password': '_', 'audit_db_password': '_'}
    )
    self.assertResourceCalled('Execute', '/usr/bin/ranger-admin-start',
        environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        not_if = 'ps -ef | grep proc_rangeradmin | grep -v grep',
        user = 'ranger',
    )
    self.assertNoMoreResources()
    
  def test_stop_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_admin.py",
                   classname = "RangerAdmin",
                   command = "stop",
                   config_file="ranger-admin-secured.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', '/usr/bin/ranger-admin-stop',
        user = 'ranger',
        environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'}
    )
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Execute', 'mysql -u root --password=aa -h localhost  -s -e "select version();"',logoutput = True,
                              environment = {})
    self.assertResourceCalled('File', '/tmp/mysql-connector-java.jar',
        content = DownloadSource('http://c6401.ambari.apache.org:8080/resources//mysql-connector-java.jar'),
        mode = 0644
    )
    self.assertResourceCalled('Execute', ('cp',
     '--remove-destination',
     '/tmp/mysql-connector-java.jar',
     '/usr/share/java/mysql-connector-java.jar'),
        sudo = True,
        path = ['/bin', '/usr/bin/'],
    )
    self.assertResourceCalled('File', '/usr/share/java/mysql-connector-java.jar',
      mode = 0644
    )
    self.assertResourceCalled('ModifyPropertiesFile', '/usr/hdp/current/ranger-admin/install.properties',
        properties = self.getConfig()['configurations']['admin-properties'],
    )
    custom_config=dict()
    custom_config['unix_user'] = "ranger"
    custom_config['unix_group'] = "ranger"
    self.assertResourceCalled('ModifyPropertiesFile', '/usr/hdp/current/ranger-admin/install.properties',
        properties = custom_config,
    )
    self.assertResourceCalled('ModifyPropertiesFile', '/usr/hdp/current/ranger-admin/install.properties',
        properties = {'SQL_CONNECTOR_JAR': '/usr/share/java/mysql-connector-java.jar'}
    )
    self.assertResourceCalled('Execute', 'cd /usr/hdp/current/ranger-admin && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E /usr/hdp/current/ranger-admin/setup.sh',
        logoutput = True,
        environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
    )
    self.assertResourceCalled('ModifyPropertiesFile', '/etc/ranger/admin/conf/xa_system.properties',
        properties = self.getConfig()['configurations']['ranger-site'],
    )
    self.assertResourceCalled('ModifyPropertiesFile', '/etc/ranger/admin/conf/ranger_webserver.properties',
        mode = 0744,
        properties = self.getConfig()['configurations']['ranger-site']
    )
    self.assertResourceCalled('Directory', '/var/log/ranger/admin',
        owner = custom_config['unix_user'],
        group = custom_config['unix_group']
    )

  def assert_configure_secured(self):
    self.assertResourceCalled('Execute', 'mysql -u root --password=rootpassword -h localhost  -s -e "select version();"',logoutput = True,
                              environment = {})
    self.assertResourceCalled('File', '/tmp/mysql-connector-java.jar',
        content = DownloadSource('http://c6401.ambari.apache.org:8080/resources//mysql-connector-java.jar'),
        mode = 0644
    )
    self.assertResourceCalled('Execute', ('cp',
     '--remove-destination',
     '/tmp/mysql-connector-java.jar',
     '/usr/share/java/mysql-connector-java.jar'),
        sudo = True,
        path = ['/bin', '/usr/bin/'],
    )
    self.assertResourceCalled('File', '/usr/share/java/mysql-connector-java.jar',
      mode = 0644
    )
    self.assertResourceCalled('ModifyPropertiesFile', '/usr/hdp/current/ranger-admin/install.properties',
        properties = self.getConfig()['configurations']['admin-properties'],
    )
    custom_config=dict()
    custom_config['unix_user'] = "ranger"
    custom_config['unix_group'] = "ranger"
    self.assertResourceCalled('ModifyPropertiesFile', '/usr/hdp/current/ranger-admin/install.properties',
        properties = custom_config,
    )
    self.assertResourceCalled('ModifyPropertiesFile', '/usr/hdp/current/ranger-admin/install.properties',
        properties = {'SQL_CONNECTOR_JAR': '/usr/share/java/mysql-connector-java.jar'}
    )
    self.assertResourceCalled('Execute', 'cd /usr/hdp/current/ranger-admin && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E /usr/hdp/current/ranger-admin/setup.sh',
        logoutput = True,
        environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
    )
    self.assertResourceCalled('ModifyPropertiesFile', '/etc/ranger/admin/conf/xa_system.properties',
        properties = self.getConfig()['configurations']['ranger-site'],
    )
    self.assertResourceCalled('ModifyPropertiesFile', '/etc/ranger/admin/conf/ranger_webserver.properties',
        mode = 0744,
        properties = self.getConfig()['configurations']['ranger-site']
    )
    self.assertResourceCalled('Directory', '/var/log/ranger/admin',
        owner = custom_config['unix_user'],
        group = custom_config['unix_group']
    )


  def test_pre_upgrade_restart_23(self, ):
    config_file = self.get_src_folder()+"/test/python/stacks/2.2/configs/ranger-admin-upgrade.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    json_content['commandParams']['version'] = '2.3.0.0-1234'

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_usersync.py",
                       classname = "RangerAdmin",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       mocks_dict = mocks_dict)

    self.assertResourceCalledIgnoreEarlier("Execute", ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'ranger-admin', '2.3.0.0-1234'), sudo=True)
