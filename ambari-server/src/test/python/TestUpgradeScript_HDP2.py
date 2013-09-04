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
import subprocess
from mock.mock import MagicMock
from unittest import TestCase
from mock.mock import patch
import sys
import unittest
import UpgradeHelper_HDP2
import StringIO

class TestUpgradeHDP2Script(TestCase):

  def setUp(self):
    out = StringIO.StringIO()
    sys.stdout = out


  def tearDown(self):
    sys.stdout = sys.__stdout__


  @patch('optparse.Values')
  @patch.object(UpgradeHelper_HDP2, 'backup_single_config_type')
  def test_backup_configs(self, backup_config_mock, optparse_mock):

    opm =  optparse_mock.return_value
    options = MagicMock()
    args = ["backup-configs"]
    opm.parse_args.return_value = (options, args)
    UpgradeHelper_HDP2.backup_configs(None)
    self.assertTrue(backup_config_mock.called)

if __name__ == "__main__":
  unittest.main()
