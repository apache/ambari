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

# Python imports
import imp
import os
import traceback
import inspect
import socket
import math
from math import floor, ceil

# Local imports


SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
STACKS_DIR = os.path.join(SCRIPT_DIR, '../../../stacks/')
PARENT_FILE = os.path.join(STACKS_DIR, 'service_advisor.py')

try:
  with open(PARENT_FILE, 'rb') as fp:
    service_advisor = imp.load_module('service_advisor', fp, PARENT_FILE, ('.py', 'rb', imp.PY_SOURCE))
except Exception as e:
  traceback.print_exc()
  print "Failed to load parent"


class YARNServiceAdvisor(service_advisor.ServiceAdvisor):

  def __init__(self, *args, **kwargs):
    self.as_super = super(YARNServiceAdvisor, self)
    self.as_super.__init__(*args, **kwargs)

    self.initialize_logger("YARNServiceAdvisorf")

    self.CLUSTER_CREATE_OPERATION = "ClusterCreate"

    # Always call these methods
    self.modifyMastersWithMultipleInstances()
    self.modifyCardinalitiesDict()
    self.modifyHeapSizeProperties()
    self.modifyNotValuableComponents()
    self.modifyComponentsNotPreferableOnServer()
    self.modifyComponentLayoutSchemes()

  def modifyMastersWithMultipleInstances(self):
    """
    Modify the set of masters with multiple instances.
    Must be overriden in child class.
    """
    # Nothing to do
    pass

  def modifyCardinalitiesDict(self):
    """
    Modify the dictionary of cardinalities.
    Must be overriden in child class.
    """
    # Nothing to do
    pass

  def modifyHeapSizeProperties(self):
    """
    Modify the dictionary of heap size properties.
    Must be overriden in child class.
    """
    self.heap_size_properties = {}

  def modifyNotValuableComponents(self):
    """
    Modify the set of components whose host assignment is based on other services.
    Must be overriden in child class.
    """
    self.notValuableComponents.add("APP_TIMELINE_SERVER")

  def modifyComponentsNotPreferableOnServer(self):
    """
    Modify the set of components that are not preferable on the server.
    Must be overriden in child class.
    """
    # Nothing to do
    pass

  def modifyComponentLayoutSchemes(self):
    """
    Modify layout scheme dictionaries for components.
    The scheme dictionary basically maps the number of hosts to
    host index where component should exist.
    Must be overriden in child class.
    """
    self.componentLayoutSchemes.update({
      'APP_TIMELINE_SERVER': {31: 1, "else": 2},
    })

  def getServiceComponentLayoutValidations(self, services, hosts):
    """
    Get a list of errors.
    Must be overriden in child class.
    """
    self.logger.info("Class: %s, Method: %s. Validating Service Component Layout." %
                (self.__class__.__name__, inspect.stack()[0][3]))

    return self.as_super.getServiceComponentLayoutValidations(services, hosts)

  def getServiceConfigurationRecommendations(self, configurations, clusterData, services, hosts):
    """
    Entry point.
    Must be overriden in child class.
    """
    self.logger.info("Class: %s, Method: %s. Recommending Service Configurations." %
                (self.__class__.__name__, inspect.stack()[0][3]))

    # Due to the existing stack inheritance, make it clear where each calculation came from.
    recommender = YARNRecommender()

    # YARN
    recommender.recommendYARNConfigurationsFromHDP206(configurations, clusterData, services, hosts)
    recommender.recommendYARNConfigurationsFromHDP22(configurations, clusterData, services, hosts)
    recommender.recommendYARNConfigurationsFromHDP23(configurations, clusterData, services, hosts)
    recommender.recommendYARNConfigurationsFromHDP25(configurations, clusterData, services, hosts)
    recommender.recommendYARNConfigurationsFromHDP26(configurations, clusterData, services, hosts)

  def getServiceConfigurationsValidationItems(self, configurations, recommendedDefaults, services, hosts):
    """
    Entry point.
    Validate configurations for the service. Return a list of errors.
    The code for this function should be the same for each Service Advisor.
    """
    self.logger.info("Class: %s, Method: %s. Validating Configurations." %
                (self.__class__.__name__, inspect.stack()[0][3]))

    validator = YARNValidator()
    # Calls the methods of the validator using arguments,
    # method(siteProperties, siteRecommendations, configurations, services, hosts)
    return validator.validateListOfConfigUsingMethod(configurations, recommendedDefaults, services, hosts, validator.validators)


class MAPREDUCE2ServiceAdvisor(service_advisor.ServiceAdvisor):

  def __init__(self, *args, **kwargs):
    self.as_super = super(MAPREDUCE2ServiceAdvisor, self)
    self.as_super.__init__(*args, **kwargs)

    # Always call these methods
    self.modifyMastersWithMultipleInstances()
    self.modifyCardinalitiesDict()
    self.modifyHeapSizeProperties()
    self.modifyNotValuableComponents()
    self.modifyComponentsNotPreferableOnServer()
    self.modifyComponentLayoutSchemes()

  def modifyMastersWithMultipleInstances(self):
    """
    Modify the set of masters with multiple instances.
    Must be overriden in child class.
    """
    # Nothing to do
    pass

  def modifyCardinalitiesDict(self):
    """
    Modify the dictionary of cardinalities.
    Must be overriden in child class.
    """
    # Nothing to do
    pass

  def modifyHeapSizeProperties(self):
    """
    Modify the dictionary of heap size properties.
    Must be overriden in child class.
    """
    self.heap_size_properties = {}

  def modifyNotValuableComponents(self):
    """
    Modify the set of components whose host assignment is based on other services.
    Must be overriden in child class.
    """
    # Nothing to do
    pass

  def modifyComponentsNotPreferableOnServer(self):
    """
    Modify the set of components that are not preferable on the server.
    Must be overriden in child class.
    """
    # Nothing to do
    pass

  def modifyComponentLayoutSchemes(self):
    """
    Modify layout scheme dictionaries for components.
    The scheme dictionary basically maps the number of hosts to
    host index where component should exist.
    Must be overriden in child class.
    """
    self.componentLayoutSchemes.update({
      'HISTORYSERVER': {31: 1, "else": 2},
    })

  def getServiceComponentLayoutValidations(self, services, hosts):
    """
    Get a list of errors.
    Must be overriden in child class.
    """
    self.logger.info("Class: %s, Method: %s. Validating Service Component Layout." %
                (self.__class__.__name__, inspect.stack()[0][3]))

    return self.as_super.getServiceComponentLayoutValidations(services, hosts)

  def getServiceConfigurationRecommendations(self, configurations, clusterData, services, hosts):
    """
    Entry point.
    Must be overriden in child class.
    """
    self.logger.info("Class: %s, Method: %s. Recommending Service Configurations." %
                (self.__class__.__name__, inspect.stack()[0][3]))

    # Due to the existing stack inheritance, make it clear where each calculation came from.
    recommender = MAPREDUCE2Recommender()
    recommender.recommendMapReduce2ConfigurationsFromHDP206(configurations, clusterData, services, hosts)
    recommender.recommendMapReduce2ConfigurationsFromHDP22(configurations, clusterData, services, hosts)

  def getServiceConfigurationsValidationItems(self, configurations, recommendedDefaults, services, hosts):
    """
    Entry point.
    Validate configurations for the service. Return a list of errors.
    The code for this function should be the same for each Service Advisor.
    """
    self.logger.info("Class: %s, Method: %s. Validating Configurations." %
                (self.__class__.__name__, inspect.stack()[0][3]))

    validator = YARNValidator()
    # Calls the methods of the validator using arguments,
    # method(siteProperties, siteRecommendations, configurations, services, hosts)
    return validator.validateListOfConfigUsingMethod(configurations, recommendedDefaults, services, hosts, validator.validators)


class YARNRecommender(service_advisor.ServiceAdvisor):
  """
  YARN Recommender suggests properties when adding the service for the first time or modifying configs via the UI.
  """

  HIVE_INTERACTIVE_SITE = 'hive-interactive-site'
  YARN_ROOT_DEFAULT_QUEUE_NAME = 'default'
  CONFIG_VALUE_UINITIALIZED = 'SET_ON_FIRST_INVOCATION'

  def __init__(self, *args, **kwargs):
    self.as_super = super(YARNRecommender, self)
    self.as_super.__init__(*args, **kwargs)

  def recommendYARNConfigurationsFromHDP206(self, configurations, clusterData, services, hosts):
    """
    Recommend configurations for this service based on HDP 2.0.6.
    """
    self.logger.info("Class: %s, Method: %s. Recommending Service Configurations." %
                (self.__class__.__name__, inspect.stack()[0][3]))

    putYarnProperty = self.putProperty(configurations, "yarn-site", services)
    putYarnPropertyAttribute = self.putPropertyAttribute(configurations, "yarn-site")
    putYarnEnvProperty = self.putProperty(configurations, "yarn-env", services)

    self.calculateYarnAllocationSizes(configurations, services, hosts)

    putYarnEnvProperty('min_user_id', self.get_system_min_uid())

    yarn_mount_properties = [
      ("yarn.nodemanager.local-dirs", "NODEMANAGER", "/hadoop/yarn/local", "multi"),
      ("yarn.nodemanager.log-dirs", "NODEMANAGER", "/hadoop/yarn/log", "multi"),
      ("yarn.timeline-service.leveldb-timeline-store.path", "APP_TIMELINE_SERVER", "/hadoop/yarn/timeline", "single"),
      ("yarn.timeline-service.leveldb-state-store.path", "APP_TIMELINE_SERVER", "/hadoop/yarn/timeline", "single")
    ]

    self.updateMountProperties("yarn-site", yarn_mount_properties, configurations, services, hosts)

    sc_queue_name = self.recommendYarnQueue(services, "yarn-env", "service_check.queue.name")
    if sc_queue_name is not None:
      putYarnEnvProperty("service_check.queue.name", sc_queue_name)

    containerExecutorGroup = 'hadoop'
    if 'cluster-env' in services['configurations'] and 'user_group' in services['configurations']['cluster-env']['properties']:
      containerExecutorGroup = services['configurations']['cluster-env']['properties']['user_group']
    putYarnProperty("yarn.nodemanager.linux-container-executor.group", containerExecutorGroup)

    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if "TEZ" in servicesList:
      ambari_user = self.getAmbariUser(services)
      ambariHostName = socket.getfqdn()
      putYarnProperty("yarn.timeline-service.http-authentication.proxyuser.{0}.hosts".format(ambari_user), ambariHostName)
      putYarnProperty("yarn.timeline-service.http-authentication.proxyuser.{0}.groups".format(ambari_user), "*")
      old_ambari_user = self.getOldAmbariUser(services)
      if old_ambari_user is not None:
        putYarnPropertyAttribute("yarn.timeline-service.http-authentication.proxyuser.{0}.hosts".format(old_ambari_user), 'delete', 'true')
        putYarnPropertyAttribute("yarn.timeline-service.http-authentication.proxyuser.{0}.groups".format(old_ambari_user), 'delete', 'true')

  def recommendYARNConfigurationsFromHDP22(self, configurations, clusterData, services, hosts):
    putYarnProperty = self.putProperty(configurations, "yarn-site", services)
    putYarnProperty('yarn.nodemanager.resource.cpu-vcores', clusterData['cpu'])
    putYarnProperty('yarn.scheduler.minimum-allocation-vcores', 1)
    putYarnProperty('yarn.scheduler.maximum-allocation-vcores', configurations["yarn-site"]["properties"]["yarn.nodemanager.resource.cpu-vcores"])
    # Property Attributes
    putYarnPropertyAttribute = self.putPropertyAttribute(configurations, "yarn-site")
    nodeManagerHost = self.getHostWithComponent("YARN", "NODEMANAGER", services, hosts)
    if (nodeManagerHost is not None):
      cpuPercentageLimit = 80.0
      if "yarn-site" in services["configurations"] and "yarn.nodemanager.resource.percentage-physical-cpu-limit" in services["configurations"]["yarn-site"]["properties"]:
        cpuPercentageLimit = float(services["configurations"]["yarn-site"]["properties"]["yarn.nodemanager.resource.percentage-physical-cpu-limit"])
      cpuLimit = max(1, int(floor(nodeManagerHost["Hosts"]["cpu_count"] * (cpuPercentageLimit / 100.0))))
      putYarnProperty('yarn.nodemanager.resource.cpu-vcores', str(cpuLimit))
      putYarnProperty('yarn.scheduler.maximum-allocation-vcores', configurations["yarn-site"]["properties"]["yarn.nodemanager.resource.cpu-vcores"])
      putYarnPropertyAttribute('yarn.nodemanager.resource.memory-mb', 'maximum', int(nodeManagerHost["Hosts"]["total_mem"] / 1024)) # total_mem in kb
      putYarnPropertyAttribute('yarn.nodemanager.resource.cpu-vcores', 'maximum', nodeManagerHost["Hosts"]["cpu_count"] * 2)
      putYarnPropertyAttribute('yarn.scheduler.minimum-allocation-vcores', 'maximum', configurations["yarn-site"]["properties"]["yarn.nodemanager.resource.cpu-vcores"])
      putYarnPropertyAttribute('yarn.scheduler.maximum-allocation-vcores', 'maximum', configurations["yarn-site"]["properties"]["yarn.nodemanager.resource.cpu-vcores"])

      kerberos_authentication_enabled = self.isSecurityEnabled(services)
      if kerberos_authentication_enabled:
        putYarnProperty('yarn.nodemanager.container-executor.class',
                        'org.apache.hadoop.yarn.server.nodemanager.LinuxContainerExecutor')

      if "yarn-env" in services["configurations"] and "yarn_cgroups_enabled" in services["configurations"]["yarn-env"]["properties"]:
        yarn_cgroups_enabled = services["configurations"]["yarn-env"]["properties"]["yarn_cgroups_enabled"].lower() == "true"
        if yarn_cgroups_enabled:
          putYarnProperty('yarn.nodemanager.container-executor.class', 'org.apache.hadoop.yarn.server.nodemanager.LinuxContainerExecutor')
          putYarnProperty('yarn.nodemanager.linux-container-executor.group', 'hadoop')
          putYarnProperty('yarn.nodemanager.linux-container-executor.resources-handler.class', 'org.apache.hadoop.yarn.server.nodemanager.util.CgroupsLCEResourcesHandler')
          putYarnProperty('yarn.nodemanager.linux-container-executor.cgroups.hierarchy', '/yarn')
          putYarnProperty('yarn.nodemanager.linux-container-executor.cgroups.mount', 'true')
          putYarnProperty('yarn.nodemanager.linux-container-executor.cgroups.mount-path', '/cgroup')
        else:
          if not kerberos_authentication_enabled:
            putYarnProperty('yarn.nodemanager.container-executor.class', 'org.apache.hadoop.yarn.server.nodemanager.DefaultContainerExecutor')
          putYarnPropertyAttribute('yarn.nodemanager.linux-container-executor.resources-handler.class', 'delete', 'true')
          putYarnPropertyAttribute('yarn.nodemanager.linux-container-executor.cgroups.hierarchy', 'delete', 'true')
          putYarnPropertyAttribute('yarn.nodemanager.linux-container-executor.cgroups.mount', 'delete', 'true')
          putYarnPropertyAttribute('yarn.nodemanager.linux-container-executor.cgroups.mount-path', 'delete', 'true')
    # recommend hadoop.registry.rm.enabled based on SLIDER in services
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if "SLIDER" in servicesList:
      putYarnProperty('hadoop.registry.rm.enabled', 'true')
    else:
      putYarnProperty('hadoop.registry.rm.enabled', 'false')

  def recommendYARNConfigurationsFromHDP23(self, configurations, clusterData, services, hosts):
    putYarnSiteProperty = self.putProperty(configurations, "yarn-site", services)
    putYarnSitePropertyAttributes = self.putPropertyAttribute(configurations, "yarn-site")

    if "tez-site" not in services["configurations"]:
      putYarnSiteProperty('yarn.timeline-service.entity-group-fs-store.group-id-plugin-classes', '')
    else:
      putYarnSiteProperty('yarn.timeline-service.entity-group-fs-store.group-id-plugin-classes', 'org.apache.tez.dag.history.logging.ats.TimelineCachePluginImpl')

    if "ranger-env" in services["configurations"] and "ranger-yarn-plugin-properties" in services["configurations"] and \
            "ranger-yarn-plugin-enabled" in services["configurations"]["ranger-env"]["properties"]:
      putYarnRangerPluginProperty = self.putProperty(configurations, "ranger-yarn-plugin-properties", services)
      rangerEnvYarnPluginProperty = services["configurations"]["ranger-env"]["properties"]["ranger-yarn-plugin-enabled"]
      putYarnRangerPluginProperty("ranger-yarn-plugin-enabled", rangerEnvYarnPluginProperty)
    rangerPluginEnabled = ''
    if 'ranger-yarn-plugin-properties' in configurations and 'ranger-yarn-plugin-enabled' in configurations['ranger-yarn-plugin-properties']['properties']:
      rangerPluginEnabled = configurations['ranger-yarn-plugin-properties']['properties']['ranger-yarn-plugin-enabled']
    elif 'ranger-yarn-plugin-properties' in services['configurations'] and 'ranger-yarn-plugin-enabled' in services['configurations']['ranger-yarn-plugin-properties']['properties']:
      rangerPluginEnabled = services['configurations']['ranger-yarn-plugin-properties']['properties']['ranger-yarn-plugin-enabled']

    if rangerPluginEnabled and (rangerPluginEnabled.lower() == 'Yes'.lower()):
      putYarnSiteProperty('yarn.acl.enable','true')
      putYarnSiteProperty('yarn.authorization-provider','org.apache.ranger.authorization.yarn.authorizer.RangerYarnAuthorizer')
    else:
      putYarnSitePropertyAttributes('yarn.authorization-provider', 'delete', 'true')

  def recommendYARNConfigurationsFromHDP25(self, configurations, clusterData, services, hosts):
    hsi_env_poperties = self.getServicesSiteProperties(services, "hive-interactive-env")
    cluster_env = self.getServicesSiteProperties(services, "cluster-env")

    # Queue 'llap' creation/removal logic (Used by Hive Interactive server and associated LLAP)
    if hsi_env_poperties and 'enable_hive_interactive' in hsi_env_poperties:
      enable_hive_interactive = hsi_env_poperties['enable_hive_interactive']
      LLAP_QUEUE_NAME = 'llap'

      # Hive Server interactive is already added or getting added
      if enable_hive_interactive == 'true':
        self.updateLlapConfigs(configurations, services, hosts, LLAP_QUEUE_NAME)
      else:  # When Hive Interactive Server is in 'off/removed' state.
        self.checkAndStopLlapQueue(services, configurations, LLAP_QUEUE_NAME)

    putYarnSiteProperty = self.putProperty(configurations, "yarn-site", services)
    stack_root = "/usr/hdp"
    if cluster_env and "stack_root" in cluster_env:
      stack_root = cluster_env["stack_root"]

    timeline_plugin_classes_values = []
    timeline_plugin_classpath_values = []

    if self.isServiceDeployed(services, "TEZ"):
      timeline_plugin_classes_values.append('org.apache.tez.dag.history.logging.ats.TimelineCachePluginImpl')

    if self.isServiceDeployed(services, "SPARK"):
      timeline_plugin_classes_values.append('org.apache.spark.deploy.history.yarn.plugin.SparkATSPlugin')
      timeline_plugin_classpath_values.append(stack_root + "/${hdp.version}/spark/hdpLib/*")

    putYarnSiteProperty('yarn.timeline-service.entity-group-fs-store.group-id-plugin-classes', ",".join(timeline_plugin_classes_values))
    putYarnSiteProperty('yarn.timeline-service.entity-group-fs-store.group-id-plugin-classpath', ":".join(timeline_plugin_classpath_values))


  def recommendYARNConfigurationsFromHDP26(self, configurations, clusterData, services, hosts):
    putYarnSiteProperty = self.putProperty(configurations, "yarn-site", services)
    putYarnEnvProperty = self.putProperty(configurations, "yarn-env", services)

    if "yarn-site" in services["configurations"] and \
                    "yarn.resourcemanager.scheduler.monitor.enable" in services["configurations"]["yarn-site"]["properties"]:
      scheduler_monitor_enabled = services["configurations"]["yarn-site"]["properties"]["yarn.resourcemanager.scheduler.monitor.enable"]
      if scheduler_monitor_enabled.lower() == 'true':
        putYarnSiteProperty('yarn.scheduler.capacity.ordering-policy.priority-utilization.underutilized-preemption.enabled', "true")
      else:
        putYarnSiteProperty('yarn.scheduler.capacity.ordering-policy.priority-utilization.underutilized-preemption.enabled', "false")

    # calculate total_preemption_per_round
    total_preemption_per_round = str(round(max(float(1)/len(hosts['items']), 0.1),2))
    putYarnSiteProperty('yarn.resourcemanager.monitor.capacity.preemption.total_preemption_per_round', total_preemption_per_round)

    if 'yarn-env' in services['configurations'] and 'yarn_user' in services['configurations']['yarn-env']['properties']:
      yarn_user = services['configurations']['yarn-env']['properties']['yarn_user']
    else:
      yarn_user = 'yarn'
    if 'ranger-yarn-plugin-properties' in configurations and 'ranger-yarn-plugin-enabled' in configurations['ranger-yarn-plugin-properties']['properties']:
      ranger_yarn_plugin_enabled = (configurations['ranger-yarn-plugin-properties']['properties']['ranger-yarn-plugin-enabled'].lower() == 'Yes'.lower())
    elif 'ranger-yarn-plugin-properties' in services['configurations'] and 'ranger-yarn-plugin-enabled' in services['configurations']['ranger-yarn-plugin-properties']['properties']:
      ranger_yarn_plugin_enabled = (services['configurations']['ranger-yarn-plugin-properties']['properties']['ranger-yarn-plugin-enabled'].lower() == 'Yes'.lower())
    else:
      ranger_yarn_plugin_enabled = False

     #yarn timeline service url depends on http policy and takes the host name of the yarn webapp.
    if "yarn-site" in services["configurations"] and \
         "yarn.http.policy" in services["configurations"]["yarn-site"]["properties"] and \
          "yarn.log.server.web-service.url" in services["configurations"]["yarn-site"]["properties"]:
      webservice_url = ''
      if services["configurations"]["yarn-site"]["properties"]["yarn.http.policy"] == 'HTTP_ONLY':
         if "yarn.timeline-service.webapp.address" in services["configurations"]["yarn-site"]["properties"]:
           webapp_address = services["configurations"]["yarn-site"]["properties"]["yarn.timeline-service.webapp.address"]
           webservice_url = "http://"+webapp_address+"/ws/v1/applicationhistory"
         else:
           self.logger.error("Required config yarn.timeline-service.webapp.address in yarn-site does not exist. Unable to set yarn.log.server.web-service.url")
      else:
         if "yarn.timeline-service.webapp.https.address" in services["configurations"]["yarn-site"]["properties"]:
           webapp_address = services["configurations"]["yarn-site"]["properties"]["yarn.timeline-service.webapp.https.address"]
           webservice_url = "https://"+webapp_address+"/ws/v1/applicationhistory"
         else:
           self.logger.error("Required config yarn.timeline-service.webapp.https.address in yarn-site does not exist. Unable to set yarn.log.server.web-service.url")
      putYarnSiteProperty('yarn.log.server.web-service.url',webservice_url )

    if ranger_yarn_plugin_enabled and 'ranger-yarn-plugin-properties' in services['configurations'] and 'REPOSITORY_CONFIG_USERNAME' in services['configurations']['ranger-yarn-plugin-properties']['properties']:
      self.logger.info("Setting Yarn Repo user for Ranger.")
      putRangerYarnPluginProperty = self.putProperty(configurations, "ranger-yarn-plugin-properties", services)
      putRangerYarnPluginProperty("REPOSITORY_CONFIG_USERNAME",yarn_user)
    else:
      self.logger.info("Not setting Yarn Repo user for Ranger.")

    yarn_timeline_app_cache_size = None
    host_mem = None
    for host in hosts["items"]:
      host_mem = host["Hosts"]["total_mem"]
      break
    # Check if 'yarn.timeline-service.entity-group-fs-store.app-cache-size' in changed configs.
    changed_configs_has_ats_cache_size = self.isConfigPropertiesChanged(
      services, "yarn-site", ['yarn.timeline-service.entity-group-fs-store.app-cache-size'], False)
    # Check if it's : 1. 'apptimelineserver_heapsize' changed detected in changed-configurations)
    # OR 2. cluster initialization (services['changed-configurations'] should be empty in this case)
    if changed_configs_has_ats_cache_size:
      yarn_timeline_app_cache_size = self.read_yarn_apptimelineserver_cache_size(services)
    elif 0 == len(services['changed-configurations']):
      # Fetch host memory from 1st host, to be used for ATS config calculations below.
      if host_mem is not None:
        yarn_timeline_app_cache_size = self.calculate_yarn_apptimelineserver_cache_size(host_mem)
        putYarnSiteProperty('yarn.timeline-service.entity-group-fs-store.app-cache-size', yarn_timeline_app_cache_size)
        self.logger.info("Updated YARN config 'yarn.timeline-service.entity-group-fs-store.app-cache-size' as : {0}, "
                         "using 'host_mem' = {1}".format(yarn_timeline_app_cache_size, host_mem))
      else:
        self.logger.info("Couldn't update YARN config 'yarn.timeline-service.entity-group-fs-store.app-cache-size' as "
                         "'host_mem' read = {0}".format(host_mem))

    if yarn_timeline_app_cache_size is not None:
      # Calculation for 'ats_heapsize' is in MB.
      ats_heapsize = self.calculate_yarn_apptimelineserver_heapsize(host_mem, yarn_timeline_app_cache_size)
      putYarnEnvProperty('apptimelineserver_heapsize', ats_heapsize) # Value in MB
      self.logger.info("Updated YARN config 'apptimelineserver_heapsize' as : {0}, ".format(ats_heapsize))

  """
  Calculate YARN config 'apptimelineserver_heapsize' in MB.
  """
  def calculate_yarn_apptimelineserver_heapsize(self, host_mem, yarn_timeline_app_cache_size):
    ats_heapsize = None
    if host_mem < 4096:
      ats_heapsize = 1024
    else:
      ats_heapsize = long(min(math.floor(host_mem/2), long(yarn_timeline_app_cache_size) * 500 + 3072))
    return ats_heapsize

  """
  Calculates for YARN config 'yarn.timeline-service.entity-group-fs-store.app-cache-size', based on YARN's NodeManager size.
  """
  def calculate_yarn_apptimelineserver_cache_size(self, host_mem):
    yarn_timeline_app_cache_size = None
    if host_mem < 4096:
      yarn_timeline_app_cache_size = 3
    elif host_mem >= 4096 and host_mem < 8192:
      yarn_timeline_app_cache_size = 7
    elif host_mem >= 8192:
      yarn_timeline_app_cache_size = 10
    self.logger.info("Calculated and returning 'yarn_timeline_app_cache_size' : {0}".format(yarn_timeline_app_cache_size))
    return yarn_timeline_app_cache_size


  """
  Reads YARN config 'yarn.timeline-service.entity-group-fs-store.app-cache-size'.
  """
  def read_yarn_apptimelineserver_cache_size(self, services):
    """
    :type services dict
    :rtype str
    """
    yarn_ats_app_cache_size = None
    yarn_ats_app_cache_size_config = "yarn.timeline-service.entity-group-fs-store.app-cache-size"
    yarn_site_in_services = self.getServicesSiteProperties(services, "yarn-site")

    if yarn_site_in_services and yarn_ats_app_cache_size_config in yarn_site_in_services:
      yarn_ats_app_cache_size = yarn_site_in_services[yarn_ats_app_cache_size_config]
      self.logger.info("'yarn.scheduler.minimum-allocation-mb' read from services as : {0}".format(yarn_ats_app_cache_size))

    if not yarn_ats_app_cache_size:
      self.logger.error("'{0}' was not found in the services".format(yarn_ats_app_cache_size_config))

    return yarn_ats_app_cache_size

  #region LLAP
  def updateLlapConfigs(self, configurations, services, hosts, llap_queue_name):
    """
    Entry point for updating Hive's 'LLAP app' configs namely :
      (1). num_llap_nodes (2). hive.llap.daemon.yarn.container.mb
      (3). hive.llap.daemon.num.executors (4). hive.llap.io.memory.size (5). llap_heap_size (6). slider_am_container_mb,
      (7). hive.server2.tez.sessions.per.default.queue, (8). tez.am.resource.memory.mb (9). hive.tez.container.size
      (10). tez.runtime.io.sort.mb  (11). tez.runtime.unordered.output.buffer.size-mb (12). hive.llap.io.threadpool.size, and
      (13). hive.llap.io.enabled.

      The trigger point for updating LLAP configs (mentioned above) is change in values of any of the following:
      (1). 'enable_hive_interactive' set to 'true' (2). 'num_llap_nodes' (3). 'hive.server2.tez.sessions.per.default.queue'
      (4). Change in queue selection for config 'hive.llap.daemon.queue.name'.

      If change in value for 'num_llap_nodes' or 'hive.server2.tez.sessions.per.default.queue' is detected, that config
      value is not calulated, but read and use in calculation for dependent configs.

      Note: All memory calculations are in MB, unless specified otherwise.
    """
    self.logger.info("DBG: Entered updateLlapConfigs")

    # Determine if we entered here during cluster creation.
    operation = getUserOperationContext(services, "operation")
    is_cluster_create_opr = False
    if operation == self.CLUSTER_CREATE_OPERATION:
      is_cluster_create_opr = True
    self.logger.info("Is cluster create operation ? = {0}".format(is_cluster_create_opr))

    putHiveInteractiveSiteProperty = self.putProperty(configurations, YARNRecommender.HIVE_INTERACTIVE_SITE, services)
    putHiveInteractiveSitePropertyAttribute = self.putPropertyAttribute(configurations, YARNRecommender.HIVE_INTERACTIVE_SITE)
    putHiveInteractiveEnvProperty = self.putProperty(configurations, "hive-interactive-env", services)
    putHiveInteractiveEnvPropertyAttribute = self.putPropertyAttribute(configurations, "hive-interactive-env")
    putTezInteractiveSiteProperty = self.putProperty(configurations, "tez-interactive-site", services)
    putTezInteractiveSitePropertyAttribute = self.putPropertyAttribute(configurations, "tez-interactive-site")
    llap_daemon_selected_queue_name = None
    selected_queue_is_ambari_managed_llap = None  # Queue named 'llap' at root level is Ambari managed.
    llap_selected_queue_am_percent = None
    DEFAULT_EXECUTOR_TO_AM_RATIO = 20
    MIN_EXECUTOR_TO_AM_RATIO = 10
    MAX_CONCURRENT_QUERIES = 32
    MAX_CONCURRENT_QUERIES_SMALL_CLUSTERS = 4 # Concurrency for clusters with <10 executors
    leafQueueNames = None
    MB_TO_BYTES = 1048576
    hsi_site = self.getServicesSiteProperties(services, YARNRecommender.HIVE_INTERACTIVE_SITE)
    yarn_site = self.getServicesSiteProperties(services, "yarn-site")
    min_memory_required = 0

    # Update 'hive.llap.daemon.queue.name' prop combo entries
    self.setLlapDaemonQueuePropAttributes(services, configurations)

    if not services["changed-configurations"]:
      read_llap_daemon_yarn_cont_mb = long(self.get_yarn_min_container_size(services, configurations))
      putHiveInteractiveSiteProperty("hive.llap.daemon.yarn.container.mb", read_llap_daemon_yarn_cont_mb)

    if hsi_site and "hive.llap.daemon.queue.name" in hsi_site:
      llap_daemon_selected_queue_name = hsi_site["hive.llap.daemon.queue.name"]

    # Update Visibility of 'num_llap_nodes' slider. Visible only if selected queue is Ambari created 'llap'.
    capacity_scheduler_properties, received_as_key_value_pair = self.getCapacitySchedulerProperties(services)
    if capacity_scheduler_properties:
      # Get all leaf queues.
      leafQueueNames = self.getAllYarnLeafQueues(capacity_scheduler_properties)
      self.logger.info("YARN leaf Queues = {0}".format(leafQueueNames))
      if len(leafQueueNames) == 0:
        self.logger.error("Queue(s) couldn't be retrieved from capacity-scheduler.")
        return

      # Check if it's 1st invocation after enabling Hive Server Interactive (config: enable_hive_interactive).
      changed_configs_has_enable_hive_int = self.isConfigPropertiesChanged(services, "hive-interactive-env", ['enable_hive_interactive'], False)
      llap_named_queue_selected_in_curr_invocation = None
      # Check if its : 1. 1st invocation from UI ('enable_hive_interactive' in changed-configurations)
      # OR 2. 1st invocation from BP (services['changed-configurations'] should be empty in this case)
      if (changed_configs_has_enable_hive_int or  0 == len(services['changed-configurations'])) \
        and services['configurations']['hive-interactive-env']['properties']['enable_hive_interactive']:
        if len(leafQueueNames) == 1 or (len(leafQueueNames) == 2 and llap_queue_name in leafQueueNames):
          llap_named_queue_selected_in_curr_invocation = True
          putHiveInteractiveSiteProperty('hive.llap.daemon.queue.name', llap_queue_name)
          putHiveInteractiveSiteProperty('hive.server2.tez.default.queues', llap_queue_name)
        else:
          first_leaf_queue = list(leafQueueNames)[0]  # 1st invocation, pick the 1st leaf queue and set it as selected.
          putHiveInteractiveSiteProperty('hive.llap.daemon.queue.name', first_leaf_queue)
          putHiveInteractiveSiteProperty('hive.server2.tez.default.queues', first_leaf_queue)
          llap_named_queue_selected_in_curr_invocation = False
      self.logger.info("DBG: llap_named_queue_selected_in_curr_invocation = {0}".format(llap_named_queue_selected_in_curr_invocation))

      if (len(leafQueueNames) == 2 and (llap_daemon_selected_queue_name and llap_daemon_selected_queue_name == llap_queue_name) or
            llap_named_queue_selected_in_curr_invocation) or \
        (len(leafQueueNames) == 1 and llap_daemon_selected_queue_name == 'default' and llap_named_queue_selected_in_curr_invocation):
        self.logger.info("DBG: Setting 'num_llap_nodes' config's  READ ONLY attribute as 'False'.")
        putHiveInteractiveEnvPropertyAttribute("num_llap_nodes", "read_only", "false")
        selected_queue_is_ambari_managed_llap = True
        self.logger.info("DBG: Selected YARN queue for LLAP is : '{0}'. Current YARN queues : {1}. Setting 'Number of LLAP nodes' "
                    "slider visibility to 'True'".format(llap_queue_name, list(leafQueueNames)))
      else:
        self.logger.info("DBG: Setting 'num_llap_nodes' config's  READ ONLY attribute as 'True'.")
        putHiveInteractiveEnvPropertyAttribute("num_llap_nodes", "read_only", "true")
        self.logger.info("Selected YARN queue for LLAP is : '{0}'. Current YARN queues : {1}. Setting 'Number of LLAP nodes' "
                    "visibility to 'False'.".format(llap_daemon_selected_queue_name, list(leafQueueNames)))
        selected_queue_is_ambari_managed_llap = False

      if not llap_named_queue_selected_in_curr_invocation:  # We would be creating the 'llap' queue later. Thus, cap-sched doesn't have
        # state information pertaining to 'llap' queue.
        # Check: State of the selected queue should not be STOPPED.
        if llap_daemon_selected_queue_name:
          llap_selected_queue_state = self.__getQueueStateFromCapacityScheduler(capacity_scheduler_properties, llap_daemon_selected_queue_name)
          if llap_selected_queue_state is None or llap_selected_queue_state == "STOPPED":
            self.logger.error("Selected LLAP app queue '{0}' current state is : '{1}'. Setting LLAP configs to default "
                         "values.".format(llap_daemon_selected_queue_name, llap_selected_queue_state))
            self.recommendDefaultLlapConfiguration(configurations, services, hosts)
            return
        else:
          self.logger.error("Retrieved LLAP app queue name is : '{0}'. Setting LLAP configs to default values."
                       .format(llap_daemon_selected_queue_name))
          self.recommendDefaultLlapConfiguration(configurations, services, hosts)
          return
    else:
      self.logger.error("Couldn't retrieve 'capacity-scheduler' properties while doing YARN queue adjustment for Hive Server Interactive."
                   " Not calculating LLAP configs.")
      return

    changed_configs_in_hive_int_env = None
    llap_concurrency_in_changed_configs = None
    llap_daemon_queue_in_changed_configs = None
    # Calculations are triggered only if there is change in any one of the following props :
    # 'num_llap_nodes', 'enable_hive_interactive', 'hive.server2.tez.sessions.per.default.queue'
    # or 'hive.llap.daemon.queue.name' has change in value selection.
    # OR
    # services['changed-configurations'] is empty implying that this is the Blueprint call. (1st invocation)
    if 'changed-configurations' in services.keys():
      config_names_to_be_checked = set(['num_llap_nodes', 'enable_hive_interactive'])
      changed_configs_in_hive_int_env = self.isConfigPropertiesChanged(services, "hive-interactive-env", config_names_to_be_checked, False)

      # Determine if there is change detected in "hive-interactive-site's" configs based on which we calculate llap configs.
      llap_concurrency_in_changed_configs = self.isConfigPropertiesChanged(services, YARNRecommender.HIVE_INTERACTIVE_SITE, ['hive.server2.tez.sessions.per.default.queue'], False)
      llap_daemon_queue_in_changed_configs = self.isConfigPropertiesChanged(services, YARNRecommender.HIVE_INTERACTIVE_SITE, ['hive.llap.daemon.queue.name'], False)

    if not changed_configs_in_hive_int_env and not llap_concurrency_in_changed_configs and \
        not llap_daemon_queue_in_changed_configs and services["changed-configurations"]:
      self.logger.info("DBG: LLAP parameters not modified. Not adjusting LLAP configs.")
      self.logger.info("DBG: Current 'changed-configuration' received is : {0}".format(services["changed-configurations"]))
      return

    self.logger.info("\nDBG: Performing LLAP config calculations ......")
    node_manager_host_list = self.getHostsForComponent(services, "YARN", "NODEMANAGER")
    node_manager_cnt = len(node_manager_host_list)
    yarn_nm_mem_in_mb = self.get_yarn_nm_mem_in_mb(services, configurations)
    total_cluster_capacity = node_manager_cnt * yarn_nm_mem_in_mb
    self.logger.info("DBG: Calculated total_cluster_capacity : {0}, using following : node_manager_cnt : {1}, "
                "yarn_nm_mem_in_mb : {2}".format(total_cluster_capacity, node_manager_cnt, yarn_nm_mem_in_mb))
    yarn_min_container_size = float(self.get_yarn_min_container_size(services, configurations))
    tez_am_container_size = self.calculate_tez_am_container_size(services, long(total_cluster_capacity), is_cluster_create_opr,
                                                                 changed_configs_has_enable_hive_int)
    normalized_tez_am_container_size = self._normalizeUp(tez_am_container_size, yarn_min_container_size)

    if yarn_site and "yarn.nodemanager.resource.cpu-vcores" in yarn_site:
      cpu_per_nm_host = float(yarn_site["yarn.nodemanager.resource.cpu-vcores"])
    else:
      self.recommendDefaultLlapConfiguration(configurations, services, hosts)
      return
    self.logger.info("DBG Calculated normalized_tez_am_container_size : {0}, using following : tez_am_container_size : {1}, "
                "total_cluster_capacity : {2}".format(normalized_tez_am_container_size, tez_am_container_size,
                                                      total_cluster_capacity))

    # Calculate the available memory for LLAP app
    yarn_nm_mem_in_mb_normalized = self._normalizeDown(yarn_nm_mem_in_mb, yarn_min_container_size)
    mem_per_thread_for_llap = float(self.calculate_mem_per_thread_for_llap(services, yarn_nm_mem_in_mb_normalized, cpu_per_nm_host,
                                                                           is_cluster_create_opr, changed_configs_has_enable_hive_int))
    self.logger.info("DBG: Calculated mem_per_thread_for_llap : {0}, using following: yarn_nm_mem_in_mb_normalized : {1}, "
                "cpu_per_nm_host : {2}".format(mem_per_thread_for_llap, yarn_nm_mem_in_mb_normalized, cpu_per_nm_host))


    if mem_per_thread_for_llap is None:
      self.recommendDefaultLlapConfiguration(configurations, services, hosts)
      return

    # Get calculated value for Slider AM container Size
    slider_am_container_size = self._normalizeUp(self.calculate_slider_am_size(yarn_min_container_size),
                                                 yarn_min_container_size)
    self.logger.info("DBG: Calculated 'slider_am_container_size' : {0}, using following: yarn_min_container_size : "
                "{1}".format(slider_am_container_size, yarn_min_container_size))

    min_memory_required = normalized_tez_am_container_size + slider_am_container_size + self._normalizeUp(mem_per_thread_for_llap, yarn_min_container_size)
    self.logger.info("DBG: Calculated 'min_memory_required': {0} using following : slider_am_container_size: {1}, "
                "normalized_tez_am_container_size : {2}, mem_per_thread_for_llap : {3}, yarn_min_container_size : "
                "{4}".format(min_memory_required, slider_am_container_size, normalized_tez_am_container_size, mem_per_thread_for_llap, yarn_min_container_size))

    min_nodes_required = int(ceil( min_memory_required / yarn_nm_mem_in_mb_normalized))
    self.logger.info("DBG: Calculated 'min_node_required': {0}, using following : min_memory_required : {1}, yarn_nm_mem_in_mb_normalized "
                ": {2}".format(min_nodes_required, min_memory_required, yarn_nm_mem_in_mb_normalized))
    if min_nodes_required > node_manager_cnt:
      self.logger.warning("ERROR: Not enough memory/nodes to run LLAP");
      self.recommendDefaultLlapConfiguration(configurations, services, hosts)
      return

    mem_per_thread_for_llap = float(mem_per_thread_for_llap)

    self.logger.info("DBG: selected_queue_is_ambari_managed_llap = {0}".format(selected_queue_is_ambari_managed_llap))
    if not selected_queue_is_ambari_managed_llap:
      llap_daemon_selected_queue_cap = self.__getSelectedQueueTotalCap(capacity_scheduler_properties, llap_daemon_selected_queue_name, total_cluster_capacity)

      if llap_daemon_selected_queue_cap <= 0:
        self.logger.warning("'{0}' queue capacity percentage retrieved = {1}. Expected > 0.".format(
          llap_daemon_selected_queue_name, llap_daemon_selected_queue_cap))
        self.recommendDefaultLlapConfiguration(configurations, services, hosts)
        return

      total_llap_mem_normalized = self._normalizeDown(llap_daemon_selected_queue_cap, yarn_min_container_size)
      self.logger.info("DBG: Calculated '{0}' queue available capacity : {1}, using following: llap_daemon_selected_queue_cap : {2}, "
                  "yarn_min_container_size : {3}".format(llap_daemon_selected_queue_name, total_llap_mem_normalized,
                                                         llap_daemon_selected_queue_cap, yarn_min_container_size))
      '''Rounding up numNodes so that we run more daemons, and utilitze more CPUs. The rest of the calcaulations will take care of cutting this down if required'''
      num_llap_nodes_requested = ceil(total_llap_mem_normalized / yarn_nm_mem_in_mb_normalized)
      self.logger.info("DBG: Calculated 'num_llap_nodes_requested' : {0}, using following: total_llap_mem_normalized : {1}, "
                  "yarn_nm_mem_in_mb_normalized : {2}".format(num_llap_nodes_requested, total_llap_mem_normalized, yarn_nm_mem_in_mb_normalized))
      # Pouplate the 'num_llap_nodes_requested' in config 'num_llap_nodes', a read only config for non-Ambari managed queue case.
      putHiveInteractiveEnvProperty('num_llap_nodes', num_llap_nodes_requested)
      self.logger.info("Setting config 'num_llap_nodes' as : {0}".format(num_llap_nodes_requested))
      queue_am_fraction_perc = float(self.__getQueueAmFractionFromCapacityScheduler(capacity_scheduler_properties, llap_daemon_selected_queue_name))
      hive_tez_am_cap_available = queue_am_fraction_perc * total_llap_mem_normalized
      self.logger.info("DBG: Calculated 'hive_tez_am_cap_available' : {0}, using following: queue_am_fraction_perc : {1}, "
                  "total_llap_mem_normalized : {2}".format(hive_tez_am_cap_available, queue_am_fraction_perc, total_llap_mem_normalized))
    else:  # Ambari managed 'llap' named queue at root level.
      # Set 'num_llap_nodes_requested' for 1st invocation, as it gets passed as 1 otherwise, read from config.

      # Check if its : 1. 1st invocation from UI ('enable_hive_interactive' in changed-configurations)
      # OR 2. 1st invocation from BP (services['changed-configurations'] should be empty in this case)
      if (changed_configs_has_enable_hive_int or  0 == len(services['changed-configurations'])) \
        and services['configurations']['hive-interactive-env']['properties']['enable_hive_interactive']:
        num_llap_nodes_requested = min_nodes_required
      else:
        num_llap_nodes_requested = self.get_num_llap_nodes(services, configurations) #Input
      total_llap_mem = num_llap_nodes_requested * yarn_nm_mem_in_mb_normalized
      self.logger.info("DBG: Calculated 'total_llap_mem' : {0}, using following: num_llap_nodes_requested : {1}, "
                  "yarn_nm_mem_in_mb_normalized : {2}".format(total_llap_mem, num_llap_nodes_requested, yarn_nm_mem_in_mb_normalized))
      total_llap_mem_normalized = float(self._normalizeDown(total_llap_mem, yarn_min_container_size))
      self.logger.info("DBG: Calculated 'total_llap_mem_normalized' : {0}, using following: total_llap_mem : {1}, "
                  "yarn_min_container_size : {2}".format(total_llap_mem_normalized, total_llap_mem, yarn_min_container_size))

      # What percent is 'total_llap_mem' of 'total_cluster_capacity' ?
      llap_named_queue_cap_fraction = ceil(total_llap_mem_normalized / total_cluster_capacity * 100)
      self.logger.info("DBG: Calculated '{0}' queue capacity percent = {1}.".format(llap_queue_name, llap_named_queue_cap_fraction))

      if llap_named_queue_cap_fraction > 100:
        self.logger.warning("Calculated '{0}' queue size = {1}. Cannot be > 100.".format(llap_queue_name, llap_named_queue_cap_fraction))
        self.recommendDefaultLlapConfiguration(configurations, services, hosts)
        return

      # Adjust capacity scheduler for the 'llap' named queue.
      self.checkAndManageLlapQueue(services, configurations, hosts, llap_queue_name, llap_named_queue_cap_fraction)
      hive_tez_am_cap_available = total_llap_mem_normalized
      self.logger.info("DBG: hive_tez_am_cap_available : {0}".format(hive_tez_am_cap_available))

    # Common calculations now, irrespective of the queue selected.

    llap_mem_for_tezAm_and_daemons = total_llap_mem_normalized - slider_am_container_size
    self.logger.info("DBG: Calculated 'llap_mem_for_tezAm_and_daemons' : {0}, using following : total_llap_mem_normalized : {1}, "
                "slider_am_container_size : {2}".format(llap_mem_for_tezAm_and_daemons, total_llap_mem_normalized, slider_am_container_size))

    if llap_mem_for_tezAm_and_daemons < 2 * yarn_min_container_size:
      self.logger.warning("Not enough capacity available on the cluster to run LLAP")
      self.recommendDefaultLlapConfiguration(configurations, services, hosts)
      return

    # Calculate llap concurrency (i.e. Number of Tez AM's)
    max_executors_per_node = self.get_max_executors_per_node(yarn_nm_mem_in_mb_normalized, cpu_per_nm_host, mem_per_thread_for_llap)

    # Read 'hive.server2.tez.sessions.per.default.queue' prop if it's in changed-configs, else calculate it.
    if not llap_concurrency_in_changed_configs:
      if max_executors_per_node <= 0:
        self.logger.warning("Calculated 'max_executors_per_node' = {0}. Expected value >= 1.".format(max_executors_per_node))
        self.recommendDefaultLlapConfiguration(configurations, services, hosts)
        return

      self.logger.info("DBG: Calculated 'max_executors_per_node' : {0}, using following: yarn_nm_mem_in_mb_normalized : {1}, cpu_per_nm_host : {2}, "
                  "mem_per_thread_for_llap: {3}".format(max_executors_per_node, yarn_nm_mem_in_mb_normalized, cpu_per_nm_host, mem_per_thread_for_llap))

      # Default 1 AM for every 20 executor threads.
      # The second part of the min calculates based on mem required for DEFAULT_EXECUTOR_TO_AM_RATIO executors + 1 AM,
      # making use of total memory. However, it's possible that total memory will not be used - and the numExecutors is
      # instead limited by #CPUs. Use maxPerNode to factor this in.
      llap_concurreny_limit = min(floor(max_executors_per_node * num_llap_nodes_requested / DEFAULT_EXECUTOR_TO_AM_RATIO), MAX_CONCURRENT_QUERIES)
      self.logger.info("DBG: Calculated 'llap_concurreny_limit' : {0}, using following : max_executors_per_node : {1}, num_llap_nodes_requested : {2}, DEFAULT_EXECUTOR_TO_AM_RATIO "
                  ": {3}, MAX_CONCURRENT_QUERIES : {4}".format(llap_concurreny_limit, max_executors_per_node, num_llap_nodes_requested, DEFAULT_EXECUTOR_TO_AM_RATIO, MAX_CONCURRENT_QUERIES))
      llap_concurrency = min(llap_concurreny_limit, floor(llap_mem_for_tezAm_and_daemons / (DEFAULT_EXECUTOR_TO_AM_RATIO * mem_per_thread_for_llap + normalized_tez_am_container_size)))
      self.logger.info("DBG: Calculated 'llap_concurrency' : {0}, using following : llap_concurreny_limit : {1}, llap_mem_for_tezAm_and_daemons : "
                  "{2}, DEFAULT_EXECUTOR_TO_AM_RATIO : {3}, mem_per_thread_for_llap : {4}, normalized_tez_am_container_size : "
                  "{5}".format(llap_concurrency, llap_concurreny_limit, llap_mem_for_tezAm_and_daemons, DEFAULT_EXECUTOR_TO_AM_RATIO,
                               mem_per_thread_for_llap, normalized_tez_am_container_size))
      if llap_concurrency == 0:
        llap_concurrency = 1
        self.logger.info("DBG: Readjusted 'llap_concurrency' to : 1. Earlier calculated value : 0")

      if llap_concurrency * normalized_tez_am_container_size > hive_tez_am_cap_available:
        llap_concurrency = long(math.floor(hive_tez_am_cap_available / normalized_tez_am_container_size))
        self.logger.info("DBG: Readjusted 'llap_concurrency' to : {0}, as llap_concurrency({1}) * normalized_tez_am_container_size({2}) > hive_tez_am_cap_available({3}))"
                    .format(llap_concurrency, llap_concurrency, normalized_tez_am_container_size, hive_tez_am_cap_available))

        if llap_concurrency <= 0:
          self.logger.warning("DBG: Calculated 'LLAP Concurrent Queries' = {0}. Expected value >= 1.".format(llap_concurrency))
          self.recommendDefaultLlapConfiguration(configurations, services, hosts)
          return
        self.logger.info("DBG: Adjusted 'llap_concurrency' : {0}, using following: hive_tez_am_cap_available : {1}, normalized_tez_am_container_size: "
                    "{2}".format(llap_concurrency, hive_tez_am_cap_available, normalized_tez_am_container_size))
    else:
      # Read current value
      if 'hive.server2.tez.sessions.per.default.queue' in hsi_site:
        llap_concurrency = long(hsi_site['hive.server2.tez.sessions.per.default.queue'])
        if llap_concurrency <= 0:
          self.logger.warning("'hive.server2.tez.sessions.per.default.queue' current value : {0}. Expected value : >= 1".format(llap_concurrency))
          self.recommendDefaultLlapConfiguration(configurations, services, hosts)
          return
        self.logger.info("DBG: Read 'llap_concurrency' : {0}".format(llap_concurrency ))
      else:
        llap_concurrency = 1
        self.logger.warning("Couldn't retrieve Hive Server interactive's 'hive.server2.tez.sessions.per.default.queue' config. Setting default value 1.")
        self.recommendDefaultLlapConfiguration(configurations, services, hosts)
        return

    # Calculate 'Max LLAP Consurrency', irrespective of whether 'llap_concurrency' was read or calculated.
    max_llap_concurreny_limit = min(floor(max_executors_per_node * num_llap_nodes_requested / MIN_EXECUTOR_TO_AM_RATIO), MAX_CONCURRENT_QUERIES)
    self.logger.info("DBG: Calculated 'max_llap_concurreny_limit' : {0}, using following : max_executors_per_node : {1}, num_llap_nodes_requested "
                ": {2}, MIN_EXECUTOR_TO_AM_RATIO : {3}, MAX_CONCURRENT_QUERIES : {4}".format(max_llap_concurreny_limit, max_executors_per_node,
                                                                                             num_llap_nodes_requested, MIN_EXECUTOR_TO_AM_RATIO,
                                                                                             MAX_CONCURRENT_QUERIES))
    max_llap_concurreny = long(min(max_llap_concurreny_limit, floor(llap_mem_for_tezAm_and_daemons / (MIN_EXECUTOR_TO_AM_RATIO *
                                                                                                      mem_per_thread_for_llap + normalized_tez_am_container_size))))
    self.logger.info("DBG: Calculated 'max_llap_concurreny' : {0}, using following : max_llap_concurreny_limit : {1}, llap_mem_for_tezAm_and_daemons : "
                "{2}, MIN_EXECUTOR_TO_AM_RATIO : {3}, mem_per_thread_for_llap : {4}, normalized_tez_am_container_size : "
                "{5}".format(max_llap_concurreny, max_llap_concurreny_limit, llap_mem_for_tezAm_and_daemons, MIN_EXECUTOR_TO_AM_RATIO,
                             mem_per_thread_for_llap, normalized_tez_am_container_size))
    if int(max_llap_concurreny) < MAX_CONCURRENT_QUERIES_SMALL_CLUSTERS:
      self.logger.info("DBG: Adjusting 'max_llap_concurreny' from {0} to {1}".format(max_llap_concurreny, MAX_CONCURRENT_QUERIES_SMALL_CLUSTERS))
      max_llap_concurreny = MAX_CONCURRENT_QUERIES_SMALL_CLUSTERS

    if (max_llap_concurreny * normalized_tez_am_container_size) > hive_tez_am_cap_available:
      max_llap_concurreny = floor(hive_tez_am_cap_available / normalized_tez_am_container_size)
      if max_llap_concurreny <= 0:
        self.logger.warning("Calculated 'Max. LLAP Concurrent Queries' = {0}. Expected value > 1".format(max_llap_concurreny))
        self.recommendDefaultLlapConfiguration(configurations, services, hosts)
        return
      self.logger.info("DBG: Adjusted 'max_llap_concurreny' : {0}, using following: hive_tez_am_cap_available : {1}, normalized_tez_am_container_size: "
                  "{2}".format(max_llap_concurreny, hive_tez_am_cap_available, normalized_tez_am_container_size))

    # Calculate value for 'num_llap_nodes', an across cluster config.
    tez_am_memory_required = llap_concurrency * normalized_tez_am_container_size
    self.logger.info("DBG: Calculated 'tez_am_memory_required' : {0}, using following : llap_concurrency : {1}, normalized_tez_am_container_size : "
                "{2}".format(tez_am_memory_required, llap_concurrency, normalized_tez_am_container_size))
    llap_mem_daemon_size = llap_mem_for_tezAm_and_daemons - tez_am_memory_required

    if llap_mem_daemon_size < yarn_min_container_size:
      self.logger.warning("Calculated 'LLAP Daemon Size = {0}'. Expected >= 'YARN Minimum Container Size' ({1})'".format(
        llap_mem_daemon_size, yarn_min_container_size))
      self.recommendDefaultLlapConfiguration(configurations, services, hosts)
      return

    if llap_mem_daemon_size < mem_per_thread_for_llap or llap_mem_daemon_size < yarn_min_container_size:
      self.logger.warning("Not enough memory available for executors.")
      self.recommendDefaultLlapConfiguration(configurations, services, hosts)
      return
    self.logger.info("DBG: Calculated 'llap_mem_daemon_size' : {0}, using following : llap_mem_for_tezAm_and_daemons : {1}, tez_am_memory_required : "
                "{2}".format(llap_mem_daemon_size, llap_mem_for_tezAm_and_daemons, tez_am_memory_required))

    llap_daemon_mem_per_node = self._normalizeDown(llap_mem_daemon_size / num_llap_nodes_requested, yarn_min_container_size)
    # This value takes into account total cluster capacity, and may not have left enough capcaity on each node to launch an AM.
    self.logger.info("DBG: Calculated 'llap_daemon_mem_per_node' : {0}, using following : llap_mem_daemon_size : {1}, num_llap_nodes_requested : {2}, "
                "yarn_min_container_size: {3}".format(llap_daemon_mem_per_node, llap_mem_daemon_size, num_llap_nodes_requested, yarn_min_container_size))
    if llap_daemon_mem_per_node == 0:
      # Small cluster. No capacity left on a node after running AMs.
      llap_daemon_mem_per_node = self._normalizeUp(mem_per_thread_for_llap, yarn_min_container_size)
      num_llap_nodes = floor(llap_mem_daemon_size / llap_daemon_mem_per_node)
      self.logger.info("DBG: 'llap_daemon_mem_per_node' : 0, adjusted 'llap_daemon_mem_per_node' : {0}, 'num_llap_nodes' : {1}, using following: llap_mem_daemon_size : {2}, "
                  "mem_per_thread_for_llap : {3}".format(llap_daemon_mem_per_node, num_llap_nodes, llap_mem_daemon_size, mem_per_thread_for_llap))
    elif llap_daemon_mem_per_node < mem_per_thread_for_llap:
      # Previously computed value of memory per thread may be too high. Cut the number of nodes. (Alternately reduce memory per node)
      llap_daemon_mem_per_node = mem_per_thread_for_llap
      num_llap_nodes = floor(llap_mem_daemon_size / mem_per_thread_for_llap)
      self.logger.info("DBG: 'llap_daemon_mem_per_node'({0}) < mem_per_thread_for_llap({1}), adjusted 'llap_daemon_mem_per_node' "
                  ": {2}".format(llap_daemon_mem_per_node, mem_per_thread_for_llap, llap_daemon_mem_per_node))
    else:
      # All good. We have a proper value for memoryPerNode.
      num_llap_nodes = num_llap_nodes_requested
      self.logger.info("DBG: num_llap_nodes : {0}".format(num_llap_nodes))

    # Make sure we have enough memory on each node to run AMs.
    # If nodes vs nodes_requested is different - AM memory is already factored in.
    # If llap_node_count < total_cluster_nodes - assuming AMs can run on a different node.
    # Else factor in min_concurrency_per_node * tez_am_size, and slider_am_size
    # Also needs to factor in whether num_llap_nodes = cluster_node_count
    min_mem_reserved_per_node = 0
    if num_llap_nodes == num_llap_nodes_requested and num_llap_nodes == node_manager_cnt:
      min_mem_reserved_per_node = max(normalized_tez_am_container_size, slider_am_container_size)
      tez_AMs_per_node = llap_concurrency / num_llap_nodes
      tez_AMs_per_node_low = int(math.floor(tez_AMs_per_node))
      tez_AMs_per_node_high = int(math.ceil(tez_AMs_per_node))
      min_mem_reserved_per_node = int(max(tez_AMs_per_node_high * normalized_tez_am_container_size, tez_AMs_per_node_low * normalized_tez_am_container_size + slider_am_container_size))
      self.logger.info("DBG: Determined 'AM reservation per node': {0}, using following : concurrency: {1}, num_llap_nodes: {2}, AMsPerNode: {3}"
                  .format(min_mem_reserved_per_node, llap_concurrency, num_llap_nodes,  tez_AMs_per_node))

    max_single_node_mem_available_for_daemon = self._normalizeDown(yarn_nm_mem_in_mb_normalized - min_mem_reserved_per_node, yarn_min_container_size)
    if max_single_node_mem_available_for_daemon <=0 or max_single_node_mem_available_for_daemon < mem_per_thread_for_llap:
      self.logger.warning("Not enough capacity available per node for daemons after factoring in AM memory requirements. NM Mem: {0}, "
                     "minAMMemPerNode: {1}, available: {2}".format(yarn_nm_mem_in_mb_normalized, min_mem_reserved_per_node, max_single_node_mem_available_for_daemon))
      self.recommendDefaultLlapConfiguration(configurations, services, hosts)

    llap_daemon_mem_per_node = min(max_single_node_mem_available_for_daemon, llap_daemon_mem_per_node)
    self.logger.info("DBG: Determined final memPerDaemon: {0}, using following: concurrency: {1}, numNMNodes: {2}, numLlapNodes: {3} "
                .format(llap_daemon_mem_per_node, llap_concurrency, node_manager_cnt, num_llap_nodes))

    num_executors_per_node_max = self.get_max_executors_per_node(yarn_nm_mem_in_mb_normalized, cpu_per_nm_host, mem_per_thread_for_llap)
    if num_executors_per_node_max < 1:
      self.logger.warning("Calculated 'Max. Executors per Node' = {0}. Expected values >= 1.".format(num_executors_per_node_max))
      self.recommendDefaultLlapConfiguration(configurations, services, hosts)
      return
    self.logger.info("DBG: Calculated 'num_executors_per_node_max' : {0}, using following : yarn_nm_mem_in_mb_normalized : {1}, cpu_per_nm_host : {2}, "
                "mem_per_thread_for_llap: {3}".format(num_executors_per_node_max, yarn_nm_mem_in_mb_normalized, cpu_per_nm_host, mem_per_thread_for_llap))

    # NumExecutorsPerNode is not necessarily max - since some capacity would have been reserved for AMs, if this value were based on mem.
    num_executors_per_node = min(floor(llap_daemon_mem_per_node / mem_per_thread_for_llap), num_executors_per_node_max)
    if num_executors_per_node <= 0:
      self.logger.warning("Calculated 'Number of Executors Per Node' = {0}. Expected value >= 1".format(num_executors_per_node))
      self.recommendDefaultLlapConfiguration(configurations, services, hosts)
      return
    self.logger.info("DBG: Calculated 'num_executors_per_node' : {0}, using following : llap_daemon_mem_per_node : {1}, num_executors_per_node_max : {2}, "
                "mem_per_thread_for_llap: {3}".format(num_executors_per_node, llap_daemon_mem_per_node, num_executors_per_node_max, mem_per_thread_for_llap))

    # Now figure out how much of the memory will be used by the executors, and how much will be used by the cache.
    total_mem_for_executors_per_node = num_executors_per_node * mem_per_thread_for_llap
    cache_mem_per_node = llap_daemon_mem_per_node - total_mem_for_executors_per_node
    self.logger.info("DBG: Calculated 'Cache per node' : {0}, using following : llap_daemon_mem_per_node : {1}, total_mem_for_executors_per_node : {2}"
            .format(cache_mem_per_node, llap_daemon_mem_per_node, total_mem_for_executors_per_node))

    tez_runtime_io_sort_mb = (long((0.8 * mem_per_thread_for_llap) / 3))
    tez_runtime_unordered_output_buffer_size = long(0.8 * 0.075 * mem_per_thread_for_llap)
    # 'hive_auto_convert_join_noconditionaltask_size' value is in bytes. Thus, multiplying it by 1048576.
    hive_auto_convert_join_noconditionaltask_size = (long((0.8 * mem_per_thread_for_llap) / 3)) * MB_TO_BYTES

    # Calculate value for prop 'llap_heap_size'
    llap_xmx = max(total_mem_for_executors_per_node * 0.8, total_mem_for_executors_per_node - self.get_llap_headroom_space(services, configurations))
    self.logger.info("DBG: Calculated llap_app_heap_size : {0}, using following : total_mem_for_executors : {1}".format(llap_xmx, total_mem_for_executors_per_node))

    # Calculate 'hive_heapsize' for Hive2/HiveServer2 (HSI)
    hive_server_interactive_heapsize = None
    hive_server_interactive_hosts = self.getHostsWithComponent("HIVE", "HIVE_SERVER_INTERACTIVE", services, hosts)
    if hive_server_interactive_hosts is None:
      # If its None, read the base service YARN's NODEMANAGER node memory, as are host are considered homogenous.
      hive_server_interactive_hosts = self.getHostsWithComponent("YARN", "NODEMANAGER", services, hosts)
    if hive_server_interactive_hosts is not None and len(hive_server_interactive_hosts) > 0:
      host_mem = long(hive_server_interactive_hosts[0]["Hosts"]["total_mem"])
      hive_server_interactive_heapsize = min(max(2048.0, 400.0*llap_concurrency), 3.0/8 * host_mem)
      self.logger.info("DBG: Calculated 'hive_server_interactive_heapsize' : {0}, using following : llap_concurrency : {1}, host_mem : "
                  "{2}".format(hive_server_interactive_heapsize, llap_concurrency, host_mem))

    # Done with calculations, updating calculated configs.
    self.logger.info("DBG: Applying the calculated values....")

    if is_cluster_create_opr or changed_configs_has_enable_hive_int:
      normalized_tez_am_container_size = long(normalized_tez_am_container_size)
      putTezInteractiveSiteProperty('tez.am.resource.memory.mb', normalized_tez_am_container_size)
      self.logger.info("DBG: Setting 'tez.am.resource.memory.mb' config value as : {0}".format(normalized_tez_am_container_size))

    if not llap_concurrency_in_changed_configs:
      min_llap_concurrency = 1
      putHiveInteractiveSiteProperty('hive.server2.tez.sessions.per.default.queue', long(llap_concurrency))
      putHiveInteractiveSitePropertyAttribute('hive.server2.tez.sessions.per.default.queue', "minimum", min_llap_concurrency)

    # Check if 'max_llap_concurreny' < 'llap_concurrency'.
    if max_llap_concurreny < llap_concurrency:
      self.logger.info("DBG: Adjusting 'max_llap_concurreny' to : {0}, based on 'llap_concurrency' : {1} and "
                       "earlier 'max_llap_concurreny' : {2}. ".format(llap_concurrency, llap_concurrency, max_llap_concurreny))
      max_llap_concurreny = llap_concurrency
    putHiveInteractiveSitePropertyAttribute('hive.server2.tez.sessions.per.default.queue', "maximum", long(max_llap_concurreny))

    num_llap_nodes = long(num_llap_nodes)
    putHiveInteractiveEnvPropertyAttribute('num_llap_nodes', "minimum", min_nodes_required)
    putHiveInteractiveEnvPropertyAttribute('num_llap_nodes', "maximum", node_manager_cnt)
    #TODO A single value is not being set for numNodes in case of a custom queue. Also the attribute is set to non-visible, so the UI likely ends up using an old cached value
    if (num_llap_nodes != num_llap_nodes_requested):
      self.logger.info("DBG: User requested num_llap_nodes : {0}, but used/adjusted value for calculations is : {1}".format(num_llap_nodes_requested, num_llap_nodes))
    else:
      self.logger.info("DBG: Used num_llap_nodes for calculations : {0}".format(num_llap_nodes_requested))

    # Safeguard for not adding "num_llap_nodes_for_llap_daemons" if it doesnt exist in hive-interactive-site.
    # This can happen if we upgrade from Ambari 2.4 (with HDP 2.5) to Ambari 2.5, as this config is from 2.6 stack onwards only.
    if "hive-interactive-env" in services["configurations"] and \
        "num_llap_nodes_for_llap_daemons" in services["configurations"]["hive-interactive-env"]["properties"]:
      putHiveInteractiveEnvProperty('num_llap_nodes_for_llap_daemons', num_llap_nodes)
      self.logger.info("DBG: Setting config 'num_llap_nodes_for_llap_daemons' as : {0}".format(num_llap_nodes))

    llap_container_size = long(llap_daemon_mem_per_node)
    putHiveInteractiveSiteProperty('hive.llap.daemon.yarn.container.mb', llap_container_size)

    # Set 'hive.tez.container.size' only if it is read as "SET_ON_FIRST_INVOCATION", implying initialization.
    # Else, we don't (1). Override the previous calculated value or (2). User provided value.
    if is_cluster_create_opr or changed_configs_has_enable_hive_int:
      mem_per_thread_for_llap = long(mem_per_thread_for_llap)
      putHiveInteractiveSiteProperty('hive.tez.container.size', mem_per_thread_for_llap)
      self.logger.info("DBG: Setting 'hive.tez.container.size' config value as : {0}".format(mem_per_thread_for_llap))

    putTezInteractiveSiteProperty('tez.runtime.io.sort.mb', tez_runtime_io_sort_mb)
    if "tez-site" in services["configurations"] and "tez.runtime.sorter.class" in services["configurations"]["tez-site"]["properties"]:
      if services["configurations"]["tez-site"]["properties"]["tez.runtime.sorter.class"] == "LEGACY":
        putTezInteractiveSitePropertyAttribute("tez.runtime.io.sort.mb", "maximum", 1800)

    putTezInteractiveSiteProperty('tez.runtime.unordered.output.buffer.size-mb', tez_runtime_unordered_output_buffer_size)
    putHiveInteractiveSiteProperty('hive.auto.convert.join.noconditionaltask.size', hive_auto_convert_join_noconditionaltask_size)

    num_executors_per_node = long(num_executors_per_node)
    self.logger.info("DBG: Putting num_executors_per_node as {0}".format(num_executors_per_node))
    putHiveInteractiveSiteProperty('hive.llap.daemon.num.executors', num_executors_per_node)
    putHiveInteractiveSitePropertyAttribute('hive.llap.daemon.num.executors', "minimum", 1)
    putHiveInteractiveSitePropertyAttribute('hive.llap.daemon.num.executors', "maximum", long(num_executors_per_node_max))

    # 'hive.llap.io.threadpool.size' config value is to be set same as value calculated for
    # 'hive.llap.daemon.num.executors' at all times.
    cache_mem_per_node = long(cache_mem_per_node)

    putHiveInteractiveSiteProperty('hive.llap.io.threadpool.size', num_executors_per_node)
    putHiveInteractiveSiteProperty('hive.llap.io.memory.size', cache_mem_per_node)

    if hive_server_interactive_heapsize is not None:
      putHiveInteractiveEnvProperty("hive_heapsize", int(hive_server_interactive_heapsize))

    llap_io_enabled = 'true' if long(cache_mem_per_node) >= 64 else 'false'
    putHiveInteractiveSiteProperty('hive.llap.io.enabled', llap_io_enabled)

    putHiveInteractiveEnvProperty('llap_heap_size', long(llap_xmx))
    putHiveInteractiveEnvProperty('slider_am_container_mb', long(slider_am_container_size))
    self.logger.info("DBG: Done putting all configs")

  def recommendDefaultLlapConfiguration(self, configurations, services, hosts):
    self.logger.info("DBG: Something likely went wrong. recommendDefaultLlapConfiguration")
    putHiveInteractiveSiteProperty = self.putProperty(configurations, YARNRecommender.HIVE_INTERACTIVE_SITE, services)
    putHiveInteractiveSitePropertyAttribute = self.putPropertyAttribute(configurations, YARNRecommender.HIVE_INTERACTIVE_SITE)

    putHiveInteractiveEnvProperty = self.putProperty(configurations, "hive-interactive-env", services)
    putHiveInteractiveEnvPropertyAttribute = self.putPropertyAttribute(configurations, "hive-interactive-env")

    yarn_min_container_size = long(self.get_yarn_min_container_size(services, configurations))
    slider_am_container_size = long(self.calculate_slider_am_size(yarn_min_container_size))

    node_manager_host_list = self.getHostsForComponent(services, "YARN", "NODEMANAGER")
    node_manager_cnt = len(node_manager_host_list)

    putHiveInteractiveSiteProperty('hive.server2.tez.sessions.per.default.queue', 1)
    putHiveInteractiveSitePropertyAttribute('hive.server2.tez.sessions.per.default.queue', "minimum", 1)
    putHiveInteractiveSitePropertyAttribute('hive.server2.tez.sessions.per.default.queue', "maximum", 1)
    putHiveInteractiveEnvProperty('num_llap_nodes', 0)

    # Safeguard for not adding "num_llap_nodes_for_llap_daemons" if it doesnt exist in hive-interactive-site.
    # This can happen if we upgrade from Ambari 2.4 (with HDP 2.5) to Ambari 2.5, as this config is from 2.6 stack onwards only.
    if "hive-interactive-env" in services["configurations"] and \
        "num_llap_nodes_for_llap_daemons" in services["configurations"]["hive-interactive-env"]["properties"]:
      putHiveInteractiveEnvProperty('num_llap_nodes_for_llap_daemons', 0)
    putHiveInteractiveEnvPropertyAttribute('num_llap_nodes', "minimum", 1)
    putHiveInteractiveEnvPropertyAttribute('num_llap_nodes', "maximum", node_manager_cnt)
    putHiveInteractiveSiteProperty('hive.llap.daemon.yarn.container.mb', yarn_min_container_size)
    putHiveInteractiveSitePropertyAttribute('hive.llap.daemon.yarn.container.mb', "minimum", yarn_min_container_size)
    putHiveInteractiveSiteProperty('hive.llap.daemon.num.executors', 0)
    putHiveInteractiveSitePropertyAttribute('hive.llap.daemon.num.executors', "minimum", 1)
    putHiveInteractiveSiteProperty('hive.llap.io.threadpool.size', 0)
    putHiveInteractiveSiteProperty('hive.llap.io.memory.size', 0)
    putHiveInteractiveEnvProperty('llap_heap_size', 0)
    putHiveInteractiveEnvProperty('slider_am_container_mb', slider_am_container_size)

  def get_num_llap_nodes(self, services, configurations):
    """
    Returns current value of number of LLAP nodes in cluster (num_llap_nodes)

    :type services: dict
    :type configurations: dict
    :rtype int
    """
    hsi_env = self.getServicesSiteProperties(services, "hive-interactive-env")
    hsi_env_properties = self.getSiteProperties(configurations, "hive-interactive-env")
    num_llap_nodes = 0

    # Check if 'num_llap_nodes' is modified in current ST invocation.
    if hsi_env_properties and 'num_llap_nodes' in hsi_env_properties:
      num_llap_nodes = hsi_env_properties['num_llap_nodes']
    elif hsi_env and 'num_llap_nodes' in hsi_env:
      num_llap_nodes = hsi_env['num_llap_nodes']
    else:
      self.logger.error("Couldn't retrieve Hive Server 'num_llap_nodes' config. Setting value to {0}".format(num_llap_nodes))

    return float(num_llap_nodes)

  def get_max_executors_per_node(self, nm_mem_per_node_normalized, nm_cpus_per_node, mem_per_thread):
    # TODO: This potentially takes up the entire node leaving no space for AMs.
    return min(floor(nm_mem_per_node_normalized / mem_per_thread), nm_cpus_per_node)

  def calculate_mem_per_thread_for_llap(self, services, nm_mem_per_node_normalized, cpu_per_nm_host, is_cluster_create_opr=False,
                                        enable_hive_interactive_1st_invocation=False):
    """
    Calculates 'mem_per_thread_for_llap' for 1st time initialization. Else returns 'hive.tez.container.size' read value.
    """
    hive_tez_container_size = self.get_hive_tez_container_size(services)

    if is_cluster_create_opr or enable_hive_interactive_1st_invocation:
      if nm_mem_per_node_normalized <= 1024:
        calculated_hive_tez_container_size = min(512, nm_mem_per_node_normalized)
      elif nm_mem_per_node_normalized <= 4096:
        calculated_hive_tez_container_size = 1024
      elif nm_mem_per_node_normalized <= 10240:
        calculated_hive_tez_container_size = 2048
      elif nm_mem_per_node_normalized <= 24576:
        calculated_hive_tez_container_size = 3072
      else:
        calculated_hive_tez_container_size = 4096

      self.logger.info("DBG: Calculated and returning 'hive_tez_container_size' : {0}".format(calculated_hive_tez_container_size))
      return calculated_hive_tez_container_size
    else:
      self.logger.info("DBG: Returning 'hive_tez_container_size' : {0}".format(hive_tez_container_size))
      return hive_tez_container_size

  def get_hive_tez_container_size(self, services):
    """
    Gets HIVE Tez container size (hive.tez.container.size).
    """
    hive_container_size = None
    hsi_site = self.getServicesSiteProperties(services, YARNRecommender.HIVE_INTERACTIVE_SITE)
    if hsi_site and 'hive.tez.container.size' in hsi_site:
      hive_container_size = hsi_site['hive.tez.container.size']

    if not hive_container_size:
      # This can happen (1). If config is missing in hive-interactive-site or (2). its an
      # upgrade scenario from Ambari 2.4 to Ambari 2.5 with HDP 2.5 installed. Read it
      # from hive-site.
      #
      # If Ambari 2.5 after upgrade from 2.4 is managing HDP 2.6 here, this config would have
      # already been added in hive-interactive-site as part of HDP upgrade from 2.5 to 2.6,
      # and we wont end up in this block to look up in hive-site.
      hive_site = self.getServicesSiteProperties(services, "hive-site")
      if hive_site and 'hive.tez.container.size' in hive_site:
        hive_container_size = hive_site['hive.tez.container.size']

    return hive_container_size

  def get_llap_headroom_space(self, services, configurations):
    """
    Gets HIVE Server Interactive's 'llap_headroom_space' config. (Default value set to 6144 bytes).
    """
    llap_headroom_space = None
    # Check if 'llap_headroom_space' is modified in current SA invocation.
    if 'hive-interactive-env' in configurations and 'llap_headroom_space' in configurations['hive-interactive-env']['properties']:
      hive_container_size = float(configurations['hive-interactive-env']['properties']['llap_headroom_space'])
      self.logger.info("'llap_headroom_space' read from configurations as : {0}".format(llap_headroom_space))

    if llap_headroom_space is None:
      # Check if 'llap_headroom_space' is input in services array.
      if 'llap_headroom_space' in services['configurations']['hive-interactive-env']['properties']:
        llap_headroom_space = float(services['configurations']['hive-interactive-env']['properties']['llap_headroom_space'])
        self.logger.info("'llap_headroom_space' read from services as : {0}".format(llap_headroom_space))
    if not llap_headroom_space or llap_headroom_space < 1:
      llap_headroom_space = 6144 # 6GB
      self.logger.info("Couldn't read 'llap_headroom_space' from services or configurations. Returing default value : 6144 bytes")

    return llap_headroom_space

  def checkAndManageLlapQueue(self, services, configurations, hosts, llap_queue_name, llap_queue_cap_perc):
    """
    Checks and (1). Creates 'llap' queue if only 'default' queue exist at leaf level and is consuming 100% capacity OR
               (2). Updates 'llap' queue capacity and state, if current selected queue is 'llap', and only 2 queues exist
                    at root level : 'default' and 'llap'.
    """
    self.logger.info("Determining creation/adjustment of 'capacity-scheduler' for 'llap' queue.")
    putHiveInteractiveEnvProperty = self.putProperty(configurations, "hive-interactive-env", services)
    putHiveInteractiveSiteProperty = self.putProperty(configurations, YARNRecommender.HIVE_INTERACTIVE_SITE, services)
    putHiveInteractiveEnvPropertyAttribute = self.putPropertyAttribute(configurations, "hive-interactive-env")
    putCapSchedProperty = self.putProperty(configurations, "capacity-scheduler", services)
    leafQueueNames = None
    hsi_site = self.getServicesSiteProperties(services, YARNRecommender.HIVE_INTERACTIVE_SITE)

    capacity_scheduler_properties, received_as_key_value_pair = self.getCapacitySchedulerProperties(services)
    if capacity_scheduler_properties:
      leafQueueNames = self.getAllYarnLeafQueues(capacity_scheduler_properties)
      cap_sched_config_keys = capacity_scheduler_properties.keys()

      yarn_default_queue_capacity = -1
      if 'yarn.scheduler.capacity.root.default.capacity' in cap_sched_config_keys:
        yarn_default_queue_capacity = float(capacity_scheduler_properties.get('yarn.scheduler.capacity.root.default.capacity'))

      # Get 'llap' queue state
      currLlapQueueState = ''
      if 'yarn.scheduler.capacity.root.'+llap_queue_name+'.state' in cap_sched_config_keys:
        currLlapQueueState = capacity_scheduler_properties.get('yarn.scheduler.capacity.root.'+llap_queue_name+'.state')

      # Get 'llap' queue capacity
      currLlapQueueCap = -1
      if 'yarn.scheduler.capacity.root.'+llap_queue_name+'.capacity' in cap_sched_config_keys:
        currLlapQueueCap = int(float(capacity_scheduler_properties.get('yarn.scheduler.capacity.root.'+llap_queue_name+'.capacity')))

      updated_cap_sched_configs_str = ''

      enabled_hive_int_in_changed_configs = self.isConfigPropertiesChanged(services, "hive-interactive-env", ['enable_hive_interactive'], False)
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
                    ((currLlapQueueState == 'STOPPED' and enabled_hive_int_in_changed_configs) or (currLlapQueueState == 'RUNNING' and currLlapQueueCap != llap_queue_cap_perc)))):
        adjusted_default_queue_cap = str(100 - llap_queue_cap_perc)

        hive_user = '*'  # Open to all
        if 'hive_user' in services['configurations']['hive-env']['properties']:
          hive_user = services['configurations']['hive-env']['properties']['hive_user']

        llap_queue_cap_perc = str(llap_queue_cap_perc)

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
          updated_cap_sched_configs_str += """yarn.scheduler.capacity.root.{0}.user-limit-factor=1
yarn.scheduler.capacity.root.{0}.state=RUNNING
yarn.scheduler.capacity.root.{0}.ordering-policy=fifo
yarn.scheduler.capacity.root.{0}.minimum-user-limit-percent=100
yarn.scheduler.capacity.root.{0}.maximum-capacity={1}
yarn.scheduler.capacity.root.{0}.capacity={1}
yarn.scheduler.capacity.root.{0}.acl_submit_applications={2}
yarn.scheduler.capacity.root.{0}.acl_administer_queue={2}
yarn.scheduler.capacity.root.{0}.maximum-am-resource-percent=1""".format(llap_queue_name, llap_queue_cap_perc, hive_user)

          putCapSchedProperty("capacity-scheduler", updated_cap_sched_configs_str)
          self.logger.info("Updated 'capacity-scheduler' configs as one concatenated string.")
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
          putCapSchedProperty("yarn.scheduler.capacity.root." + llap_queue_name + ".maximum-capacity", llap_queue_cap_perc)
          putCapSchedProperty("yarn.scheduler.capacity.root." + llap_queue_name + ".capacity", llap_queue_cap_perc)
          putCapSchedProperty("yarn.scheduler.capacity.root." + llap_queue_name + ".acl_submit_applications", hive_user)
          putCapSchedProperty("yarn.scheduler.capacity.root." + llap_queue_name + ".acl_administer_queue", hive_user)
          putCapSchedProperty("yarn.scheduler.capacity.root." + llap_queue_name + ".maximum-am-resource-percent", "1")

          self.logger.info("Updated 'capacity-scheduler' configs as a dictionary.")
          updated_cap_sched_configs_as_dict = True

        if updated_cap_sched_configs_str or updated_cap_sched_configs_as_dict:
          if len(leafQueueNames) == 1: # 'llap' queue didn't exist before
            self.logger.info("Created YARN Queue : '{0}' with capacity : {1}%. Adjusted 'default' queue capacity to : {2}%" \
                        .format(llap_queue_name, llap_queue_cap_perc, adjusted_default_queue_cap))
          else: # Queue existed, only adjustments done.
            self.logger.info("Adjusted YARN Queue : '{0}'. Current capacity : {1}%. State: RUNNING.".format(llap_queue_name, llap_queue_cap_perc))
            self.logger.info("Adjusted 'default' queue capacity to : {0}%".format(adjusted_default_queue_cap))

          # Update Hive 'hive.llap.daemon.queue.name' prop to use 'llap' queue.
          putHiveInteractiveSiteProperty('hive.llap.daemon.queue.name', llap_queue_name)
          putHiveInteractiveSiteProperty('hive.server2.tez.default.queues', llap_queue_name)
          # Update 'hive.llap.daemon.queue.name' prop combo entries and llap capacity slider visibility.
          self.setLlapDaemonQueuePropAttributes(services, configurations)
      else:
        self.logger.debug("Not creating/adjusting {0} queue. Current YARN queues : {1}".format(llap_queue_name, list(leafQueueNames)))
    else:
      self.logger.error("Couldn't retrieve 'capacity-scheduler' properties while doing YARN queue adjustment for Hive Server Interactive.")

  def checkAndStopLlapQueue(self, services, configurations, llap_queue_name):
    """
    Checks and sees (1). If only two leaf queues exist at root level, namely: 'default' and 'llap',
                and (2). 'llap' is in RUNNING state.

    If yes, performs the following actions:   (1). 'llap' queue state set to STOPPED,
                                              (2). 'llap' queue capacity set to 0 %,
                                              (3). 'default' queue capacity set to 100 %
    """
    putCapSchedProperty = self.putProperty(configurations, "capacity-scheduler", services)
    putHiveInteractiveSiteProperty = self.putProperty(configurations, YARNRecommender.HIVE_INTERACTIVE_SITE, services)
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
          self.logger.error("{0} queue 'state' property not present in capacity scheduler. Skipping adjusting queues.".format(llap_queue_name))
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
          self.logger.debug("{0} queue state is : {1}. Skipping adjusting queues.".format(llap_queue_name, currLlapQueueState))
          return

        if updated_default_queue_configs and updated_llap_queue_configs:
          putCapSchedProperty("capacity-scheduler", updated_default_queue_configs+updated_llap_queue_configs)
          self.logger.info("Changed YARN '{0}' queue state to 'STOPPED', and capacity to 0%. Adjusted 'default' queue capacity to : {1}%" \
                      .format(llap_queue_name, DEFAULT_MAX_CAPACITY))

          # Update Hive 'hive.llap.daemon.queue.name' prop to use 'default' queue.
          putHiveInteractiveSiteProperty('hive.llap.daemon.queue.name', YARNRecommender.YARN_ROOT_DEFAULT_QUEUE_NAME)
          putHiveInteractiveSiteProperty('hive.server2.tez.default.queues', YARNRecommender.YARN_ROOT_DEFAULT_QUEUE_NAME)
      else:
        self.logger.debug("Not removing '{0}' queue as number of Queues not equal to 2. Current YARN queues : {1}".format(llap_queue_name, list(leafQueueNames)))
    else:
      self.logger.error("Couldn't retrieve 'capacity-scheduler' properties while doing YARN queue adjustment for Hive Server Interactive.")

  def setLlapDaemonQueuePropAttributes(self, services, configurations):
    """
    Checks and sets the 'Hive Server Interactive' 'hive.llap.daemon.queue.name' config Property Attributes.  Takes into
    account that 'capacity-scheduler' may have changed (got updated) in current Stack Advisor invocation.
    """
    self.logger.info("Determining 'hive.llap.daemon.queue.name' config Property Attributes.")
    #TODO Determine if this is doing the right thing if some queue is setup with capacity=0, or is STOPPED. Maybe don't list it.
    putHiveInteractiveSitePropertyAttribute = self.putPropertyAttribute(configurations, YARNRecommender.HIVE_INTERACTIVE_SITE)

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
            self.logger.info("'capacity-scheduler' configs is set as a single '\\n' separated string in current invocation. "
                        "count(configurations['capacity-scheduler']['properties']['capacity-scheduler']) = "
                        "{0}".format(len(capacity_scheduler_properties)))
          else:
            self.logger.info("Read configurations['capacity-scheduler']['properties']['capacity-scheduler'] is : {0}".format(cap_sched_props_as_str))
        else:
          self.logger.info("configurations['capacity-scheduler']['properties']['capacity-scheduler'] : {0}.".format(cap_sched_props_as_str))

      # if 'capacity_scheduler_properties' is empty, implies we may have 'capacity-scheduler' configs as dictionary
      # in configurations, if 'capacity-scheduler' changed in current invocation.
      if not capacity_scheduler_properties:
        if isinstance(cap_sched_props_as_dict, dict) and len(cap_sched_props_as_dict) > 1:
          capacity_scheduler_properties = cap_sched_props_as_dict
          self.logger.info("'capacity-scheduler' changed in current Stack Advisor invocation. Retrieved the configs as dictionary from configurations.")
        else:
          self.logger.info("Read configurations['capacity-scheduler']['properties'] is : {0}".format(cap_sched_props_as_dict))
    else:
      self.logger.info("'capacity-scheduler' not modified in the current Stack Advisor invocation.")


    # if 'capacity_scheduler_properties' is still empty, implies 'capacity_scheduler' wasn't change in current
    # SA invocation. Thus, read it from input : 'services'.
    if not capacity_scheduler_properties:
      capacity_scheduler_properties, received_as_key_value_pair = self.getCapacitySchedulerProperties(services)
      self.logger.info("'capacity-scheduler' not changed in current Stack Advisor invocation. Retrieved the configs from services.")

    # Get set of current YARN leaf queues.
    leafQueueNames = self.getAllYarnLeafQueues(capacity_scheduler_properties)
    if leafQueueNames:
      leafQueues = [{"label": str(queueName), "value": queueName} for queueName in leafQueueNames]
      leafQueues = sorted(leafQueues, key=lambda q: q['value'])
      putHiveInteractiveSitePropertyAttribute("hive.llap.daemon.queue.name", "entries", leafQueues)
      self.logger.info("'hive.llap.daemon.queue.name' config Property Attributes set to : {0}".format(leafQueues))
    else:
      self.logger.error("Problem retrieving YARN queues. Skipping updating HIVE Server Interactve "
                   "'hive.server2.tez.default.queues' property attributes.")

  #TODO  Convert this to a helper. It can apply to any property. Check config, or check if in the list of changed configurations and read the latest value
  def get_yarn_min_container_size(self, services, configurations):
    """
    Gets YARN's minimum container size (yarn.scheduler.minimum-allocation-mb).
    Reads from:
      - configurations (if changed as part of current Stack Advisor invocation (output)), and services["changed-configurations"]
        is empty, else
      - services['configurations'] (input).

    services["changed-configurations"] would be empty if Stack Advisor call is made from Blueprints (1st invocation). Subsequent
    Stack Advisor calls will have it non-empty. We do this because in subsequent invocations, even if Stack Advisor calculates this
    value (configurations), it is finally not recommended, making 'input' value to survive.

    :type services dict
    :type configurations dict
    :rtype str
    """
    yarn_min_container_size = None
    yarn_min_allocation_property = "yarn.scheduler.minimum-allocation-mb"
    yarn_site = self.getSiteProperties(configurations, "yarn-site")
    yarn_site_properties = self.getServicesSiteProperties(services, "yarn-site")

    # Check if services["changed-configurations"] is empty and 'yarn.scheduler.minimum-allocation-mb' is modified in current ST invocation.
    if not services["changed-configurations"] and yarn_site and yarn_min_allocation_property in yarn_site:
      yarn_min_container_size = yarn_site[yarn_min_allocation_property]
      self.logger.info("DBG: 'yarn.scheduler.minimum-allocation-mb' read from output as : {0}".format(yarn_min_container_size))

    # Check if 'yarn.scheduler.minimum-allocation-mb' is input in services array.
    elif yarn_site_properties and yarn_min_allocation_property in yarn_site_properties:
      yarn_min_container_size = yarn_site_properties[yarn_min_allocation_property]
      self.logger.info("DBG: 'yarn.scheduler.minimum-allocation-mb' read from services as : {0}".format(yarn_min_container_size))

    if not yarn_min_container_size:
      self.logger.error("{0} was not found in the configuration".format(yarn_min_allocation_property))

    return yarn_min_container_size

  def calculate_slider_am_size(self, yarn_min_container_size):
    """
    Calculates the Slider App Master size based on YARN's Minimum Container Size.

    :type yarn_min_container_size int
    """
    if yarn_min_container_size >= 1024:
      return 1024
    else:
      return 512

  def get_yarn_nm_mem_in_mb(self, services, configurations):
    """
    Gets YARN NodeManager memory in MB (yarn.nodemanager.resource.memory-mb).
    Reads from:
      - configurations (if changed as part of current Stack Advisor invocation (output)), and services["changed-configurations"]
        is empty, else
      - services['configurations'] (input).

    services["changed-configurations"] would be empty is Stack Advisor call if made from Blueprints (1st invocation). Subsequent
    Stack Advisor calls will have it non-empty. We do this because in subsequent invocations, even if Stack Advsior calculates this
    value (configurations), it is finally not recommended, making 'input' value to survive.
    """
    yarn_nm_mem_in_mb = None

    yarn_site = self.getServicesSiteProperties(services, "yarn-site")
    yarn_site_properties = self.getSiteProperties(configurations, "yarn-site")

    # Check if services["changed-configurations"] is empty and 'yarn.nodemanager.resource.memory-mb' is modified in current ST invocation.
    if not services["changed-configurations"] and yarn_site_properties and 'yarn.nodemanager.resource.memory-mb' in yarn_site_properties:
      yarn_nm_mem_in_mb = float(yarn_site_properties['yarn.nodemanager.resource.memory-mb'])
    elif yarn_site and 'yarn.nodemanager.resource.memory-mb' in yarn_site:
      # Check if 'yarn.nodemanager.resource.memory-mb' is input in services array.
      yarn_nm_mem_in_mb = float(yarn_site['yarn.nodemanager.resource.memory-mb'])

    if yarn_nm_mem_in_mb <= 0.0:
      self.logger.warning("'yarn.nodemanager.resource.memory-mb' current value : {0}. Expected value : > 0".format(yarn_nm_mem_in_mb))

    return yarn_nm_mem_in_mb

  def calculate_tez_am_container_size(self, services, total_cluster_capacity, is_cluster_create_opr=False,
                                      enable_hive_interactive_1st_invocation=False):
    """
    Calculates Tez App Master container size (tez.am.resource.memory.mb) for tez_hive2/tez-site on initialization if values read is 0.
    Else returns the read value.
    """
    tez_am_resource_memory_mb = self.get_tez_am_resource_memory_mb(services)
    calculated_tez_am_resource_memory_mb = None
    if is_cluster_create_opr or enable_hive_interactive_1st_invocation:
      if total_cluster_capacity <= 4096:
        calculated_tez_am_resource_memory_mb = 512
      elif total_cluster_capacity > 4096 and total_cluster_capacity <= 98304:
        calculated_tez_am_resource_memory_mb = 1024
      elif total_cluster_capacity > 98304:
        calculated_tez_am_resource_memory_mb = 4096

      self.logger.info("DBG: Calculated and returning 'tez_am_resource_memory_mb' as : {0}".format(calculated_tez_am_resource_memory_mb))
      return float(calculated_tez_am_resource_memory_mb)
    else:
      self.logger.info("DBG: Returning 'tez_am_resource_memory_mb' as : {0}".format(tez_am_resource_memory_mb))
      return float(tez_am_resource_memory_mb)

  def get_tez_am_resource_memory_mb(self, services):
    """
    Gets Tez's AM resource memory (tez.am.resource.memory.mb) from services.
    """
    tez_am_resource_memory_mb = None
    if 'tez.am.resource.memory.mb' in services['configurations']['tez-interactive-site']['properties']:
      tez_am_resource_memory_mb = services['configurations']['tez-interactive-site']['properties']['tez.am.resource.memory.mb']

    return tez_am_resource_memory_mb

  def min_queue_perc_reqd_for_llap_and_hive_app(self, services, hosts, configurations):
    """
    Calculate minimum queue capacity required in order to get LLAP and HIVE2 app into running state.
    """
    # Get queue size if sized at 20%
    node_manager_hosts = self.getHostsForComponent(services, "YARN", "NODEMANAGER")
    yarn_rm_mem_in_mb = self.get_yarn_nm_mem_in_mb(services, configurations)
    total_cluster_cap = len(node_manager_hosts) * yarn_rm_mem_in_mb
    total_queue_size_at_20_perc = 20.0 / 100 * total_cluster_cap

    # Calculate based on minimum size required by containers.
    yarn_min_container_size = long(self.get_yarn_min_container_size(services, configurations))
    slider_am_size = self.calculate_slider_am_size(float(yarn_min_container_size))
    hive_tez_container_size = long(self.get_hive_tez_container_size(services))
    tez_am_container_size = self.calculate_tez_am_container_size(services, long(total_cluster_cap))
    normalized_val = self._normalizeUp(slider_am_size, yarn_min_container_size) \
                     + self._normalizeUp(hive_tez_container_size, yarn_min_container_size) \
                     + self._normalizeUp(tez_am_container_size, yarn_min_container_size)

    min_required = max(total_queue_size_at_20_perc, normalized_val)
    min_required_perc = min_required * 100 / total_cluster_cap

    return int(ceil(min_required_perc))

  def _normalizeDown(self, val1, val2):
    """
    Normalize down 'val2' with respect to 'val1'.
    """
    tmp = floor(val1 / val2)
    if tmp < 1.00:
      return val1
    return tmp * val2

  def _normalizeUp(self, val1, val2):
    """
    Normalize up 'val2' with respect to 'val1'.
    """
    tmp = ceil(val1 / val2)
    return tmp * val2

  def __getQueueStateFromCapacityScheduler(self, capacity_scheduler_properties, llap_daemon_selected_queue_name):
    """
    Retrieves the passed in queue's 'state' from Capacity Scheduler.
    """
    # Identify the key which contains the state for 'llap_daemon_selected_queue_name'.
    cap_sched_keys = capacity_scheduler_properties.keys()
    llap_selected_queue_state_key =  None
    llap_selected_queue_state = None
    for key in cap_sched_keys:
      if key.endswith(llap_daemon_selected_queue_name+".state"):
        llap_selected_queue_state_key = key
        break
    llap_selected_queue_state = capacity_scheduler_properties.get(llap_selected_queue_state_key)
    return llap_selected_queue_state

  def __getQueueAmFractionFromCapacityScheduler(self, capacity_scheduler_properties, llap_daemon_selected_queue_name):
    """
    Retrieves the passed in queue's 'AM fraction' from Capacity Scheduler. Returns default value of 0.1 if AM Percent
    pertaining to passed-in queue is not present.
    """
    # Identify the key which contains the AM fraction for 'llap_daemon_selected_queue_name'.
    cap_sched_keys = capacity_scheduler_properties.keys()
    llap_selected_queue_am_percent_key = None
    for key in cap_sched_keys:
      if key.endswith("." + llap_daemon_selected_queue_name+".maximum-am-resource-percent"):
        llap_selected_queue_am_percent_key = key
        self.logger.info("AM percent key got for '{0}' queue is : '{1}'".format(llap_daemon_selected_queue_name, llap_selected_queue_am_percent_key))
        break
    if llap_selected_queue_am_percent_key is None:
      self.logger.info("Returning default AM percent value : '0.1' for queue : {0}".format(llap_daemon_selected_queue_name))
      return 0.1 # Default value to use if we couldn't retrieve queue's corresponding AM Percent key.
    else:
      llap_selected_queue_am_percent = capacity_scheduler_properties.get(llap_selected_queue_am_percent_key)
      self.logger.info("Returning read value for key '{0}' as : '{1}' for queue : '{2}'".format(llap_selected_queue_am_percent_key,
                                                                                           llap_selected_queue_am_percent,
                                                                                           llap_daemon_selected_queue_name))
      return llap_selected_queue_am_percent

  def __getSelectedQueueTotalCap(self, capacity_scheduler_properties, llap_daemon_selected_queue_name, total_cluster_capacity):
    """
    Calculates the total available capacity for the passed-in YARN queue of any level based on the percentages.
    """
    self.logger.info("Entered __getSelectedQueueTotalCap fn() with llap_daemon_selected_queue_name= '{0}'.".format(llap_daemon_selected_queue_name))
    available_capacity = total_cluster_capacity
    queue_cap_key = self.__getQueueCapacityKeyFromCapacityScheduler(capacity_scheduler_properties, llap_daemon_selected_queue_name)
    if queue_cap_key:
      queue_cap_key = queue_cap_key.strip()
      if len(queue_cap_key) >= 34:  # len('yarn.scheduler.capacity.<single letter queue name>.capacity') = 34
        # Expected capacity prop key is of form : 'yarn.scheduler.capacity.<one or more queues (path)>.capacity'
        queue_path = queue_cap_key[24:]  # Strip from beginning 'yarn.scheduler.capacity.'
        queue_path = queue_path[0:-9]  # Strip from end '.capacity'
        queues_list = queue_path.split('.')
        self.logger.info("Queue list : {0}".format(queues_list))
        if queues_list:
          for queue in queues_list:
            queue_cap_key = self.__getQueueCapacityKeyFromCapacityScheduler(capacity_scheduler_properties, queue)
            queue_cap_perc = float(capacity_scheduler_properties.get(queue_cap_key))
            available_capacity = queue_cap_perc / 100 * available_capacity
            self.logger.info("Total capacity available for queue {0} is : {1}".format(queue, available_capacity))

    # returns the capacity calculated for passed-in queue in 'llap_daemon_selected_queue_name'.
    return available_capacity

  def __getQueueCapacityKeyFromCapacityScheduler(self, capacity_scheduler_properties, llap_daemon_selected_queue_name):
    """
    Retrieves the passed in queue's 'capacity' related key from Capacity Scheduler.
    """
    # Identify the key which contains the capacity for 'llap_daemon_selected_queue_name'.
    cap_sched_keys = capacity_scheduler_properties.keys()
    llap_selected_queue_cap_key =  None
    current_selected_queue_for_llap_cap = None
    for key in cap_sched_keys:
      # Expected capacity prop key is of form : 'yarn.scheduler.capacity.<one or more queues in path separated by '.'>.[llap_daemon_selected_queue_name].capacity'
      if key.endswith(llap_daemon_selected_queue_name+".capacity") and key.startswith("yarn.scheduler.capacity.root"):
        self.logger.info("DBG: Selected queue name as: " + key)
        llap_selected_queue_cap_key = key
        break
    return llap_selected_queue_cap_key
  #endregion


class MAPREDUCE2Recommender(YARNRecommender):
  """
  MAPREDUCE2 Recommender suggests properties when adding the service for the first time or modifying configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(MAPREDUCE2Recommender, self)
    self.as_super.__init__(*args, **kwargs)

  def recommendMapReduce2ConfigurationsFromHDP206(self, configurations, clusterData, services, hosts):
    putMapredProperty = self.putProperty(configurations, "mapred-site", services)
    putMapredProperty('yarn.app.mapreduce.am.resource.mb', int(clusterData['amMemory']))
    putMapredProperty('yarn.app.mapreduce.am.command-opts', "-Xmx" + str(int(round(0.8 * clusterData['amMemory']))) + "m")
    putMapredProperty('mapreduce.map.memory.mb', clusterData['mapMemory'])
    putMapredProperty('mapreduce.reduce.memory.mb', int(clusterData['reduceMemory']))
    putMapredProperty('mapreduce.map.java.opts', "-Xmx" + str(int(round(0.8 * clusterData['mapMemory']))) + "m")
    putMapredProperty('mapreduce.reduce.java.opts', "-Xmx" + str(int(round(0.8 * clusterData['reduceMemory']))) + "m")
    putMapredProperty('mapreduce.task.io.sort.mb', min(int(round(0.4 * clusterData['mapMemory'])), 1024))

    mapred_mounts = [
      ("mapred.local.dir", ["TASKTRACKER", "NODEMANAGER"], "/hadoop/mapred", "multi")
    ]

    self.updateMountProperties("mapred-site", mapred_mounts, configurations, services, hosts)

    mr_queue = self.recommendYarnQueue(services, "mapred-site", "mapreduce.job.queuename")
    if mr_queue is not None:
      putMapredProperty("mapreduce.job.queuename", mr_queue)

  def recommendMapReduce2ConfigurationsFromHDP22(self, configurations, clusterData, services, hosts):
    # Needs to be able to access yarn-site
    # TODO, this is a hack that was introduced in 2015. The yarn-site configs will not actually be saved
    # as part of MAPREDUCE2 because yarn-site doesn't belong to it according to its metainfo.xml
    # MAPREDUCE2 Recommender first needs to call all methods from YARN Recommender
    self.recommendYARNConfigurationsFromHDP206(configurations, clusterData, services, hosts)
    self.recommendYARNConfigurationsFromHDP22(configurations, clusterData, services, hosts)

    putMapredProperty = self.putProperty(configurations, "mapred-site", services)
    nodemanagerMinRam = 1048576 # 1TB in mb
    if "referenceNodeManagerHost" in clusterData:
      nodemanagerMinRam = min(clusterData["referenceNodeManagerHost"]["total_mem"]/1024, nodemanagerMinRam)

    putMapredProperty('yarn.app.mapreduce.am.resource.mb', max(int(clusterData['ramPerContainer']),int(configurations["yarn-site"]["properties"]["yarn.scheduler.minimum-allocation-mb"])))
    putMapredProperty('yarn.app.mapreduce.am.command-opts', "-Xmx" + str(int(0.8 * int(configurations["mapred-site"]["properties"]["yarn.app.mapreduce.am.resource.mb"]))) + "m" + " -Dhdp.version=${hdp.version}")
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    min_mapreduce_map_memory_mb = 0
    min_mapreduce_reduce_memory_mb = 0
    min_mapreduce_map_java_opts = 0
    if ("PIG" in servicesList) and clusterData["totalAvailableRam"] >= 4096:
      min_mapreduce_map_memory_mb = 1536
      min_mapreduce_reduce_memory_mb = 1536
      min_mapreduce_map_java_opts = 1024

    putMapredProperty('mapreduce.map.memory.mb',
                      min(int(configurations["yarn-site"]["properties"]["yarn.scheduler.maximum-allocation-mb"]),
                          max(min_mapreduce_map_memory_mb,
                              max(int(clusterData['ramPerContainer']),
                                  int(configurations["yarn-site"]["properties"]["yarn.scheduler.minimum-allocation-mb"])))))
    putMapredProperty('mapreduce.reduce.memory.mb',
                      min(int(configurations["yarn-site"]["properties"]["yarn.scheduler.maximum-allocation-mb"]),
                          max(max(min_mapreduce_reduce_memory_mb,
                                  int(configurations["yarn-site"]["properties"]["yarn.scheduler.minimum-allocation-mb"])),
                              min(2*int(clusterData['ramPerContainer']),
                                  int(nodemanagerMinRam)))))

    mapredMapXmx = int(0.8*int(configurations["mapred-site"]["properties"]["mapreduce.map.memory.mb"]))
    putMapredProperty('mapreduce.map.java.opts', "-Xmx" + str(max(min_mapreduce_map_java_opts, mapredMapXmx)) + "m")
    putMapredProperty('mapreduce.reduce.java.opts', "-Xmx" + str(int(0.8*int(configurations["mapred-site"]["properties"]["mapreduce.reduce.memory.mb"]))) + "m")
    putMapredProperty('mapreduce.task.io.sort.mb', str(min(int(0.7*mapredMapXmx), 2047)))
    # Property Attributes
    putMapredPropertyAttribute = self.putPropertyAttribute(configurations, "mapred-site")
    yarnMinAllocationSize = int(configurations["yarn-site"]["properties"]["yarn.scheduler.minimum-allocation-mb"])
    yarnMaxAllocationSize = min(30 * int(configurations["yarn-site"]["properties"]["yarn.scheduler.minimum-allocation-mb"]), int(configurations["yarn-site"]["properties"]["yarn.scheduler.maximum-allocation-mb"]))
    putMapredPropertyAttribute("mapreduce.map.memory.mb", "maximum", yarnMaxAllocationSize)
    putMapredPropertyAttribute("mapreduce.map.memory.mb", "minimum", yarnMinAllocationSize)
    putMapredPropertyAttribute("mapreduce.reduce.memory.mb", "maximum", yarnMaxAllocationSize)
    putMapredPropertyAttribute("mapreduce.reduce.memory.mb", "minimum", yarnMinAllocationSize)
    putMapredPropertyAttribute("yarn.app.mapreduce.am.resource.mb", "maximum", yarnMaxAllocationSize)
    putMapredPropertyAttribute("yarn.app.mapreduce.am.resource.mb", "minimum", yarnMinAllocationSize)
    # Hadoop MR limitation
    putMapredPropertyAttribute("mapreduce.task.io.sort.mb", "maximum", "2047")

    # TODO, this is repeated in 2.0.6 recommendation
    mr_queue = self.recommendYarnQueue(services, "mapred-site", "mapreduce.job.queuename")
    if mr_queue is not None:
      putMapredProperty("mapreduce.job.queuename", mr_queue)


class YARNValidator(service_advisor.ServiceAdvisor):
  """
  YARN Validator checks the correctness of properties whenever the service is first added or the user attempts to
  change configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(YARNValidator, self)
    self.as_super.__init__(*args, **kwargs)

    self.validators = [("yarn-site", self.validateYARNSiteConfigurationsFromHDP206),
                       ("yarn-site", self.validateYARNSiteConfigurationsFromHDP25),
                       ("yarn-site" , self.validateYARNSiteConfigurationsFromHDP26),
                       ("yarn-env", self.validateYARNEnvConfigurationsFromHDP206),
                       ("yarn-env", self.validateYARNEnvConfigurationsFromHDP22),
                       ("ranger-yarn-plugin-properties", self.validateYARNRangerPluginConfigurationsFromHDP22)]

    # **********************************************************
    # Example of how to add a function that validates a certain config type.
    # If the same config type has multiple functions, can keep adding tuples to self.validators
    #self.validators.append(("hadoop-env", self.sampleValidator))

  def sampleValidator(self, properties, recommendedDefaults, configurations, services, hosts):
    """
    Example of a validator function other other Service Advisors to emulate.
    :return: A list of configuration validation problems.
    """
    validationItems = []

    '''
    Item is a simple dictionary.
    Two functions can be used to construct it depending on the log level: WARN|ERROR
    E.g.,
    self.getErrorItem(message) or self.getWarnItem(message)

    item = {"level": "ERROR|WARN", "message": "value"}
    '''
    validationItems.append({"config-name": "my_config_property_name",
                            "item": self.getErrorItem("My custom message in method %s" % inspect.stack()[0][3])})
    return self.toConfigurationValidationProblems(validationItems, "hadoop-env")

  def validateYARNSiteConfigurationsFromHDP206(self, properties, recommendedDefaults, configurations, services, hosts):
    """
    This was copied from HDP 2.0.6; validate yarn-site
    :return: A list of configuration validation problems.
    """
    clusterEnv = self.getSiteProperties(configurations, "cluster-env")
    validationItems = [ {"config-name": 'yarn.nodemanager.resource.memory-mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'yarn.nodemanager.resource.memory-mb')},
                        {"config-name": 'yarn.scheduler.minimum-allocation-mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'yarn.scheduler.minimum-allocation-mb')},
                        {"config-name": 'yarn.nodemanager.linux-container-executor.group', "item": self.validatorEqualsPropertyItem(properties, "yarn.nodemanager.linux-container-executor.group", clusterEnv, "user_group")},
                        {"config-name": 'yarn.scheduler.maximum-allocation-mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'yarn.scheduler.maximum-allocation-mb')} ]
    return self.toConfigurationValidationProblems(validationItems, "yarn-site")

  def validateYARNSiteConfigurationsFromHDP25(self, properties, recommendedDefaults, configurations, services, hosts):
    yarn_site_properties = self.getSiteProperties(configurations, "yarn-site")
    validationItems = []

    hsi_hosts = self.getHostsForComponent(services, "HIVE", "HIVE_SERVER_INTERACTIVE")
    if len(hsi_hosts) > 0:
      # HIVE_SERVER_INTERACTIVE is mapped to a host
      if 'yarn.resourcemanager.work-preserving-recovery.enabled' not in yarn_site_properties or \
              'true' != yarn_site_properties['yarn.resourcemanager.work-preserving-recovery.enabled']:
        validationItems.append({"config-name": "yarn.resourcemanager.work-preserving-recovery.enabled",
                                "item": self.getWarnItem(
                                  "While enabling HIVE_SERVER_INTERACTIVE it is recommended that you enable work preserving restart in YARN.")})

    validationProblems = self.toConfigurationValidationProblems(validationItems, "yarn-site")
    return validationProblems

  def validateYARNSiteConfigurationsFromHDP26(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = []
    siteProperties = services["configurations"]["yarn-site"]["properties"]
    if services["configurations"]["yarn-site"]["properties"]["yarn.http.policy"] == 'HTTP_ONLY':
      webapp_address = services["configurations"]["yarn-site"]["properties"]["yarn.timeline-service.webapp.address"]
      propertyValue = "http://"+webapp_address+"/ws/v1/applicationhistory"
    else:
      webapp_address = services["configurations"]["yarn-site"]["properties"]["yarn.timeline-service.webapp.https.address"]
      propertyValue = "https://"+webapp_address+"/ws/v1/applicationhistory"
      self.logger.info("validateYarnSiteConfigurations: recommended value for webservice url"+services["configurations"]["yarn-site"]["properties"]["yarn.log.server.web-service.url"])
    if services["configurations"]["yarn-site"]["properties"]["yarn.log.server.web-service.url"] != propertyValue:
      validationItems = [
                      {"config-name": "yarn.log.server.web-service.url",
                       "item": self.getWarnItem("Value should be %s" % propertyValue)}]
    return self.toConfigurationValidationProblems(validationItems, "yarn-site")

  def validateYARNEnvConfigurationsFromHDP206(self, properties, recommendedDefaults, configurations, services, hosts):
    """
    This was copied from HDP 2.0.6; validate yarn-env
    :return: A list of configuration validation problems.
    """
    validationItems = [{"config-name": 'service_check.queue.name', "item": self.validatorYarnQueue(properties, recommendedDefaults, 'service_check.queue.name', services)} ]
    return self.toConfigurationValidationProblems(validationItems, "yarn-env")

  def validateYARNEnvConfigurationsFromHDP22(self, properties, recommendedDefaults, configurations, services, hosts):
    """
    This was copied from HDP 2.2; validate yarn-env
    :return: A list of configuration validation problems.
    """
    validationItems = []
    if "yarn_cgroups_enabled" in properties:
      yarn_cgroups_enabled = properties["yarn_cgroups_enabled"].lower() == "true"
      core_site_properties = self.getSiteProperties(configurations, "core-site")
      security_enabled = False
      if core_site_properties:
        security_enabled = core_site_properties['hadoop.security.authentication'] == 'kerberos' and core_site_properties['hadoop.security.authorization'] == 'true'
      if not security_enabled and yarn_cgroups_enabled:
        validationItems.append({"config-name": "yarn_cgroups_enabled",
                                "item": self.getWarnItem("CPU Isolation should only be enabled if security is enabled")})
    validationProblems = self.toConfigurationValidationProblems(validationItems, "yarn-env")
    return validationProblems

  def validateYARNRangerPluginConfigurationsFromHDP22(self, properties, recommendedDefaults, configurations, services, hosts):
    """
    This was copied from HDP 2.2; validate ranger-yarn-plugin-properties
    :return: A list of configuration validation problems.
    """
    validationItems = []
    ranger_plugin_properties = self.getSiteProperties(configurations, "ranger-yarn-plugin-properties")
    ranger_plugin_enabled = ranger_plugin_properties['ranger-yarn-plugin-enabled'] if ranger_plugin_properties else 'No'
    if ranger_plugin_enabled.lower() == 'yes':
      # ranger-hdfs-plugin must be enabled in ranger-env
      ranger_env = self.getServicesSiteProperties(services, 'ranger-env')
      if not ranger_env or not 'ranger-yarn-plugin-enabled' in ranger_env or \
              ranger_env['ranger-yarn-plugin-enabled'].lower() != 'yes':
        validationItems.append({"config-name": 'ranger-yarn-plugin-enabled',
                                "item": self.getWarnItem(
                                  "ranger-yarn-plugin-properties/ranger-yarn-plugin-enabled must correspond ranger-env/ranger-yarn-plugin-enabled")})
    return self.toConfigurationValidationProblems(validationItems, "ranger-yarn-plugin-properties")


class MAPREDUCE2Validator(service_advisor.ServiceAdvisor):
  """
  YARN Validator checks the correctness of properties whenever the service is first added or the user attempts to
  change configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(MAPREDUCE2Validator, self)
    self.as_super.__init__(*args, **kwargs)

    self.validators = [("mapred-site", self.validateMapReduce2SiteConfigurationsFromHDP206),
                       ("mapred-site", self.validateMapReduce2SiteConfigurationsFromHDP22)]

    # **********************************************************
    # Example of how to add a function that validates a certain config type.
    # If the same config type has multiple functions, can keep adding tuples to self.validators
    #self.validators.append(("hadoop-env", self.sampleValidator))

  def sampleValidator(self, properties, recommendedDefaults, configurations, services, hosts):
    """
    Example of a validator function other other Service Advisors to emulate.
    :return: A list of configuration validation problems.
    """
    validationItems = []

    '''
    Item is a simple dictionary.
    Two functions can be used to construct it depending on the log level: WARN|ERROR
    E.g.,
    self.getErrorItem(message) or self.getWarnItem(message)

    item = {"level": "ERROR|WARN", "message": "value"}
    '''
    validationItems.append({"config-name": "my_config_property_name",
                            "item": self.getErrorItem("My custom message in method %s" % inspect.stack()[0][3])})
    return self.toConfigurationValidationProblems(validationItems, "hadoop-env")

  def validateMapReduce2SiteConfigurationsFromHDP206(self, properties, recommendedDefaults, configurations, services, hosts):
    """
    This was copied from HDP 2.0.6; validate mapred-site
    :return: A list of configuration validation problems.
    """
    validationItems = [ {"config-name": 'mapreduce.map.java.opts', "item": self.validateXmxValue(properties, recommendedDefaults, 'mapreduce.map.java.opts')},
                        {"config-name": 'mapreduce.reduce.java.opts', "item": self.validateXmxValue(properties, recommendedDefaults, 'mapreduce.reduce.java.opts')},
                        {"config-name": 'mapreduce.task.io.sort.mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'mapreduce.task.io.sort.mb')},
                        {"config-name": 'mapreduce.map.memory.mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'mapreduce.map.memory.mb')},
                        {"config-name": 'mapreduce.reduce.memory.mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'mapreduce.reduce.memory.mb')},
                        {"config-name": 'yarn.app.mapreduce.am.resource.mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'yarn.app.mapreduce.am.resource.mb')},
                        {"config-name": 'yarn.app.mapreduce.am.command-opts', "item": self.validateXmxValue(properties, recommendedDefaults, 'yarn.app.mapreduce.am.command-opts')},
                        {"config-name": 'mapreduce.job.queuename', "item": self.validatorYarnQueue(properties, recommendedDefaults, 'mapreduce.job.queuename', services)} ]
    return self.toConfigurationValidationProblems(validationItems, "mapred-site")

  def validateMapReduce2SiteConfigurationsFromHDP22(self, properties, recommendedDefaults, configurations, services, hosts):
    """
    This was copied from HDP 2.2; validate mapred-site
    :return: A list of configuration validation problems.
    """
    validationItems = [ {"config-name": 'mapreduce.map.java.opts', "item": self.validateXmxValue(properties, recommendedDefaults, 'mapreduce.map.java.opts')},
                        {"config-name": 'mapreduce.reduce.java.opts', "item": self.validateXmxValue(properties, recommendedDefaults, 'mapreduce.reduce.java.opts')},
                        {"config-name": 'mapreduce.task.io.sort.mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'mapreduce.task.io.sort.mb')},
                        {"config-name": 'mapreduce.map.memory.mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'mapreduce.map.memory.mb')},
                        {"config-name": 'mapreduce.reduce.memory.mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'mapreduce.reduce.memory.mb')},
                        {"config-name": 'yarn.app.mapreduce.am.resource.mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'yarn.app.mapreduce.am.resource.mb')},
                        {"config-name": 'yarn.app.mapreduce.am.command-opts', "item": self.validateXmxValue(properties, recommendedDefaults, 'yarn.app.mapreduce.am.command-opts')},
                        {"config-name": 'mapreduce.job.queuename', "item": self.validatorYarnQueue(properties, recommendedDefaults, 'mapreduce.job.queuename', services)} ]

    if 'mapreduce.map.java.opts' in properties and \
        self.checkXmxValueFormat(properties['mapreduce.map.java.opts']):
      mapreduceMapJavaOpts = self.formatXmxSizeToBytes(self.getXmxSize(properties['mapreduce.map.java.opts'])) / (1024.0 * 1024)
      mapreduceMapMemoryMb = self.to_number(properties['mapreduce.map.memory.mb'])
      if mapreduceMapJavaOpts > mapreduceMapMemoryMb:
        validationItems.append({"config-name": 'mapreduce.map.java.opts', "item": self.getWarnItem("mapreduce.map.java.opts Xmx should be less than mapreduce.map.memory.mb ({0})".format(mapreduceMapMemoryMb))})

    if 'mapreduce.reduce.java.opts' in properties and \
        self.checkXmxValueFormat(properties['mapreduce.reduce.java.opts']):
      mapreduceReduceJavaOpts = self.formatXmxSizeToBytes(self.getXmxSize(properties['mapreduce.reduce.java.opts'])) / (1024.0 * 1024)
      mapreduceReduceMemoryMb = self.to_number(properties['mapreduce.reduce.memory.mb'])
      if mapreduceReduceJavaOpts > mapreduceReduceMemoryMb:
        validationItems.append({"config-name": 'mapreduce.reduce.java.opts', "item": self.getWarnItem("mapreduce.reduce.java.opts Xmx should be less than mapreduce.reduce.memory.mb ({0})".format(mapreduceReduceMemoryMb))})

    if 'yarn.app.mapreduce.am.command-opts' in properties and \
        self.checkXmxValueFormat(properties['yarn.app.mapreduce.am.command-opts']):
      yarnAppMapreduceAmCommandOpts = self.formatXmxSizeToBytes(self.getXmxSize(properties['yarn.app.mapreduce.am.command-opts'])) / (1024.0 * 1024)
      yarnAppMapreduceAmResourceMb = self.to_number(properties['yarn.app.mapreduce.am.resource.mb'])
      if yarnAppMapreduceAmCommandOpts > yarnAppMapreduceAmResourceMb:
        validationItems.append({"config-name": 'yarn.app.mapreduce.am.command-opts', "item": self.getWarnItem("yarn.app.mapreduce.am.command-opts Xmx should be less than yarn.app.mapreduce.am.resource.mb ({0})".format(yarnAppMapreduceAmResourceMb))})

    return self.toConfigurationValidationProblems(validationItems, "mapred-site")
