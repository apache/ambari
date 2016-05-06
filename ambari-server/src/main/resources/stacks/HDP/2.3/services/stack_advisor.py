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
import os
import re
import fnmatch
import math
import socket

DB_TYPE_DEFAULT_PORT_MAP = {"MYSQL":"3306", "ORACLE":"1521", "POSTGRES":"5432", "MSSQL":"1433", "SQLA":"2638"}

class HDP23StackAdvisor(HDP22StackAdvisor):

  def getComponentLayoutValidations(self, services, hosts):
    parentItems = super(HDP23StackAdvisor, self).getComponentLayoutValidations(services, hosts)

    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item["StackServiceComponents"] for sublist in componentsListList for item in sublist]
    childItems = []

    if "SPARK" in servicesList:
      if "SPARK_THRIFTSERVER" in servicesList:
        if not "HIVE_SERVER" in servicesList:
          message = "SPARK_THRIFTSERVER requires HIVE services to be selected."
          childItems.append( {"type": 'host-component', "level": 'ERROR', "message": message, "component-name": 'SPARK_THRIFTSERVER'} )

      hmsHosts = self.__getHosts(componentsList, "HIVE_METASTORE") if "HIVE" in servicesList else []
      sparkTsHosts = self.__getHosts(componentsList, "SPARK_THRIFTSERVER") if "SPARK" in servicesList else []

      # if Spark Thrift Server is deployed but no Hive Server is deployed
      if len(sparkTsHosts) > 0 and len(hmsHosts) == 0:
        message = "SPARK_THRIFTSERVER requires HIVE_METASTORE to be selected/deployed."
        childItems.append( { "type": 'host-component', "level": 'ERROR', "message": message, "component-name": 'SPARK_THRIFTSERVER' } )

    parentItems.extend(childItems)
    return parentItems

  def __getHosts(self, componentsList, componentName):
    host_lists = [component["hostnames"] for component in componentsList if
                  component["component_name"] == componentName]
    if host_lists and len(host_lists) > 0:
      return host_lists[0]
    else:
      return []

  def getServiceConfigurationRecommenderDict(self):
    parentRecommendConfDict = super(HDP23StackAdvisor, self).getServiceConfigurationRecommenderDict()
    childRecommendConfDict = {
      "TEZ": self.recommendTezConfigurations,
      "HDFS": self.recommendHDFSConfigurations,
      "YARN": self.recommendYARNConfigurations,
      "HIVE": self.recommendHIVEConfigurations,
      "HBASE": self.recommendHBASEConfigurations,
      "KAFKA": self.recommendKAFKAConfigurations,
      "RANGER": self.recommendRangerConfigurations,
      "RANGER_KMS": self.recommendRangerKMSConfigurations,
      "FALCON": self.recommendFalconConfigurations,
      "STORM": self.recommendStormConfigurations,
      "SQOOP": self.recommendSqoopConfigurations,
      "LOGSEARCH" : self.recommendLogsearchConfigurations
    }
    parentRecommendConfDict.update(childRecommendConfDict)
    return parentRecommendConfDict

  def recommendTezConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP23StackAdvisor, self).recommendTezConfigurations(configurations, clusterData, services, hosts)

    putTezProperty = self.putProperty(configurations, "tez-site")
    if "hive-site" in services["configurations"] and "hive.tez.container.size" in services["configurations"]["hive-site"]["properties"]:
      putTezProperty("tez.task.resource.memory.mb", services["configurations"]["hive-site"]["properties"]["hive.tez.container.size"])
    # remove 2gb limit for tez.runtime.io.sort.mb
    # in HDP 2.3 "tez.runtime.sorter.class" is set by default to PIPELINED, in other case comment calculation code below
    taskResourceMemory = int(configurations["tez-site"]["properties"]["tez.task.resource.memory.mb"])
    putTezProperty("tez.runtime.io.sort.mb", int(taskResourceMemory * 0.4))

    if "tez-site" in services["configurations"] and "tez.runtime.sorter.class" in services["configurations"]["tez-site"]["properties"]:
      if services["configurations"]["tez-site"]["properties"]["tez.runtime.sorter.class"] == "LEGACY":
        putTezAttribute = self.putPropertyAttribute(configurations, "tez-site")
        putTezAttribute("tez.runtime.io.sort.mb", "maximum", 1800)
    pass

    serverProperties = services["ambari-server-properties"]
    latest_tez_jar_version = None

    server_host = socket.getfqdn()
    server_port = '8080'
    server_protocol = 'http'
    views_dir = '/var/lib/ambari-server/resources/views/'

    if serverProperties:
      if 'client.api.port' in serverProperties:
        server_port = serverProperties['client.api.port']
      if 'views.dir' in serverProperties:
        views_dir = serverProperties['views.dir']
      if 'api.ssl' in serverProperties:
        if serverProperties['api.ssl'].lower() == 'true':
          server_protocol = 'https'

      views_work_dir = os.path.join(views_dir, 'work')

      if os.path.exists(views_work_dir) and os.path.isdir(views_work_dir):
        last_version = '0.0.0'
        for file in os.listdir(views_work_dir):
          if fnmatch.fnmatch(file, 'TEZ{*}'):
            current_version = file.lstrip("TEZ{").rstrip("}") # E.g.: TEZ{0.7.0.2.3.0.0-2154}
            if self.versionCompare(current_version.replace("-", "."), last_version.replace("-", ".")) >= 0:
              latest_tez_jar_version = current_version
              last_version = current_version
            pass
        pass
      pass
    pass

    if latest_tez_jar_version:
      tez_url = '{0}://{1}:{2}/#/main/views/TEZ/{3}/TEZ_CLUSTER_INSTANCE'.format(server_protocol, server_host, server_port, latest_tez_jar_version)
      putTezProperty("tez.tez-ui.history-url.base", tez_url)
    pass

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
    putTezProperty('tez.am.launch.cmd-opts', "-XX:+PrintGCDetails -verbose:gc -XX:+PrintGCTimeStamps -XX:+UseNUMA " + jvmGCParams)
    putTezProperty('tez.task.launch.cmd-opts', "-XX:+PrintGCDetails -verbose:gc -XX:+PrintGCTimeStamps -XX:+UseNUMA " + jvmGCParams)


  def recommendHBASEConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP23StackAdvisor, self).recommendHBASEConfigurations(configurations, clusterData, services, hosts)
    putHbaseSiteProperty = self.putProperty(configurations, "hbase-site", services)
    putHbaseSitePropertyAttributes = self.putPropertyAttribute(configurations, "hbase-site")
    putHbaseEnvProperty = self.putProperty(configurations, "hbase-env", services)
    putHbaseEnvPropertyAttributes = self.putPropertyAttribute(configurations, "hbase-env")

    # bucket cache for 1.x is configured slightly differently, HBASE-11520
    threshold = 23 # 2 Gb is reserved for other offheap memory
    if (int(clusterData["hbaseRam"]) > threshold):
      # To enable cache - calculate values
      regionserver_total_ram = int(clusterData["hbaseRam"]) * 1024
      regionserver_heap_size = 20480
      regionserver_max_direct_memory_size = regionserver_total_ram - regionserver_heap_size
      hfile_block_cache_size = '0.4'
      block_cache_heap = 8192 # int(regionserver_heap_size * hfile_block_cache_size)
      hbase_regionserver_global_memstore_size = '0.4'
      reserved_offheap_memory = 2048
      bucketcache_offheap_memory = regionserver_max_direct_memory_size - reserved_offheap_memory
      hbase_bucketcache_size = bucketcache_offheap_memory

      # Set values in hbase-site
      putHbaseSiteProperty('hfile.block.cache.size', hfile_block_cache_size)
      putHbaseSiteProperty('hbase.regionserver.global.memstore.size', hbase_regionserver_global_memstore_size)
      putHbaseSiteProperty('hbase.bucketcache.ioengine', 'offheap')
      putHbaseSiteProperty('hbase.bucketcache.size', hbase_bucketcache_size)
      # 2.2 stack method was called earlier, unset
      putHbaseSitePropertyAttributes('hbase.bucketcache.percentage.in.combinedcache', 'delete', 'true')

      # Enable in hbase-env
      putHbaseEnvProperty('hbase_max_direct_memory_size', regionserver_max_direct_memory_size)
      putHbaseEnvProperty('hbase_regionserver_heapsize', regionserver_heap_size)
    else:
      # Disable
      putHbaseSitePropertyAttributes('hbase.bucketcache.ioengine', 'delete', 'true')
      putHbaseSitePropertyAttributes('hbase.bucketcache.size', 'delete', 'true')
      putHbaseSitePropertyAttributes('hbase.bucketcache.percentage.in.combinedcache', 'delete', 'true')

      putHbaseEnvPropertyAttributes('hbase_max_direct_memory_size', 'delete', 'true')

    if 'hbase-env' in services['configurations'] and 'phoenix_sql_enabled' in services['configurations']['hbase-env']['properties'] and \
       'true' == services['configurations']['hbase-env']['properties']['phoenix_sql_enabled'].lower():
      putHbaseSiteProperty("hbase.rpc.controllerfactory.class", "org.apache.hadoop.hbase.ipc.controller.ServerRpcControllerFactory")
      putHbaseSiteProperty("hbase.region.server.rpc.scheduler.factory.class", "org.apache.hadoop.hbase.ipc.PhoenixRpcSchedulerFactory")
    else:
      putHbaseSitePropertyAttributes('hbase.region.server.rpc.scheduler.factory.class', 'delete', 'true')


  def recommendHIVEConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP23StackAdvisor, self).recommendHIVEConfigurations(configurations, clusterData, services, hosts)
    putHiveSiteProperty = self.putProperty(configurations, "hive-site", services)
    putHiveServerProperty = self.putProperty(configurations, "hiveserver2-site", services)
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

    # atlas
    hooks_property = "hive.exec.post.hooks"
    if hooks_property in configurations["hive-site"]["properties"]:
      hooks_value = configurations["hive-site"]["properties"][hooks_property]
    else:
      hooks_value = " "

    include_atlas = "ATLAS" in servicesList
    atlas_hook_class = "org.apache.atlas.hive.hook.HiveHook"
    if include_atlas and atlas_hook_class not in hooks_value:
      if hooks_value == " ":
        hooks_value = atlas_hook_class
      else:
        hooks_value = hooks_value + "," + atlas_hook_class
    if not include_atlas and atlas_hook_class in hooks_value:
      hooks_classes = []
      for hook_class in hooks_value.split(","):
        if hook_class != atlas_hook_class and hook_class != " ":
          hooks_classes.append(hook_class)
      if hooks_classes:
        hooks_value = ",".join(hooks_classes)
      else:
        hooks_value = " "
    putHiveSiteProperty(hooks_property, hooks_value)

    atlas_server_host_info = self.getHostWithComponent("ATLAS", "ATLAS_SERVER", services, hosts)
    if include_atlas and atlas_server_host_info:
      atlas_rest_host = atlas_server_host_info['Hosts']['host_name']
      scheme = "http"
      metadata_port = "21000"
      if 'application-properties' in services['configurations']:
        tls_enabled = services['configurations']['application-properties']['properties']['atlas.enableTLS']
        metadata_port =  services['configurations']['application-properties']['properties']['atlas.server.http.port']
        if tls_enabled.lower() == "true":
          scheme = "https"
          metadata_port =  services['configurations']['application-properties']['properties']['atlas.server.https.port']
      putHiveSiteProperty('atlas.rest.address', '{0}://{1}:{2}'.format(scheme, atlas_rest_host, metadata_port))
    else:
      putHiveSitePropertyAttribute('atlas.cluster.name', 'delete', 'true')
      putHiveSitePropertyAttribute('atlas.rest.address', 'delete', 'true')

  def recommendHDFSConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP23StackAdvisor, self).recommendHDFSConfigurations(configurations, clusterData, services, hosts)

    putHdfsSiteProperty = self.putProperty(configurations, "hdfs-site", services)
    putHdfsSitePropertyAttribute = self.putPropertyAttribute(configurations, "hdfs-site")

    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if "HAWQ" in servicesList:
      # Set dfs.allow.truncate to true
      putHdfsSiteProperty('dfs.allow.truncate', 'true')

    if ('ranger-hdfs-plugin-properties' in services['configurations']) and ('ranger-hdfs-plugin-enabled' in services['configurations']['ranger-hdfs-plugin-properties']['properties']):
      rangerPluginEnabled = ''
      if 'ranger-hdfs-plugin-properties' in configurations and 'ranger-hdfs-plugin-enabled' in  configurations['ranger-hdfs-plugin-properties']['properties']:
        rangerPluginEnabled = configurations['ranger-hdfs-plugin-properties']['properties']['ranger-hdfs-plugin-enabled']
      elif 'ranger-hdfs-plugin-properties' in services['configurations'] and 'ranger-hdfs-plugin-enabled' in services['configurations']['ranger-hdfs-plugin-properties']['properties']:
        rangerPluginEnabled = services['configurations']['ranger-hdfs-plugin-properties']['properties']['ranger-hdfs-plugin-enabled']

      if rangerPluginEnabled and (rangerPluginEnabled.lower() == 'Yes'.lower()):
        putHdfsSiteProperty("dfs.namenode.inode.attributes.provider.class",'org.apache.ranger.authorization.hadoop.RangerHdfsAuthorizer')
      else:
        putHdfsSitePropertyAttribute('dfs.namenode.inode.attributes.provider.class', 'delete', 'true')
    else:
      putHdfsSitePropertyAttribute('dfs.namenode.inode.attributes.provider.class', 'delete', 'true')

  def recommendKAFKAConfigurations(self, configurations, clusterData, services, hosts):
    kafka_broker = getServicesSiteProperties(services, "kafka-broker")

    # kerberos security for kafka is decided from `security.inter.broker.protocol` property value
    security_enabled = (kafka_broker is not None and 'security.inter.broker.protocol' in  kafka_broker
                        and 'SASL' in kafka_broker['security.inter.broker.protocol'])
    putKafkaBrokerProperty = self.putProperty(configurations, "kafka-broker", services)
    putKafkaLog4jProperty = self.putProperty(configurations, "kafka-log4j", services)
    putKafkaBrokerAttributes = self.putPropertyAttribute(configurations, "kafka-broker")

    #If AMS is part of Services, use the KafkaTimelineMetricsReporter for metric reporting. Default is ''.
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if "AMBARI_METRICS" in servicesList:
      putKafkaBrokerProperty('kafka.metrics.reporters', 'org.apache.hadoop.metrics2.sink.kafka.KafkaTimelineMetricsReporter')

    if "ranger-env" in services["configurations"] and "ranger-kafka-plugin-properties" in services["configurations"] and \
        "ranger-kafka-plugin-enabled" in services["configurations"]["ranger-env"]["properties"]:
      putKafkaRangerPluginProperty = self.putProperty(configurations, "ranger-kafka-plugin-properties", services)
      rangerEnvKafkaPluginProperty = services["configurations"]["ranger-env"]["properties"]["ranger-kafka-plugin-enabled"]
      putKafkaRangerPluginProperty("ranger-kafka-plugin-enabled", rangerEnvKafkaPluginProperty)

    if 'ranger-kafka-plugin-properties' in services['configurations'] and ('ranger-kafka-plugin-enabled' in services['configurations']['ranger-kafka-plugin-properties']['properties']):
      kafkaLog4jRangerLines = [{
        "name": "log4j.appender.rangerAppender",
        "value": "org.apache.log4j.DailyRollingFileAppender"
        },
        {
          "name": "log4j.appender.rangerAppender.DatePattern",
          "value": "'.'yyyy-MM-dd-HH"
        },
        {
          "name": "log4j.appender.rangerAppender.File",
          "value": "${kafka.logs.dir}/ranger_kafka.log"
        },
        {
          "name": "log4j.appender.rangerAppender.layout",
          "value": "org.apache.log4j.PatternLayout"
        },
        {
          "name": "log4j.appender.rangerAppender.layout.ConversionPattern",
          "value": "%d{ISO8601} %p [%t] %C{6} (%F:%L) - %m%n"
        },
        {
          "name": "log4j.logger.org.apache.ranger",
          "value": "INFO, rangerAppender"
        }]

      rangerPluginEnabled=''
      if 'ranger-kafka-plugin-properties' in configurations and 'ranger-kafka-plugin-enabled' in  configurations['ranger-kafka-plugin-properties']['properties']:
        rangerPluginEnabled = configurations['ranger-kafka-plugin-properties']['properties']['ranger-kafka-plugin-enabled']
      elif 'ranger-kafka-plugin-properties' in services['configurations'] and 'ranger-kafka-plugin-enabled' in services['configurations']['ranger-kafka-plugin-properties']['properties']:
        rangerPluginEnabled = services['configurations']['ranger-kafka-plugin-properties']['properties']['ranger-kafka-plugin-enabled']

      if  rangerPluginEnabled and rangerPluginEnabled.lower() == "Yes".lower():
        # recommend authorizer.class.name
        putKafkaBrokerProperty("authorizer.class.name", 'org.apache.ranger.authorization.kafka.authorizer.RangerKafkaAuthorizer')
        # change kafka-log4j when ranger plugin is installed

        if 'kafka-log4j' in services['configurations'] and 'content' in services['configurations']['kafka-log4j']['properties']:
          kafkaLog4jContent = services['configurations']['kafka-log4j']['properties']['content']
          for item in range(len(kafkaLog4jRangerLines)):
            if kafkaLog4jRangerLines[item]["name"] not in kafkaLog4jContent:
              kafkaLog4jContent+= '\n' + kafkaLog4jRangerLines[item]["name"] + '=' + kafkaLog4jRangerLines[item]["value"]
          putKafkaLog4jProperty("content",kafkaLog4jContent)


      else:
        # Kerberized Cluster with Ranger plugin disabled
        if security_enabled and 'kafka-broker' in services['configurations'] and 'authorizer.class.name' in services['configurations']['kafka-broker']['properties'] and \
          services['configurations']['kafka-broker']['properties']['authorizer.class.name'] == 'org.apache.ranger.authorization.kafka.authorizer.RangerKafkaAuthorizer':
          putKafkaBrokerProperty("authorizer.class.name", 'kafka.security.auth.SimpleAclAuthorizer')
        # Non-kerberos Cluster with Ranger plugin disabled
        else:
          putKafkaBrokerAttributes('authorizer.class.name', 'delete', 'true')

    # Non-Kerberos Cluster without Ranger
    elif not security_enabled:
      putKafkaBrokerAttributes('authorizer.class.name', 'delete', 'true')

  def recommendRangerKMSConfigurations(self, configurations, clusterData, services, hosts):
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    putRangerKmsDbksProperty = self.putProperty(configurations, "dbks-site", services)
    putRangerKmsProperty = self.putProperty(configurations, "kms-properties", services)
    kmsEnvProperties = getSiteProperties(services['configurations'], 'kms-env')
    putCoreSiteProperty = self.putProperty(configurations, "core-site", services)
    putCoreSitePropertyAttribute = self.putPropertyAttribute(configurations, "core-site")

    if 'kms-properties' in services['configurations'] and ('DB_FLAVOR' in services['configurations']['kms-properties']['properties']):

      rangerKmsDbFlavor = services['configurations']["kms-properties"]["properties"]["DB_FLAVOR"]

      if ('db_host' in services['configurations']['kms-properties']['properties']) and ('db_name' in services['configurations']['kms-properties']['properties']):

        rangerKmsDbHost =   services['configurations']["kms-properties"]["properties"]["db_host"]
        rangerKmsDbName =   services['configurations']["kms-properties"]["properties"]["db_name"]

        ranger_kms_db_url_dict = {
          'MYSQL': {'ranger.ks.jpa.jdbc.driver': 'com.mysql.jdbc.Driver',
                    'ranger.ks.jpa.jdbc.url': 'jdbc:mysql://' + self.getDBConnectionHostPort(rangerKmsDbFlavor, rangerKmsDbHost) + '/' + rangerKmsDbName},
          'ORACLE': {'ranger.ks.jpa.jdbc.driver': 'oracle.jdbc.driver.OracleDriver',
                     'ranger.ks.jpa.jdbc.url': 'jdbc:oracle:thin:@//' + self.getDBConnectionHostPort(rangerKmsDbFlavor, rangerKmsDbHost) + '/' + rangerKmsDbName},
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
      kmsUserOld = getOldValue(self, services, 'kms-env', 'kms_user')
      putCoreSiteProperty('hadoop.proxyuser.{0}.groups'.format(kmsUser), '*')
      if kmsUserOld is not None and kmsUser != kmsUserOld:
        putCoreSitePropertyAttribute("hadoop.proxyuser.{0}.groups".format(kmsUserOld), 'delete', 'true')
        services["forced-configurations"].append({"type" : "core-site", "name" : "hadoop.proxyuser.{0}.groups".format(kmsUserOld)})
        services["forced-configurations"].append({"type" : "core-site", "name" : "hadoop.proxyuser.{0}.groups".format(kmsUser)})


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

    return connection_string


  def recommendRangerConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP23StackAdvisor, self).recommendRangerConfigurations(configurations, clusterData, services, hosts)
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
                   'ranger.jpa.jdbc.url': 'jdbc:oracle:thin:@//' + self.getDBConnectionHostPort(rangerDbFlavor, rangerDbHost) + '/' + rangerDbName},
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
          'ORACLE': {'ranger_privelege_user_jdbc_url': 'jdbc:oracle:thin:@//' + self.getDBConnectionHostPort(rangerDbFlavor, rangerDbHost)},
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


  def recommendYARNConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP23StackAdvisor, self).recommendYARNConfigurations(configurations, clusterData, services, hosts)
    putYarnSiteProperty = self.putProperty(configurations, "yarn-site", services)
    putYarnSitePropertyAttributes = self.putPropertyAttribute(configurations, "yarn-site")
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

    if "tez-site" not in services["configurations"]:
      putYarnSiteProperty('yarn.timeline-service.entity-group-fs-store.group-id-plugin-classes', '')
    else:
      putYarnSiteProperty('yarn.timeline-service.entity-group-fs-store.group-id-plugin-classes', 'org.apache.tez.dag.history.logging.ats.TimelineCachePluginImpl')

    if "ranger-env" in services["configurations"] and "ranger-yarn-plugin-properties" in services["configurations"] and \
        "ranger-yarn-plugin-enabled" in services["configurations"]["ranger-env"]["properties"]:
      putYarnRangerPluginProperty = self.putProperty(configurations, "ranger-yarn-plugin-properties", services)
      rangerEnvYarnPluginProperty = services["configurations"]["ranger-env"]["properties"]["ranger-yarn-plugin-enabled"]
      putYarnRangerPluginProperty("ranger-yarn-plugin-enabled", rangerEnvYarnPluginProperty)
    rangerPluginEnabled = ''
    if 'ranger-yarn-plugin-properties' in configurations and 'ranger-yarn-plugin-enabled' in  configurations['ranger-yarn-plugin-properties']['properties']:
      rangerPluginEnabled = configurations['ranger-yarn-plugin-properties']['properties']['ranger-yarn-plugin-enabled']
    elif 'ranger-yarn-plugin-properties' in services['configurations'] and 'ranger-yarn-plugin-enabled' in services['configurations']['ranger-yarn-plugin-properties']['properties']:
      rangerPluginEnabled = services['configurations']['ranger-yarn-plugin-properties']['properties']['ranger-yarn-plugin-enabled']

    if rangerPluginEnabled and (rangerPluginEnabled.lower() == 'Yes'.lower()):
      putYarnSiteProperty('yarn.acl.enable','true')
      putYarnSiteProperty('yarn.authorization-provider','org.apache.ranger.authorization.yarn.authorizer.RangerYarnAuthorizer')
    else:
      putYarnSitePropertyAttributes('yarn.authorization-provider', 'delete', 'true')

    if 'RANGER_KMS' in servicesList and 'KERBEROS' in servicesList:
      if 'yarn-site' in services["configurations"] and 'yarn.resourcemanager.proxy-user-privileges.enabled' in services["configurations"]["yarn-site"]["properties"]:
        putYarnSiteProperty('yarn.resourcemanager.proxy-user-privileges.enabled', 'false')


  def recommendSqoopConfigurations(self, configurations, clusterData, services, hosts):
    putSqoopSiteProperty = self.putProperty(configurations, "sqoop-site", services)

    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if "ATLAS" in servicesList:
      putSqoopSiteProperty('sqoop.job.data.publish.class', 'org.apache.atlas.sqoop.hook.SqoopHook')

  def recommendStormConfigurations(self, configurations, clusterData, services, hosts):
    putStormStartupProperty = self.putProperty(configurations, "storm-site", services)
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

    if "storm-site" in services["configurations"]:
      # atlas
      notifier_plugin_property = "storm.topology.submission.notifier.plugin.class"
      if notifier_plugin_property in services["configurations"]["storm-site"]["properties"]:
        notifier_plugin_value = services["configurations"]["storm-site"]["properties"][notifier_plugin_property]
        if notifier_plugin_value is None:
          notifier_plugin_value = " "
      else:
        notifier_plugin_value = " "

      include_atlas = "ATLAS" in servicesList
      atlas_hook_class = "org.apache.atlas.storm.hook.StormAtlasHook"
      if include_atlas and atlas_hook_class not in notifier_plugin_value:
        if notifier_plugin_value == " ":
          notifier_plugin_value = atlas_hook_class
        else:
          notifier_plugin_value = notifier_plugin_value + "," + atlas_hook_class
      if not include_atlas and atlas_hook_class in notifier_plugin_value:
        application_classes = []
        for application_class in notifier_plugin_value.split(","):
          if application_class != atlas_hook_class and application_class != " ":
            application_classes.append(application_class)
        if application_classes:
          notifier_plugin_value = ",".join(application_classes)
        else:
          notifier_plugin_value = " "
      putStormStartupProperty(notifier_plugin_property, notifier_plugin_value)

  def recommendFalconConfigurations(self, configurations, clusterData, services, hosts):

    putFalconStartupProperty = self.putProperty(configurations, "falcon-startup.properties", services)
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

    # atlas
    application_services_property = "*.application.services"
    if "falcon-startup.properties" in services["configurations"] and application_services_property in services["configurations"]["falcon-startup.properties"]["properties"]:
      application_services_value = services["configurations"]["falcon-startup.properties"]["properties"][application_services_property]
    else:
      application_services_value = " "

    include_atlas = "ATLAS" in servicesList
    atlas_application_class = "org.apache.falcon.atlas.service.AtlasService"
    if include_atlas and atlas_application_class not in application_services_value:
      if application_services_value == " ":
        application_services_value = atlas_application_class
      else:
        application_services_value = application_services_value + "," + atlas_application_class
    if not include_atlas and atlas_application_class in application_services_value:
      application_classes = []
      for application_class in application_services_value.split(","):
        if application_class != atlas_application_class and application_class != " ":
          application_classes.append(application_class)
      if application_classes:
        application_services_value = ",".join(application_classes)
      else:
        application_services_value = " "
    putFalconStartupProperty(application_services_property, application_services_value)

  def recommendLogsearchConfigurations(self, configurations, clusterData, services, hosts):
    putLogsearchProperty = self.putProperty(configurations, "logsearch-properties", services)
    logsearchSolrHosts = self.getComponentHostNames(services, "LOGSEARCH", "LOGSEARCH_SOLR")

    if logsearchSolrHosts is not None and len(logsearchSolrHosts) > 0 \
      and "logsearch-properties" in services["configurations"]:
      # recommend number of shard
      putLogsearchAttribute = self.putPropertyAttribute(configurations, "logsearch-properties")
      putLogsearchAttribute('logsearch.collection.numshards', 'minimum', len(logsearchSolrHosts))
      putLogsearchAttribute('logsearch.collection.numshards', 'maximum', 3 * len(logsearchSolrHosts))
      putLogsearchProperty("logsearch.collection.numshards", 2 * len(logsearchSolrHosts))
      # recommend replication factor
      replicationReccomendFloat = math.log(len(logsearchSolrHosts), 5)
      recommendedReplicationFactor = int(1 + math.floor(replicationReccomendFloat))
      putLogsearchProperty("logsearch.collection.replication.factor", recommendedReplicationFactor)


  def getServiceConfigurationValidators(self):
    parentValidators = super(HDP23StackAdvisor, self).getServiceConfigurationValidators()
    childValidators = {
      "HDFS": {"hdfs-site": self.validateHDFSConfigurations},
      "HIVE": {"hiveserver2-site": self.validateHiveServer2Configurations,
               "hive-site": self.validateHiveConfigurations},
      "HBASE": {"hbase-site": self.validateHBASEConfigurations},
      "KAKFA": {"kafka-broker": self.validateKAFKAConfigurations},
      "YARN": {"yarn-site": self.validateYARNConfigurations}
    }
    self.mergeValidators(parentValidators, childValidators)
    return parentValidators

  def validateHDFSConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    parentValidationProblems = super(HDP23StackAdvisor, self).validateHDFSConfigurations(properties, recommendedDefaults, configurations, services, hosts)

    # We can not access property hadoop.security.authentication from the
    # other config (core-site). That's why we are using another heuristics here
    hdfs_site = properties
    validationItems = [] #Adding Ranger Plugin logic here
    ranger_plugin_properties = getSiteProperties(configurations, "ranger-hdfs-plugin-properties")
    ranger_plugin_enabled = ranger_plugin_properties['ranger-hdfs-plugin-enabled'] if ranger_plugin_properties else 'No'
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if ("RANGER" in servicesList) and (ranger_plugin_enabled.lower() == 'Yes'.lower()):
      if 'dfs.namenode.inode.attributes.provider.class' not in hdfs_site or \
        hdfs_site['dfs.namenode.inode.attributes.provider.class'].lower() != 'org.apache.ranger.authorization.hadoop.RangerHdfsAuthorizer'.lower():
        validationItems.append({"config-name": 'dfs.namenode.inode.attributes.provider.class',
                                    "item": self.getWarnItem(
                                      "dfs.namenode.inode.attributes.provider.class needs to be set to 'org.apache.ranger.authorization.hadoop.RangerHdfsAuthorizer' if Ranger HDFS Plugin is enabled.")})

    # Check if dfs.allow.truncate is true
    if "HAWQ" in servicesList and \
        not ("dfs.allow.truncate" in services["configurations"]["hdfs-site"]["properties"] and \
        services["configurations"]["hdfs-site"]["properties"]["dfs.allow.truncate"].lower() == 'true'):
        validationItems.append({"config-name": "dfs.allow.truncate",
                                "item": self.getWarnItem("HAWQ requires dfs.allow.truncate in hdfs-site.xml set to True.")})

    validationProblems = self.toConfigurationValidationProblems(validationItems, "hdfs-site")
    validationProblems.extend(parentValidationProblems)
    return validationProblems


  def validateHiveConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    parentValidationProblems = super(HDP23StackAdvisor, self).validateHiveConfigurations(properties, recommendedDefaults, configurations, services, hosts)
    hive_site = properties
    hive_env_properties = getSiteProperties(configurations, "hive-env")
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
    configurationValidationProblems.extend(parentValidationProblems)
    return configurationValidationProblems

  def validateHiveServer2Configurations(self, properties, recommendedDefaults, configurations, services, hosts):
    super(HDP23StackAdvisor, self).validateHiveServer2Configurations(properties, recommendedDefaults, configurations, services, hosts)
    hive_server2 = properties
    validationItems = []
    #Adding Ranger Plugin logic here
    ranger_plugin_properties = getSiteProperties(configurations, "ranger-hive-plugin-properties")
    hive_env_properties = getSiteProperties(configurations, "hive-env")
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
    return self.toConfigurationValidationProblems(validationItems, "hiveserver2-site")

  def validateHBASEConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    super(HDP23StackAdvisor, self).validateHBASEConfigurations(properties, recommendedDefaults, configurations, services, hosts)
    hbase_site = properties
    validationItems = []

    #Adding Ranger Plugin logic here
    ranger_plugin_properties = getSiteProperties(configurations, "ranger-hbase-plugin-properties")
    ranger_plugin_enabled = ranger_plugin_properties['ranger-hbase-plugin-enabled'] if ranger_plugin_properties else 'No'
    prop_name = 'hbase.security.authorization'
    prop_val = "true"
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if ("RANGER" in servicesList) and (ranger_plugin_enabled.lower() == 'Yes'.lower()):
      if hbase_site[prop_name] != prop_val:
        validationItems.append({"config-name": prop_name,
                                "item": self.getWarnItem(
                                "If Ranger HBase Plugin is enabled."\
                                "{0} needs to be set to {1}".format(prop_name,prop_val))})
      prop_name = "hbase.coprocessor.master.classes"
      prop_val = "org.apache.ranger.authorization.hbase.RangerAuthorizationCoprocessor"
      exclude_val = "org.apache.hadoop.hbase.security.access.AccessController"
      if (prop_val in hbase_site[prop_name] and exclude_val not in hbase_site[prop_name]):
        pass
      else:
        validationItems.append({"config-name": prop_name,
                                "item": self.getWarnItem(
                                "If Ranger HBase Plugin is enabled."\
                                " {0} needs to contain {1} instead of {2}".format(prop_name,prop_val,exclude_val))})
      prop_name = "hbase.coprocessor.region.classes"
      prop_val = "org.apache.ranger.authorization.hbase.RangerAuthorizationCoprocessor"
      if (prop_val in hbase_site[prop_name] and exclude_val not in hbase_site[prop_name]):
        pass
      else:
        validationItems.append({"config-name": prop_name,
                                "item": self.getWarnItem(
                                "If Ranger HBase Plugin is enabled."\
                                " {0} needs to contain {1} instead of {2}".format(prop_name,prop_val,exclude_val))})

    return self.toConfigurationValidationProblems(validationItems, "hbase-site")

  def validateKAFKAConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    kafka_broker = properties
    validationItems = []

    #Adding Ranger Plugin logic here
    ranger_plugin_properties = getSiteProperties(configurations, "ranger-kafka-plugin-properties")
    ranger_plugin_enabled = ranger_plugin_properties['ranger-kafka-plugin-enabled']
    prop_name = 'authorizer.class.name'
    prop_val = "org.apache.ranger.authorization.kafka.authorizer.RangerKafkaAuthorizer"
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if ("RANGER" in servicesList) and (ranger_plugin_enabled.lower() == 'Yes'.lower()):
      if kafka_broker[prop_name] != prop_val:
        validationItems.append({"config-name": prop_name,
                                "item": self.getWarnItem(
                                "If Ranger Kafka Plugin is enabled."\
                                "{0} needs to be set to {1}".format(prop_name,prop_val))})

    return self.toConfigurationValidationProblems(validationItems, "kafka-broker")

  def validateYARNConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    yarn_site = properties
    validationItems = []
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if 'RANGER_KMS' in servicesList and 'KERBEROS' in servicesList:
      yarn_resource_proxy_enabled = yarn_site['yarn.resourcemanager.proxy-user-privileges.enabled']
      if yarn_resource_proxy_enabled.lower() == 'true':
        validationItems.append({"config-name": 'yarn.resourcemanager.proxy-user-privileges.enabled',
          "item": self.getWarnItem("If Ranger KMS service is installed set yarn.resourcemanager.proxy-user-privileges.enabled "\
          "property value as false under yarn-site"
        )})

    return self.toConfigurationValidationProblems(validationItems, "yarn-site")

  def isComponentUsingCardinalityForLayout(self, componentName):
    return componentName in ['NFS_GATEWAY', 'PHOENIX_QUERY_SERVER', 'SPARK_THRIFTSERVER']
