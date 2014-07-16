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

class TestSqoop(RMFTestCase):

  def test_configure_default(self):
    self.executeScript("1.3.2/services/SQOOP/package/scripts/sqoop_client.py",
                       classname = "SqoopClient",
                       command = "configure",
                       config_file="default.json"
    )
    self.assertResourceCalled('Link', '/usr/lib/sqoop/lib/mysql-connector-java.jar',
                              to = '/usr/share/java/mysql-connector-java.jar',)
    self.assertResourceCalled('Directory', '/usr/lib/sqoop/conf',
                              owner = 'sqoop',
                              group = 'hadoop',)
    self.assertResourceCalled('File', '/usr/lib/sqoop/conf/sqoop-env.sh',
                              owner = 'sqoop',
                              content = InlineTemplate(self.getConfig()['configurations']['sqoop-env']['content']),)
    self.assertResourceCalled('File', '/usr/lib/sqoop/conf/sqoop-env-template.sh',
                              owner = 'sqoop',
                              group = 'hadoop',)
    self.assertResourceCalled('File', '/usr/lib/sqoop/conf/sqoop-site-template.xml',
                              owner = 'sqoop',
                              group = 'hadoop',)
    self.assertResourceCalled('File', '/usr/lib/sqoop/conf/sqoop-site.xml',
                              owner = 'sqoop',
                              group = 'hadoop',)
    self.assertNoMoreResources()




