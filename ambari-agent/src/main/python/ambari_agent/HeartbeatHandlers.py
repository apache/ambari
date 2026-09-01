#!/usr/bin/env python3

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

from ambari_commons.exceptions import FatalException
from ambari_commons.os_check import OSConst, OSCheck
import os
import logging
import signal
import sys
import threading
import traceback
from ambari_commons.os_family_impl import OsFamilyImpl

logger = logging.getLogger()

_handler = None


class HeartbeatStopHandlers(object):
  pass


# linux impl


def signal_handler(signum, frame):
  logger.info(f"Ambari-agent received {signum} signal, stopping...")
  _handler.set()


def debug(sig, frame):
  """Interrupt running process, and provide a stacktrace of threads"""
  d = {"_frame": frame}  # Allow access to frame object.
  d.update(frame.f_globals)  # Uamnless shadowed by global
  d.update(frame.f_locals)

  message = "Signal received.\nTraceback:\n"
  message += "".join(traceback.format_stack(frame))
  logger.info(message)


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class HeartbeatStopHandlersLinux(HeartbeatStopHandlers):
  def __init__(self):
    self.heartbeat_wait_event = threading.Event()
    self._stop = False

  def set_heartbeat(self):
    self.heartbeat_wait_event.set()

  def reset_heartbeat(self):
    self.heartbeat_wait_event.clear()

  def set_stop(self):
    self._stop = True

  def wait(self, timeout1, timeout2=0):
    if self._stop:
      logger.info("Stop event received")
      return 0

    if self.heartbeat_wait_event.wait(timeout=timeout1):
      return 1
    return -1


def bind_signal_handlers(agentPid, stop_event):
  global _handler
  if os.getpid() == agentPid:
    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)
    signal.signal(signal.SIGUSR1, log_thread_stack_traces)

  _handler = stop_event
  return _handler


def log_thread_stack_traces(sig, frame):
  logger.warning("*** THREAD STACK TRACES - START ***")
  for thread_id, stack in sys._current_frames().items():
    logger.warning("ThreadID: %s", thread_id)
    for filename, lineno, name, line in traceback.extract_stack(stack):
      logger.warning('File: "%s", line %s, in %s', filename, lineno, name)
      if line:
        logger.warning("  %s", line.strip())
  logger.warning("*** THREAD STACK TRACES - END ***")
