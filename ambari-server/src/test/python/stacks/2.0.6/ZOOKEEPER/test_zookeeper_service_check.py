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

class TestServiceCheck(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "ZOOKEEPER/3.4.5/package"
  STACK_VERSION = "2.0.6"

  def test_service_check_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/service_check.py",
                       classname = "ZookeeperServiceCheck",
                       command = "service_check",
                       config_file = "default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('File', '/tmp/zkSmoke.out',
                              action=['delete']
    )
    self.assertResourceCalled('File', '/tmp/zkSmoke.sh',
                       content = StaticFile('zkSmoke.sh'),
                       mode = 0755,
    )
    self.assertResourceCalled('Execute', '/tmp/zkSmoke.sh /usr/lib/zookeeper/bin/zkCli.sh ambari-qa /etc/zookeeper/conf 2181 False /usr/bin/kinit no_keytab no_principal /tmp/zkSmoke.out',
                       logoutput = True,
                       path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
                       tries = 3,
                       try_sleep = 5,
    )
    self.assertNoMoreResources()

  def test_service_check_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/service_check.py",
                       classname = "ZookeeperServiceCheck",
                       command = "service_check",
                       config_file = "secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('File', '/tmp/zkSmoke.out',
                              action=['delete']
    )
    self.assertResourceCalled('File', '/tmp/zkSmoke.sh',
                       content = StaticFile('zkSmoke.sh'),
                       mode = 0755,
    )
    self.assertResourceCalled('Execute', '/tmp/zkSmoke.sh /usr/lib/zookeeper/bin/zkCli.sh ambari-qa /etc/zookeeper/conf 2181 True /usr/bin/kinit /etc/security/keytabs/smokeuser.headless.keytab ambari-qa@EXAMPLE.COM /tmp/zkSmoke.out',
                       logoutput = True,
                       path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
                       tries = 3,
                       try_sleep = 5,
    )
    self.assertNoMoreResources()

  def test_service_check_22(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/service_check.py",
                       classname = "ZookeeperServiceCheck",
                       command = "service_check",
                       config_file = "zk-service_check_2.2.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('File', '/tmp/zkSmoke.out',
                              action=['delete']
    )
    self.assertResourceCalled('File', '/tmp/zkSmoke.sh',
                       content = StaticFile('zkSmoke.sh'),
                       mode = 0755,
    )
    self.assertResourceCalled('Execute', '/tmp/zkSmoke.sh /usr/hdp/current/zookeeper-client/bin/zkCli.sh ambari-qa /usr/hdp/current/zookeeper-client/conf 2181 False /usr/bin/kinit no_keytab no_principal /tmp/zkSmoke.out',
                       logoutput = True,
                       path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
                       tries = 3,
                       try_sleep = 5,
    )
    self.assertNoMoreResources()
