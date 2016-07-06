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

class BigInsights42StackAdvisor(BigInsights41StackAdvisor):

  def getServiceConfigurationRecommenderDict(self):
    parentRecommendConfDict = super(BigInsights42StackAdvisor, self).getServiceConfigurationRecommenderDict()
    childRecommendConfDict = {
      "YARN": self.recommendYARNConfigurations,
      "KAFKA": self.recommendKAFKAConfigurations,
      "SOLR": self.recommendSolrConfigurations
    }
    parentRecommendConfDict.update(childRecommendConfDict)
    return parentRecommendConfDict

  def recommendYARNConfigurations(self, configurations, clusterData, services, hosts):
    super(BigInsights42StackAdvisor, self).recommendYARNConfigurations(configurations, clusterData, services, hosts)
    putYarnProperty = self.putProperty(configurations, "yarn-site", services)

    # Property Attributes
    putYarnPropertyAttribute = self.putPropertyAttribute(configurations, "yarn-site")
    nodeManagerHost = self.getHostWithComponent("YARN", "NODEMANAGER", services, hosts)
    if (nodeManagerHost is not None):
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
          putYarnProperty('yarn.nodemanager.container-executor.class', 'org.apache.hadoop.yarn.server.nodemanager.DefaultContainerExecutor')
          putYarnPropertyAttribute('yarn.nodemanager.linux-container-executor.resources-handler.class', 'delete', 'true')
          putYarnPropertyAttribute('yarn.nodemanager.linux-container-executor.cgroups.hierarchy', 'delete', 'true')
          putYarnPropertyAttribute('yarn.nodemanager.linux-container-executor.cgroups.mount', 'delete', 'true')
          putYarnPropertyAttribute('yarn.nodemanager.linux-container-executor.cgroups.mount-path', 'delete', 'true')

        if ("yarn-site" in services["configurations"]):
          if ("yarn.nodemanager.resource.memory-mb" in services["configurations"]["yarn-site"]["properties"]):
            # Compare the values derived from the hosts vs the yarn-site.xml static values, if the derived values are greater than the static ,
            # override the static values with the derived to prevent the warnings that will appear in ambari
            yarnPropertiesNmMemMb = int(configurations["yarn-site"]["properties"]["yarn.nodemanager.resource.memory-mb"])
            yarnConfigurationsNmMemMb = int(services["configurations"]["yarn-site"]["properties"]["yarn.nodemanager.resource.memory-mb"])
            if yarnPropertiesNmMemMb > yarnConfigurationsNmMemMb:
              putYarnPropertyAttribute('yarn.scheduler.maximum-allocation-mb', 'maximum', configurations["yarn-site"]["properties"]["yarn.nodemanager.resource.memory-mb"])
              putYarnPropertyAttribute('yarn.scheduler.minimum-allocation-mb', 'maximum', configurations["yarn-site"]["properties"]["yarn.nodemanager.resource.memory-mb"])

  def recommendKAFKAConfigurations(self, configurations, clusterData, services, hosts):
    super(BigInsights42StackAdvisor, self).recommendKAFKAConfigurations(configurations, clusterData, services, hosts)

    putKafkaBrokerAttributes = self.putPropertyAttribute(configurations, "kafka-broker")
    putKafkaBrokerAttributes('port','delete','true')

  def recommendSolrConfigurations(self, configurations, clusterData, services, hosts):
    putSolrEnvProperty = self.putProperty(configurations, "solr-env", services)
    putSolrAttributes = self.putPropertyAttribute(configurations, "solr-env")
    putSolrAttributes('solr_lib_dir','delete','true')
