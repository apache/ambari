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
import re
import os
import sys
import socket
from math import ceil, floor, log

# Local Imports
from resource_management.libraries.functions.mounted_dirs_helper import get_mounts_with_multiple_data_dirs
from resource_management.libraries.functions.data_structure_utils import get_from_dict
from resource_management.core.logger import Logger
from stack_advisor import DefaultStackAdvisor


class HDP206StackAdvisor(DefaultStackAdvisor):

  def __init__(self):
    super(HDP206StackAdvisor, self).__init__()
    self.initialize_logger("HDP206StackAdvisor")
    Logger.logger = self.logger

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
    self.notValuableComponents |= set(['JOURNALNODE', 'ZKFC', 'GANGLIA_MONITOR'])

  def modifyComponentsNotPreferableOnServer(self):
    """
    Modify the set of components that are not preferable on the server.
    Must be overriden in child class.
    """
    self.notPreferableOnServerComponents |= set(['GANGLIA_SERVER', 'METRICS_COLLECTOR'])

  def modifyComponentLayoutSchemes(self):
    """
    Modify layout scheme dictionaries for components.
    The scheme dictionary basically maps the number of hosts to
    host index where component should exist.
    Must be overriden in child class.
    """
    self.componentLayoutSchemes.update({
      'NAMENODE': {"else": 0},
      'SECONDARY_NAMENODE': {"else": 1},
      'HBASE_MASTER': {6: 0, 31: 2, "else": 3},

      'HISTORYSERVER': {31: 1, "else": 2},
      'RESOURCEMANAGER': {31: 1, "else": 2},

      'OOZIE_SERVER': {6: 1, 31: 2, "else": 3},

      'HIVE_SERVER': {6: 1, 31: 2, "else": 4},
      'HIVE_METASTORE': {6: 1, 31: 2, "else": 4},
      'WEBHCAT_SERVER': {6: 1, 31: 2, "else": 4},
      'METRICS_COLLECTOR': {3: 2, 6: 2, 31: 3, "else": 5},
    })

  def getComponentLayoutValidations(self, services, hosts):
    """Returns array of Validation objects about issues with hostnames components assigned to"""
    items = super(HDP206StackAdvisor, self).getComponentLayoutValidations(services, hosts)

    # Validating NAMENODE and SECONDARY_NAMENODE are on different hosts if possible
    # Use a set for fast lookup
    hostsSet =  set(super(HDP206StackAdvisor, self).getActiveHosts([host["Hosts"] for host in hosts["items"]]))  #[host["Hosts"]["host_name"] for host in hosts["items"]]
    hostsCount = len(hostsSet)

    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item for sublist in componentsListList for item in sublist]
    nameNodeHosts = [component["StackServiceComponents"]["hostnames"] for component in componentsList if component["StackServiceComponents"]["component_name"] == "NAMENODE"]
    secondaryNameNodeHosts = [component["StackServiceComponents"]["hostnames"] for component in componentsList if component["StackServiceComponents"]["component_name"] == "SECONDARY_NAMENODE"]

    # Validating cardinality
    for component in componentsList:
      if component["StackServiceComponents"]["cardinality"] is not None:
         componentName = component["StackServiceComponents"]["component_name"]
         componentDisplayName = component["StackServiceComponents"]["display_name"]
         componentHosts = []
         if component["StackServiceComponents"]["hostnames"] is not None:
           componentHosts = [componentHost for componentHost in component["StackServiceComponents"]["hostnames"] if componentHost in hostsSet]
         componentHostsCount = len(componentHosts)
         cardinality = str(component["StackServiceComponents"]["cardinality"])
         # cardinality types: null, 1+, 1-2, 1, ALL
         message = None
         if "+" in cardinality:
           hostsMin = int(cardinality[:-1])
           if componentHostsCount < hostsMin:
             message = "at least {0} {1} components should be installed in cluster.".format(hostsMin, componentDisplayName)
         elif "-" in cardinality:
           nums = cardinality.split("-")
           hostsMin = int(nums[0])
           hostsMax = int(nums[1])
           if componentHostsCount > hostsMax or componentHostsCount < hostsMin:
             message = "between {0} and {1} {2} components should be installed in cluster.".format(hostsMin, hostsMax, componentDisplayName)
         elif "ALL" == cardinality:
           if componentHostsCount != hostsCount:
             message = "{0} component should be installed on all hosts in cluster.".format(componentDisplayName)
         else:
           if componentHostsCount != int(cardinality):
             message = "exactly {0} {1} components should be installed in cluster.".format(int(cardinality), componentDisplayName)

         if message is not None:
           message = "You have selected {0} {1} components. Please consider that {2}".format(componentHostsCount, componentDisplayName, message)
           items.append({"type": 'host-component', "level": 'ERROR', "message": message, "component-name": componentName})

    # Validating host-usage
    usedHostsListList = [component["StackServiceComponents"]["hostnames"] for component in componentsList if not self.isComponentNotValuable(component)]
    usedHostsList = [item for sublist in usedHostsListList for item in sublist]
    nonUsedHostsList = [item for item in hostsSet if item not in usedHostsList]
    for host in nonUsedHostsList:
      items.append( { "type": 'host-component', "level": 'ERROR', "message": 'Host is not used', "host": str(host) } )

    return items

  def getServiceConfigurationRecommenderDict(self):
    return {
      "YARN": self.recommendYARNConfigurations,
      "MAPREDUCE2": self.recommendMapReduce2Configurations,
      "HDFS": self.recommendHDFSConfigurations,
      "HBASE": self.recommendHbaseConfigurations,
      "STORM": self.recommendStormConfigurations,
      "RANGER": self.recommendRangerConfigurations,
      "ZOOKEEPER": self.recommendZookeeperConfigurations,
      "OOZIE": self.recommendOozieConfigurations
    }

  def recommendOozieConfigurations(self, configurations, clusterData, services, hosts):
    oozie_mount_properties = [
      ("oozie_data_dir", "OOZIE_SERVER", "/hadoop/oozie/data", "single"),
    ]
    self.updateMountProperties("oozie-env", oozie_mount_properties, configurations, services, hosts)

  def recommendZookeeperConfigurations(self, configurations, clusterData, services, hosts):
    zk_mount_properties = [
      ("dataDir", "ZOOKEEPER_SERVER", "/hadoop/zookeeper", "single"),
    ]
    self.updateMountProperties("zoo.cfg", zk_mount_properties, configurations, services, hosts)

  def recommendYARNConfigurations(self, configurations, clusterData, services, hosts):
    putYarnProperty = self.putProperty(configurations, "yarn-site", services)
    putYarnPropertyAttribute = self.putPropertyAttribute(configurations, "yarn-site")
    putYarnEnvProperty = self.putProperty(configurations, "yarn-env", services)
    nodemanagerMinRam = 1048576 # 1TB in mb
    if "referenceNodeManagerHost" in clusterData:
      nodemanagerMinRam = min(clusterData["referenceNodeManagerHost"]["total_mem"]/1024, nodemanagerMinRam)

    callContext = self.getCallContext(services)
    putYarnProperty('yarn.nodemanager.resource.memory-mb', int(round(min(clusterData['containers'] * clusterData['ramPerContainer'], nodemanagerMinRam))))
    # read from the supplied config
    #if 'recommendConfigurations' != callContext and \
    #        "yarn-site" in services["configurations"] and \
    #        "yarn.nodemanager.resource.memory-mb" in services["configurations"]["yarn-site"]["properties"]:
    #    putYarnProperty('yarn.nodemanager.resource.memory-mb', int(services["configurations"]["yarn-site"]["properties"]["yarn.nodemanager.resource.memory-mb"]))
    if 'recommendConfigurations' == callContext:
      putYarnProperty('yarn.nodemanager.resource.memory-mb', int(round(min(clusterData['containers'] * clusterData['ramPerContainer'], nodemanagerMinRam))))
    else:
      # read from the supplied config
      if "yarn-site" in services["configurations"] and "yarn.nodemanager.resource.memory-mb" in services["configurations"]["yarn-site"]["properties"]:
        putYarnProperty('yarn.nodemanager.resource.memory-mb', int(services["configurations"]["yarn-site"]["properties"]["yarn.nodemanager.resource.memory-mb"]))
      else:
        putYarnProperty('yarn.nodemanager.resource.memory-mb', int(round(min(clusterData['containers'] * clusterData['ramPerContainer'], nodemanagerMinRam))))
      pass
    pass

    putYarnProperty('yarn.scheduler.minimum-allocation-mb', int(clusterData['yarnMinContainerSize']))
    putYarnProperty('yarn.scheduler.maximum-allocation-mb', int(configurations["yarn-site"]["properties"]["yarn.nodemanager.resource.memory-mb"]))
    putYarnEnvProperty('min_user_id', self.get_system_min_uid())

    yarn_mount_properties = [
      ("yarn.nodemanager.local-dirs", "NODEMANAGER", "/hadoop/yarn/local", "multi"),
      ("yarn.nodemanager.log-dirs", "NODEMANAGER", "/hadoop/yarn/log", "multi"),
      ("yarn.timeline-service.leveldb-timeline-store.path", "APP_TIMELINE_SERVER", "/hadoop/yarn/timeline", "single"),
      ("yarn.timeline-service.leveldb-state-store.path", "APP_TIMELINE_SERVER", "/hadoop/yarn/timeline", "single")
    ]

    self.updateMountProperties("yarn-site", yarn_mount_properties, configurations, services, hosts)

    sc_queue_name = self.recommendYarnQueue(services, "yarn-env", "service_check.queue.name")
    if sc_queue_name is not None:
      putYarnEnvProperty("service_check.queue.name", sc_queue_name)

    containerExecutorGroup = 'hadoop'
    if 'cluster-env' in services['configurations'] and 'user_group' in services['configurations']['cluster-env']['properties']:
      containerExecutorGroup = services['configurations']['cluster-env']['properties']['user_group']
    putYarnProperty("yarn.nodemanager.linux-container-executor.group", containerExecutorGroup)

    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if "TEZ" in servicesList:
        ambari_user = self.getAmbariUser(services)
        ambariHostName = socket.getfqdn()
        putYarnProperty("yarn.timeline-service.http-authentication.proxyuser.{0}.hosts".format(ambari_user), ambariHostName)
        putYarnProperty("yarn.timeline-service.http-authentication.proxyuser.{0}.groups".format(ambari_user), "*")
        old_ambari_user = self.getOldAmbariUser(services)
        if old_ambari_user is not None:
            putYarnPropertyAttribute("yarn.timeline-service.http-authentication.proxyuser.{0}.hosts".format(old_ambari_user), 'delete', 'true')
            putYarnPropertyAttribute("yarn.timeline-service.http-authentication.proxyuser.{0}.groups".format(old_ambari_user), 'delete', 'true')

  def recommendMapReduce2Configurations(self, configurations, clusterData, services, hosts):
    putMapredProperty = self.putProperty(configurations, "mapred-site", services)
    putMapredProperty('yarn.app.mapreduce.am.resource.mb', int(clusterData['amMemory']))
    putMapredProperty('yarn.app.mapreduce.am.command-opts', "-Xmx" + str(int(round(0.8 * clusterData['amMemory']))) + "m")
    putMapredProperty('mapreduce.map.memory.mb', clusterData['mapMemory'])
    putMapredProperty('mapreduce.reduce.memory.mb', int(clusterData['reduceMemory']))
    putMapredProperty('mapreduce.map.java.opts', "-Xmx" + str(int(round(0.8 * clusterData['mapMemory']))) + "m")
    putMapredProperty('mapreduce.reduce.java.opts', "-Xmx" + str(int(round(0.8 * clusterData['reduceMemory']))) + "m")
    putMapredProperty('mapreduce.task.io.sort.mb', min(int(round(0.4 * clusterData['mapMemory'])), 1024))

    mapred_mounts = [
      ("mapred.local.dir", ["TASKTRACKER", "NODEMANAGER"], "/hadoop/mapred", "multi")
    ]

    self.updateMountProperties("mapred-site", mapred_mounts, configurations, services, hosts)

    mr_queue = self.recommendYarnQueue(services, "mapred-site", "mapreduce.job.queuename")
    if mr_queue is not None:
      putMapredProperty("mapreduce.job.queuename", mr_queue)

  def getAmbariProxyUsersForHDFSValidationItems(self, properties, services):
    validationItems = []
    servicesList = self.get_services_list(services)

    if "HDFS" in servicesList:
      ambari_user = self.getAmbariUser(services)
      props = (
        "hadoop.proxyuser.{0}.hosts".format(ambari_user),
        "hadoop.proxyuser.{0}.groups".format(ambari_user)
      )
      for prop in props:
        validationItems.append({"config-name": prop, "item": self.validatorNotEmpty(properties, prop)})

    return validationItems

  def recommendHDFSConfigurations(self, configurations, clusterData, services, hosts):
    putHDFSProperty = self.putProperty(configurations, "hadoop-env", services)
    putHDFSSiteProperty = self.putProperty(configurations, "hdfs-site", services)
    putHDFSSitePropertyAttributes = self.putPropertyAttribute(configurations, "hdfs-site")
    putHDFSProperty('namenode_heapsize', max(int(clusterData['totalAvailableRam'] / 2), 1024))
    putHDFSProperty = self.putProperty(configurations, "hadoop-env", services)
    putHDFSProperty('namenode_opt_newsize', max(int(clusterData['totalAvailableRam'] / 8), 128))
    putHDFSProperty = self.putProperty(configurations, "hadoop-env", services)
    putHDFSProperty('namenode_opt_maxnewsize', max(int(clusterData['totalAvailableRam'] / 8), 256))

    # Check if NN HA is enabled and recommend removing dfs.namenode.rpc-address
    hdfsSiteProperties = getServicesSiteProperties(services, "hdfs-site")
    nameServices = None
    if hdfsSiteProperties and 'dfs.internal.nameservices' in hdfsSiteProperties:
      nameServices = hdfsSiteProperties['dfs.internal.nameservices']
    if nameServices is None and hdfsSiteProperties and 'dfs.nameservices' in hdfsSiteProperties:
      nameServices = hdfsSiteProperties['dfs.nameservices']
    if nameServices and "dfs.ha.namenodes.%s" % nameServices in hdfsSiteProperties:
      namenodes = hdfsSiteProperties["dfs.ha.namenodes.%s" % nameServices]
      if len(namenodes.split(',')) > 1:
        putHDFSSitePropertyAttributes("dfs.namenode.rpc-address", "delete", "true")

    hdfs_mount_properties = [
      ("dfs.datanode.data.dir", "DATANODE", "/hadoop/hdfs/data", "multi"),
      ("dfs.namenode.name.dir", "DATANODE", "/hadoop/hdfs/namenode", "multi"),
      ("dfs.namenode.checkpoint.dir", "SECONDARY_NAMENODE", "/hadoop/hdfs/namesecondary", "single")
    ]

    self.updateMountProperties("hdfs-site", hdfs_mount_properties, configurations, services, hosts)
    dataDirs = []
    if configurations and "hdfs-site" in configurations and \
            "dfs.datanode.data.dir" in configurations["hdfs-site"]["properties"] and \
                    configurations["hdfs-site"]["properties"]["dfs.datanode.data.dir"] is not None:
      dataDirs = configurations["hdfs-site"]["properties"]["dfs.datanode.data.dir"].split(",")

    elif hdfsSiteProperties and "dfs.datanode.data.dir" in hdfsSiteProperties and \
                    hdfsSiteProperties["dfs.datanode.data.dir"] is not None:
      dataDirs = hdfsSiteProperties["dfs.datanode.data.dir"].split(",")

    # dfs.datanode.du.reserved should be set to 10-15% of volume size
    # For each host selects maximum size of the volume. Then gets minimum for all hosts.
    # This ensures that each host will have at least one data dir with available space.
    reservedSizeRecommendation = 0l #kBytes
    for host in hosts["items"]:
      mountPoints = []
      mountPointDiskAvailableSpace = [] #kBytes
      for diskInfo in host["Hosts"]["disk_info"]:
        mountPoints.append(diskInfo["mountpoint"])
        mountPointDiskAvailableSpace.append(long(diskInfo["size"]))

      maxFreeVolumeSizeForHost = 0l #kBytes
      for dataDir in dataDirs:
        mp = self.getMountPointForDir(dataDir, mountPoints)
        for i in range(len(mountPoints)):
          if mp == mountPoints[i]:
            if mountPointDiskAvailableSpace[i] > maxFreeVolumeSizeForHost:
              maxFreeVolumeSizeForHost = mountPointDiskAvailableSpace[i]

      if not reservedSizeRecommendation or maxFreeVolumeSizeForHost and maxFreeVolumeSizeForHost < reservedSizeRecommendation:
        reservedSizeRecommendation = maxFreeVolumeSizeForHost

    if reservedSizeRecommendation:
      reservedSizeRecommendation = max(reservedSizeRecommendation * 1024 / 8, 1073741824) # At least 1Gb is reserved
      putHDFSSiteProperty('dfs.datanode.du.reserved', reservedSizeRecommendation) #Bytes

    # recommendations for "hadoop.proxyuser.*.hosts", "hadoop.proxyuser.*.groups" properties in core-site
    self.recommendHadoopProxyUsers(configurations, services, hosts)

  def getLZOSupportValidationItems(self, properties, services):
    '''
    Checks GPL license is accepted when GPL software is used.
    :param properties: dict of properties' name and value pairs
    :param services: list of services
    :return: NOT_APPLICABLE messages in case GPL license is not accepted
    '''
    services_list = self.get_services_list(services)

    validations = []
    if "HDFS" in services_list:
      lzo_allowed = services["gpl-license-accepted"]

      self.validatePropertyToLZOCodec("io.compression.codecs", properties, lzo_allowed, validations)
      self.validatePropertyToLZOCodec("io.compression.codec.lzo.class", properties, lzo_allowed, validations)
    return validations

  def validatePropertyToLZOCodec(self, property_name, properties, lzo_allowed, validations):
    '''
    Checks specified property contains LZO codec class and requires GPL license acceptance.
    :param property_name: property name
    :param properties: dict of properties' name and value pairs
    :param lzo_allowed: is gpl license accepted
    :param validations: list with validation failures
    '''
    lzo_codec_class = "com.hadoop.compression.lzo.LzoCodec"
    if property_name in properties:
      property_value = properties.get(property_name)
      if not lzo_allowed and lzo_codec_class in property_value:
        validations.append({"config-name": property_name, "item": self.getNotApplicableItem(
          "Your Ambari Server has not been configured to download LZO and install it. "
          "LZO is GPL software and requires you to explicitly enable Ambari to install and download LZO. "
          "Please refer to the documentation to configure Ambari before proceeding.")})

  def recommendHbaseConfigurations(self, configurations, clusterData, services, hosts):
    # recommendations for HBase env config

    # If cluster size is < 100, hbase master heap = 2G
    # else If cluster size is < 500, hbase master heap = 4G
    # else hbase master heap = 8G
    # for small test clusters use 1 gb
    hostsCount = 0
    if hosts and "items" in hosts:
      hostsCount = len(hosts["items"])

    hbaseMasterRam = {
      hostsCount < 20: 1,
      20 <= hostsCount < 100: 2,
      100 <= hostsCount < 500: 4,
      500 <= hostsCount: 8
    }[True]

    putHbaseProperty = self.putProperty(configurations, "hbase-env", services)
    putHbaseProperty('hbase_regionserver_heapsize', int(clusterData['hbaseRam']) * 1024)
    putHbaseProperty('hbase_master_heapsize', hbaseMasterRam * 1024)

    # recommendations for HBase site config
    putHbaseSiteProperty = self.putProperty(configurations, "hbase-site", services)

    if 'hbase-site' in services['configurations'] and 'hbase.superuser' in services['configurations']['hbase-site']['properties'] \
      and 'hbase-env' in services['configurations'] and 'hbase_user' in services['configurations']['hbase-env']['properties'] \
      and services['configurations']['hbase-env']['properties']['hbase_user'] != services['configurations']['hbase-site']['properties']['hbase.superuser']:
      putHbaseSiteProperty("hbase.superuser", services['configurations']['hbase-env']['properties']['hbase_user'])


  def recommendRangerConfigurations(self, configurations, clusterData, services, hosts):

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



  def recommendStormConfigurations(self, configurations, clusterData, services, hosts):
    putStormSiteProperty = self.putProperty(configurations, "storm-site", services)
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    # Storm AMS integration
    if 'AMBARI_METRICS' in servicesList:
      putStormSiteProperty('metrics.reporter.register', 'org.apache.hadoop.metrics2.sink.storm.StormTimelineMetricsReporter')


  def getServiceConfigurationValidators(self):
    return {
      "HDFS": { "hdfs-site": self.validateHDFSConfigurations,
                "hadoop-env": self.validateHDFSConfigurationsEnv,
                "core-site": self.validateHDFSConfigurationsCoreSite},
      "MAPREDUCE2": {"mapred-site": self.validateMapReduce2Configurations},
      "YARN": {"yarn-site": self.validateYARNConfigurations,
               "yarn-env": self.validateYARNEnvConfigurations},
      "HBASE": {"hbase-env": self.validateHbaseEnvConfigurations},
      "STORM": {"storm-site": self.validateStormConfigurations}
    }

  def validateMinMax(self, items, recommendedDefaults, configurations):

    # required for casting to the proper numeric type before comparison
    def convertToNumber(number):
      try:
        return int(number)
      except ValueError:
        return float(number)

    for configName in configurations:
      validationItems = []
      if configName in recommendedDefaults and "property_attributes" in recommendedDefaults[configName]:
        for propertyName in recommendedDefaults[configName]["property_attributes"]:
          if propertyName in configurations[configName]["properties"]:
            if "maximum" in recommendedDefaults[configName]["property_attributes"][propertyName] and \
                propertyName in recommendedDefaults[configName]["properties"]:
              userValue = convertToNumber(configurations[configName]["properties"][propertyName])
              maxValue = convertToNumber(recommendedDefaults[configName]["property_attributes"][propertyName]["maximum"])
              if userValue > maxValue:
                validationItems.extend([{"config-name": propertyName, "item": self.getWarnItem("Value is greater than the recommended maximum of {0} ".format(maxValue))}])
            if "minimum" in recommendedDefaults[configName]["property_attributes"][propertyName] and \
                    propertyName in recommendedDefaults[configName]["properties"]:
              userValue = convertToNumber(configurations[configName]["properties"][propertyName])
              minValue = convertToNumber(recommendedDefaults[configName]["property_attributes"][propertyName]["minimum"])
              if userValue < minValue:
                validationItems.extend([{"config-name": propertyName, "item": self.getWarnItem("Value is less than the recommended minimum of {0} ".format(minValue))}])
      items.extend(self.toConfigurationValidationProblems(validationItems, configName))
    pass



  def validateStormConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = []
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    # Storm AMS integration
    if 'AMBARI_METRICS' in servicesList and "metrics.reporter.register" in properties and \
      "org.apache.hadoop.metrics2.sink.storm.StormTimelineMetricsReporter" not in properties.get("metrics.reporter.register"):

      validationItems.append({"config-name": 'metrics.reporter.register',
                              "item": self.getWarnItem(
                                "Should be set to org.apache.hadoop.metrics2.sink.storm.StormTimelineMetricsReporter to report the metrics to Ambari Metrics service.")})

    return self.toConfigurationValidationProblems(validationItems, "storm-site")





  def getMemorySizeRequired(self, services, components, configurations):
    totalMemoryRequired = 512*1024*1024 # 512Mb for OS needs
    # Dictionary from component name to list of dictionary with keys: config-name, property, default.
    heapSizeProperties = self.get_heap_size_properties(services)
    for component in components:
      if component in heapSizeProperties.keys():
        heapSizePropertiesForComp = heapSizeProperties[component]
        for heapSizeProperty in heapSizePropertiesForComp:
          try:
            properties = configurations[heapSizeProperty["config-name"]]["properties"]
            heapsize = properties[heapSizeProperty["property"]]
          except KeyError:
            heapsize = heapSizeProperty["default"]

          # Assume Mb if no modifier
          if len(heapsize) > 1 and heapsize[-1] in '0123456789':
            heapsize = str(heapsize) + "m"

          totalMemoryRequired += self.formatXmxSizeToBytes(heapsize)
      else:
        if component == "METRICS_MONITOR" or "CLIENT" in component:
          heapsize = '512m'
        else:
          heapsize = '1024m'
        totalMemoryRequired += self.formatXmxSizeToBytes(heapsize)
    return totalMemoryRequired

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

    yarn_site = getServicesSiteProperties(services, "yarn-site")
    yarn_site_properties = getSiteProperties(configurations, "yarn-site")

    # Check if services["changed-configurations"] is empty and 'yarn.nodemanager.resource.memory-mb' is modified in current ST invocation.
    if not ("changed-configurations" in services and services["changed-configurations"]) and yarn_site_properties and 'yarn.nodemanager.resource.memory-mb' in yarn_site_properties:
      yarn_nm_mem_in_mb = float(yarn_site_properties['yarn.nodemanager.resource.memory-mb'])
    elif yarn_site and 'yarn.nodemanager.resource.memory-mb' in yarn_site:
      # Check if 'yarn.nodemanager.resource.memory-mb' is input in services array.
      yarn_nm_mem_in_mb = float(yarn_site['yarn.nodemanager.resource.memory-mb'])

    if yarn_nm_mem_in_mb <= 0.0:
      self.logger.warning("'yarn.nodemanager.resource.memory-mb' current value : {0}. Expected value : > 0".format(yarn_nm_mem_in_mb))

    return yarn_nm_mem_in_mb

  def getPreferredMountPoints(self, hostInfo):

    # '/etc/resolv.conf', '/etc/hostname', '/etc/hosts' are docker specific mount points
    undesirableMountPoints = ["/", "/home", "/etc/resolv.conf", "/etc/hosts",
                              "/etc/hostname", "/tmp"]
    undesirableFsTypes = ["devtmpfs", "tmpfs", "vboxsf", "CDFS"]
    mountPoints = []
    if hostInfo and "disk_info" in hostInfo:
      mountPointsDict = {}
      for mountpoint in hostInfo["disk_info"]:
        if not (mountpoint["mountpoint"] in undesirableMountPoints or
                mountpoint["mountpoint"].startswith(("/boot", "/mnt")) or
                mountpoint["type"] in undesirableFsTypes or
                mountpoint["available"] == str(0)):
          mountPointsDict[mountpoint["mountpoint"]] = self.to_number(mountpoint["available"])
      if mountPointsDict:
        mountPoints = sorted(mountPointsDict, key=mountPointsDict.get, reverse=True)
    mountPoints.append("/")
    return mountPoints

  def validateMapReduce2Configurations(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = [ {"config-name": 'mapreduce.map.java.opts', "item": self.validateXmxValue(properties, recommendedDefaults, 'mapreduce.map.java.opts')},
                        {"config-name": 'mapreduce.reduce.java.opts', "item": self.validateXmxValue(properties, recommendedDefaults, 'mapreduce.reduce.java.opts')},
                        {"config-name": 'mapreduce.task.io.sort.mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'mapreduce.task.io.sort.mb')},
                        {"config-name": 'mapreduce.map.memory.mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'mapreduce.map.memory.mb')},
                        {"config-name": 'mapreduce.reduce.memory.mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'mapreduce.reduce.memory.mb')},
                        {"config-name": 'yarn.app.mapreduce.am.resource.mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'yarn.app.mapreduce.am.resource.mb')},
                        {"config-name": 'yarn.app.mapreduce.am.command-opts', "item": self.validateXmxValue(properties, recommendedDefaults, 'yarn.app.mapreduce.am.command-opts')},
                        {"config-name": 'mapreduce.job.queuename', "item": self.validatorYarnQueue(properties, recommendedDefaults, 'mapreduce.job.queuename', services)} ]
    return self.toConfigurationValidationProblems(validationItems, "mapred-site")

  def validateYARNConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    clusterEnv = getSiteProperties(configurations, "cluster-env")

    validationItems = [ {"config-name": 'yarn.nodemanager.resource.memory-mb', "item": self.validatorGreaterThenDefaultValue(properties, recommendedDefaults, 'yarn.nodemanager.resource.memory-mb')},
                        {"config-name": 'yarn.scheduler.minimum-allocation-mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'yarn.scheduler.minimum-allocation-mb')},
                        {"config-name": 'yarn.nodemanager.linux-container-executor.group', "item": self.validatorEqualsPropertyItem(properties, "yarn.nodemanager.linux-container-executor.group", clusterEnv, "user_group")},
                        {"config-name": 'yarn.scheduler.maximum-allocation-mb', "item": self.validatorGreaterThenDefaultValue(properties, recommendedDefaults, 'yarn.scheduler.maximum-allocation-mb')} ]
    nmMemory = int(self.get_yarn_nm_mem_in_mb(services, configurations))
    if "items" in hosts and len(hosts["items"]) > 0:
      nodeManagerHosts = self.getHostsWithComponent("YARN", "NODEMANAGER", services, hosts)
      nmLowMemoryHosts = []
      # NodeManager host with least memory is generally used in calculations as it will work in larger hosts.
      if nodeManagerHosts is not None and len(nodeManagerHosts) > 0:
        for nmHost in nodeManagerHosts:
          nmHostName = nmHost["Hosts"]["host_name"]
          componentNames = []
          for service in services["services"]:
            for component in service["components"]:
              if not self.isClientComponent(component) and component["StackServiceComponents"]["hostnames"] is not None:
                if nmHostName in component["StackServiceComponents"]["hostnames"]:
                  componentNames.append(component["StackServiceComponents"]["component_name"])
          requiredMemory = self.getMemorySizeRequired(services, componentNames, configurations)
          unusedMemory = int((nmHost["Hosts"]["total_mem"] * 1024 - requiredMemory)/ (1024 * 1024)) # in MB
          if nmMemory > unusedMemory:
            nmLowMemoryHosts.append(nmHostName)

        if len(nmLowMemoryHosts) > 0:
          validationItems.append({"config-name": "yarn.nodemanager.resource.memory-mb",
            "item": self.getWarnItem(
                "Node manager hosts with high memory usage found (examples : {0}). Consider reducing the allocated "
                "memory for containers or moving other co-located components "
                "to a different host.".format(",".join(nmLowMemoryHosts[:3])))})

    return self.toConfigurationValidationProblems(validationItems, "yarn-site")

  def validateYARNEnvConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = [{"config-name": 'service_check.queue.name', "item": self.validatorYarnQueue(properties, recommendedDefaults, 'service_check.queue.name', services)} ]
    return self.toConfigurationValidationProblems(validationItems, "yarn-env")

  def validateHbaseEnvConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    hbase_site = getSiteProperties(configurations, "hbase-site")
    validationItems = [ {"config-name": 'hbase_regionserver_heapsize', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'hbase_regionserver_heapsize')},
                        {"config-name": 'hbase_master_heapsize', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'hbase_master_heapsize')},
                        {"config-name": "hbase_user", "item": self.validatorEqualsPropertyItem(properties, "hbase_user", hbase_site, "hbase.superuser")} ]
    return self.toConfigurationValidationProblems(validationItems, "hbase-env")

  def validateHDFSConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    clusterEnv = getSiteProperties(configurations, "cluster-env")
    validationItems = [{"config-name": 'dfs.datanode.du.reserved', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'dfs.datanode.du.reserved')},
                       {"config-name": 'dfs.datanode.data.dir', "item": self.validatorOneDataDirPerPartition(properties, 'dfs.datanode.data.dir', services, hosts, clusterEnv)}]
    return self.toConfigurationValidationProblems(validationItems, "hdfs-site")

  def validateHDFSConfigurationsEnv(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = [ {"config-name": 'namenode_heapsize', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'namenode_heapsize')},
                        {"config-name": 'namenode_opt_newsize', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'namenode_opt_newsize')},
                        {"config-name": 'namenode_opt_maxnewsize', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'namenode_opt_maxnewsize')}]
    return self.toConfigurationValidationProblems(validationItems, "hadoop-env")

  def validateHDFSConfigurationsCoreSite(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = []
    validationItems.extend(self.getHadoopProxyUsersValidationItems(properties, services, hosts, configurations))
    validationItems.extend(self.getAmbariProxyUsersForHDFSValidationItems(properties, services))
    validationItems.extend(self.getLZOSupportValidationItems(properties, services))
    return self.toConfigurationValidationProblems(validationItems, "core-site")

  def validatorOneDataDirPerPartition(self, properties, propertyName, services, hosts, clusterEnv):
    if not propertyName in properties:
      return self.getErrorItem("Value should be set")
    dirs = properties[propertyName]

    if not (clusterEnv and "one_dir_per_partition" in clusterEnv and clusterEnv["one_dir_per_partition"].lower() == "true"):
      return None

    dataNodeHosts = self.getDataNodeHosts(services, hosts)

    warnings = set()
    for host in dataNodeHosts:
      hostName = host["Hosts"]["host_name"]

      mountPoints = []
      for diskInfo in host["Hosts"]["disk_info"]:
        mountPoints.append(diskInfo["mountpoint"])

      if get_mounts_with_multiple_data_dirs(mountPoints, dirs):
        # A detailed message can be too long on large clusters:
        # warnings.append("Host: " + hostName + "; Mount: " + mountPoint + "; Data directories: " + ", ".join(dirList))
        warnings.add(hostName)
        break;

    if len(warnings) > 0:
      return self.getWarnItem("cluster-env/one_dir_per_partition is enabled but there are multiple data directories on the same mount. Affected hosts: {0}".format(", ".join(sorted(warnings))))

    return None

  """
  Returns the list of Data Node hosts.
  """
  def getDataNodeHosts(self, services, hosts):
    if len(hosts["items"]) > 0:
      dataNodeHosts = self.getHostsWithComponent("HDFS", "DATANODE", services, hosts)
      if dataNodeHosts is not None:
        return dataNodeHosts
    return []

  def mergeValidators(self, parentValidators, childValidators):
    for service, configsDict in childValidators.iteritems():
      if service not in parentValidators:
        parentValidators[service] = {}
      parentValidators[service].update(configsDict)


  def get_service_component_meta(self, service, component, services):
    """
    Function retrieve service component meta information as dict from services.json
    If no service or component found, would be returned empty dict

    Return value example:
        "advertise_version" : true,
        "bulk_commands_display_name" : "",
        "bulk_commands_master_component_name" : "",
        "cardinality" : "1+",
        "component_category" : "CLIENT",
        "component_name" : "HBASE_CLIENT",
        "custom_commands" : [ ],
        "decommission_allowed" : false,
        "display_name" : "HBase Client",
        "has_bulk_commands_definition" : false,
        "is_client" : true,
        "is_master" : false,
        "reassign_allowed" : false,
        "recovery_enabled" : false,
        "service_name" : "HBASE",
        "stack_name" : "HDP",
        "stack_version" : "2.5",
        "hostnames" : [ "host1", "host2" ]

    :type service str
    :type component str
    :type services dict
    :rtype dict
    """
    __stack_services = "StackServices"
    __stack_service_components = "StackServiceComponents"

    if not services:
      return {}

    service_meta = [item for item in services["services"] if item[__stack_services]["service_name"] == service]
    if len(service_meta) == 0:
      return {}

    service_meta = service_meta[0]
    component_meta = [item for item in service_meta["components"] if item[__stack_service_components]["component_name"] == component]

    if len(component_meta) == 0:
      return {}

    return component_meta[0][__stack_service_components]


  def get_components_list(self, service, services):
    """
    Return list of components for specific service
    :type service str
    :type services dict
    :rtype list
    """
    __stack_services = "StackServices"
    __stack_service_components = "StackServiceComponents"

    if not services:
      return []

    service_meta = [item for item in services["services"] if item[__stack_services]["service_name"] == service]
    if len(service_meta) == 0:
      return []

    service_meta = service_meta[0]
    return [item[__stack_service_components]["component_name"] for item in service_meta["components"]]

def getUserOperationContext(services, contextName):
  if services:
    if 'user-context' in services.keys():
      userContext = services["user-context"]
      if contextName in userContext:
        return userContext[contextName]
  return None

# if serviceName is being added
def isServiceBeingAdded(services, serviceName):
  if services:
    if 'user-context' in services.keys():
      userContext = services["user-context"]
      if DefaultStackAdvisor.OPERATION in userContext and \
              'AddService' == userContext[DefaultStackAdvisor.OPERATION] and \
              DefaultStackAdvisor.OPERATION_DETAILS in userContext:
        if -1 != userContext["operation_details"].find(serviceName):
          return True
  return False

# Validation helper methods
def getSiteProperties(configurations, siteName):
  siteConfig = configurations.get(siteName)
  if siteConfig is None:
    return None
  return siteConfig.get("properties")

def getServicesSiteProperties(services, siteName):
  configurations = services.get("configurations")
  if not configurations:
    return None
  siteConfig = configurations.get(siteName)
  if siteConfig is None:
    return None
  return siteConfig.get("properties")



def round_to_n(mem_size, n=128):
  return int(round(mem_size / float(n))) * int(n)
