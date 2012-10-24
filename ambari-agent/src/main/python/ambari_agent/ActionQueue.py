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

import logging
import traceback
import logging.handlers
import Queue
import threading
import AmbariConfig
from shell import shellRunner
from FileUtil import writeFile, createStructure, deleteStructure, getFilePath, appendToFile
from shell import shellRunner
import json
import pprint
import os
import time
import subprocess
import copy
import puppetExecutor

logger = logging.getLogger()
installScriptHash = -1

class ActionQueue(threading.Thread):
  """ Action Queue for the agent. We pick one command at a time from the queue
  and execute that """
  global commandQueue, resultQueue
  commandQueue = Queue.Queue()
  resultQueue = Queue.Queue()
 
  def __init__(self, config):
    super(ActionQueue, self).__init__()
    #threading.Thread.__init__(self)
    self.config = config
    self.sh = shellRunner()
    self._stop = threading.Event()
    self.maxRetries = config.getint('command', 'maxretries') 
    self.sleepInterval = config.getint('command', 'sleepBetweenRetries')
    self.executor = puppetExecutor.puppetExecutor(config.get('puppet', 'puppetmodules'),
                                   config.get('puppet', 'puppet_home'),
                                   config.get('puppet', 'facter_home'),
                                   config.get('agent', 'prefix'))
  def stop(self):
    self._stop.set()

  def stopped(self):
    return self._stop.isSet()

  def getshellinstance(self):
    """ For Testing purpose only.""" 
    return self.sh

  def put(self, command):
    logger.info("The command from the server is \n" + pprint.pformat(command))
    commandQueue.put(command)
    pass

  def run(self):
    result = []
    while not self.stopped():
      while not commandQueue.empty():
        command = commandQueue.get()
        try:
          #pass a copy of action since we don't want anything to change in the 
          #action dict 
          commandCopy = copy.copy(command)
          result = self.executeCommand(commandCopy)
          
        except Exception, err:
          traceback.print_exc()  
          logger.warn(err)
          pass
        
        for entry in result:
          resultQueue.put(entry)
        pass
      if not self.stopped():
        time.sleep(5)

  # Store action result to agent response queue
  def result(self):
    result = []
    while not resultQueue.empty():
      result.append(resultQueue.get())
    return result

  def registerCommand(self, command):
    return {}
  
  def statusCommand(self, command):
    return {}
  
  def executeCommand(self, command):
    logger.info("Executing command \n" + pprint.pformat(command))
    clusterName = command['clusterName']
    commandId = command['commandId']
    hostname = command['hostname']
    params = command['hostLevelParams']
    clusterHostInfo = command['clusterHostInfo']
    roleCommand = command['roleCommand']
    serviceName = command['serviceName']
    configurations = command['configurations']
    result = []
    commandresult = self.executor.runCommand(command)
    status = "COMPLETED"
    if (commandresult['exitcode'] != 0):
      status = "FAILED"
      
    # assume some puppet pluing to run these commands
    roleResult = {'role' : command['role'],
                  'actionId' : commandId,
                  'stdout' : commandresult['stdout'],
                  'clusterName' : clusterName,
                  'stderr' : commandresult['stderr'],
                  'exitCode' : commandresult['exitcode'],
                  'serviceName' : serviceName,
                  'status' : status}
    result.append(roleResult)
    pass
    return result

  def noOpCommand(self, command):
    result = {'commandId' : command['Id']}
    return result

  def unknownAction(self, action):
    logger.error('Unknown action: %s' % action['id'])
    result = { 'id': action['id'] }
    return result

  def isIdle(self):
    return commandQueue.empty()
