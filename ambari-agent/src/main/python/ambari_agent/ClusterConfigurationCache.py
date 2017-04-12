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

from ambari_agent.ClusterCache import ClusterCache
import logging

logger = logging.getLogger(__name__)

class ClusterConfigurationCache(ClusterCache):
  """
  Maintains an in-memory cache and disk cache of the configurations for
  every cluster. This is useful for having quick access to any of the
  configuration properties.
  """

  def __init__(self, cluster_cache_dir):
    """
    Initializes the configuration cache.
    :param cluster_cache_dir: directory the changed json are saved
    :return:
    """
    super(ClusterConfigurationCache, self).__init__(cluster_cache_dir)

  def get_cache_name(self):
    return 'configurations'

  def get_configuration_value(self, cluster_name, key):
    """
    Gets a value from the cluster configuration map for the given cluster and
    key. The key is expected to be of the form 'foo-bar/baz' or
    'foo-bar/bar-baz/foobarbaz' where every / denotes a new mapping
    :param key:  a lookup key, like 'foo-bar/baz'
    :return: the value, or None if not found
    """
    self._cache_lock.acquire()
    try:
      dictionary = self[cluster_name]
      for layer_key in key.split('/'):
        dictionary = dictionary[layer_key]

      return dictionary

    except KeyError:
      logger.debug("Cache miss for configuration property {0} in cluster {1}".format(key, cluster_name))
      return None
    finally:
      self._cache_lock.release()
