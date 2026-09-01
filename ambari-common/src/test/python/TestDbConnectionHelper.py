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

from types import SimpleNamespace
import struct
from unittest import TestCase
from unittest.mock import patch

from ambari_commons import db_connection_helper
from resource_management.core.exceptions import Fail


class TestDbConnectionHelper(TestCase):
  def test_password_is_framed_on_stdin_and_excluded_from_argv(self):
    captured = {}
    secret = "line one\nline two\0\u4e2d\u6587"

    def capture_run(command, payload, environment):
      captured["command"] = command
      captured["payload"] = bytes(payload)
      captured["payload_reference"] = payload
      captured["environment"] = environment
      return SimpleNamespace(returncode=0, stdout=b"Connected\n")

    with patch.object(
      db_connection_helper,
      "_run_db_connection_verification",
      side_effect=capture_run,
    ):
      output = db_connection_helper.verify_db_connection(
        "/ambari/java/bin/java",
        "/agent/DBConnectionVerification.jar:/jdbc/*",
        "jdbc:test:'; touch /tmp/not-executed",
        "user name",
        secret,
        "example.Driver",
        environment={"LD_LIBRARY_PATH": "/native/lib"},
        java_options=["-Djava.library.path=/native/lib"],
      )

    self.assertEqual("Connected", output)
    self.assertNotIn(secret, captured["command"])
    self.assertEqual(
      [
        "/ambari/java/bin/java",
        "-cp",
        "/agent/DBConnectionVerification.jar:/jdbc/*",
        "-Djava.library.path=/native/lib",
        db_connection_helper.DB_CONNECTION_VERIFICATION_CLASS,
        "jdbc:test:'; touch /tmp/not-executed",
        "user name",
        "example.Driver",
      ],
      captured["command"],
    )
    payload = captured["payload"]
    length = struct.unpack(">I", payload[:4])[0]
    self.assertEqual(secret, payload[4 : 4 + length].decode("utf-8"))
    self.assertEqual({0}, set(captured["payload_reference"]))
    self.assertEqual(
      "/native/lib", captured["environment"]["LD_LIBRARY_PATH"]
    )

  def test_failure_retries_and_does_not_report_password(self):
    secret = "do-not-report-this-password"
    result = SimpleNamespace(
      returncode=7,
      stdout=f"driver echoed {secret} before failing".encode(),
    )
    with patch.object(
      db_connection_helper,
      "_run_db_connection_verification",
      return_value=result,
    ) as run_mock, patch.object(db_connection_helper.time, "sleep") as sleep_mock:
      with self.assertRaises(Fail) as context:
        db_connection_helper.verify_db_connection(
          "/java/bin/java",
          "/verification.jar",
          "jdbc:test",
          "user",
          secret,
          "example.Driver",
          tries=3,
          try_sleep=2,
        )

    self.assertEqual(3, run_mock.call_count)
    self.assertEqual(2, sleep_mock.call_count)
    self.assertIn("exit code 7", str(context.exception))
    self.assertIn("[REDACTED]", str(context.exception))
    self.assertNotIn(secret, str(context.exception))

  def test_success_output_redacts_password_echoed_by_driver(self):
    secret = "do-not-return-this-password"
    result = SimpleNamespace(
      returncode=0,
      stdout=f"connected with {secret}".encode(),
    )
    with patch.object(
      db_connection_helper,
      "_run_db_connection_verification",
      return_value=result,
    ):
      output = db_connection_helper.verify_db_connection(
        "/java/bin/java",
        "/verification.jar",
        "jdbc:test",
        "user",
        secret,
        "example.Driver",
      )

    self.assertEqual("connected with [REDACTED]", output)

  def test_oversized_password_is_rejected_before_process_start(self):
    with patch.object(
      db_connection_helper, "_run_db_connection_verification"
    ) as run_mock:
      with self.assertRaisesRegex(ValueError, "exceeds"):
        db_connection_helper.verify_db_connection(
          "/java/bin/java",
          "/verification.jar",
          "jdbc:test",
          "user",
          "x" * (db_connection_helper.MAX_PASSWORD_BYTES + 1),
          "example.Driver",
        )
    run_mock.assert_not_called()

  def test_invalid_retry_count_is_rejected(self):
    with self.assertRaisesRegex(ValueError, "positive integer"):
      db_connection_helper.verify_db_connection(
        "/java/bin/java",
        "/verification.jar",
        "jdbc:test",
        "user",
        "password",
        "example.Driver",
        tries=0,
      )
