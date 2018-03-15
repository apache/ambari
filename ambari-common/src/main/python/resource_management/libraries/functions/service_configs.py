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

"""

"""

import json
from collections import namedtuple

from decorator import null_key_exception

class ServiceConfigs:

  """
  Two private helper functions used to convert nested json string to python object
  """
  def __object_hook(self, node):
    return namedtuple('dict', node.keys())(*node.values())

  def __get_jsonObject(self, data):
    if isinstance(data, file):
      return json.load(data, object_hook=self.__object_hook)
    else:
      return json.loads(data, object_hook=self.__object_hook)

  def __init__(self, config):
    """
    __service_config is an internal python dict object to store configuration info
    :param config: The construct accepts three types of arguments: json string, json file handle
    and a dict python object
    """
    if config.__class__.__name__ == "dict":
      self.__service_config = config
    else:
      self.__service_config = self.__get_jsonObject(config)

  def value(self):
    """
    Sometimes user wants to get raw python dict object from config_object instance, and then access
    to it with key
    :return: python dict object
    """
    return self.__service_config

  def get_config_object(self, key, default_value=None):
    """
    Query config object to get property value or sub config object with key, if  default_value is provided and
    no key is found, default_value is returned
    :param key: a string represents a query key such as 'service_name/config_type/property_name'
    :param default_value: expected returned object or value if no key exists
    :return: a sub config object or a property value
    """
    sub_keys = key.split('/')
    config_obj = self.__service_config
    try:
      for sub_key in sub_keys:
        config_obj = config_obj.sub_key
      """
      If the query result is dict object, it is returned as wrapped ServiceConfigs instance so the user can use it to 
      do further query with new key, or the user can invoke value() to get the raw dict object. Otherwise, the query 
      result should be the value of primitive type (i.e string, int bool etc)
      """
      if config_obj.__class__.__name__ == "dict":
        return ServiceConfigs(config_obj)
      else:
        return config_obj
    except:
      return default_value

  @null_key_exception
  def get_service_config_property_value(self, service_name, config_type, property_name, default_value=None):
    """
    Query property value based on service_name/config_type/property_name, but now the service specific configuration
    is located in configurations block, so service_name is not used yet
    :param service_name: This is used in multiple service instances situation
    :param config_type:
    :param property_name:
    :param default_value: If no config_type or property_name is found in queried service, default_value is returned
    :return:
    """
    key = '/'.join('configurations', config_type, property_name)
    return self.get_config_objec(key, default_value)

  def get_service_config_property_entries(self, service_name, config_type, property_names):
    """
    Query a list of property_names and return the corresponding property value list
    :param service_name: This is used in multiple service instances situation
    :param config_type:
    :param property_names:
    :return: A list of config values based on property_names
    """
    return map(lambda property_name: self.get_service_config_property_value(service_name, config_type, property_name))

  """
  ======================================================================================================================
  The following are a set of utilities functions to query service independent configuration information. The user does 
  not have to know the detailed query path for a specific configuration setting.
  Also it is more convenient for the developer to update the configuration settings in the future
  ======================================================================================================================
  """

  @null_key_exception
  def get_repo_file(self):
    """
    repo_file is always a dict object in command.json
    :return:
    """
    repo_file = self.get_config_object('repositoryFile')
    return repo_file.value() if repo_file else None

  @null_key_exception
  def check_repo_resolved(self, default_value):
    return self.get_config_object('repositoryFile/resolved', default_value)

  @null_key_exception
  def get_repo_version(self):
    return self.get_config_object('repositoryFile/repoVersion')

  @null_key_exception
  def get_repo_version_id(self):
    return self.get_config_object('hostLevelParams/repository_version_id')

  @null_key_exception
  def get_stack_name_from_cluster_settings(self, default_name):
    return self.get_config_object('configurations/clusterSettings/stack_name', default_name)

  @null_key_exception
  def get_stack_name_from_stack_settings(self, default_name):
    return self.get_config_object('configurations/stackSettings/stack_name', default_name)

  @null_key_exception
  def get_stack_name_from_host_level(self, default_name):
    return self.get_config_object('configurations/hostLevelParams/stack_name', default_name)

  @null_key_exception
  def get_command_role(self, default_role):
    return self.get_config_object('role', default_role)

  @null_key_exception
  def get_request_version(self):
    return self.get_config_object('commandParams/request_version')

  @null_key_exception
  def get_stack_root(self):
    return self.get_config_object('configurations/clusterSettings/stack_root')

  @null_key_exception(result='')
  def get_stack_version(self):
    return self.get_config_object('hostLevelParams/stack_version')

  @null_key_exception
  def get_host_level_params(self):
    return self.get_config_object('hostLevelParams')

  @null_key_exception
  def get_upgrade_direction(self):
    return self.get_config_object('commandParams/upgrade_direction')

  @null_key_exception
  def get_host_sys_preppend(self):
    return self.get_config_object('hostLevelParams/host_sys_prepped')

  @null_key_exception
  def get_package_list(self):
    return self.get_config_object('hostLevelParams/package_list')

  @null_key_exception
  def check_agent_stack_retry_on_unavailability(self):
    return bool(self.get_config_object('hostLevelParams/agent_stack_retry_on_unavailability'))

  @null_key_exception
  def get_agent_stack_retry_count(self):
    return int(self.get_config_object('hostLevelParams/agent_stack_retry_count'))

  @null_key_exception
  def get_component_category(self):
    return self.get_config_object('roleParams/component_category')

  @null_key_exception
  def get_command_params(self):
    return self.get_config_object('commandParams')

  @null_key_exception
  def get_upgrade_type(self):
    return self.get_config_object('commandParams/upgrade_type')

  @null_key_exception
  def get_upgrade_direction(self):
    return self.get_config_object('commandParams/upgrade_direction')

  @null_key_exception
  def get_service_name(self):
    return self.get_config_object('serviceName')

  @null_key_exception
  def get_reconfigure_action(self):
    return self.get_config_object('commandParams/reconfigureAction')

  @null_key_exception
  def get_xml_configs_list(self):
    return self.get_config_object('commandParams/xml_configs_list')

  @null_key_exception
  def get_env_configs_list(self):
    return self.get_config_object('commandParams/env_configs_list')

  @null_key_exception
  def get_properties_configs_list(self):
    return self.get_config_object('commandParams/properties_configs_list')

  @null_key_exception
  def get_output_configs_file_name(self):
    return self.get_config_object('commandParams/output_file')

  @null_key_exception
  def get_host_name(self):
    return self.get_config_object('hostname')

  @null_key_exception
  def check_cluster_security_enabled(self):
    return self.get_config_object('configurations/clusterSettings/security_enabled')

  @null_key_exception
  def get_kinit_path(self):
    return self.get_config_object('configurations/kerberos-env/executable_search_paths')

  @null_key_exception
  def get_host_java_home(self):
    return self.get_config_object('hostLevelParams/java_home')

  @null_key_exception
  def get_host_java_version(self):
    return self.get_config_object('hostLevelParams/java_version')

  @null_key_exception
  def get_smokeuser_keytab_path(self):
    return self.get_config_object('configurations/cluster-settings/smokeuser_keytab')

  @null_key_exception
  def get_smokeuser(self):
    return self.get_config_object('configurations/cluster-settings/smokeuser')

  @null_key_exception
  def get_smokeuser_principal_name(self):
    return self.get_config_object('configurations/cluster-settings/smokeuser_principal_name')

  @null_key_exception
  def check_agent_parallel_execution(self, default_value):
    return self.get_config_object('agentConfigParams/agent/parallel_execution', default_value)

  @null_key_exception
  def get_package_version_from_role_params(self):
    return self.get_config_object('roleParams/package_version')

  @null_key_exception
  def get_package_version_from_host_level_params(self):
    return self.get_config_object('hostLevelParams/package_version')

  @null_key_exception
  def get_role_command(self):
    return self.get_config_object('roleCommand')

  @null_key_exception
  def get_effective_version(self):
    return self.get_config_object('commandParams/version')

  @null_key_exception
  def get_hadoop_user_name(self):
    return self.get_config_object('configurations/cluster-settings/hadoop.user.name')

  @null_key_exception
  def check_agent_use_system_proxy_settings(self, default_value):
    return self.get_config_object('agentConfigParams/agent/use_system_proxy_settings', default_value)
