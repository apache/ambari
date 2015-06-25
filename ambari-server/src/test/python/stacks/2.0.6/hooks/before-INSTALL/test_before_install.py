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
import getpass

@patch.object(getpass, "getuser", new = MagicMock(return_value='some_user'))
@patch.object(Hook, "run_custom_hook", new = MagicMock())
class TestHookBeforeInstall(RMFTestCase):
  def test_hook_default(self):
    self.executeScript("2.0.6/hooks/before-INSTALL/scripts/hook.py",
                       classname="BeforeInstallHook",
                       command="hook",
                       config_file="default.json"
    )
    self.assertResourceCalled('Repository', 'HDP-2.0._',
        action=['create'],
        base_url='http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.0.6.0',
        components=['HDP', 'main'],
        mirror_list=None,
        repo_file_name='HDP',
        repo_template='[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0'
    )
    self.assertResourceCalled('Package', 'unzip',)
    self.assertResourceCalled('Package', 'curl',)
    self.assertResourceCalled('Directory', '/tmp/AMBARI-artifacts/',
        recursive = True,
    )
    self.assertResourceCalled('File', '/tmp/jdk-7u67-linux-x64.tar.gz',
        content = DownloadSource('http://c6401.ambari.apache.org:8080/resources//jdk-7u67-linux-x64.tar.gz'),
        not_if = 'test -f /tmp/jdk-7u67-linux-x64.tar.gz',
    )
    self.assertResourceCalled('Directory', '/usr/jdk64',)
    self.assertResourceCalled('Execute', ('chmod', 'a+x', u'/usr/jdk64'),
        not_if = 'test -e /usr/jdk64/jdk1.7.0_45/bin/java',
        sudo = True,
    )
    self.assertResourceCalled('Execute', 'mkdir -p /tmp/jdk && cd /tmp/jdk && tar -xf /tmp/jdk-7u67-linux-x64.tar.gz && ambari-sudo.sh cp -rp /tmp/jdk/* /usr/jdk64',
        not_if = 'test -e /usr/jdk64/jdk1.7.0_45/bin/java',
    )
    self.assertResourceCalled('File', '/usr/jdk64/jdk1.7.0_45/bin/java',
        mode = 0755,
        cd_access = "a",
    )
    self.assertResourceCalled('Execute', ('chgrp', '-R', u'hadoop', u'/usr/jdk64/jdk1.7.0_45'),
        sudo = True,
    )
    self.assertResourceCalled('Execute', ('chown', '-R', 'some_user', u'/usr/jdk64/jdk1.7.0_45'),
        sudo = True,
    )
    self.assertNoMoreResources()
