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

class RangerServiceAdvisor(service_advisor.ServiceAdvisor):

  def __init__(self, *args, **kwargs):
    self.as_super = super(RangerServiceAdvisor, self)
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

    recommender = RangerRecommender()
    recommender.recommendRangerConfigurationsFromHDP206(configurations, clusterData, services, hosts)
    recommender.recommendRangerConfigurationsFromHDP22(configurations, clusterData, services, hosts)
    recommender.recommendRangerConfigurationsFromHDP23(configurations, clusterData, services, hosts)
    recommender.recommendRangerConfigurationsFromHDP25(configurations, clusterData, services, hosts)
    recommender.recommendRangerConfigurationsFromHDP26(configurations, clusterData, services, hosts)

  def getServiceConfigurationsValidationItems(self, configurations, recommendedDefaults, services, hosts):
    """
    Entry point.
    Validate configurations for the service. Return a list of errors.
    The code for this function should be the same for each Service Advisor.
    """
    # Logger.info("Class: %s, Method: %s. Validating Configurations." %
    #            (self.__class__.__name__, inspect.stack()[0][3]))

    validator = RangerValidator()
    # Calls the methods of the validator using arguments,
    # method(siteProperties, siteRecommendations, configurations, services, hosts)
    return validator.validateListOfConfigUsingMethod(configurations, recommendedDefaults, services, hosts, validator.validators)

class RangerRecommender(service_advisor.ServiceAdvisor):
  """
  Ranger Recommender suggests properties when adding the service for the first time or modifying configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(RangerRecommender, self)
    self.as_super.__init__(*args, **kwargs)

  def recommendRangerConfigurationsFromHDP206(self, configurations, clusterData, services, hosts):

    putRangerAdminProperty = self.putProperty(configurations, "admin-properties", services)

    # Build policymgr_external_url
    protocol = 'http'
    ranger_admin_host = 'localhost'
    port = '6080'

    # Check if http is disabled. For HDP-2.3 this can be checked in ranger-admin-site/ranger.service.http.enabled
    # For Ranger-0.4.0 this can be checked in ranger-site/http.enabled
    if ('ranger-site' in services['configurations'] and 'http.enabled' in services['configurations']['ranger-site']['properties'] \
          and services['configurations']['ranger-site']['properties']['http.enabled'].lower() == 'false') or \
            ('ranger-admin-site' in services['configurations'] and 'ranger.service.http.enabled' in services['configurations']['ranger-admin-site']['properties'] \
               and services['configurations']['ranger-admin-site']['properties']['ranger.service.http.enabled'].lower() == 'false'):
      # HTTPS protocol is used
      protocol = 'https'
      # Starting Ranger-0.5.0.2.3 port stored in ranger-admin-site ranger.service.https.port
      if 'ranger-admin-site' in services['configurations'] and \
                      'ranger.service.https.port' in services['configurations']['ranger-admin-site']['properties']:
        port = services['configurations']['ranger-admin-site']['properties']['ranger.service.https.port']
      # In Ranger-0.4.0 port stored in ranger-site https.service.port
      elif 'ranger-site' in services['configurations'] and \
                      'https.service.port' in services['configurations']['ranger-site']['properties']:
        port = services['configurations']['ranger-site']['properties']['https.service.port']
    else:
      # HTTP protocol is used
      # Starting Ranger-0.5.0.2.3 port stored in ranger-admin-site ranger.service.http.port
      if 'ranger-admin-site' in services['configurations'] and \
                      'ranger.service.http.port' in services['configurations']['ranger-admin-site']['properties']:
        port = services['configurations']['ranger-admin-site']['properties']['ranger.service.http.port']
      # In Ranger-0.4.0 port stored in ranger-site http.service.port
      elif 'ranger-site' in services['configurations'] and \
                      'http.service.port' in services['configurations']['ranger-site']['properties']:
        port = services['configurations']['ranger-site']['properties']['http.service.port']

    ranger_admin_hosts = self.getComponentHostNames(services, "RANGER", "RANGER_ADMIN")
    if ranger_admin_hosts:
      if len(ranger_admin_hosts) > 1 \
        and services['configurations'] \
        and 'admin-properties' in services['configurations'] and 'policymgr_external_url' in services['configurations']['admin-properties']['properties'] \
        and services['configurations']['admin-properties']['properties']['policymgr_external_url'] \
        and services['configurations']['admin-properties']['properties']['policymgr_external_url'].strip():

        # in case of HA deployment keep the policymgr_external_url specified in the config
        policymgr_external_url = services['configurations']['admin-properties']['properties']['policymgr_external_url']
      else:

        ranger_admin_host = ranger_admin_hosts[0]
        policymgr_external_url = "%s://%s:%s" % (protocol, ranger_admin_host, port)

      putRangerAdminProperty('policymgr_external_url', policymgr_external_url)

    rangerServiceVersion = [service['StackServices']['service_version'] for service in services["services"] if service['StackServices']['service_name'] == 'RANGER'][0]
    if rangerServiceVersion == '0.4.0':
      # Recommend ldap settings based on ambari.properties configuration
      # If 'ambari.ldap.isConfigured' == true
      # For Ranger version 0.4.0
      if 'ambari-server-properties' in services and \
                      'ambari.ldap.isConfigured' in services['ambari-server-properties'] and \
                      services['ambari-server-properties']['ambari.ldap.isConfigured'].lower() == "true":
        putUserSyncProperty = self.putProperty(configurations, "usersync-properties", services)
        serverProperties = services['ambari-server-properties']
        if 'authentication.ldap.managerDn' in serverProperties:
          putUserSyncProperty('SYNC_LDAP_BIND_DN', serverProperties['authentication.ldap.managerDn'])
        if 'authentication.ldap.primaryUrl' in serverProperties:
          ldap_protocol =  'ldap://'
          if 'authentication.ldap.useSSL' in serverProperties and serverProperties['authentication.ldap.useSSL'] == 'true':
            ldap_protocol =  'ldaps://'
          ldapUrl = ldap_protocol + serverProperties['authentication.ldap.primaryUrl'] if serverProperties['authentication.ldap.primaryUrl'] else serverProperties['authentication.ldap.primaryUrl']
          putUserSyncProperty('SYNC_LDAP_URL', ldapUrl)
        if 'authentication.ldap.userObjectClass' in serverProperties:
          putUserSyncProperty('SYNC_LDAP_USER_OBJECT_CLASS', serverProperties['authentication.ldap.userObjectClass'])
        if 'authentication.ldap.usernameAttribute' in serverProperties:
          putUserSyncProperty('SYNC_LDAP_USER_NAME_ATTRIBUTE', serverProperties['authentication.ldap.usernameAttribute'])


      # Set Ranger Admin Authentication method
      if 'admin-properties' in services['configurations'] and 'usersync-properties' in services['configurations'] and \
                      'SYNC_SOURCE' in services['configurations']['usersync-properties']['properties']:
        rangerUserSyncSource = services['configurations']['usersync-properties']['properties']['SYNC_SOURCE']
        authenticationMethod = rangerUserSyncSource.upper()
        if authenticationMethod != 'FILE':
          putRangerAdminProperty('authentication_method', authenticationMethod)

      # Recommend xasecure.audit.destination.hdfs.dir
      # For Ranger version 0.4.0
      servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
      putRangerEnvProperty = self.putProperty(configurations, "ranger-env", services)
      include_hdfs = "HDFS" in servicesList
      if include_hdfs:
        if 'core-site' in services['configurations'] and ('fs.defaultFS' in services['configurations']['core-site']['properties']):
          default_fs = services['configurations']['core-site']['properties']['fs.defaultFS']
          default_fs += '/ranger/audit/%app-type%/%time:yyyyMMdd%'
          putRangerEnvProperty('xasecure.audit.destination.hdfs.dir', default_fs)

      # Recommend Ranger Audit properties for ranger supported services
      # For Ranger version 0.4.0
      ranger_services = [
        {'service_name': 'HDFS', 'audit_file': 'ranger-hdfs-plugin-properties'},
        {'service_name': 'HBASE', 'audit_file': 'ranger-hbase-plugin-properties'},
        {'service_name': 'HIVE', 'audit_file': 'ranger-hive-plugin-properties'},
        {'service_name': 'KNOX', 'audit_file': 'ranger-knox-plugin-properties'},
        {'service_name': 'STORM', 'audit_file': 'ranger-storm-plugin-properties'}
      ]

      for item in range(len(ranger_services)):
        if ranger_services[item]['service_name'] in servicesList:
          component_audit_file =  ranger_services[item]['audit_file']
          if component_audit_file in services["configurations"]:
            ranger_audit_dict = [
              {'filename': 'ranger-env', 'configname': 'xasecure.audit.destination.db', 'target_configname': 'XAAUDIT.DB.IS_ENABLED'},
              {'filename': 'ranger-env', 'configname': 'xasecure.audit.destination.hdfs', 'target_configname': 'XAAUDIT.HDFS.IS_ENABLED'},
              {'filename': 'ranger-env', 'configname': 'xasecure.audit.destination.hdfs.dir', 'target_configname': 'XAAUDIT.HDFS.DESTINATION_DIRECTORY'}
            ]
            putRangerAuditProperty = self.putProperty(configurations, component_audit_file, services)

            for item in ranger_audit_dict:
              if item['filename'] in services["configurations"] and item['configname'] in  services["configurations"][item['filename']]["properties"]:
                if item['filename'] in configurations and item['configname'] in  configurations[item['filename']]["properties"]:
                  rangerAuditProperty = configurations[item['filename']]["properties"][item['configname']]
                else:
                  rangerAuditProperty = services["configurations"][item['filename']]["properties"][item['configname']]
                putRangerAuditProperty(item['target_configname'], rangerAuditProperty)

  def recommendRangerConfigurationsFromHDP22(self, configurations, clusterData, services, hosts):
    putRangerEnvProperty = self.putProperty(configurations, "ranger-env")
    cluster_env = self.getServicesSiteProperties(services, "cluster-env")
    security_enabled = cluster_env is not None and "security_enabled" in cluster_env and \
                       cluster_env["security_enabled"].lower() == "true"
    if "ranger-env" in configurations and not security_enabled:
      putRangerEnvProperty("ranger-storm-plugin-enabled", "No")

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

  def recommendRangerUrlConfigurations(self, configurations, services, requiredServices):
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

    policymgr_external_url = ""
    if 'admin-properties' in services['configurations'] and 'policymgr_external_url' in services['configurations']['admin-properties']['properties']:
      if 'admin-properties' in configurations and 'policymgr_external_url' in configurations['admin-properties']['properties']:
        policymgr_external_url = configurations['admin-properties']['properties']['policymgr_external_url']
      else:
        policymgr_external_url = services['configurations']['admin-properties']['properties']['policymgr_external_url']

    for index in range(len(requiredServices)):
      if requiredServices[index]['service_name'] in servicesList:
        component_config_type = requiredServices[index]['config_type']
        component_name = requiredServices[index]['service_name']
        component_config_property = 'ranger.plugin.{0}.policy.rest.url'.format(component_name.lower())
        if requiredServices[index]['service_name'] == 'RANGER_KMS':
          component_config_property = 'ranger.plugin.kms.policy.rest.url'
        putRangerSecurityProperty = self.putProperty(configurations, component_config_type, services)
        if component_config_type in services["configurations"] and component_config_property in services["configurations"][component_config_type]["properties"]:
          putRangerSecurityProperty(component_config_property, policymgr_external_url)

  def recommendRangerConfigurationsFromHDP23(self, configurations, clusterData, services, hosts):
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    putRangerAdminProperty = self.putProperty(configurations, "ranger-admin-site", services)
    putRangerEnvProperty = self.putProperty(configurations, "ranger-env", services)
    putRangerUgsyncSite = self.putProperty(configurations, "ranger-ugsync-site", services)

    if 'admin-properties' in services['configurations'] and ('DB_FLAVOR' in services['configurations']['admin-properties']['properties'])\
      and ('db_host' in services['configurations']['admin-properties']['properties']) and ('db_name' in services['configurations']['admin-properties']['properties']):

      rangerDbFlavor = services['configurations']["admin-properties"]["properties"]["DB_FLAVOR"]
      rangerDbHost =   services['configurations']["admin-properties"]["properties"]["db_host"]
      rangerDbName =   services['configurations']["admin-properties"]["properties"]["db_name"]
      ranger_db_url_dict = {
        'MYSQL': {'ranger.jpa.jdbc.driver': 'com.mysql.jdbc.Driver',
                  'ranger.jpa.jdbc.url': 'jdbc:mysql://' + self.getDBConnectionHostPort(rangerDbFlavor, rangerDbHost) + '/' + rangerDbName},
        'ORACLE': {'ranger.jpa.jdbc.driver': 'oracle.jdbc.driver.OracleDriver',
                   'ranger.jpa.jdbc.url': 'jdbc:oracle:thin:@' + self.getOracleDBConnectionHostPort(rangerDbFlavor, rangerDbHost, rangerDbName)},
        'POSTGRES': {'ranger.jpa.jdbc.driver': 'org.postgresql.Driver',
                     'ranger.jpa.jdbc.url': 'jdbc:postgresql://' + self.getDBConnectionHostPort(rangerDbFlavor, rangerDbHost) + '/' + rangerDbName},
        'MSSQL': {'ranger.jpa.jdbc.driver': 'com.microsoft.sqlserver.jdbc.SQLServerDriver',
                  'ranger.jpa.jdbc.url': 'jdbc:sqlserver://' + self.getDBConnectionHostPort(rangerDbFlavor, rangerDbHost) + ';databaseName=' + rangerDbName},
        'SQLA': {'ranger.jpa.jdbc.driver': 'sap.jdbc4.sqlanywhere.IDriver',
                 'ranger.jpa.jdbc.url': 'jdbc:sqlanywhere:host=' + self.getDBConnectionHostPort(rangerDbFlavor, rangerDbHost) + ';database=' + rangerDbName}
      }
      rangerDbProperties = ranger_db_url_dict.get(rangerDbFlavor, ranger_db_url_dict['MYSQL'])
      for key in rangerDbProperties:
        putRangerAdminProperty(key, rangerDbProperties.get(key))

      if 'admin-properties' in services['configurations'] and ('DB_FLAVOR' in services['configurations']['admin-properties']['properties']) \
        and ('db_host' in services['configurations']['admin-properties']['properties']):

        rangerDbFlavor = services['configurations']["admin-properties"]["properties"]["DB_FLAVOR"]
        rangerDbHost =   services['configurations']["admin-properties"]["properties"]["db_host"]
        ranger_db_privelege_url_dict = {
          'MYSQL': {'ranger_privelege_user_jdbc_url': 'jdbc:mysql://' + self.getDBConnectionHostPort(rangerDbFlavor, rangerDbHost)},
          'ORACLE': {'ranger_privelege_user_jdbc_url': 'jdbc:oracle:thin:@' + self.getOracleDBConnectionHostPort(rangerDbFlavor, rangerDbHost, None)},
          'POSTGRES': {'ranger_privelege_user_jdbc_url': 'jdbc:postgresql://' + self.getDBConnectionHostPort(rangerDbFlavor, rangerDbHost) + '/postgres'},
          'MSSQL': {'ranger_privelege_user_jdbc_url': 'jdbc:sqlserver://' + self.getDBConnectionHostPort(rangerDbFlavor, rangerDbHost) + ';'},
          'SQLA': {'ranger_privelege_user_jdbc_url': 'jdbc:sqlanywhere:host=' + self.getDBConnectionHostPort(rangerDbFlavor, rangerDbHost) + ';'}
        }
        rangerPrivelegeDbProperties = ranger_db_privelege_url_dict.get(rangerDbFlavor, ranger_db_privelege_url_dict['MYSQL'])
        for key in rangerPrivelegeDbProperties:
          putRangerEnvProperty(key, rangerPrivelegeDbProperties.get(key))

    # Recommend ldap settings based on ambari.properties configuration
    if 'ambari-server-properties' in services and \
        'ambari.ldap.isConfigured' in services['ambari-server-properties'] and \
        services['ambari-server-properties']['ambari.ldap.isConfigured'].lower() == "true":
      serverProperties = services['ambari-server-properties']
      if 'authentication.ldap.baseDn' in serverProperties:
        putRangerUgsyncSite('ranger.usersync.ldap.searchBase', serverProperties['authentication.ldap.baseDn'])
      if 'authentication.ldap.groupMembershipAttr' in serverProperties:
        putRangerUgsyncSite('ranger.usersync.group.memberattributename', serverProperties['authentication.ldap.groupMembershipAttr'])
      if 'authentication.ldap.groupNamingAttr' in serverProperties:
        putRangerUgsyncSite('ranger.usersync.group.nameattribute', serverProperties['authentication.ldap.groupNamingAttr'])
      if 'authentication.ldap.groupObjectClass' in serverProperties:
        putRangerUgsyncSite('ranger.usersync.group.objectclass', serverProperties['authentication.ldap.groupObjectClass'])
      if 'authentication.ldap.managerDn' in serverProperties:
        putRangerUgsyncSite('ranger.usersync.ldap.binddn', serverProperties['authentication.ldap.managerDn'])
      if 'authentication.ldap.primaryUrl' in serverProperties:
        ldap_protocol =  'ldap://'
        if 'authentication.ldap.useSSL' in serverProperties and serverProperties['authentication.ldap.useSSL'] == 'true':
          ldap_protocol =  'ldaps://'
        ldapUrl = ldap_protocol + serverProperties['authentication.ldap.primaryUrl'] if serverProperties['authentication.ldap.primaryUrl'] else serverProperties['authentication.ldap.primaryUrl']
        putRangerUgsyncSite('ranger.usersync.ldap.url', ldapUrl)
      if 'authentication.ldap.userObjectClass' in serverProperties:
        putRangerUgsyncSite('ranger.usersync.ldap.user.objectclass', serverProperties['authentication.ldap.userObjectClass'])
      if 'authentication.ldap.usernameAttribute' in serverProperties:
        putRangerUgsyncSite('ranger.usersync.ldap.user.nameattribute', serverProperties['authentication.ldap.usernameAttribute'])

    # Recommend Ranger Authentication method
    authMap = {
      'org.apache.ranger.unixusersync.process.UnixUserGroupBuilder': 'UNIX',
      'org.apache.ranger.ldapusersync.process.LdapUserGroupBuilder': 'LDAP'
    }

    if 'ranger-ugsync-site' in services['configurations'] and 'ranger.usersync.source.impl.class' in services['configurations']["ranger-ugsync-site"]["properties"]:
      rangerUserSyncClass = services['configurations']["ranger-ugsync-site"]["properties"]["ranger.usersync.source.impl.class"]
      if rangerUserSyncClass in authMap:
        rangerSqlConnectorProperty = authMap.get(rangerUserSyncClass)
        putRangerAdminProperty('ranger.authentication.method', rangerSqlConnectorProperty)


    if 'ranger-env' in services['configurations'] and 'is_solrCloud_enabled' in services['configurations']["ranger-env"]["properties"]:
      isSolrCloudEnabled = services['configurations']["ranger-env"]["properties"]["is_solrCloud_enabled"]  == "true"
    else:
      isSolrCloudEnabled = False

    if isSolrCloudEnabled:
      zookeeper_host_port = self.getZKHostPortString(services)
      ranger_audit_zk_port = ''
      if zookeeper_host_port:
        ranger_audit_zk_port = '{0}/{1}'.format(zookeeper_host_port, 'ranger_audits')
        putRangerAdminProperty('ranger.audit.solr.zookeepers', ranger_audit_zk_port)
    else:
      putRangerAdminProperty('ranger.audit.solr.zookeepers', 'NONE')

    # Recommend ranger.audit.solr.zookeepers and xasecure.audit.destination.hdfs.dir
    include_hdfs = "HDFS" in servicesList
    if include_hdfs:
      if 'core-site' in services['configurations'] and ('fs.defaultFS' in services['configurations']['core-site']['properties']):
        default_fs = services['configurations']['core-site']['properties']['fs.defaultFS']
        putRangerEnvProperty('xasecure.audit.destination.hdfs.dir', '{0}/{1}/{2}'.format(default_fs,'ranger','audit'))

    # Recommend Ranger supported service's audit properties
    ranger_services = [
      {'service_name': 'HDFS', 'audit_file': 'ranger-hdfs-audit'},
      {'service_name': 'YARN', 'audit_file': 'ranger-yarn-audit'},
      {'service_name': 'HBASE', 'audit_file': 'ranger-hbase-audit'},
      {'service_name': 'HIVE', 'audit_file': 'ranger-hive-audit'},
      {'service_name': 'KNOX', 'audit_file': 'ranger-knox-audit'},
      {'service_name': 'KAFKA', 'audit_file': 'ranger-kafka-audit'},
      {'service_name': 'STORM', 'audit_file': 'ranger-storm-audit'}
    ]

    for item in range(len(ranger_services)):
      if ranger_services[item]['service_name'] in servicesList:
        component_audit_file =  ranger_services[item]['audit_file']
        if component_audit_file in services["configurations"]:
          ranger_audit_dict = [
            {'filename': 'ranger-env', 'configname': 'xasecure.audit.destination.db', 'target_configname': 'xasecure.audit.destination.db'},
            {'filename': 'ranger-env', 'configname': 'xasecure.audit.destination.hdfs', 'target_configname': 'xasecure.audit.destination.hdfs'},
            {'filename': 'ranger-env', 'configname': 'xasecure.audit.destination.hdfs.dir', 'target_configname': 'xasecure.audit.destination.hdfs.dir'},
            {'filename': 'ranger-env', 'configname': 'xasecure.audit.destination.solr', 'target_configname': 'xasecure.audit.destination.solr'},
            {'filename': 'ranger-admin-site', 'configname': 'ranger.audit.solr.urls', 'target_configname': 'xasecure.audit.destination.solr.urls'},
            {'filename': 'ranger-admin-site', 'configname': 'ranger.audit.solr.zookeepers', 'target_configname': 'xasecure.audit.destination.solr.zookeepers'}
          ]
          putRangerAuditProperty = self.putProperty(configurations, component_audit_file, services)

          for item in ranger_audit_dict:
            if item['filename'] in services["configurations"] and item['configname'] in  services["configurations"][item['filename']]["properties"]:
              if item['filename'] in configurations and item['configname'] in  configurations[item['filename']]["properties"]:
                rangerAuditProperty = configurations[item['filename']]["properties"][item['configname']]
              else:
                rangerAuditProperty = services["configurations"][item['filename']]["properties"][item['configname']]
              putRangerAuditProperty(item['target_configname'], rangerAuditProperty)

    audit_solr_flag = 'false'
    audit_db_flag = 'false'
    ranger_audit_source_type = 'solr'
    if 'ranger-env' in services['configurations'] and 'xasecure.audit.destination.solr' in services['configurations']["ranger-env"]["properties"]:
      audit_solr_flag = services['configurations']["ranger-env"]["properties"]['xasecure.audit.destination.solr']
    if 'ranger-env' in services['configurations'] and 'xasecure.audit.destination.db' in services['configurations']["ranger-env"]["properties"]:
      audit_db_flag = services['configurations']["ranger-env"]["properties"]['xasecure.audit.destination.db']

    if audit_db_flag == 'true' and audit_solr_flag == 'false':
      ranger_audit_source_type = 'db'
    putRangerAdminProperty('ranger.audit.source.type',ranger_audit_source_type)

    knox_host = 'localhost'
    knox_port = '8443'
    if 'KNOX' in servicesList:
      knox_hosts = self.getComponentHostNames(services, "KNOX", "KNOX_GATEWAY")
      if len(knox_hosts) > 0:
        knox_hosts.sort()
        knox_host = knox_hosts[0]
      if 'gateway-site' in services['configurations'] and 'gateway.port' in services['configurations']["gateway-site"]["properties"]:
        knox_port = services['configurations']["gateway-site"]["properties"]['gateway.port']
      putRangerAdminProperty('ranger.sso.providerurl', 'https://{0}:{1}/gateway/knoxsso/api/v1/websso'.format(knox_host, knox_port))

    required_services = [
      {'service_name': 'HDFS', 'config_type': 'ranger-hdfs-security'},
      {'service_name': 'YARN', 'config_type': 'ranger-yarn-security'},
      {'service_name': 'HBASE', 'config_type': 'ranger-hbase-security'},
      {'service_name': 'HIVE', 'config_type': 'ranger-hive-security'},
      {'service_name': 'KNOX', 'config_type': 'ranger-knox-security'},
      {'service_name': 'KAFKA', 'config_type': 'ranger-kafka-security'},
      {'service_name': 'RANGER_KMS','config_type': 'ranger-kms-security'},
      {'service_name': 'STORM', 'config_type': 'ranger-storm-security'}
    ]

    # recommendation for ranger url for ranger-supported plugins
    self.recommendRangerUrlConfigurations(configurations, services, required_services)

  def recommendRangerConfigurationsFromHDP25(self, configurations, clusterData, services, hosts):
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    has_ranger_tagsync = False

    putTagsyncAppProperty = self.putProperty(configurations, "tagsync-application-properties", services)
    putTagsyncSiteProperty = self.putProperty(configurations, "ranger-tagsync-site", services)
    putRangerAdminProperty = self.putProperty(configurations, "ranger-admin-site", services)
    putRangerEnvProperty = self.putProperty(configurations, "ranger-env", services)

    application_properties = self.getServicesSiteProperties(services, "application-properties")

    ranger_tagsync_host = self.getHostsForComponent(services, "RANGER", "RANGER_TAGSYNC")
    has_ranger_tagsync = len(ranger_tagsync_host) > 0

    if 'ATLAS' in servicesList and has_ranger_tagsync:
      atlas_hosts = self.getHostNamesWithComponent("ATLAS", "ATLAS_SERVER", services)
      atlas_host = 'localhost' if len(atlas_hosts) == 0 else atlas_hosts[0]
      protocol = 'http'
      atlas_port = '21000'

      if application_properties and 'atlas.enableTLS' in application_properties and application_properties['atlas.enableTLS'].lower() == 'true':
        protocol = 'https'
        if 'atlas.server.https.port' in application_properties:
          atlas_port = application_properties['atlas.server.https.port']
      else:
        protocol = 'http'
        if application_properties and 'atlas.server.http.port' in application_properties:
          atlas_port = application_properties['atlas.server.http.port']

      atlas_rest_endpoint = '{0}://{1}:{2}'.format(protocol, atlas_host, atlas_port)

      putTagsyncSiteProperty('ranger.tagsync.source.atlas', 'true')
      putTagsyncSiteProperty('ranger.tagsync.source.atlasrest.endpoint', atlas_rest_endpoint)

    zookeeper_host_port = self.getZKHostPortString(services)
    if zookeeper_host_port and has_ranger_tagsync:
      putTagsyncAppProperty('atlas.kafka.zookeeper.connect', zookeeper_host_port)

    if 'KAFKA' in servicesList and has_ranger_tagsync:
      kafka_hosts = self.getHostNamesWithComponent("KAFKA", "KAFKA_BROKER", services)
      kafka_port = '6667'
      if 'kafka-broker' in services['configurations'] and (
          'port' in services['configurations']['kafka-broker']['properties']):
        kafka_port = services['configurations']['kafka-broker']['properties']['port']
      kafka_host_port = []
      for i in range(len(kafka_hosts)):
        kafka_host_port.append(kafka_hosts[i] + ':' + kafka_port)

      final_kafka_host = ",".join(kafka_host_port)
      putTagsyncAppProperty('atlas.kafka.bootstrap.servers', final_kafka_host)

    is_solr_cloud_enabled = False
    if 'ranger-env' in services['configurations'] and 'is_solrCloud_enabled' in services['configurations']['ranger-env']['properties']:
      is_solr_cloud_enabled = services['configurations']['ranger-env']['properties']['is_solrCloud_enabled']  == 'true'

    is_external_solr_cloud_enabled = False
    if 'ranger-env' in services['configurations'] and 'is_external_solrCloud_enabled' in services['configurations']['ranger-env']['properties']:
      is_external_solr_cloud_enabled = services['configurations']['ranger-env']['properties']['is_external_solrCloud_enabled']  == 'true'

    ranger_audit_zk_port = ''

    if 'AMBARI_INFRA' in servicesList and zookeeper_host_port and is_solr_cloud_enabled and not is_external_solr_cloud_enabled:
      zookeeper_host_port = zookeeper_host_port.split(',')
      zookeeper_host_port.sort()
      zookeeper_host_port = ",".join(zookeeper_host_port)
      infra_solr_znode = '/infra-solr'

      if 'infra-solr-env' in services['configurations'] and \
        ('infra_solr_znode' in services['configurations']['infra-solr-env']['properties']):
        infra_solr_znode = services['configurations']['infra-solr-env']['properties']['infra_solr_znode']
        ranger_audit_zk_port = '{0}{1}'.format(zookeeper_host_port, infra_solr_znode)
      putRangerAdminProperty('ranger.audit.solr.zookeepers', ranger_audit_zk_port)
    elif zookeeper_host_port and is_solr_cloud_enabled and is_external_solr_cloud_enabled:
      ranger_audit_zk_port = '{0}/{1}'.format(zookeeper_host_port, 'ranger_audits')
      putRangerAdminProperty('ranger.audit.solr.zookeepers', ranger_audit_zk_port)
    else:
      putRangerAdminProperty('ranger.audit.solr.zookeepers', 'NONE')

    ranger_services = [
      {'service_name': 'HDFS', 'audit_file': 'ranger-hdfs-audit'},
      {'service_name': 'YARN', 'audit_file': 'ranger-yarn-audit'},
      {'service_name': 'HBASE', 'audit_file': 'ranger-hbase-audit'},
      {'service_name': 'HIVE', 'audit_file': 'ranger-hive-audit'},
      {'service_name': 'KNOX', 'audit_file': 'ranger-knox-audit'},
      {'service_name': 'KAFKA', 'audit_file': 'ranger-kafka-audit'},
      {'service_name': 'STORM', 'audit_file': 'ranger-storm-audit'},
      {'service_name': 'RANGER_KMS', 'audit_file': 'ranger-kms-audit'},
      {'service_name': 'ATLAS', 'audit_file': 'ranger-atlas-audit'}
    ]

    for item in range(len(ranger_services)):
      if ranger_services[item]['service_name'] in servicesList:
        component_audit_file =  ranger_services[item]['audit_file']
        if component_audit_file in services["configurations"]:
          ranger_audit_dict = [
            {'filename': 'ranger-admin-site', 'configname': 'ranger.audit.solr.urls', 'target_configname': 'xasecure.audit.destination.solr.urls'},
            {'filename': 'ranger-admin-site', 'configname': 'ranger.audit.solr.zookeepers', 'target_configname': 'xasecure.audit.destination.solr.zookeepers'}
          ]
          putRangerAuditProperty = self.putProperty(configurations, component_audit_file, services)

          for item in ranger_audit_dict:
            if item['filename'] in services["configurations"] and item['configname'] in  services["configurations"][item['filename']]["properties"]:
              if item['filename'] in configurations and item['configname'] in  configurations[item['filename']]["properties"]:
                rangerAuditProperty = configurations[item['filename']]["properties"][item['configname']]
              else:
                rangerAuditProperty = services["configurations"][item['filename']]["properties"][item['configname']]
              putRangerAuditProperty(item['target_configname'], rangerAuditProperty)

    if "HDFS" in servicesList:
      hdfs_user = None
      if "hadoop-env" in services["configurations"] and "hdfs_user" in services["configurations"]["hadoop-env"]["properties"]:
        hdfs_user = services["configurations"]["hadoop-env"]["properties"]["hdfs_user"]
        putRangerAdminProperty('ranger.kms.service.user.hdfs', hdfs_user)

    if "HIVE" in servicesList:
      hive_user = None
      if "hive-env" in services["configurations"] and "hive_user" in services["configurations"]["hive-env"]["properties"]:
        hive_user = services["configurations"]["hive-env"]["properties"]["hive_user"]
        putRangerAdminProperty('ranger.kms.service.user.hive', hive_user)

    ranger_plugins_serviceuser = [
      {'service_name': 'HDFS', 'file_name': 'hadoop-env', 'config_name': 'hdfs_user', 'target_configname': 'ranger.plugins.hdfs.serviceuser'},
      {'service_name': 'HIVE', 'file_name': 'hive-env', 'config_name': 'hive_user', 'target_configname': 'ranger.plugins.hive.serviceuser'},
      {'service_name': 'YARN', 'file_name': 'yarn-env', 'config_name': 'yarn_user', 'target_configname': 'ranger.plugins.yarn.serviceuser'},
      {'service_name': 'HBASE', 'file_name': 'hbase-env', 'config_name': 'hbase_user', 'target_configname': 'ranger.plugins.hbase.serviceuser'},
      {'service_name': 'KNOX', 'file_name': 'knox-env', 'config_name': 'knox_user', 'target_configname': 'ranger.plugins.knox.serviceuser'},
      {'service_name': 'STORM', 'file_name': 'storm-env', 'config_name': 'storm_user', 'target_configname': 'ranger.plugins.storm.serviceuser'},
      {'service_name': 'KAFKA', 'file_name': 'kafka-env', 'config_name': 'kafka_user', 'target_configname': 'ranger.plugins.kafka.serviceuser'},
      {'service_name': 'RANGER_KMS', 'file_name': 'kms-env', 'config_name': 'kms_user', 'target_configname': 'ranger.plugins.kms.serviceuser'},
      {'service_name': 'ATLAS', 'file_name': 'atlas-env', 'config_name': 'metadata_user', 'target_configname': 'ranger.plugins.atlas.serviceuser'}
    ]

    for item in range(len(ranger_plugins_serviceuser)):
      if ranger_plugins_serviceuser[item]['service_name'] in servicesList:
        file_name = ranger_plugins_serviceuser[item]['file_name']
        config_name = ranger_plugins_serviceuser[item]['config_name']
        target_configname = ranger_plugins_serviceuser[item]['target_configname']
        if file_name in services["configurations"] and config_name in services["configurations"][file_name]["properties"]:
          service_user = services["configurations"][file_name]["properties"][config_name]
          putRangerAdminProperty(target_configname, service_user)

    if "ATLAS" in servicesList:
      if "ranger-env" in services["configurations"]:
        putAtlasRangerAuditProperty = self.putProperty(configurations, 'ranger-atlas-audit', services)
        xasecure_audit_destination_hdfs = ''
        xasecure_audit_destination_hdfs_dir = ''
        xasecure_audit_destination_solr = ''
        if 'xasecure.audit.destination.hdfs' in configurations['ranger-env']['properties']:
          xasecure_audit_destination_hdfs = configurations['ranger-env']['properties']['xasecure.audit.destination.hdfs']
        else:
          xasecure_audit_destination_hdfs = services['configurations']['ranger-env']['properties']['xasecure.audit.destination.hdfs']

        if 'core-site' in services['configurations'] and ('fs.defaultFS' in services['configurations']['core-site']['properties']):
          xasecure_audit_destination_hdfs_dir = '{0}/{1}/{2}'.format(services['configurations']['core-site']['properties']['fs.defaultFS'] ,'ranger','audit')

        if 'xasecure.audit.destination.solr' in configurations['ranger-env']['properties']:
          xasecure_audit_destination_solr = configurations['ranger-env']['properties']['xasecure.audit.destination.solr']
        else:
          xasecure_audit_destination_solr = services['configurations']['ranger-env']['properties']['xasecure.audit.destination.solr']

        putAtlasRangerAuditProperty('xasecure.audit.destination.hdfs',xasecure_audit_destination_hdfs)
        putAtlasRangerAuditProperty('xasecure.audit.destination.hdfs.dir',xasecure_audit_destination_hdfs_dir)
        putAtlasRangerAuditProperty('xasecure.audit.destination.solr',xasecure_audit_destination_solr)
    required_services = [
      {'service_name': 'ATLAS', 'config_type': 'ranger-atlas-security'}
    ]

    # recommendation for ranger url for ranger-supported plugins
    self.recommendRangerUrlConfigurations(configurations, services, required_services)

  def recommendRangerConfigurationsFromHDP26(self, configurations, clusterData, services, hosts):

    putRangerUgsyncSite = self.putProperty(configurations, 'ranger-ugsync-site', services)

    delta_sync_enabled = False
    if 'ranger-ugsync-site' in services['configurations'] and 'ranger.usersync.ldap.deltasync' in services['configurations']['ranger-ugsync-site']['properties']:
      delta_sync_enabled = services['configurations']['ranger-ugsync-site']['properties']['ranger.usersync.ldap.deltasync'] == "true"

    if delta_sync_enabled:
      putRangerUgsyncSite("ranger.usersync.group.searchenabled", "true")
    else:
      putRangerUgsyncSite("ranger.usersync.group.searchenabled", "false")

class RangerValidator(service_advisor.ServiceAdvisor):
  """
  Ranger Validator checks the correctness of properties whenever the service is first added or the user attempts to
  change configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(RangerValidator, self)
    self.as_super.__init__(*args, **kwargs)

    self.validators = [("ranger-env", self.validateRangerConfigurationsEnvFromHDP22),
                       ("admin-properties", self.validateRangerAdminConfigurationsFromHDP23),
                       ("ranger-env", self.validateRangerConfigurationsEnvFromHDP23),
                       ("ranger-tagsync-site", self.validateRangerTagsyncConfigurationsFromHDP25),
                       ("ranger-ugsync-site", self.validateRangerUsersyncConfigurationsFromHDP26)]

  def validateRangerConfigurationsEnvFromHDP22(self, properties, recommendedDefaults, configurations, services, hosts):
    ranger_env_properties = properties
    validationItems = []
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if "ranger-storm-plugin-enabled" in ranger_env_properties and ranger_env_properties['ranger-storm-plugin-enabled'].lower() == 'yes' and not 'KERBEROS' in servicesList:
      validationItems.append({"config-name": "ranger-storm-plugin-enabled",
                              "item": self.getWarnItem("Ranger Storm plugin should not be enabled in non-kerberos environment.")})
    return self.toConfigurationValidationProblems(validationItems, "ranger-env")

  def validateRangerAdminConfigurationsFromHDP23(self, properties, recommendedDefaults, configurations, services, hosts):
    ranger_site = properties
    validationItems = []
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if 'RANGER' in servicesList and 'policymgr_external_url' in ranger_site:
      policymgr_mgr_url = ranger_site['policymgr_external_url']
      if policymgr_mgr_url.endswith('/'):
        validationItems.append({'config-name':'policymgr_external_url',
                                'item':self.getWarnItem('Ranger External URL should not contain trailing slash "/"')})
    return self.toConfigurationValidationProblems(validationItems,'admin-properties')

  def validateRangerConfigurationsEnvFromHDP23(self, properties, recommendedDefaults, configurations, services, hosts):
    ranger_env_properties = properties
    validationItems = []
    security_enabled = self.isSecurityEnabled(services)

    if "ranger-kafka-plugin-enabled" in ranger_env_properties and ranger_env_properties["ranger-kafka-plugin-enabled"].lower() == 'yes' and not security_enabled:
      validationItems.append({"config-name": "ranger-kafka-plugin-enabled",
                              "item": self.getWarnItem(
                                "Ranger Kafka plugin should not be enabled in non-kerberos environment.")})

    validationProblems = self.toConfigurationValidationProblems(validationItems, "ranger-env")
    return validationProblems

  def validateRangerTagsyncConfigurationsFromHDP25(self, properties, recommendedDefaults, configurations, services, hosts):
    ranger_tagsync_properties = properties
    validationItems = []
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

    has_atlas = False
    if "RANGER" in servicesList:
      has_atlas = not "ATLAS" in servicesList

      if has_atlas and 'ranger.tagsync.source.atlas' in ranger_tagsync_properties and \
                      ranger_tagsync_properties['ranger.tagsync.source.atlas'].lower() == 'true':
        validationItems.append({"config-name": "ranger.tagsync.source.atlas",
                                "item": self.getWarnItem(
                                  "Need to Install ATLAS service to set ranger.tagsync.source.atlas as true.")})

    return self.toConfigurationValidationProblems(validationItems, "ranger-tagsync-site")

  def validateRangerUsersyncConfigurationsFromHDP26(self, properties, recommendedDefaults, configurations, services, hosts):
    ranger_usersync_properties = properties
    validationItems = []

    delta_sync_enabled = 'ranger.usersync.ldap.deltasync' in ranger_usersync_properties \
      and ranger_usersync_properties['ranger.usersync.ldap.deltasync'].lower() == 'true'
    group_sync_enabled = 'ranger.usersync.group.searchenabled' in ranger_usersync_properties \
      and ranger_usersync_properties['ranger.usersync.group.searchenabled'].lower() == 'true'
    usersync_source_ldap_enabled = 'ranger.usersync.source.impl.class' in ranger_usersync_properties \
      and ranger_usersync_properties['ranger.usersync.source.impl.class'] == 'org.apache.ranger.ldapusersync.process.LdapUserGroupBuilder'

    if usersync_source_ldap_enabled and delta_sync_enabled and not group_sync_enabled:
      validationItems.append({"config-name": "ranger.usersync.group.searchenabled",
                            "item": self.getWarnItem(
                            "Need to set ranger.usersync.group.searchenabled as true, as ranger.usersync.ldap.deltasync is enabled")})

    return self.toConfigurationValidationProblems(validationItems, "ranger-ugsync-site")