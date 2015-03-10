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
import  resource_management.core.source
from test_storm_base import TestStormBase

class TestStormRestApi(TestStormBase):

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/rest_api.py",
                       classname = "StormRestApi",
                       command = "configure",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/rest_api.py",
                       classname = "StormRestApi",
                       command = "start",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_default()


    self.assertResourceCalled('Execute', 'source /etc/storm/conf/storm-env.sh ; export PATH=$JAVA_HOME/bin:$PATH ; java -jar /usr/lib/storm/contrib/storm-rest/`ls /usr/lib/storm/contrib/storm-rest | grep -wE storm-rest-[0-9.-]+\\.jar` server /etc/storm/conf/config.yaml > /var/log/storm/restapi.log 2>&1',
        wait_for_finish = False,
        path = ['/usr/bin'],
        user = 'storm',
        not_if = 'ls /var/run/storm/restapi.pid >/dev/null 2>&1 && ps -p `cat /var/run/storm/restapi.pid` >/dev/null 2>&1',
    )
    self.assertResourceCalled('Execute', "/usr/jdk64/jdk1.7.0_45/bin/jps -l  | grep /usr/lib/storm/contrib/storm-rest/storm-rest-.*\\.jar$ && /usr/jdk64/jdk1.7.0_45/bin/jps -l  | grep /usr/lib/storm/contrib/storm-rest/storm-rest-.*\\.jar$ | awk {'print $1'} > /var/run/storm/restapi.pid",
        logoutput = True,
        path = ['/usr/bin'],
        tries = 6,
        user = 'storm',
        try_sleep = 10,
    )
    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/rest_api.py",
                       classname = "StormRestApi",
                       command = "stop",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh kill `cat /var/run/storm/restapi.pid`',
        not_if = '! (ls /var/run/storm/restapi.pid >/dev/null 2>&1 && ps -p `cat /var/run/storm/restapi.pid` >/dev/null 2>&1)',
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh kill -9 `cat /var/run/storm/restapi.pid`',
        not_if = 'sleep 2; ! (ls /var/run/storm/restapi.pid >/dev/null 2>&1 && ps -p `cat /var/run/storm/restapi.pid` >/dev/null 2>&1) || sleep 20; ! (ls /var/run/storm/restapi.pid >/dev/null 2>&1 && ps -p `cat /var/run/storm/restapi.pid` >/dev/null 2>&1)',
        ignore_failures = True,
    )
    self.assertResourceCalled('File', '/var/run/storm/restapi.pid',
        action = ['delete'],
    )
    self.assertNoMoreResources()

  def test_configure_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/rest_api.py",
                       classname = "StormRestApi",
                       command = "configure",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()

  def test_start_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/rest_api.py",
                       classname = "StormRestApi",
                       command = "start",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_secured()

    self.assertResourceCalled('Execute', 'source /etc/storm/conf/storm-env.sh ; export PATH=$JAVA_HOME/bin:$PATH ; java -jar /usr/lib/storm/contrib/storm-rest/`ls /usr/lib/storm/contrib/storm-rest | grep -wE storm-rest-[0-9.-]+\\.jar` server /etc/storm/conf/config.yaml > /var/log/storm/restapi.log 2>&1',
        wait_for_finish = False,
        path = ['/usr/bin'],
        user = 'storm',
        not_if = 'ls /var/run/storm/restapi.pid >/dev/null 2>&1 && ps -p `cat /var/run/storm/restapi.pid` >/dev/null 2>&1',
    )
    self.assertResourceCalled('Execute', "/usr/jdk64/jdk1.7.0_45/bin/jps -l  | grep /usr/lib/storm/contrib/storm-rest/storm-rest-.*\\.jar$ && /usr/jdk64/jdk1.7.0_45/bin/jps -l  | grep /usr/lib/storm/contrib/storm-rest/storm-rest-.*\\.jar$ | awk {'print $1'} > /var/run/storm/restapi.pid",
        logoutput = True,
        path = ['/usr/bin'],
        tries = 6,
        user = 'storm',
        try_sleep = 10,
    )
    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/rest_api.py",
                       classname = "StormRestApi",
                       command = "stop",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh kill `cat /var/run/storm/restapi.pid`',
        not_if = '! (ls /var/run/storm/restapi.pid >/dev/null 2>&1 && ps -p `cat /var/run/storm/restapi.pid` >/dev/null 2>&1)',
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh kill -9 `cat /var/run/storm/restapi.pid`',
        not_if = 'sleep 2; ! (ls /var/run/storm/restapi.pid >/dev/null 2>&1 && ps -p `cat /var/run/storm/restapi.pid` >/dev/null 2>&1) || sleep 20; ! (ls /var/run/storm/restapi.pid >/dev/null 2>&1 && ps -p `cat /var/run/storm/restapi.pid` >/dev/null 2>&1)',
        ignore_failures = True,
    )
    self.assertResourceCalled('File', '/var/run/storm/restapi.pid',
        action = ['delete'],
    )
    self.assertNoMoreResources()

  def test_pre_rolling_restart(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/rest_api.py",
                       classname = "StormRestApi",
                       command = "pre_rolling_restart",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertResourceCalled("Execute", "hdp-select set storm-client 2.2.1.0-2067")

