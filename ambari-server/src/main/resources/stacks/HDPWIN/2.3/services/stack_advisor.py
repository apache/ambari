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

class HDPWIN23StackAdvisor(HDPWIN22StackAdvisor):

  def getServiceConfigurationRecommenderDict(self):
    parentRecommendConfDict = super(HDPWIN23StackAdvisor, self).getServiceConfigurationRecommenderDict()
    childRecommendConfDict = {
      "TEZ": self.recommendTezConfigurations,
      "OOZIE": self.recommendOozieConfigurations
    }
    parentRecommendConfDict.update(childRecommendConfDict)
    return parentRecommendConfDict

  def recommendTezConfigurations(self, configurations, clusterData, services, hosts):
    super(HDPWIN23StackAdvisor, self).recommendTezConfigurations(configurations, clusterData, services, hosts)

    putTezProperty = self.putProperty(configurations, "tez-site")
    # remove 2gb limit for tez.runtime.io.sort.mb
    # in HDPWIN 2.3 "tez.runtime.sorter.class" is set by default to PIPELINED, in other case comment calculation code below
    taskResourceMemory = clusterData['mapMemory'] if clusterData['mapMemory'] > 2048 else int(clusterData['reduceMemory'])
    taskResourceMemory = min(clusterData['containers'] * clusterData['ramPerContainer'], taskResourceMemory)
    putTezProperty("tez.runtime.io.sort.mb", int(taskResourceMemory * 0.4))

    if "tez-site" in services["configurations"] and "tez.runtime.sorter.class" in services["configurations"]["tez-site"]["properties"]:
      if services["configurations"]["tez-site"]["properties"]["tez.runtime.sorter.class"] == "LEGACY":
        putTezAttribute = self.putPropertyAttribute(configurations, "tez-site")
        putTezAttribute("tez.runtime.io.sort.mb", "maximum", 2047)

  def recommendOozieConfigurations(self, configurations, clusterData, services, hosts):
    super(HDPWIN23StackAdvisor, self).recommendOozieConfigurations(configurations, clusterData, services, hosts)

    oozieSiteProperties = getSiteProperties(services['configurations'], 'oozie-site')
    oozieEnvProperties = getSiteProperties(services['configurations'], 'oozie-env')
    putOozieProperty = self.putProperty(configurations, "oozie-site", services)
    putOozieEnvProperty = self.putProperty(configurations, "oozie-env", services)

    if oozieEnvProperties and oozieSiteProperties and self.checkSiteProperties(oozieSiteProperties, 'oozie.service.JPAService.jdbc.driver') and self.checkSiteProperties(oozieEnvProperties, 'oozie_database'):
      putOozieProperty('oozie.service.JPAService.jdbc.driver', self.getDBDriver(oozieEnvProperties['oozie_database']))
    if oozieSiteProperties and oozieEnvProperties and self.checkSiteProperties(oozieSiteProperties, 'oozie.db.schema.name', 'oozie.service.JPAService.jdbc.url') and self.checkSiteProperties(oozieEnvProperties, 'oozie_database'):
      oozieServerHost = self.getHostWithComponent('OOZIE', 'OOZIE_SERVER', services, hosts)
      if oozieServerHost is not None:
        dbConnection = self.getDBConnectionString(oozieEnvProperties['oozie_database']).format(oozieServerHost['Hosts']['host_name'], oozieSiteProperties['oozie.db.schema.name'])
        putOozieProperty('oozie.service.JPAService.jdbc.url', dbConnection)
