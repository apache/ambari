#!/usr/bin/env ambari-python-wrap
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

import math
import traceback

from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail

class HDP25StackAdvisor(HDP24StackAdvisor):

  def __init__(self):
    super(HDP25StackAdvisor, self).__init__()
    Logger.initialize_logger()
    self.HIVE_INTERACTIVE_SITE = 'hive-interactive-site'

  def createComponentLayoutRecommendations(self, services, hosts):
    parentComponentLayoutRecommendations = super(HDP25StackAdvisor, self).createComponentLayoutRecommendations(
      services, hosts)
    return parentComponentLayoutRecommendations

  def getComponentLayoutValidations(self, services, hosts):
    parentItems = super(HDP25StackAdvisor, self).getComponentLayoutValidations(services, hosts)
    childItems = []

    hsi_hosts = self.__getHostsForComponent(services, "HIVE", "HIVE_SERVER_INTERACTIVE")
    if len(hsi_hosts) > 1:
      message = "Only one host can install HIVE_SERVER_INTERACTIVE. "
      childItems.append(
        {"type": 'host-component', "level": 'ERROR', "message": message, "component-name": 'HIVE_SERVER_INTERACTIVE'})

    parentItems.extend(childItems)
    return parentItems

  def getServiceConfigurationValidators(self):
    parentValidators = super(HDP25StackAdvisor, self).getServiceConfigurationValidators()
    childValidators = {
      "HIVE": {"hive-interactive-env": self.validateHiveInteractiveEnvConfigurations},
      "YARN": {"yarn-site": self.validateYarnConfigurations},
      "RANGER": {"ranger-tagsync-site": self.validateRangerTagsyncConfigurations}
    }
    self.mergeValidators(parentValidators, childValidators)
    return parentValidators

  def validateYarnConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    parentValidationProblems = super(HDP25StackAdvisor, self).validateYARNConfigurations(properties, recommendedDefaults, configurations, services, hosts)
    yarn_site_properties = getSiteProperties(configurations, "yarn-site")
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item["StackServiceComponents"] for sublist in componentsListList for item in sublist]
    validationItems = []

    hsi_hosts = self.__getHostsForComponent(services, "HIVE", "HIVE_SERVER_INTERACTIVE")
    if len(hsi_hosts) > 0:
      # HIVE_SERVER_INTERACTIVE is mapped to a host
      if 'yarn.resourcemanager.work-preserving-recovery.enabled' not in yarn_site_properties or \
              'true' != yarn_site_properties['yarn.resourcemanager.work-preserving-recovery.enabled']:
        validationItems.append({"config-name": "yarn.resourcemanager.work-preserving-recovery.enabled",
                                    "item": self.getWarnItem(
                                      "While enabling HIVE_SERVER_INTERACTIVE it is recommended that you enable work preserving restart in YARN.")})

    validationProblems = self.toConfigurationValidationProblems(validationItems, "yarn-site")
    validationProblems.extend(parentValidationProblems)
    return validationProblems

  def validateHiveInteractiveEnvConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    hive_site_env_properties = getSiteProperties(configurations, "hive-interactive-env")
    validationItems = []
    hsi_hosts = self.__getHostsForComponent(services, "HIVE", "HIVE_SERVER_INTERACTIVE")
    if len(hsi_hosts) > 0:
      # HIVE_SERVER_INTERACTIVE is mapped to a host
      if 'enable_hive_interactive' not in hive_site_env_properties or (
            'enable_hive_interactive' in hive_site_env_properties and hive_site_env_properties[
          'enable_hive_interactive'].lower() != 'true'):
        validationItems.append({"config-name": "enable_hive_interactive",
                                "item": self.getErrorItem(
                                  "HIVE_SERVER_INTERACTIVE requires enable_hive_interactive in hive-interactive-env set to true.")})
      if 'hive_server_interactive_host' in hive_site_env_properties:
        hsi_host = hsi_hosts[0]
        if hive_site_env_properties['hive_server_interactive_host'].lower() != hsi_host.lower():
          validationItems.append({"config-name": "hive_server_interactive_host",
                                  "item": self.getErrorItem(
                                    "HIVE_SERVER_INTERACTIVE requires hive_server_interactive_host in hive-interactive-env set to its host name.")})
        pass
      if 'hive_server_interactive_host' not in hive_site_env_properties:
        validationItems.append({"config-name": "hive_server_interactive_host",
                                "item": self.getErrorItem(
                                  "HIVE_SERVER_INTERACTIVE requires hive_server_interactive_host in hive-interactive-env set to its host name.")})
        pass

    else:
      # no  HIVE_SERVER_INTERACTIVE
      if 'enable_hive_interactive' in hive_site_env_properties and hive_site_env_properties[
        'enable_hive_interactive'].lower() != 'false':
        validationItems.append({"config-name": "enable_hive_interactive",
                                "item": self.getErrorItem(
                                  "enable_hive_interactive in hive-interactive-env should be set to false.")})
        pass
      pass

    validationProblems = self.toConfigurationValidationProblems(validationItems, "hive-interactive-env")
    return validationProblems

  def getServiceConfigurationRecommenderDict(self):
    parentRecommendConfDict = super(HDP25StackAdvisor, self).getServiceConfigurationRecommenderDict()
    childRecommendConfDict = {
      "RANGER": self.recommendRangerConfigurations,
      "HIVE": self.recommendHIVEConfigurations,
      "ATLAS": self.recommendAtlasConfigurations
    }
    parentRecommendConfDict.update(childRecommendConfDict)
    return parentRecommendConfDict

  def recommendAtlasConfigurations(self, configurations, clusterData, services, hosts):
    putAtlasApplicationProperty = self.putProperty(configurations, "application-properties", services)

    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

    include_logsearch = "LOGSEARCH" in servicesList
    if include_logsearch:
      logsearch_solr_znode = services["configurations"]["logsearch-solr-env"]["properties"]['logsearch_solr_znode']
      putAtlasApplicationProperty('atlas.graph.index.search.solr.zookeeper-url', '{{zookeeper_quorum}}' + logsearch_solr_znode)

  def recommendHIVEConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP25StackAdvisor, self).recommendHIVEConfigurations(configurations, clusterData, services, hosts)
    putHiveInteractiveEnvProperty = self.putProperty(configurations, "hive-interactive-env", services)

    # For 'Hive Server Interactive', if the component exists.
    hsi_hosts = self.__getHostsForComponent(services, "HIVE", "HIVE_SERVER_INTERACTIVE")
    if len(hsi_hosts) > 0:
      hsi_host = hsi_hosts[0]
      putHiveInteractiveEnvProperty('enable_hive_interactive', 'true')
      putHiveInteractiveEnvProperty('hive_server_interactive_host', hsi_host)

      if 'hive.llap.zk.sm.connectionString' in services['configurations'][self.HIVE_INTERACTIVE_SITE]['properties']:
        # Fill the property 'hive.llap.zk.sm.connectionString' required by Hive Server Interactive (HiveServer2)
        zookeeper_host_port = self.getZKHostPortString(services)
        if zookeeper_host_port:
          putHiveInteractiveSiteProperty = self.putProperty(configurations, self.HIVE_INTERACTIVE_SITE, services)
          putHiveInteractiveSiteProperty("hive.llap.zk.sm.connectionString", zookeeper_host_port)

      # Update 'hive.llap.daemon.queue.name' if capacity scheduler is changed.
      if 'hive.llap.daemon.queue.name' in services['configurations'][self.HIVE_INTERACTIVE_SITE]['properties']:
        self.setLlapDaemonQueueName(services, configurations)

      # Check to see if 'cache' config HS2 'hive.llap.io.memory.size' has been modified by user.
      # 'cache' size >= 64m implies config 'hive.llap.io.enabled' set to true, else false
      cache_size_per_node_in_changed_configs = self.are_config_props_in_changed_configs(services,
                                                                                        "hive-interactive-site",
                                                                                        set(['hive.llap.io.memory.size']),
                                                                                        False)
      if cache_size_per_node_in_changed_configs:
        cache_size_per_node = self.get_cache_size_per_node_for_llap_nodes(services)
        llap_io_enabled = 'false'
        if cache_size_per_node >= 64:
          llap_io_enabled = 'true'
        putHiveInteractiveSiteProperty('hive.llap.io.enabled', llap_io_enabled)
        Logger.info("Updated 'Hive Server interactive' config 'hive.llap.io.enabled' to '{0}'.".format(llap_io_enabled))
    else:
      putHiveInteractiveEnvProperty('enable_hive_interactive', 'false')
    pass



  def recommendYARNConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP25StackAdvisor, self).recommendYARNConfigurations(configurations, clusterData, services, hosts)

    # Queue 'llap' creation/removal logic (Used by Hive Interactive server and associated LLAP)
    if 'hive-interactive-env' in services['configurations'] and \
        'enable_hive_interactive' in services['configurations']['hive-interactive-env']['properties']:
      enable_hive_interactive = services['configurations']['hive-interactive-env']['properties']['enable_hive_interactive']
      LLAP_QUEUE_NAME = 'llap'

      # Hive Server interactive is already added or getting added
      if enable_hive_interactive == 'true':
        self.checkAndManageLlapQueue(services, configurations, hosts, LLAP_QUEUE_NAME)
        self.updateLlapConfigs(configurations, services, hosts, LLAP_QUEUE_NAME)
      else:  # When Hive Interactive Server is in 'off/removed' state.
        self.checkAndStopLlapQueue(services, configurations, LLAP_QUEUE_NAME)

  """
  Entry point for updating Hive's 'LLAP app' configs namely : (1). num_llap_nodes (2). hive.llap.daemon.yarn.container.mb
    (3). hive.llap.daemon.num.executors (4). hive.llap.io.memory.size (5). llap_heap_size (6). slider_am_container_mb.

    The trigger point for updating LLAP configs (mentioned above) is change in values of :
    'llap_queue_capacity' or 'num_llap_nodes' or 'llap' named queue get selected for config 'hive.llap.daemon.queue.name'.

    'llap_queue_capacity', 'hive.llap.daemon.queue.name' : Change detection for this property triggers change for all
     the configs mentioned.

    'num_llap_nodes' : If there is a change in value for 'num_llap_nodes', it's value is not updated/calulated,
     but other dependent configs get calculated.
  """
  def updateLlapConfigs(self, configurations, services, hosts, llap_queue_name):
    putHiveInteractiveSiteProperty = self.putProperty(configurations, self.HIVE_INTERACTIVE_SITE, services)
    putHiveInteractiveSitePropertyAttribute = self.putPropertyAttribute(configurations, self.HIVE_INTERACTIVE_SITE)

    putHiveInteractiveEnvProperty = self.putProperty(configurations, "hive-interactive-env", services)
    putHiveInteractiveEnvPropertyAttribute = self.putPropertyAttribute(configurations, "hive-interactive-env")

    num_llap_nodes_in_changed_configs = False
    llap_daemon_selected_queue_name = None
    llap_queue_selected_in_current_call = None
    cache_size_per_node_in_changed_configs = False

    try:
      if self.HIVE_INTERACTIVE_SITE in services['configurations'] and \
          'hive.llap.daemon.queue.name' in services['configurations'][self.HIVE_INTERACTIVE_SITE]['properties']:
        llap_daemon_selected_queue_name =  services['configurations'][self.HIVE_INTERACTIVE_SITE]['properties']['hive.llap.daemon.queue.name']

      if 'hive.llap.daemon.queue.name' in configurations[self.HIVE_INTERACTIVE_SITE]['properties']:
        llap_queue_selected_in_current_call = configurations[self.HIVE_INTERACTIVE_SITE]['properties']['hive.llap.daemon.queue.name']

      # Update Visibility of LLAP configs.
      capacitySchedulerProperties = self.getCapacitySchedulerProperties(services)
      if capacitySchedulerProperties:
        # Get all leaf queues.
        leafQueueNames = self.getAllYarnLeafQueues(capacitySchedulerProperties)
        if (llap_daemon_selected_queue_name != None and llap_daemon_selected_queue_name == llap_queue_name) or \
          (llap_queue_selected_in_current_call != None and llap_queue_selected_in_current_call == llap_queue_name):
            putHiveInteractiveEnvPropertyAttribute("llap_queue_capacity", "visible", "true")
            Logger.debug("Selected YARN queue is '{0}'. Setting LLAP queue capacity slider visibility to True".format(llap_queue_name))
        else:
          putHiveInteractiveEnvPropertyAttribute("llap_queue_capacity", "visible", "false")
          Logger.debug("Queue selected for LLAP app is : '{0}'. Current YARN queues : {1}. "
                    "Setting LLAP queue capacity slider visibility to False. "
                    "Skipping updating values for LLAP related configs".format(llap_daemon_selected_queue_name, list(leafQueueNames)))
          return
      else:
        Logger.error("Couldn't retrieve 'capacity-scheduler' properties while doing YARN queue adjustment for Hive Server Interactive."
                     " Not calculating LLAP configs.")
        return
      # Won't be calculating values if queues not equal to 2 and queue in use is not llap
      if len(leafQueueNames) > 2 or (len(leafQueueNames) == 1 and llap_queue_selected_in_current_call != llap_queue_name):
        return

      # 'llap' queue exists at this point.
      if 'changed-configurations' in services.keys():
        llap_queue_prop_old_val = None

        # Calculations are triggered only if there is change in following props : 'llap_queue_capacity' or 'enable_hive_interactive',
        # 'num_llap_nodes' or hive.llap.daemon.queue.name has change in value and selection is 'llap'
        config_names_to_be_checked = set(['llap_queue_capacity', 'enable_hive_interactive', 'num_llap_nodes'])
        changed_configs_in_hive_int_env = self.are_config_props_in_changed_configs(services, "hive-interactive-env",
                                                                                   config_names_to_be_checked, False)

        llap_queue_prop_in_changed_configs = self.are_config_props_in_changed_configs(services, self.HIVE_INTERACTIVE_SITE,
                                                                                      set(['hive.llap.daemon.queue.name']), False)

        if not changed_configs_in_hive_int_env and not llap_queue_prop_in_changed_configs:
          Logger.info("LLAP parameters not modified. Not adjusting LLAP configs. "
                       "Current changed-configuration received is : {0}".format(services["changed-configurations"]))
          return

        node_manager_hosts = self.get_node_manager_hosts(services, hosts)
        node_manager_host_cnt = len(node_manager_hosts)

        # If changed configurations contains 'num_llap_nodes' prop, we don't calulate it and use the same value.
        num_llap_nodes_in_changed_configs = self.are_config_props_in_changed_configs(services, "hive-interactive-env",
                                                                                     set(['num_llap_nodes']), False)

        # Get value for prop 'num_llap_nodes'.
        if not num_llap_nodes_in_changed_configs:
          num_llap_nodes, num_llap_nodes_max_limit = self.calculate_num_llap_nodes(services, hosts, configurations)
        else:
          num_llap_nodes = self.get_num_llap_nodes(services)
          num_llap_nodes_max_limit = node_manager_host_cnt

        # Get calculated value for prop 'hive.llap.daemon.yarn.container.mb'
        llap_container_size, llap_container_size_min_limit = self.calculate_llap_app_container_size(services, hosts, configurations)

        # Get calculated value for prop 'hive.llap.daemon.num.executors'
        num_executors_per_node, num_executors_per_node_max_limit = self.calculate_llap_daemon_executors_count(services,
                                                                                                              llap_container_size)
        assert (num_executors_per_node >= 0), "'Number of executors per node' : {0}. Expected value : > 0".format(
          num_executors_per_node)


        # We calculate and update 'cache' value only if user has not modified it.
        cache_size_per_node_in_changed_configs = self.are_config_props_in_changed_configs(services,
                                                                                  "hive-interactive-site",
                                                                                  set(['hive.llap.io.memory.size']),
                                                                                  False)
        if not cache_size_per_node_in_changed_configs:
          # Get calculated value for prop 'hive.llap.io.memory.size'
          cache_size_per_node, cache_size_per_node_max_limit = self.calculate_llap_cache_size_per_executor(services,
                                                                                                 llap_container_size,
                                                                                                 num_executors_per_node)
          if cache_size_per_node < 0:
            Logger.info("Calculated 'cache_size_per_node' : {0}. Setting 'cache_size_per_node' to 0.".format(cache_size_per_node))
            cache_size_per_node = 0
          if cache_size_per_node_max_limit < 0:
            Logger.info("Calculated 'cache_size_per_node_max_limit' : {0}. Setting 'cache_size_per_node_max_limit' to "
                        "0.".format(cache_size_per_node_max_limit))
            cache_size_per_node_max_limit = 0

        # Get calculated value for prop 'llap_heap_size'
        llap_xmx = self.calculate_llap_app_heap_size(services, num_executors_per_node)

        # Get calculated Slider AM container Size
        yarn_min_container_size = self.get_yarn_min_container_size(services)
        slider_am_container_mb = self.calculate_slider_am_size(yarn_min_container_size)


        # Updating configs.
        if not num_llap_nodes_in_changed_configs:
          num_llap_nodes = long(num_llap_nodes)
          num_llap_nodes_max_limit= long(num_llap_nodes_max_limit)

          Logger.info("LLAP config 'num_llap_nodes' updated. Min : {0}, Curr: {1}, Max: {2}" \
                      .format('1', num_llap_nodes, num_llap_nodes_max_limit))
          putHiveInteractiveEnvProperty('num_llap_nodes', num_llap_nodes)
          putHiveInteractiveEnvPropertyAttribute('num_llap_nodes', "minimum", 1)
          putHiveInteractiveEnvPropertyAttribute('num_llap_nodes', "maximum", int(num_llap_nodes_max_limit))

        llap_container_size = long(llap_container_size)
        llap_container_size_min_limit = long(llap_container_size_min_limit)
        putHiveInteractiveSiteProperty('hive.llap.daemon.yarn.container.mb', llap_container_size)
        putHiveInteractiveSitePropertyAttribute('hive.llap.daemon.yarn.container.mb', "minimum",
                                                llap_container_size_min_limit)
        putHiveInteractiveSitePropertyAttribute('hive.llap.daemon.yarn.container.mb', "maximum",
                                                llap_container_size)
        Logger.info("LLAP config 'hive.llap.daemon.yarn.container.mb' updated. Min : {0}, Curr: {1}, Max: {2}" \
                    .format(llap_container_size_min_limit, llap_container_size, llap_container_size))

        num_executors_per_node = long(num_executors_per_node)
        num_executors_per_node_max_limit = long(num_executors_per_node_max_limit)
        putHiveInteractiveSiteProperty('hive.llap.daemon.num.executors', num_executors_per_node)
        putHiveInteractiveSitePropertyAttribute('hive.llap.daemon.num.executors', "minimum", 1)
        putHiveInteractiveSitePropertyAttribute('hive.llap.daemon.num.executors', "maximum",
                                                num_executors_per_node_max_limit)
        Logger.info("LLAP config 'hive.llap.daemon.num.executors' updated. Min : {0}, Curr: {1}, Max: {2}" \
                    .format('1', num_executors_per_node, num_executors_per_node_max_limit))

        if not cache_size_per_node_in_changed_configs:
          cache_size_per_node = long(cache_size_per_node)
          cache_size_per_node_max_limit = long(cache_size_per_node_max_limit)
          putHiveInteractiveSiteProperty('hive.llap.io.memory.size', cache_size_per_node)
          putHiveInteractiveSitePropertyAttribute('hive.llap.io.memory.size', "minimum", 0)  # 0 -> Disables caching.
          putHiveInteractiveSitePropertyAttribute('hive.llap.io.memory.size', "maximum",
                                                  cache_size_per_node_max_limit)
          llap_io_enabled = 'false'
          if cache_size_per_node >= 64:
            llap_io_enabled = 'true'

          putHiveInteractiveSiteProperty('hive.llap.io.enabled', llap_io_enabled)
          Logger.info("LLAP config 'hive.llap.io.memory.size' updated. Min : {0}, Curr: {1}, Max: {2}" \
                      .format('0', cache_size_per_node, cache_size_per_node_max_limit))
          Logger.info("HiveServer2 config 'hive.llap.io.enabled' updated to '{0}' as part of "
                      "'hive.llap.io.memory.size' calculation.".format(llap_io_enabled))

        llap_xmx = long(llap_xmx)
        putHiveInteractiveEnvProperty('llap_heap_size', llap_xmx)
        Logger.info("LLAP config 'llap_heap_size' updated. Curr: {0}".format(llap_xmx))

        slider_am_container_mb = long(slider_am_container_mb)
        putHiveInteractiveEnvProperty('slider_am_container_mb', slider_am_container_mb)
        Logger.info("LLAP config 'slider_am_container_mb' updated. Curr: {0}".format(slider_am_container_mb))

    except Exception as e:
      Logger.info(e.message+" Skipping calculating LLAP configs. Setting them to minimum values.")
      traceback.print_exc()

      try:
        yarn_min_container_size = self.get_yarn_min_container_size(services)
        slider_am_container_mb = self.calculate_slider_am_size(yarn_min_container_size)

        putHiveInteractiveEnvProperty('num_llap_nodes', 0)
        putHiveInteractiveEnvPropertyAttribute('num_llap_nodes', "minimum", 0)
        putHiveInteractiveEnvPropertyAttribute('num_llap_nodes', "maximum", 0)

        putHiveInteractiveSiteProperty('hive.llap.daemon.yarn.container.mb', yarn_min_container_size)
        putHiveInteractiveSitePropertyAttribute('hive.llap.daemon.yarn.container.mb', "minimum", yarn_min_container_size)
        putHiveInteractiveSitePropertyAttribute('hive.llap.daemon.yarn.container.mb', "maximum", yarn_min_container_size)

        putHiveInteractiveSiteProperty('hive.llap.daemon.num.executors', 0)
        putHiveInteractiveSitePropertyAttribute('hive.llap.daemon.num.executors', "minimum", 0)
        putHiveInteractiveSitePropertyAttribute('hive.llap.daemon.num.executors', "maximum", 0)

        putHiveInteractiveSiteProperty('hive.llap.io.memory.size', 0)
        putHiveInteractiveSitePropertyAttribute('hive.llap.io.memory.size', "minimum", 0)
        putHiveInteractiveSitePropertyAttribute('hive.llap.io.memory.size', "maximum", 0)

        putHiveInteractiveEnvProperty('llap_heap_size', 0)

        putHiveInteractiveEnvProperty('slider_am_container_mb', slider_am_container_mb)

      except Exception as e:
        Logger.info("Problem setting minimum values for LLAP configs in except code.")
        traceback.print_exc()

  """
  Checks for the presence of passed-in configuration properties in a given config, if they are changed.
  Reads from services["changed-configurations"].
  Parameters:
     services: Configuration information for the cluster
     config_type : Type of the configuration
     config_names_set : Set of configuration properties to be checked if they are changed.
     all_exists: If True : returns True only if all properties mentioned in 'config_names_set' we found
                           in services["changed-configurations"].
                           Otherwise, returns False.
                 If False : return True, if any of the properties mentioned in config_names_set we found in
                           services["changed-configurations"].
                           Otherwise, returns False.
  """
  def are_config_props_in_changed_configs(self, services, config_type, config_names_set, all_exists):
    changedConfigs = services["changed-configurations"]
    changed_config_names_set = set()
    for changedConfig in changedConfigs:
      if changedConfig['type'] == config_type:
        changed_config_names_set.update([changedConfig['name']])

    if changed_config_names_set is None:
      return False
    else:
      configs_intersection = changed_config_names_set.intersection(config_names_set)
      if all_exists:
        if configs_intersection == config_names_set:
          return True
      else:
        if len(configs_intersection) > 0 :
          return True
    return False

  """
  Returns all the Node Manager configs in cluster.
  """
  def get_node_manager_hosts(self, services, hosts):
    if len(hosts["items"]) > 0:
      node_manager_hosts = self.getHostsWithComponent("YARN", "NODEMANAGER", services, hosts)
      assert (node_manager_hosts is not None), "Information about NODEMANAGER not found in cluster."
      return node_manager_hosts


  """
  Returns the current LLAP queue capacity percentage value. (llap_queue_capacity)
  """
  def get_llap_cap_percent_slider(self, services, configurations):
    if 'llap_queue_capacity' in services['configurations']['hive-interactive-env']['properties']:
      llap_slider_cap_percentage = float(
        services['configurations']['hive-interactive-env']['properties']['llap_queue_capacity'])
      if llap_slider_cap_percentage <= 0 :
        if 'hive-interactive-env' in configurations and \
            'llap_queue_capacity' in configurations["hive-interactive-env"]["properties"]:
          llap_slider_cap_percentage = configurations["hive-interactive-env"]["properties"]["llap_queue_capacity"]
      assert (llap_slider_cap_percentage > 0), "'llap_queue_capacity' is set to 0."
      return llap_slider_cap_percentage


  """
  Returns current value of cache per node for LLAP (hive.llap.io.memory.size)
  """
  def get_cache_size_per_node_for_llap_nodes(self, services):
    if 'hive.llap.io.memory.size' in services['configurations']['hive-interactive-site']['properties']:
      cache_size_per_node = float(
        services['configurations']['hive-interactive-site']['properties']['hive.llap.io.memory.size'])
      return cache_size_per_node
    else:
      Logger.error("Couldn't retrieve Hive Server interactive's 'hive.llap.io.memory.size' config.")
      # Not doing raise as the Exception that catches it will set all other LLAP configs related
      # to LLAP package as 0, a way to tell that calulations couldn't be done. This is not the intention here.
      # Just keep cache 0, if it couldn't be retrieved.
      return 0

  """
  Returns current value of number of LLAP nodes in cluster (num_llap_nodes)
  """
  def get_num_llap_nodes(self, services):
    if 'num_llap_nodes' in services['configurations']['hive-interactive-env']['properties']:
      num_llap_nodes = float(
        services['configurations']['hive-interactive-env']['properties']['num_llap_nodes'])
      assert (num_llap_nodes > 0), "Number of LLAP nodes read : {0}. Expected value : > 0".format(
        num_llap_nodes)
      return num_llap_nodes
    else:
      raise Fail("Couldn't retrieve Hive Server interactive's 'num_llap_nodes' config.")

  """
  Calculates recommended and maximum LLAP nodes in the cluster.
  """
  def calculate_num_llap_nodes(self, services, hosts, configurations):
    # TODO : Read NodeManager confis and figure the smallest sized NM.
    size_of_smallest_nm = self.get_yarn_rm_mem_in_mb(services)
    assert (
      size_of_smallest_nm > 0), "Size of smallest NODEMANAGER calculated value : {0}. Expected value : > 0".format(
      size_of_smallest_nm)
    yarn_min_container_size = self.get_yarn_min_container_size(services)
    node_size_usable = self._normalizeDown(size_of_smallest_nm, yarn_min_container_size)
    cap_available_for_daemons = self.calculate_cap_available_for_llap_daemons(services, hosts, configurations)
    num_llap_nodes = float(math.ceil(cap_available_for_daemons / node_size_usable))
    assert (num_llap_nodes > 0), "Number of LLAP nodes calculated : {0}. Expected value : > 0".format(
      num_llap_nodes)
    # Maximum number of nodes that LLAP can use.
    num_llap_nodes_max_limit = len(self.get_node_manager_hosts(services, hosts))
    Logger.info("Calculated num_llap_nodes {3}, num_llap_nodes_max_limit : {4}, using following : "
                "yarn_min_container_size : {0}, node_size_usable : {1}, cap_available_for_daemons :"
                " {2}. ".format(yarn_min_container_size, node_size_usable, \
                cap_available_for_daemons, num_llap_nodes, num_llap_nodes_max_limit))
    return num_llap_nodes, num_llap_nodes_max_limit


  """
  Gets Tez container size (hive.tez.container.size)
  """
  def get_tez_container_size(self, services):
    hive_container_size = 0
    if 'hive.tez.container.size' in services['configurations']['hive-site']['properties']:
      hive_container_size = float(
        services['configurations']['hive-site']['properties']['hive.tez.container.size'])
      assert (
        hive_container_size > 0), "'hive.tez.container.size' current value : {0}. Expected value : > 0".format(
        hive_container_size)
    else:
      raise Fail("Couldn't retrieve Hive Server 'hive.tez.container.size' config.")
    return hive_container_size



  """
  Gets YARN's mimimum container size (yarn.scheduler.minimum-allocation-mb)
  """
  def get_yarn_min_container_size(self, services):
    yarn_min_container_size = 0
    if 'yarn.scheduler.minimum-allocation-mb' in services['configurations']['yarn-site']['properties']:
      yarn_min_container_size = float(
        services['configurations']['yarn-site']['properties']['yarn.scheduler.minimum-allocation-mb'])
      assert (
        yarn_min_container_size > 0), "'yarn.scheduler.minimum-allocation-mb' current value : {0}. Expected value : > 0".format(
        yarn_min_container_size)
    else:
      raise Fail("Couldn't retrieve YARN's 'yarn.scheduler.minimum-allocation-mb' config.")
    return yarn_min_container_size

  """
  Calculates recommended and minimum container size for LLAP app.
  """
  def calculate_llap_app_container_size(self, services, hosts, configurations):
    cap_available_for_daemons = self.calculate_cap_available_for_llap_daemons(services, hosts, configurations)

    node_manager_hosts = self.get_node_manager_hosts(services, hosts)
    node_manager_host_cnt = len(node_manager_hosts)

    num_llap_nodes_in_changed_configs = self.are_config_props_in_changed_configs(services, "hive-interactive-env",
                                                                                 set(['num_llap_nodes']), False)
    if not num_llap_nodes_in_changed_configs:
      num_llap_nodes, num_llap_nodes_max_limit = self.calculate_num_llap_nodes(services, hosts, configurations)
    else:
      num_llap_nodes = self.get_num_llap_nodes(services)

    llap_container_size_raw = cap_available_for_daemons / num_llap_nodes
    llap_container_size_raw_min_limit = cap_available_for_daemons / node_manager_host_cnt

    yarn_min_container_size = self.get_yarn_min_container_size(services)

    llap_container_size = self._normalizeDown(llap_container_size_raw, yarn_min_container_size)
    llap_container_size_min_limit = self._normalizeDown(llap_container_size_raw_min_limit, yarn_min_container_size)
    '''
    if llap_container_size_max_limit < llap_container_size:
      llap_container_size_max_limit = llap_container_size
    '''
    Logger.info("Calculated llap_container_size : {0}, llap_container_size_min_limit : {1}, using following : "
                "cap_available_for_daemons : {2}, node_manager_host_cnt : {3}, llap_container_size_raw : {4}, "
                "llap_container_size_raw_max_limit : {5}, yarn_min_container_size : {6} "\
                .format(llap_container_size, llap_container_size_min_limit, cap_available_for_daemons, node_manager_host_cnt,
                        llap_container_size_raw, llap_container_size_raw_min_limit, yarn_min_container_size))
    return llap_container_size, llap_container_size_min_limit


  def calculate_cap_available_for_llap_daemons(self, services, hosts, configurations):
    llap_concurrency = 0
    llap_slider_cap_percentage = self.get_llap_cap_percent_slider(services, configurations)
    yarn_rm_mem_in_mb = self.get_yarn_rm_mem_in_mb(services)


    node_manager_hosts = self.get_node_manager_hosts(services, hosts)
    assert (node_manager_hosts is not None), "Information about NODEMANAGER not found in cluster."

    total_cluster_cap = len(node_manager_hosts) * yarn_rm_mem_in_mb

    total_llap_queue_size = float(llap_slider_cap_percentage) / 100 * total_cluster_cap

    llap_daemon_container_size = self.get_hive_am_container_size(services)

    yarn_min_container_size = self.get_yarn_min_container_size(services)

    if 'hive.server2.tez.sessions.per.default.queue' in services['configurations'][self.HIVE_INTERACTIVE_SITE][
      'properties']:
      llap_concurrency = float(services['configurations'][self.HIVE_INTERACTIVE_SITE]['properties'][
                                 'hive.server2.tez.sessions.per.default.queue'])
      assert (llap_concurrency > 0), "'hive.server2.tez.sessions.per.default.queue' current value : {0}. Expected value : > 0"\
        .format(llap_concurrency)
    else:
      raise Fail("Couldn't retrieve Hive Server interactive's 'hive.server2.tez.sessions.per.default.queue' config.")

    total_am_capacity_required = self._normalizeUp(llap_daemon_container_size, yarn_min_container_size) \
                                 * llap_concurrency + self.calculate_slider_am_size(yarn_min_container_size)
    cap_available_for_daemons = total_llap_queue_size - total_am_capacity_required
    if cap_available_for_daemons < yarn_min_container_size :
      raise Fail("'Capacity available for LLAP daemons'({0}) < 'YARN minimum container size'({1}). Invalid configuration detected. "
                 "Increase LLAP queue size.".format(cap_available_for_daemons, yarn_min_container_size))
    assert (
      cap_available_for_daemons > 0), "'Capacity available for daemons' calculated value : {0}. Expected value : > 0".format(
      cap_available_for_daemons)
    Logger.info("Calculated cap_available_for_daemons : {0}, using following : llap_slider_cap_percentage : {1}, "
                "yarn_rm_mem_in_mb : {2}, total_cluster_cap : {3}, total_llap_queue_size : {4}, llap_daemon_container_size"
                " : {5}, yarn_min_container_size : {6}, llap_concurrency : {7}, total_am_capacity_required : {8}, "
                .format(cap_available_for_daemons, llap_slider_cap_percentage, yarn_rm_mem_in_mb, total_cluster_cap,
                        total_llap_queue_size, llap_daemon_container_size, yarn_min_container_size, llap_concurrency,
                        total_am_capacity_required))
    return cap_available_for_daemons

  """
  Calculates the Slider App Master size based on YARN's Minimum Container Size.
  """
  def calculate_slider_am_size(self, yarn_min_container_size):
    if yarn_min_container_size > 1024:
      return 1024
    if yarn_min_container_size >= 256 and yarn_min_container_size <= 1024:
      return yarn_min_container_size
    if yarn_min_container_size < 256:
      return 256

  """
  Gets YARN Resource Manager memory in MB (yarn.nodemanager.resource.memory-mb).
  """
  def get_yarn_rm_mem_in_mb(self, services):
    if 'yarn-site' in services['configurations'] and \
        'yarn.nodemanager.resource.memory-mb' in services['configurations']['yarn-site']['properties']:
      yarn_rm_mem_in_mb = float(
        services['configurations']['yarn-site']['properties']['yarn.nodemanager.resource.memory-mb'])
      assert (
        yarn_rm_mem_in_mb > 0.0), "'yarn.nodemanager.resource.memory-mb' current value : {0}. Expected value : > 0".format(
        yarn_rm_mem_in_mb)
    else:
      raise Fail(
        "Couldn't retrieve YARN's 'yarn.nodemanager.resource.memory-mb' config.")
    return yarn_rm_mem_in_mb

  """
  Gets HIVE App Master container size (tez.am.resource.memory.mb)
  """
  def get_hive_am_container_size(self, services):
    llap_daemon_container_size = 0
    if self.HIVE_INTERACTIVE_SITE in services['configurations'] and \
        'tez.am.resource.memory.mb' in services['configurations']['tez-interactive-site']['properties']:
      llap_daemon_container_size = float(
        services['configurations']['tez-interactive-site']['properties']['tez.am.resource.memory.mb'])
      assert (
        llap_daemon_container_size > 0), "'tez.am.resource.memory.mb' current value : {0}. Expected value : > 0".format(
        llap_daemon_container_size)
    else:
      raise Fail("Couldn't retrieve Hive Server interactive's 'tez.am.resource.memory.mb' config.")
    return llap_daemon_container_size


  """
  Calculates suggested and maximum value for number of LLAP executors.
  """
  def calculate_llap_daemon_executors_count(self, services, llap_container_size):
    cpu_per_nm_host = 0
    exec_to_cache_ratio = 1.5

    hive_container_size = self.get_tez_container_size(services)

    if 'yarn.nodemanager.resource.cpu-vcores' in services['configurations']['yarn-site']['properties']:
      cpu_per_nm_host = float(services['configurations']['yarn-site']['properties'][
                                'yarn.nodemanager.resource.cpu-vcores'])
      assert (cpu_per_nm_host > 0), "'yarn.nodemanager.resource.cpu-vcores' current value : {0}. Expected value : > 0"\
        .format(cpu_per_nm_host)
    else:
      raise Fail("Couldn't retrieve YARN's 'yarn.nodemanager.resource.cpu-vcores' config.")

    mem_per_executor = hive_container_size * exec_to_cache_ratio;
    if mem_per_executor > llap_container_size:
      mem_per_executor = llap_container_size

    num_executors_per_node_raw = math.floor(llap_container_size / mem_per_executor)
    num_executors_per_node = min(num_executors_per_node_raw, cpu_per_nm_host)
    # Allow 4x over-subscription of CPU as a max value
    num_executors_per_node_max_limit = min(num_executors_per_node_raw, 4 * cpu_per_nm_host)
    Logger.info("calculated num_executors_per_node: {0}, num_executors_per_node_max_limit : {1}, using following "
                ":  hive_container_size : {2}, cpu_per_nm_host : {3}, mem_per_executor : {4}, num_executors_per_node_raw : {5}"
                .format(num_executors_per_node, num_executors_per_node_max_limit, hive_container_size,
                        cpu_per_nm_host, mem_per_executor, num_executors_per_node_raw))
    return num_executors_per_node, num_executors_per_node_max_limit


  """
  Calculates suggested and maximum value for LLAP cache size per node.
  """
  def calculate_llap_cache_size_per_executor(self, services, llap_container_size, num_executors_per_node):
    hive_container_size = self.get_tez_container_size(services)
    cache_size_per_node = llap_container_size - (num_executors_per_node * hive_container_size)
    # Reserved memory for minExecutors, which is 1.
    cache_size_per_node_max_limit = llap_container_size - (1 * hive_container_size)
    Logger.info("Calculated cache_size_per_node : {0}, cache_size_per_node_max_limit : {1}, using following : "
                "hive_container_size : {2}, llap_container_size : {3}, num_executors_per_node : {4}"
                .format(cache_size_per_node, cache_size_per_node_max_limit, hive_container_size, llap_container_size,
                        num_executors_per_node))
    return cache_size_per_node, cache_size_per_node_max_limit


  """
  Calculates recommended heap size for LLAP app.
  """
  def calculate_llap_app_heap_size(self, services, num_executors_per_node):
    hive_container_size = self.get_tez_container_size(services)
    total_mem_for_executors = num_executors_per_node * hive_container_size
    llap_app_heap_size = max(total_mem_for_executors * 0.8, total_mem_for_executors - 1024)
    Logger.info("Calculated llap_app_heap_size : {0}, using following : hive_container_size : {1}, "
                "total_mem_for_executors : {2}".format(llap_app_heap_size, hive_container_size, total_mem_for_executors))
    return llap_app_heap_size

  """
  Minimum 'llap' queue capacity required in order to get LLAP app running.
  """
  def min_llap_queue_perc_required_in_cluster(self, services, hosts):
    # Get llap queue size if seized at 20%
    node_manager_hosts = self.get_node_manager_hosts(services, hosts)
    yarn_rm_mem_in_mb = self.get_yarn_rm_mem_in_mb(services)
    total_cluster_cap = len(node_manager_hosts) * yarn_rm_mem_in_mb
    total_llap_queue_size_at_20_perc = 20.0 / 100 * total_cluster_cap

    # Calculate based on minimum size required by containers.
    yarn_min_container_size = self.get_yarn_min_container_size(services)
    slider_am_size = self.calculate_slider_am_size(yarn_min_container_size)
    tez_container_size = self.get_tez_container_size(services)
    hive_am_container_size = self.get_hive_am_container_size(services)
    normalized_val = self._normalizeUp(slider_am_size, yarn_min_container_size) + self._normalizeUp\
      (tez_container_size, yarn_min_container_size) + self._normalizeUp(hive_am_container_size, yarn_min_container_size)

    min_required = max(total_llap_queue_size_at_20_perc, normalized_val)

    min_required_perc = min_required * 100 / total_cluster_cap
    Logger.info("Calculated min_llap_queue_perc_required_in_cluster : {0} and min_llap_queue_cap_required_in_cluster: {1} "
                "using following : yarn_min_container_size : {2}, ""slider_am_size : {3}, tez_container_size : {4}, "
                "hive_am_container_size : {5}".format(min_required_perc, min_required, yarn_min_container_size,
                slider_am_size, tez_container_size, hive_am_container_size))
    return int(math.ceil(min_required_perc))

  """
  Normalize down 'val2' with respect to 'val1'.
  """
  def _normalizeDown(self, val1, val2):
    tmp = math.floor(val1 / val2);
    if tmp < 1.00:
      return val1
    return tmp * val2;

  """
  Normalize up 'val2' with respect to 'val1'.
  """
  def _normalizeUp(self, val1, val2):
    tmp = math.ceil(val1 / val2)
    return tmp * val2

  """
  Checks and (1). Creates 'llap' queue if only 'default' queue exist at leaf level and is consuming 100% capacity OR
             (2). Updates 'llap' queue capacity and state, if 'llap' queue exists.
  """
  def checkAndManageLlapQueue(self, services, configurations, hosts, llap_queue_name):
    putHiveInteractiveEnvProperty = self.putProperty(configurations, "hive-interactive-env", services)
    putHiveInteractiveSiteProperty = self.putProperty(configurations, self.HIVE_INTERACTIVE_SITE, services)
    putHiveInteractiveEnvPropertyAttribute = self.putPropertyAttribute(configurations, "hive-interactive-env")
    putCapSchedProperty = self.putProperty(configurations, "capacity-scheduler", services)

    capacitySchedulerProperties = self.getCapacitySchedulerProperties(services)

    if capacitySchedulerProperties:
      # Get the llap Cluster percentage used for 'llap' Queue creation
      if 'llap_queue_capacity' in services['configurations']['hive-interactive-env']['properties']:
        llap_slider_cap_percentage = int(
          services['configurations']['hive-interactive-env']['properties']['llap_queue_capacity'])
        llap_min_reqd_cap_percentage = self.min_llap_queue_perc_required_in_cluster(services, hosts)
        if llap_slider_cap_percentage <= 0 or llap_slider_cap_percentage > 100:
          Logger.info("Adjusting HIVE 'llap_queue_capacity' from {0}% to {1}%".format(llap_slider_cap_percentage, llap_min_reqd_cap_percentage))
          putHiveInteractiveEnvProperty('llap_queue_capacity', llap_min_reqd_cap_percentage)
          llap_slider_cap_percentage = llap_min_reqd_cap_percentage
      else:
        Logger.error("Problem retrieving LLAP Queue Capacity. Skipping creating {0} queue".format(llap_queue_name))
        return
      leafQueueNames = self.getAllYarnLeafQueues(capacitySchedulerProperties)
      capSchedConfigKeys = capacitySchedulerProperties.keys()

      yarn_default_queue_capacity = -1
      if 'yarn.scheduler.capacity.root.default.capacity' in capSchedConfigKeys:
        yarn_default_queue_capacity = capacitySchedulerProperties.get('yarn.scheduler.capacity.root.default.capacity')

      # Get 'llap' queue state
      currLlapQueueState = ''
      if 'yarn.scheduler.capacity.root.'+llap_queue_name+'.state' in capSchedConfigKeys:
        currLlapQueueState = capacitySchedulerProperties.get('yarn.scheduler.capacity.root.'+llap_queue_name+'.state')

      # Get 'llap' queue capacity
      currLlapQueueCap = -1
      if 'yarn.scheduler.capacity.root.'+llap_queue_name+'.capacity' in capSchedConfigKeys:
        currLlapQueueCap = int(capacitySchedulerProperties.get('yarn.scheduler.capacity.root.'+llap_queue_name+'.capacity'))

      if self.HIVE_INTERACTIVE_SITE in services['configurations'] and \
          'hive.llap.daemon.queue.name' in services['configurations'][self.HIVE_INTERACTIVE_SITE]['properties']:
        llap_daemon_selected_queue_name =  services['configurations'][self.HIVE_INTERACTIVE_SITE]['properties']['hive.llap.daemon.queue.name']
      else:
        Logger.debug("Couldn't retrive 'hive.llap.daemon.queue.name' property. Skipping adjusting queues.")
        return
      updated_cap_sched_configs = ''

      enabled_hive_int_in_changed_configs = self.are_config_props_in_changed_configs(services, "hive-interactive-env",
                                                                                   set(['enable_hive_interactive']), False)
      """
      We create OR "modify 'llap' queue 'state and/or capacity' " based on below conditions:
       - if only 1 queue exists at root level and is 'default' queue and has 100% cap -> Create 'llap' queue,  OR
       - if 2 queues exists at root level ('llap' and 'default') :
           - Queue selected is 'llap' and state is STOPPED -> Modify 'llap' queue state to RUNNING, adjust capacity, OR
           - Queue selected is 'llap', state is RUNNING and 'llap_queue_capacity' prop != 'llap' queue current running capacity ->
              Modify 'llap' queue capacity to 'llap_queue_capacity'
      """
      if 'default' in leafQueueNames and \
        ((len(leafQueueNames) == 1 and int(yarn_default_queue_capacity) == 100) or \
        ((len(leafQueueNames) == 2 and llap_queue_name in leafQueueNames) and \
           ((currLlapQueueState == 'STOPPED' and enabled_hive_int_in_changed_configs) or (currLlapQueueState == 'RUNNING' and currLlapQueueCap != llap_slider_cap_percentage)))):
        adjusted_default_queue_cap = str(100 - llap_slider_cap_percentage)
        for prop, val in capacitySchedulerProperties.items():
          if llap_queue_name not in prop:
            if prop == 'yarn.scheduler.capacity.root.queues':
              updated_cap_sched_configs = updated_cap_sched_configs \
                                          + prop + "=default,llap\n"
            elif prop == 'yarn.scheduler.capacity.root.default.capacity':
              updated_cap_sched_configs = updated_cap_sched_configs \
                                          + prop + "=" + adjusted_default_queue_cap + "\n"
            elif prop == 'yarn.scheduler.capacity.root.default.maximum-capacity':
              updated_cap_sched_configs = updated_cap_sched_configs \
                                          + prop + "=" + adjusted_default_queue_cap + "\n"
            elif prop.startswith('yarn.') and '.llap.' not in prop:
              updated_cap_sched_configs = updated_cap_sched_configs + prop + "=" + val + "\n"

        llap_slider_cap_percentage = str(llap_slider_cap_percentage)
        hive_user = '*'  # Open to all
        if 'hive_user' in services['configurations']['hive-env']['properties']:
          hive_user = services['configurations']['hive-env']['properties']['hive_user']

        # Now, append the 'llap' queue related properties
        updated_cap_sched_configs = updated_cap_sched_configs \
                                    + "yarn.scheduler.capacity.root." + llap_queue_name + ".user-limit-factor=1\n" \
                                    + "yarn.scheduler.capacity.root." + llap_queue_name + ".state=RUNNING\n" \
                                    + "yarn.scheduler.capacity.root." + llap_queue_name + ".ordering-policy=fifo\n" \
                                    + "yarn.scheduler.capacity.root." + llap_queue_name + ".minimum-user-limit-percent=100\n" \
                                    + "yarn.scheduler.capacity.root." + llap_queue_name + ".maximum-capacity=" \
                                    + llap_slider_cap_percentage + "\n" \
                                    + "yarn.scheduler.capacity.root." + llap_queue_name + ".capacity=" \
                                    + llap_slider_cap_percentage + "\n" \
                                    + "yarn.scheduler.capacity.root." + llap_queue_name + ".acl_submit_applications=" \
                                    + hive_user + "\n" \
                                    + "yarn.scheduler.capacity.root." + llap_queue_name + ".acl_administer_queue=" \
                                    + hive_user + "\n" \
                                    + "yarn.scheduler.capacity.root." + llap_queue_name + ".maximum-am-resource-percent=1"

        if updated_cap_sched_configs:
          putCapSchedProperty("capacity-scheduler", updated_cap_sched_configs)
          if len(leafQueueNames) == 1: # 'llap' queue didn't exist before
            Logger.info("Created YARN Queue : '{0}' with capacity : {1}%. Adjusted default queue capacity to : {2}%" \
                      .format(llap_queue_name, llap_slider_cap_percentage, adjusted_default_queue_cap))
          else: # Queue existed, only adjustments done.
            Logger.info("Adjusted YARN Queue : '{0}'. Current capacity : {1}%. State: RUNNING.".format(llap_queue_name, llap_slider_cap_percentage))
            Logger.info("Adjusted 'default' queue capacity to : {0}%".format(adjusted_default_queue_cap))

          # Update Hive 'hive.llap.daemon.queue.name' prop to use 'llap' queue.
          putHiveInteractiveSiteProperty('hive.llap.daemon.queue.name', 'llap')
          putHiveInteractiveEnvPropertyAttribute('llap_queue_capacity', "minimum", llap_min_reqd_cap_percentage)
          putHiveInteractiveEnvPropertyAttribute('llap_queue_capacity', "maximum", 100)

          # Update 'hive.llap.daemon.queue.name' prop combo entries.
          self.setLlapDaemonQueueName(services, configurations)
      else:
        Logger.debug("Not creating {0} queue. Current YARN queues : {1}".format(llap_queue_name, list(leafQueueNames)))
    else:
      Logger.error("Couldn't retrieve 'capacity-scheduler' properties while doing YARN queue adjustment for Hive Server Interactive.")

  """
  Checks and sees (1). If only two leaf queues exist at root level, namely: 'default' and 'llap',
              and (2). 'llap' is in RUNNING state.

  If yes, performs the following actions:   (1). 'llap' queue state set to STOPPED,
                                            (2). 'llap' queue capacity set to 0 %,
                                            (3). 'default' queue capacity set to 100 %
  """
  def checkAndStopLlapQueue(self, services, configurations, llap_queue_name):
    putCapSchedProperty = self.putProperty(configurations, "capacity-scheduler", services)
    putHiveInteractiveSiteProperty = self.putProperty(configurations, self.HIVE_INTERACTIVE_SITE, services)
    capacitySchedulerProperties = self.getCapacitySchedulerProperties(services)
    updated_default_queue_configs = ''
    updated_llap_queue_configs = ''
    if capacitySchedulerProperties:
      # Get all leaf queues.
      leafQueueNames = self.getAllYarnLeafQueues(capacitySchedulerProperties)

      if len(leafQueueNames) == 2 and llap_queue_name in leafQueueNames and 'default' in leafQueueNames:
        # Get 'llap' queue state
        currLlapQueueState = 'STOPPED'
        if 'yarn.scheduler.capacity.root.'+llap_queue_name+'.state' in capacitySchedulerProperties.keys():
          currLlapQueueState = capacitySchedulerProperties.get('yarn.scheduler.capacity.root.'+llap_queue_name+'.state')
        else:
          Logger.error("{0} queue 'state' property not present in capacity scheduler. Skipping adjusting queues.".format(llap_queue_name))
          return
        if currLlapQueueState == 'RUNNING':
          DEFAULT_MAX_CAPACITY = '100'
          for prop, val in capacitySchedulerProperties.items():
            # Update 'default' related configs in 'updated_cap_sched_configs'
            if llap_queue_name not in prop:
              if prop == 'yarn.scheduler.capacity.root.default.capacity':
                # Set 'default' capacity back to maximum val
                updated_default_queue_configs = updated_default_queue_configs \
                                            + prop + "="+DEFAULT_MAX_CAPACITY + "\n"
              elif prop == 'yarn.scheduler.capacity.root.default.maximum-capacity':
                # Set 'default' max. capacity back to maximum val
                updated_default_queue_configs = updated_default_queue_configs \
                                            + prop + "="+DEFAULT_MAX_CAPACITY + "\n"
              elif prop.startswith('yarn.'):
                updated_default_queue_configs = updated_default_queue_configs + prop + "=" + val + "\n"
            else: # Update 'llap' related configs in 'updated_cap_sched_configs'
              if prop == 'yarn.scheduler.capacity.root.'+llap_queue_name+'.state':
                updated_llap_queue_configs = updated_llap_queue_configs \
                                            + prop + "=STOPPED\n"
              elif prop == 'yarn.scheduler.capacity.root.'+llap_queue_name+'.capacity':
                updated_llap_queue_configs = updated_llap_queue_configs \
                                            + prop + "=0\n"
              elif prop == 'yarn.scheduler.capacity.root.'+llap_queue_name+'.maximum-capacity':
                updated_llap_queue_configs = updated_llap_queue_configs \
                                            + prop + "=0\n"
              elif prop.startswith('yarn.'):
                updated_llap_queue_configs = updated_llap_queue_configs + prop + "=" + val + "\n"
        else:
          Logger.debug("{0} queue state is : {1}. Skipping adjusting queues.".format(llap_queue_name, currLlapQueueState))
          return

        if updated_default_queue_configs and updated_llap_queue_configs:
          putCapSchedProperty("capacity-scheduler", updated_default_queue_configs+updated_llap_queue_configs)
          Logger.info("Changed YARN '{0}' queue state to 'STOPPED', and capacity to 0%. Adjusted 'default' queue capacity to : {1}%" \
            .format(llap_queue_name, DEFAULT_MAX_CAPACITY))

          # Update Hive 'hive.llap.daemon.queue.name' prop to use 'default' queue.
          putHiveInteractiveSiteProperty('hive.llap.daemon.queue.name', 'default')
      else:
        Logger.debug("Not removing '{0}' queue as number of Queues not equal to 2. Current YARN queues : {1}".format(llap_queue_name, list(leafQueueNames)))
    else:
      Logger.error("Couldn't retrieve 'capacity-scheduler' properties while doing YARN queue adjustment for Hive Server Interactive.")

  """
  Checks and sets the 'Hive Server Interactive' 'hive.llap.daemon.queue.name' config.
  """
  def setLlapDaemonQueueName(self, services, configurations):
    putHiveInteractiveSitePropertyAttribute = self.putPropertyAttribute(configurations, self.HIVE_INTERACTIVE_SITE)
    capacitySchedulerProperties = dict()

    # Read 'capacity-scheduler' from configurations if we modified and added recommendation to it, as part of current
    # StackAdvisor invocation.
    if 'capacity-scheduler' in configurations and \
        'capacity-scheduler' in configurations['capacity-scheduler']['properties']:
      properties = str(configurations['capacity-scheduler']['properties']['capacity-scheduler']).split('\n')
      for property in properties:
        key, sep, value = property.partition("=")
        capacitySchedulerProperties[key] = value
    else: # read from input : services
      capacitySchedulerProperties = self.getCapacitySchedulerProperties(services)

    leafQueueNames = self.getAllYarnLeafQueues(capacitySchedulerProperties)
    if leafQueueNames:
      leafQueues = [{"label": str(queueName), "value": queueName} for queueName in leafQueueNames]
      leafQueues = sorted(leafQueues, key=lambda q:q['value'])
      putHiveInteractiveSitePropertyAttribute("hive.llap.daemon.queue.name", "entries", leafQueues)
    else:
      Logger.error("Problem retrieving YARN queues. Skipping updating HIVE Server Interactve "
                   "'hive.server2.tez.default.queues' property.")

  """
  Gets all YARN leaf queues.
  """
  def getAllYarnLeafQueues(self, capacitySchedulerProperties):
    config_list = capacitySchedulerProperties.keys()
    yarn_queues = []
    leafQueueNames = set()
    if 'yarn.scheduler.capacity.root.queues' in config_list:
      yarn_queues = capacitySchedulerProperties.get('yarn.scheduler.capacity.root.queues')

    if yarn_queues:
      toProcessQueues = yarn_queues.split(",")
      while len(toProcessQueues) > 0:
        queue = toProcessQueues.pop()
        queueKey = "yarn.scheduler.capacity.root." + queue + ".queues"
        if queueKey in capacitySchedulerProperties:
          # If parent queue, add children
          subQueues = capacitySchedulerProperties[queueKey].split(",")
          for subQueue in subQueues:
            toProcessQueues.append(queue + "." + subQueue)
        else:
          # Leaf queue
          queueName = queue.split(".")[-1]
          leafQueueNames.add(queueName)
    return leafQueueNames

  """
  Returns the dictionary of 'capacity-scheduler' related configs.
  """
  def getCapacitySchedulerProperties(self, services):
    capacitySchedulerProperties = dict()
    if "capacity-scheduler" in services['configurations']:
      if "capacity-scheduler" in services['configurations']["capacity-scheduler"]["properties"]:
        properties = str(services['configurations']["capacity-scheduler"]["properties"]["capacity-scheduler"])\
          .split('\n')

        if properties:
          for property in properties:
            key, sep, value = property.partition("=")
            capacitySchedulerProperties[key] = value
    return capacitySchedulerProperties

  def recommendRangerConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP25StackAdvisor, self).recommendRangerConfigurations(configurations, clusterData, services, hosts)
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

    putTagsyncAppProperty = self.putProperty(configurations, "tagsync-application-properties", services)
    putTagsyncSiteProperty = self.putProperty(configurations, "ranger-tagsync-site", services)

    has_ranger_tagsync = False
    if 'RANGER' in servicesList:
      ranger_tagsync_host = self.__getHostsForComponent(services, "RANGER", "RANGER_TAGSYNC")
      has_ranger_tagsync = len(ranger_tagsync_host) > 0

    zookeeper_host_port = self.getZKHostPortString(services)
    if zookeeper_host_port and has_ranger_tagsync:
      zookeeper_host_list = zookeeper_host_port.split(',')
      putTagsyncAppProperty('atlas.kafka.zookeeper.connect', zookeeper_host_list[0])
    else:
      putTagsyncAppProperty('atlas.kafka.zookeeper.connect', 'localhost:6667')

    if 'KAFKA' in servicesList and has_ranger_tagsync:
      kafka_hosts = self.getHostNamesWithComponent("KAFKA", "KAFKA_BROKER", services)
      kafka_port = '6667'
      if 'kafka-broker' in services['configurations'] and (
          'port' in services['configurations']['kafka-broker']['properties']):
        kafka_port = services['configurations']['kafka-broker']['properties']['port']
      kafka_host_port = []
      for i in range(len(kafka_hosts)):
        kafka_host_port.append(kafka_hosts[i] + ':' + kafka_port)

      final_kafka_host = ",".join(kafka_host_port)
      putTagsyncAppProperty('atlas.kafka.bootstrap.servers', final_kafka_host)
    else:
      putTagsyncAppProperty('atlas.kafka.bootstrap.servers', 'localhost:2181')

  def validateRangerTagsyncConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    ranger_tagsync_properties = getSiteProperties(configurations, "ranger-tagsync-site")
    validationItems = []
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

    has_ranger_tagsync = False
    if "RANGER" in servicesList:
      ranger_tagsync_host = self.__getHostsForComponent(services, "RANGER", "RANGER_TAGSYNC")
      has_ranger_tagsync = len(ranger_tagsync_host) == 0

      if has_ranger_tagsync and 'ranger.tagsync.enabled' in ranger_tagsync_properties and \
        ranger_tagsync_properties['ranger.tagsync.enabled'].lower() == 'true':
        validationItems.append({"config-name": "ranger.tagsync.enabled",
                                  "item": self.getWarnItem(
                                    "Need to Install RANGER TAGSYNC component to set ranger.tagsync.enabled as true.")})

    return self.toConfigurationValidationProblems(validationItems, "ranger-tagsync-site")

  """
  Returns the host(s) on which a requested service's component is hosted.
  Parameters :
    services : Configuration information for the cluster
    serviceName : Passed-in service in consideration
    componentName : Passed-in component in consideration
  """
  def __getHostsForComponent(self, services, serviceName, componentName):
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item["StackServiceComponents"] for sublist in componentsListList for item in sublist]
    hosts_for_component = []
    if serviceName in servicesList:
      hosts_for_component = [component["hostnames"] for component in componentsList if component["component_name"] == componentName][0]
    return hosts_for_component