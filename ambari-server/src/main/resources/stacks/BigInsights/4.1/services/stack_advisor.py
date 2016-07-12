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

from math import ceil, sqrt


class BigInsights41StackAdvisor(BigInsights40StackAdvisor):

  def getServiceConfigurationRecommenderDict(self):
    parentRecommendConfDict = super(BigInsights41StackAdvisor, self).getServiceConfigurationRecommenderDict()
    childRecommendConfDict = {
      "MAPREDUCE2": self.recommendMapReduce2Configurations
    }
    parentRecommendConfDict.update(childRecommendConfDict)
    return parentRecommendConfDict

  def recommendMapReduce2Configurations(self, configurations, clusterData, services, hosts):
    super(BigInsights41StackAdvisor, self).recommendMapReduce2Configurations(configurations, clusterData, services, hosts)

    putMapredProperty = self.putProperty(configurations, "mapred-site", services)
    putMapredProperty('yarn.app.mapreduce.am.command-opts', "-Xmx" + str(int(0.8 * int(configurations["mapred-site"]["properties"]["yarn.app.mapreduce.am.resource.mb"]))) + "m" + " -Diop.version=${iop.version}")
    putMapredProperty('mapreduce.client.submit.file.replication', int(clusterData['mrFileReplication']))

  def getConfigurationClusterSummary(self, servicesList, hosts, components, services):
    cluster = super(BigInsights41StackAdvisor, self).getConfigurationClusterSummary(servicesList, hosts, components, services)

    # Replication factor for M/R submission files. Recommendation is sqrt of
    # Datanode number. However, don't set it to less than 3 unless it's a 1 or 2
    # node cluster.
    num_datanodes = len(self.getHostsWithComponent('HDFS', 'DATANODE', services, hosts))
    if (num_datanodes < 3):
      cluster['mrFileReplication'] = num_datanodes
    else:
      cluster['mrFileReplication'] = ceil(max(sqrt(num_datanodes), 3))

    return cluster

  def getHostsWithComponent(self, serviceName, componentName, services, hosts):
    if services is not None and hosts is not None and serviceName in [service["StackServices"]["service_name"] for service in services["services"]]:
      service = [serviceEntry for serviceEntry in services["services"] if serviceEntry["StackServices"]["service_name"] == serviceName][0]
      components = [componentEntry for componentEntry in service["components"] if componentEntry["StackServiceComponents"]["component_name"] == componentName]
      if (len(components) > 0 and len(components[0]["StackServiceComponents"]["hostnames"]) > 0):
        componentHostnames = components[0]["StackServiceComponents"]["hostnames"]
        componentHosts = [host for host in hosts["items"] if host["Hosts"]["host_name"] in componentHostnames]
        return componentHosts
    return []

