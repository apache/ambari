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

logger = logging.getLogger(__name__)

class ClusterCache(object):
  """
  Maintains an in-memory cache and disk cache (for debugging purposes) for
  every cluster. This is useful for having quick access to any of the properties.
  """

  def __init__(self, cluster_cache_dir):
    """
    Initializes the cache.
    :param cluster_cache_dir:
    :return:
    """
    self.cluster_cache_dir = cluster_cache_dir

    # keys are cluster names, values are caches for the clusters
    self._cache_dict = {}

    self.__file_lock = threading.RLock()
    self._cache_lock = threading.RLock()
    self.__current_cache_json_file = os.path.join(self.cluster_cache_dir, self.get_file_name())

    # ensure that our cache directory exists
    if not os.path.exists(cluster_cache_dir):
      os.makedirs(cluster_cache_dir)

    # if the file exists, then load it
    if os.path.isfile(self.__current_cache_json_file):
      with open(self.__current_cache_json_file, 'r') as fp:
        self._cache_dict = json.load(fp)

  def update_cache(self, cluster_name, cache):
    """
    Thread-safe method for writing out the specified cluster cache
    and updating the in-memory representation.
    :param cluster_name:
    :param cache:
    :return:
    """
    logger.info("Updating cache {0} for cluster {1}".format(self.__class__.__name__, cluster_name))

    self._cache_lock.acquire()
    try:
      self._cache_dict[cluster_name] = cache
    finally:
      self._cache_lock.release()


    self.__file_lock.acquire()
    try:
      with os.fdopen(os.open(self.__current_cache_json_file, os.O_WRONLY | os.O_CREAT, 0o600), "w") as f:
        json.dump(self._cache_dict, f, indent=2)
    finally:
      self.__file_lock.release()

  def get_cache(self):
    self._cache_lock.acquire()
    cache_copy = self._cache_dict[:]
    self._cache_lock.release()
    return cache_copy
