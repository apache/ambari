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

class ClusterConfiguration():
  """
  Maintains an in-memory cache and disk cache of the configurations for
  every cluster. This is useful for having quick access to any of the
  configuration properties.
  """

  FILENAME = 'configurations.json'

  # constants that define which commands hold configurations that can be
  # used to populate this cache
  EXECUTION_COMMANDS = 'executionCommands'
  ALERT_DEFINITION_COMMANDS = 'alertDefinitionCommands'
  COMMANDS_WITH_CONFIGURATIONS = [EXECUTION_COMMANDS, ALERT_DEFINITION_COMMANDS]

  def __init__(self, cluster_config_cache_dir):
    """
    Initializes the configuration cache.
    :param cluster_config_cache_dir:
    :return:
    """
    self.cluster_config_cache_dir = cluster_config_cache_dir

    # keys are cluster names, values are configurations
    self.__configurations = {}

    self.__file_lock = threading.RLock()
    self.__cache_lock = threading.RLock()
    self.__config_json_file = os.path.join(self.cluster_config_cache_dir, self.FILENAME)

    # ensure that our cache directory exists
    if not os.path.exists(cluster_config_cache_dir):
      try:
        os.makedirs(cluster_config_cache_dir)
      except:
        logger.critical("Could not create the cluster configuration cache directory {0}".format(cluster_config_cache_dir))

    # if the file exists, then load it
    try:
      if os.path.isfile(self.__config_json_file):
        with open(self.__config_json_file, 'r') as fp:
          self.__configurations = json.load(fp)
    except Exception, exception:
      logger.warning("Unable to load configurations from {0}. This file will be regenerated on registration".format(self.__config_json_file))


  def update_configurations_from_heartbeat(self, heartbeat):
    """
    Updates the in-memory and disk-based cluster configurations based on
    the heartbeat. This will only update configurations on the following
    types of commands in the heartbeat: execution, and alert definition.
    :param new_configurations:
    :return:
    """
    heartbeat_keys = heartbeat.keys()

    heartbeat_contains_configurations = False
    for commandType in self.COMMANDS_WITH_CONFIGURATIONS:
      if commandType in heartbeat_keys:
        heartbeat_contains_configurations = True

    # if this heartbeat doesn't contain a command with configurations, then
    # don't process it
    if not heartbeat_contains_configurations:
      return

    if self.EXECUTION_COMMANDS in heartbeat_keys:
      execution_commands = heartbeat[self.EXECUTION_COMMANDS]
      for command in execution_commands:
        if 'clusterName' in command and 'configurations' in command:
          cluster_name = command['clusterName']
          configurations = command['configurations']
          self._update_configurations(cluster_name, configurations)

      return

    if self.ALERT_DEFINITION_COMMANDS in heartbeat_keys:
      alert_definition_commands = heartbeat[self.ALERT_DEFINITION_COMMANDS]
      for command in alert_definition_commands:
        if 'clusterName' in command and 'configurations' in command:
          cluster_name = command['clusterName']
          configurations = command['configurations']
          self._update_configurations(cluster_name, configurations)

      return


  def _update_configurations(self, cluster_name, configuration):
    """
    Thread-safe method for writing out the specified cluster configuration
    and updating the in-memory representation.
    :param cluster_name:
    :param configuration:
    :return:
    """
    logger.info("Updating cached configurations for cluster {0}".format(cluster_name))

    self.__cache_lock.acquire()
    try:
      self.__configurations[cluster_name] = configuration
    except Exception, exception :
      logger.exception("Unable to update configurations for cluster {0}".format(cluster_name))
    finally:
      self.__cache_lock.release()


    self.__file_lock.acquire()
    try:
      with open(self.__config_json_file, 'w') as f:
        json.dump(self.__configurations, f, indent=2)
    except Exception, exception :
      logger.exception("Unable to update configurations for cluster {0}".format(cluster_name))
    finally:
      self.__file_lock.release()


  def get_configuration_value(self, cluster_name, key):
    """
    Gets a value from the cluster configuration map for the given cluster and
    key. The key is expected to be of the form 'foo-bar/baz' or
    'foo-bar/bar-baz/foobarbaz' where every / denotes a new mapping
    :param key:  a lookup key, like 'foo-bar/baz'
    :return: the value, or None if not found
    """
    self.__cache_lock.acquire()
    try:
      dictionary = self.__configurations[cluster_name]
      for layer_key in key.split('/'):
        dictionary = dictionary[layer_key]

      return dictionary

    except Exception:
      logger.debug("Cache miss for configuration property {0} in cluster {1}".format(key, cluster_name))
      return None
    finally:
      self.__cache_lock.release()
