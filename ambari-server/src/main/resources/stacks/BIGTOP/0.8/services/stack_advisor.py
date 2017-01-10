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

# Python Imports
import re
from math import ceil

# Local Imports
from resource_management.core.logger import Logger
from stack_advisor import DefaultStackAdvisor

class BaseBIGTOP08StackAdvisor(DefaultStackAdvisor):


  def __init__(self):
    super(BaseBIGTOP08StackAdvisor, self).__init__()
    Logger.initialize_logger()

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
    self.mastersWithMultipleInstances |= set(['ZOOKEEPER_SERVER', 'HBASE_MASTER'])

  def modifyCardinalitiesDict(self):
    """
    Modify the dictionary of cardinalities.
    Must be overriden in child class.
    """
    self.cardinalitiesDict.update(
      {
        'ZOOKEEPER_SERVER': {"min": 3},
        'HBASE_MASTER': {"min": 1}
      }
    )

  def modifyHeapSizeProperties(self):
    """
    Modify the dictionary of heap size properties.
    Must be overriden in child class.
    """
    # Nothing to do
    pass

  def modifyNotValuableComponents(self):
    """
    Modify the set of components whose host assignment is based on other services.
    Must be overriden in child class.
    """
    self.notValuableComponents |= set(['JOURNALNODE', 'ZKFC', 'GANGLIA_MONITOR'])

  def modifyComponentsNotPreferableOnServer(self):
    """
    Modify the set of components that are not preferable on the server.
    Must be overriden in child class.
    """
    self.notPreferableOnServerComponents |= set(['GANGLIA_SERVER'])

  def modifyComponentLayoutSchemes(self):
    """
    Modify layout scheme dictionaries for components.
    The scheme dictionary basically maps the number of hosts to
    host index where component should exist.
    Must be overriden in child class.
    """
    self.componentLayoutSchemes = {
      'NAMENODE': {"else": 0},
      'SECONDARY_NAMENODE': {"else": 1},
      'HBASE_MASTER': {6: 0, 31: 2, "else": 3},

      'HISTORYSERVER': {31: 1, "else": 2},
      'RESOURCEMANAGER': {31: 1, "else": 2},

      'OOZIE_SERVER': {6: 1, 31: 2, "else": 3},

      'HIVE_SERVER': {6: 1, 31: 2, "else": 4},
      'HIVE_METASTORE': {6: 1, 31: 2, "else": 4},
      'WEBHCAT_SERVER': {6: 1, 31: 2, "else": 4},
    }

  def getComponentLayoutValidations(self, services, hosts):
    """Returns array of Validation objects about issues with hostnames components assigned to"""
    items = []

    # Validating NAMENODE and SECONDARY_NAMENODE are on different hosts if possible
    hostsSet = set(super(BaseBIGTOP08StackAdvisor, self).getActiveHosts([host["Hosts"] for host in hosts["items"]]))
    hostsCount = len(hostsSet)

    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item for sublist in componentsListList for item in sublist]
    nameNodeHosts = [component["StackServiceComponents"]["hostnames"] for component in componentsList if component["StackServiceComponents"]["component_name"] == "NAMENODE"]
    secondaryNameNodeHosts = [component["StackServiceComponents"]["hostnames"] for component in componentsList if component["StackServiceComponents"]["component_name"] == "SECONDARY_NAMENODE"]

    # Validating cardinality
    for component in componentsList:
      if component["StackServiceComponents"]["cardinality"] is not None:
         componentName = component["StackServiceComponents"]["component_name"]
         componentDisplayName = component["StackServiceComponents"]["display_name"]
         componentHosts = []
         if component["StackServiceComponents"]["hostnames"] is not None:
           componentHosts = [componentHost for componentHost in component["StackServiceComponents"]["hostnames"] if componentHost in hostsSet]
         componentHostsCount = len(componentHosts)
         cardinality = str(component["StackServiceComponents"]["cardinality"])
         # cardinality types: null, 1+, 1-2, 1, ALL
         message = None
         if "+" in cardinality:
           hostsMin = int(cardinality[:-1])
           if componentHostsCount < hostsMin:
             message = "At least {0} {1} components should be installed in cluster.".format(hostsMin, componentDisplayName)
         elif "-" in cardinality:
           nums = cardinality.split("-")
           hostsMin = int(nums[0])
           hostsMax = int(nums[1])
           if componentHostsCount > hostsMax or componentHostsCount < hostsMin:
             message = "Between {0} and {1} {2} components should be installed in cluster.".format(hostsMin, hostsMax, componentDisplayName)
         elif "ALL" == cardinality:
           if componentHostsCount != hostsCount:
             message = "{0} component should be installed on all hosts in cluster.".format(componentDisplayName)
         else:
           if componentHostsCount != int(cardinality):
             message = "Exactly {0} {1} components should be installed in cluster.".format(int(cardinality), componentDisplayName)

         if message is not None:
           items.append({"type": 'host-component', "level": 'ERROR', "message": message, "component-name": componentName})

    # Validating host-usage
    usedHostsListList = [component["StackServiceComponents"]["hostnames"] for component in componentsList if not self.isComponentNotValuable(component)]
    usedHostsList = [item for sublist in usedHostsListList for item in sublist]
    nonUsedHostsList = [item for item in hostsSet if item not in usedHostsList]
    for host in nonUsedHostsList:
      items.append( { "type": 'host-component', "level": 'ERROR', "message": 'Host is not used', "host": str(host) } )

    return items

  def getServiceConfigurationRecommenderDict(self):
    return {
      "YARN": self.recommendYARNConfigurations,
      "MAPREDUCE2": self.recommendMapReduce2Configurations
    }

  def putProperty(self, config, configType):
    config[configType] = {"properties": {}}
    def appendProperty(key, value):
      config[configType]["properties"][key] = str(value)
    return appendProperty

  def recommendYARNConfigurations(self, configurations, clusterData):
    putYarnProperty = self.putProperty(configurations, "yarn-site")
    putYarnProperty('yarn.nodemanager.resource.memory-mb', int(round(clusterData['containers'] * clusterData['ramPerContainer'])))
    putYarnProperty('yarn.scheduler.minimum-allocation-mb', int(clusterData['ramPerContainer']))
    putYarnProperty('yarn.scheduler.maximum-allocation-mb', int(round(clusterData['containers'] * clusterData['ramPerContainer'])))

  def recommendMapReduce2Configurations(self, configurations, clusterData):
    putMapredProperty = self.putProperty(configurations, "mapred-site")
    putMapredProperty('yarn.app.mapreduce.am.resource.mb', int(clusterData['amMemory']))
    putMapredProperty('yarn.app.mapreduce.am.command-opts', "-Xmx" + str(int(round(0.8 * clusterData['amMemory']))) + "m")
    putMapredProperty('mapreduce.map.memory.mb', clusterData['mapMemory'])
    putMapredProperty('mapreduce.reduce.memory.mb', int(clusterData['reduceMemory']))
    putMapredProperty('mapreduce.map.java.opts', "-Xmx" + str(int(round(0.8 * clusterData['mapMemory']))) + "m")
    putMapredProperty('mapreduce.reduce.java.opts', "-Xmx" + str(int(round(0.8 * clusterData['reduceMemory']))) + "m")
    putMapredProperty('mapreduce.task.io.sort.mb', min(int(round(0.4 * clusterData['mapMemory'])), 1024))

  def getConfigurationClusterSummary(self, servicesList, hosts, components, services):

    hBaseInstalled = False
    if 'HBASE' in servicesList:
      hBaseInstalled = True

    cluster = {
      "cpu": 0,
      "disk": 0,
      "ram": 0,
      "hBaseInstalled": hBaseInstalled,
      "components": components
    }

    if len(hosts["items"]) > 0:
      host = hosts["items"][0]["Hosts"]
      cluster["cpu"] = host["cpu_count"]
      cluster["disk"] = len(host["disk_info"])
      cluster["ram"] = int(host["total_mem"] / (1024 * 1024))

    ramRecommendations = [
      {"os":1, "hbase":1},
      {"os":2, "hbase":1},
      {"os":2, "hbase":2},
      {"os":4, "hbase":4},
      {"os":6, "hbase":8},
      {"os":8, "hbase":8},
      {"os":8, "hbase":8},
      {"os":12, "hbase":16},
      {"os":24, "hbase":24},
      {"os":32, "hbase":32},
      {"os":64, "hbase":64}
    ]
    index = {
      cluster["ram"] <= 4: 0,
      4 < cluster["ram"] <= 8: 1,
      8 < cluster["ram"] <= 16: 2,
      16 < cluster["ram"] <= 24: 3,
      24 < cluster["ram"] <= 48: 4,
      48 < cluster["ram"] <= 64: 5,
      64 < cluster["ram"] <= 72: 6,
      72 < cluster["ram"] <= 96: 7,
      96 < cluster["ram"] <= 128: 8,
      128 < cluster["ram"] <= 256: 9,
      256 < cluster["ram"]: 10
    }[1]
    cluster["reservedRam"] = ramRecommendations[index]["os"]
    cluster["hbaseRam"] = ramRecommendations[index]["hbase"]

    cluster["minContainerSize"] = {
      cluster["ram"] <= 4: 256,
      4 < cluster["ram"] <= 8: 512,
      8 < cluster["ram"] <= 24: 1024,
      24 < cluster["ram"]: 2048
    }[1]

    totalAvailableRam = cluster["ram"] - cluster["reservedRam"]
    if cluster["hBaseInstalled"]:
      totalAvailableRam -= cluster["hbaseRam"]
    cluster["totalAvailableRam"] = max(2048, totalAvailableRam * 1024)
    '''containers = max(3, min (2*cores,min (1.8*DISKS,(Total available RAM) / MIN_CONTAINER_SIZE))))'''
    cluster["containers"] = round(max(3,
                                min(2 * cluster["cpu"],
                                    min(ceil(1.8 * cluster["disk"]),
                                            cluster["totalAvailableRam"] / cluster["minContainerSize"]))))

    '''ramPerContainers = max(2GB, RAM - reservedRam - hBaseRam) / containers'''
    cluster["ramPerContainer"] = abs(cluster["totalAvailableRam"] / cluster["containers"])
    '''If greater than 1GB, value will be in multiples of 512.'''
    if cluster["ramPerContainer"] > 1024:
      cluster["ramPerContainer"] = int(cluster["ramPerContainer"] / 512) * 512

    cluster["mapMemory"] = int(cluster["ramPerContainer"])
    cluster["reduceMemory"] = cluster["ramPerContainer"]
    cluster["amMemory"] = max(cluster["mapMemory"], cluster["reduceMemory"])

    return cluster

  def getConfigurationsValidationItems(self, services, hosts):
    """Returns array of Validation objects about issues with configuration values provided in services"""
    items = []

    recommendations = self.recommendConfigurations(services, hosts)
    recommendedDefaults = recommendations["recommendations"]["blueprint"]["configurations"]

    configurations = services["configurations"]
    for service in services["services"]:
      serviceName = service["StackServices"]["service_name"]
      validator = self.validateServiceConfigurations(serviceName)
      if validator is not None:
        siteName = validator[0]
        method = validator[1]
        if siteName in recommendedDefaults:
          siteProperties = getSiteProperties(configurations, siteName)
          if siteProperties is not None:
            resultItems = method(siteProperties, recommendedDefaults[siteName]["properties"], configurations)
            items.extend(resultItems)
    return items

  def getServiceConfigurationValidators(self):
    return {
      "MAPREDUCE2": ["mapred-site", self.validateMapReduce2Configurations],
      "YARN": ["yarn-site", self.validateYARNConfigurations]
    }

  def validateServiceConfigurations(self, serviceName):
    return self.getServiceConfigurationValidators().get(serviceName, None)

  def toConfigurationValidationProblems(self, validationProblems, siteName):
    result = []
    for validationProblem in validationProblems:
      validationItem = validationProblem.get("item", None)
      if validationItem is not None:
        problem = {"type": 'configuration', "level": validationItem["level"], "message": validationItem["message"],
                   "config-type": siteName, "config-name": validationProblem["config-name"] }
        result.append(problem)
    return result

  def getWarnItem(self, message):
    return {"level": "WARN", "message": message}

  def getErrorItem(self, message):
    return {"level": "ERROR", "message": message}

  def validatorLessThenDefaultValue(self, properties, recommendedDefaults, propertyName):
    if not propertyName in properties:
      return self.getErrorItem("Value should be set")
    value = to_number(properties[propertyName])
    if value is None:
      return self.getErrorItem("Value should be integer")
    defaultValue = to_number(recommendedDefaults[propertyName])
    if defaultValue is None:
      return None
    if value < defaultValue:
      return self.getWarnItem("Value is less than the recommended default of {0}".format(defaultValue))
    return None

  def validateXmxValue(self, properties, recommendedDefaults, propertyName):
    if not propertyName in properties:
      return self.getErrorItem("Value should be set")
    value = properties[propertyName]
    defaultValue = recommendedDefaults[propertyName]
    if defaultValue is None:
      return self.getErrorItem("Config's default value can't be null or undefined")
    if not checkXmxValueFormat(value):
      return self.getErrorItem('Invalid value format')
    valueInt = formatXmxSizeToBytes(getXmxSize(value))
    defaultValueXmx = getXmxSize(defaultValue)
    defaultValueInt = formatXmxSizeToBytes(defaultValueXmx)
    if valueInt < defaultValueInt:
      return self.getWarnItem("Value is less than the recommended default of -Xmx" + defaultValueXmx)
    return None

  def validateMapReduce2Configurations(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = [ {"config-name": 'mapreduce.map.java.opts', "item": self.validateXmxValue(properties, recommendedDefaults, 'mapreduce.map.java.opts')},
                        {"config-name": 'mapreduce.reduce.java.opts', "item": self.validateXmxValue(properties, recommendedDefaults, 'mapreduce.reduce.java.opts')},
                        {"config-name": 'mapreduce.task.io.sort.mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'mapreduce.task.io.sort.mb')},
                        {"config-name": 'mapreduce.map.memory.mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'mapreduce.map.memory.mb')},
                        {"config-name": 'mapreduce.reduce.memory.mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'mapreduce.reduce.memory.mb')},
                        {"config-name": 'yarn.app.mapreduce.am.resource.mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'yarn.app.mapreduce.am.resource.mb')},
                        {"config-name": 'yarn.app.mapreduce.am.command-opts', "item": self.validateXmxValue(properties, recommendedDefaults, 'yarn.app.mapreduce.am.command-opts')} ]
    return self.toConfigurationValidationProblems(validationItems, "mapred-site")

  def validateYARNConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = [ {"config-name": 'yarn.nodemanager.resource.memory-mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'yarn.nodemanager.resource.memory-mb')},
                        {"config-name": 'yarn.scheduler.minimum-allocation-mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'yarn.scheduler.minimum-allocation-mb')},
                        {"config-name": 'yarn.scheduler.maximum-allocation-mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'yarn.scheduler.maximum-allocation-mb')} ]
    return self.toConfigurationValidationProblems(validationItems, "yarn-site")

class BIGTOP08StackAdvisor(BaseBIGTOP08StackAdvisor):

  def __init__(self):
    super(BIGTOP08StackAdvisor, self).__init__()

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
    # Nothing to do
    pass

  def modifyNotValuableComponents(self):
    """
    Modify the set of components whose host assignment is based on other services.
    Must be overriden in child class.
    """
    self.notValuableComponents |= set(['APP_TIMELINE_SERVER'])

  def modifyComponentsNotPreferableOnServer(self):
    """
    Modify the set of components that are not preferable on the server.
    Must be overriden in child class.
    """
    self.notPreferableOnServerComponents |= set(['STORM_UI_SERVER', 'DRPC_SERVER', 'STORM_REST_API', 'NIMBUS'])

  def modifyComponentLayoutSchemes(self):
    """
    Modify layout scheme dictionaries for components.
    The scheme dictionary basically maps the number of hosts to
    host index where component should exist.
    Must be overriden in child class.
    """
    self.componentLayoutSchemes.update({
      'APP_TIMELINE_SERVER': {31: 1, "else": 2},
      'FALCON_SERVER': {6: 1, 31: 2, "else": 3}
    })

  def getServiceConfigurationRecommenderDict(self):
    parentRecommendConfDict = super(BIGTOP08StackAdvisor, self).getServiceConfigurationRecommenderDict()
    childRecommendConfDict = {
      "OOZIE": self.recommendOozieConfigurations,
      "HIVE": self.recommendHiveConfigurations,
      "TEZ": self.recommendTezConfigurations
    }
    parentRecommendConfDict.update(childRecommendConfDict)
    return parentRecommendConfDict

  def recommendOozieConfigurations(self, configurations, clusterData, services, hosts):
    if "FALCON_SERVER" in clusterData["components"]:
      putMapredProperty = self.putProperty(configurations, "oozie-site")
      putMapredProperty("oozie.services.ext",
                        "org.apache.oozie.service.JMSAccessorService," +
                        "org.apache.oozie.service.PartitionDependencyManagerService," +
                        "org.apache.oozie.service.HCatAccessorService")

  def recommendHiveConfigurations(self, configurations, clusterData, services, hosts):
    containerSize = clusterData['mapMemory'] if clusterData['mapMemory'] > 2048 else int(clusterData['reduceMemory'])
    containerSize = min(clusterData['containers'] * clusterData['ramPerContainer'], containerSize)
    putHiveProperty = self.putProperty(configurations, "hive-site")
    putHiveProperty('hive.auto.convert.join.noconditionaltask.size', int(round(containerSize / 3)) * 1048576)
    putHiveProperty('hive.tez.java.opts', "-server -Xmx" + str(int(round((0.8 * containerSize) + 0.5)))
                    + "m -Djava.net.preferIPv4Stack=true -XX:NewRatio=8 -XX:+UseNUMA -XX:+UseParallelGC")
    putHiveProperty('hive.tez.container.size', containerSize)

  def recommendTezConfigurations(self, configurations, clusterData, services, hosts):
    putTezProperty = self.putProperty(configurations, "tez-site")
    putTezProperty("tez.am.resource.memory.mb", int(clusterData['amMemory']))
    putTezProperty("tez.am.java.opts",
                   "-server -Xmx" + str(int(0.8 * clusterData["amMemory"]))
                   + "m -Djava.net.preferIPv4Stack=true -XX:+UseNUMA -XX:+UseParallelGC")

  def getServiceConfigurationValidators(self):
    parentValidators = super(BIGTOP08StackAdvisor, self).getServiceConfigurationValidators()
    childValidators = {
      "HIVE": ["hive-site", self.validateHiveConfigurations],
      "TEZ": ["tez-site", self.validateTezConfigurations]
    }
    parentValidators.update(childValidators)
    return parentValidators

  def validateHiveConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = [ {"config-name": 'hive.tez.container.size', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'hive.tez.container.size')},
                        {"config-name": 'hive.tez.java.opts', "item": self.validateXmxValue(properties, recommendedDefaults, 'hive.tez.java.opts')},
                        {"config-name": 'hive.auto.convert.join.noconditionaltask.size', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'hive.auto.convert.join.noconditionaltask.size')} ]
    return self.toConfigurationValidationProblems(validationItems, "hive-site")

  def validateTezConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = [ {"config-name": 'tez.am.resource.memory.mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'tez.am.resource.memory.mb')},
                        {"config-name": 'tez.am.java.opts', "item": self.validateXmxValue(properties, recommendedDefaults, 'tez.am.java.opts')} ]
    return self.toConfigurationValidationProblems(validationItems, "tez-site")

# Validation helper methods
def getSiteProperties(configurations, siteName):
  siteConfig = configurations.get(siteName)
  if siteConfig is None:
    return None
  return siteConfig.get("properties")

def to_number(s):
  try:
    return int(re.sub("\D", "", s))
  except ValueError:
    return None

def checkXmxValueFormat(value):
  p = re.compile('-Xmx(\d+)(b|k|m|g|p|t|B|K|M|G|P|T)?')
  matches = p.findall(value)
  return len(matches) == 1

def getXmxSize(value):
  p = re.compile("-Xmx(\d+)(.?)")
  result = p.findall(value)[0]
  if len(result) > 1:
    # result[1] - is a space or size formatter (b|k|m|g etc)
    return result[0] + result[1].lower()
  return result[0]

def formatXmxSizeToBytes(value):
  value = value.lower()
  if len(value) == 0:
    return 0
  modifier = value[-1]

  if modifier == ' ' or modifier in "0123456789":
    modifier = 'b'
  m = {
    modifier == 'b': 1,
    modifier == 'k': 1024,
    modifier == 'm': 1024 * 1024,
    modifier == 'g': 1024 * 1024 * 1024,
    modifier == 't': 1024 * 1024 * 1024 * 1024,
    modifier == 'p': 1024 * 1024 * 1024 * 1024 * 1024
    }[1]
  return to_number(value) * m

def getPort(address):
  """
  Extracts port from the address like 0.0.0.0:1019
  """
  if address is None:
    return None
  m = re.search(r'(?:http(?:s)?://)?([\w\d.]*):(\d{1,5})', address)
  if m is not None:
    return int(m.group(2))
  else:
    return None

def isSecurePort(port):
  """
  Returns True if port is root-owned at *nix systems
  """
  if port is not None:
    return port < 1024
  else:
    return False
