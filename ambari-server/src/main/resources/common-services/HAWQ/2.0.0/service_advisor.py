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
import imp
import math
import os
import re
import socket
import traceback

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
STACKS_DIR = os.path.join(SCRIPT_DIR, '../../../stacks/')
PARENT_FILE = os.path.join(STACKS_DIR, 'service_advisor.py')

try:
  with open(PARENT_FILE, 'rb') as fp:
    service_advisor = imp.load_module('service_advisor', fp, PARENT_FILE, ('.py', 'rb', imp.PY_SOURCE))
except Exception as e:
  traceback.print_exc()
  print "Failed to load parent"

class HAWQ200ServiceAdvisor(service_advisor.ServiceAdvisor):

  def __init__(self, *args, **kwargs):
    self.as_super = super(HAWQ200ServiceAdvisor, self)
    self.as_super.__init__(*args, **kwargs)

  def getHostsForMasterComponent(self, services, hosts, component, hostsList):
    if component["StackServiceComponents"]["component_name"] == 'HAWQSTANDBY':
      # Do not recommend HAWQSTANDBY on single node cluster, or cluster with no active hosts
      if len(hostsList) <= 1:
        return []
      
      componentsListList = [service["components"] for service in services["services"]]
      componentsList = [item["StackServiceComponents"] for sublist in componentsListList for item in sublist]
      hawqMasterHosts = self.getHosts(componentsList, "HAWQMASTER")
      hawqStandbyHosts = self.getHosts(componentsList, "HAWQSTANDBY")

      # if HAWQMASTER has already been assigned and HAWQSTANDBY has not been assigned, try to ensure HAWQSTANDBY is not placed on the same host
      if len(hawqMasterHosts) > 0 and len(hawqStandbyHosts) == 0:
        ambariServerHost = socket.getfqdn()
        availableHosts = [host for host in hostsList if host not in (hawqMasterHosts[0], ambariServerHost)]
        # Return list containing first available host if there are available hosts
        if len(availableHosts) > 0:
          return availableHosts[:1]
        return [ambariServerHost]

    return self.as_super.getHostsForMasterComponent(services, hosts, component, hostsList)

  def getNotPreferableOnServerComponents(self):
    return ['HAWQMASTER', 'HAWQSTANDBY']

  def getComponentLayoutSchemes(self):
    return {
      'HAWQMASTER': {6: 2, 31: 1, "else": 5},
      'HAWQSTANDBY': {6: 1, 31: 2, "else": 3}
    }

  def colocateService(self, hostsComponentsMap, serviceComponents):
    # colocate HAWQSEGMENT with DATANODE, if no hosts have been allocated for HAWQSEGMENT
    hawqSegment = [component for component in serviceComponents if component["StackServiceComponents"]["component_name"] == "HAWQSEGMENT"][0]
    if not self.isComponentHostsPopulated(hawqSegment):
      for hostName in hostsComponentsMap.keys():
        hostComponents = hostsComponentsMap[hostName]
        if {"name": "DATANODE"} in hostComponents and {"name": "HAWQSEGMENT"} not in hostComponents:
          hostsComponentsMap[hostName].append( { "name": "HAWQSEGMENT" } )
        if {"name": "DATANODE"} not in hostComponents and {"name": "HAWQSEGMENT"} in hostComponents:
          hostComponents.remove({"name": "HAWQSEGMENT"})

  def getServiceComponentLayoutValidations(self, services, hosts):
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item["StackServiceComponents"] for sublist in componentsListList for item in sublist]
    hostsList = [host["Hosts"]["host_name"] for host in hosts["items"]]
    hostsCount = len(hostsList)

    hawqMasterHosts = self.getHosts(componentsList, "HAWQMASTER")
    hawqStandbyHosts = self.getHosts(componentsList, "HAWQSTANDBY")
    hawqSegmentHosts = self.getHosts(componentsList, "HAWQSEGMENT")
    datanodeHosts = self.getHosts(componentsList, "DATANODE")

    items = []

    # Generate WARNING if any HAWQSEGMENT is not colocated with a DATANODE
    mismatchHosts = sorted(set(hawqSegmentHosts).symmetric_difference(set(datanodeHosts)))
    if len(mismatchHosts) > 0:
      hostsString = ', '.join(mismatchHosts)
      message = "HAWQ Segment must be installed on all DataNodes. " \
                  "The following {0} host(s) do not satisfy the colocation recommendation: {1}".format(len(mismatchHosts), hostsString)
      items.append( { "type": 'host-component', "level": 'WARN', "message": message, "component-name": 'HAWQSEGMENT' } )

    # single node case is not analyzed because HAWQ Standby Master will not be present in single node topology due to logic in createComponentLayoutRecommendations()
    if len(hawqMasterHosts) == 1 and len(hawqStandbyHosts) == 1 and hawqMasterHosts == hawqStandbyHosts:
      message = "HAWQ Master and HAWQ Standby Master cannot be deployed on the same host."
      items.append( { "type": 'host-component', "level": 'ERROR', "message": message, "component-name": 'HAWQSTANDBY', "host": hawqStandbyHosts[0] } )

    if len(hawqMasterHosts) ==  1 and hostsCount > 1 and self.isLocalHost(hawqMasterHosts[0]):
      message = "The default Postgres port (5432) on the Ambari Server conflicts with the default HAWQ Masters port. " \
                "If you are using port 5432 for Postgres, you must either deploy the HAWQ Master on a different host " \
                "or configure a different port for the HAWQ Masters in the HAWQ Configuration page."
      items.append( { "type": 'host-component', "level": 'WARN', "message": message, "component-name": 'HAWQMASTER', "host": hawqMasterHosts[0] } )

    if len(hawqStandbyHosts) ==  1 and hostsCount > 1 and self.isLocalHost(hawqStandbyHosts[0]):
      message = "The default Postgres port (5432) on the Ambari Server conflicts with the default HAWQ Masters port. " \
                "If you are using port 5432 for Postgres, you must either deploy the HAWQ Standby Master on a different host " \
                "or configure a different port for the HAWQ Masters in the HAWQ Configuration page."
      items.append( { "type": 'host-component', "level": 'WARN', "message": message, "component-name": 'HAWQSTANDBY', "host": hawqStandbyHosts[0] } )

    return items

  def isHawqMasterComponentOnAmbariServer(self, services):
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item for sublist in componentsListList for item in sublist]
    hawqMasterComponentHosts = [hostname for component in componentsList if component["StackServiceComponents"]["component_name"] in ("HAWQMASTER", "HAWQSTANDBY") for hostname in component["StackServiceComponents"]["hostnames"]]
    return any([self.isLocalHost(host) for host in hawqMasterComponentHosts])

  def getServiceConfigurationRecommendations(self, configurations, clusterData, services, hosts):

    # Update HDFS properties in hdfs-site
    if "hdfs-site" in services["configurations"]:
      hdfs_site = services["configurations"]["hdfs-site"]["properties"]
      putHdfsSiteProperty = self.putProperty(configurations, "hdfs-site", services)

      for property, desired_value in self.getHDFSSiteDesiredValues(self.isSecurityEnabled(services)).iteritems():
        if property not in hdfs_site or hdfs_site[property] != desired_value:
          putHdfsSiteProperty(property, desired_value)

    # Update HDFS properties in core-site
    if "core-site" in services["configurations"]:
      core_site = services["configurations"]["core-site"]["properties"]
      putCoreSiteProperty = self.putProperty(configurations, "core-site", services)

      for property, desired_value in self.getCORESiteDesiredValues().iteritems():
        if property not in core_site or core_site[property] != desired_value:
          putCoreSiteProperty(property, desired_value)

    # Process HAWQ specific properties
    if all(x in services["configurations"] for x in ["hawq-site", "hdfs-client", "hawq-sysctl-env"]):
      componentsListList = [service["components"] for service in services["services"]]
      componentsList = [item["StackServiceComponents"] for sublist in componentsListList for item in sublist]
      servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

      hawqMasterHosts = set(self.getHosts(componentsList, "HAWQMASTER")).union(set(self.getHosts(componentsList, "HAWQSTANDBY")))
      hawqSegmentHosts = set(self.getHosts(componentsList, "HAWQSEGMENT"))
      hawqHosts = hawqMasterHosts.union(hawqSegmentHosts)
      numSegments = len(hawqSegmentHosts)
      minHawqHostsMemory = min([host['Hosts']['total_mem'] for host in hosts['items'] if host['Hosts']['host_name'] in hawqHosts])
      minHawqHostsCoreCount = min([host['Hosts']['cpu_count'] for host in hosts['items'] if host['Hosts']['host_name'] in hawqHosts])

      hawq_site = services["configurations"]["hawq-site"]["properties"]
      hawq_sysctl_env = services["configurations"]["hawq-sysctl-env"]["properties"]

      putHawqSiteProperty = self.putProperty(configurations, "hawq-site", services)
      putHawqSysctlEnvProperty = self.putProperty(configurations, "hawq-sysctl-env", services)
      putHdfsClientProperty = self.putProperty(configurations, "hdfs-client", services)

      # remove master port when master is colocated with Ambari server
      if self.isHawqMasterComponentOnAmbariServer(services) and "hawq_master_address_port" in hawq_site:
        putHawqSiteProperty("hawq_master_address_port", "")

      putHawqSiteProperty("hawq_rm_nvcore_limit_perseg", minHawqHostsCoreCount)

      MEMORY_THRESHOLD = 33554432 # 32GB, minHawqHostsMemory is represented in kB

      # set vm.overcommit_memory to 2 if the minimum memory among all hawqHosts is greater than 32GB
      vm_overcommit_memory = 2 if minHawqHostsMemory >= MEMORY_THRESHOLD else 1
      vm_overcommit_ratio = int(hawq_sysctl_env["vm.overcommit_ratio"]) if "vm.overcommit_ratio" in hawq_sysctl_env else 50

      putHawqSysctlEnvProperty("vm.overcommit_memory", vm_overcommit_memory)
      putHawqSysctlEnvProperty("vm.overcommit_ratio", vm_overcommit_ratio)

      host_ram_kb = (float(minHawqHostsMemory) * float(vm_overcommit_ratio) / 100) if vm_overcommit_memory == 2 else minHawqHostsMemory
      host_ram_gb = float(host_ram_kb) / (1024 * 1024)
      recommended_mem_percentage = {
        host_ram_gb <= 64: .75,
        64 < host_ram_gb <= 512: .85,
        host_ram_gb > 512: .95
      }[True]

      hawq_rm_memory_limit_perseg_value = math.ceil(host_ram_gb * recommended_mem_percentage)
      hawq_rm_memory_limit_perseg_unit = "GB"
      # If RAM on a host is very low ~ 2 GB, ceil function may result in making it equal to total mem, in that case we recommend the value in MB not GB
      if hawq_rm_memory_limit_perseg_value >= host_ram_gb :
        hawq_rm_memory_limit_perseg_value = math.ceil(float(host_ram_kb)/1024 * recommended_mem_percentage)
        hawq_rm_memory_limit_perseg_unit = "MB"

      # hawq_rm_memory_limit_perseg does not support decimal value so trim decimal using int
      putHawqSiteProperty("hawq_rm_memory_limit_perseg", "{0}{1}".format(int(hawq_rm_memory_limit_perseg_value), hawq_rm_memory_limit_perseg_unit))

      if numSegments and "hawq_rm_nvseg_perquery_limit" in hawq_site:
        factor_min = 1
        factor_max = 6
        limit = int(hawq_site["hawq_rm_nvseg_perquery_limit"])
        factor = limit / numSegments
        # if too many segments or default limit is too low --> stick with the limit
        if factor < factor_min:
          buckets = limit
        # if the limit is large and results in factor > max --> limit factor to max
        elif factor > factor_max:
          buckets = factor_max * numSegments
        else:
          buckets = factor * numSegments

        # In low memory environment, recalculate buckets with factor of 4
        # When host_ram_kb = 2796202.66667kB, hawq_rm_memory_limit_perseg is 2GB
        if host_ram_kb < 2796202.66667:
          factor = 4
          buckets = min(factor * numSegments, buckets)

        putHawqSiteProperty('default_hash_table_bucket_number', buckets)

      # update YARN RM urls with the values from yarn-site if YARN is installed
      if "YARN" in servicesList and "yarn-site" in services["configurations"]:
        yarn_site = services["configurations"]["yarn-site"]["properties"]
        for hs_prop, ys_prop in self.getHAWQYARNPropertyMapping().items():
          if hs_prop in hawq_site and ys_prop in yarn_site:
            putHawqSiteProperty(hs_prop, yarn_site[ys_prop])

      # set output.replace-datanode-on-failure in HAWQ hdfs-client depending on the cluster size
      # if number of segments is greater than 3, set to true
      putHdfsClientProperty("output.replace-datanode-on-failure", str(numSegments > 3).lower())

      putHawqSitePropertyAttribute = self.putPropertyAttribute(configurations, "hawq-site")
      putHawqSysctlEnvPropertyAttribute = self.putPropertyAttribute(configurations, "hawq-sysctl-env")

      ## Set ranges and visibility of properties
      # Show/hide properties based on the value of hawq_global_rm_type
      YARN_MODE = hawq_site["hawq_global_rm_type"].lower() == "yarn"
      yarn_mode_properties_visibility = {
        "hawq_rm_memory_limit_perseg": False,
        "hawq_rm_nvcore_limit_perseg": False,
        "hawq_rm_yarn_app_name": True,
        "hawq_rm_yarn_queue_name": True,
        "hawq_rm_yarn_scheduler_address": True,
        "hawq_rm_yarn_address": True
      }

      for property, visibility in yarn_mode_properties_visibility.iteritems():
        putHawqSitePropertyAttribute(property, "visible", str(visibility if YARN_MODE else not visibility).lower())

      putHawqSitePropertyAttribute("default_hash_table_bucket_number", "maximum", numSegments * 16 if numSegments * 16 < 10000 else 10000)

  def getHAWQYARNPropertyMapping(self):
    return {
      "hawq_rm_yarn_address": "yarn.resourcemanager.address",
      "hawq_rm_yarn_scheduler_address": "yarn.resourcemanager.scheduler.address"
    }

  def getServiceConfigurationsValidationItems(self, configurations, recommendedDefaults, services, hosts):
    siteName = "hawq-site"
    method = self.validateHAWQSiteConfigurations
    items = self.validateConfigurationsForSite(configurations, recommendedDefaults, services, hosts, siteName, method)

    siteName = "hdfs-client"
    method = self.validateHAWQHdfsClientConfigurations
    resultItems = self.validateConfigurationsForSite(configurations, recommendedDefaults, services, hosts, siteName, method)
    items.extend(resultItems)

    # validate recommended properties in hdfs-site
    siteName = "hdfs-site"
    method = self.validateHDFSSiteConfigurations
    resultItems = self.validateConfigurationsForSite(configurations, recommendedDefaults, services, hosts, siteName, method)
    items.extend(resultItems)

    # validate recommended properties in core-site
    siteName = "core-site"
    method = self.validateCORESiteConfigurations
    resultItems = self.validateConfigurationsForSite(configurations, recommendedDefaults, services, hosts, siteName, method)
    items.extend(resultItems)

    return items

  def isHawqMasterPortConflict(self, configurations):
    prop_name = 'hawq_master_address_port'
    default_ambari_port = 5432
    if prop_name in configurations["hawq-site"]["properties"]:
      portValue = int(configurations["hawq-site"]["properties"][prop_name])
      return portValue == default_ambari_port

    return False

  def validateIfRootDir(self, properties, validationItems, prop_name, display_name):
    root_dir = '/'
    if prop_name in properties and properties[prop_name].strip() == root_dir:
      validationItems.append({"config-name": prop_name,
                              "item": self.getWarnItem(
                              "It is not advisable to have " + display_name + " at " + root_dir +". Consider creating a sub directory for HAWQ")})

  def checkForMultipleDirs(self, properties, validationItems, prop_name, display_name):
    # check for delimiters space, comma, colon and semi-colon
    if prop_name in properties and len(re.sub(r'[,;:]', ' ', properties[prop_name]).split(' ')) > 1:
      validationItems.append({"config-name": prop_name,
                              "item": self.getErrorItem(
                              "Multiple directories for " + display_name + " are not allowed.")})

  def validateHAWQSiteConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):

    hawq_site = properties
    validationItems = []

    # 1. Check if HAWQ master/standby port numbers don't conflict with Ambari ports. Both Ambari and HAWQ use postgres DB and 5432 port.
    if self.isHawqMasterComponentOnAmbariServer(services) and self.isHawqMasterPortConflict(configurations):
      prop_name = 'hawq_master_address_port'
      validationItems.append({"config-name": prop_name,
                                "item": self.getWarnItem(
                                "The default Postgres port (5432) on the Ambari Server conflicts with the default HAWQ Masters port. "
                                "If you are using port 5432 for Postgres, you must either deploy the HAWQ Masters on a different host "
                                "or configure a different port for the HAWQ Masters in the HAWQ Configuration page.")})

    # 2. Check if any data directories are pointing to root dir '/'
    directories = {
                    'hawq_master_directory': 'HAWQ Master directory',
                    'hawq_master_temp_directory': 'HAWQ Master temp directory',
                    'hawq_segment_directory': 'HAWQ Segment directory',
                    'hawq_segment_temp_directory': 'HAWQ Segment temp directory'
                  }
    for property_name, display_name in directories.iteritems():
      self.validateIfRootDir(properties, validationItems, property_name, display_name)

    # 2.1 Check if any master or segment directories has multiple values
    directories = {
                    'hawq_master_directory': 'HAWQ Master directory',
                    'hawq_segment_directory': 'HAWQ Segment directory'
                  }
    for property_name, display_name in directories.iteritems():
      self.checkForMultipleDirs(properties, validationItems, property_name, display_name)

    # 3. Check YARN RM address properties
    YARN = "YARN"
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if YARN in servicesList and "yarn-site" in configurations:
      yarn_site = self.getSiteProperties(configurations, "yarn-site")
      for hs_prop, ys_prop in self.getHAWQYARNPropertyMapping().items():
        if hs_prop in hawq_site and ys_prop in yarn_site and hawq_site[hs_prop] != yarn_site[ys_prop]:
          message = "Expected value: {0} (this property should have the same value as the property {1} in yarn-site)".format(yarn_site[ys_prop], ys_prop)
          validationItems.append({"config-name": hs_prop, "item": self.getWarnItem(message)})

    # 4. Check HAWQ Resource Manager type
    HAWQ_GLOBAL_RM_TYPE = "hawq_global_rm_type"
    if YARN not in servicesList and HAWQ_GLOBAL_RM_TYPE in hawq_site and hawq_site[HAWQ_GLOBAL_RM_TYPE].upper() == YARN:
      message = "{0} must be set to none if YARN service is not installed".format(HAWQ_GLOBAL_RM_TYPE)
      validationItems.append({"config-name": HAWQ_GLOBAL_RM_TYPE, "item": self.getErrorItem(message)})

    # 5. Check query limits
    if ("default_hash_table_bucket_number" in hawq_site and
        "hawq_rm_nvseg_perquery_limit"     in hawq_site and
        int(hawq_site["default_hash_table_bucket_number"]) > int(hawq_site["hawq_rm_nvseg_perquery_limit"])):
      message = "Default buckets for Hash Distributed tables parameter value should not be greater than the value of Virtual Segments Limit per Query (Total) parameter, currently set to {0}.".format(hawq_site["hawq_rm_nvseg_perquery_limit"])
      validationItems.append({"config-name": "default_hash_table_bucket_number", "item": self.getErrorItem(message)})

    if "hawq_global_rm_type" in hawq_site and "hawq_rm_memory_limit_perseg" in hawq_site:
      hawq_rm_memory_limit_perseg = hawq_site["hawq_rm_memory_limit_perseg"]
      unit = hawq_rm_memory_limit_perseg[-2:]
      value = hawq_rm_memory_limit_perseg[:-2]
      # For clusters running with hawq_rm_memory_limit_perseg less than 1GB
      if (unit == "GB" and int(value) < 1) or (unit == "MB" and int(value) < 1024):
        message = "HAWQ Segment Memory less than 1GB is not sufficient"
        validationItems.append({"config-name": "hawq_global_rm_type", "item": self.getErrorItem(message)})

    return self.toConfigurationValidationProblems(validationItems, "hawq-site")

  def validateHAWQHdfsClientConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    hdfs_client = properties
    validationItems = []

    # check HAWQ hdfs-client output.replace-datanode-on-failure property
    PROP_NAME = "output.replace-datanode-on-failure"
    if PROP_NAME in hdfs_client:
      value = hdfs_client[PROP_NAME].upper()
      componentsListList = [service["components"] for service in services["services"]]
      componentsList = [item["StackServiceComponents"] for sublist in componentsListList for item in sublist]
      numSegments = len(self.getHosts(componentsList, "HAWQSEGMENT"))

      message = None
      MIN_NUM_SEGMENT_THRESHOLD = 3
      if numSegments > MIN_NUM_SEGMENT_THRESHOLD and value != 'TRUE':
        message = "{0} should be set to true (checked) for clusters with more than {1} HAWQ Segments"
      elif numSegments <= MIN_NUM_SEGMENT_THRESHOLD and value != 'FALSE':
        message = "{0} should be set to false (unchecked) for clusters with {1} or less HAWQ Segments"
      if message:
        validationItems.append({"config-name": PROP_NAME, "item": self.getWarnItem(message.format(PROP_NAME, str(MIN_NUM_SEGMENT_THRESHOLD)))})

    return self.toConfigurationValidationProblems(validationItems, "hdfs-client")

  def validateHDFSSiteConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    hdfs_site = properties
    validationItems = []
    for property, desired_value in self.getHDFSSiteDesiredValues(self.isSecurityEnabled(services)).iteritems():
      if property not in hdfs_site or hdfs_site[property] != desired_value:
        message = "HAWQ requires this property to be set to the recommended value of " + desired_value
        item = self.getErrorItem(message) if property == "dfs.allow.truncate" else self.getWarnItem(message)
        validationItems.append({"config-name": property, "item": item})
    return self.toConfigurationValidationProblems(validationItems, "hdfs-site")

  def validateCORESiteConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    core_site = properties
    validationItems = []
    for property, desired_value in self.getCORESiteDesiredValues().iteritems():
      if property not in core_site or core_site[property] != desired_value:
        message = "HAWQ requires this property to be set to the recommended value of " + desired_value
        validationItems.append({"config-name": property, "item": self.getWarnItem(message)})
    return self.toConfigurationValidationProblems(validationItems, "core-site")

  def getHDFSSiteDesiredValues(self, is_secure):
    hdfs_site_desired_values = {
      "dfs.allow.truncate" : "true",
      "dfs.block.access.token.enable" : str(is_secure).lower(),
      "dfs.block.local-path-access.user" : "gpadmin",
      "dfs.client.read.shortcircuit" : "true",
      "dfs.client.use.legacy.blockreader.local" : "false",
      "dfs.datanode.data.dir.perm" : "750",
      "dfs.datanode.handler.count" : "60",
      "dfs.datanode.max.transfer.threads" : "40960",
      "dfs.namenode.accesstime.precision" : "0",
      "dfs.support.append" : "true"
    }
    return hdfs_site_desired_values

  def getCORESiteDesiredValues(self):
    core_site_desired_values = {
      "ipc.server.listen.queue.size" : "3300"
    }
    return core_site_desired_values