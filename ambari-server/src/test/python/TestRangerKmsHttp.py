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

from io import BytesIO
import json
from pathlib import Path
import socket
from unittest import TestCase
from unittest.mock import MagicMock, patch
import urllib.error

from ambari_commons import import_utils
from resource_management.libraries.functions import decorator


KMS_PATH = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.3.0/services/RANGER_KMS/package/scripts/kms.py"
)
kms = import_utils.load_source("bigtop_ranger_kms_http", str(KMS_PATH))


def response(code, body):
  result = MagicMock()
  result.getcode.return_value = code
  result.read.return_value = body
  return result


class TestRangerKmsHttp(TestCase):
  def setUp(self):
    self.info_patch = patch.object(kms.Logger, "info")
    self.error_patch = patch.object(kms.Logger, "error")
    self.info_patch.start()
    self.error_patch.start()
    self.addCleanup(self.info_patch.stop)
    self.addCleanup(self.error_patch.stop)

  def test_create_repo_accepts_python3_bytes_response(self):
    result = response(200, b'{"name":"kms-repository"}')
    with patch.object(kms.urllib.request, "urlopen", return_value=result) as open_mock:
      self.assertTrue(
        kms.create_repo(
          "https://ranger.example",
          '{"name":"kms-repository"}',
          "admin:password",
        )
      )

    request = open_mock.call_args.args[0]
    self.assertEqual(b'{"name":"kms-repository"}', request.data)
    result.close.assert_called_once_with()

  def test_get_repo_finds_non_first_match_and_skips_malformed_entries(self):
    payload = json.dumps(
      [
        {"name": "different"},
        {},
        {"name": None},
        "not-a-repository",
        {"name": "KMS-Repository"},
      ]
    ).encode("utf-8")
    result = response(200, payload)
    with patch.object(kms.urllib.request, "urlopen", return_value=result):
      self.assertTrue(
        kms.get_repo(
          "https://ranger.example",
          "kms-repository",
          "admin:password",
        )
      )
    result.close.assert_called_once_with()

  def test_get_repo_returns_false_when_no_repository_matches(self):
    result = response(200, b'[{"name":"different"},{}]')
    with patch.object(kms.urllib.request, "urlopen", return_value=result):
      self.assertFalse(
        kms.get_repo(
          "https://ranger.example",
          "kms-repository",
          "admin:password",
        )
      )

  def test_get_repo_returns_false_for_empty_repository_list(self):
    result = response(200, b"[]")
    with patch.object(kms.urllib.request, "urlopen", return_value=result):
      self.assertFalse(
        kms.get_repo(
          "https://ranger.example",
          "kms-repository",
          "admin:password",
        )
      )
    result.close.assert_called_once_with()

  def test_repo_operations_return_false_for_non_success_responses(self):
    cases = (
      (kms.create_repo, b'{}', '{"name":"kms-repository"}'),
      (kms.get_repo, b"[]", "kms-repository"),
    )
    for operation, body, argument in cases:
      result = response(503, body)
      with self.subTest(operation=operation.__name__), patch.object(
        kms.urllib.request, "urlopen", return_value=result
      ):
        self.assertFalse(
          operation(
            "https://ranger.example",
            argument,
            "admin:password",
          )
        )
      result.close.assert_called_once_with()

  def test_get_repo_retries_invalid_json_without_logging_response_body(self):
    with patch.object(
      kms.urllib.request,
      "urlopen",
      side_effect=lambda *_args, **_kwargs: response(200, b"{not-json"),
    ) as open_mock, patch.object(decorator.time, "sleep") as sleep_mock:
      self.assertFalse(
        kms.get_repo(
          "https://ranger.example",
          "kms-repository",
          "admin:password",
        )
      )

    self.assertEqual(5, open_mock.call_count)
    self.assertEqual(4, sleep_mock.call_count)

  def test_get_repo_retries_http_error_and_returns_false(self):
    def http_error(*_args, **_kwargs):
      raise urllib.error.HTTPError(
        "https://ranger.example",
        503,
        "Unavailable",
        {},
        BytesIO(b"failure"),
      )

    with patch.object(
      kms.urllib.request, "urlopen", side_effect=http_error
    ) as open_mock, patch.object(decorator.time, "sleep"):
      self.assertFalse(
        kms.get_repo(
          "https://ranger.example",
          "kms-repository",
          "admin:password",
        )
      )
    self.assertEqual(5, open_mock.call_count)

  def test_get_repo_retries_timeout_and_returns_false(self):
    with patch.object(
      kms.urllib.request,
      "urlopen",
      side_effect=socket.timeout("timed out"),
    ) as open_mock, patch.object(decorator.time, "sleep") as sleep_mock:
      self.assertFalse(
        kms.get_repo(
          "https://ranger.example",
          "kms-repository",
          "admin:password",
        )
      )

    self.assertEqual(5, open_mock.call_count)
    self.assertEqual(4, sleep_mock.call_count)

  def test_create_repo_retries_url_and_timeout_failures(self):
    for error in (urllib.error.URLError("offline"), socket.timeout("timed out")):
      with self.subTest(error=type(error).__name__), patch.object(
        kms.urllib.request, "urlopen", side_effect=error
      ) as open_mock, patch.object(decorator.time, "sleep"):
        self.assertFalse(
          kms.create_repo(
            "https://ranger.example",
            '{"name":"kms-repository"}',
            "admin:password",
          )
        )
      self.assertEqual(5, open_mock.call_count)
