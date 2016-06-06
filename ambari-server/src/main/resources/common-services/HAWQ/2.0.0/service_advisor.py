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
import imp
import re
import socket
import traceback
import math

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

  def getHostsForMasterComponent(self, stackAdvisor, services, hosts, component, hostsList, hostsComponentsMap):
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

    return stackAdvisor.getHostsForMasterComponent(services, hosts, component, hostsList, hostsComponentsMap)

  def isComponentNotPreferableOnAmbariServerHost(self, componentName):
    return componentName in ('HAWQMASTER', 'HAWQSTANDBY')

  def getComponentLayoutScheme(self, componentName):
    if componentName == 'HAWQMASTER':
      return {6: 2, 31: 1, "else": 5}

    if componentName == 'HAWQSTANDBY':
      return {6: 1, 31: 2, "else": 3}

    return None

  def colocateService(self, stackAdvisor, hostsComponentsMap, serviceComponents):
    # colocate HAWQSEGMENT with DATANODE, if no hosts have been allocated for HAWQSEGMENT
    hawqSegment = [component for component in serviceComponents if component["StackServiceComponents"]["component_name"] == "HAWQSEGMENT"][0]
    if not stackAdvisor.isComponentHostsPopulated(hawqSegment):
      for hostName in hostsComponentsMap.keys():
        hostComponents = hostsComponentsMap[hostName]
        if {"name": "DATANODE"} in hostComponents and {"name": "HAWQSEGMENT"} not in hostComponents:
          hostsComponentsMap[hostName].append( { "name": "HAWQSEGMENT" } )
        if {"name": "DATANODE"} not in hostComponents and {"name": "HAWQSEGMENT"} in hostComponents:
          hostComponents.remove({"name": "HAWQSEGMENT"})

  def getComponentLayoutValidations(self, stackAdvisor, services, hosts):
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

    if len(hawqMasterHosts) ==  1 and hostsCount > 1 and stackAdvisor.isLocalHost(hawqMasterHosts[0]):
      message = "The default Postgres port (5432) on the Ambari Server conflicts with the default HAWQ Masters port. " \
                "If you are using port 5432 for Postgres, you must either deploy the HAWQ Master on a different host " \
                "or configure a different port for the HAWQ Masters in the HAWQ Configuration page."
      items.append( { "type": 'host-component', "level": 'WARN', "message": message, "component-name": 'HAWQMASTER', "host": hawqMasterHosts[0] } )

    if len(hawqStandbyHosts) ==  1 and hostsCount > 1 and stackAdvisor.isLocalHost(hawqStandbyHosts[0]):
      message = "The default Postgres port (5432) on the Ambari Server conflicts with the default HAWQ Masters port. " \
                "If you are using port 5432 for Postgres, you must either deploy the HAWQ Standby Master on a different host " \
                "or configure a different port for the HAWQ Masters in the HAWQ Configuration page."
      items.append( { "type": 'host-component', "level": 'WARN', "message": message, "component-name": 'HAWQSTANDBY', "host": hawqStandbyHosts[0] } )

    return items

  def isHawqMasterComponentOnAmbariServer(self, stackAdvisor, services):
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item for sublist in componentsListList for item in sublist]
    hawqMasterComponentHosts = [hostname for component in componentsList if component["StackServiceComponents"]["component_name"] in ("HAWQMASTER", "HAWQSTANDBY") for hostname in component["StackServiceComponents"]["hostnames"]]
    return any([stackAdvisor.isLocalHost(host) for host in hawqMasterComponentHosts])

  def getServiceConfigurationRecommendations(self, stackAdvisor, configurations, clusterData, services, hosts):
    putHdfsSiteProperty = self.putProperty(configurations, "hdfs-site", services)

    # Set dfs.allow.truncate to true
    putHdfsSiteProperty('dfs.allow.truncate', 'true')

    if any(x in services["configurations"] for x in ["hawq-site", "hdfs-client", "hawq-sysctl-env"]):
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
    putHawqSiteProperty = self.putProperty(configurations, "hawq-site", services)
    putHawqSitePropertyAttribute = self.putPropertyAttribute(configurations, "hawq-site")
    hawq_sysctl_env = services["configurations"]["hawq-sysctl-env"]["properties"]
    putHawqSysctlEnvProperty = self.putProperty(configurations, "hawq-sysctl-env", services)

    # remove master port when master is colocated with Ambari server
    if self.isHawqMasterComponentOnAmbariServer(stackAdvisor, services) and "hawq_master_address_port" in hawq_site:
      putHawqSiteProperty('hawq_master_address_port', '')

    # update query limits if segments are deployed
    if numSegments and "default_hash_table_bucket_number" in hawq_site and "hawq_rm_nvseg_perquery_limit" in hawq_site:
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
      putHawqSiteProperty('default_hash_table_bucket_number', buckets)


    # update YARN RM urls with the values from yarn-site if YARN is installed
    if "YARN" in servicesList and "yarn-site" in services["configurations"]:
      yarn_site = services["configurations"]["yarn-site"]["properties"]
      for hs_prop, ys_prop in self.getHAWQYARNPropertyMapping().items():
        if hs_prop in hawq_site and ys_prop in yarn_site:
          putHawqSiteProperty(hs_prop, yarn_site[ys_prop])

    putHawqSiteProperty('hawq_rm_nvcore_limit_perseg', minHawqHostsCoreCount)


    if "vm.overcommit_memory" in hawq_sysctl_env:
      MEM_THRESHOLD = 33554432 # 32GB, minHawqHostsMemory is represented in kB
      # Set the value for hawq_rm_memory_limit_perseg based on vm.overcommit value and RAM available on HAWQ Hosts
      # If value of hawq_rm_memory_limit_perseg is 67108864KB, it indicates hawq is being added and recommendation
      # has not be made yet, since after recommendation it will be in GB in case its 67108864KB.
      vm_overcommit_ratio = int(hawq_sysctl_env["vm.overcommit_ratio"]) if "vm.overcommit_ratio" in hawq_sysctl_env else 50
      if "hawq_rm_memory_limit_perseg" in hawq_site and hawq_site["hawq_rm_memory_limit_perseg"] == "65535MB":
        vm_overcommit_mem_value = 2 if minHawqHostsMemory >= MEM_THRESHOLD else 1
      else:
        # set vm.overcommit_memory to 2 if the minimum memory among all hawqHosts is greater than 32GB
        vm_overcommit_mem_value = int(hawq_sysctl_env["vm.overcommit_memory"])
      putHawqSysctlEnvProperty("vm.overcommit_ratio", vm_overcommit_ratio)
      putHawqSysctlEnvProperty("vm.overcommit_memory", vm_overcommit_mem_value)
      host_ram_kb = minHawqHostsMemory * vm_overcommit_ratio / 100 if vm_overcommit_mem_value == 2 else minHawqHostsMemory
      host_ram_gb = float(host_ram_kb) / (1024 * 1024)
      recommended_mem_percentage = {
        host_ram_gb <= 64: .75,
        64 < host_ram_gb <= 512: .85,
        host_ram_gb > 512: .95
      }[True]
      recommended_mem = math.ceil(host_ram_gb * recommended_mem_percentage)
      unit = "GB"
      # If RAM on a host is very low ~ 2 GB, ceil function may result in making it equal to total mem,
      # in that case we recommend the value in MB not GB
      if recommended_mem >= host_ram_gb:
        recommended_mem = math.ceil(float(host_ram_kb)/1024 * recommended_mem_percentage)
        unit = "MB"
      # hawq_rm_memory_limit_perseg does not support decimal value so trim decimal using int
      putHawqSiteProperty("hawq_rm_memory_limit_perseg", "{0}{1}".format(int(recommended_mem), unit))

    # Show / Hide properties based on the value of hawq_global_rm_type
    YARN_MODE = True if hawq_site["hawq_global_rm_type"].lower() == "yarn" else False
    yarn_mode_properties_visibility = {
      "hawq_rm_memory_limit_perseg": False,
      "hawq_rm_nvcore_limit_perseg": False,
      "hawq_rm_yarn_app_name": True,
      "hawq_rm_yarn_queue_name": True,
      "hawq_rm_yarn_scheduler_address": True,
      "hawq_rm_yarn_address": True
    }
    for property, visible_status in yarn_mode_properties_visibility.iteritems():
      putHawqSitePropertyAttribute(property, "visible", str(visible_status if YARN_MODE else not visible_status).lower())

    # set output.replace-datanode-on-failure in HAWQ hdfs-client depending on the cluster size
    if "hdfs-client" in services["configurations"]:
      MIN_NUM_SEGMENT_THRESHOLD = 3
      hdfs_client = services["configurations"]["hdfs-client"]["properties"]
      if "output.replace-datanode-on-failure" in hdfs_client:
        propertyValue = "true" if numSegments > MIN_NUM_SEGMENT_THRESHOLD else "false"
        putHdfsClientProperty = self.putProperty(configurations, "hdfs-client", services)
        putHdfsClientProperty("output.replace-datanode-on-failure", propertyValue)

  def getHAWQYARNPropertyMapping(self):
    return { "hawq_rm_yarn_address": "yarn.resourcemanager.address", "hawq_rm_yarn_scheduler_address": "yarn.resourcemanager.scheduler.address" }

  def getConfigurationsValidationItems(self, stackAdvisor, configurations, recommendedDefaults, services, hosts):
    siteName = "hawq-site"
    method = self.validateHAWQSiteConfigurations
    items = self.validateConfigurationsForSite(stackAdvisor, configurations, recommendedDefaults, services, hosts, siteName, method)

    siteName = "hdfs-client"
    method = self.validateHAWQHdfsClientConfigurations
    resultItems = self.validateConfigurationsForSite(stackAdvisor, configurations, recommendedDefaults, services, hosts, siteName, method)
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

  def validateHAWQSiteConfigurations(self, stackAdvisor, properties, recommendedDefaults, configurations, services, hosts):

    hawq_site = properties
    validationItems = []

    # 1. Check if HAWQ master/standby port numbers don't conflict with Ambari ports. Both Ambari and HAWQ use postgres DB and 5432 port.
    if self.isHawqMasterComponentOnAmbariServer(stackAdvisor, services) and self.isHawqMasterPortConflict(configurations):
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

    return stackAdvisor.toConfigurationValidationProblems(validationItems, "hawq-site")

  def validateHAWQHdfsClientConfigurations(self, stackAdvisor, properties, recommendedDefaults, configurations, services, hosts):
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

    return stackAdvisor.toConfigurationValidationProblems(validationItems, "hdfs-client")
