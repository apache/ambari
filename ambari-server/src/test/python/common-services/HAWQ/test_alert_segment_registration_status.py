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
from stacks.utils.RMFTestCase import RMFTestCase

HAWQ_ALERTS_DIR = "HAWQ/2.0.0/package/alerts"

file_path = os.path.dirname(os.path.abspath(__file__))
file_path = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(file_path))))
file_path = os.path.join(file_path, "main", "resources", "common-services", HAWQ_ALERTS_DIR)

RESULT_STATE_OK = 'OK'
RESULT_STATE_WARNING = 'WARNING'
RESULT_STATE_UNKNOWN = 'UNKNOWN'
RESULT_STATE_SKIPPED = 'SKIPPED'

class TestAlertRegistrationStatus(RMFTestCase):

  HOST_LIST_A = ['host1','host2','host3','host4']
  HOST_LIST_B = ['host1','host3','host5','host4']
  HOST_LIST_C = ['host1','host2','host3']

  def setUp(self):
    """
    Import the class under test.
    Because the class is present in a different folder, append its dir to the system path.
    Also, shorten the import name and make it a global so the test functions can access it.
    :return:
    """
    sys.path.append(file_path)
    global alert_segment_registration_status
    import alert_segment_registration_status

  def test_missing_configs(self):
    """
    Check if the status is UNKNOWN when configs are missing.
    """
    configs = None
    [status, messages] = alert_segment_registration_status.execute(configurations=configs)
    self.assertEqual(status, RESULT_STATE_UNKNOWN)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertEqual(messages[0], 'There were no configurations supplied to the script.')

  @patch("os.path.isfile", return_value=False)
  def test_missing_slave_file(self, os_path_file_mock):
    """
    Check if the status is SKIPPED when slaves file is missing.
    """
    configs={
      "{{hawq-site/hawq_master_address_port}}": "5432"
     }
    [status, messages] = alert_segment_registration_status.execute(configurations=configs)
    self.assertEqual(status, RESULT_STATE_SKIPPED)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertEqual(messages[0], 'Slaves file is not present in /usr/local/hawq/etc')

  @patch("alert_segment_registration_status.get_segment_list_db")
  @patch("alert_segment_registration_status.get_segment_list_ambari")
  @patch("os.path.isfile", return_value=True)
  def test_successful_registration_status(self, os_path_isfile_mock, get_segment_list_ambari_mock, get_segment_list_db_mock):
    """
    Check if the status is OK if no difference in registration segment number and slaves count.
    """
    get_segment_list_ambari_mock.return_value=self.HOST_LIST_A
    get_segment_list_db_mock.return_value=self.HOST_LIST_A
    configs={
      "{{hawq-site/hawq_master_address_port}}": "5432"
     }

    [status, messages] = alert_segment_registration_status.execute(configurations=configs)
    self.assertEqual(status, RESULT_STATE_OK)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertEqual(messages[0], 'All HAWQ Segments are registered.')

  @patch("alert_segment_registration_status.get_segment_list_db")
  @patch("alert_segment_registration_status.get_segment_list_ambari")
  @patch("os.path.isfile", return_value=True)
  def test_unsuccessful_registration_status_plural(self, os_path_isfile_mock, get_segment_list_ambari_mock, get_segment_list_db_mock):
    """
    Check if the status is WARNING if a difference is present in registration segment number and slaves count.
    """
    get_segment_list_ambari_mock.return_value=self.HOST_LIST_A
    get_segment_list_db_mock.return_value=self.HOST_LIST_B
    configs={
      "{{hawq-site/hawq_master_address_port}}": "5432"
     }

    [status, messages] = alert_segment_registration_status.execute(configurations=configs)
    self.assertEqual(status, RESULT_STATE_WARNING)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertEqual(messages[0], '2 HAWQ Segments are not registered with HAWQ Master. Try restarting HAWQ service if a segment has been added/removed. Check the log file in /var/log/ambari-agent/ambari-alerts.log for more details on unregistered hosts.')

  @patch("alert_segment_registration_status.get_segment_list_db")
  @patch("alert_segment_registration_status.get_segment_list_ambari")
  @patch("os.path.isfile", return_value=True)
  def test_unsuccessful_registration_status(self, os_path_isfile_mock, get_segment_list_ambari_mock, get_segment_list_db_mock):
    """
    Check if the status is WARNING if a difference is present in registration segment number and slaves count.
    """
    get_segment_list_ambari_mock.return_value=self.HOST_LIST_A
    get_segment_list_db_mock.return_value=self.HOST_LIST_C
    configs={
      "{{hawq-site/hawq_master_address_port}}": "5432"
     }

    [status, messages] = alert_segment_registration_status.execute(configurations=configs)
    self.assertEqual(status, RESULT_STATE_WARNING)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertEqual(messages[0], '1 HAWQ Segment is not registered with HAWQ Master. Try restarting HAWQ service if a segment has been added/removed. Check the log file in /var/log/ambari-agent/ambari-alerts.log for more details on unregistered hosts.')

  @patch("alert_segment_registration_status.get_segment_list_db")
  @patch("alert_segment_registration_status.get_segment_list_ambari")
  @patch("os.path.isfile", return_value=True)
  def test_exception_registration_status(self, os_path_isfile_mock, get_segment_list_ambari_mock, get_segment_list_db_mock):
    """
    Check if the status is UNKNOWN if an exception is thrown when finding registration segment number and slaves count.
    """
    get_segment_list_ambari_mock.return_value=self.HOST_LIST_A
    get_segment_list_db_mock.side_effect=Exception("Exception raised to fail")
    configs={
      "{{hawq-site/hawq_master_address_port}}": "5432"
     }

    [status, messages] = alert_segment_registration_status.execute(configurations=configs)
    self.assertEqual(status, RESULT_STATE_UNKNOWN)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertEqual(messages[0], 'HAWQ Segments Registration Status cannot be determined.')

  @patch("alert_segment_registration_status.get_segment_list_db")
  @patch("alert_segment_registration_status.get_segment_list_ambari")
  @patch("os.path.isfile", return_value=True)
  def test_unsuccessful_empty_db_registration_status(self, os_path_isfile_mock, get_segment_list_ambari_mock, get_segment_list_db_mock):
    """
    Check if the status is WARNING if a difference is present in registration segment number and slaves count.
    """
    get_segment_list_ambari_mock.return_value=[]
    get_segment_list_db_mock.return_value=self.HOST_LIST_C
    configs={
      "{{hawq-site/hawq_master_address_port}}": "5432"
     }

    [status, messages] = alert_segment_registration_status.execute(configurations=configs)
    self.assertEqual(status, RESULT_STATE_WARNING)
    self.assertTrue(messages is not None and len(messages) == 1)
    self.assertEqual(messages[0], '3 HAWQ Segments are not registered with HAWQ Master. Try restarting HAWQ service if a segment has been added/removed. Check the log file in /var/log/ambari-agent/ambari-alerts.log for more details on unregistered hosts.')

