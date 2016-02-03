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

# System imports
import os
import sys

from mock.mock import patch

# Local imports
from stacks.utils.RMFTestCase import *

COMMON_SERVICES_ALERTS_DIR = "HAWQ/2.0.0/package/alerts"

file_path = os.path.dirname(os.path.abspath(__file__))
file_path = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(file_path)))))
file_path = os.path.join(file_path, "main", "resources", "common-services", COMMON_SERVICES_ALERTS_DIR)

RESULT_STATE_OK = 'OK'
RESULT_STATE_WARNING = 'WARNING'
RESULT_STATE_UNKNOWN = 'UNKNOWN'
RESULT_STATE_SKIPPED = 'SKIPPED'

class TestAlertSyncStatus(RMFTestCase):

  def setUp(self):
    """
    Import the class under test.
    Because the class is present in a different folder, append its dir to the system path.
    Also, shorten the import name and make it a global so the test functions can access it.
    :return:
    """
    sys.path.append(file_path)
    global alert_sync_status
    import alert_sync_status
  
  def test_missing_configs(self):
    """
    Check that the status is UNKNOWN when configs are missing.
    """
    configs = None
    [status, messages] = alert_sync_status.execute(configurations=configs)
    self.assertEqual(status, RESULT_STATE_UNKNOWN)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertEqual(messages[0], 'There were no configurations supplied to the script.')


  @patch("alert_sync_status.get_sync_status")
  def test_no_standby_state(self, get_sync_status_mock):
    """
    Test that the status is SKIPPED when HAWQSTANDBY is not in configurations
    """
    configs = {
      "{{hawq-site/hawq_master_address_port}}": "5432"
    }

    # Mock calls
    get_sync_status_mock.return_value = 'Not Configured'

    [status, messages] = alert_sync_status.execute(configurations=configs)
    self.assertEqual(status, RESULT_STATE_SKIPPED)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertEqual(messages[0], 'HAWQSTANDBY is not installed.')


  @patch("alert_sync_status.get_sync_status")
  def test_synchronized_state(self, get_sync_status_mock):
    """
    Test that the status is OK when HAWQSTANDBY is 'Synchronized' with HAWQMASTER
    """
    configs = {
      "{{hawq-site/hawq_master_address_port}}": "5432",
      "{{hawq-site/hawq_standby_address_host}}": "c6402.ambari.apache.org"
    }

    # Mock calls
    get_sync_status_mock.return_value = 'Synchronized'

    [status, messages] = alert_sync_status.execute(configurations=configs)
    self.assertEqual(status, RESULT_STATE_OK)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertEqual(messages[0], 'HAWQSTANDBY is in sync with HAWQMASTER.')


  @patch("alert_sync_status.get_sync_status")
  def test_synchronizing_state(self, get_sync_status_mock):
    """
    Test that the status is OK when HAWQSTANDBY is 'Synchronizing' with HAWQMASTER
    """
    configs = {
      "{{hawq-site/hawq_master_address_port}}": "5432",
      "{{hawq-site/hawq_standby_address_host}}": "c6402.ambari.apache.org"
    }

    # Mock calls
    get_sync_status_mock.return_value = 'Synchronizing'

    [status, messages] = alert_sync_status.execute(configurations=configs)
    self.assertEqual(status, RESULT_STATE_OK)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertEqual(messages[0], 'HAWQSTANDBY is in sync with HAWQMASTER.')


  @patch("alert_sync_status.get_sync_status")
  def test_not_synchronized_state(self, get_sync_status_mock):
    """
    Test that the status is WARNING when HAWQSTANDBY is 'Noe Synchronized' with HAWQMASTER
    """
    configs = {
      "{{hawq-site/hawq_master_address_port}}": "5432",
      "{{hawq-site/hawq_standby_address_host}}": "c6402.ambari.apache.org"
    }

    # Mock calls
    get_sync_status_mock.return_value = 'Not Synchronized'

    [status, messages] = alert_sync_status.execute(configurations=configs)
    self.assertEqual(status, RESULT_STATE_WARNING)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertEqual(messages[0], 'HAWQSTANDBY is not in sync with HAWQMASTER.')


  @patch("alert_sync_status.get_sync_status")
  def test_none_state(self, get_sync_status_mock):
    """
    Test that the status is UNKNOWN when HAWQMASTER returns summary_state as 'None'
    """
    configs = {
      "{{hawq-site/hawq_master_address_port}}": "5432",
      "{{hawq-site/hawq_standby_address_host}}": "c6402.ambari.apache.org"
    }

    # Mock calls
    get_sync_status_mock.return_value = 'None'

    [status, messages] = alert_sync_status.execute(configurations=configs)
    self.assertEqual(status, RESULT_STATE_UNKNOWN)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertEqual(messages[0], 'Sync status cannot be determined.')


  @patch("alert_sync_status.get_sync_status")
  def test_not_configured_state(self, get_sync_status_mock):
    """
    Test that the status is UNKNOWN when HAWQMASTER returns summary_state as 'Not Configured'
    """
    configs = {
      "{{hawq-site/hawq_master_address_port}}": "5432",
      "{{hawq-site/hawq_standby_address_host}}": "c6402.ambari.apache.org"
    }

    # Mock calls
    get_sync_status_mock.return_value = 'Not Configured'

    [status, messages] = alert_sync_status.execute(configurations=configs)
    self.assertEqual(status, RESULT_STATE_UNKNOWN)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertEqual(messages[0], 'Sync status cannot be determined.')


  @patch("alert_sync_status.get_sync_status")
  def test_unknown_state(self, get_sync_status_mock):
    """
    Test that the status is UNKNOWN when HAWQMASTER returns summary_state as 'Unknown'
    """
    configs = {
      "{{hawq-site/hawq_master_address_port}}": "5432",
      "{{hawq-site/hawq_standby_address_host}}": "c6402.ambari.apache.org"
    }

    # Mock calls
    get_sync_status_mock.return_value = 'Unknown'

    [status, messages] = alert_sync_status.execute(configurations=configs)
    self.assertEqual(status, RESULT_STATE_UNKNOWN)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertEqual(messages[0], 'Sync status cannot be determined.')
