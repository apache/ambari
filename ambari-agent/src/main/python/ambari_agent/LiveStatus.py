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
    "YARN", "MAPREDUCE2", "FLUME", "TEZ",
    "FALCON", "STORM"
  ]

  CLIENT_COMPONENTS = [
    {"serviceName" : "HBASE",
     "componentName" : "HBASE_CLIENT"},
    {"serviceName" : "HDFS",
     "componentName" : "HDFS_CLIENT"},
    {"serviceName" : "MAPREDUCE",
     "componentName" : "MAPREDUCE_CLIENT"},
    {"serviceName" : "ZOOKEEPER",
     "componentName" : "ZOOKEEPER_CLIENT"},
    {"serviceName" : "OOZIE",
     "componentName" : "OOZIE_CLIENT"},
    {"serviceName" : "HCATALOG",
     "componentName" : "HCAT"},
    {"serviceName" : "HIVE",
     "componentName" : "HIVE_CLIENT"},
    {"serviceName" : "YARN",
     "componentName" : "YARN_CLIENT"},
    {"serviceName" : "MAPREDUCE2",
     "componentName" : "MAPREDUCE2_CLIENT"},
    {"serviceName" : "PIG",
     "componentName" : "PIG"},
    {"serviceName" : "SQOOP",
     "componentName" : "SQOOP"},
    {"serviceName" : "TEZ",
     "componentName" : "TEZ_CLIENT"},
    {"serviceName" : "FALCON",
     "componentName" : "FALCON_CLIENT"}
  ]

  COMPONENTS = [
      {"serviceName" : "HDFS",
       "componentName" : "DATANODE"},
      {"serviceName" : "HDFS",
       "componentName" : "NAMENODE"},
      {"serviceName" : "HDFS",
       "componentName" : "SECONDARY_NAMENODE"},
      {"serviceName" : "HDFS",
       "componentName" : "JOURNALNODE"},
      {"serviceName" : "HDFS",
       "componentName" : "ZKFC"},

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

      {"serviceName" : "FLUME",
       "componentName" : "FLUME_SERVER"},

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
      {"serviceName" : "YARN",
       "componentName" : "APP_TIMELINE_SERVER"},

      {"serviceName" : "MAPREDUCE2",
       "componentName" : "HISTORYSERVER"},

      {"serviceName" : "FALCON",
       "componentName" : "FALCON_SERVER"},

      {"serviceName" : "STORM",
       "componentName" : "NIMBUS"},
      {"serviceName" : "STORM",
       "componentName" : "STORM_REST_API"},
      {"serviceName" : "STORM",
       "componentName" : "SUPERVISOR"},
      {"serviceName" : "STORM",
       "componentName" : "STORM_UI_SERVER"},
      {"serviceName" : "STORM",
       "componentName" : "DRPC_SERVER"}
  ]

  LIVE_STATUS = "STARTED"
  DEAD_STATUS = "INSTALLED"

  def __init__(self, cluster, service, component, globalConfig, config,
               configTags):
    self.cluster = cluster
    self.service = service
    self.component = component
    self.globalConfig = globalConfig
    versionsFileDir = config.get('agent', 'prefix')
    self.versionsHandler = StackVersionsFileHandler(versionsFileDir)
    self.configTags = configTags
    self.actualConfigHandler = ActualConfigHandler(config, configTags)

  def belongsToService(self, component):
    #TODO: Should also check belonging of server to cluster
    return component['serviceName'] == self.service

  def build(self, forsed_component_status = None):
    """
    If forsed_component_status is explicitly defined, than StatusCheck methods are
    not used. This feature has been added to support custom (ver 2.0) services.
    """
    global SERVICES, CLIENT_COMPONENTS, COMPONENTS, LIVE_STATUS, DEAD_STATUS

    livestatus = None
    component = {"serviceName" : self.service, "componentName" : self.component}
    if forsed_component_status: # If already determined
      status = forsed_component_status  # Nothing to do
    elif component in self.CLIENT_COMPONENTS:
      status = self.DEAD_STATUS # CLIENT components can't have status STARTED
    elif component in self.COMPONENTS:
      statusCheck = StatusCheck(AmbariConfig.servicesToPidNames,
                                AmbariConfig.pidPathesVars, self.globalConfig,
                                AmbariConfig.servicesToLinuxUser)
      serviceStatus = statusCheck.getStatus(self.component)
      if serviceStatus is None:
        logger.warn("There is no service to pid mapping for " + self.component)
      status = self.LIVE_STATUS if serviceStatus else self.DEAD_STATUS

    livestatus ={"componentName" : self.component,
                 "msg" : "",
                 "status" : status,
                 "clusterName" : self.cluster,
                 "serviceName" : self.service,
                 "stackVersion": self.versionsHandler.
                 read_stack_version(self.component)
    }
    active_config = self.actualConfigHandler.read_actual_component(self.component)
    if not active_config is None:
      livestatus['configurationTags'] = active_config

    logger.debug("The live status for component " + str(self.component) +\
                " of service " + str(self.service) + " is " + str(livestatus))
    return livestatus

def main(argv=None):
  for service in SERVICES:
    livestatus = LiveStatus('', service)
    print json.dumps(livestatus.build())

if __name__ == '__main__':
  main()
