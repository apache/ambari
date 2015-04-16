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
from mock.mock import MagicMock, patch
from stacks.utils.RMFTestCase import *

class TestRangerAdmin(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "RANGER/0.4.0/package"
  STACK_VERSION = "2.2"

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_admin.py",
                   classname = "RangerAdmin",
                   command = "configure",
                   config_file="default.json",
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()
    
  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_admin.py",
                   classname = "RangerAdmin",
                   command = "start",
                   config_file="default.json",
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertResourceCalled('Execute', '/usr/bin/ranger-admin-start',
        not_if = 'ps -ef | grep proc_rangeradmin | grep -v grep',
        user = 'ranger',
    )
    self.assertNoMoreResources()
    
  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_admin.py",
                   classname = "RangerAdmin",
                   command = "stop",
                   config_file="default.json",
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', '/usr/bin/ranger-admin-stop',
        user = 'ranger',
    )
    self.assertNoMoreResources()
    
  def test_configure_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_admin.py",
                   classname = "RangerAdmin",
                   command = "configure",
                   config_file="secured.json",
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()
    
  def test_start_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_admin.py",
                   classname = "RangerAdmin",
                   command = "start",
                   config_file="secured.json",
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertResourceCalled('Execute', '/usr/bin/ranger-admin-start',
        not_if = 'ps -ef | grep proc_rangeradmin | grep -v grep',
        user = 'ranger',
    )
    self.assertNoMoreResources()
    
  def test_stop_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_admin.py",
                   classname = "RangerAdmin",
                   command = "stop",
                   config_file="secured.json",
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', '/usr/bin/ranger-admin-stop',
        user = 'ranger',
    )
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Execute', 'mysql -u root --password=aa -h localhost  -s -e "select version();"',)
    self.assertResourceCalled('File', '/tmp/mysql-connector-java.jar',
        content = DownloadSource('http://c6401.ambari.apache.org:8080/resources//mysql-jdbc-driver.jar'),
    )
    self.assertResourceCalled('Execute', ('cp',
     '--remove-destination',
     '/tmp/mysql-connector-java.jar',
     '/usr/share/java/mysql-connector-java.jar'),
        not_if = 'test -f /usr/share/java/mysql-connector-java.jar',
        sudo = True,
        path = ['/bin', '/usr/bin/'],
    )
    self.assertResourceCalled('ModifyPropertiesFile', '/usr/hdp/current/ranger-admin/install.properties',
        properties = self.getConfig()['configurations']['admin-properties'],
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
      
  def assert_configure_secured(self):
    self.assertResourceCalled('Execute', 'mysql -u root --password=rootpassword -h localhost  -s -e "select version();"',)
    self.assertResourceCalled('File', '/tmp/mysql-connector-java.jar',
        content = DownloadSource('http://c6401.ambari.apache.org:8080/resources//mysql-jdbc-driver.jar'),
    )
    self.assertResourceCalled('Execute', ('cp',
     '--remove-destination',
     '/tmp/mysql-connector-java.jar',
     '/usr/share/java/mysql-connector-java.jar'),
        not_if = 'test -f /usr/share/java/mysql-connector-java.jar',
        sudo = True,
        path = ['/bin', '/usr/bin/'],
    )
    self.assertResourceCalled('ModifyPropertiesFile', '/usr/hdp/current/ranger-admin/install.properties',
        properties = self.getConfig()['configurations']['admin-properties'],
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
