#!/usr/bin/env python
"""
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

Ambari Agent

"""
import os, sys

from mock.mock import patch
from mock.mock import MagicMock
from unittest import TestCase

from resource_management import *
from resource_management import Script

from ambari_commons.os_check import OSCheck
from update_repo import UpdateRepo

class TestUpdateRepo(TestCase):


  @patch.object(OSCheck, "is_suse_family")
  @patch.object(OSCheck, "is_ubuntu_family")
  @patch.object(OSCheck, "is_redhat_family")
  @patch.object(Script, 'get_config')
  @patch("resource_management.libraries.providers.repository.File")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  @patch.object(System, "os_family", new='redhat')
  def testUpdateRepo(self, structured_out_mock, file_mock, mock_config, is_redhat_mock, is_ubuntu_mock, is_suse_mock):
    ###### valid case
    is_suse_mock.return_value = False
    is_ubuntu_mock.return_value = False
    is_redhat_mock.return_value = True
    updateRepo = UpdateRepo()

    mock_config.return_value = { "configurations": {
                                        "cluster-env": {
                                                "repo_suse_rhel_template": "REPO_SUSE_RHEL_TEST_TEMPLATE",
                                                "repo_ubuntu_template": "REPO_UBUNTU_TEMPLATE"
                                        }
                                 },
                                 "hostLevelParams": {
                                   "repo_info": '{"repositories":[{"base_url":"TEST_BASE_URL","repo_name":"TEST_REPO_NAME","repo_id":"TEST_REPO_ID"}]}'
                                 }
                               }

    with Environment('/') as env:
      updateRepo.actionexecute(None)

    self.assertTrue(file_mock.called)
    self.assertEquals(file_mock.call_args[0][0], "/etc/yum.repos.d/TEST_REPO_NAME.repo")
    self.assertEquals(structured_out_mock.call_args[0][0], {'repo_update': {'message': 'Repository files successfully updated!', 'exit_code': 0}})

    ###### invalid repo info
    file_mock.reset_mock()
    failed = False
    mock_config.return_value = { "configurations": {
                                        "cluster-env": {
                                                "repo_suse_rhel_template": "REPO_SUSE_RHEL_TEST_TEMPLATE",
                                                "repo_ubuntu_template": "REPO_UBUNTU_TEMPLATE"
                                        }
                                 },
                                 "hostLevelParams": {
                                   "repo_info": '{}'
                                 }
                               }
    try:
      with Environment('/') as env:
        updateRepo.actionexecute(None)
    except Exception, exception:
      failed = True

    self.assertFalse(file_mock.called)
    self.assertTrue(failed)