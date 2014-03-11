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


class TestFalconServer(RMFTestCase):

  def test_service_check(self):
    self.executeScript("2.1/services/FALCON/package/scripts/service_check.py",
                       classname="FalconServiceCheck",
                       command="service_check",
                       config_file="default.json"
    )
    self.assertResourceCalled('Execute', 'env '
                                         'JAVA_HOME=/usr/jdk64/jdk1.7.0_45 FALCON_LOG_DIR=/var/log/falcon '
                                         'FALCON_PID_DIR=/var/run/falcon '
                                         'FALCON_DATA_DIR=/hadoop/falcon/activemq '
                                         '/usr/lib/falcon/bin/falcon admin -version',
                              logoutput = True,
                              tries = 3,
                              user = 'ambari-qa',
                              try_sleep = 20,)
    self.assertNoMoreResources()
  def test_service_check_secured(self):
    self.executeScript("2.1/services/FALCON/package/scripts/service_check.py",
                       classname="FalconServiceCheck",
                       command="service_check",
                       config_file="secured.json"
    )
    self.assertResourceCalled('Execute','/usr/bin/kinit -kt /etc/security/keytabs/spnego.service.keytab HTTP/c6401.ambari.apache.org@EXAMPLE.COM',
                              user='ambari-qa'
    )
    self.assertResourceCalled('Execute', 'env '
                                         'JAVA_HOME=/usr/jdk64/jdk1.7.0_45 FALCON_LOG_DIR=/var/log/falcon '
                                         'FALCON_PID_DIR=/var/run/falcon '
                                         'FALCON_DATA_DIR=/hadoop/falcon/activemq '
                                         '/usr/lib/falcon/bin/falcon admin -version',
                              logoutput = True,
                              tries = 3,
                              user = 'ambari-qa',
                              try_sleep = 20,)
    self.assertNoMoreResources()

