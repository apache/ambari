#!/usr/bin/env python

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

from ambari_agent.Utils import Utils

logger = logging.getLogger(__name__)

class ClusterCache(dict):
  """
  Maintains an in-memory cache and disk cache (for debugging purposes) for
  every cluster. This is useful for having quick access to any of the properties.
  """
  COMMON_DATA_CLUSTER = '-1'

  file_locks = defaultdict(threading.RLock)

  def __init__(self, cluster_cache_dir):
    """
    Initializes the cache.
    :param cluster_cache_dir:
    :return:
    """

    self.cluster_cache_dir = cluster_cache_dir
    self.hash = None

    self.__current_cache_json_file = os.path.join(self.cluster_cache_dir, self.get_cache_name()+'.json')

    self._cache_lock = threading.RLock()
    self.__file_lock = ClusterCache.file_locks[self.__current_cache_json_file]

    # if the file exists, then load it
    cache_dict = {}
    if os.path.isfile(self.__current_cache_json_file):
      with self.__file_lock:
        with open(self.__current_cache_json_file, 'r') as fp:
          cache_dict = json.load(fp)

    self.rewrite_cache(cache_dict)

  def get_cluster_indepedent_data(self):
    return self[ClusterCache.COMMON_DATA_CLUSTER]

  def get_cluster_ids(self):
    cluster_ids = self.keys()[:]
    if ClusterCache.COMMON_DATA_CLUSTER in cluster_ids:
      cluster_ids.remove(ClusterCache.COMMON_DATA_CLUSTER)
    return cluster_ids

  def rewrite_cache(self, cache):
    cache_ids_to_delete = []
    for existing_cluster_id in self:
      if not existing_cluster_id in cache:
        cache_ids_to_delete.append(existing_cluster_id)

    for cluster_id, cluster_cache in cache.iteritems():
      self.rewrite_cluster_cache(cluster_id, cluster_cache)

    with self._cache_lock:
      for cache_id_to_delete in cache_ids_to_delete:
        del self[cache_id_to_delete]

    self.on_cache_update()
    self.persist_cache()


  def rewrite_cluster_cache(self, cluster_id, cache):
    """
    Thread-safe method for writing out the specified cluster cache
    and rewriting the in-memory representation.
    :param cluster_id:
    :param cache:
    :return:
    """
    logger.info("Rewriting cache {0} for cluster {1}".format(self.__class__.__name__, cluster_id))

    # The cache should contain exactly the data received from server.
    # Modifications on agent-side will lead to unnecessary cache sync every agent registration. Which is a big concern on perf clusters!
    # Also immutability can lead to multithreading issues.
    immutable_cache = Utils.make_immutable(cache)
    with self._cache_lock:
      self[cluster_id] = immutable_cache

  def persist_cache(self):
    # ensure that our cache directory exists
    if not os.path.exists(self.cluster_cache_dir):
      os.makedirs(self.cluster_cache_dir)

    with self.__file_lock:
      with open(self.__current_cache_json_file, 'w') as f:
        json.dump(self, f, indent=2)

  def _get_mutable_copy(self):
    with self._cache_lock:
      return Utils.get_mutable_copy(self)

  def on_cache_update(self):
    """
    Call back function called then cache is updated
    """
    pass

  def get_cache_name(self):
    raise NotImplemented()

  def __deepcopy__(self, memo):
    return self.__class__(self.cluster_cache_dir)

  def __copy__(self):
    return self.__class__(self.cluster_cache_dir)