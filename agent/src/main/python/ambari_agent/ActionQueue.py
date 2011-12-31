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
import os
import time
import subprocess

logger = logging.getLogger()
installScriptHash = -1

class ActionQueue(threading.Thread):
  global q, r, clusterId, clusterDefinitionRevision
  q = Queue.Queue()
  r = Queue.Queue()
  clusterId = 'unknown'
  clusterDefinitionRevision = 0

  def __init__(self, config):
    global clusterId, clusterDefinitionRevision 
    super(ActionQueue, self).__init__()
    #threading.Thread.__init__(self)
    self.config = config
    self.sh = shellRunner()
    self._stop = threading.Event()
    self.maxRetries = config.getint('command', 'maxretries') 
    self.sleepInterval = config.getint('command', 'sleepBetweenRetries')

  def stop(self):
    self._stop.set()

  def stopped(self):
    return self._stop.isSet()

  #For unittest
  def getshellinstance(self):
    return self.sh

  def put(self, response):
    if 'actions' in response:
      actions = response['actions']
      logger.debug(actions)
      # for the servers, take a diff of what's running, and what the controller
      # asked the agent to start. Kill all those servers that the controller
      # didn't ask us to start
      sh = shellRunner()
      runningServers = sh.getServerTracker()

      # get the list of servers the controller wants running
      serversToRun = {}
      for action in actions:
        if action['kind'] == 'START_ACTION':
          processKey = sh.getServerKey(action['clusterId'],action['clusterDefinitionRevision'],
            action['component'], action['role'])
          serversToRun[processKey] = 1

      # create stop actions for the servers that the controller wants stopped
      for server in runningServers.keys():
        if server not in serversToRun:
          sh.stopProcess(server)
      # now put all the actions in the queue. The ordering is important (we stopped
      # all unneeded servers first)
      for action in actions:
        q.put(action)

  def run(self):
    global clusterId, clusterDefinitionRevision
    while not self.stopped():
      while not q.empty():
        action = q.get()
        switches = {
                     'START_ACTION'              : self.startAction,
                     'RUN_ACTION'                : self.runAction,
                     'CREATE_STRUCTURE_ACTION'   : self.createStructureAction,
                     'DELETE_STRUCTURE_ACTION'   : self.deleteStructureAction,
                     'WRITE_FILE_ACTION'         : self.writeFileAction,
                     'INSTALL_AND_CONFIG_ACTION' : self.installAndConfigAction,
                     'NO_OP_ACTION'              : self.noOpAction
                   }
        
        exitCode = 1
        retryCount = 1
        while (exitCode != 0 and retryCount <= self.maxRetries):
          try:
            result = switches.get(action['kind'], self.unknownAction)(action) 
            if ('commandResult' in result):
              commandResult = result['commandResult']
              exitCode = commandResult['exitCode']
              if (exitCode == 0):
                break
              else:
                logger.warn(str(action) + " exited with code " + str(exitCode))
            else:
              #Really, no commandResult? Is this possible?
              #TODO: check
              exitCode = 0
              break
          except Exception, err:
            traceback.print_exc()  
            logger.warn(err)
            if ('commandResult' in result):
              commandResult = result['commandResult']
              if ('exitCode' in commandResult):
                exitCode = commandResult['exitCode']
          #retry in 5 seconds  
          time.sleep(self.sleepInterval)
          retryCount += 1
          
        if (exitCode != 0):
          result = self.genResult(action)
          result['exitCode']=exitCode
          result['retryActionCount'] = retryCount - 1
        else:
          result['retryActionCount'] = retryCount
        # Update the result
        r.put(result)
      if not self.stopped():
        time.sleep(5)

  # Store action result to agent response queue
  def result(self):
    result = []
    while not r.empty():
      result.append(r.get())
    return result

  # Generate default action response
  def genResult(self, action):
    result={}
    if (action['kind'] == 'INSTALL_AND_CONFIG_ACTION' or action['kind'] == 'NO_OP_ACTION'):
      result = {
               'id'                        : action['id'],
               'kind'                      : action['kind'],
             }
    else:
      result = { 
               'id'                        : action['id'],
               'clusterId'                 : action['clusterId'],
               'kind'                      : action['kind'],
               'clusterDefinitionRevision' : action['clusterDefinitionRevision'],
               'componentName'             : action['component'],
               'role'                      : action['role']
             }
    return result

  # Run start action, start a server process and
  # track the liveness of the children process
  def startAction(self, action):
    result = self.genResult(action)
    return self.sh.startProcess(action['clusterId'],
      action['clusterDefinitionRevision'],
      action['component'], 
      action['role'], 
      action['command'], 
      action['user'], result)

  # Write file action
  def writeFileAction(self, action, fileName=""):
    result = self.genResult(action)
    return writeFile(action, result, fileName)

  # get the install file
  def getInstallFilename(self,id):
    return "ambari-install-file-"+id

  # Install and configure action
  def installAndConfigAction(self, action):
    global installScriptHash
    r=self.genResult(action)
    w = self.writeFileAction(action,self.getInstallFilename(action['id']))
    commandResult = {}
    if w['exitCode']!=0:
      commandResult['error'] = w['stderr'] 
      commandResult['exitCode'] = w['exitCode']
      r['commandResult'] = commandResult
      return r
     
    if 'command' not in action:
      # this is hardcoded to do puppet specific stuff for now
      # append the content of the puppet file to the file written above
      filepath = getFilePath(action,self.getInstallFilename(action['id'])) 
      logger.info("File path for puppet top level script: " + filepath)
      p = self.sh.run(['/bin/cat',AmbariConfig.config.get('puppet','driver')])
      if p['exitCode']!=0:
        commandResult['error'] = p['error']
        commandResult['exitCode'] = p['exitCode']
        r['commandResult'] = commandResult
        return r
      logger.debug("The contents of the static file " + p['output'])
      appendToFile(p['output'],filepath) 
      arr = [AmbariConfig.config.get('puppet','commandpath') , filepath]
      logger.debug(arr)
      action['command'] = arr
    logger.debug(action['command'])
    commandResult = self.sh.run(action['command'])
    logger.debug("PUPPET COMMAND OUTPUT: " + commandResult['output'])
    logger.debug("PUPPET COMMAND ERROR: " + commandResult['error'])
    if commandResult['exitCode'] == 0:
      installScriptHash = action['id'] 
    r['commandResult'] = commandResult
    return r

  # Run command action
  def runAction(self, action):
    result = self.genResult(action)
    return self.sh.runAction(action['clusterId'], 
      action['component'],
      action['role'],
      action['user'], 
      action['command'], 
      action['cleanUpCommand'], result)

  # Create directory structure for cluster
  def createStructureAction(self, action):
    result = self.genResult(action)
    result['exitCode'] = 0
    return createStructure(action, result)

  # Delete directory structure for cluster
  def deleteStructureAction(self, action):
    result = self.genResult(action)
    result['exitCode'] = 0
    return deleteStructure(action, result)

  def noOpAction(self, action):
    r = {'id' : action['id']}
    return r

  # Handle unknown action
  def unknownAction(self, action):
    logger.error('Unknown action: %s' % action['id'])
    result = { 'id': action['id'] }
    return result

  # Discover agent idle state
  def isIdle(self):
    return q.empty()

  # Get the hash of the script currently used for install/config
  def getInstallScriptHash(self):
    return installScriptHash
