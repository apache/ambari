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


class TestRangerUserSync(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "RANGER/0.4.0/package"
  STACK_VERSION = "2.2"

  def setUp(self):
    sys.path.insert(0, os.path.join(os.getcwd(),
      "../../main/resources/common-services", self.COMMON_SERVICES_PACKAGE_DIR,
      "scripts"))

  @patch("setup_ranger.setup_usersync")
  def test_upgrade(self, setup_usersync_mock):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_usersync.py",
                       classname = "RangerUsersync",
                       command = "restart",
                       config_file="ranger-usersync-upgrade.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertTrue(setup_usersync_mock.called)
    self.assertResourceCalled("Execute", "/usr/bin/ranger-usersync-stop")
    self.assertResourceCalled("Execute", "hdp-select set ranger-usersync 2.2.2.0-2399")