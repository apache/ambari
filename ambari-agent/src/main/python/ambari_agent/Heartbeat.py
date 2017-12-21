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

import ambari_simplejson as json
import logging
import os
import time
from pprint import pformat

from ambari_agent.hostname import hostname
from ambari_agent.HostInfo import HostInfo
from ambari_agent.Hardware import Hardware


logger = logging.getLogger(__name__)

firstContact = True
class Heartbeat:

  def __init__(self, actionQueue, config=None, alert_collector=None):
    self.actionQueue = actionQueue
    self.config = config
    self.reports = []
    self.collector = alert_collector

  def build(self, id='-1', add_state=False, componentsMapped=False):
    global clusterId, clusterDefinitionRevision, firstContact
    timestamp = int(time.time()*1000)
    queueResult = self.actionQueue.result()
    recovery_timestamp = self.actionQueue.controller.recovery_manager.recovery_timestamp

    nodeStatus = { "status" : "HEALTHY",
                   "cause" : "NONE" }

    heartbeat = { 'responseId'        : int(id),
                  'timestamp'         : timestamp,
                  'hostname'          : hostname(self.config),
                  'nodeStatus'        : nodeStatus,
                  'recoveryTimestamp' : recovery_timestamp
                }

    rec_status = self.actionQueue.controller.recovery_manager.get_recovery_status()
    heartbeat['recoveryReport'] = rec_status

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

    if logger.isEnabledFor(logging.DEBUG):
      logger.debug("Building Heartbeat: {responseId = %s, timestamp = %s, "
                   "commandsInProgress = %s, componentsMapped = %s, "
                   "recoveryTimestamp = %s}",
                   id, timestamp, commandsInProgress, componentsMapped, recovery_timestamp)
      logger.debug("Heartbeat: %s", pformat(heartbeat))

    hostInfo = HostInfo(self.config)
    if add_state:
      logger.info("Adding host info/state to heartbeat message.")
      nodeInfo = { }
      # for now, just do the same work as registration
      # this must be the last step before returning heartbeat
      hostInfo.register(nodeInfo, componentsMapped, commandsInProgress)
      heartbeat['agentEnv'] = nodeInfo
      mounts = Hardware(config=self.config, cache_info=False).osdisks()
      heartbeat['mounts'] = mounts

      if logger.isEnabledFor(logging.DEBUG):
        logger.debug("agentEnv: %s", nodeInfo)
        logger.debug("mounts: %s", mounts)

    if self.collector is not None:
      heartbeat['alerts'] = self.collector.alerts()
    
    return heartbeat


def main(argv=None):
  from ambari_agent.ActionQueue import ActionQueue
  from ambari_agent.AmbariConfig import AmbariConfig
  from ambari_agent.Controller import Controller

  cfg = AmbariConfig()
  config_file_path = AmbariConfig.getConfigFile(home_dir="")
  if os.path.exists(config_file_path):
    cfg.read(config_file_path)
  else:
    raise Exception("No config found, use default")

  ctl = Controller(cfg)
  actionQueue = ActionQueue(cfg, ctl)
  heartbeat = Heartbeat(actionQueue)
  print json.dumps(heartbeat.build('3',3))


if __name__ == '__main__':
  main()
