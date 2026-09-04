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

import unittest
import importlib
from unittest.mock import MagicMock, patch

from resource_management.core.exceptions import Fail
curl_krb_request = importlib.import_module(
  "resource_management.libraries.functions.curl_krb_request"
)


class TestCurlKerberosRequest(unittest.TestCase):
  def test_request_body_uses_private_file_and_never_crosses_argv(self):
    body = '{"configs":{"password":"credential-value"}}'
    body_path = "/tmp/private-cache/ambari-curl-body-random"
    cache = MagicMock()
    cache.cache_dir = "/tmp/private-cache"
    cache.environment = {"KRB5CCNAME": "FILE:/tmp/private-cache/krb5cc"}
    cache.__enter__.return_value = cache
    cache.__exit__.return_value = False
    body_context = MagicMock()
    body_context.__enter__.return_value = body_path
    body_context.__exit__.return_value = False

    with patch.object(
      curl_krb_request, "PrivateKerberosCache", return_value=cache
    ), patch.object(
      curl_krb_request, "get_kinit_path", return_value="/usr/bin/kinit"
    ), patch.object(
      curl_krb_request,
      "private_temporary_file",
      return_value=body_context,
    ) as private_file, patch.object(
      curl_krb_request,
      "get_user_call_output",
      return_value=(0, '{"name":"kms"}', ""),
    ) as execute:
      response, error, _ = curl_krb_request.curl_krb_request(
        "/tmp",
        "/etc/security/keytabs/kms.keytab",
        "kms/host@EXAMPLE.COM",
        "https://ranger.example/service",
        "ranger-admin",
        None,
        False,
        "Ranger API",
        "kms",
        method="POST",
        body=body,
        header="Content-Type: application/json",
      )

    self.assertEqual('{"name":"kms"}', response)
    self.assertIsNone(error)
    private_file.assert_called_once_with(
      body,
      "kms",
      temp_dir="/tmp/private-cache",
      prefix="ambari-curl-body-",
    )
    command = execute.call_args.args[0]
    self.assertNotIn(body, command)
    self.assertNotIn("credential-value", " ".join(command))
    self.assertIn("--data-binary", command)
    self.assertIn(f"@{body_path}", command)
    self.assertTrue(execute.call_args.kwargs["quiet"])
    self.assertEqual(cache.environment, execute.call_args.kwargs["env"])

  def test_request_failure_does_not_expose_response_or_body(self):
    body = '{"password":"credential-value"}'
    cache = MagicMock()
    cache.cache_dir = "/tmp/private-cache"
    cache.environment = {"KRB5CCNAME": "FILE:/tmp/private-cache/krb5cc"}
    cache.__enter__.return_value = cache
    cache.__exit__.return_value = False
    body_context = MagicMock()
    body_context.__enter__.return_value = "/tmp/private-cache/body"
    body_context.__exit__.return_value = False
    unsafe_failure = Fail("server echoed credential-value")

    with patch.object(
      curl_krb_request, "PrivateKerberosCache", return_value=cache
    ), patch.object(
      curl_krb_request, "get_kinit_path", return_value="/usr/bin/kinit"
    ), patch.object(
      curl_krb_request, "private_temporary_file", return_value=body_context
    ), patch.object(
      curl_krb_request, "get_user_call_output", side_effect=unsafe_failure
    ), patch.object(curl_krb_request.logger, "debug") as debug:
      with self.assertRaises(Fail) as raised:
        curl_krb_request.curl_krb_request(
          "/tmp",
          "/etc/security/keytabs/kms.keytab",
          "kms/host@EXAMPLE.COM",
          "https://ranger.example/service",
          "ranger-admin",
          None,
          False,
          "Ranger API",
          "kms",
          method="POST",
          body=body,
          header="Content-Type: application/json",
        )

    self.assertNotIn("credential-value", str(raised.exception))
    self.assertIsNone(raised.exception.__cause__)
    self.assertNotIn("credential-value", str(debug.call_args))


if __name__ == "__main__":
  unittest.main()
