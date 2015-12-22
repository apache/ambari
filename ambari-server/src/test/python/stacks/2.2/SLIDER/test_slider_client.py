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
import json
from stacks.utils.RMFTestCase import *


class TestSliderClient(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "SLIDER/0.60.0.2.2/package"
  STACK_VERSION = "2.2"

  def test_configure_default(self):
    self.maxDiff = None
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/slider_client.py",
                       classname="SliderClient",
                       command="configure",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Directory',
                              '/usr/hdp/current/slider-client/conf',
                              recursive=True
    )

    self.assertResourceCalled('XmlConfig',
                              'slider-client.xml',
                              conf_dir='/usr/hdp/current/slider-client/conf',
                              configurations=self.getConfig()['configurations']['slider-client'],
                              mode=0644
    )

    self.assertResourceCalled('File', '/usr/hdp/current/slider-client/conf/slider-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['slider-env']['content']),
                              mode = 0755,
                              )

    self.assertResourceCalled('Directory',
                              '/usr/hdp/current/storm-slider-client/conf',
                              recursive=True
    )

    self.assertResourceCalled('File', '/usr/hdp/current/storm-slider-client/conf/storm-slider-env.sh',
                              content=Template('storm-slider-env.sh.j2'),
                              mode = 0755,
                              )

    self.assertResourceCalled('File',
                              '/usr/hdp/current/slider-client/conf/log4j.properties',
                              mode=0644,
                              content='log4jproperties\nline2'
    )
    self.assertResourceCalled('File', '/usr/hdp/current/slider-client/lib/slider.tar.gz',
        owner = 'hdfs',
        group = 'hadoop',
    )

    self.assertNoMoreResources()


  def test_svc_check_secured(self):
    self.maxDiff = None
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/service_check.py",
                       classname="SliderServiceCheck",
                       command="service_check",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/smokeuser.headless.keytab ambari-qa@EXAMPLE.COM; /usr/hdp/current/slider-client/bin/slider list',
        logoutput = True,
        tries = 3,
        user = 'ambari-qa',
        try_sleep = 5,
    )
    self.assertNoMoreResources()

  def test_svc_check_default(self):
    self.maxDiff = None
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/service_check.py",
                       classname="SliderServiceCheck",
                       command="service_check",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Execute', ' /usr/hdp/current/slider-client/bin/slider list',
        logoutput = True,
        tries = 3,
        user = 'ambari-qa',
        try_sleep = 5,
    )
    self.assertNoMoreResources()


  def test_pre_upgrade_restart(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/slider_client.py",
                       classname = "SliderClient",
                       command = "pre_upgrade_restart",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertResourceCalled("Execute", ('hdp-select', 'set', 'slider-client', '2.2.1.0-2067'), sudo=True)
    self.assertResourceCalled("Execute", ('hdp-select', 'set', 'hadoop-client', '2.2.1.0-2067'), sudo=True)
    self.assertNoMoreResources()


  def test_pre_upgrade_restart_23(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.2/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    json_content['commandParams']['version'] = '2.3.0.0-1234'
    
    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/slider_client.py",
                       classname = "SliderClient",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = [(0, None, ''), (0, None, ''), (0, None, ''), (0, None, '')],
                       mocks_dict = mocks_dict)

    self.assertResourceCalledIgnoreEarlier("Execute", ('hdp-select', 'set', 'slider-client', '2.3.0.0-1234'), sudo=True)
    self.assertResourceCalledIgnoreEarlier("Execute", ('hdp-select', 'set', 'hadoop-client', '2.3.0.0-1234'), sudo=True)
    self.assertNoMoreResources()

    self.assertEquals(2, mocks_dict['call'].call_count)
    self.assertEquals(2, mocks_dict['checked_call'].call_count)
    self.assertEquals(
      ('conf-select', 'set-conf-dir', '--package', 'slider', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['checked_call'].call_args_list[0][0][0])
    self.assertEquals(
      ('conf-select', 'create-conf-dir', '--package', 'slider', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['call'].call_args_list[0][0][0])
    self.assertEquals(
      ('conf-select', 'set-conf-dir', '--package', 'hadoop', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['checked_call'].call_args_list[1][0][0])
    self.assertEquals(
      ('conf-select', 'create-conf-dir', '--package', 'hadoop', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['call'].call_args_list[1][0][0])
