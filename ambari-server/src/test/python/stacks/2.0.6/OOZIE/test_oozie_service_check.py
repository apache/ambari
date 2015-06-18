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
import json

@patch("platform.linux_distribution", new = MagicMock(return_value="Linux"))
class TestOozieServiceCheck(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "OOZIE/4.0.0.2.0/package"
  STACK_VERSION = "2.0.6"

  @patch("resource_management.core.shell.call")
  @patch("glob.glob")
  def test_service_check(self, glob_mock, call_mock):
    glob_mock.return_value = ["examples-dir", "b"]

    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version
    json_content['hostLevelParams']['stack_name'] = 'HDP'
    json_content['hostLevelParams']['stack_version'] = '2.2'

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/service_check.py",
                       classname = "OozieServiceCheck",
                       command = "service_check",
                       config_dict = json_content,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = [(0, None), (0, None)],
                       mocks_dict = mocks_dict)

    self.maxDiff = None

    self.assertResourceCalled('XmlConfig',
      "yarn-site.xml",
      owner = "hdfs",
      group = "hadoop",
      mode = 0644,
      conf_dir="/usr/hdp/current/hadoop-client/conf",
      configurations = json_content['configurations']['yarn-site'])

    self.assertResourceCalled('File',
      "/tmp/oozieSmoke2.sh",
      content = StaticFile("oozieSmoke2.sh"),
      mode = 0755)

    self.assertResourceCalled('File',
      "/tmp/prepareOozieHdfsDirectories.sh",
      content = StaticFile("prepareOozieHdfsDirectories.sh"),
      mode = 0755)

    self.assertResourceCalled('Execute',
      "/tmp/prepareOozieHdfsDirectories.sh /usr/hdp/current/oozie-client/conf examples-dir /usr/hdp/current/hadoop-client/conf ",
      tries = 3,
      try_sleep = 5,
      logoutput = True)

    # far enough
