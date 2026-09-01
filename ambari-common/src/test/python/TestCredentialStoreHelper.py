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

from concurrent.futures import ThreadPoolExecutor
import os
import struct
import tempfile
import threading
import time
from types import SimpleNamespace
from unittest import TestCase
from unittest.mock import patch

from ambari_commons import credential_store_helper
from resource_management.core.exceptions import Fail


class TestCredentialStoreHelper(TestCase):
  def test_secret_is_framed_on_stdin_and_excluded_from_argv(self):
    captured = {}
    secret = "line one\nline two\0\u4e2d\u6587"

    def capture_run(command, **kwargs):
      captured["command"] = command
      captured["input"] = bytes(kwargs["input"])
      captured["input_reference"] = kwargs["input"]
      captured["check"] = kwargs["check"]
      provider_path = command[command.index("-provider") + 1]
      with open(
        provider_path.removeprefix("jceks://file"), "wb"
      ) as stream:
        stream.write(b"store")
      return SimpleNamespace(returncode=0)

    with patch.object(
      credential_store_helper.subprocess, "run", side_effect=capture_run
    ):
      result = credential_store_helper.create_credential_store_entry(
        "/java/bin/java",
        "/component/credential/lib/*",
        "database.password",
        "jceks://file/tmp/credentials.jceks",
        secret,
        overwrite=True,
      )

    self.assertEqual(0, result)
    command = captured["command"]
    self.assertNotIn(secret, command)
    self.assertNotIn("-value", command)
    self.assertIn(
      credential_store_helper.credential_store_create_cmd, command
    )
    self.assertEqual("-f", command[-1])
    self.assertIn(
      credential_store_helper.credential_store_create_lib_path,
      command[command.index("-cp") + 1].split(":"),
    )
    payload = captured["input"]
    length = struct.unpack(">I", payload[:4])[0]
    self.assertEqual(secret, payload[4 : 4 + length].decode("utf-8"))
    self.assertEqual(4 + length, len(payload))
    self.assertEqual({0}, set(captured["input_reference"]))
    self.assertFalse(captured["check"])

  def test_public_create_reports_failure_without_secret(self):
    secret = "do-not-report-this-value"
    with patch.object(
      credential_store_helper,
      "create_credential_store_entry",
      return_value=9,
    ) as create_mock:
      with self.assertRaises(Fail) as context:
        credential_store_helper.create_password_in_credential_store(
          "alias",
          "jceks://file/tmp/store.jceks",
          "/credential/lib/*",
          "/java",
          "https://server/resources",
          secret,
        )

    self.assertIn("exit code 9", str(context.exception))
    self.assertNotIn(secret, str(context.exception))
    create_mock.assert_called_once_with(
      "/java/bin/java",
      "/credential/lib/*",
      "alias",
      "jceks://file/tmp/store.jceks",
      secret,
      overwrite=True,
    )

  def test_oversized_secret_is_rejected_before_process_start(self):
    with patch.object(credential_store_helper.subprocess, "run") as run_mock:
      with self.assertRaisesRegex(ValueError, "exceeds"):
        credential_store_helper.create_credential_store_entry(
          "/java/bin/java",
          "/credential/lib/*",
          "alias",
          "jceks://file/tmp/store.jceks",
          "x" * (credential_store_helper.max_credential_bytes + 1),
        )
    run_mock.assert_not_called()

  def test_local_update_is_atomic_and_removes_stale_checksum(self):
    with tempfile.TemporaryDirectory() as directory:
      store = os.path.join(directory, "store.jceks")
      checksum = os.path.join(directory, ".store.jceks.crc")
      with open(store, "wb") as stream:
        stream.write(b"old-store")
      with open(checksum, "wb") as stream:
        stream.write(b"old-checksum")

      def update_store(command, payload):
        temporary_provider = command[command.index("-provider") + 1]
        temporary_store = temporary_provider.removeprefix("jceks://file")
        with open(temporary_store, "wb") as stream:
          stream.write(b"new-store")
        with open(
          os.path.join(os.path.dirname(temporary_store), ".store.jceks.crc"),
          "wb",
        ) as stream:
          stream.write(b"new-checksum")
        return 0

      with patch.object(
        credential_store_helper,
        "_run_credential_store_create",
        side_effect=update_store,
      ):
        result = credential_store_helper.create_credential_store_entry(
          "/java/bin/java",
          "/credential/lib/*",
          "alias",
          f"jceks://file{store}",
          "secret",
          overwrite=True,
        )

      self.assertEqual(0, result)
      with open(store, "rb") as stream:
        self.assertEqual(b"new-store", stream.read())
      self.assertFalse(os.path.exists(checksum))
      self.assertFalse(
        any(name.startswith(".jceks-") for name in os.listdir(directory))
      )

  def test_local_update_failure_preserves_existing_store_and_checksum(self):
    with tempfile.TemporaryDirectory() as directory:
      store = os.path.join(directory, "store.jceks")
      checksum = os.path.join(directory, ".store.jceks.crc")
      with open(store, "wb") as stream:
        stream.write(b"old-store")
      with open(checksum, "wb") as stream:
        stream.write(b"old-checksum")

      with patch.object(
        credential_store_helper,
        "_run_credential_store_create",
        return_value=9,
      ):
        result = credential_store_helper.create_credential_store_entry(
          "/java/bin/java",
          "/credential/lib/*",
          "alias",
          f"jceks://file{store}",
          "secret",
          overwrite=True,
        )

      self.assertEqual(9, result)
      with open(store, "rb") as stream:
        self.assertEqual(b"old-store", stream.read())
      with open(checksum, "rb") as stream:
        self.assertEqual(b"old-checksum", stream.read())

  def test_local_replace_failure_preserves_readable_store_without_stale_checksum(self):
    with tempfile.TemporaryDirectory() as directory:
      store = os.path.join(directory, "store.jceks")
      checksum = os.path.join(directory, ".store.jceks.crc")
      with open(store, "wb") as stream:
        stream.write(b"old-store")
      with open(checksum, "wb") as stream:
        stream.write(b"old-checksum")

      def update_store(command, payload):
        temporary_provider = command[command.index("-provider") + 1]
        with open(
          temporary_provider.removeprefix("jceks://file"), "wb"
        ) as stream:
          stream.write(b"new-store")
        return 0

      with patch.object(
        credential_store_helper,
        "_run_credential_store_create",
        side_effect=update_store,
      ), patch.object(
        credential_store_helper.os,
        "replace",
        side_effect=OSError("replace failed"),
      ):
        with self.assertRaisesRegex(OSError, "replace failed"):
          credential_store_helper.create_credential_store_entry(
            "/java/bin/java",
            "/credential/lib/*",
            "alias",
            f"jceks://file{store}",
            "secret",
            overwrite=True,
          )

      with open(store, "rb") as stream:
        self.assertEqual(b"old-store", stream.read())
      self.assertFalse(os.path.exists(checksum))

  def test_local_updates_are_serialized_without_losing_aliases(self):
    with tempfile.TemporaryDirectory() as directory:
      store = os.path.join(directory, "store.jceks")
      active = 0
      maximum_active = 0
      state_lock = threading.Lock()

      def update_store(command, payload):
        nonlocal active, maximum_active
        temporary_provider = command[command.index("-provider") + 1]
        temporary_store = temporary_provider.removeprefix("jceks://file")
        alias = command[command.index("create") + 1]
        with state_lock:
          active += 1
          maximum_active = max(maximum_active, active)
        try:
          time.sleep(0.02)
          aliases = set()
          if os.path.isfile(temporary_store):
            with open(temporary_store, encoding="utf-8") as stream:
              aliases.update(stream.read().splitlines())
          aliases.add(alias)
          with open(temporary_store, "w", encoding="utf-8") as stream:
            stream.write("\n".join(sorted(aliases)))
          return 0
        finally:
          with state_lock:
            active -= 1

      with patch.object(
        credential_store_helper,
        "_run_credential_store_create",
        side_effect=update_store,
      ), ThreadPoolExecutor(max_workers=2) as executor:
        results = list(
          executor.map(
            lambda alias: credential_store_helper.create_credential_store_entry(
              "/java/bin/java",
              "/credential/lib/*",
              alias,
              f"jceks://file{store}",
              "secret",
              overwrite=True,
            ),
            ("alias-one", "alias-two"),
          )
        )

      self.assertEqual([0, 0], results)
      self.assertEqual(1, maximum_active)
      with open(store, encoding="utf-8") as stream:
        self.assertEqual({"alias-one", "alias-two"}, set(stream.read().splitlines()))
