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

import grp
import os
import pwd
import stat
import tempfile
import unittest
from unittest.mock import patch

from resource_management.core.exceptions import Fail
from resource_management.libraries.functions import private_temporary_file


class TestPrivateTemporaryFile(unittest.TestCase):
  def test_context_creates_unique_0600_file_and_removes_it(self):
    owner = pwd.getpwuid(os.getuid()).pw_name
    group = grp.getgrgid(os.getgid()).gr_name
    with tempfile.TemporaryDirectory() as temp_dir:
      with private_temporary_file.private_temporary_file(
        "first secret",
        owner,
        group,
        temp_dir=temp_dir,
        prefix="ambari-test-secret-",
      ) as first_path, private_temporary_file.private_temporary_file(
        "second secret",
        owner,
        group,
        temp_dir=temp_dir,
        prefix="ambari-test-secret-",
      ) as second_path:
        self.assertNotEqual(first_path, second_path)
        for path, content in (
          (first_path, "first secret"),
          (second_path, "second secret"),
        ):
          self.assertEqual(0o600, stat.S_IMODE(os.stat(path).st_mode))
          with open(path, encoding="utf-8") as stream:
            self.assertEqual(content, stream.read())

      self.assertFalse(os.path.exists(first_path))
      self.assertFalse(os.path.exists(second_path))

  def test_cleanup_failure_after_success_does_not_expose_content(self):
    owner = pwd.getpwuid(os.getuid()).pw_name
    group = grp.getgrgid(os.getgid()).gr_name
    with tempfile.TemporaryDirectory() as temp_dir:
      with patch.object(
        private_temporary_file.os,
        "unlink",
        side_effect=OSError("cleanup denied"),
      ):
        with self.assertRaisesRegex(
          Fail, "Could not remove private temporary file"
        ) as raised:
          with private_temporary_file.private_temporary_file(
            "must remain confidential",
            owner,
            group,
            temp_dir=temp_dir,
          ):
            pass

      self.assertNotIn("must remain confidential", str(raised.exception))
      for name in os.listdir(temp_dir):
        os.unlink(os.path.join(temp_dir, name))

  def test_permission_failure_rolls_back_created_file(self):
    descriptor, path = tempfile.mkstemp(prefix="ambari-test-secret-")
    owner = pwd.getpwuid(os.getuid()).pw_name
    group = grp.getgrgid(os.getgid()).gr_name
    with patch.object(
      private_temporary_file.tempfile,
      "mkstemp",
      return_value=(descriptor, path),
    ), patch.object(
      private_temporary_file.os,
      "fchmod",
      side_effect=OSError("permission denied"),
    ):
      with self.assertRaisesRegex(OSError, "permission denied"):
        with private_temporary_file.private_temporary_file(
          "must not leak", owner, group
        ):
          pass

    self.assertFalse(os.path.exists(path))

  def test_primary_error_is_preserved_when_cleanup_also_fails(self):
    owner = pwd.getpwuid(os.getuid()).pw_name
    group = grp.getgrgid(os.getgid()).gr_name
    with tempfile.TemporaryDirectory() as temp_dir, patch.object(
      private_temporary_file.os,
      "unlink",
      side_effect=OSError("cleanup denied"),
    ):
      with patch.object(private_temporary_file.Logger, "warning") as warning, \
        self.assertRaisesRegex(Fail, "operation failed") as raised:
        with private_temporary_file.private_temporary_file(
          "highly confidential value",
          owner,
          group,
          temp_dir=temp_dir,
        ):
          raise Fail("operation failed")

    self.assertNotIn("highly confidential value", str(raised.exception))
    self.assertIsNone(raised.exception.__cause__)
    warning.assert_called_once()
    self.assertNotIn("highly confidential value", warning.call_args.args[0])

  def test_owner_primary_group_is_used_when_group_is_omitted(self):
    owner_record = pwd.getpwuid(os.getuid())
    owner = owner_record.pw_name
    with tempfile.TemporaryDirectory() as temp_dir, patch.object(
      private_temporary_file.os, "fchown"
    ) as fchown:
      with private_temporary_file.private_temporary_file(
        "secret",
        owner,
        temp_dir=temp_dir,
      ):
        pass

    self.assertEqual(owner_record.pw_gid, fchown.call_args.args[2])


if __name__ == "__main__":
  unittest.main()
