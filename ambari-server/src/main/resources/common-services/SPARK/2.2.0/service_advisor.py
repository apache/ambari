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

class SparkServiceAdvisor(service_advisor.ServiceAdvisor):

  def __init__(self, *args, **kwargs):
    self.as_super = super(SparkServiceAdvisor, self)
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
    self.heap_size_properties = {"SPARK_JOBHISTORYSERVER":
                                   [{"config-name": "spark-env",
                                     "property": "spark_daemon_memory",
                                     "default": "1024m"}]
                                 }

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
    # TODO this is from HDP25StackAdvisor, do we have something similar in Service Advisor?
    #
    # return super(HDP25StackAdvisor, self).isComponentUsingCardinalityForLayout (componentName) or  componentName in ['SPARK_THRIFTSERVER', 'LIVY_SERVER']

    # Nothing to do
    return []

  def getServiceConfigurationRecommendations(self, configurations, clusterData, services, hosts):
    """
    Entry point.
    Must be overriden in child class.
    """
    Logger.info("Class: %s, Method: %s. Recommending Service Configurations." %
                (self.__class__.__name__, inspect.stack()[0][3]))
    recommender = SparkRecommender()

    recommender.recommendSparkConfigurationsFromHDP25(configurations, clusterData, services, hosts)
    # Nothing to do
    pass

  def getServiceConfigurationsValidationItems(self, configurations, recommendedDefaults, services, hosts):
    """
    Entry point.
    Validate configurations for the service. Return a list of errors.
    The code for this function should be the same for each Service Advisor.
    """
    Logger.info("Class: %s, Method: %s. Validating Configurations." %
                (self.__class__.__name__, inspect.stack()[0][3]))

    validator = SparkValidator()
    # Calls the methods of the validator using arguments,
    # method(siteProperties, siteRecommendations, configurations, services, hosts)
    return validator.validateListOfConfigUsingMethod(configurations, recommendedDefaults, services, hosts, validator.validators)

class SparkRecommender(service_advisor.ServiceAdvisor):
  """
  Spark Recommender suggests properties when adding the service for the first time or modifying configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(SparkRecommender, self)
    self.as_super.__init__(*args, **kwargs)

  def recommendSparkConfigurationsFromHDP25(self, configurations, clusterData, services, hosts):
    """
    :type configurations dict
    :type clusterData dict
    :type services dict
    :type hosts dict
    """
    putSparkProperty = self.putProperty(configurations, "spark-defaults", services)
    putSparkThriftSparkConf = self.putProperty(configurations, "spark-thrift-sparkconf", services)

    spark_queue = self.recommendYarnQueue(services, "spark-defaults", "spark.yarn.queue")
    if spark_queue is not None:
      putSparkProperty("spark.yarn.queue", spark_queue)

    spark_thrift_queue = self.recommendYarnQueue(services, "spark-thrift-sparkconf", "spark.yarn.queue")
    if spark_thrift_queue is not None:
      putSparkThriftSparkConf("spark.yarn.queue", spark_thrift_queue)

class SparkValidator(service_advisor.ServiceAdvisor):
  """
  Spark Validator checks the correctness of properties whenever the service is first added or the user attempts to
  change configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(SparkValidator, self)
    self.as_super.__init__(*args, **kwargs)

    self.validators = [("spark-defaults", self.validateSparkDefaultsFromHDP25),
                       ("spark-thrift-sparkconf", self.validateSparkThriftSparkConfFromHDP25)]


  def validateSparkDefaultsFromHDP25(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = [
      {
        "config-name": 'spark.yarn.queue',
        "item": self.validatorYarnQueue(properties, recommendedDefaults, 'spark.yarn.queue', services)
      }
    ]
    return self.toConfigurationValidationProblems(validationItems, "spark-defaults")

  def validateSparkThriftSparkConfFromHDP25(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = [
      {
        "config-name": 'spark.yarn.queue',
        "item": self.validatorYarnQueue(properties, recommendedDefaults, 'spark.yarn.queue', services)
      }
    ]
    return self.toConfigurationValidationProblems(validationItems, "spark-thrift-sparkconf")