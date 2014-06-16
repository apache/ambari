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

class TestMySqlServer(RMFTestCase):

  def test_configure_default(self):
    self.executeScript("2.0.6/services/HIVE/package/scripts/mysql_server.py",
                       classname = "MysqlServer",
                       command = "configure",
                       config_file="default.json"
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript("2.0.6/services/HIVE/package/scripts/mysql_server.py",
                       classname = "MysqlServer",
                       command = "start",
                       config_file="default.json"
    )

    self.assertResourceCalled('Execute', "sed -i 's|^bind-address[ \t]*=.*|bind-address = 0.0.0.0|' /etc/my.cnf",
    )
    self.assertResourceCalled('Execute', 'service mysql start',
                       logoutput = True,
                       not_if = 'service mysql status | grep running'
    )
    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript("2.0.6/services/HIVE/package/scripts/mysql_server.py",
                       classname = "MysqlServer",
                       command = "stop",
                       config_file="default.json"
    )
    self.assertResourceCalled('Execute', 'service mysql stop',
                              logoutput = True,
                              only_if = 'service mysql status | grep running',
    )
    self.assertNoMoreResources()


  def test_configure_secured(self):
    self.executeScript("2.0.6/services/HIVE/package/scripts/mysql_server.py",
                       classname = "MysqlServer",
                       command = "configure",
                       config_file="secured.json"
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()

  def test_start_secured(self):
    self.executeScript("2.0.6/services/HIVE/package/scripts/mysql_server.py",
                       classname = "MysqlServer",
                       command = "start",
                       config_file="secured.json"
    )

    self.assertResourceCalled('Execute', "sed -i 's|^bind-address[ \t]*=.*|bind-address = 0.0.0.0|' /etc/my.cnf",
    )
    self.assertResourceCalled('Execute', 'service mysql start',
                              logoutput = True,
                              not_if = 'service mysql status | grep running'
                              )
    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript("2.0.6/services/HIVE/package/scripts/mysql_server.py",
                       classname = "MysqlServer",
                       command = "stop",
                       config_file="secured.json"
    )
    
    self.assertResourceCalled('Execute', 'service mysql stop',
                              logoutput = True,
                              only_if = 'service mysql status | grep running'
                              )
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Execute', "sed -i 's|^bind-address[ \t]*=.*|bind-address = 0.0.0.0|' /etc/my.cnf",
    )
    self.assertResourceCalled('Execute', 'service mysql start',
      logoutput = True,
      not_if = 'service mysql status | grep running'
    )
    self.assertResourceCalled('File', '/tmp/addMysqlUser.sh',
      content = StaticFile('addMysqlUser.sh'),
      mode = 0755,
    )
    self.assertResourceCalled('Execute', 'bash -x /tmp/addMysqlUser.sh mysql hive \'!`"\'"\'"\' 1\' c6402.ambari.apache.org',
      path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
      tries = 3,
      try_sleep = 5,
    )

  def assert_configure_secured(self):
    self.assertResourceCalled('Execute', "sed -i 's|^bind-address[ \t]*=.*|bind-address = 0.0.0.0|' /etc/my.cnf",
    )
    self.assertResourceCalled('Execute', 'service mysql start',
      logoutput = True,
      not_if = 'service mysql status | grep running'
    )
    self.assertResourceCalled('File', '/tmp/addMysqlUser.sh',
      content = StaticFile('addMysqlUser.sh'),
      mode = 0755,
    )
    self.assertResourceCalled('Execute', 'bash -x /tmp/addMysqlUser.sh mysql hive \'!`"\'"\'"\' 1\' c6402.ambari.apache.org',
      path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
      tries = 3,
      try_sleep = 5,
    )
