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
import ambari_simplejson as json
import time
import signal

from AgentException import AgentException
from LiveStatus import LiveStatus
from ActualConfigHandler import ActualConfigHandler
from ambari_agent.BackgroundCommandExecutionHandle import BackgroundCommandExecutionHandle
from ambari_commons.str_utils import split_on_chunks
from resource_management.libraries.script import Script


logger = logging.getLogger()
installScriptHash = -1

MAX_SYMBOLS_PER_LOG_MESSAGE = 7900

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
  AUTO_EXECUTION_COMMAND = 'AUTO_EXECUTION_COMMAND'
  BACKGROUND_EXECUTION_COMMAND = 'BACKGROUND_EXECUTION_COMMAND'
  ROLE_COMMAND_INSTALL = 'INSTALL'
  ROLE_COMMAND_START = 'START'
  ROLE_COMMAND_STOP = 'STOP'
  ROLE_COMMAND_CUSTOM_COMMAND = 'CUSTOM_COMMAND'
  CUSTOM_COMMAND_RESTART = 'RESTART'
  CUSTOM_COMMAND_RECONFIGURE = 'RECONFIGURE'
  CUSTOM_COMMAND_START = ROLE_COMMAND_START

  IN_PROGRESS_STATUS = 'IN_PROGRESS'
  COMPLETED_STATUS = 'COMPLETED'
  FAILED_STATUS = 'FAILED'

  def __init__(self, initializer_module):
    super(ActionQueue, self).__init__()
    self.commandQueue = Queue.Queue()
    self.backgroundCommandQueue = Queue.Queue()
    self.commandStatuses = initializer_module.commandStatuses
    self.config = initializer_module.config
    self.recovery_manager = initializer_module.recovery_manager
    self.configTags = {}
    self.stop_event = initializer_module.stop_event
    self.tmpdir = self.config.get('agent', 'prefix')
    self.customServiceOrchestrator = initializer_module.customServiceOrchestrator
    self.parallel_execution = self.config.get_parallel_exec_option()
    if self.parallel_execution == 1:
      logger.info("Parallel execution is enabled, will execute agent commands in parallel")
    self.lock = threading.Lock()

  def put(self, commands):
    for command in commands:
      if not command.has_key('serviceName'):
        command['serviceName'] = "null"
      if not command.has_key('clusterId'):
        command['clusterId'] = "null"

      logger.info("Adding " + command['commandType'] + " for role " + \
                  command['role'] + " for service " + \
                  command['serviceName'] + " of cluster_id " + \
                  command['clusterId'] + " to the queue.")
      if command['commandType'] == self.BACKGROUND_EXECUTION_COMMAND :
        self.backgroundCommandQueue.put(self.createCommandHandle(command))
      else:
        self.commandQueue.put(command)

  def interrupt(self):
    self.commandQueue.put(None)

  def cancel(self, commands):
    for command in commands:

      logger.info("Canceling command with taskId = {tid}".format(tid = str(command['target_task_id'])))
      if logger.isEnabledFor(logging.DEBUG):
        logger.debug(pprint.pformat(command))

      task_id = command['target_task_id']
      reason = command['reason']

      # Remove from the command queue by task_id
      queue = self.commandQueue
      self.commandQueue = Queue.Queue()

      while not queue.empty():
        queued_command = queue.get(False)
        if queued_command['taskId'] != task_id:
          self.commandQueue.put(queued_command)
        else:
          logger.info("Canceling " + queued_command['commandType'] + \
                      " for service " + queued_command['serviceName'] + \
                      " and role " +  queued_command['role'] + \
                      " with taskId " + str(queued_command['taskId']))

      # Kill if in progress
      self.customServiceOrchestrator.cancel_command(task_id, reason)

  def run(self):
    while not self.stop_event.is_set():
      try:
        self.processBackgroundQueueSafeEmpty()
        self.fillRecoveryCommands()
        try:
          if self.parallel_execution == 0:
            command = self.commandQueue.get(True, self.EXECUTION_COMMAND_WAIT_TIME)

            if command == None:
              break

            self.process_command(command)
          else:
            # If parallel execution is enabled, just kick off all available
            # commands using separate threads
            while not self.stop_event.is_set():
              command = self.commandQueue.get(True, self.EXECUTION_COMMAND_WAIT_TIME)

              if command == None:
                break
              # If command is not retry_enabled then do not start them in parallel
              # checking just one command is enough as all commands for a stage is sent
              # at the same time and retry is only enabled for initial start/install
              retryAble = False
              if 'commandParams' in command and 'command_retry_enabled' in command['commandParams']:
                retryAble = command['commandParams']['command_retry_enabled'] == "true"
              if retryAble:
                logger.info("Kicking off a thread for the command, id=" +
                            str(command['commandId']) + " taskId=" + str(command['taskId']))
                t = threading.Thread(target=self.process_command, args=(command,))
                t.daemon = True
                t.start()
              else:
                self.process_command(command)
                break
              pass
            pass
        except (Queue.Empty):
          pass
      except:
        logger.exception("ActionQueue thread failed with exception. Re-running it")
    logger.info("ActionQueue thread has successfully finished")

  def fillRecoveryCommands(self):
    if not self.tasks_in_progress_or_pending():
      self.put(self.recovery_manager.get_recovery_commands())

  def processBackgroundQueueSafeEmpty(self):
    while not self.backgroundCommandQueue.empty():
      try:
        command = self.backgroundCommandQueue.get(False)
        if command.has_key('__handle') and command['__handle'].status == None:
          self.process_command(command)
      except Queue.Empty:
        pass

  def createCommandHandle(self, command):
    if command.has_key('__handle'):
      raise AgentException("Command already has __handle")
    command['__handle'] = BackgroundCommandExecutionHandle(command, command['commandId'], None, self.on_background_command_complete_callback)
    return command

  def process_command(self, command):
    # make sure we log failures
    commandType = command['commandType']
    logger.debug("Took an element of Queue (command type = %s).", commandType)
    try:
      if commandType in [self.EXECUTION_COMMAND, self.BACKGROUND_EXECUTION_COMMAND, self.AUTO_EXECUTION_COMMAND]:
        try:
          if self.recovery_manager.enabled():
            self.recovery_manager.on_execution_command_start()
            self.recovery_manager.process_execution_command(command)

          self.execute_command(command)
        finally:
          if self.recovery_manager.enabled():
            self.recovery_manager.on_execution_command_finish()
      else:
        logger.error("Unrecognized command %s", pprint.pformat(command))
    except Exception:
      logger.exception("Exception while processing {0} command".format(commandType))

  def tasks_in_progress_or_pending(self):
    return not self.commandQueue.empty() or self.recovery_manager.has_active_command()

  def execute_command(self, command):
    '''
    Executes commands of type EXECUTION_COMMAND
    '''
    clusterId = command['clusterId']
    commandId = command['commandId']
    isCommandBackground = command['commandType'] == self.BACKGROUND_EXECUTION_COMMAND
    isAutoExecuteCommand = command['commandType'] == self.AUTO_EXECUTION_COMMAND
    message = "Executing command with id = {commandId}, taskId = {taskId} for role = {role} of " \
              "cluster_id {cluster}.".format(
              commandId = str(commandId), taskId = str(command['taskId']),
              role=command['role'], cluster=clusterId)
    logger.info(message)

    taskId = command['taskId']
    # Preparing 'IN_PROGRESS' report
    in_progress_status = self.commandStatuses.generate_report_template(command)
    # The path of the files that contain the output log and error log use a prefix that the agent advertises to the
    # server. The prefix is defined in agent-config.ini
    if not isAutoExecuteCommand:
      in_progress_status.update({
        'tmpout': self.tmpdir + os.sep + 'output-' + str(taskId) + '.txt',
        'tmperr': self.tmpdir + os.sep + 'errors-' + str(taskId) + '.txt',
        'structuredOut' : self.tmpdir + os.sep + 'structured-out-' + str(taskId) + '.json',
        'status': self.IN_PROGRESS_STATUS
      })
    else:
      in_progress_status.update({
        'tmpout': self.tmpdir + os.sep + 'auto_output-' + str(taskId) + '.txt',
        'tmperr': self.tmpdir + os.sep + 'auto_errors-' + str(taskId) + '.txt',
        'structuredOut' : self.tmpdir + os.sep + 'auto_structured-out-' + str(taskId) + '.json',
        'status': self.IN_PROGRESS_STATUS
      })

    self.commandStatuses.put_command_status(command, in_progress_status)

    numAttempts = 0
    retryDuration = 0  # even with 0 allow one attempt
    retryAble = False
    delay = 1
    log_command_output = True
    if 'commandParams' in command and 'log_output' in command['commandParams'] and "false" == command['commandParams']['log_output']:
      log_command_output = False

    if 'commandParams' in command:
      if 'max_duration_for_retries' in command['commandParams']:
        retryDuration = int(command['commandParams']['max_duration_for_retries'])
      if 'command_retry_enabled' in command['commandParams']:
        retryAble = command['commandParams']['command_retry_enabled'] == "true"
    if isAutoExecuteCommand:
      retryAble = False

    logger.info("Command execution metadata - taskId = {taskId}, retry enabled = {retryAble}, max retry duration (sec) = {retryDuration}, log_output = {log_command_output}".
                 format(taskId=taskId, retryAble=retryAble, retryDuration=retryDuration, log_command_output=log_command_output))
    command_canceled = False
    while retryDuration >= 0:
      numAttempts += 1
      start = 0
      if retryAble:
        start = int(time.time())
      # running command
      commandresult = self.customServiceOrchestrator.runCommand(command,
                                                                in_progress_status['tmpout'],
                                                                in_progress_status['tmperr'],
                                                                override_output_files=numAttempts == 1,
                                                                retry=numAttempts > 1)
      end = 1
      if retryAble:
        end = int(time.time())
      retryDuration -= (end - start)

      # dumping results
      if isCommandBackground:
        logger.info("Command is background command, quit retrying. Exit code: {exitCode}, retryAble: {retryAble}, retryDuration (sec): {retryDuration}, last delay (sec): {delay}"
                    .format(cid=taskId, exitCode=commandresult['exitcode'], retryAble=retryAble, retryDuration=retryDuration, delay=delay))
        return
      else:
        if commandresult['exitcode'] == 0:
          status = self.COMPLETED_STATUS
        else:
          status = self.FAILED_STATUS
          if (commandresult['exitcode'] == -signal.SIGTERM) or (commandresult['exitcode'] == -signal.SIGKILL):
            logger.info('Command with taskId = {cid} was canceled!'.format(cid=taskId))
            command_canceled = True
            break

      if status != self.COMPLETED_STATUS and retryAble and retryDuration > 0:
        delay = self.get_retry_delay(delay)
        if delay > retryDuration:
          delay = retryDuration
        retryDuration -= delay  # allow one last attempt
        commandresult['stderr'] += "\n\nCommand failed. Retrying command execution ...\n\n"
        logger.info("Retrying command with taskId = {cid} after a wait of {delay}".format(cid=taskId, delay=delay))
        if 'agentLevelParams' not in command:
          command['agentLevelParams'] = {}

        command['agentLevelParams']['commandBeingRetried'] = "true"
        time.sleep(delay)
        continue
      else:
        logger.info("Quit retrying for command with taskId = {cid}. Status: {status}, retryAble: {retryAble}, retryDuration (sec): {retryDuration}, last delay (sec): {delay}"
                    .format(cid=taskId, status=status, retryAble=retryAble, retryDuration=retryDuration, delay=delay))
        break

    # do not fail task which was rescheduled from server
    if command_canceled:
      with self.lock:
        with self.commandQueue.mutex:
          for com in self.commandQueue.queue:
            if com['taskId'] == command['taskId']:
              logger.info('Command with taskId = {cid} was rescheduled by server. '
                          'Fail report on cancelled command won\'t be sent with heartbeat.'.format(cid=taskId))
              return

    # final result to stdout
    commandresult['stdout'] += '\n\nCommand completed successfully!\n' if status == self.COMPLETED_STATUS else '\n\nCommand failed after ' + str(numAttempts) + ' tries\n'
    logger.info('Command with taskId = {cid} completed successfully!'.format(cid=taskId) if status == self.COMPLETED_STATUS else 'Command with taskId = {cid} failed after {attempts} tries'.format(cid=taskId, attempts=numAttempts))

    roleResult = self.commandStatuses.generate_report_template(command)
    roleResult.update({
      'stdout': commandresult['stdout'],
      'stderr': commandresult['stderr'],
      'exitCode': commandresult['exitcode'],
      'status': status,
    })

    if self.config.has_option("logging","log_command_executes") \
        and int(self.config.get("logging", "log_command_executes")) == 1 \
        and log_command_output:

      if roleResult['stdout'] != '':
          logger.info("Begin command output log for command with id = " + str(command['taskId']) + ", role = "
                      + command['role'] + ", roleCommand = " + command['roleCommand'])
          self.log_command_output(roleResult['stdout'], str(command['taskId']))
          logger.info("End command output log for command with id = " + str(command['taskId']) + ", role = "
                      + command['role'] + ", roleCommand = " + command['roleCommand'])

      if roleResult['stderr'] != '':
          logger.info("Begin command stderr log for command with id = " + str(command['taskId']) + ", role = "
                      + command['role'] + ", roleCommand = " + command['roleCommand'])
          self.log_command_output(roleResult['stderr'], str(command['taskId']))
          logger.info("End command stderr log for command with id = " + str(command['taskId']) + ", role = "
                      + command['role'] + ", roleCommand = " + command['roleCommand'])

    if roleResult['stdout'] == '':
      roleResult['stdout'] = 'None'
    if roleResult['stderr'] == '':
      roleResult['stderr'] = 'None'

    # let ambari know name of custom command

    if 'commandParams' in command and command['commandParams'].has_key('custom_command'):
      roleResult['customCommand'] = command['commandParams']['custom_command']

    if 'structuredOut' in commandresult:
      roleResult['structuredOut'] = str(json.dumps(commandresult['structuredOut']))
    else:
      roleResult['structuredOut'] = ''

    # let recovery manager know the current state
    if status == self.COMPLETED_STATUS:
      # let ambari know that configuration tags were applied
      configHandler = ActualConfigHandler(self.config, self.configTags)
      """
      #update
      if 'commandParams' in command:
        command_params = command['commandParams']
        if command_params and command_params.has_key('forceRefreshConfigTags') and len(command_params['forceRefreshConfigTags']) > 0  :
          forceRefreshConfigTags = command_params['forceRefreshConfigTags'].split(',')
          logger.info("Got refresh additional component tags command")

          for configTag in forceRefreshConfigTags :
            configHandler.update_component_tag(command['role'], configTag, command['configurationTags'][configTag])

          roleResult['customCommand'] = self.CUSTOM_COMMAND_RESTART # force restart for component to evict stale_config on server side
          command['configurationTags'] = configHandler.read_actual_component(command['role'])

      if command.has_key('configurationTags'):
        configHandler.write_actual(command['configurationTags'])
        roleResult['configurationTags'] = command['configurationTags']
      component = {'serviceName':command['serviceName'],'componentName':command['role']}
      if 'roleCommand' in command and \
          (command['roleCommand'] == self.ROLE_COMMAND_START or
             (command['roleCommand'] == self.ROLE_COMMAND_INSTALL and component in LiveStatus.CLIENT_COMPONENTS) or
               (command['roleCommand'] == self.ROLE_COMMAND_CUSTOM_COMMAND and
                  'custom_command' in command['hostLevelParams'] and
                      command['hostLevelParams']['custom_command'] in (self.CUSTOM_COMMAND_RESTART,
                                                                       self.CUSTOM_COMMAND_START,
                                                                       self.CUSTOM_COMMAND_RECONFIGURE))):
        configHandler.write_actual_component(command['role'],
                                             command['configurationTags'])
        if 'clientsToUpdateConfigs' in command['hostLevelParams'] and command['hostLevelParams']['clientsToUpdateConfigs']:
          configHandler.write_client_components(command['serviceName'],
                                                command['configurationTags'],
                                                command['hostLevelParams']['clientsToUpdateConfigs'])
        roleResult['configurationTags'] = configHandler.read_actual_component(
            command['role'])
    """

    self.recovery_manager.process_execution_command_result(command, status)
    self.commandStatuses.put_command_status(command, roleResult)

  def log_command_output(self, text, taskId):
    """
    Logs a message as multiple enumerated log messages every of which is not larger than MAX_SYMBOLS_PER_LOG_MESSAGE.

    If logs are redirected to syslog (syslog_enabled=1), this is very useful for logging big messages.
    As syslog usually truncates long messages.
    """
    chunks = split_on_chunks(text, MAX_SYMBOLS_PER_LOG_MESSAGE)
    if len(chunks) > 1:
      for i in range(len(chunks)):
        logger.info("Cmd log for taskId={0} and chunk {1}/{2} of log for command: \n".format(taskId, i+1, len(chunks)) + chunks[i])
    else:
      logger.info("Cmd log for taskId={0}: ".format(taskId) + text)

  def get_retry_delay(self, last_delay):
    """
    Returns exponentially growing delay. The idea being if number of retries is high then the reason to retry
    is probably a host or environment specific issue requiring longer waits
    """
    return last_delay * 2

  def command_was_canceled(self):
    self.customServiceOrchestrator

  def on_background_command_complete_callback(self, process_condensed_result, handle):
    logger.debug('Start callback: %s', process_condensed_result)
    logger.debug('The handle is: %s', handle)
    status = self.COMPLETED_STATUS if handle.exitCode == 0 else self.FAILED_STATUS

    aborted_postfix = self.customServiceOrchestrator.command_canceled_reason(handle.command['taskId'])
    if aborted_postfix:
      status = self.FAILED_STATUS
      logger.debug('Set status to: %s , reason = %s', status, aborted_postfix)
    else:
      aborted_postfix = ''


    roleResult = self.commandStatuses.generate_report_template(handle.command)

    roleResult.update({
      'stdout': process_condensed_result['stdout'] + aborted_postfix,
      'stderr': process_condensed_result['stderr'] + aborted_postfix,
      'exitCode': process_condensed_result['exitcode'],
      'structuredOut': str(json.dumps(process_condensed_result['structuredOut'])) if 'structuredOut' in process_condensed_result else '',
      'status': status,
    })

    self.commandStatuses.put_command_status(handle.command, roleResult)

  # Removes all commands from the queue
  def reset(self):
    queue = self.commandQueue
    with queue.mutex:
      queue.queue.clear()
