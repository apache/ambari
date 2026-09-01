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

from cryptography.exceptions import InvalidTag

from resource_management.core.encryption import ensure_decrypted


class TestEncryption(TestCase):
  encrypted_value = "${enc=aes256_hex, value=616639333036363938646230613262383a3a32313537386561376136326362656436656135626165313664613265316336663a3a6361633666333432653532393863313364393064626133653562353663663235}"
  encrypted_gcm_value = "${enc=aes256_gcm_hex, value=30303131323233333434353536363737383839396161626263636464656566663a3a3130323133323433353436353736383739386139626163623a3a3631306139313833316330663132366634643433386639363435666164636138323438656130356232623664666233313136633339623638643936613832}"
  encryption_key = "i%r041K%1VC!C5 K=("

  def test_decrypts_java_generated_v1_value(self):
    self.assertEqual(
      "mysecret", ensure_decrypted(self.encrypted_value, self.encryption_key)
    )

  def test_decrypts_java_compatible_v2_value(self):
    self.assertEqual(
      "mysecret-配置",
      ensure_decrypted(self.encrypted_gcm_value, self.encryption_key),
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

  def test_v2_rejects_tampered_authentication_tag(self):
    corrupted_value = self.encrypted_gcm_value[:-2] + "3}"

    with self.assertRaises(InvalidTag):
      ensure_decrypted(corrupted_value, self.encryption_key)

  def test_v2_rejects_wrong_key(self):
    with self.assertRaises(InvalidTag):
      ensure_decrypted(self.encrypted_gcm_value, "wrong-key")

  def test_rejects_malformed_envelope(self):
    malformed = "${enc=aes256_gcm_hex, value=30303a3a3131}"

    with self.assertRaisesRegex(ValueError, "exactly three fields"):
      ensure_decrypted(malformed, self.encryption_key)

  def test_v1_rejects_non_legacy_salt_length(self):
    decoded = bytes.fromhex(self.encrypted_value.split("value=", 1)[1][:-1]).decode()
    _salt, iv, ciphertext = decoded.split("::")
    malformed = "${enc=aes256_hex, value=" + (
      ("00" * 16 + "::" + iv + "::" + ciphertext).encode("ascii").hex()
    ) + "}"

    with self.assertRaisesRegex(ValueError, "invalid salt or nonce length"):
      ensure_decrypted(malformed, self.encryption_key)
