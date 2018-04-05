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

__all__ = ["ExecutionCommand"]

import ambari_simplejson

from resource_management.libraries.execution_command import module_configs


class ExecutionCommand(object):
  """
  The class maps to a command.json. All command related info should be retrieved from this class object
  """

  def __init__(self, command):
    """
    _execution_command is an internal dict object maps to command.json
    :param command: json string or a python dict object
    """
    self._execution_command = command
    self._module_configs = module_configs.ModuleConfigs(self.__get_value("configurations"))

  def __get_value(self, key, default_value=None):
    """
    A private method to query value with the full path of key, if key does not exist, return default_value
    :param key:
    :param default_value:
    :return:
    """
    sub_keys = key.split('/')
    value = self._execution_command
    try:
      for sub_key in sub_keys:
        value = value[sub_key]
      return value
    except:
      return default_value

  def query_config_attri_directly(self, query_string, default_value=None):
    """
    Query config attribute from execution_command directly, i.e "clusterHostInfo/zookeeper_server_hosts"
    :param query_string: full query key string
    :param default_value: if key does not exist, return default value
    :return: config attribute
    """
    return self.__get_value(query_string, default_value)

  """
  Global variables section
  """

  def get_module_configs(self):
    return self._module_configs

  def get_module_name(self):
    return self.__get_value("serviceName")

  def get_component_type(self):
    return self.__get_value("role", "")

  def get_component_instance_name(self):
    """
    At this time it returns hardcoded 'default' name
    :return:
    """
    return "default"

  def get_servicegroup_name(self):
    return self.__get_value("serviceGroupName")

  def get_cluster_name(self):
    return self.__get_value("clusterName")

  """
  Ambari variables section
  """

  def get_jdk_location(self):
    return self.__get_value("ambariLevelParams/jdk_location")

  def get_jdk_name(self):
    return self.__get_value("ambariLevelParams/jdk_name")

  def get_java_home(self):
    return self.__get_value("ambariLevelParams/java_home")

  def get_java_version(self):
    return int(self.__get_value("ambariLevelParams/java_version"))

  def get_jce_name(self):
    return self.__get_value("ambariLevelParams/jce_name")

  def get_db_driver_file_name(self):
    return self.__get_value('ambariLevelParams/db_driver_filename')

  def get_db_name(self):
    return self.__get_value('ambariLevelParams/db_name')

  def get_oracle_jdbc_url(self):
    return self.__get_value('ambariLevelParams/oracle_jdbc_url')

  def get_mysql_jdbc_url(self):
    return self.__get_value('ambariLevelParams/mysql_jdbc_url')

  def get_agent_stack_retry_count_on_unavailability(self):
    return self.__get_value('ambariLevelParams/agent_stack_retry_count', 5)

  def check_agent_stack_want_retry_on_unavailability(self):
    return self.__get_value('ambariLevelParams/agent_stack_retry_on_unavailability')

  def get_ambari_server_host(self):
    return self.__get_value("ambariLevelParams/ambari_server_host")

  def get_ambari_server_port(self):
    return self.__get_value("ambariLevelParams/ambari_server_port")

  def is_ambari_server_use_ssl(self):
    return self.__get_value("ambariLevelParams/ambari_server_use_ssl", False)

  def is_host_system_prepared(self):
    return self.__get_value("ambariLevelParams/host_sys_prepped", False)

  def is_gpl_license_accepted(self):
    return self.__get_value("ambariLevelParams/gpl_license_accepted", False)

  """
  Cluster related variables section
  """

  def get_mpack_name(self):
    return self.__get_value("clusterLevelParams/stack_name")

  def get_mpack_version(self):
    return self.__get_value("clusterLevelParams/stack_version")

  def get_user_groups(self):
    return self.__get_value("clusterLevelParams/user_groups")

  def get_group_list(self):
    return self.__get_value("clusterLevelParams/group_list")

  def get_user_list(self):
    return self.__get_value("clusterLevelParams/user_list")

  """
  Agent related variable section
  """

  def get_host_name(self):
    return self.__get_value("agentLevelParams/hostname")

  def check_agent_config_execute_in_parallel(self):
    return int(self.__get_value("agentConfigParams/agent/parallel_execution", 0))

  def get_agent_cache_dir(self):
    return self.__get_value('agentLevelParams/agentCacheDir')

  """
  Host related variables section
  """

  def get_repo_info(self):
    return self.__get_value('hostLevelParams/repoInfo')

  def get_service_repo_info(self):
    return self.__get_value('hostLevelParams/service_repo_info')

  """
  Component related variables section
  """

  def check_unlimited_key_jce_required(self):
    return self.__get_value('componentLevelParams/unlimited_key_jce_required', False)

  """
  Command related variables section
  """

  def get_new_mpack_version_for_upgrade(self):
    """
    New Cluster Stack Version that is defined during the RESTART of a Rolling Upgrade
    :return:
    """
    return self.__get_value("commandParams/version")

  def check_command_retry_enabled(self):
    return self.__get_value('commandParams/command_retry_enabled', False)

  def check_upgrade_direction(self):
    return self.__get_value('commandParams/upgrade_direction')

  def get_upgrade_type(self):
    return self.__get_value('commandParams/upgrade_type', '')

  def is_rolling_restart_in_upgrade(self):
    return self.__get_value('commandParams/rolling_restart', False)

  def is_update_files_only(self):
    return self.__get_value('commandParams/update_files_only', False)

  def get_deploy_phase(self):
    return self.__get_value('commandParams/phase', '')

  def get_dfs_type(self):
    return self.__get_value('commandParams/dfs_type', '')

  def get_module_package_folder(self):
    return self.__get_value('commandParams/service_package_folder')

  def get_ambari_java_home(self):
    return self.__get_value('commandParams/ambari_java_home')

  def get_ambari_java_name(self):
    return self.__get_value('commandParams/ambari_java_name')

  def get_ambari_jce_name(self):
    return self.__get_value('commandParams/ambari_jce_name')

  def get_ambari_jdk_name(self):
    return self.__get_value('commandParams/ambari_jdk_name')

  def need_refresh_topology(self):
    return self.__get_value('commandParams/refresh_topology', False)

  def check_only_update_files(self):
    return self.__get_value('commandParams/update_files_only', False)


  """
  Role related variables section
  """

  def is_upgrade_suspended(self):
    return self.__get_value('roleParams/upgrade_suspended', False)