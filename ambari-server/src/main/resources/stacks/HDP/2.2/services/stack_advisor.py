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

class HDP22StackAdvisor(HDP21StackAdvisor):

  def getServiceConfigurationRecommenderDict(self):
    parentRecommendConfDict = super(HDP22StackAdvisor, self).getServiceConfigurationRecommenderDict()
    childRecommendConfDict = {
      "HDFS": self.recommendHDFSConfigurations,
      "MAPREDUCE2": self.recommendMapReduce2Configurations,
      "TEZ": self.recommendTezConfigurations,
      "YARN": self.recommendYARNConfigurations
    }
    parentRecommendConfDict.update(childRecommendConfDict)
    return parentRecommendConfDict

  def recommendYARNConfigurations(self, configurations, clusterData):
    super(HDP22StackAdvisor, self).recommendYARNConfigurations(configurations, clusterData)
    putYarnProperty = self.putProperty(configurations, "yarn-site")
    putYarnProperty('yarn.nodemanager.resource.cpu-vcores', clusterData['cpu'])

  def recommendHDFSConfigurations(self, configurations, clusterData):
    putHdfsPropery = self.putProperty(configurations, "hdfs-site")
    putHdfsPropery("dfs.datanode.max.transfer.threads", 16384 if clusterData["hBaseInstalled"] else 4096)

  def recommendTezConfigurations(self, configurations, clusterData):
    putTezProperty = self.putProperty(configurations, "tez-site")
    putTezProperty("tez.am.resource.memory.mb", int(clusterData['amMemory']) * 2 if int(clusterData['amMemory']) < 3072 else int(clusterData['amMemory']))
    putTezProperty("tez.am.java.opts",
                   "-server -Xmx" + str(int(0.8 * clusterData["amMemory"]))
                   + "m -Djava.net.preferIPv4Stack=true -XX:+UseNUMA -XX:+UseParallelGC")

    taskResourceMemory = clusterData['mapMemory'] if clusterData['mapMemory'] > 2048 else int(clusterData['reduceMemory'])
    taskResourceMemory = min(clusterData['containers'] * clusterData['ramPerContainer'], taskResourceMemory)
    putTezProperty("tez.task.resource.memory.mb", taskResourceMemory)
    putTezProperty("tez.runtime.io.sort.mb", int(taskResourceMemory * 0.4) if int(taskResourceMemory * 0.4) <= 2147483644 else 2147483644)
    putTezProperty("tez.runtime.unordered.output.buffer.size-mb", int(taskResourceMemory * 0.075))

  def getServiceConfigurationValidators(self):
    parentValidators = super(HDP22StackAdvisor, self).getServiceConfigurationValidators()
    childValidators = {
      "HDFS": ["hdfs-site", self.validateHDFSConfigurations],
      "MAPREDUCE2": ["mapred-site", self.validateMapReduce2Configurations],
      "TEZ": ["tez-site", self.validateTezConfigurations]
    }
    parentValidators.update(childValidators)
    return parentValidators

  def validateTezConfigurations(self, properties, recommendedDefaults, configurations):
    validationItems = [ {"config-name": 'tez.am.resource.memory.mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'tez.am.resource.memory.mb')},
                        {"config-name": 'tez.am.java.opts', "item": self.validateXmxValue(properties, recommendedDefaults, 'tez.am.java.opts')},
                        {"config-name": 'tez.task.resource.memory.mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'tez.task.resource.memory.mb')},
                        {"config-name": 'tez.runtime.io.sort.mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'tez.runtime.io.sort.mb')},
                        {"config-name": 'tez.runtime.unordered.output.buffer.size-mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'tez.runtime.unordered.output.buffer.size-mb')},]
    return self.toConfigurationValidationProblems(validationItems, "tez-site")

  def recommendMapReduce2Configurations(self, configurations, clusterData):
    putMapredProperty = self.putProperty(configurations, "mapred-site")
    putMapredProperty('yarn.app.mapreduce.am.resource.mb', int(clusterData['amMemory']))
    putMapredProperty('yarn.app.mapreduce.am.command-opts', "-Xmx" + str(int(round(0.8 * clusterData['amMemory']))) + "m" + " -Dhdp.version=${hdp.version}")
    putMapredProperty('mapreduce.map.memory.mb', clusterData['mapMemory'])
    putMapredProperty('mapreduce.reduce.memory.mb', int(clusterData['reduceMemory']))
    putMapredProperty('mapreduce.map.java.opts', "-Xmx" + str(int(round(0.8 * clusterData['mapMemory']))) + "m")
    putMapredProperty('mapreduce.reduce.java.opts', "-Xmx" + str(int(round(0.8 * clusterData['reduceMemory']))) + "m")
    putMapredProperty('mapreduce.task.io.sort.mb', min(int(round(0.4 * clusterData['mapMemory'])), 1024))

  def validateMapReduce2Configurations(self, properties, recommendedDefaults, configurations):
    validationItems = [ {"config-name": 'mapreduce.map.java.opts', "item": self.validateXmxValue(properties, recommendedDefaults, 'mapreduce.map.java.opts')},
                        {"config-name": 'mapreduce.reduce.java.opts', "item": self.validateXmxValue(properties, recommendedDefaults, 'mapreduce.reduce.java.opts')},
                        {"config-name": 'mapreduce.task.io.sort.mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'mapreduce.task.io.sort.mb')},
                        {"config-name": 'mapreduce.map.memory.mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'mapreduce.map.memory.mb')},
                        {"config-name": 'mapreduce.reduce.memory.mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'mapreduce.reduce.memory.mb')},
                        {"config-name": 'yarn.app.mapreduce.am.resource.mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'yarn.app.mapreduce.am.resource.mb')}]
    return self.toConfigurationValidationProblems(validationItems, "mapred-site")

  def validateHDFSConfigurations(self, properties, recommendedDefaults, configurations):
    # We can not access property hadoop.security.authentication from the
    # other config (core-site). That's why we are using another heuristics here
    hdfs_site = properties
    core_site = getSiteProperties(configurations, "core-site")

    dfs_encrypt_data_transfer = 'dfs.encrypt.data.transfer'  # Hadoop Wire encryption
    try:
      wire_encryption_enabled = hdfs_site[dfs_encrypt_data_transfer] == "true"
    except KeyError:
      wire_encryption_enabled = False

    HTTP_ONLY = 'HTTP_ONLY'
    HTTPS_ONLY = 'HTTPS_ONLY'
    HTTP_AND_HTTPS = 'HTTP_AND_HTTPS'

    VALID_HTTP_POLICY_VALUES = [HTTP_ONLY, HTTPS_ONLY, HTTP_AND_HTTPS]
    VALID_TRANSFER_PROTECTION_VALUES = ['authentication', 'integrity', 'privacy']

    validationItems = []
    if (not wire_encryption_enabled and   # If wire encryption is enabled at Hadoop, it disables all our checks
          core_site['hadoop.security.authentication'] == 'kerberos' and
          core_site['hadoop.security.authorization'] == 'true'):
      # security is enabled

      dfs_http_policy = 'dfs.http.policy'
      dfs_datanode_address = 'dfs.datanode.address'
      datanode_http_address = 'dfs.datanode.http.address'
      datanode_https_address = 'dfs.datanode.https.address'
      data_transfer_protection = 'dfs.data.transfer.protection'

      try: # Params may be absent
        privileged_dfs_dn_port = isSecurePort(getPort(hdfs_site[dfs_datanode_address]))
      except KeyError:
        privileged_dfs_dn_port = False
      try:
        privileged_dfs_http_port = isSecurePort(getPort(hdfs_site[datanode_http_address]))
      except KeyError:
        privileged_dfs_http_port = False
      try:
        privileged_dfs_https_port = isSecurePort(getPort(hdfs_site[datanode_https_address]))
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
        if not privileged_dfs_dn_port and (privileged_dfs_https_port or datanode_https_address not in hdfs_site):
          important_properties = [dfs_datanode_address, datanode_https_address]
          message = "You set up datanode to use some non-secure ports. " \
                    "If you want to run Datanode under non-root user in a secure cluster, " \
                    "you should set all these properties {2} " \
                    "to use non-secure ports (if property {3} does not exist, " \
                    "just add it). You may also set up property {4} ('{5}' is a good default value). " \
                    "Also, set up WebHDFS with SSL as " \
                    "described in manual in order to be able to " \
                    "use HTTPS.".format(dfs_http_policy, dfs_http_policy_value, important_properties,
                                        datanode_https_address, data_transfer_protection,
                                        VALID_TRANSFER_PROTECTION_VALUES[0])
          address_properties_with_warnings.extend(important_properties)
      else:  # dfs_http_policy_value == HTTP_AND_HTTPS or HTTP_ONLY
        # We don't enforce datanode_https_address to use privileged ports here
        any_nonprivileged_ports_are_in_use = not privileged_dfs_dn_port or not privileged_dfs_http_port
        if any_nonprivileged_ports_are_in_use:
          important_properties = [dfs_datanode_address, datanode_http_address]
          message = "You have set up datanode to use some non-secure ports, but {0} is set to {1}. " \
                    "In a secure cluster, Datanode forbids using non-secure ports " \
                    "if {0} is not set to {3}. " \
                    "Please make sure that properties {2} use secure ports.".format(
                      dfs_http_policy, dfs_http_policy_value, important_properties, HTTPS_ONLY)
          address_properties_with_warnings.extend(important_properties)

      # Generate port-related warnings if any
      for prop in address_properties_with_warnings:
        validationItems.append({"config-name": prop,
                                "item": self.getWarnItem(message)})

      # Check if it is appropriate to use dfs.data.transfer.protection
      if data_transfer_protection_value is not None:
        if dfs_http_policy_value in [HTTP_ONLY, HTTP_AND_HTTPS]:
          validationItems.append({"config-name": data_transfer_protection,
                                  "item": self.getWarnItem(
                                    "{0} property can not be used when {1} is set to any "
                                    "value other then {2}. Tip: When {1} property is not defined, it defaults to {3}".format(
                                    data_transfer_protection, dfs_http_policy, HTTPS_ONLY, HTTP_ONLY))})
        elif not data_transfer_protection_value in VALID_TRANSFER_PROTECTION_VALUES:
          validationItems.append({"config-name": data_transfer_protection,
                                  "item": self.getWarnItem(
                                    "Invalid property value: {0}. Valid values are {1}.".format(
                                      data_transfer_protection_value, VALID_TRANSFER_PROTECTION_VALUES))})
    return self.toConfigurationValidationProblems(validationItems, "hdfs-site")



