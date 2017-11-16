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

class HBASEServiceAdvisor(service_advisor.ServiceAdvisor):

  def __init__(self, *args, **kwargs):
    self.as_super = super(HBASEServiceAdvisor, self)
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
    self.mastersWithMultipleInstances.add("HBASE_MASTER")

  def modifyCardinalitiesDict(self):
    """
    Modify the dictionary of cardinalities.
    Must be overriden in child class.
    """
    self.cardinalitiesDict["HBASE_MASTER"] = {"min": 1}

  def modifyHeapSizeProperties(self):
    """
    Modify the dictionary of heap size properties.
    Must be overriden in child class.
    """
    self.heap_size_properties = {"REGIONSERVER":
                                   [{"config-name": "hbase-env",
                                     "property": "hbase_regionserver_heapsize",
                                     "default": "1024m"}],
                                 "HBASE_MASTER":
                                   [{"config-name": "hbase-env",
                                     "property": "hbase_master_heapsize",
                                     "default": "1024m"}]}

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
      'HBASE_MASTER': {6: 0, 31: 2, "else": 3}
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
    #Logger.info("Class: %s, Method: %s. Recommending Service Configurations." %
    #            (self.__class__.__name__, inspect.stack()[0][3]))

    recommender = HBASERecommender()
    recommender.recommendHbaseConfigurationsFromHDP206(configurations, clusterData, services, hosts)
    recommender.recommendHBASEConfigurationsFromHDP22(configurations, clusterData, services, hosts)
    recommender.recommendHBASEConfigurationsFromHDP23(configurations, clusterData, services, hosts)
    recommender.recommendHBASEConfigurationsFromHDP25(configurations, clusterData, services, hosts)
    recommender.recommendHBASEConfigurationsFromHDP26(configurations, clusterData, services, hosts)


  def getServiceConfigurationsValidationItems(self, configurations, recommendedDefaults, services, hosts):
    """
    Entry point.
    Validate configurations for the service. Return a list of errors.
    The code for this function should be the same for each Service Advisor.
    """
    #Logger.info("Class: %s, Method: %s. Validating Configurations." %
    #            (self.__class__.__name__, inspect.stack()[0][3]))

    validator = HBASEValidator()
    # Calls the methods of the validator using arguments,
    # method(siteProperties, siteRecommendations, configurations, services, hosts)
    return validator.validateListOfConfigUsingMethod(configurations, recommendedDefaults, services, hosts, validator.validators)



class HBASERecommender(service_advisor.ServiceAdvisor):
  """
  HBASE Recommender suggests properties when adding the service for the first time or modifying configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(HBASERecommender, self)
    self.as_super.__init__(*args, **kwargs)


  def recommendHbaseConfigurationsFromHDP206(self, configurations, clusterData, services, hosts):
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


  def recommendHBASEConfigurationsFromHDP22(self, configurations, clusterData, services, hosts):
    putHbaseEnvPropertyAttributes = self.putPropertyAttribute(configurations, "hbase-env")

    hmaster_host = self.getHostWithComponent("HBASE", "HBASE_MASTER", services, hosts)
    if hmaster_host is not None:
      host_ram = hmaster_host["Hosts"]["total_mem"]
      putHbaseEnvPropertyAttributes('hbase_master_heapsize', 'maximum', max(1024, int(host_ram/1024)))

    rs_hosts = self.getHostsWithComponent("HBASE", "HBASE_REGIONSERVER", services, hosts)
    if rs_hosts is not None and len(rs_hosts) > 0:
      min_ram = rs_hosts[0]["Hosts"]["total_mem"]
      for host in rs_hosts:
        host_ram = host["Hosts"]["total_mem"]
        min_ram = min(min_ram, host_ram)

      putHbaseEnvPropertyAttributes('hbase_regionserver_heapsize', 'maximum', max(1024, int(min_ram*0.8/1024)))

    putHbaseSiteProperty = self.putProperty(configurations, "hbase-site", services)
    putHbaseSitePropertyAttributes = self.putPropertyAttribute(configurations, "hbase-site")
    putHbaseSiteProperty("hbase.regionserver.global.memstore.size", '0.4')

    if 'hbase-env' in services['configurations'] and 'phoenix_sql_enabled' in services['configurations']['hbase-env']['properties'] and \
                    'true' == services['configurations']['hbase-env']['properties']['phoenix_sql_enabled'].lower():
      putHbaseSiteProperty("hbase.regionserver.wal.codec", 'org.apache.hadoop.hbase.regionserver.wal.IndexedWALEditCodec')
      putHbaseSiteProperty("phoenix.functions.allowUserDefinedFunctions", 'true')
    else:
      putHbaseSiteProperty("hbase.regionserver.wal.codec", 'org.apache.hadoop.hbase.regionserver.wal.WALCellCodec')
      if ('hbase.rpc.controllerfactory.class' in configurations["hbase-site"]["properties"]) or \
              ('hbase-site' in services['configurations'] and 'hbase.rpc.controllerfactory.class' in services['configurations']["hbase-site"]["properties"]):
        putHbaseSitePropertyAttributes('hbase.rpc.controllerfactory.class', 'delete', 'true')
      if ('phoenix.functions.allowUserDefinedFunctions' in configurations["hbase-site"]["properties"]) or \
              ('hbase-site' in services['configurations'] and 'phoenix.functions.allowUserDefinedFunctions' in services['configurations']["hbase-site"]["properties"]):
        putHbaseSitePropertyAttributes('phoenix.functions.allowUserDefinedFunctions', 'delete', 'true')

    if "ranger-env" in services["configurations"] and "ranger-hbase-plugin-properties" in services["configurations"] and \
                    "ranger-hbase-plugin-enabled" in services["configurations"]["ranger-env"]["properties"]:
      putHbaseRangerPluginProperty = self.putProperty(configurations, "ranger-hbase-plugin-properties", services)
      rangerEnvHbasePluginProperty = services["configurations"]["ranger-env"]["properties"]["ranger-hbase-plugin-enabled"]
      putHbaseRangerPluginProperty("ranger-hbase-plugin-enabled", rangerEnvHbasePluginProperty)
      if "cluster-env" in services["configurations"] and "smokeuser" in services["configurations"]["cluster-env"]["properties"]:
        smoke_user = services["configurations"]["cluster-env"]["properties"]["smokeuser"]
        putHbaseRangerPluginProperty("policy_user", smoke_user)
    rangerPluginEnabled = ''
    if 'ranger-hbase-plugin-properties' in configurations and 'ranger-hbase-plugin-enabled' in  configurations['ranger-hbase-plugin-properties']['properties']:
      rangerPluginEnabled = configurations['ranger-hbase-plugin-properties']['properties']['ranger-hbase-plugin-enabled']
    elif 'ranger-hbase-plugin-properties' in services['configurations'] and 'ranger-hbase-plugin-enabled' in services['configurations']['ranger-hbase-plugin-properties']['properties']:
      rangerPluginEnabled = services['configurations']['ranger-hbase-plugin-properties']['properties']['ranger-hbase-plugin-enabled']

    if rangerPluginEnabled and rangerPluginEnabled.lower() == 'Yes'.lower():
      putHbaseSiteProperty('hbase.security.authorization','true')

    # Recommend configs for bucket cache
    threshold = 23 # 2 Gb is reserved for other offheap memory
    mb = 1024
    if (int(clusterData["hbaseRam"]) > threshold):
      # To enable cache - calculate values
      regionserver_total_ram = int(clusterData["hbaseRam"]) * mb
      regionserver_heap_size = 20480
      regionserver_max_direct_memory_size = regionserver_total_ram - regionserver_heap_size
      hfile_block_cache_size = '0.4'
      block_cache_heap = 8192 # int(regionserver_heap_size * hfile_block_cache_size)
      hbase_regionserver_global_memstore_size = '0.4'
      reserved_offheap_memory = 2048
      bucketcache_offheap_memory = regionserver_max_direct_memory_size - reserved_offheap_memory
      hbase_bucketcache_size = bucketcache_offheap_memory
      hbase_bucketcache_percentage_in_combinedcache = float(bucketcache_offheap_memory) / hbase_bucketcache_size
      hbase_bucketcache_percentage_in_combinedcache_str = "{0:.4f}".format(math.ceil(hbase_bucketcache_percentage_in_combinedcache * 10000) / 10000.0)

      # Set values in hbase-site
      putHbaseSiteProperty('hfile.block.cache.size', hfile_block_cache_size)
      putHbaseSiteProperty('hbase.regionserver.global.memstore.size', hbase_regionserver_global_memstore_size)
      putHbaseSiteProperty('hbase.bucketcache.ioengine', 'offheap')
      putHbaseSiteProperty('hbase.bucketcache.size', hbase_bucketcache_size)
      putHbaseSiteProperty('hbase.bucketcache.percentage.in.combinedcache', hbase_bucketcache_percentage_in_combinedcache_str)

      # Enable in hbase-env
      putHbaseEnvProperty = self.putProperty(configurations, "hbase-env", services)
      putHbaseEnvProperty('hbase_max_direct_memory_size', regionserver_max_direct_memory_size)
      putHbaseEnvProperty('hbase_regionserver_heapsize', regionserver_heap_size)
    else:
      # Disable
      if ('hbase.bucketcache.ioengine' in configurations["hbase-site"]["properties"]) or \
              ('hbase-site' in services['configurations'] and 'hbase.bucketcache.ioengine' in services['configurations']["hbase-site"]["properties"]):
        putHbaseSitePropertyAttributes('hbase.bucketcache.ioengine', 'delete', 'true')
      if ('hbase.bucketcache.size' in configurations["hbase-site"]["properties"]) or \
              ('hbase-site' in services['configurations'] and 'hbase.bucketcache.size' in services['configurations']["hbase-site"]["properties"]):
        putHbaseSitePropertyAttributes('hbase.bucketcache.size', 'delete', 'true')
      if ('hbase.bucketcache.percentage.in.combinedcache' in configurations["hbase-site"]["properties"]) or \
              ('hbase-site' in services['configurations'] and 'hbase.bucketcache.percentage.in.combinedcache' in services['configurations']["hbase-site"]["properties"]):
        putHbaseSitePropertyAttributes('hbase.bucketcache.percentage.in.combinedcache', 'delete', 'true')
      if ('hbase_max_direct_memory_size' in configurations["hbase-env"]["properties"]) or \
              ('hbase-env' in services['configurations'] and 'hbase_max_direct_memory_size' in services['configurations']["hbase-env"]["properties"]):
        putHbaseEnvPropertyAttributes('hbase_max_direct_memory_size', 'delete', 'true')

    # Authorization
    hbaseCoProcessorConfigs = {
      'hbase.coprocessor.region.classes': [],
      'hbase.coprocessor.regionserver.classes': [],
      'hbase.coprocessor.master.classes': []
    }
    for key in hbaseCoProcessorConfigs:
      hbase_coprocessor_classes = None
      if key in configurations["hbase-site"]["properties"]:
        hbase_coprocessor_classes = configurations["hbase-site"]["properties"][key].strip()
      elif 'hbase-site' in services['configurations'] and key in services['configurations']["hbase-site"]["properties"]:
        hbase_coprocessor_classes = services['configurations']["hbase-site"]["properties"][key].strip()
      if hbase_coprocessor_classes:
        hbaseCoProcessorConfigs[key] = hbase_coprocessor_classes.split(',')

    # If configurations has it - it has priority as it is calculated. Then, the service's configurations will be used.
    hbase_security_authorization = None
    if 'hbase-site' in configurations and 'hbase.security.authorization' in configurations['hbase-site']['properties']:
      hbase_security_authorization = configurations['hbase-site']['properties']['hbase.security.authorization']
    elif 'hbase-site' in services['configurations'] and 'hbase.security.authorization' in services['configurations']['hbase-site']['properties']:
      hbase_security_authorization = services['configurations']['hbase-site']['properties']['hbase.security.authorization']
    if hbase_security_authorization:
      if 'true' == hbase_security_authorization.lower():
        hbaseCoProcessorConfigs['hbase.coprocessor.master.classes'].append('org.apache.hadoop.hbase.security.access.AccessController')
        hbaseCoProcessorConfigs['hbase.coprocessor.regionserver.classes'].append('org.apache.hadoop.hbase.security.access.AccessController')
        # regional classes when hbase authorization is enabled
        authRegionClasses = ['org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint', 'org.apache.hadoop.hbase.security.access.AccessController']
        for item in range(len(authRegionClasses)):
          hbaseCoProcessorConfigs['hbase.coprocessor.region.classes'].append(authRegionClasses[item])
      else:
        if 'org.apache.hadoop.hbase.security.access.AccessController' in hbaseCoProcessorConfigs['hbase.coprocessor.region.classes']:
          hbaseCoProcessorConfigs['hbase.coprocessor.region.classes'].remove('org.apache.hadoop.hbase.security.access.AccessController')
        if 'org.apache.hadoop.hbase.security.access.AccessController' in hbaseCoProcessorConfigs['hbase.coprocessor.master.classes']:
          hbaseCoProcessorConfigs['hbase.coprocessor.master.classes'].remove('org.apache.hadoop.hbase.security.access.AccessController')

        hbaseCoProcessorConfigs['hbase.coprocessor.region.classes'].append("org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint")
        if ('hbase.coprocessor.regionserver.classes' in configurations["hbase-site"]["properties"]) or \
                ('hbase-site' in services['configurations'] and 'hbase.coprocessor.regionserver.classes' in services['configurations']["hbase-site"]["properties"]):
          putHbaseSitePropertyAttributes('hbase.coprocessor.regionserver.classes', 'delete', 'true')
    else:
      hbaseCoProcessorConfigs['hbase.coprocessor.region.classes'].append("org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint")
      if ('hbase.coprocessor.regionserver.classes' in configurations["hbase-site"]["properties"]) or \
              ('hbase-site' in services['configurations'] and 'hbase.coprocessor.regionserver.classes' in services['configurations']["hbase-site"]["properties"]):
        putHbaseSitePropertyAttributes('hbase.coprocessor.regionserver.classes', 'delete', 'true')

    # Authentication
    if 'hbase-site' in services['configurations'] and 'hbase.security.authentication' in services['configurations']['hbase-site']['properties']:
      if 'kerberos' == services['configurations']['hbase-site']['properties']['hbase.security.authentication'].lower():
        if 'org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint' not in hbaseCoProcessorConfigs['hbase.coprocessor.region.classes']:
          hbaseCoProcessorConfigs['hbase.coprocessor.region.classes'].append('org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint')
        if 'org.apache.hadoop.hbase.security.token.TokenProvider' not in hbaseCoProcessorConfigs['hbase.coprocessor.region.classes']:
          hbaseCoProcessorConfigs['hbase.coprocessor.region.classes'].append('org.apache.hadoop.hbase.security.token.TokenProvider')
      else:
        if 'org.apache.hadoop.hbase.security.token.TokenProvider' in hbaseCoProcessorConfigs['hbase.coprocessor.region.classes']:
          hbaseCoProcessorConfigs['hbase.coprocessor.region.classes'].remove('org.apache.hadoop.hbase.security.token.TokenProvider')

    #Remove duplicates
    for key in hbaseCoProcessorConfigs:
      uniqueCoprocessorRegionClassList = []
      [uniqueCoprocessorRegionClassList.append(i)
       for i in hbaseCoProcessorConfigs[key] if
       not i in uniqueCoprocessorRegionClassList
       and (i.strip() not in ['{{hbase_coprocessor_region_classes}}', '{{hbase_coprocessor_master_classes}}', '{{hbase_coprocessor_regionserver_classes}}'])]
      putHbaseSiteProperty(key, ','.join(set(uniqueCoprocessorRegionClassList)))


    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    rangerServiceVersion=''
    if 'RANGER' in servicesList:
      rangerServiceVersion = [service['StackServices']['service_version'] for service in services["services"] if service['StackServices']['service_name'] == 'RANGER'][0]

    if rangerServiceVersion and rangerServiceVersion == '0.4.0':
      rangerClass = 'com.xasecure.authorization.hbase.XaSecureAuthorizationCoprocessor'
    else:
      rangerClass = 'org.apache.ranger.authorization.hbase.RangerAuthorizationCoprocessor'

    nonRangerClass = 'org.apache.hadoop.hbase.security.access.AccessController'
    hbaseClassConfigs =  hbaseCoProcessorConfigs.keys()

    for item in range(len(hbaseClassConfigs)):
      if 'hbase-site' in services['configurations']:
        if hbaseClassConfigs[item] in services['configurations']['hbase-site']['properties']:
          if 'hbase-site' in configurations and hbaseClassConfigs[item] in configurations['hbase-site']['properties']:
            coprocessorConfig = configurations['hbase-site']['properties'][hbaseClassConfigs[item]]
          else:
            coprocessorConfig = services['configurations']['hbase-site']['properties'][hbaseClassConfigs[item]]
          coprocessorClasses = coprocessorConfig.split(",")
          coprocessorClasses = filter(None, coprocessorClasses) # Removes empty string elements from array
          if rangerPluginEnabled and rangerPluginEnabled.lower() == 'Yes'.lower():
            if nonRangerClass in coprocessorClasses:
              coprocessorClasses.remove(nonRangerClass)
            if not rangerClass in coprocessorClasses:
              coprocessorClasses.append(rangerClass)
            putHbaseSiteProperty(hbaseClassConfigs[item], ','.join(coprocessorClasses))
          elif rangerPluginEnabled and rangerPluginEnabled.lower() == 'No'.lower():
            if rangerClass in coprocessorClasses:
              coprocessorClasses.remove(rangerClass)
              if not nonRangerClass in coprocessorClasses:
                coprocessorClasses.append(nonRangerClass)
              putHbaseSiteProperty(hbaseClassConfigs[item], ','.join(coprocessorClasses))
        elif rangerPluginEnabled and rangerPluginEnabled.lower() == 'Yes'.lower():
          putHbaseSiteProperty(hbaseClassConfigs[item], rangerClass)


  def recommendHBASEConfigurationsFromHDP23(self, configurations, clusterData, services, hosts):
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


  def recommendHBASEConfigurationsFromHDP25(self, configurations, clusterData, services, hosts):
    putHbaseSiteProperty = self.putProperty(configurations, "hbase-site", services)
    putCoreSiteProperty = self.putProperty(configurations, "core-site", services)

    if "cluster-env" in services["configurations"] \
      and "security_enabled" in services["configurations"]["cluster-env"]["properties"] \
      and services["configurations"]["cluster-env"]["properties"]["security_enabled"].lower() == "true":
      # Set the master's UI to readonly
      putHbaseSiteProperty('hbase.master.ui.readonly', 'true')

      phoenix_query_server_hosts = self.get_phoenix_query_server_hosts(services, hosts)
      self.logger.debug("Calculated Phoenix Query Server hosts: %s" % str(phoenix_query_server_hosts))
      if phoenix_query_server_hosts:
        self.logger.debug("Attempting to update hadoop.proxyuser.HTTP.hosts with %s" % str(phoenix_query_server_hosts))
        # The PQS hosts we want to ensure are set
        new_value = ','.join(phoenix_query_server_hosts)
        # Update the proxyuser setting, deferring to out callback to merge results together
        self.put_proxyuser_value("HTTP", new_value, services=services, configurations=configurations, put_function=putCoreSiteProperty)
      else:
        self.logger.debug("No phoenix query server hosts to update")
    else:
      putHbaseSiteProperty('hbase.master.ui.readonly', 'false')

  """
  Returns the list of Phoenix Query Server host names, or None.
  """
  def get_phoenix_query_server_hosts(self, services, hosts):
    if len(hosts['items']) > 0:
      phoenix_query_server_hosts = self.getHostsWithComponent("HBASE", "PHOENIX_QUERY_SERVER", services, hosts)
      if phoenix_query_server_hosts is None:
        return []
      return [host['Hosts']['host_name'] for host in phoenix_query_server_hosts]


  def recommendHBASEConfigurationsFromHDP26(self, configurations, clusterData, services, hosts):
    if 'hbase-env' in services['configurations'] and 'hbase_user' in services['configurations']['hbase-env']['properties']:
      hbase_user = services['configurations']['hbase-env']['properties']['hbase_user']
    else:
      hbase_user = 'hbase'

    if 'ranger-hbase-plugin-properties' in configurations and 'ranger-hbase-plugin-enabled' in configurations['ranger-hbase-plugin-properties']['properties']:
      ranger_hbase_plugin_enabled = (configurations['ranger-hbase-plugin-properties']['properties']['ranger-hbase-plugin-enabled'].lower() == 'Yes'.lower())
    elif 'ranger-hbase-plugin-properties' in services['configurations'] and 'ranger-hbase-plugin-enabled' in services['configurations']['ranger-hbase-plugin-properties']['properties']:
      ranger_hbase_plugin_enabled = (services['configurations']['ranger-hbase-plugin-properties']['properties']['ranger-hbase-plugin-enabled'].lower() == 'Yes'.lower())
    else:
      ranger_hbase_plugin_enabled = False

    if ranger_hbase_plugin_enabled and 'ranger-hbase-plugin-properties' in services['configurations'] and 'REPOSITORY_CONFIG_USERNAME' in services['configurations']['ranger-hbase-plugin-properties']['properties']:
      self.logger.info("Setting Hbase Repo user for Ranger.")
      putRangerHbasePluginProperty = self.putProperty(configurations, "ranger-hbase-plugin-properties", services)
      putRangerHbasePluginProperty("REPOSITORY_CONFIG_USERNAME",hbase_user)
    else:
      self.logger.info("Not setting Hbase Repo user for Ranger.")


class HBASEValidator(service_advisor.ServiceAdvisor):
  """
  HBASE Validator checks the correctness of properties whenever the service is first added or the user attempts to
  change configs via the UI.
  """

  def __init__(self, *args, **kwargs):
    self.as_super = super(HBASEValidator, self)
    self.as_super.__init__(*args, **kwargs)

    self.validators = [("hbase-env", self.validateHbaseEnvConfigurationsFromHDP206),
                       ("hbase-site", self.validateHBASEConfigurationsFromHDP22),
                       ("hbase-env", self.validateHBASEEnvConfigurationsFromHDP22),
                       ("ranger-hbase-plugin-properties", self.validateHBASERangerPluginConfigurationsFromHDP22),
                       ("hbase-site", self.validateHBASEConfigurationsFromHDP23)]


  def validateHbaseEnvConfigurationsFromHDP206(self, properties, recommendedDefaults, configurations, services, hosts):
    hbase_site = self.getSiteProperties(configurations, "hbase-site")
    validationItems = [ {"config-name": 'hbase_regionserver_heapsize', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'hbase_regionserver_heapsize')},
                        {"config-name": 'hbase_master_heapsize', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'hbase_master_heapsize')},
                        {"config-name": "hbase_user", "item": self.validatorEqualsPropertyItem(properties, "hbase_user", hbase_site, "hbase.superuser")} ]
    return self.toConfigurationValidationProblems(validationItems, "hbase-env")

  def is_number(self, s):
    try:
      float(s)
      return True
    except ValueError:
      pass


  def validateHBASEConfigurationsFromHDP22(self, properties, recommendedDefaults, configurations, services, hosts):
    hbase_site = properties
    validationItems = []

    prop_name1 = 'hbase.regionserver.global.memstore.size'
    prop_name2 = 'hfile.block.cache.size'
    props_max_sum = 0.8

    if prop_name1 in hbase_site and not self.is_number(hbase_site[prop_name1]):
      validationItems.append({"config-name": prop_name1,
                              "item": self.getWarnItem(
                                "{0} should be float value".format(prop_name1))})
    elif prop_name2 in hbase_site and not self.is_number(hbase_site[prop_name2]):
      validationItems.append({"config-name": prop_name2,
                              "item": self.getWarnItem(
                                "{0} should be float value".format(prop_name2))})
    elif prop_name1 in hbase_site and prop_name2 in hbase_site and \
                            float(hbase_site[prop_name1]) + float(hbase_site[prop_name2]) > props_max_sum:
      validationItems.append({"config-name": prop_name1,
                              "item": self.getWarnItem(
                                "{0} and {1} sum should not exceed {2}".format(prop_name1, prop_name2, props_max_sum))})

    #Adding Ranger Plugin logic here
    ranger_plugin_properties = self.getSiteProperties(configurations, "ranger-hbase-plugin-properties")
    ranger_plugin_enabled = ranger_plugin_properties['ranger-hbase-plugin-enabled'] if ranger_plugin_properties else 'No'
    prop_name = 'hbase.security.authorization'
    prop_val = "true"
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if ("RANGER" in servicesList) and (ranger_plugin_enabled.lower() == 'Yes'.lower()):
      if hbase_site[prop_name] != prop_val:
        validationItems.append({"config-name": prop_name,
                                "item": self.getWarnItem(
                                  "If Ranger HBase Plugin is enabled." \
                                  "{0} needs to be set to {1}".format(prop_name,prop_val))})
      prop_name = "hbase.coprocessor.master.classes"
      prop_val = "com.xasecure.authorization.hbase.XaSecureAuthorizationCoprocessor"
      exclude_val = "org.apache.hadoop.hbase.security.access.AccessController"
      if (prop_val in hbase_site[prop_name] and exclude_val not in hbase_site[prop_name]):
        pass
      else:
        validationItems.append({"config-name": prop_name,
                                "item": self.getWarnItem(
                                  "If Ranger HBase Plugin is enabled." \
                                  " {0} needs to contain {1} instead of {2}".format(prop_name,prop_val,exclude_val))})
      prop_name = "hbase.coprocessor.region.classes"
      prop_val = "com.xasecure.authorization.hbase.XaSecureAuthorizationCoprocessor"
      if (prop_val in hbase_site[prop_name] and exclude_val not in hbase_site[prop_name]):
        pass
      else:
        validationItems.append({"config-name": prop_name,
                                "item": self.getWarnItem(
                                  "If Ranger HBase Plugin is enabled." \
                                  " {0} needs to contain {1} instead of {2}".format(prop_name,prop_val,exclude_val))})

    # Validate bucket cache correct config
    prop_name = "hbase.bucketcache.ioengine"
    prop_val = "offheap"
    if prop_name in hbase_site and not (not hbase_site[prop_name] or hbase_site[prop_name] == prop_val):
      validationItems.append({"config-name": prop_name,
                              "item": self.getWarnItem(
                                "Recommended values of " \
                                " {0} is empty or '{1}'".format(prop_name,prop_val))})

    prop_name1 = "hbase.bucketcache.ioengine"
    prop_name2 = "hbase.bucketcache.size"
    prop_name3 = "hbase.bucketcache.percentage.in.combinedcache"

    if prop_name1 in hbase_site and prop_name2 in hbase_site and hbase_site[prop_name1] and not hbase_site[prop_name2]:
      validationItems.append({"config-name": prop_name2,
                              "item": self.getWarnItem(
                                "If bucketcache ioengine is enabled, {0} should be set".format(prop_name2))})
    if prop_name1 in hbase_site and prop_name3 in hbase_site and hbase_site[prop_name1] and not hbase_site[prop_name3]:
      validationItems.append({"config-name": prop_name3,
                              "item": self.getWarnItem(
                                "If bucketcache ioengine is enabled, {0} should be set".format(prop_name3))})

    # Validate hbase.security.authentication.
    # Kerberos works only when security enabled.
    if "hbase.security.authentication" in properties:
      hbase_security_kerberos = properties["hbase.security.authentication"].lower() == "kerberos"
      core_site_properties = self.getSiteProperties(configurations, "core-site")
      security_enabled = False
      if core_site_properties:
        security_enabled = core_site_properties['hadoop.security.authentication'] == 'kerberos' and core_site_properties['hadoop.security.authorization'] == 'true'
      if not security_enabled and hbase_security_kerberos:
        validationItems.append({"config-name": "hbase.security.authentication",
                                "item": self.getWarnItem("Cluster must be secured with Kerberos before hbase.security.authentication's value of kerberos will have effect")})

    return self.toConfigurationValidationProblems(validationItems, "hbase-site")

  def validateHBASEEnvConfigurationsFromHDP22(self, properties, recommendedDefaults, configurations, services, hosts):
    hbase_env = properties
    validationItems = [ {"config-name": 'hbase_regionserver_heapsize', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'hbase_regionserver_heapsize')},
                        {"config-name": 'hbase_master_heapsize', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'hbase_master_heapsize')} ]
    prop_name = "hbase_max_direct_memory_size"
    hbase_site_properties = self.getSiteProperties(configurations, "hbase-site")
    prop_name1 = "hbase.bucketcache.ioengine"

    if prop_name1 in hbase_site_properties and prop_name in hbase_env and hbase_site_properties[prop_name1] and hbase_site_properties[prop_name1] == "offheap" and not hbase_env[prop_name]:
      validationItems.append({"config-name": prop_name,
                              "item": self.getWarnItem(
                                "If bucketcache ioengine is enabled, {0} should be set".format(prop_name))})

    return self.toConfigurationValidationProblems(validationItems, "hbase-env")

  def validateHBASERangerPluginConfigurationsFromHDP22(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = []
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    ranger_plugin_properties = self.getSiteProperties(configurations, "ranger-hbase-plugin-properties")
    ranger_plugin_enabled = ranger_plugin_properties['ranger-hbase-plugin-enabled'] if ranger_plugin_properties else 'No'
    if 'RANGER' in servicesList and ranger_plugin_enabled.lower() == 'yes':
      # ranger-hdfs-plugin must be enabled in ranger-env
      ranger_env = self.getServicesSiteProperties(services, 'ranger-env')
      if not ranger_env or not 'ranger-hbase-plugin-enabled' in ranger_env or \
                      ranger_env['ranger-hbase-plugin-enabled'].lower() != 'yes':
        validationItems.append({"config-name": 'ranger-hbase-plugin-enabled',
                                "item": self.getWarnItem(
                                  "ranger-hbase-plugin-properties/ranger-hbase-plugin-enabled must correspond ranger-env/ranger-hbase-plugin-enabled")})
    return self.toConfigurationValidationProblems(validationItems, "ranger-hbase-plugin-properties")

  def validateHBASEConfigurationsFromHDP23(self, properties, recommendedDefaults, configurations, services, hosts):
    hbase_site = properties
    validationItems = []

    #Adding Ranger Plugin logic here
    ranger_plugin_properties = self.getSiteProperties(configurations, "ranger-hbase-plugin-properties")
    ranger_plugin_enabled = ranger_plugin_properties['ranger-hbase-plugin-enabled'] if ranger_plugin_properties else 'No'
    prop_name = 'hbase.security.authorization'
    prop_val = "true"
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if ("RANGER" in servicesList) and (ranger_plugin_enabled.lower() == 'Yes'.lower()):
      if hbase_site[prop_name] != prop_val:
        validationItems.append({"config-name": prop_name,
                                "item": self.getWarnItem(
                                  "If Ranger HBase Plugin is enabled." \
                                  "{0} needs to be set to {1}".format(prop_name,prop_val))})
      prop_name = "hbase.coprocessor.master.classes"
      prop_val = "org.apache.ranger.authorization.hbase.RangerAuthorizationCoprocessor"
      exclude_val = "org.apache.hadoop.hbase.security.access.AccessController"
      if (prop_val in hbase_site[prop_name] and exclude_val not in hbase_site[prop_name]):
        pass
      else:
        validationItems.append({"config-name": prop_name,
                                "item": self.getWarnItem(
                                  "If Ranger HBase Plugin is enabled." \
                                  " {0} needs to contain {1} instead of {2}".format(prop_name,prop_val,exclude_val))})
      prop_name = "hbase.coprocessor.region.classes"
      prop_val = "org.apache.ranger.authorization.hbase.RangerAuthorizationCoprocessor"
      if (prop_val in hbase_site[prop_name] and exclude_val not in hbase_site[prop_name]):
        pass
      else:
        validationItems.append({"config-name": prop_name,
                                "item": self.getWarnItem(
                                  "If Ranger HBase Plugin is enabled." \
                                  " {0} needs to contain {1} instead of {2}".format(prop_name,prop_val,exclude_val))})

    validationProblems = self.toConfigurationValidationProblems(validationItems, "hbase-site")
    return validationProblems
