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

class ClusterTopologyCache(ClusterCache):
  """
  Maintains an in-memory cache and disk cache of the topology for
  every cluster. This is useful for having quick access to any of the
  topology properties.
  """

  def __init__(self, cluster_cache_dir):
    """
    Initializes the topology cache.
    :param cluster_cache_dir:
    :return:
    """
    super(ClusterTopologyCache, self).__init__(cluster_cache_dir)

  def get_cache_name(self):
    return 'topology'

  @staticmethod
  def find_host_by_id(host_dicts, cluster_id, host_id):
    for host_dict in host_dicts:
      if host_dict['hostId'] == host_id:
        return host_dict
    return None

  @staticmethod
  def find_component(component_dicts, cluster_id, service_name, component_name):
    for component_dict in component_dicts:
      if component_dict['serviceName'] == service_name and component_dict['componentName'] == component_name:
        return component_dict
    return None

  def cache_update(self, cache_update):
    mutable_dict = self._get_mutable_copy()

    for cluster_id, cluster_updates_dict in cache_update.iteritems():
      if not cluster_id in mutable_dict:
        logger.error("Cannot do topology update for cluster cluster_id={0}, because do not have information about the cluster")
        continue

      if 'hosts' in cluster_updates_dict:
        hosts_mutable_list = mutable_dict[cluster_id]['hosts']
        for host_updates_dict in cluster_updates_dict['hosts']:
          host_mutable_dict = ClusterTopologyCache.find_host_by_id(hosts_mutable_list, cluster_id, host_updates_dict['hostId'])
          if host_mutable_dict is not None:
            host_mutable_dict.update(host_updates_dict)
          else:
            hosts_mutable_list.append(host_updates_dict)

      if 'components' in cluster_updates_dict:
        components_mutable_list = mutable_dict[cluster_id]['components']
        for component_updates_dict in cluster_updates_dict['components']:
          component_mutable_dict = ClusterTopologyCache.find_component(components_mutable_list, cluster_id, component_updates_dict['serviceName'], component_updates_dict['componentName'])
          if component_mutable_dict is not None:
            component_updates_dict['hostIds'] += component_mutable_dict['hostIds']
            component_updates_dict['hostIds'] = list(set(component_updates_dict['hostIds']))
            component_mutable_dict.update(component_updates_dict)
          else:
            components_mutable_list.append(component_updates_dict)

    self.rewrite_cache(mutable_dict)

  def cache_delete(self, cache_update):
    mutable_dict = self._get_mutable_copy()
    clusters_ids_to_delete = []

    for cluster_id, cluster_updates_dict in cache_update.iteritems():
      if not cluster_id in mutable_dict:
        logger.error("Cannot do topology delete for cluster cluster_id={0}, because do not have information about the cluster")
        continue

      if 'hosts' in cluster_updates_dict:
        hosts_mutable_list = mutable_dict[cluster_id]['hosts']
        for host_updates_dict in cluster_updates_dict['hosts']:
          host_to_delete = ClusterTopologyCache.find_host_by_id(hosts_mutable_list, cluster_id, host_updates_dict['hostId'])
          if host_to_delete is not None:
            mutable_dict[cluster_id]['hosts'] = [host_dict for host_dict in hosts_mutable_list if host_dict != host_to_delete]
          else:
            logger.error("Cannot do topology delete for cluster_id={0}, host_id={1}, because cannot find the host in cache".format(cluster_id, host_updates_dict['hostId']))

      if 'components' in cluster_updates_dict:
        components_mutable_list = mutable_dict[cluster_id]['components']
        for component_updates_dict in cluster_updates_dict['components']:
          component_mutable_dict = ClusterTopologyCache.find_component(components_mutable_list, cluster_id, component_updates_dict['serviceName'], component_updates_dict['componentName'])
          if 'hostIds' in component_mutable_dict:
            exclude_host_ids = component_updates_dict['hostIds']
            component_mutable_dict['hostIds'] = [host_id for host_id in component_mutable_dict['hostIds'] if host_id not in exclude_host_ids]
          if not 'hostIds' in component_mutable_dict or component_mutable_dict['hostIds'] == []:
            if component_mutable_dict is not None:
              mutable_dict[cluster_id]['components'] = [component_dict for component_dict in components_mutable_list if component_dict != component_mutable_dict]
            else:
              logger.error("Cannot do component delete for cluster_id={0}, serviceName={1}, componentName={2}, because cannot find the host in cache".format(cluster_id, component_updates_dict['serviceName'], component_updates_dict['componentName']))

      if cluster_updates_dict == {}:
        clusters_ids_to_delete.append(cluster_id)

    for cluster_id in clusters_ids_to_delete:
      del mutable_dict[cluster_id]

    self.rewrite_cache(mutable_dict)


