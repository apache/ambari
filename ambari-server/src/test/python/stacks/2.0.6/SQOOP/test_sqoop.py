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
import json

class TestSqoop(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "SQOOP/1.4.4.2.0/package"
  STACK_VERSION = "2.0.6"

  CONFIG_OVERRIDES = {"serviceName":"SQOOP", "role":"SQOOP"}

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/sqoop_client.py",
                       classname = "SqoopClient",
                       command = "configure",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Link', '/usr/lib/sqoop/lib/mysql-connector-java.jar',
                              to = '/usr/share/java/mysql-connector-java.jar',)
    self.assertResourceCalled('Directory', '/usr/lib/sqoop/conf',
                              create_parents = True,
                              owner = 'sqoop',
                              group = 'hadoop',)
    self.assertResourceCalled('XmlConfig', 'sqoop-site.xml',
                              owner = 'sqoop',
                              group = 'hadoop',
                              conf_dir = '/usr/lib/sqoop/conf',
                              configurations = self.getConfig()['configurations']['sqoop-site'],
                              configuration_attributes = self.getConfig()['configurationAttributes']['sqoop-site'])
    self.assertResourceCalled('File', '/usr/lib/sqoop/conf/sqoop-env.sh',
                              owner = 'sqoop',
                              group = 'hadoop',
                              content = InlineTemplate(self.getConfig()['configurations']['sqoop-env']['content'])
                              )
    self.assertResourceCalled('File', '/usr/lib/sqoop/conf/sqoop-env-template.sh',
                              only_if = 'test -e /usr/lib/sqoop/conf/sqoop-env-template.sh',
                              owner = 'sqoop',
                              group = 'hadoop',)
    self.assertResourceCalled('File', '/usr/lib/sqoop/conf/sqoop-site-template.xml',
                              only_if = 'test -e /usr/lib/sqoop/conf/sqoop-site-template.xml',
                              owner = 'sqoop',
                              group = 'hadoop',)
    self.assertResourceCalled('File', '/usr/lib/sqoop/conf/sqoop-site.xml',
                              only_if = 'test -e /usr/lib/sqoop/conf/sqoop-site.xml',
                              owner = 'sqoop',
                              group = 'hadoop',)
    self.assertNoMoreResources()

  def test_configure_add_jdbc(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/secured.json"
    with open(config_file, "r") as f:
      loaded_json = json.load(f)

    loaded_json['configurations']['sqoop-env']['jdbc_drivers'] = 'org.postgresql.Driver, oracle.jdbc.driver.OracleDriver'


    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/sqoop_client.py",
                       classname = "SqoopClient",
                       command = "configure",
                       config_dict = loaded_json,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Link', '/usr/lib/sqoop/lib/mysql-connector-java.jar',
                              to = '/usr/share/java/mysql-connector-java.jar',
                              )
    self.assertResourceCalled('File', '/usr/lib/sqoop/lib/test-postgres-jdbc.jar',
                              content = DownloadSource('http://c6401.ambari.apache.org:8080/resources/test-postgres-jdbc.jar'),
                              mode = 0o644,
                              )
    self.assertResourceCalled('File', '/usr/lib/sqoop/lib/oracle-jdbc-driver.jar',
                              content = DownloadSource('http://c6401.ambari.apache.org:8080/resources/oracle-jdbc-driver.jar'),
                              mode = 0o644,
                              )
    self.assertResourceCalled('Directory', '/usr/lib/sqoop/conf',
                              owner = 'sqoop',
                              group = 'hadoop',
                              create_parents = True,
                              )
    self.assertResourceCalled('XmlConfig', 'sqoop-site.xml',
                              owner = 'sqoop',
                              group = 'hadoop',
                              conf_dir = '/usr/lib/sqoop/conf',
                              configurations = self.getConfig()['configurations']['sqoop-site'],
                              configuration_attributes = self.getConfig()['configurationAttributes']['sqoop-site'])
    self.assertResourceCalled('File', '/usr/lib/sqoop/conf/sqoop-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['sqoop-env']['content']),
                              owner = 'sqoop',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/usr/lib/sqoop/conf/sqoop-env-template.sh',
                              owner = 'sqoop',
                              only_if = 'test -e /usr/lib/sqoop/conf/sqoop-env-template.sh',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/usr/lib/sqoop/conf/sqoop-site-template.xml',
                              owner = 'sqoop',
                              only_if = 'test -e /usr/lib/sqoop/conf/sqoop-site-template.xml',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/usr/lib/sqoop/conf/sqoop-site.xml',
                              owner = 'sqoop',
                              only_if = 'test -e /usr/lib/sqoop/conf/sqoop-site.xml',
                              group = 'hadoop',
                              )
    self.assertNoMoreResources()


  def test_pre_upgrade_restart_23(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/sqoop_client.py",
                       classname = "SqoopClient",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       config_overrides = self.CONFIG_OVERRIDES,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       mocks_dict = mocks_dict)

    self.assertResourceCalled("Execute", ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'sqoop-client', version), sudo=True)
