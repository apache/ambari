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
      "SOLR": self.recommendSolrConfigurations,
      "HIVE": self.recommendHIVEConfigurations,
      "AMBARI_METRICS": self.recommendAmsConfigurations
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
    security_enabled = self.isSecurityEnabled(services)
    putKafkaBrokerProperty = self.putProperty(configurations, "kafka-broker", services)
    if (security_enabled):
      putKafkaBrokerProperty("security.inter.broker.protocol", "SASL_PLAINTEXT")

  def recommendSolrConfigurations(self, configurations, clusterData, services, hosts):
    putSolrEnvProperty = self.putProperty(configurations, "solr-env", services)
    putSolrAttributes = self.putPropertyAttribute(configurations, "solr-env")
    putSolrAttributes('solr_lib_dir','delete','true')

  def recommendHIVEConfigurations(self, configurations, clusterData, services, hosts):
    super(BigInsights42StackAdvisor, self).recommendHIVEConfigurations(configurations, clusterData, services, hosts)
    putHiveSiteProperty = self.putProperty(configurations, "hive-site", services)
    putHiveSiteProperty("hive.exec.reducers.bytes.per.reducer", "256000000")

  def recommendAmsConfigurations(self, configurations, clusterData, services, hosts):
    super(BigInsights42StackAdvisor, self).recommendAmsConfigurations(configurations, clusterData, services, hosts)

    putAmsEnvProperty = self.putProperty(configurations, "ams-env", services)
    putHbaseEnvProperty = self.putProperty(configurations, "ams-hbase-env", services)

    operatingMode = "embedded"
    if "ams-site" in services["configurations"]:
      if "timeline.metrics.service.operation.mode" in services["configurations"]["ams-site"]["properties"]:
        operatingMode = services["configurations"]["ams-site"]["properties"]["timeline.metrics.service.operation.mode"]

    hostsCount = 0
    if hosts and "items" in hosts:
      hostsCount = len(hosts["items"])

    collector_heapsize, hbase_heapsize, total_sinks_count = self.getAmsMemoryRecommendation(services, hosts)
    collector_heapsize = max(collector_heapsize, 2048)
    putAmsEnvProperty("metrics_collector_heapsize", collector_heapsize)

    # Distributed mode heap size
    if operatingMode == "distributed":
      hbase_heapsize = max(hbase_heapsize, 2048)
      putHbaseEnvProperty("hbase_master_heapsize", "2048")
      putHbaseEnvProperty("hbase_master_xmn_size", "409") #20% of 2048 heap size
      putHbaseEnvProperty("hbase_regionserver_heapsize", hbase_heapsize)
      putHbaseEnvProperty("regionserver_xmn_size", round_to_n(0.15*hbase_heapsize,64))
    else:
      # Embedded mode heap size : master + regionserver
      hbase_rs_heapsize = 768
      if hostsCount >= 6:
        hbase_heapsize = max(hbase_heapsize, 2048)
      putHbaseEnvProperty("hbase_regionserver_heapsize", hbase_rs_heapsize)
      putHbaseEnvProperty("hbase_master_heapsize", hbase_heapsize)
      putHbaseEnvProperty("hbase_master_xmn_size", round_to_n(0.15*(hbase_heapsize+hbase_rs_heapsize),64))
