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

import unittest
from unittest.mock import MagicMock, patch

from resource_management.core import shell
from resource_management.core.logger import Logger


class TestResourceManagementShell(unittest.TestCase):
  @classmethod
  def setUpClass(cls):
    Logger.initialize_logger()

  def test_user_wrapper_is_executed_by_bash_when_shell_is_false(self):
    process = MagicMock()
    process.poll.return_value = 0
    process.returncode = 0
    process.stdout = None
    process.stderr = None

    with patch.object(shell.subprocess, "Popen", return_value=process) as popen:
      result = shell.call(
        ("hdfs", "zkfc", "-formatZK", "-nonInteractive"),
        user="hdfs",
        shell=False,
        stdout=None,
        stderr=None,
      )

    self.assertEqual((0, ""), result)
    command = popen.call_args.args[0]
    self.assertEqual("/bin/bash", command[0])
    self.assertEqual("-c", command[3])
    self.assertIn("ambari-sudo.sh su hdfs", command[4])
    self.assertIn("hdfs zkfc -formatZK -nonInteractive", command[4])


if __name__ == "__main__":
  unittest.main()
