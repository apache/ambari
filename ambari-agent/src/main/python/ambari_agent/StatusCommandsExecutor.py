#!/usr/bin/env python
"""
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
"""

import Queue
import logging
import multiprocessing
import os
import pprint
import threading

import time

import signal
from ambari_agent.RemoteDebugUtils import bind_debug_signal_handlers

logger = logging.getLogger(__name__)

class StatusCommandsExecutor(object):
  def put_commands(self, commands):
    raise NotImplemented()

  def process_results(self):
    raise NotImplemented()

  def relaunch(self, reason=None):
    raise NotImplemented()

  def kill(self, reason=None, can_relaunch=True):
    raise NotImplemented()

class SingleProcessStatusCommandsExecutor(StatusCommandsExecutor):
  def __init__(self, config, actionQueue):
    self.config = config
    self.actionQueue = actionQueue
    self.statusCommandQueue = Queue.Queue()
    self.need_relaunch = False

  def put_commands(self, commands):
    while not self.statusCommandQueue.empty():
      self.statusCommandQueue.get()

    for command in commands:
      logger.info("Adding " + command['commandType'] + " for component " + \
                  command['componentName'] + " of service " + \
                  command['serviceName'] + " of cluster " + \
                  command['clusterName'] + " to the queue.")
      self.statusCommandQueue.put(command)
      logger.debug(pprint.pformat(command))

  def process_results(self):
    """
    Execute a single command from the queue and process it
    """
    while not self.statusCommandQueue.empty():
      try:
        command = self.statusCommandQueue.get(False)
        self.actionQueue.process_status_command_result(self.actionQueue.execute_status_command_and_security_status(command))
      except Queue.Empty:
        pass

  def relaunch(self, reason=None):
    pass

  def kill(self, reason=None, can_relaunch=True):
    pass

class MultiProcessStatusCommandsExecutor(StatusCommandsExecutor):
  def __init__(self, config, actionQueue):
    self.config = config
    self.actionQueue = actionQueue

    self._can_relaunch_lock = threading.RLock()
    self._can_relaunch = True

    # used to prevent queues from been used during creation of new one to prevent threads messing up with combination of
    # old and new queues
    self.usage_lock = threading.RLock()

    self.status_command_timeout = int(self.config.get('agent', 'status_command_timeout', 5))
    self.customServiceOrchestrator = self.actionQueue.customServiceOrchestrator

    self.worker_process = None
    self.mustDieEvent = multiprocessing.Event()
    self.timedOutEvent = multiprocessing.Event()

    # multiprocessing stuff that need to be cleaned every time
    self.mp_result_queue = multiprocessing.Queue()
    self.mp_result_logs = multiprocessing.Queue()
    self.mp_task_queue = multiprocessing.Queue()

  @property
  def can_relaunch(self):
    with self._can_relaunch_lock:
      return self._can_relaunch

  @can_relaunch.setter
  def can_relaunch(self, value):
    with self._can_relaunch_lock:
      self._can_relaunch = value

  def _log_message(self, level, message, exception=None):
    """
    Put log message to logging queue. Must be used only for logging from child process(in _worker_process_target).

    :param level:
    :param message:
    :param exception:
    :return:
    """
    result_message = "StatusCommandExecutor reporting at {0}: ".format(time.time()) + message
    self.mp_result_logs.put((level, result_message, exception))

  def _get_log_messages(self):
    """
    Returns list of (level, message, exception) log messages.

    :return: list of (level, message, exception)
    """
    results = []
    with self.usage_lock:
      try:
        while not self.mp_result_logs.empty():
          try:
            results.append(self.mp_result_logs.get(False))
          except Queue.Empty:
            pass
          except IOError:
            pass
          except UnicodeDecodeError:
            pass
      except IOError:
        pass
    return results

  def _process_logs(self):
    """
    Get all available at this moment logs and prints them to logger.
    """
    for level, message, exception in self._get_log_messages():
      if level == logging.ERROR:
        logger.debug(message, exc_info=exception)
      if level == logging.WARN:
        logger.warn(message)
      if level == logging.INFO:
        logger.info(message)

  def _worker_process_target(self):
    """
    Internal method that running in separate process.
    """
    bind_debug_signal_handlers()
    self._log_message(logging.INFO, "StatusCommandsExecutor process started")

    # region StatusCommandsExecutor process internals
    internal_in_queue = Queue.Queue()
    internal_out_queue = Queue.Queue()

    def _internal_worker():
      """
      thread that actually executes status commands
      """
      while True:
        _cmd = internal_in_queue.get()
        internal_out_queue.put(self.actionQueue.execute_status_command_and_security_status(_cmd))

    worker = threading.Thread(target=_internal_worker)
    worker.daemon = True
    worker.start()

    def _internal_process_command(_command):
      internal_in_queue.put(_command)
      start_time = time.time()
      result = None
      while not self.mustDieEvent.is_set() and not result and time.time() - start_time < self.status_command_timeout:
        try:
          result = internal_out_queue.get(timeout=1)
        except Queue.Empty:
          pass

      if result:
        self.mp_result_queue.put(result)
        return True
      else:
        # do not set timed out event twice
        if not self.timedOutEvent.is_set():
          self._set_timed_out(_command)
        return False

    # endregion

    try:
      while not self.mustDieEvent.is_set():
        try:
          command = self.mp_task_queue.get(False)
        except Queue.Empty:
          # no command, lets try in other loop iteration
          time.sleep(.1)
          continue

        self._log_message(logging.DEBUG, "Running status command for {0}".format(command['componentName']))

        if _internal_process_command(command):
          self._log_message(logging.DEBUG, "Completed status command for {0}".format(command['componentName']))

    except Exception as e:
      self._log_message(logging.ERROR, "StatusCommandsExecutor process failed with exception:", e)
      raise

    self._log_message(logging.INFO, "StatusCommandsExecutor subprocess finished")

  def _set_timed_out(self, command):
    """
    Set timeout event and adding log entry for given command.

    :param command:
    :return:
    """
    msg = "Command {0} for {1} is running for more than {2} seconds. Terminating it due to timeout.".format(
        command['commandType'],
        command['componentName'],
        self.status_command_timeout
    )
    self._log_message(logging.WARN, msg)
    self.timedOutEvent.set()

  def put_commands(self, commands):
    """
    Put given commands to command executor.

    :param commands: status commands to execute
    :return:
    """
    with self.usage_lock:
      if not self.mp_task_queue.empty():
        status_command_queue_size = 0
        try:
          while not self.mp_task_queue.empty():
            self.mp_task_queue.get(False)
            status_command_queue_size += 1
        except Queue.Empty:
          pass

        logger.info("Number of status commands removed from queue : " + str(status_command_queue_size))
      for command in commands:
        logger.info("Adding " + command['commandType'] + " for component " + \
                    command['componentName'] + " of service " + \
                    command['serviceName'] + " of cluster " + \
                    command['clusterName'] + " to the queue.")
        self.mp_task_queue.put(command)
        logger.debug(pprint.pformat(command))

  def process_results(self):
    """
    Process all the results from the internal worker
    """
    self._process_logs()
    for result in self._get_results():
      try:
        self.actionQueue.process_status_command_result(result)
      except UnicodeDecodeError:
        pass

  def _get_results(self):
    """
    Get all available results for status commands.

    :return: list of results
    """
    results = []
    with self.usage_lock:
      try:
        while not self.mp_result_queue.empty():
          try:
            results.append(self.mp_result_queue.get(False))
          except Queue.Empty:
            pass
          except IOError:
            pass
          except UnicodeDecodeError:
            pass
      except IOError:
        pass
    return results

  @property
  def need_relaunch(self):
    """
    Indicates if process need to be relaunched due to timeout or it is dead or even was not created.
    """
    return self.timedOutEvent.is_set() or not self.worker_process or not self.worker_process.is_alive()

  def relaunch(self, reason=None):
    """
    Restart status command executor internal process.

    :param reason: reason of restart
    :return:
    """
    if self.can_relaunch:
      self.kill(reason)
      self.worker_process = multiprocessing.Process(target=self._worker_process_target)
      self.worker_process.start()
      logger.info("Started process with pid {0}".format(self.worker_process.pid))
    else:
      logger.debug("Relaunch does not allowed, can not relaunch")

  def kill(self, reason=None, can_relaunch=True):
    """
    Tries to stop command executor internal process for sort time, otherwise killing it. Closing all possible queues to
    unblock threads that probably blocked on read or write operations to queues. Must be called from threads different
    from threads that calling read or write methods(get_log_messages, get_results, put_commands).

    :param can_relaunch: indicates if StatusCommandsExecutor can be relaunched after this kill
    :param reason: reason of killing
    :return:
    """
    logger.info("Killing child process reason:" + str(reason))
    self.can_relaunch = can_relaunch

    if not self.can_relaunch:
      logger.info("Killing without possibility to relaunch...")

    # try graceful stop, otherwise hard-kill
    if self.worker_process and self.worker_process.is_alive():
      self.mustDieEvent.set()
      self.worker_process.join(timeout=3)
      if self.worker_process.is_alive():
        os.kill(self.worker_process.pid, signal.SIGKILL)
        logger.info("Child process killed by -9")
      else:
        # get log messages only if we died gracefully, otherwise we will have chance to block here forever, in most cases
        # this call will do nothing, as all logs will be processed in ActionQueue loop
        self._process_logs()
        logger.info("Child process died gracefully")
    else:
      logger.info("Child process already dead")

    # close queues and acquire usage lock
    # closing both sides of pipes here, we need this hack in case of blocking on recv() call
    self.mp_result_queue.close()
    self.mp_result_queue._writer.close()
    self.mp_result_logs.close()
    self.mp_result_logs._writer.close()
    self.mp_task_queue.close()
    self.mp_task_queue._writer.close()

    with self.usage_lock:
      self.mp_result_queue.join_thread()
      self.mp_result_queue = multiprocessing.Queue()
      self.mp_task_queue.join_thread()
      self.mp_task_queue = multiprocessing.Queue()
      self.mp_result_logs.join_thread()
      self.mp_result_logs = multiprocessing.Queue()
      self.customServiceOrchestrator = self.actionQueue.customServiceOrchestrator
      self.mustDieEvent.clear()
      self.timedOutEvent.clear()
