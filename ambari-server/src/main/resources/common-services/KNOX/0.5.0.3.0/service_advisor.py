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

# Python imports
import imp
import os
import traceback
import re
import socket
import fnmatch
import xml.etree.ElementTree as ET


from resource_management.core.logger import Logger

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
STACKS_DIR = os.path.join(SCRIPT_DIR, '../../../stacks/')
PARENT_FILE = os.path.join(STACKS_DIR, 'service_advisor.py')

try:
  with open(PARENT_FILE, 'rb') as fp:
    service_advisor = imp.load_module('service_advisor', fp, PARENT_FILE, ('.py', 'rb', imp.PY_SOURCE))
except Exception as e:
  traceback.print_exc()
  print "Failed to load parent"

class KnoxServiceAdvisor(service_advisor.ServiceAdvisor):

  def __init__(self, *args, **kwargs):
    self.as_super = super(KnoxServiceAdvisor, self)
    self.as_super.__init__(*args, **kwargs)

    # Always call these methods
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
    pass

  def modifyNotValuableComponents(self):
    """
    Modify the set of components whose host assignment is based on other services.
    Must be overriden in child class.
    """
    # Nothing to do
    pass

  def modifyComponentsNotPreferableOnServer(self):
    """
    Modify the set of components that are not preferable on the server.
    Must be overriden in child class.
    """
    # Nothing to do
    pass

  def modifyComponentLayoutSchemes(self):
    """
    Modify layout scheme dictionaries for components.
    The scheme dictionary basically maps the number of hosts to
    host index where component should exist.
    Must be overriden in child class.
    """
    # Nothing to do
    pass

  def getServiceComponentLayoutValidations(self, services, hosts):
    """
    Get a list of errors.
    Must be overriden in child class.
    """

    return []

  def getServiceConfigurationRecommendations(self, configurations, clusterData, services, hosts):
    """
    Entry point.
    Must be overriden in child class.
    """
    #Logger.info("Class: %s, Method: %s. Recommending Service Configurations." %
    #            (self.__class__.__name__, inspect.stack()[0][3]))

    recommender = KnoxRecommender()
    recommender.recommendKnoxConfigurationsFromHDP22(configurations, clusterData, services, hosts)



  def getServiceConfigurationsValidationItems(self, configurations, recommendedDefaults, services, hosts):
    """
    Entry point.
    Validate configurations for the service. Return a list of errors.
    The code for this function should be the same for each Service Advisor.
    """
    #Logger.info("Class: %s, Method: %s. Validating Configurations." %
    #            (self.__class__.__name__, inspect.stack()[0][3]))

    validator = KnoxValidator()
    # Calls the methods of the validator using arguments,
    # method(siteProperties, siteRecommendations, configurations, services, hosts)
    return validator.validateListOfConfigUsingMethod(configurations, recommendedDefaults, services, hosts, validator.validators)



class KnoxRecommender(service_advisor.ServiceAdvisor):
  """
  Knox Recommender suggests properties when adding the service for the first time or modifying configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(KnoxRecommender, self)
    self.as_super.__init__(*args, **kwargs)


  def recommendKnoxConfigurationsFromHDP22(self, configurations, clusterData, services, hosts):
    if "ranger-env" in services["configurations"] and "ranger-knox-plugin-properties" in services["configurations"] and \
        "ranger-knox-plugin-enabled" in services["configurations"]["ranger-env"]["properties"]:
      putKnoxRangerPluginProperty = self.putProperty(configurations, "ranger-knox-plugin-properties", services)
      rangerEnvKnoxPluginProperty = services["configurations"]["ranger-env"]["properties"]["ranger-knox-plugin-enabled"]
      putKnoxRangerPluginProperty("ranger-knox-plugin-enabled", rangerEnvKnoxPluginProperty)

    if 'topology' in services["configurations"] and 'content' in services["configurations"]["topology"]["properties"]:
      putKnoxTopologyContent = self.putProperty(configurations, "topology", services)
      rangerPluginEnabled = ''
      if 'ranger-knox-plugin-properties' in configurations and 'ranger-knox-plugin-enabled' in  configurations['ranger-knox-plugin-properties']['properties']:
        rangerPluginEnabled = configurations['ranger-knox-plugin-properties']['properties']['ranger-knox-plugin-enabled']
      elif 'ranger-knox-plugin-properties' in services['configurations'] and 'ranger-knox-plugin-enabled' in services['configurations']['ranger-knox-plugin-properties']['properties']:
        rangerPluginEnabled = services['configurations']['ranger-knox-plugin-properties']['properties']['ranger-knox-plugin-enabled']

      # check if authorization provider already added
      topologyContent = services["configurations"]["topology"]["properties"]["content"]
      authorizationProviderExists = False
      authNameChanged = False
      root = ET.fromstring(topologyContent)
      if root is not None:
        gateway = root.find("gateway")
        if gateway is not None:
          for provider in gateway.findall('provider'):
            role = provider.find('role')
            if role is not None and role.text and role.text.lower() == "authorization":
              authorizationProviderExists = True

            name = provider.find('name')
            if name is not None and name.text == "AclsAuthz" and rangerPluginEnabled \
               and rangerPluginEnabled.lower() == "Yes".lower():
              newAuthName = "XASecurePDPKnox"
              authNameChanged = True
            elif name is not None and (((not rangerPluginEnabled) or rangerPluginEnabled.lower() != "Yes".lower()) \
               and name.text == 'XASecurePDPKnox'):
              newAuthName = "AclsAuthz"
              authNameChanged = True

            if authNameChanged:
              name.text = newAuthName
              putKnoxTopologyContent('content', ET.tostring(root))

            if authorizationProviderExists:
              break

      if not authorizationProviderExists:
        if root is not None:
          gateway = root.find("gateway")
          if gateway is not None:
            provider = ET.SubElement(gateway, 'provider')

            role = ET.SubElement(provider, 'role')
            role.text = "authorization"

            name = ET.SubElement(provider, 'name')
            if rangerPluginEnabled and rangerPluginEnabled.lower() == "Yes".lower():
              name.text = "XASecurePDPKnox"
            else:
              name.text = "AclsAuthz"

            enabled = ET.SubElement(provider, 'enabled')
            enabled.text = "true"

            #TODO add pretty format for newly added provider
            putKnoxTopologyContent('content', ET.tostring(root))




class KnoxValidator(service_advisor.ServiceAdvisor):
  """
  Knox Validator checks the correctness of properties whenever the service is first added or the user attempts to
  change configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(KnoxValidator, self)
    self.as_super.__init__(*args, **kwargs)

    self.validators = [("ranger-knox-plugin-properties", self.validateKnoxRangerPluginConfigurationsFromHDP22),
                       ]

  def validateKnoxRangerPluginConfigurationsFromHDP22(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = []
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    ranger_plugin_properties = self.getSiteProperties(configurations, "ranger-knox-plugin-properties")
    ranger_plugin_enabled = ranger_plugin_properties['ranger-knox-plugin-enabled'] if ranger_plugin_properties else 'No'
    if 'RANGER' in servicesList and ranger_plugin_enabled.lower() == 'yes':
      # ranger-hdfs-plugin must be enabled in ranger-env
      ranger_env = self.getServicesSiteProperties(services, 'ranger-env')
      if not ranger_env or not 'ranger-knox-plugin-enabled' in ranger_env or \
          ranger_env['ranger-knox-plugin-enabled'].lower() != 'yes':
        validationItems.append({"config-name": 'ranger-knox-plugin-enabled',
                                "item": self.getWarnItem(
                                  "ranger-knox-plugin-properties/ranger-knox-plugin-enabled must correspond ranger-env/ranger-knox-plugin-enabled")})
    return self.toConfigurationValidationProblems(validationItems, "ranger-knox-plugin-properties")





