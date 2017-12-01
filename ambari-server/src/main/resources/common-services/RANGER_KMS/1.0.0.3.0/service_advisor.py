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

DB_TYPE_DEFAULT_PORT_MAP = {"MYSQL":"3306", "ORACLE":"1521", "POSTGRES":"5432", "MSSQL":"1433", "SQLA":"2638"}

class Ranger_KMSServiceAdvisor(service_advisor.ServiceAdvisor):

  def __init__(self, *args, **kwargs):
    self.as_super = super(Ranger_KMSServiceAdvisor, self)
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
    # Logger.info("Class: %s, Method: %s. Recommending Service Configurations." %
    #            (self.__class__.__name__, inspect.stack()[0][3]))

    recommender = RangerKMSRecommender()
    recommender.recommendRangerKMSConfigurationsFromHDP23(configurations, clusterData, services, hosts)
    recommender.recommendRangerKMSConfigurationsFromHDP25(configurations, clusterData, services, hosts)
    recommender.recommendRangerKMSConfigurationsFromHDP26(configurations, clusterData, services, hosts)

  def getServiceConfigurationsValidationItems(self, configurations, recommendedDefaults, services, hosts):
    """
    Entry point.
    Validate configurations for the service. Return a list of errors.
    The code for this function should be the same for each Service Advisor.
    """
    # Logger.info("Class: %s, Method: %s. Validating Configurations." %
    #            (self.__class__.__name__, inspect.stack()[0][3]))

    validator = RangerKMSValidator()
    # Calls the methods of the validator using arguments,
    # method(siteProperties, siteRecommendations, configurations, services, hosts)
    return validator.validateListOfConfigUsingMethod(configurations, recommendedDefaults, services, hosts, validator.validators)

class RangerKMSRecommender(service_advisor.ServiceAdvisor):
  """
  RangerKMS Recommender suggests properties when adding the service for the first time or modifying configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(RangerKMSRecommender, self)
    self.as_super.__init__(*args, **kwargs)

  def recommendRangerKMSConfigurationsFromHDP23(self, configurations, clusterData, services, hosts):
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    putRangerKmsDbksProperty = self.putProperty(configurations, "dbks-site", services)
    putRangerKmsProperty = self.putProperty(configurations, "kms-properties", services)
    kmsEnvProperties = self.getSiteProperties(services['configurations'], 'kms-env')
    putCoreSiteProperty = self.putProperty(configurations, "core-site", services)
    putCoreSitePropertyAttribute = self.putPropertyAttribute(configurations, "core-site")
    putRangerKmsAuditProperty = self.putProperty(configurations, "ranger-kms-audit", services)
    security_enabled = self.isSecurityEnabled(services)
    putRangerKmsSiteProperty = self.putProperty(configurations, "kms-site", services)
    putRangerKmsSitePropertyAttribute = self.putPropertyAttribute(configurations, "kms-site")

    if 'kms-properties' in services['configurations'] and ('DB_FLAVOR' in services['configurations']['kms-properties']['properties']):

      rangerKmsDbFlavor = services['configurations']["kms-properties"]["properties"]["DB_FLAVOR"]

      if ('db_host' in services['configurations']['kms-properties']['properties']) and ('db_name' in services['configurations']['kms-properties']['properties']):

        rangerKmsDbHost =   services['configurations']["kms-properties"]["properties"]["db_host"]
        rangerKmsDbName =   services['configurations']["kms-properties"]["properties"]["db_name"]

        ranger_kms_db_url_dict = {
          'MYSQL': {'ranger.ks.jpa.jdbc.driver': 'com.mysql.jdbc.Driver',
                    'ranger.ks.jpa.jdbc.url': 'jdbc:mysql://' + self.getDBConnectionHostPort(rangerKmsDbFlavor, rangerKmsDbHost) + '/' + rangerKmsDbName},
          'ORACLE': {'ranger.ks.jpa.jdbc.driver': 'oracle.jdbc.driver.OracleDriver',
                     'ranger.ks.jpa.jdbc.url': 'jdbc:oracle:thin:@' + self.getOracleDBConnectionHostPort(rangerKmsDbFlavor, rangerKmsDbHost, rangerKmsDbName)},
          'POSTGRES': {'ranger.ks.jpa.jdbc.driver': 'org.postgresql.Driver',
                       'ranger.ks.jpa.jdbc.url': 'jdbc:postgresql://' + self.getDBConnectionHostPort(rangerKmsDbFlavor, rangerKmsDbHost) + '/' + rangerKmsDbName},
          'MSSQL': {'ranger.ks.jpa.jdbc.driver': 'com.microsoft.sqlserver.jdbc.SQLServerDriver',
                    'ranger.ks.jpa.jdbc.url': 'jdbc:sqlserver://' + self.getDBConnectionHostPort(rangerKmsDbFlavor, rangerKmsDbHost) + ';databaseName=' + rangerKmsDbName},
          'SQLA': {'ranger.ks.jpa.jdbc.driver': 'sap.jdbc4.sqlanywhere.IDriver',
                   'ranger.ks.jpa.jdbc.url': 'jdbc:sqlanywhere:host=' + self.getDBConnectionHostPort(rangerKmsDbFlavor, rangerKmsDbHost) + ';database=' + rangerKmsDbName}
        }

        rangerKmsDbProperties = ranger_kms_db_url_dict.get(rangerKmsDbFlavor, ranger_kms_db_url_dict['MYSQL'])
        for key in rangerKmsDbProperties:
          putRangerKmsDbksProperty(key, rangerKmsDbProperties.get(key))

    if kmsEnvProperties and self.checkSiteProperties(kmsEnvProperties, 'kms_user') and 'KERBEROS' in servicesList:
      kmsUser = kmsEnvProperties['kms_user']
      kmsUserOld = self.getOldValue(services, 'kms-env', 'kms_user')
      self.put_proxyuser_value(kmsUser, '*', is_groups=True, services=services, configurations=configurations, put_function=putCoreSiteProperty)
      if kmsUserOld is not None and kmsUser != kmsUserOld:
        putCoreSitePropertyAttribute("hadoop.proxyuser.{0}.groups".format(kmsUserOld), 'delete', 'true')
        services["forced-configurations"].append({"type" : "core-site", "name" : "hadoop.proxyuser.{0}.groups".format(kmsUserOld)})
        services["forced-configurations"].append({"type" : "core-site", "name" : "hadoop.proxyuser.{0}.groups".format(kmsUser)})

    if "HDFS" in servicesList:
      if 'core-site' in services['configurations'] and ('fs.defaultFS' in services['configurations']['core-site']['properties']):
        default_fs = services['configurations']['core-site']['properties']['fs.defaultFS']
        putRangerKmsAuditProperty('xasecure.audit.destination.hdfs.dir', '{0}/{1}/{2}'.format(default_fs,'ranger','audit'))

    required_services = [{'service' : 'YARN', 'config-type': 'yarn-env', 'property-name': 'yarn_user', 'proxy-category': ['hosts', 'users', 'groups']},
                         {'service' : 'SPARK', 'config-type': 'livy-env', 'property-name': 'livy_user', 'proxy-category': ['hosts', 'users', 'groups']}]

    required_services_for_secure = [{'service' : 'HIVE', 'config-type': 'hive-env', 'property-name': 'hive_user', 'proxy-category': ['hosts', 'users']},
                                    {'service' : 'OOZIE', 'config-type': 'oozie-env', 'property-name': 'oozie_user', 'proxy-category': ['hosts', 'users']}]

    if security_enabled:
      required_services.extend(required_services_for_secure)

    # recommendations for kms proxy related properties
    self.recommendKMSProxyUsers(configurations, services, hosts, required_services)

    ambari_user = self.getAmbariUser(services)
    if security_enabled:
      # adding for ambari user
      putRangerKmsSiteProperty('hadoop.kms.proxyuser.{0}.users'.format(ambari_user), '*')
      putRangerKmsSiteProperty('hadoop.kms.proxyuser.{0}.hosts'.format(ambari_user), '*')
      # adding for HTTP
      putRangerKmsSiteProperty('hadoop.kms.proxyuser.HTTP.users', '*')
      putRangerKmsSiteProperty('hadoop.kms.proxyuser.HTTP.hosts', '*')
    else:
      self.deleteKMSProxyUsers(configurations, services, hosts, required_services_for_secure)
      # deleting ambari user proxy properties
      putRangerKmsSitePropertyAttribute('hadoop.kms.proxyuser.{0}.hosts'.format(ambari_user), 'delete', 'true')
      putRangerKmsSitePropertyAttribute('hadoop.kms.proxyuser.{0}.users'.format(ambari_user), 'delete', 'true')
      # deleting HTTP proxy properties
      putRangerKmsSitePropertyAttribute('hadoop.kms.proxyuser.HTTP.hosts', 'delete', 'true')
      putRangerKmsSitePropertyAttribute('hadoop.kms.proxyuser.HTTP.users', 'delete', 'true')

  def recommendRangerKMSConfigurationsFromHDP25(self, configurations, clusterData, services, hosts):

    security_enabled = self.isSecurityEnabled(services)
    required_services = [{'service' : 'RANGER', 'config-type': 'ranger-env', 'property-name': 'ranger_user', 'proxy-category': ['hosts', 'users', 'groups']},
                        {'service' : 'SPARK2', 'config-type': 'livy2-env', 'property-name': 'livy2_user', 'proxy-category': ['hosts', 'users', 'groups']}]

    if security_enabled:
      # recommendations for kms proxy related properties
      self.recommendKMSProxyUsers(configurations, services, hosts, required_services)
    else:
      self.deleteKMSProxyUsers(configurations, services, hosts, required_services)

  def recommendRangerKMSConfigurationsFromHDP26(self, configurations, clusterData, services, hosts):
    putRangerKmsEnvProperty = self.putProperty(configurations, "kms-env", services)

    ranger_kms_ssl_enabled = False
    ranger_kms_ssl_port = "9393"
    if 'ranger-kms-site' in services['configurations'] and 'ranger.service.https.attrib.ssl.enabled' in services['configurations']['ranger-kms-site']['properties']:
      ranger_kms_ssl_enabled = services['configurations']['ranger-kms-site']['properties']['ranger.service.https.attrib.ssl.enabled'].lower() == "true"

    if 'ranger-kms-site' in services['configurations'] and 'ranger.service.https.port' in services['configurations']['ranger-kms-site']['properties']:
      ranger_kms_ssl_port = services['configurations']['ranger-kms-site']['properties']['ranger.service.https.port']

    if ranger_kms_ssl_enabled:
      putRangerKmsEnvProperty("kms_port", ranger_kms_ssl_port)
    else:
      putRangerKmsEnvProperty("kms_port", "9292")

  def getDBConnectionHostPort(self, db_type, db_host):
    connection_string = ""
    if db_type is None or db_type == "":
      return connection_string
    else:
      colon_count = db_host.count(':')
      if colon_count == 0:
        if DB_TYPE_DEFAULT_PORT_MAP.has_key(db_type):
          connection_string = db_host + ":" + DB_TYPE_DEFAULT_PORT_MAP[db_type]
        else:
          connection_string = db_host
      elif colon_count == 1:
        connection_string = db_host
      elif colon_count == 2:
        connection_string = db_host

    return connection_string

  def getOracleDBConnectionHostPort(self, db_type, db_host, rangerDbName):
    connection_string = self.getDBConnectionHostPort(db_type, db_host)
    colon_count = db_host.count(':')
    if colon_count == 1 and '/' in db_host:
      connection_string = "//" + connection_string
    elif colon_count == 0 or colon_count == 1:
      connection_string = "//" + connection_string + "/" + rangerDbName if rangerDbName else "//" + connection_string

    return connection_string

  def recommendKMSProxyUsers(self, configurations, services, hosts, requiredServices):
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    putRangerKmsSiteProperty = self.putProperty(configurations, "kms-site", services)
    putRangerKmsSitePropertyAttribute = self.putPropertyAttribute(configurations, "kms-site")

    if 'forced-configurations' not in services:
      services["forced-configurations"] = []

    for index in range(len(requiredServices)):
      service = requiredServices[index]['service']
      config_type = requiredServices[index]['config-type']
      property_name = requiredServices[index]['property-name']
      proxy_category = requiredServices[index]['proxy-category']

      if service in servicesList:
        if config_type in services['configurations'] and property_name in services['configurations'][config_type]['properties']:
          service_user = services['configurations'][config_type]['properties'][property_name]
          service_old_user = self.getOldValue(services, config_type, property_name)

          if 'groups' in proxy_category:
            putRangerKmsSiteProperty('hadoop.kms.proxyuser.{0}.groups'.format(service_user), '*')
          if 'hosts' in proxy_category:
            putRangerKmsSiteProperty('hadoop.kms.proxyuser.{0}.hosts'.format(service_user), '*')
          if 'users' in proxy_category:
            putRangerKmsSiteProperty('hadoop.kms.proxyuser.{0}.users'.format(service_user), '*')

          if service_old_user is not None and service_user != service_old_user:
            if 'groups' in proxy_category:
              putRangerKmsSitePropertyAttribute('hadoop.kms.proxyuser.{0}.groups'.format(service_old_user), 'delete', 'true')
              services["forced-configurations"].append({"type" : "kms-site", "name" : "hadoop.kms.proxyuser.{0}.groups".format(service_old_user)})
              services["forced-configurations"].append({"type" : "kms-site", "name" : "hadoop.kms.proxyuser.{0}.groups".format(service_user)})
            if 'hosts' in proxy_category:
              putRangerKmsSitePropertyAttribute('hadoop.kms.proxyuser.{0}.hosts'.format(service_old_user), 'delete', 'true')
              services["forced-configurations"].append({"type" : "kms-site", "name" : "hadoop.kms.proxyuser.{0}.hosts".format(service_old_user)})
              services["forced-configurations"].append({"type" : "kms-site", "name" : "hadoop.kms.proxyuser.{0}.hosts".format(service_user)})
            if 'users' in proxy_category:
              putRangerKmsSitePropertyAttribute('hadoop.kms.proxyuser.{0}.users'.format(service_old_user), 'delete', 'true')
              services["forced-configurations"].append({"type" : "kms-site", "name" : "hadoop.kms.proxyuser.{0}.users".format(service_old_user)})
              services["forced-configurations"].append({"type" : "kms-site", "name" : "hadoop.kms.proxyuser.{0}.users".format(service_user)})

  def deleteKMSProxyUsers(self, configurations, services, hosts, requiredServices):
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    putRangerKmsSitePropertyAttribute = self.putPropertyAttribute(configurations, "kms-site")

    for index in range(len(requiredServices)):
      service = requiredServices[index]['service']
      config_type = requiredServices[index]['config-type']
      property_name = requiredServices[index]['property-name']
      proxy_category = requiredServices[index]['proxy-category']

      if service in servicesList:
        if config_type in services['configurations'] and property_name in services['configurations'][config_type]['properties']:
          service_user = services['configurations'][config_type]['properties'][property_name]

          if 'groups' in proxy_category:
            putRangerKmsSitePropertyAttribute('hadoop.kms.proxyuser.{0}.groups'.format(service_user), 'delete', 'true')
          if 'hosts' in proxy_category:
            putRangerKmsSitePropertyAttribute('hadoop.kms.proxyuser.{0}.hosts'.format(service_user), 'delete', 'true')
          if 'users' in proxy_category:
            putRangerKmsSitePropertyAttribute('hadoop.kms.proxyuser.{0}.users'.format(service_user), 'delete', 'true')

class RangerKMSValidator(service_advisor.ServiceAdvisor):
  """
  RangerKMS Validator checks the correctness of properties whenever the service is first added or the user attempts to
  change configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(RangerKMSValidator, self)
    self.as_super.__init__(*args, **kwargs)

    self.validators = []
