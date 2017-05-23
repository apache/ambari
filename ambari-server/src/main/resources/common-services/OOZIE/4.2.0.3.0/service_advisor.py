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
STACKS_DIR = os.path.join(SCRIPT_DIR, '../../../stacks/')
PARENT_FILE = os.path.join(STACKS_DIR, 'service_advisor.py')

try:
  with open(PARENT_FILE, 'rb') as fp:
    service_advisor = imp.load_module('service_advisor', fp, PARENT_FILE, ('.py', 'rb', imp.PY_SOURCE))
except Exception as e:
  traceback.print_exc()
  print "Failed to load parent"

class OozieServiceAdvisor(service_advisor.ServiceAdvisor):

  def __init__(self, *args, **kwargs):
    self.as_super = super(OozieServiceAdvisor, self)
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
    self.componentLayoutSchemes.update({
      'OOZIE_SERVER': {6: 1, 31: 2, "else": 3},
      })

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

    recommender = OozieRecommender()
    recommender.recommendOozieConfigurationsFromHDP206(configurations, clusterData, services, hosts)
    recommender.recommendOozieConfigurationsFromHDP21(configurations, clusterData, services, hosts)
    recommender.recommendOozieConfigurationsFromHDP25(configurations, clusterData, services, hosts)



  def getServiceConfigurationsValidationItems(self, configurations, recommendedDefaults, services, hosts):
    """
    Entry point.
    Validate configurations for the service. Return a list of errors.
    The code for this function should be the same for each Service Advisor.
    """
    #Logger.info("Class: %s, Method: %s. Validating Configurations." %
    #            (self.__class__.__name__, inspect.stack()[0][3]))

    validator = OozieValidator()
    # Calls the methods of the validator using arguments,
    # method(siteProperties, siteRecommendations, configurations, services, hosts)
    return validator.validateListOfConfigUsingMethod(configurations, recommendedDefaults, services, hosts, validator.validators)



class OozieRecommender(service_advisor.ServiceAdvisor):
  """
  Oozie Recommender suggests properties when adding the service for the first time or modifying configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(OozieRecommender, self)
    self.as_super.__init__(*args, **kwargs)


  def recommendOozieConfigurationsFromHDP206(self, configurations, clusterData, services, hosts):
    oozie_mount_properties = [
      ("oozie_data_dir", "OOZIE_SERVER", "/hadoop/oozie/data", "single"),
    ]
    self.updateMountProperties("oozie-env", oozie_mount_properties, configurations, services, hosts)


  def getDBConnectionString(self, databaseType):
    driverDict = {
      'NEW MYSQL DATABASE': 'jdbc:mysql://{0}/{1}?createDatabaseIfNotExist=true',
      'NEW DERBY DATABASE': 'jdbc:derby:${{oozie.data.dir}}/${{oozie.db.schema.name}}-db;create=true',
      'EXISTING MYSQL DATABASE': 'jdbc:mysql://{0}/{1}',
      'EXISTING MYSQL / MARIADB DATABASE': 'jdbc:mysql://{0}/{1}',
      'EXISTING POSTGRESQL DATABASE': 'jdbc:postgresql://{0}:5432/{1}',
      'EXISTING ORACLE DATABASE': 'jdbc:oracle:thin:@//{0}:1521/{1}',
      'EXISTING SQL ANYWHERE DATABASE': 'jdbc:sqlanywhere:host={0};database={1}'
    }
    return driverDict.get(databaseType.upper())


  def getDBDriver(self, databaseType):
    driverDict = {
      'NEW MYSQL DATABASE': 'com.mysql.jdbc.Driver',
      'NEW DERBY DATABASE': 'org.apache.derby.jdbc.EmbeddedDriver',
      'EXISTING MYSQL DATABASE': 'com.mysql.jdbc.Driver',
      'EXISTING MYSQL / MARIADB DATABASE': 'com.mysql.jdbc.Driver',
      'EXISTING POSTGRESQL DATABASE': 'org.postgresql.Driver',
      'EXISTING ORACLE DATABASE': 'oracle.jdbc.driver.OracleDriver',
      'EXISTING SQL ANYWHERE DATABASE': 'sap.jdbc4.sqlanywhere.IDriver'
    }
    return driverDict.get(databaseType.upper())


  def getProtocol(self, databaseType):
    first_parts_of_connection_string = {
      'NEW MYSQL DATABASE': 'jdbc:mysql',
      'NEW DERBY DATABASE': 'jdbc:derby',
      'EXISTING MYSQL DATABASE': 'jdbc:mysql',
      'EXISTING MYSQL / MARIADB DATABASE': 'jdbc:mysql',
      'EXISTING POSTGRESQL DATABASE': 'jdbc:postgresql',
      'EXISTING ORACLE DATABASE': 'jdbc:oracle',
      'EXISTING SQL ANYWHERE DATABASE': 'jdbc:sqlanywhere'
    }
    return first_parts_of_connection_string.get(databaseType.upper())


  def recommendOozieConfigurationsFromHDP21(self, configurations, clusterData, services, hosts):

    oozieSiteProperties = self.getSiteProperties(services['configurations'], 'oozie-site')
    oozieEnvProperties = self.getSiteProperties(services['configurations'], 'oozie-env')
    putOozieProperty = self.putProperty(configurations, "oozie-site", services)
    putOozieEnvProperty = self.putProperty(configurations, "oozie-env", services)

    if "FALCON_SERVER" in clusterData["components"]:
      putOozieSiteProperty = self.putProperty(configurations, "oozie-site", services)
      falconUser = None
      if "falcon-env" in services["configurations"] and "falcon_user" in services["configurations"]["falcon-env"]["properties"]:
        falconUser = services["configurations"]["falcon-env"]["properties"]["falcon_user"]
        if falconUser is not None:
          putOozieSiteProperty("oozie.service.ProxyUserService.proxyuser.{0}.groups".format(falconUser) , "*")
          putOozieSiteProperty("oozie.service.ProxyUserService.proxyuser.{0}.hosts".format(falconUser) , "*")
        falconUserOldValue = self.getOldValue(services, "falcon-env", "falcon_user")
        if falconUserOldValue is not None:
          if 'forced-configurations' not in services:
            services["forced-configurations"] = []
          putOozieSitePropertyAttribute = self.putPropertyAttribute(configurations, "oozie-site")
          putOozieSitePropertyAttribute("oozie.service.ProxyUserService.proxyuser.{0}.groups".format(falconUserOldValue), 'delete', 'true')
          putOozieSitePropertyAttribute("oozie.service.ProxyUserService.proxyuser.{0}.hosts".format(falconUserOldValue), 'delete', 'true')
          services["forced-configurations"].append({"type" : "oozie-site", "name" : "oozie.service.ProxyUserService.proxyuser.{0}.hosts".format(falconUserOldValue)})
          services["forced-configurations"].append({"type" : "oozie-site", "name" : "oozie.service.ProxyUserService.proxyuser.{0}.groups".format(falconUserOldValue)})
          if falconUser is not None:
            services["forced-configurations"].append({"type" : "oozie-site", "name" : "oozie.service.ProxyUserService.proxyuser.{0}.hosts".format(falconUser)})
            services["forced-configurations"].append({"type" : "oozie-site", "name" : "oozie.service.ProxyUserService.proxyuser.{0}.groups".format(falconUser)})

      putMapredProperty = self.putProperty(configurations, "oozie-site")
      putMapredProperty("oozie.services.ext",
                        "org.apache.oozie.service.JMSAccessorService," +
                        "org.apache.oozie.service.PartitionDependencyManagerService," +
                        "org.apache.oozie.service.HCatAccessorService")
    if oozieEnvProperties and oozieSiteProperties and self.checkSiteProperties(oozieSiteProperties, 'oozie.service.JPAService.jdbc.driver') and self.checkSiteProperties(oozieEnvProperties, 'oozie_database'):
      putOozieProperty('oozie.service.JPAService.jdbc.driver', self.getDBDriver(oozieEnvProperties['oozie_database']))
    if oozieSiteProperties and oozieEnvProperties and self.checkSiteProperties(oozieSiteProperties, 'oozie.db.schema.name', 'oozie.service.JPAService.jdbc.url') and self.checkSiteProperties(oozieEnvProperties, 'oozie_database'):
      oozieServerHost = self.getHostWithComponent('OOZIE', 'OOZIE_SERVER', services, hosts)
      oozieDBConnectionURL = oozieSiteProperties['oozie.service.JPAService.jdbc.url']
      protocol = self.getProtocol(oozieEnvProperties['oozie_database'])
      oldSchemaName = self.getOldValue(services, "oozie-site", "oozie.db.schema.name")
      # under these if constructions we are checking if oozie server hostname available,
      # if schema name was changed or if protocol according to current db type differs with protocol in db connection url(db type was changed)
      if oozieServerHost is not None:
        if oldSchemaName or (protocol and oozieDBConnectionURL and not oozieDBConnectionURL.startswith(protocol)):
          dbConnection = self.getDBConnectionString(oozieEnvProperties['oozie_database']).format(oozieServerHost['Hosts']['host_name'], oozieSiteProperties['oozie.db.schema.name'])
          putOozieProperty('oozie.service.JPAService.jdbc.url', dbConnection)


  def recommendOozieConfigurationsFromHDP25(self, configurations, clusterData, services, hosts):
    putOozieEnvProperty = self.putProperty(configurations, "oozie-env", services)

    if not "oozie-env" in services["configurations"] :
      self.logger.info("No oozie configurations available")
      return

    if not "FALCON_SERVER" in clusterData["components"] :
      self.logger.info("Falcon is not part of the installation")
      return

    falconUser = 'falcon'

    if "falcon-env" in services["configurations"] :
      if "falcon_user" in services["configurations"]["falcon-env"]["properties"] :
        falconUser = services["configurations"]["falcon-env"]["properties"]["falcon_user"]
        self.logger.info("Falcon user from configuration: %s " % falconUser)

    self.logger.info("Falcon user : %s" % falconUser)

    oozieUser = 'oozie'

    if "oozie_user" \
      in services["configurations"]["oozie-env"]["properties"] :
      oozieUser = services["configurations"]["oozie-env"]["properties"]["oozie_user"]
      self.logger.info("Oozie user from configuration %s" % oozieUser)

    self.logger.info("Oozie user %s" % oozieUser)

    if "oozie_admin_users" \
            in services["configurations"]["oozie-env"]["properties"] :
      currentAdminUsers =  services["configurations"]["oozie-env"]["properties"]["oozie_admin_users"]
      self.logger.info("Oozie admin users from configuration %s" % currentAdminUsers)
    else :
      currentAdminUsers = "{0}, oozie-admin".format(oozieUser)
      self.logger.info("Setting default oozie admin users to %s" % currentAdminUsers)


    if falconUser in currentAdminUsers :
      self.logger.info("Falcon user %s already member of  oozie admin users " % falconUser)
      return

    newAdminUsers = "{0},{1}".format(currentAdminUsers, falconUser)

    self.logger.info("new oozie admin users : %s" % newAdminUsers)

    services["forced-configurations"].append({"type" : "oozie-env", "name" : "oozie_admin_users"})
    putOozieEnvProperty("oozie_admin_users", newAdminUsers)


class OozieValidator(service_advisor.ServiceAdvisor):
  """
  Oozie Validator checks the correctness of properties whenever the service is first added or the user attempts to
  change configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(OozieValidator, self)
    self.as_super.__init__(*args, **kwargs)

    self.validators = []





