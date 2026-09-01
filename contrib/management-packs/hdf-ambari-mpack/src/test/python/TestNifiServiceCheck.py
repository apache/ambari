#!/usr/bin/env python3

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from io import BytesIO
import http.client
import importlib.util
from pathlib import Path
import sys
from unittest import TestCase
from unittest.mock import Mock, patch
import urllib.error


REPOSITORY_ROOT = Path(__file__).resolve().parents[6]
sys.path.insert(0, str(REPOSITORY_ROOT / "ambari-common/src/main/python"))

MODULE_PATH = (
  Path(__file__).resolve().parents[2]
  / "main/resources/common-services/NIFI/1.0.0/package/scripts/service_check.py"
)
SPEC = importlib.util.spec_from_file_location("hdf_nifi_service_check", MODULE_PATH)
service_check = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(service_check)


class TestNifiServiceCheck(TestCase):
  URL = "https://nifi.example.com:9091/nifi"

  def setUp(self):
    logger_patcher = patch.object(service_check.Logger, "info")
    logger_patcher.start()
    self.addCleanup(logger_patcher.stop)

  def test_accepts_success_and_unauthorized_responses(self):
    response = Mock()
    response.getcode.return_value = 200

    with patch.object(service_check, "openurl", return_value=response) as openurl:
      service_check.NifiServiceCheck.check_nifi_portal(self.URL)

    request = openurl.call_args.args[0]
    self.assertIsInstance(request, service_check.urllib.request.Request)
    self.assertEqual(self.URL, request.full_url)
    openurl.assert_called_once_with(request, timeout=20)

    unauthorized = urllib.error.HTTPError(
      self.URL, 401, "Unauthorized", {}, BytesIO(b"")
    )
    with patch.object(service_check, "openurl", side_effect=unauthorized):
      service_check.NifiServiceCheck.check_nifi_portal(self.URL)

  def test_retries_and_fails_for_non_success_response(self):
    response = Mock()
    response.getcode.return_value = 503

    with (
      patch.object(service_check, "openurl", return_value=response) as openurl,
      patch(
        "resource_management.libraries.functions.decorator.time.sleep"
      ) as sleep,
      self.assertRaisesRegex(service_check.Fail, "Response code 503"),
    ):
      service_check.NifiServiceCheck.check_nifi_portal(self.URL)

    self.assertEqual(15, openurl.call_count)
    self.assertEqual(14, sleep.call_count)

  def test_retries_and_fails_for_network_error(self):
    with (
      patch.object(
        service_check,
        "openurl",
        side_effect=urllib.error.URLError("connection refused"),
      ) as openurl,
      patch(
        "resource_management.libraries.functions.decorator.time.sleep"
      ) as sleep,
      self.assertRaisesRegex(service_check.Fail, "connection refused"),
    ):
      service_check.NifiServiceCheck.check_nifi_portal(self.URL)

    self.assertEqual(15, openurl.call_count)
    self.assertEqual(14, sleep.call_count)

  def test_retries_and_fails_for_bad_http_status_line(self):
    with (
      patch.object(
        service_check,
        "openurl",
        side_effect=http.client.BadStatusLine("not HTTP"),
      ) as openurl,
      patch(
        "resource_management.libraries.functions.decorator.time.sleep"
      ) as sleep,
      self.assertRaisesRegex(service_check.Fail, "Not Reachable"),
    ):
      service_check.NifiServiceCheck.check_nifi_portal(self.URL)

    self.assertEqual(15, openurl.call_count)
    self.assertEqual(14, sleep.call_count)
