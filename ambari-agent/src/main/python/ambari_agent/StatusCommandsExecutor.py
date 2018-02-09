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
      if logger.isEnabledFor(logging.DEBUG):
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

# TODO make reliable MultiProcessStatusCommandsExecutor implementation
MultiProcessStatusCommandsExecutor = SingleProcessStatusCommandsExecutor
