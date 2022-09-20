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
import math



SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
STACKS_DIR = os.path.join(SCRIPT_DIR, "../../../../")
PARENT_FILE = os.path.join(STACKS_DIR, "service_advisor.py")

try:
  if "BASE_SERVICE_ADVISOR" in os.environ:
    PARENT_FILE = os.environ["BASE_SERVICE_ADVISOR"]
  with open(PARENT_FILE, "rb") as fp:
    service_advisor = imp.load_module("service_advisor", fp, PARENT_FILE, (".py", "rb", imp.PY_SOURCE))
except Exception as e:
  traceback.print_exc()
  print "Failed to load parent"

class HiveServiceAdvisor(service_advisor.ServiceAdvisor):

  def __init__(self, *args, **kwargs):
    self.as_super = super(HiveServiceAdvisor, self)
    self.as_super.__init__(*args, **kwargs)

    self.initialize_logger("HiveServiceAdvisor")

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
      "HIVE_SERVER": {6: 1, 31: 2, "else": 4},
      "HIVE_METASTORE": {6: 1, 31: 2, "else": 4}
    })

  def getServiceComponentLayoutValidations(self, services, hosts):
    """
    Get a list of errors.
    Must be overriden in child class.
    """

    return self.getServiceComponentCardinalityValidations(services, hosts, "HIVE")

  def getServiceConfigurationRecommendations(self, configurations, clusterData, services, hosts):
    """
    Entry point.
    Must be overriden in child class.
    """
    # self.logger.info("Class: %s, Method: %s. Recommending Service Configurations." %
    #             (self.__class__.__name__, inspect.stack()[0][3]))

    recommender = HiveRecommender()
    recommender.recommendHiveConfigurationsFromHDP30(configurations, clusterData, services, hosts)



  def getServiceConfigurationsValidationItems(self, configurations, recommendedDefaults, services, hosts):
    """
    Entry point.
    Validate configurations for the service. Return a list of errors.
    The code for this function should be the same for each Service Advisor.
    """
    # self.logger.info("Class: %s, Method: %s. Validating Configurations." %
    #             (self.__class__.__name__, inspect.stack()[0][3]))

    validator = HiveValidator()
    # Calls the methods of the validator using arguments,
    # method(siteProperties, siteRecommendations, configurations, services, hosts)
    return validator.validateListOfConfigUsingMethod(configurations, recommendedDefaults, services, hosts, validator.validators)

  @staticmethod
  def isKerberosEnabled(services, configurations):
    """
    Determines if security is enabled by testing the value of hive-site/hive.server2.authentication enabled.
    If the property exists and is equal to "kerberos", then is it enabled; otherwise is it assumed to be
    disabled.

    :type services: dict
    :param services: the dictionary containing the existing configuration values
    :type configurations: dict
    :param configurations: the dictionary containing the updated configuration values
    :rtype: bool
    :return: True or False
    """
    if configurations and "hive-site" in configurations and \
            "hive.server2.authentication" in configurations["hive-site"]["properties"]:
      return configurations["hive-site"]["properties"]["hive.server2.authentication"].lower() == "kerberos"
    elif services and "hive-site" in services["configurations"] and \
            "hive.server2.authentication" in services["configurations"]["hive-site"]["properties"]:
      return services["configurations"]["hive-site"]["properties"]["hive.server2.authentication"].lower() == "kerberos"
    else:
      return False




class HiveRecommender(service_advisor.ServiceAdvisor):
  """
  Hive Recommender suggests properties when adding the service for the first time or modifying configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(HiveRecommender, self)
    self.as_super.__init__(*args, **kwargs)
    self.HIVE_INTERACTIVE_SITE = "hive-interactive-site"


  def recommendHiveConfigurationsFromHDP30(self, configurations, clusterData, services, hosts):
    hiveSiteProperties = self.getSiteProperties(services["configurations"], "hive-site")
    hiveEnvProperties = self.getSiteProperties(services["configurations"], "hive-env")
    
    putHiveEnvProperty = self.putProperty(configurations, "hive-env", services)
    putHiveSiteProperty = self.putProperty(configurations, "hive-site", services)
    putHiveProperty = self.putProperty(configurations, "hive-site", services)
    putHiveServerProperty = self.putProperty(configurations, "hiveserver2-site", services)
    putHiveInteractiveEnvProperty = self.putProperty(configurations, "hive-interactive-env", services)
    putHiveInteractiveSiteProperty = self.putProperty(configurations, self.HIVE_INTERACTIVE_SITE, services)
    putRangerHivePluginProperty = self.putProperty(configurations, "ranger-hive-plugin-properties", services)
    putHiveAtlasHookProperty = self.putProperty(configurations, "hive-atlas-application.properties", services)

    putHiveSitePropertyAttribute = self.putPropertyAttribute(configurations, "hive-site")
    putHiveEnvPropertyAttribute = self.putPropertyAttribute(configurations, "hive-env")
    putHiveServerPropertyAttribute = self.putPropertyAttribute(configurations, "hiveserver2-site")
    putHiveInteractiveEnvPropertyAttribute = self.putPropertyAttribute(configurations, "hive-interactive-env")
    putHiveInteractiveSitePropertyAttribute = self.putPropertyAttribute(configurations, "hive-interactive-site")
    putHiveAtlasHookPropertyAttribute = self.putPropertyAttribute(configurations,"hive-atlas-application.properties")

    hive_server_hosts = self.getHostsWithComponent("HIVE", "HIVE_SERVER", services, hosts)
    hive_server_interactive_hosts = self.getHostsWithComponent("HIVE", "HIVE_SERVER_INTERACTIVE", services, hosts)
    hive_client_hosts = self.getHostsWithComponent("HIVE", "HIVE_CLIENT", services, hosts)

    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    
    # druid is not in list of services to be installed
    if 'DRUID' in servicesList:
      putHiveInteractiveSiteProperty = self.putProperty(configurations, "hive-interactive-site", services)

      druid_coordinator_host_port = self.druid_host('DRUID_COORDINATOR', 'druid-coordinator', services, hosts, default_host='localhost:8081')
      druid_overlord_host_port = self.druid_host('DRUID_OVERLORD', 'druid-overlord', services, hosts, default_host='localhost:8090')
      druid_broker_host_port = self.druid_host('DRUID_ROUTER', 'druid-router', services, hosts)
      if druid_broker_host_port is None:
        druid_broker_host_port = self.druid_host('DRUID_BROKER', 'druid-broker', services, hosts, default_host='localhost:8083')

      druid_metadata_uri = ""
      druid_metadata_user = ""
      druid_metadata_type = ""
      if 'druid-common' in services['configurations']:
        druid_metadata_uri = services['configurations']['druid-common']['properties']['druid.metadata.storage.connector.connectURI']
        druid_metadata_type = services['configurations']['druid-common']['properties']['druid.metadata.storage.type']
        if 'druid.metadata.storage.connector.user' in services['configurations']['druid-common']['properties']:
          druid_metadata_user = services['configurations']['druid-common']['properties']['druid.metadata.storage.connector.user']
        else:
          druid_metadata_user = ""

      putHiveInteractiveSiteProperty('hive.druid.broker.address.default', druid_broker_host_port)
      putHiveInteractiveSiteProperty('hive.druid.coordinator.address.default', druid_coordinator_host_port)
      putHiveInteractiveSiteProperty('hive.druid.overlord.address.default', druid_overlord_host_port)
      putHiveInteractiveSiteProperty('hive.druid.metadata.uri', druid_metadata_uri)
      putHiveInteractiveSiteProperty('hive.druid.metadata.username', druid_metadata_user)
      putHiveInteractiveSiteProperty('hive.druid.metadata.db.type', druid_metadata_type)

    # javax.jdo.option.ConnectionURL recommendations
    if hiveEnvProperties and self.checkSiteProperties(hiveEnvProperties, "hive_database", "hive_database_type"):
      putHiveEnvProperty("hive_database_type", self.getDBTypeAlias(hiveEnvProperties["hive_database"]))
    if hiveEnvProperties and hiveSiteProperties and self.checkSiteProperties(hiveSiteProperties, "javax.jdo.option.ConnectionDriverName") and self.checkSiteProperties(hiveEnvProperties, "hive_database"):
      putHiveProperty("javax.jdo.option.ConnectionDriverName", self.getDBDriver(hiveEnvProperties["hive_database"]))
    if hiveSiteProperties and hiveEnvProperties and self.checkSiteProperties(hiveSiteProperties, "ambari.hive.db.schema.name", "javax.jdo.option.ConnectionURL") and self.checkSiteProperties(hiveEnvProperties, "hive_database"):
      hiveServerHost = self.getHostWithComponent("HIVE", "HIVE_SERVER", services, hosts)
      hiveDBConnectionURL = hiveSiteProperties["javax.jdo.option.ConnectionURL"]
      protocol = self.getProtocol(hiveEnvProperties["hive_database"])
      oldSchemaName = self.getOldValue(services, "hive-site", "ambari.hive.db.schema.name")
      oldDBType = self.getOldValue(services, "hive-env", "hive_database")
      # under these if constructions we are checking if hive server hostname available,
      # if it's default db connection url with "localhost" or if schema name was changed or if db type was changed (only for db type change from default mysql to existing mysql)
      # or if protocol according to current db type differs with protocol in db connection url(other db types changes)
      if hiveServerHost is not None:
        if (hiveDBConnectionURL and "//localhost" in hiveDBConnectionURL) or oldSchemaName or oldDBType or (protocol and hiveDBConnectionURL and not hiveDBConnectionURL.startswith(protocol)):
          dbConnection = self.getDBConnectionString(hiveEnvProperties["hive_database"]).format(hiveServerHost["Hosts"]["host_name"], hiveSiteProperties["ambari.hive.db.schema.name"])
          putHiveProperty("javax.jdo.option.ConnectionURL", dbConnection)

    # Transactions
    cpu_count = 0
    for hostData in hive_server_hosts:
      cpu_count = max(cpu_count, hostData["Hosts"]["cpu_count"])
    putHiveSiteProperty("hive.compactor.worker.threads", str(max(cpu_count / 8, 1)))

    hiveMetastoreHost = self.getHostWithComponent("HIVE", "HIVE_METASTORE", services, hosts)
    if hiveMetastoreHost is not None and len(hiveMetastoreHost) > 0:
      putHiveSiteProperty("hive.metastore.uris", "thrift://" + hiveMetastoreHost["Hosts"]["host_name"] + ":9083")

    # DAS Hook
    putHiveEnvProperty("hive_timeline_logging_enabled", "false")
    das_hook_class = "org.apache.hadoop.hive.ql.hooks.HiveProtoLoggingHook"

    hooks_properties = ["hive.exec.pre.hooks", "hive.exec.post.hooks", "hive.exec.failure.hooks"]
    for hooks_property in hooks_properties:
      if hooks_property in configurations["hive-site"]["properties"]:
        hooks_value = configurations["hive-site"]["properties"][hooks_property]
      else:
        hooks_value = " "
      if das_hook_class not in hooks_value:
        if hooks_value == " ":
          hooks_value = das_hook_class
        else:
          hooks_value = hooks_value + "," + das_hook_class
        # Put updated hooks property
        # Maybe org.apache.hadoop.hive.ql.hooks.HiveProtoLoggingHook has a bug, so I comment out this code
        # putHiveSiteProperty(hooks_property, hooks_value)

    if not "yarn-site" in configurations:
      self.calculateYarnAllocationSizes(configurations, services, hosts)


    containerSize = clusterData["mapMemory"] if clusterData["mapMemory"] > 2048 else int(clusterData["reduceMemory"])
    containerSize = min(clusterData["containers"] * clusterData["ramPerContainer"], containerSize)
    container_size_bytes = int(containerSize)*1024*1024

    yarnMaxAllocationSize = min(30 * int(configurations["yarn-site"]["properties"]["yarn.scheduler.minimum-allocation-mb"]), int(configurations["yarn-site"]["properties"]["yarn.scheduler.maximum-allocation-mb"]))
    #duplicate tez task resource calc logic, direct dependency doesn't look good here (in case of Hive without Tez)
    tez_container_size = min(containerSize, yarnMaxAllocationSize)
    putHiveSitePropertyAttribute("hive.tez.container.size", "minimum", int(configurations["yarn-site"]["properties"]["yarn.scheduler.minimum-allocation-mb"]))
    putHiveSitePropertyAttribute("hive.tez.container.size", "maximum", int(configurations["yarn-site"]["properties"]["yarn.scheduler.maximum-allocation-mb"]))
    if "yarn-site" in configurations:
      if "yarn.scheduler.minimum-allocation-mb" in configurations["yarn-site"]["properties"]:
        putHiveSitePropertyAttribute("hive.tez.container.size", "minimum", int(configurations["yarn-site"]["properties"]["yarn.scheduler.minimum-allocation-mb"]))
        tez_container_size = max(tez_container_size, int(configurations["yarn-site"]["properties"]["yarn.scheduler.minimum-allocation-mb"]))
      if "yarn.scheduler.maximum-allocation-mb" in configurations["yarn-site"]["properties"]:
        putHiveSitePropertyAttribute("hive.tez.container.size", "maximum", int(configurations["yarn-site"]["properties"]["yarn.scheduler.maximum-allocation-mb"]))
        tez_container_size = min(tez_container_size, int(configurations["yarn-site"]["properties"]["yarn.scheduler.maximum-allocation-mb"]))

    putHiveSiteProperty("hive.tez.container.size", tez_container_size)
    tez_container_size_bytes = int(int(tez_container_size)*0.8*1024*1024) # Xmx == 80% of container

    # Memory
    putHiveSiteProperty("hive.auto.convert.join.noconditionaltask.size", int(round(tez_container_size_bytes/3)))
    putHiveSitePropertyAttribute("hive.auto.convert.join.noconditionaltask.size", "maximum", tez_container_size_bytes)

    # CBO
    if "hive-site" in services["configurations"] and "hive.cbo.enable" in services["configurations"]["hive-site"]["properties"]:
      hive_cbo_enable = services["configurations"]["hive-site"]["properties"]["hive.cbo.enable"]
      putHiveSiteProperty("hive.stats.fetch.partition.stats", hive_cbo_enable)
      putHiveSiteProperty("hive.stats.fetch.column.stats", hive_cbo_enable)

    # Interactive Query
    yarn_queues = "default"
    capacitySchedulerProperties = {}
    if "capacity-scheduler" in services["configurations"]:
      if "capacity-scheduler" in services["configurations"]["capacity-scheduler"]["properties"]:
        properties = str(services["configurations"]["capacity-scheduler"]["properties"]["capacity-scheduler"]).split("\n")
        for property in properties:
          key,sep,value = property.partition("=")
          capacitySchedulerProperties[key] = value
      if "yarn.scheduler.capacity.root.queues" in capacitySchedulerProperties:
        yarn_queues = str(capacitySchedulerProperties["yarn.scheduler.capacity.root.queues"])
      elif "yarn.scheduler.capacity.root.queues" in services["configurations"]["capacity-scheduler"]["properties"]:
        yarn_queues =  services["configurations"]["capacity-scheduler"]["properties"]["yarn.scheduler.capacity.root.queues"]
    
    toProcessQueues = yarn_queues.split(",")
    leafQueueNames = set() # Remove duplicates
    while len(toProcessQueues) > 0:
      queue = toProcessQueues.pop()
      queueKey = "yarn.scheduler.capacity.root." + queue + ".queues"
      if queueKey in capacitySchedulerProperties:
        # This is a parent queue - need to add children
        subQueues = capacitySchedulerProperties[queueKey].split(",")
        for subQueue in subQueues:
          toProcessQueues.append(queue + "." + subQueue)
      else:
        # This is a leaf queue
        queueName = queue.split(".")[-1] # Fully qualified queue name does not work, we should use only leaf name
        leafQueueNames.add(queueName)
    leafQueues = [{"label": str(queueName) + " queue", "value": queueName} for queueName in leafQueueNames]
    leafQueues = sorted(leafQueues, key=lambda q:q["value"])
    putHiveSitePropertyAttribute("hive.server2.tez.default.queues", "entries", leafQueues)
    putHiveSiteProperty("hive.server2.tez.default.queues", ",".join([leafQueue["value"] for leafQueue in leafQueues]))

    #HSI HA
    is_hsi_ha = len(hive_server_interactive_hosts) > 1
    putHiveInteractiveSitePropertyAttribute("hive.server2.active.passive.ha.registry.namespace", "visible", str(is_hsi_ha).lower())

    # Security
    if ("configurations" not in services) or ("hive-env" not in services["configurations"]) or \
              ("properties" not in services["configurations"]["hive-env"]) or \
              ("hive_security_authorization" not in services["configurations"]["hive-env"]["properties"]) or \
              str(services["configurations"]["hive-env"]["properties"]["hive_security_authorization"]).lower() == "none":
      putHiveEnvProperty("hive_security_authorization", "None")
    else:
      putHiveEnvProperty("hive_security_authorization", services["configurations"]["hive-env"]["properties"]["hive_security_authorization"])

    # Recommend Ranger Hive authorization as per Ranger Hive plugin property
    if "ranger-env" in services["configurations"] and "hive-env" in services["configurations"] and \
        "ranger-hive-plugin-enabled" in services["configurations"]["ranger-env"]["properties"]:
      rangerEnvHivePluginProperty = services["configurations"]["ranger-env"]["properties"]["ranger-hive-plugin-enabled"]
      rangerEnvHiveAuthProperty = services["configurations"]["hive-env"]["properties"]["hive_security_authorization"]
      if (rangerEnvHivePluginProperty.lower() == "yes"):
        putHiveEnvProperty("hive_security_authorization", "Ranger")
      elif (rangerEnvHiveAuthProperty.lower() == "ranger"):
        putHiveEnvProperty("hive_security_authorization", "None")

    try:
      auth_manager_value = str(configurations["hive-env"]["properties"]["hive.security.metastore.authorization.manager"])
    except KeyError:
      auth_manager_value = "org.apache.hadoop.hive.ql.security.authorization.StorageBasedAuthorizationProvider"
      pass
    auth_manager_values = auth_manager_value.split(",")
    sqlstdauth_class = "org.apache.hadoop.hive.ql.security.authorization.MetaStoreAuthzAPIAuthorizerEmbedOnly"

    # hive_security_authorization == "sqlstdauth"
    if str(configurations["hive-env"]["properties"]["hive_security_authorization"]).lower() == "sqlstdauth":
      putHiveSiteProperty("hive.server2.enable.doAs", "false")
      putHiveServerProperty("hive.security.authorization.enabled", "true")
      putHiveServerProperty("hive.security.authorization.manager", "org.apache.hadoop.hive.ql.security.authorization.plugin.sqlstd.SQLStdHiveAuthorizerFactory")
      putHiveServerProperty("hive.security.authenticator.manager", "org.apache.hadoop.hive.ql.security.SessionStateUserAuthenticator")
      putHiveServerProperty("hive.conf.restricted.list", "hive.security.authenticator.manager,hive.security.authorization.manager,hive.security.metastore.authorization.manager,"
                                                         "hive.security.metastore.authenticator.manager,hive.users.in.admin.role,hive.server2.xsrf.filter.enabled,hive.security.authorization.enabled")
      if sqlstdauth_class not in auth_manager_values:
        auth_manager_values.append(sqlstdauth_class)
    elif sqlstdauth_class in auth_manager_values:
      #remove item from csv
      auth_manager_values = [x for x in auth_manager_values if x != sqlstdauth_class]
      pass
    putHiveSiteProperty("hive.security.metastore.authorization.manager", ",".join(auth_manager_values))

    # hive_security_authorization == "ranger"
    if str(configurations["hive-env"]["properties"]["hive_security_authorization"]).lower() == "ranger":
      putHiveSiteProperty("hive.server2.enable.doAs", "false")
      putHiveServerProperty("hive.security.authorization.enabled", "true")
      putHiveServerProperty("hive.security.authorization.manager", "org.apache.ranger.authorization.hive.authorizer.RangerHiveAuthorizerFactory")
      putHiveServerProperty("hive.security.authenticator.manager", "org.apache.hadoop.hive.ql.security.SessionStateUserAuthenticator")
      putHiveServerProperty("hive.conf.restricted.list", "hive.security.authenticator.manager,hive.security.authorization.manager,hive.security.metastore.authorization.manager,"
                                                         "hive.security.metastore.authenticator.manager,hive.users.in.admin.role,hive.server2.xsrf.filter.enabled,hive.security.authorization.enabled")

    # hive_security_authorization == "None"
    if str(configurations["hive-env"]["properties"]["hive_security_authorization"]).lower() == "none":
      putHiveSiteProperty("hive.server2.enable.doAs", "true")
      putHiveServerProperty("hive.security.authorization.enabled", "false")
      putHiveServerPropertyAttribute("hive.security.authorization.manager", "delete", "true")
      putHiveServerPropertyAttribute("hive.security.authenticator.manager", "delete", "true")
      putHiveServerPropertyAttribute("hive.conf.restricted.list", "delete", "true")

    #Hive authentication
    hive_server2_auth = None
    if "hive-site" in services["configurations"] and "hive.server2.authentication" in services["configurations"]["hive-site"]["properties"]:
      hive_server2_auth = str(services["configurations"]["hive-site"]["properties"]["hive.server2.authentication"]).lower()
    elif "hive.server2.authentication" in configurations["hive-site"]["properties"]:
      hive_server2_auth = str(configurations["hive-site"]["properties"]["hive.server2.authentication"]).lower()

    if hive_server2_auth == "ldap":
      putHiveSiteProperty("hive.server2.authentication.ldap.url", "")
      putHiveSitePropertyAttribute("hive.server2.authentication.ldap.url", "delete", "false")
    else:
      if ("hive.server2.authentication.ldap.url" in configurations["hive-site"]["properties"]) or \
              ("hive-site" not in services["configurations"]) or \
              ("hive-site" in services["configurations"] and "hive.server2.authentication.ldap.url" in services["configurations"]["hive-site"]["properties"]):
        putHiveSitePropertyAttribute("hive.server2.authentication.ldap.url", "delete", "true")

    if hive_server2_auth == "kerberos":
      if "hive-site" in services["configurations"] and "hive.server2.authentication.kerberos.keytab" not in services["configurations"]["hive-site"]["properties"]:
        putHiveSiteProperty("hive.server2.authentication.kerberos.keytab", "")
      if "hive-site" in services["configurations"] and "hive.server2.authentication.kerberos.principal" not in services["configurations"]["hive-site"]["properties"]:
        putHiveSiteProperty("hive.server2.authentication.kerberos.principal", "")
    elif "KERBEROS" not in servicesList: # Since "hive_server2_auth" cannot be relied on within the default, empty recommendations request
      if ("hive.server2.authentication.kerberos.keytab" in configurations["hive-site"]["properties"]) or \
              ("hive-site" not in services["configurations"]) or \
              ("hive-site" in services["configurations"] and "hive.server2.authentication.kerberos.keytab" in services["configurations"]["hive-site"]["properties"]):
        putHiveSitePropertyAttribute("hive.server2.authentication.kerberos.keytab", "delete", "true")
      if ("hive.server2.authentication.kerberos.principal" in configurations["hive-site"]["properties"]) or \
              ("hive-site" not in services["configurations"]) or \
              ("hive-site" in services["configurations"] and "hive.server2.authentication.kerberos.principal" in services["configurations"]["hive-site"]["properties"]):
        putHiveSitePropertyAttribute("hive.server2.authentication.kerberos.principal", "delete", "true")

    if hive_server2_auth == "pam":
      putHiveSiteProperty("hive.server2.authentication.pam.services", "")
    else:
      if ("hive.server2.authentication.pam.services" in configurations["hive-site"]["properties"]) or \
              ("hive-site" not in services["configurations"]) or \
              ("hive-site" in services["configurations"] and "hive.server2.authentication.pam.services" in services["configurations"]["hive-site"]["properties"]):
        putHiveSitePropertyAttribute("hive.server2.authentication.pam.services", "delete", "true")

    if hive_server2_auth == "custom":
      putHiveSiteProperty("hive.server2.custom.authentication.class", "")
    else:
      if ("hive.server2.authentication" in configurations["hive-site"]["properties"]) or \
              ("hive-site" not in services["configurations"]) or \
              ("hive-site" in services["configurations"] and "hive.server2.custom.authentication.class" in services["configurations"]["hive-site"]["properties"]):
        putHiveSitePropertyAttribute("hive.server2.custom.authentication.class", "delete", "true")

    # HiveServer, Client, Metastore heapsize
    hs_heapsize_multiplier = 3.0/8
    hm_heapsize_multiplier = 1.0/8
    # HiveServer2 and HiveMetastore located on the same host
    if hive_server_hosts is not None and len(hive_server_hosts):
      hs_host_ram = hive_server_hosts[0]["Hosts"]["total_mem"]/1024
      putHiveEnvProperty("hive.metastore.heapsize", max(512, int(hs_host_ram*hm_heapsize_multiplier)))
      putHiveEnvProperty("hive.heapsize", max(512, int(hs_host_ram*hs_heapsize_multiplier)))
      putHiveEnvPropertyAttribute("hive.metastore.heapsize", "maximum", max(1024, hs_host_ram))
      putHiveEnvPropertyAttribute("hive.heapsize", "maximum", max(1024, hs_host_ram))

    # if hive using sqla db, then we should add DataNucleus property
    sqla_db_used = "hive-env" in services["configurations"] and "hive_database" in services["configurations"]["hive-env"]["properties"] and \
                   services["configurations"]["hive-env"]["properties"]["hive_database"] == "Existing SQL Anywhere Database"
    if sqla_db_used:
      putHiveSiteProperty("datanucleus.rdbms.datastoreAdapterClassName","org.datanucleus.store.rdbms.adapter.SQLAnywhereAdapter")
    else:
      putHiveSitePropertyAttribute("datanucleus.rdbms.datastoreAdapterClassName", "delete", "true")

    # Atlas
    hooks_value = ""
    if "hive.exec.post.hooks" in configurations["hive-site"]["properties"]:
      hooks_value = configurations["hive-site"]["properties"]["hive.exec.post.hooks"]

    hive_hooks = [x.strip() for x in hooks_value.split(",")]
    hive_hooks = [x for x in hive_hooks if x != ""]
    
    is_atlas_present_in_cluster = "ATLAS" in servicesList

    enable_external_atlas_for_hive = False
    if "hive-atlas-application.properties" in services["configurations"] and "enable.external.atlas.for.hive" in services["configurations"]["hive-atlas-application.properties"]["properties"]:
      enable_external_atlas_for_hive = services["configurations"]["hive-atlas-application.properties"]["properties"]["enable.external.atlas.for.hive"].lower() == "true"

    if is_atlas_present_in_cluster or enable_external_atlas_for_hive:
      putHiveEnvProperty("hive.atlas.hook", "true")
    else:
      putHiveEnvProperty("hive.atlas.hook", "false")

    enable_atlas_hook = False
    if "hive-env" in configurations and "hive.atlas.hook" in configurations["hive-env"]["properties"]:
      enable_atlas_hook = configurations["hive-env"]["properties"]["hive.atlas.hook"] == "true"
    elif "hive-env" in services["configurations"] and "hive.atlas.hook" in services["configurations"]["hive-env"]["properties"]:
      enable_atlas_hook = services["configurations"]["hive-env"]["properties"]["hive.atlas.hook"] == "true"

    atlas_hook_class = "org.apache.atlas.hive.hook.HiveHook"
    if enable_atlas_hook:
      # Append atlas hook if not already present.
      is_atlas_hook_in_config = atlas_hook_class in hive_hooks
      if not is_atlas_hook_in_config:
        hive_hooks.append(atlas_hook_class)
    else:
      # Remove the atlas hook since Atlas service is not present.
      hive_hooks = [x for x in hive_hooks if x != atlas_hook_class]

    # Convert hive_hooks back to a csv, unless there are 0 elements, which should be " "
    hooks_value = " " if len(hive_hooks) == 0 else ",".join(hive_hooks)
    putHiveSiteProperty("hive.exec.post.hooks", hooks_value)

    # This is no longer used in HDP 2.5, but still needed in HDP 2.3 and 2.4
    atlas_server_host_info = self.getHostWithComponent("ATLAS", "ATLAS_SERVER", services, hosts)
    if is_atlas_present_in_cluster and atlas_server_host_info:
      atlas_rest_host = atlas_server_host_info["Hosts"]["host_name"]
      scheme = "http"
      metadata_port = "21000"
      atlas_server_default_https_port = "21443"
      tls_enabled = "false"
      if "application-properties" in services["configurations"]:
        if "atlas.enableTLS" in services["configurations"]["application-properties"]["properties"]:
          tls_enabled = services["configurations"]["application-properties"]["properties"]["atlas.enableTLS"]
        if "atlas.server.http.port" in services["configurations"]["application-properties"]["properties"]:
          metadata_port = services["configurations"]["application-properties"]["properties"]["atlas.server.http.port"]
        if tls_enabled.lower() == "true":
          scheme = "https"
          if "atlas.server.https.port" in services["configurations"]["application-properties"]["properties"]:
            metadata_port =  services["configurations"]["application-properties"]["properties"]["atlas.server.https.port"]
          else:
            metadata_port = atlas_server_default_https_port
      putHiveSiteProperty("atlas.rest.address", "{0}://{1}:{2}".format(scheme, atlas_rest_host, metadata_port))
    else:
      putHiveSitePropertyAttribute("atlas.cluster.name", "delete", "true")
      putHiveSitePropertyAttribute("atlas.rest.address", "delete", "true")

    # For "Hive Server Interactive", if the component exists.
    hsi_hosts = self.getHostsForComponent(services, "HIVE", "HIVE_SERVER_INTERACTIVE")
    hsi_properties = self.getServicesSiteProperties(services, self.HIVE_INTERACTIVE_SITE)

    if len(hsi_hosts) > 0:
      putHiveInteractiveEnvProperty("enable_hive_interactive", "true")

      # Update "hive.llap.daemon.queue.name" property attributes if capacity scheduler is changed.
      if hsi_properties and "hive.llap.daemon.queue.name" in hsi_properties:
          self.setLlapDaemonQueuePropAttributes(services, configurations)

          hsi_conf_properties = self.getSiteProperties(configurations, self.HIVE_INTERACTIVE_SITE)

          hive_tez_default_queue = hsi_properties["hive.llap.daemon.queue.name"]
          if hsi_conf_properties and "hive.llap.daemon.queue.name" in hsi_conf_properties:
            hive_tez_default_queue = hsi_conf_properties["hive.llap.daemon.queue.name"]

          if hive_tez_default_queue:
            putHiveInteractiveSiteProperty("hive.server2.tez.default.queues", hive_tez_default_queue)
            self.logger.debug("Updated 'hive.server2.tez.default.queues' config : '{0}'".format(hive_tez_default_queue))
    else:
      self.logger.info("DBG: Setting 'num_llap_nodes' config's  READ ONLY attribute as 'True'.")
      putHiveInteractiveEnvProperty("enable_hive_interactive", "false")
      putHiveInteractiveEnvPropertyAttribute("num_llap_nodes", "read_only", "true")

    if hsi_properties and "hive.llap.zk.sm.connectionString" in hsi_properties:
      zookeeper_host_port = self.getZKHostPortString(services)
      if zookeeper_host_port:
        putHiveInteractiveSiteProperty("hive.llap.zk.sm.connectionString", zookeeper_host_port)

    hive_user = "hive"
    if "hive-env" in services["configurations"] and "hive_user" in services["configurations"]["hive-env"]["properties"]:
      hive_user = services["configurations"]["hive-env"]["properties"]["hive_user"]

    # Ranger user
    ranger_hive_plugin_enabled = False
    if "hive-env" in configurations and "hive_security_authorization" in configurations["hive-env"]["properties"]:
      ranger_hive_plugin_enabled = (configurations["hive-env"]["properties"]["hive_security_authorization"].lower() == "ranger")
    elif "hive-env" in services["configurations"] and "hive_security_authorization" in services["configurations"]["hive-env"]["properties"]:
      ranger_hive_plugin_enabled = (services["configurations"]["hive-env"]["properties"]["hive_security_authorization"].lower() == "ranger")

    if ranger_hive_plugin_enabled and "ranger-hive-plugin-properties" in services["configurations"] and "REPOSITORY_CONFIG_USERNAME" in services["configurations"]["ranger-hive-plugin-properties"]["properties"]:
      self.logger.info("Setting Hive Repo user for Ranger.")
      putRangerHivePluginProperty("REPOSITORY_CONFIG_USERNAME", hive_user)
    else:
      self.logger.info("Not setting Hive Repo user for Ranger.")

    # Atlas Kerberos settings
    if "hive-atlas-application.properties" in services["configurations"]:
      security_enabled = HiveServiceAdvisor.isKerberosEnabled(services, configurations)
      
      enable_atlas_hook = False
      if "hive-env" in configurations and "hive.atlas.hook" in configurations["hive-env"]["properties"]:
        enable_atlas_hook = configurations["hive-env"]["properties"]["hive.atlas.hook"].lower() == "true"
      elif "hive-env" in services["configurations"] and "hive.atlas.hook" in services["configurations"]["hive-env"]["properties"]:
        enable_atlas_hook = services["configurations"]["hive-env"]["properties"]["hive.atlas.hook"].lower() == "true"
      
      if security_enabled and enable_atlas_hook:
        putHiveAtlasHookProperty("atlas.jaas.ticketBased-KafkaClient.loginModuleControlFlag", "required")
        putHiveAtlasHookProperty("atlas.jaas.ticketBased-KafkaClient.loginModuleName", "com.sun.security.auth.module.Krb5LoginModule")
        putHiveAtlasHookProperty("atlas.jaas.ticketBased-KafkaClient.option.useTicketCache", "true")
      else:
        putHiveAtlasHookPropertyAttribute("atlas.jaas.ticketBased-KafkaClient.loginModuleControlFlag", "delete", "true")
        putHiveAtlasHookPropertyAttribute("atlas.jaas.ticketBased-KafkaClient.loginModuleName", "delete", "true")
        putHiveAtlasHookPropertyAttribute("atlas.jaas.ticketBased-KafkaClient.option.useTicketCache", "delete", "true")

    #beeline-site
    beeline_jdbc_url_default = "llap" if (hive_server_interactive_hosts and not hive_server_hosts) else "container"
    putHiveEnvProperty("beeline_jdbc_url_default", beeline_jdbc_url_default)

  def druid_host(self, component_name, config_type, services, hosts, default_host=None):
    hosts = self.getHostsWithComponent('DRUID', component_name, services, hosts)
    if hosts and config_type in services['configurations']:
      host = hosts[0]['Hosts']['host_name']
      port = services['configurations'][config_type]['properties']['druid.port']
      return "%s:%s" % (host, port)
    else:
      return default_host

  def setLlapDaemonQueuePropAttributes(self, services, configurations):
    """
    Checks and sets the 'Hive Server Interactive' 'hive.llap.daemon.queue.name' config Property Attributes.  Takes into
    account that "capacity-scheduler' may have changed (got updated) in current Stack Advisor invocation.
    """
    self.logger.info("Determining 'hive.llap.daemon.queue.name' config Property Attributes.")
    #TODO Determine if this is doing the right thing if some queue is setup with capacity=0, or is STOPPED. Maybe don't list it.
    putHiveInteractiveSitePropertyAttribute = self.putPropertyAttribute(configurations, self.HIVE_INTERACTIVE_SITE)

    capacity_scheduler_properties = dict()

    # Read "capacity-scheduler" from configurations if we modified and added recommendation to it, as part of current
    # StackAdvisor invocation.
    if "capacity-scheduler" in configurations:
        cap_sched_props_as_dict = configurations["capacity-scheduler"]["properties"]
        if "capacity-scheduler" in cap_sched_props_as_dict:
          cap_sched_props_as_str = configurations["capacity-scheduler"]["properties"]["capacity-scheduler"]
          if cap_sched_props_as_str:
            cap_sched_props_as_str = str(cap_sched_props_as_str).split("\n")
            if len(cap_sched_props_as_str) > 0 and cap_sched_props_as_str[0] != "null":
              # Got "capacity-scheduler" configs as one "\n" separated string
              for property in cap_sched_props_as_str:
                key, sep, value = property.partition("=")
                capacity_scheduler_properties[key] = value
              self.logger.info("'capacity-scheduler' configs is set as a single '\\n' separated string in current invocation. "
                          "count(configurations['capacity-scheduler']['properties']['capacity-scheduler']) = "
                          "{0}".format(len(capacity_scheduler_properties)))
            else:
              self.logger.info("Read configurations['capacity-scheduler']['properties']['capacity-scheduler'] is : {0}".format(cap_sched_props_as_str))
          else:
            self.logger.info("configurations['capacity-scheduler']['properties']['capacity-scheduler'] : {0}.".format(cap_sched_props_as_str))

        # if "capacity_scheduler_properties" is empty, implies we may have "capacity-scheduler" configs as dictionary
        # in configurations, if "capacity-scheduler" changed in current invocation.
        if not capacity_scheduler_properties:
          if isinstance(cap_sched_props_as_dict, dict) and len(cap_sched_props_as_dict) > 1:
            capacity_scheduler_properties = cap_sched_props_as_dict
            self.logger.info("'capacity-scheduler' changed in current Stack Advisor invocation. Retrieved the configs as dictionary from configurations.")
          else:
            self.logger.info("Read configurations['capacity-scheduler']['properties'] is : {0}".format(cap_sched_props_as_dict))
    else:
      self.logger.info("'capacity-scheduler' not modified in the current Stack Advisor invocation.")


    # if "capacity_scheduler_properties" is still empty, implies "capacity_scheduler" wasn't change in current
    # SA invocation. Thus, read it from input : "services".
    if not capacity_scheduler_properties:
      capacity_scheduler_properties, received_as_key_value_pair = self.getCapacitySchedulerProperties(services)
      self.logger.info("'capacity-scheduler' not changed in current Stack Advisor invocation. Retrieved the configs from services.")

    # Get set of current YARN leaf queues.
    leafQueueNames = self.getAllYarnLeafQueues(capacity_scheduler_properties)
    if leafQueueNames:
      leafQueues = [{"label": str(queueName), "value": queueName} for queueName in leafQueueNames]
      leafQueues = sorted(leafQueues, key=lambda q: q["value"])
      putHiveInteractiveSitePropertyAttribute("hive.llap.daemon.queue.name", "entries", leafQueues)
      self.logger.info("'hive.llap.daemon.queue.name' config Property Attributes set to : {0}".format(leafQueues))
    else:
      self.logger.error("Problem retrieving YARN queues. Skipping updating HIVE Server Interactve "
                   "'hive.server2.tez.default.queues' property attributes.")


class HiveValidator(service_advisor.ServiceAdvisor):
  """
  Hive Validator checks the correctness of properties whenever the service is first added or the user attempts to
  change configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(HiveValidator, self)
    self.as_super.__init__(*args, **kwargs)
    self.HIVE_INTERACTIVE_SITE = "hive-interactive-site"
    self.AMBARI_MANAGED_LLAP_QUEUE_NAME = "llap"

    self.validators = [("hive-site", self.validateHiveConfigurationsFromHDP30),
                       ("hive-env", self.validateHiveConfigurationsEnvFromHDP30),
                       ("hiveserver2-site", self.validateHiveServer2ConfigurationsFromHDP30),
                       ("hive-interactive-env", self.validateHiveInteractiveEnvConfigurationsFromHDP30),
                       ("hive-interactive-site", self.validateHiveInteractiveSiteConfigurationsFromHDP30)]


  def validateHiveConfigurationsFromHDP30(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = [ {"config-name": "hive.tez.container.size", "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, "hive.tez.container.size")},
                        {"config-name": "hive.auto.convert.join.noconditionaltask.size", "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, "hive.auto.convert.join.noconditionaltask.size")} ]
    
    hive_site = properties
    hive_env = self.getSiteProperties(configurations, "hive-env")
    yarn_site = self.getSiteProperties(configurations, "yarn-site")
    
    if yarn_site:
      yarnSchedulerMaximumAllocationMb = self.to_number(yarn_site["yarn.scheduler.maximum-allocation-mb"])
      hiveTezContainerSize = self.to_number(properties["hive.tez.container.size"])
      if hiveTezContainerSize is not None and yarnSchedulerMaximumAllocationMb is not None and hiveTezContainerSize > yarnSchedulerMaximumAllocationMb:
        validationItems.append({"config-name": "hive.tez.container.size", "item": self.getWarnItem("hive.tez.container.size is greater than the maximum container size specified in yarn.scheduler.maximum-allocation-mb")})
    
    authentication_property = "hive.server2.authentication"
    ldap_baseDN_property = "hive.server2.authentication.ldap.baseDN"
    ldap_domain_property = "hive.server2.authentication.ldap.Domain"
    if authentication_property in properties and properties[authentication_property].lower() == "ldap" \
        and not (ldap_baseDN_property in properties or ldap_domain_property in properties):
      validationItems.append({"config-name" : authentication_property, "item" :
        self.getWarnItem("According to LDAP value for " + authentication_property + ", you should add " +
            ldap_domain_property + " property, if you are using AD, if not, then " + ldap_baseDN_property + "!")})
    
    hive_enforce_bucketing = "hive.enforce.bucketing"
    if hive_enforce_bucketing in properties and properties[hive_enforce_bucketing].lower() == "false":
      validationItems.append({"config-name" : hive_enforce_bucketing, "item" :
        self.getWarnItem("Set " + hive_enforce_bucketing + " to true otherwise there is a potential of data corruption!")})
    
    sqla_db_used = "hive_database" in hive_env and \
                   hive_env["hive_database"] == "Existing SQL Anywhere Database"
    prop_name = "datanucleus.rdbms.datastoreAdapterClassName"
    prop_value = "org.datanucleus.store.rdbms.adapter.SQLAnywhereAdapter"
    if sqla_db_used:
      if not prop_name in hive_site:
        validationItems.append({"config-name": prop_name,
                              "item": self.getWarnItem(
                              "If Hive using SQL Anywhere db." \
                              " {0} needs to be added with value {1}".format(prop_name,prop_value))})
      elif prop_name in hive_site and hive_site[prop_name] != "org.datanucleus.store.rdbms.adapter.SQLAnywhereAdapter":
        validationItems.append({"config-name": prop_name,
                                "item": self.getWarnItem(
                                  "If Hive using SQL Anywhere db." \
                                  " {0} needs to be set to {1}".format(prop_name,prop_value))})
    
    return self.toConfigurationValidationProblems(validationItems, "hive-site")


  def validateHiveConfigurationsEnvFromHDP30(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = []
    
    hive_env = properties
    hive_site = self.getSiteProperties(configurations, "hive-site")
    hiveserver2_site = self.getSiteProperties(configurations, "hiveserver2-site")
    
    hive_server_hosts = self.getHostsWithComponent("HIVE", "HIVE_SERVER", services, hosts)
    hive_server_interactive_hosts = self.getHostsWithComponent("HIVE", "HIVE_SERVER_INTERACTIVE", services, hosts)
    hive_client_hosts = self.getHostsWithComponent("HIVE", "HIVE_CLIENT", services, hosts)
    
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if "hive_security_authorization" in hive_env and \
        str(hive_env["hive_security_authorization"]).lower() == "none" \
      and str(hiveserver2_site["hive.security.authorization.enabled"]).lower() == "true":
      authorization_item = self.getErrorItem("hive_security_authorization should not be None "
                                             "if hive.security.authorization.enabled is set")
      validationItems.append({"config-name": "hive_security_authorization", "item": authorization_item})
    if "hive_security_authorization" in hive_env and \
        str(hive_env["hive_security_authorization"]).lower() == "ranger":
      # ranger-hive-plugin must be enabled in ranger-env
      if "RANGER" in servicesList:
        ranger_env = self.getServicesSiteProperties(services, "ranger-env")
        if not ranger_env or not "ranger-hive-plugin-enabled" in ranger_env or \
            ranger_env["ranger-hive-plugin-enabled"].lower() != "yes":
          validationItems.append({"config-name": "hive_security_authorization",
                                  "item": self.getWarnItem(
                                    "ranger-env/ranger-hive-plugin-enabled must be enabled when hive_security_authorization is set to Ranger")})
    
    if "hive.server2.authentication" in hive_site and "LDAP" == hive_site["hive.server2.authentication"]:
      if "alert_ldap_username" not in hive_env or hive_env["alert_ldap_username"] == "":
        validationItems.append({"config-name": "alert_ldap_username",
                                "item": self.getWarnItem(
                                  "Provide an user to be used for alerts. Hive authentication type LDAP requires valid LDAP credentials for the alerts.")})
      if "alert_ldap_password" not in hive_env or hive_env["alert_ldap_password"] == "":
        validationItems.append({"config-name": "alert_ldap_password",
                                "item": self.getWarnItem(
                                  "Provide the password for the alert user. Hive authentication type LDAP requires valid LDAP credentials for the alerts.")})
    
    beeline_jdbc_url_default = hive_env["beeline_jdbc_url_default"]
    if beeline_jdbc_url_default not in ["container", "llap"]:
      validationItems.append({"config-name": "beeline_jdbc_url_default",
                                "item": self.getWarnItem(
                                  "beeline_jdbc_url_default should be \"container\" or \"llap\".")})
    if beeline_jdbc_url_default == "container" and not hive_server_hosts and hive_server_interactive_hosts:
      validationItems.append({"config-name": "beeline_jdbc_url_default",
                                "item": self.getWarnItem(
                                  "beeline_jdbc_url_default may not be \"container\" if only HSI is installed.")})
    if beeline_jdbc_url_default == "llap" and not hive_server_interactive_hosts:
      validationItems.append({"config-name": "beeline_jdbc_url_default",
                                "item": self.getWarnItem(
                                  "beeline_jdbc_url_default may not be \"llap\" if no HSI is installed.")})
    
    return self.toConfigurationValidationProblems(validationItems, "hive-env")


  def validateHiveServer2ConfigurationsFromHDP30(self, properties, recommendedDefaults, configurations, services, hosts):

    hive_server2 = properties
    validationItems = []
    #Adding Ranger Plugin logic here
    ranger_plugin_properties = self.getSiteProperties(configurations, "ranger-hive-plugin-properties")
    hive_env_properties = self.getSiteProperties(configurations, "hive-env")
    ranger_plugin_enabled = "hive_security_authorization" in hive_env_properties and hive_env_properties["hive_security_authorization"].lower() == "ranger"
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    ##Add stack validations only if Ranger is enabled.
    if ("RANGER" in servicesList):
      ##Add stack validations for  Ranger plugin enabled.
      if ranger_plugin_enabled:
        prop_name = "hive.security.authorization.manager"
        prop_val = "org.apache.ranger.authorization.hive.authorizer.RangerHiveAuthorizerFactory"
        if prop_name in hive_server2 and hive_server2[prop_name] != prop_val:
          validationItems.append({"config-name": prop_name,
                                  "item": self.getWarnItem(
                                  "If Ranger Hive Plugin is enabled."\
                                  " {0} under hiveserver2-site needs to be set to {1}".format(prop_name,prop_val))})
        prop_name = "hive.security.authenticator.manager"
        prop_val = "org.apache.hadoop.hive.ql.security.SessionStateUserAuthenticator"
        if prop_name in hive_server2 and hive_server2[prop_name] != prop_val:
          validationItems.append({"config-name": prop_name,
                                  "item": self.getWarnItem(
                                  "If Ranger Hive Plugin is enabled."\
                                  " {0} under hiveserver2-site needs to be set to {1}".format(prop_name,prop_val))})
        prop_name = "hive.security.authorization.enabled"
        prop_val = "true"
        if prop_name in hive_server2 and hive_server2[prop_name] != prop_val:
          validationItems.append({"config-name": prop_name,
                                  "item": self.getWarnItem(
                                  "If Ranger Hive Plugin is enabled."\
                                  " {0} under hiveserver2-site needs to be set to {1}".format(prop_name, prop_val))})
        prop_name = "hive.conf.restricted.list"
        prop_vals = "hive.security.authorization.enabled,hive.security.authorization.manager,hive.security.authenticator.manager".split(",")
        current_vals = []
        missing_vals = []
        if hive_server2 and prop_name in hive_server2:
          current_vals = hive_server2[prop_name].split(",")
          current_vals = [x.strip() for x in current_vals]

        for val in prop_vals:
          if not val in current_vals:
            missing_vals.append(val)

        if missing_vals:
          validationItems.append({"config-name": prop_name,
            "item": self.getWarnItem("If Ranger Hive Plugin is enabled."\
            " {0} under hiveserver2-site needs to contain missing value {1}".format(prop_name, ",".join(missing_vals)))})
      ##Add stack validations for  Ranger plugin disabled.
      elif not ranger_plugin_enabled:
        prop_name = "hive.security.authorization.manager"
        prop_val = "org.apache.hadoop.hive.ql.security.authorization.plugin.sqlstd.SQLStdHiveAuthorizerFactory"
        if prop_name in hive_server2 and hive_server2[prop_name] != prop_val:
          validationItems.append({"config-name": prop_name,
                                  "item": self.getWarnItem(
                                  "If Ranger Hive Plugin is disabled."\
                                  " {0} needs to be set to {1}".format(prop_name,prop_val))})
        prop_name = "hive.security.authenticator.manager"
        prop_val = "org.apache.hadoop.hive.ql.security.SessionStateUserAuthenticator"
        if prop_name in hive_server2 and hive_server2[prop_name] != prop_val:
          validationItems.append({"config-name": prop_name,
                                  "item": self.getWarnItem(
                                  "If Ranger Hive Plugin is disabled."\
                                  " {0} needs to be set to {1}".format(prop_name,prop_val))})

    validationProblems = self.toConfigurationValidationProblems(validationItems, "hiveserver2-site")
    return validationProblems


  def validateHiveInteractiveEnvConfigurationsFromHDP30(self, properties, recommendedDefaults, configurations, services, hosts):
    hive_site_env_properties = self.getSiteProperties(configurations, "hive-interactive-env")
    yarn_site_properties = self.getSiteProperties(configurations, "yarn-site")
    validationItems = []
    hsi_hosts = self.getHostsForComponent(services, "HIVE", "HIVE_SERVER_INTERACTIVE")

    # Check for expecting "enable_hive_interactive" is ON given that there is HSI on at least one host present.
    if len(hsi_hosts) > 0:
      # HIVE_SERVER_INTERACTIVE is mapped to a host
      if "enable_hive_interactive" not in hive_site_env_properties or (
            "enable_hive_interactive" in hive_site_env_properties and
            hive_site_env_properties["enable_hive_interactive"].lower() != "true"):

        validationItems.append({"config-name": "enable_hive_interactive",
                                "item": self.getErrorItem(
                                  "HIVE_SERVER_INTERACTIVE requires enable_hive_interactive in hive-interactive-env set to true.")})
    else:
      # no  HIVE_SERVER_INTERACTIVE
      if "enable_hive_interactive" in hive_site_env_properties and hive_site_env_properties[
        "enable_hive_interactive"].lower() != "false":
        validationItems.append({"config-name": "enable_hive_interactive",
                                "item": self.getErrorItem(
                                  "enable_hive_interactive in hive-interactive-env should be set to false.")})

    # Check for "yarn.resourcemanager.scheduler.monitor.enable" config to be true if HSI is ON.
    if yarn_site_properties and "yarn.resourcemanager.scheduler.monitor.enable" in yarn_site_properties:
      scheduler_monitor_enabled = yarn_site_properties["yarn.resourcemanager.scheduler.monitor.enable"]
      if scheduler_monitor_enabled.lower() == "false" and hive_site_env_properties and "enable_hive_interactive" in hive_site_env_properties and \
        hive_site_env_properties["enable_hive_interactive"].lower() == "true":
        validationItems.append({"config-name": "enable_hive_interactive",
                                "item": self.getWarnItem(
                                  "When enabling LLAP, set 'yarn.resourcemanager.scheduler.monitor.enable' to true to ensure that LLAP gets the full allocated capacity.")})

    validationProblems = self.toConfigurationValidationProblems(validationItems, "hive-interactive-env")
    return validationProblems


  def validateHiveInteractiveSiteConfigurationsFromHDP30(self, properties, recommendedDefaults, configurations, services, hosts):
    """
    Does the following validation checks for HIVE_SERVER_INTERACTIVE's hive-interactive-site configs.
        1. Queue selected in 'hive.llap.daemon.queue.name' config should be sized >= to minimum required to run LLAP
           and Hive2 app.
        2. Queue selected in 'hive.llap.daemon.queue.name' config state should not be 'STOPPED'.
        3. 'hive.server2.enable.doAs' config should be set to 'false' for Hive2.
        4. 'Maximum Total Concurrent Queries'(hive.server2.tez.sessions.per.default.queue) should not consume more that 50% of selected queue for LLAP.
        5. if 'llap' queue is selected, in order to run Service Checks, 'remaining available capacity' in cluster is atleast 512 MB.
    """
    validationItems = []
    hsi_hosts = self.getHostsForComponent(services, "HIVE", "HIVE_SERVER_INTERACTIVE")
    llap_queue_name = None
    llap_queue_cap_perc = None
    MIN_ASSUMED_CAP_REQUIRED_FOR_SERVICE_CHECKS = 512
    llap_queue_cap = None
    hsi_site = self.getServicesSiteProperties(services, self.HIVE_INTERACTIVE_SITE)

    if len(hsi_hosts) == 0:
      return []

    # Get total cluster capacity
    node_manager_host_list = self.getHostsForComponent(services, "YARN", "NODEMANAGER")
    node_manager_cnt = len(node_manager_host_list)
    yarn_nm_mem_in_mb = self.get_yarn_nm_mem_in_mb(services, configurations)
    total_cluster_cap = node_manager_cnt * yarn_nm_mem_in_mb
    capacity_scheduler_properties, received_as_key_value_pair = self.getCapacitySchedulerProperties(services)

    if not capacity_scheduler_properties:
      self.logger.warning("Couldn't retrieve 'capacity-scheduler' properties while doing validation checks for Hive Server Interactive.")
      return []

    if hsi_site:
      if "hive.llap.daemon.queue.name" in hsi_site and hsi_site["hive.llap.daemon.queue.name"]:
        llap_queue_name = hsi_site["hive.llap.daemon.queue.name"]
        llap_queue_cap = self.__getSelectedQueueTotalCap(capacity_scheduler_properties, llap_queue_name, total_cluster_cap)

        if llap_queue_cap:
          llap_queue_cap_perc = float(llap_queue_cap * 100 / total_cluster_cap)
          min_reqd_queue_cap_perc = self.min_queue_perc_reqd_for_llap_and_hive_app(services, hosts, configurations)

          # Validate that the selected queue in "hive.llap.daemon.queue.name" should be sized >= to minimum required
          # to run LLAP and Hive2 app.
          if llap_queue_cap_perc < min_reqd_queue_cap_perc:
            errMsg1 = "Selected queue '{0}' capacity ({1}%) is less than minimum required capacity ({2}%) for LLAP " \
                      "app to run".format(llap_queue_name, llap_queue_cap_perc, min_reqd_queue_cap_perc)
            validationItems.append({"config-name": "hive.llap.daemon.queue.name", "item": self.getErrorItem(errMsg1)})
        else:
          self.logger.error("Couldn't retrieve '{0}' queue's capacity from 'capacity-scheduler' while doing validation checks for "
           "Hive Server Interactive.".format(llap_queue_name))

        # Validate that current selected queue in 'hive.llap.daemon.queue.name' state is not STOPPED.
        llap_selected_queue_state = self.__getQueueStateFromCapacityScheduler(capacity_scheduler_properties, llap_queue_name)
        if llap_selected_queue_state:
          if llap_selected_queue_state == "STOPPED":
            errMsg2 = "Selected queue '{0}' current state is : '{1}'. It is required to be in 'RUNNING' state for LLAP to run"\
              .format(llap_queue_name, llap_selected_queue_state)
            validationItems.append({"config-name": "hive.llap.daemon.queue.name","item": self.getErrorItem(errMsg2)})
        else:
          self.logger.error("Couldn't retrieve '{0}' queue's state from 'capacity-scheduler' while doing validation checks for "
                       "Hive Server Interactive.".format(llap_queue_name))
      else:
        self.logger.error("Couldn't retrieve 'hive.llap.daemon.queue.name' config from 'hive-interactive-site' while doing "
                     "validation checks for Hive Server Interactive.")

      # Validate that "hive.server2.enable.doAs" config is not set to "true" for Hive2.
      if "hive.server2.enable.doAs" in hsi_site and hsi_site["hive.server2.enable.doAs"] == "true":
          validationItems.append({"config-name": "hive.server2.enable.doAs", "item": self.getErrorItem("Value should be set to 'false' for Hive2.")})

      # Validate that "Maximum Total Concurrent Queries" (hive.server2.tez.sessions.per.default.queue) is not consuming more that
      # 50% of selected queue for LLAP.
      if llap_queue_cap and "hive.server2.tez.sessions.per.default.queue" in hsi_site:
        num_tez_sessions = hsi_site["hive.server2.tez.sessions.per.default.queue"]
        if num_tez_sessions:
          num_tez_sessions = long(num_tez_sessions)
          yarn_min_container_size = long(self.get_yarn_min_container_size(services, configurations))
          tez_am_container_size = self.calculate_tez_am_container_size(services, long(total_cluster_cap))
          normalized_tez_am_container_size = self._normalizeUp(tez_am_container_size, yarn_min_container_size)
          llap_selected_queue_cap_remaining = llap_queue_cap - (normalized_tez_am_container_size * num_tez_sessions)
          if llap_selected_queue_cap_remaining <= llap_queue_cap/2:
            errMsg3 = " Reducing the 'Maximum Total Concurrent Queries' (value: {0}) is advisable as it is consuming more than 50% of " \
                      "'{1}' queue for LLAP.".format(num_tez_sessions, llap_queue_name)
            validationItems.append({"config-name": "hive.server2.tez.sessions.per.default.queue","item": self.getWarnItem(errMsg3)})
      
      if int(hsi_site["hive.llap.io.memory.size"]) > int(hsi_site["hive.llap.daemon.yarn.container.mb"]):
        errorMessage = "In-Memory Cache per Daemon (value: {0}) may not be more then Memory per Daemon (value: {1})".format(hsi_site["hive.llap.io.memory.size"], hsi_site["hive.llap.daemon.yarn.container.mb"])
        validationItems.append({"config-name": "hive.llap.io.memory.size","item": self.getErrorItem(errorMessage)})
      
    # Validate that "remaining available capacity" in cluster is at least 512 MB, after "llap" queue is selected,
    # in order to run Service Checks.
    if llap_queue_name and llap_queue_cap_perc and llap_queue_name == self.AMBARI_MANAGED_LLAP_QUEUE_NAME:
      curr_selected_queue_for_llap_cap = float(llap_queue_cap_perc) / 100 * total_cluster_cap
      available_cap_in_cluster = total_cluster_cap - curr_selected_queue_for_llap_cap
      if available_cap_in_cluster < MIN_ASSUMED_CAP_REQUIRED_FOR_SERVICE_CHECKS:
        errMsg4 = "Capacity used by '{0}' queue is '{1}'. Service checks may not run as remaining available capacity " \
                   "({2}) in cluster is less than 512 MB.".format(self.AMBARI_MANAGED_LLAP_QUEUE_NAME, curr_selected_queue_for_llap_cap, available_cap_in_cluster)
        validationItems.append({"config-name": "hive.llap.daemon.queue.name","item": self.getWarnItem(errMsg4)})

    validationProblems = self.toConfigurationValidationProblems(validationItems, "hive-interactive-site")
    return validationProblems

  def get_yarn_nm_mem_in_mb(self, services, configurations):
    """
    Gets YARN NodeManager memory in MB (yarn.nodemanager.resource.memory-mb).
    Reads from:
      - configurations (if changed as part of current Stack Advisor invocation (output)), and services["changed-configurations"]
        is empty, else
      - services['configurations'] (input).

    services["changed-configurations"] would be empty is Stack Advisor call if made from Blueprints (1st invocation). Subsequent
    Stack Advisor calls will have it non-empty. We do this because in subsequent invocations, even if Stack Advsior calculates this
    value (configurations), it is finally not recommended, making 'input' value to survive.
    """
    yarn_nm_mem_in_mb = None

    yarn_site = self.getServicesSiteProperties(services, "yarn-site")
    yarn_site_properties = self.getSiteProperties(configurations, "yarn-site")

    # Check if services["changed-configurations"] is empty and "yarn.nodemanager.resource.memory-mb" is modified in current ST invocation.
    if not ("changed-configurations" in services and services["changed-configurations"]) and yarn_site_properties and "yarn.nodemanager.resource.memory-mb" in yarn_site_properties:
      yarn_nm_mem_in_mb = float(yarn_site_properties["yarn.nodemanager.resource.memory-mb"])
    elif yarn_site and "yarn.nodemanager.resource.memory-mb" in yarn_site:
      # Check if "yarn.nodemanager.resource.memory-mb" is input in services array.
      yarn_nm_mem_in_mb = float(yarn_site["yarn.nodemanager.resource.memory-mb"])

    if yarn_nm_mem_in_mb <= 0.0:
      self.logger.warning("'yarn.nodemanager.resource.memory-mb' current value : {0}. Expected value : > 0".format(yarn_nm_mem_in_mb))

    return yarn_nm_mem_in_mb

  def __getQueueCapacityKeyFromCapacityScheduler(self, capacity_scheduler_properties, llap_daemon_selected_queue_name):
    """
    Retrieves the passed in queue's 'capacity' related key from Capacity Scheduler.
    """
    # Identify the key which contains the capacity for "llap_daemon_selected_queue_name".
    cap_sched_keys = capacity_scheduler_properties.keys()
    llap_selected_queue_cap_key =  None
    current_selected_queue_for_llap_cap = None
    for key in cap_sched_keys:
      # Expected capacity prop key is of form : "yarn.scheduler.capacity.<one or more queues in path separated by ".'>.[llap_daemon_selected_queue_name].capacity'
      if key.endswith(llap_daemon_selected_queue_name+".capacity") and key.startswith("yarn.scheduler.capacity.root"):
        self.logger.info("DBG: Selected queue name as: " + key)
        llap_selected_queue_cap_key = key
        break;
    return llap_selected_queue_cap_key

  def __getQueueStateFromCapacityScheduler(self, capacity_scheduler_properties, llap_daemon_selected_queue_name):
    """
    Retrieves the passed in queue's 'state' from Capacity Scheduler.
    """
    # Identify the key which contains the state for "llap_daemon_selected_queue_name".
    cap_sched_keys = capacity_scheduler_properties.keys()
    llap_selected_queue_state_key =  None
    llap_selected_queue_state = None
    for key in cap_sched_keys:
      if key.endswith(llap_daemon_selected_queue_name+".state"):
        llap_selected_queue_state_key = key
        break;
    llap_selected_queue_state = capacity_scheduler_properties.get(llap_selected_queue_state_key)
    return llap_selected_queue_state

  def __getQueueAmFractionFromCapacityScheduler(self, capacity_scheduler_properties, llap_daemon_selected_queue_name):
    """
    Retrieves the passed in queue's 'AM fraction' from Capacity Scheduler. Returns default value of 0.1 if AM Percent
    pertaining to passed-in queue is not present.
    """
    # Identify the key which contains the AM fraction for "llap_daemon_selected_queue_name".
    cap_sched_keys = capacity_scheduler_properties.keys()
    llap_selected_queue_am_percent_key = None
    for key in cap_sched_keys:
      if key.endswith("."+llap_daemon_selected_queue_name+".maximum-am-resource-percent"):
        llap_selected_queue_am_percent_key = key
        self.logger.info("AM percent key got for '{0}' queue is : '{1}'".format(llap_daemon_selected_queue_name, llap_selected_queue_am_percent_key))
        break;
    if llap_selected_queue_am_percent_key is None:
      self.logger.info("Returning default AM percent value : '0.1' for queue : {0}".format(llap_daemon_selected_queue_name))
      return 0.1 # Default value to use if we couldn't retrieve queue's corresponding AM Percent key.
    else:
      llap_selected_queue_am_percent = capacity_scheduler_properties.get(llap_selected_queue_am_percent_key)
      self.logger.info("Returning read value for key '{0}' as : '{1}' for queue : '{2}'".format(llap_selected_queue_am_percent_key,
                                                                                     llap_selected_queue_am_percent,
                                                                                     llap_daemon_selected_queue_name))
      return llap_selected_queue_am_percent

  def __getSelectedQueueTotalCap(self, capacity_scheduler_properties, llap_daemon_selected_queue_name, total_cluster_capacity):
    """
    Calculates the total available capacity for the passed-in YARN queue of any level based on the percentages.
    """
    self.logger.info("Entered __getSelectedQueueTotalCap fn() with llap_daemon_selected_queue_name= '{0}'.".format(llap_daemon_selected_queue_name))
    available_capacity = total_cluster_capacity
    queue_cap_key = self.__getQueueCapacityKeyFromCapacityScheduler(capacity_scheduler_properties, llap_daemon_selected_queue_name)
    if queue_cap_key:
      queue_cap_key = queue_cap_key.strip()
      if len(queue_cap_key) >= 34:  # len("yarn.scheduler.capacity.<single letter queue name>.capacity") = 34
        # Expected capacity prop key is of form : "yarn.scheduler.capacity.<one or more queues (path)>.capacity"
        queue_path = queue_cap_key[24:]  # Strip from beginning "yarn.scheduler.capacity."
        queue_path = queue_path[0:-9]  # Strip from end ".capacity"
        queues_list = queue_path.split(".")
        self.logger.info("Queue list : {0}".format(queues_list))
        if queues_list:
          for queue in queues_list:
            queue_cap_key = self.__getQueueCapacityKeyFromCapacityScheduler(capacity_scheduler_properties, queue)
            queue_cap_perc = float(capacity_scheduler_properties.get(queue_cap_key))
            available_capacity = queue_cap_perc / 100 * available_capacity
            self.logger.info("Total capacity available for queue {0} is : {1}".format(queue, available_capacity))

    # returns the capacity calculated for passed-in queue in "llap_daemon_selected_queue_name".
    return available_capacity

  def min_queue_perc_reqd_for_llap_and_hive_app(self, services, hosts, configurations):
    """
    Calculate minimum queue capacity required in order to get LLAP and HIVE2 app into running state.
    """
    # Get queue size if sized at 20%
    node_manager_hosts = self.getHostsForComponent(services, "YARN", "NODEMANAGER")
    yarn_rm_mem_in_mb = self.get_yarn_nm_mem_in_mb(services, configurations)
    total_cluster_cap = len(node_manager_hosts) * yarn_rm_mem_in_mb
    total_queue_size_at_20_perc = 20.0 / 100 * total_cluster_cap

    # Calculate based on minimum size required by containers.
    yarn_min_container_size = long(self.get_yarn_min_container_size(services, configurations))
    hive_tez_container_size = long(self.get_hive_tez_container_size(services))
    tez_am_container_size = self.calculate_tez_am_container_size(services, long(total_cluster_cap))
    normalized_val = self._normalizeUp(hive_tez_container_size, yarn_min_container_size) \
                     + self._normalizeUp(tez_am_container_size, yarn_min_container_size)

    min_required = max(total_queue_size_at_20_perc, normalized_val)
    min_required_perc = min_required * 100 / total_cluster_cap

    return int(math.ceil(min_required_perc))


  #TODO  Convert this to a helper. It can apply to any property. Check config, or check if in the list of changed configurations and read the latest value
  def get_yarn_min_container_size(self, services, configurations):
    """
    Gets YARN's minimum container size (yarn.scheduler.minimum-allocation-mb).
    Reads from:
      - configurations (if changed as part of current Stack Advisor invocation (output)), and services["changed-configurations"]
        is empty, else
      - services['configurations'] (input).

    services["changed-configurations"] would be empty if Stack Advisor call is made from Blueprints (1st invocation). Subsequent
    Stack Advisor calls will have it non-empty. We do this because in subsequent invocations, even if Stack Advisor calculates this
    value (configurations), it is finally not recommended, making 'input' value to survive.

    :type services dict
    :type configurations dict
    :rtype str
    """
    yarn_min_container_size = None
    yarn_min_allocation_property = "yarn.scheduler.minimum-allocation-mb"
    yarn_site = self.getSiteProperties(configurations, "yarn-site")
    yarn_site_properties = self.getServicesSiteProperties(services, "yarn-site")

    # Check if services["changed-configurations"] is empty and "yarn.scheduler.minimum-allocation-mb" is modified in current ST invocation.
    if not services["changed-configurations"] and yarn_site and yarn_min_allocation_property in yarn_site:
      yarn_min_container_size = yarn_site[yarn_min_allocation_property]
      self.logger.info("DBG: 'yarn.scheduler.minimum-allocation-mb' read from output as : {0}".format(yarn_min_container_size))

    # Check if "yarn.scheduler.minimum-allocation-mb" is input in services array.
    elif yarn_site_properties and yarn_min_allocation_property in yarn_site_properties:
      yarn_min_container_size = yarn_site_properties[yarn_min_allocation_property]
      self.logger.info("DBG: 'yarn.scheduler.minimum-allocation-mb' read from services as : {0}".format(yarn_min_container_size))

    if not yarn_min_container_size:
      self.logger.error("{0} was not found in the configuration".format(yarn_min_allocation_property))

    return yarn_min_container_size

  def get_hive_tez_container_size(self, services):
    """
    Gets HIVE Tez container size (hive.tez.container.size).
    """
    hive_container_size = None
    hsi_site = self.getServicesSiteProperties(services, self.HIVE_INTERACTIVE_SITE)
    if hsi_site and "hive.tez.container.size" in hsi_site:
      hive_container_size = hsi_site["hive.tez.container.size"]

    if not hive_container_size:
      # This can happen (1). If config is missing in hive-interactive-site or (2). its an
      # upgrade scenario from Ambari 2.4 to Ambari 2.5 with HDP 2.5 installed. Read it
      # from hive-site.
      #
      # If Ambari 2.5 after upgrade from 2.4 is managing HDP 2.6 here, this config would have
      # already been added in hive-interactive-site as part of HDP upgrade from 2.5 to 2.6,
      # and we wont end up in this block to look up in hive-site.
      hive_site = self.getServicesSiteProperties(services, "hive-site")
      if hive_site and "hive.tez.container.size" in hive_site:
        hive_container_size = hive_site["hive.tez.container.size"]
    return hive_container_size

  def calculate_tez_am_container_size(self, services, total_cluster_capacity, is_cluster_create_opr=False, enable_hive_interactive_1st_invocation=False):
    """
    Calculates Tez App Master container size (tez.am.resource.memory.mb) for tez_hive2/tez-site on initialization if values read is 0.
    Else returns the read value.
    """
    tez_am_resource_memory_mb = self.get_tez_am_resource_memory_mb(services)
    calculated_tez_am_resource_memory_mb = None
    if is_cluster_create_opr or enable_hive_interactive_1st_invocation:
      if total_cluster_capacity <= 4096:
        calculated_tez_am_resource_memory_mb = 512
      elif total_cluster_capacity > 4096 and total_cluster_capacity <= 98304:
        calculated_tez_am_resource_memory_mb = 1024
      elif total_cluster_capacity > 98304:
        calculated_tez_am_resource_memory_mb = 4096

      self.logger.info("DBG: Calculated and returning 'tez_am_resource_memory_mb' as : {0}".format(calculated_tez_am_resource_memory_mb))
      return float(calculated_tez_am_resource_memory_mb)
    else:
      self.logger.info("DBG: Returning 'tez_am_resource_memory_mb' as : {0}".format(tez_am_resource_memory_mb))
      return float(tez_am_resource_memory_mb)

  def get_tez_am_resource_memory_mb(self, services):
    """
    Gets Tez's AM resource memory (tez.am.resource.memory.mb) from services.
    """
    tez_am_resource_memory_mb = None
    if "tez.am.resource.memory.mb" in services["configurations"]["tez-interactive-site"]["properties"]:
      tez_am_resource_memory_mb = services["configurations"]["tez-interactive-site"]["properties"]["tez.am.resource.memory.mb"]

    return tez_am_resource_memory_mb

  def _normalizeUp(self, val1, val2):
    """
    Normalize up 'val2' with respect to 'val1'.
    """
    tmp = math.ceil(val1 / val2)
    return tmp * val2
