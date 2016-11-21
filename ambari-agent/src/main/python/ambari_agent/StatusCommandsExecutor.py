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

import os
import signal
import threading
import logging
import multiprocessing
from ambari_agent.PythonReflectiveExecutor import PythonReflectiveExecutor
from ambari_agent.RemoteDebugUtils import bind_debug_signal_handlers
from ambari_agent.ExitHelper import ExitHelper

logger = logging.getLogger(__name__)

class StatusCommandsExecutor(multiprocessing.Process):
  """
  A process which executes status/security status commands.

  It dies and respawns itself on timeout of the command. Which is the most graceful way to end the currently running status command.
  """
  def __init__(self, config, actionQueue):
    multiprocessing.Process.__init__(self)

    self.config = config
    self.actionQueue = actionQueue

    self.status_command_timeout = int(self.config.get('agent', 'status_command_timeout', 5)) # in seconds
    self.hasTimeoutedEvent = multiprocessing.Event()
    ExitHelper().register(self.kill)

  def run(self):
    try:
      bind_debug_signal_handlers()
      while True:
        command = self.actionQueue.statusCommandQueue.get(True) # blocks until status status command appears
        logger.debug("Running status command for {0}".format(command['componentName']))
        
        timeout_timer = threading.Timer( self.status_command_timeout, self.respawn, [command])
        timeout_timer.start()

        self.process_status_command(command)

        timeout_timer.cancel()
        logger.debug("Completed status command for {0}".format(command['componentName']))
    except:
      logger.exception("StatusCommandsExecutor process failed with exception:")
      raise

    logger.warn("StatusCommandsExecutor process has finished")

  def process_status_command(self, command):
    component_status_result = self.actionQueue.customServiceOrchestrator.requestComponentStatus(command)
    component_security_status_result = self.actionQueue.customServiceOrchestrator.requestComponentSecurityState(command)
    result = (command, component_status_result, component_security_status_result)

    self.actionQueue.statusCommandResultQueue.put(result)

  def respawn(self, command):
    try:
      if hasattr(PythonReflectiveExecutor, "last_context"):
        # Force context to reset to normal. By context we mean sys.path, imports, etc. They are set by specific status command, and are not relevant to ambari-agent.
        PythonReflectiveExecutor.last_context.revert()

      logger.warn("Command {0} for {1} is running for more than {2} seconds. Terminating it due to timeout.".format(command['commandType'], command['componentName'], self.status_command_timeout))

      self.hasTimeoutedEvent.set()
    except:
      logger.exception("StatusCommandsExecutor.finish thread failed with exception:")
      raise

  def kill(self):
    os.kill(self.pid, signal.SIGKILL)

    # prevent queue from ending up with non-freed semaphores, locks during put. Which would result in dead-lock in process executing get.
    self.actionQueue.statusCommandResultQueue.close()
    self.actionQueue.statusCommandResultQueue.join_thread()
    self.actionQueue.statusCommandResultQueue = multiprocessing.Queue()
