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


  @patch.object(UpgradeHelper_HDP2, 'backup_single_config_type')
  def test_backup_configs(self, backup_config_mock):
    UpgradeHelper_HDP2.backup_configs(None)
    self.assertTrue(backup_config_mock.called)

  @patch.object(UpgradeHelper_HDP2, 'update_config')
  @patch.object(UpgradeHelper_HDP2, 'get_config')
  @patch('optparse.Values')
  def test_update_with_append(self, optparse_mock, get_config_mock, update_config_mock):
    opm = optparse_mock.return_value
    update_config_mock.return_value = None
    options = MagicMock()
    args = ["save-configs"]
    opm.parse_args.return_value = (options, args)
    get_config_mock.return_value = {"a1": "va1", "a2": "va2", "b1": "vb1", "b2": "vb2", "c1": "vc1"}
    site_template = {"y1": "vy1", "a1": "REPLACE_WITH_", "a2": "REPLACE_WITH_", "nb1": "REPLACE_WITH_b1",
                     "nb2": "REPLACE_WITH_b2"}
    expected_site = {"y1": "vy1", "a1": "va1", "a2": "va2", "nb1": "vb1", "nb2": "vb2", "c1": "vc1"}
    UpgradeHelper_HDP2.update_config_using_existing(opm, "global", site_template, True)
    get_config_mock.assert_called_once_with(opm, "global")
    update_config_mock.assert_called_once_with(opm, expected_site, "global")


  @patch.object(UpgradeHelper_HDP2, 'update_config')
  @patch.object(UpgradeHelper_HDP2, 'get_config')
  @patch('optparse.Values')
  def test_update_without_append(self, optparse_mock, get_config_mock, update_config_mock):
    opm = optparse_mock.return_value
    update_config_mock.return_value = None
    options = MagicMock()
    args = ["save-configs"]
    opm.parse_args.return_value = (options, args)
    get_config_mock.return_value = {"a1": "va1", "a2": "va2", "b1": "vb1", "b2": "vb2", "c1": "vc1"}
    site_template = {"y1": "vy1", "a1": "REPLACE_WITH_", "a2": "REPLACE_WITH_", "nb1": "REPLACE_WITH_b1",
                     "nb2": "REPLACE_WITH_b2"}
    expected_site = {"y1": "vy1", "a1": "va1", "a2": "va2", "nb1": "vb1", "nb2": "vb2"}
    UpgradeHelper_HDP2.update_config_using_existing(opm, "global", site_template)
    get_config_mock.assert_called_once_with(opm, "global")
    update_config_mock.assert_called_once_with(opm, expected_site, "global")


if __name__ == "__main__":
  unittest.main()
