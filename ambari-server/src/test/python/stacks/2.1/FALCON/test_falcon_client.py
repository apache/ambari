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

from stacks.utils.RMFTestCase import *


class TestFalconClient(RMFTestCase):
  def test_configure_default(self):
    self.executeScript("2.1/services/FALCON/package/scripts/falcon_client.py",
                       classname="FalconClient",
                       command="configure",
                       config_file="default.json"
    )
    self.assertResourceCalled('Directory', '/var/run/falcon',
                              owner = 'falcon',
                              )
    self.assertResourceCalled('Directory', '/var/log/falcon',
                              owner = 'falcon',
                              recursive = True
                              )
    self.assertResourceCalled('Directory', '/var/lib/falcon/webapp',
                              owner = 'falcon',
                              )
    self.assertResourceCalled('Directory', '/usr/lib/falcon',
                              owner = 'falcon',
                              )
    self.assertResourceCalled('Directory', '/etc/falcon/conf',
                              owner = 'falcon',
                              recursive = True
                              )
    self.assertResourceCalled('File', '/etc/falcon/conf/falcon-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['falcon-env']['content']),
                              )
    self.assertResourceCalled('File', '/etc/falcon/conf/client.properties',
                              content = Template('client.properties.j2'),
                              mode = 0644,
                              )
    self.assertResourceCalled('PropertiesFile', '/etc/falcon/conf/runtime.properties',
                              mode = 0644,
                              properties = self.getConfig()['configurations']['falcon-runtime.properties'],
                              )
    self.assertResourceCalled('PropertiesFile', '/etc/falcon/conf/startup.properties',
                              mode = 0644,
                              properties = self.getConfig()['configurations']['falcon-startup.properties'],
                              )
    self.assertNoMoreResources()
