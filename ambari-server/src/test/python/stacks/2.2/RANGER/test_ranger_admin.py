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

class TestRangerAdmin(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "RANGER/0.4.0/package"
  STACK_VERSION = "2.2"

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_admin.py",
                   classname = "RangerAdmin",
                   command = "configure",
                   config_file="ranger-admin-default.json",
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()
    
  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_admin.py",
                   classname = "RangerAdmin",
                   command = "start",
                   config_file="ranger-admin-default.json",
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
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
                   hdp_stack_version = self.STACK_VERSION,
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
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()
    
  def test_start_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_admin.py",
                   classname = "RangerAdmin",
                   command = "start",
                   config_file="ranger-admin-secured.json",
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
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
                   hdp_stack_version = self.STACK_VERSION,
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
    self.assertResourceCalled('Execute', 'mysql -u root --password=rootpassword -h localhost  -s -e "select version();"',logoutput = True,
                              environment = {})
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


  def test_pre_rolling_upgrade_23(self, ):
    config_file = self.get_src_folder()+"/test/python/stacks/2.2/configs/ranger-admin-upgrade.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    json_content['commandParams']['version'] = '2.3.0.0-1234'

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_usersync.py",
                       classname = "RangerAdmin",
                       command = "pre_rolling_restart",
                       config_dict = json_content,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = [(0, None), (0, None)],
                       mocks_dict = mocks_dict)

    self.assertResourceCalled("Execute", ('hdp-select', 'set', 'ranger-admin', '2.3.0.0-1234'), sudo=True)

    self.assertEquals(2, mocks_dict['call'].call_count)
    self.assertEquals(
      "conf-select create-conf-dir --package ranger-admin --stack-version 2.3.0.0-1234 --conf-version 0",
       mocks_dict['call'].call_args_list[0][0][0])
    self.assertEquals(
      "conf-select set-conf-dir --package ranger-admin --stack-version 2.3.0.0-1234 --conf-version 0",
       mocks_dict['call'].call_args_list[1][0][0])
