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

import sys
import os
from stacks.utils.RMFTestCase import RMFTestCase
from mock.mock import patch


class TestRangerAdmin(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "RANGER/0.4.0/package"
  STACK_VERSION = "2.2"

  def setUp(self):
    sys.path.insert(0, os.path.join(os.getcwd(), "../../main/resources/common-services", self.COMMON_SERVICES_PACKAGE_DIR, "scripts"))

  @patch("setup_ranger.setup_ranger")
  def test_upgrade(self, setup_ranger_mock):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_admin.py",
                       classname = "RangerAdmin",
                       command = "restart",
                       config_file="ranger-admin-upgrade.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertTrue(setup_ranger_mock.called)
    self.assertResourceCalled("Execute", "/usr/bin/ranger-admin-stop", user="ranger")
    self.assertResourceCalled("Execute", "hdp-select set ranger-admin 2.2.2.0-2399")