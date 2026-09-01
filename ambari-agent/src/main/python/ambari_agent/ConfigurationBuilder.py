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
import os

import ambari_simplejson as json

from ambari_agent import hostname


logger = logging.getLogger(__name__)


class ConfigurationBuilder:
  def __init__(self, initializer_module):
    self.config = initializer_module.config
    self.metadata_cache = initializer_module.metadata_cache
    self.topology_cache = initializer_module.topology_cache
    self.host_level_params_cache = initializer_module.host_level_params_cache
    self.configurations_cache = initializer_module.configurations_cache

  def get_configuration(
    self, cluster_id, service_name, component_name, configurations_timestamp=None
  ):
    if cluster_id:
      if (
        configurations_timestamp
        and self.configurations_cache.timestamp < configurations_timestamp
      ):
        raise Exception(
          f"Command requires configs with timestamp={configurations_timestamp} but configs on agent have timestamp={self.configurations_cache.timestamp}"
        )

      metadata_cache = self.metadata_cache[cluster_id]
      configurations_cache = self.configurations_cache[cluster_id]
      host_level_params_cache = self.host_level_params_cache[cluster_id]

      command_dict = {
        "clusterLevelParams": metadata_cache.clusterLevelParams,
        "hostLevelParams": host_level_params_cache,
        "clusterHostInfo": self.topology_cache.get_cluster_host_info(cluster_id),
        "localComponents": self.topology_cache.get_cluster_local_components(cluster_id),
        "componentVersionMap": self.topology_cache.get_cluster_component_version_map(
          cluster_id
        ),
        "agentLevelParams": {
          "hostname": self.topology_cache.get_current_host_info(cluster_id)["hostName"]
        },
        "clusterName": metadata_cache.clusterLevelParams.cluster_name,
      }

      if service_name is not None and service_name != "null":
        command_dict["serviceLevelParams"] = metadata_cache.serviceLevelParams[
          service_name
        ]

      component_dict = self.topology_cache.get_component_info_by_key(
        cluster_id, service_name, component_name
      )
      if component_dict is not None:
        command_dict.update(
          {
            "componentLevelParams": component_dict.componentLevelParams,
            "commandParams": component_dict.commandParams,
          }
        )

      command_dict.update(configurations_cache)
    else:
      command_dict = {"agentLevelParams": {}}

    command_dict["ambariLevelParams"] = dict(
      self.metadata_cache.get_cluster_indepedent_data().clusterLevelParams
    )

    if cluster_id:
      self._apply_java_home_override(command_dict, service_name, component_name)

    command_dict["agentLevelParams"].update(
      {
        "public_hostname": self.public_fqdn,
        "agentCacheDir": self.config.get("agent", "cache_dir"),
      }
    )
    command_dict["agentLevelParams"]["agentConfigParams"] = {
      "agent": {
        "parallel_execution": self.config.get_parallel_exec_option(),
        "use_system_proxy_settings": self.config.use_system_proxy_setting(),
      }
    }
    return command_dict

  @staticmethod
  def _apply_java_home_override(command_dict, service_name, component_name):
    raw_overrides = (
      command_dict.get("configurations", {})
      .get("cluster-env", {})
      .get("java_home_overrides")
    )
    if not raw_overrides:
      return

    try:
      overrides = json.loads(raw_overrides)
    except (TypeError, ValueError) as exception:
      logger.warning("Ignoring invalid cluster-env/java_home_overrides: %s", exception)
      return

    if not isinstance(overrides, dict):
      logger.warning("Ignoring cluster-env/java_home_overrides because it is not a JSON object")
      return

    normalized_overrides = {
      str(key).upper(): value for key, value in overrides.items()
    }
    override_key = next(
      (
        str(name).upper()
        for name in (component_name, service_name)
        if name and str(name).upper() in normalized_overrides
      ),
      None,
    )
    if override_key is None:
      return

    override = normalized_overrides[override_key]
    if not isinstance(override, dict):
      logger.warning("Ignoring Java home override for %s because it is not an object", override_key)
      return

    java_home = override.get("home")
    java_version = override.get("version")
    if isinstance(java_version, bool) or not isinstance(java_version, (int, str)):
      java_version = None
    else:
      try:
        java_version = int(java_version)
      except ValueError:
        java_version = None

    if not isinstance(java_home, str) or not java_home.strip():
      logger.warning(
        "Ignoring Java home override for %s because both home and version are required",
        override_key,
      )
      return

    java_home = java_home.strip()
    if java_version is None or java_version < 1:
      logger.warning(
        "Ignoring Java home override for %s because both home and version are required",
        override_key,
      )
      return

    if not os.path.isfile(os.path.join(java_home, "bin", "java")):
      logger.warning(
        "Ignoring Java home override for %s because %s/bin/java is not installed",
        override_key,
        java_home,
      )
      return

    ambari_level_params = command_dict["ambariLevelParams"]
    ambari_level_params["java_home"] = java_home
    ambari_level_params["java_version"] = str(java_version)
    ambari_level_params["jdk_name"] = None
    ambari_level_params["jce_name"] = None

  @property
  def public_fqdn(self):
    return hostname.public_hostname(self.config)
