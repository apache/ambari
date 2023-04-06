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


class TestStormBase(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "STORM/0.9.1/package"
  STACK_VERSION = "2.3"

  def assert_configure_default(self, confDir="/etc/storm/conf"):
    import params
    self.assertResourceCalled('Directory', '/var/log/storm',
      owner = 'storm',
      group = 'hadoop',
      mode = 0o777,
      create_parents = True,
      cd_access='a',
    )
    self.assertResourceCalled('Directory', '/var/run/storm',
      owner = 'storm',
      group = 'hadoop',
      create_parents = True,
      cd_access='a'
    )
    self.assertResourceCalled('Directory', '/hadoop/storm',
      owner = 'storm',
      group = 'hadoop',
      create_parents = True,
      cd_access='a'
    )
    self.assertResourceCalled('Directory', confDir,
      group = 'hadoop',
      create_parents = True,
      cd_access='a'
    )
    self.assertResourceCalled('File', confDir + '/config.yaml',
      owner = 'storm',
      content = Template('config.yaml.j2'),
      group = 'hadoop',
    )
    
    storm_yarn_content = self.call_storm_template_and_assert(confDir=confDir)
    
    self.assertTrue(storm_yarn_content.find('_JAAS_PLACEHOLDER') == -1, 'Placeholder have to be substituted')

    self.assertResourceCalled('File', confDir + '/storm-env.sh',
                              owner = 'storm',
                              content = InlineTemplate(self.getConfig()['configurations']['storm-env']['content'])
                              )
    return storm_yarn_content

  def assert_configure_secured(self, confDir='/etc/storm/conf'):
    import params
    self.assertResourceCalled('Directory', '/var/log/storm',
      owner = 'storm',
      group = 'hadoop',
      mode = 0o777,
      create_parents = True,
      cd_access='a',
    )
    self.assertResourceCalled('Directory', '/var/run/storm',
      owner = 'storm',
      group = 'hadoop',
      create_parents = True,
      cd_access='a'
    )
    self.assertResourceCalled('Directory', '/hadoop/storm',
      owner = 'storm',
      group = 'hadoop',
      create_parents = True,
      cd_access='a'
    )
    self.assertResourceCalled('Directory', confDir,
      group = 'hadoop',
      create_parents = True,
      cd_access='a'
    )
    self.assertResourceCalled('File', confDir + '/config.yaml',
      owner = 'storm',
      content = Template('config.yaml.j2'),
      group = 'hadoop',
    )
    storm_yarn_content = self.call_storm_template_and_assert(confDir=confDir)
    
    self.assertTrue(storm_yarn_content.find('_JAAS_PLACEHOLDER') == -1, 'Placeholder have to be substituted')
    
    self.assertResourceCalled('File', confDir + '/storm-env.sh',
                              owner = 'storm',
                              content = InlineTemplate(self.getConfig()['configurations']['storm-env']['content'])
                              )
    self.assertResourceCalled('TemplateConfig', confDir + '/storm_jaas.conf',
      owner = 'storm',
      mode = 0o644,
    )
    return storm_yarn_content

  def call_storm_template_and_assert(self, confDir="/etc/storm/conf"):
    import storm_yaml_utils

    with RMFTestCase.env as env:
      storm_yarn_temlate = storm_yaml_utils.yaml_config_template(self.getConfig()['configurations']['storm-site'])

      self.assertResourceCalled('File', confDir + '/storm.yaml',
        owner = 'storm',
        content= storm_yarn_temlate,
        group = 'hadoop'
      )

      return storm_yarn_temlate.get_content()
