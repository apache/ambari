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

from resource_management.core.logger import Logger

class HDP25StackAdvisor(HDP24StackAdvisor):

  def __init__(self):
    super(HDP25StackAdvisor, self).__init__()
    Logger.initialize_logger()

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
      "HIVE": self.recommendHIVEConfigurations
    }
    parentRecommendConfDict.update(childRecommendConfDict)
    return parentRecommendConfDict

  def recommendHIVEConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP25StackAdvisor, self).recommendHIVEConfigurations(configurations, clusterData, services, hosts)
    putHiveInteractiveEnvProperty = self.putProperty(configurations, "hive-interactive-env", services)

    # For 'Hive Server Interactive', if the component exists.
    hsi_hosts = self.__getHostsForComponent(services, "HIVE", "HIVE_SERVER_INTERACTIVE")
    if len(hsi_hosts) > 0:
      hsi_host = hsi_hosts[0]
      putHiveInteractiveEnvProperty('enable_hive_interactive', 'true')
      putHiveInteractiveEnvProperty('hive_server_interactive_host', hsi_host)

      if 'hive.llap.zk.sm.connectionString' in services['configurations']['hive-interactive-site']['properties']:
        # Fill the property 'hive.llap.zk.sm.connectionString' required by Hive Server Interactive (HiveServer2)
        zookeeper_host_port = self.getZKHostPortString(services)
        if zookeeper_host_port:
          putHiveInteractiveSiteProperty = self.putProperty(configurations, "hive-interactive-site", services)
          putHiveInteractiveSiteProperty("hive.llap.zk.sm.connectionString", zookeeper_host_port)

      # Update 'hive.llap.daemon.queue.name' if capacity scheduler is changed.
      if 'hive.llap.daemon.queue.name' in services['configurations']['hive-interactive-site']['properties']:
        self.setLlapDaemonQueueName(services, configurations)
    else:
      putHiveInteractiveEnvProperty('enable_hive_interactive', 'false')
    pass



  def recommendYARNConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP25StackAdvisor, self).recommendYARNConfigurations(configurations, clusterData, services, hosts)

    # Queue 'llap' creation/removal logic (Used by Hive Interactive server and associated LLAP)
    hsi_hosts = self.__getHostsForComponent(services, "HIVE", "HIVE_SERVER_INTERACTIVE")
    if len(hsi_hosts) > 0:
      if 'hive-interactive-env' in services['configurations'] and \
          'enable_hive_interactive' in services['configurations']['hive-interactive-env']['properties']:
        enable_hive_interactive = services['configurations']['hive-interactive-env']['properties']['enable_hive_interactive']
        llap_queue_name = 'llap'

        # Hive Server interactive is already added or getting added
        if enable_hive_interactive == 'true':
          self.checkAndManageLlapQueue(services, configurations, llap_queue_name)
        else:  # When Hive Interactive Server is in 'off/removed' state.
          self.checkAndStopLlapQueue(services, configurations, llap_queue_name)

  """
  Checks and (1). Creates 'llap' queue if only 'default' queue exist at leaf level and is consuming 100% capacity OR
             (2). Updates 'llap' queue capacity and state, if 'llap' queue exists.
  """
  def checkAndManageLlapQueue(self, services, configurations, llap_queue_name):
    DEFAULT_LLAP_QUEUE_CAP_PERCENT = 20
    putHiveInteractiveEnvProperty = self.putProperty(configurations, "hive-interactive-env", services)
    putHiveInteractiveSiteProperty = self.putProperty(configurations, "hive-interactive-site", services)
    putCapSchedProperty = self.putProperty(configurations, "capacity-scheduler", services)

    capacitySchedulerProperties = self.getCapacitySchedulerProperties(services)

    if capacitySchedulerProperties:
      # Get the llap Cluster percentage used for 'llap' Queue creation
      if 'llap_queue_capacity' in services['configurations']['hive-interactive-env']['properties']:
        llapSliderCapPercentage = int(
          services['configurations']['hive-interactive-env']['properties']['llap_queue_capacity'])
        if llapSliderCapPercentage not in range(1,101):
          Logger.debug("Adjusting HIVE 'llap_queue_capacity' from {0}% to {1}%".format(llapSliderCapPercentage, DEFAULT_LLAP_QUEUE_CAP_PERCENT))
          llapSliderCapPercentage = DEFAULT_LLAP_QUEUE_CAP_PERCENT  # Set the default value to 20.
          putHiveInteractiveEnvProperty('llap_queue_capacity', llapSliderCapPercentage)
      else:
        Logger.error("Problem retrieving LLAP Queue Capacity. Skipping creating {0} queue".format(llap_queue_name))
        return

      leafQueueNames = self.getAllYarnLeafQueues(capacitySchedulerProperties)
      capSchedConfigKeys = capacitySchedulerProperties.keys()

      yarn_default_queue_capacity = -1
      if 'yarn.scheduler.capacity.root.capacity' in capSchedConfigKeys:
        yarn_default_queue_capacity = capacitySchedulerProperties.get('yarn.scheduler.capacity.root.capacity')

      # Get 'llap' queue state
      currLlapQueueState = ''
      if 'yarn.scheduler.capacity.root.'+llap_queue_name+'.state' in capSchedConfigKeys:
        currLlapQueueState = capacitySchedulerProperties.get('yarn.scheduler.capacity.root.'+llap_queue_name+'.state')

      # Get 'llap' queue capacity
      currLlapQueueCap = -1
      if 'yarn.scheduler.capacity.root.'+llap_queue_name+'.capacity' in capSchedConfigKeys:
        currLlapQueueCap = capacitySchedulerProperties.get('yarn.scheduler.capacity.root.'+llap_queue_name+'.capacity')

      updated_cap_sched_configs = ''

      """
      We create OR "modify 'llap' queue 'state and/or capacity' " based on below conditions:
       - if only 1 queue exists at root level and is 'default' queue and has 100% cap -> Create 'llap' queue,  OR
       - if 2 queues exists at root level ('llap' and 'default') :
           - 'llap' queue state is STOPPED -> Modify 'llap' queue state to RUNNING, adjust capacity, OR
           - 'llap' queue state is RUNNING and 'llap_queue_capacity' prop != 'llap' queue current running capacity ->
              Modify 'llap' queue capacity to 'llap_queue_capacity'
      """
      if 'default' in leafQueueNames and \
        ((len(leafQueueNames) == 1 and int(yarn_default_queue_capacity) == 100) or \
        ((len(leafQueueNames) == 2 and llap_queue_name in leafQueueNames) and \
           (currLlapQueueState == 'STOPPED' or (currLlapQueueState == 'RUNNING' and currLlapQueueCap != llapSliderCapPercentage)))):
        adjusted_default_queue_cap = str(int(yarn_default_queue_capacity) - llapSliderCapPercentage)
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

        llapSliderCapPercentage = str(llapSliderCapPercentage)
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
                                    + llapSliderCapPercentage + "\n" \
                                    + "yarn.scheduler.capacity.root." + llap_queue_name + ".capacity=" \
                                    + llapSliderCapPercentage + "\n" \
                                    + "yarn.scheduler.capacity.root." + llap_queue_name + ".acl_submit_applications=" \
                                    + hive_user + "\n" \
                                    + "yarn.scheduler.capacity.root." + llap_queue_name + ".acl_administer_queue=" \
                                    + hive_user + "\n" \
                                    + "yarn.scheduler.capacity.root." + llap_queue_name + ".maximum-am-resource-percent=1"

        if updated_cap_sched_configs:
          putCapSchedProperty("capacity-scheduler", updated_cap_sched_configs)
          if len(leafQueueNames) == 1: # 'llap' queue didn't exist before
            Logger.info("Created YARN Queue : '{0}' with capacity : {1}%. Adjusted default queue capacity to : {2}%" \
                      .format(llap_queue_name, llapSliderCapPercentage, adjusted_default_queue_cap))
          else: # Queue existed, only adjustments done.
            Logger.info("Adjusted YARN Queue : '{0}'. Current capacity : {1}%. State: RUNNING.".format(llap_queue_name, llapSliderCapPercentage))
            Logger.info("Adjusted 'default' queue capacity to : {0}%".format(adjusted_default_queue_cap))

          # Update Hive 'hive.llap.daemon.queue.name' prop to use 'llap' queue.
          putHiveInteractiveSiteProperty('hive.llap.daemon.queue.name', 'llap')
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
    putHiveInteractiveSiteProperty = self.putProperty(configurations, "hive-interactive-site", services)

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
    putHiveInteractiveSitePropertyAttribute = self.putPropertyAttribute(configurations, "hive-interactive-site")
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
