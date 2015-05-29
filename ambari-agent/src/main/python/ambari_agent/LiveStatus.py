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
from StatusCheck import StatusCheck
import AmbariConfig
from StackVersionsFileHandler import StackVersionsFileHandler
from ActualConfigHandler import ActualConfigHandler

logger = logging.getLogger()

class LiveStatus:

  SERVICES = []
  CLIENT_COMPONENTS = []
  COMPONENTS = []

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

  def build(self, forced_component_status = None):
    """
    If forced_component_status is explicitly defined, than StatusCheck methods are
    not used. This feature has been added to support custom (ver 2.0) services.
    """
    global SERVICES, CLIENT_COMPONENTS, COMPONENTS, LIVE_STATUS, DEAD_STATUS

    component = {"serviceName" : self.service, "componentName" : self.component}
    if forced_component_status: # If already determined
      status = forced_component_status  # Nothing to do
    elif component in self.CLIENT_COMPONENTS:
      status = self.DEAD_STATUS # CLIENT components can't have status STARTED
    elif component in self.COMPONENTS:
      statusCheck = StatusCheck(AmbariConfig.servicesToPidNames,
                                AmbariConfig.pidPathVars, self.globalConfig,
                                AmbariConfig.servicesToLinuxUser)
      serviceStatus = statusCheck.getStatus(self.component)
      if serviceStatus is None:
        logger.warn("There is no service to pid mapping for " + self.component)
      status = self.LIVE_STATUS if serviceStatus else self.DEAD_STATUS

    livestatus = {"componentName" : self.component,
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
