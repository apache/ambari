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

from mock.mock import MagicMock, call, patch
from resource_management import *
from stacks.utils.RMFTestCase import *

@patch.object(Hook, "run_custom_hook", new = MagicMock())
class TestHookBeforeInstall(RMFTestCase):
  def test_hook_default(self):
    self.executeScript("1.3.2/hooks/before-ANY/scripts/hook.py",
                       classname="BeforeAnyHook",
                       command="hook",
                       config_file="default.json"
    )
    self.assertResourceCalled('Execute', 'mkdir -p /tmp/AMBARI-artifacts/;     curl -kf -x "" --retry 10     http://c6401.ambari.apache.org:8080/resources//UnlimitedJCEPolicyJDK7.zip -o /tmp/AMBARI-artifacts//UnlimitedJCEPolicyJDK7.zip',
        environment = {'no_proxy': 'c6401.ambari.apache.org'},
        not_if = 'test -e /tmp/AMBARI-artifacts//UnlimitedJCEPolicyJDK7.zip',
        ignore_failures = True,
        path = ['/bin', '/usr/bin/'],
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
    self.assertResourceCalled('Group', 'nagios',
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
        groups = [u'hadoop'],
    )
    self.assertResourceCalled('User', 'nobody',
        gid = 'hadoop',
        ignore_failures = False,
        groups = [u'nobody'],
    )
    self.assertResourceCalled('User', 'nagios',
        gid = 'nagios',
        ignore_failures = False,
        groups = [u'hadoop'],
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
        gid = 'hadoop',
        ignore_failures = False,
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
        groups = [u'hadoop'],
    )
    self.assertResourceCalled('User', 'zookeeper',
        gid = 'hadoop',
        ignore_failures = False,
        groups = [u'hadoop'],
    )
    self.assertResourceCalled('User', 'falcon',
        gid = 'hadoop',
        ignore_failures = False,
        groups = [u'hadoop'],
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
    self.assertResourceCalled('Execute', '/tmp/changeUid.sh ambari-qa /tmp/hadoop-ambari-qa,/tmp/hsperfdata_ambari-qa,/home/ambari-qa,/tmp/ambari-qa,/tmp/sqoop-ambari-qa 2>/dev/null',
        not_if = 'test $(id -u ambari-qa) -gt 1000',
    )
    self.assertResourceCalled('File', '/tmp/changeUid.sh',
        content = StaticFile('changeToSecureUid.sh'),
        mode = 0555,
    )
    self.assertResourceCalled('Execute', '/tmp/changeUid.sh hbase /home/hbase,/tmp/hbase,/usr/bin/hbase,/var/log/hbase,/hadoop/hbase 2>/dev/null',
        not_if = 'test $(id -u hbase) -gt 1000',
    )
    self.assertResourceCalled('Directory', '/etc/hadoop/conf.empty',
        owner = 'root',
        group = 'root',
        recursive = True,
    )
    self.assertResourceCalled('Link', '/etc/hadoop/conf',
        not_if = 'ls /etc/hadoop/conf',
        to = '/etc/hadoop/conf.empty',
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/hadoop-env.sh',
        content = InlineTemplate(self.getConfig()['configurations']['hadoop-env']['content']),
        owner = 'hdfs',
    )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
        owner = 'hdfs',
        group = 'hadoop',
        conf_dir = '/etc/hadoop/conf',
        configuration_attributes = self.getConfig()['configuration_attributes']['core-site'],
        configurations = self.getConfig()['configurations']['core-site'],
    )
    self.assertNoMoreResources()
