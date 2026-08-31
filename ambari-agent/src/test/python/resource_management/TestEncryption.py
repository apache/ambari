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

from unittest import TestCase
import os
from unittest.mock import patch

from resource_management.core.encryption import ensure_decrypted


class TestEncryption(TestCase):
  encrypted_value = "${enc=aes256_hex, value=616639333036363938646230613262383a3a32313537386561376136326362656436656135626165313664613265316336663a3a6361633666333432653532393863313364393064626133653562353663663235}"
  encryption_key = "i%r041K%1VC!C5 K=("

  def test_decrypts_java_generated_v1_value(self):
    self.assertEqual(
      b"mysecret", ensure_decrypted(self.encrypted_value, self.encryption_key)
    )

  def test_returns_unencrypted_value_unchanged(self):
    self.assertEqual("mysecret", ensure_decrypted("mysecret"))

  @patch.dict(os.environ, {}, clear=True)
  def test_encrypted_value_requires_agent_key(self):
    with self.assertRaisesRegex(RuntimeError, "AGENT_ENCRYPTION_KEY"):
      ensure_decrypted(self.encrypted_value)

  def test_rejects_invalid_padding(self):
    corrupted_value = self.encrypted_value[:-3] + "00}"

    with self.assertRaises(ValueError):
      ensure_decrypted(corrupted_value, self.encryption_key)
