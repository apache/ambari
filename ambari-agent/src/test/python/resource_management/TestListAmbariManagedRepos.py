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
from mock.mock import patch
from mock.mock import MagicMock
from mock.mock import patch, MagicMock
import glob
from ambari_commons.os_check import OSCheck
from resource_management.libraries.functions.list_ambari_managed_repos import *
from resource_management.core.exceptions import Fail
from unittest import TestCase


class TestListAmbariManagedRepos(TestCase):

  @patch("glob.glob")
  @patch.object(OSCheck, "is_ubuntu_family")
  @patch.object(OSCheck, "is_redhat_family")
  @patch.object(OSCheck, "is_suse_family")
  def test_normal_flow_ubuntu(self, is_suse_family_mock,
                       is_redhat_family_mock, is_ubuntu_family_mock, glob_mock):
    is_ubuntu_family_mock.return_value = True
    is_redhat_family_mock.return_value = False
    is_suse_family_mock.return_value = False
    glob_mock.side_effect = \
    [
      [
        "/etc/apt/sources.list.d/HDP-1.1.1.repo",
        "/etc/apt/sources.list.d/HDP-1.1.2.repo",
        "/etc/apt/sources.list.d/HDP-1.1.3.repo",
        "/etc/apt/sources.list.d/HDP-UTILS-1.1.3.repo",
      ],
      [
        "/etc/apt/sources.list.d/HDP-UTILS-1.1.3.repo",
      ],
      []
    ]
    res = list_ambari_managed_repos()
    self.assertEquals(glob_mock.call_args_list[0][0][0], "/etc/apt/sources.list.d/HDP*")
    self.assertEquals(res, ['HDP-1.1.1', 'HDP-1.1.2', 'HDP-1.1.3', 'HDP-UTILS-1.1.3'])
    self.assertTrue(glob_mock.call_count > 1)

  @patch("glob.glob")
  @patch.object(OSCheck, "is_ubuntu_family")
  @patch.object(OSCheck, "is_redhat_family")
  @patch.object(OSCheck, "is_suse_family")
  def test_normal_flow_rhel(self, is_suse_family_mock,
                              is_redhat_family_mock, is_ubuntu_family_mock, glob_mock):
    is_ubuntu_family_mock.return_value = False
    is_redhat_family_mock.return_value = True
    is_suse_family_mock.return_value = False
    glob_mock.side_effect = \
      [
        [
          "/etc/yum.repos.d/HDP-1.1.1.repo",
          "/etc/yum.repos.d/HDP-1.1.2.repo",
          "/etc/yum.repos.d/HDP-1.1.3.repo",
          "/etc/yum.repos.d/HDP-UTILS-1.1.3.repo",
          ],
        [
          "/etc/yum.repos.d/HDP-UTILS-1.1.3.repo",
          ],
        []
      ]
    res = list_ambari_managed_repos()
    self.assertEquals(glob_mock.call_args_list[0][0][0], "/etc/yum.repos.d/HDP*")
    self.assertEquals(res, ['HDP-1.1.1', 'HDP-1.1.2', 'HDP-1.1.3', 'HDP-UTILS-1.1.3'])
    self.assertTrue(glob_mock.call_count > 1)


  @patch("glob.glob")
  @patch.object(OSCheck, "is_ubuntu_family")
  @patch.object(OSCheck, "is_redhat_family")
  @patch.object(OSCheck, "is_suse_family")
  def test_normal_flow_sles(self, is_suse_family_mock,
                              is_redhat_family_mock, is_ubuntu_family_mock, glob_mock):
    is_ubuntu_family_mock.return_value = False
    is_redhat_family_mock.return_value = False
    is_suse_family_mock.return_value = True
    glob_mock.side_effect = \
      [
        [
          "/etc/zypp/repos.d/HDP-1.1.1.repo",
          "/etc/zypp/repos.d/HDP-1.1.2.repo",
          "/etc/zypp/repos.d/HDP-1.1.3.repo",
          "/etc/zypp/repos.d/HDP-UTILS-1.1.3.repo",
          ],
        [
          "/etc/zypp/repos.d/HDP-UTILS-1.1.3.repo",
          ],
        []
      ]
    res = list_ambari_managed_repos()
    self.assertEquals(glob_mock.call_args_list[0][0][0], "/etc/zypp/repos.d/HDP*")
    self.assertEquals(res, ['HDP-1.1.1', 'HDP-1.1.2', 'HDP-1.1.3', 'HDP-UTILS-1.1.3'])
    self.assertTrue(glob_mock.call_count > 1)


  @patch.object(OSCheck, "is_ubuntu_family")
  @patch.object(OSCheck, "is_redhat_family")
  @patch.object(OSCheck, "is_suse_family")
  def test_normal_flow_unknown_os(self, is_suse_family_mock,
                            is_redhat_family_mock, is_ubuntu_family_mock):
    is_ubuntu_family_mock.return_value = False
    is_redhat_family_mock.return_value = False
    is_suse_family_mock.return_value = False
    try:
      list_ambari_managed_repos()
      self.fail("Should throw a Fail")
    except Fail:
      pass  # Expected
