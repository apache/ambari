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
from resource_management.core.logger import Logger
from resource_management.libraries.functions.mounted_dirs_helper import get_mounts_with_multiple_data_dirs
from resource_management.libraries.functions.data_structure_utils import get_from_dict
from stack_advisor import DefaultStackAdvisor


class HDP206StackAdvisor(DefaultStackAdvisor):

  def __init__(self):
    super(HDP206StackAdvisor, self).__init__()
    Logger.initialize_logger()

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
      "AMBARI_METRICS": self.recommendAmsConfigurations,
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

    callContext = getCallContext(services)
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

  def getAmbariUser(self, services):
    ambari_user = services['ambari-server-properties']['ambari-server.user']
    if "cluster-env" in services["configurations"] \
          and "ambari_principal_name" in services["configurations"]["cluster-env"]["properties"] \
                and "security_enabled" in services["configurations"]["cluster-env"]["properties"] \
                    and services["configurations"]["cluster-env"]["properties"]["security_enabled"].lower() == "true":
      ambari_user = services["configurations"]["cluster-env"]["properties"]["ambari_principal_name"]
      ambari_user = ambari_user.split('@')[0]
    return ambari_user

  def getOldAmbariUser(self, services):
    ambari_user = None
    if "cluster-env" in services["configurations"]:
      if "security_enabled" in services["configurations"]["cluster-env"]["properties"] \
              and services["configurations"]["cluster-env"]["properties"]["security_enabled"].lower() == "true":
         ambari_user = services['ambari-server-properties']['ambari-server.user']
      elif "ambari_principal_name" in services["configurations"]["cluster-env"]["properties"]:
         ambari_user = services["configurations"]["cluster-env"]["properties"]["ambari_principal_name"]
         ambari_user = ambari_user.split('@')[0]
    return ambari_user

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


  def getAmsMemoryRecommendation(self, services, hosts):
    # MB per sink in hbase heapsize
    HEAP_PER_MASTER_COMPONENT = 50
    HEAP_PER_SLAVE_COMPONENT = 10

    schMemoryMap = {
      "HDFS": {
        "NAMENODE": HEAP_PER_MASTER_COMPONENT,
        "DATANODE": HEAP_PER_SLAVE_COMPONENT
      },
      "YARN": {
        "RESOURCEMANAGER": HEAP_PER_MASTER_COMPONENT,
      },
      "HBASE": {
        "HBASE_MASTER": HEAP_PER_MASTER_COMPONENT,
        "HBASE_REGIONSERVER": HEAP_PER_SLAVE_COMPONENT
      },
      "ACCUMULO": {
        "ACCUMULO_MASTER": HEAP_PER_MASTER_COMPONENT,
        "ACCUMULO_TSERVER": HEAP_PER_SLAVE_COMPONENT
      },
      "KAFKA": {
        "KAFKA_BROKER": HEAP_PER_MASTER_COMPONENT
      },
      "FLUME": {
        "FLUME_HANDLER": HEAP_PER_SLAVE_COMPONENT
      },
      "STORM": {
        "NIMBUS": HEAP_PER_MASTER_COMPONENT,
      },
      "AMBARI_METRICS": {
        "METRICS_COLLECTOR": HEAP_PER_MASTER_COMPONENT,
        "METRICS_MONITOR": HEAP_PER_SLAVE_COMPONENT
      }
    }
    total_sinks_count = 0
    # minimum heap size
    hbase_heapsize = 500
    for serviceName, componentsDict in schMemoryMap.items():
      for componentName, multiplier in componentsDict.items():
        schCount = len(
          self.getHostsWithComponent(serviceName, componentName, services,
                                     hosts))
        hbase_heapsize += int((schCount * multiplier) ** 0.9)
        total_sinks_count += schCount
    collector_heapsize = int(hbase_heapsize/4 if hbase_heapsize > 2048 else 512)

    return round_to_n(collector_heapsize), round_to_n(hbase_heapsize), total_sinks_count

  def recommendStormConfigurations(self, configurations, clusterData, services, hosts):
    putStormSiteProperty = self.putProperty(configurations, "storm-site", services)
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    # Storm AMS integration
    if 'AMBARI_METRICS' in servicesList:
      putStormSiteProperty('metrics.reporter.register', 'org.apache.hadoop.metrics2.sink.storm.StormTimelineMetricsReporter')

  def recommendAmsConfigurations(self, configurations, clusterData, services, hosts):
    putAmsEnvProperty = self.putProperty(configurations, "ams-env", services)
    putAmsHbaseSiteProperty = self.putProperty(configurations, "ams-hbase-site", services)
    putAmsSiteProperty = self.putProperty(configurations, "ams-site", services)
    putHbaseEnvProperty = self.putProperty(configurations, "ams-hbase-env", services)
    putGrafanaProperty = self.putProperty(configurations, "ams-grafana-env", services)
    putGrafanaPropertyAttribute = self.putPropertyAttribute(configurations, "ams-grafana-env")

    amsCollectorHosts = self.getComponentHostNames(services, "AMBARI_METRICS", "METRICS_COLLECTOR")

    # TODO set "timeline.metrics.service.webapp.address" to 0.0.0.0:port in upgrade catalog
    timeline_metrics_service_webapp_address = '0.0.0.0'

    putAmsSiteProperty("timeline.metrics.service.webapp.address", str(timeline_metrics_service_webapp_address) + ":6188")

    log_dir = "/var/log/ambari-metrics-collector"
    if "ams-env" in services["configurations"]:
      if "metrics_collector_log_dir" in services["configurations"]["ams-env"]["properties"]:
        log_dir = services["configurations"]["ams-env"]["properties"]["metrics_collector_log_dir"]
      putHbaseEnvProperty("hbase_log_dir", log_dir)

    defaultFs = 'file:///'
    if "core-site" in services["configurations"] and \
      "fs.defaultFS" in services["configurations"]["core-site"]["properties"]:
      defaultFs = services["configurations"]["core-site"]["properties"]["fs.defaultFS"]

    operatingMode = "embedded"
    if "ams-site" in services["configurations"]:
      if "timeline.metrics.service.operation.mode" in services["configurations"]["ams-site"]["properties"]:
        operatingMode = services["configurations"]["ams-site"]["properties"]["timeline.metrics.service.operation.mode"]

    if len(amsCollectorHosts) > 1 :
      operatingMode = "distributed"
      putAmsSiteProperty("timeline.metrics.service.operation.mode", operatingMode)

    if operatingMode == "distributed":
      putAmsSiteProperty("timeline.metrics.service.watcher.disabled", 'true')
      putAmsHbaseSiteProperty("hbase.cluster.distributed", 'true')
    else:
      putAmsSiteProperty("timeline.metrics.service.watcher.disabled", 'false')
      putAmsHbaseSiteProperty("hbase.cluster.distributed", 'false')

    rootDir = "file:///var/lib/ambari-metrics-collector/hbase"
    tmpDir = "/var/lib/ambari-metrics-collector/hbase-tmp"
    zk_port_default = []
    if "ams-hbase-site" in services["configurations"]:
      if "hbase.rootdir" in services["configurations"]["ams-hbase-site"]["properties"]:
        rootDir = services["configurations"]["ams-hbase-site"]["properties"]["hbase.rootdir"]
      if "hbase.tmp.dir" in services["configurations"]["ams-hbase-site"]["properties"]:
        tmpDir = services["configurations"]["ams-hbase-site"]["properties"]["hbase.tmp.dir"]
      if "hbase.zookeeper.property.clientPort" in services["configurations"]["ams-hbase-site"]["properties"]:
        zk_port_default = services["configurations"]["ams-hbase-site"]["properties"]["hbase.zookeeper.property.clientPort"]

      # Skip recommendation item if default value is present
    if operatingMode == "distributed" and not "{{zookeeper_clientPort}}" in zk_port_default:
      zkPort = self.getZKPort(services)
      putAmsHbaseSiteProperty("hbase.zookeeper.property.clientPort", zkPort)
    elif operatingMode == "embedded" and not "{{zookeeper_clientPort}}" in zk_port_default:
      putAmsHbaseSiteProperty("hbase.zookeeper.property.clientPort", "61181")

    mountpoints = ["/"]
    for collectorHostName in amsCollectorHosts:
      for host in hosts["items"]:
        if host["Hosts"]["host_name"] == collectorHostName:
          mountpoints = self.getPreferredMountPoints(host["Hosts"])
          break
    isLocalRootDir = rootDir.startswith("file://") or (defaultFs.startswith("file://") and rootDir.startswith("/"))
    if isLocalRootDir:
      rootDir = re.sub("^file:///|/", "", rootDir, count=1)
      rootDir = "file://" + os.path.join(mountpoints[0], rootDir)
    tmpDir = re.sub("^file:///|/", "", tmpDir, count=1)
    if len(mountpoints) > 1 and isLocalRootDir:
      tmpDir = os.path.join(mountpoints[1], tmpDir)
    else:
      tmpDir = os.path.join(mountpoints[0], tmpDir)
    putAmsHbaseSiteProperty("hbase.tmp.dir", tmpDir)

    if operatingMode == "distributed":
      putAmsHbaseSiteProperty("hbase.rootdir", "/user/ams/hbase")

    if operatingMode == "embedded":
      if isLocalRootDir:
        putAmsHbaseSiteProperty("hbase.rootdir", rootDir)
      else:
        putAmsHbaseSiteProperty("hbase.rootdir", "file:///var/lib/ambari-metrics-collector/hbase")

    collector_heapsize, hbase_heapsize, total_sinks_count = self.getAmsMemoryRecommendation(services, hosts)

    putAmsEnvProperty("metrics_collector_heapsize", collector_heapsize)

    putAmsSiteProperty("timeline.metrics.cache.size", max(100, int(log(total_sinks_count)) * 100))
    putAmsSiteProperty("timeline.metrics.cache.commit.interval", min(10, max(12 - int(log(total_sinks_count)), 2)))

    # blockCache = 0.3, memstore = 0.35, phoenix-server = 0.15, phoenix-client = 0.25
    putAmsHbaseSiteProperty("hfile.block.cache.size", 0.3)
    putAmsHbaseSiteProperty("hbase.hregion.memstore.flush.size", 134217728)
    putAmsHbaseSiteProperty("hbase.regionserver.global.memstore.upperLimit", 0.35)
    putAmsHbaseSiteProperty("hbase.regionserver.global.memstore.lowerLimit", 0.3)

    if len(amsCollectorHosts) > 1:
      pass
    else:
      # blockCache = 0.3, memstore = 0.3, phoenix-server = 0.2, phoenix-client = 0.3
      if total_sinks_count >= 2000:
        putAmsHbaseSiteProperty("hbase.regionserver.handler.count", 60)
        putAmsHbaseSiteProperty("hbase.regionserver.hlog.blocksize", 134217728)
        putAmsHbaseSiteProperty("hbase.regionserver.maxlogs", 64)
        putAmsHbaseSiteProperty("hbase.hregion.memstore.flush.size", 268435456)
        putAmsHbaseSiteProperty("hbase.regionserver.global.memstore.upperLimit", 0.3)
        putAmsHbaseSiteProperty("hbase.regionserver.global.memstore.lowerLimit", 0.25)
        putAmsHbaseSiteProperty("phoenix.query.maxGlobalMemoryPercentage", 20)
        putAmsHbaseSiteProperty("phoenix.coprocessor.maxMetaDataCacheSize", 81920000)
        putAmsSiteProperty("phoenix.query.maxGlobalMemoryPercentage", 30)
        putAmsSiteProperty("timeline.metrics.service.resultset.fetchSize", 10000)
      elif total_sinks_count >= 500:
        putAmsHbaseSiteProperty("hbase.regionserver.handler.count", 60)
        putAmsHbaseSiteProperty("hbase.regionserver.hlog.blocksize", 134217728)
        putAmsHbaseSiteProperty("hbase.regionserver.maxlogs", 64)
        putAmsHbaseSiteProperty("hbase.hregion.memstore.flush.size", 268435456)
        putAmsHbaseSiteProperty("phoenix.coprocessor.maxMetaDataCacheSize", 40960000)
        putAmsSiteProperty("timeline.metrics.service.resultset.fetchSize", 5000)
      else:
        putAmsHbaseSiteProperty("phoenix.coprocessor.maxMetaDataCacheSize", 20480000)
      pass

    metrics_api_handlers = min(50, max(20, int(total_sinks_count / 100)))
    putAmsSiteProperty("timeline.metrics.service.handler.thread.count", metrics_api_handlers)

    # Distributed mode heap size
    if operatingMode == "distributed":
      hbase_heapsize = max(hbase_heapsize, 768)
      putHbaseEnvProperty("hbase_master_heapsize", "512")
      putHbaseEnvProperty("hbase_master_xmn_size", "102") #20% of 512 heap size
      putHbaseEnvProperty("hbase_regionserver_heapsize", hbase_heapsize)
      putHbaseEnvProperty("regionserver_xmn_size", round_to_n(0.15*hbase_heapsize,64))
    else:
      # Embedded mode heap size : master + regionserver
      hbase_rs_heapsize = 768
      putHbaseEnvProperty("hbase_regionserver_heapsize", hbase_rs_heapsize)
      putHbaseEnvProperty("hbase_master_heapsize", hbase_heapsize)
      putHbaseEnvProperty("hbase_master_xmn_size", round_to_n(0.15*(hbase_heapsize+hbase_rs_heapsize),64))

    # If no local DN in distributed mode
    if operatingMode == "distributed":
      dn_hosts = self.getComponentHostNames(services, "HDFS", "DATANODE")
      # call by Kerberos wizard sends only the service being affected
      # so it is possible for dn_hosts to be None but not amsCollectorHosts
      if dn_hosts and len(dn_hosts) > 0:
        if set(amsCollectorHosts).intersection(dn_hosts):
          collector_cohosted_with_dn = "true"
        else:
          collector_cohosted_with_dn = "false"
        putAmsHbaseSiteProperty("dfs.client.read.shortcircuit", collector_cohosted_with_dn)

    #split points
    scriptDir = os.path.dirname(os.path.abspath(__file__))
    metricsDir = os.path.join(scriptDir, '../../../../common-services/AMBARI_METRICS/0.1.0/package')
    serviceMetricsDir = os.path.join(metricsDir, 'files', 'service-metrics')
    customServiceMetricsDir = os.path.join(scriptDir, '../../../../dashboards/service-metrics')
    sys.path.append(os.path.join(metricsDir, 'scripts'))
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

    from split_points import FindSplitPointsForAMSRegions

    ams_hbase_site = None
    ams_hbase_env = None

    # Overriden properties form the UI
    if "ams-hbase-site" in services["configurations"]:
      ams_hbase_site = services["configurations"]["ams-hbase-site"]["properties"]
    if "ams-hbase-env" in services["configurations"]:
       ams_hbase_env = services["configurations"]["ams-hbase-env"]["properties"]

    # Recommendations
    if not ams_hbase_site:
      ams_hbase_site = configurations["ams-hbase-site"]["properties"]
    if not ams_hbase_env:
      ams_hbase_env = configurations["ams-hbase-env"]["properties"]

    split_point_finder = FindSplitPointsForAMSRegions(
      ams_hbase_site, ams_hbase_env, serviceMetricsDir, customServiceMetricsDir, operatingMode, servicesList)

    result = split_point_finder.get_split_points()
    precision_splits = ' '
    aggregate_splits = ' '
    if result.precision:
      precision_splits = result.precision
    if result.aggregate:
      aggregate_splits = result.aggregate
    putAmsSiteProperty("timeline.metrics.host.aggregate.splitpoints", ','.join(precision_splits))
    putAmsSiteProperty("timeline.metrics.cluster.aggregate.splitpoints", ','.join(aggregate_splits))

    component_grafana_exists = False
    for service in services['services']:
      if 'components' in service:
        for component in service['components']:
          if 'StackServiceComponents' in component:
            # If Grafana is installed the hostnames would indicate its location
            if 'METRICS_GRAFANA' in component['StackServiceComponents']['component_name'] and\
              len(component['StackServiceComponents']['hostnames']) != 0:
              component_grafana_exists = True
              break
    pass

    if not component_grafana_exists:
      putGrafanaPropertyAttribute("metrics_grafana_password", "visible", "false")

    pass

  # TODO, move this to a generic stack advisor.
  def getZKHostPortString(self, services, include_port=True):
    """
    Returns the comma delimited string of zookeeper server host with the configure port installed in a cluster
    Example: zk.host1.org:2181,zk.host2.org:2181,zk.host3.org:2181
    include_port boolean param -> If port is also needed.
    """
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    include_zookeeper = "ZOOKEEPER" in servicesList
    zookeeper_host_port = ''

    if include_zookeeper:
      zookeeper_hosts = self.getHostNamesWithComponent("ZOOKEEPER", "ZOOKEEPER_SERVER", services)
      zookeeper_host_port_arr = []

      if include_port:
        zookeeper_port = self.getZKPort(services)
        for i in range(len(zookeeper_hosts)):
          zookeeper_host_port_arr.append(zookeeper_hosts[i] + ':' + zookeeper_port)
      else:
        for i in range(len(zookeeper_hosts)):
          zookeeper_host_port_arr.append(zookeeper_hosts[i])

      zookeeper_host_port = ",".join(zookeeper_host_port_arr)
    return zookeeper_host_port

  # TODO, move this to a generic stack advisor
  def getZKPort(self, services):
    zookeeper_port = '2181'     #default port
    if 'zoo.cfg' in services['configurations'] and ('clientPort' in services['configurations']['zoo.cfg']['properties']):
      zookeeper_port = services['configurations']['zoo.cfg']['properties']['clientPort']
    return zookeeper_port

  def getConfigurationClusterSummary(self, servicesList, hosts, components, services):

    hBaseInstalled = False
    if 'HBASE' in servicesList:
      hBaseInstalled = True

    cluster = {
      "cpu": 0,
      "disk": 0,
      "ram": 0,
      "hBaseInstalled": hBaseInstalled,
      "components": components
    }

    if len(hosts["items"]) > 0:
      nodeManagerHosts = self.getHostsWithComponent("YARN", "NODEMANAGER", services, hosts)
      # NodeManager host with least memory is generally used in calculations as it will work in larger hosts.
      if nodeManagerHosts is not None and len(nodeManagerHosts) > 0:
        nodeManagerHost = nodeManagerHosts[0];
        for nmHost in nodeManagerHosts:
          if nmHost["Hosts"]["total_mem"] < nodeManagerHost["Hosts"]["total_mem"]:
            nodeManagerHost = nmHost
        host = nodeManagerHost["Hosts"]
        cluster["referenceNodeManagerHost"] = host
      else:
        host = hosts["items"][0]["Hosts"]
      cluster["referenceHost"] = host
      cluster["cpu"] = host["cpu_count"]
      cluster["disk"] = len(host["disk_info"])
      cluster["ram"] = int(host["total_mem"] / (1024 * 1024))

    ramRecommendations = [
      {"os":1, "hbase":1},
      {"os":2, "hbase":1},
      {"os":2, "hbase":2},
      {"os":4, "hbase":4},
      {"os":6, "hbase":8},
      {"os":8, "hbase":8},
      {"os":8, "hbase":8},
      {"os":12, "hbase":16},
      {"os":24, "hbase":24},
      {"os":32, "hbase":32},
      {"os":64, "hbase":32}
    ]
    index = {
      cluster["ram"] <= 4: 0,
      4 < cluster["ram"] <= 8: 1,
      8 < cluster["ram"] <= 16: 2,
      16 < cluster["ram"] <= 24: 3,
      24 < cluster["ram"] <= 48: 4,
      48 < cluster["ram"] <= 64: 5,
      64 < cluster["ram"] <= 72: 6,
      72 < cluster["ram"] <= 96: 7,
      96 < cluster["ram"] <= 128: 8,
      128 < cluster["ram"] <= 256: 9,
      256 < cluster["ram"]: 10
    }[1]


    cluster["reservedRam"] = ramRecommendations[index]["os"]
    cluster["hbaseRam"] = ramRecommendations[index]["hbase"]


    cluster["minContainerSize"] = {
      cluster["ram"] <= 3: 128,
      3 < cluster["ram"] <= 4: 256,
      4 < cluster["ram"] <= 8: 512,
      8 < cluster["ram"] <= 24: 1024,
      24 < cluster["ram"]: 2048
    }[1]

    totalAvailableRam = cluster["ram"] - cluster["reservedRam"]
    if cluster["hBaseInstalled"]:
      totalAvailableRam -= cluster["hbaseRam"]
    cluster["totalAvailableRam"] = max(512, totalAvailableRam * 1024)
    Logger.info("Memory for YARN apps - cluster[totalAvailableRam]: " + str(cluster["totalAvailableRam"]))

    suggestedMinContainerRam = 1024   # new smaller value for YARN min container
    callContext = getCallContext(services)

    operation = getUserOperationContext(services, DefaultStackAdvisor.OPERATION)
    if operation:
      Logger.info("user operation context : " + str(operation))

    if services:  # its never None but some unit tests pass it as None
      # If min container value is changed (user is changing it)
      # if its a validation call - just used what ever value is set
      # If its not a cluster create or add yarn service (TBD)
      if (getOldValue(self, services, "yarn-site", "yarn.scheduler.minimum-allocation-mb") or \
              'recommendConfigurations' != callContext) and operation != DefaultStackAdvisor.CLUSTER_CREATE_OPERATION:
        '''yarn.scheduler.minimum-allocation-mb has changed - then pick this value up'''
        if "yarn-site" in services["configurations"] and \
                "yarn.scheduler.minimum-allocation-mb" in services["configurations"]["yarn-site"]["properties"] and \
                str(services["configurations"]["yarn-site"]["properties"]["yarn.scheduler.minimum-allocation-mb"]).isdigit():
          Logger.info("Using user provided yarn.scheduler.minimum-allocation-mb = " +
                      str(services["configurations"]["yarn-site"]["properties"]["yarn.scheduler.minimum-allocation-mb"]))
          cluster["yarnMinContainerSize"] = int(services["configurations"]["yarn-site"]["properties"]["yarn.scheduler.minimum-allocation-mb"])
          Logger.info("Minimum ram per container due to user input - cluster[yarnMinContainerSize]: " + str(cluster["yarnMinContainerSize"]))
          if cluster["yarnMinContainerSize"] > cluster["totalAvailableRam"]:
            cluster["yarnMinContainerSize"] = cluster["totalAvailableRam"]
            Logger.info("Minimum ram per container after checking against limit - cluster[yarnMinContainerSize]: " + str(cluster["yarnMinContainerSize"]))
            pass
          cluster["minContainerSize"] = cluster["yarnMinContainerSize"]    # set to what user has suggested as YARN min container size
          suggestedMinContainerRam = cluster["yarnMinContainerSize"]
          pass
        pass
      pass


    '''containers = max(3, min (2*cores,min (1.8*DISKS,(Total available RAM) / MIN_CONTAINER_SIZE))))'''
    cluster["containers"] = int(round(max(3,
                                min(2 * cluster["cpu"],
                                    min(ceil(1.8 * cluster["disk"]),
                                            cluster["totalAvailableRam"] / cluster["minContainerSize"])))))
    Logger.info("Containers per node - cluster[containers]: " + str(cluster["containers"]))

    if cluster["containers"] * cluster["minContainerSize"] > cluster["totalAvailableRam"]:
      cluster["containers"] = ceil(cluster["totalAvailableRam"] / cluster["minContainerSize"])
      Logger.info("Modified number of containers based on provided value for yarn.scheduler.minimum-allocation-mb")
      pass

    cluster["ramPerContainer"] = int(abs(cluster["totalAvailableRam"] / cluster["containers"]))
    cluster["yarnMinContainerSize"] = min(suggestedMinContainerRam, cluster["ramPerContainer"])
    Logger.info("Ram per containers before normalization - cluster[ramPerContainer]: " + str(cluster["ramPerContainer"]))

    '''If greater than cluster["yarnMinContainerSize"], value will be in multiples of cluster["yarnMinContainerSize"]'''
    if cluster["ramPerContainer"] > cluster["yarnMinContainerSize"]:
      cluster["ramPerContainer"] = int(cluster["ramPerContainer"] / cluster["yarnMinContainerSize"]) * cluster["yarnMinContainerSize"]


    cluster["mapMemory"] = int(cluster["ramPerContainer"])
    cluster["reduceMemory"] = cluster["ramPerContainer"]
    cluster["amMemory"] = max(cluster["mapMemory"], cluster["reduceMemory"])

    Logger.info("Min container size - cluster[yarnMinContainerSize]: " + str(cluster["yarnMinContainerSize"]))
    Logger.info("Available memory for map - cluster[mapMemory]: " + str(cluster["mapMemory"]))
    Logger.info("Available memory for reduce - cluster[reduceMemory]: " + str(cluster["reduceMemory"]))
    Logger.info("Available memory for am - cluster[amMemory]: " + str(cluster["amMemory"]))


    return cluster

  def getServiceConfigurationValidators(self):
    return {
      "HDFS": { "hdfs-site": self.validateHDFSConfigurations,
                "hadoop-env": self.validateHDFSConfigurationsEnv,
                "core-site": self.validateHDFSConfigurationsCoreSite},
      "MAPREDUCE2": {"mapred-site": self.validateMapReduce2Configurations},
      "YARN": {"yarn-site": self.validateYARNConfigurations,
               "yarn-env": self.validateYARNEnvConfigurations},
      "HBASE": {"hbase-env": self.validateHbaseEnvConfigurations},
      "STORM": {"storm-site": self.validateStormConfigurations},
      "AMBARI_METRICS": {"ams-hbase-site": self.validateAmsHbaseSiteConfigurations,
              "ams-hbase-env": self.validateAmsHbaseEnvConfigurations,
              "ams-site": self.validateAmsSiteConfigurations,
              "ams-env": self.validateAmsEnvConfigurations}
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

  def validateAmsSiteConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = []

    op_mode = properties.get("timeline.metrics.service.operation.mode")
    correct_op_mode_item = None
    if op_mode not in ("embedded", "distributed"):
      correct_op_mode_item = self.getErrorItem("Correct value should be set.")
      pass
    elif len(self.getComponentHostNames(services, "AMBARI_METRICS", "METRICS_COLLECTOR")) > 1 and op_mode != 'distributed':
      correct_op_mode_item = self.getErrorItem("Correct value should be 'distributed' for clusters with more then 1 Metrics collector")
    validationItems.extend([{"config-name":'timeline.metrics.service.operation.mode', "item": correct_op_mode_item }])
    return self.toConfigurationValidationProblems(validationItems, "ams-site")

  def validateAmsHbaseSiteConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):

    amsCollectorHosts = self.getComponentHostNames(services, "AMBARI_METRICS", "METRICS_COLLECTOR")
    ams_site = getSiteProperties(configurations, "ams-site")
    core_site = getSiteProperties(configurations, "core-site")

    collector_heapsize, hbase_heapsize, total_sinks_count = self.getAmsMemoryRecommendation(services, hosts)
    recommendedDiskSpace = 10485760
    # TODO validate configuration for multiple AMBARI_METRICS collectors
    if len(amsCollectorHosts) > 1:
      pass
    else:
      if total_sinks_count > 2000:
        recommendedDiskSpace  = 104857600  # * 1k == 100 Gb
      elif total_sinks_count > 500:
        recommendedDiskSpace  = 52428800  # * 1k == 50 Gb
      elif total_sinks_count > 250:
        recommendedDiskSpace  = 20971520  # * 1k == 20 Gb

    validationItems = []

    rootdir_item = None
    op_mode = ams_site.get("timeline.metrics.service.operation.mode")
    default_fs = core_site.get("fs.defaultFS") if core_site else "file:///"
    hbase_rootdir = properties.get("hbase.rootdir")
    hbase_tmpdir = properties.get("hbase.tmp.dir")
    distributed = properties.get("hbase.cluster.distributed")
    is_local_root_dir = hbase_rootdir.startswith("file://") or (default_fs.startswith("file://") and hbase_rootdir.startswith("/"))

    if op_mode == "distributed" and is_local_root_dir:
      rootdir_item = self.getWarnItem("In distributed mode hbase.rootdir should point to HDFS.")
    elif op_mode == "embedded":
      if distributed.lower() == "false" and hbase_rootdir.startswith('/') or hbase_rootdir.startswith("hdfs://"):
        rootdir_item = self.getWarnItem("In embedded mode hbase.rootdir cannot point to schemaless values or HDFS, "
                                        "Example - file:// for localFS")
      pass

    distributed_item = None
    if op_mode == "distributed" and not distributed.lower() == "true":
      distributed_item = self.getErrorItem("hbase.cluster.distributed property should be set to true for "
                                           "distributed mode")
    if op_mode == "embedded" and distributed.lower() == "true":
      distributed_item = self.getErrorItem("hbase.cluster.distributed property should be set to false for embedded mode")

    hbase_zk_client_port = properties.get("hbase.zookeeper.property.clientPort")
    zkPort = self.getZKPort(services)
    hbase_zk_client_port_item = None
    if distributed.lower() == "true" and op_mode == "distributed" and \
        hbase_zk_client_port != zkPort and hbase_zk_client_port != "{{zookeeper_clientPort}}":
      hbase_zk_client_port_item = self.getErrorItem("In AMS distributed mode, hbase.zookeeper.property.clientPort "
                                                    "should be the cluster zookeeper server port : {0}".format(zkPort))

    if distributed.lower() == "false" and op_mode == "embedded" and \
        hbase_zk_client_port == zkPort and hbase_zk_client_port != "{{zookeeper_clientPort}}":
      hbase_zk_client_port_item = self.getErrorItem("In AMS embedded mode, hbase.zookeeper.property.clientPort "
                                                    "should be a different port than cluster zookeeper port."
                                                    "(default:61181)")

    validationItems.extend([{"config-name":'hbase.rootdir', "item": rootdir_item },
                            {"config-name":'hbase.cluster.distributed', "item": distributed_item },
                            {"config-name":'hbase.zookeeper.property.clientPort', "item": hbase_zk_client_port_item }])

    for collectorHostName in amsCollectorHosts:
      for host in hosts["items"]:
        if host["Hosts"]["host_name"] == collectorHostName:
          if op_mode == 'embedded' or is_local_root_dir:
            validationItems.extend([{"config-name": 'hbase.rootdir', "item": self.validatorEnoughDiskSpace(properties, 'hbase.rootdir', host["Hosts"], recommendedDiskSpace)}])
            validationItems.extend([{"config-name": 'hbase.rootdir', "item": self.validatorNotRootFs(properties, recommendedDefaults, 'hbase.rootdir', host["Hosts"])}])
            validationItems.extend([{"config-name": 'hbase.tmp.dir', "item": self.validatorNotRootFs(properties, recommendedDefaults, 'hbase.tmp.dir', host["Hosts"])}])

          dn_hosts = self.getComponentHostNames(services, "HDFS", "DATANODE")
          if is_local_root_dir:
            mountPoints = []
            for mountPoint in host["Hosts"]["disk_info"]:
              mountPoints.append(mountPoint["mountpoint"])
            hbase_rootdir_mountpoint = self.getMountPointForDir(hbase_rootdir, mountPoints)
            hbase_tmpdir_mountpoint = self.getMountPointForDir(hbase_tmpdir, mountPoints)
            preferred_mountpoints = self.getPreferredMountPoints(host['Hosts'])
            # hbase.rootdir and hbase.tmp.dir shouldn't point to the same partition
            # if multiple preferred_mountpoints exist
            if hbase_rootdir_mountpoint == hbase_tmpdir_mountpoint and \
              len(preferred_mountpoints) > 1:
              item = self.getWarnItem("Consider not using {0} partition for storing metrics temporary data. "
                                      "{0} partition is already used as hbase.rootdir to store metrics data".format(hbase_tmpdir_mountpoint))
              validationItems.extend([{"config-name":'hbase.tmp.dir', "item": item}])

            # if METRICS_COLLECTOR is co-hosted with DATANODE
            # cross-check dfs.datanode.data.dir and hbase.rootdir
            # they shouldn't share same disk partition IO
            hdfs_site = getSiteProperties(configurations, "hdfs-site")
            dfs_datadirs = hdfs_site.get("dfs.datanode.data.dir").split(",") if hdfs_site and "dfs.datanode.data.dir" in hdfs_site else []
            if dn_hosts and collectorHostName in dn_hosts and ams_site and \
              dfs_datadirs and len(preferred_mountpoints) > len(dfs_datadirs):
              for dfs_datadir in dfs_datadirs:
                dfs_datadir_mountpoint = self.getMountPointForDir(dfs_datadir, mountPoints)
                if dfs_datadir_mountpoint == hbase_rootdir_mountpoint:
                  item = self.getWarnItem("Consider not using {0} partition for storing metrics data. "
                                          "{0} is already used by datanode to store HDFS data".format(hbase_rootdir_mountpoint))
                  validationItems.extend([{"config-name": 'hbase.rootdir', "item": item}])
                  break
          # If no local DN in distributed mode
          elif collectorHostName not in dn_hosts and distributed.lower() == "true":
            item = self.getWarnItem("It's recommended to install Datanode component on {0} "
                                    "to speed up IO operations between HDFS and Metrics "
                                    "Collector in distributed mode ".format(collectorHostName))
            validationItems.extend([{"config-name": "hbase.cluster.distributed", "item": item}])
          # Short circuit read should be enabled in distibuted mode
          # if local DN installed
          else:
            validationItems.extend([{"config-name": "dfs.client.read.shortcircuit", "item": self.validatorEqualsToRecommendedItem(properties, recommendedDefaults, "dfs.client.read.shortcircuit")}])

    return self.toConfigurationValidationProblems(validationItems, "ams-hbase-site")

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

  def validateAmsHbaseEnvConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):

    ams_env = getSiteProperties(configurations, "ams-env")
    amsHbaseSite = getSiteProperties(configurations, "ams-hbase-site")
    validationItems = []
    mb = 1024 * 1024
    gb = 1024 * mb

    regionServerItem = self.validatorLessThenDefaultValue(properties, recommendedDefaults, "hbase_regionserver_heapsize") ## FIXME if new service added
    if regionServerItem:
      validationItems.extend([{"config-name": "hbase_regionserver_heapsize", "item": regionServerItem}])

    hbaseMasterHeapsizeItem = self.validatorLessThenDefaultValue(properties, recommendedDefaults, "hbase_master_heapsize")
    if hbaseMasterHeapsizeItem:
      validationItems.extend([{"config-name": "hbase_master_heapsize", "item": hbaseMasterHeapsizeItem}])

    logDirItem = self.validatorEqualsPropertyItem(properties, "hbase_log_dir", ams_env, "metrics_collector_log_dir")
    if logDirItem:
      validationItems.extend([{"config-name": "hbase_log_dir", "item": logDirItem}])

    hbase_master_heapsize = self.to_number(properties["hbase_master_heapsize"])
    hbase_master_xmn_size = self.to_number(properties["hbase_master_xmn_size"])
    hbase_regionserver_heapsize = self.to_number(properties["hbase_regionserver_heapsize"])
    hbase_regionserver_xmn_size = self.to_number(properties["regionserver_xmn_size"])

    # Validate Xmn settings.
    masterXmnItem = None
    regionServerXmnItem = None
    is_hbase_distributed = amsHbaseSite.get("hbase.cluster.distributed").lower() == 'true'

    if is_hbase_distributed:
      minMasterXmn = 0.12 * hbase_master_heapsize
      maxMasterXmn = 0.2 * hbase_master_heapsize
      if hbase_master_xmn_size < minMasterXmn:
        masterXmnItem = self.getWarnItem("Value is lesser than the recommended minimum Xmn size of {0} "
                                         "(12% of hbase_master_heapsize)".format(int(ceil(minMasterXmn))))

      if hbase_master_xmn_size > maxMasterXmn:
        masterXmnItem = self.getWarnItem("Value is greater than the recommended maximum Xmn size of {0} "
                                         "(20% of hbase_master_heapsize)".format(int(floor(maxMasterXmn))))

      minRegionServerXmn = 0.12 * hbase_regionserver_heapsize
      maxRegionServerXmn = 0.2 * hbase_regionserver_heapsize
      if hbase_regionserver_xmn_size < minRegionServerXmn:
        regionServerXmnItem = self.getWarnItem("Value is lesser than the recommended minimum Xmn size of {0} "
                                               "(12% of hbase_regionserver_heapsize)"
                                               .format(int(ceil(minRegionServerXmn))))

      if hbase_regionserver_xmn_size > maxRegionServerXmn:
        regionServerXmnItem = self.getWarnItem("Value is greater than the recommended maximum Xmn size of {0} "
                                               "(20% of hbase_regionserver_heapsize)"
                                               .format(int(floor(maxRegionServerXmn))))
    else:
      minMasterXmn = 0.12 * (hbase_master_heapsize + hbase_regionserver_heapsize)
      maxMasterXmn = 0.2 *  (hbase_master_heapsize + hbase_regionserver_heapsize)
      if hbase_master_xmn_size < minMasterXmn:
        masterXmnItem = self.getWarnItem("Value is lesser than the recommended minimum Xmn size of {0} "
                                         "(12% of hbase_master_heapsize + hbase_regionserver_heapsize)"
                                         .format(int(ceil(minMasterXmn))))

      if hbase_master_xmn_size > maxMasterXmn:
        masterXmnItem = self.getWarnItem("Value is greater than the recommended maximum Xmn size of {0} "
                                         "(20% of hbase_master_heapsize + hbase_regionserver_heapsize)"
                                         .format(int(floor(maxMasterXmn))))
    if masterXmnItem:
      validationItems.extend([{"config-name": "hbase_master_xmn_size", "item": masterXmnItem}])

    if regionServerXmnItem:
      validationItems.extend([{"config-name": "regionserver_xmn_size", "item": regionServerXmnItem}])

    if hbaseMasterHeapsizeItem is None:
      hostMasterComponents = {}

      for service in services["services"]:
        for component in service["components"]:
          if component["StackServiceComponents"]["hostnames"] is not None:
            for hostName in component["StackServiceComponents"]["hostnames"]:
              if self.isMasterComponent(component):
                if hostName not in hostMasterComponents.keys():
                  hostMasterComponents[hostName] = []
                hostMasterComponents[hostName].append(component["StackServiceComponents"]["component_name"])

      amsCollectorHosts = self.getComponentHostNames(services, "AMBARI_METRICS", "METRICS_COLLECTOR")
      for collectorHostName in amsCollectorHosts:
        for host in hosts["items"]:
          if host["Hosts"]["host_name"] == collectorHostName:
            # AMS Collector co-hosted with other master components in bigger clusters
            if len(hosts['items']) > 31 and \
                            len(hostMasterComponents[collectorHostName]) > 2 and \
                            host["Hosts"]["total_mem"] < 32*mb: # < 32Gb(total_mem in k)
              masterHostMessage = "Host {0} is used by multiple master components ({1}). " \
                                  "It is recommended to use a separate host for the " \
                                  "Ambari Metrics Collector component and ensure " \
                                  "the host has sufficient memory available."

              hbaseMasterHeapsizeItem = self.getWarnItem(masterHostMessage.format(
                  collectorHostName, str(", ".join(hostMasterComponents[collectorHostName]))))
              if hbaseMasterHeapsizeItem:
                validationItems.extend([{"config-name": "hbase_master_heapsize", "item": hbaseMasterHeapsizeItem}])

            # Check for unused RAM on AMS Collector node
            hostComponents = []
            for service in services["services"]:
              for component in service["components"]:
                if component["StackServiceComponents"]["hostnames"] is not None:
                  if collectorHostName in component["StackServiceComponents"]["hostnames"]:
                    hostComponents.append(component["StackServiceComponents"]["component_name"])

            requiredMemory = self.getMemorySizeRequired(services, hostComponents, configurations)
            unusedMemory = host["Hosts"]["total_mem"] * 1024 - requiredMemory # in bytes

            heapPropertyToIncrease = "hbase_regionserver_heapsize" if is_hbase_distributed else "hbase_master_heapsize"
            xmnPropertyToIncrease = "regionserver_xmn_size" if is_hbase_distributed else "hbase_master_xmn_size"
            hbase_needs_increase = self.to_number(properties[heapPropertyToIncrease]) * mb < 32 * gb

            if unusedMemory > 4*gb and hbase_needs_increase:  # warn user, if more than 4GB RAM is unused

              recommended_hbase_heapsize = int((unusedMemory - 4*gb)*4/5) + self.to_number(properties.get(heapPropertyToIncrease))*mb
              recommended_hbase_heapsize = min(32*gb, recommended_hbase_heapsize) #Make sure heapsize <= 32GB
              recommended_hbase_heapsize = round_to_n(recommended_hbase_heapsize/mb,128) # Round to 128m multiple
              if self.to_number(properties[heapPropertyToIncrease]) < recommended_hbase_heapsize:
                hbaseHeapsizeItem = self.getWarnItem("Consider allocating {0} MB to {1} in ams-hbase-env to use up some "
                                                     "unused memory on host"
                                                         .format(recommended_hbase_heapsize,
                                                                 heapPropertyToIncrease))
                validationItems.extend([{"config-name": heapPropertyToIncrease, "item": hbaseHeapsizeItem}])

              recommended_xmn_size = round_to_n(0.15*recommended_hbase_heapsize,128)
              if self.to_number(properties[xmnPropertyToIncrease]) < recommended_xmn_size:
                xmnPropertyToIncreaseItem = self.getWarnItem("Consider allocating {0} MB to use up some unused memory "
                                                             "on host".format(recommended_xmn_size))
                validationItems.extend([{"config-name": xmnPropertyToIncrease, "item": xmnPropertyToIncreaseItem}])
      pass

    return self.toConfigurationValidationProblems(validationItems, "ams-hbase-env")

  def validateAmsEnvConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):

    ams_env = getSiteProperties(configurations, "ams-env")
    mb = 1024 * 1024
    gb = 1024 * mb
    validationItems = []
    collector_heapsize = self.to_number(ams_env.get("metrics_collector_heapsize"))
    amsCollectorHosts = self.getComponentHostNames(services, "AMBARI_METRICS", "METRICS_COLLECTOR")
    for collectorHostName in amsCollectorHosts:
      for host in hosts["items"]:
        if host["Hosts"]["host_name"] == collectorHostName:
          hostComponents = []
          for service in services["services"]:
            for component in service["components"]:
              if component["StackServiceComponents"]["hostnames"] is not None:
                if collectorHostName in component["StackServiceComponents"]["hostnames"]:
                  hostComponents.append(component["StackServiceComponents"]["component_name"])

          requiredMemory = self.getMemorySizeRequired(services, hostComponents, configurations)
          unusedMemory = host["Hosts"]["total_mem"] * 1024 - requiredMemory # in bytes
          collector_needs_increase = collector_heapsize * mb < 16 * gb

          if unusedMemory > 4*gb and collector_needs_increase:  # warn user, if more than 4GB RAM is unused
            recommended_collector_heapsize = int((unusedMemory - 4*gb)/5) + collector_heapsize * mb
            recommended_collector_heapsize = round_to_n(recommended_collector_heapsize/mb,128) # Round to 128m multiple
            if collector_heapsize < recommended_collector_heapsize:
              validation_msg = "Consider allocating {0} MB to metrics_collector_heapsize in ams-env to use up some " \
                               "unused memory on host"
              collectorHeapsizeItem = self.getWarnItem(validation_msg.format(recommended_collector_heapsize))
              validationItems.extend([{"config-name": "metrics_collector_heapsize", "item": collectorHeapsizeItem}])
    pass
    return self.toConfigurationValidationProblems(validationItems, "ams-env")

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

  # TODO, move to YARN Service Advisor
  def validatorYarnQueue(self, properties, recommendedDefaults, propertyName, services):
    if propertyName not in properties:
      return self.getErrorItem("Value should be set")

    capacity_scheduler_properties, _ = self.getCapacitySchedulerProperties(services)
    leaf_queue_names = self.getAllYarnLeafQueues(capacity_scheduler_properties)
    queue_name = properties[propertyName]

    if len(leaf_queue_names) == 0:
      return None
    elif queue_name not in leaf_queue_names:
      return self.getErrorItem("Queue is not exist or not corresponds to existing YARN leaf queue")

    return None

  # TODO, move to YARN Service Advisor
  def recommendYarnQueue(self, services, catalog_name=None, queue_property=None):
    old_queue_name = None

    if services and 'configurations' in services:
        configurations = services["configurations"]
        if catalog_name in configurations and queue_property in configurations[catalog_name]["properties"]:
          old_queue_name = configurations[catalog_name]["properties"][queue_property]

        capacity_scheduler_properties, _ = self.getCapacitySchedulerProperties(services)
        leaf_queues = sorted(self.getAllYarnLeafQueues(capacity_scheduler_properties))

        if leaf_queues and (old_queue_name is None or old_queue_name not in leaf_queues):
          return leaf_queues.pop()
        elif old_queue_name and old_queue_name in leaf_queues:
          return None

    return "default"

  def validateXmxValue(self, properties, recommendedDefaults, propertyName):
    if not propertyName in properties:
      return self.getErrorItem("Value should be set")
    value = properties[propertyName]
    defaultValue = recommendedDefaults[propertyName]
    if defaultValue is None:
      return self.getErrorItem("Config's default value can't be null or undefined")
    if not self.checkXmxValueFormat(value) and self.checkXmxValueFormat(defaultValue):
      # Xmx is in the default-value but not the value, should be an error
      return self.getErrorItem('Invalid value format')
    if not self.checkXmxValueFormat(defaultValue):
      # if default value does not contain Xmx, then there is no point in validating existing value
      return None
    valueInt = self.formatXmxSizeToBytes(self.getXmxSize(value))
    defaultValueXmx = self.getXmxSize(defaultValue)
    defaultValueInt = self.formatXmxSizeToBytes(defaultValueXmx)
    if valueInt < defaultValueInt:
      return self.getWarnItem("Value is less than the recommended default of -Xmx" + defaultValueXmx)
    return None

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
    validationItems = [ {"config-name": 'yarn.nodemanager.resource.memory-mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'yarn.nodemanager.resource.memory-mb')},
                        {"config-name": 'yarn.scheduler.minimum-allocation-mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'yarn.scheduler.minimum-allocation-mb')},
                        {"config-name": 'yarn.nodemanager.linux-container-executor.group', "item": self.validatorEqualsPropertyItem(properties, "yarn.nodemanager.linux-container-executor.group", clusterEnv, "user_group")},
                        {"config-name": 'yarn.scheduler.maximum-allocation-mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'yarn.scheduler.maximum-allocation-mb')} ]
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

  def get_system_min_uid(self):
    login_defs = '/etc/login.defs'
    uid_min_tag = 'UID_MIN'
    comment_tag = '#'
    uid_min = uid_default = '1000'
    uid = None

    if os.path.exists(login_defs):
      with open(login_defs, 'r') as f:
        data = f.read().split('\n')
        # look for uid_min_tag in file
        uid = filter(lambda x: uid_min_tag in x, data)
        # filter all lines, where uid_min_tag was found in comments
        uid = filter(lambda x: x.find(comment_tag) > x.find(uid_min_tag) or x.find(comment_tag) == -1, uid)

      if uid is not None and len(uid) > 0:
        uid = uid[0]
        comment = uid.find(comment_tag)
        tag = uid.find(uid_min_tag)
        if comment == -1:
          uid_tag = tag + len(uid_min_tag)
          uid_min = uid[uid_tag:].strip()
        elif comment > tag:
          uid_tag = tag + len(uid_min_tag)
          uid_min = uid[uid_tag:comment].strip()

    # check result for value
    try:
      int(uid_min)
    except ValueError:
      return uid_default

    return uid_min

  def mergeValidators(self, parentValidators, childValidators):
    for service, configsDict in childValidators.iteritems():
      if service not in parentValidators:
        parentValidators[service] = {}
      parentValidators[service].update(configsDict)

  def checkSiteProperties(self, siteProperties, *propertyNames):
    """
    Check if properties defined in site properties.
    :param siteProperties: config properties dict
    :param *propertyNames: property names to validate
    :returns: True if all properties defined, in other cases returns False
    """
    if siteProperties is None:
      return False
    for name in propertyNames:
      if not (name in siteProperties):
        return False
    return True

  """
  Returns the dictionary of configs for 'capacity-scheduler'.
  """
  def getCapacitySchedulerProperties(self, services):
    capacity_scheduler_properties = dict()
    received_as_key_value_pair = True
    if "capacity-scheduler" in services['configurations']:
      if "capacity-scheduler" in services['configurations']["capacity-scheduler"]["properties"]:
        cap_sched_props_as_str = services['configurations']["capacity-scheduler"]["properties"]["capacity-scheduler"]
        if cap_sched_props_as_str:
          cap_sched_props_as_str = str(cap_sched_props_as_str).split('\n')
          if len(cap_sched_props_as_str) > 0 and cap_sched_props_as_str[0] != 'null':
            # Received confgs as one "\n" separated string
            for property in cap_sched_props_as_str:
              key, sep, value = property.partition("=")
              capacity_scheduler_properties[key] = value
            Logger.info("'capacity-scheduler' configs is passed-in as a single '\\n' separated string. "
                        "count(services['configurations']['capacity-scheduler']['properties']['capacity-scheduler']) = "
                        "{0}".format(len(capacity_scheduler_properties)))
            received_as_key_value_pair = False
          else:
            Logger.info("Passed-in services['configurations']['capacity-scheduler']['properties']['capacity-scheduler'] is 'null'.")
        else:
          Logger.info("'capacity-schdeuler' configs not passed-in as single '\\n' string in "
                      "services['configurations']['capacity-scheduler']['properties']['capacity-scheduler'].")
      if not capacity_scheduler_properties:
        # Received configs as a dictionary (Generally on 1st invocation).
        capacity_scheduler_properties = services['configurations']["capacity-scheduler"]["properties"]
        Logger.info("'capacity-scheduler' configs is passed-in as a dictionary. "
                    "count(services['configurations']['capacity-scheduler']['properties']) = {0}".format(len(capacity_scheduler_properties)))
    else:
      Logger.error("Couldn't retrieve 'capacity-scheduler' from services.")

    Logger.info("Retrieved 'capacity-scheduler' received as dictionary : '{0}'. configs : {1}" \
                .format(received_as_key_value_pair, capacity_scheduler_properties.items()))
    return capacity_scheduler_properties, received_as_key_value_pair

  """
  Gets all YARN leaf queues.
  """
  def getAllYarnLeafQueues(self, capacitySchedulerProperties):
    config_list = capacitySchedulerProperties.keys()
    yarn_queues = None
    leafQueueNames = set()
    if 'yarn.scheduler.capacity.root.queues' in config_list:
      yarn_queues = capacitySchedulerProperties.get('yarn.scheduler.capacity.root.queues')

    if yarn_queues:
      toProcessQueues = yarn_queues.split(",")
      while len(toProcessQueues) > 0:
        queue = toProcessQueues.pop()
        queueKey = "yarn.scheduler.capacity.root." + queue + ".queues"
        if queueKey in capacitySchedulerProperties:
          # If parent queue, add children
          subQueues = capacitySchedulerProperties[queueKey].split(",")
          for subQueue in subQueues:
            toProcessQueues.append(queue + "." + subQueue)
        else:
          # Leaf queues
          # We only take the leaf queue name instead of the complete path, as leaf queue names are unique in YARN.
          # Eg: If YARN queues are like :
          #     (1). 'yarn.scheduler.capacity.root.a1.b1.c1.d1',
          #     (2). 'yarn.scheduler.capacity.root.a1.b1.c2',
          #     (3). 'yarn.scheduler.capacity.root.default,
          # Added leaf queues names are as : d1, c2 and default for the 3 leaf queues.
          leafQueuePathSplits = queue.split(".")
          if leafQueuePathSplits > 0:
            leafQueueName = leafQueuePathSplits[-1]
            leafQueueNames.add(leafQueueName)
    return leafQueueNames

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

  def is_secured_cluster(self, services):
    """
    Detects if cluster is secured or not
    :type services dict
    :rtype bool
    """
    return services and "cluster-env" in services["configurations"] and\
           "security_enabled" in services["configurations"]["cluster-env"]["properties"] and\
           services["configurations"]["cluster-env"]["properties"]["security_enabled"].lower() == "true"



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

def getCallContext(services):
  if services:
    if DefaultStackAdvisor.ADVISOR_CONTEXT in services:
      Logger.info("call type context : " + str(services[DefaultStackAdvisor.ADVISOR_CONTEXT]))
      return services[DefaultStackAdvisor.ADVISOR_CONTEXT][DefaultStackAdvisor.CALL_TYPE]
  return ""


def getOldValue(self, services, configType, propertyName):
  if services:
    if 'changed-configurations' in services.keys():
      changedConfigs = services["changed-configurations"]
      for changedConfig in changedConfigs:
        if changedConfig["type"] == configType and changedConfig["name"]== propertyName and "old_value" in changedConfig:
          return changedConfig["old_value"]
  return None

def getUserOperationContext(services, contextName):
  if services:
    if 'user-context' in services.keys():
      userContext = services["user-context"]
      if contextName in userContext:
        return userContext[contextName]
  return None

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
