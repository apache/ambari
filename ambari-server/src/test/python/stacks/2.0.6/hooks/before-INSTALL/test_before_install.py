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
import json

@patch.object(getpass, "getuser", new = MagicMock(return_value='some_user'))
@patch.object(Hook, "run_custom_hook", new = MagicMock())
class TestHookBeforeInstall(RMFTestCase):
  STACK_VERSION = '2.0.6'

  def test_hook_default(self):
    self.executeScript("before-INSTALL/scripts/hook.py",
                       classname="BeforeInstallHook",
                       stack_version = self.STACK_VERSION,
                       target=RMFTestCase.TARGET_STACK_HOOKS,
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

    self.assertResourceCalled('Repository', 'KIBANA-4.5',
        action=['create'],
        base_url='http://packages.elastic.co/kibana/4.5/debian',
        components=['stable', 'com1 com2'],
        mirror_list=None,
        repo_file_name='KIBANA',
        repo_template='[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0'
    )

    self.assertResourceCalled('Package', 'unzip', retry_count=5, retry_on_repo_unavailability=False)
    self.assertResourceCalled('Package', 'curl', retry_count=5, retry_on_repo_unavailability=False)
    self.assertNoMoreResources()

  def test_hook_no_repos(self):

    config_file = self.get_src_folder() + "/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      command_json = json.load(f)

    command_json['hostLevelParams']['repo_info'] = "[]"

    self.executeScript("before-INSTALL/scripts/hook.py",
                       classname="BeforeInstallHook",
                       command="hook",
                       stack_version = self.STACK_VERSION,
                       target=RMFTestCase.TARGET_STACK_HOOKS,
                       config_dict=command_json)

    self.assertResourceCalled('Package', 'unzip', retry_count=5, retry_on_repo_unavailability=False)
    self.assertResourceCalled('Package', 'curl', retry_count=5, retry_on_repo_unavailability=False)
    self.assertNoMoreResources()



  def test_hook_default_repository_file(self):
    self.executeScript("before-INSTALL/scripts/hook.py",
                       classname="BeforeInstallHook",
                       command="hook",
                       stack_version = self.STACK_VERSION,
                       target=RMFTestCase.TARGET_STACK_HOOKS,
                       config_file="repository_file.json"
    )
    self.assertResourceCalled('Repository', 'HDP-2.2-repo-4',
        action=['create'],
        base_url='http://repo1/HDP/centos5/2.x/updates/2.2.0.0',
        components=['HDP', 'main'],
        mirror_list=None,
        repo_file_name='ambari-hdp-4',
        repo_template='[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0',
        append_to_file=False)

    self.assertResourceCalled('Repository', 'HDP-UTILS-1.1.0.20-repo-4',
        action=['create'],
        base_url='http://repo1/HDP-UTILS/centos5/2.x/updates/2.2.0.0',
        components=['HDP-UTILS', 'main'],
        mirror_list=None,
        repo_file_name='ambari-hdp-4',
        repo_template='[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0',
        append_to_file=True)

    self.assertResourceCalled('Package', 'unzip', retry_count=5, retry_on_repo_unavailability=False)
    self.assertResourceCalled('Package', 'curl', retry_count=5, retry_on_repo_unavailability=False)
    self.assertNoMoreResources()
