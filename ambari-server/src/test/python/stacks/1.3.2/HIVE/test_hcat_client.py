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

class TestHcatClient(RMFTestCase):

  def test_configure_default(self):
    self.executeScript("1.3.2/services/HIVE/package/scripts/hcat_client.py",
                       classname = "HCatClient",
                       command = "configure",
                       config_file="default.json"
    )

    self.assertResourceCalled('Directory', '/etc/hcatalog/conf',
      owner = 'hcat',
      group = 'hadoop',
    )
    self.assertResourceCalled('Directory', '/var/run/webhcat',
      owner = 'hcat',
      recursive = True,
    )
    self.assertResourceCalled('XmlConfig', 'hive-site.xml',
      owner = 'hive',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hive/conf',
      configurations = self.getConfig()['configurations']['hive-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['hive-site']
    )
    self.assertResourceCalled('File', '/etc/hcatalog/conf/hcat-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['hcat-env']['content']),
                              owner = 'hcat',
                              group = 'hadoop',
                              )

    self.assertNoMoreResources()



  def test_configure_secured(self):
    self.executeScript("1.3.2/services/HIVE/package/scripts/hcat_client.py",
                         classname = "HCatClient",
                         command = "configure",
                         config_file="secured.json"
    )

    self.assertResourceCalled('Directory', '/etc/hcatalog/conf',
      owner = 'hcat',
      group = 'hadoop',
    )
    self.assertResourceCalled('Directory', '/var/run/webhcat',
      owner = 'hcat',
      recursive = True,
    )
    self.assertResourceCalled('XmlConfig', 'hive-site.xml',
      owner = 'hive',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hive/conf',
      configurations = self.getConfig()['configurations']['hive-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['hive-site']
    )
    self.assertResourceCalled('File', '/etc/hcatalog/conf/hcat-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['hcat-env']['content']),
                              owner = 'hcat',
                              group = 'hadoop',
                              )
    self.assertNoMoreResources()
