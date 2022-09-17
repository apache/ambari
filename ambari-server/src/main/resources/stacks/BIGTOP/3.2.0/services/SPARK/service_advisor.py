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
STACKS_DIR = os.path.join(SCRIPT_DIR, '../../../../../stacks/')
PARENT_FILE = os.path.join(STACKS_DIR, 'service_advisor.py')

try:
  if "BASE_SERVICE_ADVISOR" in os.environ:
    PARENT_FILE = os.environ["BASE_SERVICE_ADVISOR"]
  with open(PARENT_FILE, 'rb') as fp:
    service_advisor = imp.load_module('service_advisor', fp, PARENT_FILE, ('.py', 'rb', imp.PY_SOURCE))
except Exception as e:
  traceback.print_exc()
  print "Failed to load parent"

class Spark2ServiceAdvisor(service_advisor.ServiceAdvisor):

  def __init__(self, *args, **kwargs):
    self.as_super = super(Spark2ServiceAdvisor, self)
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
    self.heap_size_properties = {"SPARK2_JOBHISTORYSERVER":
                                   [{"config-name": "spark2-env",
                                     "property": "spark_daemon_memory",
                                     "default": "2048m"}]}

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

    return self.getServiceComponentCardinalityValidations(services, hosts, "SPARK2")

  def getServiceConfigurationRecommendations(self, configurations, clusterData, services, hosts):
    """
    Entry point.
    Must be overriden in child class.
    """
    #Logger.info("Class: %s, Method: %s. Recommending Service Configurations." %
    #            (self.__class__.__name__, inspect.stack()[0][3]))

    recommender = Spark2Recommender()
    recommender.recommendSpark2ConfigurationsFromHDP25(configurations, clusterData, services, hosts)
    recommender.recommendSPARK2ConfigurationsFromHDP26(configurations, clusterData, services, hosts)
    recommender.recommendSPARK2ConfigurationsFromHDP30(configurations, clusterData, services, hosts)



  def getServiceConfigurationsValidationItems(self, configurations, recommendedDefaults, services, hosts):
    """
    Entry point.
    Validate configurations for the service. Return a list of errors.
    The code for this function should be the same for each Service Advisor.
    """
    #Logger.info("Class: %s, Method: %s. Validating Configurations." %
    #            (self.__class__.__name__, inspect.stack()[0][3]))

    validator = Spark2Validator()
    # Calls the methods of the validator using arguments,
    # method(siteProperties, siteRecommendations, configurations, services, hosts)
    return validator.validateListOfConfigUsingMethod(configurations, recommendedDefaults, services, hosts, validator.validators)

  def isComponentUsingCardinalityForLayout(self, componentName):
    return componentName in ('SPARK2_THRIFTSERVER', 'LIVY2_SERVER')

  @staticmethod
  def isKerberosEnabled(services, configurations):
    """
    Determines if security is enabled by testing the value of spark2-defaults/spark.history.kerberos.enabled enabled.
    If the property exists and is equal to "true", then is it enabled; otherwise is it assumed to be
    disabled.

    :type services: dict
    :param services: the dictionary containing the existing configuration values
    :type configurations: dict
    :param configurations: the dictionary containing the updated configuration values
    :rtype: bool
    :return: True or False
    """
    if configurations and "spark2-defaults" in configurations and \
            "spark.history.kerberos.enabled" in configurations["spark2-defaults"]["properties"]:
      return configurations["spark2-defaults"]["properties"]["spark.history.kerberos.enabled"].lower() == "true"
    elif services and "spark2-defaults" in services["configurations"] and \
            "spark.history.kerberos.enabled" in services["configurations"]["spark2-defaults"]["properties"]:
      return services["configurations"]["spark2-defaults"]["properties"]["spark.history.kerberos.enabled"].lower() == "true"
    else:
      return False


class Spark2Recommender(service_advisor.ServiceAdvisor):
  """
  Spark2 Recommender suggests properties when adding the service for the first time or modifying configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(Spark2Recommender, self)
    self.as_super.__init__(*args, **kwargs)

  def recommendSpark2ConfigurationsFromHDP25(self, configurations, clusterData, services, hosts):
    """
    :type configurations dict
    :type clusterData dict
    :type services dict
    :type hosts dict
    """
    putSparkProperty = self.putProperty(configurations, "spark2-defaults", services)
    putSparkThriftSparkConf = self.putProperty(configurations, "spark2-thrift-sparkconf", services)

    spark_queue = self.recommendYarnQueue(services, "spark2-defaults", "spark.yarn.queue")
    if spark_queue is not None:
      putSparkProperty("spark.yarn.queue", spark_queue)

    spark_thrift_queue = self.recommendYarnQueue(services, "spark2-thrift-sparkconf", "spark.yarn.queue")
    if spark_thrift_queue is not None:
      putSparkThriftSparkConf("spark.yarn.queue", spark_thrift_queue)


  def recommendSPARK2ConfigurationsFromHDP26(self, configurations, clusterData, services, hosts):
    """
    :type configurations dict
    :type clusterData dict
    :type services dict
    :type hosts dict
    """

    if Spark2ServiceAdvisor.isKerberosEnabled(services, configurations):

      spark2_defaults = self.getServicesSiteProperties(services, "spark2-defaults")

      if spark2_defaults:
        putSpark2DafaultsProperty = self.putProperty(configurations, "spark2-defaults", services)
        putSpark2DafaultsProperty('spark.acls.enable', 'true')
        putSpark2DafaultsProperty('spark.admin.acls', '')
        putSpark2DafaultsProperty('spark.history.ui.acls.enable', 'true')
        putSpark2DafaultsProperty('spark.history.ui.admin.acls', '')


    self.__addZeppelinToLivy2SuperUsers(configurations, services)


  def recommendSPARK2ConfigurationsFromHDP30(self, configurations, clusterData, services, hosts):

    # SAC
    if "spark2-atlas-application-properties-override" in services["configurations"]:
      spark2_atlas_application_properties_override = self.getServicesSiteProperties(services, "spark2-atlas-application-properties-override")
      spark2_defaults_properties = self.getServicesSiteProperties(services, "spark2-defaults")
      spark2_thriftspark_conf_properties = self.getServicesSiteProperties(services, "spark2-thrift-sparkconf")
      putSpark2DefautlsProperty = self.putProperty(configurations, "spark2-defaults", services)
      putSpark2DefaultsPropertyAttribute = self.putPropertyAttribute(configurations,"spark2-defaults")
      putSpark2ThriftSparkConfProperty = self.putProperty(configurations, "spark2-thrift-sparkconf", services)
      putSpark2AtlasHookProperty = self.putProperty(configurations, "spark2-atlas-application-properties-override", services)
      putSpark2AtlasHookPropertyAttribute = self.putPropertyAttribute(configurations,"spark2-atlas-application-properties-override")
      spark2_sac_enabled = None
      if self.checkSiteProperties(spark2_atlas_application_properties_override, "atlas.spark.enabled"):
        spark2_sac_enabled = spark2_atlas_application_properties_override["atlas.spark.enabled"]
        spark2_sac_enabled = str(spark2_sac_enabled).upper() == 'TRUE'

      if spark2_sac_enabled:

        self.setOrAddValueToProperty(putSpark2DefautlsProperty, spark2_defaults_properties, "spark.driver.extraClassPath", "/usr/hdp/current/spark-atlas-connector/*", ":")
        self.setOrAddValueToProperty(putSpark2DefautlsProperty, spark2_defaults_properties, "spark.yarn.dist.files", "/etc/spark2/conf/atlas-application.properties.yarn#atlas-application.properties", ",")
        self.setOrAddValueToProperty(putSpark2ThriftSparkConfProperty, spark2_thriftspark_conf_properties, "spark.driver.extraClassPath", "/usr/hdp/current/spark-atlas-connector/*", ":")

        self.setOrAddValueToProperty(putSpark2DefautlsProperty, spark2_defaults_properties, "spark.extraListeners", "com.hortonworks.spark.atlas.SparkAtlasEventTracker", ",")
        self.setOrAddValueToProperty(putSpark2DefautlsProperty, spark2_defaults_properties, "spark.sql.queryExecutionListeners", "com.hortonworks.spark.atlas.SparkAtlasEventTracker", ",")
        self.setOrAddValueToProperty(putSpark2ThriftSparkConfProperty, spark2_thriftspark_conf_properties, "spark.extraListeners", "com.hortonworks.spark.atlas.SparkAtlasEventTracker", ",")
        self.setOrAddValueToProperty(putSpark2ThriftSparkConfProperty, spark2_thriftspark_conf_properties, "spark.sql.queryExecutionListeners", "com.hortonworks.spark.atlas.SparkAtlasEventTracker", ",")

        self.setOrAddValueToProperty(putSpark2DefautlsProperty, spark2_defaults_properties, "spark.sql.streaming.streamingQueryListeners", "com.hortonworks.spark.atlas.SparkAtlasStreamingQueryEventTracker", ",")
        self.setOrAddValueToProperty(putSpark2ThriftSparkConfProperty, spark2_thriftspark_conf_properties, "spark.sql.streaming.streamingQueryListeners", "com.hortonworks.spark.atlas.SparkAtlasStreamingQueryEventTracker", ",")

        putSpark2AtlasHookProperty("atlas.client.checkModelInStart", "false")

      else:

        self.removeValueFromProperty(putSpark2DefautlsProperty, spark2_defaults_properties, "spark.driver.extraClassPath", "/usr/hdp/current/spark-atlas-connector/*", ":")
        self.removeValueFromProperty(putSpark2DefautlsProperty, spark2_defaults_properties, "spark.yarn.dist.files", "/etc/spark2/conf/atlas-application.properties.yarn#atlas-application.properties", ",")
        self.removeValueFromProperty(putSpark2ThriftSparkConfProperty, spark2_thriftspark_conf_properties, "spark.driver.extraClassPath", "/usr/hdp/current/spark-atlas-connector/*", ":")

        self.removeValueFromProperty(putSpark2DefautlsProperty, spark2_defaults_properties, "spark.extraListeners", "com.hortonworks.spark.atlas.SparkAtlasEventTracker", ",")
        self.removeValueFromProperty(putSpark2DefautlsProperty, spark2_defaults_properties, "spark.sql.queryExecutionListeners", "com.hortonworks.spark.atlas.SparkAtlasEventTracker", ",")
        self.removeValueFromProperty(putSpark2ThriftSparkConfProperty, spark2_thriftspark_conf_properties, "spark.extraListeners", "com.hortonworks.spark.atlas.SparkAtlasEventTracker", ",")
        self.removeValueFromProperty(putSpark2ThriftSparkConfProperty, spark2_thriftspark_conf_properties, "spark.sql.queryExecutionListeners", "com.hortonworks.spark.atlas.SparkAtlasEventTracker", ",")

        self.removeValueFromProperty(putSpark2DefautlsProperty, spark2_defaults_properties, "spark.sql.streaming.streamingQueryListeners", "com.hortonworks.spark.atlas.SparkAtlasStreamingQueryEventTracker", ",")
        self.removeValueFromProperty(putSpark2ThriftSparkConfProperty, spark2_thriftspark_conf_properties, "spark.sql.streaming.streamingQueryListeners", "com.hortonworks.spark.atlas.SparkAtlasStreamingQueryEventTracker", ",")

        putSpark2AtlasHookPropertyAttribute("atlas.client.checkModelInStart", "delete", "true")



  def setOrAddValueToProperty(self, putConfigProperty, config, propertyName, propertyValue, separator):
    if self.checkSiteProperties(config, propertyName) and len(str(config[propertyName]).strip()) > 0:
      putConfigProperty(propertyName, str(config[propertyName]).strip() + separator + propertyValue)
    else:
      putConfigProperty(propertyName, propertyValue)

  def removeValueFromProperty(self, putConfigProperty, config, propertyName, propertyValue, separator):
    if not self.checkSiteProperties(config, propertyName):
      return
    if str(config[propertyName]).strip() == propertyValue:
      putConfigProperty(propertyName, " ")
    else:
      putConfigProperty(propertyName, str(config[propertyName]).replace(separator + propertyValue, ""))

  def __addZeppelinToLivy2SuperUsers(self, configurations, services):
    """
    If Kerberos is enabled AND Zeppelin is installed AND Spark2 Livy Server is installed, then set
    livy2-conf/livy.superusers to contain the Zeppelin principal name from
    zeppelin-site/zeppelin.server.kerberos.principal

    :param configurations:
    :param services:
    """
    if Spark2ServiceAdvisor.isKerberosEnabled(services, configurations):
      zeppelin_site = self.getServicesSiteProperties(services, "zeppelin-site")

      if zeppelin_site and 'zeppelin.server.kerberos.principal' in zeppelin_site:
        zeppelin_principal = zeppelin_site['zeppelin.server.kerberos.principal']
        zeppelin_user = zeppelin_principal.split('@')[0] if zeppelin_principal else None

        if zeppelin_user:
          livy2_conf = self.getServicesSiteProperties(services, 'livy2-conf')

          if livy2_conf:
            superusers = livy2_conf['livy.superusers'] if livy2_conf and 'livy.superusers' in livy2_conf else None

            # add the Zeppelin user to the set of users
            if superusers:
              _superusers = superusers.split(',')
              _superusers = [x.strip() for x in _superusers]
              _superusers = filter(None, _superusers)  # Removes empty string elements from array
            else:
              _superusers = []

            if zeppelin_user not in _superusers:
              _superusers.append(zeppelin_user)

              putLivy2ConfProperty = self.putProperty(configurations, 'livy2-conf', services)
              putLivy2ConfProperty('livy.superusers', ','.join(_superusers))


class Spark2Validator(service_advisor.ServiceAdvisor):
  """
  Spark2 Validator checks the correctness of properties whenever the service is first added or the user attempts to
  change configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(Spark2Validator, self)
    self.as_super.__init__(*args, **kwargs)

    self.validators = [("spark2-defaults", self.validateSpark2DefaultsFromHDP25),
                       ("spark2-thrift-sparkconf", self.validateSpark2ThriftSparkConfFromHDP25),
                       ("spark2-atlas-application-properties-override", self.validateSpark2AtlasApplicationPropertiesFromHDP30)]


  def validateSpark2DefaultsFromHDP25(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = [
      {
        "config-name": 'spark.yarn.queue',
        "item": self.validatorYarnQueue(properties, recommendedDefaults, 'spark.yarn.queue', services)
      }
    ]
    return self.toConfigurationValidationProblems(validationItems, "spark2-defaults")


  def validateSpark2ThriftSparkConfFromHDP25(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = [
      {
        "config-name": 'spark.yarn.queue',
        "item": self.validatorYarnQueue(properties, recommendedDefaults, 'spark.yarn.queue', services)
      }
    ]
    return self.toConfigurationValidationProblems(validationItems, "spark2-thrift-sparkconf")

  def validateSpark2AtlasApplicationPropertiesFromHDP30(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = []
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if not "ATLAS" in servicesList and 'atlas.spark.enabled' in services['configurations']['spark2-atlas-application-properties-override']['properties'] and \
            str(services['configurations']['spark2-atlas-application-properties-override']['properties']['atlas.spark.enabled']).upper() == 'TRUE':
      validationItems.append({"config-name": "spark2-atlas-application-properties-override",
                                 +                              "item": self.getErrorItem("SAC could be enabled only if ATLAS service is available on cluster!")})
    return self.toConfigurationValidationProblems(validationItems, "spark2-atlas-application-properties-override")




