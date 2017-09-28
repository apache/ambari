# !/usr/bin/env python

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

import logging
import os
import signal
import subprocess
import threading
from contextlib import contextmanager

from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyImpl, OsFamilyFuncImpl
from ambari_commons.process_utils import get_flat_process_tree, kill_pids, wait_for_entire_process_tree_death, \
  get_processes_running, get_command_by_pid

logger = logging.getLogger()

threadLocal = threading.local()

# default timeout for async invoked processes
TIMEOUT_SECONDS = 300

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


def launch_subprocess(command):
  """
  Process launch helper

  :param command Command to execute
  :type command list[str]|str
  :return Popen object
  """
  is_shell = not isinstance(command, (list, tuple))
  return subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=is_shell, close_fds=True)


def watchdog_func(event, cmd, exec_timeout):
  """
  Watchdog function for subprocess executors

  :type event Event
  :type cmd Popen
  :type exec_timeout int


  Usage example:
      event = threading.Event()

      cmd = Popen(...)

      thread = threading.Thread(target=watchdog_func, args=(event, cmd, execution_timeout,))
      thread.start()

      ....cmd.communicate() or any another processing....

      event.set()
      thread.join()
      ......result code....
  """
  event.wait(exec_timeout)
  if cmd.returncode is None:
    logger.error("Task timed out and will be killed")
    kill_process_with_children(cmd.pid)


def subprocess_with_timeout(command, execution_timeout=None):
  """
  Run command with limited time for execution, after timeout command would be killed

  :param command Command to execute
  :param execution_timeout execution time limit in seconds. Defaulting to TIMEOUT_SECONDS global constant

  :type command list[str]|str
  :type execution_timeout int
  :rtype dict
  """
  event = threading.Event()

  if execution_timeout is None:
    execution_timeout = TIMEOUT_SECONDS

  os_stat = launch_subprocess(command)
  logger.debug("Launching watchdog thread")

  event.clear()

  thread = threading.Thread(target=watchdog_func, args=(event, os_stat, execution_timeout,))
  thread.start()

  out, err = os_stat.communicate()

  result = {
    "out": out,
    "err": err,
    "retCode": os_stat.returncode
  }

  event.set()
  thread.join()
  return result


@contextmanager
def process_executor(command, timeout=None, error_callback=None):
  """
  Context manager for command execution

  :type command list|str
  :type timeout None|int
  :type error_callback func

  :return stdout stream

  Usage example:

   Option 1. Basic
     with process_executor(["ls", "-la]) as stdout:
       for line in stdout:
         print line

   Option 2. Extended
     def error_handler(command, error_log, exit_code):
       print "Command '{}' failed".format(command)
       print "Exit Code: {}   StdOut: {} \n".format(exit_code, "\n".join(error_log))

     with process_executor(["ls", "-la], timeout=10, error_callback=error_handler) as stdout:
       for line in stdout:
         print line

  """
  if not timeout:
    timeout = TIMEOUT_SECONDS

  event = threading.Event()
  cmd = launch_subprocess(command)

  thread = threading.Thread(target=watchdog_func, args=(event, cmd, timeout,))
  thread.start()

  yield cmd.stdout

  exit_code = cmd.poll()
  event.set()
  thread.join()

  if exit_code is None:
    kill_process_with_children(cmd.pid)

  if error_callback and exit_code and exit_code > 0:
    error_callback(command, cmd.stderr.readlines(), exit_code)


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
  """
  Kills process tree starting from a given pid.
  :param parent_pid: head of tree
  :param graceful_kill_delays: map <command name, custom delay between SIGTERM and SIGKILL>
  :return:
  """

  pids = get_flat_process_tree(parent_pid)
  try:
    kill_pids(pids, signal.SIGTERM)
  except Exception, e:
    logger.warn("Failed to kill PID %d" % parent_pid)
    logger.warn("Reported error: " + repr(e))

  wait_for_entire_process_tree_death(pids)

  try:
    running_processes = get_processes_running(pids)
    if running_processes:
      process_names = map(lambda x: get_command_by_pid(x),  running_processes)
      logger.warn("These PIDs %s did not die after SIGTERM, sending SIGKILL. Exact commands to be killed:\n %s" %
                  (", ".join(running_processes), "\n".join(process_names)))
      kill_pids(running_processes, signal.SIGKILL)
  except Exception, e:
    logger.error("Failed to send SIGKILL to PID %d. Process exited?" % parent_pid)
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
