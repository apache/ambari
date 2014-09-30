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
from test_storm_base import TestStormBase

class TestStormNimbus(TestStormBase):

  def test_configure_default(self):
    self.executeScript("2.1/services/STORM/package/scripts/nimbus_prod.py",
                       classname = "Nimbus",
                       command = "configure",
                       config_file="default.json"
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript("2.1/services/STORM/package/scripts/nimbus_prod.py",
                       classname = "Nimbus",
                       command = "start",
                       config_file="default.json"
    )

    self.assert_configure_default()
    self.assertResourceCalled('Execute', 'supervisorctl start storm-nimbus',
                        wait_for_finish = False,
    )

    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript("2.1/services/STORM/package/scripts/nimbus_prod.py",
                       classname = "Nimbus",
                       command = "stop",
                       config_file="default.json"
    )
    self.assertResourceCalled('Execute', 'supervisorctl stop storm-nimbus',
                              wait_for_finish = False,
    )
    self.assertNoMoreResources()

  def test_configure_secured(self):
    self.executeScript("2.1/services/STORM/package/scripts/nimbus_prod.py",
                       classname = "Nimbus",
                       command = "configure",
                       config_file="secured.json"
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()

  def test_start_secured(self):
    self.executeScript("2.1/services/STORM/package/scripts/nimbus_prod.py",
                       classname = "Nimbus",
                       command = "start",
                       config_file="secured.json"
    )

    self.assert_configure_secured()
    self.assertResourceCalled('Execute', 'supervisorctl start storm-nimbus',
                        wait_for_finish = False,
    )

    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript("2.1/services/STORM/package/scripts/nimbus_prod.py",
                       classname = "Nimbus",
                       command = "stop",
                       config_file="secured.json"
    )
    self.assertResourceCalled('Execute', 'supervisorctl stop storm-nimbus',
                              wait_for_finish = False,
    )
    self.assertNoMoreResources()