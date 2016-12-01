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
import re
from mock.mock import MagicMock, call, patch
from stacks.utils.RMFTestCase import *

curl_returns = [(0, "{\"clusterInfo\":{\"id\": \"1471586271500\",\"haState\": \"ACTIVE\"}}",''),
                         (0, "{\"app\":{\"state\": \"FINISHED\",\"finalStatus\":\"SUCCEEDED\"}}",'')]

@patch("platform.linux_distribution", new = MagicMock(return_value="Linux"))
@patch("sys.executable", new = '/usr/bin/python2.6')
class TestServiceCheck(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "YARN/2.1.0.2.0/package"
  STACK_VERSION = "2.0.6"
  DEFAULT_IMMUTABLE_PATHS = ['/apps/hive/warehouse', '/apps/falcon', '/mr-history/done', '/app-logs', '/tmp']

  @patch("re.search")
  def test_service_check_default(self, re_search_mock):
    m = MagicMock()
    re_search_mock.return_value = m
    m.group.return_value = "http://c6402.ambari.apache.org:8088/proxy/application_1429699682952_0010/"

    with patch("resource_management.libraries.functions.get_user_call_output.get_user_call_output", side_effect = curl_returns) as mock_curl:
        self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/service_check.py",
                           classname="ServiceCheck",
                           command="service_check",
                           config_file="default.json",
                           stack_version = self.STACK_VERSION,
                           target = RMFTestCase.TARGET_COMMON_SERVICES,
                           checked_call_mocks = [(0, "some test text, appTrackingUrl=http:"
                                                  "//c6402.ambari.apache.org:8088/proxy/application_1429885383763_0001/, some test text")]
                           )
        self.assertResourceCalled('HdfsResource', '/user/ambari-qa',
                                  immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
                                  security_enabled = False,
                                  hadoop_bin_dir = '/usr/bin',
                                  keytab = UnknownConfigurationMock(),
                                  kinit_path_local = '/usr/bin/kinit',
                                  user = 'hdfs',
                                  dfs_type = '',
                                  mode = 0770,
                                  owner = 'ambari-qa',
                                  action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
                                  hadoop_conf_dir = '/etc/hadoop/conf',
                                  type = 'directory',
                                  )
        self.assertCurlCallForwardsCredentialsOnRedirect(mock_curl_call = mock_curl)
        self.assertNoMoreResources()

  @patch("re.search")
  def test_service_check_secured(self, re_search_mock):
    m = MagicMock()
    re_search_mock.return_value = m
    m.group.return_value = "http://c6402.ambari.apache.org:8088/proxy/application_1429699682952_0010/"

    with patch("resource_management.libraries.functions.get_user_call_output.get_user_call_output", side_effect = curl_returns) as mock_curl:
        self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/service_check.py",
                           classname="ServiceCheck",
                           command="service_check",
                           config_file="secured.json",
                           stack_version = self.STACK_VERSION,
                           target = RMFTestCase.TARGET_COMMON_SERVICES,
                           checked_call_mocks = [(0, "some test text, appTrackingUrl=http:"
                                                  "//c6402.ambari.apache.org:8088/proxy/application_1429885383763_0001/, some test text")]
                           )
        self.assertResourceCalled('HdfsResource', '/user/ambari-qa',
                                  immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
                                  security_enabled = True,
                                  hadoop_bin_dir = '/usr/bin',
                                  keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                                  kinit_path_local = '/usr/bin/kinit',
                                  user = 'hdfs',
                                  dfs_type = '',
                                  mode = 0770,
                                  owner = 'ambari-qa',
                                  action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='hdfs', default_fs='hdfs://c6401.ambari.apache.org:8020',
                                  hadoop_conf_dir = '/etc/hadoop/conf',
                                  type = 'directory',
                                  )
        self.assertCurlCallForwardsCredentialsOnRedirect(mock_curl_call = mock_curl)
        self.assertNoMoreResources()

  def assertCurlCallForwardsCredentialsOnRedirect(self, mock_curl_call):
    self.assertIn('--location-trusted', mock_curl_call.call_args[0][0])
