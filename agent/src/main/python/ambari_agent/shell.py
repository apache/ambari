#!/usr/bin/env python2.6

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
import tempfile
import signal
import sys

global serverTracker
serverTracker = {}
logger = logging.getLogger()

class shellRunner:
  # Run any command
  def run(self, script, user=None):
    code = 0
    cmd = " "
    cmd = cmd.join(script)
    p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True, close_fds=True)
    out, err = p.communicate()
    code = p.wait()
    return {'exitCode': code, 'output': out, 'error': err}

  # dispatch action types
  def runAction(self, clusterId, component, role, user, command, cleanUpCommand, result):
    code = 0
    cmd = sys.executable
    tempfilename = tempfile.mktemp()
    tmp = open(tempfilename, 'w')
    tmp.write(command['script'])
    tmp.close()
    cmd = "%s %s %s" % (cmd, tempfilename, " ".join(command['param']))
    commandResult = {}
    p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True, close_fds=True)
    out, err = p.communicate()
    code = p.wait()
    if code != 0:
      commandResult['output'] = out
      commandResult['error'] = err
    commandResult['exitCode'] = code
    result['commandResult'] = commandResult
    os.unlink(tempfilename)
    if code != 0:
      tempfilename = tempfile.mktemp()
      tmp = open(tempfilename, 'w')
      tmp.write(command['script'])
      tmp.close()
      cmd = sys.executable
      cmd = "%s %s %s" % (cmd, tempfilename, " ".join(cleanUpCommand['param']))
      cleanUpCode = 0
      cleanUpResult = {}
      p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True, close_fds=True)
      out, err = p.communicate()
      cleanUpCode = p.wait()
      if cleanUpCode != 0:
        cleanUpResult['output'] = out
        cleanUpResult['error'] = err
      cleanUpResult['exitCode'] = cleanUpCode
      result['cleanUpResult'] = cleanUpResult
      os.unlink(tempfilename)
    return result

  # Start a process and presist its state
  def startProcess(self, clusterId, bluePrintName, bluePrintRevision, component, role, script, user, result):
    global serverTracker
    code = 0
    commandResult = {}
    process = clusterId+"/"+bluePrintName+"/"+bluePrintRevision+"/"+component+"/"+role
    if not process in serverTracker:
      cmd = sys.executable
      tempfilename = tempfile.mktemp()
      tmp = open(tempfilename, 'w')
      tmp.write(script['script'])
      tmp.close()
      cmd = "%s %s %s" % (cmd, tempfilename, " ".join(script['param']))
      child_pid = os.fork()
      if child_pid == 0:
        p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True, close_fds=True)
        out, err = p.communicate()
        code = p.wait()
        os.unlink(tempfilename)
        serverTracker[process] = None
      else:
        serverTracker[process] = child_pid
        commandResult['exitCode'] = 0
      result['commandResult'] = commandResult
    return result

  # Stop a process and remove presisted state
  def stopProcess(self, clusterId, bluePrintName, bluePrintRevision, component, role, sig, result):
    global serverTracker
    process = clusterId+"/"+bluePrintName+"/"+bluePrintRevision+"/"+component+"/"+role
    commandResult = {'exitCode': 0}
    if process in serverTracker:
      if sig=='TERM':
        os.kill(serverTracker[process], signal.SIGTERM)
        # TODO: gracefully check if process is still alive
        # before remove from serverTracker
        del serverTracker[process]
      else:
        os.kill(serverTracker[process], signal.SIGKILL)
        del serverTracker[process]
    result['commandResult'] = commandResult
    return result

  def getServerTracker(self):
    return serverTracker
