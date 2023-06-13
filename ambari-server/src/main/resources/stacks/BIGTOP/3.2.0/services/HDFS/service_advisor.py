#!/usr/bin/env ambari-python-wrap
"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
disass HDFSRecommender(service_advisor.ServiceAdvisributed with this work for additional information
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
import inspect

# Local imports
from resource_management.libraries.functions.mounted_dirs_helper import get_mounts_with_multiple_data_dirs


SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
STACKS_DIR = os.path.join(SCRIPT_DIR, '../../../../../stacks/')
PARENT_FILE = os.path.join(STACKS_DIR, 'service_advisor.py')

try:
  if "BASE_SERVICE_ADVISOR" in os.environ:
    PARENT_FILE = os.environ["BASE_SERVICE_ADVISOR"]
  with open(PARENT_FILE, 'rb') as fp:
    service_advisor = imp.load_module('service_advisor', fp, PARENT_FILE, ('.py', 'rb', imp.PY_SOURCE))
except Exception as e:
  traceback.print_exc()
  print "Failed to load parent."


class HDFSServiceAdvisor(service_advisor.ServiceAdvisor):

  def __init__(self, *args, **kwargs):
    self.as_super = super(HDFSServiceAdvisor, self)
    self.as_super.__init__(*args, **kwargs)

    self.initialize_logger("HDFSServiceAdvisor")

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
    Must be overridden in child class.
    """
    # Nothing to do
    pass

  def modifyCardinalitiesDict(self):
    """
    Modify the dictionary of cardinalities.
    Must be overridden in child class.
    """
    # Nothing to do
    pass

  def modifyHeapSizeProperties(self):
    """
    Modify the dictionary of heap size properties.
    Must be overridden in child class.
    """
    self.heap_size_properties = {"NAMENODE":
                                   [{"config-name": "hadoop-env",
                                     "property": "namenode_heapsize",
                                     "default": "1024m"}],
                                 "SECONDARY_NAMENODE":
                                   [{"config-name": "hadoop-env",
                                     "property": "namenode_heapsize",
                                     "default": "1024m"}],
                                 "DATANODE":
                                   [{"config-name": "hadoop-env",
                                     "property": "dtnode_heapsize",
                                     "default": "1024m"}]}

  def modifyNotValuableComponents(self):
    """
    Modify the set of components whose host assignment is based on other services.
    Must be overridden in child class.
    """
    self.notValuableComponents |= set(['JOURNALNODE', 'ZKFC'])

  def modifyComponentsNotPreferableOnServer(self):
    """
    Modify the set of components that are not preferable on the server.
    Must be overridden in child class.
    """
    # Nothing to do
    pass

  def modifyComponentLayoutSchemes(self):
    """
    Modify layout scheme dictionaries for components.
    The scheme dictionary basically maps the number of hosts to
    host index where component should exist.
    Must be overridden in child class.
    """
    self.componentLayoutSchemes.update({
      'NAMENODE': {"else": 0},
      'SECONDARY_NAMENODE': {"else": 1}
    })

  def getServiceComponentLayoutValidations(self, services, hosts):
    """
    Get a list of errors.
    Must be overridden in child class.
    """
    self.logger.info("Class: %s, Method: %s. Validating Service Component Layout." %
                (self.__class__.__name__, inspect.stack()[0][3]))

    # HDFS allows NameNode and Secondary NameNode to be on the same host.
    return self.getServiceComponentCardinalityValidations(services, hosts, "HDFS")

  def getServiceConfigurationRecommendations(self, configurations, clusterData, services, hosts):
    """
    Entry point.
    Must be overridden in child class.
    """
    self.logger.info("Class: %s, Method: %s. Recommending Service Configurations." %
                (self.__class__.__name__, inspect.stack()[0][3]))

    # Due to the existing stack inheritance, make it clear where each calculation came from.
    recommender = HDFSRecommender()
    recommender.recommendConfigurationsFromHDP206(configurations, clusterData, services, hosts)
    recommender.recommendConfigurationsFromHDP22(configurations, clusterData, services, hosts)
    recommender.recommendConfigurationsFromHDP23(configurations, clusterData, services, hosts)
    recommender.recommendConfigurationsFromHDP26(configurations, clusterData, services, hosts)
    recommender.recommendConfigurationsForSSO(configurations, clusterData, services, hosts)

  def getServiceConfigurationRecommendationsForSSO(self, configurations, clusterData, services, hosts):
    """
    Entry point.
    Must be overridden in child class.
    """
    recommender = HDFSRecommender()
    recommender.recommendConfigurationsForSSO(configurations, clusterData, services, hosts)

  def getServiceConfigurationsValidationItems(self, configurations, recommendedDefaults, services, hosts):
    """
    Entry point.
    Validate configurations for the service. Return a list of errors.
    The code for this function should be the same for each Service Advisor.
    """
    self.logger.info("Class: %s, Method: %s. Validating Configurations." %
                (self.__class__.__name__, inspect.stack()[0][3]))

    validator = HDFSValidator()
    # Calls the methods of the validator using arguments,
    # method(siteProperties, siteRecommendations, configurations, services, hosts)
    return validator.validateListOfConfigUsingMethod(configurations, recommendedDefaults, services, hosts, validator.validators)

  def isComponentUsingCardinalityForLayout(self, componentName):
    return componentName == 'NFS_GATEWAY'


class HDFSRecommender(service_advisor.ServiceAdvisor):
  """
  HDFS Recommender suggests properties when adding the service for the first time or modifying configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(HDFSRecommender, self)
    self.as_super.__init__(*args, **kwargs)

  def recommendConfigurationsFromHDP206(self, configurations, clusterData, services, hosts):
    """
    Recommend configurations for this service based on HDP 2.0.6.
    """
    self.logger.info("Class: %s, Method: %s. Recommending Service Configurations." %
                (self.__class__.__name__, inspect.stack()[0][3]))

    putHDFSProperty = self.putProperty(configurations, "hadoop-env", services)
    putHDFSSiteProperty = self.putProperty(configurations, "hdfs-site", services)
    putHDFSSitePropertyAttributes = self.putPropertyAttribute(configurations, "hdfs-site")

    totalAvailableRam = clusterData['totalAvailableRam']
    self.logger.info("Class: %s, Method: %s. Total Available Ram: %s" % (self.__class__.__name__, inspect.stack()[0][3], str(totalAvailableRam)))
    putHDFSProperty('namenode_heapsize', max(int(totalAvailableRam / 2), 1024))
    putHDFSProperty = self.putProperty(configurations, "hadoop-env", services)
    putHDFSProperty('namenode_opt_newsize', max(int(totalAvailableRam / 8), 128))
    putHDFSProperty = self.putProperty(configurations, "hadoop-env", services)
    putHDFSProperty('namenode_opt_maxnewsize', max(int(totalAvailableRam / 8), 256))

    # Check if NN HA is enabled and recommend removing dfs.namenode.rpc-address
    hdfsSiteProperties = self.getServicesSiteProperties(services, "hdfs-site")
    nameServices = None
    if hdfsSiteProperties and 'dfs.internal.nameservices' in hdfsSiteProperties:
      nameServices = hdfsSiteProperties['dfs.internal.nameservices']
    if nameServices is None and hdfsSiteProperties and 'dfs.nameservices' in hdfsSiteProperties:
      nameServices = hdfsSiteProperties['dfs.nameservices']
    if nameServices and "dfs.ha.namenodes.%s" % nameServices in hdfsSiteProperties:
      namenodes = hdfsSiteProperties["dfs.ha.namenodes.%s" % nameServices]
      if len(namenodes.split(',')) > 1:
        putHDFSSitePropertyAttributes("dfs.namenode.rpc-address", "delete", "true")

    self.logger.info("Class: %s, Method: %s. HDFS nameservices: %s" %
                (self.__class__.__name__, inspect.stack()[0][3], str(nameServices)))

    hdfs_mount_properties = [
      ("dfs.datanode.data.dir", "DATANODE", "/hadoop/hdfs/data", "multi"),
      ("dfs.namenode.name.dir", "DATANODE", "/hadoop/hdfs/namenode", "multi"),
      ("dfs.namenode.checkpoint.dir", "SECONDARY_NAMENODE", "/hadoop/hdfs/namesecondary", "single")
    ]

    self.logger.info("Class: %s, Method: %s. Updating HDFS mount properties." %
                (self.__class__.__name__, inspect.stack()[0][3]))
    self.updateMountProperties("hdfs-site", hdfs_mount_properties, configurations, services, hosts)

    dataDirs = []
    if configurations and "hdfs-site" in configurations and \
            "dfs.datanode.data.dir" in configurations["hdfs-site"]["properties"] and \
            configurations["hdfs-site"]["properties"]["dfs.datanode.data.dir"] is not None:
      dataDirs = configurations["hdfs-site"]["properties"]["dfs.datanode.data.dir"].split(",")

    elif hdfsSiteProperties and "dfs.datanode.data.dir" in hdfsSiteProperties and \
            hdfsSiteProperties["dfs.datanode.data.dir"] is not None:
      dataDirs = hdfsSiteProperties["dfs.datanode.data.dir"].split(",")

    self.logger.info("Class: %s, Method: %s. HDFS Data Dirs: %s" %
                (self.__class__.__name__, inspect.stack()[0][3], str(dataDirs)))

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
        mp = HDFSRecommender.getMountPointForDir(dataDir, mountPoints)
        for i in range(len(mountPoints)):
          if mp == mountPoints[i]:
            if mountPointDiskAvailableSpace[i] > maxFreeVolumeSizeForHost:
              maxFreeVolumeSizeForHost = mountPointDiskAvailableSpace[i]

      if (not reservedSizeRecommendation) or (maxFreeVolumeSizeForHost and maxFreeVolumeSizeForHost < reservedSizeRecommendation):
        reservedSizeRecommendation = maxFreeVolumeSizeForHost

    self.logger.info("Class: %s, Method: %s. HDFS Datanode recommended reserved size: %d" %
                (self.__class__.__name__, inspect.stack()[0][3], reservedSizeRecommendation))

    if reservedSizeRecommendation:
      reservedSizeRecommendation = max(reservedSizeRecommendation * 1024 / 8, 1073741824) # At least 1Gb is reserved
      putHDFSSiteProperty('dfs.datanode.du.reserved', reservedSizeRecommendation) #Bytes

    # recommendations for "hadoop.proxyuser.*.hosts", "hadoop.proxyuser.*.groups" properties in core-site
    self.recommendHadoopProxyUsers(configurations, services, hosts)

  def recommendConfigurationsFromHDP22(self, configurations, clusterData, services, hosts):
    """
    Recommend configurations for this service based on HDP 2.2
    """
    putHdfsSiteProperty = self.putProperty(configurations, "hdfs-site", services)
    putCoreSiteProperty = self.putProperty(configurations, "core-site", services)
    putHdfsSitePropertyAttribute = self.putPropertyAttribute(configurations, "hdfs-site")
    putHdfsSiteProperty("dfs.datanode.max.transfer.threads", 16384 if clusterData["hBaseInstalled"] else 4096)

    dataDirsCount = 1
    # Use users 'dfs.datanode.data.dir' first
    if "hdfs-site" in services["configurations"] and "dfs.datanode.data.dir" in services["configurations"]["hdfs-site"][
      "properties"]:
      dataDirsCount = len(
        str(services["configurations"]["hdfs-site"]["properties"]["dfs.datanode.data.dir"]).split(","))
    elif "dfs.datanode.data.dir" in configurations["hdfs-site"]["properties"]:
      dataDirsCount = len(str(configurations["hdfs-site"]["properties"]["dfs.datanode.data.dir"]).split(","))
    if dataDirsCount <= 2:
      failedVolumesTolerated = 0
    elif dataDirsCount <= 4:
      failedVolumesTolerated = 1
    else:
      failedVolumesTolerated = 2
    putHdfsSiteProperty("dfs.datanode.failed.volumes.tolerated", failedVolumesTolerated)

    namenodeHosts = self.getHostsWithComponent("HDFS", "NAMENODE", services, hosts)

    # 25 * # of cores on NameNode
    nameNodeCores = 4
    if namenodeHosts is not None and len(namenodeHosts):
      nameNodeCores = int(namenodeHosts[0]['Hosts']['cpu_count'])
    putHdfsSiteProperty("dfs.namenode.handler.count", 25 * nameNodeCores)
    if 25 * nameNodeCores > 200:
      putHdfsSitePropertyAttribute("dfs.namenode.handler.count", "maximum", 25 * nameNodeCores)

    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if ('ranger-hdfs-plugin-properties' in services['configurations']) and (
      'ranger-hdfs-plugin-enabled' in services['configurations']['ranger-hdfs-plugin-properties']['properties']):
      rangerPluginEnabled = services['configurations']['ranger-hdfs-plugin-properties']['properties'][
        'ranger-hdfs-plugin-enabled']
      if ("RANGER" in servicesList) and (rangerPluginEnabled.lower() == 'Yes'.lower()):
        putHdfsSiteProperty("dfs.permissions.enabled", 'true')

    putHdfsSiteProperty("dfs.namenode.safemode.threshold-pct", "0.999" if len(namenodeHosts) > 1 else "1.000")

    putHdfsEnvProperty = self.putProperty(configurations, "hadoop-env", services)
    putHdfsEnvPropertyAttribute = self.putPropertyAttribute(configurations, "hadoop-env")

    putHdfsEnvProperty('namenode_heapsize', max(int(clusterData['totalAvailableRam'] / 2), 1024))

    nn_heapsize_limit = None
    if (namenodeHosts is not None and len(namenodeHosts) > 0):
      if len(namenodeHosts) > 1:
        nn_max_heapsize = min(int(namenodeHosts[0]["Hosts"]["total_mem"]),
                              int(namenodeHosts[1]["Hosts"]["total_mem"])) / 1024
        masters_at_host = max(
          self.getHostComponentsByCategories(namenodeHosts[0]["Hosts"]["host_name"], ["MASTER"], services, hosts),
          self.getHostComponentsByCategories(namenodeHosts[1]["Hosts"]["host_name"], ["MASTER"], services, hosts))
      else:
        nn_max_heapsize = int(namenodeHosts[0]["Hosts"]["total_mem"] / 1024)  # total_mem in kb
        masters_at_host = self.getHostComponentsByCategories(namenodeHosts[0]["Hosts"]["host_name"], ["MASTER"],
                                                             services, hosts)

      putHdfsEnvPropertyAttribute('namenode_heapsize', 'maximum', max(nn_max_heapsize, 1024))

      nn_heapsize_limit = nn_max_heapsize
      nn_heapsize_limit -= clusterData["reservedRam"]
      if len(masters_at_host) > 1:
        nn_heapsize_limit = int(nn_heapsize_limit / 2)

      putHdfsEnvProperty('namenode_heapsize', max(nn_heapsize_limit, 1024))

    datanodeHosts = self.getHostsWithComponent("HDFS", "DATANODE", services, hosts)
    if datanodeHosts is not None and len(datanodeHosts) > 0:
      min_datanode_ram_kb = 1073741824  # 1 TB
      for datanode in datanodeHosts:
        ram_kb = datanode['Hosts']['total_mem']
        min_datanode_ram_kb = min(min_datanode_ram_kb, ram_kb)

      datanodeFilesM = len(datanodeHosts) * dataDirsCount / 10  # in millions, # of files = # of disks * 100'000
      nn_memory_configs = [
        {'nn_heap': 1024, 'nn_opt': 128},
        {'nn_heap': 3072, 'nn_opt': 512},
        {'nn_heap': 5376, 'nn_opt': 768},
        {'nn_heap': 9984, 'nn_opt': 1280},
        {'nn_heap': 14848, 'nn_opt': 2048},
        {'nn_heap': 19456, 'nn_opt': 2560},
        {'nn_heap': 24320, 'nn_opt': 3072},
        {'nn_heap': 33536, 'nn_opt': 4352},
        {'nn_heap': 47872, 'nn_opt': 6144},
        {'nn_heap': 59648, 'nn_opt': 7680},
        {'nn_heap': 71424, 'nn_opt': 8960},
        {'nn_heap': 94976, 'nn_opt': 8960}
      ]
      index = {
        datanodeFilesM < 1: 0,
        1 <= datanodeFilesM < 5: 1,
        5 <= datanodeFilesM < 10: 2,
        10 <= datanodeFilesM < 20: 3,
        20 <= datanodeFilesM < 30: 4,
        30 <= datanodeFilesM < 40: 5,
        40 <= datanodeFilesM < 50: 6,
        50 <= datanodeFilesM < 70: 7,
        70 <= datanodeFilesM < 100: 8,
        100 <= datanodeFilesM < 125: 9,
        125 <= datanodeFilesM < 150: 10,
        150 <= datanodeFilesM: 11
      }[1]

      nn_memory_config = nn_memory_configs[index]

      # override with new values if applicable
      if nn_heapsize_limit is not None and nn_memory_config['nn_heap'] <= nn_heapsize_limit:
        putHdfsEnvProperty('namenode_heapsize', nn_memory_config['nn_heap'])

      putHdfsEnvPropertyAttribute('dtnode_heapsize', 'maximum', int(min_datanode_ram_kb / 1024))

    nn_heapsize = int(configurations["hadoop-env"]["properties"]["namenode_heapsize"])
    putHdfsEnvProperty('namenode_opt_newsize', max(int(nn_heapsize / 8), 128))
    putHdfsEnvProperty('namenode_opt_maxnewsize', max(int(nn_heapsize / 8), 128))

    putHdfsSitePropertyAttribute = self.putPropertyAttribute(configurations, "hdfs-site")
    putHdfsSitePropertyAttribute('dfs.datanode.failed.volumes.tolerated', 'maximum', dataDirsCount)

    keyserverHostsString = None
    keyserverPortString = None
    if "hadoop-env" in services["configurations"] and "keyserver_host" in services["configurations"]["hadoop-env"]["properties"] and "keyserver_port" in services["configurations"]["hadoop-env"]["properties"]:
      keyserverHostsString = services["configurations"]["hadoop-env"]["properties"]["keyserver_host"]
      keyserverPortString = services["configurations"]["hadoop-env"]["properties"]["keyserver_port"]

    # Irrespective of what hadoop-env has, if Ranger-KMS is installed, we use its values.
    rangerKMSServerHosts = self.getHostsWithComponent("RANGER_KMS", "RANGER_KMS_SERVER", services, hosts)
    if rangerKMSServerHosts is not None and len(rangerKMSServerHosts) > 0:
      rangerKMSServerHostsArray = []
      for rangeKMSServerHost in rangerKMSServerHosts:
        rangerKMSServerHostsArray.append(rangeKMSServerHost["Hosts"]["host_name"])
      keyserverHostsString = ";".join(rangerKMSServerHostsArray)
      if "kms-env" in services["configurations"] and "kms_port" in services["configurations"]["kms-env"]["properties"]:
        keyserverPortString = services["configurations"]["kms-env"]["properties"]["kms_port"]

    if keyserverHostsString is not None and len(keyserverHostsString.strip()) > 0:
      urlScheme = "http"
      if "ranger-kms-site" in services["configurations"] and \
          "ranger.service.https.attrib.ssl.enabled" in services["configurations"]["ranger-kms-site"]["properties"] and \
          services["configurations"]["ranger-kms-site"]["properties"]["ranger.service.https.attrib.ssl.enabled"].lower() == "true":
        urlScheme = "https"

      if keyserverPortString is None or len(keyserverPortString.strip()) < 1:
        keyserverPortString = ":9292"
      else:
        keyserverPortString = ":" + keyserverPortString.strip()

      kmsPath = "kms://" + urlScheme + "@" + keyserverHostsString.strip() + keyserverPortString + "/kms"
      putCoreSiteProperty("hadoop.security.key.provider.path", kmsPath)
      putHdfsSiteProperty("dfs.encryption.key.provider.uri", kmsPath)

    putHdfsSitePropertyAttribute = self.putPropertyAttribute(configurations, "hdfs-site")
    putCoreSitePropertyAttribute = self.putPropertyAttribute(configurations, "core-site")
    if not "RANGER_KMS" in servicesList:
      putCoreSitePropertyAttribute('hadoop.security.key.provider.path','delete','true')
      putHdfsSitePropertyAttribute('dfs.encryption.key.provider.uri','delete','true')

    if "ranger-env" in services["configurations"] and "ranger-hdfs-plugin-properties" in services["configurations"] and \
      "ranger-hdfs-plugin-enabled" in services["configurations"]["ranger-env"]["properties"]:
      putHdfsRangerPluginProperty = self.putProperty(configurations, "ranger-hdfs-plugin-properties", services)
      rangerEnvHdfsPluginProperty = services["configurations"]["ranger-env"]["properties"]["ranger-hdfs-plugin-enabled"]
      putHdfsRangerPluginProperty("ranger-hdfs-plugin-enabled", rangerEnvHdfsPluginProperty)

  def recommendConfigurationsFromHDP23(self, configurations, clusterData, services, hosts):
    """
    Recommend configurations for this service based on HDP 2.3.
    """
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

  def recommendConfigurationsFromHDP26(self, configurations, clusterData, services, hosts):
    """
    Recommend configurations for this service based on HDP 2.6
    """
    if 'hadoop-env' in services['configurations'] and 'hdfs_user' in  services['configurations']['hadoop-env']['properties']:
      hdfs_user = services['configurations']['hadoop-env']['properties']['hdfs_user']
    else:
      hdfs_user = 'hadoop'

    if 'ranger-hdfs-plugin-properties' in configurations and 'ranger-hdfs-plugin-enabled' in configurations['ranger-hdfs-plugin-properties']['properties']:
      ranger_hdfs_plugin_enabled = (configurations['ranger-hdfs-plugin-properties']['properties']['ranger-hdfs-plugin-enabled'].lower() == 'Yes'.lower())
    elif 'ranger-hdfs-plugin-properties' in services['configurations'] and 'ranger-hdfs-plugin-enabled' in services['configurations']['ranger-hdfs-plugin-properties']['properties']:
      ranger_hdfs_plugin_enabled = (services['configurations']['ranger-hdfs-plugin-properties']['properties']['ranger-hdfs-plugin-enabled'].lower() == 'Yes'.lower())
    else:
      ranger_hdfs_plugin_enabled = False

    if ranger_hdfs_plugin_enabled and 'ranger-hdfs-plugin-properties' in services['configurations'] and 'REPOSITORY_CONFIG_USERNAME' in services['configurations']['ranger-hdfs-plugin-properties']['properties']:
      self.logger.info("Setting HDFS Repo user for Ranger.")
      putRangerHDFSPluginProperty = self.putProperty(configurations, "ranger-hdfs-plugin-properties", services)
      putRangerHDFSPluginProperty("REPOSITORY_CONFIG_USERNAME",hdfs_user)
    else:
      self.logger.info("Not setting HDFS Repo user for Ranger.")

  def recommendConfigurationsForSSO(self, configurations, clusterData, services, hosts):
    ambari_configuration = self.get_ambari_configuration(services)
    ambari_sso_details = ambari_configuration.get_ambari_sso_details() if ambari_configuration else None

    if ambari_sso_details and ambari_sso_details.is_managing_services():
      putHdfsSiteProperty = self.putProperty(configurations, "hdfs-site", services)

      # If SSO should be enabled for this service
      if ambari_sso_details.should_enable_sso('HDFS'):
        if(self.is_kerberos_enabled(configurations, services)):
          putHdfsSiteProperty('hadoop.http.authentication.type', "org.apache.hadoop.security.authentication.server.JWTRedirectAuthenticationHandler")
          putHdfsSiteProperty('hadoop.http.authentication.authentication.provider.url', ambari_sso_details.get_sso_provider_url())
          putHdfsSiteProperty('hadoop.http.authentication.public.key.pem', ambari_sso_details.get_sso_provider_certificate(False, True))
        else:
          # Since Kerberos is not enabled, we can not enable SSO
          self.logger.warn("Enabling SSO integration for HDFS requires Kerberos, Since Kerberos is not enabled, SSO integration is not being recommended.")
          putHdfsSiteProperty('hadoop.http.authentication.type', "simple")
          pass

      # If SSO should be disabled for this service
      elif ambari_sso_details.should_disable_sso('HDFS'):
        if(self.is_kerberos_enabled(configurations, services)):
          putHdfsSiteProperty('hadoop.http.authentication.type', "kerberos")
        else:
          putHdfsSiteProperty('hadoop.http.authentication.type', "simple")

  def is_kerberos_enabled(self, configurations, services):
    """
    Tests if HDFS has Kerberos enabled by first checking the recommended changes and then the
    existing settings.
    :type configurations dict
    :type services dict
    :rtype bool
    """
    return self._is_kerberos_enabled(configurations) or \
           (services and 'configurations' in services and self._is_kerberos_enabled(services['configurations']))

  def _is_kerberos_enabled(self, config):
    """
    Detects if HDFS has Kerberos enabled given a dictionary of configurations.
    :type config dict
    :rtype bool
    """
    return config and \
           (
             (
               "hdfs-site" in config and
               'hadoop.security.authentication' in config['hdfs-site']["properties"] and
               config['hdfs-site']["properties"]['hadoop.security.authentication'] == 'kerberos'
             ) or (
               "core-site" in config and
               'hadoop.security.authentication' in config['core-site']["properties"] and
               config['core-site']["properties"]['hadoop.security.authentication'] == 'kerberos'
             )
           )

class HDFSValidator(service_advisor.ServiceAdvisor):
  """
  HDFS Validator checks the correctness of properties whenever the service is first added or the user attempts to
  change configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(HDFSValidator, self)
    self.as_super.__init__(*args, **kwargs)

    self.validators = [("hdfs-site", self.validateHDFSConfigurationsFromHDP206),
                       ("hadoop-env", self.validateHadoopEnvConfigurationsFromHDP206),
                       ("core-site", self.validateHDFSCoreSiteFromHDP206),
                       ("hdfs-site", self.validateHDFSConfigurationsFromHDP22),
                       ("hadoop-env", self.validateHadoopEnvConfigurationsFromHDP22),
                       ("ranger-hdfs-plugin-properties", self.validateHDFSRangerPluginConfigurationsFromHDP22),
                       ("hdfs-site", self.validateRangerAuthorizerFromHDP23)]

    # **********************************************************
    # Example of how to add a function that validates a certain config type.
    # If the same config type has multiple functions, can keep adding tuples to self.validators
    #self.validators.append(("hadoop-env", self.sampleValidator))

  def sampleValidator(self, properties, recommendedDefaults, configurations, services, hosts):
    """
    Example of a validator function other other Service Advisors to emulate.
    :return: A list of configuration validation problems.
    """
    validationItems = []

    '''
    Item is a simple dictionary.
    Two functions can be used to construct it depending on the log level: WARN|ERROR
    E.g.,
    self.getErrorItem(message) or self.getWarnItem(message)

    item = {"level": "ERROR|WARN", "message": "value"}
    '''
    validationItems.append({"config-name": "my_config_property_name",
                            "item": self.getErrorItem("My custom message in method %s" % inspect.stack()[0][3])})
    return self.toConfigurationValidationProblems(validationItems, "hadoop-env")

  def validateHDFSConfigurationsFromHDP206(self, properties, recommendedDefaults, configurations, services, hosts):
    """
    This was copied from HDP 2.0.6; validate hdfs-site
    :return: A list of configuration validation problems.
    """
    clusterEnv = self.getSiteProperties(configurations, "cluster-env")
    validationItems = [{"config-name": 'dfs.datanode.du.reserved', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'dfs.datanode.du.reserved')},
                       {"config-name": 'dfs.datanode.data.dir', "item": self.validatorOneDataDirPerPartition(properties, 'dfs.datanode.data.dir', services, hosts, clusterEnv)}]
    return self.toConfigurationValidationProblems(validationItems, "hdfs-site")

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
        break

    if len(warnings) > 0:
      return self.getWarnItem("cluster-env/one_dir_per_partition is enabled but there are multiple data directories on the same mount. Affected hosts: {0}".format(", ".join(sorted(warnings))))

    return None

  def validateHadoopEnvConfigurationsFromHDP206(self, properties, recommendedDefaults, configurations, services, hosts):
    """
    This was copied from HDP 2.0.6; validate hadoop-env
    :return: A list of configuration validation problems.
    """
    validationItems = [ {"config-name": 'namenode_heapsize', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'namenode_heapsize')},
                        {"config-name": 'namenode_opt_newsize', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'namenode_opt_newsize')},
                        {"config-name": 'namenode_opt_maxnewsize', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'namenode_opt_maxnewsize')}]
    return self.toConfigurationValidationProblems(validationItems, "hadoop-env")

  def validateHDFSCoreSiteFromHDP206(self, properties, recommendedDefaults, configurations, services, hosts):
    """
    This was copied from HDP 2.0.6; validate core-site
    :return: A list of configuration validation problems.
    """
    validationItems = []
    validationItems.extend(self.getHadoopProxyUsersValidationItems(properties, services, hosts, configurations))
    validationItems.extend(self.getAmbariProxyUsersForHDFSValidationItems(properties, services))
    validationItems.extend(self.getLZOSupportValidationItems(properties, services))
    return self.toConfigurationValidationProblems(validationItems, "core-site")

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

  def validateHDFSConfigurationsFromHDP22(self, properties, recommendedDefaults, configurations, services, hosts):
    """
    This was copied from HDP 2.2; validate hdfs-site
    :return: A list of configuration validation problems.
    """
    # We can not access property hadoop.security.authentication from the
    # other config (core-site). That's why we are using another heuristic here
    hdfs_site = properties
    core_site = self.getSiteProperties(configurations, "core-site")

    dfs_encrypt_data_transfer = 'dfs.encrypt.data.transfer'  # Hadoop Wire encryption
    wire_encryption_enabled = False
    try:
      wire_encryption_enabled = hdfs_site[dfs_encrypt_data_transfer] == "true"
    except KeyError:
      pass

    HTTP_ONLY = 'HTTP_ONLY'
    HTTPS_ONLY = 'HTTPS_ONLY'
    HTTP_AND_HTTPS = 'HTTP_AND_HTTPS'
    VALID_HTTP_POLICY_VALUES = [HTTP_ONLY, HTTPS_ONLY, HTTP_AND_HTTPS]

    TRANSFER_PROTECTION_AUTHENTICATION = 'authentication'
    TRANSFER_PROTECTION_INTEGRITY = 'integrity'
    TRANSFER_PROTECTION_PRIVACY = 'privacy'
    TRANSFER_PROTECTION_AUTHENTICATION_AND_PRIVACY = 'authentication,privacy'
    VALID_TRANSFER_PROTECTION_VALUES = [TRANSFER_PROTECTION_AUTHENTICATION, TRANSFER_PROTECTION_INTEGRITY, TRANSFER_PROTECTION_PRIVACY, TRANSFER_PROTECTION_AUTHENTICATION_AND_PRIVACY]

    validationItems = []
    address_properties = [
      # "dfs.datanode.address",
      # "dfs.datanode.http.address",
      # "dfs.datanode.https.address",
      # "dfs.datanode.ipc.address",
      # "dfs.journalnode.http-address",
      # "dfs.journalnode.https-address",
      # "dfs.namenode.rpc-address",
      # "dfs.namenode.secondary.http-address",
      "dfs.namenode.http-address",
      "dfs.namenode.https-address",
    ]
    #Validating *address properties for correct values

    for address_property in address_properties:
      if address_property in hdfs_site:
        value = hdfs_site[address_property]
        if not HDFSValidator.is_valid_host_port_authority(value):
          validationItems.append({"config-name" : address_property, "item" :
            self.getErrorItem(address_property + " does not contain a valid host:port authority: " + value)})

    #Adding Ranger Plugin logic here
    ranger_plugin_properties = self.getSiteProperties(configurations, "ranger-hdfs-plugin-properties")
    ranger_plugin_enabled = ranger_plugin_properties['ranger-hdfs-plugin-enabled'] if ranger_plugin_properties else 'No'
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if ("RANGER" in servicesList) and (ranger_plugin_enabled.lower() == 'Yes'.lower()):
      if 'dfs.permissions.enabled' in hdfs_site and \
              hdfs_site['dfs.permissions.enabled'] != 'true':
        validationItems.append({"config-name": 'dfs.permissions.enabled',
                                "item": self.getWarnItem("dfs.permissions.enabled needs to be set to true if Ranger HDFS Plugin is enabled.")})

    if (not wire_encryption_enabled and   # If wire encryption is enabled at Hadoop, it disables all our checks
            'hadoop.security.authentication' in core_site and
            core_site['hadoop.security.authentication'] == 'kerberos' and
            'hadoop.security.authorization' in core_site and
            core_site['hadoop.security.authorization'] == 'true'):
      # security is enabled

      dfs_http_policy = 'dfs.http.policy'
      dfs_datanode_address = 'dfs.datanode.address'
      datanode_http_address = 'dfs.datanode.http.address'
      datanode_https_address = 'dfs.datanode.https.address'
      data_transfer_protection = 'dfs.data.transfer.protection'

      try: # Params may be absent
        privileged_dfs_dn_port = HDFSValidator.isSecurePort(HDFSValidator.getPort(hdfs_site[dfs_datanode_address]))
      except KeyError:
        privileged_dfs_dn_port = False
      try:
        privileged_dfs_http_port = HDFSValidator.isSecurePort(HDFSValidator.getPort(hdfs_site[datanode_http_address]))
      except KeyError:
        privileged_dfs_http_port = False
      try:
        privileged_dfs_https_port = HDFSValidator.isSecurePort(HDFSValidator.getPort(hdfs_site[datanode_https_address]))
      except KeyError:
        privileged_dfs_https_port = False
      try:
        dfs_http_policy_value = hdfs_site[dfs_http_policy]
      except KeyError:
        dfs_http_policy_value = HTTP_ONLY  # Default
      try:
        data_transfer_protection_value = hdfs_site[data_transfer_protection]
      except KeyError:
        data_transfer_protection_value = None

      if dfs_http_policy_value not in VALID_HTTP_POLICY_VALUES:
        validationItems.append({"config-name": dfs_http_policy,
                                "item": self.getWarnItem(
                                  "Invalid property value: {0}. Valid values are {1}".format(
                                    dfs_http_policy_value, VALID_HTTP_POLICY_VALUES))})

      # determine whether we use secure ports
      address_properties_with_warnings = []
      if dfs_http_policy_value == HTTPS_ONLY:
        if not privileged_dfs_dn_port and data_transfer_protection_value is None:
          important_properties = [dfs_datanode_address, datanode_https_address]
          message = "You have set up datanode to use non-secure rpc port. " \
                    "If you want to run Datanode under non-root user in a secure cluster, " \
                    "you should set {0} ('{1}' is a good default value).".format(
            data_transfer_protection, VALID_TRANSFER_PROTECTION_VALUES[0])
          address_properties_with_warnings.extend(important_properties)
      else:  # dfs_http_policy_value == HTTP_AND_HTTPS or HTTP_ONLY
        # We don't enforce datanode_https_address to use privileged ports here
        any_nonprivileged_ports_are_in_use = not privileged_dfs_dn_port or not privileged_dfs_http_port
        if any_nonprivileged_ports_are_in_use:
          important_properties = [dfs_datanode_address, datanode_http_address]
          message = "You have set up datanode to use some non-secure ports. " \
                    "In a secure cluster:" \
                    "1. non-secure rpc port is allowed only if sasl is enabled by setting {0}." \
                    "2. non-secure http port is allowed only if {1} is set to {2}".format(
              data_transfer_protection, dfs_http_policy, HTTPS_ONLY)
          address_properties_with_warnings.extend(important_properties)

      # Generate port-related warnings if any
      for prop in address_properties_with_warnings:
        validationItems.append({"config-name": prop,
                                "item": self.getWarnItem(message)})

      # Check if it is appropriate to use dfs.data.transfer.protection
      if data_transfer_protection_value is not None:
        if data_transfer_protection_value not in VALID_TRANSFER_PROTECTION_VALUES:
          validationItems.append({"config-name": data_transfer_protection, "item": self.getWarnItem(
                                      "Invalid property value: {0}. Valid values are {1}.".format(data_transfer_protection_value, VALID_TRANSFER_PROTECTION_VALUES)
                                  )})
    return self.toConfigurationValidationProblems(validationItems, "hdfs-site")

  def validateHadoopEnvConfigurationsFromHDP22(self, properties, recommendedDefaults, configurations, services, hosts):
    """
    This was copied from HDP 2.2; validate hadoop-env
    :return: A list of configuration validation problems.
    """
    validationItems = [ {"config-name": 'namenode_heapsize', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'namenode_heapsize')},
                        {"config-name": 'namenode_opt_newsize', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'namenode_opt_newsize')},
                        {"config-name": 'namenode_opt_maxnewsize', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'namenode_opt_maxnewsize')}]
    return self.toConfigurationValidationProblems(validationItems, "hadoop-env")

  def validateHDFSRangerPluginConfigurationsFromHDP22(self, properties, recommendedDefaults, configurations, services, hosts):
    """
    This was copied from HDP 2.2; validate ranger-hdfs-plugin-properties
    :return: A list of configuration validation problems.
    """
    validationItems = []
    ranger_plugin_properties = self.getSiteProperties(configurations, "ranger-hdfs-plugin-properties")
    ranger_plugin_enabled = ranger_plugin_properties['ranger-hdfs-plugin-enabled'] if ranger_plugin_properties else 'No'
    if ranger_plugin_enabled.lower() == 'yes':
      # ranger-hdfs-plugin must be enabled in ranger-env
      ranger_env = self.getServicesSiteProperties(services, 'ranger-env')
      if not ranger_env or not 'ranger-hdfs-plugin-enabled' in ranger_env or ranger_env['ranger-hdfs-plugin-enabled'].lower() != 'yes':
        validationItems.append({"config-name": 'ranger-hdfs-plugin-enabled',
                                "item": self.getWarnItem(
                                  "ranger-hdfs-plugin-properties/ranger-hdfs-plugin-enabled must correspond ranger-env/ranger-hdfs-plugin-enabled")})
    return self.toConfigurationValidationProblems(validationItems, "ranger-hdfs-plugin-properties")

  def validateRangerAuthorizerFromHDP23(self, properties, recommendedDefaults, configurations, services, hosts):
    """
    This was copied from HDP 2.3
    If Ranger service is present and the ranger plugin is enabled, check that the provider class is correctly set.
    :return: A list of configuration validation problems.
    """
    self.logger.info("Class: %s, Method: %s. Checking if Ranger service is present and if the provider class is using the Ranger Authorizer." %
                (self.__class__.__name__, inspect.stack()[0][3]))
    # We can not access property hadoop.security.authentication from the
    # other config (core-site). That's why we are using another heuristics here
    hdfs_site = properties
    validationItems = [] #Adding Ranger Plugin logic here
    ranger_plugin_properties = self.getSiteProperties(configurations, "ranger-hdfs-plugin-properties")
    ranger_plugin_enabled = ranger_plugin_properties['ranger-hdfs-plugin-enabled'] if ranger_plugin_properties else 'No'
    servicesList = self.getServiceNames(services)
    if ("RANGER" in servicesList) and (ranger_plugin_enabled.lower() == 'yes'):

      try:
        if hdfs_site['dfs.namenode.inode.attributes.provider.class'].lower() != 'org.apache.ranger.authorization.hadoop.RangerHdfsAuthorizer'.lower():
          raise ValueError()
      except (KeyError, ValueError), e:
        message = "dfs.namenode.inode.attributes.provider.class needs to be set to 'org.apache.ranger.authorization.hadoop.RangerHdfsAuthorizer' if Ranger HDFS Plugin is enabled."
        validationItems.append({"config-name": 'dfs.namenode.inode.attributes.provider.class',
                                "item": self.getWarnItem(message)})

    return self.toConfigurationValidationProblems(validationItems, "hdfs-site")

  def getDataNodeHosts(self, services, hosts):
    """
    Returns the list of Data Node hosts. If none, return an empty list.
    """
    if len(hosts["items"]) > 0:
      dataNodeHosts = self.getHostsWithComponent("HDFS", "DATANODE", services, hosts)
      if dataNodeHosts is not None:
        return dataNodeHosts
    return []
