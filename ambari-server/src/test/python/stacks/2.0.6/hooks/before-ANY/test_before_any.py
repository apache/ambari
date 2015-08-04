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
from mock.mock import MagicMock, call, patch
from resource_management import Hook

@patch.object(Hook, "run_custom_hook", new = MagicMock())
class TestHookBeforeInstall(RMFTestCase):
  TMP_PATH = '/tmp/hbase-hbase'

  @patch("os.path.exists")
  def test_hook_default(self, os_path_exists_mock):

    def side_effect(path):
      if path == "/etc/hadoop/conf":
        return True
      return False

    os_path_exists_mock.side_effect = side_effect

    self.executeScript("2.0.6/hooks/before-ANY/scripts/hook.py",
                       classname="BeforeAnyHook",
                       command="hook",
                       config_file="default.json"
    )

    self.assertResourceCalled('Group', 'hadoop',
        ignore_failures = False,
    )
    self.assertResourceCalled('Group', 'nobody',
        ignore_failures = False,
    )
    self.assertResourceCalled('Group', 'users',
        ignore_failures = False,
    )
    self.assertResourceCalled('User', 'hive',
        gid = 'hadoop',
        ignore_failures = False,
        groups = [u'hadoop'],
    )
    self.assertResourceCalled('User', 'oozie',
        gid = 'hadoop',
        ignore_failures = False,
        groups = [u'users'],
    )
    self.assertResourceCalled('User', 'nobody',
        gid = 'hadoop',
        ignore_failures = False,
        groups = [u'nobody'],
    )
    self.assertResourceCalled('User', 'ambari-qa',
        gid = 'hadoop',
        ignore_failures = False,
        groups = [u'users'],
    )
    self.assertResourceCalled('User', 'flume',
        gid = 'hadoop',
        ignore_failures = False,
        groups = [u'hadoop'],
    )
    self.assertResourceCalled('User', 'hdfs',
        ignore_failures = False,
        gid = 'hadoop',
        groups = [u'hadoop'],
    )
    self.assertResourceCalled('User', 'storm',
        gid = 'hadoop',
        ignore_failures = False,
        groups = [u'hadoop'],
    )
    self.assertResourceCalled('User', 'mapred',
        gid = 'hadoop',
        ignore_failures = False,
        groups = [u'hadoop'],
    )
    self.assertResourceCalled('User', 'hbase',
        gid = 'hadoop',
        ignore_failures = False,
        groups = [u'hadoop'],
    )
    self.assertResourceCalled('User', 'tez',
        gid = 'hadoop',
        ignore_failures = False,
        groups = [u'users'],
    )
    self.assertResourceCalled('User', 'zookeeper',
        gid = 'hadoop',
        ignore_failures = False,
        groups = [u'hadoop'],
    )
    self.assertResourceCalled('User', 'falcon',
        gid = 'hadoop',
        ignore_failures = False,
        groups = [u'users'],
    )
    self.assertResourceCalled('User', 'sqoop',
        gid = 'hadoop',
        ignore_failures = False,
        groups = [u'hadoop'],
    )
    self.assertResourceCalled('User', 'yarn',
        gid = 'hadoop',
        ignore_failures = False,
        groups = [u'hadoop'],
    )
    self.assertResourceCalled('User', 'hcat',
        gid = 'hadoop',
        ignore_failures = False,
        groups = [u'hadoop'],
    )
    self.assertResourceCalled('File', '/tmp/changeUid.sh',
        content = StaticFile('changeToSecureUid.sh'),
        mode = 0555,
    )
    self.assertResourceCalled('Execute', '/tmp/changeUid.sh ambari-qa /tmp/hadoop-ambari-qa,/tmp/hsperfdata_ambari-qa,/home/ambari-qa,/tmp/ambari-qa,/tmp/sqoop-ambari-qa',
        not_if = '(test $(id -u ambari-qa) -gt 1000) || (false)',
    )
    self.assertResourceCalled('Directory', self.TMP_PATH,
        owner = 'hbase',
        mode = 0775,
        recursive = True,
        cd_access='a'
    )
    self.assertResourceCalled('File', '/tmp/changeUid.sh',
        content = StaticFile('changeToSecureUid.sh'),
        mode = 0555,
    )
    self.assertResourceCalled('Execute', '/tmp/changeUid.sh hbase /home/hbase,/tmp/hbase,/usr/bin/hbase,/var/log/hbase,' + self.TMP_PATH,
        not_if = '(test $(id -u hbase) -gt 1000) || (false)',
    )
    self.assertResourceCalled('User', 'test_user1',
        ignore_failures = False
    )
    self.assertResourceCalled('User', 'test_user2',
        ignore_failures = False
    )
    self.assertResourceCalled('Group', 'hdfs',
        ignore_failures = False,
    )
    self.assertResourceCalled('Group', 'test_group',
        ignore_failures = False,
    )
    self.assertResourceCalled('User', 'hdfs',
        groups = [u'hadoop', u'hdfs', u'test_group'],
        ignore_failures = False
    )
    self.assertResourceCalled('Directory', '/etc/hadoop',
        mode = 0755
    )
    self.assertResourceCalled('Directory', '/etc/hadoop/conf.empty',
        owner = 'root',
        group = 'hadoop',
        recursive = True,
    )
    self.assertResourceCalled('Link', '/etc/hadoop/conf',
        not_if = 'ls /etc/hadoop/conf',
        to = '/etc/hadoop/conf.empty',
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/hadoop-env.sh',
        content = InlineTemplate(self.getConfig()['configurations']['hadoop-env']['content']),
        owner = 'hdfs',
        group = 'hadoop'
    )
    self.assertNoMoreResources()
