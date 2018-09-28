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
import os
from ambari_commons import subprocess32
import select

from stacks.utils.RMFTestCase import *
from mock.mock import patch, MagicMock
from resource_management.core.base import Resource
from resource_management.core.exceptions import Fail

OLD_VERSION_STUB = '2.1.0.0-400'
VERSION_STUB_WITHOUT_BUILD_NUMBER = '2.2.0.1'
VERSION_STUB = '2.2.0.1-885'

subproc_mock = MagicMock()
subproc_mock.return_value = MagicMock()
subproc_stdout = MagicMock()
subproc_mock.return_value.stdout = subproc_stdout

@patch.object(os, "read", new=MagicMock(return_value=None))
@patch.object(select, "select", new=MagicMock(return_value=([subproc_stdout], None, None)))
@patch("pty.openpty", new = MagicMock(return_value=(1,5)))
@patch.object(os, "close", new=MagicMock())
@patch.object(subprocess32, "Popen", new=subproc_mock)
class TestMpackPackages(RMFTestCase):
  _install_failed = False
  def setUp(self):
    self.maxDiff = None

  @staticmethod
  def _add_packages(*args):
    return [
      ["pkg1", "1.0", "repo"],
      ["pkg2", "2.0", "repo2"]
    ]

  @staticmethod
  def _add_packages_available(*args):
    return [
      ["hadoop_2_2_0_1_885", "1.0", "HDP-2.2"],
      ["hadooplzo_2_2_0_1_885", "1.0", "HDP-2.2"],
      ["hadoop_2_2_0_1_885-libhdfs", "1.0", "HDP-2.2"]
    ]

  @staticmethod
  def _add_packages_lookUpYum(*args):
    return TestMpackPackages._add_packages_available(*args)


  @staticmethod
  def _add_packages_with_fail():
    arg = []
    arg.append(["pkg1_2_2_0_1_885_pack", "1.0", "repo"])
    arg.append(["pkg2_2_2_0_1_885_pack2", "2.0", "repo2"])
    if TestMpackPackages._install_failed:
      arg.append(["should_not_be_removed_pkg1", "1.0", "repo"])
      arg.append(["hadoop_2_2_0_1_885fake_pkg", "1.0", "repo"])
      arg.append(["snappy__2_2_0_1_885_fake_pkg", "3.0", "repo2"])
      arg.append(["ubuntu-like-2-2-0-1-885-fake-pkg", "3.0", "repo2"])
      arg.append(["should_not_be_removed_pkg2", "3.0", "repo2"])

    return arg

  @staticmethod
  def _new_with_exception(cls, name, env=None, provider=None, **kwargs):
    if (name != "snappy-devel"):
      return Resource.__new__(cls, name, env, provider, **kwargs)
    else:
      TestMpackPackages._install_failed = True
      raise Exception()

  @staticmethod
  def side_effect(*args):
    TestMpackPackages._install_failed = True
    raise Exception()

  @patch("resource_management.libraries.functions.list_ambari_managed_repos.list_ambari_managed_repos")
  @patch("ambari_commons.os_check.OSCheck.is_redhat_family")
  @patch("ambari_commons.repo_manager.ManagerFactory.get")
  @patch("resource_management.core.resources.packaging.Package.__new__")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  @patch("ambari_commons.shell.launch_subprocess")
  def test_fail(self, subprocess_with_timeout, put_structured_out_mock, Package__mock, get_provider,
                is_redhat_family_mock, list_ambari_managed_repos_mock):
    from ambari_commons.os_check import OSConst
    from ambari_commons.repo_manager import ManagerFactory

    pkg_manager = ManagerFactory.get_new_instance(OSConst.REDHAT_FAMILY)

    with patch.object(pkg_manager, "all_packages") as all_packages, \
      patch.object(pkg_manager, "available_packages") as available_packages, \
      patch.object(pkg_manager, "installed_packages") as installed_packages, \
      patch.object(pkg_manager, "check_uncompleted_transactions") as check_uncompleted_transactions, \
      patch.object(pkg_manager, "upgrade_package") as upgrade_package:
      all_packages.side_effect = TestMpackPackages._add_packages_with_fail
      all_packages.return_value = TestMpackPackages._add_packages_with_fail
      available_packages.side_effect = TestMpackPackages._add_packages_with_fail
      installed_packages.side_effect = TestMpackPackages._add_packages_with_fail
      upgrade_package.side_effect = TestMpackPackages.side_effect
      check_uncompleted_transactions.return_value = False

      get_provider.return_value = pkg_manager

      is_redhat_family_mock.return_value = True
      list_ambari_managed_repos_mock.return_value = []

      Package__mock.side_effect = TestMpackPackages.side_effect
      self.assertRaises(Fail, self.executeScript, "scripts/mpack_packages.py",
                         classname="MpackPackages",
                         command="actionexecute",
                         config_file="mpack_packages.json",
                         target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                         os_type=('Redhat', '6.4', 'Final'),
      )
      self.assertTrue(put_structured_out_mock.called)
      # script.put_structured_out only display repo file info (mpack info), no installation info
      self.assertEquals(put_structured_out_mock.call_args[0][0],
                        {
                          'mpack_installation':
                            {
                              'mpackId': 2,
                              'mpackName': 'HDPCORE',
                              'mpackVersion': '1.0.0-b251'
                            }
                        })
      # Since installation fails, no resource is consumed
      # After merge, now env includes a None repository
      self.assertEqual(3, len(self.get_resources()))

      TestMpackPackages._install_failed = False

  @patch("resource_management.libraries.functions.list_ambari_managed_repos.list_ambari_managed_repos")
  @patch("ambari_commons.repo_manager.ManagerFactory.get")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  @patch("ambari_commons.shell.launch_subprocess")
  def test_normal_flow_rhel(self,
                                    subprocess_with_timeout,
                                    put_structured_out_mock,
                                    get_provider,
                                    list_ambari_managed_repos_mock):
    from ambari_commons.os_check import OSConst
    from ambari_commons.repo_manager import ManagerFactory

    pkg_manager = ManagerFactory.get_new_instance(OSConst.REDHAT_FAMILY)

    with patch.object(pkg_manager, "all_packages") as all_packages, \
      patch.object(pkg_manager, "available_packages") as available_packages, \
      patch.object(pkg_manager, "installed_packages") as installed_packages, \
      patch.object(pkg_manager, "check_uncompleted_transactions") as check_uncompleted_transactions:
      all_packages.side_effect = TestMpackPackages._add_packages_available
      available_packages.side_effect = TestMpackPackages._add_packages_available
      installed_packages.side_effect = TestMpackPackages._add_packages_available
      check_uncompleted_transactions.return_value = False

      get_provider.return_value = pkg_manager
      list_ambari_managed_repos_mock.return_value=[]
      repo_file_name = 'ambari-hdp-4'

      self.executeScript("scripts/mpack_packages.py",
                         classname="MpackPackages",
                         command="actionexecute",
                         config_file="mpack_packages.json",
                         target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                         os_type=('Redhat', '6.4', 'Final'),
      )
      self.assertTrue(put_structured_out_mock.called)
      self.assertEquals(put_structured_out_mock.call_args[0][0],
        {
          'mpack_installation':
            {
              'mpackId': 2,
              'mpackName': 'HDPCORE',
              'mpackVersion': '1.0.0-b251'
            }
        })

      self.assertResourceCalled('Repository', 'HDP-UTILS-1.1.0.21-repo-hdpcore',
        base_url = u'http://repos.ambari.apache.org/hdp/HDP-UTILS-1.1.0.21',
        action = ['prepare'],
        components = [u'HDP-UTILS', 'main'],
        repo_template = None,
        repo_file_name = u'ambari-hdpcore-2',
        mirror_list = None,
      )


      self.assertResourceCalled('Repository', 'HDPCORE-1.0.0-b251-repo-hdpcore',
        base_url = u'http://repos.ambari.apache.org/hdp/HDPCORE-1.0.0-b251',
        action = ['prepare'],
        components = [u'HDPCORE', 'main'],
        repo_template = None,
        repo_file_name = u'ambari-hdpcore-2',
        mirror_list = None,
      )

      self.assertEqual(1, len(self.get_resources()))

