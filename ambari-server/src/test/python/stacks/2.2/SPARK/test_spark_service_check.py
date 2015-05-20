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

@patch("resource_management.libraries.functions.get_hdp_version", new=MagicMock(return_value="2.3.0.0-1597"))
class TestServiceCheck(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "SPARK/1.2.0.2.2/package"
  STACK_VERSION = "2.2"

  def test_service_check_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/service_check.py",
                        classname="SparkServiceCheck",
                        command="service_check",
                        config_file="default.json",
                        hdp_stack_version = self.STACK_VERSION,
                        target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', "curl -s -o /dev/null -w'%{http_code}' --negotiate -u: -k http://localhost:18080 | grep 200",
        tries = 10,
        try_sleep = 3,
        logoutput = True
    )
    self.assertNoMoreResources()
    
    
  def test_service_check_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/service_check.py",
                        classname="SparkServiceCheck",
                        command="service_check",
                        config_file="secured.json",
                        hdp_stack_version = self.STACK_VERSION,
                        target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/spark.service.keytab spark/localhost@EXAMPLE.COM; ',
        user = 'spark',
    )
    self.assertResourceCalled('Execute', "curl -s -o /dev/null -w'%{http_code}' --negotiate -u: -k http://localhost:18080 | grep 200",
        tries = 10,
        try_sleep = 3,
        logoutput = True
    )
    self.assertNoMoreResources()