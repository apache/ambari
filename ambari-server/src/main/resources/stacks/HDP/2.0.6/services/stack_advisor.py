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

import re
import socket
import sys

from stack_advisor import StackAdvisor

class HDP206StackAdvisor(StackAdvisor):

  def recommendComponentLayout(self, services, hosts):
    """
    Returns Services object with hostnames array populated for components
    If hostnames are populated for some components (partial blueprint) - these components will not be processed
    """
    stackName = services["Versions"]["stack_name"]
    stackVersion = services["Versions"]["stack_version"]
    hostsList = [host["Hosts"]["host_name"] for host in hosts["items"]]
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

    recommendations = {
      "Versions": {"stack_name": stackName, "stack_version": stackVersion},
      "hosts": hostsList,
      "services": servicesList,
      "recommendations": {
        "blueprint": {
          "configurations": {
            "global": {
              "properties": { }
            },
            "core-site": { },
            "hdfs-site": { },
            "yarn-site": { },
            "hbase-site": { }
          },
          "host_groups": [ ]
        },
        "blueprint_cluster_binding": {
          "host_groups": [ ]
        }
      }
    }

    hostsComponentsMap = {}

    #extend 'hostsComponentsMap' with MASTER components
    for service in services["services"]:
      masterComponents = [component for component in service["components"] if isMaster(component)]
      for component in masterComponents:
        componentName = component["StackServiceComponents"]["component_name"]
        hostsForComponent = []

        if isAlreadyPopulated(component):
          hostsForComponent = component["StackServiceComponents"]["hostnames"]
        else:
          availableHosts = hostsList
          if len(hostsList) > 1 and isNotPreferableOnAmbariServerHost(component):
            availableHosts = [hostName for hostName in hostsList if not isLocalHost(hostName)]

          if isMasterWithMultipleInstances(component):
            hostsCount = defaultNoOfMasterHosts(component)
            if hostsCount > 1: # get first 'hostsCount' available hosts
              if len(availableHosts) < hostsCount:
                hostsCount = len(availableHosts)
              hostsForComponent = availableHosts[:hostsCount]
            else:
              hostsForComponent = [getHostForComponent(component, availableHosts)]
          else:
            hostsForComponent = [getHostForComponent(component, availableHosts)]

        #extend 'hostsComponentsMap' with 'hostsForComponent'
        for hostName in hostsForComponent:
          if hostName not in hostsComponentsMap:
            hostsComponentsMap[hostName] = []
          hostsComponentsMap[hostName].append( { "name":componentName } )

    #extend 'hostsComponentsMap' with Slave and Client Components
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item for sublist in componentsListList for item in sublist]
    usedHostsListList = [component["StackServiceComponents"]["hostnames"] for component in componentsList if not isNotValuable(component)]
    utilizedHosts = [item for sublist in usedHostsListList for item in sublist]
    freeHosts = [hostName for hostName in hostsList if hostName not in utilizedHosts]

    for service in services["services"]:
      slaveClientComponents = [component for component in service["components"] if isSlave(component) or isClient(component)]
      for component in slaveClientComponents:
        componentName = component["StackServiceComponents"]["component_name"]
        hostsForComponent = []

        if isAlreadyPopulated(component):
          hostsForComponent = component["StackServiceComponents"]["hostnames"]
        elif component["StackServiceComponents"]["cardinality"] == "ALL":
          hostsForComponent = hostsList
        else:
          if len(freeHosts) == 0:
            hostsForComponent = hostsList[-1:]
          else: # len(freeHosts) >= 1
            hostsForComponent = freeHosts
            if isClient(component):
              hostsForComponent = freeHosts[0:1]

        #extend 'hostsComponentsMap' with 'hostsForComponent'
        for hostName in hostsForComponent:
          if hostName not in hostsComponentsMap:
            hostsComponentsMap[hostName] = []
          hostsComponentsMap[hostName].append( { "name": componentName } )

    #prepare 'host-group's from 'hostsComponentsMap'
    host_groups = recommendations["recommendations"]["blueprint"]["host_groups"]
    bindings = recommendations["recommendations"]["blueprint_cluster_binding"]["host_groups"]
    index = 0
    for key in hostsComponentsMap.keys():
      index += 1
      host_group_name = "host-group-{0}".format(index)
      host_groups.append( { "name": host_group_name, "components": hostsComponentsMap[key] } )
      bindings.append( { "name": host_group_name, "hosts": [{ "fqdn": socket.getfqdn(key) }] } )

    return recommendations
  pass

  def validateComponentLayout(self, services, hosts):
    """Returns array of Validation objects about issues with hostnames components assigned to"""
    stackName = services["Versions"]["stack_name"]
    stackVersion = services["Versions"]["stack_version"]

    validations = {
      "Versions": {"stack_name": stackName, "stack_version": stackVersion},
      "items": [ ]
    }
    items = validations["items"]

    # Validating NAMENODE and SECONDARY_NAMENODE are on different hosts if possible
    hostsList = [host["Hosts"]["host_name"] for host in hosts["items"]]
    hostsCount = len(hostsList)
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item for sublist in componentsListList for item in sublist]
    nameNodeHosts = [component["StackServiceComponents"]["hostnames"] for component in componentsList if component["StackServiceComponents"]["component_name"] == "NAMENODE"]
    secondaryNameNodeHosts = [component["StackServiceComponents"]["hostnames"] for component in componentsList if component["StackServiceComponents"]["component_name"] == "SECONDARY_NAMENODE"]

    if hostsCount > 1 and len(nameNodeHosts) > 0 and len(secondaryNameNodeHosts) > 0:
      nameNodeHosts = nameNodeHosts[0]
      secondaryNameNodeHosts = secondaryNameNodeHosts[0]
      commonHosts = list(set(nameNodeHosts).intersection(secondaryNameNodeHosts))
      for host in commonHosts:
        items.append( { "type": 'host-component', "level": 'ERROR', "message": 'NameNode and Secondary NameNode cannot be hosted on same machine', "component-name": 'NAMENODE', "host": str(host) } )
        items.append( { "type": 'host-component', "level": 'ERROR', "message": 'NameNode and Secondary NameNode cannot be hosted on same machine', "component-name": 'SECONDARY_NAMENODE', "host": str(host) } )

    # Validating cardinality
    for component in componentsList:
      if component["StackServiceComponents"]["cardinality"] is not None:
         componentName = component["StackServiceComponents"]["component_name"]
         componentHostsCount = 0
         if component["StackServiceComponents"]["hostnames"] is not None:
           componentHostsCount = len(component["StackServiceComponents"]["hostnames"])
         cardinality = str(component["StackServiceComponents"]["cardinality"])
         # cardinality types: null, 1+, 1-2, 1, ALL
         hostsMax = -sys.maxint - 1
         hostsMin = sys.maxint
         hostsMin = 0
         hostsMax = 0
         if "+" in cardinality:
           hostsMin = int(cardinality[:-1])
           hostsMax = sys.maxint
         elif "-" in cardinality:
           nums = cardinality.split("-")
           hostsMin = int(nums[0])
           hostsMax = int(nums[1])
         elif "ALL" == cardinality:
           hostsMin = hostsCount
           hostsMax = hostsCount
         else:
           hostsMin = int(cardinality)
           hostsMax = int(cardinality)

         if componentHostsCount > hostsMax or componentHostsCount < hostsMin:
           items.append( { "type": 'host-component', "level": 'ERROR', "message": 'Cardinality violation, cardinality={0}, hosts count={1}'.format(cardinality, str(componentHostsCount)), "component-name": str(componentName) } )

    # Validating host-usage
    usedHostsListList = [component["StackServiceComponents"]["hostnames"] for component in componentsList if not isNotValuable(component)]
    usedHostsList = [item for sublist in usedHostsListList for item in sublist]
    nonUsedHostsList = [item for item in hostsList if item not in usedHostsList]
    for host in nonUsedHostsList:
      items.append( { "type": 'host-component', "level": 'ERROR', "message": 'Host is not used', "host": str(host) } )

    return validations
  pass

  def recommendConfigurations(self, services, hosts):
    stackName = services["Versions"]["stack_name"]
    stackVersion = services["Versions"]["stack_version"]
    hostsList = [host["Hosts"]["host_name"] for host in hosts["items"]]
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    components = [component["StackServiceComponents"]["component_name"]
                  for service in services["services"]
                  for component in service["components"]]

    clusterData = self.getClusterData(servicesList, hosts, components)

    recommendations = {
      "Versions": {"stack_name": stackName, "stack_version": stackVersion},
      "hosts": hostsList,
      "services": servicesList,
      "recommendations": {
        "blueprint": {
          "configurations": {},
          "host_groups": []
        },
        "blueprint_cluster_binding": {
          "host_groups": []
        }
      }
    }

    configurations = recommendations["recommendations"]["blueprint"]["configurations"]

    for service in servicesList:
      calculation = self.recommendServiceConfigurations(service)
      if calculation is not None:
        calculation(configurations, clusterData)

    return recommendations

  def recommendServiceConfigurations(self, service):
    return {
      "YARN": self.recommendYARNConfigurations,
      "MAPREDUCE2": self.recommendMapReduce2Configurations,
      "HIVE": self.recommendHiveConfigurations,
      "OOZIE": self.recommendOozieConfigurations
    }.get(service, None)

  def putProperty(self, config, configType):
    config[configType] = {"properties": {}}
    def appendProperty(key, value):
      config[configType]["properties"][key] = str(value)
    return appendProperty

  def recommendYARNConfigurations(self, configurations, clusterData):
    putYarnProperty = self.putProperty(configurations, "yarn-site")
    putYarnProperty('yarn.nodemanager.resource.memory-mb', clusterData['containers'] * clusterData['ramPerContainer'])
    putYarnProperty('yarn.scheduler.minimum-allocation-mb', clusterData['ramPerContainer'])
    putYarnProperty('yarn.scheduler.maximum-allocation-mb', clusterData['containers'] * clusterData['ramPerContainer'])

  def recommendHiveConfigurations(self, configurations, clusterData):
    containerSize = clusterData['mapMemory'] if clusterData['mapMemory'] > 2048 else clusterData['reduceMemory']
    containerSize = min(clusterData['containers'] * clusterData['ramPerContainer'], containerSize)
    putHiveProperty = self.putProperty(configurations, "hive-site")
    putHiveProperty('hive.auto.convert.join.noconditionaltask.size', int(containerSize / 3) * 1048576)
    putHiveProperty('hive.tez.java.opts', "-server -Xmx" + str(int(0.8 * containerSize))
                    + "m -Djava.net.preferIPv4Stack=true -XX:NewRatio=8 -XX:+UseNUMA -XX:+UseParallelGC")
    putHiveProperty('hive.tez.container.size', containerSize)

  def recommendMapReduce2Configurations(self, configurations, clusterData):
    putMapredProperty = self.putProperty(configurations, "mapred-site")
    putMapredProperty('yarn.app.mapreduce.am.resource.mb', clusterData['amMemory'])
    putMapredProperty('yarn.app.mapreduce.am.command-opts', "-Xmx" + str(int(0.8 * clusterData['amMemory'])) + "m")
    putMapredProperty('mapreduce.map.memory.mb', clusterData['mapMemory'])
    putMapredProperty('mapreduce.reduce.memory.mb', clusterData['reduceMemory'])
    putMapredProperty('mapreduce.map.java.opts', "-Xmx" + str(int(0.8 * clusterData['mapMemory'])) + "m")
    putMapredProperty('mapreduce.reduce.java.opts', "-Xmx" + str(int(0.8 * clusterData['reduceMemory'])) + "m")
    putMapredProperty('mapreduce.task.io.sort.mb', int(min(0.4 * clusterData['mapMemory'], 1024)))

  def recommendOozieConfigurations(self, configurations, clusterData):
    if "FALCON_SERVER" in clusterData["components"]:
      putMapredProperty = self.putProperty(configurations, "oozie-site")
      putMapredProperty("oozie.services.ext",
                        "org.apache.oozie.service.JMSAccessorService," +
                        "org.apache.oozie.service.PartitionDependencyManagerService," +
                        "org.apache.oozie.service.HCatAccessorService")

  def getClusterData(self, servicesList, hosts, components):

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

    '''containers = max(3, min (2*cores,min (1.8*DISKS,(Total available RAM) / MIN_CONTAINER_SIZE))))'''
    cluster["containers"] = max(3,
                                min(2 * cluster["cpu"],
                                    int(min(1.8 * cluster["disk"],
                                            cluster["ram"] / cluster["minContainerSize"]))))

    '''ramPerContainers = max(2GB, RAM - reservedRam - hBaseRam) / containers'''
    cluster["ramPerContainer"] = max(2048,
                                     cluster["ram"] - cluster["reservedRam"] - cluster["hbaseRam"])
    cluster["ramPerContainer"] /= cluster["containers"]
    '''If greater than 1GB, value will be in multiples of 512.'''
    if cluster["ramPerContainer"] > 1024:
      cluster["ramPerContainer"] = ceil(cluster["ramPerContainer"] / 512) * 512

    cluster["mapMemory"] = int(cluster["ramPerContainer"])
    cluster["reduceMemory"] = cluster["ramPerContainer"]
    cluster["amMemory"] = max(cluster["mapMemory"], cluster["reduceMemory"])

    return cluster


  def validateConfigurations(self, services, hosts):
    """Returns array of Validation objects about issues with configuration values provided in services"""
    stackName = services["Versions"]["stack_name"]
    stackVersion = services["Versions"]["stack_version"]

    validations = {
      "Versions": {"stack_name": stackName, "stack_version": stackVersion},
      "items": [ ]
    }
    items = validations["items"]

    recommendations = self.recommendConfigurations(services, hosts)
    recommendedDefaults = recommendations["recommendations"]["blueprint"]["configurations"]

    configurations = services["configurations"]
    for service in services["services"]:
      serviceName = service["StackServices"]["service_name"]
      if serviceName == "MAPREDUCE2":
        mapReduceErrors = validateMapReduce2Configurations(getSiteProperties(configurations, "mapred-site"), recommendedDefaults["mapred-site"]["properties"])
        items.extend(mapReduceErrors)
      elif serviceName == "HIVE":
        hiveErrors = validateHiveConfigurations(getSiteProperties(configurations, "hive-site"), recommendedDefaults["hive-site"]["properties"])
        items.extend(hiveErrors)
      elif serviceName == "STORM":
        oozieErrors = [] #validateStormConfigurations(getSiteProperties(configurations, "storm-site"), recommendedDefaults["storm-site"]["properties"])
        items.extend(oozieErrors)
      elif serviceName == "TEZ":
        tezErrors = validateTezConfigurations(getSiteProperties(configurations, "tez-site"), recommendedDefaults["tez-site"]["properties"])
        items.extend(tezErrors)
      elif serviceName == "YARN":
        yarnErrors = validateYARNConfigurations(getSiteProperties(configurations, "yarn-site"), recommendedDefaults["yarn-site"]["properties"])
        items.extend(yarnErrors)
      else:
        pass
    return validations
    pass

def getSiteProperties(configurations, siteName):
  if configurations[siteName] is None:
    return {}
  if configurations[siteName]["properties"] is None:
    return {}
  return configurations[siteName]["properties"]

def to_number(s):
  try:
    return int(re.sub("\D", "", s))
  except ValueError:
    return None

def toConfigurationValidationErrors(items, siteName):
  result = []
  for item in items:
    if item["message"] is not None:
      error = { "type": 'configuration', "level": 'ERROR', "message": item["message"], "config-type": siteName, "config-name": item["config-name"] }
      result.append(error)
  return result

def validatorLessThenDefaultValue(properties, recommendedDefaults, propertyName):
  value = to_number(properties[propertyName])
  if value is None:
    return "Value should be integer"
  defaultValue = to_number(recommendedDefaults[propertyName])
  if defaultValue is None:
    return None
  if value < defaultValue:
    return "Value is less than the recommended default of {0}".format(defaultValue)
  return None

def validateXmxValue(properties, recommendedDefaults, propertyName):
  value = properties[propertyName]
  defaultValue = recommendedDefaults[propertyName]
  if defaultValue is None:
    return "Config's default value can't be null or undefined"
  if not checkXmxValueFormat(value):
    return 'Invalid value format'
  valueInt = formatXmxSizeToBytes(getXmxSize(value))
  defaultValueXmx = getXmxSize(defaultValue)
  defaultValueInt = formatXmxSizeToBytes(defaultValueXmx)
  if valueInt < defaultValueInt:
    return "Value is less than the recommended default of -Xmx" + defaultValueXmx
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

def validateMapReduce2Configurations(properties, recommendedDefaults):
  validationItems = [ {"config-name": 'mapreduce.map.java.opts', "message": validateXmxValue(properties, recommendedDefaults, 'mapreduce.map.java.opts')},
                      {"config-name": 'mapreduce.reduce.java.opts', "message": validateXmxValue(properties, recommendedDefaults, 'mapreduce.reduce.java.opts')},
                      {"config-name": 'mapreduce.task.io.sort.mb', "message": validatorLessThenDefaultValue(properties, recommendedDefaults, 'mapreduce.task.io.sort.mb')},
                      {"config-name": 'mapreduce.map.memory.mb', "message": validatorLessThenDefaultValue(properties, recommendedDefaults, 'mapreduce.map.memory.mb')},
                      {"config-name": 'mapreduce.reduce.memory.mb', "message": validatorLessThenDefaultValue(properties, recommendedDefaults, 'mapreduce.reduce.memory.mb')},
                      {"config-name": 'yarn.app.mapreduce.am.resource.mb', "message": validatorLessThenDefaultValue(properties, recommendedDefaults, 'yarn.app.mapreduce.am.resource.mb')},
                      {"config-name": 'yarn.app.mapreduce.am.command-opts', "message": validateXmxValue(properties, recommendedDefaults, 'yarn.app.mapreduce.am.command-opts')} ]
  return toConfigurationValidationErrors(validationItems, "mapred-site")

def validateHiveConfigurations(properties, recommendedDefaults):
  validationItems = [ {"config-name": 'hive.tez.container.size', "message": validatorLessThenDefaultValue(properties, recommendedDefaults, 'hive.tez.container.size')},
                      {"config-name": 'hive.tez.java.opts', "message": validateXmxValue(properties, recommendedDefaults, 'hive.tez.java.opts')},
                      {"config-name": 'hive.auto.convert.join.noconditionaltask.size', "message": validatorLessThenDefaultValue(properties, recommendedDefaults, 'hive.auto.convert.join.noconditionaltask.size')} ]
  return toConfigurationValidationErrors(validationItems, "hive-site")

def validateStormConfigurations(properties, recommendedDefaults):
  validationItems = [ {"config-name": 'drpc.childopts', "message": validateXmxValue(properties, recommendedDefaults, 'drpc.childopts')},
                      {"config-name": 'ui.childopts', "message": validateXmxValue(properties, recommendedDefaults, 'ui.childopts')},
                      {"config-name": 'logviewer.childopts', "message": validateXmxValue(properties, recommendedDefaults, 'logviewer.childopts')} ]
  return toConfigurationValidationErrors(validationItems, "storm-site")

def validateTezConfigurations(properties, recommendedDefaults):
  validationItems = [ {"config-name": 'tez.am.resource.memory.mb', "message": validatorLessThenDefaultValue(properties, recommendedDefaults, 'tez.am.resource.memory.mb')},
                      {"config-name": 'tez.am.java.opts', "message": validateXmxValue(properties, recommendedDefaults, 'tez.am.java.opts')} ]
  return toConfigurationValidationErrors(validationItems, "tez-site")

def validateYARNConfigurations(properties, recommendedDefaults):
  validationItems = [ {"config-name": 'yarn.nodemanager.resource.memory-mb', "message": validatorLessThenDefaultValue(properties, recommendedDefaults, 'yarn.nodemanager.resource.memory-mb')},
                      {"config-name": 'yarn.scheduler.minimum-allocation-mb', "message": validatorLessThenDefaultValue(properties, recommendedDefaults, 'yarn.scheduler.minimum-allocation-mb')},
                      {"config-name": 'yarn.scheduler.maximum-allocation-mb', "message": validatorLessThenDefaultValue(properties, recommendedDefaults, 'yarn.scheduler.maximum-allocation-mb')} ]
  return toConfigurationValidationErrors(validationItems, "yarn-site")

# Helper methods
def getHostForComponent(component, hostsList):
  componentName = component["StackServiceComponents"]["component_name"]
  scheme = selectionScheme(componentName)

  if len(hostsList) == 1:
    return hostsList[0]
  else:
    for key in scheme.keys():
      if isinstance(key, ( int, long )):
        if len(hostsList) < key:
          return hostsList[scheme[key]]
    return hostsList[scheme['else']]

def isNotValuable(component):
  componentName = component["StackServiceComponents"]["component_name"]
  service = ['JOURNALNODE', 'ZKFC', 'APP_TIMELINE_SERVER', 'GANGLIA_MONITOR']
  return componentName in service

def isAlreadyPopulated(component):
  if component["StackServiceComponents"]["hostnames"] is not None:
    return len(component["StackServiceComponents"]["hostnames"]) > 0
  return False

def isClient(component):
  return component["StackServiceComponents"]["component_category"] == 'CLIENT'

def isSlave(component):
  componentName = component["StackServiceComponents"]["component_name"]
  isSlave = component["StackServiceComponents"]["component_category"] == 'SLAVE'
  return isSlave

def isMaster(component):
  componentName = component["StackServiceComponents"]["component_name"]
  isMaster = component["StackServiceComponents"]["is_master"]
  return isMaster

def isLocalHost(hostName):
  return socket.getfqdn(hostName) == socket.getfqdn()

def isNotPreferableOnAmbariServerHost(component):
  componentName = component["StackServiceComponents"]["component_name"]
  service = ['STORM_UI_SERVER', 'DRPC_SERVER', 'STORM_REST_API', 'NIMBUS', 'GANGLIA_SERVER', 'NAGIOS_SERVER', 'HUE_SERVER']
  return componentName in service

def isMasterWithMultipleInstances(component):
  componentName = component["StackServiceComponents"]["component_name"]
  masters = ['ZOOKEEPER_SERVER', 'HBASE_MASTER']
  return componentName in masters

def defaultNoOfMasterHosts(component):
  componentName = component["StackServiceComponents"]["component_name"]
  return cardinality(componentName)[min]


# Helper dictionaries
def cardinality(componentName):
  return {
    'ZOOKEEPER_SERVER': {min: 3},
    'HBASE_MASTER': {min: 1},
    }.get(componentName, {min:1, max:1})

def selectionScheme(componentName):
  return {
    'NAMENODE': {"else": 0},
    'SECONDARY_NAMENODE': {"else": 1},
    'HBASE_MASTER': {6: 0, 31: 2, "else": 3},

    'JOBTRACKER': {31: 1, "else": 2},
    'HISTORYSERVER': {31: 1, "else": 2},
    'RESOURCEMANAGER': {31: 1, "else": 2},
    'APP_TIMELINE_SERVER': {31: 1, "else": 2},

    'OOZIE_SERVER': {6: 1, 31: 2, "else": 3},
    'FALCON_SERVER': {6: 1, 31: 2, "else": 3},

    'HIVE_SERVER': {6: 1, 31: 2, "else": 4},
    'HIVE_METASTORE': {6: 1, 31: 2, "else": 4},
    'WEBHCAT_SERVER': {6: 1, 31: 2, "else": 4},
    }.get(componentName, {"else": 0})

