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

COMMON_SERVICES_ALERTS_DIR_PATH = "../../../../../main/resources/common-services/PXF/3.0.0/package/alerts"

current_dir = os.path.dirname(os.path.abspath(__file__))
alerts_dir = os.path.abspath(os.path.join(current_dir, COMMON_SERVICES_ALERTS_DIR_PATH))

class TestAlertsApiStatus(RMFTestCase):

  def setUp(self):
    """
    Import the class under test.
    Because the class is present in a different folder, append its dir to the system path.
    Also, shorten the import name and make it a global so the test functions can access it.
    """
    sys.path.append(alerts_dir)
    global api_status
    import api_status

  @patch("api_status._makeHTTPCall")
  def test_get_pxf_protocol_version(self, makeHTTPCall_mock):

    mock_response = '{ "version": "v14"}'
    makeHTTPCall_mock.return_value = mock_response
    BASE_URL = "http://localhost:51200/pxf/"

    version = api_status._get_pxf_protocol_version(BASE_URL)
    self.assertEqual(version, "v14")

    mock_response = 'BAD RESPONSE'
    makeHTTPCall_mock.return_value = mock_response

    try:
      api_status._get_pxf_protocol_version(BASE_URL)
      self.fail()
    except Exception as e:
      self.assertEqual(str(e), "version could not be found in response BAD RESPONSE")

  @patch("api_status._makeHTTPCall")
  def test_execute(self, makeHTTPCall_mock):

    mock_response = '{ "version": "v14"}'
    makeHTTPCall_mock.return_value = mock_response

    result = api_status.execute(configurations = {})
    self.assertEqual(result, (api_status.RESULT_STATE_OK, ['PXF is functional']))

    mock_response = 'BAD RESPONSE'
    makeHTTPCall_mock.return_value = mock_response

    result = api_status.execute(configurations = {})
    self.assertEqual(result, ('WARNING', ['PXF is not functional on host, None: version could not be found in response BAD RESPONSE']))
