"""
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
"""

import os.path
import sys
from unittest import TestCase

sys.path.append(os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../main/resources/scripts'))
from mpack_advisor_wrapper import main

class TestMpackAdvisor(TestCase):

  def setUp(self):
    self.testDirectory = os.path.dirname(os.path.abspath(__file__))
    self.testServicesJson = os.path.join(self.testDirectory, 'mpacks/mpack_advisor_data/services.json')
    self.testHostsJson = os.path.join(self.testDirectory, 'mpacks/mpack_advisor_data/hosts.json')

  def test_get_component_layout(self):
    argv = ['dummy', 'recommend-component-layout', self.testHostsJson, self.testServicesJson]
    main(argv)
    resultJson = os.path.join(self.testDirectory, 'mpacks/mpack_advisor_data/component-layout.json')
    self.assertTrue(os.path.isfile(resultJson))

  def test_validate_component_layout(self):
    argv = ['dummy', 'validate-component-layout', self.testHostsJson, self.testServicesJson]
    main(argv)
    resultJson = os.path.join(self.testDirectory, 'mpacks/mpack_advisor_data/component-layout-validation.json')
    self.assertTrue(os.path.isfile(resultJson))
