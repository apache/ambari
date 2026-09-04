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
import json
import os
import shutil
import threading
import tempfile
from collections import defaultdict

from ambari_agent.Utils import Utils

logger = logging.getLogger(__name__)


class ClusterCache(dict):
  """
  Maintains an in-memory cache and disk cache (for debugging purposes) for
  every cluster. This is useful for having quick access to any of the properties.
  """

  COMMON_DATA_CLUSTER = "-1"

  file_locks = defaultdict(threading.RLock)

  def __init__(self, cluster_cache_dir):
    """
    Initializes the cache.
    :param cluster_cache_dir:
    :return:
    """

    self.cluster_cache_dir = cluster_cache_dir

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
          with open(self.__current_cache_json_file, "r", encoding="utf-8") as fp:
            cache_dict = json.load(fp)

        if os.path.isfile(self.__current_cache_hash_file):
          with open(self.__current_cache_hash_file, "r", encoding="utf-8") as fp:
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

  def get_cluster_indepedent_data(self):
    return self[ClusterCache.COMMON_DATA_CLUSTER]

  def get_cluster_ids(self):
    cluster_ids = list(self.keys())[:]
    if ClusterCache.COMMON_DATA_CLUSTER in cluster_ids:
      cluster_ids.remove(ClusterCache.COMMON_DATA_CLUSTER)
    return cluster_ids

  def rewrite_cache(self, cache, cache_hash):
    with self._cache_lock:
      previous_cache = dict(self)
      previous_hash = self.hash
      try:
        cache_ids_to_delete = [
          cluster_id for cluster_id in self if cluster_id not in cache
        ]
        for cluster_id, cluster_cache in cache.items():
          self.rewrite_cluster_cache(cluster_id, cluster_cache)
        for cluster_id in cache_ids_to_delete:
          del self[cluster_id]

        self.on_cache_update()
        self.persist_cache(cache_hash)
      except Exception:
        dict.clear(self)
        dict.update(self, previous_cache)
        self.hash = previous_hash
        try:
          self.on_cache_update()
        except Exception:
          logger.exception("Failed to restore derived cluster cache state")
        raise

  def cache_update(self, update_dict, cache_hash):
    """
    Update the current dictionary by other one
    """
    with self._cache_lock:
      merged_dict = Utils.update_nested(self._get_mutable_copy(), update_dict)
      self.rewrite_cache(merged_dict, cache_hash)

  def cache_delete(self, delete_dict, cache_hash):
    raise NotImplementedError

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
    os.makedirs(self.cluster_cache_dir, exist_ok=True)

    with self.__file_lock:
      json_descriptor, json_temporary_path = tempfile.mkstemp(
        prefix=".cache-json-", dir=self.cluster_cache_dir, text=True
      )
      hash_temporary_path = None
      json_backup_path = None
      hash_backup_path = None
      rollback_failed = False
      try:
        with os.fdopen(json_descriptor, "w", encoding="utf-8") as stream:
          json.dump(self, stream, indent=2)
          stream.flush()
          os.fsync(stream.fileno())
        os.chmod(json_temporary_path, 0o644)

        if cache_hash is not None:
          hash_descriptor, hash_temporary_path = tempfile.mkstemp(
            prefix=".cache-hash-", dir=self.cluster_cache_dir, text=True
          )
          with os.fdopen(hash_descriptor, "w", encoding="utf-8") as stream:
            stream.write(cache_hash)
            stream.flush()
            os.fsync(stream.fileno())
          os.chmod(hash_temporary_path, 0o644)

        json_backup_path = self._backup_cache_file(
          self.__current_cache_json_file, ".cache-json-previous-"
        )
        hash_backup_path = self._backup_cache_file(
          self.__current_cache_hash_file, ".cache-hash-previous-"
        )

        # Invalidate the old hash before committing new data. Any crash or
        # failure until the final hash replace therefore forces a full refresh.
        try:
          if os.path.exists(self.__current_cache_hash_file):
            os.unlink(self.__current_cache_hash_file)
            self._fsync_cache_directory()

          os.replace(json_temporary_path, self.__current_cache_json_file)
          json_temporary_path = None
          # The hash is the commit marker. Persist the JSON rename before
          # publishing a hash that tells the server this generation is usable.
          self._fsync_cache_directory()
          if hash_temporary_path is not None:
            os.replace(hash_temporary_path, self.__current_cache_hash_file)
            hash_temporary_path = None
          self._fsync_cache_directory()
        except Exception:
          try:
            if json_backup_path is not None:
              os.replace(json_backup_path, self.__current_cache_json_file)
              json_backup_path = None
            elif os.path.exists(self.__current_cache_json_file):
              os.unlink(self.__current_cache_json_file)

            if hash_backup_path is not None:
              os.replace(hash_backup_path, self.__current_cache_hash_file)
              hash_backup_path = None
            elif os.path.exists(self.__current_cache_hash_file):
              os.unlink(self.__current_cache_hash_file)
            self._fsync_cache_directory()
          except Exception:
            rollback_failed = True
            logger.critical(
              "Failed to restore previous cluster cache files; backups remain "
              "at json=%s hash=%s",
              json_backup_path,
              hash_backup_path,
              exc_info=True,
            )
          raise
      finally:
        cleanup_paths = (json_temporary_path, hash_temporary_path)
        if not rollback_failed:
          cleanup_paths += (json_backup_path, hash_backup_path)
        for temporary_path in cleanup_paths:
          if temporary_path:
            try:
              os.unlink(temporary_path)
            except OSError:
              pass

    # if all of above are successful finally set the hash
    self.hash = cache_hash

  def _backup_cache_file(self, path, prefix):
    if not os.path.isfile(path):
      return None
    descriptor, backup_path = tempfile.mkstemp(
      prefix=prefix, dir=self.cluster_cache_dir
    )
    os.close(descriptor)
    try:
      shutil.copy2(path, backup_path)
      descriptor = os.open(backup_path, os.O_RDONLY)
      try:
        os.fsync(descriptor)
      finally:
        os.close(descriptor)
      return backup_path
    except Exception:
      try:
        os.unlink(backup_path)
      except OSError:
        pass
      raise

  def _fsync_cache_directory(self):
    directory_fd = os.open(self.cluster_cache_dir, os.O_RDONLY)
    try:
      os.fsync(directory_fd)
    finally:
      os.close(directory_fd)

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
    raise NotImplementedError

  def __deepcopy__(self, memo):
    return self.__class__(self.cluster_cache_dir)

  def __copy__(self):
    return self.__class__(self.cluster_cache_dir)
