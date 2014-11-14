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

import win32event

from ambari_commons.exceptions import FatalException


def bind_signal_handlers(agentPid):
  return HeartbeatStopHandler()


class HeartbeatStopHandler:
  def __init__(self, stopEvent = None):
    # Event is used for synchronizing heartbeat iterations (to make possible
    # manual wait() interruption between heartbeats )
    self._heventHeartbeat = win32event.CreateEvent(None, 0, 0, None)

    # Event is used to stop the Agent process
    if stopEvent is None:
      #Allow standalone testing
      self._heventStop = win32event.CreateEvent(None, 0, 0, None)
    else:
      #Allow one unique event per process
      self._heventStop = stopEvent

  def set_heartbeat(self):
    win32event.SetEvent(self._heventHeartbeat)

  def reset_heartbeat(self):
    win32event.ResetEvent(self._heventHeartbeat)

  def wait(self, timeout1, timeout2 = 0):
    timeout = int(timeout1 + timeout2) * 1000

    result = win32event.WaitForMultipleObjects([self._heventStop, self._heventHeartbeat], False, timeout)
    if(win32event.WAIT_OBJECT_0 != result and win32event.WAIT_OBJECT_0 + 1 != result and win32event.WAIT_TIMEOUT != result):
      raise FatalException(-1, "Error waiting for stop/heartbeat events: " + string(result))
    if(win32event.WAIT_TIMEOUT == result):
      return -1
    return result - win32event.WAIT_OBJECT_0
