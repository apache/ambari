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

class TestHiveClient(RMFTestCase):

  def test_configure_default(self):
    self.executeScript("1.3.2/services/HIVE/package/scripts/hive_client.py",
                       classname = "HiveClient",
                       command = "configure",
                       config_file="default.json"
    )
    self.assertResourceCalled('Directory', '/etc/hive/conf',
      owner = 'hive',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('XmlConfig', 'hive-site.xml',
      owner = 'hive',
      group = 'hadoop',
      mode = 420,
      conf_dir = '/etc/hive/conf',
      configurations = self.getConfig()['configurations']['hive-site'],
    )
    self.assertResourceCalled('Execute', "/bin/sh -c 'cd /usr/lib/ambari-agent/ && curl -kf --retry 5 http://c6401.ambari.apache.org:8080/resources/DBConnectionVerification.jar -o DBConnectionVerification.jar'",
      not_if = '[ -f DBConnectionVerification.jar]',
    )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-env.sh',
      content = Template('hive-env.sh.j2', conf_dir="/etc/hive/conf"),
      owner = 'hive',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-default.xml.template',
      owner = 'hive',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-env.sh.template',
      owner = 'hive',
      group = 'hadoop',
    )
    self.assertResourceCalled('PropertiesFile',
                              'hive-exec-log4j.properties',
                              dir='/etc/hive/conf',
                              properties={'property1': 'value1'},
                              mode=0664,
                              owner='hive',
                              group='hadoop'
    )
    self.assertResourceCalled('PropertiesFile',
                              'hive-log4j.properties',
                              dir='/etc/hive/conf',
                              properties={'property1': 'value1'},
                              mode=0664,
                              owner='hive',
                              group='hadoop'
    )
    self.assertNoMoreResources()



  def test_configure_secured(self):
    self.executeScript("1.3.2/services/HIVE/package/scripts/hive_client.py",
                       classname = "HiveClient",
                       command = "configure",
                       config_file="secured.json"
    )
    self.assertResourceCalled('Directory', '/etc/hive/conf',
      owner = 'hive',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('XmlConfig', 'hive-site.xml',
      owner = 'hive',
      group = 'hadoop',
      mode = 420,
      conf_dir = '/etc/hive/conf',
      configurations = self.getConfig()['configurations']['hive-site'],
    )
    self.assertResourceCalled('Execute', "/bin/sh -c 'cd /usr/lib/ambari-agent/ && curl -kf --retry 5 http://c6401.ambari.apache.org:8080/resources/DBConnectionVerification.jar -o DBConnectionVerification.jar'",
      not_if = '[ -f DBConnectionVerification.jar]',
    )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-env.sh',
      content = Template('hive-env.sh.j2', conf_dir="/etc/hive/conf"),
      owner = 'hive',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-default.xml.template',
      owner = 'hive',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-env.sh.template',
      owner = 'hive',
      group = 'hadoop',
    )
    self.assertResourceCalled('PropertiesFile',
                              'hive-exec-log4j.properties',
                              dir='/etc/hive/conf',
                              properties={'property1': 'value1'},
                              mode=0664,
                              owner='hive',
                              group='hadoop'
    )
    self.assertResourceCalled('PropertiesFile',
                              'hive-log4j.properties',
                              dir='/etc/hive/conf',
                              properties={'property1': 'value1'},
                              mode=0664,
                              owner='hive',
                              group='hadoop'
    )
    self.assertNoMoreResources()