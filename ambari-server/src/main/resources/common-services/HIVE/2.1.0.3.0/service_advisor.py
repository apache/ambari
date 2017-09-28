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



SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
STACKS_DIR = os.path.join(SCRIPT_DIR, '../../../stacks/')
PARENT_FILE = os.path.join(STACKS_DIR, 'service_advisor.py')

try:
  with open(PARENT_FILE, 'rb') as fp:
    service_advisor = imp.load_module('service_advisor', fp, PARENT_FILE, ('.py', 'rb', imp.PY_SOURCE))
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
      'HIVE_SERVER': {6: 1, 31: 2, "else": 4},
      'HIVE_METASTORE': {6: 1, 31: 2, "else": 4},
      'WEBHCAT_SERVER': {6: 1, 31: 2, "else": 4},
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
    self.logger.info("Class: %s, Method: %s. Recommending Service Configurations." %
                (self.__class__.__name__, inspect.stack()[0][3]))

    recommender = HiveRecommender()
    recommender.recommendHiveConfigurationsFromHDP21(configurations, clusterData, services, hosts)
    recommender.recommendHIVEConfigurationsFromHDP22(configurations, clusterData, services, hosts)
    recommender.recommendHIVEConfigurationsFromHDP23(configurations, clusterData, services, hosts)
    recommender.recommendHIVEConfigurationsFromHDP25(configurations, clusterData, services, hosts)
    recommender.recommendHIVEConfigurationsFromHDP26(configurations, clusterData, services, hosts)



  def getServiceConfigurationsValidationItems(self, configurations, recommendedDefaults, services, hosts):
    """
    Entry point.
    Validate configurations for the service. Return a list of errors.
    The code for this function should be the same for each Service Advisor.
    """
    self.logger.info("Class: %s, Method: %s. Validating Configurations." %
                (self.__class__.__name__, inspect.stack()[0][3]))

    validator = HiveValidator()
    # Calls the methods of the validator using arguments,
    # method(siteProperties, siteRecommendations, configurations, services, hosts)
    return validator.validateListOfConfigUsingMethod(configurations, recommendedDefaults, services, hosts, validator.validators)



class HiveRecommender(service_advisor.ServiceAdvisor):
  """
  Hive Recommender suggests properties when adding the service for the first time or modifying configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(HiveRecommender, self)
    self.as_super.__init__(*args, **kwargs)
    self.HIVE_INTERACTIVE_SITE = 'hive-interactive-site'


  def recommendHiveConfigurationsFromHDP21(self, configurations, clusterData, services, hosts):
    hiveSiteProperties = self.getSiteProperties(services['configurations'], 'hive-site')
    hiveEnvProperties = self.getSiteProperties(services['configurations'], 'hive-env')
    containerSize = clusterData['mapMemory'] if clusterData['mapMemory'] > 2048 else int(clusterData['reduceMemory'])
    containerSize = min(clusterData['containers'] * clusterData['ramPerContainer'], containerSize)
    container_size_bytes = int(containerSize)*1024*1024
    putHiveEnvProperty = self.putProperty(configurations, "hive-env", services)
    putHiveProperty = self.putProperty(configurations, "hive-site", services)
    putHiveProperty('hive.auto.convert.join.noconditionaltask.size', int(round(container_size_bytes / 3)))
    putHiveProperty('hive.tez.java.opts', "-server -Xmx" + str(int(round((0.8 * containerSize) + 0.5)))
                    + "m -Djava.net.preferIPv4Stack=true -XX:NewRatio=8 -XX:+UseNUMA -XX:+UseParallelGC -XX:+PrintGCDetails -verbose:gc -XX:+PrintGCTimeStamps")
    putHiveProperty('hive.tez.container.size', containerSize)

    # javax.jdo.option.ConnectionURL recommendations
    if hiveEnvProperties and self.checkSiteProperties(hiveEnvProperties, 'hive_database', 'hive_database_type'):
      putHiveEnvProperty('hive_database_type', self.getDBTypeAlias(hiveEnvProperties['hive_database']))
    if hiveEnvProperties and hiveSiteProperties and self.checkSiteProperties(hiveSiteProperties, 'javax.jdo.option.ConnectionDriverName') and self.checkSiteProperties(hiveEnvProperties, 'hive_database'):
      putHiveProperty('javax.jdo.option.ConnectionDriverName', self.getDBDriver(hiveEnvProperties['hive_database']))
    if hiveSiteProperties and hiveEnvProperties and self.checkSiteProperties(hiveSiteProperties, 'ambari.hive.db.schema.name', 'javax.jdo.option.ConnectionURL') and self.checkSiteProperties(hiveEnvProperties, 'hive_database'):
      hiveServerHost = self.getHostWithComponent('HIVE', 'HIVE_SERVER', services, hosts)
      hiveDBConnectionURL = hiveSiteProperties['javax.jdo.option.ConnectionURL']
      protocol = self.getProtocol(hiveEnvProperties['hive_database'])
      oldSchemaName = self.getOldValue(services, "hive-site", "ambari.hive.db.schema.name")
      oldDBType = self.getOldValue(services, "hive-env", "hive_database")
      # under these if constructions we are checking if hive server hostname available,
      # if it's default db connection url with "localhost" or if schema name was changed or if db type was changed (only for db type change from default mysql to existing mysql)
      # or if protocol according to current db type differs with protocol in db connection url(other db types changes)
      if hiveServerHost is not None:
        if (hiveDBConnectionURL and "//localhost" in hiveDBConnectionURL) or oldSchemaName or oldDBType or (protocol and hiveDBConnectionURL and not hiveDBConnectionURL.startswith(protocol)):
          dbConnection = self.getDBConnectionString(hiveEnvProperties['hive_database']).format(hiveServerHost['Hosts']['host_name'], hiveSiteProperties['ambari.hive.db.schema.name'])
          putHiveProperty('javax.jdo.option.ConnectionURL', dbConnection)

    servicesList = self.get_services_list(services)
    if "PIG" in servicesList:
        ambari_user = self.getAmbariUser(services)
        ambariHostName = socket.getfqdn()
        webHcatSiteProperty = self.putProperty(configurations, "webhcat-site", services)
        webHcatSiteProperty("webhcat.proxyuser.{0}.hosts".format(ambari_user), ambariHostName)
        webHcatSiteProperty("webhcat.proxyuser.{0}.groups".format(ambari_user), "*")
        old_ambari_user = self.getOldAmbariUser(services)
        if old_ambari_user is not None:
            webHcatSitePropertyAttributes = self.putPropertyAttribute(configurations, "webhcat-site")
            webHcatSitePropertyAttributes("webhcat.proxyuser.{0}.hosts".format(old_ambari_user), 'delete', 'true')
            webHcatSitePropertyAttributes("webhcat.proxyuser.{0}.groups".format(old_ambari_user), 'delete', 'true')

    if self.is_secured_cluster(services):
      putCoreSiteProperty = self.putProperty(configurations, "core-site", services)

      meta = self.get_service_component_meta("HIVE", "WEBHCAT_SERVER", services)
      if "hostnames" in meta:
        self.put_proxyuser_value("HTTP", meta["hostnames"], services=services, configurations=configurations, put_function=putCoreSiteProperty)


  def recommendHIVEConfigurationsFromHDP22(self, configurations, clusterData, services, hosts):

    putHiveServerProperty = self.putProperty(configurations, "hiveserver2-site", services)
    putHiveEnvProperty = self.putProperty(configurations, "hive-env", services)
    putHiveSiteProperty = self.putProperty(configurations, "hive-site", services)
    putWebhcatSiteProperty = self.putProperty(configurations, "webhcat-site", services)
    putHiveSitePropertyAttribute = self.putPropertyAttribute(configurations, "hive-site")
    putHiveEnvPropertyAttributes = self.putPropertyAttribute(configurations, "hive-env")
    putHiveServerPropertyAttributes = self.putPropertyAttribute(configurations, "hiveserver2-site")
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

    #  Storage
    putHiveEnvProperty("hive_exec_orc_storage_strategy", "SPEED")
    putHiveSiteProperty("hive.exec.orc.encoding.strategy", configurations["hive-env"]["properties"]["hive_exec_orc_storage_strategy"])
    putHiveSiteProperty("hive.exec.orc.compression.strategy", configurations["hive-env"]["properties"]["hive_exec_orc_storage_strategy"])

    putHiveSiteProperty("hive.exec.orc.default.stripe.size", "67108864")
    putHiveSiteProperty("hive.exec.orc.default.compress", "ZLIB")
    putHiveSiteProperty("hive.optimize.index.filter", "true")
    putHiveSiteProperty("hive.optimize.sort.dynamic.partition", "false")

    # Vectorization
    putHiveSiteProperty("hive.vectorized.execution.enabled", "true")
    putHiveSiteProperty("hive.vectorized.execution.reduce.enabled", "false")

    # Transactions
    putHiveEnvProperty("hive_txn_acid", "off")
    if str(configurations["hive-env"]["properties"]["hive_txn_acid"]).lower() == "on":
      putHiveSiteProperty("hive.txn.manager", "org.apache.hadoop.hive.ql.lockmgr.DbTxnManager")
      putHiveSiteProperty("hive.support.concurrency", "true")
      putHiveSiteProperty("hive.compactor.initiator.on", "true")
      putHiveSiteProperty("hive.compactor.worker.threads", "1")
      putHiveSiteProperty("hive.exec.dynamic.partition.mode", "nonstrict")
    else:
      putHiveSiteProperty("hive.txn.manager", "org.apache.hadoop.hive.ql.lockmgr.DummyTxnManager")
      putHiveSiteProperty("hive.support.concurrency", "false")
      putHiveSiteProperty("hive.compactor.initiator.on", "false")
      putHiveSiteProperty("hive.compactor.worker.threads", "0")
      putHiveSiteProperty("hive.exec.dynamic.partition.mode", "strict")

    hiveMetastoreHost = self.getHostWithComponent("HIVE", "HIVE_METASTORE", services, hosts)
    if hiveMetastoreHost is not None and len(hiveMetastoreHost) > 0:
      putHiveSiteProperty("hive.metastore.uris", "thrift://" + hiveMetastoreHost["Hosts"]["host_name"] + ":9083")

    # ATS
    putHiveEnvProperty("hive_timeline_logging_enabled", "true")

    hooks_properties = ["hive.exec.pre.hooks", "hive.exec.post.hooks", "hive.exec.failure.hooks"]
    include_ats_hook = str(configurations["hive-env"]["properties"]["hive_timeline_logging_enabled"]).lower() == "true"

    ats_hook_class = "org.apache.hadoop.hive.ql.hooks.ATSHook"
    for hooks_property in hooks_properties:
      if hooks_property in configurations["hive-site"]["properties"]:
        hooks_value = configurations["hive-site"]["properties"][hooks_property]
      else:
        hooks_value = " "
      if include_ats_hook and ats_hook_class not in hooks_value:
        if hooks_value == " ":
          hooks_value = ats_hook_class
        else:
          hooks_value = hooks_value + "," + ats_hook_class
      if not include_ats_hook and ats_hook_class in hooks_value:
        hooks_classes = []
        for hook_class in hooks_value.split(","):
          if hook_class != ats_hook_class and hook_class != " ":
            hooks_classes.append(hook_class)
        if hooks_classes:
          hooks_value = ",".join(hooks_classes)
        else:
          hooks_value = " "

      putHiveSiteProperty(hooks_property, hooks_value)

    # Tez Engine
    if "TEZ" in servicesList:
      putHiveSiteProperty("hive.execution.engine", "tez")
    else:
      putHiveSiteProperty("hive.execution.engine", "mr")

    container_size = "512"

    if not "yarn-site" in configurations:
      self.calculateYarnAllocationSizes(configurations, services, hosts)
    #properties below should be always present as they are provided in HDP206 stack advisor at least
    yarnMaxAllocationSize = min(30 * int(configurations["yarn-site"]["properties"]["yarn.scheduler.minimum-allocation-mb"]), int(configurations["yarn-site"]["properties"]["yarn.scheduler.maximum-allocation-mb"]))
    #duplicate tez task resource calc logic, direct dependency doesn't look good here (in case of Hive without Tez)
    container_size = clusterData['mapMemory'] if clusterData['mapMemory'] > 2048 else int(clusterData['reduceMemory'])
    container_size = min(clusterData['containers'] * clusterData['ramPerContainer'], container_size, yarnMaxAllocationSize)

    putHiveSiteProperty("hive.tez.container.size", min(int(configurations["yarn-site"]["properties"]["yarn.scheduler.maximum-allocation-mb"]), container_size))

    putHiveSitePropertyAttribute("hive.tez.container.size", "minimum", int(configurations["yarn-site"]["properties"]["yarn.scheduler.minimum-allocation-mb"]))
    putHiveSitePropertyAttribute("hive.tez.container.size", "maximum", int(configurations["yarn-site"]["properties"]["yarn.scheduler.maximum-allocation-mb"]))

    if "yarn-site" in services["configurations"]:
      if "yarn.scheduler.minimum-allocation-mb" in services["configurations"]["yarn-site"]["properties"]:
        putHiveSitePropertyAttribute("hive.tez.container.size", "minimum", int(services["configurations"]["yarn-site"]["properties"]["yarn.scheduler.minimum-allocation-mb"]))
      if "yarn.scheduler.maximum-allocation-mb" in services["configurations"]["yarn-site"]["properties"]:
        putHiveSitePropertyAttribute("hive.tez.container.size", "maximum", int(services["configurations"]["yarn-site"]["properties"]["yarn.scheduler.maximum-allocation-mb"]))

    putHiveSiteProperty("hive.prewarm.enabled", "false")
    putHiveSiteProperty("hive.prewarm.numcontainers", "3")
    putHiveSiteProperty("hive.tez.auto.reducer.parallelism", "true")
    putHiveSiteProperty("hive.tez.dynamic.partition.pruning", "true")

    container_size = configurations["hive-site"]["properties"]["hive.tez.container.size"]
    container_size_bytes = int(int(container_size)*0.8*1024*1024) # Xmx == 80% of container
    # Memory
    putHiveSiteProperty("hive.auto.convert.join.noconditionaltask.size", int(round(container_size_bytes/3)))
    putHiveSitePropertyAttribute("hive.auto.convert.join.noconditionaltask.size", "maximum", container_size_bytes)
    putHiveSiteProperty("hive.exec.reducers.bytes.per.reducer", "67108864")

    # CBO
    if "hive-site" in services["configurations"] and "hive.cbo.enable" in services["configurations"]["hive-site"]["properties"]:
      hive_cbo_enable = services["configurations"]["hive-site"]["properties"]["hive.cbo.enable"]
      putHiveSiteProperty("hive.stats.fetch.partition.stats", hive_cbo_enable)
      putHiveSiteProperty("hive.stats.fetch.column.stats", hive_cbo_enable)

    putHiveSiteProperty("hive.compute.query.using.stats", "true")

    # Interactive Query
    putHiveSiteProperty("hive.server2.tez.initialize.default.sessions", "false")
    putHiveSiteProperty("hive.server2.tez.sessions.per.default.queue", "1")
    putHiveSiteProperty("hive.server2.enable.doAs", "true")

    yarn_queues = "default"
    capacitySchedulerProperties = {}
    if "capacity-scheduler" in services['configurations']:
      if "capacity-scheduler" in services['configurations']["capacity-scheduler"]["properties"]:
        properties = str(services['configurations']["capacity-scheduler"]["properties"]["capacity-scheduler"]).split('\n')
        for property in properties:
          key,sep,value = property.partition("=")
          capacitySchedulerProperties[key] = value
      if "yarn.scheduler.capacity.root.queues" in capacitySchedulerProperties:
        yarn_queues = str(capacitySchedulerProperties["yarn.scheduler.capacity.root.queues"])
      elif "yarn.scheduler.capacity.root.queues" in services['configurations']["capacity-scheduler"]["properties"]:
        yarn_queues =  services['configurations']["capacity-scheduler"]["properties"]["yarn.scheduler.capacity.root.queues"]
    # Interactive Queues property attributes
    putHiveServerPropertyAttribute = self.putPropertyAttribute(configurations, "hiveserver2-site")
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
    leafQueues = sorted(leafQueues, key=lambda q:q['value'])
    putHiveSitePropertyAttribute("hive.server2.tez.default.queues", "entries", leafQueues)
    putHiveSiteProperty("hive.server2.tez.default.queues", ",".join([leafQueue['value'] for leafQueue in leafQueues]))

    webhcat_queue = self.recommendYarnQueue(services, "webhcat-site", "templeton.hadoop.queue.name")
    if webhcat_queue is not None:
      putWebhcatSiteProperty("templeton.hadoop.queue.name", webhcat_queue)

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

    # hive_security_authorization == 'none'
    # this property is unrelated to Kerberos
    if str(configurations["hive-env"]["properties"]["hive_security_authorization"]).lower() == "none":
      putHiveSiteProperty("hive.security.authorization.manager", "org.apache.hadoop.hive.ql.security.authorization.plugin.sqlstd.SQLStdConfOnlyAuthorizerFactory")
      if ("hive.security.authorization.manager" in configurations["hiveserver2-site"]["properties"]) or \
              ("hiveserver2-site" not in services["configurations"]) or \
              ("hiveserver2-site" in services["configurations"] and "hive.security.authorization.manager" in services["configurations"]["hiveserver2-site"]["properties"]):
        putHiveServerPropertyAttribute("hive.security.authorization.manager", "delete", "true")
      if ("hive.security.authenticator.manager" in configurations["hiveserver2-site"]["properties"]) or \
              ("hiveserver2-site" not in services["configurations"]) or \
              ("hiveserver2-site" in services["configurations"] and "hive.security.authenticator.manager" in services["configurations"]["hiveserver2-site"]["properties"]):
        putHiveServerPropertyAttribute("hive.security.authenticator.manager", "delete", "true")
      if ("hive.conf.restricted.list" in configurations["hiveserver2-site"]["properties"]) or \
              ("hiveserver2-site" not in services["configurations"]) or \
              ("hiveserver2-site" in services["configurations"] and "hive.conf.restricted.list" in services["configurations"]["hiveserver2-site"]["properties"]):
        putHiveServerPropertyAttribute("hive.conf.restricted.list", "delete", "true")
      if "KERBEROS" not in servicesList: # Kerberos security depends on this property
        putHiveSiteProperty("hive.security.authorization.enabled", "false")
    else:
      putHiveSiteProperty("hive.security.authorization.enabled", "true")

    try:
      auth_manager_value = str(configurations["hive-env"]["properties"]["hive.security.metastore.authorization.manager"])
    except KeyError:
      auth_manager_value = 'org.apache.hadoop.hive.ql.security.authorization.StorageBasedAuthorizationProvider'
      pass
    auth_manager_values = auth_manager_value.split(",")
    sqlstdauth_class = "org.apache.hadoop.hive.ql.security.authorization.MetaStoreAuthzAPIAuthorizerEmbedOnly"

    putHiveSiteProperty("hive.server2.enable.doAs", "true")

    # hive_security_authorization == 'sqlstdauth'
    if str(configurations["hive-env"]["properties"]["hive_security_authorization"]).lower() == "sqlstdauth":
      putHiveSiteProperty("hive.server2.enable.doAs", "false")
      putHiveServerProperty("hive.security.authorization.enabled", "true")
      putHiveServerProperty("hive.security.authorization.manager", "org.apache.hadoop.hive.ql.security.authorization.plugin.sqlstd.SQLStdHiveAuthorizerFactory")
      putHiveServerProperty("hive.security.authenticator.manager", "org.apache.hadoop.hive.ql.security.SessionStateUserAuthenticator")
      putHiveServerProperty("hive.conf.restricted.list", "hive.security.authenticator.manager,hive.security.authorization.manager,hive.security.metastore.authorization.manager,"
                                                         "hive.security.metastore.authenticator.manager,hive.users.in.admin.role,hive.server2.xsrf.filter.enabled,hive.security.authorization.enabled")
      putHiveSiteProperty("hive.security.authorization.manager", "org.apache.hadoop.hive.ql.security.authorization.plugin.sqlstd.SQLStdConfOnlyAuthorizerFactory")
      if sqlstdauth_class not in auth_manager_values:
        auth_manager_values.append(sqlstdauth_class)
    elif sqlstdauth_class in auth_manager_values:
      #remove item from csv
      auth_manager_values = [x for x in auth_manager_values if x != sqlstdauth_class]
      pass
    putHiveSiteProperty("hive.security.metastore.authorization.manager", ",".join(auth_manager_values))

    # hive_security_authorization == 'ranger'
    if str(configurations["hive-env"]["properties"]["hive_security_authorization"]).lower() == "ranger":
      putHiveSiteProperty("hive.server2.enable.doAs", "false")
      putHiveServerProperty("hive.security.authorization.enabled", "true")
      putHiveServerProperty("hive.security.authorization.manager", "com.xasecure.authorization.hive.authorizer.XaSecureHiveAuthorizerFactory")
      putHiveServerProperty("hive.security.authenticator.manager", "org.apache.hadoop.hive.ql.security.SessionStateUserAuthenticator")
      putHiveServerProperty("hive.conf.restricted.list", "hive.security.authenticator.manager,hive.security.authorization.manager,hive.security.metastore.authorization.manager,"
                                                         "hive.security.metastore.authenticator.manager,hive.users.in.admin.role,hive.server2.xsrf.filter.enabled,hive.security.authorization.enabled")

    # hive_security_authorization == 'None'
    if str(configurations["hive-env"]["properties"]["hive_security_authorization"]).lower() == "None":
      putHiveSiteProperty("hive.server2.enable.doAs", "true")
      putHiveServerProperty("hive.security.authorization.enabled", "false")
      putHiveServerPropertyAttributes("hive.security.authorization.manager", 'delete', 'true')
      putHiveServerPropertyAttributes("hive.security.authenticator.manager", 'delete', 'true')
      putHiveServerPropertyAttributes("hive.conf.restricted.list", 'delete', 'true')

    putHiveSiteProperty("hive.server2.use.SSL", "false")

    #Hive authentication
    hive_server2_auth = None
    if "hive-site" in services["configurations"] and "hive.server2.authentication" in services["configurations"]["hive-site"]["properties"]:
      hive_server2_auth = str(services["configurations"]["hive-site"]["properties"]["hive.server2.authentication"]).lower()
    elif "hive.server2.authentication" in configurations["hive-site"]["properties"]:
      hive_server2_auth = str(configurations["hive-site"]["properties"]["hive.server2.authentication"]).lower()

    if hive_server2_auth == "ldap":
      putHiveSiteProperty("hive.server2.authentication.ldap.url", "")
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
    elif "KERBEROS" not in servicesList: # Since 'hive_server2_auth' cannot be relied on within the default, empty recommendations request
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
    hive_server_hosts = self.getHostsWithComponent("HIVE", "HIVE_SERVER", services, hosts)
    hive_client_hosts = self.getHostsWithComponent("HIVE", "HIVE_CLIENT", services, hosts)

    if hive_server_hosts is not None and len(hive_server_hosts):
      hs_host_ram = hive_server_hosts[0]["Hosts"]["total_mem"]/1024
      putHiveEnvProperty("hive.metastore.heapsize", max(512, int(hs_host_ram*hm_heapsize_multiplier)))
      putHiveEnvProperty("hive.heapsize", max(512, int(hs_host_ram*hs_heapsize_multiplier)))
      putHiveEnvPropertyAttributes("hive.metastore.heapsize", "maximum", max(1024, hs_host_ram))
      putHiveEnvPropertyAttributes("hive.heapsize", "maximum", max(1024, hs_host_ram))

    if hive_client_hosts is not None and len(hive_client_hosts):
      putHiveEnvProperty("hive.client.heapsize", 1024)
      putHiveEnvPropertyAttributes("hive.client.heapsize", "maximum", max(1024, int(hive_client_hosts[0]["Hosts"]["total_mem"]/1024)))


  def recommendHIVEConfigurationsFromHDP23(self, configurations, clusterData, services, hosts):

    putHiveSiteProperty = self.putProperty(configurations, "hive-site", services)
    putHiveServerProperty = self.putProperty(configurations, "hiveserver2-site", services)
    putHiveEnvProperty = self.putProperty(configurations, "hive-env", services)
    putHiveSitePropertyAttribute = self.putPropertyAttribute(configurations, "hive-site")
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    # hive_security_authorization == 'ranger'
    if str(configurations["hive-env"]["properties"]["hive_security_authorization"]).lower() == "ranger":
      putHiveServerProperty("hive.security.authorization.manager", "org.apache.ranger.authorization.hive.authorizer.RangerHiveAuthorizerFactory")

    # TEZ JVM options
    jvmGCParams = "-XX:+UseParallelGC"
    if "ambari-server-properties" in services and "java.home" in services["ambari-server-properties"]:
      # JDK8 needs different parameters
      match = re.match(".*\/jdk(1\.\d+)[\-\_\.][^/]*$", services["ambari-server-properties"]["java.home"])
      if match and len(match.groups()) > 0:
        # Is version >= 1.8
        versionSplits = re.split("\.", match.group(1))
        if versionSplits and len(versionSplits) > 1 and int(versionSplits[0]) > 0 and int(versionSplits[1]) > 7:
          jvmGCParams = "-XX:+UseG1GC -XX:+ResizeTLAB"
    putHiveSiteProperty('hive.tez.java.opts', "-server -Djava.net.preferIPv4Stack=true -XX:NewRatio=8 -XX:+UseNUMA " + jvmGCParams + " -XX:+PrintGCDetails -verbose:gc -XX:+PrintGCTimeStamps")

    # if hive using sqla db, then we should add DataNucleus property
    sqla_db_used = 'hive-env' in services['configurations'] and 'hive_database' in services['configurations']['hive-env']['properties'] and \
                   services['configurations']['hive-env']['properties']['hive_database'] == 'Existing SQL Anywhere Database'
    if sqla_db_used:
      putHiveSiteProperty('datanucleus.rdbms.datastoreAdapterClassName','org.datanucleus.store.rdbms.adapter.SQLAnywhereAdapter')
    else:
      putHiveSitePropertyAttribute('datanucleus.rdbms.datastoreAdapterClassName', 'delete', 'true')

    # Atlas
    hooks_property = "hive.exec.post.hooks"
    atlas_hook_class = "org.apache.atlas.hive.hook.HiveHook"
    if hooks_property in configurations["hive-site"]["properties"]:
      hooks_value = configurations["hive-site"]["properties"][hooks_property]
    else:
      hooks_value = ""

    hive_hooks = [x.strip() for x in hooks_value.split(",")]
    hive_hooks = [x for x in hive_hooks if x != ""]
    is_atlas_present_in_cluster = "ATLAS" in servicesList

    enable_external_atlas_for_hive = False
    enable_atlas_hook = False

    if 'hive-atlas-application.properties' in services['configurations'] and 'enable.external.atlas.for.hive' in services['configurations']['hive-atlas-application.properties']['properties']:
      enable_external_atlas_for_hive = services['configurations']['hive-atlas-application.properties']['properties']['enable.external.atlas.for.hive'].lower() == "true"

    if is_atlas_present_in_cluster:
      putHiveEnvProperty("hive.atlas.hook", "true")
    elif enable_external_atlas_for_hive:
      putHiveEnvProperty("hive.atlas.hook", "true")
    else:
      putHiveEnvProperty("hive.atlas.hook", "false")

    if 'hive-env' in configurations and 'hive.atlas.hook' in configurations['hive-env']['properties']:
      enable_atlas_hook = configurations['hive-env']['properties']['hive.atlas.hook'] == "true"
    elif 'hive-env' in services['configurations'] and 'hive.atlas.hook' in services['configurations']['hive-env']['properties']:
      enable_atlas_hook = services['configurations']['hive-env']['properties']['hive.atlas.hook'] == "true"

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
    putHiveSiteProperty(hooks_property, hooks_value)

    # This is no longer used in HDP 2.5, but still needed in HDP 2.3 and 2.4
    atlas_server_host_info = self.getHostWithComponent("ATLAS", "ATLAS_SERVER", services, hosts)
    if is_atlas_present_in_cluster and atlas_server_host_info:
      atlas_rest_host = atlas_server_host_info['Hosts']['host_name']
      scheme = "http"
      metadata_port = "21000"
      atlas_server_default_https_port = "21443"
      tls_enabled = "false"
      if 'application-properties' in services['configurations']:
        if 'atlas.enableTLS' in services['configurations']['application-properties']['properties']:
          tls_enabled = services['configurations']['application-properties']['properties']['atlas.enableTLS']
        if 'atlas.server.http.port' in services['configurations']['application-properties']['properties']:
          metadata_port = services['configurations']['application-properties']['properties']['atlas.server.http.port']
        if tls_enabled.lower() == "true":
          scheme = "https"
          if 'atlas.server.https.port' in services['configurations']['application-properties']['properties']:
            metadata_port =  services['configurations']['application-properties']['properties']['atlas.server.https.port']
          else:
            metadata_port = atlas_server_default_https_port
      putHiveSiteProperty('atlas.rest.address', '{0}://{1}:{2}'.format(scheme, atlas_rest_host, metadata_port))
    else:
      putHiveSitePropertyAttribute('atlas.cluster.name', 'delete', 'true')
      putHiveSitePropertyAttribute('atlas.rest.address', 'delete', 'true')


  def recommendHIVEConfigurationsFromHDP25(self, configurations, clusterData, services, hosts):
    self.logger.info("DBG: Invoked recommendHiveConfiguration")

    putHiveInteractiveEnvProperty = self.putProperty(configurations, "hive-interactive-env", services)
    putHiveInteractiveSiteProperty = self.putProperty(configurations, self.HIVE_INTERACTIVE_SITE, services)
    putHiveInteractiveEnvPropertyAttribute = self.putPropertyAttribute(configurations, "hive-interactive-env")

    # For 'Hive Server Interactive', if the component exists.
    hsi_hosts = self.getHostsForComponent(services, "HIVE", "HIVE_SERVER_INTERACTIVE")
    hsi_properties = self.getServicesSiteProperties(services, self.HIVE_INTERACTIVE_SITE)

    if len(hsi_hosts) > 0:
      putHiveInteractiveEnvProperty('enable_hive_interactive', 'true')

      # Update 'hive.llap.daemon.queue.name' property attributes if capacity scheduler is changed.
      if hsi_properties and 'hive.llap.daemon.queue.name' in hsi_properties:
          self.setLlapDaemonQueuePropAttributes(services, configurations)

          hsi_conf_properties = self.getSiteProperties(configurations, self.HIVE_INTERACTIVE_SITE)

          hive_tez_default_queue = hsi_properties["hive.llap.daemon.queue.name"]
          if hsi_conf_properties and "hive.llap.daemon.queue.name" in hsi_conf_properties:
            hive_tez_default_queue = hsi_conf_properties['hive.llap.daemon.queue.name']

          if hive_tez_default_queue:
            putHiveInteractiveSiteProperty("hive.server2.tez.default.queues", hive_tez_default_queue)
            self.logger.debug("Updated 'hive.server2.tez.default.queues' config : '{0}'".format(hive_tez_default_queue))
    else:
      self.logger.info("DBG: Setting 'num_llap_nodes' config's  READ ONLY attribute as 'True'.")
      putHiveInteractiveEnvProperty('enable_hive_interactive', 'false')
      putHiveInteractiveEnvPropertyAttribute("num_llap_nodes", "read_only", "true")

    if hsi_properties and "hive.llap.zk.sm.connectionString" in hsi_properties:
      zookeeper_host_port = self.getZKHostPortString(services)
      if zookeeper_host_port:
        putHiveInteractiveSiteProperty("hive.llap.zk.sm.connectionString", zookeeper_host_port)


  def recommendHIVEConfigurationsFromHDP26(self, configurations, clusterData, services, hosts):

    if 'hive-env' in services['configurations'] and 'hive_user' in services['configurations']['hive-env']['properties']:
      hive_user = services['configurations']['hive-env']['properties']['hive_user']
    else:
      hive_user = 'hive'

    if 'hive-env' in configurations and 'hive_security_authorization' in configurations['hive-env']['properties']:
      ranger_hive_plugin_enabled = (configurations['hive-env']['properties']['hive_security_authorization'].lower() == 'ranger')
    elif 'hive-env' in services['configurations'] and 'hive_security_authorization' in services['configurations']['hive-env']['properties']:
      ranger_hive_plugin_enabled = (services['configurations']['hive-env']['properties']['hive_security_authorization'].lower() == 'ranger')
    else :
      ranger_hive_plugin_enabled = False

    if ranger_hive_plugin_enabled and 'ranger-hive-plugin-properties' in services['configurations'] and 'REPOSITORY_CONFIG_USERNAME' in services['configurations']['ranger-hive-plugin-properties']['properties']:
      self.logger.info("Setting Hive Repo user for Ranger.")
      putRangerHivePluginProperty = self.putProperty(configurations, "ranger-hive-plugin-properties", services)
      putRangerHivePluginProperty("REPOSITORY_CONFIG_USERNAME",hive_user)
    else:
      self.logger.info("Not setting Hive Repo user for Ranger.")

    security_enabled = self.isSecurityEnabled(services)
    enable_atlas_hook = False

    if 'hive-env' in configurations and 'hive.atlas.hook' in configurations['hive-env']['properties']:
      enable_atlas_hook = configurations['hive-env']['properties']['hive.atlas.hook'].lower() == 'true'
    elif 'hive-env' in services['configurations'] and 'hive.atlas.hook' in services['configurations']['hive-env']['properties']:
      enable_atlas_hook = services['configurations']['hive-env']['properties']['hive.atlas.hook'].lower() == 'true'

    if 'hive-atlas-application.properties' in services['configurations']:
      putHiveAtlasHookProperty = self.putProperty(configurations, "hive-atlas-application.properties", services)
      putHiveAtlasHookPropertyAttribute = self.putPropertyAttribute(configurations,"hive-atlas-application.properties")
      if security_enabled and enable_atlas_hook:
        putHiveAtlasHookProperty('atlas.jaas.ticketBased-KafkaClient.loginModuleControlFlag', 'required')
        putHiveAtlasHookProperty('atlas.jaas.ticketBased-KafkaClient.loginModuleName', 'com.sun.security.auth.module.Krb5LoginModule')
        putHiveAtlasHookProperty('atlas.jaas.ticketBased-KafkaClient.option.useTicketCache', 'true')
      else:
        putHiveAtlasHookPropertyAttribute('atlas.jaas.ticketBased-KafkaClient.loginModuleControlFlag', 'delete', 'true')
        putHiveAtlasHookPropertyAttribute('atlas.jaas.ticketBased-KafkaClient.loginModuleName', 'delete', 'true')
        putHiveAtlasHookPropertyAttribute('atlas.jaas.ticketBased-KafkaClient.option.useTicketCache', 'delete', 'true')

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

  def getDBTypeAlias(self, databaseType):
    driverDict = {
      'NEW MYSQL DATABASE': 'mysql',
      'NEW DERBY DATABASE': 'derby',
      'EXISTING MYSQL / MARIADB DATABASE': 'mysql',
      'EXISTING MYSQL DATABASE': 'mysql',
      'EXISTING POSTGRESQL DATABASE': 'postgres',
      'EXISTING ORACLE DATABASE': 'oracle',
      'EXISTING SQL ANYWHERE DATABASE': 'sqla'
    }
    return driverDict.get(databaseType.upper())


class HiveValidator(service_advisor.ServiceAdvisor):
  """
  Hive Validator checks the correctness of properties whenever the service is first added or the user attempts to
  change configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(HiveValidator, self)
    self.as_super.__init__(*args, **kwargs)
    self.HIVE_INTERACTIVE_SITE = 'hive-interactive-site'

    self.validators = [("hive-site", self.validateHiveConfigurationsFromHDP21),
                       ("hiveserver2-site", self.validateHiveServer2ConfigurationsFromHDP22),
                       ("hive-site", self.validateHiveConfigurationsFromHDP22),
                       ("hive-env", self.validateHiveConfigurationsEnvFromHDP22),
                       ("webhcat-site", self.validateWebhcatConfigurationsFromHDP22),
                       ("hiveserver2-site", self.validateHiveServer2ConfigurationsFromHDP23),
                       ("hive-site", self.validateHiveConfigurationsFromHDP23),
                       ("hive-interactive-env", self.validateHiveInteractiveEnvConfigurationsFromHDP25),
                       ("hive-interactive-site", self.validateHiveInteractiveSiteConfigurationsFromHDP25),
                       ("hive-env", self.validateHiveConfigurationsEnvFromHDP25)]


  def validateHiveConfigurationsFromHDP21(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = [ {"config-name": 'hive.tez.container.size', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'hive.tez.container.size')},
                        {"config-name": 'hive.tez.java.opts', "item": self.validateXmxValue(properties, recommendedDefaults, 'hive.tez.java.opts')},
                        {"config-name": 'hive.auto.convert.join.noconditionaltask.size', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'hive.auto.convert.join.noconditionaltask.size')} ]
    yarnSiteProperties = self.getSiteProperties(configurations, "yarn-site")
    if yarnSiteProperties:
      yarnSchedulerMaximumAllocationMb = self.to_number(yarnSiteProperties["yarn.scheduler.maximum-allocation-mb"])
      hiveTezContainerSize = self.to_number(properties['hive.tez.container.size'])
      if hiveTezContainerSize is not None and yarnSchedulerMaximumAllocationMb is not None and hiveTezContainerSize > yarnSchedulerMaximumAllocationMb:
        validationItems.append({"config-name": 'hive.tez.container.size', "item": self.getWarnItem("hive.tez.container.size is greater than the maximum container size specified in yarn.scheduler.maximum-allocation-mb")})
    return self.toConfigurationValidationProblems(validationItems, "hive-site")


  def validateHiveServer2ConfigurationsFromHDP22(self, properties, recommendedDefaults, configurations, services, hosts):
    hive_server2 = properties
    validationItems = []
    #Adding Ranger Plugin logic here
    ranger_plugin_properties = self.getSiteProperties(configurations, "ranger-hive-plugin-properties")
    hive_env_properties = self.getSiteProperties(configurations, "hive-env")
    ranger_plugin_enabled = 'hive_security_authorization' in hive_env_properties and hive_env_properties['hive_security_authorization'].lower() == 'ranger'
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    ##Add stack validations only if Ranger is enabled.
    if ("RANGER" in servicesList):
      ##Add stack validations for  Ranger plugin enabled.
      if ranger_plugin_enabled:
        prop_name = 'hive.security.authorization.manager'
        prop_val = "com.xasecure.authorization.hive.authorizer.XaSecureHiveAuthorizerFactory"
        if prop_name not in hive_server2 or hive_server2[prop_name] != prop_val:
          validationItems.append({"config-name": prop_name,
                                  "item": self.getWarnItem(
                                  "If Ranger Hive Plugin is enabled."\
                                  " {0} under hiveserver2-site needs to be set to {1}".format(prop_name,prop_val))})
        prop_name = 'hive.security.authenticator.manager'
        prop_val = "org.apache.hadoop.hive.ql.security.SessionStateUserAuthenticator"
        if prop_name not in hive_server2 or hive_server2[prop_name] != prop_val:
          validationItems.append({"config-name": prop_name,
                                  "item": self.getWarnItem(
                                  "If Ranger Hive Plugin is enabled."\
                                  " {0} under hiveserver2-site needs to be set to {1}".format(prop_name,prop_val))})
        prop_name = 'hive.security.authorization.enabled'
        prop_val = 'true'
        if prop_name in hive_server2 and hive_server2[prop_name] != prop_val:
          validationItems.append({"config-name": prop_name,
                                  "item": self.getWarnItem(
                                  "If Ranger Hive Plugin is enabled."\
                                  " {0} under hiveserver2-site needs to be set to {1}".format(prop_name, prop_val))})
        prop_name = 'hive.conf.restricted.list'
        prop_vals = 'hive.security.authorization.enabled,hive.security.authorization.manager,hive.security.authenticator.manager'.split(',')
        current_vals = []
        missing_vals = []
        if hive_server2 and prop_name in hive_server2:
          current_vals = hive_server2[prop_name].split(',')
          current_vals = [x.strip() for x in current_vals]

        for val in prop_vals:
          if not val in current_vals:
            missing_vals.append(val)

        if missing_vals:
          validationItems.append({"config-name": prop_name,
            "item": self.getWarnItem("If Ranger Hive Plugin is enabled."\
            " {0} under hiveserver2-site needs to contain missing value {1}".format(prop_name, ','.join(missing_vals)))})
      ##Add stack validations for  Ranger plugin disabled.
      elif not ranger_plugin_enabled:
        prop_name = 'hive.security.authorization.manager'
        prop_val = "org.apache.hadoop.hive.ql.security.authorization.plugin.sqlstd.SQLStdHiveAuthorizerFactory"
        if prop_name in hive_server2 and hive_server2[prop_name] != prop_val:
          validationItems.append({"config-name": prop_name,
                                  "item": self.getWarnItem(
                                  "If Ranger Hive Plugin is disabled."\
                                  " {0} needs to be set to {1}".format(prop_name,prop_val))})
        prop_name = 'hive.security.authenticator.manager'
        prop_val = "org.apache.hadoop.hive.ql.security.SessionStateUserAuthenticator"
        if prop_name in hive_server2 and hive_server2[prop_name] != prop_val:
          validationItems.append({"config-name": prop_name,
                                  "item": self.getWarnItem(
                                  "If Ranger Hive Plugin is disabled."\
                                  " {0} needs to be set to {1}".format(prop_name,prop_val))})
    return self.toConfigurationValidationProblems(validationItems, "hiveserver2-site")


  def validateHiveConfigurationsFromHDP22(self, properties, recommendedDefaults, configurations, services, hosts):

    hive_site = properties
    validationItems = []
    stripe_size_values = [8388608, 16777216, 33554432, 67108864, 134217728, 268435456]
    stripe_size_property = "hive.exec.orc.default.stripe.size"
    if stripe_size_property in properties and \
        int(properties[stripe_size_property]) not in stripe_size_values:
      validationItems.append({"config-name": stripe_size_property,
                              "item": self.getWarnItem("Correct values are {0}".format(stripe_size_values))
                             }
      )
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

    configurationValidationProblems = self.toConfigurationValidationProblems(validationItems, "hive-site")
    return configurationValidationProblems


  def validateHiveConfigurationsEnvFromHDP22(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = []
    hive_env = properties
    hive_site = self.getSiteProperties(configurations, "hive-site")
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if "hive_security_authorization" in hive_env and \
        str(hive_env["hive_security_authorization"]).lower() == "none" \
      and str(hive_site["hive.security.authorization.enabled"]).lower() == "true":
      authorization_item = self.getErrorItem("hive_security_authorization should not be None "
                                             "if hive.security.authorization.enabled is set")
      validationItems.append({"config-name": "hive_security_authorization", "item": authorization_item})
    if "hive_security_authorization" in hive_env and \
        str(hive_env["hive_security_authorization"]).lower() == "ranger":
      # ranger-hive-plugin must be enabled in ranger-env
      if 'RANGER' in servicesList:
        ranger_env = self.getServicesSiteProperties(services, 'ranger-env')
        if not ranger_env or not 'ranger-hive-plugin-enabled' in ranger_env or \
            ranger_env['ranger-hive-plugin-enabled'].lower() != 'yes':
          validationItems.append({"config-name": 'hive_security_authorization',
                                  "item": self.getWarnItem(
                                    "ranger-env/ranger-hive-plugin-enabled must be enabled when hive_security_authorization is set to Ranger")})
    return self.toConfigurationValidationProblems(validationItems, "hive-env")


  def validateWebhcatConfigurationsFromHDP22(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = [{"config-name": 'templeton.hadoop.queue.name', "item": self.validatorYarnQueue(properties, recommendedDefaults, 'templeton.hadoop.queue.name', services)}]
    return self.toConfigurationValidationProblems(validationItems, "webhcat-site")


  def validateHiveServer2ConfigurationsFromHDP23(self, properties, recommendedDefaults, configurations, services, hosts):

    hive_server2 = properties
    validationItems = []
    #Adding Ranger Plugin logic here
    ranger_plugin_properties = self.getSiteProperties(configurations, "ranger-hive-plugin-properties")
    hive_env_properties = self.getSiteProperties(configurations, "hive-env")
    ranger_plugin_enabled = 'hive_security_authorization' in hive_env_properties and hive_env_properties['hive_security_authorization'].lower() == 'ranger'
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    ##Add stack validations only if Ranger is enabled.
    if ("RANGER" in servicesList):
      ##Add stack validations for  Ranger plugin enabled.
      if ranger_plugin_enabled:
        prop_name = 'hive.security.authorization.manager'
        prop_val = "org.apache.ranger.authorization.hive.authorizer.RangerHiveAuthorizerFactory"
        if prop_name in hive_server2 and hive_server2[prop_name] != prop_val:
          validationItems.append({"config-name": prop_name,
                                  "item": self.getWarnItem(
                                  "If Ranger Hive Plugin is enabled."\
                                  " {0} under hiveserver2-site needs to be set to {1}".format(prop_name,prop_val))})
        prop_name = 'hive.security.authenticator.manager'
        prop_val = "org.apache.hadoop.hive.ql.security.SessionStateUserAuthenticator"
        if prop_name in hive_server2 and hive_server2[prop_name] != prop_val:
          validationItems.append({"config-name": prop_name,
                                  "item": self.getWarnItem(
                                  "If Ranger Hive Plugin is enabled."\
                                  " {0} under hiveserver2-site needs to be set to {1}".format(prop_name,prop_val))})
        prop_name = 'hive.security.authorization.enabled'
        prop_val = 'true'
        if prop_name in hive_server2 and hive_server2[prop_name] != prop_val:
          validationItems.append({"config-name": prop_name,
                                  "item": self.getWarnItem(
                                  "If Ranger Hive Plugin is enabled."\
                                  " {0} under hiveserver2-site needs to be set to {1}".format(prop_name, prop_val))})
        prop_name = 'hive.conf.restricted.list'
        prop_vals = 'hive.security.authorization.enabled,hive.security.authorization.manager,hive.security.authenticator.manager'.split(',')
        current_vals = []
        missing_vals = []
        if hive_server2 and prop_name in hive_server2:
          current_vals = hive_server2[prop_name].split(',')
          current_vals = [x.strip() for x in current_vals]

        for val in prop_vals:
          if not val in current_vals:
            missing_vals.append(val)

        if missing_vals:
          validationItems.append({"config-name": prop_name,
            "item": self.getWarnItem("If Ranger Hive Plugin is enabled."\
            " {0} under hiveserver2-site needs to contain missing value {1}".format(prop_name, ','.join(missing_vals)))})
      ##Add stack validations for  Ranger plugin disabled.
      elif not ranger_plugin_enabled:
        prop_name = 'hive.security.authorization.manager'
        prop_val = "org.apache.hadoop.hive.ql.security.authorization.plugin.sqlstd.SQLStdHiveAuthorizerFactory"
        if prop_name in hive_server2 and hive_server2[prop_name] != prop_val:
          validationItems.append({"config-name": prop_name,
                                  "item": self.getWarnItem(
                                  "If Ranger Hive Plugin is disabled."\
                                  " {0} needs to be set to {1}".format(prop_name,prop_val))})
        prop_name = 'hive.security.authenticator.manager'
        prop_val = "org.apache.hadoop.hive.ql.security.SessionStateUserAuthenticator"
        if prop_name in hive_server2 and hive_server2[prop_name] != prop_val:
          validationItems.append({"config-name": prop_name,
                                  "item": self.getWarnItem(
                                  "If Ranger Hive Plugin is disabled."\
                                  " {0} needs to be set to {1}".format(prop_name,prop_val))})

    validationProblems = self.toConfigurationValidationProblems(validationItems, "hiveserver2-site")
    return validationProblems


  def validateHiveConfigurationsFromHDP23(self, properties, recommendedDefaults, configurations, services, hosts):

    hive_site = properties
    hive_env_properties = self.getSiteProperties(configurations, "hive-env")
    validationItems = []
    sqla_db_used = "hive_database" in hive_env_properties and \
                   hive_env_properties['hive_database'] == 'Existing SQL Anywhere Database'
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

    configurationValidationProblems = self.toConfigurationValidationProblems(validationItems, "hive-site")
    return configurationValidationProblems


  def validateHiveInteractiveEnvConfigurationsFromHDP25(self, properties, recommendedDefaults, configurations, services, hosts):
    hive_site_env_properties = self.getSiteProperties(configurations, "hive-interactive-env")
    yarn_site_properties = self.getSiteProperties(configurations, "yarn-site")
    validationItems = []
    hsi_hosts = self.getHostsForComponent(services, "HIVE", "HIVE_SERVER_INTERACTIVE")

    # Check for expecting 'enable_hive_interactive' is ON given that there is HSI on atleast one host present.
    if len(hsi_hosts) > 0:
      # HIVE_SERVER_INTERACTIVE is mapped to a host
      if 'enable_hive_interactive' not in hive_site_env_properties or (
            'enable_hive_interactive' in hive_site_env_properties and
            hive_site_env_properties['enable_hive_interactive'].lower() != 'true'):

        validationItems.append({"config-name": "enable_hive_interactive",
                                "item": self.getErrorItem(
                                  "HIVE_SERVER_INTERACTIVE requires enable_hive_interactive in hive-interactive-env set to true.")})
    else:
      # no  HIVE_SERVER_INTERACTIVE
      if 'enable_hive_interactive' in hive_site_env_properties and hive_site_env_properties[
        'enable_hive_interactive'].lower() != 'false':
        validationItems.append({"config-name": "enable_hive_interactive",
                                "item": self.getErrorItem(
                                  "enable_hive_interactive in hive-interactive-env should be set to false.")})

    # Check for 'yarn.resourcemanager.scheduler.monitor.enable' config to be true if HSI is ON.
    if yarn_site_properties and 'yarn.resourcemanager.scheduler.monitor.enable' in yarn_site_properties:
      scheduler_monitor_enabled = yarn_site_properties['yarn.resourcemanager.scheduler.monitor.enable']
      if scheduler_monitor_enabled.lower() == 'false' and hive_site_env_properties and 'enable_hive_interactive' in hive_site_env_properties and \
        hive_site_env_properties['enable_hive_interactive'].lower() == 'true':
        validationItems.append({"config-name": "enable_hive_interactive",
                                "item": self.getWarnItem(
                                  "When enabling LLAP, set 'yarn.resourcemanager.scheduler.monitor.enable' to true to ensure that LLAP gets the full allocated capacity.")})

    validationProblems = self.toConfigurationValidationProblems(validationItems, "hive-interactive-env")
    return validationProblems


  def validateHiveInteractiveSiteConfigurationsFromHDP25(self, properties, recommendedDefaults, configurations, services, hosts):
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
      if "hive.llap.daemon.queue.name" in hsi_site and hsi_site['hive.llap.daemon.queue.name']:
        llap_queue_name = hsi_site['hive.llap.daemon.queue.name']
        llap_queue_cap = self.__getSelectedQueueTotalCap(capacity_scheduler_properties, llap_queue_name, total_cluster_cap)

        if llap_queue_cap:
          llap_queue_cap_perc = float(llap_queue_cap * 100 / total_cluster_cap)
          min_reqd_queue_cap_perc = self.min_queue_perc_reqd_for_llap_and_hive_app(services, hosts, configurations)

          # Validate that the selected queue in 'hive.llap.daemon.queue.name' should be sized >= to minimum required
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

      # Validate that 'hive.server2.enable.doAs' config is not set to 'true' for Hive2.
      if 'hive.server2.enable.doAs' in hsi_site and hsi_site['hive.server2.enable.doAs'] == "true":
          validationItems.append({"config-name": "hive.server2.enable.doAs", "item": self.getErrorItem("Value should be set to 'false' for Hive2.")})

      # Validate that 'Maximum Total Concurrent Queries'(hive.server2.tez.sessions.per.default.queue) is not consuming more that
      # 50% of selected queue for LLAP.
      if llap_queue_cap and 'hive.server2.tez.sessions.per.default.queue' in hsi_site:
        num_tez_sessions = hsi_site['hive.server2.tez.sessions.per.default.queue']
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

    # Validate that 'remaining available capacity' in cluster is at least 512 MB, after 'llap' queue is selected,
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


  def validateHiveConfigurationsEnvFromHDP25(self, properties, recommendedDefaults, configurations, services, hosts):

    hive_site_properties = self.getSiteProperties(configurations, "hive-site")
    hive_env_properties = self.getSiteProperties(configurations, "hive-env")
    validationItems = []

    if 'hive.server2.authentication' in hive_site_properties and "LDAP" == hive_site_properties['hive.server2.authentication']:
      if 'alert_ldap_username' not in hive_env_properties or hive_env_properties['alert_ldap_username'] == "":
        validationItems.append({"config-name": "alert_ldap_username",
                                "item": self.getWarnItem(
                                  "Provide an user to be used for alerts. Hive authentication type LDAP requires valid LDAP credentials for the alerts.")})
      if 'alert_ldap_password' not in hive_env_properties or hive_env_properties['alert_ldap_password'] == "":
        validationItems.append({"config-name": "alert_ldap_password",
                                "item": self.getWarnItem(
                                  "Provide the password for the alert user. Hive authentication type LDAP requires valid LDAP credentials for the alerts.")})

    return self.toConfigurationValidationProblems(validationItems, "hive-env")
