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

import os
import time
import subprocess
from Hardware import Hardware
import hostname
from HostInfo import HostInfo


firstContact = True
class Register:
  """ Registering with the server. Get the hardware profile and
  declare success for now """
  def __init__(self, config):
    self.config = config
    self.hardware = Hardware(self.config)

  def build(self, version, id='-1'):
    global clusterId, clusterDefinitionRevision, firstContact
    timestamp = int(time.time()*1000)

    hostInfo = HostInfo(self.config)
    agentEnv = { }
    hostInfo.register(agentEnv, False, False)

    current_ping_port = self.config.get('agent','current_ping_port')

    register = { 'responseId'        : int(id),
                 'timestamp'         : timestamp,
                 'hostname'          : hostname.hostname(self.config),
                 'currentPingPort'   : int(current_ping_port),
                 'publicHostname'    : hostname.public_hostname(self.config),
                 'hardwareProfile'   : self.hardware.get(),
                 'agentEnv'          : agentEnv,
                 'agentVersion'      : version,
                 'prefix'            : self.config.get('agent', 'prefix')
               }
    return register
