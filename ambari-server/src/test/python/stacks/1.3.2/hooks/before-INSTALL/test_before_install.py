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
    self.executeScript("1.3.2/hooks/before-INSTALL/scripts/hook.py",
                       classname="BeforeInstallHook",
                       command="hook",
                       config_file="default.json"
    )
    self.assertResourceCalled('Repository', 'HDP-1.3.4',
        action=['create'],
        base_url='http://public-repo-1.hortonworks.com/HDP/centos6/1.x/updates/1.3.3.0',
        components=['HDP', 'main'],
        mirror_list=None,
        repo_file_name='HDP',
        repo_template='repo_suse_rhel.j2'
    )
    self.assertResourceCalled('Execute', 'mkdir -p /tmp/AMBARI-artifacts/ ; curl -kf -x \"\" --retry 10 http://c6401.ambari.apache.org:8080/resources//jdk-7u67-linux-x64.tar.gz -o /tmp/AMBARI-artifacts//jdk-7u67-linux-x64.tar.gz',
        not_if = 'test -e /usr/jdk64/jdk1.7.0_45/bin/java',
        path = ['/bin', '/usr/bin/'],
        environment = {'no_proxy': 'c6401.ambari.apache.org'}
    )
    self.assertResourceCalled('Execute', 'mkdir -p /usr/jdk64 ; cd /usr/jdk64 ; tar -xf /tmp/AMBARI-artifacts//jdk-7u67-linux-x64.tar.gz > /dev/null 2>&1',
        not_if = 'test -e /usr/jdk64/jdk1.7.0_45/bin/java',
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
    self.assertResourceCalled('Package', 'unzip',)
    self.assertNoMoreResources()
