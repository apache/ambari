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
import json
import math
import re
from resource_management.libraries.functions import format


class HDP26StackAdvisor(HDP25StackAdvisor):
  def __init__(self):
      super(HDP26StackAdvisor, self).__init__()
      self.initialize_logger("HDP26StackAdvisor")

  def getServiceConfigurationRecommenderDict(self):
      parentRecommendConfDict = super(HDP26StackAdvisor, self).getServiceConfigurationRecommenderDict()
      childRecommendConfDict = {
        "DRUID": self.recommendDruidConfigurations,
        "SUPERSET": self.recommendSupersetConfigurations,
        "ATLAS": self.recommendAtlasConfigurations,
        "TEZ": self.recommendTezConfigurations,
        "RANGER": self.recommendRangerConfigurations,
        "RANGER_KMS": self.recommendRangerKMSConfigurations,
        "HDFS": self.recommendHDFSConfigurations,
        "HIVE": self.recommendHIVEConfigurations,
        "HBASE": self.recommendHBASEConfigurations,
        "YARN": self.recommendYARNConfigurations,
        "KAFKA": self.recommendKAFKAConfigurations,
        "SPARK2": self.recommendSPARK2Configurations,
        "ZEPPELIN": self.recommendZEPPELINConfigurations
      }
      parentRecommendConfDict.update(childRecommendConfDict)
      return parentRecommendConfDict

  def recommendSPARK2Configurations(self, configurations, clusterData, services, hosts):
    """
    :type configurations dict
    :type clusterData dict
    :type services dict
    :type hosts dict
    """
    super(HDP26StackAdvisor, self).recommendSpark2Configurations(configurations, clusterData, services, hosts)
    self.__addZeppelinToLivy2SuperUsers(configurations, services)

  def recommendZEPPELINConfigurations(self, configurations, clusterData, services, hosts):
    """
    :type configurations dict
    :type clusterData dict
    :type services dict
    :type hosts dict
    """
    super(HDP26StackAdvisor, self).recommendZeppelinConfigurations(configurations, clusterData, services, hosts)
    self.__addZeppelinToLivy2SuperUsers(configurations, services)

  def recommendAtlasConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP26StackAdvisor, self).recommendAtlasConfigurations(configurations, clusterData, services, hosts)
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    putAtlasApplicationProperty = self.putProperty(configurations, "application-properties", services)

    knox_host = 'localhost'
    knox_port = '8443'
    if 'KNOX' in servicesList:
      knox_hosts = self.getComponentHostNames(services, "KNOX", "KNOX_GATEWAY")
      if len(knox_hosts) > 0:
        knox_hosts.sort()
        knox_host = knox_hosts[0]
      if 'gateway-site' in services['configurations'] and 'gateway.port' in services['configurations']["gateway-site"]["properties"]:
        knox_port = services['configurations']["gateway-site"]["properties"]['gateway.port']
      putAtlasApplicationProperty('atlas.sso.knox.providerurl', 'https://{0}:{1}/gateway/knoxsso/api/v1/websso'.format(knox_host, knox_port))

    knox_service_user = ''
    if 'KNOX' in servicesList and 'knox-env' in services['configurations']:
      knox_service_user = services['configurations']['knox-env']['properties']['knox_user']
    else:
      knox_service_user = 'knox'
    putAtlasApplicationProperty('atlas.proxyusers',knox_service_user)

  def recommendDruidConfigurations(self, configurations, clusterData, services, hosts):

      # druid is not in list of services to be installed
      if 'druid-common' not in services['configurations']:
        return

      componentsListList = [service["components"] for service in services["services"]]
      componentsList = [item["StackServiceComponents"] for sublist in componentsListList for item in sublist]
      servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
      putCommonProperty = self.putProperty(configurations, "druid-common", services)

      putCommonProperty('druid.zk.service.host', self.getZKHostPortString(services))
      self.recommendDruidMaxMemoryLimitConfigurations(configurations, clusterData, services, hosts)

      # recommending the metadata storage uri
      database_name = services['configurations']["druid-common"]["properties"]["database_name"]
      metastore_hostname = services['configurations']["druid-common"]["properties"]["metastore_hostname"]
      database_type = services['configurations']["druid-common"]["properties"]["druid.metadata.storage.type"]
      metadata_storage_port = "1527"
      mysql_module_name = "mysql-metadata-storage"
      postgres_module_name = "postgresql-metadata-storage"
      extensions_load_list = services['configurations']['druid-common']['properties']['druid.extensions.loadList']
      putDruidCommonProperty = self.putProperty(configurations, "druid-common", services)

      extensions_load_list = self.removeFromList(extensions_load_list, mysql_module_name)
      extensions_load_list = self.removeFromList(extensions_load_list, postgres_module_name)

      if database_type == 'mysql':
          metadata_storage_port = "3306"
          extensions_load_list = self.addToList(extensions_load_list, mysql_module_name)

      if database_type == 'postgresql':
          extensions_load_list = self.addToList(extensions_load_list, postgres_module_name)
          metadata_storage_port = "5432"

      putDruidCommonProperty('druid.metadata.storage.connector.port', metadata_storage_port)
      putDruidCommonProperty('druid.metadata.storage.connector.connectURI',
                             self.getMetadataConnectionString(database_type).format(metastore_hostname, database_name,
                                                                                    metadata_storage_port))
      # HDFS is installed
      if "HDFS" in servicesList and "hdfs-site" in services["configurations"]:
          # recommend HDFS as default deep storage
          extensions_load_list = self.addToList(extensions_load_list, "druid-hdfs-storage")
          putCommonProperty("druid.storage.type", "hdfs")
          putCommonProperty("druid.storage.storageDirectory", "/apps/druid/warehouse")
          # configure indexer logs configs
          putCommonProperty("druid.indexer.logs.type", "hdfs")
          putCommonProperty("druid.indexer.logs.directory", "/user/druid/logs")

      if "KAFKA" in servicesList:
          extensions_load_list = self.addToList(extensions_load_list, "druid-kafka-indexing-service")

      if 'AMBARI_METRICS' in servicesList:
        extensions_load_list = self.addToList(extensions_load_list, "ambari-metrics-emitter")

      putCommonProperty('druid.extensions.loadList', extensions_load_list)

      # JVM Configs go to env properties
      putEnvProperty = self.putProperty(configurations, "druid-env", services)

      # processing thread pool and memory configs
      for component in ['DRUID_HISTORICAL', 'DRUID_BROKER']:
          component_hosts = self.getHostsWithComponent("DRUID", component, services, hosts)
          nodeType = self.DRUID_COMPONENT_NODE_TYPE_MAP[component]
          putComponentProperty = self.putProperty(configurations, format("druid-{nodeType}"), services)
          if (component_hosts is not None and len(component_hosts) > 0):
              totalAvailableCpu = self.getMinCpu(component_hosts)
              processingThreads = 1
              if totalAvailableCpu > 1:
                  processingThreads = totalAvailableCpu - 1
              numMergeBuffers = max(2, processingThreads/4)
              putComponentProperty('druid.processing.numThreads', processingThreads)
              putComponentProperty('druid.server.http.numThreads', max(10, (totalAvailableCpu * 17) / 16 + 2) + 30)
              putComponentProperty('druid.processing.numMergeBuffers', numMergeBuffers)
              totalAvailableMemInMb = self.getMinMemory(component_hosts) / 1024
              maxAvailableBufferSizeInMb = totalAvailableMemInMb/(processingThreads + numMergeBuffers)
              putComponentProperty('druid.processing.buffer.sizeBytes', self.getDruidProcessingBufferSizeInMb(maxAvailableBufferSizeInMb) * 1024 * 1024)


  # returns the recommended druid processing buffer size in Mb.
  # the recommended buffer size is kept lower then the max available memory to have enough free memory to load druid data.
  # for low memory nodes, the actual allocated buffer size is small to keep some free memory for memory mapping of segments
  # If user installs all druid processes on a single node, memory available for loading segments will be further decreased.
  def getDruidProcessingBufferSizeInMb(self, maxAvailableBufferSizeInMb):
      if maxAvailableBufferSizeInMb <= 256:
          return min(64, maxAvailableBufferSizeInMb)
      elif maxAvailableBufferSizeInMb <= 1024:
          return 128
      elif maxAvailableBufferSizeInMb <= 2048:
          return 256
      elif maxAvailableBufferSizeInMb <= 6144:
          return 512
      # High Memory nodes below
      else :
          return 1024

  def recommendSupersetConfigurations(self, configurations, clusterData, services, hosts):
      # superset is in list of services to be installed
      if 'superset' in services['configurations']:
        # Recommendations for Superset
        superset_database_type = services['configurations']["superset"]["properties"]["SUPERSET_DATABASE_TYPE"]
        putSupersetProperty = self.putProperty(configurations, "superset", services)

        if superset_database_type == "mysql":
            putSupersetProperty("SUPERSET_DATABASE_PORT", "3306")
        elif superset_database_type == "postgresql":
            putSupersetProperty("SUPERSET_DATABASE_PORT", "5432")

  def recommendYARNConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP26StackAdvisor, self).recommendYARNConfigurations(configurations, clusterData, services, hosts)
    putYarnSiteProperty = self.putProperty(configurations, "yarn-site", services)
    putYarnEnvProperty = self.putProperty(configurations, "yarn-env", services)
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

    if 'HIVE' in servicesList and "yarn-site" in services["configurations"] and "yarn.nodemanager.kill-escape.user" in \
                services["configurations"]["yarn-site"]["properties"] and 'hive-env' in services['configurations'] and \
                'hive_user' in services['configurations']['hive-env']['properties']:
      hive_user_name = services['configurations']['hive-env']['properties']['hive_user']
      old_hive_user_name = self.getOldValue(services, "hive-env", "hive_user")
      yarn_nm_kill_escape_user = services["configurations"]["yarn-site"]["properties"]["yarn.nodemanager.kill-escape.user"]
      if not hive_user_name in yarn_nm_kill_escape_user:
        if not yarn_nm_kill_escape_user or yarn_nm_kill_escape_user.strip() == "":
          yarn_nm_kill_escape_user = hive_user_name
        else:
          escape_user_names = yarn_nm_kill_escape_user.split(",")
          if old_hive_user_name in escape_user_names:
            escape_user_names.remove(old_hive_user_name)
          escape_user_names.append(hive_user_name)
          yarn_nm_kill_escape_user = ",".join(escape_user_names)

        putYarnSiteProperty("yarn.nodemanager.kill-escape.user", yarn_nm_kill_escape_user)


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
                    "yarn.timeline-service.webapp.https.address" in services["configurations"]["yarn-site"]["properties"] and \
                    "yarn.http.policy" in services["configurations"]["yarn-site"]["properties"] and \
                    "yarn.log.server.web-service.url" in services["configurations"]["yarn-site"]["properties"]:
        if services["configurations"]["yarn-site"]["properties"]["yarn.http.policy"] == 'HTTP_ONLY':
            webapp_address = services["configurations"]["yarn-site"]["properties"]["yarn.timeline-service.webapp.address"]
            webservice_url = "http://"+webapp_address+"/ws/v1/applicationhistory"
        else:
            webapp_address = services["configurations"]["yarn-site"]["properties"]["yarn.timeline-service.webapp.https.address"]
            webservice_url = "https://"+webapp_address+"/ws/v1/applicationhistory"
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

  def getMetadataConnectionString(self, database_type):
      driverDict = {
          'mysql': 'jdbc:mysql://{0}:{2}/{1}?createDatabaseIfNotExist=true',
          'derby': 'jdbc:derby://{0}:{2}/{1};create=true',
          'postgresql': 'jdbc:postgresql://{0}:{2}/{1}'
      }
      return driverDict.get(database_type.lower())

  def addToList(self, json_list, word):
      desr_list = json.loads(json_list)
      if word not in desr_list:
          desr_list.append(word)
      return json.dumps(desr_list)

  def removeFromList(self, json_list, word):
      desr_list = json.loads(json_list)
      if word in desr_list:
          desr_list.remove(word)
      return json.dumps(desr_list)

  def recommendDruidMaxMemoryLimitConfigurations(self, configurations, clusterData, services, hosts):
      putEnvPropertyAttribute = self.putPropertyAttribute(configurations, "druid-env")
      for component in ["DRUID_HISTORICAL", "DRUID_MIDDLEMANAGER", "DRUID_BROKER", "DRUID_OVERLORD",
                        "DRUID_COORDINATOR"]:
          component_hosts = self.getHostsWithComponent("DRUID", component, services, hosts)
          if component_hosts is not None and len(component_hosts) > 0:
              totalAvailableMem = self.getMinMemory(component_hosts) / 1024  # In MB
              nodeType = self.DRUID_COMPONENT_NODE_TYPE_MAP[component]
              putEnvPropertyAttribute(format('druid.{nodeType}.jvm.heap.memory'), 'maximum',
                                      max(totalAvailableMem, 1024))

  DRUID_COMPONENT_NODE_TYPE_MAP = {
      'DRUID_BROKER': 'broker',
      'DRUID_COORDINATOR': 'coordinator',
      'DRUID_HISTORICAL': 'historical',
      'DRUID_MIDDLEMANAGER': 'middlemanager',
      'DRUID_OVERLORD': 'overlord',
      'DRUID_ROUTER': 'router'
  }

  def getMinMemory(self, component_hosts):
      min_ram_kb = 1073741824  # 1 TB
      for host in component_hosts:
          ram_kb = host['Hosts']['total_mem']
          min_ram_kb = min(min_ram_kb, ram_kb)
      return min_ram_kb

  def getMinCpu(self, component_hosts):
      min_cpu = 256
      for host in component_hosts:
          cpu_count = host['Hosts']['cpu_count']
          min_cpu = min(min_cpu, cpu_count)
      return min_cpu

  def getServiceConfigurationValidators(self):
      parentValidators = super(HDP26StackAdvisor, self).getServiceConfigurationValidators()
      childValidators = {
          "DRUID": {"druid-env": self.validateDruidEnvConfigurations,
                    "druid-historical": self.validateDruidHistoricalConfigurations,
                    "druid-broker": self.validateDruidBrokerConfigurations},
          "RANGER": {"ranger-ugsync-site": self.validateRangerUsersyncConfigurations},
          "YARN" : {"yarn-site": self.validateYarnSiteConfigurations}
      }
      self.mergeValidators(parentValidators, childValidators)
      return parentValidators

  def validateDruidEnvConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
      validationItems = []
      #  Minimum Direct memory Validation
      envProperties = services['configurations']['druid-env']['properties']
      for nodeType in ['broker', 'historical']:
          properties = services['configurations'][format('druid-{nodeType}')]['properties']
          intermediateBufferSize = int(properties['druid.processing.buffer.sizeBytes']) / (1024 * 1024)  # In MBs
          processingThreads = int(properties['druid.processing.numThreads'])
          directMemory = int(envProperties[format('druid.{nodeType}.jvm.direct.memory')])
          if directMemory < (processingThreads + 1) * intermediateBufferSize:
              validationItems.extend(
                  {"config-name": format("druid.{nodeType}.jvm.direct.memory"), "item": self.getErrorItem(
                      format(
                          "Not enough direct memory available for {nodeType} Node."
                          "Please adjust druid.{nodeType}.jvm.direct.memory, druid.processing.buffer.sizeBytes, druid.processing.numThreads"
                      )
                  )
                   })
      return self.toConfigurationValidationProblems(validationItems, "druid-env")

  def validateYarnSiteConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
      validationItems = []
      siteProperties = services["configurations"]["yarn-site"]["properties"]
      servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

      if 'HIVE' in servicesList and "yarn-site" in services["configurations"] and "yarn.nodemanager.kill-escape.user" in \
          services["configurations"]["yarn-site"]["properties"] and 'hive-env' in services['configurations'] and \
                  'hive_user' in services['configurations']['hive-env']['properties']:
        hive_user = services['configurations']['hive-env']['properties']['hive_user']
        yarn_nm_kill_escape_user = services["configurations"]["yarn-site"]["properties"]["yarn.nodemanager.kill-escape.user"]
        if not hive_user in yarn_nm_kill_escape_user:
          validationItems.append(
            {"config-name": "yarn.nodemanager.kill-escape.user",
             "item": self.getWarnItem("Value should contain %s" % hive_user)})

      if services["configurations"]["yarn-site"]["properties"]["yarn.http.policy"] == 'HTTP_ONLY':
         webapp_address = services["configurations"]["yarn-site"]["properties"]["yarn.timeline-service.webapp.address"]
         propertyValue = "http://"+webapp_address+"/ws/v1/applicationhistory"
      else:
         webapp_address = services["configurations"]["yarn-site"]["properties"]["yarn.timeline-service.webapp.https.address"]
         propertyValue = "https://"+webapp_address+"/ws/v1/applicationhistory"
      self.logger.info("validateYarnSiteConfigurations: recommended value for webservice url"+services["configurations"]["yarn-site"]["properties"]["yarn.log.server.web-service.url"])
      if services["configurations"]["yarn-site"]["properties"]["yarn.log.server.web-service.url"] != propertyValue:
         validationItems.append(
              {"config-name": "yarn.log.server.web-service.url",
               "item": self.getWarnItem("Value should be %s" % propertyValue)})
      return self.toConfigurationValidationProblems(validationItems, "yarn-site")

  def validateDruidHistoricalConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
      validationItems = [
          {"config-name": "druid.processing.numThreads",
           "item": self.validatorEqualsToRecommendedItem(properties, recommendedDefaults,
                                                         "druid.processing.numThreads")}
      ]
      return self.toConfigurationValidationProblems(validationItems, "druid-historical")

  def validateDruidBrokerConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
        validationItems = [
            {"config-name": "druid.processing.numThreads",
             "item": self.validatorEqualsToRecommendedItem(properties, recommendedDefaults,
                                                           "druid.processing.numThreads")}
        ]
        return self.toConfigurationValidationProblems(validationItems, "druid-broker")

  def recommendTezConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP26StackAdvisor, self).recommendTezConfigurations(configurations, clusterData, services, hosts)
    putTezProperty = self.putProperty(configurations, "tez-site")

    # TEZ JVM options
    jvmGCParams = "-XX:+UseParallelGC"
    if "ambari-server-properties" in services and "java.home" in services["ambari-server-properties"]:
      # JDK8 needs different parameters
      match = re.match(".*\/jdk(1\.\d+)[\-\_\.][^/]*$", services["ambari-server-properties"]["java.home"])
      if match and len(match.groups()) > 0:
        # Is version >= 1.8
        versionSplits = re.split("\.", match.group(1))
        if versionSplits and len(versionSplits) > 1 and int(versionSplits[0]) > 0 and int(versionSplits[1]) > 7:
          jvmGCParams = "-XX:+UseG1GC -XX:+ResizeTLAB"
    tez_jvm_opts = "-XX:+PrintGCDetails -verbose:gc -XX:+PrintGCTimeStamps -XX:+UseNUMA "
    # Append 'jvmGCParams' and 'Heap Dump related option' (({{heap_dump_opts}}) Expanded while writing the
    # configurations at start/restart time).
    tez_jvm_updated_opts = tez_jvm_opts + jvmGCParams + "{{heap_dump_opts}}"
    putTezProperty('tez.am.launch.cmd-opts', tez_jvm_updated_opts)
    putTezProperty('tez.task.launch.cmd-opts', tez_jvm_updated_opts)
    self.logger.info("Updated 'tez-site' config 'tez.task.launch.cmd-opts' and 'tez.am.launch.cmd-opts' as "
                ": {0}".format(tez_jvm_updated_opts))

  def recommendRangerConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP26StackAdvisor, self).recommendRangerConfigurations(configurations, clusterData, services, hosts)

    putRangerUgsyncSite = self.putProperty(configurations, 'ranger-ugsync-site', services)

    delta_sync_enabled = False
    if 'ranger-ugsync-site' in services['configurations'] and 'ranger.usersync.ldap.deltasync' in services['configurations']['ranger-ugsync-site']['properties']:
      delta_sync_enabled = services['configurations']['ranger-ugsync-site']['properties']['ranger.usersync.ldap.deltasync'] == "true"

    if delta_sync_enabled:
      putRangerUgsyncSite("ranger.usersync.group.searchenabled", "true")
    else:
      putRangerUgsyncSite("ranger.usersync.group.searchenabled", "false")

  def validateRangerUsersyncConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    ranger_usersync_properties = properties
    validationItems = []

    delta_sync_enabled = 'ranger.usersync.ldap.deltasync' in ranger_usersync_properties \
      and ranger_usersync_properties['ranger.usersync.ldap.deltasync'].lower() == 'true'
    group_sync_enabled = 'ranger.usersync.group.searchenabled' in ranger_usersync_properties \
      and ranger_usersync_properties['ranger.usersync.group.searchenabled'].lower() == 'true'
    usersync_source_ldap_enabled = 'ranger.usersync.source.impl.class' in ranger_usersync_properties \
      and ranger_usersync_properties['ranger.usersync.source.impl.class'] == 'org.apache.ranger.ldapusersync.process.LdapUserGroupBuilder'

    if usersync_source_ldap_enabled and delta_sync_enabled and not group_sync_enabled:
      validationItems.append({"config-name": "ranger.usersync.group.searchenabled",
                            "item": self.getWarnItem(
                            "Need to set ranger.usersync.group.searchenabled as true, as ranger.usersync.ldap.deltasync is enabled")})

    return self.toConfigurationValidationProblems(validationItems, "ranger-ugsync-site")

  def recommendRangerKMSConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP26StackAdvisor, self).recommendRangerKMSConfigurations(configurations, clusterData, services, hosts)
    putRangerKmsEnvProperty = self.putProperty(configurations, "kms-env", services)

    ranger_kms_ssl_enabled = False
    ranger_kms_ssl_port = "9393"
    if 'ranger-kms-site' in services['configurations'] and 'ranger.service.https.attrib.ssl.enabled' in services['configurations']['ranger-kms-site']['properties']:
      ranger_kms_ssl_enabled = services['configurations']['ranger-kms-site']['properties']['ranger.service.https.attrib.ssl.enabled'].lower() == "true"

    if 'ranger-kms-site' in services['configurations'] and 'ranger.service.https.port' in services['configurations']['ranger-kms-site']['properties']:
      ranger_kms_ssl_port = services['configurations']['ranger-kms-site']['properties']['ranger.service.https.port']

    if ranger_kms_ssl_enabled:
      putRangerKmsEnvProperty("kms_port", ranger_kms_ssl_port)
    else:
      putRangerKmsEnvProperty("kms_port", "9292")

  def recommendHDFSConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP26StackAdvisor, self).recommendHDFSConfigurations(configurations, clusterData, services, hosts)
    if 'hadoop-env' in services['configurations'] and 'hdfs_user' in  services['configurations']['hadoop-env']['properties']:
      hdfs_user = services['configurations']['hadoop-env']['properties']['hdfs_user']
    else:
      hdfs_user = 'hadoop'

    if 'ranger-hdfs-plugin-properties' in configurations and 'ranger-hdfs-plugin-enabled' in configurations['ranger-hdfs-plugin-properties']['properties']:
      ranger_hdfs_plugin_enabled = (configurations['ranger-hdfs-plugin-properties']['properties']['ranger-hdfs-plugin-enabled'].lower() == 'Yes'.lower())
    elif 'ranger-hdfs-plugin-properties' in services['configurations'] and 'ranger-hdfs-plugin-enabled' in services['configurations']['ranger-hdfs-plugin-properties']['properties']:
      ranger_hdfs_plugin_enabled = (services['configurations']['ranger-hdfs-plugin-properties']['properties']['ranger-hdfs-plugin-enabled'].lower() == 'Yes'.lower())
    else:
      ranger_hdfs_plugin_enabled = False

    if ranger_hdfs_plugin_enabled and 'ranger-hdfs-plugin-properties' in services['configurations'] and 'REPOSITORY_CONFIG_USERNAME' in services['configurations']['ranger-hdfs-plugin-properties']['properties']:
      self.logger.info("Setting HDFS Repo user for Ranger.")
      putRangerHDFSPluginProperty = self.putProperty(configurations, "ranger-hdfs-plugin-properties", services)
      putRangerHDFSPluginProperty("REPOSITORY_CONFIG_USERNAME",hdfs_user)
    else:
      self.logger.info("Not setting HDFS Repo user for Ranger.")

  def recommendHIVEConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP26StackAdvisor, self).recommendHIVEConfigurations(configurations, clusterData, services, hosts)
    putHiveAtlasHookProperty = self.putProperty(configurations, "hive-atlas-application.properties", services)
    putHiveAtlasHookPropertyAttribute = self.putPropertyAttribute(configurations,"hive-atlas-application.properties")

    if 'hive-env' in services['configurations'] and 'hive_user' in services['configurations']['hive-env']['properties']:
      hive_user = services['configurations']['hive-env']['properties']['hive_user']
    else:
      hive_user = 'hive'

    if 'hive-env' in configurations and 'hive_security_authorization' in configurations['hive-env']['properties']:
      ranger_hive_plugin_enabled = (configurations['hive-env']['properties']['hive_security_authorization'].lower() == 'ranger')
    elif 'hive-env' in services['configurations'] and 'hive_security_authorization' in services['configurations']['hive-env']['properties']:
      ranger_hive_plugin_enabled = (services['configurations']['hive-env']['properties']['hive_security_authorization'].lower() == 'ranger')
    else :
      ranger_hive_plugin_enabled = False

    if ranger_hive_plugin_enabled and 'ranger-hive-plugin-properties' in services['configurations'] and 'REPOSITORY_CONFIG_USERNAME' in services['configurations']['ranger-hive-plugin-properties']['properties']:
      self.logger.info("Setting Hive Repo user for Ranger.")
      putRangerHivePluginProperty = self.putProperty(configurations, "ranger-hive-plugin-properties", services)
      putRangerHivePluginProperty("REPOSITORY_CONFIG_USERNAME",hive_user)
    else:
      self.logger.info("Not setting Hive Repo user for Ranger.")

    security_enabled = self.isSecurityEnabled(services)
    enable_atlas_hook = False

    if 'hive-env' in configurations and 'hive.atlas.hook' in configurations['hive-env']['properties']:
      enable_atlas_hook = configurations['hive-env']['properties']['hive.atlas.hook'].lower() == 'true'
    elif 'hive-env' in services['configurations'] and 'hive.atlas.hook' in services['configurations']['hive-env']['properties']:
      enable_atlas_hook = services['configurations']['hive-env']['properties']['hive.atlas.hook'].lower() == 'true'

    if 'hive-atlas-application.properties' in services['configurations']:
      if security_enabled and enable_atlas_hook:
        putHiveAtlasHookProperty('atlas.jaas.ticketBased-KafkaClient.loginModuleControlFlag', 'required')
        putHiveAtlasHookProperty('atlas.jaas.ticketBased-KafkaClient.loginModuleName', 'com.sun.security.auth.module.Krb5LoginModule')
        putHiveAtlasHookProperty('atlas.jaas.ticketBased-KafkaClient.option.useTicketCache', 'true')
      else:
        putHiveAtlasHookPropertyAttribute('atlas.jaas.ticketBased-KafkaClient.loginModuleControlFlag', 'delete', 'true')
        putHiveAtlasHookPropertyAttribute('atlas.jaas.ticketBased-KafkaClient.loginModuleName', 'delete', 'true')
        putHiveAtlasHookPropertyAttribute('atlas.jaas.ticketBased-KafkaClient.option.useTicketCache', 'delete', 'true')

    # druid is not in list of services to be installed
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if 'DRUID' in servicesList:
        putHiveInteractiveSiteProperty = self.putProperty(configurations, "hive-interactive-site", services)
        if 'druid-coordinator' in services['configurations']:
            component_hosts = self.getHostsWithComponent("DRUID", 'DRUID_COORDINATOR', services, hosts)
            if component_hosts is not None and len(component_hosts) > 0:
                # pick the first
                host = component_hosts[0]
            druid_coordinator_host_port = str(host['Hosts']['host_name']) + ":" + str(
                services['configurations']['druid-coordinator']['properties']['druid.port'])
        else:
            druid_coordinator_host_port = "localhost:8081"

        if 'druid-router' in services['configurations']:
            component_hosts = self.getHostsWithComponent("DRUID", 'DRUID_ROUTER', services, hosts)
            if component_hosts is not None and len(component_hosts) > 0:
                # pick the first
                host = component_hosts[0]
            druid_broker_host_port = str(host['Hosts']['host_name']) + ":" + str(
                services['configurations']['druid-router']['properties']['druid.port'])
        elif 'druid-broker' in services['configurations']:
            component_hosts = self.getHostsWithComponent("DRUID", 'DRUID_BROKER', services, hosts)
            if component_hosts is not None and len(component_hosts) > 0:
                # pick the first
                host = component_hosts[0]
            druid_broker_host_port = str(host['Hosts']['host_name']) + ":" + str(
                services['configurations']['druid-broker']['properties']['druid.port'])
        else:
            druid_broker_host_port = "localhost:8083"

        druid_metadata_uri = ""
        druid_metadata_user = ""
        druid_metadata_type = ""
        if 'druid-common' in services['configurations']:
            druid_metadata_uri = services['configurations']['druid-common']['properties']['druid.metadata.storage.connector.connectURI']
            druid_metadata_type = services['configurations']['druid-common']['properties']['druid.metadata.storage.type']
            if 'druid.metadata.storage.connector.user' in services['configurations']['druid-common']['properties']:
                druid_metadata_user = services['configurations']['druid-common']['properties']['druid.metadata.storage.connector.user']
            else:
                druid_metadata_user = ""

        putHiveInteractiveSiteProperty('hive.druid.broker.address.default', druid_broker_host_port)
        putHiveInteractiveSiteProperty('hive.druid.coordinator.address.default', druid_coordinator_host_port)
        putHiveInteractiveSiteProperty('hive.druid.metadata.uri', druid_metadata_uri)
        putHiveInteractiveSiteProperty('hive.druid.metadata.username', druid_metadata_user)
        putHiveInteractiveSiteProperty('hive.druid.metadata.db.type', druid_metadata_type)


  def recommendHBASEConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP26StackAdvisor, self).recommendHBASEConfigurations(configurations, clusterData, services, hosts)
    if 'hbase-env' in services['configurations'] and 'hbase_user' in services['configurations']['hbase-env']['properties']:
      hbase_user = services['configurations']['hbase-env']['properties']['hbase_user']
    else:
      hbase_user = 'hbase'

    if 'ranger-hbase-plugin-properties' in configurations and 'ranger-hbase-plugin-enabled' in configurations['ranger-hbase-plugin-properties']['properties']:
      ranger_hbase_plugin_enabled = (configurations['ranger-hbase-plugin-properties']['properties']['ranger-hbase-plugin-enabled'].lower() == 'Yes'.lower())
    elif 'ranger-hbase-plugin-properties' in services['configurations'] and 'ranger-hbase-plugin-enabled' in services['configurations']['ranger-hbase-plugin-properties']['properties']:
      ranger_hbase_plugin_enabled = (services['configurations']['ranger-hbase-plugin-properties']['properties']['ranger-hbase-plugin-enabled'].lower() == 'Yes'.lower())
    else:
      ranger_hbase_plugin_enabled = False

    if ranger_hbase_plugin_enabled and 'ranger-hbase-plugin-properties' in services['configurations'] and 'REPOSITORY_CONFIG_USERNAME' in services['configurations']['ranger-hbase-plugin-properties']['properties']:
      self.logger.info("Setting Hbase Repo user for Ranger.")
      putRangerHbasePluginProperty = self.putProperty(configurations, "ranger-hbase-plugin-properties", services)
      putRangerHbasePluginProperty("REPOSITORY_CONFIG_USERNAME",hbase_user)
    else:
      self.logger.info("Not setting Hbase Repo user for Ranger.")

  def recommendKAFKAConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP26StackAdvisor, self).recommendKAFKAConfigurations(configurations, clusterData, services, hosts)
    if 'kafka-env' in services['configurations'] and 'kafka_user' in services['configurations']['kafka-env']['properties']:
      kafka_user = services['configurations']['kafka-env']['properties']['kafka_user']
    else:
      kafka_user = "kafka"

    if 'ranger-kafka-plugin-properties' in configurations and  'ranger-kafka-plugin-enabled' in configurations['ranger-kafka-plugin-properties']['properties']:
      ranger_kafka_plugin_enabled = (configurations['ranger-kafka-plugin-properties']['properties']['ranger-kafka-plugin-enabled'].lower() == 'Yes'.lower())
    elif 'ranger-kafka-plugin-properties' in services['configurations'] and 'ranger-kafka-plugin-enabled' in services['configurations']['ranger-kafka-plugin-properties']['properties']:
      ranger_kafka_plugin_enabled = (services['configurations']['ranger-kafka-plugin-properties']['properties']['ranger-kafka-plugin-enabled'].lower() == 'Yes'.lower())
    else:
      ranger_kafka_plugin_enabled = False

    if ranger_kafka_plugin_enabled and 'ranger-kafka-plugin-properties' in services['configurations'] and 'REPOSITORY_CONFIG_USERNAME' in services['configurations']['ranger-kafka-plugin-properties']['properties']:
      self.logger.info("Setting Kafka Repo user for Ranger.")
      putRangerKafkaPluginProperty = self.putProperty(configurations, "ranger-kafka-plugin-properties", services)
      putRangerKafkaPluginProperty("REPOSITORY_CONFIG_USERNAME",kafka_user)
    else:
      self.logger.info("Not setting Kafka Repo user for Ranger.")

  def __addZeppelinToLivy2SuperUsers(self, configurations, services):
    """
    If Kerberos is enabled AND Zeppelin is installed AND Spark2 Livy Server is installed, then set
    livy2-conf/livy.superusers to contain the Zeppelin principal name from
    zeppelin-env/zeppelin.server.kerberos.principal

    :param configurations:
    :param services:
    """
    if self.isSecurityEnabled(services):
      zeppelin_env = self.getServicesSiteProperties(services, "zeppelin-env")

      if zeppelin_env and 'zeppelin.server.kerberos.principal' in zeppelin_env:
        zeppelin_principal = zeppelin_env['zeppelin.server.kerberos.principal']
        zeppelin_user = zeppelin_principal.split('@')[0] if zeppelin_principal else None

        if zeppelin_user:
          livy2_conf = self.getServicesSiteProperties(services, 'livy2-conf')

          if livy2_conf:
            superusers = livy2_conf['livy.superusers'] if livy2_conf and 'livy.superusers' in livy2_conf else None

            # add the Zeppelin user to the set of users
            if superusers:
              _superusers = superusers.split(',')
              _superusers = [x.strip() for x in _superusers]
              _superusers = filter(None, _superusers)  # Removes empty string elements from array
            else:
              _superusers = []

            if zeppelin_user not in _superusers:
              _superusers.append(zeppelin_user)

              putLivy2ConfProperty = self.putProperty(configurations, 'livy2-conf', services)
              putLivy2ConfProperty('livy.superusers', ','.join(_superusers))