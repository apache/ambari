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
import inspect

# Local imports
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


class ZookeeperServiceAdvisor(service_advisor.ServiceAdvisor):

  def __init__(self, *args, **kwargs):
    self.as_super = super(ZookeeperServiceAdvisor, self)
    self.as_super.__init__(*args, **kwargs)

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
    self.mastersWithMultipleInstances.add("ZOOKEEPER_SERVER")

  def modifyCardinalitiesDict(self):
    """
    Modify the dictionary of cardinalities.
    Must be overriden in child class.
    """
    self.cardinalitiesDict["ZOOKEEPER_SERVER"] = {"min": 3}

  def modifyHeapSizeProperties(self):
    """
    Modify the dictionary of heap size properties.
    Must be overriden in child class.
    """
    self.heap_size_properties = {"ZOOKEEPER_SERVER": [{"config-name": "zookeeper-env",
                                                       "property": "zk_server_heapsize",
                                                       "default": "1024m"}]}

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
    Get a list of errors. Zookeeper does not have any validations in this version.
    """
    Logger.info("Class: %s, Method: %s. Validating Service Component Layout." %
                (self.__class__.__name__, inspect.stack()[0][3]))
    return self.as_super.getServiceComponentLayoutValidations(services, hosts)

  def getServiceConfigurationRecommendations(self, configurations, clusterData, services, hosts):
    """
    Recommend configurations to set. Zookeeper does not have any recommendations in this version.
    """
    Logger.info("Class: %s, Method: %s. Recommending Service Configurations." %
                (self.__class__.__name__, inspect.stack()[0][3]))

    self.recommendConfigurations(configurations, clusterData, services, hosts)

  def recommendConfigurations(self, configurations, clusterData, services, hosts):
    """
    Recommend configurations for this service.
    """
    Logger.info("Class: %s, Method: %s. Recommending Service Configurations." %
                (self.__class__.__name__, inspect.stack()[0][3]))

    Logger.info("Setting zoo.cfg to default dataDir to /hadoop/zookeeper on the best matching mount")

    zk_mount_properties = [
      ("dataDir", "ZOOKEEPER_SERVER", "/hadoop/zookeeper", "single"),
    ]
    self.updateMountProperties("zoo.cfg", zk_mount_properties, configurations, services, hosts)

  def getServiceConfigurationsValidationItems(self, configurations, recommendedDefaults, services, hosts):
    """
    Validate configurations for the service. Return a list of errors.
    """
    Logger.info("Class: %s, Method: %s. Validating Configurations." %
                (self.__class__.__name__, inspect.stack()[0][3]))

    items = []

    # Example of validating by calling helper methods
    '''
    configType = "zookeeper-env"
    method = self.someMethodInThisClass
    resultItems = self.validateConfigurationsForSite(configurations, recommendedDefaults, services, hosts, configType, method)
    items.extend(resultItems)

    method = self.anotherMethodInThisClass
    resultItems = self.validateConfigurationsForSite(configurations, recommendedDefaults, services, hosts, configType, method)
    items.extend(resultItems)
    '''

    return items

  '''
  def someMethodInThisClass(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = []
    validationItems.append({"config-name": "zookeeper-env", "item": self.getErrorItem("My custom message 1")})
    return self.toConfigurationValidationProblems(validationItems, "zookeeper-env")

  def anotherMethodInThisClass(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = []
    validationItems.append({"config-name": "zookeeper-env", "item": self.getErrorItem("My custom message 2")})
    return self.toConfigurationValidationProblems(validationItems, "zookeeper-env")
  '''