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

# Python Imports
import os
import re
import fnmatch
import math
import socket

# Local Imports
from resource_management.core.logger import Logger


DB_TYPE_DEFAULT_PORT_MAP = {"MYSQL":"3306", "ORACLE":"1521", "POSTGRES":"5432", "MSSQL":"1433", "SQLA":"2638"}

class HDP23StackAdvisor(HDP22StackAdvisor):

  def __init__(self):
    super(HDP23StackAdvisor, self).__init__()
    Logger.initialize_logger()

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
      "STORM": self.recommendStormConfigurations,
      "SQOOP": self.recommendSqoopConfigurations,
      "FALCON": self.recommendFalconConfigurations
    }
    parentRecommendConfDict.update(childRecommendConfDict)
    return parentRecommendConfDict

  def recommendTezConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP23StackAdvisor, self).recommendTezConfigurations(configurations, clusterData, services, hosts)

    putTezProperty = self.putProperty(configurations, "tez-site")

    if "HIVE" in self.getServiceNames(services):
      if not "hive-site" in configurations:
        self.recommendHIVEConfigurations(configurations, clusterData, services, hosts)

      if "hive-site" in configurations and "hive.tez.container.size" in configurations["hive-site"]["properties"]:
        putTezProperty("tez.task.resource.memory.mb", configurations["hive-site"]["properties"]["hive.tez.container.size"])

    # remove 2gb limit for tez.runtime.io.sort.mb
    # in HDP 2.3 "tez.runtime.sorter.class" is set by default to PIPELINED, in other case comment calculation code below
    taskResourceMemory = int(configurations["tez-site"]["properties"]["tez.task.resource.memory.mb"])
    # fit io.sort.mb into tenured regions
    putTezProperty("tez.runtime.io.sort.mb", int(taskResourceMemory * 0.8 * 0.33))

    if "tez-site" in services["configurations"] and "tez.runtime.sorter.class" in services["configurations"]["tez-site"]["properties"]:
      if services["configurations"]["tez-site"]["properties"]["tez.runtime.sorter.class"] == "LEGACY":
        putTezAttribute = self.putPropertyAttribute(configurations, "tez-site")
        putTezAttribute("tez.runtime.io.sort.mb", "maximum", 1800)
    pass

    serverProperties = services["ambari-server-properties"]
    latest_tez_jar_version = None

    server_host = socket.getfqdn()
    for host in hosts["items"]:
      if server_host == host["Hosts"]["host_name"]:
        server_host = host["Hosts"]["public_host_name"]
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
    # Note: Same calculation is done in 2.6/stack_advisor::recommendTezConfigurations() for 'tez.task.launch.cmd-opts',
    # and along with it, are appended heap dump opts. If something changes here, make sure to change it in 2.6 stack.
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
      if 'hbase.rpc.controllerfactory.class' in services['configurations']['hbase-site']['properties'] and \
          services['configurations']['hbase-site']['properties']['hbase.rpc.controllerfactory.class'] == \
            'org.apache.hadoop.hbase.ipc.controller.ServerRpcControllerFactory':
        putHbaseSitePropertyAttributes('hbase.rpc.controllerfactory.class', 'delete', 'true')

      putHbaseSiteProperty("hbase.region.server.rpc.scheduler.factory.class", "org.apache.hadoop.hbase.ipc.PhoenixRpcSchedulerFactory")
    else:
      putHbaseSitePropertyAttributes('hbase.region.server.rpc.scheduler.factory.class', 'delete', 'true')

  def recommendHIVEConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP23StackAdvisor, self).recommendHIVEConfigurations(configurations, clusterData, services, hosts)
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

    enable_atlas_hook = False
    if is_atlas_present_in_cluster:
      putHiveEnvProperty("hive.atlas.hook", "true")
    else:
      putHiveEnvProperty("hive.atlas.hook", "false")

    if ('hive-env' in services['configurations']) and ('hive.atlas.hook' in services['configurations']['hive-env']['properties']):
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

  def recommendHDFSConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP23StackAdvisor, self).recommendHDFSConfigurations(configurations, clusterData, services, hosts)

    putHdfsSiteProperty = self.putProperty(configurations, "hdfs-site", services)
    putHdfsSitePropertyAttribute = self.putPropertyAttribute(configurations, "hdfs-site")

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

    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    kafka_broker = getServicesSiteProperties(services, "kafka-broker")

    security_enabled = self.isSecurityEnabled(services)

    putKafkaBrokerProperty = self.putProperty(configurations, "kafka-broker", services)
    putKafkaLog4jProperty = self.putProperty(configurations, "kafka-log4j", services)
    putKafkaBrokerAttributes = self.putPropertyAttribute(configurations, "kafka-broker")

    if security_enabled:
      kafka_env = getServicesSiteProperties(services, "kafka-env")
      kafka_user = kafka_env.get('kafka_user') if kafka_env is not None else None

      if kafka_user is not None:
        kafka_super_users = kafka_broker.get('super.users') if kafka_broker is not None else None

        # kafka_super_super_users is expected to be formatted as:  User:user1;User:user2
        if kafka_super_users is not None and kafka_super_users != '':
          # Parse kafka_super_users to get a set of unique user names and rebuild the property value
          user_names = set()
          user_names.add(kafka_user)
          for match in re.findall('User:([^;]*)', kafka_super_users):
            user_names.add(match)
          kafka_super_users = 'User:' + ";User:".join(user_names)
        else:
          kafka_super_users = 'User:' + kafka_user

        putKafkaBrokerProperty("super.users", kafka_super_users)

      putKafkaBrokerProperty("principal.to.local.class", "kafka.security.auth.KerberosPrincipalToLocal")
      putKafkaBrokerProperty("security.inter.broker.protocol", "PLAINTEXTSASL")
      putKafkaBrokerProperty("zookeeper.set.acl", "true")

    else:  # not security_enabled
      # remove unneeded properties
      putKafkaBrokerAttributes('super.users', 'delete', 'true')
      putKafkaBrokerAttributes('principal.to.local.class', 'delete', 'true')
      putKafkaBrokerAttributes('security.inter.broker.protocol', 'delete', 'true')

    # Update ranger-kafka-plugin-properties/ranger-kafka-plugin-enabled to match ranger-env/ranger-kafka-plugin-enabled
    if "ranger-env" in services["configurations"] \
      and "ranger-kafka-plugin-properties" in services["configurations"] \
      and "ranger-kafka-plugin-enabled" in services["configurations"]["ranger-env"]["properties"]:
      putKafkaRangerPluginProperty = self.putProperty(configurations, "ranger-kafka-plugin-properties", services)
      ranger_kafka_plugin_enabled = services["configurations"]["ranger-env"]["properties"]["ranger-kafka-plugin-enabled"]
      putKafkaRangerPluginProperty("ranger-kafka-plugin-enabled", ranger_kafka_plugin_enabled)

    # Determine if the Ranger/Kafka Plugin is enabled
    ranger_plugin_enabled = "RANGER" in servicesList
    # Only if the RANGER service is installed....
    if ranger_plugin_enabled:
      # If ranger-kafka-plugin-properties/ranger-kafka-plugin-enabled,
      # determine if the Ranger/Kafka plug-in enabled enabled or not
      if 'ranger-kafka-plugin-properties' in configurations and \
          'ranger-kafka-plugin-enabled' in configurations['ranger-kafka-plugin-properties']['properties']:
        ranger_plugin_enabled = configurations['ranger-kafka-plugin-properties']['properties']['ranger-kafka-plugin-enabled'].lower() == 'yes'
      # If ranger-kafka-plugin-properties/ranger-kafka-plugin-enabled was not changed,
      # determine if the Ranger/Kafka plug-in enabled enabled or not
      elif 'ranger-kafka-plugin-properties' in services['configurations'] and \
          'ranger-kafka-plugin-enabled' in services['configurations']['ranger-kafka-plugin-properties']['properties']:
        ranger_plugin_enabled = services['configurations']['ranger-kafka-plugin-properties']['properties']['ranger-kafka-plugin-enabled'].lower() == 'yes'

    # Determine the value for kafka-broker/authorizer.class.name
    if ranger_plugin_enabled:
      # If the Ranger plugin for Kafka is enabled, set authorizer.class.name to
      # "org.apache.ranger.authorization.kafka.authorizer.RangerKafkaAuthorizer" whether Kerberos is
      # enabled or not.
      putKafkaBrokerProperty("authorizer.class.name", 'org.apache.ranger.authorization.kafka.authorizer.RangerKafkaAuthorizer')
    elif security_enabled:
      putKafkaBrokerProperty("authorizer.class.name", 'kafka.security.auth.SimpleAclAuthorizer')
    else:
      putKafkaBrokerAttributes('authorizer.class.name', 'delete', 'true')

    #If AMS is part of Services, use the KafkaTimelineMetricsReporter for metric reporting. Default is ''.
    if "AMBARI_METRICS" in servicesList:
      putKafkaBrokerProperty('kafka.metrics.reporters', 'org.apache.hadoop.metrics2.sink.kafka.KafkaTimelineMetricsReporter')

    if ranger_plugin_enabled:
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

      # change kafka-log4j when ranger plugin is installed
      if 'kafka-log4j' in services['configurations'] and 'content' in services['configurations']['kafka-log4j']['properties']:
        kafkaLog4jContent = services['configurations']['kafka-log4j']['properties']['content']
        for item in range(len(kafkaLog4jRangerLines)):
          if kafkaLog4jRangerLines[item]["name"] not in kafkaLog4jContent:
            kafkaLog4jContent+= '\n' + kafkaLog4jRangerLines[item]["name"] + '=' + kafkaLog4jRangerLines[item]["value"]
        putKafkaLog4jProperty("content",kafkaLog4jContent)

  def recommendRangerKMSConfigurations(self, configurations, clusterData, services, hosts):
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    putRangerKmsDbksProperty = self.putProperty(configurations, "dbks-site", services)
    putRangerKmsProperty = self.putProperty(configurations, "kms-properties", services)
    kmsEnvProperties = getSiteProperties(services['configurations'], 'kms-env')
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
      kmsUserOld = getOldValue(self, services, 'kms-env', 'kms_user')
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
          service_old_user = getOldValue(self, services, config_type, property_name)

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

  def getOracleDBConnectionHostPort(self, db_type, db_host, rangerDbName):
    connection_string = self.getDBConnectionHostPort(db_type, db_host)
    colon_count = db_host.count(':')
    if colon_count == 1 and '/' in db_host:
      connection_string = "//" + connection_string
    elif colon_count == 0 or colon_count == 1:
      connection_string = "//" + connection_string + "/" + rangerDbName if rangerDbName else "//" + connection_string

    return connection_string

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


  def recommendSqoopConfigurations(self, configurations, clusterData, services, hosts):
    putSqoopSiteProperty = self.putProperty(configurations, "sqoop-site", services)
    putSqoopEnvProperty = self.putProperty(configurations, "sqoop-env", services)

    enable_atlas_hook = False
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if "ATLAS" in servicesList:
      putSqoopEnvProperty("sqoop.atlas.hook", "true")
    else:
      putSqoopEnvProperty("sqoop.atlas.hook", "false")

    if ('sqoop-env' in services['configurations']) and ('sqoop.atlas.hook' in services['configurations']['sqoop-env']['properties']):
      if 'sqoop-env' in configurations and 'sqoop.atlas.hook' in configurations['sqoop-env']['properties']:
        enable_atlas_hook = configurations['sqoop-env']['properties']['sqoop.atlas.hook'] == "true"
      elif 'sqoop-env' in services['configurations'] and 'sqoop.atlas.hook' in services['configurations']['sqoop-env']['properties']:
        enable_atlas_hook = services['configurations']['sqoop-env']['properties']['sqoop.atlas.hook'] == "true"

    if enable_atlas_hook:
      putSqoopSiteProperty('sqoop.job.data.publish.class', 'org.apache.atlas.sqoop.hook.SqoopHook')

  def recommendStormConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP23StackAdvisor, self).recommendStormConfigurations(configurations, clusterData, services, hosts)
    putStormStartupProperty = self.putProperty(configurations, "storm-site", services)
    putStormEnvProperty = self.putProperty(configurations, "storm-env", services)
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

    if "storm-site" in services["configurations"]:
      # atlas
      notifier_plugin_property = "storm.topology.submission.notifier.plugin.class"
      if notifier_plugin_property in services["configurations"]["storm-site"]["properties"] and \
         services["configurations"]["storm-site"]["properties"][notifier_plugin_property] is not None:

        notifier_plugin_value = services["configurations"]["storm-site"]["properties"][notifier_plugin_property]
      else:
        notifier_plugin_value = " "

      atlas_is_present = "ATLAS" in servicesList
      atlas_hook_class = "org.apache.atlas.storm.hook.StormAtlasHook"
      atlas_hook_is_set = atlas_hook_class in notifier_plugin_value
      enable_atlas_hook = False

      if atlas_is_present:
        putStormEnvProperty("storm.atlas.hook", "true")
      else:
        putStormEnvProperty("storm.atlas.hook", "false")

      if ('storm-env' in services['configurations']) and ('storm.atlas.hook' in services['configurations']['storm-env']['properties']):
        if 'storm-env' in configurations and 'storm.atlas.hook' in configurations['storm-env']['properties']:
          enable_atlas_hook = configurations['storm-env']['properties']['storm.atlas.hook'] == "true"
        elif 'storm-env' in services['configurations'] and 'storm.atlas.hook' in services['configurations']['storm-env']['properties']:
          enable_atlas_hook = services['configurations']['storm-env']['properties']['storm.atlas.hook'] == "true"

      if enable_atlas_hook and not atlas_hook_is_set:
        notifier_plugin_value = atlas_hook_class if notifier_plugin_value == " " else ",".join([notifier_plugin_value, atlas_hook_class])

      if not enable_atlas_hook and atlas_hook_is_set:
        application_classes = [item for item in notifier_plugin_value.split(",") if item != atlas_hook_class and item != " "]
        notifier_plugin_value = ",".join(application_classes) if application_classes else " "

      if notifier_plugin_value.strip() != "":
        putStormStartupProperty(notifier_plugin_property, notifier_plugin_value)
      else:
        putStormStartupPropertyAttribute = self.putPropertyAttribute(configurations, "storm-site")
        putStormStartupPropertyAttribute(notifier_plugin_property, 'delete', 'true')

  def recommendFalconConfigurations(self, configurations, clusterData, services, hosts):

    putFalconEnvProperty = self.putProperty(configurations, "falcon-env", services)
    enable_atlas_hook = False
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

    if "ATLAS" in servicesList:
      putFalconEnvProperty("falcon.atlas.hook", "true")
    else:
      putFalconEnvProperty("falcon.atlas.hook", "false")

  def getServiceConfigurationValidators(self):
    parentValidators = super(HDP23StackAdvisor, self).getServiceConfigurationValidators()
    childValidators = {
      "HDFS": {"hdfs-site": self.validateHDFSConfigurations},
      "HIVE": {"hiveserver2-site": self.validateHiveServer2Configurations,
               "hive-site": self.validateHiveConfigurations},
      "HBASE": {"hbase-site": self.validateHBASEConfigurations},
      "KAKFA": {"kafka-broker": self.validateKAFKAConfigurations},
      "RANGER": {"admin-properties": self.validateRangerAdminConfigurations,
                 "ranger-env": self.validateRangerConfigurationsEnv}
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
    parentValidationProblems = super(HDP23StackAdvisor, self).validateHiveServer2Configurations(properties, recommendedDefaults, configurations, services, hosts)
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

    validationProblems = self.toConfigurationValidationProblems(validationItems, "hiveserver2-site")
    validationProblems.extend(parentValidationProblems)
    return validationProblems

  def validateHBASEConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    parentValidationProblems = super(HDP23StackAdvisor, self).validateHBASEConfigurations(properties, recommendedDefaults, configurations, services, hosts)
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

    validationProblems = self.toConfigurationValidationProblems(validationItems, "hbase-site")
    validationProblems.extend(parentValidationProblems)
    return validationProblems

  def validateKAFKAConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    kafka_broker = properties
    validationItems = []
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
 
    #Adding Ranger Plugin logic here
    ranger_plugin_properties = getSiteProperties(configurations, "ranger-kafka-plugin-properties")
    ranger_plugin_enabled = ranger_plugin_properties['ranger-kafka-plugin-enabled'] if ranger_plugin_properties else 'No'
    prop_name = 'authorizer.class.name'
    prop_val = "org.apache.ranger.authorization.kafka.authorizer.RangerKafkaAuthorizer"
    if ("RANGER" in servicesList) and (ranger_plugin_enabled.lower() == 'Yes'.lower()):
      if kafka_broker[prop_name] != prop_val:
        validationItems.append({"config-name": prop_name,
                                "item": self.getWarnItem(
                                "If Ranger Kafka Plugin is enabled."\
                                "{0} needs to be set to {1}".format(prop_name,prop_val))})

    if 'KERBEROS' in servicesList and 'security.inter.broker.protocol' in properties:
      interBrokerValue = properties['security.inter.broker.protocol']
      prop_name = 'listeners'
      prop_value =  properties[prop_name]
      if interBrokerValue and interBrokerValue not in prop_value:
        validationItems.append({"config-name": "listeners",
                                "item": self.getWarnItem("If kerberos is enabled "\
                                "{0}  need to contain {1} as one of "\
                                "the protocol".format(prop_name, interBrokerValue))})


    return self.toConfigurationValidationProblems(validationItems, "kafka-broker")

  def isComponentUsingCardinalityForLayout(self, componentName):
    return componentName in ['NFS_GATEWAY', 'PHOENIX_QUERY_SERVER', 'SPARK_THRIFTSERVER']

  def validateRangerAdminConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    ranger_site = properties
    validationItems = []
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if 'RANGER' in servicesList and 'policymgr_external_url' in ranger_site:
      policymgr_mgr_url = ranger_site['policymgr_external_url']
      if policymgr_mgr_url.endswith('/'):
        validationItems.append({'config-name':'policymgr_external_url',
                               'item':self.getWarnItem('Ranger External URL should not contain trailing slash "/"')})
    return self.toConfigurationValidationProblems(validationItems,'admin-properties')

  def validateRangerConfigurationsEnv(self, properties, recommendedDefaults, configurations, services, hosts):
    parentValidationProblems = super(HDP23StackAdvisor, self).validateRangerConfigurationsEnv(properties, recommendedDefaults, configurations, services, hosts)
    ranger_env_properties = properties
    validationItems = []
    security_enabled = self.isSecurityEnabled(services)

    if "ranger-kafka-plugin-enabled" in ranger_env_properties and ranger_env_properties["ranger-kafka-plugin-enabled"].lower() == 'yes' and not security_enabled:
      validationItems.append({"config-name": "ranger-kafka-plugin-enabled",
                              "item": self.getWarnItem(
                                "Ranger Kafka plugin should not be enabled in non-kerberos environment.")})

    validationProblems = self.toConfigurationValidationProblems(validationItems, "ranger-env")
    validationProblems.extend(parentValidationProblems)
    return validationProblems

