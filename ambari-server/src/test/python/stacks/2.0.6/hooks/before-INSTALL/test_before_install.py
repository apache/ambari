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


class TestHookBeforeInstall(RMFTestCase):
  def test_hook_default(self):
    self.executeScript("2.0.6/hooks/before-INSTALL/scripts/hook.py",
                       classname="BeforeConfigureHook",
                       command="hook",
                       config_file="default.json"
    )
    self.assertResourceCalled('Group', 'hadoop', )
    self.assertResourceCalled('Group', 'users', )
    self.assertResourceCalled('Group', 'users', )
    self.assertResourceCalled('User', 'ambari-qa',
                          gid='hadoop',
                          groups=['users'], )
    self.assertResourceCalled('User', 'tez',
                              gid='hadoop',
                              groups=['users'], )
    self.assertResourceCalled('File', '/tmp/changeUid.sh',
                          content=StaticFile('changeToSecureUid.sh'),
                          mode=0555, )
    self.assertResourceCalled('Execute',
                          '/tmp/changeUid.sh ambari-qa /tmp/hadoop-ambari-qa,/tmp/hsperfdata_ambari-qa,/home/ambari-qa,/tmp/ambari-qa,/tmp/sqoop-ambari-qa 2>/dev/null',
                          not_if='test $(id -u ambari-qa) -gt 1000', )
    self.assertResourceCalled('User', 'hbase',
                          gid='hadoop',
                          groups=['hadoop'], )
    self.assertResourceCalled('File', '/tmp/changeUid.sh',
                          content=StaticFile('changeToSecureUid.sh'),
                          mode=0555, )
    self.assertResourceCalled('Execute',
                          '/tmp/changeUid.sh hbase /home/hbase,/tmp/hbase,/usr/bin/hbase,/var/log/hbase,/hadoop/hbase 2>/dev/null',
                          not_if='test $(id -u hbase) -gt 1000', )
    self.assertResourceCalled('Group', 'nagios', )
    self.assertResourceCalled('User', 'nagios',
                          gid='nagios', )
    self.assertResourceCalled('User', 'oozie',
                          gid='hadoop', )
    self.assertResourceCalled('User', 'hcat',
                          gid='hadoop', )
    self.assertResourceCalled('User', 'hcat',
                          gid='hadoop', )
    self.assertResourceCalled('User', 'hive',
                          gid='hadoop', )
    self.assertResourceCalled('User', 'yarn',
                          gid='hadoop', )
    self.assertResourceCalled('Group', 'nobody', )
    self.assertResourceCalled('Group', 'nobody', )
    self.assertResourceCalled('User', 'nobody',
                          gid='hadoop',
                          groups=['nobody'], )
    self.assertResourceCalled('User', 'nobody',
                          gid='hadoop',
                          groups=['nobody'], )
    self.assertResourceCalled('User', 'hdfs',
                          gid='hadoop',
                          groups=['hadoop'], )
    self.assertResourceCalled('User', 'mapred',
                          gid='hadoop',
                          groups=['hadoop'], )
    self.assertResourceCalled('User', 'zookeeper',
                          gid='hadoop', )
    self.assertResourceCalled('User', 'storm',
                          gid='hadoop',
                          groups=['hadoop'], )
    self.assertResourceCalled('User', 'falcon',
                              gid='hadoop',
                              groups=['hadoop'], )
    self.assertResourceCalled('Package', 'unzip', )
    self.assertResourceCalled('Package', 'net-snmp', )
    self.assertNoMoreResources()
