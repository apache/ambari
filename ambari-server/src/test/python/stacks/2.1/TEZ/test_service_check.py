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


class TestTezServiceCheck(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "TEZ/0.4.0.2.1/package"
  STACK_VERSION = "2.1"

  def test_service_check(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/service_check.py",
                       classname="TezServiceCheck",
                       command="service_check",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('File', '/tmp/sample-tez-test',
        content = 'foo\nbar\nfoo\nbar\nfoo',
        mode = 0755,
    )
    self.assertResourceCalled('HdfsResource', '/tmp/tezsmokeinput',
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        owner = 'ambari-qa',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
    )
    self.assertResourceCalled('HdfsResource', '/tmp/tezsmokeinput/sample-tez-test',
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        source = '/tmp/sample-tez-test',
        user = 'hdfs',
        owner = 'ambari-qa',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'file',
        action = ['create_on_execute'], hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
    )
    self.assertResourceCalled('HdfsResource', None,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        action = ['execute'], hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
        hadoop_conf_dir = '/etc/hadoop/conf',
    )
    self.assertResourceCalled('ExecuteHadoop', 'jar /usr/lib/tez/tez-mapreduce-examples*.jar orderedwordcount /tmp/tezsmokeinput/sample-tez-test /tmp/tezsmokeoutput/',
        try_sleep = 5,
        tries = 3,
        bin_dir = '/usr/bin',
        user = 'ambari-qa',
        conf_dir = '/etc/hadoop/conf',
    )
    self.assertResourceCalled('ExecuteHadoop', 'fs -test -e /tmp/tezsmokeoutput/_SUCCESS',
        try_sleep = 6,
        tries = 10,
        bin_dir = '/usr/bin',
        user = 'ambari-qa',
        conf_dir = '/etc/hadoop/conf',
    )
    self.assertNoMoreResources()


