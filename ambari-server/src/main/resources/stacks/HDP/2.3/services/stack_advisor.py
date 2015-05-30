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

class HDP23StackAdvisor(HDP22StackAdvisor):

  def getServiceConfigurationRecommenderDict(self):
    parentRecommendConfDict = super(HDP23StackAdvisor, self).getServiceConfigurationRecommenderDict()
    childRecommendConfDict = {
      "TEZ": self.recommendTezConfigurations,
      "HDFS": self.recommendHDFSConfigurations,
      "HIVE": self.recommendHIVEConfigurations,
      "HBASE": self.recommendHBASEConfigurations
    }
    parentRecommendConfDict.update(childRecommendConfDict)
    return parentRecommendConfDict

  def recommendTezConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP23StackAdvisor, self).recommendTezConfigurations(configurations, clusterData, services, hosts)

    putTezProperty = self.putProperty(configurations, "tez-site")
    # remove 2gb limit for tez.runtime.io.sort.mb
    # in HDP 2.3 "tez.runtime.sorter.class" is set by default to PIPELINED, in other case comment calculation code below
    taskResourceMemory = clusterData['mapMemory'] if clusterData['mapMemory'] > 2048 else int(clusterData['reduceMemory'])
    taskResourceMemory = min(clusterData['containers'] * clusterData['ramPerContainer'], taskResourceMemory)
    putTezProperty("tez.runtime.io.sort.mb", int(taskResourceMemory * 0.4))

    if "tez-site" in services["configurations"] and "tez.runtime.sorter.class" in services["configurations"]["tez-site"]["properties"]:
      if services["configurations"]["tez-site"]["properties"]["tez.runtime.sorter.class"] == "LEGACY":
        putTezAttribute = self.putPropertyAttribute(configurations, "tez-site")
        putTezAttribute("tez.runtime.io.sort.mb", "maximum", 2047)

  def recommendHBASEConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP23StackAdvisor, self).recommendHBASEConfigurations(configurations, clusterData, services, hosts)
    putHbaseSiteProperty = self.putProperty(configurations, "hbase-site", services)
    putHbaseSitePropertyAttributes = self.putPropertyAttribute(configurations, "hbase-site")

    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if 'ranger-hbase-plugin-properties' in services['configurations'] and ('ranger-hbase-plugin-enabled' in services['configurations']['ranger-hbase-plugin-properties']['properties']):
      rangerPluginEnabled = services['configurations']['ranger-hbase-plugin-properties']['properties']['ranger-hbase-plugin-enabled']
      if ("RANGER" in servicesList) and (rangerPluginEnabled.lower() == "Yes".lower()):
        putHbaseSiteProperty("hbase.security.authorization", 'true')
        putHbaseSiteProperty("hbase.coprocessor.master.classes", 'org.apache.ranger.authorization.hbase.RangerAuthorizationCoprocessor')
        putHbaseSiteProperty("hbase.coprocessor.region.classes", 'org.apache.ranger.authorization.hbase.RangerAuthorizationCoprocessor')


  def recommendHIVEConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP23StackAdvisor, self).recommendHIVEConfigurations(configurations, clusterData, services, hosts)

    putHiveServerProperty = self.putProperty(configurations, "hiveserver2-site", services)

    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if 'ranger-hive-plugin-properties' in services['configurations'] and ('ranger-hive-plugin-enabled' in services['configurations']['ranger-hive-plugin-properties']['properties']):
      rangerPluginEnabled = services['configurations']['ranger-hive-plugin-properties']['properties']['ranger-hive-plugin-enabled']
      if ("RANGER" in servicesList) :
        if (rangerPluginEnabled.lower() == "Yes".lower()):
          putHiveServerProperty("hive.security.authorization.manager", 'org.apache.ranger.authorization.hive.authorizer.RangerHiveAuthorizerFactory')
          putHiveServerProperty("hive.security.authenticator.manager", 'org.apache.hadoop.hive.ql.security.SessionStateUserAuthenticator')
        elif (rangerPluginEnabled.lower() == "No".lower()):
          putHiveServerProperty("hive.security.authorization.manager", 'org.apache.hadoop.hive.ql.security.authorization.plugin.sqlstd.SQLStdHiveAuthorizerFactory')
          putHiveServerProperty("hive.security.authenticator.manager", 'org.apache.hadoop.hive.ql.security.SessionStateUserAuthenticator')


  def recommendHDFSConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP23StackAdvisor, self).recommendHDFSConfigurations(configurations, clusterData, services, hosts)

    putHdfsSiteProperty = self.putProperty(configurations, "hdfs-site", services)
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if ('ranger-hdfs-plugin-properties' in services['configurations']) and ('ranger-hdfs-plugin-enabled' in services['configurations']['ranger-hdfs-plugin-properties']['properties']):
      rangerPluginEnabled = services['configurations']['ranger-hdfs-plugin-properties']['properties']['ranger-hdfs-plugin-enabled']
      if ("RANGER" in servicesList) and (rangerPluginEnabled.lower() == 'Yes'.lower()):
        putHdfsSiteProperty("dfs.namenode.inode.attributes.provider.class",'org.apache.ranger.authorization.hadoop.RangerHdfsAuthorizer')

  def getServiceConfigurationValidators(self):
      parentValidators = super(HDP23StackAdvisor, self).getServiceConfigurationValidators()
      childValidators = {
        "HDFS": {"hdfs-site": self.validateHDFSConfigurations},
        "HIVE": {"hiveserver2-site": self.validateHiveServer2Configurations},
        "HBASE": {"hbase-site": self.validateHBASEConfigurations}
      }
      parentValidators.update(childValidators)
      return parentValidators

  def validateHDFSConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    super(HDP23StackAdvisor, self).validateHDFSConfigurations(properties, recommendedDefaults, configurations, services, hosts)

    # We can not access property hadoop.security.authentication from the
    # other config (core-site). That's why we are using another heuristics here
    hdfs_site = properties
    validationItems = [] #Adding Ranger Plugin logic here
    ranger_plugin_properties = getSiteProperties(configurations, "ranger-hdfs-plugin-properties")
    ranger_plugin_enabled = ranger_plugin_properties['ranger-hdfs-plugin-enabled'] if ranger_plugin_properties else 'No'
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    if ("RANGER" in servicesList) and (ranger_plugin_enabled.lower() == 'Yes'.lower()):
      if hdfs_site['dfs.namenode.inode.attributes.provider.class'].lower() != 'org.apache.ranger.authorization.hadoop.RangerHdfsAuthorizer'.lower():
        validationItems.append({"config-name": 'dfs.namenode.inode.attributes.provider.class',
                                    "item": self.getWarnItem(
                                      "dfs.namenode.inode.attributes.provider.class needs to be set to 'org.apache.ranger.authorization.hadoop.RangerHdfsAuthorizer' if Ranger HDFS Plugin is enabled.")})
    return self.toConfigurationValidationProblems(validationItems, "hdfs-site")


  def validateHiveServer2Configurations(self, properties, recommendedDefaults, configurations, services, hosts):
    super(HDP23StackAdvisor, self).validateHiveServer2Configurations(properties, recommendedDefaults, configurations, services, hosts)
    hive_server2 = properties
    validationItems = []
    #Adding Ranger Plugin logic here
    ranger_plugin_properties = getSiteProperties(configurations, "ranger-hive-plugin-properties")
    ranger_plugin_enabled = ranger_plugin_properties['ranger-hive-plugin-enabled'] if ranger_plugin_properties else 'No'
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    ##Add stack validations only if Ranger is enabled.
    if ("RANGER" in servicesList):
      ##Add stack validations for  Ranger plugin enabled.
      if (ranger_plugin_enabled.lower() == 'Yes'.lower()):
        prop_name = 'hive.security.authorization.manager'
        prop_val = "org.apache.ranger.authorization.hive.authorizer.RangerHiveAuthorizerFactory"
        if hive_server2[prop_name] != prop_val:
          validationItems.append({"config-name": prop_name,
                                  "item": self.getWarnItem(
                                  "If Ranger Hive Plugin is enabled."\
                                  " {0} needs to be set to {1}".format(prop_name,prop_val))})
        prop_name = 'hive.security.authenticator.manager'
        prop_val = "org.apache.hadoop.hive.ql.security.SessionStateUserAuthenticator"
        if hive_server2[prop_name] != prop_val:
          validationItems.append({"config-name": prop_name,
                                  "item": self.getWarnItem(
                                  "If Ranger Hive Plugin is enabled."\
                                  " {0} needs to be set to {1}".format(prop_name,prop_val))})
      ##Add stack validations for  Ranger plugin disabled.
      elif (ranger_plugin_enabled.lower() == 'No'.lower()):
        prop_name = 'hive.security.authorization.manager'
        prop_val = "org.apache.hadoop.hive.ql.security.authorization.plugin.sqlstd.SQLStdHiveAuthorizerFactory"
        if hive_server2[prop_name] != prop_val:
          validationItems.append({"config-name": prop_name,
                                  "item": self.getWarnItem(
                                  "If Ranger Hive Plugin is disabled."\
                                  " {0} needs to be set to {1}".format(prop_name,prop_val))})
        prop_name = 'hive.security.authenticator.manager'
        prop_val = "org.apache.hadoop.hive.ql.security.SessionStateUserAuthenticator"
        if hive_server2[prop_name] != prop_val:
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


  def isComponentUsingCardinalityForLayout(self, componentName):
    return componentName == 'NFS_GATEWAY'
