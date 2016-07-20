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
import os
import imp
import traceback

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
STACKS_DIR = os.path.join(SCRIPT_DIR, '../../../stacks/')
PARENT_FILE = os.path.join(STACKS_DIR, 'service_advisor.py')

try:
  with open(PARENT_FILE, 'rb') as fp:
    service_advisor = imp.load_module('service_advisor', fp, PARENT_FILE, ('.py', 'rb', imp.PY_SOURCE))
except Exception as e:
  traceback.print_exc()
  print "Failed to load parent"

class PXF300ServiceAdvisor(service_advisor.ServiceAdvisor):

  def colocateService(self, hostsComponentsMap, serviceComponents):
    # colocate PXF with NAMENODE and DATANODE, if no hosts have been allocated for PXF
    pxf = [component for component in serviceComponents if component["StackServiceComponents"]["component_name"] == "PXF"][0]
    if not self.isComponentHostsPopulated(pxf):
      for hostName in hostsComponentsMap.keys():
        hostComponents = hostsComponentsMap[hostName]
        if ({"name": "NAMENODE"} in hostComponents or {"name": "DATANODE"} in hostComponents) \
            and {"name": "PXF"} not in hostComponents:
          hostsComponentsMap[hostName].append({ "name": "PXF" })
        if ({"name": "NAMENODE"} not in hostComponents and {"name": "DATANODE"} not in hostComponents) \
            and {"name": "PXF"} in hostComponents:
          hostsComponentsMap[hostName].remove({"name": "PXF"})

  def getServiceComponentLayoutValidations(self, services, hosts):
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item["StackServiceComponents"] for sublist in componentsListList for item in sublist]

    pxfHosts = self.getHosts(componentsList, "PXF")
    expectedPxfHosts = set(self.getHosts(componentsList, "NAMENODE") + self.getHosts(componentsList, "DATANODE"))
    items = []

    # Generate WARNING if any PXF is not colocated with NAMENODE or DATANODE
    mismatchHosts = sorted(expectedPxfHosts.symmetric_difference(set(pxfHosts)))
    if len(mismatchHosts) > 0:
      hostsString = ', '.join(mismatchHosts)
      message = "PXF must be installed on the NameNode, Standby NameNode and all DataNodes. " \
                "The following {0} host(s) do not satisfy the colocation recommendation: {1}".format(len(mismatchHosts), hostsString)
      items.append( { "type": 'host-component', "level": 'WARN', "message": message, "component-name": 'PXF' } )

    return items

  def getServiceConfigurationRecommendations(self, configurations, clusterData, services, hosts):
    if "hbase-env" in services["configurations"]:
      hbase_env = services["configurations"]["hbase-env"]["properties"]
      if "content" in hbase_env:
        content = hbase_env["content"]
        PXF_PATH = "export HBASE_CLASSPATH=${HBASE_CLASSPATH}:/usr/lib/pxf/pxf-hbase.jar"
        if "pxf-hbase.jar" not in content:
          PXF_PATH = "#Add pxf-hbase.jar to HBASE_CLASSPATH\n" + PXF_PATH
          content = "\n\n".join((content, PXF_PATH))
          putHbaseEnvProperty = self.putProperty(configurations, "hbase-env", services)
          putHbaseEnvProperty("content", content)

  def validatePXFHBaseEnvConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    # Check if HBASE_CLASSPATH should has the location of pxf-hbase.jar
    hbase_env = properties
    validationItems = []
    if "content" in hbase_env and "pxf-hbase.jar" not in hbase_env["content"]:
      message = "HBASE_CLASSPATH must contain the location of pxf-hbase.jar"
      validationItems.append({"config-name": "content", "item": self.getWarnItem(message)})

    return self.toConfigurationValidationProblems(validationItems, "hbase-env")

  def getServiceConfigurationsValidationItems(self, configurations, recommendedDefaults, services, hosts):
    siteName = "hbase-env"
    method = self.validatePXFHBaseEnvConfigurations
    items = self.validateConfigurationsForSite(configurations, recommendedDefaults, services, hosts, siteName, method)
    return items