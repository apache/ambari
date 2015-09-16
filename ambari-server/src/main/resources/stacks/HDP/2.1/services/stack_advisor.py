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

class HDP21StackAdvisor(HDP206StackAdvisor):

  def getServiceConfigurationRecommenderDict(self):
    parentRecommendConfDict = super(HDP21StackAdvisor, self).getServiceConfigurationRecommenderDict()
    childRecommendConfDict = {
      "OOZIE": self.recommendOozieConfigurations,
      "HIVE": self.recommendHiveConfigurations,
      "TEZ": self.recommendTezConfigurations
    }
    parentRecommendConfDict.update(childRecommendConfDict)
    return parentRecommendConfDict

  def recommendOozieConfigurations(self, configurations, clusterData, services, hosts):
    if "FALCON_SERVER" in clusterData["components"]:
      putOozieSiteProperty = self.putProperty(configurations, "oozie-site", services)
      falconUser = None
      if "falcon-env" in services["configurations"] and "falcon_user" in services["configurations"]["falcon-env"]["properties"]:
        falconUser = services["configurations"]["falcon-env"]["properties"]["falcon_user"]
        if falconUser is not None:
          putOozieSiteProperty("oozie.service.ProxyUserService.proxyuser.{0}.groups".format(falconUser) , "*")
          putOozieSiteProperty("oozie.service.ProxyUserService.proxyuser.{0}.hosts".format(falconUser) , "*")
        falconUserOldValue = getOldValue(self, services, "falcon-env", "falcon_user")
        if falconUserOldValue is not None:
          if 'forced-configurations' not in services:
            services["forced-configurations"] = []
          putOozieSitePropertyAttribute = self.putPropertyAttribute(configurations, "oozie-site")
          putOozieSitePropertyAttribute("oozie.service.ProxyUserService.proxyuser.{0}.groups".format(falconUserOldValue), 'delete', 'true')
          putOozieSitePropertyAttribute("oozie.service.ProxyUserService.proxyuser.{0}.hosts".format(falconUserOldValue), 'delete', 'true')
          services["forced-configurations"].append({"type" : "oozie-site", "name" : "oozie.service.ProxyUserService.proxyuser.{0}.hosts".format(falconUserOldValue)})
          services["forced-configurations"].append({"type" : "oozie-site", "name" : "oozie.service.ProxyUserService.proxyuser.{0}.groups".format(falconUserOldValue)})
          if falconUser is not None:
            services["forced-configurations"].append({"type" : "oozie-site", "name" : "oozie.service.ProxyUserService.proxyuser.{0}.hosts".format(falconUser)})
            services["forced-configurations"].append({"type" : "oozie-site", "name" : "oozie.service.ProxyUserService.proxyuser.{0}.groups".format(falconUser)})

      putMapredProperty = self.putProperty(configurations, "oozie-site")
      putMapredProperty("oozie.services.ext",
                        "org.apache.oozie.service.JMSAccessorService," +
                        "org.apache.oozie.service.PartitionDependencyManagerService," +
                        "org.apache.oozie.service.HCatAccessorService")

  def recommendHiveConfigurations(self, configurations, clusterData, services, hosts):
    containerSize = clusterData['mapMemory'] if clusterData['mapMemory'] > 2048 else int(clusterData['reduceMemory'])
    containerSize = min(clusterData['containers'] * clusterData['ramPerContainer'], containerSize)
    container_size_bytes = int(containerSize)*1024*1024
    putHiveProperty = self.putProperty(configurations, "hive-site", services)
    putHiveProperty('hive.auto.convert.join.noconditionaltask.size', int(round(container_size_bytes / 3)))
    putHiveProperty('hive.tez.java.opts', "-server -Xmx" + str(int(round((0.8 * containerSize) + 0.5)))
                    + "m -Djava.net.preferIPv4Stack=true -XX:NewRatio=8 -XX:+UseNUMA -XX:+UseParallelGC -XX:+PrintGCDetails -verbose:gc -XX:+PrintGCTimeStamps")
    putHiveProperty('hive.tez.container.size', containerSize)

  def recommendTezConfigurations(self, configurations, clusterData, services, hosts):
    putTezProperty = self.putProperty(configurations, "tez-site")
    putTezProperty("tez.am.resource.memory.mb", int(clusterData['amMemory']))
    putTezProperty("tez.am.java.opts",
                   "-server -Xmx" + str(int(0.8 * clusterData["amMemory"]))
                   + "m -Djava.net.preferIPv4Stack=true -XX:+UseNUMA -XX:+UseParallelGC")


  def getNotPreferableOnServerComponents(self):
    return ['STORM_UI_SERVER', 'DRPC_SERVER', 'STORM_REST_API', 'NIMBUS', 'GANGLIA_SERVER', 'METRICS_COLLECTOR']

  def getNotValuableComponents(self):
    return ['JOURNALNODE', 'ZKFC', 'GANGLIA_MONITOR', 'APP_TIMELINE_SERVER']

  def getComponentLayoutSchemes(self):
    parentSchemes = super(HDP21StackAdvisor, self).getComponentLayoutSchemes()
    childSchemes = {
        'APP_TIMELINE_SERVER': {31: 1, "else": 2},
        'FALCON_SERVER': {6: 1, 31: 2, "else": 3}
    }
    parentSchemes.update(childSchemes)
    return parentSchemes

  def getServiceConfigurationValidators(self):
    parentValidators = super(HDP21StackAdvisor, self).getServiceConfigurationValidators()
    childValidators = {
      "HIVE": {"hive-site": self.validateHiveConfigurations},
      "TEZ": {"tez-site": self.validateTezConfigurations}
    }
    self.mergeValidators(parentValidators, childValidators)
    return parentValidators

  def validateHiveConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = [ {"config-name": 'hive.tez.container.size', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'hive.tez.container.size')},
                        {"config-name": 'hive.tez.java.opts', "item": self.validateXmxValue(properties, recommendedDefaults, 'hive.tez.java.opts')},
                        {"config-name": 'hive.auto.convert.join.noconditionaltask.size', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'hive.auto.convert.join.noconditionaltask.size')} ]
    yarnSiteProperties = getSiteProperties(configurations, "yarn-site")
    if yarnSiteProperties:
      yarnSchedulerMaximumAllocationMb = to_number(yarnSiteProperties["yarn.scheduler.maximum-allocation-mb"])
      hiveTezContainerSize = to_number(properties['hive.tez.container.size'])
      if hiveTezContainerSize is not None and yarnSchedulerMaximumAllocationMb is not None and hiveTezContainerSize > yarnSchedulerMaximumAllocationMb:
        validationItems.append({"config-name": 'hive.tez.container.size', "item": self.getWarnItem("hive.tez.container.size is greater than the maximum container size specified in yarn.scheduler.maximum-allocation-mb")})
    return self.toConfigurationValidationProblems(validationItems, "hive-site")

  def validateTezConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = [ {"config-name": 'tez.am.resource.memory.mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'tez.am.resource.memory.mb')},
                        {"config-name": 'tez.am.java.opts', "item": self.validateXmxValue(properties, recommendedDefaults, 'tez.am.java.opts')} ]
    return self.toConfigurationValidationProblems(validationItems, "tez-site")


