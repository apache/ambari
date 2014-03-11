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

class TestTezClient(RMFTestCase):

  def test_configure_default(self):
    self.executeScript("2.1/services/TEZ/package/scripts/tez_client.py",
                       classname = "TezClient",
                       command = "configure",
                       config_file="default.json"
    )

    self.assertResourceCalled('Directory', '/etc/tez/conf',
      owner = 'tez',
      group = 'hadoop',
      recursive = True
    )

    self.assertResourceCalled('XmlConfig', 'tez-site.xml',
      owner = 'tez',
      group = 'hadoop',
      conf_dir = '/etc/tez/conf',
      configurations = self.getConfig()['configurations']['tez-site'],
      mode = 0664
    )

    self.assertResourceCalled('TemplateConfig', '/etc/tez/conf/tez-env.sh',
      owner = 'tez'
    )

    self.assertNoMoreResources()


