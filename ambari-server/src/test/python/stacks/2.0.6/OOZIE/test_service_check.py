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

@patch("glob.glob", new = MagicMock(return_value="/usr/something/oozie-client/lib"))
@patch("platform.linux_distribution", new = MagicMock(return_value="Linux"))
class TestServiceCheck(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "OOZIE/4.0.0.2.0/package"
  STACK_VERSION = "2.0.6"
  DEFAULT_IMMUTABLE_PATHS = ['/apps/hive/warehouse', '/apps/falcon', '/mr-history/done', '/app-logs', '/tmp']
  
  def test_service_check_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/service_check.py",
                        classname="OozieServiceCheck",
                        command="service_check",
                        config_file="default.json",
                        hdp_stack_version = self.STACK_VERSION,
                        target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('XmlConfig', 'yarn-site.xml',
      owner = 'hdfs',
      group = 'hadoop',
      conf_dir = '/etc/hadoop/conf',
      mode = 0644,
      configurations = self.getConfig()['configurations']['yarn-site'],
    )

    self.assert_service_check()
    self.assertNoMoreResources()
    
  def test_service_check_secured(self):
    self.maxDiff = None
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/service_check.py",
                        classname="OozieServiceCheck",
                        command="service_check",
                        config_file="default.json",
                        hdp_stack_version = self.STACK_VERSION,
                        target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('XmlConfig', 'yarn-site.xml',
      owner = 'hdfs',
      group = 'hadoop',
      conf_dir = '/etc/hadoop/conf',
      mode = 0644,
      configurations = self.getConfig()['configurations']['yarn-site'],
    )

    self.assert_service_check()
    self.assertNoMoreResources()
        
  def assert_service_check(self):
    self.assertResourceCalled('File', '/tmp/oozieSmoke2.sh',
        content = StaticFile('oozieSmoke2.sh'),
        mode = 0755,
    )
    self.assertResourceCalled('File', '/tmp/prepareOozieHdfsDirectories.sh',
        content = StaticFile('prepareOozieHdfsDirectories.sh'),
        mode = 0755,
    )
    self.assertResourceCalled('Execute', '/tmp/prepareOozieHdfsDirectories.sh /etc/oozie/conf / /etc/hadoop/conf ',
        logoutput = True,
        tries = 3,
        try_sleep = 5,
    )
    self.assertResourceCalled('HdfsResource', '/user/ambari-qa/examples',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        action = ['delete_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
    )
    self.assertResourceCalled('HdfsResource', '/user/ambari-qa/examples',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        source = '//examples',
        user = 'hdfs',
        dfs_type = '',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        owner = 'ambari-qa',
        group = 'hadoop'
    )
    self.assertResourceCalled('HdfsResource', '/user/ambari-qa/input-data',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        action = ['delete_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
    )
    self.assertResourceCalled('HdfsResource', '/user/ambari-qa/input-data',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        source = '//examples/input-data',
        user = 'hdfs',
        dfs_type = '',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        owner = 'ambari-qa',
        group = 'hadoop'
    )
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
        hadoop_conf_dir = '/etc/hadoop/conf',
    )
    self.assertResourceCalled('Execute', '/tmp/oozieSmoke2.sh suse /var/lib/oozie /etc/oozie/conf /usr/bin http://c6402.ambari.apache.org:11000/oozie / /etc/hadoop/conf /usr/bin ambari-qa False',
        logoutput = True,
        path = ['/usr/bin:/usr/bin'],
        tries = 3,
        try_sleep = 5,
    )

