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
import resource_management.core.source
import re


class TestStormBase(RMFTestCase):
  def assert_configure_default(self):
    import params
    self.assertResourceCalled('Directory', '/var/log/storm',
      owner = 'storm',
      group = 'hadoop',
      mode = 0775,
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/run/storm',
      owner = 'storm',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/hadoop/storm',
      owner = 'storm',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/etc/storm/conf',
      owner = 'storm',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('File', '/etc/storm/conf/config.yaml',
      owner = 'storm',
      content = Template('config.yaml.j2'),
      group = 'hadoop',
    )
    
    storm_yarn_content = self.call_storm_template_and_assert()
    
    self.assertTrue(storm_yarn_content.find('_JAAS_PLACEHOLDER') == -1, 'Placeholder have to be substituted')
    
    self.assertResourceCalled('File', '/etc/storm/conf/storm-env.sh',
                              owner = 'storm',
                              content = InlineTemplate(self.getConfig()['configurations']['storm-env']['content'])
                              )
    return storm_yarn_content

  def assert_configure_secured(self):
    import params
    self.assertResourceCalled('Directory', '/var/log/storm',
      owner = 'storm',
      group = 'hadoop',
      mode = 0775,
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/run/storm',
      owner = 'storm',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/hadoop/storm',
      owner = 'storm',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/etc/storm/conf',
      owner = 'storm',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('File', '/etc/storm/conf/config.yaml',
      owner = 'storm',
      content = Template('config.yaml.j2'),
      group = 'hadoop',
    )
    storm_yarn_content = self.call_storm_template_and_assert()
    
    self.assertTrue(storm_yarn_content.find('_JAAS_PLACEHOLDER') == -1, 'Placeholder have to be substituted')
    
    self.assertResourceCalled('File', '/etc/storm/conf/storm-env.sh',
                              owner = 'storm',
                              content = InlineTemplate(self.getConfig()['configurations']['storm-env']['content'])
                              )
    self.assertResourceCalled('TemplateConfig', '/etc/storm/conf/storm_jaas.conf',
      owner = 'storm',
    )
    return storm_yarn_content
    
  def call_storm_template_and_assert(self):
    import yaml_utils
    storm_yarn_template = Template(
                        "storm.yaml.j2", 
                        extra_imports=[yaml_utils.escape_yaml_propetry], 
                        configurations = self.getConfig()['configurations']['storm-site'])
    storm_yarn_content = storm_yarn_template.get_content()
    
    self.assertResourceCalled('File', '/etc/storm/conf/storm.yaml',
      owner = 'storm',
      content= storm_yarn_template, 
      group = 'hadoop'
    )
    return storm_yarn_content
