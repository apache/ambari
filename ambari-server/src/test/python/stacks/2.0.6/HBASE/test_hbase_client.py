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

@patch("os.path.exists", new = MagicMock(return_value=True))
class TestHBaseClient(RMFTestCase):
  
  def test_configure_secured(self):
    self.executeScript("2.0.6/services/HBASE/package/scripts/hbase_client.py",
                   classname = "HbaseClient",
                   command = "configure",
                   config_file="secured.json"
    )
    
    self.assertResourceCalled('Directory', '/etc/hbase/conf',
      owner = 'hbase',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('XmlConfig', 'hbase-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/etc/hbase/conf',
      configurations = self.getConfig()['configurations']['hbase-site'], # don't hardcode all the properties
    )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/etc/hbase/conf',
      configurations = self.getConfig()['configurations']['hdfs-site'], # don't hardcode all the properties
    )
    self.assertResourceCalled('File', '/etc/hbase/conf/hbase-policy.xml',
      owner = 'hbase',
      group = 'hadoop',
    )
    self.assertResourceCalled('TemplateConfig', '/etc/hbase/conf/hbase-env.sh',
      owner = 'hbase',
      template_tag = None,
    )
    self.assertResourceCalled('TemplateConfig', '/etc/hbase/conf/hadoop-metrics2-hbase.properties',
      owner = 'hbase',
      template_tag = 'GANGLIA-RS',
    )
    self.assertResourceCalled('TemplateConfig', '/etc/hbase/conf/regionservers',
      owner = 'hbase',
      template_tag = None,
    )
    self.assertResourceCalled('TemplateConfig', '/etc/hbase/conf/hbase_client_jaas.conf',
      owner = 'hbase',
      template_tag = None,
    )
    self.assertResourceCalled('PropertiesFile',
                              'log4j.properties',
                              dir='/etc/hbase/conf',
                              properties={'property1': 'value1'},
                              mode=0664,
                              owner='hbase',
                              group='hadoop'
    )
    self.assertNoMoreResources()
    
  def test_configure_default(self):
    self.executeScript("2.0.6/services/HBASE/package/scripts/hbase_client.py",
                   classname = "HbaseClient",
                   command = "configure",
                   config_file="default.json"
    )
    
    self.assertResourceCalled('Directory', '/etc/hbase/conf',
      owner = 'hbase',
      group = 'hadoop',
      recursive = True,
    )    
    self.assertResourceCalled('XmlConfig', 'hbase-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/etc/hbase/conf',
      configurations = self.getConfig()['configurations']['hbase-site'], # don't hardcode all the properties
    )    
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/etc/hbase/conf',
      configurations = self.getConfig()['configurations']['hdfs-site'], # don't hardcode all the properties
    )    
    self.assertResourceCalled('File', '/etc/hbase/conf/hbase-policy.xml',
      owner = 'hbase',
      group = 'hadoop',
    )    
    self.assertResourceCalled('TemplateConfig', '/etc/hbase/conf/hbase-env.sh',
      owner = 'hbase',
      template_tag = None,
    )    
    self.assertResourceCalled('TemplateConfig', '/etc/hbase/conf/hadoop-metrics2-hbase.properties',
      owner = 'hbase',
      template_tag = 'GANGLIA-RS',
    )    
    self.assertResourceCalled('TemplateConfig', '/etc/hbase/conf/regionservers',
      owner = 'hbase',
      template_tag = None,
    )
    self.assertResourceCalled('PropertiesFile',
                              'log4j.properties',
                              dir='/etc/hbase/conf',
                              properties={'property1': 'value1'},
                              mode=0664,
                              owner='hbase',
                              group='hadoop'
    )
    self.assertNoMoreResources()
    

    

