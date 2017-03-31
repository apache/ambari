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


class TestAtlasCheck(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "ATLAS/0.1.0.2.3/package"
  STACK_VERSION = "2.3"

  def test_service_check(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/service_check.py",
                       classname="AtlasServiceCheck",
                       command="service_check",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
                       )

    self.assertResourceCalled('Execute', 'curl -k -s -o /dev/null -w "%{http_code}" http://c6401.ambari.apache.org:21000/',
                              user = 'ambari-qa',
                              tries = 5,
                              try_sleep = 10)

    self.assertNoMoreResources()

  def test_service_check_secure(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/service_check.py",
                       classname="AtlasServiceCheck",
                       command="service_check",
                       config_file="secure.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
                       )

    self.assertResourceCalled('Execute',
                              '/usr/bin/kinit -kt /etc/security/keytabs/smokeuser.headless.keytab ambari-qa@EXAMPLE.COM',
                              user = 'ambari-qa')

    self.assertResourceCalled('Execute', 'curl -k --negotiate -u : -b ~/cookiejar.txt -c ~/cookiejar.txt -s -o /dev/null -w "%{http_code}" https://c6401.ambari.apache.org:21443/',
                              user = 'ambari-qa',
                              tries = 5,
                              try_sleep = 10)

    self.assertNoMoreResources()

