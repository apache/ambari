#!/usr/bin/env python2.6

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
from Hardware import Hardware
from ActionQueue import ActionQueue
from ServerStatus import ServerStatus
from StatusCheck import StatusCheck
import AmbariConfig
import socket
import time
import traceback
from pprint import pprint, pformat

logger = logging.getLogger()

COMPONENTS = [
               {"serviceName" : "HDFS",
                "componentName" : "DATANODE"},
               {"serviceName" : "HDFS",
                "componentName" : "NAMENODE"},
               {"serviceName" : "HDFS",
                "componentName" : "SECONDARYNAMENODE"}
]

LIVE_STATUS = "LIVE"
DEAD_STATUS = "DEAD"

firstContact = True
class Heartbeat:

  def __init__(self, actionQueue):
    self.actionQueue = actionQueue
    self.reports = []

  def build(self, id='-1'):
    global clusterId, clusterDefinitionRevision, firstContact
    serverStatus = ServerStatus()
    timestamp = int(time.time()*1000)
    queueResult = self.actionQueue.result()
    installedRoleStates = serverStatus.build()
    pidLookupPath = AmbariConfig.config.get('services','pidLookupPath')
    serviceToPidMapFile = AmbariConfig.config.get('services','serviceToPidMapFile')
    statusCheck = StatusCheck(pidLookupPath, serviceToPidMapFile)
    servicesStatusesDict = {}
    componentStatus = []
    for component in COMPONENTS:
      serviceStatus = statusCheck.getStatus(component["componentName"])
      if serviceStatus == None:
        logger.warn("There is no service to pid mapping for " + component["componentName"])
      status = LIVE_STATUS if serviceStatus else DEAD_STATUS 
      componentStatus.append({"componentName" : component["componentName"],
                                   "msg" : "",
                                   "status" : status,
                                   "serviceName" : component["serviceName"],
                                   "clusterName" : ""})
     
    
    nodeStatus = { "status" : "HEALTHY",
                   "cause" : "NONE"}
    
    heartbeat = { 'responseId'        : int(id),
                  'timestamp'         : timestamp,
                  'hostname'          : socket.gethostname(),
                  'componentStatus'   : componentStatus,
                  'nodeStatus'        : nodeStatus
                }
    if len(queueResult) != 0:
      heartbeat['reports'] = queueResult
      pass
    logger.info("Status for node heartbeat: " + pformat(nodeStatus))
    return heartbeat

def main(argv=None):
  actionQueue = ActionQueue()
  heartbeat = Heartbeat(actionQueue)
  print json.dumps(heartbeat.build())

if __name__ == '__main__':
  main()
