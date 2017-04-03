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
    self.need_relaunch = (False, None) #  tuple (bool, str|None) with flag to relaunch and reason of relaunch

  def put_commands(self, commands):
    with self.statusCommandQueue.mutex:
      qlen = len(self.statusCommandQueue.queue)
      if qlen:
        logger.info("Removing %s stale status commands from queue", qlen)
      self.statusCommandQueue.queue.clear()

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

    self.can_relaunch = True

    # used to prevent queues from been used during creation of new one to prevent threads messing up with combination of
    # old and new queues
    self.usage_lock = threading.RLock()
    # protects against simultaneous killing/creating from different threads.
    self.kill_lock = threading.RLock()

    self.status_command_timeout = int(self.config.get('agent', 'status_command_timeout', 5))
    self.customServiceOrchestrator = self.actionQueue.customServiceOrchestrator

    self.worker_process = None
    self.mustDieEvent = multiprocessing.Event()
    self.timedOutEvent = multiprocessing.Event()

    # multiprocessing stuff that need to be cleaned every time
    self.mp_result_queue = multiprocessing.Queue()
    self.mp_result_logs = multiprocessing.Queue()
    self.mp_task_queue = multiprocessing.Queue()

  def _drain_queue(self, target_queue, max_time=5, max_empty_count=15, read_break=.001):
    """
    Read everything that available in queue. Using not reliable multiprocessing.Queue methods(qsize, empty), so contains
    extremely dumb protection against blocking too much at this method: will try to get all possible items for not more
    than ``max_time`` seconds; will return after ``max_empty_count`` calls of ``target_queue.get(False)`` that raised
    ``Queue.Empty`` exception. Notice ``read_break`` argument, with default values this method will be able to read
    ~4500 ``range(1,10000)`` objects for 5 seconds. So don't fill queue too fast.

    :param target_queue: queue to read from
    :param max_time: maximum time to spend in this method call
    :param max_empty_count: maximum allowed ``Queue.Empty`` in a row
    :param read_break: time to wait before next read cycle iteration
    :return: list of resulting objects
    """
    results = []
    _empty = 0
    _start = time.time()
    with self.usage_lock:
      try:
        while (not target_queue.empty() or target_queue.qsize() > 0) and time.time() - _start < max_time and _empty < max_empty_count:
          try:
            results.append(target_queue.get(False))
            _empty = 0
            time.sleep(read_break) # sleep a little to get more accurate empty and qsize results
          except Queue.Empty:
            _empty += 1
          except IOError:
            pass
          except UnicodeDecodeError:
            pass
      except IOError:
        pass
    return results

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

  def _process_logs(self):
    """
    Get all available at this moment logs and prints them to logger.
    """
    for level, message, exception in self._drain_queue(self.mp_result_logs):
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
    # cleanup monkey-patching results in child process, as it causing problems
    import subprocess
    reload(subprocess)
    import multiprocessing
    reload(multiprocessing)

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
      for command in commands:
        logger.info("Adding " + command['commandType'] + " for component " + \
                    command['componentName'] + " of service " + \
                    command['serviceName'] + " of cluster " + \
                    command['clusterName'] + " to the queue.")
        self.mp_task_queue.put(command)
        logger.debug(pprint.pformat(command))

  def process_results(self):
    """
    Process all the results from the SCE worker process.
    """
    self._process_logs()
    results = self._drain_queue(self.mp_result_queue)
    logger.debug("Drained %s status commands results, ~%s remains in queue", len(results), self.mp_result_queue.qsize())
    for result in results:
      try:
        self.actionQueue.process_status_command_result(result)
      except UnicodeDecodeError:
        pass

  @property
  def need_relaunch(self):
    """
    Indicates if process need to be relaunched due to timeout or it is dead or even was not created.

    :return: tuple (bool, str|None) with flag to relaunch and reason of relaunch
    """
    if not self.worker_process or not self.worker_process.is_alive():
      return True, "WORKER_DEAD"
    elif self.timedOutEvent.is_set():
      return True, "COMMAND_TIMEOUT"
    return False, None

  def relaunch(self, reason=None):
    """
    Restart status command executor internal process.

    :param reason: reason of restart
    :return:
    """
    with self.kill_lock:
      logger.info("Relaunching child process reason:" + str(reason))
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
    with self.kill_lock:
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