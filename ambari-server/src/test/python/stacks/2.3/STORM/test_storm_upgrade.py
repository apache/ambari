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
from mock.mock import MagicMock, call, patch
from stacks.utils.RMFTestCase import *


class TestStormUpgrade(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "STORM/0.9.1.2.1/package"
  STACK_VERSION = "2.3"

  def test_delete_zookeeper_data(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.3/configs/storm_default.json"

    with open(config_file, "r") as f:
      json_content = json.load(f)

    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/storm_upgrade.py",
      classname = "StormUpgrade",
      command = "delete_storm_zookeeper_data",
      config_dict = json_content,
      hdp_stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES,
      call_mocks = [],
      mocks_dict = mocks_dict)

    self.assertResourceCalled("Execute",
      "/usr/hdp/2.3.0.0-1234/zookeeper/bin/zkCli.sh -server c6401.ambari.apache.org:2181 rmr /storm",
      environment = {'JAVA_HOME': '/usr/jdk64/jdk1.8.0_40',},
      logoutput = True,
      tries = 1,
      user = 'storm')


  def test_delete_zookeeper_data_jaas(self):
    """
    Tests that the JAAS file is exported when calling zkCli.sh rmr
    :return:
    """
    config_file = self.get_src_folder()+"/test/python/stacks/2.3/configs/storm_default_secure.json"

    with open(config_file, "r") as f:
      json_content = json.load(f)

    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version
    json_content['configurations']['cluster-env']['security_enabled'] = True

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/storm_upgrade.py",
      classname = "StormUpgrade",
      command = "delete_storm_zookeeper_data",
      config_dict = json_content,
      hdp_stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES,
      call_mocks = [],
      mocks_dict = mocks_dict)

    environment = {'JAVA_HOME': '/usr/jdk64/jdk1.8.0_40',
      'JVMFLAGS': '-Djava.security.auth.login.config=/usr/hdp/current/storm-client/conf/storm_jaas.conf'}

    self.assertResourceCalled("Execute",
      "/usr/hdp/2.3.0.0-1234/zookeeper/bin/zkCli.sh -server c6401.ambari.apache.org:2181 rmr /storm",
      environment = environment,
      logoutput = True,
      tries = 1,
      user = 'storm')


