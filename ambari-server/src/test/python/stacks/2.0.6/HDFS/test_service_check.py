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
import resource_management.libraries.functions
from mock.mock import MagicMock, call, patch

@patch.object(resource_management.libraries.functions, "get_unique_id_and_date", new = MagicMock(return_value=''))
class TestServiceCheck(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "HDFS/2.1.0.2.0/package"
  STACK_VERSION = "2.0.6"
  DEFAULT_IMMUTABLE_PATHS = ['/apps/hive/warehouse', '/apps/falcon', '/mr-history/done', '/app-logs', '/tmp']

  def test_service_check_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/service_check.py",
                       classname = "HdfsServiceCheck",
                       command = "service_check",
                       config_file = "default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_service_check()
    self.assertNoMoreResources()

  def test_service_check_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/service_check.py",
                       classname = "HdfsServiceCheck",
                       command = "service_check",
                       config_file = "default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_service_check()
    self.assertNoMoreResources()

  def assert_service_check(self):
    #self.assertResourceCalled('ExecuteHadoop', 'dfsadmin -fs hdfs://c6401.ambari.apache.org:8020 -safemode get | grep OFF',
    #    logoutput = True,
    #    tries = 20,
    #    conf_dir = '/etc/hadoop/conf',
    #    try_sleep = 3,
    #    bin_dir = '/usr/bin',
    #    user = 'hdfs',
    #)
    self.assertResourceCalled('HdfsResource', '/tmp',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = None,
        user = 'hdfs',
        dfs_type = '',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        mode = 0777,
    )
    self.assertResourceCalled('HdfsResource', '/tmp/',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = None,
        user = 'hdfs',
        dfs_type = '',
        action = ['delete_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'file',
    )
    self.assertResourceCalled('HdfsResource', '/tmp/',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        source = '/etc/passwd',
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = None,
        user = 'hdfs',
        dfs_type = '',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'file',
    )
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = None,
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hadoop_conf_dir = '/etc/hadoop/conf',
    )
    self.assertNoMoreResources()
