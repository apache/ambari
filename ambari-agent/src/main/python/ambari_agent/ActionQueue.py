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
import Queue

import logging
import traceback
import threading
import pprint
import os
import json

from LiveStatus import LiveStatus
from shell import shellRunner
from ActualConfigHandler import ActualConfigHandler
from CommandStatusDict import CommandStatusDict
from CustomServiceOrchestrator import CustomServiceOrchestrator


logger = logging.getLogger()
installScriptHash = -1

class ActionQueue(threading.Thread):
  """ Action Queue for the agent. We pick one command at a time from the queue
  and execute it
  Note: Action and command terms in this and related classes are used interchangeably
  """

  # How many actions can be performed in parallel. Feel free to change
  MAX_CONCURRENT_ACTIONS = 5


  #How much time(in seconds) we need wait for new incoming execution command before checking
  #status command queue
  EXECUTION_COMMAND_WAIT_TIME = 2

  STATUS_COMMAND = 'STATUS_COMMAND'
  EXECUTION_COMMAND = 'EXECUTION_COMMAND'
  ROLE_COMMAND_INSTALL = 'INSTALL'
  ROLE_COMMAND_START = 'START'
  ROLE_COMMAND_STOP = 'STOP'
  ROLE_COMMAND_CUSTOM_COMMAND = 'CUSTOM_COMMAND'
  CUSTOM_COMMAND_RESTART = 'RESTART'

  IN_PROGRESS_STATUS = 'IN_PROGRESS'
  COMPLETED_STATUS = 'COMPLETED'
  FAILED_STATUS = 'FAILED'

  def __init__(self, config, controller):
    super(ActionQueue, self).__init__()
    self.commandQueue = Queue.Queue()
    self.statusCommandQueue = Queue.Queue()
    self.commandStatuses = CommandStatusDict(callback_action =
      self.status_update_callback)
    self.config = config
    self.controller = controller
    self.sh = shellRunner()
    self.configTags = {}
    self._stop = threading.Event()
    self.tmpdir = config.get('agent', 'prefix')
    self.customServiceOrchestrator = CustomServiceOrchestrator(config,
                                                               controller)


  def stop(self):
    self._stop.set()

  def stopped(self):
    return self._stop.isSet()

  def put_status(self, commands):
    #Was supposed that we got all set of statuses, we don't need to keep old ones
    self.statusCommandQueue.queue.clear()

    for command in commands:
      logger.info("Adding " + command['commandType'] + " for service " + \
                    command['serviceName'] + " of cluster " + \
                    command['clusterName'] + " to the queue.")
      logger.debug(pprint.pformat(command))
      self.statusCommandQueue.put(command)

  def put(self, commands):
    for command in commands:
      if not command.has_key('serviceName'):
        command['serviceName'] = "null"
      if not command.has_key('clusterName'):
        command['clusterName'] = 'null'

      logger.info("Adding " + command['commandType'] + " for service " + \
                  command['serviceName'] + " of cluster " + \
                  command['clusterName'] + " to the queue.")
      logger.debug(pprint.pformat(command))
      self.commandQueue.put(command)

  def run(self):
    while not self.stopped():
      while  not self.statusCommandQueue.empty():
        try:
          command = self.statusCommandQueue.get(False)
          self.process_command(command)
        except (Queue.Empty):
          pass
      try:
        command = self.commandQueue.get(True, self.EXECUTION_COMMAND_WAIT_TIME)
        self.process_command(command)
      except (Queue.Empty):
        pass




  def process_command(self, command):
    logger.debug("Took an element of Queue: " + pprint.pformat(command))
    # make sure we log failures
    try:
      if command['commandType'] == self.EXECUTION_COMMAND:
        self.execute_command(command)
      elif command['commandType'] == self.STATUS_COMMAND:
        self.execute_status_command(command)
      else:
        logger.error("Unrecognized command " + pprint.pformat(command))
    except Exception, err:
      # Should not happen
      traceback.print_exc()
      logger.warn(err)

  def execute_command(self, command):
    '''
    Executes commands of type  EXECUTION_COMMAND
    '''
    clusterName = command['clusterName']
    commandId = command['commandId']

    message = "Executing command with id = {commandId} for role = {role} of " \
              "cluster {cluster}.".format(
              commandId = str(commandId), role=command['role'],
              cluster=clusterName)
    logger.info(message)
    logger.debug(pprint.pformat(command))

    taskId = command['taskId']
    # Preparing 'IN_PROGRESS' report
    in_progress_status = self.commandStatuses.generate_report_template(command)
    in_progress_status.update({
      'tmpout': self.tmpdir + os.sep + 'output-' + str(taskId) + '.txt',
      'tmperr': self.tmpdir + os.sep + 'errors-' + str(taskId) + '.txt',
      'structuredOut' : self.tmpdir + os.sep + 'structured-out-' + str(taskId) + '.json',
      'status': self.IN_PROGRESS_STATUS
    })
    self.commandStatuses.put_command_status(command, in_progress_status)
    # running command
    commandresult = self.customServiceOrchestrator.runCommand(command,
      in_progress_status['tmpout'], in_progress_status['tmperr'])
    # dumping results
    status = self.COMPLETED_STATUS
    if commandresult['exitcode'] != 0:
      status = self.FAILED_STATUS
    roleResult = self.commandStatuses.generate_report_template(command)
    roleResult.update({
      'stdout': commandresult['stdout'],
      'stderr': commandresult['stderr'],
      'exitCode': commandresult['exitcode'],
      'status': status,
    })
    if roleResult['stdout'] == '':
      roleResult['stdout'] = 'None'
    if roleResult['stderr'] == '':
      roleResult['stderr'] = 'None'

    # let ambari know name of custom command
    if command['hostLevelParams'].has_key('custom_command'):
      roleResult['customCommand'] = command['hostLevelParams']['custom_command']

    if 'structuredOut' in commandresult:
      roleResult['structuredOut'] = str(json.dumps(commandresult['structuredOut']))
    else:
      roleResult['structuredOut'] = ''

    # let ambari know that configuration tags were applied
    if status == self.COMPLETED_STATUS:
      configHandler = ActualConfigHandler(self.config, self.configTags)
      if command.has_key('configurationTags'):
        configHandler.write_actual(command['configurationTags'])
        roleResult['configurationTags'] = command['configurationTags']
      component = {'serviceName':command['serviceName'],'componentName':command['role']}
      if command.has_key('roleCommand') and \
        (command['roleCommand'] == self.ROLE_COMMAND_START or \
        (command['roleCommand'] == self.ROLE_COMMAND_INSTALL \
        and component in LiveStatus.CLIENT_COMPONENTS) or \
        (command['roleCommand'] == self.ROLE_COMMAND_CUSTOM_COMMAND and \
        command['hostLevelParams'].has_key('custom_command') and \
        command['hostLevelParams']['custom_command'] == self.CUSTOM_COMMAND_RESTART)):
        configHandler.write_actual_component(command['role'], command['configurationTags'])
        configHandler.write_client_components(command['serviceName'], command['configurationTags'])
        roleResult['configurationTags'] = configHandler.read_actual_component(command['role'])

    self.commandStatuses.put_command_status(command, roleResult)


  def execute_status_command(self, command):
    '''
    Executes commands of type STATUS_COMMAND
    '''
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
                              globalConfig, self.config, self.configTags)

      component_extra = None

      # For custom services, responsibility to determine service status is
      # delegated to python scripts
      component_status_result = self.customServiceOrchestrator.requestComponentStatus(command)

      if component_status_result['exitcode'] == 0:
        component_status = LiveStatus.LIVE_STATUS
      else:
        component_status = LiveStatus.DEAD_STATUS

      if component_status_result.has_key('structuredOut'):
        component_extra = component_status_result['structuredOut']

      result = livestatus.build(forsed_component_status= component_status)

      if component_extra is not None and len(component_extra) != 0:
        result['extra'] = component_extra

      logger.debug("Got live status for component " + component + \
                   " of service " + str(service) + \
                   " of cluster " + str(cluster))

      logger.debug(pprint.pformat(result))
      if result is not None:
        self.commandStatuses.put_command_status(command, result)
    except Exception, err:
      traceback.print_exc()
      logger.warn(err)
    pass


  # Store action result to agent response queue
  def result(self):
    return self.commandStatuses.generate_report()


  def status_update_callback(self):
    """
    Actions that are executed every time when command status changes
    """
    self.controller.heartbeat_wait_event.set()
