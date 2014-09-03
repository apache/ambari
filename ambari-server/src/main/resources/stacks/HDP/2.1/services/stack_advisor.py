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

class HDP21StackAdvisor(HDP206StackAdvisor):

  def recommendServiceConfigurations(self, service):
    calculator = super(HDP21StackAdvisor, self).recommendServiceConfigurations(service)
    if calculator is None:
      return {
        "OOZIE": self.recommendOozieConfigurations,
        "HIVE": self.recommendHiveConfigurations,
        "TEZ": self.recommendTezConfigurations
      }.get(service, None)
    else:
      return calculator

  def recommendOozieConfigurations(self, configurations, clusterData):
    if "FALCON_SERVER" in clusterData["components"]:
      putMapredProperty = self.putProperty(configurations, "oozie-site")
      putMapredProperty("oozie.services.ext",
                        "org.apache.oozie.service.JMSAccessorService," +
                        "org.apache.oozie.service.PartitionDependencyManagerService," +
                        "org.apache.oozie.service.HCatAccessorService")

  def recommendHiveConfigurations(self, configurations, clusterData):
    containerSize = clusterData['mapMemory'] if clusterData['mapMemory'] > 2048 else clusterData['reduceMemory']
    containerSize = min(clusterData['containers'] * clusterData['ramPerContainer'], containerSize)
    putHiveProperty = self.putProperty(configurations, "hive-site")
    putHiveProperty('hive.auto.convert.join.noconditionaltask.size', int(containerSize / 3) * 1048576)
    putHiveProperty('hive.tez.java.opts', "-server -Xmx" + str(int(0.8 * containerSize))
                    + "m -Djava.net.preferIPv4Stack=true -XX:NewRatio=8 -XX:+UseNUMA -XX:+UseParallelGC")
    putHiveProperty('hive.tez.container.size', containerSize)

  def recommendTezConfigurations(self, configurations, clusterData):
    putTezProperty = self.putProperty(configurations, "tez-site")
    putTezProperty("tez.am.resource.memory.mb", clusterData['amMemory'])
    putTezProperty("tez.am.java.opts",
                   "-server -Xmx" + str(int(0.8 * clusterData["amMemory"]))
                   + "m -Djava.net.preferIPv4Stack=true -XX:+UseNUMA -XX:+UseParallelGC")

  def getNotPreferableOnServerComponents(self):
    return ['STORM_UI_SERVER', 'DRPC_SERVER', 'STORM_REST_API', 'NIMBUS', 'GANGLIA_SERVER', 'NAGIOS_SERVER']

  def getNotValuableComponents(self):
    return ['JOURNALNODE', 'ZKFC', 'GANGLIA_MONITOR', 'APP_TIMELINE_SERVER']

  def selectionSchemes(self):
    return {
      'NAMENODE': {"else": 0},
      'SECONDARY_NAMENODE': {"else": 1},
      'HBASE_MASTER': {6: 0, 31: 2, "else": 3},

      'HISTORYSERVER': {31: 1, "else": 2},
      'RESOURCEMANAGER': {31: 1, "else": 2},

      'OOZIE_SERVER': {6: 1, 31: 2, "else": 3},

      'HIVE_SERVER': {6: 1, 31: 2, "else": 4},
      'HIVE_METASTORE': {6: 1, 31: 2, "else": 4},
      'WEBHCAT_SERVER': {6: 1, 31: 2, "else": 4},
      }

  def selectionScheme(self):
    parentSchemes = super(HDP21StackAdvisor, self).selectionSchemes()
    childSchemes = {
        'APP_TIMELINE_SERVER': {31: 1, "else": 2},
        'FALCON_SERVER': {6: 1, 31: 2, "else": 3}
    }
    parentSchemes.update(childSchemes)
    return parentSchemes

  def getServiceConfigurationValidators(self):
    parentValidators = super(HDP21StackAdvisor, self).getServiceConfigurationValidators()
    childValidators = {
      "HIVE": ["hive-site", self.validateHiveConfigurations],
      "TEZ": ["tez-site", self.validateTezConfigurations]
    }
    parentValidators.update(childValidators)
    return parentValidators

  def validateHiveConfigurations(self, properties, recommendedDefaults):
    validationItems = [ {"config-name": 'hive.tez.container.size', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'hive.tez.container.size')},
                        {"config-name": 'hive.tez.java.opts', "item": self.validateXmxValue(properties, recommendedDefaults, 'hive.tez.java.opts')},
                        {"config-name": 'hive.auto.convert.join.noconditionaltask.size', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'hive.auto.convert.join.noconditionaltask.size')} ]
    return self.toConfigurationValidationProblems(validationItems, "hive-site")

  def validateTezConfigurations(self, properties, recommendedDefaults):
    validationItems = [ {"config-name": 'tez.am.resource.memory.mb', "item": self.validatorLessThenDefaultValue(properties, recommendedDefaults, 'tez.am.resource.memory.mb')},
                        {"config-name": 'tez.am.java.opts', "item": self.validateXmxValue(properties, recommendedDefaults, 'tez.am.java.opts')} ]
    return self.toConfigurationValidationProblems(validationItems, "tez-site")


