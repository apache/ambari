#!/usr/bin/env python3

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
"""

from types import SimpleNamespace
from unittest import TestCase
from unittest.mock import patch

from ambari_commons.repo_manager.yum_manager import YumManager
from ambari_commons.repo_manager.zypper_manager import ZypperManager


class TestRepositoryManagers(TestCase):
  def test_yum_queries_installed_package_names_with_rpm_cli(self):
    self._assert_package_matching(
      YumManager(),
      "ambari_commons.repo_manager.yum_manager.shell.subprocess_executor",
    )

  def test_zypper_queries_installed_package_names_with_rpm_cli(self):
    self._assert_package_matching(
      ZypperManager(),
      "ambari_commons.repo_manager.zypper_manager.shell.subprocess_executor",
    )

  def _assert_package_matching(self, manager, executor_path):
    with patch(executor_path) as executor:
      executor.return_value = SimpleNamespace(
        code=0,
        out="ambari-agent\nhadoop_3_3_6\n",
      )

      self.assertTrue(manager.rpm_check_package_available("ambari-*"))
      self.assertTrue(manager.rpm_check_package_available("hadoop_?_?_?"))
      self.assertFalse(manager.rpm_check_package_available("ambari"))
      self.assertFalse(manager.rpm_check_package_available("hadoop_3_3"))

      expected_command = manager.properties.installed_package_names_command
      self.assertTrue(
        all(call.args == (expected_command,) for call in executor.call_args_list)
      )

      executor.return_value = SimpleNamespace(code=1, out="ambari-agent\n")
      self.assertFalse(manager.rpm_check_package_available("ambari-agent"))
