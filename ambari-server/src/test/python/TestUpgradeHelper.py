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


from mock.mock import MagicMock, call
from mock.mock import patch

from unittest import TestCase
import sys
import unittest
import upgradeHelper
import StringIO
import logging


class TestUpgradeHelper(TestCase):
  original_curl = None
  out = None

  def setUp(self):
    # replace original curl call to mock
    self.original_curl = upgradeHelper.curl
    upgradeHelper.curl = self.magic_curl

    # mock logging methods
    upgradeHelper.logging.getLogger = MagicMock()
    upgradeHelper.logging.FileHandler = MagicMock()

    self.out = StringIO.StringIO()
    sys.stdout = self.out

  def magic_curl(self, *args, **kwargs):
    def ret_object():
      return ""

    def communicate():
      return "{}", ""

    ret_object.returncode = 0
    ret_object.communicate = communicate

    with patch("upgradeHelper.subprocess") as subprocess:
      subprocess.Popen.return_value = ret_object
      self.original_curl(*args, **kwargs)

  def tearDown(self):
    sys.stdout = sys.__stdout__

  @patch("optparse.OptionParser")
  @patch("upgradeHelper.modify_configs")
  @patch("upgradeHelper.backup_file")
  @patch("__builtin__.open")
  def test_ParseOptions(self, open_mock, backup_file_mock, modify_action_mock, option_parser_mock):
    class options(object):
      user = "test_user"
      hostname = "127.0.0.1"
      clustername = "test1"
      password = "test_password"
      upgrade_json = "catalog_file"
      from_stack = "0.0"
      to_stack = "1.3"
      logfile = "test.log"
      report = "report.txt"
      warnings = []
      printonly = False

    args = ["update-configs"]
    modify_action_mock.return_value = MagicMock()
    backup_file_mock.return_value = MagicMock()
    test_mock = MagicMock()
    test_mock.parse_args = lambda: (options, args)
    option_parser_mock.return_value = test_mock

    upgradeHelper.main()
    self.assertEqual(backup_file_mock.call_count, 0)
    self.assertEqual(modify_action_mock.call_count, 1)
    self.assertEqual({"user": options.user, "pass": options.password}, upgradeHelper.Options.API_TOKENS)
    self.assertEqual(options.clustername, upgradeHelper.Options.CLUSTER_NAME)


if __name__ == "__main__":
  unittest.main()
