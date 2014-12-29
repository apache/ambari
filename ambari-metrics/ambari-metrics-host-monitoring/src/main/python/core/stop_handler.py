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

import logging
import os
import signal
import threading
import traceback

from ambari_commons import OSConst, OSCheck
from ambari_commons.exceptions import FatalException
from ambari_commons.os_family_impl import OsFamilyImpl


logger = logging.getLogger()

_handler = None


class StopHandler(object):
  def set_stop(self):
    pass

  def wait(self, timeout=None):
    return -1


#
# Windows implementation
#
@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class StopHandlerWindows(StopHandler):
  def __init__(self, stopEvent=None):
    import win32event
    # Event used to gracefully stop the process
    if stopEvent is None:
      # Allow standalone testing
      self._heventStop = win32event.CreateEvent(None, 0, 0, None)
    else:
      # Allow one unique event per process
      self._heventStop = stopEvent

  def set_stop(self):
    import win32event
    win32event.SetEvent(self._heventStop)

  def wait(self, timeout=None):
    '''
    :param timeout: Time to wait, in seconds.
    :return: 0 == stop event signaled, -1 = timeout
    '''
    import win32event

    if timeout is None:
      timeout = win32event.INFINITE
    else:
      timeout = timeout * 1000

    result = win32event.WaitForSingleObject(self._heventStop, timeout)
    if(win32event.WAIT_OBJECT_0 != result and win32event.WAIT_TIMEOUT != result):
      raise FatalException(-1, "Error waiting for stop event: " + str(result))
    if (win32event.WAIT_TIMEOUT == result):
      return -1
      logger.info("Stop event received")
    return result # 0 -> stop


#
# Linux implementation
#
def signal_handler(signum, frame):
  global _handler
  _handler.set_stop()

def debug(sig, frame):
  """Interrupt running process, and provide a python prompt for
  interactive debugging."""
  d = {'_frame': frame}  # Allow access to frame object.
  d.update(frame.f_globals)  # Unless shadowed by global
  d.update(frame.f_locals)

  message = "Signal received : entering python shell.\nTraceback:\n"
  message += ''.join(traceback.format_stack(frame))
  logger.info(message)


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class StopHandlerLinux(StopHandler):
  def __init__(self, stopEvent=None):
    # Event used to gracefully stop the process
    if stopEvent is None:
      # Allow standalone testing
      self.stop_event = threading.Event()
    else:
      # Allow one unique event per process
      self.stop_event = stopEvent

  def set_stop(self):
    self.stop_event.set()

  def wait(self, timeout=None):
    # Stop process when stop event received
    if self.stop_event.wait(timeout):
      logger.info("Stop event received")
      return 0
    # Timeout
    return -1


def bind_signal_handlers(new_handler=None):
  if OSCheck.get_os_family() != OSConst.WINSRV_FAMILY:
      signal.signal(signal.SIGINT, signal_handler)
      signal.signal(signal.SIGTERM, signal_handler)
      signal.signal(signal.SIGUSR1, debug)

  if new_handler is None:
    global _handler
    _handler = StopHandler()
  else:
    _handler = new_handler
  return _handler
