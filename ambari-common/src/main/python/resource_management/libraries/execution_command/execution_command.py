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
    __execution_command is an internal dot access dict object maps to command.json
    :param command: json string or a python dict object
    """
    self.__execution_command = command
    self.__module_configs = module_configs.ModuleConfigs(self.__get_value("configurations"))

  def __get_value(self, key, default_value=None):
    """
    A private method to query value with the full path of key, if key does not exist, return default_value
    :param key:
    :param default_value:
    :return:
    """
    sub_keys = key.split('/')
    value = self.__execution_command
    try:
      for sub_key in sub_keys:
        value = value[sub_key]
      return value
    except:
      return default_value

  def get_module_name(self):
    return self.__get_value("serviceName")

  def get_stack_name(self):
    return self.__get_value("hostLevelParams/stack_name")

  def get_stack_version(self):
    return self.__get_value("hostLevelParams/stack_version")

  def get_new_stack_version_for_upgrade(self):
    """
    New Cluster Stack Version that is defined during the RESTART of a Rolling Upgrade
    :return:
    """
    return self.__get_value("commandParams/version")

  def get_host_name(self):
    return self.__get_value("hostname")

  def get_java_home(self):
    return self.__get_value("hostLevelParams/java_home")

  def get_java_version(self):
    java_version = self.__get_value("hostLevelParams/java_version")
    return int(java_version) if java_version else None

  def get_module_configs(self):
    return self.__module_configs

