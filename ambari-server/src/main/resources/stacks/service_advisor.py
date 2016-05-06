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

"""
The naming convention for ServiceAdvisor subclasses depends on whether they are
in common-services or are part of the stack version's services.

In common-services, the naming convention is <service_name><service_version>ServiceAdvisor.
In the stack, the naming convention is <stack_name><stack_version><service_name>ServiceAdvisor.

Unlike the StackAdvisor, the ServiceAdvisor does NOT provide any inheritance.
If you want to use inheritance to augment a previous version of a service's
advisor you can use the following code to dynamically load the previous advisor.
Some changes will be need to provide the correct path and class names.

  SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
  PARENT_DIR = os.path.join(SCRIPT_DIR, '../<old_version>')
  PARENT_FILE = os.path.join(PARENT_DIR, 'service_advisor.py')

  try:
    with open(PARENT_FILE, 'rb') as fp:
      service_advisor = imp.load_module('service_advisor', fp, PARENT_FILE, ('.py', 'rb', imp.PY_SOURCE))
  except Exception as e:
    traceback.print_exc()
    print "Failed to load parent"

  class <NewServiceAdvisorClassName>(service_advisor.<OldServiceAdvisorClassName>)

where the NewServiceAdvisorClassName and OldServiceAdvisorClassName follow the naming
convention listed above.

For examples see: common-services/HAWQ/2.0.0/service_advisor.py
and common-services/PXF/3.0.0/service_advisor.py
"""
class ServiceAdvisor(object):
  """
  Abstract class implemented by all service advisors.
  """

  """
  Provides a scheme for laying out a given component on different number of hosts.
  It should return a json object such as {6: 2, 31: 1, "else": 5}.  If a service
  contains more than one component, it may have a statement such as:

    if componentName == 'HAWQMASTER':
      return {6: 2, 31: 1, "else": 5}

    return None
  """
  def getComponentLayoutScheme(self, componentName):
    return None

  """
  Returns the cardinality of the component as a json object with min and max attributes.
  Example: {"min": 1, "max": 1}
  """
  def getComponentCardinality(self, componentName):
    return {"min": 1, "max": 1}

  """
  Returns True if the component should avoid being configured on the ambari server host.
  """
  def isComponentNotPreferableOnAmbariServerHost(self, componentName):
    return False

  """
  Return True if the component is not considered valuable, otherwise returns False.
  Hosts which are only running non valuable components are considered to be not used.
  """
  def isComponentNotValuable(self, component):
    return False

  """
  Returns True if the component should use its cardinatity to determine its layout.
  """
  def isComponentUsingCardinalityForLayout(self, componentName):
    return False

  """
  Returns True if the component is a master component with multiple instances.
  """
  def isMasterComponentWithMultipleInstances(self, component):
    return False

  """
  If any components of the service should be colocated with other services,
  this is where you should set up that layout.  Example:

    # colocate HAWQSEGMENT with DATANODE, if no hosts have been allocated for HAWQSEGMENT
    hawqSegment = [component for component in serviceComponents if component["StackServiceComponents"]["component_name"] == "HAWQSEGMENT"][0]
    if not stackAdvisor.isComponentHostsPopulated(hawqSegment):
      for hostName in hostsComponentsMap.keys():
        hostComponents = hostsComponentsMap[hostName]
        if {"name": "DATANODE"} in hostComponents and {"name": "HAWQSEGMENT"} not in hostComponents:
          hostsComponentsMap[hostName].append( { "name": "HAWQSEGMENT" } )
        if {"name": "DATANODE"} not in hostComponents and {"name": "HAWQSEGMENT"} in hostComponents:
          hostComponents.remove({"name": "HAWQSEGMENT"})
  """
  def colocateService(self, stackAdvisor, hostsComponentsMap, serviceComponents):
    pass

  """
  Any configuration recommendations for the service should be defined in this function.
  This should be similar to any of the recommendXXXXConfigurations functions in the stack_advisor.py
  such as recommendYARNConfigurations().
  """
  def getServiceConfigurationRecommendations(self, stackAdvisor, configurations, clusterSummary, services, hosts):
    pass

  """
  Returns an array of Validation objects about issues with the hostnames to which components are assigned.
  This should detect validation issues which are different than those the stack_advisor.py detects.
  The default validations are in stack_advisor.py getComponentLayoutValidations function.
  """
  def getComponentLayoutValidations(self, stackAdvisor, services, hosts):
    return []

  """
  Any configuration validations for the service should be defined in this function.
  This should be similar to any of the validateXXXXConfigurations functions in the stack_advisor.py
  such as validateHDFSConfigurations.
  """
  def getConfigurationsValidationItems(self, stackAdvisor, configurations, recommendedDefaults, services, hosts):
    return []

  """
  If the service needs to any processing differently than the stack_advisor.py getHostsForMasterComponents
  function, then this logic should be added in this function.
  """
  def getHostsForMasterComponent(self, stackAdvisor, services, hosts, component, hostsList, hostsComponentsMap):
    return stackAdvisor.getHostsForMasterComponent(services, hosts, component, hostsList, hostsComponentsMap)

  """
  If the service needs to any processing differently than the stack_advisor.py getHostsForSlaveComponents
  function, then this logic should be added in this function.
  """
  def getHostsForSlaveComponent(self, stackAdvisor, services, hosts, component, hostsList, hostsComponentsMap, freeHosts):
    return stackAdvisor.getHostsForSlaveComponent(services, hosts, component, hostsList, hostsComponentsMap, freeHosts)


  """
  Utility methods
  """

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

  """
  Returns the hosts which are running the given component.
  """
  def getHosts(self, componentsList, componentName):
    return [component["hostnames"] for component in componentsList if component["component_name"] == componentName][0]

  """
  Utility method for setting a configuration property value.
  """
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

  """
  Utility method to determine if the configuration property value has been changed.
  """
  def __isPropertyInChangedConfigs(self, configType, propertyName, changedConfigs):
    for changedConfig in changedConfigs:
      if changedConfig['type']==configType and changedConfig['name']==propertyName:
        return True
    return False

  """
  Utility method for setting a configuration property attribute.
  """
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
  Utility method to validate configuration settings for a given configuration file.
  This function will call the method provided.
  """
  def validateConfigurationsForSite(self, stackAdvisor, configurations, recommendedDefaults, services, hosts, siteName, method):
    if siteName in recommendedDefaults:
      siteProperties = self.getSiteProperties(configurations, siteName)
      if siteProperties is not None:
        siteRecommendations = recommendedDefaults[siteName]["properties"]
        print("SiteName: %s, method: %s\n" % (siteName, method.__name__))
        print("Site properties: %s\n" % str(siteProperties))
        print("Recommendations: %s\n********\n" % str(siteRecommendations))
        return method(stackAdvisor, siteProperties, siteRecommendations, configurations, services, hosts)
    return []

  """
  Utility method used to return all the properties from a given configuration file.
  """
  def getSiteProperties(self, configurations, siteName):
    siteConfig = configurations.get(siteName)
    if siteConfig is None:
      return None
    return siteConfig.get("properties")
