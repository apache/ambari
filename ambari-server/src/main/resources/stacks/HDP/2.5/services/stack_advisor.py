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


class HDP25StackAdvisor(HDP24StackAdvisor):
  def createComponentLayoutRecommendations(self, services, hosts):
    parentComponentLayoutRecommendations = super(HDP25StackAdvisor, self).createComponentLayoutRecommendations(
      services, hosts)
    return parentComponentLayoutRecommendations

  def getComponentLayoutValidations(self, services, hosts):
    parentItems = super(HDP25StackAdvisor, self).getComponentLayoutValidations(services, hosts)
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item["StackServiceComponents"] for sublist in componentsListList for item in sublist]
    childItems = []

    if "HIVE" in servicesList:
      hsi_hosts = self.__getHosts(componentsList, "HIVE_SERVER_INTERACTIVE")
      if len(hsi_hosts) > 1:
        message = "Only one host can install HIVE_SERVER_INTERACTIVE. "
        childItems.append(
          {"type": 'host-component', "level": 'ERROR', "message": message, "component-name": 'HIVE_SERVER_INTERACTIVE'})
        print '3329'

    parentItems.extend(childItems)
    return parentItems

  def getServiceConfigurationValidators(self):
    parentValidators = super(HDP25StackAdvisor, self).getServiceConfigurationValidators()
    childValidators = {
      "HIVE": {"hive-interactive-env": self.validateHiveInteractiveEnvConfigurations}
    }
    self.mergeValidators(parentValidators, childValidators)
    return parentValidators

  def validateHiveInteractiveEnvConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    hive_site_env_properties = getSiteProperties(configurations, "hive-interactive-env")
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item["StackServiceComponents"] for sublist in componentsListList for item in sublist]
    validationItems = []

    if "HIVE" in servicesList:
      hsi_hosts = self.__getHosts(componentsList, "HIVE_SERVER_INTERACTIVE")
      if len(hsi_hosts) > 0:
        # HIVE_SERVER_INTERACTIVE is mapped to a host
        if 'enable_hive_interactive' not in hive_site_env_properties or (
                'enable_hive_interactive' in hive_site_env_properties and hive_site_env_properties[
              'enable_hive_interactive'].lower() != 'true'):
          validationItems.append({"config-name": "enable_hive_interactive",
                                  "item": self.getWarnItem(
                                    "HIVE_SERVER_INTERACTIVE requires enable_hive_interactive in hive-interactive-env set to true.")})
        if 'hive_server_interactive_host' in hive_site_env_properties:
          hsi_host = hsi_hosts[0]
          if hive_site_env_properties['hive_server_interactive_host'].lower() != hsi_host.lower():
            validationItems.append({"config-name": "hive_server_interactive_host",
                                    "item": self.getWarnItem(
                                      "HIVE_SERVER_INTERACTIVE requires hive_server_interactive_host in hive-interactive-env set to its host name.")})
          pass
        if 'hive_server_interactive_host' not in hive_site_env_properties:
          validationItems.append({"config-name": "hive_server_interactive_host",
                                  "item": self.getWarnItem(
                                    "HIVE_SERVER_INTERACTIVE requires hive_server_interactive_host in hive-interactive-env set to its host name.")})
          pass

    else:
      # no  HIVE_SERVER_INTERACTIVE
      if 'enable_hive_interactive' in hive_site_env_properties and hive_site_env_properties[
        'enable_hive_interactive'].lower() != 'false':
        validationItems.append({"config-name": "enable_hive_interactive",
                                "item": self.getWarnItem(
                                  "enable_hive_interactive in hive-interactive-env should be set to false.")})
        pass
      pass

    validationProblems = self.toConfigurationValidationProblems(validationItems, "hdfs-site")
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
    super(HDP23StackAdvisor, self).recommendHIVEConfigurations(configurations, clusterData, services, hosts)
    putHiveInteractiveEnvProperty = self.putProperty(configurations, "hive-interactive-env", services)
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item["StackServiceComponents"] for sublist in componentsListList for item in sublist]

    if "HIVE" in servicesList:
      hsi_hosts = self.__getHosts(componentsList, "HIVE_SERVER_INTERACTIVE")
      if len(hsi_hosts) > 0:
        hsi_host = hsi_hosts[0]
        putHiveInteractiveEnvProperty('enable_hive_interactive', 'true')
        putHiveInteractiveEnvProperty('hive_server_interactive_host', hsi_host)
      else:
        putHiveInteractiveEnvProperty('enable_hive_interactive', 'false')
      pass
    pass


  def recommendRangerConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP25StackAdvisor, self).recommendRangerConfigurations(configurations, clusterData, services, hosts)
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

    putTagsyncAppProperty = self.putProperty(configurations, "tagsync-application-properties", services)

    zookeeper_host_port = self.getZKHostPortString(services)
    if zookeeper_host_port:
      zookeeper_host_list = zookeeper_host_port.split(',')
      putTagsyncAppProperty('atlas.kafka.zookeeper.connect', zookeeper_host_list[0])
    else:
      putTagsyncAppProperty('atlas.kafka.zookeeper.connect', 'localhost:6667')

    if 'KAFKA' in servicesList:
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


  def __getHosts(self, componentsList, componentName):
    return [component["hostnames"] for component in componentsList if component["component_name"] == componentName][0]

