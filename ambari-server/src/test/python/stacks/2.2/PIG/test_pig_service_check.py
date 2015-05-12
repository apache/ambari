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

  def test_service_check_secure(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/service_check.py",
                       classname="PigServiceCheck",
                       command="service_check",
                       config_file="pig-service-check-secure.json",
                       hdp_stack_version=self.STACK_VERSION,
                       target=RMFTestCase.TARGET_COMMON_SERVICES
    )
    
    self.assertResourceCalled('HdfsResource', '/user/ambari-qa/pigsmoke.out',
        security_enabled = True,
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        kinit_path_local = '/usr/bin/kinit',
        user = 'ambari-qa',
        action = ['delete_on_execute'],
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
        type = 'directory',
    )
    self.assertResourceCalled('HdfsResource', '/user/ambari-qa/passwd',
        security_enabled = True,
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        kinit_path_local = '/usr/bin/kinit',
        source = '/etc/passwd',
        user = 'ambari-qa',
        action = ['create_on_execute'],
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
        type = 'file',
    )
    self.assertResourceCalled('HdfsResource', None,
        security_enabled = True,
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs@EXAMPLE.COM',
        action = ['execute'],
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
    )
    self.assertResourceCalled("File", "/tmp/pigSmoke.sh",
      content=StaticFile("pigSmoke.sh"),
      mode=0755
    )
    
    

    self.assertResourceCalled("Execute", "pig /tmp/pigSmoke.sh",
      path=["/usr/hdp/current/pig-client/bin:/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin"],
      tries=3,
      user="ambari-qa",
      try_sleep=5
    )

    self.assertResourceCalled('ExecuteHadoop', 'fs -test -e /user/ambari-qa/pigsmoke.out',
        bin_dir = '/usr/hdp/current/hadoop-client/bin',
        user = 'ambari-qa',
        conf_dir = '/usr/hdp/current/hadoop-client/conf',
    )
    self.assertResourceCalled('HdfsResource', '/user/ambari-qa/pigsmoke.out',
        security_enabled = True,
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        kinit_path_local = '/usr/bin/kinit',
        user = 'ambari-qa',
        action = ['delete_on_execute'],
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
        type = 'directory',
    )
    self.assertResourceCalled('HdfsResource', '/user/ambari-qa/passwd',
        security_enabled = True,
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        kinit_path_local = '/usr/bin/kinit',
        source = '/etc/passwd',
        user = 'ambari-qa',
        action = ['create_on_execute'],
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
        type = 'file',
    )
    self.assertResourceCalled('HdfsResource', 'hdfs:///hdp/apps/2.2.0.0/tez//tez.tar.gz',
        security_enabled = True,
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        source = '/usr/hdp/current/tez-client/lib/tez.tar.gz',
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs@EXAMPLE.COM',
        owner = 'hdfs',
        group = 'hadoop',
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        type = 'file',
        action = ['create_on_execute'],
    )
    self.assertResourceCalled('HdfsResource', None,
        security_enabled = True,
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs@EXAMPLE.COM',
        action = ['execute'],
        hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
    )

    self.assertResourceCalled("Execute", "/usr/bin/kinit -kt /etc/security/keytabs/smokeuser.headless.keytab ambari-qa@EXAMPLE.COM;",
      user="ambari-qa")

    self.assertResourceCalled("Execute", "pig -x tez /tmp/pigSmoke.sh",
      tries=3,
      try_sleep=5,
      path=["/usr/hdp/current/pig-client/bin:/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin"],
      user="ambari-qa"
    )

    self.assertResourceCalled('ExecuteHadoop', 'fs -test -e /user/ambari-qa/pigsmoke.out',
        bin_dir = '/usr/hdp/current/hadoop-client/bin',
        user = 'ambari-qa',
        conf_dir = '/usr/hdp/current/hadoop-client/conf',
    )
    self.assertNoMoreResources()

