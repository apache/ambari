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

class TestMahoutClient(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "MAHOUT/1.0.0.2.3/package"
  STACK_VERSION = "2.3"

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/service_check.py",
                       classname = "MahoutServiceCheck",
                       command = "service_check",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('ExecuteHadoop', 'fs -rm -r -f /user/ambari-qa/mahoutsmokeoutput /user/ambari-qa/mahoutsmokeinput',
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              try_sleep = 5,
                              kinit_path_local = '/usr/bin/kinit',
                              tries = 3,
                              user = 'ambari-qa',
                              bin_dir = '/usr/hdp/current/hadoop-client/bin',
                              principal = UnknownConfigurationMock(),
                              )
    self.assertResourceCalled('ExecuteHadoop', 'fs -mkdir /user/ambari-qa/mahoutsmokeinput',
                              try_sleep = 5,
                              tries = 3,
                              bin_dir = '/usr/hdp/current/hadoop-client/bin',
                              user = 'ambari-qa',
                              conf_dir = '/etc/hadoop/conf',
                              )
    self.assertResourceCalled('File', '/tmp/sample-mahout-test.txt',
                              content = 'Test text which will be converted to sequence file.',
                              mode = 0755,
                              )
    self.assertResourceCalled('ExecuteHadoop', 'fs -put /tmp/sample-mahout-test.txt /user/ambari-qa/mahoutsmokeinput/',
                              try_sleep = 5,
                              tries = 3,
                              bin_dir = '/usr/hdp/current/hadoop-client/bin',
                              user = 'ambari-qa',
                              conf_dir = '/etc/hadoop/conf',
                              )
    self.assertResourceCalled('Execute', 'mahout seqdirectory --input /user/ambari-qa/mahoutsmokeinput/'
                                         'sample-mahout-test.txt --output /user/ambari-qa/mahoutsmokeoutput/ '
                                         '--charset utf-8',
                              environment = {'HADOOP_CONF_DIR': '/etc/hadoop/conf',
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
                              conf_dir = '/etc/hadoop/conf',
                              )
    self.assertNoMoreResources()


