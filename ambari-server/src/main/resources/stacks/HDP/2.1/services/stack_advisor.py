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

# Local Imports
from resource_management.core.logger import Logger


class HDP21StackAdvisor(HDP206StackAdvisor):

  def __init__(self):
    super(HDP21StackAdvisor, self).__init__()
    Logger.initialize_logger()

    self.modifyMastersWithMultipleInstances()
    self.modifyCardinalitiesDict()
    self.modifyHeapSizeProperties()
    self.modifyNotValuableComponents()
    self.modifyComponentsNotPreferableOnServer()

  def modifyMastersWithMultipleInstances(self):
    """
    Modify the set of masters with multiple instances.
    Must be overriden in child class.
    """
    self.mastersWithMultipleInstances |= set(['ZOOKEEPER_SERVER', 'HBASE_MASTER'])

  def modifyCardinalitiesDict(self):
    """
    Modify the dictionary of cardinalities.
    Must be overriden in child class.
    """
    self.cardinalitiesDict.update(
      {
        'ZOOKEEPER_SERVER': {"min": 3},
        'HBASE_MASTER': {"min": 1},
      }
    )

  def modifyHeapSizeProperties(self):
    """
    Modify the dictionary of heap size properties.
    Must be overriden in child class.
    """
    # Nothing to do
    pass

  def modifyNotValuableComponents(self):
    """
    Modify the set of components whose host assignment is based on other services.
    Must be overriden in child class.
    """
    self.notValuableComponents |= set(['JOURNALNODE', 'ZKFC', 'GANGLIA_MONITOR', 'APP_TIMELINE_SERVER'])

  def modifyComponentsNotPreferableOnServer(self):
    """
    Modify the set of components that are not preferable on the server.
    Must be overriden in child class.
    """
    self.notPreferableOnServerComponents |= set(['STORM_UI_SERVER', 'DRPC_SERVER', 'STORM_REST_API', 'NIMBUS', 'GANGLIA_SERVER', 'METRICS_COLLECTOR'])

  def getServiceConfigurationRecommenderDict(self):
    parentRecommendConfDict = super(HDP21StackAdvisor, self).getServiceConfigurationRecommenderDict()
    childRecommendConfDict = {
      "OOZIE": self.recommendOozieConfigurations,
      "HIVE": self.recommendHiveConfigurations,
      "TEZ": self.recommendTezConfigurations,
      "STORM": self.recommendStormConfigurations,
      "FALCON": self.recommendFalconConfigurations
    }
    parentRecommendConfDict.update(childRecommendConfDict)
    return parentRecommendConfDict

  def recommendStormConfigurations(self, configurations, clusterData, services, hosts):
    storm_mounts = [
      ("storm.local.dir", ["NODEMANAGER", "NIMBUS"], "/hadoop/storm", "single")
    ]

    self.updateMountProperties("storm-site", storm_mounts, configurations, services, hosts)

  def recommendFalconConfigurations(self, configurations, clusterData, services, hosts):
    falcon_mounts = [
      ("*.falcon.graph.storage.directory", "FALCON_SERVER", "/hadoop/falcon/data/lineage/graphdb", "single")
    ]

    self.updateMountProperties("falcon-startup.properties", falcon_mounts, configurations, services, hosts)

  def recommendOozieConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP21StackAdvisor, self).recommendOozieConfigurations(configurations, clusterData, services, hosts)

    oozieSiteProperties = getSiteProperties(services['configurations'], 'oozie-site')
    oozieEnvProperties = getSiteProperties(services['configurations'], 'oozie-env')
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
        falconUserOldValue = getOldValue(self, services, "falcon-env", "falcon_user")
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
      oldSchemaName = getOldValue(self, services, "oozie-site", "oozie.db.schema.name")
      # under these if constructions we are checking if oozie server hostname available,
      # if schema name was changed or if protocol according to current db type differs with protocol in db connection url(db type was changed)
      if oozieServerHost is not None:
        if oldSchemaName or (protocol and oozieDBConnectionURL and not oozieDBConnectionURL.startswith(protocol)):
          dbConnection = self.getDBConnectionString(oozieEnvProperties['oozie_database']).format(oozieServerHost['Hosts']['host_name'], oozieSiteProperties['oozie.db.schema.name'])
          putOozieProperty('oozie.service.JPAService.jdbc.url', dbConnection)

  def recommendHiveConfigurations(self, configurations, clusterData, services, hosts):
    hiveSiteProperties = getSiteProperties(services['configurations'], 'hive-site')
    hiveEnvProperties = getSiteProperties(services['configurations'], 'hive-env')
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
      oldSchemaName = getOldValue(self, services, "hive-site", "ambari.hive.db.schema.name")
      oldDBType = getOldValue(self, services, "hive-env", "hive_database")
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
        self.put_proxyuser_value("HTTP", meta["hostnames"], services=services, put_function=putCoreSiteProperty)

  def recommendTezConfigurations(self, configurations, clusterData, services, hosts):
    putTezProperty = self.putProperty(configurations, "tez-site")
    putTezProperty("tez.am.resource.memory.mb", int(clusterData['amMemory']))
    putTezProperty("tez.am.java.opts",
                   "-server -Xmx" + str(int(0.8 * clusterData["amMemory"]))
                   + "m -Djava.net.preferIPv4Stack=true -XX:+UseNUMA -XX:+UseParallelGC")
    recommended_tez_queue = self.recommendYarnQueue(services, "tez-site", "tez.queue.name")
    if recommended_tez_queue is not None:
      putTezProperty("tez.queue.name", recommended_tez_queue)

  # TODO, move to Service Advisors.
  def getComponentLayoutSchemes(self):
    parentSchemes = super(HDP21StackAdvisor, self).getComponentLayoutSchemes()
    childSchemes = {
        'APP_TIMELINE_SERVER': {31: 1, "else": 2},
        'FALCON_SERVER': {6: 1, 31: 2, "else": 3}
    }
    parentSchemes.update(childSchemes)
    return parentSchemes

  def getServiceConfigurationValidators(self):
    parentValidators = super(HDP21StackAdvisor, self).getServiceConfigurationValidators()
    childValidators = {
      "HIVE": {"hive-site": self.validateHiveConfigurations},
      "TEZ": {"tez-site": self.validateTezConfigurations}
    }
    self.mergeValidators(parentValidators, childValidators)
    return parentValidators

  def validateHiveConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = [ {"config-name": 'hive.tez.container.size', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'hive.tez.container.size')},
                        {"config-name": 'hive.tez.java.opts', "item": self.validateXmxValue(properties, recommendedDefaults, 'hive.tez.java.opts')},
                        {"config-name": 'hive.auto.convert.join.noconditionaltask.size', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'hive.auto.convert.join.noconditionaltask.size')} ]
    yarnSiteProperties = getSiteProperties(configurations, "yarn-site")
    if yarnSiteProperties:
      yarnSchedulerMaximumAllocationMb = to_number(yarnSiteProperties["yarn.scheduler.maximum-allocation-mb"])
      hiveTezContainerSize = to_number(properties['hive.tez.container.size'])
      if hiveTezContainerSize is not None and yarnSchedulerMaximumAllocationMb is not None and hiveTezContainerSize > yarnSchedulerMaximumAllocationMb:
        validationItems.append({"config-name": 'hive.tez.container.size', "item": self.getWarnItem("hive.tez.container.size is greater than the maximum container size specified in yarn.scheduler.maximum-allocation-mb")})
    return self.toConfigurationValidationProblems(validationItems, "hive-site")

  def validateTezConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = [ {"config-name": 'tez.am.resource.memory.mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'tez.am.resource.memory.mb')},
                        {"config-name": 'tez.am.java.opts', "item": self.validateXmxValue(properties, recommendedDefaults, 'tez.am.java.opts')},
                        {"config-name": 'tez.queue.name', "item": self.validatorYarnQueue(properties, recommendedDefaults, 'tez.queue.name', services)} ]
    return self.toConfigurationValidationProblems(validationItems, "tez-site")

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
