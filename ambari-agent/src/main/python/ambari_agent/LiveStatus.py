#!/usr/bin/env python

'''
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
'''

import json
import logging
from StatusCheck import StatusCheck
import AmbariConfig
from StackVersionsFileHandler import StackVersionsFileHandler
from ActualConfigHandler import ActualConfigHandler

logger = logging.getLogger()

class LiveStatus:

  SERVICES = [
    "HDFS", "MAPREDUCE", "GANGLIA", "HBASE",
    "NAGIOS", "ZOOKEEPER", "OOZIE", "HCATALOG",
    "KERBEROS", "TEMPLETON", "HIVE", "WEBHCAT",
    "YARN", "MAPREDUCE2"
  ]

  COMPONENTS = [
      {"serviceName" : "HDFS",
       "componentName" : "DATANODE"},
      {"serviceName" : "HDFS",
       "componentName" : "NAMENODE"},
      {"serviceName" : "HDFS",
       "componentName" : "SECONDARY_NAMENODE"},

      {"serviceName" : "MAPREDUCE",
       "componentName" : "JOBTRACKER"},
      {"serviceName" : "MAPREDUCE",
       "componentName" : "TASKTRACKER"},

      {"serviceName" : "GANGLIA",
       "componentName" : "GANGLIA_SERVER"},
      {"serviceName" : "GANGLIA",
       "componentName" : "GANGLIA_MONITOR"},

      {"serviceName" : "HBASE",
       "componentName" : "HBASE_MASTER"},
      {"serviceName" : "HBASE",
       "componentName" : "HBASE_REGIONSERVER"},

      {"serviceName" : "NAGIOS",
       "componentName" : "NAGIOS_SERVER"},

      {"serviceName" : "ZOOKEEPER",
       "componentName" : "ZOOKEEPER_SERVER"},

      {"serviceName" : "OOZIE",
       "componentName" : "OOZIE_SERVER"},

      {"serviceName" : "HCATALOG",
       "componentName" : "HCATALOG_SERVER"},

      {"serviceName" : "KERBEROS",
       "componentName" : "KERBEROS_SERVER"},

      {"serviceName" : "HIVE",
       "componentName" : "HIVE_SERVER"},
      {"serviceName" : "HIVE",
       "componentName" : "HIVE_METASTORE"},
      {"serviceName" : "HIVE",
       "componentName" : "MYSQL_SERVER"},

      {"serviceName" : "WEBHCAT",
       "componentName" : "WEBHCAT_SERVER"},

      {"serviceName" : "YARN",
       "componentName" : "RESOURCEMANAGER"},
      {"serviceName" : "YARN",
       "componentName" : "NODEMANAGER"},

      {"serviceName" : "MAPREDUCE2",
       "componentName" : "HISTORYSERVER"},
  ]

  LIVE_STATUS = "STARTED"
  DEAD_STATUS = "INSTALLED"

  def __init__(self, cluster, service, component, globalConfig, config):
    self.cluster = cluster
    self.service = service
    self.component = component
    self.globalConfig = globalConfig
    versionsFileDir = config.get('agent', 'prefix')
    self.versionsHandler = StackVersionsFileHandler(versionsFileDir)
    self.actualConfigHandler = ActualConfigHandler(config)

  def belongsToService(self, component):
    #TODO: Should also check belonging of server to cluster
    return component['serviceName'] == self.service

  # Live status was stripped from heartbeat after revision e1718dd
  def build(self):
    global SERVICES, COMPONENTS, LIVE_STATUS, DEAD_STATUS
    statusCheck = StatusCheck(AmbariConfig.servicesToPidNames, AmbariConfig.pidPathesVars, self.globalConfig, AmbariConfig.linuxUserPattern)
    livestatus = None
    for component in self.COMPONENTS:
      if component["serviceName"] == self.service and component["componentName"] == self.component:
        serviceStatus = statusCheck.getStatus(component["componentName"])
        if serviceStatus is None:
          logger.warn("There is no service to pid mapping for " + component["componentName"])
        status = self.LIVE_STATUS if serviceStatus else self.DEAD_STATUS
        livestatus ={"componentName" : component["componentName"],
                       "msg" : "",
                       "status" : status,
                       "clusterName" : self.cluster,
                       "serviceName" : self.service,
                       "stackVersion": self.versionsHandler.
                                    read_stack_version(component["componentName"])
                    }
        active_config = self.actualConfigHandler.read_actual_component(component['componentName'])
        if not active_config is None:
          livestatus['configurationTags'] = active_config
        break
    logger.debug("The live status for component " + str(self.component) +\
                " of service " + str(self.service) + " is " + str(livestatus))
    return livestatus

def main(argv=None):
  for service in SERVICES:
    livestatus = LiveStatus('', service)
    print json.dumps(livestatus.build())

if __name__ == '__main__':
  main()
