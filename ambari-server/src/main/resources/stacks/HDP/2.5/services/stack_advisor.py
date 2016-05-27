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
      "ATLAS": {"application-properties": self.validateAtlasConfigurations},
      "HIVE": {"hive-interactive-env": self.validateHiveInteractiveEnvConfigurations},
      "YARN": {"yarn-site": self.validateYarnConfigurations},
      "RANGER": {"ranger-tagsync-site": self.validateRangerTagsyncConfigurations}
    }
    self.mergeValidators(parentValidators, childValidators)
    return parentValidators

  def validateAtlasConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    application_properties = getSiteProperties(configurations, "application-properties")
    validationItems = []

    if application_properties['atlas.graph.index.search.backend'] == 'solr5' and \
            not application_properties['atlas.graph.index.search.solr.zookeeper-url']:
      validationItems.append({"config-name": "atlas.graph.index.search.solr.zookeeper-url",
                              "item": self.getErrorItem(
                                  "If LOGSEARCH is not installed then the SOLR zookeeper url configuration must be specified.")})

    if not application_properties['atlas.kafka.bootstrap.servers']:
      validationItems.append({"config-name": "atlas.kafka.bootstrap.servers",
                              "item": self.getErrorItem(
                                  "If KAFKA is not installed then the Kafka bootstrap servers configuration must be specified.")})

    if not application_properties['atlas.kafka.zookeeper.connect']:
      validationItems.append({"config-name": "atlas.kafka.zookeeper.connect",
                              "item": self.getErrorItem(
                                  "If KAFKA is not installed then the Kafka zookeeper quorum configuration must be specified.")})

    if application_properties['atlas.graph.storage.backend'] == 'hbase':
      if not application_properties['atlas.graph.storage.hostname']:
        validationItems.append({"config-name": "atlas.graph.storage.hostname",
                                "item": self.getErrorItem(
                                    "If HBASE is not installed then the hbase zookeeper quorum configuration must be specified.")})
      elif application_properties['atlas.graph.storage.hostname'] == '{{hbase_zookeeper_quorum}}':
        validationItems.append({"config-name": "atlas.graph.storage.hostname",
                                "item": self.getWarnItem(
                                    "Note that Atlas is configured to use the HBASE instance being installed for this cluster.")})

      if not application_properties['atlas.audit.hbase.zookeeper.quorum']:
        validationItems.append({"config-name": "atlas.audit.hbase.zookeeper.quorum",
                                "item": self.getErrorItem(
                                    "If HBASE is not installed then the audit hbase zookeeper quorum configuration must be specified.")})

    validationProblems = self.toConfigurationValidationProblems(validationItems, "application-properties")
    return validationProblems

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
      "HBASE": self.recommendHBASEConfigurations,
      "HIVE": self.recommendHIVEConfigurations,
      "ATLAS": self.recommendAtlasConfigurations,
      "RANGER_KMS": self.recommendRangerKMSConfigurations
    }
    parentRecommendConfDict.update(childRecommendConfDict)
    return parentRecommendConfDict

  def recommendAtlasConfigurations(self, configurations, clusterData, services, hosts):
    putAtlasApplicationProperty = self.putProperty(configurations, "application-properties", services)

    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

    if "LOGSEARCH" in servicesList:
      putAtlasApplicationProperty('atlas.graph.index.search.solr.zookeeper-url', '{{solr_zookeeper_url}}')
    else:
      putAtlasApplicationProperty('atlas.graph.index.search.solr.zookeeper-url', "")

    if "KAFKA" in servicesList:
      putAtlasApplicationProperty('atlas.kafka.bootstrap.servers', '{{kafka_bootstrap_servers}}')
      putAtlasApplicationProperty('atlas.kafka.zookeeper.connect', '{{kafka_zookeeper_connect}}')
    else:
      putAtlasApplicationProperty('atlas.kafka.bootstrap.servers', "")
      putAtlasApplicationProperty('atlas.kafka.zookeeper.connect', "")

    if "HBASE" in servicesList:
      putAtlasApplicationProperty('atlas.graph.storage.hostname', '{{hbase_zookeeper_quorum}}')
      putAtlasApplicationProperty('atlas.audit.hbase.zookeeper.quorum', '{{hbase_zookeeper_quorum}}')
    else:
      putAtlasApplicationProperty('atlas.graph.storage.hostname', "")
      putAtlasApplicationProperty('atlas.audit.hbase.zookeeper.quorum', "")

  def recommendHBASEConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP25StackAdvisor, self).recommendHBASEConfigurations(configurations, clusterData, services, hosts)
    putHbaseSiteProperty = self.putProperty(configurations, "hbase-site", services)
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

    if 'KERBEROS' in servicesList:
      putHbaseSiteProperty('hbase.master.ui.readonly', 'true')
    else:
      putHbaseSiteProperty('hbase.master.ui.readonly', 'false')

  def recommendHIVEConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP25StackAdvisor, self).recommendHIVEConfigurations(configurations, clusterData, services, hosts)
    putHiveInteractiveEnvProperty = self.putProperty(configurations, "hive-interactive-env", services)
    putHiveInteractiveSiteProperty = self.putProperty(configurations, self.HIVE_INTERACTIVE_SITE, services)

    # For 'Hive Server Interactive', if the component exists.
    hsi_hosts = self.__getHostsForComponent(services, "HIVE", "HIVE_SERVER_INTERACTIVE")
    if len(hsi_hosts) > 0:
      hsi_host = hsi_hosts[0]
      putHiveInteractiveEnvProperty('enable_hive_interactive', 'true')
      putHiveInteractiveEnvProperty('hive_server_interactive_host', hsi_host)

      # Update 'hive.llap.daemon.queue.name' if capacity scheduler is changed.
      if self.HIVE_INTERACTIVE_SITE in services['configurations'] and \
          'hive.llap.daemon.queue.name' in services['configurations'][self.HIVE_INTERACTIVE_SITE]['properties']:
        self.setLlapDaemonQueueNamePropAttributes(services, configurations)
    else:
      putHiveInteractiveEnvProperty('enable_hive_interactive', 'false')

    if self.HIVE_INTERACTIVE_SITE in services['configurations'] and \
        'hive.llap.zk.sm.connectionString' in services['configurations'][self.HIVE_INTERACTIVE_SITE]['properties']:
      # Fill the property 'hive.llap.zk.sm.connectionString' required by Hive Server Interactive (HiveServer2)
      zookeeper_host_port = self.getZKHostPortString(services)
      if zookeeper_host_port:
        putHiveInteractiveSiteProperty("hive.llap.zk.sm.connectionString", zookeeper_host_port)
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
    (3). hive.llap.daemon.num.executors (4). hive.llap.io.memory.size (5). llap_heap_size (6). slider_am_container_size,
    and (7). hive.server2.tez.sessions.per.default.queue

    The trigger point for updating LLAP configs (mentioned above) is change in values of any of the following:
    (1). 'enable_hive_interactive' set to 'true' (2). 'llap_queue_capacity' (3). 'hive.server2.tez.sessions.per.default.queue'
    (4). 'llap' named queue get selected for config 'hive.llap.daemon.queue.name' and only 2 queues exist ('llap' and 'default')
    at root level.

    If change in value for 'llap_queue_capacity' or 'hive.server2.tez.sessions.per.default.queue' is detected, that config
    value is not calulated, but read and use in calculation for dependent configs.
  """
  def updateLlapConfigs(self, configurations, services, hosts, llap_queue_name):
    putHiveInteractiveSiteProperty = self.putProperty(configurations, self.HIVE_INTERACTIVE_SITE, services)
    putHiveInteractiveSitePropertyAttribute = self.putPropertyAttribute(configurations, self.HIVE_INTERACTIVE_SITE)

    putHiveInteractiveEnvProperty = self.putProperty(configurations, "hive-interactive-env", services)
    putHiveInteractiveEnvPropertyAttribute = self.putPropertyAttribute(configurations, "hive-interactive-env")

    llap_daemon_selected_queue_name = None
    llap_queue_selected_in_current_call = None
    LLAP_MAX_CONCURRENCY = 32 # Allow a max of 32 concurrency.

    # initial memory setting to make sure hive.llap.daemon.yarn.container.mb >= yarn.scheduler.minimum-allocation-mb
    Logger.debug("Setting hive.llap.daemon.yarn.container.mb to yarn min container size as initial size (" + str(self.get_yarn_min_container_size(services)) + " MB).")
    putHiveInteractiveSiteProperty('hive.llap.daemon.yarn.container.mb', long(self.get_yarn_min_container_size(services)))

    try:
      if self.HIVE_INTERACTIVE_SITE in services['configurations'] and \
          'hive.llap.daemon.queue.name' in services['configurations'][self.HIVE_INTERACTIVE_SITE]['properties']:
        llap_daemon_selected_queue_name =  services['configurations'][self.HIVE_INTERACTIVE_SITE]['properties']['hive.llap.daemon.queue.name']

      if 'hive.llap.daemon.queue.name' in configurations[self.HIVE_INTERACTIVE_SITE]['properties']:
        llap_queue_selected_in_current_call = configurations[self.HIVE_INTERACTIVE_SITE]['properties']['hive.llap.daemon.queue.name']

      # Update Visibility of 'llap_queue_capacity' slider.
      capacity_scheduler_properties, received_as_key_value_pair = self.getCapacitySchedulerProperties(services)
      if capacity_scheduler_properties:
        # Get all leaf queues.
        leafQueueNames = self.getAllYarnLeafQueues(capacity_scheduler_properties)
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
        # Calculations are triggered only if there is change in any one of the following props :
        # 'llap_queue_capacity', 'enable_hive_interactive', 'hive.server2.tez.sessions.per.default.queue'
        # or 'hive.llap.daemon.queue.name' has change in value and selection is 'llap'
        config_names_to_be_checked = set(['llap_queue_capacity', 'enable_hive_interactive'])
        changed_configs_in_hive_int_env = self.are_config_props_in_changed_configs(services, "hive-interactive-env",
                                                                                   config_names_to_be_checked, False)

        # Determine if the Tez AM count has changed
        llap_concurrency_in_changed_configs = self.are_config_props_in_changed_configs(services, "hive-interactive-site",
                                                                                       set(['hive.server2.tez.sessions.per.default.queue']), False)

        if not changed_configs_in_hive_int_env \
          and llap_daemon_selected_queue_name != llap_queue_name \
          and llap_queue_selected_in_current_call != llap_queue_name \
          and not llap_concurrency_in_changed_configs:
          Logger.info("LLAP parameters not modified. Not adjusting LLAP configs.")
          Logger.debug("Current changed-configuration received is : {0}".format(services["changed-configurations"]))
          return

        node_manager_host_list = self.get_node_manager_hosts(services, hosts)
        assert (node_manager_host_list is not None), "Information about NODEMANAGER not found in cluster."
        node_manager_cnt = len(node_manager_host_list)
        llap_slider_cap_percentage = self.get_llap_cap_percent_slider(services, configurations)
        yarn_nm_mem_in_mb = self.get_yarn_nm_mem_in_mb(services)
        total_cluster_capacity = node_manager_cnt * yarn_nm_mem_in_mb
        Logger.info("\n\nCalculated total_cluster_capacity : {0}, using following : node_manager_cnt : {1}, "
                    "yarn_nm_mem_in_mb : {2}".format(total_cluster_capacity, node_manager_cnt, yarn_nm_mem_in_mb))

        yarn_min_container_size = self.get_yarn_min_container_size(services)
        tez_am_container_size = self._normalizeUp(self.get_tez_am_container_size(services), yarn_min_container_size)
        total_llap_queue_size = float(llap_slider_cap_percentage) / 100 * total_cluster_capacity
        # Get calculated value for Slider AM container Size
        slider_am_container_size = self.calculate_slider_am_size(yarn_min_container_size)

        # Read 'hive.server2.tez.sessions.per.default.queue' prop if it's in changed-configs, else calculate it.
        if not llap_concurrency_in_changed_configs:
          # Calculate llap concurrency (i.e. Number of Tez AM's)
          llap_concurrency = float(total_llap_queue_size * 0.25 / tez_am_container_size)
          llap_concurrency = max(long(llap_concurrency), 1)
          Logger.info("Calculated llap_concurrency : {0}, using following : total_llap_queue_size : {1}, "
                      "tez_am_container_size : {2}".format(llap_concurrency, total_llap_queue_size, tez_am_container_size))
          # Limit 'llap_concurrency' to reach a max. of 32.
          if llap_concurrency > LLAP_MAX_CONCURRENCY:
            llap_concurrency = LLAP_MAX_CONCURRENCY
        else:
          # Read current value
          if 'hive.server2.tez.sessions.per.default.queue' in services['configurations'][self.HIVE_INTERACTIVE_SITE][
            'properties']:
            llap_concurrency = long(services['configurations'][self.HIVE_INTERACTIVE_SITE]['properties'][
                                       'hive.server2.tez.sessions.per.default.queue'])
            assert (llap_concurrency >= 1), "'hive.server2.tez.sessions.per.default.queue' current value : {0}. Expected value : >= 1" \
              .format(llap_concurrency)
          else:
            raise Fail("Couldn't retrieve Hive Server interactive's 'hive.server2.tez.sessions.per.default.queue' config.")


        # Calculate 'total memory available for llap daemons' across cluster
        total_am_capacity_required = tez_am_container_size * llap_concurrency + slider_am_container_size
        cap_available_for_daemons = total_llap_queue_size - total_am_capacity_required
        if cap_available_for_daemons < yarn_min_container_size :
          raise Fail("'Capacity available for LLAP daemons'({0}) < 'YARN minimum container size'({1}). Invalid configuration detected. "
                     "Increase LLAP queue size.".format(cap_available_for_daemons, yarn_min_container_size))
        Logger.info("Calculated cap_available_for_daemons : {0}, using following : llap_slider_cap_percentage : {1}, "
                    "yarn_nm_mem_in_mb : {2}, total_cluster_capacity : {3}, total_llap_queue_size : {4}, tez_am_container_size"
                    " : {5}, yarn_min_container_size : {6}, llap_concurrency : {7}, total_am_capacity_required : {8}"
                    .format(cap_available_for_daemons, llap_slider_cap_percentage, yarn_nm_mem_in_mb, total_cluster_capacity,
                            total_llap_queue_size, tez_am_container_size, yarn_min_container_size, llap_concurrency,
                            total_am_capacity_required))


        # Calculate value for 'num_llap_nodes', an across cluster config.
        # Also, get calculated value for 'hive.llap.daemon.yarn.container.mb' based on 'num_llap_nodes' value, a per node config.
        num_llap_nodes_raw = cap_available_for_daemons / yarn_nm_mem_in_mb
        if num_llap_nodes_raw < 1.00:
          # Set the llap nodes to min. value of 1 and 'llap_container_size' to min. YARN allocation.
          num_llap_nodes = 1
          llap_container_size = self._normalizeUp(cap_available_for_daemons, yarn_min_container_size)
          Logger.info("Calculated llap_container_size : {0}, using following : cap_available_for_daemons : {1}, "
                      "yarn_min_container_size : {2}".format(llap_container_size, cap_available_for_daemons, yarn_min_container_size))
        else:
          num_llap_nodes = math.floor(num_llap_nodes_raw)
          llap_container_size = self._normalizeDown(yarn_nm_mem_in_mb, yarn_min_container_size)
          Logger.info("Calculated llap_container_size : {0}, using following : yarn_nm_mem_in_mb : {1}, "
                    "yarn_min_container_size : {2}".format(llap_container_size, yarn_nm_mem_in_mb, yarn_min_container_size))
        Logger.info("Calculated num_llap_nodes : {0} using following : yarn_nm_mem_in_mb : {1}, cap_available_for_daemons : {2} " \
                    .format(num_llap_nodes, yarn_nm_mem_in_mb, cap_available_for_daemons))


        # Calculate value for 'hive.llap.daemon.num.executors', a per node config.
        hive_tez_container_size = self.get_hive_tez_container_size(services)
        if 'yarn.nodemanager.resource.cpu-vcores' in services['configurations']['yarn-site']['properties']:
          cpu_per_nm_host = float(services['configurations']['yarn-site']['properties'][
                                    'yarn.nodemanager.resource.cpu-vcores'])
          assert (cpu_per_nm_host > 0), "'yarn.nodemanager.resource.cpu-vcores' current value : {0}. Expected value : > 0" \
            .format(cpu_per_nm_host)
        else:
          raise Fail("Couldn't retrieve YARN's 'yarn.nodemanager.resource.cpu-vcores' config.")

        num_executors_per_node_raw = math.floor(llap_container_size / hive_tez_container_size)
        num_executors_per_node = min(num_executors_per_node_raw, cpu_per_nm_host)
        Logger.info("calculated num_executors_per_node: {0}, using following :  hive_tez_container_size : {1}, "
                    "cpu_per_nm_host : {2}, num_executors_per_node_raw : {3}, llap_container_size : {4}"
                    .format(num_executors_per_node, hive_tez_container_size, cpu_per_nm_host, num_executors_per_node_raw,
                                                        llap_container_size))
        assert (num_executors_per_node >= 0), "'Number of executors per node' : {0}. Expected value : > 0".format(
              num_executors_per_node)

        total_mem_for_executors = num_executors_per_node * hive_tez_container_size

        # Calculate value for 'cache' (hive.llap.io.memory.size), a per node config.
        cache_size_per_node = llap_container_size - total_mem_for_executors
        Logger.info("Calculated cache_size_per_node : {0} using following : hive_container_size : {1}, llap_container_size"
              " : {2}, num_executors_per_node : {3}"
              .format(cache_size_per_node, hive_tez_container_size, llap_container_size, num_executors_per_node))
        if cache_size_per_node < 0: # Run with '0' cache.
          Logger.info("Calculated 'cache_size_per_node' : {0}. Setting 'cache_size_per_node' to 0.".format(cache_size_per_node))
          cache_size_per_node = 0


        # Calculate value for prop 'llap_heap_size'
        llap_xmx = max(total_mem_for_executors * 0.8, total_mem_for_executors - 1024)
        Logger.info("Calculated llap_app_heap_size : {0}, using following : hive_container_size : {1}, "
                "total_mem_for_executors : {2}".format(llap_xmx, hive_tez_container_size, total_mem_for_executors))


        # Updating calculated configs.
        if not llap_concurrency_in_changed_configs:
          min_llap_concurrency = 1
          putHiveInteractiveSiteProperty('hive.server2.tez.sessions.per.default.queue', llap_concurrency)
          putHiveInteractiveSitePropertyAttribute('hive.server2.tez.sessions.per.default.queue', "minimum", min_llap_concurrency)
          putHiveInteractiveSitePropertyAttribute('hive.server2.tez.sessions.per.default.queue', "maximum", LLAP_MAX_CONCURRENCY)
          Logger.info("LLAP config 'hive.server2.tez.sessions.per.default.queue' updated. Min : {0}, Current: {1}, Max: {2}" \
                      .format(min_llap_concurrency, llap_concurrency, LLAP_MAX_CONCURRENCY))

        num_llap_nodes = long(num_llap_nodes)

        putHiveInteractiveEnvProperty('num_llap_nodes', num_llap_nodes)
        Logger.info("LLAP config 'num_llap_nodes' updated. Current: {0}".format(num_llap_nodes))

        llap_container_size = long(llap_container_size)
        putHiveInteractiveSiteProperty('hive.llap.daemon.yarn.container.mb', llap_container_size)
        Logger.info("LLAP config 'hive.llap.daemon.yarn.container.mb' updated. Current: {0}".format(llap_container_size))

        num_executors_per_node = long(num_executors_per_node)
        putHiveInteractiveSiteProperty('hive.llap.daemon.num.executors', num_executors_per_node)
        Logger.info("LLAP config 'hive.llap.daemon.num.executors' updated. Current: {0}".format(num_executors_per_node))

        cache_size_per_node = long(cache_size_per_node)
        putHiveInteractiveSiteProperty('hive.llap.io.memory.size', cache_size_per_node)
        Logger.info("LLAP config 'hive.llap.io.memory.size' updated. Current: {0}".format(cache_size_per_node))
        llap_io_enabled = 'false'
        if cache_size_per_node >= 64:
          llap_io_enabled = 'true'

        putHiveInteractiveSiteProperty('hive.llap.io.enabled', llap_io_enabled)
        Logger.info("HiveServer2 config 'hive.llap.io.enabled' updated to '{0}' as part of "
                    "'hive.llap.io.memory.size' calculation.".format(llap_io_enabled))

        llap_xmx = long(llap_xmx)
        putHiveInteractiveEnvProperty('llap_heap_size', llap_xmx)
        Logger.info("LLAP config 'llap_heap_size' updated. Current: {0}".format(llap_xmx))

        slider_am_container_size = long(slider_am_container_size)
        putHiveInteractiveEnvProperty('slider_am_container_size', slider_am_container_size)
        Logger.info("LLAP config 'slider_am_container_size' updated. Current: {0}".format(slider_am_container_size))

    except Exception as e:
      # Set default values, if caught an Exception. The 'llap queue capacity' is left untouched, as it can be increased,
      # triggerring recalculation.
      Logger.info(e.message+" Skipping calculating LLAP configs. Setting them to minimum values.")
      traceback.print_exc()

      try:
        yarn_min_container_size = long(self.get_yarn_min_container_size(services))
        slider_am_container_size = long(self.calculate_slider_am_size(yarn_min_container_size))
        putHiveInteractiveSiteProperty('hive.server2.tez.sessions.per.default.queue', 0)
        putHiveInteractiveSitePropertyAttribute('hive.server2.tez.sessions.per.default.queue', "minimum", 0)
        putHiveInteractiveSitePropertyAttribute('hive.server2.tez.sessions.per.default.queue', "maximum", 0)

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

        putHiveInteractiveEnvProperty('slider_am_container_size', slider_am_container_size)

      except Exception as e:
        Logger.info("Problem setting minimum values for LLAP configs in Exception code.")
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
  Returns current value of cache per node for LLAP (hive.llap.io.memory.size). If value can't be retrieved, return 0.
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
  Gets HIVE Tez container size (hive.tez.container.size)
  """
  def get_hive_tez_container_size(self, services):
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
  Gets YARN NodeManager memory in MB (yarn.nodemanager.resource.memory-mb).
  """
  def get_yarn_nm_mem_in_mb(self, services):
    if 'yarn-site' in services['configurations'] and \
        'yarn.nodemanager.resource.memory-mb' in services['configurations']['yarn-site']['properties']:
      yarn_nm_mem_in_mb = float(
        services['configurations']['yarn-site']['properties']['yarn.nodemanager.resource.memory-mb'])
      assert (
        yarn_nm_mem_in_mb > 0.0), "'yarn.nodemanager.resource.memory-mb' current value : {0}. Expected value : > 0".format(
        yarn_nm_mem_in_mb)
    else:
      raise Fail(
        "Couldn't retrieve YARN's 'yarn.nodemanager.resource.memory-mb' config.")
    return yarn_nm_mem_in_mb

  """
  Gets Tez App Master container size (tez.am.resource.memory.mb)
  """
  def get_tez_am_container_size(self, services):
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
  Minimum 'llap' queue capacity required in order to get LLAP app running.
  """
  def min_llap_queue_perc_required_in_cluster(self, services, hosts):
    # Get llap queue size if sized at 20%
    node_manager_hosts = self.get_node_manager_hosts(services, hosts)
    yarn_rm_mem_in_mb = self.get_yarn_nm_mem_in_mb(services)
    total_cluster_cap = len(node_manager_hosts) * yarn_rm_mem_in_mb
    total_llap_queue_size_at_20_perc = 20.0 / 100 * total_cluster_cap

    # Calculate based on minimum size required by containers.
    yarn_min_container_size = self.get_yarn_min_container_size(services)
    slider_am_size = self.calculate_slider_am_size(yarn_min_container_size)
    hive_tez_container_size = self.get_hive_tez_container_size(services)
    tez_am_container_size = self.get_tez_am_container_size(services)
    normalized_val = self._normalizeUp(slider_am_size, yarn_min_container_size) + self._normalizeUp\
      (hive_tez_container_size, yarn_min_container_size) + self._normalizeUp(tez_am_container_size, yarn_min_container_size)

    min_required = max(total_llap_queue_size_at_20_perc, normalized_val)

    min_required_perc = min_required * 100 / total_cluster_cap
    Logger.info("Calculated min_llap_queue_perc_required_in_cluster : {0} and min_llap_queue_cap_required_in_cluster: {1} "
                "using following : yarn_min_container_size : {2}, ""slider_am_size : {3}, hive_tez_container_size : {4}, "
                "hive_am_container_size : {5}".format(min_required_perc, min_required, yarn_min_container_size,
                slider_am_size, hive_tez_container_size, tez_am_container_size))
    return int(math.ceil(min_required_perc))

  """
  Normalize down 'val2' with respect to 'val1'.
  """
  def _normalizeDown(self, val1, val2):
    tmp = math.floor(val1 / val2)
    if tmp < 1.00:
      return val1
    return tmp * val2

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
    Logger.info("Determining creation/adjustment of 'capacity-scheduler' for 'llap' queue.")
    putHiveInteractiveEnvProperty = self.putProperty(configurations, "hive-interactive-env", services)
    putHiveInteractiveSiteProperty = self.putProperty(configurations, self.HIVE_INTERACTIVE_SITE, services)
    putHiveInteractiveEnvPropertyAttribute = self.putPropertyAttribute(configurations, "hive-interactive-env")
    putCapSchedProperty = self.putProperty(configurations, "capacity-scheduler", services)

    capacity_scheduler_properties, received_as_key_value_pair = self.getCapacitySchedulerProperties(services)
    if capacity_scheduler_properties:
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
      leafQueueNames = self.getAllYarnLeafQueues(capacity_scheduler_properties)
      cap_sched_config_keys = capacity_scheduler_properties.keys()

      yarn_default_queue_capacity = -1
      if 'yarn.scheduler.capacity.root.default.capacity' in cap_sched_config_keys:
        yarn_default_queue_capacity = capacity_scheduler_properties.get('yarn.scheduler.capacity.root.default.capacity')

      # Get 'llap' queue state
      currLlapQueueState = ''
      if 'yarn.scheduler.capacity.root.'+llap_queue_name+'.state' in cap_sched_config_keys:
        currLlapQueueState = capacity_scheduler_properties.get('yarn.scheduler.capacity.root.'+llap_queue_name+'.state')

      # Get 'llap' queue capacity
      currLlapQueueCap = -1
      if 'yarn.scheduler.capacity.root.'+llap_queue_name+'.capacity' in cap_sched_config_keys:
        currLlapQueueCap = int(capacity_scheduler_properties.get('yarn.scheduler.capacity.root.'+llap_queue_name+'.capacity'))

      if self.HIVE_INTERACTIVE_SITE in services['configurations'] and \
          'hive.llap.daemon.queue.name' in services['configurations'][self.HIVE_INTERACTIVE_SITE]['properties']:
        llap_daemon_selected_queue_name =  services['configurations'][self.HIVE_INTERACTIVE_SITE]['properties']['hive.llap.daemon.queue.name']
      else:
        Logger.error("Couldn't retrive 'hive.llap.daemon.queue.name' property. Skipping adjusting queues.")
        return
      updated_cap_sched_configs_str = ''

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

        hive_user = '*'  # Open to all
        if 'hive_user' in services['configurations']['hive-env']['properties']:
          hive_user = services['configurations']['hive-env']['properties']['hive_user']

        llap_slider_cap_percentage = str(llap_slider_cap_percentage)

        # If capacity-scheduler configs are received as one concatenated string, we deposit the changed configs back as
        # one concatenated string.
        updated_cap_sched_configs_as_dict = False
        if not received_as_key_value_pair:
          for prop, val in capacity_scheduler_properties.items():
            if llap_queue_name not in prop:
              if prop == 'yarn.scheduler.capacity.root.queues':
                updated_cap_sched_configs_str = updated_cap_sched_configs_str \
                                            + prop + "=default,llap\n"
              elif prop == 'yarn.scheduler.capacity.root.default.capacity':
                updated_cap_sched_configs_str = updated_cap_sched_configs_str \
                                            + prop + "=" + adjusted_default_queue_cap + "\n"
              elif prop == 'yarn.scheduler.capacity.root.default.maximum-capacity':
                updated_cap_sched_configs_str = updated_cap_sched_configs_str \
                                            + prop + "=" + adjusted_default_queue_cap + "\n"
              elif prop.startswith('yarn.') and '.llap.' not in prop:
                updated_cap_sched_configs_str = updated_cap_sched_configs_str + prop + "=" + val + "\n"

          # Now, append the 'llap' queue related properties
          updated_cap_sched_configs_str = updated_cap_sched_configs_str \
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

          putCapSchedProperty("capacity-scheduler", updated_cap_sched_configs_str)
          Logger.info("Updated 'capacity-scheduler' configs as one concatenated string.")
        else:
          # If capacity-scheduler configs are received as a  dictionary (generally 1st time), we deposit the changed
          # values back as dictionary itself.
          # Update existing configs in 'capacity-scheduler'.
          for prop, val in capacity_scheduler_properties.items():
            if llap_queue_name not in prop:
              if prop == 'yarn.scheduler.capacity.root.queues':
                putCapSchedProperty(prop, 'default,llap')
              elif prop == 'yarn.scheduler.capacity.root.default.capacity':
                putCapSchedProperty(prop, adjusted_default_queue_cap)
              elif prop == 'yarn.scheduler.capacity.root.default.maximum-capacity':
                putCapSchedProperty(prop, adjusted_default_queue_cap)
              elif prop.startswith('yarn.') and '.llap.' not in prop:
                putCapSchedProperty(prop, val)

          # Add new 'llap' queue related configs.
          putCapSchedProperty("yarn.scheduler.capacity.root." + llap_queue_name + ".user-limit-factor", "1")
          putCapSchedProperty("yarn.scheduler.capacity.root." + llap_queue_name + ".state", "RUNNING")
          putCapSchedProperty("yarn.scheduler.capacity.root." + llap_queue_name + ".ordering-policy", "fifo")
          putCapSchedProperty("yarn.scheduler.capacity.root." + llap_queue_name + ".minimum-user-limit-percent", "100")
          putCapSchedProperty("yarn.scheduler.capacity.root." + llap_queue_name + ".maximum-capacity", llap_slider_cap_percentage)
          putCapSchedProperty("yarn.scheduler.capacity.root." + llap_queue_name + ".capacity", llap_slider_cap_percentage)
          putCapSchedProperty("yarn.scheduler.capacity.root." + llap_queue_name + ".acl_submit_applications", hive_user)
          putCapSchedProperty("yarn.scheduler.capacity.root." + llap_queue_name + ".acl_administer_queue", hive_user)
          putCapSchedProperty("yarn.scheduler.capacity.root." + llap_queue_name + ".maximum-am-resource-percent", "1")


          Logger.info("Updated 'capacity-scheduler' configs as a dictionary.")
          updated_cap_sched_configs_as_dict = True

        if updated_cap_sched_configs_str or updated_cap_sched_configs_as_dict:
          if len(leafQueueNames) == 1: # 'llap' queue didn't exist before
            Logger.info("Created YARN Queue : '{0}' with capacity : {1}%. Adjusted 'default' queue capacity to : {2}%" \
                      .format(llap_queue_name, llap_slider_cap_percentage, adjusted_default_queue_cap))
          else: # Queue existed, only adjustments done.
            Logger.info("Adjusted YARN Queue : '{0}'. Current capacity : {1}%. State: RUNNING.".format(llap_queue_name, llap_slider_cap_percentage))
            Logger.info("Adjusted 'default' queue capacity to : {0}%".format(adjusted_default_queue_cap))

          # Update Hive 'hive.llap.daemon.queue.name' prop to use 'llap' queue.
          putHiveInteractiveSiteProperty('hive.llap.daemon.queue.name', 'llap')
          putHiveInteractiveEnvPropertyAttribute('llap_queue_capacity', "minimum", llap_min_reqd_cap_percentage)
          putHiveInteractiveEnvPropertyAttribute('llap_queue_capacity', "maximum", 100)

          # Update 'hive.llap.daemon.queue.name' prop combo entries.
          self.setLlapDaemonQueueNamePropAttributes(services, configurations)
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
    capacity_scheduler_properties, received_as_key_value_pair = self.getCapacitySchedulerProperties(services)
    updated_default_queue_configs = ''
    updated_llap_queue_configs = ''
    if capacity_scheduler_properties:
      # Get all leaf queues.
      leafQueueNames = self.getAllYarnLeafQueues(capacity_scheduler_properties)

      if len(leafQueueNames) == 2 and llap_queue_name in leafQueueNames and 'default' in leafQueueNames:
        # Get 'llap' queue state
        currLlapQueueState = 'STOPPED'
        if 'yarn.scheduler.capacity.root.'+llap_queue_name+'.state' in capacity_scheduler_properties.keys():
          currLlapQueueState = capacity_scheduler_properties.get('yarn.scheduler.capacity.root.'+llap_queue_name+'.state')
        else:
          Logger.error("{0} queue 'state' property not present in capacity scheduler. Skipping adjusting queues.".format(llap_queue_name))
          return
        if currLlapQueueState == 'RUNNING':
          DEFAULT_MAX_CAPACITY = '100'
          for prop, val in capacity_scheduler_properties.items():
            # Update 'default' related configs in 'updated_default_queue_configs'
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
            else: # Update 'llap' related configs in 'updated_llap_queue_configs'
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
  Checks and sets the 'Hive Server Interactive' 'hive.llap.daemon.queue.name' config Property Attributes.  Takes into
  account that 'capacity-scheduler' may have changed (got updated) in current Stack Advisor invocation.
  """
  def setLlapDaemonQueueNamePropAttributes(self, services, configurations):
    Logger.info("Determining 'hive.llap.daemon.queue.name' config Property Attributes.")
    putHiveInteractiveSitePropertyAttribute = self.putPropertyAttribute(configurations, self.HIVE_INTERACTIVE_SITE)
    capacity_scheduler_properties = dict()

    # Read 'capacity-scheduler' from configurations if we modified and added recommendation to it, as part of current
    # StackAdvisor invocation.
    if "capacity-scheduler" in configurations:
        cap_sched_props_as_dict = configurations["capacity-scheduler"]["properties"]
        if 'capacity-scheduler' in cap_sched_props_as_dict:
          cap_sched_props_as_str = configurations['capacity-scheduler']['properties']['capacity-scheduler']
          if cap_sched_props_as_str:
            cap_sched_props_as_str = str(cap_sched_props_as_str).split('\n')
            if len(cap_sched_props_as_str) > 0 and cap_sched_props_as_str[0] != 'null':
              # Got 'capacity-scheduler' configs as one "\n" separated string
              for property in cap_sched_props_as_str:
                key, sep, value = property.partition("=")
                capacity_scheduler_properties[key] = value
              Logger.info("'capacity-scheduler' configs is set as a single '\\n' separated string in current invocation. "
                          "count(configurations['capacity-scheduler']['properties']['capacity-scheduler']) = "
                          "{0}".format(len(capacity_scheduler_properties)))
            else:
              Logger.info("Read configurations['capacity-scheduler']['properties']['capacity-scheduler'] is : {0}".format(cap_sched_props_as_str))
          else:
            Logger.info("configurations['capacity-scheduler']['properties']['capacity-scheduler'] : {0}.".format(cap_sched_props_as_str))

        # if 'capacity_scheduler_properties' is empty, implies we may have 'capacity-scheduler' configs as dictionary
        # in configurations, if 'capacity-scheduler' changed in current invocation.
        if not capacity_scheduler_properties:
          if isinstance(cap_sched_props_as_dict, dict) and len(cap_sched_props_as_dict) > 1:
            capacity_scheduler_properties = cap_sched_props_as_dict
            Logger.info("'capacity-scheduler' changed in current Stack Advisor invocation. Retrieved the configs as dictionary from configurations.")
          else:
            Logger.info("Read configurations['capacity-scheduler']['properties'] is : {0}".format(cap_sched_props_as_dict))
    else:
      Logger.info("'capacity-scheduler' not modified in the current Stack Advisor invocation.")


    # if 'capacity_scheduler_properties' is still empty, implies 'capacity_scheduler' wasn't change in current
    # ST invocation. Thus, read it from input : 'services'.
    if not capacity_scheduler_properties:
      capacity_scheduler_properties, received_as_key_value_pair = self.getCapacitySchedulerProperties(services)
      Logger.info("'capacity-scheduler' not changed in current Stack Advisor invocation. Retrieved the configs from services.")

    # Get set of current YARN leaf queues.
    leafQueueNames = self.getAllYarnLeafQueues(capacity_scheduler_properties)
    if leafQueueNames:
      leafQueues = [{"label": str(queueName), "value": queueName} for queueName in leafQueueNames]
      leafQueues = sorted(leafQueues, key=lambda q: q['value'])
      putHiveInteractiveSitePropertyAttribute("hive.llap.daemon.queue.name", "entries", leafQueues)
      Logger.info("'hive.llap.daemon.queue.name' config Property Attributes set to : {0}".format(leafQueues))
    else:
      Logger.error("Problem retrieving YARN queues. Skipping updating HIVE Server Interactve "
                   "'hive.server2.tez.default.queues' property attributes.")

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
  Returns the dictionary of configs for 'capacity-scheduler'.
  """
  def getCapacitySchedulerProperties(self, services):
    capacity_scheduler_properties = dict()
    received_as_key_value_pair = True
    if "capacity-scheduler" in services['configurations']:
      if "capacity-scheduler" in services['configurations']["capacity-scheduler"]["properties"]:
        cap_sched_props_as_str = services['configurations']["capacity-scheduler"]["properties"]["capacity-scheduler"]
        if cap_sched_props_as_str:
          cap_sched_props_as_str = str(cap_sched_props_as_str).split('\n')
          if len(cap_sched_props_as_str) > 0 and cap_sched_props_as_str[0] != 'null':
            # Received confgs as one "\n" separated string
            for property in cap_sched_props_as_str:
              key, sep, value = property.partition("=")
              capacity_scheduler_properties[key] = value
            Logger.info("'capacity-scheduler' configs is passed-in as a single '\\n' separated string. "
                        "count(services['configurations']['capacity-scheduler']['properties']['capacity-scheduler']) = "
                        "{0}".format(len(capacity_scheduler_properties)))
            received_as_key_value_pair = False
          else:
            Logger.info("Passed-in services['configurations']['capacity-scheduler']['properties']['capacity-scheduler'] is 'null'.")
        else:
          Logger.info("'capacity-schdeuler' configs not passed-in as single '\\n' string in "
                      "services['configurations']['capacity-scheduler']['properties']['capacity-scheduler'].")
      if not capacity_scheduler_properties:
        # Received configs as a dictionary (Generally on 1st invocation).
        capacity_scheduler_properties = services['configurations']["capacity-scheduler"]["properties"]
        Logger.info("'capacity-scheduler' configs is passed-in as a dictionary. "
                    "count(services['configurations']['capacity-scheduler']['properties']) = {0}".format(len(capacity_scheduler_properties)))
    else:
      Logger.error("Couldn't retrieve 'capacity-scheduler' from services.")

    Logger.info("Retrieved 'capacity-scheduler' received as dictionary : '{0}'. configs : {1}"\
                .format(received_as_key_value_pair, capacity_scheduler_properties.items()))
    return capacity_scheduler_properties, received_as_key_value_pair

  def recommendRangerKMSConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP25StackAdvisor, self).recommendRangerKMSConfigurations(configurations, clusterData, services, hosts)
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    putRangerKmsSiteProperty = self.putProperty(configurations, "kms-site", services)

    if 'ranger-env' in services['configurations'] and 'ranger_user' in services['configurations']['ranger-env']['properties']:
      rangerUser = services['configurations']['ranger-env']['properties']['ranger_user']

      if 'kms-site' in services['configurations'] and 'KERBEROS' in servicesList:
        putRangerKmsSiteProperty('hadoop.kms.proxyuser.{0}.groups'.format(rangerUser), '*')
        putRangerKmsSiteProperty('hadoop.kms.proxyuser.{0}.hosts'.format(rangerUser), '*')
        putRangerKmsSiteProperty('hadoop.kms.proxyuser.{0}.users'.format(rangerUser), '*')

  def recommendRangerConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP25StackAdvisor, self).recommendRangerConfigurations(configurations, clusterData, services, hosts)
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

    putTagsyncAppProperty = self.putProperty(configurations, "tagsync-application-properties", services)
    putTagsyncSiteProperty = self.putProperty(configurations, "ranger-tagsync-site", services)
    putRangerAdminProperty = self.putProperty(configurations, "ranger-admin-site", services)
    putRangerEnvProperty = self.putProperty(configurations, "ranger-env", services)

    has_ranger_tagsync = False
    if 'RANGER' in servicesList:
      ranger_tagsync_host = self.__getHostsForComponent(services, "RANGER", "RANGER_TAGSYNC")
      has_ranger_tagsync = len(ranger_tagsync_host) > 0

    if 'ATLAS' in servicesList and has_ranger_tagsync:
      putTagsyncSiteProperty('ranger.tagsync.source.atlas', 'true')
    else:
      putTagsyncSiteProperty('ranger.tagsync.source.atlas', 'false')

    zookeeper_host_port = self.getZKHostPortString(services)
    if zookeeper_host_port and has_ranger_tagsync:
      zookeeper_host_list = zookeeper_host_port.split(',')
      putTagsyncAppProperty('atlas.kafka.zookeeper.connect', zookeeper_host_list[0])
    else:
      putTagsyncAppProperty('atlas.kafka.zookeeper.connect', 'localhost:2181')

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
      putTagsyncAppProperty('atlas.kafka.bootstrap.servers', 'localhost:6667')

    if 'LOGSEARCH' in servicesList and zookeeper_host_port:
      putRangerEnvProperty('is_solrCloud_enabled', 'true')
      zookeeper_host_port = zookeeper_host_port.split(',')
      zookeeper_host_port.sort()
      zookeeper_host_port = ",".join(zookeeper_host_port)
      logsearch_solr_znode = '/logsearch'
      ranger_audit_zk_port = ''
      if 'logsearch-solr-env' in services['configurations'] and \
        ('logsearch_solr_znode' in services['configurations']['logsearch-solr-env']['properties']):
        logsearch_solr_znode = services['configurations']['logsearch-solr-env']['properties']['logsearch_solr_znode']
        ranger_audit_zk_port = '{0}{1}'.format(zookeeper_host_port, logsearch_solr_znode)
      putRangerAdminProperty('ranger.audit.solr.zookeepers', ranger_audit_zk_port)
    else:
      putRangerEnvProperty('is_solrCloud_enabled', 'false')

    if 'ranger-env' in configurations and configurations["ranger-env"]["properties"]["is_solrCloud_enabled"]:
      isSolrCloudEnabled = configurations and configurations["ranger-env"]["properties"]["is_solrCloud_enabled"] == "true"
    elif 'ranger-env' in services['configurations'] and 'is_solrCloud_enabled' in services['configurations']["ranger-env"]["properties"]:
      isSolrCloudEnabled = services['configurations']["ranger-env"]["properties"]["is_solrCloud_enabled"]  == "true"
    else:
      isSolrCloudEnabled = False

    if not isSolrCloudEnabled:
      putRangerAdminProperty('ranger.audit.solr.zookeepers', 'NONE')

    ranger_services = [
      {'service_name': 'HDFS', 'audit_file': 'ranger-hdfs-audit'},
      {'service_name': 'YARN', 'audit_file': 'ranger-yarn-audit'},
      {'service_name': 'HBASE', 'audit_file': 'ranger-hbase-audit'},
      {'service_name': 'HIVE', 'audit_file': 'ranger-hive-audit'},
      {'service_name': 'KNOX', 'audit_file': 'ranger-knox-audit'},
      {'service_name': 'KAFKA', 'audit_file': 'ranger-kafka-audit'},
      {'service_name': 'STORM', 'audit_file': 'ranger-storm-audit'},
      {'service_name': 'RANGER_KMS', 'audit_file': 'ranger-kms-site'}
    ]

    for item in range(len(ranger_services)):
      if ranger_services[item]['service_name'] in servicesList:
        component_audit_file =  ranger_services[item]['audit_file']
        if component_audit_file in services["configurations"]:
          ranger_audit_dict = [
            {'filename': 'ranger-admin-site', 'configname': 'ranger.audit.solr.urls', 'target_configname': 'xasecure.audit.destination.solr.urls'},
            {'filename': 'ranger-admin-site', 'configname': 'ranger.audit.solr.zookeepers', 'target_configname': 'xasecure.audit.destination.solr.zookeepers'}
          ]
          putRangerAuditProperty = self.putProperty(configurations, component_audit_file, services)

          for item in ranger_audit_dict:
            if item['filename'] in services["configurations"] and item['configname'] in  services["configurations"][item['filename']]["properties"]:
              if item['filename'] in configurations and item['configname'] in  configurations[item['filename']]["properties"]:
                rangerAuditProperty = configurations[item['filename']]["properties"][item['configname']]
              else:
                rangerAuditProperty = services["configurations"][item['filename']]["properties"][item['configname']]
              putRangerAuditProperty(item['target_configname'], rangerAuditProperty)

  def validateRangerTagsyncConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    ranger_tagsync_properties = getSiteProperties(configurations, "ranger-tagsync-site")
    validationItems = []
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

    has_atlas = False
    if "RANGER" in servicesList:
      has_atlas = not "ATLAS" in servicesList

      if has_atlas and 'ranger.tagsync.source.atlas' in ranger_tagsync_properties and \
        ranger_tagsync_properties['ranger.tagsync.source.atlas'].lower() == 'true':
        validationItems.append({"config-name": "ranger.tagsync.source.atlas",
                                  "item": self.getWarnItem(
                                    "Need to Install ATLAS service to set ranger.tagsync.source.atlas as true.")})

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
