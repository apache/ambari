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
from LiveStatus import LiveStatus
from shell import shellRunner
from FileUtil import writeFile, createStructure, deleteStructure, getFilePath, appendToFile
import json
import pprint
import os
import time
import subprocess
import copy
import puppetExecutor
import tempfile
from Grep import Grep

logger = logging.getLogger()
installScriptHash = -1

class ActionQueue(threading.Thread):
  """ Action Queue for the agent. We pick one command at a time from the queue
  and execute that """
  global commandQueue, resultQueue #, STATUS_COMMAND, EXECUTION_COMMAND
  commandQueue = Queue.Queue()
  resultQueue = Queue.Queue()

  STATUS_COMMAND='STATUS_COMMAND'
  EXECUTION_COMMAND='EXECUTION_COMMAND'
  IDLE_SLEEP_TIME = 5

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
                                   config.get('agent', 'prefix'), config)
    self.tmpdir = config.get('agent', 'prefix')
    self.commandInProgress = None

  def stop(self):
    self._stop.set()

  def stopped(self):
    return self._stop.isSet()

  def getshellinstance(self):
    """ For Testing purpose only.""" 
    return self.sh

  def put(self, command):
    logger.info("The " + command['commandType'] + " from the server is \n" + pprint.pformat(command))
    commandQueue.put(command)
    pass

  def getCommandQueue(self):
    """ For Testing purpose only."""
    return commandQueue

  def run(self):
    result = []
    while not self.stopped():
      while not commandQueue.empty():
        command = commandQueue.get()
        logger.info("Took an element of Queue: " + pprint.pformat(command))
        if command['commandType'] == self.EXECUTION_COMMAND:
          try:
            #pass a copy of action since we don't want anything to change in the
            #action dict
            result = self.executeCommand(command)

          except Exception, err:
            traceback.print_exc()
            logger.warn(err)
            pass

          for entry in result:
            resultQueue.put((ActionQueue.EXECUTION_COMMAND, entry))
          pass
        elif command['commandType'] == self.STATUS_COMMAND:
          cluster = command['clusterName']
          service = command['serviceName']
          component = command['componentName']
          try:
            livestatus = LiveStatus(cluster, service, component)
            result = livestatus.build()
            logger.info("Got live status for component " + component + " of service " + str(service) +\
                        " of cluster " + str(cluster) + "\n" + pprint.pformat(result))
            if result is not None:
              resultQueue.put((ActionQueue.STATUS_COMMAND, result))
          except Exception, err:
            traceback.print_exc()
            logger.warn(err)
            pass
        else:
          logger.warn("Unrecognized command " + pprint.pformat(result))
      if not self.stopped():
        time.sleep(self.IDLE_SLEEP_TIME)

  # Store action result to agent response queue
  def result(self):
    resultReports = []
    resultComponentStatus = []
    while not resultQueue.empty():
      res = resultQueue.get()
      if res[0] == ActionQueue.EXECUTION_COMMAND:
        resultReports.append(res[1])
      elif res[0] == ActionQueue.STATUS_COMMAND:
        resultComponentStatus.append(res[1])

    # Building report for command in progress
    if self.commandInProgress is not None:
      try:
        tmpout= open(self.commandInProgress['tmpout'], 'r').read()
        tmperr= open(self.commandInProgress['tmperr'], 'r').read()
      except Exception, err:
        logger.warn(err)
        tmpout='...'
        tmperr='...'
      grep = Grep()
      output = grep.tail(tmpout, puppetExecutor.puppetExecutor.OUTPUT_LAST_LINES)
      inprogress = {
        'role' : self.commandInProgress['role'],
        'actionId' : self.commandInProgress['actionId'],
        'taskId' : self.commandInProgress['taskId'],
        'stdout' : grep.filterMarkup(output),
        'clusterName' : self.commandInProgress['clusterName'],
        'stderr' : tmperr,
        'exitCode' : 777,
        'serviceName' : self.commandInProgress['serviceName'],
        'status' : 'IN_PROGRESS'
      }
      resultReports.append(inprogress)
    result={
      'reports' : resultReports,
      'componentStatus' : resultComponentStatus
    }
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

    taskId = command['taskId']
    # Preparing 'IN_PROGRESS' report
    self.commandInProgress = {
      'role' : command['role'],
      'actionId' : commandId,
      'taskId' : taskId,
      'clusterName' : clusterName,
      'serviceName' : serviceName,
      'tmpout': self.tmpdir + os.sep + 'output-' + str(taskId) + '.txt',
      'tmperr': self.tmpdir + os.sep + 'errors-' + str(taskId) + '.txt'
    }
    # running command
    commandresult = self.executor.runCommand(command, self.commandInProgress['tmpout'], self.commandInProgress['tmperr'])
    # dumping results
    self.commandInProgress = None
    status = "COMPLETED"
    if commandresult['exitcode'] != 0:
      status = "FAILED"
      
    # assume some puppet pluing to run these commands
    roleResult = {'role' : command['role'],
                  'actionId' : commandId,
                  'taskId' : command['taskId'],
                  'stdout' : commandresult['stdout'],
                  'clusterName' : clusterName,
                  'stderr' : commandresult['stderr'],
                  'exitCode' : commandresult['exitcode'],
                  'serviceName' : serviceName,
                  'status' : status}
    if roleResult['stdout'] == '':
      roleResult['stdout'] = 'None'
    if roleResult['stderr'] == '':
      roleResult['stderr'] = 'None'
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
