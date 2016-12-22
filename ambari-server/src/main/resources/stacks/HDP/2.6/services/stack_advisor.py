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
import json
import re
from resource_management.libraries.functions import format


class HDP26StackAdvisor(HDP25StackAdvisor):
  def __init__(self):
      super(HDP26StackAdvisor, self).__init__()
      Logger.initialize_logger()

  def getServiceConfigurationRecommenderDict(self):
      parentRecommendConfDict = super(HDP26StackAdvisor, self).getServiceConfigurationRecommenderDict()
      childRecommendConfDict = {
          "DRUID": self.recommendDruidConfigurations,
          "ATLAS": self.recommendAtlasConfigurations,
          "TEZ": self.recommendTezConfigurations
      }
      parentRecommendConfDict.update(childRecommendConfDict)
      return parentRecommendConfDict

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
          putCommonProperty("druid.storage.storageDirectory", "/user/druid/data")
          # configure indexer logs configs
          putCommonProperty("druid.indexer.logs.type", "hdfs")
          putCommonProperty("druid.indexer.logs.directory", "/user/druid/logs")

      if "KAFKA" in servicesList:
          extensions_load_list = self.addToList(extensions_load_list, "druid-kafka-indexing-service")

      putCommonProperty('druid.extensions.loadList', extensions_load_list)

      # JVM Configs go to env properties
      putEnvProperty = self.putProperty(configurations, "druid-env", services)

      # processing thread pool Config
      for component in ['DRUID_HISTORICAL', 'DRUID_BROKER']:
          component_hosts = self.getHostsWithComponent("DRUID", component, services, hosts)
          nodeType = self.DRUID_COMPONENT_NODE_TYPE_MAP[component]
          putComponentProperty = self.putProperty(configurations, format("druid-{nodeType}"), services)
          if (component_hosts is not None and len(component_hosts) > 0):
              totalAvailableCpu = self.getMinCpu(component_hosts)
              processingThreads = 1
              if totalAvailableCpu > 1:
                  processingThreads = totalAvailableCpu - 1
              putComponentProperty('druid.processing.numThreads', processingThreads)
              putComponentProperty('druid.server.http.numThreads', max(10, (totalAvailableCpu * 17) / 16 + 2) + 30)

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
                    "druid-broker": self.validateDruidBrokerConfigurations}
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
    putTezProperty('tez.task.launch.cmd-opts', tez_jvm_updated_opts)
    Logger.info("Updated 'tez-site' config 'tez.task.launch.cmd-opts' as : {0}".format(tez_jvm_updated_opts))
