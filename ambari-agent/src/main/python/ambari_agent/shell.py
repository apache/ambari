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

from pwd import getpwnam
from grp import getgrnam
import AmbariConfig
import logging
import logging.handlers
import subprocess
import os
import tempfile
import signal
import sys
import threading
import time
import traceback
import shutil

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
        user=getpwnam(user)[2]
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

  # dispatch action types
  def runAction(self, clusterId, component, role, 
                user, command, cleanUpCommand, result):
    oldDir = os.getcwd()
    #TODO: handle this better. Don't like that it is doing a chdir for the main process
    os.chdir(self.getWorkDir(clusterId, role))
    try:
      if user is not None:
        user=getpwnam(user)[2]
      else:
        user = oldUid
      threadLocal.uid = user
    except Exception:
      logger.warn("%s %s %s can not switch user for RUN_ACTION." 
                  % (clusterId, component, role))
    code = 0
    cmd = sys.executable
    tempfilename = tempfile.mktemp()
    tmp = open(tempfilename, 'w')
    tmp.write(command['script'])
    tmp.close()
    cmd = "%s %s %s" % (cmd, tempfilename, " ".join(command['param']))
    commandResult = {}
    p = subprocess.Popen(cmd, preexec_fn=changeUid, stdout=subprocess.PIPE,
                          stderr=subprocess.PIPE, shell=True, close_fds=True)
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
      p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                            shell=True, close_fds=True)
      out, err = p.communicate()
      cleanUpCode = p.wait()
      if cleanUpCode != 0:
        cleanUpResult['output'] = out
        cleanUpResult['error'] = err
      cleanUpResult['exitCode'] = cleanUpCode
      result['cleanUpResult'] = cleanUpResult
      os.unlink(tempfilename)
      os._exit(1)
    try:
      os.chdir(oldDir)
    except Exception:
      logger.warn("%s %s %s can not restore environment for RUN_ACTION."
                   % (clusterId, component, role))
    return result

  # Start a process and presist its state
  def startProcess(self, clusterId, clusterDefinitionRevision, component,
                    role, script, user, result):
    global serverTracker
    oldDir = os.getcwd()
    try:
      os.chdir(self.getWorkDir(clusterId,role))
    except Exception:
      logger.warn("%s %s %s can not switch dir for START_ACTION."
                   % (clusterId, component, role))
    oldUid = os.getuid()
    try:
      if user is not None:
        user=getpwnam(user)[2]
      else:
        user = os.getuid()
      threadLocal.uid = user
    except Exception:
      logger.warn("%s %s %s can not switch user for START_ACTION." 
                  % (clusterId, component, role))
    code = 0
    commandResult = {}
    process = self.getServerKey(clusterId,clusterDefinitionRevision,
                                component,role)
    if not process in serverTracker:
      try:
        plauncher = processlauncher(script,user)
        plauncher.start()
        plauncher.blockUntilProcessCreation()
      except Exception:
        traceback.print_exc()
        logger.warn("Can not launch process for %s %s %s" 
                    % (clusterId, component, role))
        code = -1
      serverTracker[process] = plauncher
      commandResult['exitCode'] = code 
      result['commandResult'] = commandResult
    try:
      os.chdir(oldDir)
    except Exception:
      logger.warn("%s %s %s can not restore environment for START_ACTION." \
                   % (clusterId, component, role))
    return result

  # Stop a process and remove presisted state
  def stopProcess(self, processKey):
    global serverTracker
    keyFragments = processKey.split('/')
    process = self.getServerKey(keyFragments[0],keyFragments[1],
                                keyFragments[2],keyFragments[3])
    if process in serverTracker:
      logger.info ("Sending %s with PID %d the SIGTERM signal"
                    % (process,serverTracker[process].getpid()))
      killprocessgrp(serverTracker[process].getpid())
      del serverTracker[process]

  def getServerTracker(self):
    return serverTracker

  def getServerKey(self,clusterId, clusterDefinitionRevision, component, role):
    return clusterId+"/"+str(clusterDefinitionRevision)+"/"+component+"/"+role

  def getWorkDir(self, clusterId, role):
    prefix = AmbariConfig.config.get('stack','installprefix')
    return str(os.path.join(prefix, clusterId, role))


class processlauncher(threading.Thread):
  def __init__(self,script,uid):
    threading.Thread.__init__(self)
    self.script = script
    self.serverpid = -1
    self.uid = uid
    self.out = None
    self.err = None

  def run(self):
    try:
      tempfilename = tempfile.mktemp()
      noteTempFile(tempfilename)
      pythoncmd = sys.executable
      tmp = open(tempfilename, 'w')
      tmp.write(self.script['script'])
      tmp.close()
      threadLocal.uid = self.uid
      self.cmd = "%s %s %s" % (pythoncmd, tempfilename,
                                " ".join(self.script['param']))
      logger.info("Launching %s as uid %d" % (self.cmd,self.uid) )
      p = subprocess.Popen(self.cmd,
                            preexec_fn=self.changeUidAndSetSid, 
                            stdout=subprocess.PIPE, 
                            stderr=subprocess.PIPE, shell=True, close_fds=True)
      logger.info("Launched %s; PID %d" % (self.cmd,p.pid))
      self.serverpid = p.pid
      self.out, self.err = p.communicate()
      self.code = p.wait()
      logger.info("%s; PID %d exited with code %d \nSTDOUT: %s\nSTDERR %s" % 
                 (self.cmd,p.pid,self.code,self.out,self.err))
    except:
      logger.warn("Exception encountered while launching : " + self.cmd)
      traceback.print_exc()

    os.unlink(self.getpidfile())
    os.unlink(tempfilename)

  def blockUntilProcessCreation(self):
    self.getpid()
 
  def getpid(self):
    sleepCount = 1
    while (self.serverpid == -1):
      time.sleep(1)
      logger.info("Waiting for process %s to start" % self.cmd)
      if sleepCount > 10:
        logger.warn("Couldn't start process %s even after %d seconds"
                     % (self.cmd,sleepCount))
        os._exit(1)
    return self.serverpid

  def getpidfile(self):
    prefix = AmbariConfig.config.get('stack','installprefix')
    pidfile = os.path.join(prefix,str(self.getpid())+".pid")
    return pidfile
 
  def changeUidAndSetSid(self):
    prefix = AmbariConfig.config.get('stack','installprefix')
    pidfile = os.path.join(prefix,str(os.getpid())+".pid")
    #TODO remove try/except (when there is a way to provide
    #config files for testcases). The default config will want
    #to create files in /var/ambari which may not exist unless
    #specifically created.
    #At that point add a testcase for the pid file management.
    try: 
      f = open(pidfile,'w')
      f.close()
    except:
      logger.warn("Couldn't write pid file %s for %s" % (pidfile,self.cmd))
    changeUid()
    os.setsid() 
