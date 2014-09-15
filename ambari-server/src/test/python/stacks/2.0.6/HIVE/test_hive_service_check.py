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
@patch("socket.socket", new = MagicMock())
class TestServiceCheck(RMFTestCase):

  @patch("sys.exit")
  def test_service_check_default(self, sys_exit_mock):

    self.executeScript("2.0.6/services/HIVE/package/scripts/service_check.py",
                        classname="HiveServiceCheck",
                        command="service_check",
                        config_file="default.json"
    )
    self.assertResourceCalled('File', '/tmp/hcatSmoke.sh',
                        content = StaticFile('hcatSmoke.sh'),
                        mode = 0755,
    )
    self.assertResourceCalled('Execute', 'env JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /tmp/hcatSmoke.sh hcatsmoke prepare',
                        logoutput = True,
                        path = ['/usr/sbin', '/usr/local/nin', '/bin', '/usr/bin'],
                        tries = 3,
                        user = 'ambari-qa',
                        environment = {'PATH' : os.environ['PATH'] + os.pathsep + "/usr/lib/hive/bin"},
                        try_sleep = 5,
    )
    self.assertResourceCalled('ExecuteHadoop', 'fs -test -e /apps/hive/warehouse/hcatsmoke',
                        logoutput = True,
                        user = 'hdfs',
                        conf_dir = '/etc/hadoop/conf',
                        keytab=UnknownConfigurationMock(),
                        kinit_path_local='/usr/bin/kinit',
                        bin_dir = '/usr/lib/hive/bin',
                        security_enabled=False
    )
    self.assertResourceCalled('Execute', ' /tmp/hcatSmoke.sh hcatsmoke cleanup',
                        logoutput = True,
                        path = ['/usr/sbin', '/usr/local/nin', '/bin', '/usr/bin'],
                        tries = 3,
                        user = 'ambari-qa',
                        environment = {'PATH' : os.environ['PATH'] + os.pathsep + "/usr/lib/hive/bin"},
                        try_sleep = 5,
    )
    self.assertNoMoreResources()

  @patch("sys.exit")
  def test_service_check_secured(self, sys_exit_mock):

    self.executeScript("2.0.6/services/HIVE/package/scripts/service_check.py",
                        classname="HiveServiceCheck",
                        command="service_check",
                        config_file="secured.json"
    )
    self.assertResourceCalled('File', '/tmp/hcatSmoke.sh',
                        content = StaticFile('hcatSmoke.sh'),
                        mode = 0755,
    )
    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/smokeuser.headless.keytab ambari-qa; env JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /tmp/hcatSmoke.sh hcatsmoke prepare',
                        logoutput = True,
                        path = ['/usr/sbin', '/usr/local/nin', '/bin', '/usr/bin'],
                        tries = 3,
                        user = 'ambari-qa',
                        environment = {'PATH' : os.environ['PATH'] + os.pathsep + "/usr/lib/hive/bin"},
                        try_sleep = 5,
    )
    self.assertResourceCalled('ExecuteHadoop', 'fs -test -e /apps/hive/warehouse/hcatsmoke',
                        logoutput = True,
                        user = 'hdfs',
                        conf_dir = '/etc/hadoop/conf',
                        keytab='/etc/security/keytabs/hdfs.headless.keytab',
                        kinit_path_local='/usr/bin/kinit',
                        security_enabled=True,
                        bin_dir = '/usr/lib/hive/bin',
                        principal='hdfs'
    )
    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/smokeuser.headless.keytab ambari-qa;  /tmp/hcatSmoke.sh hcatsmoke cleanup',
                        logoutput = True,
                        path = ['/usr/sbin', '/usr/local/nin', '/bin', '/usr/bin'],
                        tries = 3,
                        user = 'ambari-qa',
                        environment = {'PATH' : os.environ['PATH'] + os.pathsep + "/usr/lib/hive/bin"},
                        try_sleep = 5,
    )
    self.assertNoMoreResources()
