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

import imp
import os
import re
import socket
import traceback

class StackAdvisor(object):
  """
  Abstract class implemented by all stack advisors. Stack advisors advise on stack specific questions. 

  Currently stack advisors provide following abilities:
  - Recommend where services should be installed in cluster
  - Recommend configurations based on host hardware
  - Validate user selection of where services are installed on cluster
  - Validate user configuration values 

  Each of the above methods is passed in parameters about services and hosts involved as described below.

    @type services: dictionary
    @param services: Dictionary containing all information about services selected by the user. 
      Example: {
      "services": [
        {
          "StackServices": {
            "service_name" : "HDFS",
            "service_version" : "2.6.0.2.2",
          },
          "components" : [ 
            {
              "StackServiceComponents" : {
                "cardinality" : "1+",
                "component_category" : "SLAVE",
                "component_name" : "DATANODE",
                "display_name" : "DataNode",
                "service_name" : "HDFS",
                "hostnames" : []
              },
              "dependencies" : []
            }, {
              "StackServiceComponents" : {
                "cardinality" : "1-2",
                "component_category" : "MASTER",
                "component_name" : "NAMENODE",
                "display_name" : "NameNode",
                "service_name" : "HDFS",
                "hostnames" : []
              },
              "dependencies" : []
            },
            ...
          ]
        },
        ...
      ]
    }
  @type hosts: dictionary
  @param hosts: Dictionary containing all information about hosts in this cluster
    Example: {
      "items": [
        {
          Hosts: {
            "host_name": "c6401.ambari.apache.org",
            "public_host_name" : "c6401.ambari.apache.org",
            "ip": "192.168.1.101",
            "cpu_count" : 1,
            "disk_info" : [
              {
              "available" : "4564632",
              "used" : "5230344",
              "percent" : "54%",
              "size" : "10319160",
              "type" : "ext4",
              "mountpoint" : "/"
              },
              {
              "available" : "1832436",
              "used" : "0",
              "percent" : "0%",
              "size" : "1832436",
              "type" : "tmpfs",
              "mountpoint" : "/dev/shm"
              }
            ],
            "host_state" : "HEALTHY",
            "os_arch" : "x86_64",
            "os_type" : "centos6",
            "total_mem" : 3664872
          }
        },
        ...
      ]
    }

    Each of the methods can either return recommendations or validations.

    Recommendations are made in a Ambari Blueprints friendly format. 
    Validations are an array of validation objects.
  """

  def recommendComponentLayout(self, services, hosts):
    """
    Returns recommendation of which hosts various service components should be installed on.

    This function takes as input all details about services being installed, and hosts
    they are being installed into, to generate hostname assignments to various components
    of each service.

    @type services: dictionary
    @param services: Dictionary containing all information about services selected by the user.
    @type hosts: dictionary
    @param hosts: Dictionary containing all information about hosts in this cluster
    @rtype: dictionary
    @return: Layout recommendation of service components on cluster hosts in Ambari Blueprints friendly format. 
        Example: {
          "resources" : [
            {
              "hosts" : [
                "c6402.ambari.apache.org",
                "c6401.ambari.apache.org"
              ],
              "services" : [
                "HDFS"
              ],
              "recommendations" : {
                "blueprint" : {
                  "host_groups" : [
                    {
                      "name" : "host-group-2",
                      "components" : [
                        { "name" : "JOURNALNODE" },
                        { "name" : "ZKFC" },
                        { "name" : "DATANODE" },
                        { "name" : "SECONDARY_NAMENODE" }
                      ]
                    },
                    {
                      "name" : "host-group-1",
                      "components" : [
                        { "name" : "HDFS_CLIENT" },
                        { "name" : "NAMENODE" },
                        { "name" : "JOURNALNODE" },
                        { "name" : "ZKFC" },
                        { "name" : "DATANODE" }
                      ]
                    }
                  ]
                },
                "blueprint_cluster_binding" : {
                  "host_groups" : [
                    {
                      "name" : "host-group-1",
                      "hosts" : [ { "fqdn" : "c6401.ambari.apache.org" } ]
                    },
                    {
                      "name" : "host-group-2",
                      "hosts" : [ { "fqdn" : "c6402.ambari.apache.org" } ]
                    }
                  ]
                }
              }
            }
          ]
        }
    """
    pass

  def validateComponentLayout(self, services, hosts):
    """
    Returns array of Validation issues with service component layout on hosts

    This function takes as input all details about services being installed along with
    hosts the components are being installed on (hostnames property is populated for 
    each component).  

    @type services: dictionary
    @param services: Dictionary containing information about services and host layout selected by the user.
    @type hosts: dictionary
    @param hosts: Dictionary containing all information about hosts in this cluster
    @rtype: dictionary
    @return: Dictionary containing array of validation items
        Example: {
          "items": [
            {
              "type" : "host-group",
              "level" : "ERROR",
              "message" : "NameNode and Secondary NameNode should not be hosted on the same machine",
              "component-name" : "NAMENODE",
              "host" : "c6401.ambari.apache.org" 
            },
            ...
          ]
        }  
    """
    pass

  def recommendConfigurations(self, services, hosts):
    """
    Returns recommendation of service configurations based on host-specific layout of components.

    This function takes as input all details about services being installed, and hosts
    they are being installed into, to recommend host-specific configurations.

    @type services: dictionary
    @param services: Dictionary containing all information about services and component layout selected by the user.
    @type hosts: dictionary
    @param hosts: Dictionary containing all information about hosts in this cluster
    @rtype: dictionary
    @return: Layout recommendation of service components on cluster hosts in Ambari Blueprints friendly format. 
        Example: {
         "services": [
          "HIVE", 
          "TEZ", 
          "YARN"
         ], 
         "recommendations": {
          "blueprint": {
           "host_groups": [], 
           "configurations": {
            "yarn-site": {
             "properties": {
              "yarn.scheduler.minimum-allocation-mb": "682", 
              "yarn.scheduler.maximum-allocation-mb": "2048", 
              "yarn.nodemanager.resource.memory-mb": "2048"
             }
            }, 
            "tez-site": {
             "properties": {
              "tez.am.java.opts": "-server -Xmx546m -Djava.net.preferIPv4Stack=true -XX:+UseNUMA -XX:+UseParallelGC", 
              "tez.am.resource.memory.mb": "682"
             }
            }, 
            "hive-site": {
             "properties": {
              "hive.tez.container.size": "682", 
              "hive.tez.java.opts": "-server -Xmx546m -Djava.net.preferIPv4Stack=true -XX:NewRatio=8 -XX:+UseNUMA -XX:+UseParallelGC", 
              "hive.auto.convert.join.noconditionaltask.size": "238026752"
             }
            }
           }
          }, 
          "blueprint_cluster_binding": {
           "host_groups": []
          }
         }, 
         "hosts": [
          "c6401.ambari.apache.org", 
          "c6402.ambari.apache.org", 
          "c6403.ambari.apache.org" 
         ] 
        }
    """
    pass

  def validateConfigurations(self, services, hosts):
    """"
    Returns array of Validation issues with configurations provided by user

    This function takes as input all details about services being installed along with
    configuration values entered by the user. These configurations can be validated against
    service requirements, or host hardware to generate validation issues. 

    @type services: dictionary
    @param services: Dictionary containing information about services and user configurations.
    @type hosts: dictionary
    @param hosts: Dictionary containing all information about hosts in this cluster
    @rtype: dictionary
    @return: Dictionary containing array of validation items
        Example: {
         "items": [
          {
           "config-type": "yarn-site", 
           "message": "Value is less than the recommended default of 682", 
           "type": "configuration", 
           "config-name": "yarn.scheduler.minimum-allocation-mb", 
           "level": "WARN"
          }
         ]
       }
    """
    pass


class DefaultStackAdvisor(StackAdvisor):
  """
  Default stack advisor implementation.
  
  This implementation is used when a stack-version, or its hierarchy does not
  have an advisor. Stack-versions can extend this class to provide their own
  implement
  """

  def __init__(self):
    self.services = None
    # Dictionary that maps serviceName or componentName to serviceAdvisor
    self.serviceAdvisorsDict = {}


  def getActiveHosts(self, hosts):
    """ Filters the list of specified hosts object and returns
        a list of hosts which are not in maintenance mode. """
    hostsList = []
    if hosts is not None:
      hostsList = [host['host_name'] for host in hosts
                   if host.get('maintenance_state') is None or host.get('maintenance_state') == "OFF"]
    return hostsList

  def getServiceAdvisor(self, key):
    if len(self.serviceAdvisorsDict) == 0:
      self.loadServiceAdvisors()
    return self.serviceAdvisorsDict[key]

  def loadServiceAdvisors(self):
    for service in self.services["services"]:
      serviceName = service["StackServices"]["service_name"]
      self.serviceAdvisorsDict[serviceName] = self.instantiateServiceAdvisor(service)
      for component in service["components"]:
        componentName = self.getComponentName(component)
        self.serviceAdvisorsDict[componentName] = self.serviceAdvisorsDict[serviceName]

  def instantiateServiceAdvisor(self, service):

    serviceName = service["StackServices"]["service_name"]
    className = service["StackServices"]["advisor_name"] if "advisor_name" in service["StackServices"] else None
    path = service["StackServices"]["advisor_path"] if "advisor_path" in service["StackServices"] else None

    if path is not None and os.path.exists(path) and className is not None:
      try:
        with open(path, 'rb') as fp:
          serviceAdvisor = imp.load_module('service_advisor_impl', fp, path, ('.py', 'rb', imp.PY_SOURCE))
        if hasattr(serviceAdvisor, className):
          print "ServiceAdvisor implementation for service {0} was loaded".format(serviceName)
          return getattr(serviceAdvisor, className)()
        else:
          print "Failed to load or create ServiceAdvisor implementation for service {0}: " \
                "Expecting class name {1} but it was not found.".format(serviceName, className)
      except Exception as e:
        traceback.print_exc()
        print "Failed to load or create ServiceAdvisor implementation for service {0}".format(serviceName)

    return None

  def recommendComponentLayout(self, services, hosts):
    """Returns Services object with hostnames array populated for components"""

    stackName = services["Versions"]["stack_name"]
    stackVersion = services["Versions"]["stack_version"]
    hostsList = self.getActiveHosts([host["Hosts"] for host in hosts["items"]])
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

    layoutRecommendations = self.createComponentLayoutRecommendations(services, hosts)

    recommendations = {
      "Versions": {"stack_name": stackName, "stack_version": stackVersion},
      "hosts": hostsList,
      "services": servicesList,
      "recommendations": layoutRecommendations
    }

    return recommendations

  def createComponentLayoutRecommendations(self, services, hosts):
    self.services = services

    recommendations = {
      "blueprint": {
        "host_groups": [ ]
      },
      "blueprint_cluster_binding": {
        "host_groups": [ ]
      }
    }

    hostsList = self.getActiveHosts([host["Hosts"] for host in hosts["items"]])

    # for fast lookup
    hostsSet = set(hostsList)

    hostsComponentsMap = {}
    for hostName in hostsList:
      if hostName not in hostsComponentsMap:
        hostsComponentsMap[hostName] = []

    #extend hostsComponentsMap' with MASTER components
    for service in services["services"]:
      masterComponents = [component for component in service["components"] if self.isMasterComponent(component)]
      serviceName = service["StackServices"]["service_name"]
      serviceAdvisor = self.getServiceAdvisor(serviceName)
      for component in masterComponents:
        componentName = component["StackServiceComponents"]["component_name"]
        advisor = serviceAdvisor if serviceAdvisor is not None else self
        hostsForComponent = advisor.getHostsForMasterComponent(services, hosts, component, hostsList)

        #extend 'hostsComponentsMap' with 'hostsForComponent'
        for hostName in hostsForComponent:
          if hostName in hostsSet:
            hostsComponentsMap[hostName].append( { "name":componentName } )

    #extend 'hostsComponentsMap' with Slave and Client Components
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item for sublist in componentsListList for item in sublist]
    usedHostsListList = [component["StackServiceComponents"]["hostnames"] for component in componentsList if not self.isComponentNotValuable(component)]
    utilizedHosts = [item for sublist in usedHostsListList for item in sublist]
    freeHosts = [hostName for hostName in hostsList if hostName not in utilizedHosts]

    for service in services["services"]:
      slaveClientComponents = [component for component in service["components"]
                               if self.isSlaveComponent(component) or self.isClientComponent(component)]
      serviceName = service["StackServices"]["service_name"]
      serviceAdvisor = self.getServiceAdvisor(serviceName)
      for component in slaveClientComponents:
        componentName = component["StackServiceComponents"]["component_name"]
        advisor = serviceAdvisor if serviceAdvisor is not None else self
        hostsForComponent = advisor.getHostsForSlaveComponent(services, hosts, component, hostsList, freeHosts)

        #extend 'hostsComponentsMap' with 'hostsForComponent'
        for hostName in hostsForComponent:
          if hostName not in hostsComponentsMap and hostName in hostsSet:
            hostsComponentsMap[hostName] = []
          if hostName in hostsSet:
            hostsComponentsMap[hostName].append( { "name": componentName } )

    #colocate custom services
    for service in services["services"]:
      serviceName = service["StackServices"]["service_name"]
      serviceAdvisor = self.getServiceAdvisor(serviceName)
      if serviceAdvisor is not None:
        serviceComponents = [component for component in service["components"]]
        serviceAdvisor.colocateService(hostsComponentsMap, serviceComponents)

    #prepare 'host-group's from 'hostsComponentsMap'
    host_groups = recommendations["blueprint"]["host_groups"]
    bindings = recommendations["blueprint_cluster_binding"]["host_groups"]
    index = 0
    for key in hostsComponentsMap.keys():
      index += 1
      host_group_name = "host-group-{0}".format(index)
      host_groups.append( { "name": host_group_name, "components": hostsComponentsMap[key] } )
      bindings.append( { "name": host_group_name, "hosts": [{ "fqdn": key }] } )

    return recommendations

  def getHostsForMasterComponent(self, services, hosts, component, hostsList):
    if self.isComponentHostsPopulated(component):
      return component["StackServiceComponents"]["hostnames"]

    if len(hostsList) > 1 and self.isMasterComponentWithMultipleInstances(component):
      hostsCount = self.getMinComponentCount(component)
      if hostsCount > 1: # get first 'hostsCount' available hosts
        hostsForComponent = []
        hostIndex = 0
        while hostsCount > len(hostsForComponent) and hostIndex < len(hostsList):
          currentHost = hostsList[hostIndex]
          if self.isHostSuitableForComponent(currentHost, component):
            hostsForComponent.append(currentHost)
          hostIndex += 1
        return hostsForComponent

    return [self.getHostForComponent(component, hostsList)]

  def getHostsForSlaveComponent(self, services, hosts, component, hostsList, freeHosts):
    if component["StackServiceComponents"]["cardinality"] == "ALL":
      return hostsList

    if self.isComponentHostsPopulated(component):
      return component["StackServiceComponents"]["hostnames"]

    hostsForComponent = []
    componentName = component["StackServiceComponents"]["component_name"]
    if self.isSlaveComponent(component):
      cardinality = str(component["StackServiceComponents"]["cardinality"])
      hostsMin, hostsMax = self.parseCardinality(cardinality, len(hostsList))
      hostsMin, hostsMax = (0 if hostsMin is None else hostsMin, len(hostsList) if hostsMax is None else hostsMax)
      if self.isComponentUsingCardinalityForLayout(componentName) and cardinality:
        if hostsMin > len(hostsForComponent):
          hostsForComponent.extend(freeHosts[0:hostsMin-len(hostsForComponent)])

      else:
        hostsForComponent.extend(freeHosts)
        if not hostsForComponent:  # hostsForComponent is empty
          hostsForComponent = hostsList[-1:]
      hostsForComponent = list(set(hostsForComponent))  # removing duplicates
      if len(hostsForComponent) < hostsMin:
        hostsForComponent = list(set(hostsList))[0:hostsMin]
      elif len(hostsForComponent) > hostsMax:
        hostsForComponent = list(set(hostsList))[0:hostsMax]
    elif self.isClientComponent(component):
      hostsForComponent = freeHosts[0:1]
      if not hostsForComponent:  # hostsForComponent is empty
        hostsForComponent = hostsList[-1:]

    return hostsForComponent

  def isComponentUsingCardinalityForLayout(self, componentName):
    return False

  def createValidationResponse(self, services, validationItems):
    """Returns array of Validation objects about issues with hostnames components assigned to"""
    stackName = services["Versions"]["stack_name"]
    stackVersion = services["Versions"]["stack_version"]

    validations = {
      "Versions": {"stack_name": stackName, "stack_version": stackVersion},
      "items": validationItems
    }

    return validations

  def validateComponentLayout(self, services, hosts):
    """Returns array of Validation objects about issues with hostnames components assigned to"""
    validationItems = self.getComponentLayoutValidations(services, hosts)
    return self.createValidationResponse(services, validationItems)

  def validateConfigurations(self, services, hosts):
    """Returns array of Validation objects about issues with hostnames components assigned to"""
    self.services = services

    validationItems = self.getConfigurationsValidationItems(services, hosts)
    return self.createValidationResponse(services, validationItems)

  def getComponentLayoutValidations(self, services, hosts):
    self.services = services

    items = []
    if services is None:
      return items

    for service in services["services"]:
      serviceName = service["StackServices"]["service_name"]
      serviceAdvisor = self.getServiceAdvisor(serviceName)
      if serviceAdvisor is not None:
        items.extend(serviceAdvisor.getServiceComponentLayoutValidations(services, hosts))

    return items

  def getConfigurationClusterSummary(self, servicesList, hosts, components, services):
    return {}

  def validateClusterConfigurations(self, configurations, services, hosts):
    validationItems = []

    return self.toConfigurationValidationProblems(validationItems, "")

  def toConfigurationValidationProblems(self, validationProblems, siteName):
    result = []
    for validationProblem in validationProblems:
      validationItem = validationProblem.get("item", None)
      if validationItem is not None:
        problem = {"type": 'configuration', "level": validationItem["level"], "message": validationItem["message"],
                   "config-type": siteName, "config-name": validationProblem["config-name"] }
        result.append(problem)
    return result

  def validateServiceConfigurations(self, serviceName):
    return self.getServiceConfigurationValidators().get(serviceName, None)

  def getServiceConfigurationValidators(self):
    return {}

  def validateMinMax(self, items, recommendedDefaults, configurations):

    # required for casting to the proper numeric type before comparison
    def convertToNumber(number):
      try:
        return int(number)
      except ValueError:
        return float(number)

    for configName in configurations:
      validationItems = []
      if configName in recommendedDefaults and "property_attributes" in recommendedDefaults[configName]:
        for propertyName in recommendedDefaults[configName]["property_attributes"]:
          if propertyName in configurations[configName]["properties"]:
            if "maximum" in recommendedDefaults[configName]["property_attributes"][propertyName] and \
                propertyName in recommendedDefaults[configName]["properties"]:
              userValue = convertToNumber(configurations[configName]["properties"][propertyName])
              maxValue = convertToNumber(recommendedDefaults[configName]["property_attributes"][propertyName]["maximum"])
              if userValue > maxValue:
                validationItems.extend([{"config-name": propertyName, "item": self.getWarnItem("Value is greater than the recommended maximum of {0} ".format(maxValue))}])
            if "minimum" in recommendedDefaults[configName]["property_attributes"][propertyName] and \
                    propertyName in recommendedDefaults[configName]["properties"]:
              userValue = convertToNumber(configurations[configName]["properties"][propertyName])
              minValue = convertToNumber(recommendedDefaults[configName]["property_attributes"][propertyName]["minimum"])
              if userValue < minValue:
                validationItems.extend([{"config-name": propertyName, "item": self.getWarnItem("Value is less than the recommended minimum of {0} ".format(minValue))}])
      items.extend(self.toConfigurationValidationProblems(validationItems, configName))
    pass


  def getConfigurationsValidationItems(self, services, hosts):
    """Returns array of Validation objects about issues with configuration values provided in services"""
    items = []

    recommendations = self.recommendConfigurations(services, hosts)
    recommendedDefaults = recommendations["recommendations"]["blueprint"]["configurations"]
    configurations = services["configurations"]

    for service in services["services"]:
      items.extend(self.getConfigurationsValidationItemsForService(configurations, recommendedDefaults, service, services, hosts))

    clusterWideItems = self.validateClusterConfigurations(configurations, services, hosts)
    items.extend(clusterWideItems)
    self.validateMinMax(items, recommendedDefaults, configurations)
    return items

  def validateConfigurationsForSite(self, configurations, recommendedDefaults, services, hosts, siteName, method):
    if siteName in recommendedDefaults:
      siteProperties = self.getSiteProperties(configurations, siteName)
      if siteProperties is not None:
        siteRecommendations = recommendedDefaults[siteName]["properties"]
        print("SiteName: %s, method: %s" % (siteName, method.__name__))
        print("Site properties: %s" % str(siteProperties))
        print("Recommendations: %s" % str(siteRecommendations))
        return method(siteProperties, siteRecommendations, configurations, services, hosts)
    return []

  def getConfigurationsValidationItemsForService(self, configurations, recommendedDefaults, service, services, hosts):
    items = []
    serviceName = service["StackServices"]["service_name"]
    validator = self.validateServiceConfigurations(serviceName)
    if validator is not None:
      for siteName, method in validator.items():
        resultItems = self.validateConfigurationsForSite(configurations, recommendedDefaults, services, hosts, siteName, method)
        items.extend(resultItems)

    serviceAdvisor = self.getServiceAdvisor(serviceName)
    if serviceAdvisor is not None:
      items.extend(serviceAdvisor.getServiceConfigurationsValidationItems(configurations, recommendedDefaults, services, hosts))

    return items

  def recommendConfigGroupsConfigurations(self, recommendations, services, components, hosts,
                            servicesList):
    recommendations["recommendations"]["config-groups"] = []
    for configGroup in services["config-groups"]:

      # Override configuration with the config group values
      cgServices = services.copy()
      for configName in configGroup["configurations"].keys():
        if configName in cgServices["configurations"]:
          cgServices["configurations"][configName]["properties"].update(
            configGroup["configurations"][configName]['properties'])
        else:
          cgServices["configurations"][configName] = \
          configGroup["configurations"][configName]

      # Override hosts with the config group hosts
      cgHosts = {"items": [host for host in hosts["items"] if
                           host["Hosts"]["host_name"] in configGroup["hosts"]]}

      # Override clusterSummary
      cgClusterSummary = self.getConfigurationClusterSummary(servicesList,
                                                             cgHosts,
                                                             components,
                                                             cgServices)

      configurations = {}

      for service in services["services"]:
        serviceName = service["StackServices"]["service_name"]
        calculation = self.getServiceConfigurationRecommender(serviceName)
        if calculation is not None:
          calculation(configurations, cgClusterSummary, cgServices, cgHosts)
        else:
          serviceAdvisor = self.getServiceAdvisor(serviceName)
          if serviceAdvisor is not None:
            serviceAdvisor.getServiceConfigurationRecommendations(configurations, cgClusterSummary, cgServices, cgHosts)

      cgRecommendation = {
        "configurations": {},
        "dependent_configurations": {},
        "hosts": configGroup["hosts"]
      }

      recommendations["recommendations"]["config-groups"].append(
        cgRecommendation)

      # Parse results.
      for config in configurations.keys():
        cgRecommendation["configurations"][config] = {}
        cgRecommendation["dependent_configurations"][config] = {}
        # property + property_attributes
        for configElement in configurations[config].keys():
          cgRecommendation["configurations"][config][configElement] = {}
          cgRecommendation["dependent_configurations"][config][
            configElement] = {}
          for property, value in configurations[config][configElement].items():
            if config in configGroup["configurations"]:
              cgRecommendation["configurations"][config][configElement][
                property] = value
            else:
              cgRecommendation["dependent_configurations"][config][
                configElement][property] = value

  def recommendConfigurations(self, services, hosts):
    self.services = services

    stackName = services["Versions"]["stack_name"]
    stackVersion = services["Versions"]["stack_version"]
    hostsList = [host["Hosts"]["host_name"] for host in hosts["items"]]
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    components = [component["StackServiceComponents"]["component_name"]
                  for service in services["services"]
                  for component in service["components"]]

    clusterSummary = self.getConfigurationClusterSummary(servicesList, hosts, components, services)

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

    # If recommendation for config groups
    if "config-groups" in services:
      self.recommendConfigGroupsConfigurations(recommendations, services, components, hosts,
                                 servicesList)
    else:
      configurations = recommendations["recommendations"]["blueprint"]["configurations"]

      for service in services["services"]:
        serviceName = service["StackServices"]["service_name"]
        calculation = self.getServiceConfigurationRecommender(serviceName)
        if calculation is not None:
          calculation(configurations, clusterSummary, services, hosts)
        else:
          serviceAdvisor = self.getServiceAdvisor(serviceName)
          if serviceAdvisor is not None:
            serviceAdvisor.getServiceConfigurationRecommendations(configurations, clusterSummary, services, hosts)

    return recommendations

  def getServiceConfigurationRecommender(self, service):
    return self.getServiceConfigurationRecommenderDict().get(service, None)

  def getServiceConfigurationRecommenderDict(self):
    return {}

  # Recommendation helper methods
  def isComponentHostsPopulated(self, component):
    hostnames = self.getComponentAttribute(component, "hostnames")
    if hostnames is not None:
      return len(hostnames) > 0
    return False

  def isClientComponent(self, component):
    return self.getComponentAttribute(component, "component_category") == 'CLIENT'

  def isSlaveComponent(self, component):
    return self.getComponentAttribute(component, "component_category") == 'SLAVE'

  def isMasterComponent(self, component):
    return self.getComponentAttribute(component, "is_master")

  def getComponentAttribute(self, component, attribute):
    serviceComponent = component.get("StackServiceComponents", None)
    if serviceComponent is None:
      return None
    return serviceComponent.get(attribute, None)

  def isLocalHost(self, hostName):
    return socket.getfqdn(hostName) == socket.getfqdn()

  def isMasterComponentWithMultipleInstances(self, component):
    componentName = self.getComponentName(component)
    masters = self.getMastersWithMultipleInstances()
    return componentName in masters

  def isComponentNotValuable(self, component):
    componentName = self.getComponentName(component)
    service = self.getNotValuableComponents()
    return componentName in service

  def getMinComponentCount(self, component):
    componentName = self.getComponentName(component)
    return self.getComponentCardinality(componentName)["min"]

  # Helper dictionaries
  def getComponentCardinality(self, componentName):
    return self.getCardinalitiesDict().get(componentName, {"min": 1, "max": 1})

  def getHostForComponent(self, component, hostsList):
    if len(hostsList) == 0:
      return None

    componentName = self.getComponentName(component)

    if len(hostsList) != 1:
      scheme = self.getComponentLayoutSchemes().get(componentName, None)
      if scheme is not None:
        hostIndex = next((index for key, index in scheme.iteritems() if isinstance(key, ( int, long )) and len(hostsList) < key), scheme['else'])
      else:
        hostIndex = 0
      for host in hostsList[hostIndex:]:
        if self.isHostSuitableForComponent(host, component):
          return host
    return hostsList[0]

  def getComponentName(self, component):
    return self.getComponentAttribute(component, "component_name")

  def isHostSuitableForComponent(self, host, component):
    return not (self.getComponentName(component) in self.getNotPreferableOnServerComponents() and self.isLocalHost(host))

  def getMastersWithMultipleInstances(self):
    return []

  def getNotValuableComponents(self):
    return []

  def getNotPreferableOnServerComponents(self):
    return []

  def getCardinalitiesDict(self):
    return {}

  def getComponentLayoutSchemes(self):
    """
    Provides layout scheme dictionaries for components.

    The scheme dictionary basically maps the number of hosts to
    host index where component should exist.
    """
    return {}

  """
  Utility method used for validation warnings.
  """
  def getWarnItem(self, message):
    return {"level": "WARN", "message": message}

  """
  Utility method used for validation errors.
  """
  def getErrorItem(self, message):
    return {"level": "ERROR", "message": message}

  def getComponentHostNames(self, servicesDict, serviceName, componentName):
    for service in servicesDict["services"]:
      if service["StackServices"]["service_name"] == serviceName:
        for component in service['components']:
          if component["StackServiceComponents"]["component_name"] == componentName:
            return component["StackServiceComponents"]["hostnames"]

  def recommendConfigurationDependencies(self, services, hosts):
    result = self.recommendConfigurations(services, hosts)
    return self.filterResult(result, services)

  # returns recommendations only for changed and depended properties
  def filterResult(self, result, services):
    allRequestedProperties = self.getAllRequestedProperties(services)
    self.filterConfigs(result['recommendations']['blueprint']['configurations'], allRequestedProperties)
    if "config-groups" in services:
      for configGroup in result['recommendations']["config-groups"]:
        self.filterConfigs(configGroup["configurations"], allRequestedProperties)
        self.filterConfigs(configGroup["dependent_configurations"], allRequestedProperties)
    return result

  def filterConfigs(self, configs, requestedProperties):

    filteredConfigs = {}
    for type, names in configs.items():
      for name in names['properties']:
        if type in requestedProperties.keys() and \
                name in requestedProperties[type]:
          if type not in filteredConfigs.keys():
            filteredConfigs[type] = {'properties': {}}
          filteredConfigs[type]['properties'][name] = \
            configs[type]['properties'][name]
      if 'property_attributes' in names.keys():
        for name in names['property_attributes']:
          if type in requestedProperties.keys() and \
                  name in requestedProperties[type]:
            if type not in filteredConfigs.keys():
              filteredConfigs[type] = {'property_attributes': {}}
            elif 'property_attributes' not in filteredConfigs[type].keys():
              filteredConfigs[type]['property_attributes'] = {}
            filteredConfigs[type]['property_attributes'][name] = \
              configs[type]['property_attributes'][name]
    configs.clear()
    configs.update(filteredConfigs)

  def getAllRequestedProperties(self, services):
    affectedConfigs = self.getAffectedConfigs(services)
    allRequestedProperties = {}
    for config in affectedConfigs:
      if config['type'] in allRequestedProperties:
        allRequestedProperties[config['type']].append(config['name'])
      else:
        allRequestedProperties[config['type']] = [config['name']]
    return allRequestedProperties

  def getAffectedConfigs(self, services):
    """returns properties dict including changed-configurations and depended-by configs"""
    changedConfigs = services['changed-configurations']
    changedConfigs = [{"type": entry["type"], "name": entry["name"]} for entry in changedConfigs]
    allDependencies = []

    for item in services['services']:
      allDependencies.extend(item['configurations'])

    dependencies = []

    size = -1
    while size != len(dependencies):
      size = len(dependencies)
      for config in allDependencies:
        property = {
          "type": re.sub('\.xml$', '', config['StackConfigurations']['type']),
          "name": config['StackConfigurations']['property_name']
        }
        if property in dependencies or property in changedConfigs:
          for dependedConfig in config['dependencies']:
            dependency = {
              "name": dependedConfig["StackConfigurationDependency"]["dependency_name"],
              "type": dependedConfig["StackConfigurationDependency"]["dependency_type"]
            }
            if dependency not in dependencies:
              dependencies.append(dependency)

    if "forced-configurations" in services and services["forced-configurations"] is not None:
      dependencies.extend(services["forced-configurations"])
    return  dependencies

  def versionCompare(self, version1, version2):
    def normalize(v):
      return [int(x) for x in re.sub(r'(\.0+)*$','', v).split(".")]
    return cmp(normalize(version1), normalize(version2))
  pass

  def getSiteProperties(self, configurations, siteName):
    siteConfig = configurations.get(siteName)
    if siteConfig is None:
      return None
    return siteConfig.get("properties")

  def putProperty(self, config, configType, services=None):
    userConfigs = {}
    changedConfigs = []
    # if services parameter, prefer values, set by user
    if services:
      if 'configurations' in services.keys():
        userConfigs = services['configurations']
      if 'changed-configurations' in services.keys():
        changedConfigs = services["changed-configurations"]

    if configType not in config:
      config[configType] = {}
    if"properties" not in config[configType]:
      config[configType]["properties"] = {}
    def appendProperty(key, value):
      # If property exists in changedConfigs, do not override, use user defined property
      if self.__isPropertyInChangedConfigs(configType, key, changedConfigs):
        config[configType]["properties"][key] = userConfigs[configType]['properties'][key]
      else:
        config[configType]["properties"][key] = str(value)
    return appendProperty

  def __isPropertyInChangedConfigs(self, configType, propertyName, changedConfigs):
    for changedConfig in changedConfigs:
      if changedConfig['type']==configType and changedConfig['name']==propertyName:
        return True
    return False

  def updateProperty(self, config, configType, services=None):
    userConfigs = {}
    changedConfigs = []
    # if services parameter, prefer values, set by user
    if services:
      if 'configurations' in services.keys():
        userConfigs = services['configurations']
      if 'changed-configurations' in services.keys():
        changedConfigs = services["changed-configurations"]

    if configType not in config:
      config[configType] = {}
    if "properties" not in config[configType]:
      config[configType]["properties"] = {}

    def updatePropertyWithCallback(key, value, callback):
      # If property exists in changedConfigs, do not override, use user defined property
      if self.__isPropertyInChangedConfigs(configType, key, changedConfigs):
        config[configType]["properties"][key] = userConfigs[configType]['properties'][key]
      else:
        # Give the callback an empty string if the mapping doesn't exist
        current_value = ""
        if key in config[configType]["properties"]:
          current_value = config[configType]["properties"][key]

        config[configType]["properties"][key] = callback(current_value, value)

    return updatePropertyWithCallback

  def putPropertyAttribute(self, config, configType):
    if configType not in config:
      config[configType] = {}
    def appendPropertyAttribute(key, attribute, attributeValue):
      if "property_attributes" not in config[configType]:
        config[configType]["property_attributes"] = {}
      if key not in config[configType]["property_attributes"]:
        config[configType]["property_attributes"][key] = {}
      config[configType]["property_attributes"][key][attribute] = attributeValue if isinstance(attributeValue, list) else str(attributeValue)
    return appendPropertyAttribute


  """
  Returns the hosts which are running the given component.
  """
  def getHosts(self, componentsList, componentName):
    hostNamesList = [component["hostnames"] for component in componentsList if component["component_name"] == componentName]
    return hostNamesList[0] if len(hostNamesList) > 0 else []

  def isSecurityEnabled(self, services):
    """
    Determines if security is enabled by testing the value of cluster-env/security enabled.

    If the property exists and is equal to "true", then is it enabled; otherwise is it assumed to be
    disabled.

    :param services: the services structure containing the current configurations
    :return: true if security is enabled; otherwise false
    """
    return "cluster-env" in services["configurations"] \
           and "security_enabled" in services["configurations"]["cluster-env"]["properties"] \
           and services["configurations"]["cluster-env"]["properties"]["security_enabled"].lower() == "true"

  def parseCardinality(self, cardinality, hostsCount):
    """
    Cardinality types: 1+, 1-2, 1, ALL
    @return: a tuple: (minHosts, maxHosts) or (None, None) if cardinality string is invalid
    """
    if not cardinality:
      return (None, None)

    if "+" in cardinality:
      return (int(cardinality[:-1]), int(hostsCount))
    elif "-" in cardinality:
      nums = cardinality.split("-")
      return (int(nums[0]), int(nums[1]))
    elif "ALL" == cardinality:
      return (int(hostsCount), int(hostsCount))
    elif cardinality.isdigit():
      return (int(cardinality),int(cardinality))

    return (None, None)
