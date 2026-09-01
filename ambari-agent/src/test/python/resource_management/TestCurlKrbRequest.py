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
import math
import unittest
from unittest.mock import MagicMock, patch

from resource_management.core.exceptions import Fail

curl_krb = importlib.import_module(
  "resource_management.libraries.functions.curl_krb_request"
)


class TestCurlKrbRequest(unittest.TestCase):
  def test_request_uses_private_cache_structured_commands_and_verified_tls(self):
    cache = MagicMock()
    cache.cache_dir = "/tmp/private/cache"
    cache.environment = {"KRB5CCNAME": "FILE:/tmp/private/cache/krb5cc"}
    cache_context = MagicMock()
    cache_context.__enter__.return_value = cache
    with patch.object(curl_krb, "PrivateKerberosCache", return_value=cache_context), \
      patch.object(curl_krb, "get_kinit_path", return_value="/usr/bin/kinit"), \
      patch.object(
        curl_krb, "get_user_call_output", return_value=(0, "200", "")
      ) as call_output, \
      patch.object(curl_krb.time, "time", side_effect=(1000, 1001)):
      result = curl_krb.curl_krb_request(
        "/tmp",
        "/etc/security/service keytab",
        "service/host@EXAMPLE.COM;$(id)",
        "https://metrics.example/ws/v1/timeline/metrics",
        "ams;$(id)",
        None,
        True,
        "AMS",
        "ambari-qa",
        connection_timeout=0.1,
        ca_certs="/etc/ambari-agent/conf/ca.crt",
      )

    self.assertEqual((200, None, 1), result)
    curl_krb.PrivateKerberosCache.assert_called_once_with(
      "ambari-qa",
      temp_dir="/tmp",
      prefix="ambari-curl-ams___id_-",
    )
    cache.kinit.assert_called_once_with(
      "/usr/bin/kinit",
      "/etc/security/service keytab",
      "service/host@EXAMPLE.COM;$(id)",
    )
    command = call_output.call_args.args[0]
    self.assertIsInstance(command, list)
    self.assertIn("--cacert", command)
    self.assertIn("/etc/ambari-agent/conf/ca.crt", command)
    self.assertIn("--location", command)
    self.assertNotIn("--location-trusted", command)
    self.assertNotIn("-k", command)
    self.assertEqual("1", command[command.index("--connect-timeout") + 1])
    self.assertEqual("3", command[command.index("--max-time") + 1])
    self.assertEqual(
      {"KRB5CCNAME": "FILE:/tmp/private/cache/krb5cc"},
      call_output.call_args.kwargs["env"],
    )

  def test_invalid_timeouts_fail_before_creating_credentials(self):
    for timeout in (0, -1, math.nan, math.inf, "invalid"):
      with self.subTest(timeout=timeout):
        with patch.object(curl_krb, "PrivateKerberosCache") as cache_class:
          with self.assertRaises(Fail):
            curl_krb.curl_krb_request(
              "/tmp",
              "/etc/security/service.keytab",
              "service/host@EXAMPLE.COM",
              "https://metrics.example/",
              "ams",
              None,
              False,
              "AMS",
              "ambari-qa",
              connection_timeout=timeout,
              ca_certs="/etc/ca.crt",
            )
        cache_class.assert_not_called()

  def test_kinit_failure_prevents_curl(self):
    cache = MagicMock()
    cache.kinit.side_effect = Fail("kinit failed")
    cache_context = MagicMock()
    cache_context.__enter__.return_value = cache
    with patch.object(curl_krb, "PrivateKerberosCache", return_value=cache_context), \
      patch.object(curl_krb, "get_kinit_path", return_value="/usr/bin/kinit"), \
      patch.object(curl_krb, "get_user_call_output") as call_output:
      with self.assertRaisesRegex(Fail, "kinit failed"):
        curl_krb.curl_krb_request(
          "/tmp",
          "/etc/security/service.keytab",
          "service/host@EXAMPLE.COM",
          "https://metrics.example/",
          "ams",
          None,
          False,
          "AMS",
          "ambari-qa",
          ca_certs="/etc/ca.crt",
        )
    call_output.assert_not_called()

  def test_method_header_and_body_remain_distinct_argv_tokens(self):
    cache = MagicMock()
    cache.cache_dir = "/tmp/private/cache"
    cache.environment = {"KRB5CCNAME": "FILE:/tmp/private/cache/krb5cc"}
    cache_context = MagicMock()
    cache_context.__enter__.return_value = cache
    with patch.object(curl_krb, "PrivateKerberosCache", return_value=cache_context), \
      patch.object(curl_krb, "get_kinit_path", return_value="/usr/bin/kinit"), \
      patch.object(
        curl_krb, "get_user_call_output", return_value=(0, "{}", "")
      ) as call_output:
      result = curl_krb.curl_krb_request(
        "/tmp",
        "/etc/security/service.keytab",
        "service/host@EXAMPLE.COM",
        "https://ranger.example/api",
        "ranger",
        None,
        False,
        "Ranger",
        "ranger",
        method="POST",
        header="Content-Type: application/json;$(id)",
        body='{"value":"a;$(id)"}',
        ca_certs="/etc/ca.crt",
      )

    self.assertEqual(("{}", None, result[2]), result)
    command = call_output.call_args.args[0]
    self.assertEqual("POST", command[command.index("-X") + 1])
    self.assertEqual(
      "Content-Type: application/json;$(id)", command[command.index("-H") + 1]
    )
    self.assertEqual('{"value":"a;$(id)"}', command[command.index("-d") + 1])

  def test_http_code_result_preserves_public_return_contract(self):
    cache = MagicMock()
    cache.cache_dir = "/tmp/private/cache"
    cache.environment = {"KRB5CCNAME": "FILE:/tmp/private/cache/krb5cc"}
    cache_context = MagicMock()
    cache_context.__enter__.return_value = cache
    with patch.object(curl_krb, "PrivateKerberosCache", return_value=cache_context), \
      patch.object(curl_krb, "get_kinit_path", return_value="/usr/bin/kinit"), \
      patch.object(
        curl_krb, "get_user_call_output", return_value=(0, "200", "")
      ), \
      patch.object(curl_krb.time, "time", side_effect=(1000, 1001)):
      result = curl_krb.curl_krb_request(
        "/tmp",
        "/etc/security/service.keytab",
        "service/host@EXAMPLE.COM",
        "https://metrics.example/",
        "ams",
        None,
        True,
        "AMS",
        "ambari-qa",
        ca_certs="/etc/ca.crt",
      )
    self.assertEqual((200, None, 1), result)


if __name__ == "__main__":
  unittest.main()
