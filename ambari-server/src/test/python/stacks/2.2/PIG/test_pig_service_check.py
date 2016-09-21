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
from mock.mock import patch, MagicMock

from stacks.utils.RMFTestCase import *

class TestPigServiceCheck(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "PIG/0.12.0.2.0/package"
  STACK_VERSION = "2.2"
  DEFAULT_IMMUTABLE_PATHS = ['/apps/hive/warehouse', '/apps/falcon', '/mr-history/done', '/app-logs', '/tmp']

  @patch("resource_management.libraries.functions.copy_tarball.copy_to_hdfs")
  def test_service_check_secure(self, copy_to_hdfs_mock):
    copy_to_hdfs_mock.return_value = True

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/service_check.py",
                       classname="PigServiceCheck",
                       command="service_check",
                       config_file="pig-service-check-secure.json",
                       stack_version=self.STACK_VERSION,
                       target=RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('HdfsResource', '/user/ambari-qa',
                              immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
                              dfs_type = '',
                              security_enabled = True,
                              hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
                              keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                              kinit_path_local = '/usr/bin/kinit',
                              user = 'hdfs',
                              mode = 0770,
                              owner = 'ambari-qa',
                              action = ['create_on_execute'],
                              hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                              hdfs_site=self.getConfig()['configurations']['hdfs-site'],
                              principal_name = 'hdfs@EXAMPLE.COM',
                              default_fs='hdfs://c6401.ambari.apache.org:8020',
                              hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
                              type = 'directory',
                              )
    self.assertResourceCalled('HdfsResource', '/user/ambari-qa/pigsmoke.out',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = 'hdfs@EXAMPLE.COM',
        user = 'hdfs',
        dfs_type = '',
        owner = 'ambari-qa',
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
        type = 'directory',
        action = ['delete_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
    )
    self.assertResourceCalled('HdfsResource', '/user/ambari-qa/passwd',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        source = '/etc/passwd',
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = 'hdfs@EXAMPLE.COM',
        user = 'hdfs',
        dfs_type = '',
        owner = 'ambari-qa',
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
        type = 'file',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
    )
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = 'hdfs@EXAMPLE.COM',
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
    )
    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/smokeuser.headless.keytab ambari-qa@EXAMPLE.COM;',
        user = 'ambari-qa',
    )
    self.assertResourceCalled("File", "/tmp/pigSmoke.sh",
      content=StaticFile("pigSmoke.sh"),
      mode=0755
    )
    self.assertResourceCalled("Execute", "pig /tmp/pigSmoke.sh",
      path=["/usr/hdp/current/pig-client/bin:/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin"],
      tries=3,
      user="ambari-qa",
      try_sleep=5,
      logoutput=True
    )
    self.assertResourceCalled('ExecuteHadoop', 'fs -test -e /user/ambari-qa/pigsmoke.out',
        bin_dir = '/usr/hdp/current/hadoop-client/bin',
        user = 'ambari-qa',
        conf_dir = '/usr/hdp/current/hadoop-client/conf',
    )
    self.assertResourceCalled('HdfsResource', '/user/ambari-qa/pigsmoke.out',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = 'hdfs@EXAMPLE.COM',
        user = 'hdfs',
        dfs_type = '',
        owner = 'ambari-qa',
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
        type = 'directory',
        action = ['delete_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
    )
    self.assertResourceCalled('HdfsResource', '/user/ambari-qa/passwd',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        source = '/etc/passwd',
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = 'hdfs@EXAMPLE.COM',
        user = 'hdfs',
        dfs_type = '',
        owner = 'ambari-qa',
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
        type = 'file',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
    )

    copy_to_hdfs_mock.assert_called_with("tez", "hadoop", "hdfs", skip=False)
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = 'hdfs@EXAMPLE.COM',
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
    )

    self.assertResourceCalled("Execute", "pig -x tez /tmp/pigSmoke.sh",
      tries=3,
      try_sleep=5,
      path=["/usr/hdp/current/pig-client/bin:/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin"],
      user="ambari-qa",
      logoutput=True
    )

    self.assertResourceCalled('ExecuteHadoop', 'fs -test -e /user/ambari-qa/pigsmoke.out',
        bin_dir = '/usr/hdp/current/hadoop-client/bin',
        user = 'ambari-qa',
        conf_dir = '/usr/hdp/current/hadoop-client/conf',
    )
    self.assertNoMoreResources()

