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

import sys
import unittest
from pathlib import Path
from unittest.mock import MagicMock, patch

SOURCE_DIR = Path(__file__).resolve().parents[2] / "main" / "python"
sys.path.insert(0, str(SOURCE_DIR))

import preinstall_checker


class TestPreinstallChecker(unittest.TestCase):
  def test_parse_options_accepts_legacy_positional_arguments(self):
    options = preinstall_checker.parse_options(
      ["--password", "secret", "legacy-cluster-name"]
    )

    self.assertEqual(["legacy-cluster-name"], options.arguments)

  def test_parse_options_rejects_unknown_option(self):
    with self.assertRaises(SystemExit) as raised:
      preinstall_checker.parse_options(
        ["--password", "secret", "--operaton", "host_check"]
      )

    self.assertEqual(2, raised.exception.code)

  def test_failed_request_is_a_terminal_error(self):
    is_finished, is_successful, progress = preinstall_checker.is_request_finished(
      '{"Requests":{"request_status":"FAILED","progress_percent":37}}'
    )

    self.assertTrue(is_finished)
    self.assertFalse(is_successful)
    self.assertEqual(37, progress)

  @patch.object(preinstall_checker.logger, "info")
  def test_step_accepts_short_labels_on_python_3(self, logger_info_mock):
    preinstall_checker.step("Short check")

    self.assertEqual(2, logger_info_mock.call_count)

  @patch.object(preinstall_checker, "get_server_protocol", return_value="http")
  @patch.object(preinstall_checker.subprocess, "Popen")
  def test_curl_credentials_are_sent_only_through_stdin(self, popen_mock, _):
    process = MagicMock()
    process.communicate.return_value = (
      f"{{}}\n{preinstall_checker.HTTP_STATUS_MARKER}200",
      "",
    )
    process.returncode = 0
    popen_mock.return_value = process

    result = preinstall_checker.execute_curl_command(
      "http://ambari.example:8080/api/v1/clusters",
      user="admin-user",
      password="admin-password",
    )

    command = popen_mock.call_args.args[0]
    self.assertNotIn("admin-user", command)
    self.assertNotIn("admin-password", command)
    self.assertEqual(
      'user = "admin-user:admin-password"\n',
      process.communicate.call_args.args[0],
    )
    self.assertEqual(
      ("{}", preinstall_checker.HTTP_STATUS_MARKER + "200\n", 0), result
    )

  @patch.object(preinstall_checker, "get_server_protocol", return_value="http")
  @patch.object(preinstall_checker.subprocess, "Popen")
  def test_curl_preserves_body_and_exposes_empty_response_status(
    self, popen_mock, _
  ):
    process = MagicMock()
    process.communicate.return_value = (
      "\n{0}201".format(preinstall_checker.HTTP_STATUS_MARKER),
      "",
    )
    process.returncode = 0
    popen_mock.return_value = process

    out, err, exit_code = preinstall_checker.execute_curl_command(
      "http://ambari.example:8080/api/v1/blueprints/test",
      request_type=preinstall_checker.HTTP_REQUEST_POST,
      request_body="@/tmp/blueprint.json",
      user="admin-user",
      password="admin-password",
    )

    command = popen_mock.call_args.args[0]
    self.assertIn("--write-out", command)
    self.assertEqual("", out)
    self.assertEqual(201, preinstall_checker.get_http_response_code(err))
    self.assertEqual(0, exit_code)

  def test_http_response_code_accepts_curl_marker_and_protocol_versions(self):
    self.assertEqual(
      preinstall_checker.HTTP_FORBIDDEN,
      preinstall_checker.get_http_response_code(
        preinstall_checker.HTTP_STATUS_MARKER + "403\n"
      ),
    )
    self.assertEqual(
      preinstall_checker.HTTP_CREATED,
      preinstall_checker.get_http_response_code("HTTP/2 201 Created\n"),
    )
    self.assertEqual(-1, preinstall_checker.get_http_response_code("curl failed"))


if __name__ == "__main__":
  unittest.main()
