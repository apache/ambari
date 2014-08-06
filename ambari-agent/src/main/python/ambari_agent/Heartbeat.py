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
import time
from pprint import pformat

from ActionQueue import ActionQueue
import AmbariConfig
import hostname
from HostInfo import HostInfo
from Hardware import Hardware


logger = logging.getLogger()

firstContact = True
class Heartbeat:

  def __init__(self, actionQueue, config=None):
    self.actionQueue = actionQueue
    self.config = config
    self.reports = []

  def build(self, id='-1', state_interval=-1, componentsMapped=False):
    global clusterId, clusterDefinitionRevision, firstContact
    timestamp = int(time.time()*1000)
    queueResult = self.actionQueue.result()


    nodeStatus = { "status" : "HEALTHY",
                   "cause" : "NONE" }
    nodeStatus["alerts"] = []



    heartbeat = { 'responseId'        : int(id),
                  'timestamp'         : timestamp,
                  'hostname'          : hostname.hostname(self.config),
                  'nodeStatus'        : nodeStatus
                }

    commandsInProgress = False
    if not self.actionQueue.commandQueue.empty():
      commandsInProgress = True

    if len(queueResult) != 0:
      heartbeat['reports'] = queueResult['reports']
      heartbeat['componentStatus'] = queueResult['componentStatus']
      if len(heartbeat['reports']) > 0:
        # There may be IN_PROGRESS tasks
        commandsInProgress = True
      pass

    # For first request/heartbeat assume no components are mapped
    if int(id) == 0:
      componentsMapped = False

    logger.info("Building Heartbeat: {responseId = %s, timestamp = %s, commandsInProgress = %s, componentsMapped = %s}",
        str(id), str(timestamp), repr(commandsInProgress), repr(componentsMapped))

    if logger.isEnabledFor(logging.DEBUG):
      logger.debug("Heartbeat: %s", pformat(heartbeat))

    hostInfo = HostInfo(self.config)
    if (int(id) >= 0) and state_interval > 0 and (int(id) % state_interval) == 0:
      nodeInfo = { }
      # for now, just do the same work as registration
      # this must be the last step before returning heartbeat
      hostInfo.register(nodeInfo, componentsMapped, commandsInProgress)
      heartbeat['agentEnv'] = nodeInfo
      mounts = Hardware.osdisks()
      heartbeat['mounts'] = mounts

      if logger.isEnabledFor(logging.DEBUG):
        logger.debug("agentEnv: %s", str(nodeInfo))
        logger.debug("mounts: %s", str(mounts))

    nodeStatus["alerts"] = hostInfo.createAlerts(nodeStatus["alerts"])
    return heartbeat

def main(argv=None):
  actionQueue = ActionQueue(AmbariConfig.config)
  heartbeat = Heartbeat(actionQueue)
  print json.dumps(heartbeat.build('3',3))

if __name__ == '__main__':
  main()
