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
    self.assertResourceCalled('Execute', 'mkdir -p /tmp/HDP-artifacts/;     curl -kf -x "" --retry 10     http://c6401.ambari.apache.org:8080/resources//UnlimitedJCEPolicyJDK7.zip -o /tmp/HDP-artifacts//UnlimitedJCEPolicyJDK7.zip',
        environment = {'no_proxy': 'c6401.ambari.apache.org'},
        not_if = 'test -e /tmp/HDP-artifacts//UnlimitedJCEPolicyJDK7.zip',
        ignore_failures = True,
        path = ['/bin', '/usr/bin/'],
    )
    self.assertNoMoreResources()