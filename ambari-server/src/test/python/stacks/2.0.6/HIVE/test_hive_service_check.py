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
import os
from mock.mock import MagicMock, call, patch
from stacks.utils.RMFTestCase import *
import datetime, sys, socket
import  resource_management.libraries.functions
@patch.object(resource_management.libraries.functions, "get_unique_id_and_date", new = MagicMock(return_value=''))
@patch("socket.socket")
class TestServiceCheck(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "HIVE/0.12.0.2.0/package"
  STACK_VERSION = "2.0.6"

  @patch("sys.exit")
  def test_service_check_default(self, sys_exit_mock, socket_mock):

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/service_check.py",
                        classname="HiveServiceCheck",
                        command="service_check",
                        config_file="default.json",
                        hdp_stack_version = self.STACK_VERSION,
                        target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('File', '/tmp/hcatSmoke.sh',
                        content = StaticFile('hcatSmoke.sh'),
                        mode = 0755,
    )
    self.assertResourceCalled('Execute', 'env JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /tmp/hcatSmoke.sh hcatsmoke prepare',
        logoutput = True,
        path = ['/usr/sbin',
           '/usr/local/bin',
           '/bin',
           '/usr/bin',
           '/bin:/usr/lib/hive/bin:/usr/bin'],
        tries = 3,
        user = 'ambari-qa',
        try_sleep = 5,
    )
    self.assertResourceCalled('ExecuteHadoop', 'fs -test -e /apps/hive/warehouse/hcatsmoke',
        security_enabled = False,
        keytab = UnknownConfigurationMock(),
        conf_dir = '/etc/hadoop/conf',
        logoutput = True,
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        bin_dir = '/bin:/usr/lib/hive/bin:/usr/bin',
    )
    self.assertResourceCalled('Execute', ' /tmp/hcatSmoke.sh hcatsmoke cleanup',
        logoutput = True,
        path = ['/usr/sbin',
           '/usr/local/bin',
           '/bin',
           '/usr/bin',
           '/bin:/usr/lib/hive/bin:/usr/bin'],
        tries = 3,
        user = 'ambari-qa',
        try_sleep = 5,
    )
    self.assertResourceCalled('File', '/tmp/templetonSmoke.sh',
                              content = StaticFile('templetonSmoke.sh'),
                              mode = 0755,
                              )
    self.assertResourceCalled('Execute', '/tmp/templetonSmoke.sh c6402.ambari.apache.org ambari-qa 50111 no_keytab false /usr/bin/kinit no_principal',
                              logoutput = True,
                              path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
                              tries = 3,
                              try_sleep = 5,
                              )
    self.assertNoMoreResources()
    self.assertTrue(socket_mock.called)

  @patch("sys.exit")
  def test_service_check_secured(self, sys_exit_mock, socket_mock):

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/service_check.py",
                        classname="HiveServiceCheck",
                        command="service_check",
                        config_file="secured.json",
                        hdp_stack_version = self.STACK_VERSION,
                        target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/smokeuser.headless.keytab ambari-qa@EXAMPLE.COM; ',
                              user = 'ambari-qa',
                              )
    self.assertResourceCalled('Execute', "! beeline -u 'jdbc:hive2://c6402.ambari.apache.org:10000/;principal=hive/_HOST@EXAMPLE.COM' -e '' 2>&1| awk '{print}'|grep -i -e 'Connection refused' -e 'Invalid URL'",
                              path = ['/bin/', '/usr/bin/', '/usr/lib/hive/bin/', '/usr/sbin/'],
                              user = 'ambari-qa',
                              timeout = 30,
                              )
    self.assertResourceCalled('File', '/tmp/hcatSmoke.sh',
                        content = StaticFile('hcatSmoke.sh'),
                        mode = 0755,
    )
    self.maxDiff = None
    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/smokeuser.headless.keytab ambari-qa; env JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /tmp/hcatSmoke.sh hcatsmoke prepare',
        logoutput = True,
        path = ['/usr/sbin','/usr/local/bin','/bin','/usr/bin', '/bin:/usr/lib/hive/bin:/usr/bin'],
        tries = 3,
        user = 'ambari-qa',
        try_sleep = 5,
    )
    self.assertResourceCalled('ExecuteHadoop', 'fs -test -e /apps/hive/warehouse/hcatsmoke',
        security_enabled = True,
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        conf_dir = '/etc/hadoop/conf',
        logoutput = True,
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        bin_dir = '/bin:/usr/lib/hive/bin:/usr/bin',
        principal = 'hdfs',
    )
    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/smokeuser.headless.keytab ambari-qa;  /tmp/hcatSmoke.sh hcatsmoke cleanup',
        logoutput = True,
        path = ['/usr/sbin',
           '/usr/local/bin',
           '/bin',
           '/usr/bin',
           '/bin:/usr/lib/hive/bin:/usr/bin'],
        tries = 3,
        user = 'ambari-qa',
        try_sleep = 5,
    )
    self.assertResourceCalled('File', '/tmp/templetonSmoke.sh',
                              content = StaticFile('templetonSmoke.sh'),
                              mode = 0755,
                              )
    self.assertResourceCalled('Execute', '/tmp/templetonSmoke.sh c6402.ambari.apache.org ambari-qa 50111 /etc/security/keytabs/smokeuser.headless.keytab true /usr/bin/kinit ambari-qa@EXAMPLE.COM',
                              logoutput = True,
                              path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
                              tries = 3,
                              try_sleep = 5,
                              )
    self.assertNoMoreResources()