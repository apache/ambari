# !/usr/bin/env python

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
import pprint
import platform

from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyImpl, OsFamilyFuncImpl

logger = logging.getLogger()

shellRunner = None
threadLocal = threading.local()
gracefull_kill_delay = 5  # seconds between SIGTERM and SIGKILL

tempFiles = []


def noteTempFile(filename):
  tempFiles.append(filename)


def getTempFiles():
  return tempFiles


class _dict_to_object:
  def __init__(self, entries):
    self.__dict__.update(entries)

  def __getitem__(self, item):
    return self.__dict__[item]


# windows specific code
@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def kill_process_with_children(parent_pid):
  shellRunnerWindows().run(["taskkill", "/F", "/T", "/PID", "{0}".format(parent_pid)])

class shellRunner(object):
  def run(self, script, user=None):
    pass

  def runPowershell(self, file=None, script_block=None, args=[]):
    raise NotImplementedError()

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class shellRunnerWindows(shellRunner):
  # Run any command
  def run(self, script, user=None):
    logger.warn("user argument ignored on windows")
    code = 0
    if isinstance(script, list):
      cmd = " ".join(script)
    else:
      cmd = script
    p = subprocess.Popen(cmd, stdout=subprocess.PIPE,
                         stderr=subprocess.PIPE, shell=False)
    out, err = p.communicate()
    code = p.wait()
    logger.debug("Exitcode for %s is %d" % (cmd, code))
    return {'exitCode': code, 'output': out, 'error': err}

  def runPowershell(self, file=None, script_block=None, args=[]):
    logger.warn("user argument ignored on windows")
    code = 0
    cmd = None
    if file:
      cmd = ['powershell', '-WindowStyle', 'Hidden', '-File', file] + args
    elif script_block:
      cmd = ['powershell', '-WindowStyle', 'Hidden', '-Command', script_block] + args
    p = subprocess.Popen(cmd, stdout=subprocess.PIPE,
                         stderr=subprocess.PIPE, shell=False)
    out, err = p.communicate()
    code = p.wait()
    logger.debug("Exitcode for %s is %d" % (cmd, code))
    return _dict_to_object({'exitCode': code, 'output': out, 'error': err})


#linux specific code
@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def kill_process_with_children(parent_pid):
  def kill_tree_function(pid, signal):
    '''
    Kills process tree starting from a given pid.
    '''
    # The command below starts 'ps' linux utility and then parses it's
    # output using 'awk'. AWK recursively extracts PIDs of all children of
    # a given PID and then passes list of "kill -<SIGNAL> PID" commands to 'sh'
    # shell.
    CMD = """ps xf | awk -v PID=""" + str(pid) + \
          """ ' $1 == PID { P = $1; next } P && /_/ { P = P " " $1;""" + \
          """K=P } P && !/_/ { P="" }  END { print "kill -""" \
          + str(signal) + """ "K }' | sh """
    process = subprocess.Popen(CMD, stdout=subprocess.PIPE,
                               stderr=subprocess.PIPE, shell=True)
    process.communicate()

  _run_kill_function(kill_tree_function, parent_pid)


def _run_kill_function(kill_function, pid):
  try:
    kill_function(pid, signal.SIGTERM)
  except Exception, e:
    logger.warn("Failed to kill PID %d" % (pid))
    logger.warn("Reported error: " + repr(e))

  time.sleep(gracefull_kill_delay)

  try:
    kill_function(pid, signal.SIGKILL)
  except Exception, e:
    logger.error("Failed to send SIGKILL to PID %d. Process exited?" % (pid))
    logger.error("Reported error: " + repr(e))


def _changeUid():
  try:
    os.setuid(threadLocal.uid)
  except Exception:
    logger.warn("can not switch user for running command.")


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class shellRunnerLinux(shellRunner):
  # Run any command
  def run(self, script, user=None):
    import pwd

    try:
      if user != None:
        user = pwd.getpwnam(user)[2]
      else:
        user = os.getuid()
      threadLocal.uid = user
    except Exception:
      logger.warn("can not switch user for RUN_COMMAND.")

    cmd = script
    
    if isinstance(script, list):
      cmd = " ".join(script)

    cmd_list = ["/bin/bash","--login","--noprofile","-c", cmd]
    p = subprocess.Popen(cmd_list, preexec_fn=_changeUid, stdout=subprocess.PIPE,
                         stderr=subprocess.PIPE, shell=False, close_fds=True)
    out, err = p.communicate()
    code = p.wait()
    logger.debug("Exitcode for %s is %d" % (cmd, code))
    return {'exitCode': code, 'output': out, 'error': err}


@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def changeUid():
  #No Windows implementation
  pass

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def changeUid():
  _changeUid()
