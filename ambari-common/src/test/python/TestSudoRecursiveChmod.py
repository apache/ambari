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

Ambari Agent

"""

import importlib
import os
import unittest
from unittest.mock import patch, Mock

from resource_management.core import sudo


class TestSudoRecursiveChmod(unittest.TestCase):
  def load_sudo(self, uid):
    with patch.object(os, "geteuid", return_value=uid):
      return importlib.reload(sudo)

  def tearDown(self):
    importlib.reload(sudo)

  def test_root_file_flags_without_directory_flags(self):
    module = self.load_sudo(0)
    with (
      patch.object(module, "unicode_walk", return_value=[("/fixture", [], ["file"])]),
      patch.object(module.os, "stat", return_value=Mock(st_mode=0o644)),
      patch.object(module, "chmod") as chmod,
    ):
      module.chmod_recursive("/fixture", {"f": "u+x"})
    chmod.assert_called_once_with("/fixture/file", 0o744)

  def test_root_directory_flags_without_file_flags(self):
    module = self.load_sudo(0)
    with (
      patch.object(
        module, "unicode_walk", return_value=[("/fixture", ["dir"], ["file"])]
      ),
      patch.object(module.os, "stat", return_value=Mock(st_mode=0o755)),
      patch.object(module, "chmod") as chmod,
    ):
      module.chmod_recursive("/fixture", {"d": "o-rwx"})
    chmod.assert_called_once_with("/fixture/dir", 0o750)

  def test_non_root_default_and_sudo(self):
    module = self.load_sudo(1000)
    with patch.object(module.shell, "checked_call") as call:
      module.chmod_recursive("/fixture", {"f": "u+x"})
    call.assert_called_once_with(
      ["find", "/fixture", "-type", "f", "-exec", "chmod", "u+x", "{}", ";"], sudo=True
    )

  def test_non_root_follow_links_and_sudo(self):
    module = self.load_sudo(1000)
    with patch.object(module.shell, "checked_call") as call:
      module.chmod_recursive("/fixture", {"d": "u+x"}, True)
    call.assert_called_once_with(
      ["find", "-L", "/fixture", "-type", "d", "-exec", "chmod", "u+x", "{}", ";"],
      sudo=True,
    )


if __name__ == "__main__":
  unittest.main()
