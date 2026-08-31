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


def ensure_decrypted(value, encryption_key=None):
  if is_encrypted(value):
    return decrypt(
      encrypted_value(value),
      agent_encryption_key() if encryption_key is None else encryption_key,
    )
  else:
    return value


def decrypt(encrypted_value, encryption_key):
  salt, iv, data = [
    bytes.fromhex(each) for each in bytes.fromhex(encrypted_value).decode().split("::")
  ]
  key = hashlib.pbkdf2_hmac(
    "sha1", encryption_key.encode("utf-8"), salt, 65536, dklen=16
  )
  decryptor = Cipher(algorithms.AES(key), modes.CBC(iv)).decryptor()
  padded_data = decryptor.update(data) + decryptor.finalize()
  unpadder = padding.PKCS7(algorithms.AES.block_size).unpadder()
  return unpadder.update(padded_data) + unpadder.finalize()


def is_encrypted(value):
  return (
    isinstance(value, str) and value.startswith("${enc=aes256_hex, value=")
  )  # XXX: ideally it shouldn't be hardcoded but currently only one enc type is supported


def encrypted_value(value):
  return value.split("value=")[1][:-1]


def agent_encryption_key():
  if "AGENT_ENCRYPTION_KEY" not in os.environ:
    raise RuntimeError(
      "Missing encryption key: AGENT_ENCRYPTION_KEY is not defined at environment."
    )
  return os.environ["AGENT_ENCRYPTION_KEY"]
