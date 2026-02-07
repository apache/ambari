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

import logging
import ambari_simplejson as json
import os
import threading
from collections import defaultdict
import ambari_pyaes
from ambari_pbkdf2.pbkdf2 import PBKDF2


from ambari_agent.Utils import Utils

logger = logging.getLogger(__name__)


class ClusterCache(dict):
  """
  Maintains an in-memory cache and disk cache (for debugging purposes) for
  every cluster. This is useful for having quick access to any of the properties.
  """

  COMMON_DATA_CLUSTER = "-1"

  file_locks = defaultdict(threading.RLock)

  def __init__(self, cluster_cache_dir, secret=None):
    """
    Initializes the cache.
    :param cluster_cache_dir:
    :return:
    """

    self.cluster_cache_dir = cluster_cache_dir
    self.secret = secret

    self.__current_cache_json_file = os.path.join(
      self.cluster_cache_dir, self.get_cache_name() + ".json"
    )
    self.__current_cache_hash_file = os.path.join(
      self.cluster_cache_dir, "." + self.get_cache_name() + ".hash"
    )

    self._cache_lock = threading.RLock()
    self.__file_lock = ClusterCache.file_locks[self.__current_cache_json_file]

    self.hash = None
    cache_dict = {}

    try:
      with self.__file_lock:
        if os.path.isfile(self.__current_cache_json_file):
          with open(self.__current_cache_json_file, "rb") as fp:  # Note: 'rb' for binary
            encrypted_data = fp.read()
            decrypted_json = self._decrypt_data(encrypted_data)
            cache_dict = json.loads(decrypted_json)

        if os.path.isfile(self.__current_cache_hash_file):
          with open(self.__current_cache_hash_file, "r") as fp:
            self.hash = fp.read()
    except (IOError, ValueError):
      logger.exception(
        f"Cannot load data from {self.__current_cache_json_file} and {self.__current_cache_hash_file}"
      )
      self.hash = None
      cache_dict = {}

    try:
      self.rewrite_cache(cache_dict, self.hash)
    except:
      # Example: hostname change and restart causes old topology loading to fail with exception
      logger.exception(f"Loading saved cache for {self.__class__.__name__} failed")
      self.rewrite_cache({}, None)

  def encrypt(self, plaintext, encryption_key):
    salt = os.urandom(16)
    iv = os.urandom(16)

    key = PBKDF2(encryption_key, salt, iterations=65536).read(16)
    aes = ambari_pyaes.AESModeOfOperationCBC(key, iv=iv)

    # ensure bytes
    if not isinstance(plaintext, bytes):
      plaintext = plaintext.encode()

    # PKCS7 pad
    padded = ambari_pyaes.util.append_PKCS7_padding(plaintext)

    # CBC encrypt block-by-block
    ciphertext = b""
    for i in range(0, len(padded), 16):
      block = padded[i:i + 16]
      encrypted_block = aes.encrypt(block)  # must be exactly 16 bytes
      ciphertext += encrypted_block

    inner = "::".join([
      salt.hex(),
      iv.hex(),
      ciphertext.hex()
    ]).encode()

    return f"${{enc=aes128_hex, value={inner.hex()}}}"

  def decrypt(self, encrypted_value, encryption_key):
    if isinstance(encrypted_value, bytes):
      try:
        ev_str = encrypted_value.decode()
      except Exception:
        ev_str = None
    else:
      ev_str = encrypted_value

    if not ev_str or "value=" not in ev_str:
      return encrypted_value

    enc_text = ev_str.split("value=")[1][:-1]
    # salt::iv::ciphertext(hex)
    salt_hex, iv_hex, data_hex = (
      bytes.fromhex(part)
      for part in bytes.fromhex(enc_text).decode().split("::")
    )

    key = PBKDF2(encryption_key, salt_hex, iterations=65536).read(16)
    aes = ambari_pyaes.AESModeOfOperationCBC(key, iv=iv_hex)

    data = data_hex

    # Decrypt block-by-block (required)
    plaintext = b""
    for i in range(0, len(data), 16):
      block = data[i:i + 16]
      plaintext += aes.decrypt(block)

    # Remove padding
    return ambari_pyaes.util.strip_PKCS7_padding(plaintext)

  def _is_encryption_enabled(self):
    return not self.secret

  def _encrypt_data(self, data):
    """Encrypt string data"""
    if self._is_encryption_enabled():
      return data
    else:
      return self.encrypt(data.encode(), self.secret)

  def _decrypt_data(self, encrypted_data):
    """Decrypt encrypted bytes to string"""
    if self._is_encryption_enabled():
      return encrypted_data
    else:
      return self.decrypt(encrypted_data, self.secret).decode()

  def get_cluster_indepedent_data(self):
    return self[ClusterCache.COMMON_DATA_CLUSTER]

  def get_cluster_ids(self):
    cluster_ids = list(self.keys())[:]
    if ClusterCache.COMMON_DATA_CLUSTER in cluster_ids:
      cluster_ids.remove(ClusterCache.COMMON_DATA_CLUSTER)
    return cluster_ids

  def rewrite_cache(self, cache, cache_hash):
    cache_ids_to_delete = []
    for existing_cluster_id in self:
      if not existing_cluster_id in cache:
        cache_ids_to_delete.append(existing_cluster_id)

    for cluster_id, cluster_cache in cache.items():
      self.rewrite_cluster_cache(cluster_id, cluster_cache)

    with self._cache_lock:
      for cache_id_to_delete in cache_ids_to_delete:
        del self[cache_id_to_delete]

    self.on_cache_update()
    self.persist_cache(cache_hash)

  def cache_update(self, update_dict, cache_hash):
    """
    Update the current dictionary by other one
    """
    merged_dict = Utils.update_nested(self._get_mutable_copy(), update_dict)
    self.rewrite_cache(merged_dict, cache_hash)

  def cache_delete(self, delete_dict, cache_hash):
    raise NotImplemented()

  def rewrite_cluster_cache(self, cluster_id, cache):
    """
    Thread-safe method for writing out the specified cluster cache
    and rewriting the in-memory representation.
    :param cluster_id:
    :param cache:
    :return:
    """
    logger.info(f"Rewriting cache {self.__class__.__name__} for cluster {cluster_id}")

    # The cache should contain exactly the data received from server.
    # Modifications on agent-side will lead to unnecessary cache sync every agent registration. Which is a big concern on perf clusters!
    # Also immutability can lead to multithreading issues.
    immutable_cache = Utils.make_immutable(cache)
    with self._cache_lock:
      self[cluster_id] = immutable_cache

  def persist_cache(self, cache_hash):
    # ensure that our cache directory exists
    if not os.path.exists(self.cluster_cache_dir):
      os.makedirs(self.cluster_cache_dir)

    with self.__file_lock:
      # Encrypt JSON data
      json_str = json.dumps(self, indent=2)
      encrypted_json = self._encrypt_data(json_str)

      with open(self.__current_cache_json_file, "w") as f:
        f.write(encrypted_json)

      if self.hash is not None:
        with open(self.__current_cache_hash_file, "w") as fp:
          fp.write(cache_hash)

    # if all of above are successful finally set the hash
    self.hash = cache_hash

  def _get_mutable_copy(self):
    with self._cache_lock:
      return Utils.get_mutable_copy(self)

  def __getitem__(self, key):
    try:
      return super(ClusterCache, self).__getitem__(key)
    except KeyError:
      raise KeyError(
        f"{self.get_cache_name().title()} for cluster_id={key} is missing. Check if server sent it."
      )

  def on_cache_update(self):
    """
    Call back function called then cache is updated
    """
    pass

  def get_cache_name(self):
    raise NotImplemented()

  def __deepcopy__(self, memo):
    return self.__class__(self.cluster_cache_dir, self.secret)

  def __copy__(self):
    return self.__class__(self.cluster_cache_dir, self.secret)
