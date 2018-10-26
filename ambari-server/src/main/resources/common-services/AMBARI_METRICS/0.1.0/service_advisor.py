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
import re
import os
import sys
import socket
import traceback
from math import ceil, floor, log


from resource_management.core.logger import Logger

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
STACKS_DIR = os.path.join(SCRIPT_DIR, '../../../stacks/')
PARENT_FILE = os.path.join(STACKS_DIR, 'service_advisor.py')
if "BASE_SERVICE_ADVISOR" in os.environ:
  PARENT_FILE = os.environ["BASE_SERVICE_ADVISOR"]

try:
  with open(PARENT_FILE, 'rb') as fp:
    service_advisor = imp.load_module('service_advisor', fp, PARENT_FILE, ('.py', 'rb', imp.PY_SOURCE))
except Exception as e:
  traceback.print_exc()
  print "Failed to load parent"

class AMBARI_METRICSServiceAdvisor(service_advisor.ServiceAdvisor):

  def __init__(self, *args, **kwargs):
    self.as_super = super(AMBARI_METRICSServiceAdvisor, self)
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
    min_val = 1

    self.cardinalitiesDict.update(
      {
        'METRICS_COLLECTOR': {"min": min_val}
      }
    )

  def modifyHeapSizeProperties(self):
    """
    Modify the dictionary of heap size properties.
    Must be overriden in child class.
    """
    self.heap_size_properties = {"METRICS_COLLECTOR":
                                   [{"config-name": "ams-hbase-env",
                                     "property": "hbase_master_heapsize",
                                     "default": "1024m"},
                                    {"config-name": "ams-hbase-env",
                                     "property": "hbase_regionserver_heapsize",
                                     "default": "1024m"},
                                    {"config-name": "ams-env",
                                     "property": "metrics_collector_heapsize",
                                     "default": "512m"}]}


  def modifyNotValuableComponents(self):
    """
    Modify the set of components whose host assignment is based on other services.
    Must be overriden in child class.
    """
    self.notValuableComponents |= set(['METRICS_MONITOR'])

  def modifyComponentsNotPreferableOnServer(self):
    """
    Modify the set of components that are not preferable on the server.
    Must be overriden in child class.
    """
    self.notPreferableOnServerComponents |= set(['METRICS_COLLECTOR'])


  def modifyComponentLayoutSchemes(self):
    """
    Modify layout scheme dictionaries for components.
    The scheme dictionary basically maps the number of hosts to
    host index where component should exist.
    Must be overriden in child class.
    """
    self.componentLayoutSchemes.update({'METRICS_COLLECTOR': {3: 2, 6: 2, 31: 3, "else": 5}})


  def getServiceComponentLayoutValidations(self, services, hosts):
    """
    Get a list of errors.
    Must be overriden in child class.
    """

    return self.getServiceComponentCardinalityValidations(services, hosts, "AMBARI_METRICS")

  def getAmsMemoryRecommendation(self, services, hosts):
    # MB per sink in hbase heapsize
    HEAP_PER_MASTER_COMPONENT = 50
    HEAP_PER_SLAVE_COMPONENT = 10

    schMemoryMap = {
      "HDFS": {
        "NAMENODE": HEAP_PER_MASTER_COMPONENT,
        "SECONDARY_NAMENODE": HEAP_PER_MASTER_COMPONENT,
        "DATANODE": HEAP_PER_SLAVE_COMPONENT
      },
      "YARN": {
        "RESOURCEMANAGER": HEAP_PER_MASTER_COMPONENT,
        "NODEMANAGER": HEAP_PER_SLAVE_COMPONENT,
        "HISTORYSERVER" : HEAP_PER_MASTER_COMPONENT,
        "APP_TIMELINE_SERVER": HEAP_PER_MASTER_COMPONENT
      },
      "HBASE": {
        "HBASE_MASTER": HEAP_PER_MASTER_COMPONENT,
        "HBASE_REGIONSERVER": HEAP_PER_SLAVE_COMPONENT
      },
      "HIVE": {
        "HIVE_METASTORE": HEAP_PER_MASTER_COMPONENT,
        "HIVE_SERVER": HEAP_PER_MASTER_COMPONENT
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
      },
      "ACCUMULO": {
        "ACCUMULO_MASTER": HEAP_PER_MASTER_COMPONENT,
        "ACCUMULO_TSERVER": HEAP_PER_SLAVE_COMPONENT
      },
      "LOGSEARCH": {
        "LOGSEARCH_LOGFEEDER" : HEAP_PER_SLAVE_COMPONENT
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
        hbase_heapsize += int((schCount * multiplier))
        total_sinks_count += schCount
    collector_heapsize = int(hbase_heapsize/3 if hbase_heapsize > 2048 else 512)
    hbase_heapsize = min(hbase_heapsize, 32768)

    return self.round_to_n(collector_heapsize), self.round_to_n(hbase_heapsize), total_sinks_count


  def round_to_n(self, mem_size, n=128):
    return int(round(float(mem_size) / float(n))) * int(n)


  def getServiceConfigurationRecommendations(self, configurations, clusterData, services, hosts):
    """
    Entry point.
    Must be overriden in child class.
    """
    #Logger.info("Class: %s, Method: %s. Recommending Service Configurations." %
    #            (self.__class__.__name__, inspect.stack()[0][3]))

    recommender = AMBARI_METRICSRecommender()
    recommender.recommendAmsConfigurationsFromHDP206(configurations, clusterData, services, hosts)




  def getServiceConfigurationsValidationItems(self, configurations, recommendedDefaults, services, hosts):
    """
    Entry point.
    Validate configurations for the service. Return a list of errors.
    The code for this function should be the same for each Service Advisor.
    """
    #Logger.info("Class: %s, Method: %s. Validating Configurations." %
    #            (self.__class__.__name__, inspect.stack()[0][3]))

    validator = self.getAMBARI_METRICSValidator()
    # Calls the methods of the validator using arguments,
    # method(siteProperties, siteRecommendations, configurations, services, hosts)
    return validator.validateListOfConfigUsingMethod(configurations, recommendedDefaults, services, hosts, validator.validators)


  def getAMBARI_METRICSValidator(self):
    return AMBARI_METRICSValidator()

class AMBARI_METRICSRecommender(service_advisor.ServiceAdvisor):
  """
  AMS Recommender suggests properties when adding the service for the first time or modifying configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(AMBARI_METRICSRecommender, self)
    self.as_super.__init__(*args, **kwargs)



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

  def recommendAmsConfigurationsFromHDP206(self, configurations, clusterData, services, hosts):
    putAmsEnvProperty = self.putProperty(configurations, "ams-env", services)
    putAmsHbaseSiteProperty = self.putProperty(configurations, "ams-hbase-site", services)
    putAmsSiteProperty = self.putProperty(configurations, "ams-site", services)
    putHbaseEnvProperty = self.putProperty(configurations, "ams-hbase-env", services)
    putGrafanaProperty = self.putProperty(configurations, "ams-grafana-env", services)
    putGrafanaPropertyAttribute = self.putPropertyAttribute(configurations, "ams-grafana-env")

    amsCollectorHosts = self.getComponentHostNames(services, "AMBARI_METRICS", "METRICS_COLLECTOR")

    serviceAdvisor = AMBARI_METRICSServiceAdvisor()

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
      putAmsHbaseSiteProperty("hbase.unsafe.stream.capability.enforce", 'true')
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

    collector_heapsize, hbase_heapsize, total_sinks_count = serviceAdvisor.getAmsMemoryRecommendation(services, hosts)

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
      elif total_sinks_count >= 1000:
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

    serviceAdvisor = AMBARI_METRICSServiceAdvisor()

    # Distributed mode heap size
    if operatingMode == "distributed":
      hbase_heapsize = max(hbase_heapsize, 1024)
      putHbaseEnvProperty("hbase_master_heapsize", "512")
      putHbaseEnvProperty("hbase_master_xmn_size", "102") #20% of 512 heap size
      putHbaseEnvProperty("hbase_regionserver_heapsize", hbase_heapsize)
      putHbaseEnvProperty("regionserver_xmn_size", serviceAdvisor.round_to_n(0.15 * hbase_heapsize,64))
    else:
      # Embedded mode heap size : master + regionserver
      hbase_rs_heapsize = 512
      putHbaseEnvProperty("hbase_regionserver_heapsize", hbase_rs_heapsize)
      putHbaseEnvProperty("hbase_master_heapsize", hbase_heapsize)
      putHbaseEnvProperty("hbase_master_xmn_size", serviceAdvisor.round_to_n(0.15*(hbase_heapsize + hbase_rs_heapsize),64))

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

    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

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



class AMBARI_METRICSValidator(service_advisor.ServiceAdvisor):
  """
  AMS Validator checks the correctness of properties whenever the service is first added or the user attempts to
  change configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(AMBARI_METRICSValidator, self)
    self.as_super.__init__(*args, **kwargs)

    self.validators = [("ams-hbase-site", self.validateAmsHbaseSiteConfigurationsFromHDP206),
                       ("ams-hbase-env", self.validateAmsHbaseEnvConfigurationsFromHDP206),
                       ("ams-site", self.validateAmsSiteConfigurationsFromHDP206),
                       ("ams-env", self.validateAmsEnvConfigurationsFromHDP206),
                       ("ams-grafana-env", self.validateGrafanaEnvConfigurationsFromHDP206)]



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

  def validateAmsHbaseSiteConfigurationsFromHDP206(self, properties, recommendedDefaults, configurations, services, hosts):

    amsCollectorHosts = self.getComponentHostNames(services, "AMBARI_METRICS", "METRICS_COLLECTOR")
    ams_site = self.getSiteProperties(configurations, "ams-site")
    core_site = self.getSiteProperties(configurations, "core-site")

    serviceAdvisor = AMBARI_METRICSServiceAdvisor()

    collector_heapsize, hbase_heapsize, total_sinks_count = serviceAdvisor.getAmsMemoryRecommendation(services, hosts)
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
            hdfs_site = self.getSiteProperties(configurations, "hdfs-site")
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


  def validateAmsHbaseEnvConfigurationsFromHDP206(self, properties, recommendedDefaults, configurations, services, hosts):

    ams_env = self.getSiteProperties(configurations, "ams-env")
    amsHbaseSite = self.getSiteProperties(configurations, "ams-hbase-site")
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

      if not regionServerItem and hbase_regionserver_heapsize > 32768:
        regionServerItem = self.getWarnItem("Value is more than the recommended maximum heap size of 32G.")
        validationItems.extend([{"config-name": "hbase_regionserver_heapsize", "item": regionServerItem}])

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

      if not hbaseMasterHeapsizeItem and (hbase_master_heapsize + hbase_regionserver_heapsize) > 32768:
        hbaseMasterHeapsizeItem = self.getWarnItem("Value of Master + Regionserver heapsize is more than the recommended maximum heap size of 32G.")
        validationItems.extend([{"config-name": "hbase_master_heapsize", "item": hbaseMasterHeapsizeItem}])

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
      pass

    return self.toConfigurationValidationProblems(validationItems, "ams-hbase-env")


  def validateAmsSiteConfigurationsFromHDP206(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = []

    serviceAdvisor = AMBARI_METRICSServiceAdvisor()

    op_mode = properties.get("timeline.metrics.service.operation.mode")
    correct_op_mode_item = None
    if op_mode not in ("embedded", "distributed"):
      correct_op_mode_item = self.getErrorItem("Correct value should be set.")
      pass
    elif len(self.getComponentHostNames(services, "AMBARI_METRICS", "METRICS_COLLECTOR")) > 1 and op_mode != 'distributed':
      correct_op_mode_item = self.getErrorItem("Correct value should be 'distributed' for clusters with more then 1 Metrics collector")
    elif op_mode == 'embedded':
      collector_heapsize, hbase_heapsize, total_sinks_count = serviceAdvisor.getAmsMemoryRecommendation(services, hosts)
      if total_sinks_count > 1000:
        correct_op_mode_item = self.getWarnItem("Number of sinks writing metrics to collector is expected to be more than 1000. "
                                                "'Embedded' mode AMS might not be able to handle the load. Consider moving to distributed mode.")

    validationItems.extend([{"config-name":'timeline.metrics.service.operation.mode', "item": correct_op_mode_item }])
    return self.toConfigurationValidationProblems(validationItems, "ams-site")


  def validateAmsEnvConfigurationsFromHDP206(self, properties, recommendedDefaults, configurations, services, hosts):

    validationItems = []
    collectorHeapsizeDefaultItem = self.validatorLessThenDefaultValue(properties, recommendedDefaults, "metrics_collector_heapsize")
    validationItems.extend([{"config-name": "metrics_collector_heapsize", "item": collectorHeapsizeDefaultItem}])

    ams_env = self.getSiteProperties(configurations, "ams-env")
    collector_heapsize = self.to_number(ams_env.get("metrics_collector_heapsize"))
    if collector_heapsize > 32768:
      collectorHeapsizeMaxItem = self.getWarnItem("Value is more than the recommended maximum heap size of 32G.")
      validationItems.extend([{"config-name": "metrics_collector_heapsize", "item": collectorHeapsizeMaxItem}])

    return self.toConfigurationValidationProblems(validationItems, "ams-env")


  def validateGrafanaEnvConfigurationsFromHDP206(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = []

    grafana_pwd = properties.get("metrics_grafana_password")
    grafana_pwd_length_item = None
    if len(grafana_pwd) < 4:
      grafana_pwd_length_item = self.getErrorItem("Grafana password length should be at least 4.")
      pass
    validationItems.extend([{"config-name":'metrics_grafana_password', "item": grafana_pwd_length_item }])
    return self.toConfigurationValidationProblems(validationItems, "ams-site")