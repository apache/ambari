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

from LiveStatus import LiveStatus
from shell import shellRunner
import PuppetExecutor
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

  COMMAND_FORMAT_V1 = "1.0"
  COMMAND_FORMAT_V2 = "2.0"

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


  def determine_command_format_version(self, command):
    """
    Returns either COMMAND_FORMAT_V1 or COMMAND_FORMAT_V2
    """
    try:
      if command['commandParams']['schema_version'] == self.COMMAND_FORMAT_V2:
        return self.COMMAND_FORMAT_V2
      else:
        return  self.COMMAND_FORMAT_V1
    except KeyError:
      pass # ignore
    return self.COMMAND_FORMAT_V1 # Fallback


  def execute_command(self, command):
    '''
    Executes commands of type  EXECUTION_COMMAND
    '''
    clusterName = command['clusterName']
    commandId = command['commandId']
    command_format = self.determine_command_format_version(command)

    message = "Executing command with id = {commandId} for role = {role} of " \
              "cluster {cluster}. Command format={command_format}".format(
              commandId = str(commandId), role=command['role'],
              cluster=clusterName, command_format=command_format)
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
    if command_format == self.COMMAND_FORMAT_V1:
      # Create a new instance of executor for the current thread
      puppetExecutor = PuppetExecutor.PuppetExecutor(
        self.config.get('puppet', 'puppetmodules'),
        self.config.get('puppet', 'puppet_home'),
        self.config.get('puppet', 'facter_home'),
        self.config.get('agent', 'prefix'), self.config)
      commandresult = puppetExecutor.runCommand(command, in_progress_status['tmpout'],
        in_progress_status['tmperr'])
    else:
      commandresult = self.customServiceOrchestrator.runCommand(command,
        in_progress_status['tmpout'], in_progress_status['tmperr'])
    # dumping results
    status = self.COMPLETED_STATUS
    if commandresult['exitcode'] != 0:
      status = self.FAILED_STATUS
    roleResult = self.commandStatuses.generate_report_template(command)
    # assume some puppet plumbing to run these commands
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

    if 'structuredOut' in commandresult:
      roleResult['structuredOut'] = str(commandresult['structuredOut'])
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

      command_format = self.determine_command_format_version(command)

      livestatus = LiveStatus(cluster, service, component,
                              globalConfig, self.config, self.configTags)
      component_status = None
      if command_format == self.COMMAND_FORMAT_V2:
        # For custom services, responsibility to determine service status is
        # delegated to python scripts
        component_status = self.customServiceOrchestrator.requestComponentStatus(command)

      result = livestatus.build(forsed_component_status= component_status)
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
