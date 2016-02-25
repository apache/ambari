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

WORKING_CONFIGS = {
                    "{{hawq-site/hawq_master_address_port}}": "5432",
                    "{{hawq-site/hawq_segment_address_port}}": "40000",
                    "{{hawq-site/hawq_standby_address_host}}": "c6402.ambari.apache.org"
                  }

class TestAlertComponentStatus(RMFTestCase):

  def setUp(self):
    """
    Import the class under test.
    Because the class is present in a different folder, append its dir to the system path.
    Also, shorten the import name and make it a global so the test functions can access it.
    :return:
    """
    sys.path.append(file_path)
    global alert_component_status
    import alert_component_status

  def test_missing_configs(self):
    """
    Check that the status is UNKNOWN when configs are missing.
    """
    configs = None
    [status, messages] = alert_component_status.execute(configurations=configs)
    self.assertEqual(status, alert_component_status.RESULT_STATE_UNKNOWN)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertEqual(messages[0], 'There were no configurations supplied to the script.')

  @patch("alert_component_status.is_component_running")
  def test_hawq_master_ok(self, is_component_running_mock):
    """
    Test that the status is OK when HAWQ Master is up
    """
    # Mock calls
    is_component_running_mock.return_value = True

    [status, messages] = alert_component_status.execute(configurations=WORKING_CONFIGS, parameters={'component_name': 'master'})
    self.assertEqual(status, alert_component_status.RESULT_STATE_OK)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertEqual(messages[0], 'HAWQ Master is running')

  @patch("alert_component_status.is_component_running")
  def test_hawq_master_critical(self, is_component_running_mock):
    """
    Test that the status is CRITICIAL when HAWQ Master is down
    """
    # Mock calls
    is_component_running_mock.return_value = False

    [status, messages] = alert_component_status.execute(configurations=WORKING_CONFIGS, parameters={'component_name': 'master'})
    self.assertEqual(status, alert_component_status.RESULT_STATE_CRITICAL)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertEqual(messages[0], 'HAWQ Master is not running')

  @patch("alert_component_status.is_component_running")
  def test_hawq_standby_ok(self, is_component_running_mock):
    """
    Test that the status is OK when HAWQ Standby is up
    """
    # Mock calls
    is_component_running_mock.return_value = True

    [status, messages] = alert_component_status.execute(configurations=WORKING_CONFIGS, parameters={'component_name': 'standby'})
    self.assertEqual(status, alert_component_status.RESULT_STATE_OK)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertEqual(messages[0], 'HAWQ Standby is running')

  @patch("alert_component_status.is_component_running")
  def test_hawq_standby_critical(self, is_component_running_mock):
    """
    Test that the status is CRITICIAL when HAWQ Standby is down
    """
    # Mock calls
    is_component_running_mock.return_value = False

    [status, messages] = alert_component_status.execute(configurations=WORKING_CONFIGS, parameters={'component_name': 'standby'})
    self.assertEqual(status, alert_component_status.RESULT_STATE_CRITICAL)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertEqual(messages[0], 'HAWQ Standby is not running')

  @patch("alert_component_status.is_component_running")
  def test_hawq_segment_ok(self, is_component_running_mock):
    """
    Test that the status is OK when HAWQ Segment is up
    """
    # Mock calls
    is_component_running_mock.return_value = True

    [status, messages] = alert_component_status.execute(configurations=WORKING_CONFIGS, parameters={'component_name': 'segment'})
    self.assertEqual(status, alert_component_status.RESULT_STATE_OK)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertEqual(messages[0], 'HAWQ Segment is running')

  @patch("alert_component_status.is_component_running")
  def test_hawq_segment_critical(self, is_component_running_mock):
    """
    Test that the status is CRITICIAL when HAWQ Segment is down
    """
    # Mock calls
    is_component_running_mock.return_value = False

    [status, messages] = alert_component_status.execute(configurations=WORKING_CONFIGS, parameters={'component_name': 'segment'})
    self.assertEqual(status, alert_component_status.RESULT_STATE_CRITICAL)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertEqual(messages[0], 'HAWQ Segment is not running')
