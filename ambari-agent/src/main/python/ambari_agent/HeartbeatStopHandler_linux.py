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
import logging
import signal
import threading
import traceback


logger = logging.getLogger()

_handler = None

def signal_handler(signum, frame):
  _handler.set_stop()

def bind_signal_handlers(agentPid):
  if os.getpid() == agentPid:
    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)
    signal.signal(signal.SIGUSR1, debug)

  global _handler
  _handler = HeartbeatStopHandler()

  return _handler

def debug(sig, frame):
  """Interrupt running process, and provide a python prompt for
  interactive debugging."""
  d={'_frame':frame}         # Allow access to frame object.
  d.update(frame.f_globals)  # Unless shadowed by global
  d.update(frame.f_locals)

  message  = "Signal received : entering python shell.\nTraceback:\n"
  message += ''.join(traceback.format_stack(frame))
  logger.info(message)

class HeartbeatStopHandler:
  def __init__(self, stopEvent = None):
    # Event is used for synchronizing heartbeat iterations (to make possible
    # manual wait() interruption between heartbeats )
    self.heartbeat_wait_event = threading.Event()

    # Event is used to stop the Agent process
    if stopEvent is None:
      #Allow standalone testing
      self.stop_event = threading.Event()
    else:
      #Allow one unique event per process
      self.stop_event = stopEvent

  def set_heartbeat(self):
    self.heartbeat_wait_event.set()

  def reset_heartbeat(self):
    self.heartbeat_wait_event.clear()

  def set_stop(self):
    self.stop_event.set()

  def wait(self, timeout1, timeout2 = 0):
    if self.heartbeat_wait_event.wait(timeout = timeout1):
      #Event signaled, exit
      return 0
    # Stop loop when stop event received
    # Otherwise sleep a bit more to allow STATUS_COMMAND results to be collected
    # and sent in one heartbeat. Also avoid server overload with heartbeats
    if self.stop_event.wait(timeout = timeout2):
      logger.info("Stop event received")
      return 1
    #Timeout
    return -1
