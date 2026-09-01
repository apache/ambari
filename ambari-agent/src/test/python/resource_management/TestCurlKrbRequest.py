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

import importlib
import unittest
from unittest.mock import MagicMock, patch

curl_krb = importlib.import_module(
  "resource_management.libraries.functions.curl_krb_request"
)


class TestCurlKrbRequest(unittest.TestCase):
  def tearDown(self):
    curl_krb._KINIT_CACHE_TIMES.clear()

  def test_kinit_refresh_timer_uses_milliseconds(self):
    self.assertTrue(curl_krb.is_kinit_refresh_required(1000, 0, 14400000))
    self.assertFalse(curl_krb.is_kinit_refresh_required(1000, 995, 10000))
    self.assertTrue(curl_krb.is_kinit_refresh_required(1000, 990, 10000))
    self.assertTrue(curl_krb.is_kinit_refresh_required(1000, 1000, 0))

  @patch.object(curl_krb.os.path, "isfile", return_value=False)
  @patch.object(curl_krb.os.path, "exists", return_value=True)
  @patch.object(curl_krb.os, "chmod")
  @patch.object(curl_krb.time, "time", side_effect=(1000, 1000, 1001))
  @patch.object(curl_krb, "get_user_call_output", return_value=(0, "200", ""))
  @patch.object(curl_krb.shell, "checked_call")
  @patch.object(curl_krb.shell, "call", return_value=(0, ""))
  @patch.object(curl_krb, "get_klist_path", return_value="/usr/bin/klist")
  @patch.object(curl_krb.global_lock, "get_lock")
  def test_cached_ticket_uses_verified_curl_without_reauthentication(
    self,
    get_lock_mock,
    _,
    shell_call_mock,
    checked_call_mock,
    get_user_call_output_mock,
    *unused_mocks,
  ):
    get_lock_mock.return_value = MagicMock()
    cache_name = curl_krb.HASH_ALGORITHM(
      b"service/host@EXAMPLE.COM|/etc/security/service.keytab"
    ).hexdigest()
    curl_krb._KINIT_CACHE_TIMES[cache_name] = 995

    result = curl_krb.curl_krb_request(
      "/tmp",
      "/etc/security/service.keytab",
      "service/host@EXAMPLE.COM",
      "https://metrics.example/ws/v1/timeline/metrics",
      "ams",
      None,
      True,
      "AMS",
      "ambari-qa",
      ca_certs="/etc/ambari-agent/conf/ca.crt",
      kinit_timer_ms=10000,
    )

    self.assertEqual((200, None, 1), result)
    shell_call_mock.assert_called_once()
    checked_call_mock.assert_not_called()
    command = get_user_call_output_mock.call_args.args[0]
    self.assertIn("--cacert", command)
    self.assertIn("/etc/ambari-agent/conf/ca.crt", command)
    self.assertNotIn("-k", command)


if __name__ == "__main__":
  unittest.main()
