#!/usr/bin/python

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
import logging.handlers
import subprocess
import os
import signal

global serverTracker
serverTracker = {}
logger = logging.getLogger()

class shellRunner:
  # Run any command
  def run(self, script, user):
    code = 0
    cmd = " "
    cmd = cmd.join(script)
    p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True, close_fds=True)
    out, err = p.communicate()
    if p.wait() != 0:
      code = 1
    return {'exit_code': code, 'output': out, 'error': err}

  # Start a process and presist its state
  def startProcess(self, component, role, script, user):
    global serverTracker
    process = component+"."+role
    if not process in serverTracker:
      cmd = " "
      cmd = cmd.join(script)
      child_pid = os.fork()
      if child_pid == 0:
        p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True, close_fds=True)
        p.wait()
        serverTracker[process] = None
      else:
        serverTracker[process] = child_pid

  # Stop a process and remove presisted state
  def stopProcess(self, component, role, sig):
    global serverTracker
    process = component+"."+role
    if process in serverTracker:
      if sig=='TERM':
        os.kill(serverTracker[process], signal.SIGTERM)
        # TODO: gracefully check if process is still alive
        # before remove from serverTracker
        del serverTracker[process]
      else:
        os.kill(serverTracker[process], signal.SIGKILL)
        del serverTracker[process]

  def getServerTracker(self):
    return serverTracker
