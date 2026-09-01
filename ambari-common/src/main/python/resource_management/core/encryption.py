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

Ambari Agent

"""

import hashlib
import os

from cryptography.hazmat.primitives import padding
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

V1_PREFIX = "${enc=aes256_hex, value="
V2_PREFIX = "${enc=aes256_gcm_hex, value="
V2_AAD = b"ambari-agent-config-v2"
V2_ITERATIONS = 210000


def ensure_decrypted(value, encryption_key=None):
  if is_encrypted(value):
    key = agent_encryption_key() if encryption_key is None else encryption_key
    if value.startswith(V2_PREFIX):
      return decrypt_gcm(encrypted_value(value), key)
    return decrypt(encrypted_value(value), key)
  else:
    return value


def decrypt(encrypted_value, encryption_key):
  # The legacy Java AESEncryptor contract uses an 8-byte PBKDF2 salt and a
  # 16-byte AES-CBC IV. Keep this exact layout for old Server compatibility.
  salt, iv, data = _decode_envelope(encrypted_value, (8, 16))
  if not data or len(data) % 16 != 0:
    raise ValueError("AES-CBC ciphertext must contain complete blocks")
  key = hashlib.pbkdf2_hmac(
    "sha1", encryption_key.encode("utf-8"), salt, 65536, dklen=16
  )
  decryptor = Cipher(algorithms.AES(key), modes.CBC(iv)).decryptor()
  padded_data = decryptor.update(data) + decryptor.finalize()
  unpadder = padding.PKCS7(algorithms.AES.block_size).unpadder()
  return (unpadder.update(padded_data) + unpadder.finalize()).decode("utf-8")


def decrypt_gcm(encrypted_value, encryption_key):
  salt, nonce, data = _decode_envelope(encrypted_value, (16, 12))
  if len(data) < 16:
    raise ValueError("AES-GCM ciphertext must include an authentication tag")
  key = hashlib.pbkdf2_hmac(
    "sha256",
    encryption_key.encode("utf-8"),
    salt,
    V2_ITERATIONS,
    dklen=32,
  )
  return AESGCM(key).decrypt(nonce, data, V2_AAD).decode("utf-8")


def is_encrypted(value):
  return (
    isinstance(value, str)
    and (value.startswith(V1_PREFIX) or value.startswith(V2_PREFIX))
  )


def encrypted_value(value):
  if not value.endswith("}"):
    raise ValueError("Encrypted property is missing its closing delimiter")
  for prefix in (V1_PREFIX, V2_PREFIX):
    if value.startswith(prefix):
      encrypted = value[len(prefix) : -1]
      if not encrypted:
        raise ValueError("Encrypted property value is empty")
      return encrypted
  raise ValueError("Unsupported encrypted property scheme")


def _decode_envelope(value, expected_prefix_lengths):
  try:
    parts = bytes.fromhex(value).decode("ascii").split("::")
  except (UnicodeDecodeError, ValueError) as error:
    raise ValueError("Encrypted envelope is not valid hex") from error
  if len(parts) != 3:
    raise ValueError("Encrypted envelope must contain exactly three fields")
  try:
    decoded = tuple(bytes.fromhex(part) for part in parts)
  except ValueError as error:
    raise ValueError("Encrypted envelope fields are not valid hex") from error
  for field, expected_length in zip(decoded[:2], expected_prefix_lengths):
    if len(field) != expected_length:
      raise ValueError("Encrypted envelope has an invalid salt or nonce length")
  return decoded


def agent_encryption_key():
  if "AGENT_ENCRYPTION_KEY" not in os.environ:
    raise RuntimeError(
      "Missing encryption key: AGENT_ENCRYPTION_KEY is not defined at environment."
    )
  return os.environ["AGENT_ENCRYPTION_KEY"]
