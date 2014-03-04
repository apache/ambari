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

class TestPigServiceCheck(RMFTestCase):

  def test_configure_default(self):
    self.executeScript("1.3.2/services/PIG/package/scripts/service_check.py",
                       classname = "PigServiceCheck",
                       command = "service_check",
                       config_file="default.json"
    )
    
    self.assertResourceCalled('ExecuteHadoop', 'dfs -rmr pigsmoke.out passwd; hadoop dfs -put /etc/passwd passwd ',
      try_sleep = 5,
      tries = 3,
      user = 'ambari-qa',
      conf_dir = '/etc/hadoop/conf',
      security_enabled = False,
      keytab = UnknownConfigurationMock(),
      kinit_path_local = '/usr/bin/kinit',
    )
       
    self.assertResourceCalled('File', '/tmp/pigSmoke.sh',
      content = StaticFile('pigSmoke.sh'),
      mode = 0755,
    )
       
    self.assertResourceCalled('Execute', 'pig /tmp/pigSmoke.sh',
      path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
      tries = 3,
      user = 'ambari-qa',
      try_sleep = 5,
    )
       
    self.assertResourceCalled('ExecuteHadoop', 'fs -test -e pigsmoke.out',
      user = 'ambari-qa',
      conf_dir = '/etc/hadoop/conf',
    )
    self.assertNoMoreResources()

  def test_configure_secured(self):
    self.executeScript("1.3.2/services/PIG/package/scripts/service_check.py",
                       classname = "PigServiceCheck",
                       command = "service_check",
                       config_file="secured.json"
    )
    
    self.assertResourceCalled('ExecuteHadoop', 'dfs -rmr pigsmoke.out passwd; hadoop dfs -put /etc/passwd passwd ',
      try_sleep = 5,
      tries = 3,
      user = 'ambari-qa',
      conf_dir = '/etc/hadoop/conf',
      security_enabled = True, 
      keytab = '/etc/security/keytabs/smokeuser.headless.keytab',
      kinit_path_local = '/usr/bin/kinit',
    )
       
    self.assertResourceCalled('File', '/tmp/pigSmoke.sh',
      content = StaticFile('pigSmoke.sh'),
      mode = 0755,
    )
       
    self.assertResourceCalled('Execute', 'pig /tmp/pigSmoke.sh',
      path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
      tries = 3,
      user = 'ambari-qa',
      try_sleep = 5,
    )
       
    self.assertResourceCalled('ExecuteHadoop', 'fs -test -e pigsmoke.out',
      user = 'ambari-qa',
      conf_dir = '/etc/hadoop/conf',
    )
    self.assertNoMoreResources()
