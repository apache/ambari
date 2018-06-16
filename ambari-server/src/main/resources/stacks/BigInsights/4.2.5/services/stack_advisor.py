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

import os, platform
from ambari_commons import subprocess32

class BigInsights425StackAdvisor(BigInsights42StackAdvisor):

  def getServiceConfigurationRecommenderDict(self):
    parentRecommendConfDict = super(BigInsights425StackAdvisor, self).getServiceConfigurationRecommenderDict()
    childRecommendConfDict = {
      "HDFS": self.recommendHDFSConfigurations,
      "JNBG": self.recommendJNBGConfigurations,
      "SOLR": self.recommendSolrConfigurations,
      "TITAN": self.recommendTitanConfigurations,
      "RANGER": self.recommendRangerConfigurations
    }
    parentRecommendConfDict.update(childRecommendConfDict)
    return parentRecommendConfDict

  def getServiceConfigurationValidators(self):
    parentValidators = super(BigInsights425StackAdvisor, self).getServiceConfigurationValidators()
    childValidators = {
      "JNBG": {"jnbg-env": self.validateJNBGConfigurations},
      "SOLR": {"ranger-solr-plugin-properties": self.validateSolrRangerPluginConfigurations}
    }
    self.mergeValidators(parentValidators, childValidators)
    return parentValidators

  def recommendJNBGConfigurations(self, configurations, clusterData, services, hosts):
    putJNBGEnvProperty = self.putProperty(configurations, "jnbg-env", services)
    putJNBGEnvPropertyAttribute = self.putPropertyAttribute(configurations, "jnbg-env")
   
    distro_version = platform.linux_distribution()[1]
    # On RHEL 6.x default path does not point to a Python 2.7
    # so empty out the field and force user to update the path
    if distro_version < "7.0":
      putJNBGEnvProperty('python_interpreter_path', "")

  def validateJNBGConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    validationItems = []
    jnbg_env = getSiteProperties(configurations, "jnbg-env")
    py_exec = jnbg_env.get("python_interpreter_path") if jnbg_env and "python_interpreter_path" in jnbg_env else []

    # Test that it is a valid executable path before proceeding
    if not os.path.isfile(py_exec) and not os.access(py_exec, os.X_OK):
      validationItems.append({"config-name": "python_interpreter_path",
                              "item": self.getErrorItem("Invalid Python interpreter path specified")})
      return self.toConfigurationValidationProblems(validationItems, "jnbg-env")

    distro_version = platform.linux_distribution()[1]
    if distro_version < "7.0" and (py_exec == "/opt/rh/python27/root/usr/bin/python" or py_exec == "/opt/rh/python27/root/usr/bin/python2" or py_exec == "/opt/rh/python27/root/usr/bin/python2.7"):
      # Special handling for RHSCL Python 2.7
      proc = subprocess32.Popen(['/usr/bin/scl', 'enable', 'python27', '/opt/rh/python27/root/usr/bin/python' ' -V'], stderr=subprocess32.PIPE)
    else:
      proc = subprocess32.Popen([py_exec, '-V'], stderr=subprocess32.PIPE)
    py_string = proc.communicate()[1]
    py_version = py_string.split()[1]

    if "Python" not in py_string:
      validationItems.append({"config-name": "python_interpreter_path",
                              "item": self.getErrorItem("Path specified does not appear to be a Python interpreter")})
      return self.toConfigurationValidationProblems(validationItems, "jnbg-env")

    # Validate that the specified python is 2.7.x (not > 2.x.x and not < 2.7)
    if not py_version.split('.')[0] == '2' or (py_version.split('.')[0] == '2' and py_version.split('.')[1] < '7'):
      validationItems.append({"config-name": "python_interpreter_path",
                              "item": self.getErrorItem("Specified Python interpreter must be version >= 2.7 and < 3.0")})
      return self.toConfigurationValidationProblems(validationItems, "jnbg-env")

    return self.toConfigurationValidationProblems(validationItems, "jnbg-env")

  def recommendRangerConfigurations(self, configurations, clusterData, services, hosts):
    super(BigInsights425StackAdvisor, self).recommendRangerConfigurations(configurations, clusterData, services, hosts)
    putRangerAdminProperty = self.putProperty(configurations, "ranger-admin-site", services)

    zookeeper_host_port = self.getZKHostPortString(services)
    zookeeper_host_port = zookeeper_host_port.split(',')
    zookeeper_host_port.sort()
    zookeeper_host_port = ",".join(zookeeper_host_port)
    ranger_audit_zk_port = '{0}/{1}'.format(zookeeper_host_port, 'solr')
    putRangerAdminProperty('ranger.audit.solr.zookeepers', ranger_audit_zk_port)

  def recommendTitanConfigurations(self, configurations, clusterData, services, hosts):
    putTitanPropertyAttribute = self.putPropertyAttribute(configurations, "titan-env")
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    knox_enabled = "KNOX" in servicesList
    if knox_enabled:
      putTitanPropertyAttribute("SimpleAuthenticator", "visible", "false")
 
  def recommendSolrConfigurations(self, configurations, clusterData, services, hosts):
    super(BigInsights425StackAdvisor, self).recommendSolrConfigurations(configurations, clusterData, services, hosts)

    # Update ranger-solr-plugin-properties/ranger-solr-plugin-enabled to match ranger-env/ranger-solr-plugin-enabled
    if "ranger-env" in services["configurations"] \
      and "ranger-solr-plugin-properties" in services["configurations"] \
      and "ranger-solr-plugin-enabled" in services["configurations"]["ranger-env"]["properties"]:
      putSolrRangerPluginProperty = self.putProperty(configurations, "ranger-solr-plugin-properties", services)
      ranger_solr_plugin_enabled = services["configurations"]["ranger-env"]["properties"]["ranger-solr-plugin-enabled"]
      putSolrRangerPluginProperty("ranger-solr-plugin-enabled", ranger_solr_plugin_enabled)

    # Determine if the Ranger/Solr Plugin is enabled
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    ranger_plugin_enabled = "RANGER" in servicesList
    # Only if the RANGER service is installed....
    if ranger_plugin_enabled:
      # If ranger-solr-plugin-properties/ranger-solr-plugin-enabled,
      # determine if the Ranger/Solr plug-in enabled enabled or not
      if 'ranger-solr-plugin-properties' in configurations and \
          'ranger-solr-plugin-enabled' in configurations['ranger-solr-plugin-properties']['properties']:
        ranger_plugin_enabled = configurations['ranger-solr-plugin-properties']['properties']['ranger-solr-plugin-enabled'].lower() == 'yes'
      # If ranger-solr-plugin-properties/ranger-solr-plugin-enabled was not changed,
      # determine if the Ranger/Solr plug-in enabled enabled or not
      elif 'ranger-solr-plugin-properties' in services['configurations'] and \
          'ranger-solr-plugin-enabled' in services['configurations']['ranger-solr-plugin-properties']['properties']:
        ranger_plugin_enabled = services['configurations']['ranger-solr-plugin-properties']['properties']['ranger-solr-plugin-enabled'].lower() == 'yes'

  def validateSolrRangerPluginConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    solr = properties
    validationItems = []
    ranger_plugin_properties = getSiteProperties(configurations, "ranger-solr-plugin-properties")
    ranger_plugin_enabled = ranger_plugin_properties['ranger-solr-plugin-enabled'] if ranger_plugin_properties else 'No'
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    security_enabled = self.isSecurityEnabled(services)

    if ("RANGER" in servicesList) and (ranger_plugin_enabled.lower() == 'yes') and not security_enabled:
          validationItems.append({"config-name": "ranger-solr-plugin-enabled",
                                  "item": self.getWarnItem(
                                  "Ranger Solr plugin should not be enabled in non-kerberos environment.")})

    return self.toConfigurationValidationProblems(validationItems, "ranger-solr-plugin-properties")

  def recommendHDFSConfigurations(self, configurations, clusterData, services, hosts):
    super(BigInsights425StackAdvisor, self).recommendHDFSConfigurations(configurations, clusterData, services, hosts)
  
  def recommendHadoopProxyUsers (self, configurations, services, hosts):
    super(BigInsights425StackAdvisor, self).recommendHadoopProxyUsers (configurations, services, hosts)
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]   
    users = {} 
   
    if 'forced-configurations' not in services:
      services["forced-configurations"] = []

    hive_user = None
    if "HIVE" in servicesList:
      if "hive-env" in services["configurations"] and "hive_user" in services["configurations"]["hive-env"]["properties"]:
        hive_user = services["configurations"]["hive-env"]["properties"]["hive_user"]
        if hive_user is not None:
           users[hive_user] = {"propertyHosts" : "*","propertyGroups" : "*", "config" : "hive-env", "propertyName" : "hive_user"}
           
    putCoreSiteProperty = self.putProperty(configurations, "core-site", services)
    putCoreSitePropertyAttribute = self.putPropertyAttribute(configurations, "core-site")   
    
    if hive_user is not None:
       user_properties = users[hive_user] 
       if "propertyHosts" in user_properties:
           services["forced-configurations"].append({"type" : "core-site", "name" : "hadoop.proxyuser.{0}.hosts".format(hive_user)})
           # Add properties "hadoop.proxyuser.*.hosts", "hadoop.proxyuser.*.groups" to core-site for hive user
           putCoreSiteProperty("hadoop.proxyuser.{0}.hosts".format(hive_user) , user_properties["propertyHosts"])
           Logger.info("Updated hadoop.proxyuser.{0}.hosts as : {1}".format(hive_user, user_properties["propertyHosts"]))
           putCoreSiteProperty("hadoop.proxyuser.{0}.groups".format(hive_user) , user_properties["propertyGroups"])
           Logger.info("Updated hadoop.proxyuser.{0}.groups : {1}".format(hive_user, user_properties["propertyGroups"]))
        
    self.recommendAmbariProxyUsersForHDFS(services, servicesList, putCoreSiteProperty, putCoreSitePropertyAttribute)
