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

import socket

from stack_advisor import StackAdvisor

class HDP206StackAdvisor(StackAdvisor):

  def recommendComponentLayout(self, services, hosts):
    """Returns Services object with hostnames array populated for components"""
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
    for service in services["services"]:
      masterComponents = [component for component in service["components"] if isMaster(component)]
      for component in masterComponents:
        componentName = component["StackServiceComponents"]["component_name"]
        hostsForComponent = []

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
    utilizedHosts = hostsComponentsMap.keys()
    freeHosts = [hostName for hostName in hostsList if hostName not in utilizedHosts]

    for service in services["services"]:
      slaveClientComponents = [component for component in service["components"] if isSlave(component) or isClient(component)]
      for component in slaveClientComponents:
        componentName = component["StackServiceComponents"]["component_name"]
        hostsForComponent = []
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
    pass

  def recommendConfigurations(self, services, hosts):
    """Returns Services object with configurations object populated"""
    pass

  def validateConfigurations(self, services, hosts):
    """Returns array of Validation objects about issues with configuration values provided in services"""
    pass


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

def isClient(component):
  return component["StackServiceComponents"]["component_category"] == 'CLIENT'

def isSlave(component):
  componentName = component["StackServiceComponents"]["component_name"]
  isSlave = component["StackServiceComponents"]["component_category"] == 'SLAVE'
  return isSlave and componentName != 'APP_TIMELINE_SERVER'

def isMaster(component):
  componentName = component["StackServiceComponents"]["component_name"]
  isMaster = component["StackServiceComponents"]["is_master"]
  return isMaster or componentName == 'APP_TIMELINE_SERVER'

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

