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

class TestPigClient(RMFTestCase):

  def test_configure_default(self):
    self.executeScript("1.3.2/services/PIG/package/scripts/pig_client.py",
                       classname = "PigClient",
                       command = "configure",
                       config_file="default.json"
    )
    
    self.assertResourceCalled('Directory', '/etc/pig/conf',
      owner = 'hdfs',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/pig/conf/pig-env.sh',
      owner = 'hdfs',
      content = InlineTemplate(self.getConfig()['configurations']['pig-env']['content'])
    )
    self.assertResourceCalled('File', '/etc/pig/conf/pig.properties',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0644,
                              content = 'pigproperties\nline2'
    )
    self.assertResourceCalled('File', '/etc/pig/conf/log4j.properties',
      owner = 'hdfs',
      group = 'hadoop',
      mode = 0644,
      content = 'log4jproperties\nline2'
    )
    self.assertNoMoreResources()

  def test_configure_secured(self):
    self.executeScript("1.3.2/services/PIG/package/scripts/pig_client.py",
                       classname = "PigClient",
                       command = "configure",
                       config_file="secured.json"
    )
    
    self.assertResourceCalled('Directory', '/etc/pig/conf',
      owner = 'hdfs',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/pig/conf/pig-env.sh',
      owner = 'hdfs',
      content = InlineTemplate(self.getConfig()['configurations']['pig-env']['content']),
    )
    self.assertResourceCalled('File', '/etc/pig/conf/pig.properties',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0644,
                              content = 'pigproperties\nline2'
    )
    self.assertResourceCalled('File', '/etc/pig/conf/log4j.properties',
      owner = 'hdfs',
      group = 'hadoop',
      mode = 0644,
      content = 'log4jproperties\nline2'
    )
    self.assertNoMoreResources()
