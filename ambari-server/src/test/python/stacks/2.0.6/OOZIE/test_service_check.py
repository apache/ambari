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

import resource_management.libraries.functions
from mock.mock import MagicMock, call, patch
from stacks.utils.RMFTestCase import *

class TestServiceCheck(RMFTestCase):
  def test_service_check_default(self):
    self.executeScript("2.0.6/services/OOZIE/package/scripts/service_check.py",
                        classname="OozieServiceCheck",
                        command="service_check",
                        config_file="default.json"
    )
    
    self.assert_service_check()
    self.assertNoMoreResources()
    
  def test_service_check_secured(self):
    self.executeScript("2.0.6/services/OOZIE/package/scripts/service_check.py",
                        classname="OozieServiceCheck",
                        command="service_check",
                        config_file="default.json"
    )
    
    self.assert_service_check()
    self.assertNoMoreResources()
        
  def assert_service_check(self):
    self.assertResourceCalled('File', '/tmp/oozieSmoke2.sh',
        content = StaticFile('oozieSmoke2.sh'),
        mode = 0755,
    )
    self.assertResourceCalled('Execute', '/tmp/oozieSmoke2.sh',
        logoutput = True,
        tries = 3,
        command = '/tmp/oozieSmoke2.sh suse /etc/oozie/conf /usr/bin /etc/hadoop/conf /usr/bin ambari-qa False',
        path = ['/usr/bin:/usr/bin'],
        try_sleep = 5,
    )
