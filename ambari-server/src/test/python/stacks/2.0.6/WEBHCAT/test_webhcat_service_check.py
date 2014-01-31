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

  def test_service_check_default(self):

    self.executeScript("2.0.6/services/WEBHCAT/package/scripts/service_check.py",
                       classname="WebHCatServiceCheck",
                       command="service_check",
                       config_file="default.json"
    )
    self.assertResourceCalled('File', '/tmp/templetonSmoke.sh',
                       content = StaticFile('templetonSmoke.sh'),
                       mode = 493,
    )
    self.assertResourceCalled('Execute', 'sh /tmp/templetonSmoke.sh c6402.ambari.apache.org ambari-qa no_keytab False /usr/bin/kinit',
                       logoutput = True,
                       path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
                       tries = 3,
                       try_sleep = 5,
    )
    self.assertNoMoreResources()

  def test_service_check_secured(self):

    self.executeScript("2.0.6/services/WEBHCAT/package/scripts/service_check.py",
                       classname="WebHCatServiceCheck",
                       command="service_check",
                       config_file="secured.json"
    )
    self.assertResourceCalled('File', '/tmp/templetonSmoke.sh',
                       content = StaticFile('templetonSmoke.sh'),
                       mode = 493,
    )
    self.assertResourceCalled('Execute', 'sh /tmp/templetonSmoke.sh c6402.ambari.apache.org ambari-qa /etc/security/keytabs/smokeuser.headless.keytab True /usr/bin/kinit',
                       logoutput = True,
                       path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
                       tries = 3,
                       try_sleep = 5,
    )
    self.assertNoMoreResources()
