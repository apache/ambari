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

__all__ = [
  "should_install_phoenix",
  "should_install_infra_solr",
  "should_install_infra_solr_client",
  "should_install_mysql",
  "should_install_ranger_hbase_plugin",
  "should_install_ranger_hdfs_plugin",
  "should_install_ranger_hive_plugin",
  "should_install_ranger_kafka_plugin",
  "should_install_ranger_yarn_plugin",
  "should_install_ranger_tagsync",
  "should_install_yarn_ats_hbase",
]

from resource_management.core.exceptions import Fail
from resource_management.libraries.script import Script
from resource_management.libraries.functions.default import default


def _has_local_components(config, components, indicator_function=any):
  if "role" not in config:
    return False
  if config["role"] == "install_packages":
    # Stack upgrades install every package on hosts running install_packages.
    if "localComponents" not in config:
      return False
    return indicator_function(
      [component in config["localComponents"] for component in components]
    )
  else:
    return config["role"] in components


def _has_applicable_local_component(config, components):
  return _has_local_components(config, components, any)


def _strict_configuration_boolean(config, config_type, property_name):
  try:
    value = config["configurations"][config_type][property_name]
  except KeyError as error:
    raise Fail(f"Missing {config_type}/{property_name} package condition") from error
  if isinstance(value, bool):
    return value
  if isinstance(value, str):
    normalized = value.strip().lower()
    if normalized == "true":
      return True
    if normalized == "false":
      return False
  raise Fail(f"{config_type}/{property_name} must be true or false")


def should_install_phoenix():
  config = Script.get_config()
  configurations = config.get("configurations", {})
  if not isinstance(configurations, dict):
    raise Fail("Phoenix package condition requires a configurations map")
  hbase_env = configurations.get("hbase-env")
  if hbase_env is None:
    return False
  if not isinstance(hbase_env, dict):
    raise Fail("hbase-env package condition must be a configuration map")
  if "phoenix_sql_enabled" not in hbase_env:
    return False
  return _strict_configuration_boolean(config, "hbase-env", "phoenix_sql_enabled")


def should_install_yarn_ats_hbase():
  config = Script.get_config()
  config_type = "yarn-hbase-env"
  backend_config = config.get("configurations", {}).get(config_type)
  if backend_config is None:
    return _has_applicable_local_component(
      config, ["RESOURCEMANAGER", "TIMELINE_READER"]
    )
  if not isinstance(backend_config, dict):
    raise Fail(f"{config_type} package condition must be a configuration map")
  system_service = _strict_configuration_boolean(
    config, config_type, "is_hbase_system_service_launch"
  )
  use_external_hbase = _strict_configuration_boolean(
    config, config_type, "use_external_hbase"
  )
  hbase_within_cluster = _strict_configuration_boolean(
    config, config_type, "hbase_within_cluster"
  )
  if use_external_hbase or hbase_within_cluster:
    return False
  components = ["RESOURCEMANAGER"] if system_service else ["TIMELINE_READER"]
  return _has_applicable_local_component(config, components)


def should_install_infra_solr():
  config = Script.get_config()
  return _has_applicable_local_component(config, ["INFRA_SOLR"])


def should_install_infra_solr_client():
  config = Script.get_config()
  return _has_applicable_local_component(
    config, ["INFRA_SOLR_CLIENT", "RANGER_ADMIN"]
  )


def should_install_mysql():
  config = Script.get_config()
  hive_database = config["configurations"]["hive-env"]["hive_database"]
  hive_use_existing_db = hive_database.startswith("Existing")

  if hive_use_existing_db:
    return False
  return _has_applicable_local_component(config, "MYSQL_SERVER")


def should_install_mysql_connector():
  config = Script.get_config()
  hive_database = config["configurations"]["hive-env"]["hive_database"]
  hive_use_existing_db = hive_database.startswith("Existing")

  if hive_use_existing_db:
    return False
  return _has_applicable_local_component(
    config, ["MYSQL_SERVER", "HIVE_METASTORE", "HIVE_SERVER", "HIVE_SERVER_INTERACTIVE"]
  )


def _is_configuration_enabled(config_type, property_name):
  value = default(f"/configurations/{config_type}/{property_name}", "No")
  if isinstance(value, bool):
    return value
  if isinstance(value, str):
    normalized = value.strip().lower()
    if normalized == "yes":
      return True
    if normalized == "no":
      return False
  raise Fail(f"{config_type}/{property_name} must be Yes or No")


def should_install_ranger_hdfs_plugin():
  return _is_configuration_enabled(
    "ranger-hdfs-plugin-properties", "ranger-hdfs-plugin-enabled"
  )


def should_install_ranger_yarn_plugin():
  return _is_configuration_enabled(
    "ranger-yarn-plugin-properties", "ranger-yarn-plugin-enabled"
  )


def should_install_ranger_hive_plugin():
  return _is_configuration_enabled(
    "ranger-hive-plugin-properties", "ranger-hive-plugin-enabled"
  )


def should_install_ranger_hbase_plugin():
  return _is_configuration_enabled(
    "ranger-hbase-plugin-properties", "ranger-hbase-plugin-enabled"
  )


def should_install_ranger_kafka_plugin():
  return _is_configuration_enabled(
    "ranger-kafka-plugin-properties", "ranger-kafka-plugin-enabled"
  )


def should_install_ranger_tagsync():
  config = Script.get_config()
  ranger_tagsync_hosts = default("/clusterHostInfo/ranger_tagsync_hosts", [])
  has_ranger_tagsync = len(ranger_tagsync_hosts) > 0

  return has_ranger_tagsync


def should_install_rpcbind():
  config = Script.get_config()
  return _has_applicable_local_component(config, ["NFS_GATEWAY"])
