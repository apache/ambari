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

class ZeppelinServiceAdvisor(service_advisor.ServiceAdvisor):

  def __init__(self, *args, **kwargs):
    self.as_super = super(ZeppelinServiceAdvisor, self)
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

    return self.getServiceComponentCardinalityValidations(services, hosts, "ZEPPELIN")

  def getServiceConfigurationRecommendations(self, configurations, clusterData, services, hosts):
    """
    Entry point.
    Must be overriden in child class.
    """
    #Logger.info("Class: %s, Method: %s. Recommending Service Configurations." %
    #            (self.__class__.__name__, inspect.stack()[0][3]))

    recommender = ZeppelinRecommender()
    recommender.recommendZeppelinConfigurationsFromHDP25(configurations, clusterData, services, hosts)
    recommender.recommendZeppelinConfigurationsFromHDP30(configurations, clusterData, services, hosts)

  def getServiceConfigurationsValidationItems(self, configurations, recommendedDefaults, services, hosts):
    """
    Entry point.
    Validate configurations for the service. Return a list of errors.
    The code for this function should be the same for each Service Advisor.
    """
    #Logger.info("Class: %s, Method: %s. Validating Configurations." %
    #            (self.__class__.__name__, inspect.stack()[0][3]))

    validator = ZeppelinValidator()
    # Calls the methods of the validator using arguments,
    # method(siteProperties, siteRecommendations, configurations, services, hosts)
    return validator.validateListOfConfigUsingMethod(configurations, recommendedDefaults, services, hosts, validator.validators)

  @staticmethod
  def isKerberosEnabled(services, configurations):
    """
    Determine if Kerberos is enabled for Zeppelin.

    If zeppelin-env/zeppelin.kerberos.enabled exists and is set to "true", return True;
    otherwise return false.

    The value of this property is first tested in the updated configurations (configurations) then
    tested in the current configuration set (services)

    :type services: dict
    :param services: the dictionary containing the existing configuration values
    :type configurations: dict
    :param configurations: the dictionary containing the updated configuration values
    :rtype: bool
    :return: True or False
    """
    if configurations and "zeppelin-env" in configurations and \
            "zeppelin.kerberos.enabled" in configurations["zeppelin-env"]["properties"]:
      return configurations["zeppelin-env"]["properties"]["zeppelin.kerberos.enabled"].lower() == "true"
    elif services and "zeppelin-env" in services["configurations"] and \
            "zeppelin.kerberos.enabled" in services["configurations"]["zeppelin-env"]["properties"]:
      return services["configurations"]["zeppelin-env"]["properties"]["zeppelin.kerberos.enabled"].lower() == "true"
    else:
      return False



class ZeppelinRecommender(service_advisor.ServiceAdvisor):
  """
  Zeppelin Recommender suggests properties when adding the service for the first time or modifying configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(ZeppelinRecommender, self)
    self.as_super.__init__(*args, **kwargs)

  def recommendZeppelinConfigurationsFromHDP25(self, configurations, clusterData, services, hosts):
    """
    :type configurations dict
    :type clusterData dict
    :type services dict
    :type hosts dict
    """
    self.__recommendLivySuperUsers(configurations, services)

    zeppelin_shiro_ini = self.getServicesSiteProperties(services, "zeppelin-shiro-ini")
    zeppelin_site = self.getServicesSiteProperties(services, "zeppelin-site")
    putZeppelinShiroIniProperty = self.putProperty(configurations, "zeppelin-shiro-ini", services)

    if zeppelin_shiro_ini and "shiro_ini_content" in zeppelin_shiro_ini:
      shiro_ini_content = zeppelin_shiro_ini['shiro_ini_content']

      if zeppelin_site and "zeppelin.ssl" in zeppelin_site and zeppelin_site["zeppelin.ssl"] == 'true':
        shiro_ini_content = shiro_ini_content.replace("#cookie.secure = true", "cookie.secure = true")
        putZeppelinShiroIniProperty('shiro_ini_content', str(shiro_ini_content))

      else:
        if not "#cookie.secure = true" in shiro_ini_content:
          shiro_ini_content = shiro_ini_content.replace("cookie.secure = true", "#cookie.secure = true")
          putZeppelinShiroIniProperty('shiro_ini_content', str(shiro_ini_content))


  def recommendZeppelinConfigurationsFromHDP30(self, configurations, clusterData, services, hosts):
    """
    :type configurations dict
    :type clusterData dict
    :type services dict
    :type hosts dict
    """
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if "SPARK2" in servicesList and "spark2-atlas-application-properties-override" in services["configurations"] \
            and 'atlas.spark.enabled' in services['configurations']['spark2-atlas-application-properties-override']['properties']:
      sac_enabled = str(services['configurations']['spark2-atlas-application-properties-override']['properties']['atlas.spark.enabled']).upper() == 'TRUE'
      zeppelin_env_properties = self.getServicesSiteProperties(services, "zeppelin-env")
      putZeppelinEnvProperty = self.putProperty(configurations, "zeppelin-env", services)
      content = zeppelin_env_properties["zeppelin_env_content"]
      content_in_lines = content.splitlines()
      result_list = []
      for line in content_in_lines:
        if "ZEPPELIN_INTP_CLASSPATH_OVERRIDES" in line:
          if sac_enabled:
            if line.lstrip().startswith("#"):
              line = "export ZEPPELIN_INTP_CLASSPATH_OVERRIDES=\"{{external_dependency_conf}}:/usr/hdp/current/spark-atlas-connector/*\""
            elif "{{external_dependency_conf}}" in line:
              line = line.replace("{{external_dependency_conf}}", "{{external_dependency_conf}}:/usr/hdp/current/spark-atlas-connector/*")
            else:
              k = line.rfind("\"")
              line = line[:k] + ":/usr/hdp/current/spark-atlas-connector/*\"" + line[k+1:]
          else:
            if ":/usr/hdp/current/spark-atlas-connector" in line:
              line = line.replace(":/usr/hdp/current/spark-atlas-connector/*", "")
            elif "/usr/hdp/current/spark-atlas-connector" in line:
              line = line.replace("/usr/hdp/current/spark-atlas-connector/*", "")

        result_list.append(line)

      content = "\n".join(result_list)
      putZeppelinEnvProperty("zeppelin_env_content", content)



  def __recommendLivySuperUsers(self, configurations, services):
    """
    If Kerberos is enabled AND Zeppelin is installed and Spark Livy Server is installed, then set
    livy-conf/livy.superusers to contain the Zeppelin principal name from
    zeppelin-site/zeppelin.server.kerberos.principal

    :param configurations:
    :param services:
    """
    if ZeppelinServiceAdvisor.isKerberosEnabled(services, configurations):
      zeppelin_site = self.getServicesSiteProperties(services, "zeppelin-site")

      if zeppelin_site and 'zeppelin.server.kerberos.principal' in zeppelin_site:
        zeppelin_principal = zeppelin_site['zeppelin.server.kerberos.principal']
        zeppelin_user = zeppelin_principal.split('@')[0] if zeppelin_principal else None

        if zeppelin_user:
          self.__conditionallyUpdateSuperUsers('livy2-conf', 'livy.superusers', zeppelin_user, configurations, services)

  def __conditionallyUpdateSuperUsers(self, config_name, property_name, user_to_add, configurations, services):
    config = self.getServicesSiteProperties(services, config_name)

    if config:
      superusers = config[property_name] if property_name in config else None

      # add the user to the set of users
      if superusers:
        _superusers = superusers.split(',')
        _superusers = [x.strip() for x in _superusers]
        _superusers = filter(None, _superusers)  # Removes empty string elements from array
      else:
        _superusers = []

      if user_to_add not in _superusers:
        _superusers.append(user_to_add)

        putProperty = self.putProperty(configurations, config_name, services)
        putProperty(property_name, ','.join(_superusers))

class ZeppelinValidator(service_advisor.ServiceAdvisor):
  """
  Zeppelin Validator checks the correctness of properties whenever the service is first added or the user attempts to
  change configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(ZeppelinValidator, self)
    self.as_super.__init__(*args, **kwargs)

    self.validators = []
