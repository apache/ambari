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

@patch("sys.executable", new = '/usr/bin/python2.6')
class TestServiceCheck(RMFTestCase):

  def test_service_check_default(self):

    self.executeScript("2.0.6/services/YARN/package/scripts/service_check.py",
                          classname="ServiceCheck",
                          command="service_check",
                          config_file="default.json"
    )
    self.assertResourceCalled('File', '/tmp/validateYarnComponentStatus.py',
                          content = StaticFile('validateYarnComponentStatus.py'),
                          mode = 0755,
    )
    self.assertResourceCalled('Execute', '/usr/bin/python2.6 /tmp/validateYarnComponentStatus.py rm -p c6402.ambari.apache.org:8088 -s False',
                          logoutput = True,
                          path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
                          tries = 3,
                          user = 'ambari-qa',
                          try_sleep = 5,
    )
    self.assertResourceCalled('Execute', 'yarn --config /etc/hadoop/conf node -list',
                              path = [os.environ['PATH'] + os.pathsep + "/usr/bin" + os.pathsep + "/usr/lib/hadoop-yarn/bin"],
                              user = 'ambari-qa',
    )
    self.assertNoMoreResources()

  def test_service_check_secured(self):
    self.executeScript("2.0.6/services/YARN/package/scripts/service_check.py",
                          classname="ServiceCheck",
                          command="service_check",
                          config_file="secured.json"
    )
    self.assertResourceCalled('File', '/tmp/validateYarnComponentStatus.py',
                          content = StaticFile('validateYarnComponentStatus.py'),
                          mode = 0755,
    )
    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/smokeuser.headless.keytab ambari-qa; /usr/bin/python2.6 /tmp/validateYarnComponentStatus.py rm -p c6402.ambari.apache.org:8088 -s False',
                          logoutput = True,
                          path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
                          tries = 3,
                          user = 'ambari-qa',
                          try_sleep = 5,
    )
    self.assertResourceCalled('Execute', 'yarn --config /etc/hadoop/conf node -list',
                              path = [os.environ['PATH'] + os.pathsep + "/usr/bin" + os.pathsep + "/usr/lib/hadoop-yarn/bin"],
                          user = 'ambari-qa',
    )
    self.assertNoMoreResources()
