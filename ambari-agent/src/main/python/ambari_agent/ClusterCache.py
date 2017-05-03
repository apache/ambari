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

import hashlib
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

  file_locks = defaultdict(threading.RLock)

  def __init__(self, cluster_cache_dir):
    """
    Initializes the cache.
    :param cluster_cache_dir:
    :return:
    """

    self.cluster_cache_dir = cluster_cache_dir

    self.__current_cache_json_file = os.path.join(self.cluster_cache_dir, self.get_cache_name()+'.json')

    self._cache_lock = threading.RLock()
    self.__file_lock = ClusterCache.file_locks[self.__current_cache_json_file]

    # if the file exists, then load it
    cache_dict = {}
    if os.path.isfile(self.__current_cache_json_file):
      with self.__file_lock:
        with open(self.__current_cache_json_file, 'r') as fp:
          cache_dict = json.load(fp)

    for cluster_id, cache in cache_dict.iteritems():
      immutable_cache = Utils.make_immutable(cache)
      cache_dict[cluster_id] = immutable_cache

    super(ClusterCache, self).__init__(cache_dict)

  def get_cluster_ids(self):
    return self.keys()

  def update_cache(self, cache):
    for cluster_id, cluster_cache in cache.iteritems():
      self.update_cluster_cache(cluster_id, cluster_cache)

  def update_cluster_cache(self, cluster_id, cache):
    """
    Thread-safe method for writing out the specified cluster cache
    and updating the in-memory representation.
    :param cluster_id:
    :param cache:
    :return:
    """
    logger.info("Updating cache {0} for cluster {1}".format(self.__class__.__name__, cluster_id))

    # The cache should contain exactly the data received from server.
    # Modifications on agent-side will lead to unnecessary cache sync every agent registration. Which is a big concern on perf clusters!
    # Also immutability can lead to multithreading issues.
    immutable_cache = Utils.make_immutable(cache)
    with self._cache_lock:
      self[cluster_id] = immutable_cache


    # ensure that our cache directory exists
    if not os.path.exists(self.cluster_cache_dir):
      os.makedirs(self.cluster_cache_dir)

    with self.__file_lock:
      with os.fdopen(os.open(self.__current_cache_json_file, os.O_WRONLY | os.O_CREAT, 0o600), "w") as f:
        json.dump(self, f, indent=2)

  def get_md5_hashsum(self, cluster_id):
    """
    Thread-safe method for writing out the specified cluster cache
    and updating the in-memory representation.
    :param cluster_id:
    :param cache:
    :return:
    """
    with self._cache_lock:
      # have to make sure server generates json in exactly the same way. So hashes are equal
      json_repr = json.dumps(self, sort_keys=True)

    md5_calculator = hashlib.md5()
    md5_calculator.update(json_repr)
    result = md5_calculator.hexdigest()

    logger.info("Cache value for {0} is {1}".format(self.__class__.__name__, result))

    return result

  def get_cache_name(self):
    raise NotImplemented()

  def __deepcopy__(self, memo):
    return self.__class__(self.cluster_cache_dir)

  def __copy__(self):
    return self.__class__(self.cluster_cache_dir)