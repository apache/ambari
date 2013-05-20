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
import Queue
import threading
import pprint
import os
import time

from LiveStatus import LiveStatus
from shell import shellRunner
import PuppetExecutor
import UpgradeExecutor
import PythonExecutor
from Grep import Grep
from ActualConfigHandler import ActualConfigHandler


logger = logging.getLogger()
installScriptHash = -1

class ActionQueue(threading.Thread):
  """ Action Queue for the agent. We pick one command at a time from the queue
  and execute that """

  commandQueue = Queue.Queue()
  resultQueue = Queue.Queue()

  STATUS_COMMAND = 'STATUS_COMMAND'
  EXECUTION_COMMAND = 'EXECUTION_COMMAND'
  UPGRADE_STATUS = 'UPGRADE'

  IDLE_SLEEP_TIME = 5

  def __init__(self, config):
    super(ActionQueue, self).__init__()
    self.config = config
    self.sh = shellRunner()
    self._stop = threading.Event()
    self.maxRetries = config.getint('command', 'maxretries')
    self.sleepInterval = config.getint('command', 'sleepBetweenRetries')
    self.puppetExecutor = PuppetExecutor.PuppetExecutor(
      config.get('puppet', 'puppetmodules'),
      config.get('puppet', 'puppet_home'),
      config.get('puppet', 'facter_home'),
      config.get('agent', 'prefix'), config)
    self.pythonExecutor = PythonExecutor.PythonExecutor(
      config.get('agent', 'prefix'), config)
    self.upgradeExecutor = UpgradeExecutor.UpgradeExecutor(self.pythonExecutor,
      self.puppetExecutor, config)
    self.tmpdir = config.get('agent', 'prefix')
    self.commandInProgress = None

  def stop(self):
    self._stop.set()

  def stopped(self):
    return self._stop.isSet()

  def put(self, command):
    logger.info("Adding " + command['commandType'] + " for service " +\
                command['serviceName'] + " of cluster " +\
                command['clusterName'] + " to the queue.")
    logger.debug(pprint.pformat(command))
    self.commandQueue.put(command)
    pass

  def run(self):
    result = []
    while not self.stopped():
      while not self.commandQueue.empty():
        command = self.commandQueue.get()
        logger.debug("Took an element of Queue: " + pprint.pformat(command))
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
            self.resultQueue.put((command['commandType'], entry))

        elif command['commandType'] == self.STATUS_COMMAND:
          try:
            cluster = command['clusterName']
            service = command['serviceName']
            component = command['componentName']
            configurations = command['configurations']
            if configurations.has_key('global'):
              globalConfig = configurations['global']
            else:
              globalConfig = {}
            livestatus = LiveStatus(cluster, service, component,
              globalConfig, self.config)
            result = livestatus.build()
            logger.debug("Got live status for component " + component +\
                         " of service " + str(service) +\
                         " of cluster " + str(cluster))
            logger.debug(pprint.pformat(result))
            if result is not None:
              self.resultQueue.put((ActionQueue.STATUS_COMMAND, result))

          except Exception, err:
            traceback.print_exc()
            logger.warn(err)
          pass
        else:
          logger.warn("Unrecognized command " + pprint.pformat(command))
      if not self.stopped():
        time.sleep(self.IDLE_SLEEP_TIME)

  # Store action result to agent response queue
  def result(self):
    resultReports = []
    resultComponentStatus = []
    while not self.resultQueue.empty():
      res = self.resultQueue.get()
      if res[0] == self.EXECUTION_COMMAND:
        resultReports.append(res[1])
      elif res[0] == ActionQueue.STATUS_COMMAND:
        resultComponentStatus.append(res[1])

    # Building report for command in progress
    if self.commandInProgress is not None:
      try:
        tmpout = open(self.commandInProgress['tmpout'], 'r').read()
        tmperr = open(self.commandInProgress['tmperr'], 'r').read()
      except Exception, err:
        logger.warn(err)
        tmpout = '...'
        tmperr = '...'
      grep = Grep()
      output = grep.tail(tmpout, Grep.OUTPUT_LAST_LINES)
      inprogress = {
        'role': self.commandInProgress['role'],
        'actionId': self.commandInProgress['actionId'],
        'taskId': self.commandInProgress['taskId'],
        'stdout': grep.filterMarkup(output),
        'clusterName': self.commandInProgress['clusterName'],
        'stderr': tmperr,
        'exitCode': 777,
        'serviceName': self.commandInProgress['serviceName'],
        'status': 'IN_PROGRESS',
        'roleCommand': self.commandInProgress['roleCommand']
      }
      resultReports.append(inprogress)
    result = {
      'reports': resultReports,
      'componentStatus': resultComponentStatus
    }
    return result

  def executeCommand(self, command):
    clusterName = command['clusterName']
    commandId = command['commandId']
    hostname = command['hostname']
    params = command['hostLevelParams']
    clusterHostInfo = command['clusterHostInfo']
    roleCommand = command['roleCommand']
    serviceName = command['serviceName']
    configurations = command['configurations']
    result = []

    logger.info("Executing command with id = " + str(commandId) +\
                " for role = " + command['role'] + " of " +\
                "cluster " + clusterName)
    logger.debug(pprint.pformat(command))

    taskId = command['taskId']
    # Preparing 'IN_PROGRESS' report
    self.commandInProgress = {
      'role': command['role'],
      'actionId': commandId,
      'taskId': taskId,
      'clusterName': clusterName,
      'serviceName': serviceName,
      'tmpout': self.tmpdir + os.sep + 'output-' + str(taskId) + '.txt',
      'tmperr': self.tmpdir + os.sep + 'errors-' + str(taskId) + '.txt',
      'roleCommand': roleCommand
    }
    # running command
    if command['commandType'] == ActionQueue.EXECUTION_COMMAND:
      if command['roleCommand'] == ActionQueue.UPGRADE_STATUS:
        commandresult = self.upgradeExecutor.perform_stack_upgrade(command, self.commandInProgress['tmpout'],
          self.commandInProgress['tmperr'])
      else:
        commandresult = self.puppetExecutor.runCommand(command, self.commandInProgress['tmpout'],
          self.commandInProgress['tmperr'])
      # dumping results
    self.commandInProgress = None
    status = "COMPLETED"
    if commandresult['exitcode'] != 0:
      status = "FAILED"

    # assume some puppet plumbing to run these commands
    roleResult = {'role': command['role'],
                  'actionId': commandId,
                  'taskId': command['taskId'],
                  'stdout': commandresult['stdout'],
                  'clusterName': clusterName,
                  'stderr': commandresult['stderr'],
                  'exitCode': commandresult['exitcode'],
                  'serviceName': serviceName,
                  'status': status,
                  'roleCommand': roleCommand}
    if roleResult['stdout'] == '':
      roleResult['stdout'] = 'None'
    if roleResult['stderr'] == '':
      roleResult['stderr'] = 'None'

    # let ambari know that configuration tags were applied
    if status == 'COMPLETED':
      configHandler = ActualConfigHandler(self.config)
      if command.has_key('configurationTags'):
        configHandler.write_actual(command['configurationTags'])
        roleResult['configurationTags'] = command['configurationTags']

      if command.has_key('roleCommand') and command['roleCommand'] == 'START':
        configHandler.copy_to_component(command['role'])
        roleResult['configurationTags'] = configHandler.read_actual_component(command['role'])

    result.append(roleResult)
    return result


  def isIdle(self):
    return self.commandQueue.empty()
