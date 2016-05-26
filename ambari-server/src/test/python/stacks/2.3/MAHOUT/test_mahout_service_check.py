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

from only_for_platform import not_for_platform, PLATFORM_WINDOWS


@not_for_platform(PLATFORM_WINDOWS)
class TestMahoutClient(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "MAHOUT/1.0.0.2.3/package"
  STACK_VERSION = "2.3"
  DEFAULT_IMMUTABLE_PATHS = ['/apps/hive/warehouse', '/apps/falcon', '/mr-history/done', '/app-logs', '/tmp']

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/service_check.py",
                       classname = "MahoutServiceCheck",
                       command = "service_check",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('File', '/tmp/sample-mahout-test.txt',
        content = 'Test text which will be converted to sequence file.',
        mode = 0755,
    )
    self.maxDiff=None
    self.assertResourceCalled('HdfsResource', '/user/ambari-qa',
                              immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
                              security_enabled = False,
                              hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
                              keytab = UnknownConfigurationMock(),
                              kinit_path_local = '/usr/bin/kinit',
                              user = 'hdfs',
                              dfs_type = '',
                              mode = 0770,
                              owner = 'ambari-qa',
                              action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
                              hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
                              type = 'directory',
                              )
    self.assertResourceCalled('HdfsResource', '/user/ambari-qa/mahoutsmokeoutput',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        action = ['delete_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
        type = 'directory',
    )
    self.assertResourceCalled('HdfsResource', '/user/ambari-qa/mahoutsmokeinput',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'ambari-qa',
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
    )
    self.assertResourceCalled('HdfsResource', '/user/ambari-qa/mahoutsmokeinput/sample-mahout-test.txt',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        source = '/tmp/sample-mahout-test.txt',
        user = 'hdfs',
        dfs_type = '',
        owner = 'ambari-qa',
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
        type = 'file',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
    )
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
    )
    self.assertResourceCalled('Execute', 'mahout seqdirectory --input /user/ambari-qa/mahoutsmokeinput/'
                                         'sample-mahout-test.txt --output /user/ambari-qa/mahoutsmokeoutput/ '
                                         '--charset utf-8',
                              environment = {'HADOOP_CONF_DIR': '/usr/hdp/current/hadoop-client/conf',
                                             'HADOOP_HOME': '/usr/hdp/current/hadoop-client',
                                             'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45',
                                             'MAHOUT_HOME': '/usr/hdp/current/mahout-client'},
                              path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
                              tries = 3,
                              user = 'ambari-qa',
                              try_sleep = 5,
                              )
    self.assertResourceCalled('ExecuteHadoop', 'fs -test -e /user/ambari-qa/mahoutsmokeoutput/_SUCCESS',
                              try_sleep = 6,
                              tries = 10,
                              bin_dir = '/usr/hdp/current/hadoop-client/bin',
                              user = 'ambari-qa',
                              conf_dir = '/usr/hdp/current/hadoop-client/conf',
                              )
    self.assertNoMoreResources()


