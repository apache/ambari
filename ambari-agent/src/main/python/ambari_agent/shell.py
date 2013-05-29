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
import subprocess
import os
import tempfile
import signal
import sys
import threading
import time
import traceback
import AmbariConfig

try:
    import pwd
except ImportError:
    import winpwd as pwd

global serverTracker
serverTracker = {}
logger = logging.getLogger()

threadLocal = threading.local()

tempFiles = [] 
def noteTempFile(filename):
  tempFiles.append(filename)

def getTempFiles():
  return tempFiles

def killstaleprocesses():
  logger.info ("Killing stale processes")
  prefix = AmbariConfig.config.get('stack','installprefix')
  files = os.listdir(prefix)
  for file in files:
    if str(file).endswith(".pid"):
      pid = str(file).split('.')[0]
      killprocessgrp(int(pid))
      os.unlink(os.path.join(prefix,file))
  logger.info ("Killed stale processes")

def killprocessgrp(pid):
  try:
    os.killpg(pid, signal.SIGTERM)
    time.sleep(5)
    try:
      os.killpg(pid, signal.SIGKILL)
    except:
      logger.warn("Failed to send SIGKILL to PID %d. Process exited?" % (pid))
  except:
    logger.warn("Failed to kill PID %d" % (pid))      

def changeUid():
  try:
    os.setuid(threadLocal.uid)
  except Exception:
    logger.warn("can not switch user for running command.")

class shellRunner:
  # Run any command
  def run(self, script, user=None):
    try:
      if user!=None:
        user = pwd.getpwnam(user)[2]
      else:
        user = os.getuid()
      threadLocal.uid = user
    except Exception:
      logger.warn("can not switch user for RUN_COMMAND.")
    code = 0
    cmd = " "
    cmd = cmd.join(script)
    p = subprocess.Popen(cmd, preexec_fn=changeUid, stdout=subprocess.PIPE, 
                         stderr=subprocess.PIPE, shell=True, close_fds=True)
    out, err = p.communicate()
    code = p.wait()
    logger.debug("Exitcode for %s is %d" % (cmd,code))
    return {'exitCode': code, 'output': out, 'error': err}

  def getServerTracker(self):
    return serverTracker